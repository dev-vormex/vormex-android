package com.kyant.backdrop.catalog.recommendation.telemetry

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [RecommendationTelemetryEntity::class], version = 2, exportSchema = false)
abstract class RecommendationTelemetryDatabase : RoomDatabase() {
    abstract fun telemetryDao(): RecommendationTelemetryDao

    companion object {
        @Volatile private var instance: RecommendationTelemetryDatabase? = null

        fun getInstance(context: Context): RecommendationTelemetryDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RecommendationTelemetryDatabase::class.java,
                    "recommendation_telemetry.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }

        /**
         * V1 events did not record their account owner, so retaining them could upload an
         * exposure under the wrong saved account. Only this disposable telemetry outbox is
         * rebuilt; no account, feed, post, chat, or other application data is touched.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS recommendation_telemetry_outbox_v2 (
                        eventId TEXT NOT NULL PRIMARY KEY,
                        ownerUserId TEXT NOT NULL,
                        dedupeKey TEXT NOT NULL,
                        eventType TEXT NOT NULL,
                        recommendationSessionId TEXT NOT NULL,
                        requestId TEXT,
                        surface TEXT NOT NULL,
                        entityType TEXT NOT NULL,
                        entityId TEXT NOT NULL,
                        reportedPosition INTEGER,
                        maxVisibleFraction REAL,
                        visibleTimeMs INTEGER,
                        playbackTimeMs INTEGER,
                        mediaDurationMs INTEGER,
                        occurredAt TEXT NOT NULL,
                        attempts INTEGER NOT NULL,
                        createdAtEpochMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE recommendation_telemetry_outbox")
                db.execSQL("ALTER TABLE recommendation_telemetry_outbox_v2 RENAME TO recommendation_telemetry_outbox")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_recommendation_telemetry_outbox_ownerUserId_dedupeKey " +
                        "ON recommendation_telemetry_outbox(ownerUserId, dedupeKey)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_recommendation_telemetry_outbox_ownerUserId_createdAtEpochMillis " +
                        "ON recommendation_telemetry_outbox(ownerUserId, createdAtEpochMillis)"
                )
            }
        }
    }
}
