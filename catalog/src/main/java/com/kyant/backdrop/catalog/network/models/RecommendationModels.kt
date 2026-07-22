package com.kyant.backdrop.catalog.network.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class RecommendationSocialActor(
    val id: String,
    val name: String,
    val profileImage: String? = null
)

@Serializable
data class HomeModulePlacement(
    val type: String,
    val position: Int,
    val reasonCode: String? = null,
    val reasonText: String? = null,
    val label: String? = null,
    val campaignId: String? = null,
    val postId: String? = null,
    val items: List<JsonElement> = emptyList()
)

@Serializable
data class RecommendationClientEvent(
    val eventId: String,
    val eventType: String,
    val recommendationSessionId: String,
    val requestId: String? = null,
    val surface: String,
    val entityType: String,
    val entityId: String,
    val reportedPosition: Int? = null,
    val maxVisibleFraction: Double? = null,
    val visibleTimeMs: Long? = null,
    val playbackTimeMs: Long? = null,
    val mediaDurationMs: Long? = null,
    val occurredAt: String
)

@Serializable
data class RecommendationEventsRequest(val events: List<RecommendationClientEvent>)

@Serializable
data class RecommendationEventsResponse(
    val accepted: Int = 0,
    val duplicate: Int = 0,
    val rejected: Int = 0
)

@Serializable
data class RecommendationFeedbackRequest(
    val action: String,
    val entityType: String,
    val entityId: String,
    val authorId: String? = null,
    val feedbackType: String? = null
)

@Serializable
data class RecommendationFeedbackResponse(
    val active: Boolean,
    val feedbackType: String
)

@Serializable
data class RecommendationPreferences(
    val personalizedRecommendationsEnabled: Boolean = true,
    val activityRecommendationsEnabled: Boolean = true
)

@Serializable
data class RecommendationPreferencesPatch(
    val personalizedRecommendationsEnabled: Boolean? = null,
    val activityRecommendationsEnabled: Boolean? = null
)

@Serializable
data class PostBoostCredits(
    val creditsGranted: Int = 0,
    val creditsConsumed: Int = 0,
    val creditsRemaining: Int = 0,
    val windowStartsAt: String,
    val windowEndsAt: String
)

@Serializable
data class PostBoostCampaignPost(
    val content: String? = null,
    val type: String? = null
)

@Serializable
data class PostBoostCampaign(
    val id: String,
    val postId: String,
    val status: String,
    val startsAt: String,
    val endsAt: String,
    val impressions: Int = 0,
    val clicks: Int = 0,
    val meaningfulActions: Int = 0,
    val negativeFeedback: Int = 0,
    val pauseReason: String? = null,
    val post: PostBoostCampaignPost? = null
)

@Serializable
data class PostBoostCampaignsResponse(val campaigns: List<PostBoostCampaign> = emptyList())

@Serializable
data class CreatePostBoostRequest(val postId: String)
