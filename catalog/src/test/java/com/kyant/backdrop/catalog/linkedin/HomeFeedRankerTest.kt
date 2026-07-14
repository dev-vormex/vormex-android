package com.kyant.backdrop.catalog.linkedin

import com.kyant.backdrop.catalog.network.models.Author
import com.kyant.backdrop.catalog.network.models.Post
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class HomeFeedRankerTest {
    private val nowMillis = java.time.Instant.parse("2026-05-15T00:00:00Z").toEpochMilli()

    @Test
    fun `skill matched post outranks generic popular post`() {
        val profile = HomeFeedRecommendationProfile(
            skillWeights = mapOf("kotlin" to 2, "jetpack compose" to 2)
        )
        val genericPopular = post(
            id = "popular",
            authorId = "stranger",
            content = "Campus fest recap and general updates",
            likesCount = 80,
            commentsCount = 12,
            createdAt = "2026-05-14T23:00:00Z"
        )
        val skillMatch = post(
            id = "skill-match",
            authorId = "builder",
            content = "Debugging Kotlin state in a Jetpack Compose home feed",
            likesCount = 1,
            createdAt = "2026-05-14T12:00:00Z"
        )

        val ranked = HomeFeedRanker.rankPosts(listOf(genericPopular, skillMatch), profile, nowMillis)

        assertEquals("skill-match", ranked.first().id)
    }

    @Test
    fun `followed authors outrank connections and strangers`() {
        val profile = HomeFeedRecommendationProfile(
            connectionAuthorIds = setOf("connected"),
            followingAuthorIds = setOf("followed")
        )
        val stranger = post("stranger-post", "stranger")
        val connection = post("connection-post", "connected")
        val followed = post("followed-post", "followed")

        val ranked = HomeFeedRanker.rankPosts(listOf(stranger, connection, followed), profile, nowMillis)

        assertEquals("followed-post", ranked[0].id)
        assertEquals("connection-post", ranked[1].id)
    }

    @Test
    fun `recently visited profile boosts that author's post`() {
        val profile = HomeFeedRecommendationProfile(
            recentProfileWeights = mapOf("recent-profile" to 24)
        )
        val ordinary = post("ordinary", "ordinary", likesCount = 20)
        val recentProfilePost = post("recent-profile-post", "recent-profile")

        val ranked = HomeFeedRanker.rankPosts(listOf(ordinary, recentProfilePost), profile, nowMillis)

        assertEquals("recent-profile-post", ranked.first().id)
    }

    @Test
    fun `author spread avoids repeating the same author immediately when possible`() {
        val profile = HomeFeedRecommendationProfile()
        val posts = listOf(
            post("a1", "author-a", likesCount = 100),
            post("a2", "author-a", likesCount = 90),
            post("a3", "author-a", likesCount = 80),
            post("b1", "author-b", likesCount = 5)
        )

        val ranked = HomeFeedRanker.rankPosts(posts, profile, nowMillis)

        assertEquals("author-a", ranked.first().authorId)
        assertNotEquals(ranked[0].authorId, ranked[1].authorId)
    }

    private fun post(
        id: String,
        authorId: String,
        content: String = "General post",
        likesCount: Int = 0,
        commentsCount: Int = 0,
        sharesCount: Int = 0,
        savesCount: Int = 0,
        createdAt: String = "2026-05-14T23:00:00Z"
    ): Post {
        return Post(
            id = id,
            authorId = authorId,
            author = Author(id = authorId, name = authorId),
            content = content,
            likesCount = likesCount,
            commentsCount = commentsCount,
            sharesCount = sharesCount,
            savesCount = savesCount,
            createdAt = createdAt
        )
    }
}
