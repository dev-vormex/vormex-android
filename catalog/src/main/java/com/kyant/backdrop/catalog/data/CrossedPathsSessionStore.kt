package com.kyant.backdrop.catalog.data

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.crossedPathsDataStore by preferencesDataStore(
    name = "crossed_paths_session",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)

data class StoredCrossedPathsSession(
    val sessionId: String,
    val generation: Int,
    val sequence: Long,
    val expiresAt: String,
    val radiusM: Int,
    val clientStartId: String,
    val stopPending: Boolean
)

object CrossedPathsSessionStore {
    private val SESSION_ID = stringPreferencesKey("session_id")
    private val GENERATION = intPreferencesKey("generation")
    private val SEQUENCE = longPreferencesKey("sequence")
    private val EXPIRES_AT = stringPreferencesKey("expires_at")
    private val RADIUS = intPreferencesKey("radius_m")
    private val CLIENT_START_ID = stringPreferencesKey("client_start_id")
    private val STOP_PENDING = booleanPreferencesKey("stop_pending")

    suspend fun read(context: Context): StoredCrossedPathsSession? {
        val values = context.crossedPathsDataStore.data.first()
        val id = values[SESSION_ID]?.takeIf { it.isNotBlank() } ?: return null
        return StoredCrossedPathsSession(id, values[GENERATION] ?: 1, values[SEQUENCE] ?: 1,
            values[EXPIRES_AT].orEmpty(), values[RADIUS] ?: 500, values[CLIENT_START_ID].orEmpty(), values[STOP_PENDING] ?: false)
    }

    suspend fun save(context: Context, value: StoredCrossedPathsSession) {
        context.crossedPathsDataStore.edit {
            it[SESSION_ID] = value.sessionId; it[GENERATION] = value.generation; it[SEQUENCE] = value.sequence
            it[EXPIRES_AT] = value.expiresAt; it[RADIUS] = value.radiusM; it[CLIENT_START_ID] = value.clientStartId
            it[STOP_PENDING] = value.stopPending
        }
    }

    suspend fun advanceSequence(context: Context, acceptedSequence: Long) {
        context.crossedPathsDataStore.edit { current -> current[SEQUENCE] = maxOf(current[SEQUENCE] ?: 1, acceptedSequence + 1) }
    }
    suspend fun markStopPending(context: Context, pending: Boolean) { context.crossedPathsDataStore.edit { it[STOP_PENDING] = pending } }
    suspend fun clear(context: Context) { context.crossedPathsDataStore.edit { it.clear() } }
}
