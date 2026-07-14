package com.kyant.backdrop.catalog.network.models

import kotlinx.serialization.Serializable

@Serializable
data class TalkConversationItem(
    val role: String,
    val content: String
)

@Serializable
data class TalkClientContext(
    val surface: String = "talk_with_vormex",
    val wantsPeople: Boolean? = null
)

@Serializable
data class TalkTurnRequest(
    val message: String,
    val history: List<TalkConversationItem> = emptyList(),
    val clientContext: TalkClientContext = TalkClientContext()
)

@Serializable
data class TalkPersonCard(
    val id: String,
    val username: String,
    val name: String,
    val profileImage: String? = null,
    val bannerImageUrl: String? = null,
    val headline: String? = null,
    val college: String? = null,
    val branch: String? = null,
    val bio: String? = null,
    val skills: List<String> = emptyList(),
    val interests: List<String> = emptyList(),
    val isOnline: Boolean = false,
    val connectionStatus: String = "none",
    val mutualConnections: Int = 0,
    val matchScore: Int = 0,
    val reasons: List<String> = emptyList(),
    val connectReason: String? = null
)

@Serializable
data class TalkTurnResponse(
    val answer: String,
    val mode: String = "general",
    val peopleTitle: String? = null,
    val people: List<TalkPersonCard> = emptyList(),
    val followUpQuestions: List<String> = emptyList(),
    val costMode: String? = null
)
