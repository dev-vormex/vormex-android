package com.kyant.backdrop.catalog.linkedin

import android.content.Context
import com.kyant.backdrop.catalog.network.models.FullProfileResponse
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal data class CachedProfileSnapshot(
    val profile: FullProfileResponse,
    val cachedAtMillis: Long
)

internal class ProfileCacheStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "vormex_profile_cache",
        Context.MODE_PRIVATE
    )
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    fun get(
        cacheOwnerId: String,
        targetUserKey: String,
        maxAgeMillis: Long
    ): CachedProfileSnapshot? {
        val prefix = keyPrefix(cacheOwnerId, targetUserKey)
        val cachedAt = prefs.getLong("${prefix}:cachedAt", 0L)
        if (cachedAt <= 0L || System.currentTimeMillis() - cachedAt > maxAgeMillis) {
            return null
        }

        val rawProfile = prefs.getString("${prefix}:profile", null) ?: return null
        return runCatching {
            CachedProfileSnapshot(
                profile = json.decodeFromString(rawProfile),
                cachedAtMillis = cachedAt
            )
        }.getOrNull()
    }

    fun put(
        cacheOwnerId: String,
        targetUserKey: String,
        profile: FullProfileResponse
    ) {
        val prefix = keyPrefix(cacheOwnerId, targetUserKey)
        runCatching {
            prefs.edit()
                .putString("${prefix}:profile", json.encodeToString(profile))
                .putLong("${prefix}:cachedAt", System.currentTimeMillis())
                .apply()
        }
    }

    fun remove(cacheOwnerId: String, targetUserKey: String) {
        val prefix = keyPrefix(cacheOwnerId, targetUserKey)
        prefs.edit()
            .remove("${prefix}:profile")
            .remove("${prefix}:cachedAt")
            .apply()
    }

    private fun keyPrefix(cacheOwnerId: String, targetUserKey: String): String {
        return "profile:${cacheOwnerId.safeCacheKey()}:${targetUserKey.safeCacheKey()}"
    }

    private fun String.safeCacheKey(): String = replace(Regex("[^A-Za-z0-9_.-]"), "_")
}
