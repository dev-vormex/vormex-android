package com.kyant.backdrop.catalog.recommendation.telemetry

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RecommendationTelemetryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: RecommendationTelemetryEntity): Long

    @Query("SELECT * FROM recommendation_telemetry_outbox WHERE ownerUserId = :ownerUserId ORDER BY createdAtEpochMillis ASC LIMIT :limit")
    suspend fun oldest(ownerUserId: String, limit: Int = 100): List<RecommendationTelemetryEntity>

    @Query("SELECT COUNT(*) FROM recommendation_telemetry_outbox WHERE ownerUserId = :ownerUserId")
    suspend fun count(ownerUserId: String): Int

    @Query("DELETE FROM recommendation_telemetry_outbox WHERE eventId IN (:eventIds)")
    suspend fun delete(eventIds: List<String>)

    @Query("UPDATE recommendation_telemetry_outbox SET attempts = attempts + 1 WHERE eventId IN (:eventIds)")
    suspend fun recordAttempt(eventIds: List<String>)
}
