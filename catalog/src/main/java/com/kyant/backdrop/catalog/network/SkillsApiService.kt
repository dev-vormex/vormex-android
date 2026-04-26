package com.kyant.backdrop.catalog.network

import android.content.Context
import android.util.Log
import com.kyant.backdrop.catalog.BuildConfig
import com.kyant.backdrop.catalog.network.models.ApiError
import com.kyant.backdrop.catalog.network.models.SkillPassportResponse
import com.kyant.backdrop.catalog.network.models.SkillSwapCompleteRequest
import com.kyant.backdrop.catalog.network.models.SkillSwapCreateRequest
import com.kyant.backdrop.catalog.network.models.SkillSwapRequestResponse
import com.kyant.backdrop.catalog.network.models.SkillSwapRespondRequest
import com.kyant.backdrop.catalog.network.models.SkillSwapSessionResponse
import com.kyant.backdrop.catalog.network.models.SkillSwapStateResponse
import com.kyant.backdrop.catalog.network.models.SkillSwapSuggestionsResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
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
            val response = client.get("$baseUrl/skills/passport/$userId") {
                authHeader(context)?.let { header("Authorization", it) }
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
            val response = client.get("$baseUrl/skill-swap/suggestions") {
                header("Authorization", token)
                parameter("mode", mode)
                skill?.takeIf { it.isNotBlank() }?.let { parameter("skill", it) }
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
            val response = client.post("$baseUrl/skill-swap/requests") {
                header("Authorization", token)
                contentType(ContentType.Application.Json)
                setBody(request)
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
            val response = client.post("$baseUrl/skill-swap/requests/$requestId/respond") {
                header("Authorization", token)
                contentType(ContentType.Application.Json)
                setBody(SkillSwapRespondRequest(action = action))
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
            val response = client.post("$baseUrl/skill-swap/sessions/$sessionId/complete") {
                header("Authorization", token)
                contentType(ContentType.Application.Json)
                setBody(
                    SkillSwapCompleteRequest(
                        rating = rating,
                        note = note,
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
