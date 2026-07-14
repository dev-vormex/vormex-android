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
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Posts API Service - Handles all post-related API calls
 */
object PostsApiService {

    private val BASE_URL = BuildConfig.API_BASE_URL
    private const val DEFAULT_REQUEST_TIMEOUT_MILLIS = 60_000L
    private const val DEFAULT_CONNECT_TIMEOUT_MILLIS = 20_000L
    private const val DEFAULT_SOCKET_TIMEOUT_MILLIS = 60_000L
    private const val UPLOAD_REQUEST_TIMEOUT_MILLIS = 300_000L
    private const val UPLOAD_CONNECT_TIMEOUT_MILLIS = 30_000L
    private const val UPLOAD_SOCKET_TIMEOUT_MILLIS = 300_000L

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        coerceInputValues = true
    }

    private fun FormBuilder.appendJsonStringList(fieldName: String, values: List<String>) {
        if (values.isNotEmpty()) {
            append(fieldName, json.encodeToString(ListSerializer(String.serializer()), values))
        }
    }

    private val allowedVisibilities = setOf("PUBLIC", "CONNECTIONS", "PRIVATE")
    private fun safeVisibility(value: String): String =
        InputSecurity.enumValue(value, allowedVisibilities, "visibility")

    private val client = createHttpClient(
        requestTimeoutMillis = DEFAULT_REQUEST_TIMEOUT_MILLIS,
        connectTimeoutMillis = DEFAULT_CONNECT_TIMEOUT_MILLIS,
        socketTimeoutMillis = DEFAULT_SOCKET_TIMEOUT_MILLIS,
        readTimeoutMillis = DEFAULT_SOCKET_TIMEOUT_MILLIS,
        writeTimeoutMillis = DEFAULT_SOCKET_TIMEOUT_MILLIS
    )

    private val uploadClient = createHttpClient(
        requestTimeoutMillis = UPLOAD_REQUEST_TIMEOUT_MILLIS,
        connectTimeoutMillis = UPLOAD_CONNECT_TIMEOUT_MILLIS,
        socketTimeoutMillis = UPLOAD_SOCKET_TIMEOUT_MILLIS,
        readTimeoutMillis = UPLOAD_SOCKET_TIMEOUT_MILLIS,
        writeTimeoutMillis = UPLOAD_SOCKET_TIMEOUT_MILLIS
    )

    private fun createHttpClient(
        requestTimeoutMillis: Long,
        connectTimeoutMillis: Long,
        socketTimeoutMillis: Long,
        readTimeoutMillis: Long,
        writeTimeoutMillis: Long
    ) = HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(connectTimeoutMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
                readTimeout(readTimeoutMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
                writeTimeout(writeTimeoutMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
            }
        }
        install(ContentNegotiation) {
            json(json)
        }
        if (BuildConfig.DEBUG) {
            install(Logging) {
                logger = Logger.ANDROID
                level = LogLevel.HEADERS
            }
        }
        install(HttpTimeout) {
            this.requestTimeoutMillis = requestTimeoutMillis
            this.connectTimeoutMillis = connectTimeoutMillis
            this.socketTimeoutMillis = socketTimeoutMillis
        }
        installVormexAppCheckInterceptor()
        defaultRequest {
            applyVormexClientHeaders()
            contentType(ContentType.Application.Json)
        }
    }

    // Helper to safely parse error response
    private suspend fun parseErrorMessage(response: HttpResponse): String {
        return try {
            val error: ApiError = response.body()
            error.getErrorMessage()
        } catch (e: Exception) {
            try {
                response.bodyAsText()
            } catch (_: Exception) {
                "Request failed with status ${response.status.value}"
            }
        }
    }

    // ==================== Feed APIs ====================

    /**
     * Get feed with cursor-based pagination
     */
    suspend fun getFeed(
        context: Context,
        cursor: String? = null,
        limit: Int = 20
    ): Result<FullFeedResponse> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeLimit = InputSecurity.boundedInt(limit, "limit", 1, 50)
            val safeCursor = InputSecurity.optionalPaginationCursor(cursor, "cursor")
            val response = client.get("$BASE_URL/posts/feed") {
                header("Authorization", "Bearer $token")
                parameter("limit", safeLimit)
                safeCursor?.let { parameter("cursor", it) }
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
     * Get a single post by ID
     */
    suspend fun getPost(context: Context, postId: String): Result<FullPost> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safePostId = InputSecurity.identifier(postId, "postId")
            val response = client.get("$BASE_URL/posts/$safePostId") {
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

    // ==================== Create Post APIs ====================

    /**
     * Create a text post
     */
    suspend fun createTextPost(
        context: Context,
        content: String,
        visibility: String = "PUBLIC",
        mentions: List<String> = emptyList(),
        defaultVideoId: String? = null,
        collaboratorIds: List<String> = emptyList()
    ): Result<FullPost> {
        return createPost(context, "TEXT", content, visibility, mentions, defaultVideoId, collaboratorIds)
    }

    /**
     * Create an image post with one or more images
     */
    suspend fun createImagePost(
        context: Context,
        content: String? = null,
        visibility: String = "PUBLIC",
        images: List<Pair<ByteArray, String>>, // (bytes, filename)
        mentions: List<String> = emptyList(),
        defaultVideoId: String? = null,
        collaboratorIds: List<String> = emptyList()
    ): Result<FullPost> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeContent = InputSecurity.optionalText(content, "content", 2_000)
            val safeVisibility = safeVisibility(visibility)
            val safeMentions = InputSecurity.sanitizeList(mentions, "mentions", 30, 80)
            val safeCollaboratorIds = InputSecurity.sanitizeList(collaboratorIds, "collaboratorIds", 30, 80)
            val safeDefaultVideoId = InputSecurity.optionalIdentifier(defaultVideoId, "defaultVideoId")
            val safeImages = images.mapIndexed { index, (bytes, filename) ->
                InputSecurity.uploadBytes(bytes, "image", 10 * 1024 * 1024) to
                    InputSecurity.fileName(filename, "image$index.jpg")
            }
            val response = uploadClient.post("$BASE_URL/posts") {
                header("Authorization", "Bearer $token")
                setBody(MultiPartFormDataContent(formData {
                    append("type", "IMAGE")
                    append("visibility", safeVisibility)
                    safeContent?.let { append("content", it) }
                    appendJsonStringList("mentions", safeMentions)
                    appendJsonStringList("collaboratorIds", safeCollaboratorIds)
                    safeDefaultVideoId?.let { append("defaultVideoId", it) }
                    safeImages.forEachIndexed { index, (bytes, filename) ->
                        append(
                            "media",
                            bytes,
                            Headers.build {
                                append(HttpHeaders.ContentType, "image/jpeg")
                                append(HttpHeaders.ContentDisposition, "filename=$filename")
                            }
                        )
                    }
                }))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val errorMsg = parseErrorMessage(response)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create a video post
     */
    suspend fun createVideoPost(
        context: Context,
        content: String? = null,
        visibility: String = "PUBLIC",
        videoBytes: ByteArray,
        videoFilename: String = "video.mp4",
        mentions: List<String> = emptyList(),
        defaultVideoId: String? = null,
        collaboratorIds: List<String> = emptyList()
    ): Result<FullPost> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeContent = InputSecurity.optionalText(content, "content", 2_000)
            val safeVisibility = safeVisibility(visibility)
            val safeVideoBytes = InputSecurity.uploadBytes(videoBytes, "video", 100 * 1024 * 1024)
            val safeVideoFilename = InputSecurity.fileName(videoFilename, "video.mp4")
            val safeMentions = InputSecurity.sanitizeList(mentions, "mentions", 30, 80)
            val safeCollaboratorIds = InputSecurity.sanitizeList(collaboratorIds, "collaboratorIds", 30, 80)
            val safeDefaultVideoId = InputSecurity.optionalIdentifier(defaultVideoId, "defaultVideoId")
            val response = uploadClient.post("$BASE_URL/posts") {
                header("Authorization", "Bearer $token")
                setBody(MultiPartFormDataContent(formData {
                    append("type", "VIDEO")
                    append("visibility", safeVisibility)
                    safeContent?.let { append("content", it) }
                    appendJsonStringList("mentions", safeMentions)
                    appendJsonStringList("collaboratorIds", safeCollaboratorIds)
                    safeDefaultVideoId?.let { append("defaultVideoId", it) }
                    append(
                        "video",
                        safeVideoBytes,
                        Headers.build {
                            append(HttpHeaders.ContentType, "video/mp4")
                            append(HttpHeaders.ContentDisposition, "filename=$safeVideoFilename")
                        }
                    )
                }))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val errorMsg = parseErrorMessage(response)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create a link post
     */
    suspend fun createLinkPost(
        context: Context,
        linkUrl: String,
        content: String? = null,
        visibility: String = "PUBLIC",
        mentions: List<String> = emptyList(),
        defaultVideoId: String? = null,
        collaboratorIds: List<String> = emptyList()
    ): Result<FullPost> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeLinkUrl = InputSecurity.url(linkUrl, "linkUrl")
            val safeContent = InputSecurity.optionalText(content, "content", 2_000)
            val safeVisibility = safeVisibility(visibility)
            val safeMentions = InputSecurity.sanitizeList(mentions, "mentions", 30, 80)
            val safeCollaboratorIds = InputSecurity.sanitizeList(collaboratorIds, "collaboratorIds", 30, 80)
            val safeDefaultVideoId = InputSecurity.optionalIdentifier(defaultVideoId, "defaultVideoId")
            val response = client.post("$BASE_URL/posts") {
                header("Authorization", "Bearer $token")
                setBody(MultiPartFormDataContent(formData {
                    append("type", "LINK")
                    append("visibility", safeVisibility)
                    append("linkUrl", safeLinkUrl)
                    safeContent?.let { append("content", it) }
                    appendJsonStringList("mentions", safeMentions)
                    appendJsonStringList("collaboratorIds", safeCollaboratorIds)
                    safeDefaultVideoId?.let { append("defaultVideoId", it) }
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
     * Create a poll post
     */
    suspend fun createPollPost(
        context: Context,
        pollOptions: List<String>,
        pollDurationHours: Int = 24,
        content: String? = null,
        visibility: String = "PUBLIC",
        showResultsBeforeVote: Boolean = false,
        mentions: List<String> = emptyList(),
        defaultVideoId: String? = null,
        collaboratorIds: List<String> = emptyList()
    ): Result<FullPost> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeOptions = InputSecurity.sanitizeList(pollOptions, "pollOptions", 6, 120)
            if (safeOptions.size < 2) return Result.failure(Exception("At least 2 poll options are required"))
            val safeDuration = InputSecurity.boundedInt(pollDurationHours, "pollDurationHours", 1, 24 * 14)
            val safeContent = InputSecurity.optionalText(content, "content", 2_000)
            val safeVisibility = safeVisibility(visibility)
            val safeMentions = InputSecurity.sanitizeList(mentions, "mentions", 30, 80)
            val safeCollaboratorIds = InputSecurity.sanitizeList(collaboratorIds, "collaboratorIds", 30, 80)
            val safeDefaultVideoId = InputSecurity.optionalIdentifier(defaultVideoId, "defaultVideoId")
            val response = client.post("$BASE_URL/posts") {
                header("Authorization", "Bearer $token")
                setBody(MultiPartFormDataContent(formData {
                    append("type", "POLL")
                    append("visibility", safeVisibility)
                    append("pollDuration", safeDuration.toString())
                    append("showResultsBeforeVote", showResultsBeforeVote.toString())
                    safeContent?.let { append("content", it) }
                    safeOptions.forEach { option ->
                        append("pollOptions", option)
                    }
                    appendJsonStringList("mentions", safeMentions)
                    appendJsonStringList("collaboratorIds", safeCollaboratorIds)
                    safeDefaultVideoId?.let { append("defaultVideoId", it) }
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
     * Create an article post
     */
    suspend fun createArticlePost(
        context: Context,
        articleTitle: String,
        content: String? = null,
        visibility: String = "PUBLIC",
        coverImage: Pair<ByteArray, String>? = null,
        articleTags: List<String> = emptyList(),
        mentions: List<String> = emptyList(),
        defaultVideoId: String? = null,
        collaboratorIds: List<String> = emptyList()
    ): Result<FullPost> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeArticleTitle = InputSecurity.text(articleTitle, "articleTitle", 140)
            val safeContent = InputSecurity.optionalText(content, "content", 8_000)
            val safeVisibility = safeVisibility(visibility)
            val safeTags = InputSecurity.sanitizeList(articleTags, "articleTags", 12, 40)
            val safeMentions = InputSecurity.sanitizeList(mentions, "mentions", 30, 80)
            val safeCollaboratorIds = InputSecurity.sanitizeList(collaboratorIds, "collaboratorIds", 30, 80)
            val safeDefaultVideoId = InputSecurity.optionalIdentifier(defaultVideoId, "defaultVideoId")
            val safeCoverImage = coverImage?.let { (bytes, filename) ->
                InputSecurity.uploadBytes(bytes, "coverImage", 10 * 1024 * 1024) to
                    InputSecurity.fileName(filename, "cover.jpg")
            }
            val response = (if (safeCoverImage != null) uploadClient else client).post("$BASE_URL/posts") {
                header("Authorization", "Bearer $token")
                setBody(MultiPartFormDataContent(formData {
                    append("type", "ARTICLE")
                    append("visibility", safeVisibility)
                    append("articleTitle", safeArticleTitle)
                    safeContent?.let { append("content", it) }
                    safeTags.forEach { tag ->
                        append("articleTags", tag)
                    }
                    appendJsonStringList("mentions", safeMentions)
                    appendJsonStringList("collaboratorIds", safeCollaboratorIds)
                    safeDefaultVideoId?.let { append("defaultVideoId", it) }
                    safeCoverImage?.let { (bytes, filename) ->
                        append(
                            "articleCoverImage",
                            bytes,
                            Headers.build {
                                append(HttpHeaders.ContentType, "image/jpeg")
                                append(HttpHeaders.ContentDisposition, "filename=$filename")
                            }
                        )
                    }
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
     * Create a celebration post
     */
    suspend fun createCelebrationPost(
        context: Context,
        celebrationType: String,
        content: String? = null,
        visibility: String = "PUBLIC",
        mentions: List<String> = emptyList(),
        celebrationGif: Pair<ByteArray, String>? = null,
        defaultVideoId: String? = null,
        collaboratorIds: List<String> = emptyList()
    ): Result<FullPost> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeCelebrationType = InputSecurity.identifier(celebrationType, "celebrationType")
            val safeContent = InputSecurity.optionalText(content, "content", 2_000)
            val safeVisibility = safeVisibility(visibility)
            val safeMentions = InputSecurity.sanitizeList(mentions, "mentions", 30, 80)
            val safeCollaboratorIds = InputSecurity.sanitizeList(collaboratorIds, "collaboratorIds", 30, 80)
            val safeDefaultVideoId = InputSecurity.optionalIdentifier(defaultVideoId, "defaultVideoId")
            val safeCelebrationGif = celebrationGif?.let { (bytes, filename) ->
                InputSecurity.uploadBytes(bytes, "celebrationGif", 10 * 1024 * 1024) to
                    InputSecurity.fileName(filename, "celebration.gif")
            }
            val response = (if (safeCelebrationGif != null) uploadClient else client).post("$BASE_URL/posts") {
                header("Authorization", "Bearer $token")
                setBody(MultiPartFormDataContent(formData {
                    append("type", "CELEBRATION")
                    append("visibility", safeVisibility)
                    append("celebrationType", safeCelebrationType)
                    safeContent?.let { append("content", it) }
                    appendJsonStringList("mentions", safeMentions)
                    appendJsonStringList("collaboratorIds", safeCollaboratorIds)
                    safeDefaultVideoId?.let { append("defaultVideoId", it) }
                    safeCelebrationGif?.let { (bytes, filename) ->
                        val mime = when {
                            filename.endsWith(".gif", ignoreCase = true) -> "image/gif"
                            filename.endsWith(".webp", ignoreCase = true) -> "image/webp"
                            filename.endsWith(".png", ignoreCase = true) -> "image/png"
                            else -> "image/jpeg"
                        }
                        append(
                            "image",
                            bytes,
                            Headers.build {
                                append(HttpHeaders.ContentType, mime)
                                append(HttpHeaders.ContentDisposition, "filename=$filename")
                            }
                        )
                    }
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
     * Generic create post for simple text posts
     */
    private suspend fun createPost(
        context: Context,
        type: String,
        content: String,
        visibility: String,
        mentions: List<String>,
        defaultVideoId: String? = null,
        collaboratorIds: List<String> = emptyList()
    ): Result<FullPost> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeContent = InputSecurity.text(content, "content", 2_000)
            val safeVisibility = safeVisibility(visibility)
            val safeMentions = InputSecurity.sanitizeList(mentions, "mentions", 30, 80)
            val safeCollaboratorIds = InputSecurity.sanitizeList(collaboratorIds, "collaboratorIds", 30, 80)
            val safeDefaultVideoId = InputSecurity.optionalIdentifier(defaultVideoId, "defaultVideoId")
            val response = client.post("$BASE_URL/posts") {
                header("Authorization", "Bearer $token")
                setBody(MultiPartFormDataContent(formData {
                    append("type", type)
                    append("visibility", safeVisibility)
                    append("content", safeContent)
                    appendJsonStringList("mentions", safeMentions)
                    appendJsonStringList("collaboratorIds", safeCollaboratorIds)
                    safeDefaultVideoId?.let { append("defaultVideoId", it) }
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

    // ==================== Update/Delete Post APIs ====================

    /**
     * Update a post (only content and visibility are editable)
     */
    suspend fun updatePost(
        context: Context,
        postId: String,
        content: String? = null,
        visibility: String? = null
    ): Result<FullPost> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safePostId = InputSecurity.identifier(postId, "postId")
            val safeContent = InputSecurity.optionalText(content, "content", 2_000)
            val safeVisibility = visibility?.let { safeVisibility(it) }
            val response = client.put("$BASE_URL/posts/$safePostId") {
                header("Authorization", "Bearer $token")
                setBody(UpdatePostRequest(safeContent, safeVisibility))
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
     * Delete a post
     */
    suspend fun deletePost(context: Context, postId: String): Result<DeletePostResponse> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safePostId = InputSecurity.identifier(postId, "postId")
            val response = client.delete("$BASE_URL/posts/$safePostId") {
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

    // ==================== Engagement APIs ====================

    /**
     * Toggle like on a post
     */
    suspend fun toggleLike(context: Context, postId: String): Result<ReactionResponse> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safePostId = InputSecurity.identifier(postId, "postId")
            val response = client.post("$BASE_URL/posts/$safePostId/like") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to toggle like"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get list of users who liked a post
     */
    suspend fun getLikes(context: Context, postId: String): Result<LikesListResponse> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safePostId = InputSecurity.identifier(postId, "postId")
            val response = client.get("$BASE_URL/posts/$safePostId/likes") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to get likes"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Vote on a poll
     */
    suspend fun votePoll(context: Context, postId: String, optionId: String): Result<PollVoteResponse> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safePostId = InputSecurity.identifier(postId, "postId")
            val safeOptionId = InputSecurity.identifier(optionId, "optionId")
            val response = client.post("$BASE_URL/posts/$safePostId/poll/vote") {
                header("Authorization", "Bearer $token")
                setBody(mapOf("optionId" to safeOptionId))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to vote on poll"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Share a post
     */
    suspend fun sharePost(context: Context, postId: String, userId: String? = null): Result<ShareResponse> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safePostId = InputSecurity.identifier(postId, "postId")
            val safeUserId = InputSecurity.optionalIdentifier(userId, "userId")
            val response = client.post("$BASE_URL/posts/$safePostId/share") {
                header("Authorization", "Bearer $token")
                safeUserId?.let { setBody(mapOf("userId" to it)) }
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to share post"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Save APIs ====================

    /**
     * Toggle save on a post
     */
    suspend fun toggleSave(context: Context, postId: String): Result<SaveResponse> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safePostId = InputSecurity.identifier(postId, "postId")
            val response = client.post("$BASE_URL/saved/$safePostId/toggle") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to toggle save"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get saved posts
     */
    suspend fun getSavedPosts(
        context: Context,
        cursor: String? = null,
        limit: Int = 20
    ): Result<FullFeedResponse> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeLimit = InputSecurity.boundedInt(limit, "limit", 1, 50)
            val safeCursor = InputSecurity.optionalPaginationCursor(cursor, "cursor")
            val response = client.get("$BASE_URL/saved") {
                header("Authorization", "Bearer $token")
                parameter("limit", safeLimit)
                safeCursor?.let { parameter("cursor", it) }
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

    // ==================== Comments APIs ====================

    /**
     * Get comments for a post
     */
    suspend fun getComments(
        context: Context,
        postId: String,
        parentId: String? = null,
        page: Int = 1,
        limit: Int = 20
    ): Result<FullCommentsResponse> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safePostId = InputSecurity.identifier(postId, "postId")
            val safeParentId = InputSecurity.optionalIdentifier(parentId, "parentId")
            val safePage = InputSecurity.boundedInt(page, "page", 1, 5_000)
            val safeLimit = InputSecurity.boundedInt(limit, "limit", 1, 100)
            val response = client.get("$BASE_URL/posts/$safePostId/comments") {
                header("Authorization", "Bearer $token")
                parameter("page", safePage)
                parameter("limit", safeLimit)
                safeParentId?.let { parameter("parentId", it) }
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
     * Create a comment
     */
    suspend fun createComment(
        context: Context,
        postId: String,
        content: String,
        parentId: String? = null,
        mentions: List<String>? = null
    ): Result<FullComment> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safePostId = InputSecurity.identifier(postId, "postId")
            val safeContent = InputSecurity.text(content, "content", 1_000)
            val safeParentId = InputSecurity.optionalIdentifier(parentId, "parentId")
            val safeMentions = mentions?.let { InputSecurity.sanitizeList(it, "mentions", 30, 80) }
            val response = client.post("$BASE_URL/posts/$safePostId/comments") {
                header("Authorization", "Bearer $token")
                setBody(CreateCommentFullRequest(safeContent, safeParentId, safeMentions))
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
     * Toggle like on a comment
     */
    suspend fun toggleCommentLike(
        context: Context,
        postId: String,
        commentId: String
    ): Result<CommentLikeResponse> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safePostId = InputSecurity.identifier(postId, "postId")
            val safeCommentId = InputSecurity.identifier(commentId, "commentId")
            val response = client.post("$BASE_URL/posts/$safePostId/comments/$safeCommentId/like") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to toggle comment like"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a comment
     */
    suspend fun deleteComment(
        context: Context,
        postId: String,
        commentId: String
    ): Result<DeletePostResponse> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safePostId = InputSecurity.identifier(postId, "postId")
            val safeCommentId = InputSecurity.identifier(commentId, "commentId")
            val response = client.delete("$BASE_URL/posts/$safePostId/comments/$safeCommentId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to delete comment"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Mention APIs ====================

    /**
     * Search users for @mention autocomplete
     */
    suspend fun searchMentions(context: Context, query: String, limit: Int = 10): Result<MentionSearchResponse> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeQuery = InputSecurity.text(query, "query", 80)
            val safeLimit = InputSecurity.boundedInt(limit, "limit", 1, 50)
            val response = client.get("$BASE_URL/mentions/search") {
                header("Authorization", "Bearer $token")
                parameter("q", safeQuery)
                parameter("limit", safeLimit)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to search mentions"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Report APIs ====================

    /**
     * Get report reasons
     */
    suspend fun getReportReasons(): Result<ReportReasonsResponse> {
        return try {
            val response = client.get("$BASE_URL/reports/reasons")
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to get report reasons"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Report a post
     */
    suspend fun reportPost(
        context: Context,
        postId: String,
        reason: String,
        description: String? = null,
        blockUser: Boolean = false
    ): Result<ReportResponse> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safePostId = InputSecurity.identifier(postId, "postId")
            val safeReason = InputSecurity.identifier(reason, "reason")
            val safeDescription = InputSecurity.optionalText(description, "description", 1_000)
            val response = client.post("$BASE_URL/reports/post/$safePostId") {
                header("Authorization", "Bearer $token")
                setBody(ReportPostRequest(safeReason, safeDescription, blockUser))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to report post"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
