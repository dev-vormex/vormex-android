package com.kyant.backdrop.catalog.network.models

import kotlinx.serialization.Serializable

@Serializable data class ProximityEnvelope<T>(val data: T)
@Serializable data class ProximityErrorDetails(val code: String, val message: String,
    val retryable: Boolean = false, val retryAfterSeconds: Int? = null)
@Serializable data class ProximityErrorEnvelope(val error: ProximityErrorDetails)
class ProximityApiException(val details: ProximityErrorDetails, val httpStatus: Int) : Exception(details.message)
@Serializable data class ProximityFlags(val entry: Boolean = false, val eventMode: Boolean = false, val publicPresence: Boolean = false,
    val liveMap: Boolean = false, val liveList: Boolean = false, val accumulation: Boolean = false,
    val summaryNotifications: Boolean = false, val persistence: Boolean = false)
@Serializable data class ProximityHeartbeatBounds(val min: Int = 45, val max: Int = 120)
@Serializable data class ProximityTileConfig(val provider: String = "openstreetmap", val url: String = "", val version: String = "1")
@Serializable data class ProximityCapabilities(val version: Int = 1, val flags: ProximityFlags = ProximityFlags(),
    val supportedRadiiM: List<Int> = listOf(200, 300, 500), val heartbeatSeconds: ProximityHeartbeatBounds = ProximityHeartbeatBounds(),
    val tile: ProximityTileConfig = ProximityTileConfig(), val degradedMode: String = "none")
@Serializable data class ProximitySettings(val crossedPathsDiscoverable: Boolean = false,
    val publicForegroundPresenceEnabled: Boolean = false, val summaryNotificationsEnabled: Boolean = true)
@Serializable data class ProximitySample(val sampleId: String, val capturedAt: String, val latitude: Double, val longitude: Double,
    val accuracyM: Double, val speedMps: Double? = null, val movement: String? = null)
@Serializable data class StartProximitySessionRequest(val clientStartId: String, val radiusM: Int, val sampleId: String,
    val capturedAt: String, val latitude: Double, val longitude: Double, val accuracyM: Double, val speedMps: Double? = null)
@Serializable data class ProximitySessionStart(val version: Int = 1, val sessionId: String, val generation: Int,
    val status: String, val sessionExpiresAt: String, val nextHeartbeatAfterSeconds: Int, val acceptedPrecision: String,
    val degradedMode: String = "none")
@Serializable data class ProximitySessionState(val id: String, val generation: Int, val status: String, val radiusM: Int,
    val expiresAt: String, val summaryStatus: String = "pending", val summaryCount: Int? = null)
@Serializable data class ProximityHeartbeatRequest(val sessionId: String, val generation: Int, val sequence: Long,
    val sampleId: String, val capturedAt: String, val latitude: Double, val longitude: Double, val accuracyM: Double,
    val speedMps: Double? = null, val movement: String? = null)
@Serializable data class ProximityHeartbeatResult(val version: Int = 1, val accepted: Boolean = false, val duplicate: Boolean = false,
    val nearbyCount: Int = 0, val nearbyCountCapped: Boolean = false, val nextHeartbeatAfterSeconds: Int = 120,
    val sessionExpiresAt: String = "", val degradedMode: String = "none", val historyLagging: Boolean = false)
@Serializable data class ProximityProfile(val id: String, val username: String, val name: String, val profileImage: String? = null,
    val headline: String? = null, val college: String? = null)
@Serializable data class ProximityMarker(val userId: String, val latitude: Double, val longitude: Double, val profileImage: String? = null, val mode: String, val displayName: String? = null)
@Serializable data class LiveProximityPerson(val id: String, val username: String, val name: String, val profileImage: String? = null,
    val headline: String? = null, val college: String? = null, val distanceBucket: String,
    val approximateLatitude: Double, val approximateLongitude: Double, val presenceMode: String)
@Serializable data class ProximityLiveData(val markers: List<ProximityMarker> = emptyList(), val people: List<LiveProximityPerson> = emptyList(),
    val nearbyCount: Int = 0, val nearbyCountCapped: Boolean = false, val totalLabel: String = "0", val nextCursor: String? = null)
@Serializable data class ProximityActions(val canConnect: Boolean = false, val canMessage: Boolean = false, val canBlock: Boolean = true)
@Serializable data class ProximityHistoryItem(val encounterId: String, val user: ProximityProfile, val areaLabel: String,
    val firstSeenAt: String, val lastSeenAt: String, val accumulatedDurationSeconds: Int, val freshnessSeconds: Int,
    val expiresAt: String, val connectionStatus: String, val actions: ProximityActions)
@Serializable data class ProximityHistoryData(val items: List<ProximityHistoryItem> = emptyList(), val nextCursor: String? = null)
@Serializable data class ProximitySummary(val id: String, val summaryCount: Int? = null, val summaryReadyAt: String? = null)
@Serializable data class ProximityStopResult(val status: String, val summaryStatus: String, val summaryCount: Int? = null)
