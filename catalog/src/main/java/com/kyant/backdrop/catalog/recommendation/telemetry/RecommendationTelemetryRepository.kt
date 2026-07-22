package com.kyant.backdrop.catalog.recommendation.telemetry

import android.content.Context
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.models.RecommendationClientEvent
import java.util.UUID

class RecommendationTelemetryRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dao = RecommendationTelemetryDatabase.getInstance(appContext).telemetryDao()

    suspend fun enqueue(event: RecommendationClientEvent, dedupeKey: String): Boolean {
        val ownerUserId = ApiClient.getCurrentUserId(appContext)?.trim()?.takeIf { it.isNotEmpty() }
            ?: return false
        val inserted = dao.insert(
            RecommendationTelemetryEntity(
                eventId = event.eventId.ifBlank { UUID.randomUUID().toString() },
                ownerUserId = ownerUserId,
                dedupeKey = dedupeKey,
                eventType = event.eventType,
                recommendationSessionId = event.recommendationSessionId,
                requestId = event.requestId,
                surface = event.surface,
                entityType = event.entityType,
                entityId = event.entityId,
                reportedPosition = event.reportedPosition,
                maxVisibleFraction = event.maxVisibleFraction,
                visibleTimeMs = event.visibleTimeMs,
                playbackTimeMs = event.playbackTimeMs,
                mediaDurationMs = event.mediaDurationMs,
                occurredAt = event.occurredAt
            )
        ) != -1L
        if (inserted) {
            RecommendationTelemetryWorker.enqueue(appContext, immediate = dao.count(ownerUserId) >= 50)
        }
        return inserted
    }

    suspend fun nextBatch(ownerUserId: String): List<RecommendationTelemetryEntity> = dao.oldest(ownerUserId, 100)
    suspend fun delete(eventIds: List<String>) = dao.delete(eventIds)
    suspend fun recordAttempt(eventIds: List<String>) = dao.recordAttempt(eventIds)

    fun flush() = RecommendationTelemetryWorker.enqueue(appContext, immediate = true)
}
