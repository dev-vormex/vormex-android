package com.kyant.backdrop.catalog.linkedin.reels.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerPoolPolicyTest {
    @Test
    fun `fast network warms previous current and next two`() {
        assertEquals(
            listOf(5, 6, 7, 4),
            targetIndicesFor(5, lastIndex = 10, networkProfile = ReelsNetworkProfile.FAST)
        )
    }

    @Test
    fun `slow network warms current and next one only`() {
        assertEquals(
            listOf(5, 6),
            targetIndicesFor(5, lastIndex = 10, networkProfile = ReelsNetworkProfile.SLOW)
        )
    }

    @Test
    fun `backward scroll rewarms previous on fast network`() {
        assertEquals(
            listOf(4, 5, 6, 3),
            targetIndicesFor(4, lastIndex = 10, networkProfile = ReelsNetworkProfile.FAST)
        )
    }

    @Test
    fun `eviction never removes active and drops previous side on tie`() {
        val bound = setOf(4, 5, 6, 7)

        assertEquals(4, evictionCandidateIndex(bound, currentIndex = 5, targetIndex = 9))
        assertNull(evictionCandidateIndex(setOf(5), currentIndex = 5, targetIndex = 9))
    }
}
