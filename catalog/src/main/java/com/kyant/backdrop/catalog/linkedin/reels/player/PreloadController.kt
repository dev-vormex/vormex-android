package com.kyant.backdrop.catalog.linkedin.reels.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.preload.DefaultPreloadManager
import androidx.media3.exoplayer.source.preload.TargetPreloadStatusControl
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.trackselection.TrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import com.kyant.backdrop.catalog.network.models.Reel

internal enum class PlaybackVariant {
    PRIMARY,
    FALLBACK_MP4
}

internal data class ReelPlaybackSource(
    val reelId: String,
    val primaryMediaItem: MediaItem,
    val fallbackMediaItem: MediaItem?
) {
    fun mediaItemFor(variant: PlaybackVariant): MediaItem {
        return when {
            variant == PlaybackVariant.FALLBACK_MP4 && fallbackMediaItem != null -> fallbackMediaItem
            else -> primaryMediaItem
        }
    }

    companion object {
        fun from(reel: Reel): ReelPlaybackSource {
            val primaryUrl = reel.hlsUrl?.takeIf { it.isNotBlank() } ?: reel.videoUrl
            val primaryItem = MediaItem.Builder()
                .setMediaId(reel.id)
                .setUri(primaryUrl)
                .setTag("loop")
                .build()

            val fallbackItem = reel.videoUrl
                .takeIf { reel.hlsUrl != null && it.isNotBlank() && it != primaryUrl }
                ?.let { progressiveUrl ->
                    MediaItem.Builder()
                        .setMediaId("${reel.id}:mp4")
                        .setUri(progressiveUrl)
                        .setTag("loop")
                        .build()
                }

            return ReelPlaybackSource(
                reelId = reel.id,
                primaryMediaItem = primaryItem,
                fallbackMediaItem = fallbackItem
            )
        }
    }
}

internal class PreloadController(context: Context) {
    private val appContext = context.applicationContext
    private val dataSourceFactoryProvider = BunnyDataSourceFactory.getInstance(appContext)
    private val mediaSourceFactory = dataSourceFactoryProvider.createMediaSourceFactory()
    private val bandwidthMeter = DefaultBandwidthMeter.Builder(appContext).build()
    private val targetStatusControl = ReelsTargetPreloadStatusControl()

    private val preloadManagerBuilder = DefaultPreloadManager.Builder(appContext, targetStatusControl)
        .setMediaSourceFactory(mediaSourceFactory)
        .setBandwidthMeter(bandwidthMeter)
        .setTrackSelectorFactory(buildTrackSelectorFactory())

    private val preloadManager = preloadManagerBuilder.build()

    private val trackedSources = LinkedHashMap<String, ReelPlaybackSource>()
    private val trackedIndexes = LinkedHashMap<String, Int>()

    fun buildPlayer(slotName: String): ExoPlayer {
        val trackSelector = buildTrackSelectorFactory().createTrackSelector(appContext) as DefaultTrackSelector
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                12_000,
                90_000,
                250,
                750
            )
            .build()

        return preloadManagerBuilder.buildExoPlayer(
            ExoPlayer.Builder(appContext)
                .setMediaSourceFactory(mediaSourceFactory)
                .setTrackSelector(trackSelector)
                .setLoadControl(loadControl)
                .setBandwidthMeter(bandwidthMeter)
                .setUseLazyPreparation(false)
                .setName(slotName)
        ).apply {
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = false
            volume = 0f
        }
    }

    fun updateWindow(reels: List<Reel>, currentIndex: Int) {
        if (reels.isEmpty()) return

        targetStatusControl.currentPlayingIndex = currentIndex

        val keepStart = (currentIndex - 6).coerceAtLeast(0)
        val keepEnd = (currentIndex + 20).coerceAtMost(reels.lastIndex)
        val desiredIds = LinkedHashSet<String>()

        for (index in keepStart..keepEnd) {
            val reel = reels[index]
            desiredIds += reel.id
            ensureTracked(reel, index)
        }

        val toRemove = trackedSources.keys.filterNot(desiredIds::contains)
        toRemove.forEach { reelId ->
            trackedSources.remove(reelId)?.let { source ->
                preloadManager.remove(source.primaryMediaItem)
            }
            trackedIndexes.remove(reelId)
        }

        preloadManager.setCurrentPlayingIndex(currentIndex)
        preloadManager.invalidate()
    }

    fun mediaSourceFor(reel: Reel, index: Int, variant: PlaybackVariant): MediaSource {
        val source = ensureTracked(reel, index)
        val mediaItem = source.mediaItemFor(variant)
        return when (variant) {
            PlaybackVariant.PRIMARY -> {
                preloadManager.getMediaSource(mediaItem) ?: mediaSourceFactory.createMediaSource(mediaItem)
            }

            PlaybackVariant.FALLBACK_MP4 -> mediaSourceFactory.createMediaSource(mediaItem)
        }
    }

    fun hasFallback(reel: Reel): Boolean {
        return ensureTracked(reel, trackedIndexes[reel.id] ?: 0).fallbackMediaItem != null
    }

    fun release() {
        trackedSources.clear()
        trackedIndexes.clear()
        preloadManager.release()
    }

    private fun ensureTracked(reel: Reel, index: Int): ReelPlaybackSource {
        val existing = trackedSources[reel.id]
        val existingIndex = trackedIndexes[reel.id]
        if (existing != null && existingIndex == index) {
            return existing
        }

        if (existing != null) {
            preloadManager.remove(existing.primaryMediaItem)
        }

        val source = ReelPlaybackSource.from(reel)
        preloadManager.add(source.primaryMediaItem, index)
        trackedSources[reel.id] = source
        trackedIndexes[reel.id] = index
        return source
    }

    private fun buildTrackSelectorFactory(): TrackSelector.Factory {
        return TrackSelector.Factory { context ->
            DefaultTrackSelector(
                context,
                AdaptiveTrackSelection.Factory(
                    500,
                    3_000,
                    1_000,
                    0.75f
                )
            )
        }
    }
}

private class ReelsTargetPreloadStatusControl : TargetPreloadStatusControl<Int> {
    @Volatile
    var currentPlayingIndex: Int = 0

    override fun getTargetPreloadStatus(index: Int): DefaultPreloadManager.Status? {
        val distance = index - currentPlayingIndex
        return when {
            distance == -2 -> DefaultPreloadManager.Status(
                DefaultPreloadManager.Status.STAGE_SOURCE_PREPARED
            )

            distance == -1 -> DefaultPreloadManager.Status(
                DefaultPreloadManager.Status.STAGE_LOADED_FOR_DURATION_MS,
                4_000L
            )

            distance == 1 -> DefaultPreloadManager.Status(
                DefaultPreloadManager.Status.STAGE_LOADED_FOR_DURATION_MS,
                15_000L
            )

            distance == 2 -> DefaultPreloadManager.Status(
                DefaultPreloadManager.Status.STAGE_LOADED_FOR_DURATION_MS,
                12_000L
            )

            distance == 3 -> DefaultPreloadManager.Status(
                DefaultPreloadManager.Status.STAGE_LOADED_FOR_DURATION_MS,
                8_000L
            )

            distance == 4 -> DefaultPreloadManager.Status(
                DefaultPreloadManager.Status.STAGE_LOADED_FOR_DURATION_MS,
                5_000L
            )

            distance in 5..10 -> DefaultPreloadManager.Status(
                DefaultPreloadManager.Status.STAGE_TRACKS_SELECTED
            )

            distance in 11..14 -> DefaultPreloadManager.Status(
                DefaultPreloadManager.Status.STAGE_SOURCE_PREPARED
            )

            else -> null
        }
    }
}
