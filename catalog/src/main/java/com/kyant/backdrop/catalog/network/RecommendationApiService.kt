package com.kyant.backdrop.catalog.network

import android.content.Context
import com.kyant.backdrop.catalog.BuildConfig
import com.kyant.backdrop.catalog.network.models.ApiError
import com.kyant.backdrop.catalog.network.models.CreatePostBoostRequest
import com.kyant.backdrop.catalog.network.models.PostBoostCampaign
import com.kyant.backdrop.catalog.network.models.PostBoostCampaignsResponse
import com.kyant.backdrop.catalog.network.models.PostBoostCredits
import com.kyant.backdrop.catalog.network.models.RecommendationEventsRequest
import com.kyant.backdrop.catalog.network.models.RecommendationEventsResponse
import com.kyant.backdrop.catalog.network.models.RecommendationFeedbackRequest
import com.kyant.backdrop.catalog.network.models.RecommendationFeedbackResponse
import com.kyant.backdrop.catalog.network.models.RecommendationPreferences
import com.kyant.backdrop.catalog.network.models.RecommendationPreferencesPatch
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Recommendation calls are deliberately isolated from rank rendering and never call OpenAI. */
object RecommendationApiService {
    private val baseUrl = BuildConfig.API_BASE_URL.trimEnd('/')
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 30_000
        }
        installVormexAppCheckInterceptor()
        defaultRequest {
            applyVormexClientHeaders()
            contentType(ContentType.Application.Json)
        }
    }

    private suspend fun token(context: Context, expectedUserId: String? = null): String {
        if (expectedUserId != null) {
            check(ApiClient.getCurrentUserId(context) == expectedUserId) {
                "Recommendation telemetry account changed before upload"
            }
        }
        val token = ApiClient.getToken(context) ?: throw IllegalStateException("Not logged in")
        if (expectedUserId != null) {
            check(ApiClient.getCurrentUserId(context) == expectedUserId) {
                "Recommendation telemetry account changed during upload preparation"
            }
        }
        return token
    }

    private suspend inline fun <reified T> decode(response: HttpResponse): T {
        if (response.status.isSuccess()) return response.body()
        val message = runCatching { response.body<ApiError>().getErrorMessage() }
            .getOrElse { runCatching { response.bodyAsText() }.getOrDefault("Request failed") }
        throw IllegalStateException(message)
    }

    suspend fun sendEvents(
        context: Context,
        request: RecommendationEventsRequest,
        expectedUserId: String
    ): Result<RecommendationEventsResponse> = runCatching {
        require(request.events.isNotEmpty() && request.events.size <= 100)
        decode(client.post("$baseUrl/discovery/events") {
            header("Authorization", "Bearer ${token(context, expectedUserId)}")
            setBody(request)
        })
    }

    suspend fun submitFeedback(
        context: Context,
        request: RecommendationFeedbackRequest
    ): Result<RecommendationFeedbackResponse> = runCatching {
        decode(client.post("$baseUrl/discovery/feedback") {
            header("Authorization", "Bearer ${token(context)}")
            setBody(request)
        })
    }

    suspend fun getPreferences(context: Context): Result<RecommendationPreferences> = runCatching {
        decode(client.get("$baseUrl/discovery/preferences") {
            header("Authorization", "Bearer ${token(context)}")
        })
    }

    suspend fun updatePreferences(
        context: Context,
        patch: RecommendationPreferencesPatch
    ): Result<RecommendationPreferences> = runCatching {
        decode(client.patch("$baseUrl/discovery/preferences") {
            header("Authorization", "Bearer ${token(context)}")
            setBody(patch)
        })
    }

    suspend fun getPostBoostCredits(context: Context): Result<PostBoostCredits> = runCatching {
        decode(client.get("$baseUrl/premium/post-boosts/credits") {
            header("Authorization", "Bearer ${token(context)}")
        })
    }

    suspend fun listPostBoosts(context: Context): Result<PostBoostCampaignsResponse> = runCatching {
        decode(client.get("$baseUrl/premium/post-boosts") {
            header("Authorization", "Bearer ${token(context)}")
        })
    }

    suspend fun createPostBoost(context: Context, postId: String): Result<PostBoostCampaign> = runCatching {
        decode(client.post("$baseUrl/premium/post-boosts") {
            header("Authorization", "Bearer ${token(context)}")
            setBody(CreatePostBoostRequest(postId))
        })
    }

    suspend fun cancelPostBoost(context: Context, campaignId: String): Result<PostBoostCampaign> = runCatching {
        decode(client.post("$baseUrl/premium/post-boosts/$campaignId/cancel") {
            header("Authorization", "Bearer ${token(context)}")
        })
    }
}
