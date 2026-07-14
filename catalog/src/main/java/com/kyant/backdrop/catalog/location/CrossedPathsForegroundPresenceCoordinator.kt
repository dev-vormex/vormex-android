package com.kyant.backdrop.catalog.location

import android.content.Context
import com.kyant.backdrop.catalog.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.kyant.backdrop.catalog.network.models.ProximityHeartbeatRequest
import java.time.Instant
import java.util.UUID

class CrossedPathsForegroundPresenceCoordinator(private val context: Context, private val scope: CoroutineScope) {
    private var job: Job? = null
    private var sequence = 1L
    private var pending: ProximityHeartbeatRequest? = null
    private val foregroundSessionId = UUID.randomUUID().toString()
    fun onForeground() { if (job?.isActive == true) return; job = scope.launch {
        // Public presence remains opt-in. The capability/settings check prevents accidental publication.
        val capabilities = ApiClient.getProximityCapabilities(context).getOrNull()
        if (capabilities?.flags?.publicPresence != true) return@launch
        val settings = ApiClient.getProximitySettings(context).getOrNull()
        if (settings?.crossedPathsDiscoverable != true || !settings.publicForegroundPresenceEnabled) return@launch
        while (isActive) {
            pending?.let { request ->
                val capturedAt = runCatching { Instant.parse(request.capturedAt).toEpochMilli() }.getOrDefault(0)
                if (capturedAt < System.currentTimeMillis() - 5 * 60_000L) pending = null
            }
            if (pending == null) {
                CrossedPathsLocationSampler.sample(context).getOrNull()?.let { sample ->
                    pending = ProximityHeartbeatRequest(foregroundSessionId, 1, sequence,
                        sample.sampleId, sample.capturedAt, sample.latitude, sample.longitude, sample.accuracyM, sample.speedMps)
                }
            }
            pending?.let { request ->
                ApiClient.publishProximityPresence(context, request).onSuccess {
                    sequence += 1
                    pending = null
                }
            }
            delay(120_000)
        }
    } }
    fun onBackground() {
        job?.cancel()
        job = null
        pending = null
        scope.launch { ApiClient.clearProximityPresence(context) }
    }
}
