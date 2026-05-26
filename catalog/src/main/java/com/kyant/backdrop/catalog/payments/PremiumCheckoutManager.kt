package com.kyant.backdrop.catalog.payments

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
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

data class PremiumPlayBillingState(
    val isReady: Boolean = false,
    val isLoadingPlans: Boolean = false,
    val isProcessingPurchase: Boolean = false,
    val plans: List<PremiumPlayPlanOffer> = emptyList(),
    val errorMessage: String? = null,
    val pendingMessage: String? = null,
    val lastResponseCode: Int? = null,
    val lastDebugMessage: String? = null
) {
    val isAvailable: Boolean
        get() = plans.isNotEmpty()
}

object PremiumCheckoutManager : PurchasesUpdatedListener {
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val pendingClientCallbacks = mutableListOf<(BillingClient) -> Unit>()
    private val pendingConnectionFailureCallbacks = mutableListOf<(String) -> Unit>()
    private val verifyingPurchaseTokens = linkedSetOf<String>()
    private val verifiedPurchaseTokens = linkedSetOf<String>()

    private var billingClient: BillingClient? = null
    private var productDetails: ProductDetails? = null
    private var appContext: Context? = null
    private var isConnecting = false

    private val _billingState = MutableStateFlow(PremiumPlayBillingState())
    val billingState = _billingState.asStateFlow()

    private val _refreshSignal = MutableStateFlow(0L)
    val refreshSignal = _refreshSignal.asStateFlow()
    private val _celebrationSignal = MutableStateFlow(0L)
    val celebrationSignal = _celebrationSignal.asStateFlow()

    fun preload(context: Context) {
        appContext = context.applicationContext
        ensureConnected(context.applicationContext)
    }

    fun loadPremiumPlans(context: Context) {
        appContext = context.applicationContext
        queryPremiumProduct(context.applicationContext)
    }

    fun restorePurchases(context: Context) {
        appContext = context.applicationContext
        ensureConnected(context.applicationContext) { client ->
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
            client.queryPurchasesAsync(params) { billingResult, purchases ->
                if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    _billingState.update {
                        it.copy(
                            isReady = client.isReady,
                            errorMessage = resolvePremiumPlayBillingErrorMessage(
                                billingResult.responseCode,
                                billingResult.debugMessage
                            ),
                            lastResponseCode = billingResult.responseCode,
                            lastDebugMessage = billingResult.debugMessage
                        )
                    }
                    return@queryPurchasesAsync
                }

                purchases
                    .filter { purchase -> purchase.products.contains(PREMIUM_PLAY_PRODUCT_ID) }
                    .forEach { purchase ->
                        processPurchase(
                            context = context.applicationContext,
                            purchase = purchase,
                            showUserFeedback = false,
                            triggerCelebration = false
                        )
                    }
            }
        }
    }

    fun startCheckout(
        activity: ComponentActivity,
        billingCycle: String,
        userId: String?
    ) {
        val normalizedUserId = userId?.trim().orEmpty()
        if (normalizedUserId.isBlank()) {
            Toast.makeText(activity, "Please sign in to upgrade to Premium.", Toast.LENGTH_SHORT).show()
            return
        }

        appContext = activity.applicationContext
        _billingState.update {
            it.copy(isProcessingPurchase = true, errorMessage = null, pendingMessage = null)
        }

        ensureConnected(
            context = activity.applicationContext,
            onFailure = { message ->
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
            }
        ) {
            val launchWithCurrentProduct = {
                val details = productDetails
                val offer = selectPremiumPlayOffer(_billingState.value.plans, billingCycle)
                if (details == null || offer == null) {
                    val message = "Google Play cannot find this Premium plan yet. Check Play Console setup and try again."
                    _billingState.update {
                        it.copy(
                            isProcessingPurchase = false,
                            errorMessage = message
                        )
                    }
                    Toast.makeText(
                        activity,
                        message,
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    launchBillingFlow(
                        activity = activity,
                        productDetails = details,
                        offer = offer,
                        userId = normalizedUserId
                    )
                }
            }

            if (productDetails == null || _billingState.value.plans.isEmpty()) {
                queryPremiumProduct(activity.applicationContext) { launchWithCurrentProduct() }
            } else {
                launchWithCurrentProduct()
            }
        }
    }

    fun openGooglePlayManagement(context: Context, productId: String? = PREMIUM_PLAY_PRODUCT_ID) {
        val packageName = context.packageName
        val url = if (productId.isNullOrBlank()) {
            "https://play.google.com/store/account/subscriptions"
        } else {
            "https://play.google.com/store/account/subscriptions?sku=$productId&package=$packageName"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
            .onFailure {
                Toast.makeText(
                    context,
                    "Open Google Play subscriptions to manage Premium.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        val context = appContext
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val premiumPurchases = purchases
                    .orEmpty()
                    .filter { purchase -> purchase.products.contains(PREMIUM_PLAY_PRODUCT_ID) }
                if (context != null && premiumPurchases.isNotEmpty()) {
                    premiumPurchases.forEach { purchase ->
                        processPurchase(
                            context = context,
                            purchase = purchase,
                            showUserFeedback = true,
                            triggerCelebration = true
                        )
                    }
                } else {
                    _billingState.update { it.copy(isProcessingPurchase = false) }
                }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                _billingState.update { it.copy(isProcessingPurchase = false) }
                context?.let {
                    Toast.makeText(
                        it,
                        "Premium is already linked to this Google Play account. Restoring access...",
                        Toast.LENGTH_SHORT
                    ).show()
                    restorePurchases(it)
                }
            }
            else -> {
                val message = resolvePremiumPlayBillingErrorMessage(
                    billingResult.responseCode,
                    billingResult.debugMessage
                )
                _billingState.update {
                    it.copy(
                        isProcessingPurchase = false,
                        errorMessage = message,
                        lastResponseCode = billingResult.responseCode,
                        lastDebugMessage = billingResult.debugMessage
                    )
                }
                if (message != null && context != null) {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun notifyPremiumStateChanged(triggerCelebration: Boolean = false) {
        val now = System.currentTimeMillis()
        _refreshSignal.value = now
        if (triggerCelebration) {
            _celebrationSignal.value = now
        }
    }

    private fun ensureConnected(
        context: Context,
        onFailure: ((String) -> Unit)? = null,
        onReady: ((BillingClient) -> Unit)? = null
    ) {
        val client = billingClient ?: buildBillingClient(context.applicationContext).also {
            billingClient = it
        }

        if (client.isReady) {
            _billingState.update { it.copy(isReady = true, errorMessage = null) }
            onReady?.invoke(client)
            return
        }

        onReady?.let { pendingClientCallbacks += it }
        onFailure?.let { pendingConnectionFailureCallbacks += it }
        if (isConnecting) return

        isConnecting = true
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                isConnecting = false
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _billingState.update { it.copy(isReady = true, errorMessage = null) }
                    val callbacks = pendingClientCallbacks.toList()
                    pendingClientCallbacks.clear()
                    pendingConnectionFailureCallbacks.clear()
                    callbacks.forEach { callback -> callback(client) }
                } else {
                    val message = resolvePremiumPlayBillingErrorMessage(
                        billingResult.responseCode,
                        billingResult.debugMessage
                    ) ?: "Google Play billing is temporarily unavailable. Please try again."
                    _billingState.update {
                        it.copy(
                            isReady = false,
                            isLoadingPlans = false,
                            isProcessingPurchase = false,
                            errorMessage = message,
                            lastResponseCode = billingResult.responseCode,
                            lastDebugMessage = billingResult.debugMessage
                        )
                    }
                    val failureCallbacks = pendingConnectionFailureCallbacks.toList()
                    pendingClientCallbacks.clear()
                    pendingConnectionFailureCallbacks.clear()
                    failureCallbacks.forEach { callback -> callback(message) }
                }
            }

            override fun onBillingServiceDisconnected() {
                isConnecting = false
                _billingState.update { it.copy(isReady = false) }
            }
        })
    }

    private fun buildBillingClient(context: Context): BillingClient {
        val pendingParams = PendingPurchasesParams.newBuilder()
            .enableOneTimeProducts()
            .enablePrepaidPlans()
            .build()
        return BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(pendingParams)
            .build()
    }

    private fun queryPremiumProduct(
        context: Context,
        onComplete: (() -> Unit)? = null
    ) {
        _billingState.update { it.copy(isLoadingPlans = true, errorMessage = null) }
        ensureConnected(context.applicationContext) { client ->
            val product = QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_PLAY_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(listOf(product))
                .build()
            client.queryProductDetailsAsync(params) { billingResult, productDetailsResult ->
                if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    _billingState.update {
                        it.copy(
                            isLoadingPlans = false,
                            errorMessage = resolvePremiumPlayBillingErrorMessage(
                                billingResult.responseCode,
                                billingResult.debugMessage
                            ),
                            lastResponseCode = billingResult.responseCode,
                            lastDebugMessage = billingResult.debugMessage
                        )
                    }
                    onComplete?.invoke()
                    return@queryProductDetailsAsync
                }

                val details = productDetailsResult.productDetailsList
                    .firstOrNull { it.productId == PREMIUM_PLAY_PRODUCT_ID }
                productDetails = details
                val offers = details?.toPremiumPlayPlanOffers().orEmpty()
                _billingState.update {
                    val emptyOfferMessage = if (offers.isEmpty()) {
                        "No active offers returned for $PREMIUM_PLAY_PRODUCT_ID. Check Play Console product, base plan IDs, pricing, tester access, and install source."
                    } else {
                        billingResult.debugMessage
                    }
                    it.copy(
                        isReady = client.isReady,
                        isLoadingPlans = false,
                        plans = offers,
                        errorMessage = if (offers.isEmpty()) {
                            "Vormex Premium is not available in Google Play yet."
                        } else {
                            null
                        },
                        lastResponseCode = billingResult.responseCode,
                        lastDebugMessage = emptyOfferMessage
                    )
                }
                onComplete?.invoke()
            }
        }
    }

    private fun ProductDetails.toPremiumPlayPlanOffers(): List<PremiumPlayPlanOffer> {
        return subscriptionOfferDetails
            .orEmpty()
            .mapNotNull { offer ->
                val billingCycle = premiumBillingCycleForBasePlan(offer.basePlanId)
                    ?: return@mapNotNull null
                val pricingPhase = offer.pricingPhases.pricingPhaseList.lastOrNull()
                    ?: return@mapNotNull null
                PremiumPlayPlanOffer(
                    billingCycle = billingCycle,
                    basePlanId = offer.basePlanId,
                    offerToken = offer.offerToken,
                    formattedPrice = pricingPhase.formattedPrice,
                    priceAmountMicros = pricingPhase.priceAmountMicros,
                    priceCurrencyCode = pricingPhase.priceCurrencyCode
                )
            }
            .distinctBy { offer -> offer.billingCycle }
            .sortedBy { offer -> if (offer.billingCycle == "monthly") 0 else 1 }
    }

    private fun launchBillingFlow(
        activity: ComponentActivity,
        productDetails: ProductDetails,
        offer: PremiumPlayPlanOffer,
        userId: String
    ) {
        val client = billingClient
        if (client == null || !client.isReady) {
            _billingState.update {
                it.copy(
                    isProcessingPurchase = false,
                    errorMessage = "Google Play billing is temporarily unavailable. Please try again."
                )
            }
            Toast.makeText(
                activity,
                "Google Play billing is temporarily unavailable. Please try again.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(offer.offerToken)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .setObfuscatedAccountId(googlePlayObfuscatedAccountId(userId))
            .build()
        val billingResult = client.launchBillingFlow(activity, flowParams)
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            val message = resolvePremiumPlayBillingErrorMessage(
                billingResult.responseCode,
                billingResult.debugMessage
            )
            _billingState.update {
                it.copy(
                    isProcessingPurchase = false,
                    errorMessage = message,
                    lastResponseCode = billingResult.responseCode,
                    lastDebugMessage = billingResult.debugMessage
                )
            }
            if (message != null) {
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
            }
            if (billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                restorePurchases(activity.applicationContext)
            }
        }
    }

    private fun processPurchase(
        context: Context,
        purchase: Purchase,
        showUserFeedback: Boolean,
        triggerCelebration: Boolean
    ) {
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> verifyPurchasedPremium(
                context = context,
                purchase = purchase,
                showUserFeedback = showUserFeedback,
                triggerCelebration = triggerCelebration
            )
            Purchase.PurchaseState.PENDING -> {
                _billingState.update {
                    it.copy(
                        isProcessingPurchase = false,
                        pendingMessage = "Google Play payment is pending. Premium unlocks after payment completes."
                    )
                }
                if (showUserFeedback) {
                    Toast.makeText(
                        context,
                        "Google Play payment is pending. Premium unlocks after payment completes.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            else -> {
                _billingState.update { it.copy(isProcessingPurchase = false) }
            }
        }
    }

    private fun verifyPurchasedPremium(
        context: Context,
        purchase: Purchase,
        showUserFeedback: Boolean,
        triggerCelebration: Boolean
    ) {
        val purchaseToken = purchase.purchaseToken.takeIf { it.isNotBlank() } ?: return
        if (purchaseToken in verifiedPurchaseTokens) return
        if (!verifyingPurchaseTokens.add(purchaseToken)) return

        _billingState.update {
            it.copy(isProcessingPurchase = true, errorMessage = null, pendingMessage = null)
        }

        managerScope.launch {
            if (ApiClient.getToken(context).isNullOrBlank()) {
                verifyingPurchaseTokens.remove(purchaseToken)
                _billingState.update { it.copy(isProcessingPurchase = false) }
                return@launch
            }

            val verificationResult = ApiClient.verifyGooglePlayPremium(
                context = context,
                request = GooglePlayPremiumVerifyRequest(
                    productId = PREMIUM_PLAY_PRODUCT_ID,
                    purchaseToken = purchaseToken
                )
            )
            verifyingPurchaseTokens.remove(purchaseToken)
            _billingState.update { it.copy(isProcessingPurchase = false) }

            verificationResult
                .onSuccess { response ->
                    verifiedPurchaseTokens += purchaseToken
                    if (showUserFeedback) {
                        Toast.makeText(
                            context,
                            response.message.ifBlank { "Premium unlocked successfully." },
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    notifyPremiumStateChanged(triggerCelebration = triggerCelebration)
                }
                .onFailure { error ->
                    val message = error.message ?: "Google Play payment verification failed."
                    _billingState.update { it.copy(errorMessage = message) }
                    if (showUserFeedback) {
                        Toast.makeText(
                            context,
                            message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }
}

fun Context.findComponentActivity(): ComponentActivity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is ComponentActivity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}
