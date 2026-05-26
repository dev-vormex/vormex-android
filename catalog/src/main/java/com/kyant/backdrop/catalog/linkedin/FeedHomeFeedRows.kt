package com.kyant.backdrop.catalog.linkedin

import androidx.compose.runtime.Immutable
import com.kyant.backdrop.catalog.network.models.Post

internal const val HomeFeedPageSize = 40
internal const val HomeStoryFeedLimit = 80
internal const val HomeFeedPrefetchRemainingPosts = 8

internal object HomeFeedPagingPolicy {
    fun canStartNextPageLoad(
        nextCursor: String?,
        hasMore: Boolean,
        isLoadingMore: Boolean
    ): Boolean {
        return hasMore && !isLoadingMore && !nextCursor.isNullOrBlank()
    }
}

/**
 * Layout discriminant for LazyColumn [contentType] — keeps poll/article/video slots
 * from being recycled into each other (reduces frame drops on fast scroll).
 */
enum class FeedPostLayoutType {
    ARTICLE,
    CELEBRATION,
    POLL,
    VIDEO,
    IMAGE_GRID,
    LINK_ONLY,
    TEXT
}

internal fun Post.feedPostLayoutType(): FeedPostLayoutType {
    val normalizedType = type.uppercase()
    if (normalizedType == "ARTICLE" || !articleTitle.isNullOrBlank()) return FeedPostLayoutType.ARTICLE
    if (normalizedType == "CELEBRATION" || !celebrationType.isNullOrBlank()) return FeedPostLayoutType.CELEBRATION
    if (pollOptions.isNotEmpty()) return FeedPostLayoutType.POLL
    if (!videoUrl.isNullOrEmpty() || !defaultVideoId.isNullOrEmpty() || normalizedType == "VIDEO") return FeedPostLayoutType.VIDEO
    if (mediaUrls.isNotEmpty()) return FeedPostLayoutType.IMAGE_GRID
    if (!linkUrl.isNullOrBlank()) return FeedPostLayoutType.LINK_ONLY
    return FeedPostLayoutType.TEXT
}

@Immutable
sealed class FeedListRow {
    abstract val itemKey: String
    abstract val contentType: Any

    data class PostItem(
        val post: Post,
        private val occurrence: Int = 0,
        private val layoutType: FeedPostLayoutType = post.feedPostLayoutType()
    ) : FeedListRow() {
        override val itemKey: String
            get() = if (occurrence == 0) {
                "post_${post.id}"
            } else {
                "post_${post.id}_duplicate_$occurrence"
            }
        override val contentType: Any get() = layoutType
    }

    data object WidgetPeopleLikeYou : FeedListRow() {
        override val itemKey = "widget_people_like_you"
        override val contentType = "widget_people_like_you"
    }

    data object WidgetTodaysMatches : FeedListRow() {
        override val itemKey = "widget_todays_matches"
        override val contentType = "widget_todays_matches"
    }

    data object WidgetWeeklyGoals : FeedListRow() {
        override val itemKey = "widget_weekly_goals"
        override val contentType = "widget_weekly_goals"
    }

    data object WidgetPeopleLikeYouFallback : FeedListRow() {
        override val itemKey = "widget_people_like_you_fallback"
        override val contentType = "widget_people_like_you"
    }

    data object WidgetTodaysMatchesFallback : FeedListRow() {
        override val itemKey = "widget_todays_matches_fallback"
        override val contentType = "widget_todays_matches"
    }

    data object WidgetWeeklyGoalsFallback : FeedListRow() {
        override val itemKey = "widget_weekly_goals_fallback"
        override val contentType = "widget_weekly_goals"
    }
}

/**
 * Flattens posts + inline widgets into a single list for [LazyColumn] [items] with
 * stable keys and [contentType] — keeps composition recycling aligned with row shape.
 */
internal fun buildHomeFeedRows(
    posts: List<Post>,
    retentionState: RetentionUiState?,
    widgetPositions: Map<Int, String>
): List<FeedListRow> {
    val out = ArrayList<FeedListRow>(posts.size + 4)
    val postIdOccurrences = HashMap<String, Int>(posts.size)
    posts.forEachIndexed { index, post ->
        retentionState?.let { state ->
            when (widgetPositions[index]) {
                "people_like_you" -> {
                    if (state.peopleLikeYou.isNotEmpty()) {
                        out.add(FeedListRow.WidgetPeopleLikeYou)
                    }
                }
                "todays_matches" -> {
                    if (state.todaysMatches.isNotEmpty()) {
                        out.add(FeedListRow.WidgetTodaysMatches)
                    }
                }
                "weekly_goals" -> {
                    out.add(FeedListRow.WidgetWeeklyGoals)
                }
            }
        }
        val occurrence = postIdOccurrences.getOrDefault(post.id, 0)
        postIdOccurrences[post.id] = occurrence + 1
        out.add(FeedListRow.PostItem(post, occurrence))
    }

    if (posts.size < 25) {
        retentionState?.let { state ->
            if (state.peopleLikeYou.isNotEmpty() && posts.size < 5) {
                out.add(FeedListRow.WidgetPeopleLikeYouFallback)
            }
            if (state.todaysMatches.isNotEmpty() && posts.size < 12) {
                out.add(FeedListRow.WidgetTodaysMatchesFallback)
            }
            if (posts.size < 20) {
                out.add(FeedListRow.WidgetWeeklyGoalsFallback)
            }
        }
    }
    return out
}
