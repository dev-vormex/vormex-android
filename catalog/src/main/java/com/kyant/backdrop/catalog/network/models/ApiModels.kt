package com.kyant.backdrop.catalog.network.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.JsonDecoder

@OptIn(ExperimentalSerializationApi::class)
object FlexibleLocationStringSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleLocationString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String? {
        val jsonDecoder = decoder as? JsonDecoder
            ?: return decoder.decodeString().takeIf { it.isNotBlank() }
        return when (val element = jsonDecoder.decodeJsonElement()) {
            JsonNull -> null
            is JsonPrimitive -> element.contentOrNull?.takeIf { it.isNotBlank() }
            is JsonObject -> profileLocationLabel(element)
            else -> null
        }
    }

    override fun serialize(encoder: Encoder, value: String?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeString(value)
        }
    }

    private fun profileLocationLabel(location: JsonObject): String? {
        val display = stringField(location, "displayName")
            ?: stringField(location, "label")
            ?: stringField(location, "formatted")
            ?: stringField(location, "name")
        if (!display.isNullOrBlank()) return display

        return listOfNotNull(
            stringField(location, "city"),
            stringField(location, "region"),
            stringField(location, "country")
        )
            .distinct()
            .joinToString(", ")
            .takeIf { it.isNotBlank() }
    }

    private fun stringField(obj: JsonObject, key: String): String? =
        obj[key]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotBlank() }
}

@Serializable
data class Author(
    val id: String,
    val username: String? = null,
    val name: String? = null,
    val profileImage: String? = null,
    val headline: String? = null,
    val verified: Boolean = false,
    val isVerified: Boolean = false,
    val profileBadgeStyle: String? = null,
    val isPremium: Boolean = false,
    val profileBoostActive: Boolean = false,
    val profileBoostEndsAt: String? = null,
    val profileBoostPriority: Int = 0,
    val discoveryPriority: Int = 0
)

@Serializable
data class Post(
    val id: String,
    val kind: String = "POST",
    val type: String = "TEXT",
    val authorId: String,
    val author: Author,
    val content: String? = null,
    val contentType: String = "text/plain",
    val mentions: List<String> = emptyList(),
    val collaboratorIds: List<String> = emptyList(),
    val pendingCollaboratorIds: List<String> = emptyList(),
    val collaborators: List<Author> = emptyList(),
    val collaborationStatus: String? = null,
    val mediaUrls: List<String> = emptyList(),
    val mediaCount: Int = 0,
    val videoUrl: String? = null,
    val videoThumbnail: String? = null,
    val videoDuration: Int? = null,
    val videoSize: Long? = null,
    val videoFormat: String? = null,
    val defaultVideoId: String? = null,
    val documentUrl: String? = null,
    val documentName: String? = null,
    val documentType: String? = null,
    val documentSize: Long? = null,
    val documentPages: Int? = null,
    val documentThumbnail: String? = null,
    val linkUrl: String? = null,
    val linkTitle: String? = null,
    val linkDescription: String? = null,
    val linkImage: String? = null,
    val linkDomain: String? = null,
    val articleTitle: String? = null,
    val articleCoverImage: String? = null,
    val articleReadTime: Int? = null,
    val articleTags: List<String> = emptyList(),
    val pollDuration: Int? = null,
    val pollEndsAt: String? = null,
    val pollOptions: List<PollOption> = emptyList(),
    val userVotedOptionId: String? = null,
    val showResultsBeforeVote: Boolean = false,
    val celebrationType: String? = null,
    val celebrationMeta: JsonElement? = null,
    val celebrationBadge: String? = null,
    val celebrationGifUrl: String? = null,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val sharesCount: Int = 0,
    val savesCount: Int = 0,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val userReactionType: String? = null,
    val reactionSummary: List<ReactionSummary> = emptyList(),
    val visibility: String = "PUBLIC",
    val createdAt: String,
    val updatedAt: String? = null,
    val reasonCode: String? = null,
    val reasonText: String? = null,
    val source: String? = null,
    val position: Int? = null,
    val isBoosted: Boolean = false,
    val socialActors: List<RecommendationSocialActor> = emptyList()
)

@Serializable
data class FeedResponse(
    val posts: List<Post>,
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
    val adPlacements: List<ManagedAdPlacement> = emptyList(),
    val recommendationSessionId: String? = null,
    val requestId: String? = null,
    val rankerVersion: String? = null,
    val experimentVariant: String? = null,
    val modulePlacements: List<HomeModulePlacement> = emptyList()
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class ForgotPasswordRequest(
    val email: String
)

@Serializable
data class ResendVerificationRequest(
    val email: String
)

@Serializable
data class VerifyEmailOtpRequest(
    val email: String,
    val code: String
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String,
    val username: String,
    val college: String? = null,
    val branch: String? = null
)

@Serializable
data class GoogleSignInRequest(
    val idToken: String
)

@Serializable
data class User(
    val id: String,
    val email: String,
    val username: String? = null,
    val name: String? = null,
    val profileImage: String? = null,
    val bio: String? = null,
    val headline: String? = null,
    val college: String? = null,
    val branch: String? = null,
    val graduationYear: Int? = null,
    val isVerified: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val isPremium: Boolean = false,
    val canUseAgent: Boolean = false,
    val canAccessProfileCustomization: Boolean = false,
    val profileBadgeStyle: String? = null,
    val profileTheme: String = "default",
    val premiumDisplayAmount: String? = null
)

@Serializable
data class AuthResponse(
    val user: User,
    val token: String? = null,
    val refreshToken: String? = null,
    val message: String? = null,
    val requiresVerification: Boolean = false
)

@Serializable
data class LikeResponse(
    val liked: Boolean,
    val likesCount: Int
)

@Serializable
data class ApiError(
    val error: String? = null,
    val message: String? = null,
    val statusCode: Int? = null,
    val code: String? = null,
    val requestId: String? = null,
    val retryAfterSeconds: Int? = null,
    val requiresVerification: Boolean = false
) {
    fun getErrorMessage(): String {
        val base = error ?: message ?: "Unknown error"
        val retryAfter = retryAfterSeconds
        return if (retryAfter != null && retryAfter > 0) {
            "$base Try again in ${formatRetryAfter(retryAfter)}."
        } else {
            base
        }
    }

    private fun formatRetryAfter(seconds: Int): String {
        return when {
            seconds < 60 -> "$seconds seconds"
            seconds < 3600 -> {
                val minutes = (seconds + 59) / 60
                if (minutes == 1) "1 minute" else "$minutes minutes"
            }
            else -> {
                val hours = (seconds + 3599) / 3600
                if (hours == 1) "1 hour" else "$hours hours"
            }
        }
    }
}

@Serializable
data class MessageResponse(
    val message: String = ""
)

// Comment models
@Serializable
data class Comment(
    val id: String,
    val postId: String,
    val authorId: String,
    val author: Author,
    val content: String,
    val parentId: String? = null,
    val likesCount: Int = 0,
    val repliesCount: Int = 0,
    val isLiked: Boolean = false,
    val createdAt: String,
    val updatedAt: String? = null
)

@Serializable
data class CommentsResponse(
    val comments: List<Comment>,
    val total: Int = 0,
    val hasMore: Boolean = false,
    val nextCursor: String? = null
)

@Serializable
data class CreateCommentRequest(
    val content: String,
    val parentId: String? = null
)

@Serializable
data class CreateCommentResponse(
    val comment: Comment
)

@Serializable
data class CommentLikeResponse(
    val liked: Boolean,
    val likesCount: Int
)

// Share models
@Serializable
data class ShareResponse(
    val shareUrl: String? = null,
    val sharesCount: Int = 0
)

// Stories models
@Serializable
data class Story(
    val id: String,
    val mediaType: String, // TEXT, IMAGE, VIDEO
    val mediaUrl: String? = null,
    val thumbnailUrl: String? = null,
    val textContent: String? = null,
    val backgroundColor: String? = null,
    val category: String = "GENERAL", // GENERAL, DAY_AT_WORK, LEARNING, ACHIEVEMENT, PROJECT, etc.
    val visibility: String = "PUBLIC", // PUBLIC, CONNECTIONS, CLOSE_FRIENDS
    val linkUrl: String? = null,
    val linkTitle: String? = null,
    val viewsCount: Int = 0,
    val reactionsCount: Int = 0,
    val isViewed: Boolean = false,
    val isOwn: Boolean = false,
    val createdAt: String,
    val expiresAt: String
)

@Serializable
data class StoryUser(
    val id: String,
    val username: String? = null,
    val name: String? = null,
    val profileImage: String? = null,
    val headline: String? = null
)

@Serializable
data class StoryGroup(
    val user: StoryUser,
    val stories: List<Story>,
    val hasUnviewed: Boolean = true,
    val lastStoryAt: String? = null,
    val isOwnStory: Boolean = false,
    val reasonCode: String? = null,
    val reasonText: String? = null,
    val source: String? = null,
    val position: Int? = null,
    val isBoosted: Boolean = false,
    val socialActors: List<RecommendationSocialActor> = emptyList()
)

@Serializable
data class StoriesFeedResponse(
    val storyGroups: List<StoryGroup>,
    val recommendationSessionId: String? = null,
    val requestId: String? = null,
    val rankerVersion: String? = null,
    val experimentVariant: String? = null,
    val nextCursor: String? = null
)

@Serializable
data class MyStoriesResponse(
    val stories: List<Story>
)

@Serializable
data class CreateStoryResponse(
    val story: Story
)

@Serializable
data class ViewStoryResponse(
    val viewsCount: Int
)

@Serializable
data class ReactToStoryResponse(
    val success: Boolean = true,
    val reactionsCount: Int = 0,
    val message: String? = null,
    val reactionType: String? = null
)

@Serializable
data class ReplyToStoryResponse(
    val success: Boolean = true,
    val message: String? = null,
    val conversationId: String? = null
)

@Serializable
data class StoryViewersResponse(
    val viewers: List<StoryViewerData>,
    val totalCount: Int,
    val nextCursor: String? = null,
    val hasMore: Boolean = false
)

@Serializable
data class StoryViewerData(
    val id: String,
    val viewedAt: String,
    val user: StoryViewerUserData? = null
)

@Serializable
data class StoryViewerUserData(
    val id: String,
    val name: String? = null,
    val username: String? = null,
    val profileImage: String? = null,
    val headline: String? = null
)

@Serializable
data class StoryCategory(
    val id: String,
    val label: String
) {
    companion object {
        val ALL_CATEGORIES = listOf(
            StoryCategory("GENERAL", "General"),
            StoryCategory("DAY_AT_WORK", "Day at Work"),
            StoryCategory("LEARNING", "Learning"),
            StoryCategory("ACHIEVEMENT", "Achievement"),
            StoryCategory("PROJECT", "Project"),
            StoryCategory("TIPS", "Tips & Advice"),
            StoryCategory("BEHIND_SCENES", "Behind the Scenes"),
            StoryCategory("QUESTION", "Question")
        )
    }
}

@Serializable
data class StoryVisibility(
    val id: String,
    val label: String
) {
    companion object {
        val ALL_OPTIONS = listOf(
            StoryVisibility("PUBLIC", "Everyone"),
            StoryVisibility("CONNECTIONS", "Connections Only"),
            StoryVisibility("CLOSE_FRIENDS", "Close Friends")
        )
    }
}

// Profile models are defined later in this file

// ==================== Find People Models ====================

@Serializable
data class PersonInfo(
    val id: String,
    val username: String? = null,
    val name: String? = null,
    val profileImage: String? = null,
    val bannerImageUrl: String? = null,
    val headline: String? = null,
    val college: String? = null,
    val branch: String? = null,
    val bio: String? = null,
    val skills: List<String> = emptyList(),
    val interests: List<String> = emptyList(),
    val isOnline: Boolean = false,
    val connectionStatus: String = "none", // none, pending_sent, pending_received, connected
    val mutualConnections: Int = 0,
    val contactName: String? = null,
    val isInContacts: Boolean = false,
    val verified: Boolean = false,
    val isVerified: Boolean = false,
    val profileBadgeStyle: String? = null,
    val isPremium: Boolean = false,
    val profileBoostActive: Boolean = false,
    val profileBoostEndsAt: String? = null,
    val profileBoostPriority: Int = 0,
    val discoveryPriority: Int = 0,
    val reasonCode: String? = null,
    val reasonText: String? = null,
    val source: String? = null,
    val position: Int? = null,
    val isBoosted: Boolean = false,
    val socialActors: List<RecommendationSocialActor> = emptyList()
)

@Serializable
data class PeopleResponse(
    val people: List<PersonInfo>,
    val total: Int = 0,
    val page: Int = 1,
    val totalPages: Int = 1,
    val hasMore: Boolean = false,
    val nextCursor: String? = null,
    val userCollege: String? = null // For same-college endpoint
)

@Serializable
data class PeopleYouKnowImportContact(
    val name: String? = null,
    val email: String? = null
)

@Serializable
data class PeopleYouKnowImportRequest(
    val source: String = "picker",
    val contacts: List<PeopleYouKnowImportContact> = emptyList()
)

@Serializable
data class PeopleYouKnowInvite(
    val id: String,
    val contactName: String? = null,
    val invitedAt: String? = null
)

@Serializable
data class PeopleYouKnowStats(
    val totalContacts: Int = 0,
    val matchedCount: Int = 0,
    val inviteCount: Int = 0
)

@Serializable
data class PeopleYouKnowResponse(
    val lastSyncedAt: String? = null,
    val matched: List<PersonInfo> = emptyList(),
    val invites: List<PeopleYouKnowInvite> = emptyList(),
    val stats: PeopleYouKnowStats = PeopleYouKnowStats()
)

@Serializable
data class PeopleYouKnowInviteResponse(
    val invitedAt: String
)

@Serializable
data class CollegeInfo(
    val name: String,
    val id: String? = null,
    val count: Int = 0,
    val logoUrl: String? = null,
    val domain: String? = null,
    val country: String? = null,
    val state: String? = null,
    val city: String? = null,
    val kind: String? = null,
    val source: String? = null
)

@Serializable
data class CollegeSearchResponse(
    val colleges: List<CollegeInfo>
)

@Serializable
data class SuggestionsResponse(
    val suggestions: List<PersonInfo>,
    val total: Int = 0,
    val hasMore: Boolean = false,
    val quota: SuggestionQuota? = null,
    val canRewind: Boolean = false,
    val recommendationSessionId: String? = null,
    val requestId: String? = null,
    val rankerVersion: String? = null,
    val experimentVariant: String? = null,
    val nextCursor: String? = null
)

@Serializable
data class SuggestionQuota(
    val isPremium: Boolean = false,
    val limit: Int? = null,
    val used: Int = 0,
    val remaining: Int? = null,
    val window: String = "day",
    val resetsAt: String? = null
)

@Serializable
data class DiscoveryPassRequest(
    val targetUserId: String
)

@Serializable
data class DiscoveryPassResponse(
    val message: String = "",
    val targetUserId: String? = null,
    val canRewind: Boolean = false
)

@Serializable
data class DiscoveryRewindResponse(
    val rewound: Boolean = false,
    val targetUserId: String? = null,
    val canRewind: Boolean = false
)

@Serializable
data class SaveDiscoverySearchRequest(
    val name: String,
    val filters: Map<String, String> = emptyMap(),
    val notificationsEnabled: Boolean = true,
    val digestEnabled: Boolean = true
)

@Serializable
data class UpdateSavedDiscoverySearchRequest(
    val name: String? = null,
    val filters: Map<String, String>? = null,
    val notificationsEnabled: Boolean? = null,
    val digestEnabled: Boolean? = null,
    val markViewed: Boolean? = null
)

@Serializable
data class SavedDiscoverySearch(
    val id: String,
    val name: String,
    val filters: Map<String, JsonElement> = emptyMap(),
    val notificationsEnabled: Boolean = true,
    val digestEnabled: Boolean = true,
    val unseenCount: Int = 0,
    val lastViewedAt: String? = null,
    val lastScannedAt: String? = null,
    val lastDigestSentAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class SavedDiscoverySearchesResponse(
    val searches: List<SavedDiscoverySearch> = emptyList()
)

@Serializable
data class SavedDiscoverySearchResponse(
    val search: SavedDiscoverySearch
)

@Serializable
data class SmartMatchUser(
    val id: String,
    val name: String? = null,
    val username: String? = null,
    val profileImage: String? = null,
    val headline: String? = null,
    val college: String? = null,
    val branch: String? = null,
    val graduationYear: Int? = null,
    val interests: List<String> = emptyList(),
    val bio: String? = null,
    val githubConnected: Boolean = false,
    val skills: List<String> = emptyList(),
    val onboarding: SmartMatchOnboarding? = null,
    val stats: SmartMatchStats? = null,
    val verified: Boolean = false,
    val isVerified: Boolean = false,
    val profileBadgeStyle: String? = null,
    val isPremium: Boolean = false,
    val profileBoostActive: Boolean = false,
    val profileBoostEndsAt: String? = null,
    val profileBoostPriority: Int = 0,
    val discoveryPriority: Int = 0
)

@Serializable
data class SmartMatchOnboarding(
    val primaryGoal: String? = null,
    val lookingFor: List<String> = emptyList()
)

@Serializable
data class SmartMatchStats(
    val connectionsCount: Int = 0,
    val xp: Int = 0,
    val level: Int = 1
)

@Serializable
data class SmartMatchScorecardItem(
    val label: String,
    val score: Int = 0,
    val max: Int = 0,
    val signals: List<String> = emptyList()
)

@Serializable
data class SmartMatchWhyMatched(
    val summary: String = "",
    val bullets: List<String> = emptyList(),
    val scorecard: List<SmartMatchScorecardItem> = emptyList()
)

@Serializable
data class SmartMatchSharedSignals(
    val skills: List<String> = emptyList(),
    val interests: List<String> = emptyList(),
    val goals: List<String> = emptyList(),
    val locationLabel: String? = null,
    val distanceKm: Double? = null
)

@Serializable
data class SmartMatch(
    val user: SmartMatchUser,
    val score: Double = 0.0,
    val matchPercentage: Int = 0,
    val reasons: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val whyMatched: SmartMatchWhyMatched? = null,
    val sharedSignals: SmartMatchSharedSignals? = null
)

@Serializable
data class SmartMatchResponse(
    val matches: List<SmartMatch>,
    val total: Int = 0,
    val hasMore: Boolean = false
)

@Serializable
data class FilterOptions(
    val colleges: List<String> = emptyList(),
    val branches: List<String> = emptyList(),
    val graduationYears: List<Int> = emptyList(),
    val locations: List<String> = emptyList()
)

@Serializable
data class NearbyUserLocation(
    val lat: Double,
    val lng: Double,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null
)

@Serializable
data class NearbyUser(
    val id: String,
    val name: String? = null,
    val username: String? = null,
    val profileImage: String? = null,
    val bannerImage: String? = null,
    val headline: String? = null,
    val skills: List<String> = emptyList(),
    val interests: List<String> = emptyList(),
    val distance: Double = 0.0,
    val isOnline: Boolean = false,
    val location: NearbyUserLocation? = null,
    val verified: Boolean = false,
    val isVerified: Boolean = false,
    val profileBadgeStyle: String? = null,
    val isPremium: Boolean = false
)

@Serializable
data class NearbyResponse(
    val users: List<NearbyUser>,
    val total: Int = 0,
    val locationRequired: Boolean = false,
    val yourLocation: YourLocation? = null
)

@Serializable
data class YourLocation(
    val lat: Double,
    val lng: Double,
    val city: String? = null
)

@Serializable
data class LocationUpdateRequest(
    val lat: Double,
    val lng: Double,
    val accuracy: Float? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val countryCode: String? = null
)

@Serializable
data class LocationSettingsRequest(
    val shareLocationPublic: Boolean? = null,
    val locationPermission: Boolean? = null
)

@Serializable
data class CurrentLocationResponse(
    val lat: Double? = null,
    val lng: Double? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val updatedAt: String? = null,
    val shareLocationPublic: Boolean = false,
    val locationPermission: Boolean = true
)

@Serializable
data class ConnectionRequest(
    val receiverId: String
)

@Serializable
data class ConnectionResponse(
    val connectionId: String? = null,
    val status: String = "pending_sent",
    val message: String? = null
)

@Serializable
data class ConnectionStatusResponse(
    val status: String = "none",
    val connectionId: String? = null
)

@Serializable
data class PendingConnectionRequestUser(
    val id: String,
    val username: String? = null,
    val name: String? = null,
    val profileImage: String? = null,
    val headline: String? = null,
    val college: String? = null,
    val verified: Boolean = false,
    val isVerified: Boolean = false,
    val profileBadgeStyle: String? = null,
    val isPremium: Boolean = false,
    val profileBoostActive: Boolean = false,
    val profileBoostEndsAt: String? = null,
    val profileBoostPriority: Int = 0,
    val discoveryPriority: Int = 0
)

@Serializable
data class PendingConnectionRequest(
    val id: String,
    val status: String = "PENDING",
    val message: String? = null,
    val createdAt: String = "",
    val priority: Int = 0,
    val priorityLabel: String? = null,
    val user: PendingConnectionRequestUser
)

@Serializable
data class PendingConnectionRequestsResponse(
    val connections: List<PendingConnectionRequest> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val totalPages: Int = 1,
    val hasMore: Boolean = false
)

@Serializable
data class ProfileRelationshipUser(
    val id: String,
    val username: String? = null,
    val name: String? = null,
    val profileImage: String? = null,
    val headline: String? = null,
    val college: String? = null,
    val isOnline: Boolean = false,
    val verified: Boolean = false,
    val isVerified: Boolean = false,
    val profileBadgeStyle: String? = null,
    val isPremium: Boolean = false
)

@Serializable
data class ProfileConnectionItem(
    val id: String,
    val createdAt: String = "",
    val status: String = "ACCEPTED",
    val user: ProfileRelationshipUser
)

@Serializable
data class ProfileConnectionsResponse(
    val connections: List<ProfileConnectionItem> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val totalPages: Int = 1,
    val hasMore: Boolean = false
)

@Serializable
data class ProfileFollowerItem(
    val id: String,
    val createdAt: String = "",
    val user: ProfileRelationshipUser
)

@Serializable
data class FollowersListResponse(
    val followers: List<ProfileFollowerItem> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val hasMore: Boolean = false
)

@Serializable
data class ProfileFollowingItem(
    val id: String,
    val createdAt: String = "",
    val user: ProfileRelationshipUser
)

@Serializable
data class FollowingListResponse(
    val following: List<ProfileFollowingItem> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val hasMore: Boolean = false
)

// ==================== Profile Models ====================

@Serializable
data class SocialUrl(
    val name: String,
    val url: String
)

@Serializable
data class ProfileUser(
    val id: String,
    val username: String,
    val name: String,
    val email: String? = null,
    val avatar: String? = null,
    val profileImage: String? = null,
    val bannerImageUrl: String? = null,
    val headline: String? = null,
    val bio: String? = null,
    @Serializable(with = FlexibleLocationStringSerializer::class)
    val location: String? = null,
    val college: String? = null,
    val degree: String? = null,
    val branch: String? = null,
    val currentYear: Int? = null,
    val graduationYear: Int? = null,
    val portfolioUrl: String? = null,
    val linkedinUrl: String? = null,
    val githubProfileUrl: String? = null,
    val otherSocialUrls: List<SocialUrl>? = null,
    val isOpenToOpportunities: Boolean = false,
    val isOnline: Boolean = false,
    val lastActiveAt: String? = null,
    val profileVisibility: String = "PUBLIC",
    val verified: Boolean = false,
    val isVerified: Boolean = false,
    val profileBadgeStyle: String? = null,
    val isPremium: Boolean = false,
    val canAccessProfileCustomization: Boolean = false,
    val interests: List<String> = emptyList(),
    val profileRing: String? = null,
    /** When set, visitors see this loader while opening this user's profile (client + optional API). */
    val visitLoaderGiftId: String? = null,
    val profileTheme: String = "default",
    val hasClaimedWelcomeGift: Boolean = false,
    val createdAt: String = ""
)

@Serializable
data class ProfileStats(
    val xp: Int = 0,
    val level: Int = 1,
    val xpToNextLevel: Int = 100,
    val totalPosts: Int = 0,
    val totalArticles: Int = 0,
    val totalShortVideos: Int = 0,
    val totalForumQuestions: Int = 0,
    val totalForumAnswers: Int = 0,
    val totalComments: Int = 0,
    val totalLikesReceived: Int = 0,
    val connectionsCount: Int = 0,
    val followersCount: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastActiveDate: String? = null,
    val totalActiveDays: Int = 0,
    val replyRate: Double? = null
)

@Serializable
data class LanguageStat(
    val bytes: Long = 0,
    val percentage: Double = 0.0
)

@Serializable
data class TopRepo(
    val name: String,
    val url: String,
    val stars: Int = 0,
    val forks: Int = 0,
    val language: String? = null,
    val description: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class GitHubContributionDay(
    val color: String = "#ebedf0",
    val contributionCount: Int = 0,
    val contributionLevel: String = "NONE",
    val date: String = "",
    val weekday: Int = 1
)

@Serializable
data class GitHubContributionWeek(
    val firstDay: String = "",
    val contributionDays: List<GitHubContributionDay> = emptyList()
)

@Serializable
data class GitHubContributionMonth(
    val firstDay: String = "",
    val name: String = "",
    val totalWeeks: Int = 0,
    val year: Int = 0
)

@Serializable
data class GitHubContributionCalendar(
    val colors: List<String> = emptyList(),
    val contributionYears: List<Int> = emptyList(),
    val months: List<GitHubContributionMonth> = emptyList(),
    val totalContributions: Int = 0,
    val weeks: List<GitHubContributionWeek> = emptyList()
)

@Serializable
data class GitHubStats(
    val totalPublicRepos: Int = 0,
    val totalStars: Int = 0,
    val totalForks: Int = 0,
    val followers: Int = 0,
    val following: Int = 0,
    val topLanguages: Map<String, LanguageStat> = emptyMap(),
    val topRepos: List<TopRepo> = emptyList()
)

@Serializable
data class GitHubOAuthStartResponse(
    val authUrl: String
)

@Serializable
data class GitHubSyncResponse(
    val message: String = "",
    val stats: JsonElement? = null,
    val syncedAt: String? = null
)

@Serializable
data class GitHubProfile(
    val connected: Boolean = false,
    val username: String? = null,
    val avatarUrl: String? = null,
    val profileUrl: String? = null,
    val stats: GitHubStats? = null,
    val contributionCalendar: GitHubContributionCalendar? = null,
    val lastSyncedAt: String? = null
)

@Serializable
data class ActivityHeatmapBreakdown(
    val posts: Int = 0,
    val articles: Int = 0,
    val comments: Int = 0,
    val forumQuestions: Int = 0,
    val forumAnswers: Int = 0,
    val likes: Int = 0,
    val messages: Int = 0
)

@Serializable
data class ActivityHeatmapDay(
    val date: String,
    val activityCount: Int = 0,
    val isActive: Boolean = false,
    val level: Int = 0,
    val breakdown: ActivityHeatmapBreakdown? = null
)

@Serializable
data class Skill(
    val id: String,
    val name: String,
    val category: String? = null
)

@Serializable
data class UserSkill(
    val id: String,
    val skill: Skill,
    val proficiency: String? = null,
    val yearsOfExp: Int? = null
)

@Serializable
data class Experience(
    val id: String,
    val title: String,
    val company: String,
    val type: String = "Full-time",
    val location: String? = null,
    val startDate: String,
    val endDate: String? = null,
    val isCurrent: Boolean = false,
    val description: String? = null,
    val skills: List<String> = emptyList(),
    val logo: String? = null
)

@Serializable
data class ExperienceInput(
    val title: String,
    val company: String,
    val type: String,
    val location: String? = null,
    val startDate: String,
    val endDate: String? = null,
    val isCurrent: Boolean? = null,
    val description: String? = null,
    val skills: List<String>? = null,
    val logo: String? = null
)

@Serializable
data class ExperiencesResponse(
    val experiences: List<Experience> = emptyList()
)

@Serializable
data class Education(
    val id: String,
    val school: String,
    val degree: String,
    val fieldOfStudy: String,
    val startDate: String,
    val endDate: String? = null,
    val isCurrent: Boolean = false,
    val grade: String? = null,
    val activities: String? = null,
    val description: String? = null,
    val logo: String? = null
)

@Serializable
data class EducationInput(
    val school: String,
    val degree: String,
    val fieldOfStudy: String,
    val startDate: String,
    val endDate: String? = null,
    val isCurrent: Boolean? = null,
    val grade: String? = null,
    val activities: String? = null,
    val description: String? = null,
    val logo: String? = null
)

@Serializable
data class EducationsResponse(
    val education: List<Education> = emptyList()
)

@Serializable
data class OtherLink(
    val name: String,
    val url: String
)

@Serializable
data class Project(
    val id: String,
    val name: String,
    val description: String,
    val role: String? = null,
    val techStack: List<String> = emptyList(),
    val startDate: String,
    val endDate: String? = null,
    val isCurrent: Boolean = false,
    val projectUrl: String? = null,
    val githubUrl: String? = null,
    val images: List<String> = emptyList(),
    val otherLinks: List<OtherLink>? = null,
    val featured: Boolean = false
)

@Serializable
data class ProjectInput(
    val name: String,
    val description: String? = null,
    val role: String? = null,
    val techStack: List<String>? = null,
    val startDate: String, // Required by backend
    val endDate: String? = null,
    val isCurrent: Boolean? = null,
    val projectUrl: String? = null,
    val githubUrl: String? = null,
    val images: List<String>? = null,
    val featured: Boolean? = null
)

@Serializable
data class ProjectResponse(
    val project: Project? = null,
    val message: String? = null
)

@Serializable
data class ProjectsResponse(
    val projects: List<Project> = emptyList()
)

@Serializable
data class FeatureProjectResponse(
    val featured: Boolean = false,
    val message: String? = null
)

@Serializable
data class ProjectImageUploadResponse(
    val url: String? = null,
    val imageUrl: String? = null
)

@Serializable
data class LogoUploadResponse(
    val logoUrl: String? = null,
    val url: String? = null
)

@Serializable
data class Certificate(
    val id: String,
    val name: String,
    val issuingOrg: String,
    val issueDate: String,
    val expiryDate: String? = null,
    val doesNotExpire: Boolean = false,
    val credentialId: String? = null,
    val credentialUrl: String? = null,
    val color: String? = null
)

@Serializable
data class CertificateInput(
    val name: String,
    val issuingOrg: String,
    val issueDate: String,
    val expiryDate: String? = null,
    val doesNotExpire: Boolean? = null,
    val credentialId: String? = null,
    val credentialUrl: String? = null,
    val color: String? = null
)

@Serializable
data class CertificatesResponse(
    val certificates: List<Certificate> = emptyList()
)

@Serializable
data class CertificateUploadResponse(
    val certificateUrl: String? = null,
    val url: String? = null
)

@Serializable
data class Achievement(
    val id: String,
    val title: String,
    val type: String = "Recognition",
    val organization: String,
    val date: String,
    val description: String? = null,
    val certificateUrl: String? = null,
    val color: String? = null
)

@Serializable
data class AchievementInput(
    val title: String,
    val type: String,
    val organization: String,
    val date: String,
    val description: String? = null,
    val certificateUrl: String? = null,
    val color: String? = null
)

@Serializable
data class AchievementsResponse(
    val achievements: List<Achievement> = emptyList()
)

@Serializable
data class FeedItem(
    val id: String,
    val contentType: String,
    val entityType: String? = null,
    val postType: String? = null,
    val title: String? = null,
    val content: String = "",
    val images: List<String>? = null,
    val mediaUrls: List<String>? = null,
    val videoUrl: String? = null,
    val videoThumbnail: String? = null,
    val defaultVideoId: String? = null,
    val linkUrl: String? = null,
    val linkTitle: String? = null,
    val linkDescription: String? = null,
    val linkDomain: String? = null,
    val pollOptions: List<PollOption> = emptyList(),
    val pollEndsAt: String? = null,
    val userVotedOptionId: String? = null,
    val showResultsBeforeVote: Boolean = false,
    val celebrationType: String? = null,
    val celebrationGifUrl: String? = null,
    val celebrationBadge: String? = null,
    val tags: List<String>? = null,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val viewsCount: Int = 0,
    val questionId: String? = null,
    val questionTitle: String? = null,
    val createdAt: String,
    val updatedAt: String = ""
)

@Serializable
data class RecentActivity(
    val items: List<FeedItem> = emptyList(),
    val totalCount: Int = 0,
    val hasMore: Boolean = false
)

@Serializable
data class FullProfileResponse(
    val user: ProfileUser,
    val stats: ProfileStats,
    val github: GitHubProfile = GitHubProfile(),
    val activityHeatmap: List<ActivityHeatmapDay> = emptyList(),
    val recentActivity: RecentActivity = RecentActivity(),
    val skills: List<UserSkill> = emptyList(),
    val experiences: List<Experience> = emptyList(),
    val education: List<Education> = emptyList(),
    val projects: List<Project> = emptyList(),
    val certificates: List<Certificate> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
    val viewerContext: ProfileViewerContext = ProfileViewerContext()
)

@Serializable
data class DiscoveryVisibility(
    val webDiscoveryEnabled: Boolean = true,
    val aiDiscoveryEnabled: Boolean = true,
    val discoveryVisibilityUpdatedAt: String? = null
)

@Serializable
data class DiscoveryVisibilityResponse(
    val visibility: DiscoveryVisibility
)

@Serializable
data class DiscoveryVisibilityUpdateRequest(
    val webDiscoveryEnabled: Boolean? = null,
    val aiDiscoveryEnabled: Boolean? = null
)

@Serializable
data class ProfileViewerContext(
    val isProfileSaved: Boolean = false
)

@Serializable
data class ActivityYearsResponse(
    val years: List<Int> = emptyList(),
    val joinedYear: Int = 2024
)

@Serializable
data class ActivityHeatmapStats(
    val totalContributions: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val contributionLevels: ContributionLevels = ContributionLevels()
)

@Serializable
data class ContributionLevels(
    val level0: Int = 0,
    val level1: Int = 0,
    val level2: Int = 0,
    val level3: Int = 0
)

@Serializable
data class ActivityHeatmapResponse(
    val days: List<ActivityHeatmapDay> = emptyList(),
    val stats: ActivityHeatmapStats = ActivityHeatmapStats()
)

// Follow/Connection status responses
@Serializable
data class FollowStatusResponse(
    val isFollowing: Boolean = false,
    val isFollowedBy: Boolean = false
)

@Serializable
data class MutualConnection(
    val id: String,
    val name: String? = null,
    val username: String? = null,
    val avatar: String? = null,
    val verified: Boolean = false,
    val isVerified: Boolean = false,
    val profileBadgeStyle: String? = null,
    val isPremium: Boolean = false
)

@Serializable
data class MutualInfoResponse(
    val mutualConnections: List<MutualConnection> = emptyList(),
    val mutualFollowers: List<MutualConnection> = emptyList(),
    val mutualConnectionsCount: Int = 0,
    val mutualFollowersCount: Int = 0
)

@Serializable
data class ProfileUpdateRequest(
    val name: String? = null,
    val headline: String? = null,
    val bio: String? = null,
    val location: String? = null,
    val currentYear: Int? = null,
    val degree: String? = null,
    val graduationYear: Int? = null,
    val portfolioUrl: String? = null,
    val linkedinUrl: String? = null,
    val githubProfileUrl: String? = null,
    val profileVisibility: String? = null,
    val isOpenToOpportunities: Boolean? = null,
    val interests: List<String>? = null,
    val profileRing: String? = null,
    val hasClaimedWelcomeGift: Boolean? = null,
    val visitLoaderGiftId: String? = null,
    val profileTheme: String? = null,
    val profileBadgeStyle: String? = null,
    val college: String? = null,
    val branch: String? = null
)

@Serializable
data class AvatarUpdateRequest(
    val avatarUrl: String
)

@Serializable
data class AvatarUploadUser(
    val id: String? = null,
    val username: String? = null,
    val name: String? = null,
    val profileImage: String? = null
)

@Serializable
data class AvatarUploadResponse(
    val avatar: String? = null,
    val url: String? = null,
    val avatarUrl: String? = null,
    val user: AvatarUploadUser? = null
)

@Serializable
data class BannerUpdateRequest(
    val bannerUrl: String
)

// ==================== Engagement/Streak Models ====================

@Serializable
data class StreakIsAtRisk(
    val daily: Boolean = false,
    val connection: Boolean = false,
    val login: Boolean = false,
    val posting: Boolean = false,
    val messaging: Boolean = false
)

@Serializable
data class StreakData(
    val dailyStreak: Int = 0,
    val longestDailyStreak: Int = 0,
    val dailyQualifiedToday: Boolean = false,
    val dailyIsAtRisk: Boolean = false,
    val dailyLastQualifiedDate: String? = null,
    val connectionStreak: Int = 0,
    val longestConnectionStreak: Int = 0,
    val loginStreak: Int = 0,
    val longestLoginStreak: Int = 0,
    val postingStreak: Int = 0,
    val longestPostingStreak: Int = 0,
    val messagingStreak: Int = 0,
    val longestMessagingStreak: Int = 0,
    val overallBestStreak: Int = 0,
    val weeklyConnectionsMade: Int = 0,
    val weeklyConnectionsGoal: Int = 10,
    val streakFreezes: Int = 0,
    val streakShieldActive: Boolean = false,
    val totalFreezesUsed: Int = 0,
    val isAtRisk: StreakIsAtRisk = StreakIsAtRisk(),
    val engagementScore: Int = 0,
    val showOnProfile: Boolean = true
)

@Serializable
data class StreakResponse(
    val data: StreakData
)

// ==================== Canonical Progress Models ====================

@Serializable
data class ProgressXpRule(
    val action: String = "",
    val amount: Int? = null,
    val description: String = ""
)

@Serializable
data class ProgressCoinRule(
    val action: String = "",
    val description: String = ""
)

@Serializable
data class ProgressTransaction(
    val id: String = "",
    val amount: Int = 0,
    val type: String = "",
    val source: String = "",
    val sourceId: String? = null,
    val description: String? = null,
    val createdAt: String = ""
)

@Serializable
data class ProgressXp(
    val lifetimeXp: Int = 0,
    val level: Int = 1,
    val levelName: String = "Explorer",
    val currentLevelXp: Int = 0,
    val nextLevelXp: Int = 100,
    val xpIntoLevel: Int = 0,
    val xpToNextLevel: Int = 100,
    val progressToNextLevel: Float = 0f,
    val rules: List<ProgressXpRule> = emptyList()
)

@Serializable
data class ProgressCoins(
    val balance: Int = 0,
    val rules: List<ProgressCoinRule> = emptyList(),
    val recentTransactions: List<ProgressTransaction> = emptyList()
)

@Serializable
data class ProgressStreakCategory(
    val current: Int = 0,
    val longest: Int = 0,
    val lastDate: String? = null
)

@Serializable
data class ProgressStreakCategories(
    val login: ProgressStreakCategory = ProgressStreakCategory(),
    val networking: ProgressStreakCategory = ProgressStreakCategory(),
    val posting: ProgressStreakCategory = ProgressStreakCategory(),
    val messaging: ProgressStreakCategory = ProgressStreakCategory()
)

@Serializable
data class ProgressStreak(
    val current: Int = 0,
    val longest: Int = 0,
    val qualifiedToday: Boolean = false,
    val isAtRisk: Boolean = false,
    val lastQualifiedDate: String? = null,
    val totalActiveDays: Int = 0,
    val rules: List<String> = emptyList(),
    val categories: ProgressStreakCategories = ProgressStreakCategories()
)

@Serializable
data class ProgressData(
    val xp: ProgressXp = ProgressXp(),
    val coins: ProgressCoins = ProgressCoins(),
    val streak: ProgressStreak = ProgressStreak()
)

@Serializable
data class ProgressResponse(
    val data: ProgressData = ProgressData()
)

// ==================== Variable Rewards Models (Hook Model) ====================

@Serializable
data class DailyMatchUser(
    val id: String,
    val username: String? = null,
    val name: String? = null,
    val profileImage: String? = null,
    val headline: String? = null,
    val college: String? = null,
    val isOnline: Boolean = false,
    val replyRate: Int = 75,
    val matchReason: String? = null
)

@Serializable
data class DailyMatchesData(
    val matches: List<DailyMatchUser> = emptyList(),
    val matchCount: Int = 0,
    val surpriseMessage: String = ""
)

@Serializable
data class DailyMatchesResponse(
    val data: DailyMatchesData
)

@Serializable
data class PeopleLikeYouData(
    val people: List<DailyMatchUser> = emptyList(),
    val count: Int = 0
)

@Serializable
data class PeopleLikeYouResponse(
    val data: PeopleLikeYouData
)

@Serializable
data class HiddenGemUser(
    val id: String,
    val username: String? = null,
    val name: String? = null,
    val profileImage: String? = null,
    val headline: String? = null,
    val college: String? = null,
    val isOnline: Boolean = false,
    val replyRate: Int = 85
)

@Serializable
data class HiddenGemData(
    val match: HiddenGemUser? = null,
    val message: String = ""
)

@Serializable
data class HiddenGemResponse(
    val data: HiddenGemData? = null
)

@Serializable
data class RewardCard(
    val id: String,
    val cardType: String,
    val name: String,
    val profileImage: String? = null,
    val headline: String? = null,
    val primaryReason: String,
    val secondaryMeta: String,
    val isOnline: Boolean = false,
    val badge: String? = null
)

@Serializable
data class RewardCardsResponse(
    val sessionId: String,
    val count: Int = 0,
    val cards: List<RewardCard> = emptyList()
)

@Serializable
data class RewardCardEventRequest(
    val sessionId: String,
    val cardId: String? = null,
    val cardType: String? = null,
    val action: String
)

// Trending Profile (for "You're trending today!" feature)
@Serializable
data class TrendingStatus(
    val isTrending: Boolean = false,
    val rank: Int? = null,
    val viewsToday: Int = 0,
    val message: String? = null
)

@Serializable
data class TrendingStatusResponse(
    val data: TrendingStatus
)

@Serializable
data class ProfileViewStatsApiResponse(
    val success: Boolean = true,
    val data: ProfileViewStats = ProfileViewStats()
)

@Serializable
data class ProfileViewStats(
    val totalViews: Int = 0,
    val todayViews: Int = 0,
    val weeklyViews: Int = 0,
    val trend: String = "stable",
    val viewsToday: Int = 0,
    val viewsLastHour: Int = 0,
    val viewsThisWeek: Int = 0,
    val trendPercent: Int = 0,
    val trendDirection: String = "up",
    val recentViewers: List<RecentProfileViewer> = emptyList(),
    val viewerCount: Int = 0
)

@Serializable
data class RecentProfileViewer(
    val id: String,
    val viewedAt: String,
    val source: String? = null,
    val viewer: ProfileViewerPerson
)

@Serializable
data class ProfileViewHistoryApiResponse(
    val success: Boolean = true,
    val data: ProfileViewHistory = ProfileViewHistory()
)

@Serializable
data class ProfileViewHistory(
    val page: Int = 1,
    val limit: Int = 50,
    val totalCount: Int = 0,
    val totalViews: Int = 0,
    val hasMore: Boolean = false,
    val viewers: List<ProfileViewHistoryItem> = emptyList()
)

@Serializable
data class ProfileViewHistoryItem(
    val viewerId: String,
    val lastViewedAt: String,
    val firstViewedAt: String,
    val viewCount: Int = 1,
    val isSameCollege: Boolean = false,
    val viewer: ProfileViewerPerson? = null
)

@Serializable
data class ProfileViewerPerson(
    val id: String,
    val name: String,
    val username: String,
    val profileImage: String? = null,
    val college: String? = null,
    val headline: String? = null,
    val isSameCollege: Boolean = false,
    val verified: Boolean = false,
    val isVerified: Boolean = false,
    val profileBadgeStyle: String? = null,
    val isPremium: Boolean = false
)

@Serializable
data class ProfileSaveToggleResponse(
    val success: Boolean = true,
    val data: ProfileSaveState = ProfileSaveState()
)

@Serializable
data class ProfileSaveState(
    val saved: Boolean = false,
    val savesCount: Int = 0
)

@Serializable
data class ProfileSaversApiResponse(
    val success: Boolean = true,
    val data: ProfileSavers = ProfileSavers()
)

@Serializable
data class ProfileSavers(
    val page: Int = 1,
    val limit: Int = 50,
    val totalCount: Int = 0,
    val hasMore: Boolean = false,
    val savers: List<ProfileSaverItem> = emptyList()
)

@Serializable
data class ProfileSaverItem(
    val id: String,
    val savedAt: String,
    val saver: ProfileViewerPerson
)

@Serializable
data class RecentViewedProfilesApiResponse(
    val success: Boolean = true,
    val data: RecentViewedProfiles = RecentViewedProfiles()
)

@Serializable
data class RecentViewedProfiles(
    val page: Int = 1,
    val limit: Int = 50,
    val totalCount: Int = 0,
    val totalViews: Int = 0,
    val hasMore: Boolean = false,
    val profiles: List<RecentViewedProfileItem> = emptyList()
)

@Serializable
data class RecentViewedProfileItem(
    val viewedId: String,
    val lastViewedAt: String,
    val firstViewedAt: String,
    val viewCount: Int = 1,
    val profile: ProfileViewerPerson
)

@Serializable
data class SavedProfilesResponse(
    val profiles: List<SavedProfileItem> = emptyList(),
    val nextCursor: String? = null,
    val hasMore: Boolean = false
)

@Serializable
data class SavedProfileItem(
    val id: String,
    val savedAt: String,
    val user: ProfileViewerPerson
)

@Serializable
data class ProfileInsightsApiResponse(
    val success: Boolean = true,
    val data: ProfileInsights = ProfileInsights()
)

@Serializable
data class ProfileInsights(
    val analytics: ProfileAnalytics = ProfileAnalytics(),
    val matchInsights: MatchInsights = MatchInsights()
)

@Serializable
data class ProfileAnalytics(
    val views: ProfileAnalyticsViews = ProfileAnalyticsViews(),
    val searchAppearances: ProfileAnalyticsMetric = ProfileAnalyticsMetric(),
    val suggestionAppearances: ProfileAnalyticsMetric = ProfileAnalyticsMetric(),
    val profileSaves: ProfileAnalyticsMetric = ProfileAnalyticsMetric(),
    val matchRate: ProfileMatchRate = ProfileMatchRate()
)

@Serializable
data class ProfileAnalyticsViews(
    val total: Int = 0,
    val today: Int = 0,
    val last7Days: Int = 0,
    val last30Days: Int = 0,
    val unique: Int = 0,
    val trendPercent: Int = 0,
    val trendDirection: String = "stable"
)

@Serializable
data class ProfileAnalyticsMetric(
    val total: Int = 0,
    val last7Days: Int = 0,
    val last30Days: Int = 0
)

@Serializable
data class ProfileMatchRate(
    val value: Double = 0.0,
    val display: String = "0%",
    val connectionRequests: Int = 0,
    val acceptedConnections: Int = 0,
    val appearances: Int = 0
)

@Serializable
data class MatchInsights(
    val reasons: List<String> = emptyList(),
    val topTags: List<MatchInsightTag> = emptyList()
)

@Serializable
data class MatchInsightTag(
    val label: String,
    val weight: Int = 0,
    val source: String = ""
)

// ==================== Chat / Messaging Models ====================

@Serializable
data class ChatUser(
    val id: String,
    val username: String? = null,
    val name: String? = null,
    val profileImage: String? = null,
    val isOnline: Boolean = false,
    val lastActiveAt: String? = null,
    val verified: Boolean = false,
    val isVerified: Boolean = false,
    val profileBadgeStyle: String? = null,
    val isPremium: Boolean = false
)

@Serializable
data class MessageReaction(
    val id: String,
    val userId: String,
    val emoji: String,
    val user: ChatUserReaction? = null
)

@Serializable
data class ChatUserReaction(
    val id: String,
    val username: String = "",
    val name: String = ""
)

@Serializable
data class ReplyTo(
    val id: String,
    val content: String,
    val contentType: String,
    val senderId: String
)

@Serializable
data class Message(
    val id: String,
    val clientMessageId: String? = null,
    val conversationId: String,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val contentType: String = "text",
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val fileName: String? = null,
    val fileSize: Int? = null,
    val status: String = "SENT",
    val deliveredAt: String? = null,
    val readAt: String? = null,
    val isDeleted: Boolean = false,
    val replyToId: String? = null,
    val replyTo: ReplyTo? = null,
    val sender: ChatUser? = null,
    val reactions: List<MessageReaction> = emptyList(),
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class ConversationLastMessage(
    val id: String,
    val content: String,
    val contentType: String,
    val senderId: String,
    val status: String,
    val createdAt: String
)

@Serializable
data class Conversation(
    val id: String,
    val participant1Id: String,
    val participant2Id: String,
    val participant1: ChatUser,
    val participant2: ChatUser,
    val otherParticipant: ChatUser,
    val lastMessage: ConversationLastMessage? = null,
    val lastMessageAt: String? = null,
    val unreadCount: Int = 0,
    val createdAt: String,
    val updatedAt: String,
    val isMessageRequest: Boolean = false
)

@Serializable
data class ConversationStatusResponse(
    val hasConversation: Boolean = false,
    val hasMessages: Boolean = false,
    val conversation: Conversation? = null
)

@Serializable
data class ConversationsResponse(
    val conversations: List<Conversation>,
    val hasMore: Boolean = false,
    val nextCursor: String? = null
)

@Serializable
data class MessagesResponse(
    val messages: List<Message>,
    val hasMore: Boolean = false,
    val nextCursor: String? = null
)

@Serializable
data class ChatSyncResponse(
    val messages: List<Message> = emptyList(),
    val statusChanges: List<Message> = emptyList(),
    val conversations: List<Conversation> = emptyList(),
    val cursor: String,
    val hasMore: Boolean = false
)

@Serializable
data class MessageLimitStatus(
    val canSend: Boolean,
    val isConnected: Boolean,
    val messagesSent: Int = 0,
    val messagesRemaining: Int = -1,
    val limit: Int = -1
)

@Serializable
data class MessageRequestsResponse(
    val messageRequests: List<Conversation>,
    val hasMore: Boolean = false,
    val nextCursor: String? = null
)

@Serializable
data class CreateConversationRequest(
    val participantId: String
)

@Serializable
data class SendMessageRequest(
    val content: String = "",
    val contentType: String = "text",
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val fileName: String? = null,
    val fileSize: Int? = null,
    val replyToId: String? = null,
    val clientMessageId: String? = null
)

@Serializable
data class UploadChatMediaResponse(
    val mediaUrl: String,
    val fileName: String,
    val fileSize: Int,
    val mediaType: String
)

@Serializable
data class EditMessageRequest(
    val content: String
)

// Shared post content parsed from message content
@Serializable
data class SharedPostAuthor(
    val id: String? = null,
    val name: String? = null,
    val username: String? = null,
    val profileImage: String? = null,
    val verified: Boolean = false,
    val isVerified: Boolean = false,
    val profileBadgeStyle: String? = null,
    val isPremium: Boolean = false
)

@Serializable
data class SharedPostContent(
    val type: String = "",
    val postId: String = "",
    val postUrl: String = "",
    val reelId: String = "",
    val reelUrl: String = "",
    val preview: String = "",
    val author: SharedPostAuthor? = null,
    val mediaUrl: String? = null,
    val videoUrl: String? = null,
    val hlsUrl: String? = null,
    val previewGifUrl: String? = null,
    val title: String? = null,
    val caption: String? = null,
    val durationSeconds: Int = 0,
    val width: Int = 1080,
    val height: Int = 1920,
    val hashtags: List<String> = emptyList()
) {
    companion object {
        private val lenientJson = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
        
        fun tryParse(content: String): SharedPostContent? {
            return try {
                if (
                    content.contains("\"type\":\"shared_post\"") ||
                    content.contains("\"type\": \"shared_post\"") ||
                    content.contains("\"type\":\"shared_reel\"") ||
                    content.contains("\"type\": \"shared_reel\"")
                ) {
                    lenientJson.decodeFromString<SharedPostContent>(content)
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }
}

@Serializable
data class AddReactionRequest(
    val emoji: String
)

@Serializable
data class DeleteMessageRequest(
    val forEveryone: Boolean = false
)

@Serializable
data class MarkAsReadResponse(
    val updatedCount: Int = 0,
    val readAt: String = ""
)

@Serializable
data class UnreadCountResponse(
    val unreadCount: Int = 0
)

@Serializable
data class MessageRequestsCountResponse(
    val count: Int = 0
)

@Serializable
data class AcceptMessageRequestResponse(
    val message: String = "",
    val conversation: Conversation
)

@Serializable
data class SearchMessagesResponse(
    val messages: List<Message> = emptyList()
)

// ==================== Reels Models ====================

@Serializable
data class ReelAuthor(
    val id: String,
    val username: String? = null,
    val name: String? = null,
    val profileImage: String? = null,
    val headline: String? = null,
    val isFollowing: Boolean = false,
    val verified: Boolean = false,
    val isVerified: Boolean = false,
    val profileBadgeStyle: String? = null,
    val isPremium: Boolean = false
)

@Serializable
data class ReelAudio(
    val id: String,
    val title: String,
    val artist: String? = null,
    val albumArt: String? = null
)

@Serializable
data class ReelPollOption(
    val id: Int,
    val text: String,
    val votes: Int = 0
)

@Serializable
data class Reel(
    val id: String,
    val author: ReelAuthor,
    val videoId: String = "",
    val videoUrl: String,
    val hlsUrl: String? = null,
    val thumbnailUrl: String? = null,
    val previewGifUrl: String? = null,
    val title: String? = null,
    val caption: String? = null,
    val durationSeconds: Int = 0,
    val width: Int = 1080,
    val height: Int = 1920,
    val aspectRatio: String = "9:16",
    val audio: ReelAudio? = null,
    val hashtags: List<String> = emptyList(),
    val mentions: List<String> = emptyList(),
    val skills: List<String> = emptyList(),
    val topics: List<String> = emptyList(),
    val category: String? = null,
    val locationName: String? = null,
    val isResponse: Boolean = false,
    val responseType: String? = null,
    val originalReelId: String? = null,
    val pollQuestion: String? = null,
    val pollOptions: List<ReelPollOption>? = null,
    val pollEndsAt: String? = null,
    val userVotedOption: Int? = null,
    val quizQuestion: String? = null,
    val quizOptions: List<ReelPollOption>? = null,
    val codeSnippet: String? = null,
    val codeLanguage: String? = null,
    val codeFileName: String? = null,
    val repoUrl: String? = null,
    val visibility: String = "PUBLIC",
    val allowComments: Boolean = true,
    val allowDuets: Boolean = true,
    val allowStitch: Boolean = true,
    val allowDownload: Boolean = true,
    val allowSharing: Boolean = true,
    val muteOriginalAudio: Boolean = false,
    val status: String = "PUBLISHED",
    val viewsCount: Int = 0,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val sharesCount: Int = 0,
    val savesCount: Int = 0,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val publishedAt: String? = null,
    val createdAt: String = "",
    val updatedAt: String? = null,
    val reasonCode: String? = null,
    val reasonText: String? = null,
    val source: String? = null,
    val position: Int? = null,
    val isBoosted: Boolean = false,
    val socialActors: List<RecommendationSocialActor> = emptyList()
)

@Serializable
data class ReelsFeedResponse(
    val reels: List<Reel>,
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
    val adPlacements: List<ManagedAdPlacement> = emptyList(),
    val recommendationSessionId: String? = null,
    val requestId: String? = null,
    val rankerVersion: String? = null,
    val experimentVariant: String? = null,
    val recommendationNextCursor: String? = null
)

@Serializable
data class ManagedAdPlacement(
    val placement: String,
    val sequence: Int = 0,
    val afterItemCount: Int = 0,
    val slotKey: String,
    val campaignId: String,
    val sponsorName: String,
    val ctaText: String? = null,
    val ctaKind: String? = null,
    val ctaUrl: String? = null,
    val feedTitle: String? = null,
    val feedBody: String? = null,
    val feedImageUrl: String? = null,
    val reelCaption: String? = null,
    val reelsVideoUrl: String? = null,
    val reelsHlsUrl: String? = null,
    val reelsThumbnailUrl: String? = null
)

@Serializable
data class ManagedAdEventRequest(
    val placement: String,
    val slotKey: String,
    val sessionId: String
)

@Serializable
data class ReelLikeResponse(
    val liked: Boolean,
    val likesCount: Int
)

@Serializable
data class ReelSaveResponse(
    val saved: Boolean,
    val savesCount: Int
)

@Serializable
data class ReelComment(
    val id: String,
    val reelId: String,
    val parentId: String? = null,
    val author: ReelAuthor,
    val content: String,
    val mentions: List<String> = emptyList(),
    val likesCount: Int = 0,
    val repliesCount: Int = 0,
    val isLiked: Boolean = false,
    val isPinned: Boolean = false,
    val isAuthorHeart: Boolean = false,
    val createdAt: String = ""
)

@Serializable
data class ReelCommentsResponse(
    val comments: List<ReelComment>,
    val nextCursor: String? = null,
    val hasMore: Boolean = false
)

@Serializable
data class ReelViewRequest(
    val watchTimeMs: Long,
    val completed: Boolean,
    val source: String = "feed"
)

@Serializable
data class ReelPollVoteResponse(
    val success: Boolean,
    val pollOptions: List<ReelPollOption>,
    val userVotedOption: Int
)

// ==================== Retention Feature Models ====================

// Weekly Goals (Zeigarnik Effect)
@Serializable
data class WeeklyGoal(
    val id: String,
    val type: String, // "connections", "posts", "messages"
    val label: String, // "Connections", "Posts", "Messages"
    val current: Int = 0,
    val target: Int = 10,
    val isComplete: Boolean = false
)

@Serializable
data class WeeklyGoalsData(
    val goals: List<WeeklyGoal> = emptyList(),
    val totalProgress: Float = 0f, // 0.0 to 1.0
    val weekStartDate: String = "",
    val weekEndDate: String = "",
    val streakAtRisk: Boolean = false,
    val reminderMessage: String = ""
)

@Serializable
data class WeeklyGoalsResponse(
    val data: WeeklyGoalsData
)

// Top Networkers Leaderboard (Social Proof)
@Serializable
data class LeaderboardUser(
    val rank: Int,
    val userId: String,
    val username: String? = null,
    val name: String? = null,
    val profileImage: String? = null,
    val headline: String? = null,
    val score: Int = 0,
    val connectionsThisPeriod: Int = 0,
    val isCurrentUser: Boolean = false
)

@Serializable
data class LeaderboardData(
    val users: List<LeaderboardUser> = emptyList(),
    val period: String = "week", // "week" or "month"
    val currentUserRank: Int? = null,
    val totalParticipants: Int = 0
)

@Serializable
data class LeaderboardResponse(
    val data: LeaderboardData
)

// Live Activity Count (Social Proof / FOMO)
@Serializable
data class LiveActivityData(
    val activeNow: Int = 0,
    val location: String? = null, // "Worldwide" or city name
    val label: String = "people networking",
    val recentJoins: List<RecentJoinUser> = emptyList()
)

@Serializable
data class RecentJoinUser(
    val id: String,
    val name: String? = null,
    val profileImage: String? = null,
    val joinedAt: String = ""
)

@Serializable
data class LiveActivityResponse(
    val data: LiveActivityData
)

// Session Summary (Peak-End Rule)
@Serializable
data class SessionSummaryData(
    val connectionsMadeToday: Int = 0,
    val messagesExchanged: Int = 0,
    val profileViews: Int = 0,
    val streakPreserved: Boolean = false,
    val currentStreak: Int = 0,
    val achievements: List<String> = emptyList(),
    val motivationalMessage: String = ""
)

@Serializable
data class SessionSummaryResponse(
    val data: SessionSummaryData
)

// Connection Celebration (Peak Moment)
@Serializable
data class ConnectionCelebrationData(
    val showCelebration: Boolean = false,
    val connectedUser: CelebratedUser? = null,
    val message: String = "",
    val milestoneReached: Boolean = false,
    val milestoneType: String? = null, // "first", "10th", "50th", etc.
    val xpEarned: Int = 0
)

@Serializable
data class CelebratedUser(
    val id: String,
    val name: String? = null,
    val profileImage: String? = null,
    val headline: String? = null,
    val mutualConnections: Int = 0
)

@Serializable
data class ConnectionCelebrationResponse(
    val data: ConnectionCelebrationData
)

// Connection Request Limits (Scarcity)
@Serializable
data class ConnectionLimitData(
    val canSend: Boolean = true,
    val used: Int = 0,
    val limit: Int = 10,
    val remaining: Int = 10,
    val isPremium: Boolean = false,
    val resetsAt: String? = null,
    val unlimitedRequests: Boolean = false,
    val window: String = "day"
)

@Serializable
data class ConnectionLimitResponse(
    val data: ConnectionLimitData
)

// Match with Expiry (Scarcity + Urgency)
@Serializable
data class ExpiringMatch(
    val matchId: String,
    val user: DailyMatchUser,
    val expiresAt: String,
    val hoursRemaining: Int = 24,
    val minutesRemaining: Int = 0,
    val isExpired: Boolean = false,
    val message: String = ""
)

@Serializable
data class ExpiringMatchesData(
    val matches: List<ExpiringMatch> = emptyList(),
    val totalExpiring: Int = 0
)

@Serializable
data class ExpiringMatchesResponse(
    val data: ExpiringMatchesData
)

// Engagement Dashboard (combines multiple retention features)
@Serializable
data class EngagementDashboardData(
    val streaks: StreakData = StreakData(),
    val weeklyGoals: WeeklyGoalsData = WeeklyGoalsData(),
    val liveActivity: LiveActivityData = LiveActivityData(),
    val connectionLimit: ConnectionLimitData = ConnectionLimitData()
)

@Serializable
data class EngagementDashboardResponse(
    val data: EngagementDashboardData
)

// ==================== Notification Models ====================

@Serializable
data class Notification(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val isRead: Boolean = false,
    val createdAt: String,
    val readAt: String? = null,
    val actor: NotificationActor? = null,
    val post: NotificationPost? = null,
    val reel: NotificationReel? = null,
    val data: Map<String, JsonElement>? = null
)

@Serializable
data class NotificationActor(
    val id: String,
    val username: String? = null,
    val name: String? = null,
    val profileImage: String? = null,
    val verified: Boolean = false,
    val isVerified: Boolean = false,
    val profileBadgeStyle: String? = null,
    val isPremium: Boolean = false
)

@Serializable
data class NotificationPost(
    val id: String,
    val content: String? = null,
    val mediaUrls: List<String>? = null
)

@Serializable
data class NotificationReel(
    val id: String,
    val title: String? = null,
    val thumbnailUrl: String? = null
)

@Serializable
data class NotificationsResponse(
    val notifications: List<Notification> = emptyList(),
    val latestCursor: String? = null,
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
    val hasMoreNewer: Boolean = false
)

@Serializable
data class PostCollabResponse(
    val success: Boolean = false,
    val status: String? = null,
    val post: Post? = null
)

@Serializable
data class NotificationSettings(
    val pushEnabled: Boolean = true,
    val messagesEnabled: Boolean = true,
    val connectionsEnabled: Boolean = true,
    val likesEnabled: Boolean = true,
    val commentsEnabled: Boolean = true,
    val mentionsEnabled: Boolean = true,
    val followsEnabled: Boolean = true,
    val matchAlertsEnabled: Boolean = true,
    val streakRemindersEnabled: Boolean = true,
    val dailyDigestEnabled: Boolean = true,
    val weeklySummaryEnabled: Boolean = true
)

@Serializable
data class IdentityEmailState(
    val verified: Boolean = false,
    val masked: String? = null
)

@Serializable
data class IdentityPhoneState(
    val verified: Boolean = false,
    val masked: String? = null,
    val verifiedAt: String? = null
)

@Serializable
data class IdentitySafetyState(
    val restrictedUntil: String? = null,
    val restrictionReason: String? = null,
    val suspendedUntil: String? = null
)

@Serializable
data class IdentityVerificationRecord(
    val id: String,
    val type: String,
    val status: String,
    val valueMasked: String? = null,
    val requestedAt: String? = null,
    val verifiedAt: String? = null,
    val expiresAt: String? = null,
    val reviewedById: String? = null,
    val evidenceDeletedAt: String? = null,
    val evidenceFileName: String? = null,
    val evidenceMimeType: String? = null,
    val evidenceSize: Int? = null,
    val evidenceHasFile: Boolean = false,
    val rejectionReason: String? = null,
    val reviewNotes: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class IdentitySummary(
    val trustLevel: String = "BASIC",
    val email: IdentityEmailState = IdentityEmailState(),
    val phone: IdentityPhoneState = IdentityPhoneState(),
    val safety: IdentitySafetyState = IdentitySafetyState(),
    val verifications: List<IdentityVerificationRecord> = emptyList()
)

@Serializable
data class IdentityMeResponse(
    val identity: IdentitySummary
)

@Serializable
data class PhoneVerifyRequest(
    val idToken: String
)

@Serializable
data class IdentityMutationResponse(
    val message: String,
    val identity: IdentitySummary? = null
)

@Serializable
data class ClaimStudentBadgeResponse(
    val message: String,
    val profileBadgeStyle: String? = null,
    val identity: IdentitySummary? = null
)

@Serializable
data class StudentEmailRequestBody(
    val studentEmail: String
)

@Serializable
data class StudentEmailRequestResponse(
    val message: String,
    val expiresAt: String,
    val studentEmail: String
)

@Serializable
data class StudentEmailConfirmRequest(
    val studentEmail: String,
    val code: String
)

@Serializable
data class IdUploadRequestResponse(
    val verificationId: String,
    val uploadMode: String,
    val submitUrl: String,
    val fieldName: String,
    val maxBytes: Long,
    val allowedMimeTypes: List<String> = emptyList(),
    val expiresAt: String? = null
)

@Serializable
data class IdSubmitResponse(
    val message: String,
    val verificationId: String,
    val status: String
)

@Serializable
data class BlockedUser(
    val id: String,
    val username: String? = null,
    val name: String? = null,
    val profileImage: String? = null,
    val isVerified: Boolean = false,
    val profileBadgeStyle: String? = null,
    val identityTrustLevel: String = "BASIC",
    val verificationBadges: List<String> = emptyList()
)

@Serializable
data class UserBlock(
    val id: String,
    val blockedUserId: String,
    val createdAt: String,
    val user: BlockedUser
)

@Serializable
data class BlocksResponse(
    val blocks: List<UserBlock> = emptyList()
)

@Serializable
data class MySafetyReport(
    val id: String,
    val reportType: String,
    val reason: String,
    val status: String,
    val conversationId: String? = null,
    val reportedPostId: String? = null,
    val reportedCommentId: String? = null,
    val reportedUserId: String? = null,
    val reportedGroupId: String? = null,
    val blockedUserAfterReport: Boolean = false,
    val createdAt: String
)

@Serializable
data class MyReportsPagination(
    val page: Int = 1,
    val limit: Int = 10,
    val total: Int = 0,
    val totalPages: Int = 0
)

@Serializable
data class MyReportsResponse(
    val reports: List<MySafetyReport> = emptyList(),
    val pagination: MyReportsPagination = MyReportsPagination()
)

@Serializable
data class BlockUserRequest(
    val reason: String? = null
)

@Serializable
data class BlockUserResponse(
    val message: String,
    val block: UserBlock? = null
)

@Serializable
data class ReportChatRequest(
    val reason: String,
    val description: String = "",
    val blockUser: Boolean = false
)

@Serializable
data class ReportUserRequest(
    val reason: String,
    val description: String? = null,
    val blockUser: Boolean = false
)
