package com.kyant.backdrop.catalog.recommendation

object RecommendationExposureRules {
    const val CARD_MIN_VISIBLE_FRACTION = 0.5
    const val CARD_MIN_VISIBLE_TIME_MS = 1_000L
    const val STORY_MIN_VISIBLE_TIME_MS = 2_000L

    fun reelRequiredPlaybackMs(durationMs: Long): Long =
        maxOf(3_000L, kotlin.math.ceil(durationMs.coerceAtLeast(0L) * 0.25).toLong())

    fun qualifiesCard(maxVisibleFraction: Double, continuouslyVisibleMs: Long): Boolean =
        maxVisibleFraction >= CARD_MIN_VISIBLE_FRACTION && continuouslyVisibleMs >= CARD_MIN_VISIBLE_TIME_MS

    fun qualifiesReel(playbackMs: Long, durationMs: Long): Boolean =
        playbackMs >= reelRequiredPlaybackMs(durationMs)

    fun qualifiesStory(visibleMs: Long): Boolean = visibleMs >= STORY_MIN_VISIBLE_TIME_MS
}
