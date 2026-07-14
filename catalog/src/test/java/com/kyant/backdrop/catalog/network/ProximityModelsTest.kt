package com.kyant.backdrop.catalog.network

import com.kyant.backdrop.catalog.network.models.ProximityHeartbeatResult
import com.kyant.backdrop.catalog.network.models.ProximityErrorEnvelope
import com.kyant.backdrop.catalog.network.models.ProximitySettings
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ProximityModelsTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `degraded heartbeat responses retain safe defaults`() {
        val result = json.decodeFromString<ProximityHeartbeatResult>(
            """{"accepted":true,"nextHeartbeatAfterSeconds":90,"degradedMode":"history_lagging"}"""
        )

        assertEquals(90, result.nextHeartbeatAfterSeconds)
        assertEquals("history_lagging", result.degradedMode)
        assertFalse(result.duplicate)
    }

    @Test
    fun `proximity discovery is opt in by default`() {
        val settings = json.decodeFromString<ProximitySettings>("{}")

        assertFalse(settings.crossedPathsDiscoverable)
        assertFalse(settings.publicForegroundPresenceEnabled)
    }

    @Test
    fun `stable proximity errors retain recovery metadata`() {
        val envelope = json.decodeFromString<ProximityErrorEnvelope>(
            """{"error":{"code":"PROXIMITY_RATE_LIMITED","message":"Try later","retryable":true,"retryAfterSeconds":12}}"""
        )

        assertEquals("PROXIMITY_RATE_LIMITED", envelope.error.code)
        assertEquals(true, envelope.error.retryable)
        assertEquals(12, envelope.error.retryAfterSeconds)
    }
}
