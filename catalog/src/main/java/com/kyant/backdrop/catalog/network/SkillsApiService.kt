package com.kyant.backdrop.catalog.network

import android.content.Context
import android.util.Log
import com.kyant.backdrop.catalog.BuildConfig
import com.kyant.backdrop.catalog.network.models.ApiError
import com.kyant.backdrop.catalog.network.models.MessageResponse
import com.kyant.backdrop.catalog.network.models.SkillPassportResponse
import com.kyant.backdrop.catalog.network.models.SkillEndorseRequest
import com.kyant.backdrop.catalog.network.models.SkillEndorseResponse
import com.kyant.backdrop.catalog.network.models.SkillSwapCompleteRequest
import com.kyant.backdrop.catalog.network.models.SkillSwapCreateRequest
import com.kyant.backdrop.catalog.network.models.SkillSwapRequestResponse
import com.kyant.backdrop.catalog.network.models.SkillSwapRespondRequest
import com.kyant.backdrop.catalog.network.models.SkillSwapSessionResponse
import com.kyant.backdrop.catalog.network.models.SkillSwapStateResponse
import com.kyant.backdrop.catalog.network.models.SkillSwapSuggestionsResponse
import com.kyant.backdrop.catalog.network.models.SkillVerificationLinkRequest
import com.kyant.backdrop.catalog.network.models.SkillVerificationLinkResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object SkillsApiService {
    private val baseUrl = BuildConfig.API_BASE_URL
    private val allowedModes = setOf("LEARN", "TEACH")
    private val allowedActions = setOf("ACCEPT", "DECLINE")

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
        coerceInputValues = true
    }

    private val client = HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
                readTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
                writeTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
            }
        }
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d("SkillsApiService", message)
                }
            }
            level = if (BuildConfig.DEBUG) LogLevel.BODY else LogLevel.NONE
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60000
            connectTimeoutMillis = 20000
            socketTimeoutMillis = 45000
        }
        installVormexAppCheckInterceptor()
        defaultRequest {
            applyVormexClientHeaders()
        }
    }

    private suspend fun authHeader(context: Context): String? {
        return ApiClient.getToken(context)?.let { "Bearer $it" }
    }

    private suspend inline fun <reified T> parseFailure(response: io.ktor.client.statement.HttpResponse): Result<T> {
        return try {
            val error: ApiError = response.body()
            Result.failure(Exception(error.getErrorMessage()))
        } catch (e: Exception) {
            Result.failure(Exception("Request failed (${response.status.value})"))
        }
    }

    suspend fun getSkillPassport(
        context: Context,
        userId: String = "me"
    ): Result<SkillPassportResponse> {
        return try {
            val safeUserId = if (userId == "me") "me" else InputSecurity.identifier(userId, "userId")
            val response = client.get("$baseUrl/skills/passport/$safeUserId") {
                authHeader(context)?.let { header("Authorization", it) }
            }
            if (response.status.isSuccess()) Result.success(response.body()) else parseFailure(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upsertVerificationLink(
        context: Context,
        provider: String,
        username: String,
        profileUrl: String? = null
    ): Result<SkillVerificationLinkResponse> {
        return try {
            val token = authHeader(context) ?: return Result.failure(Exception("Not logged in"))
            val safeProvider = InputSecurity.enumValue(provider, setOf("GITHUB", "LEETCODE", "PORTFOLIO"), "provider").lowercase()
            val safeUsername = InputSecurity.identifier(username, "username")
            val safeProfileUrl = InputSecurity.optionalText(profileUrl, "profileUrl", 500)
            val response = client.post("$baseUrl/skills/verification-links") {
                header("Authorization", token)
                contentType(ContentType.Application.Json)
                setBody(
                    SkillVerificationLinkRequest(
                        provider = safeProvider,
                        username = safeUsername,
                        profileUrl = safeProfileUrl
                    )
                )
            }
            if (response.status.isSuccess()) Result.success(response.body()) else parseFailure(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteVerificationLink(
        context: Context,
        provider: String
    ): Result<MessageResponse> {
        return try {
            val token = authHeader(context) ?: return Result.failure(Exception("Not logged in"))
            val safeProvider = InputSecurity.enumValue(provider, setOf("GITHUB", "LEETCODE", "PORTFOLIO"), "provider").lowercase()
            val response = client.delete("$baseUrl/skills/verification-links/$safeProvider") {
                header("Authorization", token)
            }
            if (response.status.isSuccess()) Result.success(response.body()) else parseFailure(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun endorseSkill(
        context: Context,
        userId: String,
        skillName: String,
        note: String? = null,
        rating: Int? = null
    ): Result<SkillEndorseResponse> {
        return try {
            val token = authHeader(context) ?: return Result.failure(Exception("Not logged in"))
            val safeUserId = InputSecurity.identifier(userId, "userId")
            val safeSkillName = InputSecurity.text(skillName, "skillName", 80)
            val safeNote = InputSecurity.optionalText(note, "note", 240)
            val safeRating = rating?.let { InputSecurity.boundedInt(it, "rating", 1, 5) }
            val response = client.post("$baseUrl/skills/$safeUserId/endorse") {
                header("Authorization", token)
                contentType(ContentType.Application.Json)
                setBody(SkillEndorseRequest(safeSkillName, safeNote, safeRating))
            }
            if (response.status.isSuccess()) Result.success(response.body()) else parseFailure(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSkillSwapSuggestions(
        context: Context,
        mode: String = "learn",
        skill: String? = null
    ): Result<SkillSwapSuggestionsResponse> {
        return try {
            val token = authHeader(context) ?: return Result.failure(Exception("Not logged in"))
            val safeMode = InputSecurity.enumValue(mode, allowedModes, "mode").lowercase()
            val safeSkill = InputSecurity.optionalText(skill, "skill", 80)
            val response = client.get("$baseUrl/skill-swap/suggestions") {
                header("Authorization", token)
                parameter("mode", safeMode)
                safeSkill?.let { parameter("skill", it) }
            }
            if (response.status.isSuccess()) Result.success(response.body()) else parseFailure(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSkillSwapState(context: Context): Result<SkillSwapStateResponse> {
        return try {
            val token = authHeader(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$baseUrl/skill-swap/requests") {
                header("Authorization", token)
            }
            if (response.status.isSuccess()) Result.success(response.body()) else parseFailure(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createSkillSwapRequest(
        context: Context,
        request: SkillSwapCreateRequest
    ): Result<SkillSwapRequestResponse> {
        return try {
            val token = authHeader(context) ?: return Result.failure(Exception("Not logged in"))
            val safeRequest = request.copy(
                recipientId = InputSecurity.identifier(request.recipientId, "recipientId"),
                skill = InputSecurity.text(request.skill, "skill", 80),
                mode = InputSecurity.enumValue(request.mode, allowedModes, "mode").lowercase(),
                message = InputSecurity.optionalText(request.message, "message", 500),
                requesterGoal = InputSecurity.optionalText(request.requesterGoal, "requesterGoal", 240),
                sessionLengthMinutes = InputSecurity.boundedInt(request.sessionLengthMinutes, "sessionLengthMinutes", 5, 240),
                scheduledFor = InputSecurity.optionalText(request.scheduledFor, "scheduledFor", 80)
            )
            val response = client.post("$baseUrl/skill-swap/requests") {
                header("Authorization", token)
                contentType(ContentType.Application.Json)
                setBody(safeRequest)
            }
            if (response.status.isSuccess()) Result.success(response.body()) else parseFailure(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun respondToSkillSwapRequest(
        context: Context,
        requestId: String,
        action: String
    ): Result<SkillSwapRequestResponse> {
        return try {
            val token = authHeader(context) ?: return Result.failure(Exception("Not logged in"))
            val safeRequestId = InputSecurity.identifier(requestId, "requestId")
            val safeAction = InputSecurity.enumValue(action, allowedActions, "action").lowercase()
            val response = client.post("$baseUrl/skill-swap/requests/$safeRequestId/respond") {
                header("Authorization", token)
                contentType(ContentType.Application.Json)
                setBody(SkillSwapRespondRequest(action = safeAction))
            }
            if (response.status.isSuccess()) Result.success(response.body()) else parseFailure(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun completeSkillSwapSession(
        context: Context,
        sessionId: String,
        rating: Int,
        note: String?,
        endorseSkill: Boolean = true
    ): Result<SkillSwapSessionResponse> {
        return try {
            val token = authHeader(context) ?: return Result.failure(Exception("Not logged in"))
            val safeSessionId = InputSecurity.identifier(sessionId, "sessionId")
            val safeRating = InputSecurity.boundedInt(rating, "rating", 1, 5)
            val safeNote = InputSecurity.optionalText(note, "note", 500)
            val response = client.post("$baseUrl/skill-swap/sessions/$safeSessionId/complete") {
                header("Authorization", token)
                contentType(ContentType.Application.Json)
                setBody(
                    SkillSwapCompleteRequest(
                        rating = safeRating,
                        note = safeNote,
                        endorseSkill = endorseSkill
                    )
                )
            }
            if (response.status.isSuccess()) Result.success(response.body()) else parseFailure(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
