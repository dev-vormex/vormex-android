package com.kyant.backdrop.catalog.linkedin.reels

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

internal object ReelsTelemetry {
    const val VIEWER_OPEN_TO_FIRST_FRAME_P75_TARGET_MS = 1_000L
    const val APP_OPEN_TO_REEL_FIRST_FRAME_P75_TARGET_MS = 1_500L
    const val EARLY_STOP_RATE_TARGET_BPS = 50L
    const val VIDEO_ERROR_RATE_TARGET_BPS = 150L
    const val PAGINATION_SUCCESS_RATE_TARGET_BPS = 9_900L

    private fun analytics(context: Context): FirebaseAnalytics =
        FirebaseAnalytics.getInstance(context.applicationContext)

    fun log(context: Context, eventName: String, params: Bundle.() -> Unit = {}) {
        analytics(context).logEvent(eventName, Bundle().apply(params))
    }

    fun viewerOpened(context: Context, source: String, cached: Boolean) {
        log(context, "reels_viewer_open") {
            putString("source", source)
            putLong("cached", if (cached) 1L else 0L)
        }
    }

    fun firstFrame(context: Context, reelId: String, viewerOpenMs: Long, appOpenMs: Long) {
        log(context, "reels_first_frame") {
            putString("reel_id", reelId.take(64))
            putLong("viewer_open_ms", viewerOpenMs.coerceAtLeast(0L))
            putLong("app_open_ms", appOpenMs.coerceAtLeast(0L))
        }
    }

    fun pagination(context: Context, success: Boolean, itemCount: Int, reason: String? = null) {
        log(context, "reels_pagination") {
            putLong("success", if (success) 1L else 0L)
            putLong("item_count", itemCount.toLong())
            reason?.let { putString("reason", it.take(80)) }
        }
    }

    fun playbackError(context: Context, reelId: String, reason: String, fallbackUsed: Boolean) {
        log(context, "reels_video_error") {
            putString("reel_id", reelId.take(64))
            putString("reason", reason.take(80))
            putLong("fallback_used", if (fallbackUsed) 1L else 0L)
        }
    }

    fun nativeAd(context: Context, action: String, slotKey: String, reason: String? = null) {
        log(context, "reels_native_ad") {
            putString("action", action.take(40))
            putString("slot_key", slotKey.take(64))
            reason?.let { putString("reason", it.take(80)) }
        }
    }

    fun sessionClosed(context: Context, watchedCount: Int) {
        log(context, "reels_session_closed") {
            putLong("watched_count", watchedCount.toLong())
        }
    }
}
