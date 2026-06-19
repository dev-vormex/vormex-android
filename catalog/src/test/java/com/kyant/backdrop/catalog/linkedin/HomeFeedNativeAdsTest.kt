package com.kyant.backdrop.catalog.linkedin

import com.kyant.backdrop.catalog.network.models.Author
import com.kyant.backdrop.catalog.network.models.DailyMatchUser
import com.kyant.backdrop.catalog.network.models.ManagedAdPlacement
import com.kyant.backdrop.catalog.network.models.Post
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeFeedNativeAdsTest {
    @Test
    fun `native ads are disabled by default in row builder`() {
        val rows = buildHomeFeedRows(
            posts = posts(12),
            retentionState = null,
            widgetPositions = emptyMap()
        )

        assertFalse(rows.any { it is FeedListRow.NativeAdItem })
    }

    @Test
    fun `native ads insert after four posts and then every eight posts`() {
        val rows = buildHomeFeedRows(
            posts = posts(20),
            retentionState = null,
            widgetPositions = emptyMap(),
            includeNativeAds = true
        )
        val adRows = rows.filterIsInstance<FeedListRow.NativeAdItem>()

        assertEquals(listOf(0, 1, 2), adRows.map { it.sequence })
        assertEquals(listOf("ad_home_feed_native_0", "ad_home_feed_native_1", "ad_home_feed_native_2"), adRows.map { it.itemKey })
        assertEquals(4, rows.indexOf(adRows[0]))
        assertEquals(13, rows.indexOf(adRows[1]))
        assertEquals(22, rows.indexOf(adRows[2]))
    }

    @Test
    fun `managed feed ads render in reserved slots before native fallback`() {
        val managedAd = ManagedAdPlacement(
            placement = "feed",
            sequence = 1,
            afterItemCount = 12,
            slotKey = "feed_1",
            campaignId = "campaign-1",
            sponsorName = "Vormex"
        )
        val rows = buildHomeFeedRows(
            posts = posts(12),
            retentionState = null,
            widgetPositions = emptyMap(),
            managedAdPlacements = listOf(managedAd),
            includeNativeAds = true
        )

        val nativeRows = rows.filterIsInstance<FeedListRow.NativeAdItem>()
        val managedRows = rows.filterIsInstance<FeedListRow.ManagedAdItem>()

        assertEquals(listOf(0), nativeRows.map { it.sequence })
        assertEquals(listOf("campaign-1"), managedRows.map { it.ad.campaignId })
        assertEquals(4, rows.indexOf(nativeRows[0]))
        assertEquals(13, rows.indexOf(managedRows[0]))
    }

    @Test
    fun `managed feed ads render even when native fallback is disabled`() {
        val rows = buildHomeFeedRows(
            posts = posts(4),
            retentionState = null,
            widgetPositions = emptyMap(),
            managedAdPlacements = listOf(
                ManagedAdPlacement(
                    placement = "feed",
                    sequence = 0,
                    afterItemCount = 4,
                    slotKey = "feed_0",
                    campaignId = "campaign-1",
                    sponsorName = "Vormex"
                )
            ),
            includeNativeAds = false
        )

        assertTrue(rows.any { it is FeedListRow.ManagedAdItem })
        assertFalse(rows.any { it is FeedListRow.NativeAdItem })
    }

    @Test
    fun `native ad policy blocks impossible insertion counts`() {
        assertFalse(HomeFeedNativeAdPolicy.shouldInsertAfterPostCount(0, includeNativeAds = true))
        assertFalse(HomeFeedNativeAdPolicy.shouldInsertAfterPostCount(3, includeNativeAds = true))
        assertTrue(HomeFeedNativeAdPolicy.shouldInsertAfterPostCount(4, includeNativeAds = true))
        assertFalse(HomeFeedNativeAdPolicy.shouldInsertAfterPostCount(4, includeNativeAds = false))
        assertTrue(HomeFeedNativeAdPolicy.shouldInsertAfterPostCount(12, includeNativeAds = true))
    }

    @Test
    fun `retention widgets keep their original post-index placement with ads enabled`() {
        val rows = buildHomeFeedRows(
            posts = posts(8),
            retentionState = RetentionUiState(
                peopleLikeYou = listOf(DailyMatchUser(id = "match-1")),
                todaysMatches = listOf(DailyMatchUser(id = "match-2"))
            ),
            widgetPositions = mapOf(
                3 to "people_like_you",
                5 to "todays_matches",
                7 to "weekly_goals"
            ),
            includeNativeAds = true
        )

        val peopleWidgetIndex = rows.indexOfFirst { it is FeedListRow.WidgetPeopleLikeYou }
        val firstAdIndex = rows.indexOfFirst { it is FeedListRow.NativeAdItem }
        val todaysWidgetIndex = rows.indexOfFirst { it is FeedListRow.WidgetTodaysMatches }
        val weeklyWidgetIndex = rows.indexOfFirst { it is FeedListRow.WidgetWeeklyGoals }

        assertTrue(peopleWidgetIndex in 0 until firstAdIndex)
        assertTrue(todaysWidgetIndex > firstAdIndex)
        assertTrue(weeklyWidgetIndex > todaysWidgetIndex)
    }

    private fun posts(count: Int): List<Post> =
        (0 until count).map { index ->
            Post(
                id = "post-$index",
                authorId = "user-$index",
                author = Author(id = "user-$index"),
                createdAt = "2026-06-07T00:00:00.000Z"
            )
        }
}
