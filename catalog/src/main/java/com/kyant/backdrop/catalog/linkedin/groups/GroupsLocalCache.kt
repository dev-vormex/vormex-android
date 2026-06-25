package com.kyant.backdrop.catalog.linkedin.groups

import android.content.Context
import com.kyant.backdrop.catalog.network.models.Circle
import com.kyant.backdrop.catalog.network.models.CirclesResponse
import com.kyant.backdrop.catalog.network.models.GroupCategoriesResponse
import com.kyant.backdrop.catalog.network.models.GroupInvitesResponse
import com.kyant.backdrop.catalog.network.models.GroupsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val GROUPS_CACHE_PREFS = "vormex_groups_local_cache"

private val groupsCacheJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@Serializable
private data class CachedGroupsResponse(
    val response: GroupsResponse,
    val savedAtMillis: Long = System.currentTimeMillis()
)

@Serializable
private data class CachedGroupInvitesResponse(
    val response: GroupInvitesResponse,
    val savedAtMillis: Long = System.currentTimeMillis()
)

@Serializable
private data class CachedGroupCategoriesResponse(
    val response: GroupCategoriesResponse,
    val savedAtMillis: Long = System.currentTimeMillis()
)

@Serializable
private data class CachedCirclesResponse(
    val response: CirclesResponse,
    val savedAtMillis: Long = System.currentTimeMillis()
)

object GroupsLocalCache {
    suspend fun readMyGroups(context: Context, userId: String?): GroupsResponse? {
        return readCache<CachedGroupsResponse>(context, key("groups_my", userId))?.response
    }

    suspend fun writeMyGroups(context: Context, userId: String?, response: GroupsResponse) {
        writeCache(context, key("groups_my", userId), CachedGroupsResponse(response))
    }

    suspend fun readDiscoverGroups(
        context: Context,
        userId: String?,
        search: String?,
        category: String?
    ): GroupsResponse? {
        return readCache<CachedGroupsResponse>(
            context,
            key("groups_discover", userId, filterKey(search), filterKey(category))
        )?.response
    }

    suspend fun writeDiscoverGroups(
        context: Context,
        userId: String?,
        search: String?,
        category: String?,
        response: GroupsResponse
    ) {
        writeCache(
            context,
            key("groups_discover", userId, filterKey(search), filterKey(category)),
            CachedGroupsResponse(response)
        )
    }

    suspend fun readPendingInvites(context: Context, userId: String?): GroupInvitesResponse? {
        return readCache<CachedGroupInvitesResponse>(context, key("groups_invites", userId))?.response
    }

    suspend fun writePendingInvites(
        context: Context,
        userId: String?,
        response: GroupInvitesResponse
    ) {
        writeCache(context, key("groups_invites", userId), CachedGroupInvitesResponse(response))
    }

    suspend fun readCategories(context: Context): GroupCategoriesResponse? {
        return readCache<CachedGroupCategoriesResponse>(context, key("groups_categories", null))?.response
    }

    suspend fun writeCategories(context: Context, response: GroupCategoriesResponse) {
        writeCache(context, key("groups_categories", null), CachedGroupCategoriesResponse(response))
    }

    suspend fun readMyCircles(context: Context, userId: String?): CirclesResponse? {
        return readCache<CachedCirclesResponse>(context, key("circles_my", userId))?.response
    }

    suspend fun writeMyCircles(context: Context, userId: String?, circles: List<Circle>) {
        writeCache(context, key("circles_my", userId), CachedCirclesResponse(CirclesResponse(circles)))
    }

    suspend fun readDiscoverCircles(
        context: Context,
        userId: String?,
        search: String?,
        category: String?
    ): CirclesResponse? {
        return readCache<CachedCirclesResponse>(
            context,
            key("circles_discover", userId, filterKey(search), filterKey(category))
        )?.response
    }

    suspend fun writeDiscoverCircles(
        context: Context,
        userId: String?,
        search: String?,
        category: String?,
        response: CirclesResponse
    ) {
        writeCache(
            context,
            key("circles_discover", userId, filterKey(search), filterKey(category)),
            CachedCirclesResponse(response)
        )
    }

    private suspend inline fun <reified T> readCache(context: Context, cacheKey: String): T? {
        return withContext(Dispatchers.IO) {
            val raw = prefs(context).getString(cacheKey, null) ?: return@withContext null
            runCatching { groupsCacheJson.decodeFromString<T>(raw) }.getOrNull()
        }
    }

    private suspend inline fun <reified T> writeCache(context: Context, cacheKey: String, value: T) {
        withContext(Dispatchers.IO) {
            runCatching {
                prefs(context)
                    .edit()
                    .putString(cacheKey, groupsCacheJson.encodeToString(value))
                    .apply()
            }
        }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(GROUPS_CACHE_PREFS, Context.MODE_PRIVATE)

    private fun key(namespace: String, userId: String?, vararg parts: String): String {
        val owner = userId?.trim()?.takeIf { it.isNotBlank() } ?: "anonymous"
        return buildString {
            append(namespace)
            append('|')
            append(owner)
            parts.forEach { part ->
                append('|')
                append(part)
            }
        }
    }

    private fun filterKey(value: String?): String {
        return value
            ?.trim()
            ?.lowercase()
            ?.take(80)
            ?.replace("|", "%7C")
            ?.takeIf { it.isNotBlank() }
            ?: "all"
    }
}
