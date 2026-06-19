package com.kyant.backdrop.catalog.network

import com.kyant.backdrop.catalog.network.models.AgentSessionBootstrapResponse
import com.kyant.backdrop.catalog.network.models.AgentTurnRequest
import com.kyant.backdrop.catalog.network.models.AiEntitlementsResponse
import com.kyant.backdrop.catalog.network.models.AssistantChatResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentModelsTest {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Test
    fun `agent turn request sends autonomy mode with legacy boolean`() {
        val encoded = json.encodeToString(
            AgentTurnRequest(
                inputText = "Find AI people",
                surface = "talk_with_vormex",
                allowAutonomousActions = true,
                autonomyMode = "power"
            )
        )

        assertTrue(encoded.contains(""""autonomyMode":"power""""))
        assertTrue(encoded.contains(""""allowAutonomousActions":true"""))
    }

    @Test
    fun `session bootstrap parses effective autonomy metadata`() {
        val response = json.decodeFromString<AgentSessionBootstrapResponse>(
            """
            {
              "sessionId": "session-1",
              "requestedAutonomyMode": "power",
              "effectiveAutonomyMode": "approval",
              "powerModeEligible": false,
              "isPremium": false,
              "sessionState": {
                "sessionId": "session-1",
                "allowAutonomousActions": false,
                "requestedAutonomyMode": "power",
                "effectiveAutonomyMode": "approval",
                "powerModeEligible": false,
                "isPremium": false
              }
            }
            """.trimIndent()
        )

        assertEquals("power", response.requestedAutonomyMode)
        assertEquals("approval", response.effectiveAutonomyMode)
        assertFalse(response.powerModeEligible)
        assertEquals("approval", response.sessionState.effectiveAutonomyMode)
    }

    @Test
    fun `ai entitlements parse free assistant access`() {
        val response = json.decodeFromString<AiEntitlementsResponse>(
            """
            {
              "tier": "free",
              "isPremium": false,
              "canUseAgent": false,
              "balance": 0,
              "creditsUsed": 2,
              "agentPromptLimit": 5
            }
            """.trimIndent()
        )

        assertEquals("free", response.tier)
        assertFalse(response.canUseAgent)
        assertEquals(2, response.creditsUsed)
    }

    @Test
    fun `assistant chat response parses free daily quota`() {
        val response = json.decodeFromString<AssistantChatResponse>(
            """
            {
              "reply": "Try improving your headline first.",
              "tier": "free",
              "canUseAgent": false,
              "assistantDailyLimit": 10,
              "assistantDailyRemaining": 7
            }
            """.trimIndent()
        )

        assertEquals("Try improving your headline first.", response.reply)
        assertEquals(10, response.assistantDailyLimit)
        assertEquals(7, response.assistantDailyRemaining)
    }
}
