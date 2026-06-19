package com.kyant.backdrop.catalog.linkedin.reels

import com.kyant.backdrop.catalog.network.models.Reel
import com.kyant.backdrop.catalog.network.models.ReelAuthor
import org.junit.Assert.assertEquals
import org.junit.Test

class ReelsFeedMergeTest {
    @Test
    fun `merge dedupes preview cache and fresh feed by reel id`() {
        val seen = LinkedHashSet<String>()
        val previewAndCache = mergeReelsById(reels("a", "b", "c"), reels("b", "d"), seen)
        val hydrated = mergeReelsById(previewAndCache, reels("c", "e", "a", "f"), seen)

        assertEquals(listOf("a", "b", "c", "d", "e", "f"), hydrated.map { it.id })
    }

    @Test
    fun `session seen ids block duplicate cursor results`() {
        val seen = linkedSetOf("old-duplicate")
        val merged = mergeReelsById(reels("a"), reels("old-duplicate", "b"), seen)

        assertEquals(listOf("a", "b"), merged.map { it.id })
    }

    @Test
    fun `current reel index is restored by id after hydration`() {
        val hydrated = reels("preview-0", "selected", "fresh-0")

        assertEquals(1, indexOfReelIdOrNearest(hydrated, "selected", fallbackIndex = 0))
        assertEquals(2, indexOfReelIdOrNearest(hydrated, "missing", fallbackIndex = 99))
    }

    private fun reels(vararg ids: String): List<Reel> =
        ids.map { id ->
            Reel(
                id = id,
                author = ReelAuthor(id = "author-$id"),
                videoUrl = "https://example.com/$id.mp4"
            )
        }
}
