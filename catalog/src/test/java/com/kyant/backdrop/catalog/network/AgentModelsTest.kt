package com.kyant.backdrop.catalog.network

import com.kyant.backdrop.catalog.network.models.AgentSessionBootstrapResponse
import com.kyant.backdrop.catalog.network.models.AgentTurnRequest
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
}
