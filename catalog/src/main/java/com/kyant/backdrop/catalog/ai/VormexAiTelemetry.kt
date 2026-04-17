package com.kyant.backdrop.catalog.ai

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

object VormexAiTelemetry {
    private const val PARAM_SURFACE = "surface"
    private const val PARAM_OPERATION = "operation"
    private const val PARAM_REASON = "reason"

    private fun analytics(context: Context): FirebaseAnalytics {
        return FirebaseAnalytics.getInstance(context.applicationContext)
    }

    private fun log(
        context: Context,
        eventName: String,
        surface: VormexAiSurface,
        operation: VormexAiOperationKind,
        reason: String? = null
    ) {
        val params = Bundle().apply {
            putString(PARAM_SURFACE, surface.wireName)
            putString(PARAM_OPERATION, operation.wireName)
            reason?.let { putString(PARAM_REASON, it.take(100)) }
        }
        analytics(context).logEvent(eventName, params)
    }

    fun localSuccess(context: Context, surface: VormexAiSurface, operation: VormexAiOperationKind) {
        log(context, "vormex_ai_local_success", surface, operation)
    }

    fun localUnsupported(
        context: Context,
        surface: VormexAiSurface,
        operation: VormexAiOperationKind,
        reason: String
    ) {
        log(context, "vormex_ai_local_unsupported", surface, operation, reason)
    }

    fun localBusy(
        context: Context,
        surface: VormexAiSurface,
        operation: VormexAiOperationKind,
        reason: String
    ) {
        log(context, "vormex_ai_local_busy", surface, operation, reason)
    }

    fun cloudUsed(context: Context, surface: VormexAiSurface, operation: VormexAiOperationKind) {
        log(context, "vormex_ai_cloud_used", surface, operation)
    }

    fun cloudBlocked(
        context: Context,
        surface: VormexAiSurface,
        operation: VormexAiOperationKind,
        reason: String
    ) {
        log(context, "vormex_ai_cloud_blocked", surface, operation, reason)
    }

    fun prepareStarted(context: Context, surface: VormexAiSurface, operation: VormexAiOperationKind) {
        log(context, "vormex_ai_prepare_start", surface, operation)
    }

    fun prepareCompleted(context: Context, surface: VormexAiSurface, operation: VormexAiOperationKind) {
        log(context, "vormex_ai_prepare_done", surface, operation)
    }
}
