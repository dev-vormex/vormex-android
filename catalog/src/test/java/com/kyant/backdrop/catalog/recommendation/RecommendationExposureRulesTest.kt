package com.kyant.backdrop.catalog.recommendation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationExposureRulesTest {
    @Test
    fun `card requires half visibility continuously for one second`() {
        assertFalse(RecommendationExposureRules.qualifiesCard(0.49, 2_000))
        assertFalse(RecommendationExposureRules.qualifiesCard(0.75, 999))
        assertTrue(RecommendationExposureRules.qualifiesCard(0.5, 1_000))
    }

    @Test
    fun `reel uses stricter of three seconds and quarter duration`() {
        assertEquals(3_000, RecommendationExposureRules.reelRequiredPlaybackMs(4_000))
        assertEquals(5_000, RecommendationExposureRules.reelRequiredPlaybackMs(20_000))
        assertFalse(RecommendationExposureRules.qualifiesReel(2_999, 4_000))
        assertTrue(RecommendationExposureRules.qualifiesReel(5_000, 20_000))
    }

    @Test
    fun `story requires two seconds`() {
        assertFalse(RecommendationExposureRules.qualifiesStory(1_999))
        assertTrue(RecommendationExposureRules.qualifiesStory(2_000))
    }
}
