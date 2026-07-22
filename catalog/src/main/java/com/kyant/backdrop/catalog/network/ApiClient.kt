package com.kyant.backdrop.catalog.network

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlinx.io.asSource
import kotlinx.io.buffered
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.UnknownHostException
import java.util.Base64
import java.util.UUID
import javax.net.ssl.SSLException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vormex_prefs")

object ApiClient {
    @Serializable
    data class SavedAccountSession(
        val userId: String,
        val email: String? = null,
        val username: String? = null,
        val name: String? = null,
        val profileImage: String? = null,
        val token: String,
        val refreshToken: String? = null,
        val updatedAtMillis: Long = System.currentTimeMillis()
    )

    internal fun normalizeSavedAccountSessions(
        accounts: List<SavedAccountSession>
    ): List<SavedAccountSession> {
        return accounts
            .filter { it.userId.isNotBlank() && it.token.isNotBlank() }
            .distinctBy { it.userId }
            .sortedByDescending { it.updatedAtMillis }
    }

    internal fun upsertSavedAccountSessions(
        existing: List<SavedAccountSession>,
        user: User,
        token: String,
        refreshToken: String?,
        nowMillis: Long = System.currentTimeMillis()
    ): List<SavedAccountSession> {
        val previous = existing.firstOrNull { it.userId == user.id }
        val next = SavedAccountSession(
            userId = user.id,
            email = user.email,
            username = user.username,
            name = user.name,
            profileImage = user.profileImage,
            token = token,
            refreshToken = refreshToken ?: previous?.refreshToken,
            updatedAtMillis = nowMillis
        )
        return normalizeSavedAccountSessions(listOf(next) + existing.filterNot { it.userId == user.id })
    }

    internal fun markSavedAccountSessionUsed(
        existing: List<SavedAccountSession>,
        userId: String,
        nowMillis: Long = System.currentTimeMillis()
    ): List<SavedAccountSession> {
        return normalizeSavedAccountSessions(
            existing.map { account ->
                if (account.userId == userId) account.copy(updatedAtMillis = nowMillis) else account
            }
        )
    }

    internal fun removeSavedAccountSession(
        existing: List<SavedAccountSession>,
        userId: String
    ): List<SavedAccountSession> {
        return normalizeSavedAccountSessions(existing.filterNot { it.userId == userId })
    }

    private const val FALLBACK_API_BASE_URL = "https://www.vormex.in/api"
    private val BASE_URL = normalizeApiBaseUrl(BuildConfig.API_BASE_URL)
    private val fallbackApiBaseUrls = if (BuildConfig.DEBUG) {
        emptyList()
    } else {
        listOf(FALLBACK_API_BASE_URL)
            .map(::normalizeApiBaseUrl)
            .filterNot { it.equals(BASE_URL, ignoreCase = true) }
            .distinct()
    }
    private val managedAdSessionId = UUID.randomUUID().toString()
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private const val DEFAULT_REQUEST_TIMEOUT_MILLIS = 60_000L
    private const val DEFAULT_CONNECT_TIMEOUT_MILLIS = 20_000L
    private const val DEFAULT_SOCKET_TIMEOUT_MILLIS = 60_000L
    private const val UPLOAD_REQUEST_TIMEOUT_MILLIS = 300_000L
    private const val UPLOAD_CONNECT_TIMEOUT_MILLIS = 30_000L
    private const val UPLOAD_SOCKET_TIMEOUT_MILLIS = 300_000L

    fun resolveCollegeLogoUrl(logoUrl: String?, domain: String?): String? {
        val normalizedDomain = domain?.trim()?.takeIf { it.isNotBlank() }
            ?: logoUrl
                ?.takeIf { it.isNotBlank() }
                ?.let { runCatching { Uri.parse(it).getQueryParameter("domain") }.getOrNull() }
                ?.trim()
                ?.takeIf { it.isNotBlank() }

        if (normalizedDomain != null) {
            return "${BASE_URL.trimEnd('/')}/people/college-logo?domain=${Uri.encode(normalizedDomain)}"
        }

        return logoUrl?.takeIf { it.isNotBlank() }
    }
    private const val AUTH_TOKEN_TRANSPORT_HEADER = "X-Auth-Token-Transport"
    private const val AUTH_TOKEN_TRANSPORT_BEARER = "bearer"
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false // Don't send null values to backend
    }
    
    private val client = createHttpClient(
        defaultJsonContentType = true,
        logBody = true,
        requestTimeoutMillis = DEFAULT_REQUEST_TIMEOUT_MILLIS,
        connectTimeoutMillis = DEFAULT_CONNECT_TIMEOUT_MILLIS,
        socketTimeoutMillis = DEFAULT_SOCKET_TIMEOUT_MILLIS,
        readTimeoutMillis = DEFAULT_SOCKET_TIMEOUT_MILLIS,
        writeTimeoutMillis = DEFAULT_SOCKET_TIMEOUT_MILLIS
    )

    private val uploadClient = createHttpClient(
        defaultJsonContentType = false,
        logBody = false,
        requestTimeoutMillis = UPLOAD_REQUEST_TIMEOUT_MILLIS,
        connectTimeoutMillis = UPLOAD_CONNECT_TIMEOUT_MILLIS,
        socketTimeoutMillis = UPLOAD_SOCKET_TIMEOUT_MILLIS,
        readTimeoutMillis = UPLOAD_SOCKET_TIMEOUT_MILLIS,
        writeTimeoutMillis = UPLOAD_SOCKET_TIMEOUT_MILLIS
    )

    private fun createHttpClient(
        defaultJsonContentType: Boolean,
        logBody: Boolean,
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
        install(HttpSend) {
            maxSendCount = 2
        }
        install(HttpTimeout) {
            this.requestTimeoutMillis = requestTimeoutMillis
            this.connectTimeoutMillis = connectTimeoutMillis
            this.socketTimeoutMillis = socketTimeoutMillis
        }
        installVormexAppCheckInterceptor()
        defaultRequest {
            applyVormexClientHeaders()
            if (defaultJsonContentType) {
                contentType(ContentType.Application.Json)
            }
        }
    }.also { httpClient ->
        httpClient.plugin(HttpSend).intercept { request ->
            try {
                execute(request)
            } catch (primaryFailure: IOException) {
                val fallbackRequest = fallbackRequestFor(request, primaryFailure)
                if (fallbackRequest == null) {
                    if (requestTargetsConfiguredApi(request) && primaryFailure.isRecoverableNetworkFailure()) {
                        throw friendlyNetworkFailure(primaryFailure)
                    }
                    throw primaryFailure
                }

                try {
                    execute(fallbackRequest)
                } catch (fallbackFailure: IOException) {
                    throw friendlyNetworkFailure(fallbackFailure)
                }
            }
        }
    }
    
    private val TOKEN_KEY = stringPreferencesKey("auth_token")
    private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
    private val USER_ID_KEY = stringPreferencesKey("user_id")
    private val SAVED_ACCOUNTS_KEY = stringPreferencesKey("saved_accounts_json")
    private val DEVICE_INSTALL_ID_KEY = stringPreferencesKey("device_install_id")
    
    private var cachedToken: String? = null
    private var cachedRefreshToken: String? = null
    private val sessionRefreshMutex = Mutex()

    private data class ApiFailure(
        val message: String
    )

    private fun normalizeApiBaseUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim().trimEnd('/')
        val usableUrl = trimmed.ifBlank { FALLBACK_API_BASE_URL }
        return if (usableUrl.endsWith("/api", ignoreCase = true)) usableUrl else "$usableUrl/api"
    }

    private fun requestTargetsConfiguredApi(request: HttpRequestBuilder): Boolean {
        val requestUrl = request.url.buildString()
        return requestUrl == BASE_URL || requestUrl.startsWith("$BASE_URL/")
    }

    private fun fallbackRequestFor(
        request: HttpRequestBuilder,
        failure: IOException
    ): HttpRequestBuilder? {
        if (!failure.isRecoverableNetworkFailure()) return null

        val requestUrl = request.url.buildString()
        val suffix = when {
            requestUrl == BASE_URL -> ""
            requestUrl.startsWith("$BASE_URL/") -> requestUrl.removePrefix(BASE_URL)
            else -> return null
        }
        val fallbackUrl = fallbackApiBaseUrls.firstOrNull()?.let { "$it$suffix" } ?: return null

        return HttpRequestBuilder()
            .takeFrom(request)
            .apply {
                url.takeFrom(fallbackUrl)
            }
    }

    private fun IOException.isRecoverableNetworkFailure(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            if (current is UnknownHostException ||
                current is ConnectException ||
                current is NoRouteToHostException ||
                current is SSLException
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }

    private fun friendlyNetworkFailure(cause: IOException): IOException {
        return IOException("Couldn't connect to Vormex. Check your internet connection and try again.", cause)
    }

    suspend fun getOrCreateDeviceInstallId(context: Context): String {
        val existing = context.dataStore.data.first()[DEVICE_INSTALL_ID_KEY]
            ?.takeIf { it.length in 16..256 }
        if (existing != null) return existing

        val installId = UUID.randomUUID().toString()
        context.dataStore.edit { prefs ->
            prefs[DEVICE_INSTALL_ID_KEY] = installId
        }
        return installId
    }

    private fun HttpRequestBuilder.attachDeviceLinkHeaders(installId: String) {
        header("X-Vormex-Install-Id", installId)
        header("X-Vormex-Platform", "android")
    }

    private class SessionRefreshException(
        message: String,
        val shouldClearSession: Boolean
    ) : Exception(message)

    private enum class SessionRefreshResult {
        SUCCESS,
        KEEP_SESSION,
        CLEAR_SESSION
    }

    private fun normalizeBearerToken(token: String?): String? {
        val trimmed = token?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return if (trimmed.startsWith("Bearer ", ignoreCase = true)) {
            trimmed.substringAfter(' ').trim().takeIf { it.isNotEmpty() }
        } else {
            trimmed
        }
    }

    private fun isLikelyJwtAccessToken(token: String): Boolean {
        val parts = token.split('.')
        return parts.size == 3 && parts.all { it.isNotBlank() }
    }

    internal fun isJwtExpiringSoon(
        token: String,
        nowMillis: Long = System.currentTimeMillis(),
        refreshSkewMillis: Long = 60_000L
    ): Boolean {
        return runCatching {
            val payload = token.split('.').getOrNull(1)?.takeIf { it.isNotBlank() }
                ?: return@runCatching true
            val decodedPayload = String(Base64.getUrlDecoder().decode(payload), Charsets.UTF_8)
            val expiresAtSeconds = Regex("\"exp\"\\s*:\\s*(\\d+)")
                .find(decodedPayload)
                ?.groupValues
                ?.getOrNull(1)
                ?.toLongOrNull()
                ?: return@runCatching true
            expiresAtSeconds * 1000L <= nowMillis + refreshSkewMillis
        }.getOrDefault(true)
    }

    private suspend fun clearAccessToken(context: Context) {
        cachedToken = null
        context.dataStore.edit { prefs ->
            prefs.remove(TOKEN_KEY)
        }
    }

    private suspend fun clearRefreshToken(context: Context) {
        cachedRefreshToken = null
        context.dataStore.edit { prefs ->
            prefs.remove(REFRESH_TOKEN_KEY)
        }
    }

    private suspend fun recoverFromMalformedAccessToken(context: Context): String? {
        clearAccessToken(context)
        if (getRefreshToken(context) == null) {
            clearToken(context)
            return null
        }

        val refreshResult = refreshSession(context)
        val refreshedToken = refreshResult
            .getOrNull()
            ?.token
            ?.let { normalizeBearerToken(it) }
            ?.takeIf { isLikelyJwtAccessToken(it) }

        if (refreshedToken == null && isConfirmedSessionExpired(refreshResult.exceptionOrNull())) {
            clearToken(context)
        }

        return refreshedToken
    }
    
    // Token management
    suspend fun saveToken(context: Context, token: String, userId: String, refreshToken: String? = null) {
        val cleanToken = normalizeBearerToken(token)
            ?: throw IllegalArgumentException("Authentication token missing")
        if (!isLikelyJwtAccessToken(cleanToken)) {
            throw IllegalArgumentException("Authentication token is invalid")
        }
        val cleanRefreshToken = refreshToken?.trim()?.takeIf { it.isNotEmpty() }
        cachedToken = cleanToken
        cachedRefreshToken = cleanRefreshToken
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = cleanToken
            prefs[USER_ID_KEY] = userId
            if (cleanRefreshToken != null) {
                prefs[REFRESH_TOKEN_KEY] = cleanRefreshToken
            } else {
                prefs.remove(REFRESH_TOKEN_KEY)
            }
        }
    }

    suspend fun saveAuthenticatedSession(
        context: Context,
        token: String,
        user: User,
        refreshToken: String? = null
    ) {
        val cleanToken = normalizeBearerToken(token)
            ?: throw IllegalArgumentException("Authentication token missing")
        if (!isLikelyJwtAccessToken(cleanToken)) {
            throw IllegalArgumentException("Authentication token is invalid")
        }
        val cleanRefreshToken = refreshToken?.trim()?.takeIf { it.isNotEmpty() }
        val effectiveRefreshToken = cleanRefreshToken
            ?: readSavedAccountSessions(context).firstOrNull { it.userId == user.id }?.refreshToken
        saveToken(context, cleanToken, user.id, effectiveRefreshToken)
        upsertSavedAccountSession(context, user, cleanToken, effectiveRefreshToken)
    }

    suspend fun saveActiveAccountSnapshot(context: Context, user: User) {
        val activeUserId = getCurrentUserId(context)?.takeIf { it == user.id } ?: return
        val token = getToken(context) ?: return
        upsertSavedAccountSession(context, user.copy(id = activeUserId), token, getRefreshToken(context))
    }

    suspend fun getSavedAccountSessions(context: Context): List<SavedAccountSession> {
        return readSavedAccountSessions(context)
    }

    suspend fun switchToSavedAccount(context: Context, userId: String): Result<SavedAccountSession> {
        return try {
            val existing = readSavedAccountSessions(context)
            val target = existing.firstOrNull { it.userId == userId }
                ?: return Result.failure(Exception("Account is not saved on this device."))
            saveToken(context, target.token, target.userId, target.refreshToken)
            val updated = target.copy(updatedAtMillis = System.currentTimeMillis())
            writeSavedAccountSessions(
                context,
                markSavedAccountSessionUsed(existing, userId, updated.updatedAtMillis)
            )
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeSavedAccount(context: Context, userId: String) {
        val activeUserId = getCurrentUserId(context)
        writeSavedAccountSessions(
            context,
            removeSavedAccountSession(readSavedAccountSessions(context), userId)
        )
        if (activeUserId == userId) {
            clearToken(context)
        }
    }

    private suspend fun upsertSavedAccountSession(
        context: Context,
        user: User,
        token: String,
        refreshToken: String?
    ) {
        val existing = readSavedAccountSessions(context)
        writeSavedAccountSessions(
            context,
            upsertSavedAccountSessions(existing, user, token, refreshToken)
        )
    }

    private suspend fun readSavedAccountSessions(context: Context): List<SavedAccountSession> {
        val raw = context.dataStore.data.first()[SAVED_ACCOUNTS_KEY] ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<SavedAccountSession>>(raw)
        }.getOrElse {
            emptyList()
        }
            .filter { it.userId.isNotBlank() && it.token.isNotBlank() }
            .distinctBy { it.userId }
            .sortedByDescending { it.updatedAtMillis }
    }

    private suspend fun writeSavedAccountSessions(
        context: Context,
        accounts: List<SavedAccountSession>
    ) {
        val normalized = normalizeSavedAccountSessions(accounts)
        context.dataStore.edit { prefs ->
            if (normalized.isEmpty()) {
                prefs.remove(SAVED_ACCOUNTS_KEY)
            } else {
                prefs[SAVED_ACCOUNTS_KEY] = json.encodeToString(normalized)
            }
        }
    }
    
    suspend fun getToken(context: Context): String? {
        cachedToken?.let { cached ->
            val normalized = normalizeBearerToken(cached)
            if (normalized == null || !isLikelyJwtAccessToken(normalized)) {
                return recoverFromMalformedAccessToken(context)
            }
            cachedToken = normalized
            return normalized
        }
        val stored = context.dataStore.data.first()[TOKEN_KEY]
        val normalized = normalizeBearerToken(stored)
        if (normalized != null && !isLikelyJwtAccessToken(normalized)) {
            return recoverFromMalformedAccessToken(context)
        }
        if (stored != null && normalized != stored) {
            context.dataStore.edit { prefs ->
                if (normalized != null) {
                    prefs[TOKEN_KEY] = normalized
                } else {
                    prefs.remove(TOKEN_KEY)
                }
            }
        }
        cachedToken = normalized
        return normalized
    }

    suspend fun getRealtimeAccessToken(context: Context): String? {
        val existingToken = getToken(context)?.takeIf { it.isNotBlank() } ?: return null
        // Reuse a healthy access token. Refreshing here on every chat send used
        // to rotate the session, reconnect the socket, and add a REST request.
        if (!isJwtExpiringSoon(existingToken) || getRefreshToken(context) == null) {
            return existingToken
        }

        return when (refreshTokenForRetry(context)) {
            SessionRefreshResult.SUCCESS -> getToken(context) ?: existingToken
            SessionRefreshResult.KEEP_SESSION -> existingToken
            SessionRefreshResult.CLEAR_SESSION -> {
                clearRefreshToken(context)
                existingToken
            }
        }
    }
    
    suspend fun getCurrentUserId(context: Context): String? {
        return context.dataStore.data.first()[USER_ID_KEY]
    }

    suspend fun getRefreshToken(context: Context): String? {
        cachedRefreshToken?.let { cached ->
            val normalized = cached.trim().takeIf { it.isNotEmpty() }
            cachedRefreshToken = normalized
            return normalized
        }
        val stored = context.dataStore.data.first()[REFRESH_TOKEN_KEY]
        val normalized = stored?.trim()?.takeIf { it.isNotEmpty() }
        cachedRefreshToken = normalized
        return normalized
    }
    
    fun getTokenFlow(context: Context): Flow<String?> {
        return context.dataStore.data.map {
            normalizeBearerToken(it[TOKEN_KEY])?.takeIf { token -> isLikelyJwtAccessToken(token) }
        }
    }

    private suspend fun parseApiFailure(response: HttpResponse): ApiFailure {
        val responseText = runCatching { response.bodyAsText() }.getOrDefault("")
        val apiError = responseText
            .takeIf { it.isNotBlank() }
            ?.let { body -> runCatching { json.decodeFromString<ApiError>(body) }.getOrNull() }

        return ApiFailure(
            message = apiError?.getErrorMessage()
                ?: responseText.ifBlank { "Request failed (${response.status.value})" }
        )
    }

    private suspend fun refreshTokenForRetry(context: Context): SessionRefreshResult {
        val refreshTokenBeforeWait = getRefreshToken(context)
        if (refreshTokenBeforeWait == null) {
            return SessionRefreshResult.CLEAR_SESSION
        }

        return sessionRefreshMutex.withLock {
            val currentRefreshToken = getRefreshToken(context)
            if (currentRefreshToken == null) {
                return@withLock SessionRefreshResult.CLEAR_SESSION
            }

            if (currentRefreshToken != refreshTokenBeforeWait && getToken(context) != null) {
                return@withLock SessionRefreshResult.SUCCESS
            }

            val refreshResult = refreshSession(context)
            if (refreshResult.isSuccess) {
                return@withLock SessionRefreshResult.SUCCESS
            }

            val refreshError = refreshResult.exceptionOrNull()
            if ((refreshError as? SessionRefreshException)?.shouldClearSession == true) {
                SessionRefreshResult.CLEAR_SESSION
            } else {
                SessionRefreshResult.KEEP_SESSION
            }
        }
    }

    fun isConfirmedSessionExpired(error: Throwable?): Boolean {
        return (error as? SessionRefreshException)?.shouldClearSession == true
    }

    private suspend inline fun <reified T> authorizedRequestWithRefresh(
        context: Context,
        crossinline request: suspend (String) -> HttpResponse
    ): Result<T> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val firstResponse = request(token)
            if (firstResponse.status.isSuccess()) {
                return Result.success(firstResponse.body())
            }

            val firstFailure = parseApiFailure(firstResponse)
            if (firstResponse.status == HttpStatusCode.Unauthorized) {
                when (refreshTokenForRetry(context)) {
                    SessionRefreshResult.SUCCESS -> Unit
                    SessionRefreshResult.CLEAR_SESSION -> {
                        clearToken(context)
                        return Result.failure(Exception("Session expired. Please log in again."))
                    }
                    SessionRefreshResult.KEEP_SESSION -> {
                        return Result.failure(Exception("Connection issue. Please try again."))
                    }
                }

                val refreshedToken = getToken(context)
                    ?: return Result.failure(Exception("Session expired. Please log in again."))
                val retryResponse = request(refreshedToken)
                if (retryResponse.status.isSuccess()) {
                    return Result.success(retryResponse.body())
                }

                val retryFailure = parseApiFailure(retryResponse)
                if (retryResponse.status == HttpStatusCode.Unauthorized) {
                    clearToken(context)
                    return Result.failure(Exception("Session expired. Please log in again."))
                }
                return Result.failure(Exception(retryFailure.message))
            }

            Result.failure(Exception(firstFailure.message))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun authorizedUnitRequestWithRefresh(
        context: Context,
        request: suspend (String) -> HttpResponse
    ): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val firstResponse = request(token)
            if (firstResponse.status.isSuccess()) {
                return Result.success(Unit)
            }

            val firstFailure = parseApiFailure(firstResponse)
            if (firstResponse.status == HttpStatusCode.Unauthorized) {
                when (refreshTokenForRetry(context)) {
                    SessionRefreshResult.SUCCESS -> Unit
                    SessionRefreshResult.CLEAR_SESSION -> {
                        clearToken(context)
                        return Result.failure(Exception("Session expired. Please log in again."))
                    }
                    SessionRefreshResult.KEEP_SESSION -> {
                        return Result.failure(Exception("Connection issue. Please try again."))
                    }
                }

                val refreshedToken = getToken(context)
                    ?: return Result.failure(Exception("Session expired. Please log in again."))
                val retryResponse = request(refreshedToken)
                if (retryResponse.status.isSuccess()) {
                    return Result.success(Unit)
                }

                val retryFailure = parseApiFailure(retryResponse)
                if (retryResponse.status == HttpStatusCode.Unauthorized) {
                    clearToken(context)
                    return Result.failure(Exception("Session expired. Please log in again."))
                }
                return Result.failure(Exception(retryFailure.message))
            }

            Result.failure(Exception(firstFailure.message))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun clearToken(context: Context) {
        cachedToken = null
        cachedRefreshToken = null
        context.dataStore.edit { prefs ->
            prefs.remove(TOKEN_KEY)
            prefs.remove(REFRESH_TOKEN_KEY)
            prefs.remove(USER_ID_KEY)
        }
    }
    
    // Auth APIs
    private fun AuthResponse.requireBearerToken(): AuthResponse {
        if (token.isNullOrBlank()) {
            throw IllegalStateException("Authentication token missing from server response")
        }
        return this
    }

    suspend fun login(context: Context, email: String, password: String): Result<AuthResponse> {
        return try {
            val safeEmail = InputSecurity.email(email)
            val safePassword = InputSecurity.text(password, "password", 256)
            val installId = getOrCreateDeviceInstallId(context)
            val response = client.post("$BASE_URL/auth/login") {
                header(AUTH_TOKEN_TRANSPORT_HEADER, AUTH_TOKEN_TRANSPORT_BEARER)
                attachDeviceLinkHeaders(installId)
                setBody(LoginRequest(safeEmail, safePassword))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<AuthResponse>().requireBearerToken())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Google Sign-In
    suspend fun googleSignIn(context: Context, idToken: String): Result<AuthResponse> {
        return try {
            val safeIdToken = InputSecurity.text(idToken, "idToken", 8_192)
            val installId = getOrCreateDeviceInstallId(context)
            val response = client.post("$BASE_URL/auth/google") {
                header(AUTH_TOKEN_TRANSPORT_HEADER, AUTH_TOKEN_TRANSPORT_BEARER)
                attachDeviceLinkHeaders(installId)
                setBody(GoogleSignInRequest(safeIdToken))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<AuthResponse>().requireBearerToken())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun forgotPassword(email: String): Result<MessageResponse> {
        return try {
            val safeEmail = InputSecurity.email(email)
            val response = client.post("$BASE_URL/auth/forgot-password") {
                setBody(ForgotPasswordRequest(safeEmail))
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

    suspend fun resendVerificationCode(email: String): Result<MessageResponse> {
        return try {
            val safeEmail = InputSecurity.email(email)
            val response = client.post("$BASE_URL/auth/resend-verification") {
                setBody(ResendVerificationRequest(safeEmail))
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

    suspend fun verifyEmailOtp(context: Context, email: String, code: String): Result<AuthResponse> {
        return try {
            val safeEmail = InputSecurity.email(email)
            val safeCode = InputSecurity.text(code, "verification code", 16)
                .filter { it.isDigit() }
            if (safeCode.length != 6) {
                return Result.failure(Exception("Enter the 6-digit verification code."))
            }
            val installId = getOrCreateDeviceInstallId(context)
            val response = client.post("$BASE_URL/auth/verify-email") {
                header(AUTH_TOKEN_TRANSPORT_HEADER, AUTH_TOKEN_TRANSPORT_BEARER)
                attachDeviceLinkHeaders(installId)
                setBody(VerifyEmailOtpRequest(safeEmail, safeCode))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<AuthResponse>().requireBearerToken())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Register new user
    suspend fun register(
        context: Context,
        email: String,
        password: String,
        name: String,
        username: String,
        college: String? = null,
        branch: String? = null
    ): Result<AuthResponse> {
        return try {
            val safeEmail = InputSecurity.email(email)
            val safePassword = InputSecurity.text(password, "password", 256)
            val safeName = InputSecurity.text(name, "name", 100)
            val safeUsername = InputSecurity.identifier(username, "username")
            val safeCollege = InputSecurity.optionalText(college, "college", 120)
            val safeBranch = InputSecurity.optionalText(branch, "branch", 120)
            val installId = getOrCreateDeviceInstallId(context)
            val response = client.post("$BASE_URL/auth/register") {
                header(AUTH_TOKEN_TRANSPORT_HEADER, AUTH_TOKEN_TRANSPORT_BEARER)
                attachDeviceLinkHeaders(installId)
                setBody(RegisterRequest(safeEmail, safePassword, safeName, safeUsername, safeCollege, safeBranch))
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

    suspend fun refreshSession(context: Context): Result<AuthResponse> {
        return try {
            val refreshToken = getRefreshToken(context)
                ?: return Result.failure(
                    SessionRefreshException(
                        message = "No refresh token available",
                        shouldClearSession = true
                    )
                )
            val installId = getOrCreateDeviceInstallId(context)
            val response = client.post("$BASE_URL/auth/refresh") {
                header(AUTH_TOKEN_TRANSPORT_HEADER, AUTH_TOKEN_TRANSPORT_BEARER)
                attachDeviceLinkHeaders(installId)
                setBody(mapOf("refreshToken" to refreshToken))
            }
            if (response.status.isSuccess()) {
                val authResponse = response.body<AuthResponse>().requireBearerToken()
                val bearerToken = authResponse.token
                    ?: throw IllegalStateException("Authentication token missing from server response")
                saveAuthenticatedSession(context, bearerToken, authResponse.user, authResponse.refreshToken)
                Result.success(authResponse)
            } else {
                val error = parseApiFailure(response)
                Result.failure(
                    SessionRefreshException(
                        message = error.message,
                        shouldClearSession = response.status == HttpStatusCode.BadRequest ||
                            response.status == HttpStatusCode.Unauthorized ||
                            response.status == HttpStatusCode.Forbidden ||
                            response.status == HttpStatusCode.NotFound
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(context: Context): Result<Unit> {
        val refreshToken = getRefreshToken(context)
        return try {
            if (refreshToken != null) {
                val response = client.post("$BASE_URL/auth/logout") {
                    setBody(mapOf("refreshToken" to refreshToken))
                }
                if (!response.status.isSuccess()) {
                    val message = try {
                        val error: ApiError = response.body()
                        error.getErrorMessage()
                    } catch (_: Exception) {
                        "Logout failed"
                    }
                    return Result.failure(Exception(message))
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            clearToken(context)
        }
    }
    
    suspend fun getCurrentUser(context: Context): Result<User> {
        return authorizedRequestWithRefresh<User>(context) { token ->
            client.get("$BASE_URL/auth/me") {
                header("Authorization", "Bearer $token")
            }
        }
    }

    suspend fun getPremiumSubscription(context: Context): Result<PremiumSubscriptionResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/premium/subscription") {
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

    suspend fun getProfileBoostState(context: Context): Result<PremiumProfileBoostState> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/premium/boosts/me") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val body: PremiumProfileBoostResponse = response.body()
                Result.success(body.profileBoost)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun activateProfileBoost(
        context: Context,
        durationHours: Int? = null
    ): Result<ActivateProfileBoostResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/premium/boosts/profile") {
                header("Authorization", "Bearer $token")
                setBody(ActivateProfileBoostRequest(durationHours))
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

    suspend fun setDeveloperPremiumOverride(
        context: Context,
        enabled: Boolean
    ): Result<PremiumVerifyResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/premium/debug-override") {
                header("Authorization", "Bearer $token")
                setBody(DeveloperPremiumOverrideRequest(enabled))
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

    suspend fun getCreatorPro(context: Context): Result<CreatorProResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/premium/creator-pro") {
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

    suspend fun updateCreatorProSettings(
        context: Context,
        request: CreatorProSettingsRequest
    ): Result<CreatorProResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.patch("$BASE_URL/premium/creator-pro/settings") {
                header("Authorization", "Bearer $token")
                setBody(request)
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

    suspend fun setDeveloperCreatorProOverride(
        context: Context,
        enabled: Boolean
    ): Result<CreatorProResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/premium/creator-pro/debug-override") {
                header("Authorization", "Bearer $token")
                setBody(DeveloperCreatorProOverrideRequest(enabled))
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

    suspend fun createPremiumCheckout(
        context: Context,
        billingCycle: String = "monthly",
        plan: String = "premium"
    ): Result<PremiumCheckoutResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/premium/checkout") {
                header("Authorization", "Bearer $token")
                setBody(
                    buildJsonObject {
                        put("billingCycle", billingCycle)
                        put("plan", plan)
                    }
                )
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

    suspend fun verifyPremiumCheckout(
        context: Context,
        request: PremiumVerifyRequest
    ): Result<PremiumVerifyResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/premium/verify") {
                header("Authorization", "Bearer $token")
                setBody(request)
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

    suspend fun verifyGooglePlayPremiumCheckout(
        context: Context,
        request: GooglePlayPremiumVerifyRequest
    ): Result<PremiumVerifyResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/premium/play/verify") {
                header("Authorization", "Bearer $token")
                setBody(request)
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

    suspend fun cancelPremiumSubscription(context: Context): Result<PremiumVerifyResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/premium/cancel") {
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
    
    // Feed APIs
    suspend fun getFeed(
        context: Context,
        cursor: String? = null,
        limit: Int = 40,
        mode: String = "recommended",
        useCache: Boolean = true,
        adItemOffset: Int = 0
    ): Result<FeedResponse> {
        return try {
            val safeMode = InputSecurity.enumValue(mode, setOf("RECOMMENDED", "LATEST"), "mode")
            val safeLimit = InputSecurity.boundedInt(limit, "limit", 1, 50)
            val safeCursor = InputSecurity.optionalPaginationCursor(cursor, "cursor")
            val safeAdItemOffset = InputSecurity.boundedInt(adItemOffset, "adItemOffset", 0, 10_000)
            authorizedRequestWithRefresh<FeedResponse>(context) { token ->
                client.get("$BASE_URL/posts/feed") {
                    header("Authorization", "Bearer $token")
                    if (!useCache) {
                        header("Cache-Control", "no-cache, no-store, must-revalidate")
                        parameter("cacheBust", System.currentTimeMillis())
                    }
                    parameter("limit", safeLimit)
                    parameter("mode", safeMode)
                    parameter("adSessionId", managedAdSessionId)
                    parameter("adItemOffset", safeAdItemOffset)
                    safeCursor?.let { parameter("cursor", it) }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Create post (multipart FormData)
    suspend fun createPost(
        context: Context,
        type: String = "TEXT",
        content: String,
        visibility: String = "PUBLIC",
        imageBytes: List<Pair<ByteArray, String>> = emptyList(), // (bytes, filename)
        videoBytes: Pair<ByteArray, String>? = null // (bytes, filename)
    ): Result<Post> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeType = InputSecurity.enumValue(type, setOf("TEXT", "IMAGE", "VIDEO", "LINK", "POLL", "ARTICLE", "CELEBRATION"), "type")
            val safeVisibility = InputSecurity.enumValue(visibility, setOf("PUBLIC", "CONNECTIONS", "PRIVATE"), "visibility")
            val safeContent = InputSecurity.text(content, "content", 2_000)
            val safeImages = imageBytes.mapIndexed { index, (bytes, filename) ->
                imageUploadPayload(
                    bytes = InputSecurity.uploadBytes(bytes, "image", 10 * 1024 * 1024),
                    requestedFilename = filename,
                    fallbackBaseName = "image$index"
                )
            }
            val safeVideo = videoBytes?.let { (bytes, filename) ->
                InputSecurity.uploadBytes(bytes, "video", 100 * 1024 * 1024) to
                    InputSecurity.fileName(filename, "video.mp4")
            }
            val response = client.post("$BASE_URL/posts") {
                header("Authorization", "Bearer $token")
                setBody(MultiPartFormDataContent(formData {
                    append("type", safeType)
                    append("visibility", safeVisibility)
                    append("content", safeContent)
                    safeImages.forEach { image ->
                        append(
                            "media",
                            image.bytes,
                            Headers.build {
                                append(HttpHeaders.ContentType, image.mimeType)
                                append(HttpHeaders.ContentDisposition, "filename=${image.filename}")
                            }
                        )
                    }
                    safeVideo?.let { (bytes, filename) ->
                        append(
                            "video",
                            bytes,
                            Headers.build {
                                append(HttpHeaders.ContentType, "video/mp4")
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
    
    // Like/Unlike post
    suspend fun toggleLike(context: Context, postId: String): Result<LikeResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/posts/$postId/like") {
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
    
    // Stories APIs
    suspend fun getStories(context: Context, limit: Int = 180): Result<StoriesFeedResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeLimit = InputSecurity.boundedInt(limit, "limit", 20, 300)
            val response = client.get("$BASE_URL/stories/feed") {
                header("Authorization", "Bearer $token")
                header("Cache-Control", "no-cache, no-store, must-revalidate")
                parameter("limit", safeLimit.toString())
                parameter("_t", System.currentTimeMillis()) // Cache buster
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
    
    // Profile APIs
    suspend fun getProfile(context: Context, userId: String = "me"): Result<FullProfileResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/users/$userId/profile") {
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

    suspend fun getDiscoveryVisibility(context: Context): Result<DiscoveryVisibility> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/public/discovery/visibility/me") {
                header("Authorization", "Bearer $token")
                header("Cache-Control", "no-cache")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<DiscoveryVisibilityResponse>().visibility)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateDiscoveryVisibility(
        context: Context,
        webDiscoveryEnabled: Boolean? = null,
        aiDiscoveryEnabled: Boolean? = null
    ): Result<DiscoveryVisibility> {
        if (webDiscoveryEnabled == null && aiDiscoveryEnabled == null) {
            return Result.failure(IllegalArgumentException("A discovery visibility value is required"))
        }
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.patch("$BASE_URL/public/discovery/visibility/me") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(DiscoveryVisibilityUpdateRequest(webDiscoveryEnabled, aiDiscoveryEnabled))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<DiscoveryVisibilityResponse>().visibility)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getOtherUserProfile(context: Context, userId: String): Result<FullProfileResponse> {
        return try {
            val token = getToken(context)
            val response = client.get("$BASE_URL/users/$userId/profile") {
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

    suspend fun getProfileViewStats(context: Context, userId: String): Result<ProfileViewStats> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/social-proof/profile-views/$userId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<ProfileViewStatsApiResponse>().data)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProfileViewHistory(
        context: Context,
        userId: String,
        page: Int = 1,
        limit: Int = 50
    ): Result<ProfileViewHistory> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/social-proof/profile-views/$userId/history") {
                header("Authorization", "Bearer $token")
                parameter("page", page)
                parameter("limit", limit)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<ProfileViewHistoryApiResponse>().data)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleProfileSave(context: Context, targetUserId: String): Result<ProfileSaveState> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/social-proof/profile-saves/$targetUserId/toggle") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<ProfileSaveToggleResponse>().data)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProfileSavers(
        context: Context,
        userId: String,
        page: Int = 1,
        limit: Int = 50
    ): Result<ProfileSavers> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/social-proof/profile-saves/$userId") {
                header("Authorization", "Bearer $token")
                parameter("page", page)
                parameter("limit", limit)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<ProfileSaversApiResponse>().data)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRecentViewedProfiles(
        context: Context,
        page: Int = 1,
        limit: Int = 50
    ): Result<RecentViewedProfiles> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/social-proof/recent-profile-views") {
                header("Authorization", "Bearer $token")
                parameter("page", page)
                parameter("limit", limit)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<RecentViewedProfilesApiResponse>().data)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProfileInsights(context: Context, userId: String): Result<ProfileInsights> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/social-proof/profile-insights/$userId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<ProfileInsightsApiResponse>().data)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSavedProfiles(
        context: Context,
        cursor: String? = null,
        limit: Int = 20
    ): Result<SavedProfilesResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/saved/profiles") {
                header("Authorization", "Bearer $token")
                parameter("limit", limit.coerceIn(1, 50))
                cursor?.let { parameter("cursor", it) }
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

    // Comments APIs
    suspend fun getComments(context: Context, postId: String, cursor: String? = null): Result<CommentsResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/posts/$postId/comments") {
                header("Authorization", "Bearer $token")
                cursor?.let { parameter("cursor", it) }
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
    
    suspend fun createComment(context: Context, postId: String, content: String, parentId: String? = null): Result<CreateCommentResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/posts/$postId/comments") {
                header("Authorization", "Bearer $token")
                setBody(CreateCommentRequest(content, parentId))
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
    
    suspend fun toggleCommentLike(context: Context, postId: String, commentId: String): Result<CommentLikeResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/posts/$postId/comments/$commentId/like") {
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
    
    suspend fun deleteComment(context: Context, postId: String, commentId: String): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.delete("$BASE_URL/posts/$postId/comments/$commentId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete comment"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Share post
    suspend fun sharePost(context: Context, postId: String): Result<ShareResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/posts/$postId/share") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to get share URL"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== Find People APIs ====================
    
    // Smart Matches
    suspend fun getSmartMatches(
        context: Context,
        type: String = "all",
        page: Int = 1,
        limit: Int = 20
    ): Result<SmartMatchResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/matching/smart") {
                header("Authorization", "Bearer $token")
                parameter("type", type)
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
    
    // All People
    suspend fun getPeople(
        context: Context,
        search: String? = null,
        college: String? = null,
        branch: String? = null,
        graduationYear: Int? = null,
        skillLevel: String? = null,
        intent: String? = null,
        availability: String? = null,
        verifiedOnly: Boolean = false,
        radiusKm: Int? = null,
        lat: Double? = null,
        lng: Double? = null,
        scope: String? = null,
        page: Int = 1,
        limit: Int = 20,
        cursor: String? = null,
        includeTotal: Boolean = true,
        includeMutuals: Boolean = true
    ): Result<PeopleResponse> {
        return try {
            val token = getToken(context)
            val response = client.get("$BASE_URL/people") {
                token?.let { header("Authorization", "Bearer $it") }
                search?.let { parameter("search", it) }
                college?.let { parameter("college", it) }
                branch?.let { parameter("branch", it) }
                graduationYear?.let { parameter("graduationYear", it) }
                skillLevel?.let { parameter("skillLevel", it) }
                intent?.let { parameter("intent", it) }
                availability?.let { parameter("availability", it) }
                if (verifiedOnly) parameter("verifiedOnly", true)
                radiusKm?.let { parameter("radiusKm", it) }
                lat?.let { parameter("lat", it) }
                lng?.let { parameter("lng", it) }
                scope?.let { parameter("scope", it) }
                parameter("page", page)
                parameter("limit", limit)
                cursor?.let { parameter("cursor", it) }
                parameter("includeTotal", includeTotal)
                parameter("includeMutuals", includeMutuals)
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
    
    // Filter Options
    suspend fun getFilterOptions(context: Context): Result<FilterOptions> {
        return try {
            val response = client.get("$BASE_URL/people/filter-options")
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to get filter options"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Suggestions (For You)
    suspend fun getPeopleSuggestions(context: Context, limit: Int = 20): Result<SuggestionsResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/people/suggestions") {
                header("Authorization", "Bearer $token")
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

    suspend fun passDiscoverySuggestion(context: Context, targetUserId: String): Result<DiscoveryPassResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/people/discovery/pass") {
                header("Authorization", "Bearer $token")
                setBody(DiscoveryPassRequest(targetUserId))
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

    suspend fun rewindDiscoveryPass(context: Context): Result<DiscoveryRewindResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/people/discovery/rewind") {
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

    suspend fun getSavedDiscoverySearches(context: Context): Result<SavedDiscoverySearchesResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/people/saved-searches") {
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

    suspend fun saveDiscoverySearch(
        context: Context,
        name: String,
        filters: Map<String, String>
    ): Result<SavedDiscoverySearchResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/people/saved-searches") {
                header("Authorization", "Bearer $token")
                setBody(SaveDiscoverySearchRequest(name = name, filters = filters))
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

    suspend fun updateSavedDiscoverySearch(
        context: Context,
        searchId: String,
        request: UpdateSavedDiscoverySearchRequest
    ): Result<SavedDiscoverySearchResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.patch("$BASE_URL/people/saved-searches/$searchId") {
                header("Authorization", "Bearer $token")
                setBody(request)
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

    suspend fun deleteSavedDiscoverySearch(context: Context, searchId: String): Result<MessageResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.delete("$BASE_URL/people/saved-searches/$searchId") {
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

    suspend fun getPeopleYouKnow(context: Context): Result<PeopleYouKnowResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/people/contacts") {
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

    suspend fun discoverPeopleYouKnow(
        context: Context,
        contacts: List<PeopleYouKnowImportContact>,
        source: String = "picker"
    ): Result<PeopleYouKnowResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/people/contacts/import") {
                header("Authorization", "Bearer $token")
                setBody(PeopleYouKnowImportRequest(source = source, contacts = contacts))
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

    suspend fun clearPeopleYouKnow(context: Context): Result<MessageResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.delete("$BASE_URL/people/contacts") {
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

    suspend fun markPeopleYouKnowInviteSent(
        context: Context,
        entryId: String
    ): Result<PeopleYouKnowInviteResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/people/contacts/$entryId/invite") {
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

    // Same College
    suspend fun getSameCollegePeople(context: Context, limit: Int = 20): Result<PeopleResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/people/same-college") {
                header("Authorization", "Bearer $token")
                parameter("limit", limit)
            }
            android.util.Log.d("ApiClient", "Same college status: ${response.status}")
            if (response.status.isSuccess()) {
                val body: PeopleResponse = response.body()
                android.util.Log.d("ApiClient", "Same college people count: ${body.people.size}")
                Result.success(body)
            } else {
                android.util.Log.e("ApiClient", "Same college error status: ${response.status}")
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            android.util.Log.e("ApiClient", "Same college exception: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    // Search colleges on the platform
    suspend fun searchColleges(
        context: Context,
        query: String,
        limit: Int = 10,
        lat: Double? = null,
        lng: Double? = null
    ): Result<CollegeSearchResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/people/colleges") {
                header("Authorization", "Bearer $token")
                parameter("q", query)
                parameter("limit", limit)
                if (lat != null) parameter("lat", lat)
                if (lng != null) parameter("lng", lng)
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
    
    // Update Location
    suspend fun updateLocation(context: Context, lat: Double, lng: Double, accuracy: Float?): Result<Unit> {
        return updateLocationWithDetails(context, lat, lng, accuracy, null, null, null, null)
    }
    
    // Update Location with geocoded details
    suspend fun updateLocationWithDetails(
        context: Context,
        lat: Double,
        lng: Double,
        accuracy: Float?,
        city: String?,
        state: String?,
        country: String?,
        countryCode: String?
    ): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/location/update") {
                header("Authorization", "Bearer $token")
                setBody(LocationUpdateRequest(
                    lat = lat,
                    lng = lng,
                    accuracy = accuracy,
                    city = city,
                    state = state,
                    country = country,
                    countryCode = countryCode
                ))
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update location"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentLocation(context: Context): Result<CurrentLocationResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/location/current") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to load location settings"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateLocationSettings(
        context: Context,
        shareLocationPublic: Boolean? = null,
        locationPermission: Boolean? = null
    ): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.put("$BASE_URL/location/settings") {
                header("Authorization", "Bearer $token")
                setBody(
                    LocationSettingsRequest(
                        shareLocationPublic = shareLocationPublic,
                        locationPermission = locationPermission
                    )
                )
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update location settings"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Nearby People
    suspend fun getNearbyPeople(
        context: Context,
        lat: Double,
        lng: Double,
        radius: Int = 50,
        limit: Int = 20
    ): Result<NearbyResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/location/nearby") {
                header("Authorization", "Bearer $token")
                parameter("lat", lat)
                parameter("lng", lng)
                parameter("radius", radius)
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
    
    // Connection APIs
    suspend fun sendConnectionRequest(context: Context, receiverId: String): Result<ConnectionResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/connections/request") {
                header("Authorization", "Bearer $token")
                setBody(ConnectionRequest(receiverId))
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
    
    suspend fun cancelConnectionRequest(context: Context, connectionId: String): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.delete("$BASE_URL/connections/$connectionId/cancel") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to cancel request"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun acceptConnection(context: Context, connectionId: String): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/connections/$connectionId/accept") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to accept connection"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun rejectConnection(context: Context, connectionId: String): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/connections/$connectionId/reject") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to reject connection"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getConnectionStatus(context: Context, userId: String): Result<ConnectionStatusResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/connections/status/$userId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to get connection status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPendingConnectionRequests(
        context: Context,
        page: Int = 1,
        limit: Int = 20
    ): Result<PendingConnectionRequestsResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/connections/pending") {
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

    suspend fun getUserConnections(
        context: Context,
        userId: String,
        page: Int = 1,
        limit: Int = 20
    ): Result<ProfileConnectionsResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/connections/user/$userId") {
                header("Authorization", "Bearer $token")
                parameter("page", page)
                parameter("limit", limit)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to load connections"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== Profile APIs (Additional) ====================
    
    suspend fun getProfileFeed(
        context: Context, 
        userId: String, 
        page: Int = 1, 
        limit: Int = 20,
        filter: String = "all"
    ): Result<RecentActivity> {
        return try {
            val token = getToken(context)
            val response = client.get("$BASE_URL/users/$userId/feed") {
                token?.let { header("Authorization", "Bearer $it") }
                parameter("page", page)
                parameter("limit", limit)
                parameter("filter", filter)
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
    
    suspend fun getActivityHeatmap(context: Context, userId: String, year: Int? = null): Result<ActivityHeatmapResponse> {
        return try {
            val response = client.get("$BASE_URL/users/$userId/activity") {
                year?.let { parameter("year", it) }
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to load activity"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getActivityYears(context: Context, userId: String): Result<ActivityYearsResponse> {
        return try {
            val response = client.get("$BASE_URL/users/$userId/activity/years")
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to load activity years"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun startGitHubOAuth(context: Context): Result<GitHubOAuthStartResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/integrations/github/start") {
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

    suspend fun getGitHubStats(context: Context): Result<GitHubProfile> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/integrations/github/stats") {
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

    suspend fun syncGitHubStats(context: Context): Result<GitHubSyncResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/integrations/github/sync") {
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

    suspend fun disconnectGitHub(context: Context): Result<MessageResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/integrations/github/disconnect") {
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
    
    private fun buildProfileUpdatePayload(
        data: ProfileUpdateRequest,
        explicitNullFields: Set<String> = emptySet()
    ) = buildJsonObject {
        data.name?.let { put("name", InputSecurity.text(it, "name", 100)) }
        when {
            data.headline != null -> put("headline", InputSecurity.text(data.headline, "headline", 120, allowBlank = true))
            "headline" in explicitNullFields -> put("headline", JsonNull)
        }
        when {
            data.bio != null -> put("bio", InputSecurity.text(data.bio, "bio", 500, allowBlank = true))
            "bio" in explicitNullFields -> put("bio", JsonNull)
        }
        when {
            data.location != null -> put("location", InputSecurity.text(data.location, "location", 120, allowBlank = true))
            "location" in explicitNullFields -> put("location", JsonNull)
        }
        when {
            data.currentYear != null -> put("currentYear", InputSecurity.boundedInt(data.currentYear, "currentYear", 1, 5))
            "currentYear" in explicitNullFields -> put("currentYear", JsonNull)
        }
        when {
            data.degree != null -> put("degree", InputSecurity.text(data.degree, "degree", 120, allowBlank = true))
            "degree" in explicitNullFields -> put("degree", JsonNull)
        }
        when {
            data.graduationYear != null -> put("graduationYear", InputSecurity.boundedInt(data.graduationYear, "graduationYear", 1950, 2100))
            "graduationYear" in explicitNullFields -> put("graduationYear", JsonNull)
        }
        when {
            data.portfolioUrl != null -> put("portfolioUrl", InputSecurity.url(data.portfolioUrl, "portfolioUrl"))
            "portfolioUrl" in explicitNullFields -> put("portfolioUrl", JsonNull)
        }
        when {
            data.linkedinUrl != null -> put("linkedinUrl", InputSecurity.url(data.linkedinUrl, "linkedinUrl"))
            "linkedinUrl" in explicitNullFields -> put("linkedinUrl", JsonNull)
        }
        when {
            data.githubProfileUrl != null -> put("githubProfileUrl", InputSecurity.url(data.githubProfileUrl, "githubProfileUrl"))
            "githubProfileUrl" in explicitNullFields -> put("githubProfileUrl", JsonNull)
        }
        data.profileVisibility?.let { put("profileVisibility", InputSecurity.enumValue(it, setOf("PUBLIC", "CONNECTIONS", "PRIVATE"), "profileVisibility")) }
        data.isOpenToOpportunities?.let { put("isOpenToOpportunities", it) }
        data.interests?.let { interests ->
            putJsonArray("interests") {
                InputSecurity.sanitizeList(interests, "interests", 40, 80).forEach { add(JsonPrimitive(it)) }
            }
        }
        when {
            data.profileRing != null -> put("profileRing", InputSecurity.identifier(data.profileRing, "profileRing"))
            "profileRing" in explicitNullFields -> put("profileRing", JsonNull)
        }
        data.hasClaimedWelcomeGift?.let { put("hasClaimedWelcomeGift", it) }
        when {
            data.visitLoaderGiftId != null -> put("visitLoaderGiftId", InputSecurity.identifier(data.visitLoaderGiftId, "visitLoaderGiftId"))
            "visitLoaderGiftId" in explicitNullFields -> put("visitLoaderGiftId", JsonNull)
        }
        when {
            data.profileTheme != null -> put("profileTheme", InputSecurity.identifier(data.profileTheme, "profileTheme"))
            "profileTheme" in explicitNullFields -> put("profileTheme", JsonNull)
        }
        when {
            data.profileBadgeStyle != null -> {
                val normalizedBadgeStyle = InputSecurity
                    .identifier(data.profileBadgeStyle, "profileBadgeStyle")
                    .lowercase()
                require(normalizedBadgeStyle in setOf("student", "professional")) {
                    "profileBadgeStyle is invalid"
                }
                put("profileBadgeStyle", normalizedBadgeStyle)
            }
            "profileBadgeStyle" in explicitNullFields -> put("profileBadgeStyle", JsonNull)
        }
        when {
            data.college != null -> put("college", InputSecurity.text(data.college, "college", 120, allowBlank = true))
            "college" in explicitNullFields -> put("college", JsonNull)
        }
        when {
            data.branch != null -> put("branch", InputSecurity.text(data.branch, "branch", 120, allowBlank = true))
            "branch" in explicitNullFields -> put("branch", JsonNull)
        }
    }

    private fun sanitizeProjectInput(input: ProjectInput) = input.copy(
        name = InputSecurity.text(input.name, "project name", 120),
        description = InputSecurity.optionalText(input.description, "description", 1_000),
        role = InputSecurity.optionalText(input.role, "role", 120),
        techStack = input.techStack?.let { InputSecurity.sanitizeList(it, "techStack", 30, 60) },
        startDate = InputSecurity.text(input.startDate, "startDate", 40),
        endDate = InputSecurity.optionalText(input.endDate, "endDate", 40),
        projectUrl = input.projectUrl?.let { InputSecurity.url(it, "projectUrl") },
        githubUrl = input.githubUrl?.let { InputSecurity.url(it, "githubUrl") },
        images = input.images?.map { InputSecurity.url(it, "image") }?.take(10)
    )

    private fun sanitizeExperienceInput(input: ExperienceInput) = input.copy(
        title = InputSecurity.text(input.title, "title", 120),
        company = InputSecurity.text(input.company, "company", 120),
        type = InputSecurity.text(input.type, "type", 80),
        location = InputSecurity.optionalText(input.location, "location", 120),
        startDate = InputSecurity.text(input.startDate, "startDate", 40),
        endDate = InputSecurity.optionalText(input.endDate, "endDate", 40),
        description = InputSecurity.optionalText(input.description, "description", 2_000),
        skills = input.skills?.let { InputSecurity.sanitizeList(it, "skills", 30, 60) },
        logo = input.logo?.let { InputSecurity.url(it, "logo") }
    )

    private fun sanitizeEducationInput(input: EducationInput) = input.copy(
        school = InputSecurity.text(input.school, "school", 120),
        degree = InputSecurity.text(input.degree, "degree", 120),
        fieldOfStudy = InputSecurity.text(input.fieldOfStudy, "fieldOfStudy", 120),
        startDate = InputSecurity.text(input.startDate, "startDate", 40),
        endDate = InputSecurity.optionalText(input.endDate, "endDate", 40),
        grade = InputSecurity.optionalText(input.grade, "grade", 80),
        activities = InputSecurity.optionalText(input.activities, "activities", 2_000),
        description = InputSecurity.optionalText(input.description, "description", 2_000),
        logo = input.logo?.let { InputSecurity.url(it, "logo") }
    )

    private data class ImageUploadPayload(
        val bytes: ByteArray,
        val mimeType: String,
        val filename: String
    )

    private fun imageUploadPayload(
        bytes: ByteArray,
        requestedFilename: String,
        fallbackBaseName: String
    ): ImageUploadPayload {
        val pngSignature = intArrayOf(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        val detected = when {
            bytes.size >= 3 &&
                (bytes[0].toInt() and 0xFF) == 0xFF &&
                (bytes[1].toInt() and 0xFF) == 0xD8 &&
                (bytes[2].toInt() and 0xFF) == 0xFF -> "image/jpeg" to "jpg"
            bytes.size >= pngSignature.size && pngSignature.indices.all { index ->
                (bytes[index].toInt() and 0xFF) == pngSignature[index]
            } ->
                "image/png" to "png"
            bytes.size >= 6 && String(bytes, 0, 6, Charsets.US_ASCII) in setOf("GIF87a", "GIF89a") ->
                "image/gif" to "gif"
            bytes.size >= 12 &&
                String(bytes, 0, 4, Charsets.US_ASCII) == "RIFF" &&
                String(bytes, 8, 4, Charsets.US_ASCII) == "WEBP" -> "image/webp" to "webp"
            else -> throw IllegalArgumentException("Choose a JPEG, PNG, GIF, or WebP image")
        }
        val baseName = requestedFilename
            .substringBeforeLast('.', requestedFilename)
            .ifBlank { fallbackBaseName }
        val safeFilename = InputSecurity.fileName("$baseName.${detected.second}", "$fallbackBaseName.${detected.second}")
        return ImageUploadPayload(bytes, detected.first, safeFilename)
    }

    private fun sanitizeCertificateInput(input: CertificateInput) = input.copy(
        name = InputSecurity.text(input.name, "name", 160),
        issuingOrg = InputSecurity.text(input.issuingOrg, "issuingOrg", 160),
        issueDate = InputSecurity.text(input.issueDate, "issueDate", 40),
        expiryDate = InputSecurity.optionalText(input.expiryDate, "expiryDate", 40),
        credentialId = InputSecurity.optionalText(input.credentialId, "credentialId", 120),
        credentialUrl = input.credentialUrl?.let { InputSecurity.url(it, "credentialUrl") },
        color = InputSecurity.optionalText(input.color, "color", 32)
    )

    private fun sanitizeAchievementInput(input: AchievementInput) = input.copy(
        title = InputSecurity.text(input.title, "title", 160),
        type = InputSecurity.text(input.type, "type", 80),
        organization = InputSecurity.text(input.organization, "organization", 160),
        date = InputSecurity.text(input.date, "date", 40),
        description = InputSecurity.optionalText(input.description, "description", 2_000),
        certificateUrl = input.certificateUrl?.let { InputSecurity.url(it, "certificateUrl") },
        color = InputSecurity.optionalText(input.color, "color", 32)
    )

    suspend fun updateProfile(
        context: Context,
        data: ProfileUpdateRequest,
        explicitNullFields: Set<String> = emptySet()
    ): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.put("$BASE_URL/users/me") {
                header("Authorization", "Bearer $token")
                contentType(io.ktor.http.ContentType.Application.Json)
                setBody(buildProfileUpdatePayload(data, explicitNullFields))
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
    
    suspend fun updateAvatar(context: Context, avatarUrl: String): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeAvatarUrl = InputSecurity.url(avatarUrl, "avatarUrl")
            val response = client.post("$BASE_URL/users/me/avatar") {
                header("Authorization", "Bearer $token")
                contentType(io.ktor.http.ContentType.Application.Json)
                setBody(AvatarUpdateRequest(safeAvatarUrl))
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update avatar"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateBanner(context: Context, bannerUrl: String): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeBannerUrl = InputSecurity.url(bannerUrl, "bannerUrl")
            val response = client.post("$BASE_URL/users/me/banner") {
                header("Authorization", "Bearer $token")
                contentType(io.ktor.http.ContentType.Application.Json)
                setBody(BannerUpdateRequest(safeBannerUrl))
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update banner"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Upload avatar image (multipart form data)
     */
    suspend fun uploadAvatarImage(
        context: Context,
        imageBytes: ByteArray,
        filename: String = "avatar.jpg"
    ): Result<String> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeImageBytes = InputSecurity.uploadBytes(imageBytes, "avatar image", 10 * 1024 * 1024)
            val image = imageUploadPayload(safeImageBytes, filename, "avatar")
            val response = client.post("$BASE_URL/upload/avatar") {
                header("Authorization", "Bearer $token")
                setBody(MultiPartFormDataContent(formData {
                    append(
                        "image",
                        image.bytes,
                        Headers.build {
                            append(HttpHeaders.ContentType, image.mimeType)
                            append(HttpHeaders.ContentDisposition, "filename=${image.filename}")
                        }
                    )
                }))
            }
            if (response.status.isSuccess()) {
                val result: AvatarUploadResponse = response.body()
                val avatarUrl = listOf(
                    result.avatar,
                    result.avatarUrl,
                    result.url,
                    result.user?.profileImage
                ).firstOrNull { !it.isNullOrBlank() }.orEmpty()
                Result.success(avatarUrl)
            } else {
                Result.failure(Exception("Failed to upload avatar"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Upload banner image (multipart form data)
     */
    suspend fun uploadBannerImage(
        context: Context,
        imageBytes: ByteArray,
        filename: String = "banner.jpg"
    ): Result<String> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeImageBytes = InputSecurity.uploadBytes(imageBytes, "banner image", 10 * 1024 * 1024)
            val image = imageUploadPayload(safeImageBytes, filename, "banner")
            val response = client.post("$BASE_URL/upload/banner") {
                header("Authorization", "Bearer $token")
                setBody(MultiPartFormDataContent(formData {
                    append(
                        "image",
                        image.bytes,
                        Headers.build {
                            append(HttpHeaders.ContentType, image.mimeType)
                            append(HttpHeaders.ContentDisposition, "filename=${image.filename}")
                        }
                    )
                }))
            }
            if (response.status.isSuccess()) {
                val result: Map<String, String> = response.body()
                Result.success(result["bannerImageUrl"] ?: result["url"] ?: "")
            } else {
                Result.failure(Exception("Failed to upload banner"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== Follow APIs ====================
    
    suspend fun followUser(context: Context, userId: String): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/follow/$userId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to follow user"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun unfollowUser(context: Context, userId: String): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.delete("$BASE_URL/follow/$userId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to unfollow user"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getFollowStatus(context: Context, userId: String): Result<FollowStatusResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/follow/status/$userId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to get follow status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getMutualInfo(context: Context, userId: String): Result<MutualInfoResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/follow/mutual/$userId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to get mutual info"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFollowers(
        context: Context,
        userId: String,
        page: Int = 1,
        limit: Int = 20
    ): Result<FollowersListResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/follow/followers/$userId") {
                header("Authorization", "Bearer $token")
                parameter("page", page)
                parameter("limit", limit)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to load followers"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFollowing(
        context: Context,
        userId: String,
        page: Int = 1,
        limit: Int = 20
    ): Result<FollowingListResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/follow/following/$userId") {
                header("Authorization", "Bearer $token")
                parameter("page", page)
                parameter("limit", limit)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to load following"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Connection Management APIs ====================
    
    suspend fun removeConnection(context: Context, connectionId: String): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.delete("$BASE_URL/connections/$connectionId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to remove connection"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== Engagement/Streak APIs ====================
    
    suspend fun getStreaks(context: Context): Result<StreakData> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/engagement/streaks") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val streakResponse: StreakResponse = response.body()
                Result.success(streakResponse.data)
            } else {
                Result.failure(Exception("Failed to fetch streaks"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun recordLogin(context: Context): Result<StreakData> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/engagement/login") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val streakResponse: StreakResponse = response.body()
                Result.success(streakResponse.data)
            } else {
                Result.failure(Exception("Failed to record login"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProgressMe(context: Context): Result<ProgressData> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/progress/me") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val progressResponse: ProgressResponse = response.body()
                Result.success(progressResponse.data)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== Variable Rewards APIs (Hook Model) ====================

    suspend fun getRewardCards(context: Context): Result<RewardCardsResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/engagement/reward-cards") {
                header("Authorization", "Bearer $token")
                header("Cache-Control", "no-cache, no-store, must-revalidate")
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

    suspend fun trackRewardCardEvent(
        context: Context,
        request: RewardCardEventRequest
    ): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/engagement/reward-events") {
                header("Authorization", "Bearer $token")
                setBody(request)
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
     * Get daily matches with variable rewards.
     * Returns 1-5 random matches with a surprise message.
     */
    suspend fun getDailyMatches(context: Context): Result<DailyMatchesData> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/engagement/daily-matches") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val dailyMatchesResponse: DailyMatchesResponse = response.body()
                Result.success(dailyMatchesResponse.data)
            } else {
                Result.failure(Exception("Failed to fetch daily matches"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get users similar to current user (social proof).
     * Returns users with same goals, interests, or college.
     */
    suspend fun getPeopleLikeYou(context: Context): Result<PeopleLikeYouData> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/engagement/people-like-you") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val peopleLikeYouResponse: PeopleLikeYouResponse = response.body()
                Result.success(peopleLikeYouResponse.data)
            } else {
                Result.failure(Exception("Failed to fetch similar users"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get weekly hidden gem - a highly connected professional match.
     * Creates anticipation through scarcity (weekly reveal).
     */
    suspend fun getHiddenGem(context: Context): Result<HiddenGemData?> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/engagement/hidden-gem") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val hiddenGemResponse: HiddenGemResponse = response.body()
                Result.success(hiddenGemResponse.data)
            } else {
                Result.failure(Exception("Failed to fetch hidden gem"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check if user is trending today (variable reward).
     * Randomly features profiles to create excitement.
     */
    suspend fun getTrendingStatus(context: Context): Result<TrendingStatus> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/engagement/trending-status") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val trendingResponse: TrendingStatusResponse = response.body()
                Result.success(trendingResponse.data)
            } else {
                // If endpoint doesn't exist yet, return not trending
                Result.success(TrendingStatus(isTrending = false))
            }
        } catch (e: Exception) {
            // Graceful degradation - trending is not critical
            Result.success(TrendingStatus(isTrending = false))
        }
    }

    // ==================== Chat / Messaging APIs ====================

    suspend fun getConversations(
        context: Context,
        limit: Int = 20,
        cursor: String? = null
    ): Result<ConversationsResponse> {
        return authorizedRequestWithRefresh<ConversationsResponse>(context) { token ->
            client.get("$BASE_URL/chat/conversations") {
                header("Authorization", "Bearer $token")
                header("Cache-Control", "no-cache, no-store, must-revalidate")
                parameter("limit", limit)
                parameter("cacheBust", System.currentTimeMillis())
                cursor?.let { parameter("cursor", it) }
            }
        }
    }

    suspend fun getOrCreateConversation(context: Context, participantId: String): Result<Conversation> {
        return authorizedRequestWithRefresh<Conversation>(context) { token ->
            client.post("$BASE_URL/chat/conversations") {
                header("Authorization", "Bearer $token")
                setBody(CreateConversationRequest(participantId))
            }
        }
    }

    suspend fun getConversationStatusWithUser(
        context: Context,
        participantId: String
    ): Result<ConversationStatusResponse> {
        return try {
            val safeParticipantId = InputSecurity.identifier(participantId, "participantId")
            authorizedRequestWithRefresh<ConversationStatusResponse>(context) { token ->
                client.get("$BASE_URL/chat/users/$safeParticipantId/status") {
                    header("Authorization", "Bearer $token")
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getConversation(context: Context, conversationId: String): Result<Conversation> {
        return authorizedRequestWithRefresh<Conversation>(context) { token ->
            client.get("$BASE_URL/chat/conversations/$conversationId") {
                header("Authorization", "Bearer $token")
                header("Cache-Control", "no-cache, no-store, must-revalidate")
                parameter("cacheBust", System.currentTimeMillis())
            }
        }
    }

    suspend fun getMessages(
        context: Context,
        conversationId: String,
        limit: Int = 50,
        cursor: String? = null
    ): Result<MessagesResponse> {
        return authorizedRequestWithRefresh<MessagesResponse>(context) { token ->
            client.get("$BASE_URL/chat/conversations/$conversationId/messages") {
                header("Authorization", "Bearer $token")
                header("Cache-Control", "no-cache, no-store, must-revalidate")
                parameter("limit", limit)
                parameter("cacheBust", System.currentTimeMillis())
                cursor?.let { parameter("cursor", it) }
            }
        }
    }

    suspend fun syncChat(context: Context, since: String? = null): Result<ChatSyncResponse> {
        return authorizedRequestWithRefresh<ChatSyncResponse>(context) { token ->
            client.get("$BASE_URL/chat/sync") {
                header("Authorization", "Bearer $token")
                header("Cache-Control", "no-cache, no-store, must-revalidate")
                since?.takeIf { it.isNotBlank() }?.let { parameter("since", it) }
            }
        }
    }

    suspend fun getChatSyncCursor(context: Context, userId: String): String? {
        return context.dataStore.data.first()[stringPreferencesKey("chat_sync_cursor_$userId")]
    }

    suspend fun saveChatSyncCursor(context: Context, userId: String, cursor: String) {
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey("chat_sync_cursor_$userId")] = cursor
        }
    }

    suspend fun sendMessage(
        context: Context,
        conversationId: String,
        content: String,
        contentType: String = "text",
        mediaUrl: String? = null,
        mediaType: String? = null,
        fileName: String? = null,
        fileSize: Int? = null,
        replyToId: String? = null,
        clientMessageId: String? = null
    ): Result<Message> {
        return try {
            val safeConversationId = InputSecurity.identifier(conversationId, "conversationId")
            val safeContent = InputSecurity.text(content, "content", 2_000, allowBlank = mediaUrl != null)
            val safeContentType = InputSecurity.identifier(contentType, "contentType")
            val safeMediaUrl = mediaUrl?.let { InputSecurity.url(it, "mediaUrl") }
            val safeMediaType = mediaType?.let { InputSecurity.identifier(it, "mediaType") }
            val safeFileName = fileName?.let { InputSecurity.fileName(it, "attachment") }
            val safeFileSize = fileSize?.let { InputSecurity.boundedInt(it, "fileSize", 1, 150 * 1024 * 1024) }
            val safeReplyToId = InputSecurity.optionalIdentifier(replyToId, "replyToId")
            val safeClientMessageId = InputSecurity.optionalIdentifier(clientMessageId, "clientMessageId")
            authorizedRequestWithRefresh<Message>(context) { token ->
                client.post("$BASE_URL/chat/conversations/$safeConversationId/messages") {
                    header("Authorization", "Bearer $token")
                    setBody(SendMessageRequest(safeContent, safeContentType, safeMediaUrl, safeMediaType, safeFileName, safeFileSize, safeReplyToId, safeClientMessageId))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload a file for chat (image, video, document, or audio).
     * Backend expects multipart field "file". Returns mediaUrl, fileName, fileSize, mediaType.
     */
    suspend fun uploadChatMedia(
        context: Context,
        uri: Uri,
        fileName: String,
        mimeType: String,
        fileSize: Long? = null,
        durationMs: Long? = null
    ): Result<UploadChatMediaResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeFileName = InputSecurity.fileName(fileName, "attachment")
            val safeMimeType = InputSecurity.chatMime(mimeType)
            val safeFileSize = fileSize?.let { InputSecurity.boundedLong(it, "fileSize", 1, 150L * 1024L * 1024L) }
            val safeDurationMs = durationMs?.let { InputSecurity.boundedLong(it, "durationMs", 1, 90_000) }
            val response = uploadClient.post("$BASE_URL/chat/upload") {
                header("Authorization", "Bearer $token")
                setBody(MultiPartFormDataContent(formData {
                    append("mediaType", chatMediaTypeFromMime(safeMimeType))
                    safeDurationMs?.let { append("durationMs", it.toString()) }
                    appendInput(
                        key = "file",
                        headers = chatFilePartHeaders(safeFileName, safeMimeType),
                        size = safeFileSize
                    ) {
                        val stream = context.contentResolver.openInputStream(uri)
                            ?: throw IOException("Could not open this file")
                        stream.asSource().buffered()
                    }
                }))
            }
            if (response.status.isSuccess()) Result.success(response.body())
            else {
                val responseText = response.bodyAsText()
                val errorMessage = runCatching {
                    json.decodeFromString<ApiError>(responseText).getErrorMessage()
                }.getOrDefault(responseText.ifBlank { "Upload failed" })
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception(chatUploadErrorMessage(e), e))
        }
    }

    suspend fun uploadChatMedia(
        context: Context,
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String
    ): Result<UploadChatMediaResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeFileName = InputSecurity.fileName(fileName, "attachment")
            val safeMimeType = InputSecurity.chatMime(mimeType)
            val safeFileBytes = InputSecurity.uploadBytes(fileBytes, "file", 150 * 1024 * 1024)
            val response = uploadClient.post("$BASE_URL/chat/upload") {
                header("Authorization", "Bearer $token")
                setBody(MultiPartFormDataContent(formData {
                    append("mediaType", chatMediaTypeFromMime(safeMimeType))
                    append(
                        "file",
                        safeFileBytes,
                        chatFilePartHeaders(safeFileName, safeMimeType)
                    )
                }))
            }
            if (response.status.isSuccess()) Result.success(response.body())
            else {
                val responseText = response.bodyAsText()
                val errorMessage = runCatching {
                    json.decodeFromString<ApiError>(responseText).getErrorMessage()
                }.getOrDefault(responseText.ifBlank { "Upload failed" })
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception(chatUploadErrorMessage(e), e))
        }
    }

    private fun chatUploadErrorMessage(error: Exception): String {
        val rawMessage = error.message.orEmpty()
        return if (
            rawMessage.contains("broken pipe", ignoreCase = true) ||
            rawMessage.contains("connection reset", ignoreCase = true)
        ) {
            "Upload was interrupted. Please choose a video that is 90 seconds or less and under 150 MB."
        } else {
            rawMessage.ifBlank { "Upload failed" }
        }
    }

    private fun chatMediaTypeFromMime(mimeType: String): String = when {
        mimeType.startsWith("image/") -> "image"
        mimeType.startsWith("video/") -> "video"
        mimeType.startsWith("audio/") -> "audio"
        else -> "document"
    }

    private fun chatFilePartHeaders(fileName: String, mimeType: String): Headers {
        val safeFileName = InputSecurity.fileName(fileName, "attachment")
        val safeMimeType = InputSecurity.chatMime(mimeType)

        return Headers.build {
            append(HttpHeaders.ContentType, safeMimeType)
            append(HttpHeaders.ContentDisposition, "filename=\"$safeFileName\"")
        }
    }

    private fun reelUploadErrorMessage(error: Exception): String {
        val rawMessage = error.message.orEmpty()
        return if (
            rawMessage.contains("broken pipe", ignoreCase = true) ||
            rawMessage.contains("connection reset", ignoreCase = true)
        ) {
            "Upload was interrupted. Please choose a reel under 150 MB and try again."
        } else {
            rawMessage.ifBlank { "Failed to upload reel" }
        }
    }

    private fun reelFilePartHeaders(fileName: String, mimeType: String): Headers {
        val safeFileName = InputSecurity.fileName(fileName, "reel.mp4")
        val safeMimeType = if (mimeType.startsWith("image/")) InputSecurity.imageMime(mimeType) else InputSecurity.videoMime(mimeType)

        return Headers.build {
            append(HttpHeaders.ContentType, safeMimeType)
            append(HttpHeaders.ContentDisposition, "filename=\"$safeFileName\"")
        }
    }

    suspend fun markAsRead(context: Context, conversationId: String): Result<MarkAsReadResponse> {
        return authorizedRequestWithRefresh<MarkAsReadResponse>(context) { token ->
            client.post("$BASE_URL/chat/conversations/$conversationId/read") {
                header("Authorization", "Bearer $token")
            }
        }
    }

    suspend fun deleteMessage(
        context: Context,
        messageId: String,
        forEveryone: Boolean = false
    ): Result<Unit> {
        return authorizedUnitRequestWithRefresh(context) { token ->
            client.delete("$BASE_URL/chat/messages/$messageId") {
                header("Authorization", "Bearer $token")
                setBody(DeleteMessageRequest(forEveryone))
            }
        }
    }

    suspend fun deleteConversation(context: Context, conversationId: String): Result<Unit> {
        return authorizedUnitRequestWithRefresh(context) { token ->
            client.delete("$BASE_URL/chat/conversations/$conversationId") {
                header("Authorization", "Bearer $token")
            }
        }
    }

    suspend fun reportChat(
        context: Context,
        conversationId: String,
        reason: String,
        description: String = "",
        blockUser: Boolean = false
    ): Result<Unit> {
        return authorizedUnitRequestWithRefresh(context) { token ->
            client.post("$BASE_URL/reports/chat/$conversationId") {
                header("Authorization", "Bearer $token")
                setBody(ReportChatRequest(reason = reason, description = description, blockUser = blockUser))
            }
        }
    }

    suspend fun reportUser(
        context: Context,
        userId: String,
        reason: String,
        description: String? = null,
        blockUser: Boolean = false
    ): Result<ReportResponse> {
        return authorizedRequestWithRefresh<ReportResponse>(context) { token ->
            val safeUserId = InputSecurity.identifier(userId, "userId")
            val safeReason = InputSecurity.identifier(reason, "reason")
            val safeDescription = InputSecurity.optionalText(description, "description", 1_000)
            client.post("$BASE_URL/reports/user/$safeUserId") {
                header("Authorization", "Bearer $token")
                setBody(ReportUserRequest(safeReason, safeDescription, blockUser))
            }
        }
    }

    suspend fun getIdentityStatus(context: Context): Result<IdentityMeResponse> {
        return authorizedRequestWithRefresh<IdentityMeResponse>(context) { token ->
            client.get("$BASE_URL/identity/me") {
                header("Authorization", "Bearer $token")
            }
        }
    }

    suspend fun verifyIdentityPhone(
        context: Context,
        firebaseIdToken: String
    ): Result<IdentityMutationResponse> {
        return authorizedRequestWithRefresh<IdentityMutationResponse>(context) { token ->
            client.post("$BASE_URL/identity/phone/verify") {
                header("Authorization", "Bearer $token")
                setBody(PhoneVerifyRequest(idToken = firebaseIdToken))
            }
        }
    }

    suspend fun requestStudentEmailVerification(
        context: Context,
        studentEmail: String
    ): Result<StudentEmailRequestResponse> {
        return authorizedRequestWithRefresh<StudentEmailRequestResponse>(context) { token ->
            client.post("$BASE_URL/identity/student-email/request") {
                header("Authorization", "Bearer $token")
                setBody(StudentEmailRequestBody(studentEmail = studentEmail))
            }
        }
    }

    suspend fun confirmStudentEmailVerification(
        context: Context,
        studentEmail: String,
        code: String
    ): Result<IdentityMutationResponse> {
        return authorizedRequestWithRefresh<IdentityMutationResponse>(context) { token ->
            client.post("$BASE_URL/identity/student-email/confirm") {
                header("Authorization", "Bearer $token")
                setBody(StudentEmailConfirmRequest(studentEmail = studentEmail, code = code))
            }
        }
    }

    suspend fun claimStudentBadge(context: Context): Result<ClaimStudentBadgeResponse> {
        return authorizedRequestWithRefresh<ClaimStudentBadgeResponse>(context) { token ->
            client.post("$BASE_URL/identity/student-badge/claim") {
                header("Authorization", "Bearer $token")
                setBody(buildJsonObject {})
            }
        }
    }

    suspend fun requestIdProofUpload(context: Context): Result<IdUploadRequestResponse> {
        return authorizedRequestWithRefresh<IdUploadRequestResponse>(context) { token ->
            client.post("$BASE_URL/identity/id/request-upload") {
                header("Authorization", "Bearer $token")
                setBody(buildJsonObject {})
            }
        }
    }

    suspend fun submitIdProof(
        context: Context,
        verificationId: String,
        bytes: ByteArray,
        fileName: String,
        mimeType: String
    ): Result<IdSubmitResponse> {
        return authorizedRequestWithRefresh<IdSubmitResponse>(context) { token ->
            val safeVerificationId = InputSecurity.identifier(verificationId, "verificationId")
            val safeFileName = InputSecurity.fileName(fileName, "identity-proof")
            val safeBytes = InputSecurity.uploadBytes(bytes, "identity-proof", 8 * 1024 * 1024)
            uploadClient.post("$BASE_URL/identity/id/submit") {
                header("Authorization", "Bearer $token")
                setBody(MultiPartFormDataContent(formData {
                    append("verificationId", safeVerificationId)
                    append(
                        "evidence",
                        safeBytes,
                        Headers.build {
                            append(HttpHeaders.ContentType, mimeType)
                            append(HttpHeaders.ContentDisposition, "filename=$safeFileName")
                        }
                    )
                }))
            }
        }
    }

    suspend fun getBlockedUsers(context: Context): Result<BlocksResponse> {
        return authorizedRequestWithRefresh<BlocksResponse>(context) { token ->
            client.get("$BASE_URL/safety/blocks") {
                header("Authorization", "Bearer $token")
            }
        }
    }

    suspend fun getMyReports(
        context: Context,
        page: Int = 1,
        limit: Int = 50
    ): Result<MyReportsResponse> {
        return authorizedRequestWithRefresh<MyReportsResponse>(context) { token ->
            client.get("$BASE_URL/reports/my-reports") {
                header("Authorization", "Bearer $token")
                parameter("page", page.coerceAtLeast(1))
                parameter("limit", limit.coerceIn(1, 50))
            }
        }
    }

    suspend fun blockUser(
        context: Context,
        userId: String,
        reason: String? = null
    ): Result<BlockUserResponse> {
        return authorizedRequestWithRefresh<BlockUserResponse>(context) { token ->
            val safeUserId = InputSecurity.identifier(userId, "userId")
            client.post("$BASE_URL/safety/blocks/$safeUserId") {
                header("Authorization", "Bearer $token")
                setBody(BlockUserRequest(reason = reason))
            }
        }
    }

    suspend fun unblockUser(context: Context, userId: String): Result<Unit> {
        return authorizedUnitRequestWithRefresh(context) { token ->
            val safeUserId = InputSecurity.identifier(userId, "userId")
            client.delete("$BASE_URL/safety/blocks/$safeUserId") {
                header("Authorization", "Bearer $token")
            }
        }
    }

    suspend fun editMessage(context: Context, messageId: String, content: String): Result<Message> {
        return authorizedRequestWithRefresh<Message>(context) { token ->
            client.patch("$BASE_URL/chat/messages/$messageId") {
                header("Authorization", "Bearer $token")
                setBody(EditMessageRequest(content))
            }
        }
    }

    suspend fun addReaction(context: Context, messageId: String, emoji: String): Result<Unit> {
        return authorizedUnitRequestWithRefresh(context) { token ->
            client.post("$BASE_URL/chat/messages/$messageId/reactions") {
                header("Authorization", "Bearer $token")
                setBody(AddReactionRequest(emoji))
            }
        }
    }

    suspend fun getUnreadCount(context: Context): Result<Int> {
        return authorizedRequestWithRefresh<UnreadCountResponse>(context) { token ->
            client.get("$BASE_URL/chat/unread-count") {
                header("Authorization", "Bearer $token")
            }
        }.map { it.unreadCount }
    }

    suspend fun searchMessages(context: Context, query: String, limit: Int = 20): Result<List<Message>> {
        return authorizedRequestWithRefresh<SearchMessagesResponse>(context) { token ->
            client.get("$BASE_URL/chat/search") {
                header("Authorization", "Bearer $token")
                parameter("q", query)
                parameter("limit", limit)
            }
        }.map { it.messages }
    }

    suspend fun getMessageLimitStatus(context: Context, userId: String): Result<MessageLimitStatus> {
        return authorizedRequestWithRefresh<MessageLimitStatus>(context) { token ->
            client.get("$BASE_URL/chat/message-limit/$userId") {
                header("Authorization", "Bearer $token")
            }
        }
    }

    suspend fun getMessageRequests(context: Context, limit: Int = 20, cursor: String? = null): Result<MessageRequestsResponse> {
        return authorizedRequestWithRefresh<MessageRequestsResponse>(context) { token ->
            client.get("$BASE_URL/chat/requests") {
                header("Authorization", "Bearer $token")
                parameter("limit", limit)
                cursor?.let { parameter("cursor", it) }
            }
        }
    }

    suspend fun getMessageRequestsCount(context: Context): Result<Int> {
        return authorizedRequestWithRefresh<MessageRequestsCountResponse>(context) { token ->
            client.get("$BASE_URL/chat/requests/count") {
                header("Authorization", "Bearer $token")
            }
        }.map { it.count }
    }

    suspend fun acceptMessageRequest(context: Context, conversationId: String): Result<Conversation> {
        return authorizedRequestWithRefresh<AcceptMessageRequestResponse>(context) { token ->
            client.post("$BASE_URL/chat/requests/$conversationId/accept") {
                header("Authorization", "Bearer $token")
            }
        }.map { it.conversation }
    }

    suspend fun declineMessageRequest(context: Context, conversationId: String): Result<Unit> {
        return authorizedUnitRequestWithRefresh(context) { token ->
            client.delete("$BASE_URL/chat/requests/$conversationId") {
                header("Authorization", "Bearer $token")
            }
        }
    }
    
    // ==================== Stories APIs ====================
    
    suspend fun getMyStories(context: Context): Result<MyStoriesResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/stories/me") {
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
    
    suspend fun createStory(
        context: Context,
        mediaBytes: Pair<ByteArray, String>? = null, // Pair of (bytes, mimeType)
        mediaType: String, // TEXT, IMAGE, VIDEO
        textContent: String? = null,
        backgroundColor: String? = null,
        category: String = "GENERAL",
        visibility: String = "PUBLIC",
        linkUrl: String? = null,
        linkTitle: String? = null
    ): Result<CreateStoryResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeMediaType = InputSecurity.enumValue(mediaType, setOf("TEXT", "IMAGE", "VIDEO"), "mediaType")
            val safeCategory = InputSecurity.identifier(category, "category")
            val safeVisibility = InputSecurity.enumValue(visibility, setOf("PUBLIC", "CONNECTIONS", "CLOSE_FRIENDS", "PRIVATE"), "visibility")
            val safeTextContent = InputSecurity.optionalText(textContent, "textContent", 1_000)
            val safeBackgroundColor = backgroundColor?.let { InputSecurity.text(it, "backgroundColor", 32, allowBlank = true) }
            val safeLinkUrl = linkUrl?.let { InputSecurity.url(it, "linkUrl") }
            val safeLinkTitle = InputSecurity.optionalText(linkTitle, "linkTitle", 140)
            val safeMediaBytes = mediaBytes?.let { (bytes, mimeType) ->
                val safeMime = if (mimeType.startsWith("video/")) InputSecurity.videoMime(mimeType) else InputSecurity.imageMime(mimeType)
                InputSecurity.uploadBytes(bytes, "story media", 50 * 1024 * 1024) to safeMime
            }
            val response = client.submitFormWithBinaryData(
                url = "$BASE_URL/stories",
                formData = formData {
                    append("mediaType", safeMediaType)
                    append("category", safeCategory)
                    append("visibility", safeVisibility)
                    safeTextContent?.let { append("textContent", it) }
                    safeBackgroundColor?.let { append("backgroundColor", it) }
                    safeLinkUrl?.let { append("linkUrl", it) }
                    safeLinkTitle?.let { append("linkTitle", it) }
                    safeMediaBytes?.let { (bytes, mimeType) ->
                        val extension = when {
                            mimeType.contains("video") -> ".mp4"
                            mimeType.contains("png") -> ".png"
                            else -> ".jpg"
                        }
                        append("media", bytes, Headers.build {
                            append(HttpHeaders.ContentType, mimeType)
                            append(HttpHeaders.ContentDisposition, "filename=\"story$extension\"")
                        })
                    }
                }
            ) {
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
    
    suspend fun viewStory(context: Context, storyId: String): Result<ViewStoryResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeStoryId = InputSecurity.identifier(storyId, "storyId")
            val response = client.post("$BASE_URL/stories/$safeStoryId/view") {
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
    
    suspend fun reactToStory(context: Context, storyId: String, reactionType: String = "LIKE"): Result<ReactToStoryResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeStoryId = InputSecurity.identifier(storyId, "storyId")
            val safeReactionType = InputSecurity.text(reactionType, "reactionType", 40)
            val response = client.post("$BASE_URL/stories/$safeStoryId/react") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(mapOf("reactionType" to safeReactionType))
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
    
    suspend fun deleteStory(context: Context, storyId: String): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeStoryId = InputSecurity.identifier(storyId, "storyId")
            val response = client.delete("$BASE_URL/stories/$safeStoryId") {
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
    
    suspend fun getStoryViewers(context: Context, storyId: String, cursor: String? = null, limit: Int = 20): Result<StoryViewersResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeStoryId = InputSecurity.identifier(storyId, "storyId")
            val safeCursor = InputSecurity.optionalPaginationCursor(cursor, "cursor")
            val safeLimit = InputSecurity.boundedInt(limit, "limit", 1, 100)
            val response = client.get("$BASE_URL/stories/$safeStoryId/viewers") {
                header("Authorization", "Bearer $token")
                safeCursor?.let { parameter("cursor", it) }
                parameter("limit", safeLimit.toString())
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
    
    suspend fun replyToStory(context: Context, storyId: String, content: String): Result<ReplyToStoryResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeStoryId = InputSecurity.identifier(storyId, "storyId")
            val safeContent = InputSecurity.text(content, "content", 1_000)
            val response = client.post("$BASE_URL/stories/$safeStoryId/reply") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(mapOf("content" to safeContent))
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
    
    // ==================== Reels APIs ====================
    
    suspend fun getReelsFeed(
        context: Context,
        cursor: String? = null,
        limit: Int = 10,
        mode: String = "foryou",
        adItemOffset: Int = 0
    ): Result<ReelsFeedResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeCursor = InputSecurity.optionalPaginationCursor(cursor, "cursor")
            val safeLimit = InputSecurity.boundedInt(limit, "limit", 1, 30)
            val safeMode = InputSecurity.identifier(mode, "mode")
            val safeAdItemOffset = InputSecurity.boundedInt(adItemOffset, "adItemOffset", 0, 10_000)
            val response = client.get("$BASE_URL/reels/feed") {
                header("Authorization", "Bearer $token")
                header("Cache-Control", "no-cache, no-store, must-revalidate")
                safeCursor?.let { parameter("cursor", it) }
                parameter("limit", safeLimit.toString())
                parameter("mode", safeMode)
                parameter("adSessionId", managedAdSessionId)
                parameter("adItemOffset", safeAdItemOffset)
                parameter("_t", System.currentTimeMillis()) // Cache buster
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
    
    suspend fun getTrendingReels(context: Context, hours: Int = 48, limit: Int = 15): Result<ReelsFeedResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeHours = InputSecurity.boundedInt(hours, "hours", 1, 24 * 30)
            val safeLimit = InputSecurity.boundedInt(limit, "limit", 1, 50)
            val response = client.get("$BASE_URL/reels/trending") {
                header("Authorization", "Bearer $token")
                header("Cache-Control", "no-cache, no-store, must-revalidate")
                parameter("hours", safeHours.toString())
                parameter("limit", safeLimit.toString())
                parameter("_t", System.currentTimeMillis()) // Cache buster
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
    
    suspend fun getReel(context: Context, reelId: String): Result<Reel> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeReelId = InputSecurity.identifier(reelId, "reelId")
            val response = client.get("$BASE_URL/reels/$safeReelId") {
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

    suspend fun createReel(
        context: Context,
        videoUri: Uri,
        videoFileName: String,
        videoMimeType: String,
        videoSize: Long? = null,
        thumbnailUri: Uri? = null,
        thumbnailFileName: String? = null,
        thumbnailMimeType: String? = null,
        thumbnailSize: Long? = null,
        title: String = "",
        caption: String = "",
        hashtags: List<String> = emptyList(),
        category: String? = null,
        visibility: String = "public",
        allowComments: Boolean = true,
        allowDuets: Boolean = true,
        allowStitch: Boolean = true,
        allowDownload: Boolean = true,
        allowSharing: Boolean = true,
        muteOriginalAudio: Boolean = false,
        saveAsDraft: Boolean = false
    ): Result<Reel> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeTitle = InputSecurity.text(title, "title", 120, allowBlank = true)
            val safeCaption = InputSecurity.text(caption, "caption", 2_000, allowBlank = true)
            val safeHashtags = InputSecurity.sanitizeList(hashtags, "hashtags", 30, 60)
            val safeCategory = InputSecurity.optionalText(category, "category", 80)
            val safeVisibility = InputSecurity.enumValue(visibility, setOf("PUBLIC", "CONNECTIONS", "PRIVATE"), "visibility").lowercase()
            val safeVideoFileName = InputSecurity.fileName(videoFileName, "reel.mp4")
            val safeVideoMimeType = InputSecurity.videoMime(videoMimeType.ifBlank { "video/mp4" })
            val safeVideoSize = videoSize?.let { InputSecurity.boundedLong(it, "videoSize", 1, 150L * 1024L * 1024L) }
            val safeThumbnailFileName = thumbnailFileName?.let { InputSecurity.fileName(it, "thumbnail.jpg") } ?: "thumbnail.jpg"
            val safeThumbnailMimeType = thumbnailMimeType?.takeIf { it.isNotBlank() }?.let { InputSecurity.imageMime(it) } ?: "image/jpeg"
            val safeThumbnailSize = thumbnailSize?.let { InputSecurity.boundedLong(it, "thumbnailSize", 1, 10L * 1024L * 1024L) }
            val response = uploadClient.post("$BASE_URL/reels") {
                header("Authorization", "Bearer $token")
                setBody(MultiPartFormDataContent(formData {
                    append("title", safeTitle)
                    append("caption", safeCaption)
                    append("hashtags", json.encodeToString(safeHashtags))
                    safeCategory?.let { append("category", it) }
                    append("visibility", safeVisibility)
                    append("allowComments", allowComments.toString())
                    append("allowDuets", allowDuets.toString())
                    append("allowStitch", allowStitch.toString())
                    append("allowDownload", allowDownload.toString())
                    append("allowSharing", allowSharing.toString())
                    append("muteOriginalAudio", muteOriginalAudio.toString())
                    append("saveAsDraft", saveAsDraft.toString())
                    appendInput(
                        key = "video",
                        headers = reelFilePartHeaders(safeVideoFileName, safeVideoMimeType),
                        size = safeVideoSize
                    ) {
                        val stream = context.contentResolver.openInputStream(videoUri)
                            ?: throw IOException("Could not open this video")
                        stream.asSource().buffered()
                    }
                    if (thumbnailUri != null) {
                        appendInput(
                            key = "thumbnail",
                            headers = reelFilePartHeaders(
                                safeThumbnailFileName,
                                safeThumbnailMimeType
                            ),
                            size = safeThumbnailSize
                        ) {
                            val stream = context.contentResolver.openInputStream(thumbnailUri)
                                ?: throw IOException("Could not open this thumbnail")
                            stream.asSource().buffered()
                        }
                    }
                }))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val responseText = response.bodyAsText()
                val errorMessage = runCatching {
                    json.decodeFromString<ApiError>(responseText).getErrorMessage()
                }.getOrDefault(responseText.ifBlank { "Failed to upload reel" })
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception(reelUploadErrorMessage(e), e))
        }
    }

    suspend fun getMyDraftReels(context: Context, cursor: String? = null, limit: Int = 20): Result<ReelsFeedResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeCursor = InputSecurity.optionalPaginationCursor(cursor, "cursor")
            val safeLimit = InputSecurity.boundedInt(limit, "limit", 1, 50)
            val response = client.get("$BASE_URL/reels/drafts") {
                header("Authorization", "Bearer $token")
                header("Cache-Control", "no-cache, no-store, must-revalidate")
                safeCursor?.let { parameter("cursor", it) }
                parameter("limit", safeLimit.toString())
                parameter("_t", System.currentTimeMillis())
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

    suspend fun publishDraftReel(context: Context, reelId: String): Result<Reel> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeReelId = InputSecurity.identifier(reelId, "reelId")
            val response = client.post("$BASE_URL/reels/$safeReelId/publish") {
                header("Authorization", "Bearer $token")
                setBody(emptyMap<String, String>())
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

    suspend fun getUserSavedReels(context: Context, userId: String, cursor: String? = null, limit: Int = 20): Result<ReelsFeedResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeUserId = InputSecurity.identifier(userId, "userId")
            val safeCursor = InputSecurity.optionalPaginationCursor(cursor, "cursor")
            val safeLimit = InputSecurity.boundedInt(limit, "limit", 1, 50)
            val response = client.get("$BASE_URL/reels/user/$safeUserId/saved") {
                header("Authorization", "Bearer $token")
                safeCursor?.let { parameter("cursor", it) }
                parameter("limit", safeLimit.toString())
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

    suspend fun getMySavedReels(context: Context, cursor: String? = null, limit: Int = 20): Result<ReelsFeedResponse> {
        val userId = getCurrentUserId(context) ?: return Result.failure(Exception("Not logged in"))
        return getUserSavedReels(context, userId, cursor, limit)
    }
    
    suspend fun toggleReelLike(context: Context, reelId: String): Result<ReelLikeResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeReelId = InputSecurity.identifier(reelId, "reelId")
            val response = client.post("$BASE_URL/reels/$safeReelId/like") {
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
    
    suspend fun toggleReelSave(context: Context, reelId: String): Result<ReelSaveResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeReelId = InputSecurity.identifier(reelId, "reelId")
            val response = client.post("$BASE_URL/reels/$safeReelId/save") {
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

    suspend fun shareReel(
        context: Context,
        reelId: String,
        shareType: String = "copy_link",
        platform: String? = null,
        recipientId: String? = null
    ): Result<ShareResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeReelId = InputSecurity.identifier(reelId, "reelId")
            val safeShareType = InputSecurity.identifier(shareType, "shareType")
            val safePlatform = InputSecurity.optionalIdentifier(platform, "platform")
            val safeRecipientId = InputSecurity.optionalIdentifier(recipientId, "recipientId")
            val body = buildMap<String, String> {
                put("shareType", safeShareType)
                safePlatform?.let { put("platform", it) }
                safeRecipientId?.let { put("recipientId", it) }
            }
            val response = client.post("$BASE_URL/reels/$safeReelId/share") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val responseText = response.bodyAsText()
                val errorMessage = runCatching {
                    json.decodeFromString<ApiError>(responseText).getErrorMessage()
                }.getOrDefault(responseText.ifBlank { "Failed to share reel" })
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun trackReelView(context: Context, reelId: String, watchTimeMs: Long, completed: Boolean, source: String = "feed"): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeReelId = InputSecurity.identifier(reelId, "reelId")
            val safeWatchTimeMs = InputSecurity.boundedLong(watchTimeMs, "watchTimeMs", 0, 24L * 60L * 60L * 1000L)
            val safeSource = InputSecurity.identifier(source, "source")
            val response = client.post("$BASE_URL/reels/$safeReelId/view") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(ReelViewRequest(safeWatchTimeMs, completed, safeSource))
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to track view"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun trackManagedAdImpression(
        context: Context,
        campaignId: String,
        placement: String,
        slotKey: String
    ): Result<Unit> = trackManagedAdEvent(context, campaignId, placement, slotKey, "impression")

    suspend fun trackManagedAdClick(
        context: Context,
        campaignId: String,
        placement: String,
        slotKey: String
    ): Result<Unit> = trackManagedAdEvent(context, campaignId, placement, slotKey, "click")

    private suspend fun trackManagedAdEvent(
        context: Context,
        campaignId: String,
        placement: String,
        slotKey: String,
        eventType: String
    ): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeCampaignId = InputSecurity.identifier(campaignId, "campaignId")
            val safePlacement = InputSecurity.enumValue(placement, setOf("FEED", "REELS"), "placement").lowercase()
            val safeSlotKey = InputSecurity.text(slotKey, "slotKey", 64)
            val response = client.post("$BASE_URL/ads/$safeCampaignId/$eventType") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(ManagedAdEventRequest(safePlacement, safeSlotKey, managedAdSessionId))
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to track ad event"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getReelComments(
        context: Context,
        reelId: String,
        cursor: String? = null,
        limit: Int = 20,
        parentId: String? = null,
        highlightCommentId: String? = null
    ): Result<ReelCommentsResponse> {
        return try {
            val token = getToken(context)
            val safeReelId = InputSecurity.identifier(reelId, "reelId")
            val safeCursor = InputSecurity.optionalPaginationCursor(cursor, "cursor")
            val safeParentId = InputSecurity.optionalIdentifier(parentId, "parentId")
            val safeHighlightCommentId = InputSecurity.optionalIdentifier(highlightCommentId, "highlightCommentId")
            val safeLimit = InputSecurity.boundedInt(limit, "limit", 1, 100)
            val response = client.get("$BASE_URL/reels/$safeReelId/comments") {
                token?.let { header("Authorization", "Bearer $it") }
                safeCursor?.let { parameter("cursor", it) }
                safeParentId?.let { parameter("parentId", it) }
                safeHighlightCommentId?.let { parameter("highlightCommentId", it) }
                parameter("limit", safeLimit.toString())
            }
            val responseText = response.bodyAsText()

            if (response.status.isSuccess()) {
                val parsed = runCatching {
                    json.decodeFromString<ReelCommentsResponse>(responseText)
                }.recoverCatching {
                    // Some deployments may wrap payload under `data`.
                    val root = json.parseToJsonElement(responseText).jsonObject
                    val wrapped = root["data"] ?: root["result"] ?: root["payload"]
                        ?: error("Missing comments payload")
                    json.decodeFromJsonElement<ReelCommentsResponse>(wrapped)
                }

                parsed.fold(
                    onSuccess = { Result.success(it) },
                    onFailure = { Result.failure(Exception("Failed to parse comments response")) }
                )
            } else {
                val errorMessage = runCatching {
                    json.decodeFromString<ApiError>(responseText).getErrorMessage()
                }.getOrNull() ?: "Failed to fetch comments"

                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createReelComment(
        context: Context,
        reelId: String,
        content: String,
        parentId: String? = null,
        mentions: List<String> = emptyList()
    ): Result<ReelComment> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeReelId = InputSecurity.identifier(reelId, "reelId")
            val safeContent = InputSecurity.text(content, "content", 1_000)
            val safeParentId = InputSecurity.optionalIdentifier(parentId, "parentId")
            val safeMentions = InputSecurity.sanitizeList(mentions, "mentions", 30, 80)
            val body = buildJsonObject {
                put("content", safeContent)
                safeParentId?.let { put("parentId", it) }
                putJsonArray("mentions") {
                    safeMentions.forEach { mention -> add(JsonPrimitive(mention)) }
                }
            }
            val response = client.post("$BASE_URL/reels/$safeReelId/comments") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(body)
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
    
    suspend fun voteReelPoll(context: Context, reelId: String, optionId: Int): Result<ReelPollVoteResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeReelId = InputSecurity.identifier(reelId, "reelId")
            val safeOptionId = InputSecurity.boundedInt(optionId, "optionId", 0, 100)
            val response = client.post("$BASE_URL/reels/$safeReelId/poll/vote") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(mapOf("optionId" to safeOptionId))
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
    
    // ==================== Retention Feature APIs ====================
    
    /**
     * Get weekly goals (Zeigarnik Effect)
     * Shows incomplete progress to motivate completion
     */
    suspend fun getWeeklyGoals(context: Context): Result<WeeklyGoalsData> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/engagement/weekly-goals") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val goalsResponse: WeeklyGoalsResponse = response.body()
                Result.success(goalsResponse.data)
            } else {
                // Return default if endpoint doesn't exist
                Result.success(WeeklyGoalsData())
            }
        } catch (e: Exception) {
            Result.success(WeeklyGoalsData())
        }
    }
    
    /**
     * Get top networkers leaderboard (Social Proof)
     * @param period "week" or "month"
     */
    suspend fun getLeaderboard(context: Context, period: String = "week", limit: Int = 100): Result<LeaderboardData> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/engagement/leaderboard") {
                header("Authorization", "Bearer $token")
                parameter("period", period)
                parameter("limit", limit.coerceIn(1, 100))
            }
            if (response.status.isSuccess()) {
                val leaderboardResponse: LeaderboardResponse = response.body()
                Result.success(leaderboardResponse.data.copy(period = period))
            } else {
                Result.success(LeaderboardData(period = period))
            }
        } catch (e: Exception) {
            Result.success(LeaderboardData(period = period))
        }
    }
    
    /**
     * Get live activity count (FOMO / Social Proof)
     * Shows how many people are currently networking
     */
    suspend fun getLiveActivity(context: Context): Result<LiveActivityData> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/engagement/live-activity") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val activityResponse: LiveActivityResponse = response.body()
                Result.success(activityResponse.data)
            } else {
                Result.success(LiveActivityData())
            }
        } catch (e: Exception) {
            Result.success(LiveActivityData())
        }
    }
    
    /**
     * Get session summary (Peak-End Rule)
     * Shows accomplishments when leaving the app
     */
    suspend fun getSessionSummary(context: Context): Result<SessionSummaryData> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/engagement/session-summary") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val summaryResponse: SessionSummaryResponse = response.body()
                Result.success(summaryResponse.data)
            } else {
                Result.success(SessionSummaryData())
            }
        } catch (e: Exception) {
            Result.success(SessionSummaryData())
        }
    }
    
    /**
     * Get connection celebration data (Peak Moment)
     * For full-screen celebration when connection is accepted
     */
    suspend fun getConnectionCelebration(context: Context, connectionId: String): Result<ConnectionCelebrationData> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/engagement/celebration/$connectionId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val celebrationResponse: ConnectionCelebrationResponse = response.body()
                Result.success(celebrationResponse.data)
            } else {
                Result.success(ConnectionCelebrationData())
            }
        } catch (e: Exception) {
            Result.success(ConnectionCelebrationData())
        }
    }
    
    /**
     * Get connection request limit (Scarcity)
     * Shows how many free-tier requests remain this month
     */
    suspend fun getConnectionLimit(context: Context): Result<ConnectionLimitData> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/engagement/connection-limit") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val limitResponse: ConnectionLimitResponse = response.body()
                Result.success(limitResponse.data)
            } else {
                // Default: unlimited if endpoint doesn't exist
                Result.success(ConnectionLimitData(unlimitedRequests = true))
            }
        } catch (e: Exception) {
            Result.success(ConnectionLimitData(unlimitedRequests = true))
        }
    }
    
    /**
     * Get engagement dashboard (all retention data at once)
     */
    suspend fun getEngagementDashboard(context: Context): Result<EngagementDashboardData> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/engagement/dashboard") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val dashboardResponse: EngagementDashboardResponse = response.body()
                Result.success(dashboardResponse.data)
            } else {
                Result.success(EngagementDashboardData())
            }
        } catch (e: Exception) {
            Result.success(EngagementDashboardData())
        }
    }
    
    // ==================== Onboarding APIs ====================
    
    /**
     * Get current user's onboarding status and data
     */
    suspend fun getOnboarding(context: Context): Result<OnboardingResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/onboarding") {
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
     * Update onboarding step data
     * Step 0: Profile (college, primaryGoal, lookingFor)
     * Step 1: Interests (interests, canTeach)
     */
    suspend fun updateOnboardingStep(
        context: Context,
        step: Int,
        data: Map<String, Any?>
    ): Result<OnboardingStepResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/onboarding/step") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    put("step", step)
                    putJsonObject("data") {
                        data.forEach { (key, value) ->
                            when (value) {
                                is String -> put(key, value)
                                is Int -> put(key, value)
                                is Boolean -> put(key, value)
                                is List<*> -> putJsonArray(key) {
                                    value.filterIsInstance<String>().forEach { 
                                        add(kotlinx.serialization.json.JsonPrimitive(it))
                                    }
                                }
                                // Skip null values - server will handle defaults
                                null -> { /* skip */ }
                            }
                        }
                    }
                })
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
     * Mark onboarding as completed
     */
    suspend fun completeOnboarding(context: Context): Result<OnboardingCompleteResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/onboarding/complete") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject { })
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
     * Get initial matches based on onboarding data
     */
    suspend fun getOnboardingMatches(context: Context): Result<OnboardingMatchesResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/onboarding/matches") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                // Return empty matches if endpoint fails
                Result.success(OnboardingMatchesResponse(emptyList()))
            }
        } catch (e: Exception) {
            Result.success(OnboardingMatchesResponse(emptyList()))
        }
    }
    
    // ==================== Projects APIs ====================
    
    /**
     * Get user's projects
     */
    suspend fun getUserProjects(context: Context, userId: String): Result<List<Project>> {
        return try {
            val token = getToken(context)
            val response = client.get("$BASE_URL/users/$userId/projects") {
                token?.let { header("Authorization", "Bearer $it") }
            }
            if (response.status.isSuccess()) {
                val projectsResponse: ProjectsResponse = response.body()
                Result.success(projectsResponse.projects)
            } else {
                Result.failure(Exception("Failed to get projects"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create a new project
     */
    suspend fun createProject(context: Context, input: ProjectInput): Result<Project> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeInput = sanitizeProjectInput(input)
            val response = client.post("$BASE_URL/users/me/projects") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(safeInput)
            }
            if (response.status.isSuccess()) {
                val project: Project = response.body()
                Result.success(project)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update an existing project
     */
    suspend fun updateProject(context: Context, projectId: String, input: ProjectInput): Result<Project> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeProjectId = InputSecurity.identifier(projectId, "projectId")
            val safeInput = sanitizeProjectInput(input)
            val response = client.put("$BASE_URL/users/me/projects/$safeProjectId") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(safeInput)
            }
            if (response.status.isSuccess()) {
                val project: Project = response.body()
                Result.success(project)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete a project
     */
    suspend fun deleteProject(context: Context, projectId: String): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.delete("$BASE_URL/users/me/projects/$projectId") {
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
     * Toggle project featured status
     */
    suspend fun toggleProjectFeatured(context: Context, projectId: String): Result<Boolean> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/users/me/projects/$projectId/feature") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val featureResponse: FeatureProjectResponse = response.body()
                Result.success(featureResponse.featured)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Upload project image
     */
    suspend fun uploadProjectImage(
        context: Context,
        imageBytes: ByteArray,
        filename: String = "project.jpg"
    ): Result<String> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val image = imageUploadPayload(
                InputSecurity.uploadBytes(imageBytes, "project image", 10 * 1024 * 1024),
                filename,
                "project"
            )
            val response = client.post("$BASE_URL/upload/project") {
                header("Authorization", "Bearer $token")
                setBody(MultiPartFormDataContent(formData {
                    append(
                        "image",
                        image.bytes,
                        Headers.build {
                            append(HttpHeaders.ContentType, image.mimeType)
                            append(HttpHeaders.ContentDisposition, "filename=${image.filename}")
                        }
                    )
                }))
            }
            if (response.status.isSuccess()) {
                val result: ProjectImageUploadResponse = response.body()
                Result.success(result.url ?: result.imageUrl ?: "")
            } else {
                Result.failure(Exception("Failed to upload project image"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadLogoImage(
        context: Context,
        imageBytes: ByteArray,
        filename: String = "logo.jpg"
    ): Result<String> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val image = imageUploadPayload(
                InputSecurity.uploadBytes(imageBytes, "logo image", 10 * 1024 * 1024),
                filename,
                "logo"
            )
            val response = client.post("$BASE_URL/upload/logo") {
                header("Authorization", "Bearer $token")
                setBody(MultiPartFormDataContent(formData {
                    append(
                        "image",
                        image.bytes,
                        Headers.build {
                            append(HttpHeaders.ContentType, image.mimeType)
                            append(HttpHeaders.ContentDisposition, "filename=${image.filename}")
                        }
                    )
                }))
            }
            if (response.status.isSuccess()) {
                val result: LogoUploadResponse = response.body()
                val logoUrl = result.logoUrl ?: result.url
                if (logoUrl.isNullOrBlank()) {
                    Result.failure(Exception("Logo upload returned no URL"))
                } else {
                    Result.success(logoUrl)
                }
            } else {
                val error = runCatching { response.body<ApiError>() }.getOrNull()
                Result.failure(Exception(error?.getErrorMessage() ?: "Failed to upload logo"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== Experience API ====================
    
    /**
     * Get user experiences
     */
    suspend fun getUserExperiences(context: Context, userId: String): Result<List<Experience>> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/users/$userId/experiences") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val experiences: List<Experience> = response.body()
                Result.success(experiences)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create a new experience
     */
    suspend fun createExperience(context: Context, input: ExperienceInput): Result<Experience> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeInput = sanitizeExperienceInput(input)
            val response = client.post("$BASE_URL/users/me/experiences") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(safeInput)
            }
            if (response.status.isSuccess()) {
                val experience: Experience = response.body()
                Result.success(experience)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update an existing experience
     */
    suspend fun updateExperience(context: Context, experienceId: String, input: ExperienceInput): Result<Experience> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeExperienceId = InputSecurity.identifier(experienceId, "experienceId")
            val safeInput = sanitizeExperienceInput(input)
            val response = client.put("$BASE_URL/users/me/experiences/$safeExperienceId") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(safeInput)
            }
            if (response.status.isSuccess()) {
                val experience: Experience = response.body()
                Result.success(experience)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete an experience
     */
    suspend fun deleteExperience(context: Context, experienceId: String): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.delete("$BASE_URL/users/me/experiences/$experienceId") {
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
    
    // ==================== Education API ====================
    
    /**
     * Get user education entries
     */
    suspend fun getUserEducation(context: Context, userId: String): Result<List<Education>> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/users/$userId/education") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val education: List<Education> = response.body()
                Result.success(education)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create a new education entry
     */
    suspend fun createEducation(context: Context, input: EducationInput): Result<Education> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeInput = sanitizeEducationInput(input)
            val response = client.post("$BASE_URL/users/me/education") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(safeInput)
            }
            if (response.status.isSuccess()) {
                val education: Education = response.body()
                Result.success(education)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update an existing education entry
     */
    suspend fun updateEducation(context: Context, educationId: String, input: EducationInput): Result<Education> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeEducationId = InputSecurity.identifier(educationId, "educationId")
            val safeInput = sanitizeEducationInput(input)
            val response = client.put("$BASE_URL/users/me/education/$safeEducationId") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(safeInput)
            }
            if (response.status.isSuccess()) {
                val education: Education = response.body()
                Result.success(education)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete an education entry
     */
    suspend fun deleteEducation(context: Context, educationId: String): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.delete("$BASE_URL/users/me/education/$educationId") {
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
    
    // ==================== Certificates API ====================
    
    /**
     * Get user certificates
     */
    suspend fun getUserCertificates(context: Context, userId: String): Result<List<Certificate>> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/users/$userId/certificates") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val certificates: List<Certificate> = response.body()
                Result.success(certificates)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create a new certificate
     */
    suspend fun createCertificate(context: Context, input: CertificateInput): Result<Certificate> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeInput = sanitizeCertificateInput(input)
            val response = client.post("$BASE_URL/users/me/certificates") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(safeInput)
            }
            if (response.status.isSuccess()) {
                val certificate: Certificate = response.body()
                Result.success(certificate)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update an existing certificate
     */
    suspend fun updateCertificate(context: Context, certificateId: String, input: CertificateInput): Result<Certificate> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeCertificateId = InputSecurity.identifier(certificateId, "certificateId")
            val safeInput = sanitizeCertificateInput(input)
            val response = client.put("$BASE_URL/users/me/certificates/$safeCertificateId") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(safeInput)
            }
            if (response.status.isSuccess()) {
                val certificate: Certificate = response.body()
                Result.success(certificate)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete a certificate
     */
    suspend fun deleteCertificate(context: Context, certificateId: String): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.delete("$BASE_URL/users/me/certificates/$certificateId") {
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
     * Upload certificate image (multipart form data)
     * Returns the URL of the uploaded certificate image
     */
    suspend fun uploadCertificateImage(
        context: Context,
        imageBytes: ByteArray,
        filename: String = "certificate.jpg"
    ): Result<String> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val image = imageUploadPayload(
                InputSecurity.uploadBytes(imageBytes, "certificate image", 10 * 1024 * 1024),
                filename,
                "certificate"
            )
            val response = client.post("$BASE_URL/upload/certificate") {
                header("Authorization", "Bearer $token")
                setBody(MultiPartFormDataContent(formData {
                    append(
                        "image",
                        image.bytes,
                        Headers.build {
                            append(HttpHeaders.ContentType, image.mimeType)
                            append(HttpHeaders.ContentDisposition, "filename=${image.filename}")
                        }
                    )
                }))
            }
            if (response.status.isSuccess()) {
                val result: CertificateUploadResponse = response.body()
                Result.success(result.certificateUrl ?: result.url ?: "")
            } else {
                Result.failure(Exception("Failed to upload certificate image"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== Achievements API ====================
    
    /**
     * Get user achievements
     */
    suspend fun getUserAchievements(context: Context, userId: String): Result<List<Achievement>> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/users/$userId/achievements") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val achievements: List<Achievement> = response.body()
                Result.success(achievements)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create a new achievement
     */
    suspend fun createAchievement(context: Context, input: AchievementInput): Result<Achievement> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeInput = sanitizeAchievementInput(input)
            val response = client.post("$BASE_URL/users/me/achievements") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(safeInput)
            }
            if (response.status.isSuccess()) {
                val achievement: Achievement = response.body()
                Result.success(achievement)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update an existing achievement
     */
    suspend fun updateAchievement(context: Context, achievementId: String, input: AchievementInput): Result<Achievement> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safeAchievementId = InputSecurity.identifier(achievementId, "achievementId")
            val safeInput = sanitizeAchievementInput(input)
            val response = client.put("$BASE_URL/users/me/achievements/$safeAchievementId") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(safeInput)
            }
            if (response.status.isSuccess()) {
                val achievement: Achievement = response.body()
                Result.success(achievement)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete an achievement
     */
    suspend fun deleteAchievement(context: Context, achievementId: String): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.delete("$BASE_URL/users/me/achievements/$achievementId") {
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
    
    // ==================== Device Token / Push Notification APIs ====================
    
    /**
     * Register FCM device token with backend for push notifications
     */
    suspend fun registerDeviceToken(context: Context, token: String, platform: String): Result<Unit> {
        return try {
            val authToken = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/devices/register") {
                header("Authorization", "Bearer $authToken")
                contentType(ContentType.Application.Json)
                setBody(mapOf("token" to token, "platform" to platform))
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to register device token"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Register FCM device token asynchronously (fire and forget)
     */
    fun registerDeviceTokenAsync(context: Context, token: String, platform: String) {
        backgroundScope.launch {
            registerDeviceToken(context, token, platform)
                .onSuccess {
                    if (BuildConfig.DEBUG) println("Device token registered successfully")
                }
                .onFailure { e ->
                    if (BuildConfig.DEBUG) println("Failed to register device token: ${e.message}")
                }
        }
    }
    
    /**
     * Unregister device token (e.g., on logout)
     */
    suspend fun unregisterDeviceToken(context: Context, token: String): Result<Unit> {
        return try {
            val authToken = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.delete("$BASE_URL/devices/unregister") {
                header("Authorization", "Bearer $authToken")
                contentType(ContentType.Application.Json)
                setBody(mapOf("token" to token))
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to unregister device token"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== Notification APIs ====================
    
    /**
     * Get notifications with pagination
     */
    suspend fun getNotifications(
        context: Context,
        cursor: String? = null,
        limit: Int = 20,
        unreadOnly: Boolean = false,
        afterCursor: String? = null
    ): Result<NotificationsResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/notifications") {
                header("Authorization", "Bearer $token")
                cursor?.let { parameter("cursor", it) }
                afterCursor?.let { parameter("afterCursor", it) }
                parameter("limit", limit)
                if (unreadOnly) parameter("unreadOnly", "true")
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
     * Get unread notification count
     */
    suspend fun getNotificationUnreadCount(context: Context): Result<Int> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/notifications/unread-count") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val body: Map<String, Int> = response.body()
                Result.success(body["count"] ?: 0)
            } else {
                Result.failure(Exception("Failed to get unread count"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Mark notifications as read
     */
    suspend fun markNotificationsAsRead(context: Context, notificationIds: List<String>): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/notifications/read") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(mapOf("notificationIds" to notificationIds))
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to mark as read"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun respondToPostCollabInvite(
        context: Context,
        postId: String,
        accept: Boolean
    ): Result<PostCollabResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val safePostId = InputSecurity.identifier(postId, "postId")
            val response = client.post("$BASE_URL/posts/$safePostId/collaborators/respond") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(mapOf("action" to if (accept) "accept" else "reject"))
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
     * Mark all notifications as read
     */
    suspend fun markAllNotificationsAsRead(context: Context): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/notifications/read-all") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to mark all as read"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get notification settings/preferences
     */
    suspend fun getNotificationSettings(context: Context): Result<NotificationSettings> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/notifications/settings") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                // Return defaults if endpoint fails
                Result.success(NotificationSettings())
            }
        } catch (e: Exception) {
            Result.success(NotificationSettings())
        }
    }
    
    /**
     * Update notification settings/preferences
     */
    suspend fun updateNotificationSettings(context: Context, settings: NotificationSettings): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.put("$BASE_URL/notifications/settings") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(settings)
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update settings"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Crossed Paths v1 ====================
    private suspend inline fun <reified T> proximityRequest(context: Context, crossinline block: suspend (String) -> HttpResponse): Result<T> = try {
        val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
        val response = block(token)
        if (response.status.isSuccess()) Result.success(response.body<ProximityEnvelope<T>>().data)
        else {
            val details = runCatching { response.body<ProximityErrorEnvelope>().error }.getOrNull()
                ?: ProximityErrorDetails("PROXIMITY_SERVICE_DEGRADED",
                    "Crossed Paths request failed (${response.status.value})", response.status.value >= 500)
            Result.failure(ProximityApiException(details, response.status.value))
        }
    } catch (error: Exception) { Result.failure(error) }

    suspend fun getProximityCapabilities(context: Context) = proximityRequest<ProximityCapabilities>(context) { token ->
        client.get("$BASE_URL/proximity/v1/capabilities") { header("Authorization", "Bearer $token") }
    }
    suspend fun getProximitySettings(context: Context) = proximityRequest<ProximitySettings>(context) { token ->
        client.get("$BASE_URL/proximity/v1/settings") { header("Authorization", "Bearer $token") }
    }
    suspend fun updateProximitySettings(context: Context, settings: ProximitySettings) = proximityRequest<ProximitySettings>(context) { token ->
        client.put("$BASE_URL/proximity/v1/settings") { header("Authorization", "Bearer $token"); contentType(ContentType.Application.Json); setBody(settings) }
    }
    suspend fun startProximitySession(context: Context, request: StartProximitySessionRequest) = proximityRequest<ProximitySessionStart>(context) { token ->
        client.post("$BASE_URL/proximity/v1/sessions") { header("Authorization", "Bearer $token"); contentType(ContentType.Application.Json); setBody(request) }
    }
    suspend fun getCurrentProximitySession(context: Context) = proximityRequest<ProximitySessionState?>(context) { token ->
        client.get("$BASE_URL/proximity/v1/sessions/current") { header("Authorization", "Bearer $token") }
    }
    suspend fun resumeProximitySession(context: Context, sessionId: String) = proximityRequest<ProximitySessionState>(context) { token ->
        client.post("$BASE_URL/proximity/v1/sessions/${InputSecurity.identifier(sessionId, "sessionId")}/resume") { header("Authorization", "Bearer $token") }
    }
    suspend fun sendProximityHeartbeat(context: Context, request: ProximityHeartbeatRequest) = proximityRequest<ProximityHeartbeatResult>(context) { token ->
        client.post("$BASE_URL/proximity/v1/sessions/${InputSecurity.identifier(request.sessionId, "sessionId")}/heartbeat") {
            header("Authorization", "Bearer $token"); contentType(ContentType.Application.Json); setBody(request)
        }
    }
    suspend fun publishProximityPresence(context: Context, request: ProximityHeartbeatRequest) = proximityRequest<ProximityHeartbeatResult>(context) { token ->
        client.post("$BASE_URL/proximity/v1/presence") { header("Authorization", "Bearer $token"); contentType(ContentType.Application.Json); setBody(request) }
    }
    suspend fun clearProximityPresence(context: Context) = proximityRequest<ProximityHeartbeatResult>(context) { token ->
        client.post("$BASE_URL/proximity/v1/presence") { header("Authorization", "Bearer $token"); contentType(ContentType.Application.Json); setBody(mapOf("clear" to true)) }
    }
    suspend fun stopProximitySession(context: Context, sessionId: String) = proximityRequest<ProximityStopResult>(context) { token ->
        client.post("$BASE_URL/proximity/v1/sessions/${InputSecurity.identifier(sessionId, "sessionId")}/stop") { header("Authorization", "Bearer $token") }
    }
    suspend fun getLiveProximity(context: Context, radiusM: Int = 500, cursor: String? = null) = proximityRequest<ProximityLiveData>(context) { token ->
        client.get("$BASE_URL/proximity/v1/live") {
            header("Authorization", "Bearer $token")
            parameter("radiusM", radiusM)
            parameter("limit", 50)
            cursor?.let { parameter("cursor", it) }
        }
    }
    suspend fun getProximityHistory(context: Context, tab: String, cursor: String? = null, query: String? = null,
        sort: String = "recent", filters: Set<String> = emptySet()) = proximityRequest<ProximityHistoryData>(context) { token ->
        client.get("$BASE_URL/proximity/v1/history") {
            header("Authorization", "Bearer $token")
            parameter("tab", tab)
            parameter("sort", sort)
            parameter("limit", 50)
            cursor?.let { parameter("cursor", it) }
            query?.takeIf { it.isNotBlank() }?.let { parameter("query", it) }
            filters.takeIf { it.isNotEmpty() }?.let { parameter("filters", it.sorted().joinToString(",")) }
        }
    }
    suspend fun setProximityHidden(context: Context, targetUserId: String, hidden: Boolean) = proximityRequest<Map<String, Boolean>>(context) { token ->
        client.put("$BASE_URL/proximity/v1/history/${InputSecurity.identifier(targetUserId, "targetUserId")}/hidden") { header("Authorization", "Bearer $token"); contentType(ContentType.Application.Json); setBody(mapOf("hidden" to hidden)) }
    }
    suspend fun removeProximityHistory(context: Context, targetUserId: String): Result<Unit> = try {
        val token = getToken(context) ?: return Result.failure(Exception("Not logged in")); val response = client.delete("$BASE_URL/proximity/v1/history/${InputSecurity.identifier(targetUserId, "targetUserId")}") { header("Authorization", "Bearer $token") }
        if (response.status.isSuccess()) Result.success(Unit) else Result.failure(Exception("Unable to remove encounter"))
    } catch (error: Exception) { Result.failure(error) }
    suspend fun getPendingProximitySummaries(context: Context) = proximityRequest<List<ProximitySummary>>(context) { token ->
        client.get("$BASE_URL/proximity/v1/summaries/pending") { header("Authorization", "Bearer $token") }
    }
    suspend fun markProximitySummaryViewed(context: Context, sessionId: String) = proximityRequest<Map<String, Boolean>>(context) { token ->
        client.post("$BASE_URL/proximity/v1/summaries/${InputSecurity.identifier(sessionId, "sessionId")}/viewed") {
            header("Authorization", "Bearer $token")
        }
    }
}
