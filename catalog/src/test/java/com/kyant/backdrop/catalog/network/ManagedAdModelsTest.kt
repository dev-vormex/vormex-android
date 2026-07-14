package com.kyant.backdrop.catalog.network

import com.kyant.backdrop.catalog.network.models.FeedResponse
import com.kyant.backdrop.catalog.network.models.ManagedAdEventRequest
import com.kyant.backdrop.catalog.network.models.ManagedAdPlacement
import com.kyant.backdrop.catalog.network.models.ReelsFeedResponse
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ManagedAdModelsTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Test
    fun `feed response decodes optional managed ad placements`() {
        val response = json.decodeFromString<FeedResponse>(
            """
            {
              "posts": [],
              "nextCursor": null,
              "hasMore": false,
              "adPlacements": [
                {
                  "placement": "feed",
                  "sequence": 0,
                  "afterItemCount": 4,
                  "slotKey": "feed_0",
                  "campaignId": "campaign-1",
                  "sponsorName": "Vormex",
                  "feedTitle": "Build your student network"
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals(1, response.adPlacements.size)
        assertEquals("campaign-1", response.adPlacements.first().campaignId)
    }

    @Test
    fun `reels response defaults missing managed ad placements to empty list`() {
        val response = json.decodeFromString<ReelsFeedResponse>(
            """
            {
              "reels": [],
              "hasMore": false
            }
            """.trimIndent()
        )

        assertTrue(response.adPlacements.isEmpty())
    }

    @Test
    fun `managed ad event request serializes session context`() {
        val encoded = json.encodeToString(
            ManagedAdEventRequest(
                placement = "reels",
                slotKey = "reels_0",
                sessionId = "session-1"
            )
        )

        assertTrue(encoded.contains("\"placement\":\"reels\""))
        assertTrue(encoded.contains("\"slotKey\":\"reels_0\""))
        assertTrue(encoded.contains("\"sessionId\":\"session-1\""))
    }

    @Test
    fun `managed ad placement keeps optional creative fields`() {
        val placement = ManagedAdPlacement(
            placement = "reels",
            sequence = 0,
            afterItemCount = 5,
            slotKey = "reels_0",
            campaignId = "campaign-1",
            sponsorName = "Vormex",
            reelCaption = "Try creator tools",
            reelsVideoUrl = "https://cdn.example.com/ad.mp4"
        )

        assertEquals("Try creator tools", placement.reelCaption)
        assertEquals("https://cdn.example.com/ad.mp4", placement.reelsVideoUrl)
    }
}
