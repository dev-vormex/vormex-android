package com.kyant.backdrop.catalog.network.models

import kotlinx.serialization.Serializable

@Serializable
data class PremiumSubscriptionResponse(
    val plan: String = "free",
    val status: String = "inactive",
    val provider: String = "manual",
    val isPremium: Boolean = false,
    val isCreatorPro: Boolean = false,
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
    val developerPremiumOverrideAvailable: Boolean = false,
    val developerPremiumOverrideActive: Boolean = false,
    val planOptions: List<PremiumPlanOption> = emptyList(),
    val creatorPro: CreatorProPlanSummary = CreatorProPlanSummary(),
    val entitlements: PremiumEntitlements = PremiumEntitlements(),
    val profileBoost: PremiumProfileBoostState = PremiumProfileBoostState()
)

@Serializable
data class PremiumEntitlements(
    val connectionRequests: PremiumConnectionRequestEntitlement = PremiumConnectionRequestEntitlement(),
    val profileBoost: PremiumProfileBoostEntitlement = PremiumProfileBoostEntitlement(),
    val priorityDiscovery: Boolean = true,
    val featuredFeedPlacement: Boolean = true,
    val requestQueuePriority: Boolean = true
)

@Serializable
data class PremiumConnectionRequestEntitlement(
    val freeLimit: Int = 10,
    val freeWindow: String = "day",
    val premiumLimit: Int? = null,
    val unlimitedForPremium: Boolean = true
)

@Serializable
data class PremiumProfileBoostEntitlement(
    val durationHours: Int = 4,
    val priority: Int = 120
)

@Serializable
data class PremiumProfileBoostState(
    val active: Boolean = false,
    val endsAt: String? = null,
    val priority: Int = 120,
    val durationHours: Int = 4,
    val canActivate: Boolean = false,
    val isPremium: Boolean = false
)

@Serializable
data class PremiumProfileBoostResponse(
    val profileBoost: PremiumProfileBoostState = PremiumProfileBoostState()
)

@Serializable
data class ActivateProfileBoostRequest(
    val durationHours: Int? = null
)

@Serializable
data class ActivateProfileBoostResponse(
    val message: String = "",
    val profileBoost: PremiumProfileBoostState = PremiumProfileBoostState(),
    val subscription: PremiumSubscriptionResponse? = null
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
data class CreatorProPlanSummary(
    val plan: String = "creator_pro",
    val isActive: Boolean = false,
    val title: String = "Vormex Creator Pro",
    val description: String = "",
    val ctaLabel: String = "Upgrade to Creator Pro",
    val features: List<String> = emptyList(),
    val planOptions: List<PremiumPlanOption> = emptyList()
)

@Serializable
data class CreatorProResponse(
    val access: CreatorProAccess = CreatorProAccess(),
    val settings: CreatorProSettings = CreatorProSettings(),
    val analytics: CreatorProAnalytics? = null,
    val subscription: PremiumSubscriptionResponse? = null,
    val message: String = ""
)

@Serializable
data class CreatorProAccess(
    val plan: String = "creator_pro",
    val isCreatorPro: Boolean = false,
    val isPremium: Boolean = false,
    val canUseCreatorPro: Boolean = false,
    val premiumRequired: Boolean = true,
    val title: String = "Vormex Creator Pro",
    val description: String = "",
    val features: List<String> = emptyList(),
    val planOptions: List<PremiumPlanOption> = emptyList()
)

@Serializable
data class CreatorProSettings(
    val monetizedDmEnabled: Boolean = false,
    val dmPriceMinor: Int = 0,
    val dmDisplayPrice: String = "",
    val dmPlatformFeeMinor: Int = 0,
    val dmCreatorReceivesMinor: Int = 0,
    val dmCreatorReceivesDisplay: String = "",
    val sessionBookingEnabled: Boolean = false,
    val sessionPriceMinor: Int = 0,
    val sessionDisplayPrice: String = "",
    val sessionDurationMinutes: Int = 30,
    val sessionCurrency: String = "INR",
    val sessionPlatformFeeMinor: Int = 0,
    val sessionCreatorReceivesMinor: Int = 0,
    val sessionCreatorReceivesDisplay: String = "",
    val platformFeeBps: Int = 1000,
    val collabPriorityEnabled: Boolean = true,
    val showcaseAmplificationEnabled: Boolean = true,
    val portfolioAmplificationEnabled: Boolean = true,
    val availabilityNote: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class CreatorProSettingsRequest(
    val monetizedDmEnabled: Boolean? = null,
    val dmPriceMinor: Int? = null,
    val sessionBookingEnabled: Boolean? = null,
    val sessionPriceMinor: Int? = null,
    val sessionDurationMinutes: Int? = null,
    val sessionCurrency: String? = null,
    val collabPriorityEnabled: Boolean? = null,
    val showcaseAmplificationEnabled: Boolean? = null,
    val portfolioAmplificationEnabled: Boolean? = null,
    val availabilityNote: String? = null
)

@Serializable
data class CreatorProAnalytics(
    val audience: CreatorProAudienceAnalytics = CreatorProAudienceAnalytics(),
    val collab: CreatorProCollabAnalytics = CreatorProCollabAnalytics(),
    val content: CreatorProContentAnalytics = CreatorProContentAnalytics(),
    val monetization: CreatorProMonetizationAnalytics = CreatorProMonetizationAnalytics(),
    val showcase: CreatorProShowcaseAnalytics = CreatorProShowcaseAnalytics()
)

@Serializable
data class CreatorProAudienceAnalytics(
    val profileViewsTotal: Int = 0,
    val profileViewsLast7Days: Int = 0,
    val profileViewsLast30Days: Int = 0,
    val uniqueViewers: Int = 0,
    val profileSavesTotal: Int = 0,
    val profileSavesLast30Days: Int = 0,
    val searchAppearancesLast30Days: Int = 0,
    val suggestionAppearancesLast30Days: Int = 0,
    val matchRateDisplay: String = "0%",
    val connectionRequestsLast30Days: Int = 0,
    val acceptedConnectionsLast30Days: Int = 0
)

@Serializable
data class CreatorProCollabAnalytics(
    val priorityEnabled: Boolean = true,
    val totalInvites: Int = 0,
    val accepted: Int = 0,
    val acceptanceRate: Double = 0.0
)

@Serializable
data class CreatorProContentAnalytics(
    val reels: CreatorProReelAnalytics = CreatorProReelAnalytics(),
    val posts: CreatorProPostAnalytics = CreatorProPostAnalytics(),
    val collaborations: CreatorProCollabSummary = CreatorProCollabSummary()
)

@Serializable
data class CreatorProReelAnalytics(
    val count: Int = 0,
    val views: Int = 0,
    val likes: Int = 0,
    val comments: Int = 0,
    val shares: Int = 0,
    val saves: Int = 0,
    val averageWatchTimeMs: Int = 0,
    val completionRate: Double = 0.0
)

@Serializable
data class CreatorProPostAnalytics(
    val count: Int = 0,
    val likes: Int = 0,
    val comments: Int = 0,
    val shares: Int = 0
)

@Serializable
data class CreatorProCollabSummary(
    val totalInvites: Int = 0,
    val accepted: Int = 0
)

@Serializable
data class CreatorProMonetizationAnalytics(
    val dmEnabled: Boolean = false,
    val sessionEnabled: Boolean = false,
    val currency: String = "INR",
    val dmPriceMinor: Int = 0,
    val sessionPriceMinor: Int = 0,
    val platformFeeBps: Int = 1000
)

@Serializable
data class CreatorProShowcaseAnalytics(
    val showcaseAmplificationEnabled: Boolean = true,
    val portfolioAmplificationEnabled: Boolean = true,
    val topTags: List<CreatorProTag> = emptyList(),
    val reasons: List<String> = emptyList()
)

@Serializable
data class CreatorProTag(
    val label: String = "",
    val weight: Double = 0.0,
    val source: String = ""
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
data class DeveloperPremiumOverrideRequest(
    val enabled: Boolean
)

@Serializable
data class DeveloperCreatorProOverrideRequest(
    val enabled: Boolean
)
