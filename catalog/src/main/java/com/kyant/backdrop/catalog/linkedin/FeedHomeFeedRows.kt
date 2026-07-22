package com.kyant.backdrop.catalog.linkedin

import androidx.compose.runtime.Immutable
import com.kyant.backdrop.catalog.network.models.ManagedAdPlacement
import com.kyant.backdrop.catalog.network.models.HomeModulePlacement
import com.kyant.backdrop.catalog.network.models.Post

internal const val HomeFeedPageSize = 40
internal const val HomeStoryFeedLimit = 80
internal const val HomeFeedPrefetchRemainingPosts = 8
internal const val HomeFeedFirstNativeAdAfterPosts = 4
internal const val HomeFeedNativeAdIntervalPosts = 8

internal object HomeFeedPagingPolicy {
    fun canStartNextPageLoad(
        nextCursor: String?,
        hasMore: Boolean,
        isLoadingMore: Boolean
    ): Boolean {
        return hasMore && !isLoadingMore && !nextCursor.isNullOrBlank()
    }
}

internal object HomeFeedNativeAdPolicy {
    fun isReservedAdSlotAfterPostCount(postCount: Int): Boolean {
        return postCount >= HomeFeedFirstNativeAdAfterPosts &&
            (postCount - HomeFeedFirstNativeAdAfterPosts) % HomeFeedNativeAdIntervalPosts == 0
    }

    fun shouldInsertAfterPostCount(
        postCount: Int,
        includeNativeAds: Boolean
    ): Boolean {
        return includeNativeAds && isReservedAdSlotAfterPostCount(postCount)
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

    data class NativeAdItem(
        val sequence: Int
    ) : FeedListRow() {
        val slotKey: String = "home_feed_native_$sequence"
        override val itemKey: String = "ad_$slotKey"
        override val contentType: Any = "native_ad"
    }

    data class ManagedAdItem(
        val ad: ManagedAdPlacement
    ) : FeedListRow() {
        override val itemKey: String = "managed_ad_${ad.slotKey}_${ad.campaignId}"
        override val contentType: Any = "managed_ad"
    }

    data class ServerModuleItem(
        val placement: HomeModulePlacement
    ) : FeedListRow() {
        override val itemKey: String = "recommendation_module_${placement.type}_${placement.position}"
        override val contentType: Any = "recommendation_module_${placement.type}"
    }
}

/**
 * Flattens posts + inline widgets into a single list for [LazyColumn] [items] with
 * stable keys and [contentType] — keeps composition recycling aligned with row shape.
 */
internal fun buildHomeFeedRows(
    posts: List<Post>,
    retentionState: RetentionUiState?,
    widgetPositions: Map<Int, String>,
    modulePlacements: List<HomeModulePlacement> = emptyList(),
    managedAdPlacements: List<ManagedAdPlacement> = emptyList(),
    includeNativeAds: Boolean = false
): List<FeedListRow> {
    val out = ArrayList<FeedListRow>(posts.size + 8)
    val postIdOccurrences = HashMap<String, Int>(posts.size)
    val managedAdsBySequence = managedAdPlacements
        .filter { it.placement.equals("feed", ignoreCase = true) }
        .associateBy { it.sequence }
    val serverModulesByPosition = modulePlacements
        .filter { it.position in 1..40 }
        .distinctBy { it.position }
        .associateBy { it.position }
    var nativeAdSequence = 0
    var weeklyGoalsAdded = false
    posts.forEachIndexed { index, post ->
        serverModulesByPosition[index + 1]?.let { placement ->
            out.add(FeedListRow.ServerModuleItem(placement))
        }
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
                    weeklyGoalsAdded = true
                }
            }
        }
        val occurrence = postIdOccurrences.getOrDefault(post.id, 0)
        postIdOccurrences[post.id] = occurrence + 1
        out.add(FeedListRow.PostItem(post, occurrence))

        // Weekly Goals is a fixed retention widget, not a randomized recommendation.
        if (!weeklyGoalsAdded && retentionState != null && index + 1 == minOf(20, posts.size)) {
            out.add(FeedListRow.WidgetWeeklyGoals)
            weeklyGoalsAdded = true
        }

        if (HomeFeedNativeAdPolicy.isReservedAdSlotAfterPostCount(index + 1)) {
            val managedAd = managedAdsBySequence[nativeAdSequence]
            when {
                managedAd != null -> out.add(FeedListRow.ManagedAdItem(managedAd))
                includeNativeAds -> out.add(FeedListRow.NativeAdItem(nativeAdSequence))
            }
            nativeAdSequence++
        }
    }

    if (posts.size < 25) {
        retentionState?.let { state ->
            if (state.peopleLikeYou.isNotEmpty() && posts.size < 5) {
                out.add(FeedListRow.WidgetPeopleLikeYouFallback)
            }
            if (state.todaysMatches.isNotEmpty() && posts.size < 12) {
                out.add(FeedListRow.WidgetTodaysMatchesFallback)
            }
            if (!weeklyGoalsAdded && posts.size < 20) {
                out.add(FeedListRow.WidgetWeeklyGoalsFallback)
            }
        }
    }
    return out
}
