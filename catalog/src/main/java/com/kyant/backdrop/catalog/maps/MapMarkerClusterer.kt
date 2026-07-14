package com.kyant.backdrop.catalog.maps

import com.kyant.backdrop.catalog.network.models.ProximityMarker
import kotlin.math.roundToInt

data class ProximityMarkerCluster(val latitude: Double, val longitude: Double, val markers: List<ProximityMarker>)
object MapMarkerClusterer {
    fun cluster(markers: List<ProximityMarker>, zoom: Double): List<ProximityMarkerCluster> {
        val scale = when { zoom >= 17 -> 10_000.0; zoom >= 15 -> 3_000.0; else -> 1_000.0 }
        return markers.take(200).groupBy { Pair((it.latitude * scale).roundToInt(), (it.longitude * scale).roundToInt()) }
            .values.map { points -> ProximityMarkerCluster(points.map { it.latitude }.average(), points.map { it.longitude }.average(), points) }
    }
}
