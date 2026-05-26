package com.kyant.backdrop.catalog.network

import android.content.Context
import android.util.Log
import com.kyant.backdrop.catalog.BuildConfig
import com.kyant.backdrop.catalog.network.models.*
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
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object HackathonsApiService {
    private val baseUrl = BuildConfig.API_BASE_URL
    private val allowedSources = setOf("DEVFOLIO", "MLH", "COLLEGE_FEST", "CUSTOM")
    private val allowedApplicationActions = setOf("ACCEPT", "REJECT")

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
                    Log.d("HackathonsApiService", message)
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
            contentType(ContentType.Application.Json)
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

    suspend fun getHackathons(
        context: Context,
        status: String = "active",
        search: String? = null,
        source: String? = null,
        skill: String? = null,
        college: String? = null,
        page: Int = 1,
        limit: Int = 20
    ): Result<HackathonsResponse> {
        return try {
            val token = authHeader(context)
            val safeStatus = InputSecurity.optionalText(status, "status", 24) ?: "active"
            val safeSearch = InputSecurity.optionalText(search, "search", 120)
            val safeSource = source?.let {
                InputSecurity.enumValue(it, allowedSources, "source").lowercase()
            }
            val safeSkill = InputSecurity.optionalText(skill, "skill", 80)
            val safeCollege = InputSecurity.optionalText(college, "college", 120)
            val safePage = InputSecurity.boundedInt(page, "page", 1, 5_000)
            val safeLimit = InputSecurity.boundedInt(limit, "limit", 1, 50)
            val response = client.get("$baseUrl/hackathons") {
                token?.let { header("Authorization", it) }
                parameter("status", safeStatus)
                safeSearch?.let { parameter("search", it) }
                safeSource?.let { parameter("source", it) }
                safeSkill?.let { parameter("skill", it) }
                safeCollege?.let { parameter("college", it) }
                parameter("page", safePage)
                parameter("limit", safeLimit)
            }
            if (response.status.isSuccess()) Result.success(response.body()) else parseFailure(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createHackathon(
        context: Context,
        request: CreateHackathonRequest
    ): Result<HackathonResponse> {
        return try {
            val token = authHeader(context) ?: return Result.failure(Exception("Not logged in"))
            val safeSource = InputSecurity.enumValue(request.source, allowedSources, "source").lowercase()
            val safeRequest = request.copy(
                title = InputSecurity.text(request.title, "title", 120),
                description = InputSecurity.text(request.description, "description", 2_000),
                source = safeSource,
                organizer = InputSecurity.optionalText(request.organizer, "organizer", 120),
                sourceUrl = InputSecurity.optionalText(request.sourceUrl, "sourceUrl", 500),
                college = InputSecurity.optionalText(request.college, "college", 160),
                theme = InputSecurity.optionalText(request.theme, "theme", 120),
                location = InputSecurity.optionalText(request.location, "location", 160),
                prizeSummary = InputSecurity.optionalText(request.prizeSummary, "prizeSummary", 240),
                tags = InputSecurity.sanitizeList(request.tags, "tags", 16, 48),
                skills = InputSecurity.sanitizeList(request.skills, "skills", 16, 48),
                bannerUrl = InputSecurity.optionalText(request.bannerUrl, "bannerUrl", 500),
                teamMin = InputSecurity.boundedInt(request.teamMin, "teamMin", 1, 12),
                teamMax = InputSecurity.boundedInt(request.teamMax, "teamMax", 1, 12)
            )
            val response = client.post("$baseUrl/hackathons") {
                header("Authorization", token)
                setBody(safeRequest)
            }
            if (response.status.isSuccess()) Result.success(response.body()) else parseFailure(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun formTeam(
        context: Context,
        hackathonId: String,
        request: FormHackathonTeamRequest = FormHackathonTeamRequest()
    ): Result<HackathonTeamResponse> {
        return try {
            val token = authHeader(context) ?: return Result.failure(Exception("Not logged in"))
            val safeHackathonId = InputSecurity.identifier(hackathonId, "hackathonId")
            val safeRequest = request.copy(
                name = InputSecurity.optionalText(request.name, "name", 90),
                pitch = InputSecurity.optionalText(request.pitch, "pitch", 600),
                lookingForRoles = InputSecurity.sanitizeList(request.lookingForRoles, "lookingForRoles", 8, 48),
                requiredSkills = InputSecurity.sanitizeList(request.requiredSkills, "requiredSkills", 12, 48),
                maxMembers = request.maxMembers?.let { InputSecurity.boundedInt(it, "maxMembers", 2, 12) }
            )
            val response = client.post("$baseUrl/hackathons/$safeHackathonId/teams/form") {
                header("Authorization", token)
                setBody(safeRequest)
            }
            if (response.status.isSuccess()) Result.success(response.body()) else parseFailure(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTeams(
        context: Context,
        hackathonId: String
    ): Result<HackathonTeamsResponse> {
        return try {
            val token = authHeader(context)
            val safeHackathonId = InputSecurity.identifier(hackathonId, "hackathonId")
            val response = client.get("$baseUrl/hackathons/$safeHackathonId/teams") {
                token?.let { header("Authorization", it) }
            }
            if (response.status.isSuccess()) Result.success(response.body()) else parseFailure(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun applyToTeam(
        context: Context,
        teamId: String,
        request: ApplyHackathonTeamRequest
    ): Result<HackathonApplicationResponse> {
        return try {
            val token = authHeader(context) ?: return Result.failure(Exception("Not logged in"))
            val safeTeamId = InputSecurity.identifier(teamId, "teamId")
            val safeRequest = request.copy(
                role = InputSecurity.optionalText(request.role, "role", 80),
                message = InputSecurity.optionalText(request.message, "message", 500),
                skills = InputSecurity.sanitizeList(request.skills, "skills", 10, 48)
            )
            val response = client.post("$baseUrl/hackathons/teams/$safeTeamId/apply") {
                header("Authorization", token)
                setBody(safeRequest)
            }
            if (response.status.isSuccess()) Result.success(response.body()) else parseFailure(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun respondToApplication(
        context: Context,
        applicationId: String,
        action: String
    ): Result<HackathonApplicationResponse> {
        return try {
            val token = authHeader(context) ?: return Result.failure(Exception("Not logged in"))
            val safeApplicationId = InputSecurity.identifier(applicationId, "applicationId")
            val safeAction = InputSecurity.enumValue(action, allowedApplicationActions, "action").lowercase()
            val response = client.post("$baseUrl/hackathons/team-applications/$safeApplicationId/respond") {
                header("Authorization", token)
                setBody(RespondHackathonApplicationRequest(action = safeAction))
            }
            if (response.status.isSuccess()) Result.success(response.body()) else parseFailure(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveHackathon(context: Context, hackathonId: String): Result<SaveHackathonResponse> {
        return try {
            val token = authHeader(context) ?: return Result.failure(Exception("Not logged in"))
            val safeHackathonId = InputSecurity.identifier(hackathonId, "hackathonId")
            val response = client.post("$baseUrl/hackathons/$safeHackathonId/save") {
                header("Authorization", token)
            }
            if (response.status.isSuccess()) Result.success(response.body()) else parseFailure(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unsaveHackathon(context: Context, hackathonId: String): Result<SaveHackathonResponse> {
        return try {
            val token = authHeader(context) ?: return Result.failure(Exception("Not logged in"))
            val safeHackathonId = InputSecurity.identifier(hackathonId, "hackathonId")
            val response = client.delete("$baseUrl/hackathons/$safeHackathonId/save") {
                header("Authorization", token)
            }
            if (response.status.isSuccess()) Result.success(response.body()) else parseFailure(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyTeams(context: Context): Result<MyHackathonTeamsResponse> {
        return try {
            val token = authHeader(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$baseUrl/hackathons/me/teams") {
                header("Authorization", token)
            }
            if (response.status.isSuccess()) Result.success(response.body()) else parseFailure(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCollegeCommunities(
        context: Context,
        search: String? = null,
        mine: Boolean = false
    ): Result<CollegeCommunitiesResponse> {
        return try {
            val token = authHeader(context)
            val safeSearch = InputSecurity.optionalText(search, "search", 120)
            val response = client.get("$baseUrl/college-communities") {
                token?.let { header("Authorization", it) }
                safeSearch?.let { parameter("search", it) }
                parameter("mine", mine)
            }
            if (response.status.isSuccess()) Result.success(response.body()) else parseFailure(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createCollegeCommunity(
        context: Context,
        request: CreateCollegeCommunityRequest
    ): Result<CollegeCommunityResponse> {
        return try {
            val token = authHeader(context) ?: return Result.failure(Exception("Not logged in"))
            val safeRequest = request.copy(
                college = InputSecurity.text(request.college, "college", 160),
                description = InputSecurity.optionalText(request.description, "description", 600),
                emailDomains = InputSecurity.sanitizeList(request.emailDomains, "emailDomains", 8, 80)
            )
            val response = client.post("$baseUrl/college-communities") {
                header("Authorization", token)
                setBody(safeRequest)
            }
            if (response.status.isSuccess()) Result.success(response.body()) else parseFailure(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyCollegeStudent(
        context: Context,
        request: VerifyCollegeStudentRequest
    ): Result<CollegeVerificationResponse> {
        return try {
            val token = authHeader(context) ?: return Result.failure(Exception("Not logged in"))
            val safeRequest = request.copy(
                college = InputSecurity.text(request.college, "college", 160),
                studentEmail = InputSecurity.optionalText(request.studentEmail, "studentEmail", 160)
            )
            val response = client.post("$baseUrl/college-communities/verify") {
                header("Authorization", token)
                setBody(safeRequest)
            }
            if (response.status.isSuccess()) Result.success(response.body()) else parseFailure(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinCollegeCommunity(
        context: Context,
        communityId: String
    ): Result<CollegeCommunityResponse> {
        return try {
            val token = authHeader(context) ?: return Result.failure(Exception("Not logged in"))
            val safeCommunityId = InputSecurity.identifier(communityId, "communityId")
            val response = client.post("$baseUrl/college-communities/$safeCommunityId/join") {
                header("Authorization", token)
            }
            if (response.status.isSuccess()) Result.success(response.body()) else parseFailure(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
