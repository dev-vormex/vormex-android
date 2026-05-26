package com.kyant.backdrop.catalog.payments

import com.android.billingclient.api.BillingClient
import java.security.MessageDigest

internal const val PREMIUM_PLAY_PRODUCT_ID = "vormex_premium"
internal const val PREMIUM_PLAY_MONTHLY_BASE_PLAN_ID = "premium-monthly-prepaid"
internal const val PREMIUM_PLAY_YEARLY_BASE_PLAN_ID = "premium-yearly-prepaid"

data class PremiumPlayPlanOffer(
    val billingCycle: String,
    val basePlanId: String,
    val offerToken: String,
    val formattedPrice: String,
    val priceAmountMicros: Long,
    val priceCurrencyCode: String
)

internal fun premiumBillingCycleForBasePlan(basePlanId: String?): String? {
    return when (basePlanId?.trim()) {
        PREMIUM_PLAY_MONTHLY_BASE_PLAN_ID -> "monthly"
        PREMIUM_PLAY_YEARLY_BASE_PLAN_ID -> "yearly"
        else -> null
    }
}

internal fun selectPremiumPlayOffer(
    offers: List<PremiumPlayPlanOffer>,
    billingCycle: String
): PremiumPlayPlanOffer? {
    val normalizedBillingCycle = billingCycle.trim().lowercase()
    return offers.firstOrNull { it.billingCycle == normalizedBillingCycle }
        ?: offers.firstOrNull { it.billingCycle == "monthly" }
        ?: offers.firstOrNull()
}

internal fun googlePlayObfuscatedAccountId(userId: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(userId.toByteArray(Charsets.UTF_8))
    return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
}

internal fun resolvePremiumPlayBillingErrorMessage(
    responseCode: Int,
    debugMessage: String? = null
): String? {
    val normalizedDebugMessage = debugMessage?.trim().orEmpty()
    return when (responseCode) {
        BillingClient.BillingResponseCode.OK -> null
        BillingClient.BillingResponseCode.USER_CANCELED -> null
        BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
        BillingClient.BillingResponseCode.NETWORK_ERROR -> {
            "Google Play billing is temporarily unavailable. Please try again."
        }
        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {
            "Google Play billing is not available on this device."
        }
        BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
            "Premium is already linked to this Google Play account. Restoring access..."
        }
        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> {
            "Vormex Premium is not available in Google Play yet."
        }
        BillingClient.BillingResponseCode.DEVELOPER_ERROR -> {
            "Premium purchase setup needs attention. Please try again later."
        }
        else -> normalizedDebugMessage.ifBlank {
            "Premium purchase could not be completed. Please try again."
        }
    }
}
