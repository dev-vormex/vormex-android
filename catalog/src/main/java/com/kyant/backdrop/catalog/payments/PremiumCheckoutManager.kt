package com.kyant.backdrop.catalog.payments

import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.kyant.backdrop.catalog.BuildConfig
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.network.ApiClient
import com.razorpay.Checkout
import com.razorpay.PaymentData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    private const val PENDING_PREFS = "vormex_premium_razorpay_checkout"
    private const val KEY_PENDING_ORDER_ID = "pending_order_id"
    private const val KEY_PENDING_BILLING_CYCLE = "pending_billing_cycle"
    private const val KEY_PENDING_AMOUNT_MINOR = "pending_amount_minor"
    private const val KEY_PENDING_CURRENCY = "pending_currency"

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var appContext: Context? = null
    private var pendingCheckout: PendingPremiumCheckout? = null

    private val _checkoutState = MutableStateFlow(PremiumCheckoutState())
    val checkoutState = _checkoutState.asStateFlow()

    private val _refreshSignal = MutableStateFlow(0L)
    val refreshSignal = _refreshSignal.asStateFlow()
    private val _celebrationSignal = MutableStateFlow(0L)
    val celebrationSignal = _celebrationSignal.asStateFlow()

    fun preload(context: Context) {
        val applicationContext = context.applicationContext
        appContext = applicationContext
        runCatching {
            Checkout.preload(applicationContext)
        }.onSuccess {
            _checkoutState.update { it.copy(isReady = true, errorMessage = null) }
        }.onFailure { error ->
            _checkoutState.update {
                it.copy(
                    isReady = false,
                    errorMessage = error.message ?: "Razorpay checkout could not be prepared."
                )
            }
        }
    }

    fun loadPremiumPlans(context: Context) {
        preload(context)
    }

    fun restorePurchases(context: Context) {
        appContext = context.applicationContext
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

        appContext = activity.applicationContext
        _checkoutState.update {
            it.copy(
                isProcessingPurchase = true,
                errorMessage = null,
                pendingMessage = null,
                lastErrorCode = null,
                lastDebugMessage = null
            )
        }

        managerScope.launch {
            val checkoutResult = ApiClient.createPremiumCheckout(
                context = activity.applicationContext,
                billingCycle = billingCycle,
                plan = plan
            )

            checkoutResult
                .onSuccess { response ->
                    val options = runCatching {
                        buildPremiumRazorpayCheckoutOptions(response)
                    }.getOrElse { error ->
                        failCheckout(
                            context = activity,
                            message = error.message ?: "Premium checkout could not be started."
                        )
                        return@onSuccess
                    }

                    val pending = PendingPremiumCheckout(
                        orderId = response.orderId,
                        billingCycle = response.billingCycle,
                        amountMinor = response.amountMinor,
                        currency = response.currency
                    )
                    rememberPendingCheckout(activity.applicationContext, pending)
                    _checkoutState.update {
                        it.copy(
                            isReady = true,
                            isProcessingPurchase = true,
                            errorMessage = null,
                            lastOrderId = pending.orderId
                        )
                    }

                    runCatching {
                        val checkout = Checkout().apply {
                            setKeyID(response.keyId.orEmpty())
                            setImage(R.drawable.vormex_logo)
                            setFullScreenDisable(true)
                        }
                        if (BuildConfig.DEBUG) {
                            Checkout.sdkCheckIntegration(activity)
                        }
                        checkout.open(activity, options)
                    }.onFailure { error ->
                        clearPendingCheckout(activity.applicationContext)
                        failCheckout(
                            context = activity,
                            message = error.message ?: "Razorpay checkout could not be opened."
                        )
                    }
                }
                .onFailure { error ->
                    failCheckout(
                        context = activity,
                        message = error.message ?: "Unable to start premium checkout right now."
                    )
                }
        }
    }

    fun onPaymentSuccess(
        context: Context,
        razorpayPaymentId: String?,
        paymentData: PaymentData?
    ) {
        val applicationContext = context.applicationContext
        appContext = applicationContext
        val pending = recoverPendingCheckout(applicationContext)
        val paymentId = paymentData?.paymentId?.takeIf { it.isNotBlank() }
            ?: razorpayPaymentId?.trim()
        val orderId = paymentData?.orderId?.takeIf { it.isNotBlank() }
            ?: pending?.orderId
        val signature = paymentData?.signature?.takeIf { it.isNotBlank() }
        val verifyRequest = buildPremiumVerifyRequestOrNull(
            razorpayOrderId = orderId,
            razorpayPaymentId = paymentId,
            razorpaySignature = signature
        )

        if (verifyRequest == null) {
            failCheckout(
                context = context,
                message = "Payment succeeded, but verification details were missing. Please contact support."
            )
            return
        }

        if (pending != null && pending.orderId != verifyRequest.razorpayOrderId) {
            failCheckout(
                context = context,
                message = "Payment order did not match the active checkout. Please contact support."
            )
            return
        }

        _checkoutState.update {
            it.copy(
                isProcessingPurchase = true,
                errorMessage = null,
                pendingMessage = null,
                lastOrderId = verifyRequest.razorpayOrderId
            )
        }

        managerScope.launch {
            val verificationResult = ApiClient.verifyPremiumCheckout(
                context = applicationContext,
                request = verifyRequest
            )

            _checkoutState.update { it.copy(isProcessingPurchase = false) }
            verificationResult
                .onSuccess { response ->
                    clearPendingCheckout(applicationContext)
                    Toast.makeText(
                        applicationContext,
                        response.message.ifBlank { "Premium unlocked successfully." },
                        Toast.LENGTH_SHORT
                    ).show()
                    notifyPremiumStateChanged(triggerCelebration = true)
                }
                .onFailure { error ->
                    val message = error.message ?: "Razorpay payment verification failed."
                    _checkoutState.update { it.copy(errorMessage = message) }
                    Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
                }
        }
    }

    fun onPaymentError(
        context: Context,
        code: Int,
        response: String?,
        paymentData: PaymentData?
    ) {
        appContext = context.applicationContext
        clearPendingCheckout(context.applicationContext)
        val message = resolveRazorpayCheckoutErrorMessage(code, response)
        _checkoutState.update {
            it.copy(
                isProcessingPurchase = false,
                errorMessage = message,
                pendingMessage = null,
                lastErrorCode = code,
                lastDebugMessage = response?.take(240),
                lastOrderId = paymentData?.orderId?.takeIf { orderId -> orderId.isNotBlank() }
            )
        }
        if (!message.isNullOrBlank()) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    fun clearUserData(context: Context) {
        val applicationContext = context.applicationContext
        clearPendingCheckout(applicationContext)
        runCatching { Checkout.clearUserData(applicationContext) }
    }

    fun notifyPremiumStateChanged(triggerCelebration: Boolean = false) {
        val now = System.currentTimeMillis()
        _refreshSignal.value = now
        if (triggerCelebration) {
            _celebrationSignal.value = now
        }
    }

    private fun failCheckout(context: Context, message: String) {
        _checkoutState.update {
            it.copy(
                isProcessingPurchase = false,
                errorMessage = message,
                pendingMessage = null
            )
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun rememberPendingCheckout(context: Context, pending: PendingPremiumCheckout) {
        pendingCheckout = pending
        context.getSharedPreferences(PENDING_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PENDING_ORDER_ID, pending.orderId)
            .putString(KEY_PENDING_BILLING_CYCLE, pending.billingCycle)
            .putInt(KEY_PENDING_AMOUNT_MINOR, pending.amountMinor)
            .putString(KEY_PENDING_CURRENCY, pending.currency)
            .apply()
    }

    private fun recoverPendingCheckout(context: Context): PendingPremiumCheckout? {
        pendingCheckout?.let { return it }
        val prefs = context.getSharedPreferences(PENDING_PREFS, Context.MODE_PRIVATE)
        val orderId = prefs.getString(KEY_PENDING_ORDER_ID, null)?.takeIf { it.isNotBlank() }
            ?: return null
        return PendingPremiumCheckout(
            orderId = orderId,
            billingCycle = prefs.getString(KEY_PENDING_BILLING_CYCLE, null).orEmpty(),
            amountMinor = prefs.getInt(KEY_PENDING_AMOUNT_MINOR, 0),
            currency = prefs.getString(KEY_PENDING_CURRENCY, null).orEmpty()
        ).also {
            pendingCheckout = it
        }
    }

    private fun clearPendingCheckout(context: Context) {
        pendingCheckout = null
        context.getSharedPreferences(PENDING_PREFS, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    private data class PendingPremiumCheckout(
        val orderId: String,
        val billingCycle: String,
        val amountMinor: Int,
        val currency: String
    )
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
