package com.kyant.backdrop.catalog.network

import android.content.Context
import com.kyant.backdrop.catalog.BuildConfig
import com.kyant.backdrop.catalog.network.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * API Service for Groups and Circles endpoints.
 * Follows the same patterns as ApiClient.
 */
object GroupsApiService {
    private val BASE_URL = BuildConfig.API_BASE_URL
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    private val client = HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            }
        }
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            logger = Logger.ANDROID
            level = LogLevel.BODY
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 120000
            connectTimeoutMillis = 30000
            socketTimeoutMillis = 60000
        }
        defaultRequest {
            contentType(ContentType.Application.Json)
        }
    }
    
    // ==================== Groups API ====================
    
    /**
     * Create a new group
     */
    suspend fun createGroup(
        context: Context,
        name: String,
        description: String? = null,
        privacy: String = "PUBLIC",
        category: String? = null,
        tags: List<String> = emptyList(),
        rules: List<String> = emptyList()
    ): Result<Group> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/groups") {
                header("Authorization", "Bearer $token")
                setBody(CreateGroupRequest(
                    name = name,
                    description = description,
                    privacy = privacy,
                    category = category,
                    tags = tags,
                    rules = rules
                ))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get a single group by ID or slug
     */
    suspend fun getGroup(context: Context, identifier: String): Result<Group> {
        return try {
            val token = ApiClient.getToken(context)
            val response = client.get("$BASE_URL/groups/$identifier") {
                token?.let { header("Authorization", "Bearer $it") }
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get user's groups (My Groups)
     */
    suspend fun getMyGroups(
        context: Context,
        page: Int = 1,
        limit: Int = 20
    ): Result<GroupsResponse> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/groups/my") {
                header("Authorization", "Bearer $token")
                parameter("page", page)
                parameter("limit", limit)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Discover public groups
     */
    suspend fun discoverGroups(
        context: Context,
        search: String? = null,
        category: String? = null,
        page: Int = 1,
        limit: Int = 20
    ): Result<GroupsResponse> {
        return try {
            val token = ApiClient.getToken(context)
            val response = client.get("$BASE_URL/groups/discover") {
                token?.let { header("Authorization", "Bearer $it") }
                search?.let { parameter("search", it) }
                category?.let { parameter("category", it) }
                parameter("page", page)
                parameter("limit", limit)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Search all groups
     */
    suspend fun searchGroups(
        context: Context,
        search: String? = null,
        category: String? = null,
        page: Int = 1,
        limit: Int = 20
    ): Result<GroupsResponse> {
        return try {
            val token = ApiClient.getToken(context)
            val response = client.get("$BASE_URL/groups") {
                token?.let { header("Authorization", "Bearer $it") }
                search?.let { parameter("search", it) }
                category?.let { parameter("category", it) }
                parameter("page", page)
                parameter("limit", limit)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update group
     */
    suspend fun updateGroup(
        context: Context,
        groupId: String,
        name: String? = null,
        description: String? = null,
        privacy: String? = null,
        category: String? = null,
        tags: List<String>? = null,
        rules: List<String>? = null
    ): Result<Group> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.put("$BASE_URL/groups/$groupId") {
                header("Authorization", "Bearer $token")
                setBody(UpdateGroupRequest(
                    name = name,
                    description = description,
                    privacy = privacy,
                    category = category,
                    tags = tags,
                    rules = rules
                ))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete group
     */
    suspend fun deleteGroup(context: Context, groupId: String): Result<Unit> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.delete("$BASE_URL/groups/$groupId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Join a group
     */
    suspend fun joinGroup(context: Context, groupId: String): Result<JoinGroupResponse> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/groups/$groupId/join") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Leave a group
     */
    suspend fun leaveGroup(context: Context, groupId: String): Result<Unit> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/groups/$groupId/leave") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get group members
     */
    suspend fun getGroupMembers(
        context: Context,
        groupId: String,
        role: String? = null,
        search: String? = null,
        page: Int = 1,
        limit: Int = 50
    ): Result<GroupMembersResponse> {
        return try {
            val token = ApiClient.getToken(context)
            val response = client.get("$BASE_URL/groups/$groupId/members") {
                token?.let { header("Authorization", "Bearer $it") }
                role?.let { parameter("role", it) }
                search?.let { parameter("search", it) }
                parameter("page", page)
                parameter("limit", limit)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update member role
     */
    suspend fun updateMemberRole(
        context: Context,
        groupId: String,
        userId: String,
        role: String
    ): Result<GroupMember> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.put("$BASE_URL/groups/$groupId/members/$userId") {
                header("Authorization", "Bearer $token")
                setBody(UpdateMemberRoleRequest(role))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Remove member from group
     */
    suspend fun removeMember(
        context: Context,
        groupId: String,
        userId: String
    ): Result<Unit> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.delete("$BASE_URL/groups/$groupId/members/$userId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get group messages
     */
    suspend fun getGroupMessages(
        context: Context,
        groupId: String,
        limit: Int = 50,
        before: String? = null
    ): Result<List<GroupMessage>> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/groups/$groupId/messages") {
                header("Authorization", "Bearer $token")
                parameter("limit", limit)
                before?.let { parameter("before", it) }
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Send message in group (REST fallback, prefer Socket.IO)
     */
    suspend fun sendGroupMessage(
        context: Context,
        groupId: String,
        content: String,
        contentType: String = "text",
        mediaUrl: String? = null,
        mediaType: String? = null,
        fileName: String? = null,
        fileSize: Int? = null,
        replyToId: String? = null
    ): Result<GroupMessage> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/groups/$groupId/messages") {
                header("Authorization", "Bearer $token")
                setBody(SendGroupMessageRequest(
                    content = content,
                    contentType = contentType,
                    mediaUrl = mediaUrl,
                    mediaType = mediaType,
                    fileName = fileName,
                    fileSize = fileSize,
                    replyToId = replyToId
                ))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get group posts
     */
    suspend fun getGroupPosts(
        context: Context,
        groupId: String,
        page: Int = 1,
        limit: Int = 20,
        pinnedFirst: Boolean = true
    ): Result<GroupPostsResponse> {
        return try {
            val token = ApiClient.getToken(context)
            val response = client.get("$BASE_URL/groups/$groupId/posts") {
                token?.let { header("Authorization", "Bearer $it") }
                parameter("page", page)
                parameter("limit", limit)
                parameter("pinnedFirst", pinnedFirst)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create post in group
     */
    suspend fun createGroupPost(
        context: Context,
        groupId: String,
        content: String,
        mediaUrls: List<String> = emptyList(),
        mediaType: String? = null
    ): Result<GroupPost> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/groups/$groupId/posts") {
                header("Authorization", "Bearer $token")
                setBody(CreateGroupPostRequest(content, mediaUrls, mediaType))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get group categories
     */
    suspend fun getCategories(context: Context): Result<GroupCategoriesResponse> {
        return try {
            val response = client.get("$BASE_URL/groups/categories")
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get pending invites
     */
    suspend fun getPendingInvites(context: Context): Result<GroupInvitesResponse> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/groups/invites/pending") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Upload group icon
     */
    suspend fun uploadGroupIcon(
        context: Context,
        groupId: String,
        imageBytes: ByteArray,
        filename: String = "icon.jpg"
    ): Result<Group> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/groups/$groupId/upload/icon") {
                header("Authorization", "Bearer $token")
                setBody(MultiPartFormDataContent(formData {
                    append(
                        "icon",
                        imageBytes,
                        Headers.build {
                            append(HttpHeaders.ContentType, "image/jpeg")
                            append(HttpHeaders.ContentDisposition, "filename=$filename")
                        }
                    )
                }))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Upload group cover
     */
    suspend fun uploadGroupCover(
        context: Context,
        groupId: String,
        imageBytes: ByteArray,
        filename: String = "cover.jpg"
    ): Result<Group> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/groups/$groupId/upload/cover") {
                header("Authorization", "Bearer $token")
                setBody(MultiPartFormDataContent(formData {
                    append(
                        "cover",
                        imageBytes,
                        Headers.build {
                            append(HttpHeaders.ContentType, "image/jpeg")
                            append(HttpHeaders.ContentDisposition, "filename=$filename")
                        }
                    )
                }))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== Circles API ====================
    
    /**
     * Discover circles
     */
    suspend fun discoverCircles(
        context: Context,
        category: String? = null,
        campus: String? = null,
        search: String? = null,
        page: Int = 1,
        limit: Int = 20
    ): Result<CirclesResponse> {
        return try {
            val token = ApiClient.getToken(context)
            val response = client.get("$BASE_URL/circles/discover") {
                token?.let { header("Authorization", "Bearer $it") }
                category?.let { parameter("category", it) }
                campus?.let { parameter("campus", it) }
                search?.let { parameter("search", it) }
                parameter("page", page)
                parameter("limit", limit)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get user's circles
     */
    suspend fun getMyCircles(context: Context): Result<CirclesResponse> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/circles/my/all") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get circle by slug
     */
    suspend fun getCircle(context: Context, slug: String): Result<Circle> {
        return try {
            val token = ApiClient.getToken(context)
            val response = client.get("$BASE_URL/circles/$slug") {
                token?.let { header("Authorization", "Bearer $it") }
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Join a circle
     */
    suspend fun joinCircle(context: Context, circleId: String): Result<JoinCircleResponse> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/circles/$circleId/join") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Leave a circle
     */
    suspend fun leaveCircle(context: Context, circleId: String): Result<Unit> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/circles/$circleId/leave") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create a circle
     */
    suspend fun createCircle(
        context: Context,
        name: String,
        description: String? = null,
        category: String? = null,
        campus: String? = null,
        tags: List<String> = emptyList(),
        emoji: String? = null,
        isPrivate: Boolean = false
    ): Result<Circle> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/circles") {
                header("Authorization", "Bearer $token")
                setBody(CreateCircleRequest(
                    name = name,
                    description = description,
                    category = category,
                    campus = campus,
                    tags = tags,
                    emoji = emoji,
                    isPrivate = isPrivate
                ))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get circle members
     */
    suspend fun getCircleMembers(
        context: Context,
        circleId: String,
        page: Int = 1
    ): Result<CircleMembersResponse> {
        return try {
            val token = ApiClient.getToken(context)
            val response = client.get("$BASE_URL/circles/$circleId/members") {
                token?.let { header("Authorization", "Bearer $it") }
                parameter("page", page)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get circle posts
     */
    suspend fun getCirclePosts(
        context: Context,
        circleId: String,
        page: Int = 1
    ): Result<CirclePostsResponse> {
        return try {
            val token = ApiClient.getToken(context)
            val response = client.get("$BASE_URL/circles/$circleId/posts") {
                token?.let { header("Authorization", "Bearer $it") }
                parameter("page", page)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create post in circle
     */
    suspend fun createCirclePost(
        context: Context,
        circleId: String,
        content: String,
        type: String? = null,
        mediaUrls: List<String> = emptyList()
    ): Result<CirclePost> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/circles/$circleId/posts") {
                header("Authorization", "Bearer $token")
                setBody(CreateCirclePostRequest(content, type, mediaUrls))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
