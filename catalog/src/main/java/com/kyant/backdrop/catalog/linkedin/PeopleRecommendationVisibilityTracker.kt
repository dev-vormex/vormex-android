package com.kyant.backdrop.catalog.linkedin

import android.os.SystemClock
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.kyant.backdrop.catalog.network.models.PersonInfo
import com.kyant.backdrop.catalog.network.models.RecommendationClientEvent
import com.kyant.backdrop.catalog.recommendation.RecommendationExposureRules
import com.kyant.backdrop.catalog.recommendation.telemetry.RecommendationTelemetryRepository
import kotlinx.coroutines.delay
import java.time.Instant
import java.util.UUID

@Composable
internal fun TrackPeopleRecommendationVisibility(
    gridState: LazyGridState,
    people: List<PersonInfo>,
    recommendationSessionId: String?,
    requestId: String?
) {
    if (recommendationSessionId.isNullOrBlank()) return
    val context = LocalContext.current
    val repository = remember(context) { RecommendationTelemetryRepository(context) }

    LaunchedEffect(gridState, people, recommendationSessionId, requestId) {
        val peopleById = people.associateBy { it.id }
        val visibleSince = mutableMapOf<String, Long>()
        val maximumFraction = mutableMapOf<String, Double>()
        val emitted = mutableSetOf<String>()
        while (true) {
            val now = SystemClock.elapsedRealtime()
            val layout = gridState.layoutInfo
            val qualifying = mutableSetOf<String>()
            layout.visibleItemsInfo.forEach { item ->
                val person = peopleById[item.key.toString()] ?: return@forEach
                if (item.size.height <= 0) return@forEach
                val start = maxOf(item.offset.y, layout.viewportStartOffset)
                val end = minOf(item.offset.y + item.size.height, layout.viewportEndOffset)
                val fraction = ((end - start).coerceAtLeast(0).toDouble() / item.size.height).coerceIn(0.0, 1.0)
                maximumFraction[person.id] = maxOf(maximumFraction[person.id] ?: 0.0, fraction)
                if (fraction >= RecommendationExposureRules.CARD_MIN_VISIBLE_FRACTION) {
                    qualifying += person.id
                    val elapsed = now - visibleSince.getOrPut(person.id) { now }
                    if (person.id !in emitted && RecommendationExposureRules.qualifiesCard(fraction, elapsed)) {
                        emitted += person.id
                        repository.enqueue(
                            RecommendationClientEvent(
                                eventId = UUID.randomUUID().toString(),
                                eventType = "VISIBILITY",
                                recommendationSessionId = recommendationSessionId,
                                requestId = requestId,
                                surface = "PEOPLE",
                                entityType = "PERSON",
                                entityId = person.id,
                                reportedPosition = person.position,
                                maxVisibleFraction = maximumFraction[person.id],
                                visibleTimeMs = elapsed,
                                occurredAt = Instant.now().toString()
                            ),
                            dedupeKey = "$recommendationSessionId:PERSON:${person.id}:VISIBILITY"
                        )
                    }
                }
            }
            visibleSince.keys.retainAll(qualifying)
            delay(250)
        }
    }
}
