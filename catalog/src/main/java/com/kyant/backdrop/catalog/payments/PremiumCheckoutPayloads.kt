package com.kyant.backdrop.catalog.payments

import com.kyant.backdrop.catalog.network.models.PremiumPlanOption

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
