package com.kyant.backdrop.catalog.location

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.kyant.backdrop.catalog.data.CrossedPathsSessionStore
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.models.ProximityHeartbeatRequest
import com.kyant.backdrop.catalog.notifications.CrossedPathsNotificationManager
import kotlinx.coroutines.*
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

class CrossedPathsLocationService : Service() {
    companion object {
        const val ACTION_START = "com.vormex.crossedpaths.START"
        const val ACTION_STOP = "com.vormex.crossedpaths.STOP"

        @Volatile
        private var running = false

        fun isRunning(): Boolean = running
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loop: Job? = null
    private val offlineBuffer = ArrayDeque<ProximityHeartbeatRequest>(3)
    private val stopping = AtomicBoolean(false)
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopEventMode(); return START_NOT_STICKY }
        if (intent?.action != ACTION_START) { stopSelf(); return START_NOT_STICKY }
        val notification = CrossedPathsNotificationManager.ongoing(this, "")
        if (Build.VERSION.SDK_INT >= 29) startForeground(CrossedPathsNotificationManager.NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        else startForeground(CrossedPathsNotificationManager.NOTIFICATION_ID, notification)
        running = true
        scope.launch {
            val state = CrossedPathsSessionStore.read(this@CrossedPathsLocationService) ?: run { stopSelf(); return@launch }
            startLoop()
        }
        return START_NOT_STICKY
    }
    private fun startLoop() { if (loop?.isActive == true) return; loop = scope.launch {
        while (isActive) {
            val state = CrossedPathsSessionStore.read(this@CrossedPathsLocationService) ?: break
            if (runCatching { Instant.parse(state.expiresAt).toEpochMilli() }.getOrDefault(0) <= System.currentTimeMillis()) { stopEventMode(); break }
            val cutoff = System.currentTimeMillis() - 5 * 60_000L
            while (offlineBuffer.firstOrNull()?.let { runCatching { Instant.parse(it.capturedAt).toEpochMilli() }.getOrDefault(0) < cutoff } == true) {
                offlineBuffer.removeFirst()
            }
            if (offlineBuffer.size < 3) {
                CrossedPathsLocationSampler.sample(this@CrossedPathsLocationService).getOrNull()?.let { sample ->
                    val nextSequence = maxOf(state.sequence, (offlineBuffer.lastOrNull()?.sequence ?: (state.sequence - 1)) + 1)
                    offlineBuffer.addLast(ProximityHeartbeatRequest(state.sessionId, state.generation, nextSequence,
                        sample.sampleId, sample.capturedAt, sample.latitude, sample.longitude, sample.accuracyM, sample.speedMps))
                }
            }
            val pending = offlineBuffer.firstOrNull()
            if (pending == null) { delay(60_000); continue }
            val result = ApiClient.sendProximityHeartbeat(this@CrossedPathsLocationService, pending).getOrNull()
            if (result != null) {
                offlineBuffer.removeFirst()
                CrossedPathsSessionStore.advanceSequence(this@CrossedPathsLocationService, pending.sequence)
                delay(result.nextHeartbeatAfterSeconds.coerceIn(45, 120) * 1_000L)
            } else delay(60_000)
        }
    } }
    private fun stopEventMode() {
        if (!stopping.compareAndSet(false, true)) return
        loop?.cancel(); scope.launch {
        val state = CrossedPathsSessionStore.read(this@CrossedPathsLocationService)
        if (state != null) {
            CrossedPathsSessionStore.markStopPending(this@CrossedPathsLocationService, true)
            if (ApiClient.stopProximitySession(this@CrossedPathsLocationService, state.sessionId).isSuccess) CrossedPathsSessionStore.clear(this@CrossedPathsLocationService)
        }
        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
    } }
    override fun onDestroy() { running = false; loop?.cancel(); offlineBuffer.clear(); scope.cancel(); super.onDestroy() }
}
