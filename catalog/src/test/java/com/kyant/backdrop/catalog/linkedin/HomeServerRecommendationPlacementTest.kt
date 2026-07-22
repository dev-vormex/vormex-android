package com.kyant.backdrop.catalog.linkedin

import com.kyant.backdrop.catalog.network.models.Author
import com.kyant.backdrop.catalog.network.models.HomeModulePlacement
import com.kyant.backdrop.catalog.network.models.DailyMatchUser
import com.kyant.backdrop.catalog.network.models.Post
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeServerRecommendationPlacementTest {
    @Test
    fun `backend post order is preserved around module placement`() {
        val posts = (1..12).map { index ->
            Post(
                id = "p$index",
                authorId = "a$index",
                author = Author(id = "a$index"),
                createdAt = "2026-07-22T00:00:00Z"
            )
        }
        val rows = buildHomeFeedRows(
            posts = posts,
            retentionState = null,
            widgetPositions = emptyMap(),
            modulePlacements = listOf(HomeModulePlacement(type = "JOBS", position = 9))
        )

        assertEquals(posts.map { it.id }, rows.filterIsInstance<FeedListRow.PostItem>().map { it.post.id })
        val moduleIndex = rows.indexOfFirst { it is FeedListRow.ServerModuleItem }
        assertEquals(8, rows.take(moduleIndex).count { it is FeedListRow.PostItem })
    }

    @Test
    fun `weekly goals remains fixed and server modules are not randomized`() {
        val posts = (1..25).map { index ->
            Post("p$index", authorId = "a$index", author = Author("a$index"), createdAt = "now")
        }
        val rows = buildHomeFeedRows(
            posts = posts,
            retentionState = RetentionUiState(),
            widgetPositions = emptyMap(),
            modulePlacements = listOf(HomeModulePlacement(type = "EVENTS", position = 16))
        )

        assertEquals(1, rows.count { it is FeedListRow.WidgetWeeklyGoals })
        assertTrue(rows.any { it is FeedListRow.ServerModuleItem && it.placement.position == 16 })
    }

    @Test
    fun `server people module is not replaced by client retention inventory`() {
        val posts = (1..10).map { index ->
            Post("p$index", authorId = "a$index", author = Author("a$index"), createdAt = "now")
        }
        val rows = buildHomeFeedRows(
            posts = posts,
            retentionState = RetentionUiState(peopleLikeYou = listOf(DailyMatchUser(id = "person", name = "Person"))),
            widgetPositions = emptyMap(),
            modulePlacements = listOf(HomeModulePlacement(type = "PEOPLE", position = 9))
        )

        assertTrue(rows.any { it is FeedListRow.ServerModuleItem && it.placement.type == "PEOPLE" })
    }
}
