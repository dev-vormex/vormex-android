package com.kyant.backdrop.catalog.network

import com.kyant.backdrop.catalog.network.models.AuthResponse
import com.kyant.backdrop.catalog.network.models.ApiError
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthModelsTest {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    @Test
    fun `auth response without token parses for verification-required registration`() {
        val response = json.decodeFromString<AuthResponse>(
            """
            {
              "user": {
                "id": "user-1",
                "email": "student@example.com",
                "isVerified": false
              },
              "message": "Registration successful. Please verify your email before logging in.",
              "requiresVerification": true
            }
            """.trimIndent()
        )

        assertNull(response.token)
        assertTrue(response.requiresVerification)
        assertEquals("user-1", response.user.id)
    }

    @Test
    fun `auth response with bearer token parses for mobile login`() {
        val response = json.decodeFromString<AuthResponse>(
            """
            {
              "user": {
                "id": "user-1",
                "email": "student@example.com",
                "isVerified": true
              },
              "token": "access-token",
              "refreshToken": "refresh-token"
            }
            """.trimIndent()
        )

        assertEquals("access-token", response.token)
        assertEquals("refresh-token", response.refreshToken)
    }

    @Test
    fun `rate limit error includes retry guidance`() {
        val error = json.decodeFromString<ApiError>(
            """
            {
              "error": "Too many login attempts.",
              "code": "login_rate_limited",
              "retryAfterSeconds": 125
            }
            """.trimIndent()
        )

        assertEquals("Too many login attempts. Try again in 3 minutes.", error.getErrorMessage())
    }
}
