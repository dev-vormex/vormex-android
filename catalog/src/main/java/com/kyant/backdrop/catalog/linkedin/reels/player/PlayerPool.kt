package com.kyant.backdrop.catalog.linkedin.reels.player

import android.content.Context
import androidx.annotation.MainThread
import androidx.media3.exoplayer.ExoPlayer
import com.kyant.backdrop.catalog.network.models.Reel
import kotlin.math.abs

object PlayerPool {
    private const val POOL_SIZE = 4

    @Volatile
    private var engine: Engine? = null

    @MainThread
    fun syncWindow(context: Context, reels: List<Reel>, currentIndex: Int) {
        if (reels.isEmpty()) return
        getEngine(context).syncWindow(reels, currentIndex)
    }

    @MainThread
    fun playerForIndex(context: Context, index: Int): ExoPlayer? {
        return getEngine(context).playerForIndex(index)
    }

    @MainThread
    fun handlePlaybackError(context: Context, index: Int): Boolean {
        return getEngine(context).handlePlaybackError(index)
    }

    @MainThread
    fun retry(context: Context, index: Int) {
        getEngine(context).retry(index)
    }

    @MainThread
    fun resume(context: Context, currentIndex: Int) {
        getEngine(context).resume(currentIndex)
    }

    @MainThread
    fun pauseAll(resetPosition: Boolean = false) {
        engine?.pauseAll(resetPosition)
    }

    @MainThread
    fun release() {
        engine?.release()
        engine = null
    }

    @MainThread
    private fun getEngine(context: Context): Engine {
        return engine ?: synchronized(this) {
            engine ?: Engine(context.applicationContext).also { engine = it }
        }
    }

    private class Engine(context: Context) {
        private val appContext = context.applicationContext
        private val preloadController = PreloadController(appContext)
        private val players = List(POOL_SIZE) { slot ->
            preloadController.buildPlayer(slotName = "reels-slot-$slot")
        }

        private val boundSlots = LinkedHashMap<Int, BoundSlot>()
        private var currentIndex: Int = 0
        private var currentFeed: List<Reel> = emptyList()

        fun syncWindow(reels: List<Reel>, currentIndex: Int) {
            this.currentFeed = reels
            this.currentIndex = currentIndex

            preloadController.updateWindow(reels, currentIndex)

            val targetIndices = buildTargetIndices(currentIndex, reels.lastIndex)

            val currentBindings = boundSlots.keys.toList()
            currentBindings
                .filterNot(targetIndices::contains)
                .forEach(::unbind)

            targetIndices.forEach { index ->
                bind(index, reels[index], preferredVariant = boundSlots[index]?.variant ?: PlaybackVariant.PRIMARY)
            }

            resume(currentIndex)
        }

        fun playerForIndex(index: Int): ExoPlayer? = boundSlots[index]?.player

        fun handlePlaybackError(index: Int): Boolean {
            val reel = currentFeed.getOrNull(index) ?: return false
            val slot = boundSlots[index] ?: return false
            val canFallback = slot.variant == PlaybackVariant.PRIMARY && preloadController.hasFallback(reel)
            if (!canFallback) return false

            bind(index, reel, preferredVariant = PlaybackVariant.FALLBACK_MP4)
            if (index == currentIndex) {
                resume(currentIndex)
            }
            return true
        }

        fun retry(index: Int) {
            val reel = currentFeed.getOrNull(index) ?: return
            val variant = boundSlots[index]?.variant ?: PlaybackVariant.PRIMARY
            bind(index, reel, preferredVariant = variant)
            if (index == currentIndex) {
                resume(currentIndex)
            }
        }

        fun resume(currentIndex: Int) {
            this.currentIndex = currentIndex
            boundSlots.forEach { (index, slot) ->
                if (index == currentIndex) {
                    slot.player.volume = 1f
                    slot.player.playWhenReady = true
                    slot.player.play()
                } else {
                    slot.player.volume = 0f
                    slot.player.playWhenReady = false
                    slot.player.pause()
                }
            }
        }

        fun pauseAll(resetPosition: Boolean) {
            boundSlots.values.forEach { slot ->
                slot.player.playWhenReady = false
                slot.player.pause()
                slot.player.volume = 0f
                if (resetPosition) {
                    slot.player.seekTo(0)
                }
            }
        }

        fun release() {
            pauseAll(resetPosition = true)
            boundSlots.clear()
            players.forEach { player ->
                player.clearMediaItems()
                player.release()
            }
            preloadController.release()
        }

        private fun bind(index: Int, reel: Reel, preferredVariant: PlaybackVariant) {
            val existing = boundSlots[index]
            if (existing != null && existing.reelId == reel.id && existing.variant == preferredVariant) {
                return
            }

            val player = existing?.player ?: acquirePlayerFor(index)
            player.playWhenReady = false
            player.pause()
            player.stop()
            player.clearMediaItems()
            player.setMediaSource(preloadController.mediaSourceFor(reel, index, preferredVariant))
            player.prepare()

            boundSlots[index] = BoundSlot(
                player = player,
                reelId = reel.id,
                variant = preferredVariant
            )
        }

        private fun unbind(index: Int) {
            val slot = boundSlots.remove(index) ?: return
            slot.player.playWhenReady = false
            slot.player.pause()
            slot.player.stop()
            slot.player.clearMediaItems()
            slot.player.volume = 0f
        }

        private fun acquirePlayerFor(targetIndex: Int): ExoPlayer {
            val unboundPlayer = players.firstOrNull { candidate ->
                boundSlots.values.none { it.player == candidate }
            }
            if (unboundPlayer != null) return unboundPlayer

            val entryToRecycle = boundSlots.entries
                .maxByOrNull { (index, _) -> abs(index - targetIndex) }
                ?: error("Player pool exhausted without a recyclable slot")

            boundSlots.remove(entryToRecycle.key)
            entryToRecycle.value.player.playWhenReady = false
            entryToRecycle.value.player.pause()
            entryToRecycle.value.player.stop()
            entryToRecycle.value.player.clearMediaItems()
            entryToRecycle.value.player.volume = 0f
            return entryToRecycle.value.player
        }

        private fun buildTargetIndices(currentIndex: Int, lastIndex: Int): List<Int> {
            return listOf(
                currentIndex,
                currentIndex + 1,
                currentIndex + 2,
                currentIndex - 1
            ).distinct().filter { it in 0..lastIndex }
        }
    }

    private data class BoundSlot(
        val player: ExoPlayer,
        val reelId: String,
        val variant: PlaybackVariant
    )
}
