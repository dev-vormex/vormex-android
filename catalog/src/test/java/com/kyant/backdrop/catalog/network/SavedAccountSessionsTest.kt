package com.kyant.backdrop.catalog.network

import com.kyant.backdrop.catalog.network.models.User
import org.junit.Assert.assertEquals
import org.junit.Test

class SavedAccountSessionsTest {
    @Test
    fun `upsert moves account to front and preserves existing refresh token`() {
        val existing = listOf(
            savedAccount("older", updatedAtMillis = 8L),
            savedAccount("current", refreshToken = "refresh-current", updatedAtMillis = 12L)
        )

        val result = ApiClient.upsertSavedAccountSessions(
            existing = existing,
            user = user("current", name = "Current User"),
            token = "new-token-current",
            refreshToken = null,
            nowMillis = 30L
        )

        assertEquals(listOf("current", "older"), result.map { it.userId })
        assertEquals("Current User", result.first().name)
        assertEquals("new-token-current", result.first().token)
        assertEquals("refresh-current", result.first().refreshToken)
        assertEquals(30L, result.first().updatedAtMillis)
    }

    @Test
    fun `mark used bumps selected account without duplicating saved sessions`() {
        val result = ApiClient.markSavedAccountSessionUsed(
            existing = listOf(
                savedAccount("alpha", updatedAtMillis = 5L),
                savedAccount("beta", updatedAtMillis = 20L),
                savedAccount("alpha", token = "duplicate-alpha", updatedAtMillis = 25L),
                savedAccount("", token = "missing-user", updatedAtMillis = 100L),
                savedAccount("blank-token", token = "", updatedAtMillis = 101L)
            ),
            userId = "alpha",
            nowMillis = 40L
        )

        assertEquals(listOf("alpha", "beta"), result.map { it.userId })
        assertEquals("token-alpha", result.first().token)
        assertEquals(40L, result.first().updatedAtMillis)
    }

    @Test
    fun `remove deletes only the requested saved account`() {
        val result = ApiClient.removeSavedAccountSession(
            existing = listOf(
                savedAccount("alpha", updatedAtMillis = 5L),
                savedAccount("beta", updatedAtMillis = 20L),
                savedAccount("gamma", updatedAtMillis = 10L)
            ),
            userId = "beta"
        )

        assertEquals(listOf("gamma", "alpha"), result.map { it.userId })
    }

    private fun user(id: String, name: String = "User $id"): User {
        return User(
            id = id,
            email = "$id@example.com",
            username = id,
            name = name,
            profileImage = "https://example.com/$id.png"
        )
    }

    private fun savedAccount(
        userId: String,
        token: String = "token-$userId",
        refreshToken: String? = "refresh-$userId",
        updatedAtMillis: Long
    ): ApiClient.SavedAccountSession {
        return ApiClient.SavedAccountSession(
            userId = userId,
            email = "$userId@example.com",
            username = userId,
            name = "User $userId",
            profileImage = "https://example.com/$userId.png",
            token = token,
            refreshToken = refreshToken,
            updatedAtMillis = updatedAtMillis
        )
    }
}
