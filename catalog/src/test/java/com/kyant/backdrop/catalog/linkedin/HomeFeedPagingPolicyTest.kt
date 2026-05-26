package com.kyant.backdrop.catalog.linkedin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeFeedPagingPolicyTest {
    @Test
    fun `allows loading when cursor exists and no load is running`() {
        assertTrue(
            HomeFeedPagingPolicy.canStartNextPageLoad(
                nextCursor = "cursor-1",
                hasMore = true,
                isLoadingMore = false
            )
        )
    }

    @Test
    fun `blocks duplicate or impossible page loads`() {
        assertFalse(HomeFeedPagingPolicy.canStartNextPageLoad("cursor-1", hasMore = true, isLoadingMore = true))
        assertFalse(HomeFeedPagingPolicy.canStartNextPageLoad(null, hasMore = true, isLoadingMore = false))
        assertFalse(HomeFeedPagingPolicy.canStartNextPageLoad(" ", hasMore = true, isLoadingMore = false))
        assertFalse(HomeFeedPagingPolicy.canStartNextPageLoad("cursor-1", hasMore = false, isLoadingMore = false))
    }

    @Test
    fun `home feed page sizes stay bounded for mobile rendering`() {
        assertTrue(HomeFeedPageSize in 20..50)
        assertTrue(HomeStoryFeedLimit <= 100)
        assertTrue(HomeFeedPrefetchRemainingPosts in 4..12)
    }
}
