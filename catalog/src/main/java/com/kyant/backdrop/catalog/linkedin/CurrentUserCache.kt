package com.kyant.backdrop.catalog.linkedin

import android.content.Context
import com.kyant.backdrop.catalog.data.VormexPerformancePolicy
import com.kyant.backdrop.catalog.network.models.User
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class CachedCurrentUser(
    val user: User,
    val cachedAtMillis: Long
)

object CurrentUserCache {
    private const val PREFS_NAME = "vormex_current_user_cache"
    private const val KEY_PREFIX = "user_"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    fun read(context: Context, userId: String?): CachedCurrentUser? {
        val cacheOwnerId = userId?.takeIf { it.isNotBlank() } ?: return null
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = cacheKey(cacheOwnerId)
        val raw = prefs.getString(key, null) ?: return null
        val cached = decodeCachedUser(raw)
        return if (cached == null || cached.isTooOld()) {
            prefs.edit().remove(key).apply()
            null
        } else {
            cached
        }
    }

    fun readLatest(context: Context): CachedCurrentUser? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        var hasInvalidEntries = false
        var latest: CachedCurrentUser? = null

        prefs.all.forEach { (key, value) ->
            if (!key.startsWith(KEY_PREFIX) || value !is String) return@forEach

            val cached = decodeCachedUser(value)
            if (cached == null || cached.isTooOld()) {
                editor.remove(key)
                hasInvalidEntries = true
                return@forEach
            }

            val previous = latest
            if (previous == null || cached.cachedAtMillis > previous.cachedAtMillis) {
                latest = cached
            }
        }

        if (hasInvalidEntries) {
            editor.apply()
        }

        return latest
    }

    fun write(context: Context, user: User) {
        val cached = CachedCurrentUser(
            user = user,
            cachedAtMillis = System.currentTimeMillis()
        )
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(cacheKey(user.id), json.encodeToString(cached))
            .apply()
    }

    fun clearAll(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    private fun cacheKey(userId: String): String = KEY_PREFIX + userId

    private fun decodeCachedUser(raw: String): CachedCurrentUser? =
        try {
            json.decodeFromString<CachedCurrentUser>(raw)
        } catch (_: Exception) {
            null
        }

    private fun CachedCurrentUser.isTooOld(): Boolean =
        System.currentTimeMillis() - cachedAtMillis >
            VormexPerformancePolicy.PersistentCurrentUserCacheMaxAgeMillis
}
