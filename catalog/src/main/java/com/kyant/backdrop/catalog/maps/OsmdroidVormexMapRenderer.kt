package com.kyant.backdrop.catalog.maps

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.kyant.backdrop.catalog.network.models.ProximityMarker
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class OsmdroidVormexMapRenderer(tileUrl: String) : VormexMapRenderer {
    private val safeTileUrl = tileUrl.takeIf {
        it.startsWith("https://") && it.contains("{z}") && it.contains("{x}") && it.contains("{y}")
    } ?: "https://tile.openstreetmap.org/{z}/{x}/{y}.png"

    private val tileSource = object : OnlineTileSourceBase(
        "Vormex OpenStreetMap", 0, 19, 256, ".png",
        arrayOf("https://tile.openstreetmap.org/"), "© OpenStreetMap contributors",
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String = safeTileUrl
            .replace("{z}", MapTileIndex.getZoom(pMapTileIndex).toString())
            .replace("{x}", MapTileIndex.getX(pMapTileIndex).toString())
            .replace("{y}", MapTileIndex.getY(pMapTileIndex).toString())
    }

    @Composable
    override fun Render(markers: List<ProximityMarker>, modifier: Modifier, onMarkerClick: (String) -> Unit, onFailure: (Throwable) -> Unit) {
        AndroidView(
            modifier = modifier,
            factory = { context ->
                runCatching {
                    Configuration.getInstance().userAgentValue = "Vormex-Android/${context.packageName}"
                    MapView(context).apply {
                        setTileSource(tileSource)
                        setMultiTouchControls(true)
                        controller.setZoom(if (markers.isEmpty()) 3.0 else 15.0)
                        controller.setCenter(GeoPoint(20.0, 0.0))
                        onResume()
                    }
                }.getOrElse {
                    onFailure(it)
                    MapView(context)
                }
            },
            update = { map ->
                runCatching {
                    map.overlays.removeAll { it is Marker }
                    val viewer = markers.firstOrNull { it.mode == "viewer" }
                    val clusters = MapMarkerClusterer.cluster(markers.filterNot { it.mode == "viewer" }, map.zoomLevelDouble)
                    clusters.forEach { cluster ->
                        map.overlays.add(Marker(map).apply {
                            position = GeoPoint(cluster.latitude, cluster.longitude)
                            title = if (cluster.markers.size == 1) "Vormex member" else "${cluster.markers.size} members nearby"
                            icon = profileMarkerIcon(map, cluster.markers.firstOrNull()?.displayName, cluster.markers.size)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            setOnMarkerClickListener { _, mapView ->
                                if (cluster.markers.size == 1) onMarkerClick(cluster.markers.first().userId)
                                else {
                                    mapView.controller.animateTo(position)
                                    mapView.controller.setZoom((mapView.zoomLevelDouble + 2).coerceAtMost(19.0))
                                }
                                true
                            }
                        })
                    }
                    clusters.firstOrNull()?.let {
                        map.controller.setZoom(15.0)
                        map.controller.setCenter(GeoPoint(it.latitude, it.longitude))
                    }
                    viewer?.let {
                        map.overlays.add(Marker(map).apply {
                            position = GeoPoint(it.latitude, it.longitude)
                            title = "You are here"
                            snippet = "Your current location"
                            icon = viewerMarkerIcon(map)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        })
                        map.controller.setZoom(16.0)
                        map.controller.setCenter(GeoPoint(it.latitude, it.longitude))
                    }
                    map.invalidate()
                }.onFailure(onFailure)
            },
            onRelease = { map ->
                map.onPause()
                map.onDetach()
            },
        )
    }

    private fun profileMarkerIcon(map: MapView, displayName: String?, count: Int): BitmapDrawable {
        val density = map.resources.displayMetrics.density
        val size = ((if (count > 1) 42 else 38) * density).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.color = if (count > 1) android.graphics.Color.rgb(109, 93, 251) else android.graphics.Color.rgb(67, 56, 202)
        canvas.drawCircle(size / 2f, size / 2f, size * .43f, paint)
        paint.color = android.graphics.Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.DEFAULT_BOLD
        if (count > 1) {
            paint.textSize = size * .36f
            canvas.drawText(count.coerceAtMost(99).toString(), size / 2f, size * .62f, paint)
        } else {
            paint.textSize = size * .38f
            canvas.drawText(displayName?.trim()?.firstOrNull()?.uppercase() ?: "•", size / 2f, size * .64f, paint)
        }
        return BitmapDrawable(map.resources, bitmap)
    }

    private fun viewerMarkerIcon(map: MapView): BitmapDrawable {
        val size = (28 * map.resources.displayMetrics.density).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.color = android.graphics.Color.rgb(67, 56, 202)
        canvas.drawCircle(size / 2f, size / 2f, size * .38f, paint)
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(size / 2f, size / 2f, size * .14f, paint)
        return BitmapDrawable(map.resources, bitmap)
    }
}
