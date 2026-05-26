package com.kyant.backdrop.catalog.linkedin

import android.content.Context
import com.kyant.backdrop.catalog.data.VormexPerformancePolicy
import com.kyant.backdrop.catalog.network.models.FeedResponse
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class CachedHomeFeed(
    val response: FeedResponse,
    val cachedAtMillis: Long
)

object HomeFeedCache {
    private const val PREFS_NAME = "vormex_home_feed_cache"
    private const val KEY_PREFIX = "feed_"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    fun read(context: Context, userId: String?): CachedHomeFeed? {
        val cacheOwnerId = userId?.takeIf { it.isNotBlank() } ?: return null
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = cacheKey(cacheOwnerId)
        val raw = prefs.getString(key, null) ?: return null
        return try {
            val cached = json.decodeFromString<CachedHomeFeed>(raw)
            val isTooOld =
                System.currentTimeMillis() - cached.cachedAtMillis >
                    VormexPerformancePolicy.PersistentFeedCacheMaxAgeMillis
            if (isTooOld) {
                prefs.edit().remove(key).apply()
                null
            } else {
                cached
            }
        } catch (_: Exception) {
            prefs.edit().remove(key).apply()
            null
        }
    }

    fun write(context: Context, userId: String?, response: FeedResponse) {
        val cacheOwnerId = userId?.takeIf { it.isNotBlank() } ?: return
        val cached = CachedHomeFeed(
            response = response,
            cachedAtMillis = System.currentTimeMillis()
        )
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(cacheKey(cacheOwnerId), json.encodeToString(cached))
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
}
