package com.kyant.backdrop.catalog.linkedin

import android.content.Context
import com.kyant.backdrop.catalog.network.models.PersonInfo
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest

@Serializable
internal data class CachedFindPeopleResult(
    val people: List<PersonInfo>,
    val total: Int,
    val hasMore: Boolean,
    val page: Int,
    val nextCursor: String?,
    val cachedAtMillis: Long
)

internal object FindPeopleCacheStore {
    private const val PrefsName = "vormex_find_people_cache"
    private const val AllPeoplePrefix = "all_people"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    fun readAllPeople(
        context: Context,
        cacheOwnerId: String?,
        queryKey: String,
        maxAgeMillis: Long
    ): CachedFindPeopleResult? {
        val ownerId = cacheOwnerId?.takeIf { it.isNotBlank() } ?: return null
        val prefs = context.applicationContext.getSharedPreferences(PrefsName, Context.MODE_PRIVATE)
        val key = allPeopleKey(ownerId, queryKey)
        val raw = prefs.getString(key, null) ?: return null

        return try {
            val cached = json.decodeFromString<CachedFindPeopleResult>(raw)
            val isTooOld = System.currentTimeMillis() - cached.cachedAtMillis > maxAgeMillis
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

    fun writeAllPeople(
        context: Context,
        cacheOwnerId: String?,
        queryKey: String,
        people: List<PersonInfo>,
        total: Int,
        hasMore: Boolean,
        page: Int,
        nextCursor: String?
    ) {
        val ownerId = cacheOwnerId?.takeIf { it.isNotBlank() } ?: return
        val cached = CachedFindPeopleResult(
            people = people,
            total = total,
            hasMore = hasMore,
            page = page,
            nextCursor = nextCursor,
            cachedAtMillis = System.currentTimeMillis()
        )

        runCatching {
            context.applicationContext
                .getSharedPreferences(PrefsName, Context.MODE_PRIVATE)
                .edit()
                .putString(allPeopleKey(ownerId, queryKey), json.encodeToString(cached))
                .apply()
        }
    }

    private fun allPeopleKey(cacheOwnerId: String, queryKey: String): String {
        return "$AllPeoplePrefix:${cacheOwnerId.safeCacheKey()}:${queryKey.sha256()}"
    }

    private fun String.safeCacheKey(): String = replace(Regex("[^A-Za-z0-9_.-]"), "_")

    private fun String.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray())
        return digest.joinToString(separator = "") { "%02x".format(it) }
    }
}
