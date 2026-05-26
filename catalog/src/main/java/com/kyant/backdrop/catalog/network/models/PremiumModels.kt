package com.kyant.backdrop.catalog.network.models

import kotlinx.serialization.Serializable

@Serializable
data class PremiumSubscriptionResponse(
    val plan: String = "free",
    val status: String = "inactive",
    val provider: String = "manual",
    val isPremium: Boolean = false,
    val title: String = "Vormex Premium",
    val description: String = "",
    val amountMinor: Int = 0,
    val currency: String = "INR",
    val displayAmount: String = "",
    val billingCycle: String = "one_time",
    val checkoutEnabled: Boolean = false,
    val ctaLabel: String = "Get Premium",
    val features: List<String> = emptyList(),
    val canUseAgent: Boolean = false,
    val canAccessProfileCustomization: Boolean = false,
    val customPriceApplied: Boolean = false,
    val premiumStartedAt: String? = null,
    val premiumEndsAt: String? = null,
    val premiumDurationDays: Int = 31,
    val premiumDaysRemaining: Int = 0,
    val autoPayEnabled: Boolean = false,
    val renewalModeLabel: String = "Manual renewal",
    val supportLabel: String = "24/7 fast support",
    val creditsUsed: Int = 0,
    val canCancel: Boolean = false,
    val canManageInGooglePlay: Boolean = false,
    val googlePlayProductId: String? = null,
    val googlePlayBasePlanId: String? = null,
    val googlePlaySubscriptionState: String? = null,
    val developerPremiumOverrideAvailable: Boolean = false,
    val developerPremiumOverrideActive: Boolean = false,
    val planOptions: List<PremiumPlanOption> = emptyList()
)

@Serializable
data class PremiumPlanOption(
    val billingCycle: String = "monthly",
    val amountMinor: Int = 0,
    val currency: String = "INR",
    val displayAmount: String = "",
    val durationDays: Int = 31,
    val label: String = "",
    val savingsLabel: String? = null
)

@Serializable
data class PremiumCheckoutPrefill(
    val name: String? = null,
    val email: String? = null
)

@Serializable
data class PremiumCheckoutResponse(
    val keyId: String? = null,
    val orderId: String = "",
    val amountMinor: Int = 0,
    val currency: String = "INR",
    val displayAmount: String = "",
    val title: String = "Vormex Premium",
    val description: String = "",
    val billingCycle: String = "one_time",
    val prefill: PremiumCheckoutPrefill = PremiumCheckoutPrefill(),
    val features: List<String> = emptyList()
)

@Serializable
data class PremiumVerifyRequest(
    val razorpayOrderId: String,
    val razorpayPaymentId: String,
    val razorpaySignature: String
)

@Serializable
data class PremiumVerifyResponse(
    val message: String = "",
    val subscription: PremiumSubscriptionResponse? = null
)

@Serializable
data class GooglePlayPremiumVerifyRequest(
    val productId: String,
    val purchaseToken: String
)
