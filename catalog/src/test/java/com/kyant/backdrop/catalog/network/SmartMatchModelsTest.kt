package com.kyant.backdrop.catalog.network

import com.kyant.backdrop.catalog.network.models.SmartMatchResponse
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmartMatchModelsTest {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun `smart match response decodes new why matched fields`() {
        val response = json.decodeFromString<SmartMatchResponse>(
            """
            {
              "matches": [
                {
                  "user": {
                    "id": "user-2",
                    "name": "Ananya",
                    "skills": ["Kotlin"],
                    "interests": ["AI"]
                  },
                  "score": 82.4,
                  "matchPercentage": 82,
                  "reasons": ["Can help with Kotlin"],
                  "tags": ["Kotlin"],
                  "whyMatched": {
                    "summary": "Ananya lines up with what you want to learn: Kotlin.",
                    "bullets": ["They can teach Kotlin."],
                    "scorecard": [
                      {
                        "label": "Skills",
                        "score": 27,
                        "max": 30,
                        "signals": ["Kotlin"]
                      }
                    ]
                  },
                  "sharedSignals": {
                    "skills": ["Kotlin"],
                    "interests": ["AI"],
                    "goals": ["Build a startup"],
                    "locationLabel": "VIT Chennai",
                    "distanceKm": 4.2
                  }
                }
              ],
              "total": 1,
              "hasMore": false
            }
            """.trimIndent()
        )

        val match = response.matches.first()
        assertEquals(82, match.matchPercentage)
        assertEquals("Ananya lines up with what you want to learn: Kotlin.", match.whyMatched?.summary)
        assertEquals("Skills", match.whyMatched?.scorecard?.first()?.label)
        assertEquals(listOf("Kotlin"), match.sharedSignals?.skills)
        assertEquals(4.2, match.sharedSignals?.distanceKm ?: 0.0, 0.001)
    }

    @Test
    fun `smart match response still decodes legacy fields only`() {
        val response = json.decodeFromString<SmartMatchResponse>(
            """
            {
              "matches": [
                {
                  "user": {
                    "id": "user-3",
                    "name": "Legacy"
                  },
                  "score": 40,
                  "matchPercentage": 40,
                  "reasons": ["Same college"],
                  "tags": ["Same college"]
                }
              ]
            }
            """.trimIndent()
        )

        val match = response.matches.first()
        assertEquals("Legacy", match.user.name)
        assertEquals(listOf("Same college"), match.reasons)
        assertNull(match.whyMatched)
        assertNull(match.sharedSignals)
    }
}
