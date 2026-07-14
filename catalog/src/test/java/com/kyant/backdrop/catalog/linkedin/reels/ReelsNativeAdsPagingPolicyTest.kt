package com.kyant.backdrop.catalog.linkedin.reels

import com.kyant.backdrop.catalog.network.models.ManagedAdPlacement
import com.kyant.backdrop.catalog.network.models.Reel
import com.kyant.backdrop.catalog.network.models.ReelAuthor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReelsNativeAdsPagingPolicyTest {
    @Test
    fun `native ad pages insert after five reels and then every eight reels`() {
        val pages = buildReelsFeedPages(
            reels = reels(20),
            includeNativeAds = true
        )
        val adPages = pages.filterIsInstance<ReelsFeedPage.NativeAdItem>()

        assertEquals(22, pages.size)
        assertEquals(listOf(0, 1), adPages.map { it.sequence })
        assertEquals(5, pages.indexOf(adPages[0]))
        assertEquals(14, pages.indexOf(adPages[1]))
        assertEquals(listOf("ad_reels_native_0", "ad_reels_native_1"), adPages.map { it.pageKey })
    }

    @Test
    fun `disabled native ads leave reels pager one-to-one`() {
        val pages = buildReelsFeedPages(
            reels = reels(20),
            includeNativeAds = false
        )

        assertEquals(20, pages.size)
        assertFalse(pages.any { it is ReelsFeedPage.NativeAdItem })
    }

    @Test
    fun `eligible native slot is skipped when ad is not loaded`() {
        val pages = buildReelsFeedPages(
            reels = reels(20),
            includeNativeAds = true,
            readyNativeAdSequences = emptySet()
        )

        assertEquals(20, pages.size)
        assertFalse(pages.any { it is ReelsFeedPage.NativeAdItem })
        assertEquals((0 until 20).toList(), pages.filterIsInstance<ReelsFeedPage.ReelItem>().map { it.reelIndex })
    }

    @Test
    fun `ready native ad is only inserted ahead of the current reel anchor`() {
        val pages = buildReelsFeedPages(
            reels = reels(20),
            includeNativeAds = true,
            readyNativeAdSequences = setOf(0, 1),
            earliestNativeAdAfterReelCount = 8
        )
        val adPages = pages.filterIsInstance<ReelsFeedPage.NativeAdItem>()

        assertEquals(listOf(1), adPages.map { it.sequence })
        assertEquals(13, pages.indexOfFirst { it is ReelsFeedPage.NativeAdItem })
    }

    @Test
    fun `managed reels ads fill reserved pages before native fallback`() {
        val managedAd = ManagedAdPlacement(
            placement = "reels",
            sequence = 1,
            afterItemCount = 13,
            slotKey = "reels_1",
            campaignId = "campaign-1",
            sponsorName = "Vormex"
        )
        val pages = buildReelsFeedPages(
            reels = reels(13),
            includeNativeAds = true,
            managedAdPlacements = listOf(managedAd)
        )

        val nativePages = pages.filterIsInstance<ReelsFeedPage.NativeAdItem>()
        val managedPages = pages.filterIsInstance<ReelsFeedPage.ManagedAdItem>()

        assertEquals(listOf(0), nativePages.map { it.sequence })
        assertEquals(listOf("campaign-1"), managedPages.map { it.ad.campaignId })
        assertEquals(5, pages.indexOf(nativePages[0]))
        assertEquals(14, pages.indexOf(managedPages[0]))
    }

    @Test
    fun `managed reels ads keep ad pages when native fallback is disabled`() {
        val pages = buildReelsFeedPages(
            reels = reels(5),
            includeNativeAds = false,
            managedAdPlacements = listOf(
                ManagedAdPlacement(
                    placement = "reels",
                    sequence = 0,
                    afterItemCount = 5,
                    slotKey = "reels_0",
                    campaignId = "campaign-1",
                    sponsorName = "Vormex"
                )
            )
        )

        assertEquals(6, pages.size)
        assertNull(ReelsNativeAdPagingPolicy.reelIndexForPageIndex(5, reelCount = 5, includeNativeAds = true))
        assertTrue(pages.last() is ReelsFeedPage.ManagedAdItem)
    }

    @Test
    fun `native fallback preload includes every unmanaged reserved reels slot`() {
        val managedAd = ManagedAdPlacement(
            placement = "reels",
            sequence = 1,
            afterItemCount = 13,
            slotKey = "reels_1",
            campaignId = "campaign-1",
            sponsorName = "Vormex"
        )

        val sequences = reelsNativeAdSequencesToPreload(
            reelCount = 21,
            includeNativeAds = true,
            managedAdPlacements = listOf(managedAd)
        )

        assertEquals(listOf(0, 2), sequences)
    }

    @Test
    fun `native fallback preload is empty when fallback is disabled`() {
        val sequences = reelsNativeAdSequencesToPreload(
            reelCount = 21,
            includeNativeAds = false
        )

        assertTrue(sequences.isEmpty())
    }

    @Test
    fun `page index maps initial reel indices around ad pages`() {
        assertEquals(0, ReelsNativeAdPagingPolicy.pageIndexForReelIndex(0, reelCount = 20, includeNativeAds = true))
        assertEquals(4, ReelsNativeAdPagingPolicy.pageIndexForReelIndex(4, reelCount = 20, includeNativeAds = true))
        assertEquals(6, ReelsNativeAdPagingPolicy.pageIndexForReelIndex(5, reelCount = 20, includeNativeAds = true))
        assertEquals(13, ReelsNativeAdPagingPolicy.pageIndexForReelIndex(12, reelCount = 20, includeNativeAds = true))
        assertEquals(15, ReelsNativeAdPagingPolicy.pageIndexForReelIndex(13, reelCount = 20, includeNativeAds = true))
    }

    @Test
    fun `reel index mapping skips ad pages`() {
        assertEquals(4, ReelsNativeAdPagingPolicy.reelIndexForPageIndex(4, reelCount = 20, includeNativeAds = true))
        assertNull(ReelsNativeAdPagingPolicy.reelIndexForPageIndex(5, reelCount = 20, includeNativeAds = true))
        assertEquals(5, ReelsNativeAdPagingPolicy.reelIndexForPageIndex(6, reelCount = 20, includeNativeAds = true))
        assertNull(ReelsNativeAdPagingPolicy.reelIndexForPageIndex(14, reelCount = 20, includeNativeAds = true))
        assertEquals(13, ReelsNativeAdPagingPolicy.reelIndexForPageIndex(15, reelCount = 20, includeNativeAds = true))
    }

    @Test
    fun `load more policy uses mixed page count near the end`() {
        val pageCount = ReelsNativeAdPagingPolicy.pageCountForReelCount(
            reelCount = 20,
            includeNativeAds = true
        )

        assertEquals(22, pageCount)
        assertFalse(ReelsNativeAdPagingPolicy.shouldLoadMoreForPage(16, pageCount))
        assertTrue(ReelsNativeAdPagingPolicy.shouldLoadMoreForPage(17, pageCount))
        assertTrue(ReelsNativeAdPagingPolicy.shouldLoadMoreForPage(21, pageCount))
    }

    @Test
    fun `item prefetch policy is based on reel index and ignores ad pages`() {
        val pages = buildReelsFeedPages(
            reels = reels(20),
            includeNativeAds = true
        )
        val pageForReel11 = pages.indexOfFirst {
            it is ReelsFeedPage.ReelItem && it.reelIndex == 11
        }
        val reelIndex = ReelsNativeAdPagingPolicy.reelIndexForPageIndex(
            pageIndex = pageForReel11,
            reelCount = 20,
            includeNativeAds = true
        )

        assertEquals(11, reelIndex)
        assertTrue(shouldPrefetchMoreReels(currentReelIndex = reelIndex ?: -1, reelCount = 20))
        assertFalse(shouldPrefetchMoreReels(currentReelIndex = 6, reelCount = 20))
        assertTrue(shouldPrefetchMoreReels(currentReelIndex = 7, reelCount = 20))
    }

    @Test
    fun `managed ad merge prefers network placement over cached placement`() {
        val cached = ManagedAdPlacement(
            placement = "reels",
            sequence = 0,
            afterItemCount = 5,
            slotKey = "reels_0",
            campaignId = "cached-campaign",
            sponsorName = "Cached",
            reelsVideoUrl = null
        )
        val network = cached.copy(
            campaignId = "network-campaign",
            sponsorName = "Network",
            reelsVideoUrl = "https://cdn.example.com/ad.mp4"
        )

        val merged = managedAdPlacementsNetworkFirst(
            existing = listOf(cached),
            incoming = listOf(network)
        )

        assertEquals(1, merged.size)
        assertEquals("network-campaign", merged.first().campaignId)
        assertEquals("https://cdn.example.com/ad.mp4", merged.first().reelsVideoUrl)
    }

    private fun reels(count: Int): List<Reel> =
        (0 until count).map { index ->
            Reel(
                id = "reel-$index",
                author = ReelAuthor(id = "user-$index"),
                videoUrl = "https://example.com/reel-$index.mp4"
            )
        }
}
