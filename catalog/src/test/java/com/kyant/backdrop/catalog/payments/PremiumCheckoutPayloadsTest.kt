package com.kyant.backdrop.catalog.payments

import com.kyant.backdrop.catalog.network.models.PremiumPlanOption
import org.junit.Assert.assertEquals
import org.junit.Test

class PremiumCheckoutPayloadsTest {
    @Test
    fun `normalizePremiumCheckoutBillingCycle maps yearly aliases and defaults to monthly`() {
        assertEquals("yearly", normalizePremiumCheckoutBillingCycle("yearly"))
        assertEquals("yearly", normalizePremiumCheckoutBillingCycle("annual"))
        assertEquals("yearly", normalizePremiumCheckoutBillingCycle(" Annually "))
        assertEquals("monthly", normalizePremiumCheckoutBillingCycle("monthly"))
        assertEquals("monthly", normalizePremiumCheckoutBillingCycle("weekly"))
        assertEquals("monthly", normalizePremiumCheckoutBillingCycle(null))
    }

    @Test
    fun `selectPremiumCheckoutPlanOption picks the requested billing cycle`() {
        val options = listOf(
            premiumPlan("monthly", "INR 99"),
            premiumPlan("yearly", "INR 999")
        )

        assertEquals("monthly", selectPremiumCheckoutPlanOption(options, "monthly")?.billingCycle)
        assertEquals("yearly", selectPremiumCheckoutPlanOption(options, "yearly")?.billingCycle)
    }

    @Test
    fun `selectPremiumCheckoutPlanOption falls back to monthly when requested cycle is missing`() {
        val options = listOf(
            premiumPlan("monthly", "INR 99"),
            premiumPlan("yearly", "INR 999")
        )

        assertEquals("monthly", selectPremiumCheckoutPlanOption(options, "weekly")?.billingCycle)
    }

    private fun premiumPlan(
        billingCycle: String,
        displayAmount: String
    ): PremiumPlanOption {
        return PremiumPlanOption(
            billingCycle = billingCycle,
            amountMinor = if (billingCycle == "yearly") 99900 else 9900,
            currency = "INR",
            displayAmount = displayAmount,
            durationDays = if (billingCycle == "yearly") 365 else 31,
            label = billingCycle.replaceFirstChar { it.uppercase() }
        )
    }
}
