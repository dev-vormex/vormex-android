package com.kyant.backdrop.catalog.payments

import com.kyant.backdrop.catalog.network.models.PremiumPlanOption
import com.razorpay.Checkout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    @Test
    fun `buildPremiumVerifyRequestOrNull requires all Razorpay verification fields`() {
        assertNull(
            buildPremiumVerifyRequestOrNull(
                razorpayOrderId = "order_123",
                razorpayPaymentId = "pay_123",
                razorpaySignature = null
            )
        )

        val request = buildPremiumVerifyRequestOrNull(
            razorpayOrderId = " order_123 ",
            razorpayPaymentId = " pay_123 ",
            razorpaySignature = " sig_123 "
        )

        assertNotNull(request)
        assertEquals("order_123", request?.razorpayOrderId)
        assertEquals("pay_123", request?.razorpayPaymentId)
        assertEquals("sig_123", request?.razorpaySignature)
    }

    @Test
    fun `resolveRazorpayCheckoutErrorMessage stays quiet when the user cancels`() {
        assertNull(
            resolveRazorpayCheckoutErrorMessage(
                code = Checkout.PAYMENT_CANCELED,
                response = "User closed checkout"
            )
        )
    }

    @Test
    fun `resolveRazorpayCheckoutErrorMessage explains gateway startup and network failures`() {
        assertEquals(
            "Razorpay checkout could not reach the network. Please check your connection and try again.",
            resolveRazorpayCheckoutErrorMessage(Checkout.NETWORK_ERROR)
        )
        assertEquals(
            "Premium checkout could not be started. Please try again.",
            resolveRazorpayCheckoutErrorMessage(Checkout.INVALID_OPTIONS)
        )
        assertEquals(
            "Premium payment could not be completed. Please try again.",
            resolveRazorpayCheckoutErrorMessage(code = 999, response = " ")
        )
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
