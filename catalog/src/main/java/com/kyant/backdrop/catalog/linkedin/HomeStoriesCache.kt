package com.kyant.backdrop.catalog.linkedin

import android.content.Context
import com.kyant.backdrop.catalog.data.VormexPerformancePolicy
import com.kyant.backdrop.catalog.network.models.StoriesFeedResponse
import com.kyant.backdrop.catalog.network.models.Story
import com.kyant.backdrop.catalog.network.models.StoryGroup
import java.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class CachedHomeStories(
    val response: StoriesFeedResponse,
    val cachedAtMillis: Long
)

data class CachedHomeStoriesSnapshot(
    val userId: String,
    val cachedStories: CachedHomeStories
)

object HomeStoriesCache {
    private const val PREFS_NAME = "vormex_home_stories_cache"
    private const val KEY_PREFIX = "stories_"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    fun read(context: Context, userId: String?): CachedHomeStories? {
        val cacheOwnerId = userId?.takeIf { it.isNotBlank() } ?: return null
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = cacheKey(cacheOwnerId)
        val raw = prefs.getString(key, null) ?: return null
        val cached = decodeCachedStories(raw)
        return if (cached == null || cached.isTooOld()) {
            prefs.edit().remove(key).apply()
            null
        } else {
            cached.withLiveStoriesOnly()
        }
    }

    fun readLatest(context: Context): CachedHomeStoriesSnapshot? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        var hasInvalidEntries = false
        var latest: CachedHomeStoriesSnapshot? = null

        prefs.all.forEach { (key, value) ->
            if (!key.startsWith(KEY_PREFIX) || value !is String) return@forEach

            val userId = key.removePrefix(KEY_PREFIX).takeIf { it.isNotBlank() }
            val cached = decodeCachedStories(value)
            if (userId == null || cached == null || cached.isTooOld()) {
                editor.remove(key)
                hasInvalidEntries = true
                return@forEach
            }

            val liveCached = cached.withLiveStoriesOnly()
            val previous = latest
            if (previous == null || liveCached.cachedAtMillis > previous.cachedStories.cachedAtMillis) {
                latest = CachedHomeStoriesSnapshot(userId, liveCached)
            }
        }

        if (hasInvalidEntries) {
            editor.apply()
        }

        return latest
    }

    fun write(context: Context, userId: String?, response: StoriesFeedResponse) {
        val cacheOwnerId = userId?.takeIf { it.isNotBlank() } ?: return
        val cached = CachedHomeStories(
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

    private fun decodeCachedStories(raw: String): CachedHomeStories? =
        try {
            json.decodeFromString<CachedHomeStories>(raw)
        } catch (_: Exception) {
            null
        }

    private fun CachedHomeStories.isTooOld(): Boolean =
        System.currentTimeMillis() - cachedAtMillis >
            VormexPerformancePolicy.PersistentHomeStoriesCacheMaxAgeMillis

    private fun CachedHomeStories.withLiveStoriesOnly(): CachedHomeStories =
        copy(
            response = response.copy(
                storyGroups = response.storyGroups
                    .mapNotNull { it.withLiveStoriesOnly() }
            )
        )

    private fun StoryGroup.withLiveStoriesOnly(): StoryGroup? {
        val liveStories = stories.filter { !it.isExpired() }
        return if (liveStories.isEmpty()) null else copy(
            stories = liveStories,
            hasUnviewed = !isOwnStory && liveStories.any { !it.isViewed }
        )
    }

    private fun Story.isExpired(): Boolean =
        try {
            Instant.parse(expiresAt).toEpochMilli() <= System.currentTimeMillis()
        } catch (_: Exception) {
            false
        }
}
