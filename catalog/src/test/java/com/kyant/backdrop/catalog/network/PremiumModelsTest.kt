package com.kyant.backdrop.catalog.network

import com.kyant.backdrop.catalog.network.models.PremiumSubscriptionResponse
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumModelsTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }


    @Test
    fun `premium subscription parses developer override metadata`() {
        val decoded = json.decodeFromString<PremiumSubscriptionResponse>(
            """
            {
              "isPremium": true,
              "provider": "developer_override",
              "developerPremiumOverrideAvailable": true,
              "developerPremiumOverrideActive": true
            }
            """.trimIndent()
        )

        assertTrue(decoded.isPremium)
        assertTrue(decoded.developerPremiumOverrideAvailable)
        assertTrue(decoded.developerPremiumOverrideActive)
        assertEquals("developer_override", decoded.provider)
    }
}
