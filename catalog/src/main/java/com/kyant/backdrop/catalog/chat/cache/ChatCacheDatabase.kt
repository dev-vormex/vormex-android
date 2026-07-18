package com.kyant.backdrop.catalog.chat.cache

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CachedConversationEntity::class, CachedMessageEntity::class, ChatOutboxEntity::class],
    version = 2,
    exportSchema = false
)
abstract class ChatCacheDatabase : RoomDatabase() {
    abstract fun chatCacheDao(): ChatCacheDao

    companion object {
        @Volatile
        private var instance: ChatCacheDatabase? = null

        fun getInstance(context: Context): ChatCacheDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ChatCacheDatabase::class.java,
                    "chat_cache.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cached_messages ADD COLUMN clientMessageId TEXT")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS chat_outbox (
                        cacheOwnerId TEXT NOT NULL,
                        clientMessageId TEXT NOT NULL,
                        conversationId TEXT NOT NULL,
                        senderId TEXT NOT NULL,
                        receiverId TEXT NOT NULL,
                        content TEXT NOT NULL,
                        contentType TEXT NOT NULL,
                        mediaUrl TEXT,
                        mediaType TEXT,
                        fileName TEXT,
                        fileSize INTEGER,
                        replyToId TEXT,
                        localFileUri TEXT,
                        localPreviewUri TEXT,
                        mimeType TEXT,
                        durationMs INTEGER,
                        createdAt TEXT NOT NULL,
                        createdAtEpochMillis INTEGER NOT NULL,
                        attempts INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        nextAttemptAtEpochMillis INTEGER NOT NULL,
                        PRIMARY KEY(cacheOwnerId, clientMessageId)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_chat_outbox_cacheOwnerId_status_nextAttemptAtEpochMillis " +
                        "ON chat_outbox(cacheOwnerId, status, nextAttemptAtEpochMillis)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_chat_outbox_cacheOwnerId_conversationId_createdAtEpochMillis " +
                        "ON chat_outbox(cacheOwnerId, conversationId, createdAtEpochMillis)"
                )
            }
        }
    }
}
