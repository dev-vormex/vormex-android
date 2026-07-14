package com.kyant.backdrop.catalog.maps

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kyant.backdrop.catalog.network.models.ProximityMarker

interface VormexMapRenderer {
    @Composable fun Render(markers: List<ProximityMarker>, modifier: Modifier, onMarkerClick: (String) -> Unit = {}, onFailure: (Throwable) -> Unit)
}
