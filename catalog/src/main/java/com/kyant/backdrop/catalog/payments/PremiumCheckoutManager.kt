package com.kyant.backdrop.catalog.payments

import android.content.Context
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.models.GooglePlayPremiumVerifyRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.MessageDigest

data class PremiumCheckoutState(
    val isReady: Boolean = false,
    val isProcessingPurchase: Boolean = false,
    val errorMessage: String? = null,
    val pendingMessage: String? = null,
    val lastErrorCode: Int? = null,
    val lastDebugMessage: String? = null,
    val lastOrderId: String? = null
)

object PremiumCheckoutManager {
    private const val PREMIUM_PRODUCT_ID = "vormex_premium"
    private const val MONTHLY_BASE_PLAN_ID = "premium-monthly-prepaid"
    private const val YEARLY_BASE_PLAN_ID = "premium-yearly-prepaid"

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val verifyingPurchaseTokens = mutableSetOf<String>()
    private val readyCallbacks = mutableListOf<(BillingClient) -> Unit>()
    private val failureCallbacks = mutableListOf<(String) -> Unit>()

    private var appContext: Context? = null
    private var billingClient: BillingClient? = null
    private var isConnecting = false

    private val _checkoutState = MutableStateFlow(PremiumCheckoutState())
    val checkoutState = _checkoutState.asStateFlow()

    private val _refreshSignal = MutableStateFlow(0L)
    val refreshSignal = _refreshSignal.asStateFlow()
    private val _celebrationSignal = MutableStateFlow(0L)
    val celebrationSignal = _celebrationSignal.asStateFlow()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        val context = appContext
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (context == null || purchases.isNullOrEmpty()) {
                    _checkoutState.update { it.copy(isProcessingPurchase = false) }
                    return@PurchasesUpdatedListener
                }
                purchases.forEach { purchase -> processPurchase(context, purchase, showToast = true) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _checkoutState.update {
                    it.copy(
                        isProcessingPurchase = false,
                        pendingMessage = null,
                        lastErrorCode = null,
                        lastDebugMessage = billingResult.debugMessage.takeIf { message -> message.isNotBlank() }
                    )
                }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                if (context != null) {
                    queryOwnedPremiumPurchases(context, showToast = true)
                } else {
                    failCheckout(null, "Premium already exists on this Google Play account. Sign in and tap retry.")
                }
            }
            else -> failCheckout(
                context = context,
                message = billingResult.toUserMessage("Google Play purchase could not be completed."),
                billingResult = billingResult
            )
        }
    }

    fun preload(context: Context) {
        val applicationContext = context.applicationContext
        appContext = applicationContext
        withReadyBillingClient(
            context = applicationContext,
            onReady = {
                _checkoutState.update { it.copy(isReady = true, errorMessage = null) }
                queryOwnedPremiumPurchases(applicationContext, showToast = false)
            },
            onFailure = { message ->
                _checkoutState.update { it.copy(isReady = false, errorMessage = message) }
            }
        )
    }

    fun loadPremiumPlans(context: Context) {
        preload(context)
    }

    fun restorePurchases(context: Context) {
        val applicationContext = context.applicationContext
        appContext = applicationContext
        queryOwnedPremiumPurchases(applicationContext, showToast = true)
    }

    fun startCheckout(
        activity: ComponentActivity,
        billingCycle: String,
        userId: String?,
        plan: String = "premium"
    ) {
        val normalizedUserId = userId?.trim().orEmpty()
        if (normalizedUserId.isBlank()) {
            Toast.makeText(activity, "Please sign in to upgrade to Premium.", Toast.LENGTH_SHORT).show()
            return
        }
        if (plan != "premium") {
            Toast.makeText(
                activity,
                "Creator Pro checkout is not available in this Play Store build yet.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        appContext = activity.applicationContext
        _checkoutState.update {
            it.copy(
                isProcessingPurchase = true,
                errorMessage = null,
                pendingMessage = null,
                lastErrorCode = null,
                lastDebugMessage = null,
                lastOrderId = null
            )
        }

        withReadyBillingClient(
            context = activity.applicationContext,
            onReady = { client ->
                queryPremiumProductDetails(
                    client = client,
                    billingCycle = billingCycle,
                    onSuccess = { productDetails, offerDetails ->
                        launchPremiumBillingFlow(
                            activity = activity,
                            client = client,
                            productDetails = productDetails,
                            offerDetails = offerDetails,
                            userId = normalizedUserId
                        )
                    },
                    onFailure = { message -> failCheckout(activity, message) }
                )
            },
            onFailure = { message -> failCheckout(activity, message) }
        )
    }

    fun clearUserData(context: Context) {
        appContext = context.applicationContext
        verifyingPurchaseTokens.clear()
        readyCallbacks.clear()
        failureCallbacks.clear()
        isConnecting = false
        billingClient?.endConnection()
        billingClient = null
        _checkoutState.value = PremiumCheckoutState()
    }

    fun notifyPremiumStateChanged(triggerCelebration: Boolean = false) {
        val now = System.currentTimeMillis()
        _refreshSignal.value = now
        if (triggerCelebration) {
            _celebrationSignal.value = now
        }
    }

    private fun withReadyBillingClient(
        context: Context,
        onReady: (BillingClient) -> Unit,
        onFailure: (String) -> Unit
    ) {
        managerScope.launch {
            val client = getOrCreateBillingClient(context.applicationContext)
            if (client.isReady) {
                _checkoutState.update { it.copy(isReady = true, errorMessage = null) }
                onReady(client)
                return@launch
            }

            readyCallbacks += onReady
            failureCallbacks += onFailure
            if (!isConnecting) {
                startBillingConnection(client)
            }
        }
    }

    private fun getOrCreateBillingClient(context: Context): BillingClient {
        billingClient?.let { return it }
        val pendingPurchasesParams = PendingPurchasesParams.newBuilder()
            .enableOneTimeProducts()
            .enablePrepaidPlans()
            .build()
        return BillingClient.newBuilder(context.applicationContext)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(pendingPurchasesParams)
            .build()
            .also { billingClient = it }
    }

    private fun startBillingConnection(client: BillingClient) {
        isConnecting = true
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                managerScope.launch {
                    isConnecting = false
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        _checkoutState.update { it.copy(isReady = true, errorMessage = null) }
                        val callbacks = readyCallbacks.toList()
                        readyCallbacks.clear()
                        failureCallbacks.clear()
                        callbacks.forEach { callback -> callback(client) }
                    } else {
                        val message = billingResult.toUserMessage("Google Play Billing is not ready yet.")
                        _checkoutState.update {
                            it.copy(
                                isReady = false,
                                isProcessingPurchase = false,
                                errorMessage = message,
                                lastErrorCode = billingResult.responseCode,
                                lastDebugMessage = billingResult.debugMessage.take(240)
                            )
                        }
                        val callbacks = failureCallbacks.toList()
                        readyCallbacks.clear()
                        failureCallbacks.clear()
                        callbacks.forEach { callback -> callback(message) }
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                managerScope.launch {
                    isConnecting = false
                    _checkoutState.update { it.copy(isReady = false) }
                }
            }
        })
    }

    private fun queryPremiumProductDetails(
        client: BillingClient,
        billingCycle: String,
        onSuccess: (ProductDetails, ProductDetails.SubscriptionOfferDetails) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PREMIUM_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()

        client.queryProductDetailsAsync(params) { billingResult, productDetailsResult ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                onFailure(billingResult.toUserMessage("Could not load Google Play premium plans."))
                return@queryProductDetailsAsync
            }

            val productDetails = productDetailsResult.productDetailsList
                .firstOrNull { it.productId == PREMIUM_PRODUCT_ID }
            if (productDetails == null) {
                onFailure("Vormex Premium is not available in Google Play for this build yet.")
                return@queryProductDetailsAsync
            }

            val offerDetails = selectPremiumOffer(productDetails, billingCycle)
            if (offerDetails == null) {
                onFailure("Vormex Premium plan is missing a Google Play base plan.")
                return@queryProductDetailsAsync
            }

            onSuccess(productDetails, offerDetails)
        }
    }

    private fun selectPremiumOffer(
        productDetails: ProductDetails,
        billingCycle: String
    ): ProductDetails.SubscriptionOfferDetails? {
        val targetBasePlanId = when (normalizePremiumCheckoutBillingCycle(billingCycle)) {
            "yearly" -> YEARLY_BASE_PLAN_ID
            else -> MONTHLY_BASE_PLAN_ID
        }
        val offers = productDetails.subscriptionOfferDetails.orEmpty()
        return offers.firstOrNull { it.basePlanId == targetBasePlanId }
            ?: offers.firstOrNull { it.basePlanId == MONTHLY_BASE_PLAN_ID }
            ?: offers.firstOrNull()
    }

    private fun launchPremiumBillingFlow(
        activity: ComponentActivity,
        client: BillingClient,
        productDetails: ProductDetails,
        offerDetails: ProductDetails.SubscriptionOfferDetails,
        userId: String
    ) {
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(offerDetails.offerToken)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .setObfuscatedAccountId(googlePlayObfuscatedAccountId(userId))
            .build()
        val billingResult = client.launchBillingFlow(activity, flowParams)

        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            _checkoutState.update {
                it.copy(
                    isReady = true,
                    isProcessingPurchase = true,
                    errorMessage = null,
                    pendingMessage = null
                )
            }
            return
        }

        if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            _checkoutState.update { it.copy(isProcessingPurchase = false, pendingMessage = null) }
            return
        }

        failCheckout(
            context = activity,
            message = billingResult.toUserMessage("Google Play checkout could not be opened."),
            billingResult = billingResult
        )
    }

    private fun queryOwnedPremiumPurchases(context: Context, showToast: Boolean) {
        withReadyBillingClient(
            context = context,
            onReady = { client ->
                val params = QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
                client.queryPurchasesAsync(params) { billingResult, purchases ->
                    if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                        if (showToast) {
                            failCheckout(
                                context = context,
                                message = billingResult.toUserMessage("Could not restore Google Play purchases."),
                                billingResult = billingResult
                            )
                        }
                        return@queryPurchasesAsync
                    }

                    val premiumPurchases = purchases.filter { purchase ->
                        purchase.products.any { it == PREMIUM_PRODUCT_ID }
                    }
                    if (premiumPurchases.isEmpty()) {
                        _checkoutState.update { it.copy(isProcessingPurchase = false) }
                        if (showToast) {
                            Toast.makeText(
                                context,
                                "No active Premium purchase found in Google Play.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        return@queryPurchasesAsync
                    }
                    premiumPurchases.forEach { purchase ->
                        processPurchase(context, purchase, showToast = showToast)
                    }
                }
            },
            onFailure = { message ->
                if (showToast) failCheckout(context, message)
            }
        )
    }

    private fun processPurchase(context: Context, purchase: Purchase, showToast: Boolean) {
        if (purchase.products.none { it == PREMIUM_PRODUCT_ID }) return

        when (purchase.purchaseState) {
            Purchase.PurchaseState.PENDING -> {
                _checkoutState.update {
                    it.copy(
                        isProcessingPurchase = false,
                        errorMessage = null,
                        pendingMessage = "Your Google Play payment is pending. Premium unlocks automatically after payment completes."
                    )
                }
                if (showToast) {
                    Toast.makeText(context, "Google Play payment is pending.", Toast.LENGTH_SHORT).show()
                }
            }
            Purchase.PurchaseState.PURCHASED -> verifyPurchaseToken(context, purchase.purchaseToken, showToast)
            else -> {
                _checkoutState.update {
                    it.copy(
                        isProcessingPurchase = false,
                        errorMessage = "Google Play purchase is not active yet.",
                        pendingMessage = null
                    )
                }
            }
        }
    }

    private fun verifyPurchaseToken(context: Context, purchaseToken: String, showToast: Boolean) {
        if (purchaseToken.isBlank() || purchaseToken in verifyingPurchaseTokens) return
        verifyingPurchaseTokens += purchaseToken
        _checkoutState.update {
            it.copy(
                isProcessingPurchase = true,
                errorMessage = null,
                pendingMessage = "Verifying Google Play purchase..."
            )
        }

        managerScope.launch {
            try {
                val result = ApiClient.verifyGooglePlayPremiumCheckout(
                    context = context.applicationContext,
                    request = GooglePlayPremiumVerifyRequest(
                        productId = PREMIUM_PRODUCT_ID,
                        purchaseToken = purchaseToken
                    )
                )

                result
                    .onSuccess { response ->
                        _checkoutState.update {
                            it.copy(
                                isReady = true,
                                isProcessingPurchase = false,
                                errorMessage = null,
                                pendingMessage = null
                            )
                        }
                        if (showToast) {
                            Toast.makeText(
                                context,
                                response.message.ifBlank { "Premium unlocked successfully." },
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        notifyPremiumStateChanged(triggerCelebration = true)
                    }
                    .onFailure { error ->
                        val message = error.message ?: "Google Play purchase verification failed."
                        _checkoutState.update {
                            it.copy(
                                isProcessingPurchase = false,
                                errorMessage = message,
                                pendingMessage = null
                            )
                        }
                        if (showToast) Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
            } finally {
                verifyingPurchaseTokens -= purchaseToken
            }
        }
    }

    private fun failCheckout(
        context: Context?,
        message: String,
        billingResult: BillingResult? = null
    ) {
        _checkoutState.update {
            it.copy(
                isProcessingPurchase = false,
                errorMessage = message,
                pendingMessage = null,
                lastErrorCode = billingResult?.responseCode,
                lastDebugMessage = billingResult?.debugMessage?.take(240)
            )
        }
        context?.let { Toast.makeText(it, message, Toast.LENGTH_LONG).show() }
    }

    private fun BillingResult.toUserMessage(fallback: String): String {
        val debugMessage = debugMessage.trim().takeIf { it.isNotBlank() }
        return when (responseCode) {
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE ->
                "Google Play Billing is not available on this device or Play account."
            BillingClient.BillingResponseCode.DEVELOPER_ERROR ->
                "Google Play premium is not configured correctly for this app build."
            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED ->
                "Google Play subscriptions are not supported on this device."
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE ->
                "Vormex Premium is not available for this Google Play account yet."
            BillingClient.BillingResponseCode.NETWORK_ERROR,
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE ->
                "Google Play Billing could not connect. Please check your connection and try again."
            else -> debugMessage ?: fallback
        }
    }

    private fun googlePlayObfuscatedAccountId(userId: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(userId.toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }
}
