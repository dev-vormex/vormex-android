package com.kyant.backdrop.catalog.payments

import com.android.billingclient.api.BillingClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumCheckoutPayloadsTest {
    @Test
    fun `premiumBillingCycleForBasePlan maps monthly and yearly prepaid base plans`() {
        assertEquals("monthly", premiumBillingCycleForBasePlan("premium-monthly-prepaid"))
        assertEquals("yearly", premiumBillingCycleForBasePlan("premium-yearly-prepaid"))
        assertNull(premiumBillingCycleForBasePlan("premium-weekly-prepaid"))
    }

    @Test
    fun `selectPremiumPlayOffer picks the requested billing cycle`() {
        val offers = listOf(
            premiumOffer("monthly", PREMIUM_PLAY_MONTHLY_BASE_PLAN_ID, "$1.99"),
            premiumOffer("yearly", PREMIUM_PLAY_YEARLY_BASE_PLAN_ID, "$9.99")
        )

        assertEquals(PREMIUM_PLAY_MONTHLY_BASE_PLAN_ID, selectPremiumPlayOffer(offers, "monthly")?.basePlanId)
        assertEquals(PREMIUM_PLAY_YEARLY_BASE_PLAN_ID, selectPremiumPlayOffer(offers, "yearly")?.basePlanId)
    }

    @Test
    fun `selectPremiumPlayOffer falls back to monthly when requested cycle is missing`() {
        val offers = listOf(
            premiumOffer("monthly", PREMIUM_PLAY_MONTHLY_BASE_PLAN_ID, "$1.99"),
            premiumOffer("yearly", PREMIUM_PLAY_YEARLY_BASE_PLAN_ID, "$9.99")
        )

        assertEquals(PREMIUM_PLAY_MONTHLY_BASE_PLAN_ID, selectPremiumPlayOffer(offers, "weekly")?.basePlanId)
    }

    @Test
    fun `googlePlayObfuscatedAccountId is deterministic and non raw`() {
        val first = googlePlayObfuscatedAccountId("user_123")
        val second = googlePlayObfuscatedAccountId("user_123")
        val other = googlePlayObfuscatedAccountId("user_456")

        assertEquals(first, second)
        assertNotEquals(first, other)
        assertEquals(64, first.length)
        assertTrue(first.all { it in '0'..'9' || it in 'a'..'f' })
        assertNotEquals("user_123", first)
    }

    @Test
    fun `resolvePremiumPlayBillingErrorMessage stays quiet when the user cancels`() {
        assertNull(
            resolvePremiumPlayBillingErrorMessage(
                responseCode = BillingClient.BillingResponseCode.USER_CANCELED,
                debugMessage = "User closed purchase sheet"
            )
        )
    }

    @Test
    fun `resolvePremiumPlayBillingErrorMessage explains pending setup and failures`() {
        assertEquals(
            "Google Play billing is temporarily unavailable. Please try again.",
            resolvePremiumPlayBillingErrorMessage(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE)
        )
        assertEquals(
            "Vormex Premium is not available in Google Play yet.",
            resolvePremiumPlayBillingErrorMessage(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE)
        )
        assertEquals(
            "Premium purchase could not be completed. Please try again.",
            resolvePremiumPlayBillingErrorMessage(responseCode = 999, debugMessage = " ")
        )
    }

    private fun premiumOffer(
        billingCycle: String,
        basePlanId: String,
        formattedPrice: String
    ): PremiumPlayPlanOffer {
        return PremiumPlayPlanOffer(
            billingCycle = billingCycle,
            basePlanId = basePlanId,
            offerToken = "offer-token-$billingCycle",
            formattedPrice = formattedPrice,
            priceAmountMicros = 1_990_000,
            priceCurrencyCode = "USD"
        )
    }
}
