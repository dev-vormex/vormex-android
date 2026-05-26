package com.kyant.backdrop.catalog.network

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kyant.backdrop.catalog.BuildConfig
import com.kyant.backdrop.catalog.network.models.AgentApproveActionResponse
import com.kyant.backdrop.catalog.network.models.AgentGoal
import com.kyant.backdrop.catalog.network.models.AgentGoalDeleteResponse
import com.kyant.backdrop.catalog.network.models.AgentGoalUpsertRequest
import com.kyant.backdrop.catalog.network.models.AgentGoalUpsertResponse
import com.kyant.backdrop.catalog.network.models.AgentGoalsResponse
import com.kyant.backdrop.catalog.network.models.AgentPendingAction
import com.kyant.backdrop.catalog.network.models.AgentPendingActionsResponse
import com.kyant.backdrop.catalog.network.models.AgentRejectActionResponse
import com.kyant.backdrop.catalog.network.models.AgentSessionBootstrapResponse
import com.kyant.backdrop.catalog.network.models.AgentSessionRequest
import com.kyant.backdrop.catalog.network.models.AgentTurnRequest
import com.kyant.backdrop.catalog.network.models.AgentTurnResponse
import com.kyant.backdrop.catalog.network.models.AgentVoiceTurnResponse
import com.kyant.backdrop.catalog.network.models.ApiError
import com.kyant.backdrop.catalog.network.models.ConversationStartersRequest
import com.kyant.backdrop.catalog.network.models.ConversationStartersResponse
import com.kyant.backdrop.catalog.network.models.SmartRepliesRequest
import com.kyant.backdrop.catalog.network.models.SmartRepliesResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.agentDataStore: DataStore<Preferences> by preferencesDataStore(name = "vormex_agent_prefs")

object AgentApiService {
    private val baseUrl = BuildConfig.API_BASE_URL
    private val agentSessionKey = stringPreferencesKey("agent_session_id")
    private val agentAutoRunKey = booleanPreferencesKey("agent_auto_run_enabled")
    private val agentAutonomyModeKey = stringPreferencesKey("agent_autonomy_mode")

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
                connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                writeTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            }
        }
        install(ContentNegotiation) {
            json(json)
        }
        if (BuildConfig.DEBUG) {
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Log.d("AgentApiService", message)
                    }
                }
                level = LogLevel.BODY
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 180000
            connectTimeoutMillis = 30000
            socketTimeoutMillis = 120000
        }
        installVormexAppCheckInterceptor()
        defaultRequest {
            applyVormexClientHeaders()
        }
    }

    private suspend fun authToken(context: Context): String? = ApiClient.getToken(context)

    private suspend fun parseError(response: HttpResponse): String {
        return try {
            val error: ApiError = response.body()
            error.getErrorMessage()
        } catch (_: Exception) {
            response.bodyAsText().ifBlank { "Request failed with status ${response.status.value}" }
        }
    }

    suspend fun getStoredSessionId(context: Context): String? {
        return context.agentDataStore.data.first()[agentSessionKey]
    }

    suspend fun getStoredAutoRunEnabled(context: Context): Boolean {
        return getStoredAutonomyMode(context) == "power"
    }

    suspend fun setStoredAutoRunEnabled(context: Context, enabled: Boolean) {
        setStoredAutonomyMode(context, if (enabled) "power" else "approval")
    }

    suspend fun getStoredAutonomyMode(context: Context): String {
        val prefs = context.agentDataStore.data.first()
        return when (prefs[agentAutonomyModeKey]?.lowercase()) {
            "power" -> "power"
            "approval" -> "approval"
            else -> if (prefs[agentAutoRunKey] == true) "power" else "approval"
        }
    }

    suspend fun setStoredAutonomyMode(context: Context, mode: String) {
        val safeMode = if (mode.equals("power", ignoreCase = true)) "power" else "approval"
        context.agentDataStore.edit { prefs ->
            prefs[agentAutoRunKey] = safeMode == "power"
            prefs[agentAutonomyModeKey] = safeMode
        }
    }

    suspend fun clearSession(context: Context) {
        context.agentDataStore.edit { prefs ->
            prefs.remove(agentSessionKey)
        }
    }

    private suspend fun saveSessionId(context: Context, sessionId: String) {
        context.agentDataStore.edit { prefs ->
            prefs[agentSessionKey] = sessionId
        }
    }

    suspend fun bootstrapSession(
        context: Context,
        mode: String = "text",
        surface: String = "global",
        allowAutonomousActions: Boolean = false,
        autonomyMode: String = if (allowAutonomousActions) "power" else "approval"
    ): Result<AgentSessionBootstrapResponse> {
        return try {
            val token = authToken(context) ?: return Result.failure(Exception("Not logged in"))
            val storedSessionId = InputSecurity.optionalIdentifier(getStoredSessionId(context), "sessionId")
            val safeMode = InputSecurity.enumValue(mode, setOf("TEXT", "VOICE"), "mode").lowercase()
            val safeSurface = InputSecurity.identifier(surface, "surface")
            val safeAutonomyMode = if (autonomyMode.equals("power", ignoreCase = true)) "power" else "approval"
            val response = client.post("$baseUrl/agent/sessions") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(
                    AgentSessionRequest(
                        sessionId = storedSessionId,
                        mode = safeMode,
                        surface = safeSurface,
                        allowAutonomousActions = safeAutonomyMode == "power",
                        autonomyMode = safeAutonomyMode
                    )
                )
            }
            if (response.status.value in 200..299) {
                val body: AgentSessionBootstrapResponse = response.body()
                if (body.sessionId.isNotBlank()) {
                    saveSessionId(context, body.sessionId)
                }
                Result.success(body)
            } else {
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun ensureSessionId(
        context: Context,
        mode: String,
        surface: String,
        allowAutonomousActions: Boolean,
        autonomyMode: String
    ): Result<String> {
        val stored = getStoredSessionId(context)
        if (!stored.isNullOrBlank()) return Result.success(stored)

        return bootstrapSession(
            context = context,
            mode = mode,
            surface = surface,
            allowAutonomousActions = allowAutonomousActions,
            autonomyMode = autonomyMode
        ).map { it.sessionId }
    }

    suspend fun sendTurn(
        context: Context,
        inputText: String,
        surface: String,
        surfaceContext: Map<String, String> = emptyMap(),
        allowAutonomousActions: Boolean = false,
        autonomyMode: String = if (allowAutonomousActions) "power" else "approval"
    ): Result<AgentTurnResponse> {
        return try {
            val token = authToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeInputText = InputSecurity.prompt(inputText, "inputText", 2_000)
            val safeSurface = InputSecurity.identifier(surface, "surface")
            val safeAutonomyMode = if (autonomyMode.equals("power", ignoreCase = true)) "power" else "approval"
            val safeSurfaceContext = surfaceContext
                .entries
                .take(30)
                .associate { (key, value) ->
                    InputSecurity.identifier(key, "surfaceContext key") to
                        InputSecurity.text(value, "surfaceContext value", 500, allowBlank = true)
                }
            val sessionId = ensureSessionId(
                context = context,
                mode = "text",
                surface = safeSurface,
                allowAutonomousActions = safeAutonomyMode == "power",
                autonomyMode = safeAutonomyMode
            ).getOrElse { return Result.failure(it) }

            val response = client.post("$baseUrl/agent/sessions/$sessionId/turns") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(
                    AgentTurnRequest(
                        inputText = safeInputText,
                        surface = safeSurface,
                        surfaceContext = safeSurfaceContext,
                        allowAutonomousActions = safeAutonomyMode == "power",
                        autonomyMode = safeAutonomyMode
                    )
                )
            }
            if (response.status.value in 200..299) {
                val body: AgentTurnResponse = response.body()
                if (body.sessionState.sessionId.isNotBlank()) {
                    saveSessionId(context, body.sessionState.sessionId)
                }
                Result.success(body)
            } else {
                if (response.status.value == 404) {
                    clearSession(context)
                }
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendVoiceTurn(
        context: Context,
        audioBytes: ByteArray,
        fileName: String,
        mimeType: String,
        surface: String,
        surfaceContext: Map<String, String> = emptyMap(),
        allowAutonomousActions: Boolean = false,
        autonomyMode: String = if (allowAutonomousActions) "power" else "approval",
        synthesizeAudio: Boolean = true
    ): Result<AgentVoiceTurnResponse> {
        return try {
            val token = authToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeFileName = InputSecurity.fileName(fileName, "agent-voice.m4a")
            val safeMimeType = InputSecurity.voiceMime(mimeType)
            val safeAudioBytes = InputSecurity.uploadBytes(audioBytes, "audio", 20 * 1024 * 1024)
            val safeSurface = InputSecurity.identifier(surface, "surface")
            val safeAutonomyMode = if (autonomyMode.equals("power", ignoreCase = true)) "power" else "approval"
            val safeSurfaceContext = surfaceContext
                .entries
                .take(30)
                .associate { (key, value) ->
                    InputSecurity.identifier(key, "surfaceContext key") to
                        InputSecurity.text(value, "surfaceContext value", 500, allowBlank = true)
                }
            val sessionId = ensureSessionId(
                context = context,
                mode = "voice",
                surface = safeSurface,
                allowAutonomousActions = safeAutonomyMode == "power",
                autonomyMode = safeAutonomyMode
            ).getOrElse { return Result.failure(it) }

            val response = client.post("$baseUrl/agent/sessions/$sessionId/voice") {
                header("Authorization", "Bearer $token")
                setBody(MultiPartFormDataContent(formData {
                    append(
                        "audio",
                        safeAudioBytes,
                        Headers.build {
                            append(HttpHeaders.ContentType, safeMimeType)
                            append(HttpHeaders.ContentDisposition, "filename=$safeFileName")
                        }
                    )
                    append("surface", safeSurface)
                    append("allowAutonomousActions", (safeAutonomyMode == "power").toString())
                    append("autonomyMode", safeAutonomyMode)
                    append("synthesizeAudio", synthesizeAudio.toString())
                    append("surfaceContext", json.encodeToString(safeSurfaceContext))
                }))
            }

            if (response.status.value in 200..299) {
                val body: AgentVoiceTurnResponse = response.body()
                if (body.sessionState.sessionId.isNotBlank()) {
                    saveSessionId(context, body.sessionState.sessionId)
                }
                Result.success(body)
            } else {
                if (response.status.value == 404) {
                    clearSession(context)
                }
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPendingActions(context: Context): Result<List<AgentPendingAction>> {
        return try {
            val token = authToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$baseUrl/agent/pending-actions") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.value in 200..299) {
                val body: AgentPendingActionsResponse = response.body()
                Result.success(body.actions)
            } else {
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun approvePendingAction(
        context: Context,
        actionId: String
    ): Result<AgentApproveActionResponse> {
        return try {
            val token = authToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeActionId = InputSecurity.identifier(actionId, "actionId")
            val response = client.post("$baseUrl/agent/approve/$safeActionId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.value in 200..299) {
                Result.success(response.body())
            } else {
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectPendingAction(
        context: Context,
        actionId: String
    ): Result<AgentRejectActionResponse> {
        return try {
            val token = authToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeActionId = InputSecurity.identifier(actionId, "actionId")
            val response = client.post("$baseUrl/agent/reject/$safeActionId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.value in 200..299) {
                Result.success(response.body())
            } else {
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGoals(context: Context): Result<List<AgentGoal>> {
        return try {
            val token = authToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$baseUrl/agent/goals") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.value in 200..299) {
                val body: AgentGoalsResponse = response.body()
                Result.success(body.goals)
            } else {
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createGoal(
        context: Context,
        goal: String,
        category: String? = null,
        priority: Int? = null
    ): Result<AgentGoalUpsertResponse> {
        return try {
            val token = authToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeGoal = InputSecurity.prompt(goal, "goal", 500)
            val safeCategory = InputSecurity.optionalText(category, "category", 80)
            val safePriority = priority?.let { InputSecurity.boundedInt(it, "priority", 0, 10) }
            val response = client.post("$baseUrl/agent/goals") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(
                    AgentGoalUpsertRequest(
                        goal = safeGoal,
                        category = safeCategory,
                        priority = safePriority
                    )
                )
            }
            if (response.status.value in 200..299) {
                Result.success(response.body())
            } else {
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteGoal(
        context: Context,
        goalId: String
    ): Result<AgentGoalDeleteResponse> {
        return try {
            val token = authToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeGoalId = InputSecurity.identifier(goalId, "goalId")
            val response = client.delete("$baseUrl/agent/goals/$safeGoalId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.value in 200..299) {
                Result.success(response.body())
            } else {
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSmartReplies(
        context: Context,
        lastMessage: String,
        conversationId: String? = null,
        contextText: String? = null
    ): Result<List<String>> {
        return try {
            val token = authToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeLastMessage = InputSecurity.prompt(lastMessage, "lastMessage", 1_000)
            val safeConversationId = InputSecurity.optionalIdentifier(conversationId, "conversationId")
            val safeContext = contextText?.let { InputSecurity.prompt(it, "context", 1_500) }
            val response = client.post("$baseUrl/ai/chat/smart-replies") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(
                    SmartRepliesRequest(
                        lastMessage = safeLastMessage,
                        conversationId = safeConversationId,
                        context = safeContext
                    )
                )
            }
            if (response.status.value in 200..299) {
                val body: SmartRepliesResponse = response.body()
                Result.success(body.replies)
            } else {
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getConversationStarters(
        context: Context,
        otherUserId: String,
        goal: String? = null,
        contextText: String? = null
    ): Result<List<String>> {
        return try {
            val token = authToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeOtherUserId = InputSecurity.identifier(otherUserId, "otherUserId")
            val safeGoal = goal?.let { InputSecurity.prompt(it, "goal", 500) }
            val safeContext = contextText?.let { InputSecurity.prompt(it, "context", 1_500) }
            val response = client.post("$baseUrl/ai/chat/conversation-starters") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(
                    ConversationStartersRequest(
                        context = safeContext,
                        goal = safeGoal,
                        otherUserId = safeOtherUserId
                    )
                )
            }
            if (response.status.value in 200..299) {
                val body: ConversationStartersResponse = response.body()
                Result.success(body.starters)
            } else {
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
