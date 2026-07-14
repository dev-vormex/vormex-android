package com.kyant.backdrop.catalog.linkedin.reels

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kyant.backdrop.catalog.network.models.ManagedAdPlacement
import com.kyant.backdrop.catalog.network.models.Reel
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.reelsFeedCacheDataStore by preferencesDataStore(name = "vormex_reels_feed_cache")

private val REELS_FEED_CACHE_JSON = stringPreferencesKey("reels_feed_cache_json")
private val REELS_FEED_CACHE_UPDATED_AT = longPreferencesKey("reels_feed_cache_updated_at")
private val REELS_FEED_CACHE_OWNER_ID = stringPreferencesKey("reels_feed_cache_owner_id")

internal const val REELS_FEED_MEMORY_FRESH_MS = 5 * 60 * 1000L
internal const val REELS_FEED_DISK_STALE_MS = 30 * 60 * 1000L
internal const val REELS_DISK_CACHE_MAX_REELS = 30
internal const val REELS_FEED_CACHE_VERSION = 2

@Serializable
internal data class CachedReelsFeed(
    val reels: List<Reel>,
    val nextCursor: String? = null,
    val hasMore: Boolean = true,
    val adPlacements: List<ManagedAdPlacement> = emptyList(),
    val updatedAt: Long = 0L,
    val ownerUserId: String? = null,
    val truncated: Boolean = false,
    val cacheVersion: Int = 0
) {
    val ageMs: Long
        get() = System.currentTimeMillis() - updatedAt
}

internal data class CachedReelsPaginationState(
    val nextCursor: String?,
    val hasMore: Boolean
)

internal object ReelsFeedCacheStore {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun read(context: Context, ownerUserId: String?): CachedReelsFeed? {
        val prefs = context.applicationContext.reelsFeedCacheDataStore.data.first()
        if (prefs[REELS_FEED_CACHE_OWNER_ID] != ownerUserId) return null
        val raw = prefs[REELS_FEED_CACHE_JSON] ?: return null
        val updatedAt = prefs[REELS_FEED_CACHE_UPDATED_AT] ?: 0L
        return decodeCachedReelsFeed(raw, updatedAt, ownerUserId)
    }

    suspend fun write(
        context: Context,
        reels: List<Reel>,
        nextCursor: String?,
        hasMore: Boolean,
        adPlacements: List<ManagedAdPlacement>,
        ownerUserId: String?
    ) {
        val now = System.currentTimeMillis()
        val cached = createCacheSnapshot(
            reels = reels,
            nextCursor = nextCursor,
            hasMore = hasMore,
            adPlacements = adPlacements,
            updatedAt = now,
            ownerUserId = ownerUserId
        )
        val encoded = json.encodeToString(cached)
        context.applicationContext.reelsFeedCacheDataStore.edit { prefs ->
            prefs[REELS_FEED_CACHE_JSON] = encoded
            prefs[REELS_FEED_CACHE_UPDATED_AT] = now
            ownerUserId?.let { prefs[REELS_FEED_CACHE_OWNER_ID] = it }
                ?: prefs.remove(REELS_FEED_CACHE_OWNER_ID)
        }
    }

    suspend fun clear(context: Context) {
        context.applicationContext.reelsFeedCacheDataStore.edit { prefs ->
            prefs.clear()
        }
    }

    internal fun decodeCachedReelsFeed(
        raw: String,
        updatedAt: Long,
        ownerUserId: String?
    ): CachedReelsFeed? {
        return runCatching {
            val decoded = json.decodeFromString<CachedReelsFeed>(raw)
            if (decoded.cacheVersion < REELS_FEED_CACHE_VERSION) return@runCatching null
            decoded.copy(
                updatedAt = updatedAt,
                ownerUserId = ownerUserId
            )
        }.getOrNull()
    }
}

internal fun createCacheSnapshot(
    reels: List<Reel>,
    nextCursor: String?,
    hasMore: Boolean,
    adPlacements: List<ManagedAdPlacement>,
    updatedAt: Long,
    ownerUserId: String?
): CachedReelsFeed {
    val cachedReels = reels.take(REELS_DISK_CACHE_MAX_REELS)
    val wasTruncated = reels.size > REELS_DISK_CACHE_MAX_REELS
    return CachedReelsFeed(
        reels = cachedReels,
        nextCursor = if (wasTruncated) null else nextCursor,
        hasMore = if (wasTruncated) true else hasMore,
        adPlacements = adPlacements.filter { placement ->
            placement.placement.equals("reels", ignoreCase = true) &&
                placement.afterItemCount in 1..cachedReels.size
        },
        updatedAt = updatedAt,
        ownerUserId = ownerUserId,
        truncated = wasTruncated,
        cacheVersion = REELS_FEED_CACHE_VERSION
    )
}

internal fun restoredPaginationState(cached: CachedReelsFeed): CachedReelsPaginationState {
    val paginationUnknown =
        cached.truncated ||
            (cached.reels.size == REELS_DISK_CACHE_MAX_REELS && !cached.hasMore)
    return if (paginationUnknown) {
        CachedReelsPaginationState(nextCursor = null, hasMore = true)
    } else {
        CachedReelsPaginationState(nextCursor = cached.nextCursor, hasMore = cached.hasMore)
    }
}

internal fun mergeReelsById(
    existing: List<Reel>,
    incoming: List<Reel>,
    seenReelIdsThisSession: MutableSet<String>? = null
): List<Reel> {
    if (existing.isEmpty() && incoming.isEmpty()) return emptyList()

    val merged = ArrayList<Reel>(existing.size + incoming.size)
    val emitted = LinkedHashSet<String>()

    existing.forEach { reel ->
        if (emitted.add(reel.id)) {
            merged += reel
            seenReelIdsThisSession?.add(reel.id)
        }
    }

    incoming.forEach { reel ->
        val sessionAllows = seenReelIdsThisSession?.add(reel.id) ?: true
        if (sessionAllows && emitted.add(reel.id)) {
            merged += reel
        }
    }

    return merged
}

internal fun indexOfReelIdOrNearest(
    reels: List<Reel>,
    reelId: String?,
    fallbackIndex: Int
): Int {
    if (reels.isEmpty()) return 0
    if (!reelId.isNullOrBlank()) {
        val exact = reels.indexOfFirst { it.id == reelId }
        if (exact >= 0) return exact
    }
    return fallbackIndex.coerceIn(0, reels.lastIndex)
}
