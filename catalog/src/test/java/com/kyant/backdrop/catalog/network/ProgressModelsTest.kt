package com.kyant.backdrop.catalog.network

import com.kyant.backdrop.catalog.network.models.ProgressResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressModelsTest {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    @Test
    fun `progress response parses XP coins and daily streak`() {
        val response = json.decodeFromString<ProgressResponse>(
            """
            {
              "data": {
                "xp": {
                  "lifetimeXp": 240,
                  "level": 3,
                  "levelName": "Builder",
                  "currentLevelXp": 100,
                  "nextLevelXp": 255,
                  "xpIntoLevel": 140,
                  "xpToNextLevel": 15,
                  "progressToNextLevel": 0.9,
                  "rules": [
                    { "action": "Post", "amount": 20, "description": "Publish a post" },
                    { "action": "Games", "amount": null, "description": "Complete games" }
                  ]
                },
                "coins": {
                  "balance": 75,
                  "rules": [{ "action": "Store", "description": "Spend Coins" }],
                  "recentTransactions": [
                    {
                      "id": "tx-1",
                      "amount": 25,
                      "type": "wordle_win",
                      "source": "tech_wordle",
                      "createdAt": "2026-04-24T00:00:00.000Z"
                    }
                  ]
                },
                "streak": {
                  "current": 4,
                  "longest": 8,
                  "qualifiedToday": true,
                  "isAtRisk": false,
                  "lastQualifiedDate": "2026-04-24",
                  "rules": ["Do one meaningful action"],
                  "categories": {
                    "login": { "current": 5, "longest": 9, "lastDate": "2026-04-24" },
                    "networking": { "current": 2, "longest": 3, "lastDate": "2026-04-23" },
                    "posting": { "current": 1, "longest": 4, "lastDate": "2026-04-22" },
                    "messaging": { "current": 0, "longest": 2, "lastDate": null }
                  }
                }
              }
            }
            """.trimIndent()
        )

        assertEquals(240, response.data.xp.lifetimeXp)
        assertEquals(3, response.data.xp.level)
        assertEquals(75, response.data.coins.balance)
        assertEquals(4, response.data.streak.current)
        assertTrue(response.data.streak.qualifiedToday)
        assertEquals(5, response.data.streak.categories.login.current)
    }
}
