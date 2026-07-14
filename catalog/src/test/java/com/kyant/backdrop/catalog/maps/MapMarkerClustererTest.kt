package com.kyant.backdrop.catalog.maps

import com.kyant.backdrop.catalog.network.models.ProximityMarker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapMarkerClustererTest {
    @Test
    fun `marker input is capped at two hundred`() {
        val markers = (0 until 250).map { index ->
            ProximityMarker("user-$index", 12.0 + index * 0.001, 77.0, null, "event")
        }

        val clusteredCount = MapMarkerClusterer.cluster(markers, 18.0).sumOf { it.markers.size }

        assertEquals(200, clusteredCount)
    }

    @Test
    fun `nearby markers cluster more aggressively at low zoom`() {
        val markers = listOf(
            ProximityMarker("one", 12.97160, 77.59460, null, "event"),
            ProximityMarker("two", 12.97162, 77.59462, null, "event"),
        )

        val lowZoom = MapMarkerClusterer.cluster(markers, 13.0)
        val highZoom = MapMarkerClusterer.cluster(markers, 18.0)

        assertEquals(1, lowZoom.size)
        assertTrue(highZoom.size >= lowZoom.size)
    }

    @Test
    fun `cluster centre is the average of displaced server markers`() {
        val markers = listOf(
            ProximityMarker("one", 10.0, 20.0, null, "public"),
            ProximityMarker("two", 10.00002, 20.00002, null, "public"),
        )

        val cluster = MapMarkerClusterer.cluster(markers, 13.0).single()

        assertEquals(10.00001, cluster.latitude, 0.000001)
        assertEquals(20.00001, cluster.longitude, 0.000001)
    }
}
