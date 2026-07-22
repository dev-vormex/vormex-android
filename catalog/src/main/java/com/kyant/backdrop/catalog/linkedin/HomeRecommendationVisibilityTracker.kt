package com.kyant.backdrop.catalog.linkedin

import android.os.SystemClock
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.kyant.backdrop.catalog.network.models.RecommendationClientEvent
import com.kyant.backdrop.catalog.recommendation.RecommendationExposureRules
import com.kyant.backdrop.catalog.recommendation.telemetry.RecommendationTelemetryRepository
import kotlinx.coroutines.delay
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

@Composable
internal fun TrackHomeRecommendationVisibility(
    listState: LazyListState,
    rows: List<FeedListRow>,
    recommendationSessionId: String?,
    requestId: String?
) {
    if (recommendationSessionId.isNullOrBlank()) return
    val context = LocalContext.current
    val repository = remember(context) { RecommendationTelemetryRepository(context) }

    LaunchedEffect(listState, rows, recommendationSessionId, requestId) {
        val rowsByKey = rows.associateBy { it.itemKey }
        val visibleSince = mutableMapOf<String, Long>()
        val maximumFraction = mutableMapOf<String, Double>()
        val emitted = mutableSetOf<String>()
        val skipped = mutableSetOf<String>()
        val postById = rows.filterIsInstance<FeedListRow.PostItem>().associateBy { it.post.id }
        val moduleVisibleSince = mutableMapOf<String, Long>()
        val moduleMaximumFraction = mutableMapOf<String, Double>()
        val emittedModuleItems = mutableSetOf<String>()

        while (true) {
            val now = SystemClock.elapsedRealtime()
            val layout = listState.layoutInfo
            val qualifyingNow = mutableSetOf<String>()
            val qualifyingModulesNow = mutableSetOf<String>()
            layout.visibleItemsInfo.forEach { item ->
                val row = rowsByKey[item.key.toString()] ?: return@forEach
                if (item.size <= 0) return@forEach
                val visibleStart = maxOf(item.offset, layout.viewportStartOffset)
                val visibleEnd = minOf(item.offset + item.size, layout.viewportEndOffset)
                val fraction = ((visibleEnd - visibleStart).coerceAtLeast(0).toDouble() / item.size)
                    .coerceIn(0.0, 1.0)
                if (row is FeedListRow.ServerModuleItem) {
                    val moduleKey = row.itemKey
                    moduleMaximumFraction[moduleKey] = maxOf(moduleMaximumFraction[moduleKey] ?: 0.0, fraction)
                    if (fraction >= RecommendationExposureRules.CARD_MIN_VISIBLE_FRACTION) {
                        qualifyingModulesNow += moduleKey
                        val elapsed = now - moduleVisibleSince.getOrPut(moduleKey) { now }
                        if (RecommendationExposureRules.qualifiesCard(moduleMaximumFraction[moduleKey] ?: fraction, elapsed)) {
                            val entityType = when (row.placement.type.uppercase()) {
                                "BOOSTED_POST" -> "POST"
                                "PEOPLE" -> "PERSON"
                                "REELS" -> "REEL"
                                "JOBS" -> "JOB"
                                "EVENTS" -> "EVENT"
                                else -> null
                            }
                            if (entityType != null) {
                                row.placement.items.forEach itemLoop@ { element ->
                                    val entityId = (element as? JsonObject)?.get("id")?.jsonPrimitive?.contentOrNull
                                        ?.takeIf { it.isNotBlank() } ?: return@itemLoop
                                    val dedupe = "$entityType:$entityId"
                                    if (emittedModuleItems.add(dedupe)) {
                                        repository.enqueue(
                                            RecommendationClientEvent(
                                                eventId = UUID.randomUUID().toString(),
                                                eventType = "VISIBILITY",
                                                recommendationSessionId = recommendationSessionId,
                                                requestId = requestId,
                                                surface = "HOME",
                                                entityType = entityType,
                                                entityId = entityId,
                                                reportedPosition = row.placement.position,
                                                maxVisibleFraction = moduleMaximumFraction[moduleKey],
                                                visibleTimeMs = elapsed,
                                                occurredAt = Instant.now().toString()
                                            ),
                                            dedupeKey = "$recommendationSessionId:$entityType:$entityId:VISIBILITY"
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        moduleVisibleSince.remove(moduleKey)
                    }
                    return@forEach
                }
                val postRow = row as? FeedListRow.PostItem ?: return@forEach
                val postId = postRow.post.id
                maximumFraction[postId] = maxOf(maximumFraction[postId] ?: 0.0, fraction)
                if (fraction >= RecommendationExposureRules.CARD_MIN_VISIBLE_FRACTION) {
                    qualifyingNow += postId
                    val startedAt = visibleSince.getOrPut(postId) { now }
                    val visibleTimeMs = now - startedAt
                    if (postId !in emitted && RecommendationExposureRules.qualifiesCard(
                            maximumFraction[postId] ?: fraction,
                            visibleTimeMs
                        )
                    ) {
                        emitted += postId
                        repository.enqueue(
                            RecommendationClientEvent(
                                eventId = UUID.randomUUID().toString(),
                                eventType = "VISIBILITY",
                                recommendationSessionId = recommendationSessionId,
                                requestId = requestId,
                                surface = "HOME",
                                entityType = "POST",
                                entityId = postId,
                                reportedPosition = postRow.post.position,
                                maxVisibleFraction = maximumFraction[postId],
                                visibleTimeMs = visibleTimeMs,
                                occurredAt = Instant.now().toString()
                            ),
                            dedupeKey = "$recommendationSessionId:POST:$postId:VISIBILITY"
                        )
                    }
                }
            }
            moduleVisibleSince.keys.retainAll(qualifyingModulesNow)
            val noLongerQualified = visibleSince.keys.filter { it !in qualifyingNow }
            noLongerQualified.forEach { postId ->
                val visibleTimeMs = now - (visibleSince[postId] ?: now)
                val row = postById[postId]
                if (row != null && visibleTimeMs < 5_000 && skipped.add(postId)) {
                    repository.enqueue(
                        RecommendationClientEvent(
                            eventId = UUID.randomUUID().toString(),
                            eventType = "SKIP",
                            recommendationSessionId = recommendationSessionId,
                            requestId = requestId,
                            surface = "HOME",
                            entityType = "POST",
                            entityId = postId,
                            reportedPosition = row.post.position,
                            maxVisibleFraction = maximumFraction[postId],
                            visibleTimeMs = visibleTimeMs,
                            occurredAt = Instant.now().toString()
                        ),
                        dedupeKey = "$recommendationSessionId:POST:$postId:SKIP"
                    )
                }
            }
            visibleSince.keys.retainAll(qualifyingNow)
            delay(250)
        }
    }
}
