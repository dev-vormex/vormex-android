package com.kyant.backdrop.catalog.payments

import com.kyant.backdrop.catalog.network.models.PremiumCheckoutResponse
import com.kyant.backdrop.catalog.network.models.PremiumPlanOption
import com.kyant.backdrop.catalog.network.models.PremiumVerifyRequest
import com.razorpay.Checkout
import com.razorpay.PayloadHelper
import org.json.JSONArray
import org.json.JSONObject

private const val RAZORPAY_MERCHANT_NAME = "Vormex"
private const val RAZORPAY_THEME_COLOR = "#111827"

internal fun normalizePremiumCheckoutBillingCycle(billingCycle: String?): String {
    return when (billingCycle?.trim()?.lowercase()) {
        "yearly", "annual", "annually" -> "yearly"
        "monthly", "month" -> "monthly"
        else -> "monthly"
    }
}

internal fun selectPremiumCheckoutPlanOption(
    planOptions: List<PremiumPlanOption>,
    billingCycle: String
): PremiumPlanOption? {
    val normalizedBillingCycle = normalizePremiumCheckoutBillingCycle(billingCycle)
    return planOptions.firstOrNull { option ->
        normalizePremiumCheckoutBillingCycle(option.billingCycle) == normalizedBillingCycle
    } ?: planOptions.firstOrNull { option ->
        normalizePremiumCheckoutBillingCycle(option.billingCycle) == "monthly"
    } ?: planOptions.firstOrNull()
}

internal fun buildPremiumRazorpayCheckoutOptions(
    checkout: PremiumCheckoutResponse
): JSONObject {
    val keyId = checkout.keyId?.trim().orEmpty()
    val orderId = checkout.orderId.trim()
    val amountMinor = checkout.amountMinor
    val currency = checkout.currency.trim().uppercase().ifBlank { "INR" }
    val description = checkout.description.trim().ifBlank { checkout.title.trim() }
        .ifBlank { "Vormex Premium" }

    require(keyId.isNotBlank()) { "Razorpay key is missing from the checkout response." }
    require(orderId.isNotBlank()) { "Razorpay order is missing from the checkout response." }
    require(amountMinor > 0) { "Premium checkout amount is invalid." }

    val payloadHelper = PayloadHelper(currency, amountMinor, orderId).apply {
        name = RAZORPAY_MERCHANT_NAME
        this.description = description
        color = RAZORPAY_THEME_COLOR
        retryEnabled = true
        retryMaxCount = 4
        modalConfirmClose = true
        checkout.prefill.name?.trim()?.takeIf { it.isNotBlank() }?.let { prefillName = it }
        checkout.prefill.email?.trim()?.takeIf { it.isNotBlank() }?.let { prefillEmail = it }
    }

    return payloadHelper.getJson().apply {
        put("key", keyId)
        put("config", buildPremiumPaymentMethodsConfig())
    }
}

private fun buildPremiumPaymentMethodsConfig(): JSONObject {
    val instruments = JSONArray().apply {
        put(JSONObject().put("method", "upi"))
        put(JSONObject().put("method", "card"))
        put(JSONObject().put("method", "netbanking"))
        put(JSONObject().put("method", "wallet"))
    }
    val allMethodsBlock = JSONObject().apply {
        put("name", "All payment options")
        put("instruments", instruments)
    }
    val blocks = JSONObject().put("all_methods", allMethodsBlock)
    val display = JSONObject().apply {
        put("blocks", blocks)
        put("sequence", JSONArray().put("block.all_methods"))
        put(
            "preferences",
            JSONObject().put("show_default_blocks", false)
        )
    }
    return JSONObject().put("display", display)
}

internal fun buildPremiumVerifyRequestOrNull(
    razorpayOrderId: String?,
    razorpayPaymentId: String?,
    razorpaySignature: String?
): PremiumVerifyRequest? {
    val orderId = razorpayOrderId?.trim().orEmpty()
    val paymentId = razorpayPaymentId?.trim().orEmpty()
    val signature = razorpaySignature?.trim().orEmpty()

    if (orderId.isBlank() || paymentId.isBlank() || signature.isBlank()) return null
    return PremiumVerifyRequest(
        razorpayOrderId = orderId,
        razorpayPaymentId = paymentId,
        razorpaySignature = signature
    )
}

internal fun resolveRazorpayCheckoutErrorMessage(
    code: Int,
    response: String? = null
): String? {
    if (code == Checkout.PAYMENT_CANCELED) return null

    val gatewayDescription = extractRazorpayErrorDescription(response)
    return when (code) {
        Checkout.NETWORK_ERROR -> {
            "Razorpay checkout could not reach the network. Please check your connection and try again."
        }
        Checkout.INVALID_OPTIONS -> {
            gatewayDescription ?: "Premium checkout could not be started. Please try again."
        }
        Checkout.TLS_ERROR -> {
            "This device could not establish a secure Razorpay connection."
        }
        Checkout.INCOMPATIBLE_PLUGIN -> {
            "Razorpay checkout could not open with this app version."
        }
        else -> gatewayDescription ?: "Premium payment could not be completed. Please try again."
    }
}

private fun extractRazorpayErrorDescription(response: String?): String? {
    val raw = response?.trim().orEmpty()
    if (raw.isBlank()) return null
    val parsedDescription = runCatching {
        val root = JSONObject(raw)
        root.optJSONObject("error")?.optString("description")
            ?: root.optString("description")
            ?: root.optString("message")
    }.getOrNull()
    return parsedDescription
        ?.trim()
        ?.takeIf { it.isNotBlank() && !it.startsWith("{") }
        ?.take(180)
}
