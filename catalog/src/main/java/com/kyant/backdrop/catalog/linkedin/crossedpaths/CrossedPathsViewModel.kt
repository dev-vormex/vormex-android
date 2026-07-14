package com.kyant.backdrop.catalog.linkedin.crossedpaths

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kyant.backdrop.catalog.data.CrossedPathsSessionStore
import com.kyant.backdrop.catalog.data.StoredCrossedPathsSession
import com.kyant.backdrop.catalog.location.CrossedPathsLocationSampler
import com.kyant.backdrop.catalog.location.CrossedPathsLocationService
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

data class CrossedPathsUiState(val loading: Boolean = true, val capabilities: ProximityCapabilities? = null,
    val session: ProximitySessionState? = null, val live: ProximityLiveData = ProximityLiveData(),
    val history: ProximityHistoryData = ProximityHistoryData(), val summaries: List<ProximitySummary> = emptyList(),
    val eventServiceRunning: Boolean = false, val loadingMore: Boolean = false, val error: String? = null,
    val actionMessage: String? = null, val demoEventActive: Boolean = false)

class CrossedPathsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application.applicationContext
    private val _state = MutableStateFlow(CrossedPathsUiState())
    val state: StateFlow<CrossedPathsUiState> = _state.asStateFlow()
    private var currentTab = "live"
    private var currentQuery = ""
    private var currentSort = "recent"
    private var currentFilters: Set<String> = emptySet()
    private var previewSample: ProximitySample? = null
    init { reconcile() }

    fun loadLocationPreview() = viewModelScope.launch {
        CrossedPathsLocationSampler.sample(app).onSuccess { sample ->
            previewSample = sample
            _state.value = _state.value.copy(
                live = withPreviewData(_state.value.live, sample),
                history = withMockHistory(_state.value.history),
                error = null,
            )
        }.onFailure { _state.value = _state.value.copy(error = it.message) }
    }
    fun advanceDemoPreview() {
        previewSample?.let { sample -> _state.value = _state.value.copy(live = withPreviewData(_state.value.live, sample)) }
    }
    fun reconcile() = viewModelScope.launch {
        var stored = CrossedPathsSessionStore.read(app)
        if (stored?.stopPending == true) {
            if (ApiClient.stopProximitySession(app, stored.sessionId).isSuccess) {
                CrossedPathsSessionStore.clear(app)
                stored = null
            }
        }
        val capabilities = ApiClient.getProximityCapabilities(app).getOrNull()
        val sessionResult = ApiClient.getCurrentProximitySession(app)
        val serverSession = sessionResult.getOrNull()
        val storedExpired = stored?.expiresAt?.let {
            runCatching { Instant.parse(it).toEpochMilli() <= System.currentTimeMillis() }.getOrDefault(true)
        } ?: false
        if (storedExpired || (sessionResult.isSuccess && serverSession == null)) {
            CrossedPathsSessionStore.clear(app)
            stored = null
        }
        val recoverableStoredSession = stored?.takeIf { !it.stopPending }?.let {
            ProximitySessionState(it.sessionId, it.generation, "active", it.radiusM, it.expiresAt)
        }
        val session = serverSession ?: if (sessionResult.isFailure) recoverableStoredSession else null
        val summaries = ApiClient.getPendingProximitySummaries(app).getOrNull() ?: _state.value.summaries
        val serviceRunning = session != null && CrossedPathsLocationService.isRunning()
        _state.value = _state.value.copy(loading = false, capabilities = capabilities ?: _state.value.capabilities,
            session = session, eventServiceRunning = serviceRunning,
            summaries = summaries, error = sessionResult.exceptionOrNull()?.message,
            actionMessage = if (session != null && !serviceRunning) "An Event Mode session is waiting. Tap Resume to continue live sharing." else _state.value.actionMessage)
        refresh("live")
    }
    fun start(radiusM: Int = 500) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        val sample = CrossedPathsLocationSampler.sample(app).getOrElse { _state.value = _state.value.copy(loading = false, error = it.message); return@launch }
        val settings = ApiClient.getProximitySettings(app).getOrElse {
            startDemoEvent(sample); return@launch
        }
        if (!settings.crossedPathsDiscoverable) ApiClient.updateProximitySettings(app, settings.copy(crossedPathsDiscoverable = true)).getOrElse {
            startDemoEvent(sample); return@launch
        }
        val clientStartId = UUID.randomUUID().toString()
        val started = ApiClient.startProximitySession(app, StartProximitySessionRequest(clientStartId, radiusM, sample.sampleId,
            sample.capturedAt, sample.latitude, sample.longitude, sample.accuracyM, sample.speedMps)).getOrElse {
            startDemoEvent(sample); return@launch
        }
        CrossedPathsSessionStore.save(app, StoredCrossedPathsSession(started.sessionId, started.generation, 2, started.sessionExpiresAt, radiusM, clientStartId, false))
        val session = ProximitySessionState(started.sessionId, started.generation, started.status, radiusM, started.sessionExpiresAt)
        runCatching {
            ContextCompat.startForegroundService(app, Intent(app, CrossedPathsLocationService::class.java)
                .setAction(CrossedPathsLocationService.ACTION_START))
        }.onSuccess {
            _state.value = _state.value.copy(loading = false, session = session, eventServiceRunning = true,
                actionMessage = "Event Mode started")
        }.onFailure {
            _state.value = _state.value.copy(loading = false, session = session, eventServiceRunning = false,
                error = "Event Mode could not start on this device. Tap Resume to retry.")
        }
        refresh("live")
    }
    fun stop() = viewModelScope.launch {
        if (_state.value.demoEventActive) {
            _state.value = _state.value.copy(demoEventActive = false, loading = false, error = null, actionMessage = "Demo Event Mode stopped")
            return@launch
        }
        val stored = CrossedPathsSessionStore.read(app) ?: return@launch
        CrossedPathsSessionStore.markStopPending(app, true)
        runCatching { app.startService(Intent(app, CrossedPathsLocationService::class.java).setAction(CrossedPathsLocationService.ACTION_STOP)) }
        val stopped = ApiClient.stopProximitySession(app, stored.sessionId)
        if (stopped.isSuccess) CrossedPathsSessionStore.clear(app)
        _state.value = _state.value.copy(session = null, eventServiceRunning = false,
            actionMessage = if (stopped.isSuccess) "Event Mode stopped" else "Event Mode stopped on this device; server sync is pending",
            error = stopped.exceptionOrNull()?.message)
    }
    fun resume() = viewModelScope.launch {
        val current = _state.value.session ?: return@launch
        val resumed = ApiClient.resumeProximitySession(app, current.id).getOrElse { _state.value = _state.value.copy(error = it.message); return@launch }
        val stored = CrossedPathsSessionStore.read(app)
        CrossedPathsSessionStore.save(app, StoredCrossedPathsSession(resumed.id, resumed.generation, 1, resumed.expiresAt, resumed.radiusM, stored?.clientStartId.orEmpty(), false))
        runCatching {
            ContextCompat.startForegroundService(app, Intent(app, CrossedPathsLocationService::class.java)
                .setAction(CrossedPathsLocationService.ACTION_START))
        }.onSuccess {
            _state.value = _state.value.copy(session = resumed, eventServiceRunning = true, actionMessage = "Event Mode resumed")
        }.onFailure {
            _state.value = _state.value.copy(session = resumed, eventServiceRunning = false,
                error = "Event Mode could not resume on this device. You can retry or stop the session.")
        }
    }
    fun refresh(tab: String, query: String = "", sort: String = "recent", filters: Set<String> = emptySet()) = viewModelScope.launch {
        currentTab = tab
        currentQuery = query
        currentSort = sort
        currentFilters = filters
        if (tab == "live") {
            ApiClient.getLiveProximity(app).fold(
                onSuccess = { live -> _state.value = _state.value.copy(live = previewSample?.let { withPreviewData(live, it) } ?: live, error = null) },
                onFailure = { _state.value = _state.value.copy(error = it.message) },
            )
        } else {
            ApiClient.getProximityHistory(app, tab, query = query, sort = sort, filters = filters).fold(
                onSuccess = { history -> _state.value = _state.value.copy(history = withMockHistory(history), error = null) },
                onFailure = { _state.value = _state.value.copy(error = it.message) },
            )
        }
    }
    fun loadMore() = viewModelScope.launch {
        if (_state.value.loadingMore) return@launch
        val cursor = if (currentTab == "live") _state.value.live.nextCursor else _state.value.history.nextCursor
        if (cursor == null) return@launch
        _state.value = _state.value.copy(loadingMore = true)
        if (currentTab == "live") {
            ApiClient.getLiveProximity(app, cursor = cursor).onSuccess { next ->
                val current = _state.value.live
                _state.value = _state.value.copy(live = next.copy(
                    markers = (current.markers + next.markers).distinctBy { it.userId },
                    people = (current.people + next.people).distinctBy { it.id },
                ))
            }.onFailure { _state.value = _state.value.copy(error = it.message) }
        } else {
            ApiClient.getProximityHistory(app, currentTab, cursor, currentQuery, currentSort, currentFilters).onSuccess { next ->
                val current = _state.value.history
                _state.value = _state.value.copy(history = next.copy(
                    items = (current.items + next.items).distinctBy { it.encounterId },
                ))
            }.onFailure { _state.value = _state.value.copy(error = it.message) }
        }
        _state.value = _state.value.copy(loadingMore = false)
    }
    fun connect(userId: String) = viewModelScope.launch { _state.value = _state.value.copy(actionMessage = ApiClient.sendConnectionRequest(app, userId).fold({ "Connection request sent" }, { it.message })) }
    fun message(userId: String) = viewModelScope.launch { _state.value = _state.value.copy(actionMessage = ApiClient.getOrCreateConversation(app, userId).fold({ "Conversation is ready" }, { it.message })) }
    fun dismissSummary(sessionId: String) = viewModelScope.launch {
        ApiClient.markProximitySummaryViewed(app, sessionId).onSuccess {
            _state.value = _state.value.copy(summaries = _state.value.summaries.filterNot { it.id == sessionId })
        }.onFailure { _state.value = _state.value.copy(error = it.message) }
    }
    fun hide(userId: String) = viewModelScope.launch { ApiClient.setProximityHidden(app, userId, true); refresh(currentTab, currentQuery, currentSort, currentFilters) }
    fun remove(userId: String) = viewModelScope.launch { ApiClient.removeProximityHistory(app, userId); refresh(currentTab, currentQuery, currentSort, currentFilters) }
    fun block(userId: String) = viewModelScope.launch { ApiClient.blockUser(app, userId, "Crossed Paths safety action"); refresh(currentTab, currentQuery, currentSort, currentFilters) }

    private fun withPreviewData(live: ProximityLiveData, sample: ProximitySample): ProximityLiveData {
        val demos = listOf(
            DemoPerson("demo-aanya", "Aanya Sharma", "Product Designer", "IIT Delhi", "within_100_m", .0007, .0003),
            DemoPerson("demo-rohan", "Rohan Mehta", "Android Developer", "BITS Pilani", "within_200_m", -.0010, .0008),
            DemoPerson("demo-mira", "Mira Nair", "UX Researcher", "NID Ahmedabad", "within_200_m", .0012, -.0006),
            DemoPerson("demo-arjun", "Arjun Rao", "AI Engineer", "IIIT Hyderabad", "within_300_m", -.0017, -.0011),
            DemoPerson("demo-zoya", "Zoya Khan", "Community Builder", "Delhi University", "within_300_m", .0019, .0010),
        )
        val phase = (System.currentTimeMillis() / 15_000L % 12).toDouble()
        val demoMarkers = demos.mapIndexed { index, it ->
            val movement = kotlin.math.sin(phase + index) * .00012
            ProximityMarker(it.id, sample.latitude + it.latOffset + movement, sample.longitude + it.lngOffset - movement, mode = "demo", displayName = it.name)
        }
        val demoPeople = demos.map { LiveProximityPerson(it.id, it.id, it.name, headline = "Demo · ${it.headline}", college = it.college, distanceBucket = it.distance, approximateLatitude = sample.latitude + it.latOffset, approximateLongitude = sample.longitude + it.lngOffset, presenceMode = "demo") }
        return live.copy(
            markers = (listOf(ProximityMarker("__viewer__", sample.latitude, sample.longitude, mode = "viewer")) + demoMarkers + live.markers).distinctBy { it.userId },
            people = (demoPeople + live.people).distinctBy { it.id },
            nearbyCount = maxOf(live.nearbyCount, demoPeople.size),
            totalLabel = maxOf(live.nearbyCount, demoPeople.size).toString(),
        )
    }

    private fun withMockHistory(history: ProximityHistoryData): ProximityHistoryData {
        val now = Instant.now()
        val demos = listOf(
            Triple("demo-aanya", "Aanya Sharma", "Product Designer · Demo profile"),
            Triple("demo-rohan", "Rohan Mehta", "Android Developer · Demo profile"),
            Triple("demo-mira", "Mira Nair", "UX Researcher · Demo profile"),
            Triple("demo-arjun", "Arjun Rao", "AI Engineer · Demo profile"),
            Triple("demo-zoya", "Zoya Khan", "Community Builder · Demo profile"),
        ).mapIndexed { index, (id, name, headline) ->
            ProximityHistoryItem(
                encounterId = "mock-encounter-$id", user = ProximityProfile(id, id, name, headline = headline),
                areaLabel = listOf("Campus café", "Main auditorium", "Innovation hub", "Library walk", "Community lounge")[index],
                firstSeenAt = now.minusSeconds((index + 1) * 3600L).toString(), lastSeenAt = now.minusSeconds((index + 1) * 1800L).toString(),
                accumulatedDurationSeconds = listOf(720, 480, 360, 240, 180)[index], freshnessSeconds = (index + 1) * 1800,
                expiresAt = now.plusSeconds(7 * 86400L).toString(), connectionStatus = "none", actions = ProximityActions(),
            )
        }
        return history.copy(items = (demos + history.items).distinctBy { it.encounterId })
    }

    private data class DemoPerson(val id: String, val name: String, val headline: String, val college: String, val distance: String, val latOffset: Double, val lngOffset: Double)

    private fun startDemoEvent(sample: ProximitySample) {
        previewSample = sample
        _state.value = _state.value.copy(
            loading = false, demoEventActive = true, error = null,
            live = withPreviewData(_state.value.live, sample),
            actionMessage = "Demo Event Mode is live. Nearby sample members will move as the map refreshes.",
        )
    }
}
