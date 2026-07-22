package com.kyant.backdrop.catalog.linkedin

import android.content.Context
import com.kyant.backdrop.catalog.network.models.Notification
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
internal data class CachedNotificationsInbox(
    val notifications: List<Notification>,
    val latestCursor: String? = null,
    val unreadCount: Int = 0,
    val cachedAtMillis: Long = 0L
)

internal object NotificationsInboxCacheStore {
    private const val PrefsName = "vormex_notifications_inbox_cache"
    private const val KeyPrefix = "notifications_"
    private const val MaxCachedNotifications = 200

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    fun read(context: Context, userId: String?): CachedNotificationsInbox? {
        val ownerId = userId?.takeIf { it.isNotBlank() } ?: return null
        val prefs = context.applicationContext.getSharedPreferences(PrefsName, Context.MODE_PRIVATE)
        val key = cacheKey(ownerId)
        val raw = prefs.getString(key, null) ?: return null

        return try {
            json.decodeFromString<CachedNotificationsInbox>(raw)
        } catch (_: Exception) {
            prefs.edit().remove(key).apply()
            null
        }
    }

    fun write(
        context: Context,
        userId: String?,
        notifications: List<Notification>,
        latestCursor: String?,
        unreadCount: Int
    ) {
        val ownerId = userId?.takeIf { it.isNotBlank() } ?: return
        val cached = CachedNotificationsInbox(
            notifications = notifications.take(MaxCachedNotifications),
            latestCursor = latestCursor,
            unreadCount = unreadCount.coerceAtLeast(0),
            cachedAtMillis = System.currentTimeMillis()
        )

        runCatching {
            context.applicationContext
                .getSharedPreferences(PrefsName, Context.MODE_PRIVATE)
                .edit()
                .putString(cacheKey(ownerId), json.encodeToString(cached))
                .apply()
        }
    }

    private fun cacheKey(userId: String): String =
        KeyPrefix + userId.replace(Regex("[^A-Za-z0-9_.-]"), "_")
}
