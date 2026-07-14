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

data class CachedHomeFeedSnapshot(
    val userId: String,
    val cachedFeed: CachedHomeFeed
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
        val cached = decodeCachedFeed(raw)
        return if (cached == null || cached.isTooOld()) {
            prefs.edit().remove(key).apply()
            null
        } else {
            cached
        }
    }

    fun readLatest(context: Context): CachedHomeFeedSnapshot? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        var hasInvalidEntries = false
        var latest: CachedHomeFeedSnapshot? = null

        prefs.all.forEach { (key, value) ->
            if (!key.startsWith(KEY_PREFIX) || value !is String) return@forEach

            val userId = key.removePrefix(KEY_PREFIX).takeIf { it.isNotBlank() }
            val cached = decodeCachedFeed(value)
            if (userId == null || cached == null || cached.isTooOld()) {
                editor.remove(key)
                hasInvalidEntries = true
                return@forEach
            }

            val previous = latest
            if (previous == null || cached.cachedAtMillis > previous.cachedFeed.cachedAtMillis) {
                latest = CachedHomeFeedSnapshot(userId, cached)
            }
        }

        if (hasInvalidEntries) {
            editor.apply()
        }

        return latest
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

    private fun decodeCachedFeed(raw: String): CachedHomeFeed? =
        try {
            json.decodeFromString<CachedHomeFeed>(raw)
        } catch (_: Exception) {
            null
        }

    private fun CachedHomeFeed.isTooOld(): Boolean =
        System.currentTimeMillis() - cachedAtMillis >
            VormexPerformancePolicy.PersistentFeedCacheMaxAgeMillis
}
