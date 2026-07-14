package com.kyant.backdrop.catalog.network.models

import kotlinx.serialization.Serializable

@Serializable
data class SkillPerson(
    val id: String = "",
    val username: String? = null,
    val name: String? = null,
    val profileImage: String? = null,
    val headline: String? = null,
    val college: String? = null,
    val branch: String? = null,
    val graduationYear: Int? = null,
    val isOnline: Boolean = false,
    val lastActiveAt: String? = null
)

@Serializable
data class SkillPassportUser(
    val id: String = "",
    val username: String? = null,
    val name: String? = null,
    val profileImage: String? = null,
    val headline: String? = null,
    val college: String? = null,
    val branch: String? = null,
    val graduationYear: Int? = null,
    val isOnline: Boolean = false,
    val lastActiveAt: String? = null,
    val bio: String? = null,
    val githubConnected: Boolean = false,
    val githubUsername: String? = null,
    val githubProfileUrl: String? = null
)

@Serializable
data class SkillPassportSummary(
    val totalSkills: Int = 0,
    val verifiedSkills: Int = 0,
    val evidenceCount: Int = 0,
    val endorsementsCount: Int = 0,
    val passportScore: Int = 0,
    val topCategory: String? = null,
    val verificationLinksCount: Int = 0,
    val hasVerifiedSkillsBadge: Boolean = false,
    val isPremium: Boolean = false
)

@Serializable
data class SkillPassportEvidence(
    val id: String = "",
    val type: String = "",
    val title: String = "",
    val subtitle: String? = null,
    val skillName: String = "",
    val sourceUrl: String? = null,
    val verified: Boolean = false,
    val createdAt: String? = null
)

@Serializable
data class SkillPassportEndorsement(
    val id: String = "",
    val skillName: String = "",
    val note: String? = null,
    val rating: Int? = null,
    val source: String = "",
    val createdAt: String? = null,
    val endorsedBy: SkillPerson? = null
)

@Serializable
data class SkillPassportSkill(
    val id: String = "",
    val name: String = "",
    val category: String? = null,
    val proficiency: String? = null,
    val yearsOfExp: Int? = null,
    val canTeach: Boolean = false,
    val wantsToLearn: Boolean = false,
    val evidenceCount: Int = 0,
    val endorsementCount: Int = 0,
    val verifiedEvidenceCount: Int = 0,
    val confidenceScore: Int = 0,
    val sources: List<String> = emptyList(),
    val evidence: List<SkillPassportEvidence> = emptyList(),
    val endorsements: List<SkillPassportEndorsement> = emptyList()
)

@Serializable
data class SkillPassportResponse(
    val user: SkillPassportUser = SkillPassportUser(),
    val summary: SkillPassportSummary = SkillPassportSummary(),
    val learningGoals: List<String> = emptyList(),
    val teachingSkills: List<String> = emptyList(),
    val skills: List<SkillPassportSkill> = emptyList(),
    val recentEvidence: List<SkillPassportEvidence> = emptyList(),
    val recentEndorsements: List<SkillPassportEndorsement> = emptyList(),
    val verificationLinks: List<SkillVerificationLink> = emptyList()
)

@Serializable
data class SkillVerificationLink(
    val id: String = "",
    val provider: String = "",
    val username: String = "",
    val profileUrl: String? = null,
    val status: String = "verified",
    val verifiedAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class SkillVerificationLinkRequest(
    val provider: String,
    val username: String,
    val profileUrl: String? = null
)

@Serializable
data class SkillVerificationLinkResponse(
    val verificationLink: SkillVerificationLink = SkillVerificationLink()
)

@Serializable
data class SkillEndorseRequest(
    val skillName: String,
    val note: String? = null,
    val rating: Int? = null
)

@Serializable
data class SkillEndorseResponse(
    val endorsement: SkillPassportEndorsement = SkillPassportEndorsement()
)

@Serializable
data class SkillSwapSharedContext(
    val sameCampus: Boolean = false,
    val sameBranch: Boolean = false,
    val college: String? = null
)

@Serializable
data class SkillSwapSuggestion(
    val user: SkillPerson = SkillPerson(),
    val skill: String = "",
    val mode: String = "learn",
    val direction: String = "learn_from",
    val matchScore: Int = 0,
    val evidenceCount: Int = 0,
    val matchReason: String = "",
    val sharedContext: SkillSwapSharedContext = SkillSwapSharedContext(),
    val activeRequestStatus: String? = null
)

@Serializable
data class SkillSwapSuggestionsResponse(
    val mode: String = "learn",
    val featuredSkills: List<String> = emptyList(),
    val suggestions: List<SkillSwapSuggestion> = emptyList()
)

@Serializable
data class SkillSwapSession(
    val id: String = "",
    val requestId: String = "",
    val skill: String = "",
    val status: String = "scheduled",
    val sessionLengthMinutes: Int = 30,
    val scheduledFor: String? = null,
    val completedAt: String? = null,
    val learnerRating: Int? = null,
    val mentorRating: Int? = null,
    val learnerNote: String? = null,
    val mentorNote: String? = null,
    val createdAt: String? = null,
    val mentor: SkillPerson? = null,
    val learner: SkillPerson? = null
)

@Serializable
data class SkillSwapRequestItem(
    val id: String = "",
    val skill: String = "",
    val message: String? = null,
    val requesterGoal: String? = null,
    val mode: String = "learn",
    val status: String = "pending",
    val sessionLengthMinutes: Int = 30,
    val scheduledFor: String? = null,
    val createdAt: String? = null,
    val respondedAt: String? = null,
    val requester: SkillPerson? = null,
    val recipient: SkillPerson? = null,
    val session: SkillSwapSession? = null
)

@Serializable
data class SkillSwapStateResponse(
    val incoming: List<SkillSwapRequestItem> = emptyList(),
    val outgoing: List<SkillSwapRequestItem> = emptyList(),
    val history: List<SkillSwapRequestItem> = emptyList(),
    val sessions: List<SkillSwapSession> = emptyList()
)

@Serializable
data class SkillSwapCreateRequest(
    val recipientId: String,
    val skill: String,
    val message: String? = null,
    val requesterGoal: String? = null,
    val mode: String = "learn",
    val sessionLengthMinutes: Int = 30,
    val scheduledFor: String? = null
)

@Serializable
data class SkillSwapRespondRequest(
    val action: String
)

@Serializable
data class SkillSwapCompleteRequest(
    val rating: Int = 5,
    val note: String? = null,
    val endorseSkill: Boolean = true
)

@Serializable
data class SkillSwapRequestResponse(
    val request: SkillSwapRequestItem = SkillSwapRequestItem(),
    val session: SkillSwapSession? = null
)

@Serializable
data class SkillSwapSessionResponse(
    val session: SkillSwapSession = SkillSwapSession(),
    val endorsement: SkillPassportEndorsement? = null
)
