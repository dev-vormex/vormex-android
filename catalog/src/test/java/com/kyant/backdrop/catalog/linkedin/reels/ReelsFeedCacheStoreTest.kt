package com.kyant.backdrop.catalog.linkedin.reels

import com.kyant.backdrop.catalog.network.models.ManagedAdPlacement
import com.kyant.backdrop.catalog.network.models.Reel
import com.kyant.backdrop.catalog.network.models.ReelAuthor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReelsFeedCacheStoreTest {
    @Test
    fun `truncated snapshot is never terminal and drops stale cursor and out of range ads`() {
        val snapshot = createCacheSnapshot(
            reels = reels(100),
            nextCursor = "full-feed-cursor",
            hasMore = false,
            adPlacements = listOf(
                managedAd(afterItemCount = 5, slotKey = "reels_0"),
                managedAd(afterItemCount = 31, slotKey = "reels_3"),
                managedAd(afterItemCount = 0, slotKey = "reels_bad")
            ),
            updatedAt = 123L,
            ownerUserId = "user-1"
        )

        assertEquals(REELS_DISK_CACHE_MAX_REELS, snapshot.reels.size)
        assertTrue(snapshot.truncated)
        assertTrue(snapshot.hasMore)
        assertNull(snapshot.nextCursor)
        assertEquals(REELS_FEED_CACHE_VERSION, snapshot.cacheVersion)
        assertEquals(listOf("reels_0"), snapshot.adPlacements.map { it.slotKey })
    }

    @Test
    fun `truncated snapshot stays non terminal even if source feed had reached end`() {
        val snapshot = createCacheSnapshot(
            reels = reels(100),
            nextCursor = null,
            hasMore = false,
            adPlacements = emptyList(),
            updatedAt = 123L,
            ownerUserId = "user-1"
        )

        assertTrue(snapshot.truncated)
        assertTrue(snapshot.hasMore)
        assertNull(snapshot.nextCursor)
    }

    @Test
    fun `untruncated snapshot preserves original cursor and hasMore`() {
        val snapshot = createCacheSnapshot(
            reels = reels(20),
            nextCursor = "server-cursor",
            hasMore = false,
            adPlacements = listOf(managedAd(afterItemCount = 13, slotKey = "reels_1")),
            updatedAt = 123L,
            ownerUserId = "user-1"
        )

        assertEquals(20, snapshot.reels.size)
        assertFalse(snapshot.truncated)
        assertFalse(snapshot.hasMore)
        assertEquals("server-cursor", snapshot.nextCursor)
        assertEquals(listOf("reels_1"), snapshot.adPlacements.map { it.slotKey })
    }

    @Test
    fun `legacy snapshot is discarded`() {
        val legacy = CachedReelsFeed(
            reels = reels(30),
            nextCursor = "legacy-cursor",
            hasMore = false,
            cacheVersion = 0
        )
        val raw = Json.encodeToString(legacy)

        val decoded = ReelsFeedCacheStore.decodeCachedReelsFeed(
            raw = raw,
            updatedAt = 123L,
            ownerUserId = "user-1"
        )

        assertNull(decoded)
    }

    @Test
    fun `restore guard overrides forged truncated terminal snapshot`() {
        val forged = CachedReelsFeed(
            reels = reels(30),
            nextCursor = "bad-cursor",
            hasMore = false,
            truncated = true,
            cacheVersion = REELS_FEED_CACHE_VERSION
        )

        val pagination = restoredPaginationState(forged)

        assertTrue(pagination.hasMore)
        assertNull(pagination.nextCursor)
    }

    @Test
    fun `restore guard treats exact max terminal snapshot as unknown`() {
        val suspicious = CachedReelsFeed(
            reels = reels(REELS_DISK_CACHE_MAX_REELS),
            nextCursor = null,
            hasMore = false,
            truncated = false,
            cacheVersion = REELS_FEED_CACHE_VERSION
        )

        val pagination = restoredPaginationState(suspicious)

        assertTrue(pagination.hasMore)
        assertNull(pagination.nextCursor)
    }

    @Test
    fun `recovery merge keeps cached reels and appends next page`() {
        val cached = CachedReelsFeed(
            reels = reels(30),
            hasMore = true,
            nextCursor = null,
            truncated = true,
            cacheVersion = REELS_FEED_CACHE_VERSION
        )
        val pagination = restoredPaginationState(cached)
        val seen = LinkedHashSet<String>()

        val afterFirstPageRefresh = mergeReelsById(cached.reels, reels(30), seen)
        val afterSecondPage = mergeReelsById(afterFirstPageRefresh, reels(30, start = 30), seen)

        assertTrue(pagination.hasMore)
        assertNull(pagination.nextCursor)
        assertEquals(30, afterFirstPageRefresh.size)
        assertEquals(60, afterSecondPage.size)
    }

    private fun reels(count: Int, start: Int = 0): List<Reel> =
        (start until start + count).map { index ->
            Reel(
                id = "reel-$index",
                author = ReelAuthor(id = "user-$index"),
                videoUrl = "https://example.com/reel-$index.mp4",
                publishedAt = "2026-06-10T00:${index.toString().padStart(2, '0')}:00.000Z"
            )
        }

    private fun managedAd(afterItemCount: Int, slotKey: String): ManagedAdPlacement =
        ManagedAdPlacement(
            placement = "reels",
            sequence = afterItemCount,
            afterItemCount = afterItemCount,
            slotKey = slotKey,
            campaignId = "campaign-$slotKey",
            sponsorName = "Vormex"
        )
}
