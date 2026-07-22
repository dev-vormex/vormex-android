package com.kyant.backdrop.catalog.recommendation.telemetry

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kyant.backdrop.catalog.network.models.RecommendationClientEvent

@Entity(
    tableName = "recommendation_telemetry_outbox",
    indices = [
        Index(value = ["ownerUserId", "dedupeKey"], unique = true),
        Index(value = ["ownerUserId", "createdAtEpochMillis"])
    ]
)
data class RecommendationTelemetryEntity(
    @PrimaryKey val eventId: String,
    val ownerUserId: String,
    val dedupeKey: String,
    val eventType: String,
    val recommendationSessionId: String,
    val requestId: String?,
    val surface: String,
    val entityType: String,
    val entityId: String,
    val reportedPosition: Int?,
    val maxVisibleFraction: Double?,
    val visibleTimeMs: Long?,
    val playbackTimeMs: Long?,
    val mediaDurationMs: Long?,
    val occurredAt: String,
    val attempts: Int = 0,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
) {
    fun toNetworkModel() = RecommendationClientEvent(
        eventId = eventId,
        eventType = eventType,
        recommendationSessionId = recommendationSessionId,
        requestId = requestId,
        surface = surface,
        entityType = entityType,
        entityId = entityId,
        reportedPosition = reportedPosition,
        maxVisibleFraction = maxVisibleFraction,
        visibleTimeMs = visibleTimeMs,
        playbackTimeMs = playbackTimeMs,
        mediaDurationMs = mediaDurationMs,
        occurredAt = occurredAt
    )
}
