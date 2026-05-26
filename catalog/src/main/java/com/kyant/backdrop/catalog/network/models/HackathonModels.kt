package com.kyant.backdrop.catalog.network.models

import kotlinx.serialization.Serializable

@Serializable
data class HackathonUser(
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
data class Hackathon(
    val id: String = "",
    val slug: String = "",
    val title: String = "",
    val organizer: String? = null,
    val source: String = "college_fest",
    val sourceUrl: String? = null,
    val sourceId: String? = null,
    val college: String? = null,
    val description: String = "",
    val theme: String? = null,
    val location: String? = null,
    val isOnline: Boolean = false,
    val startsAt: String = "",
    val endsAt: String = "",
    val registrationDeadline: String? = null,
    val teamMin: Int = 1,
    val teamMax: Int = 4,
    val prizeSummary: String? = null,
    val tags: List<String> = emptyList(),
    val skills: List<String> = emptyList(),
    val bannerUrl: String? = null,
    val status: String = "active",
    val teamsCount: Int = 0,
    val savesCount: Int = 0,
    val isSaved: Boolean = false,
    val myTeam: HackathonTeam? = null,
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Serializable
data class HackathonTeamMember(
    val id: String = "",
    val userId: String = "",
    val role: String = "member",
    val status: String = "accepted",
    val joinedAt: String? = null,
    val user: HackathonUser? = null
)

@Serializable
data class HackathonTeamApplication(
    val id: String = "",
    val teamId: String = "",
    val applicantId: String = "",
    val applicant: HackathonUser? = null,
    val role: String? = null,
    val message: String? = null,
    val skills: List<String> = emptyList(),
    val status: String = "pending",
    val respondedAt: String? = null,
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Serializable
data class HackathonTeam(
    val id: String = "",
    val hackathonId: String = "",
    val ownerId: String = "",
    val owner: HackathonUser? = null,
    val groupId: String? = null,
    val name: String = "",
    val pitch: String? = null,
    val lookingForRoles: List<String> = emptyList(),
    val requiredSkills: List<String> = emptyList(),
    val maxMembers: Int = 4,
    val status: String = "open",
    val memberCount: Int = 0,
    val pendingApplicationsCount: Int = 0,
    val members: List<HackathonTeamMember> = emptyList(),
    val myApplication: HackathonTeamApplication? = null,
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Serializable
data class HackathonsResponse(
    val hackathons: List<Hackathon> = emptyList(),
    val page: Int = 1,
    val total: Int = 0,
    val totalPages: Int = 0,
    val hasMore: Boolean = false
)

@Serializable
data class HackathonResponse(
    val hackathon: Hackathon = Hackathon()
)

@Serializable
data class HackathonTeamsResponse(
    val teams: List<HackathonTeam> = emptyList()
)

@Serializable
data class HackathonTeamResponse(
    val team: HackathonTeam = HackathonTeam(),
    val created: Boolean = false
)

@Serializable
data class HackathonApplicationResponse(
    val application: HackathonTeamApplication = HackathonTeamApplication()
)

@Serializable
data class MyHackathonTeamsResponse(
    val teams: List<HackathonTeam> = emptyList(),
    val applications: List<HackathonTeamApplication> = emptyList()
)

@Serializable
data class SaveHackathonResponse(
    val saved: Boolean = false
)

@Serializable
data class CreateHackathonRequest(
    val title: String,
    val description: String,
    val startsAt: String,
    val endsAt: String,
    val organizer: String? = null,
    val source: String = "college_fest",
    val sourceUrl: String? = null,
    val college: String? = null,
    val theme: String? = null,
    val location: String? = null,
    val isOnline: Boolean = false,
    val registrationDeadline: String? = null,
    val teamMin: Int = 1,
    val teamMax: Int = 4,
    val prizeSummary: String? = null,
    val tags: List<String> = emptyList(),
    val skills: List<String> = emptyList(),
    val bannerUrl: String? = null
)

@Serializable
data class FormHackathonTeamRequest(
    val name: String? = null,
    val pitch: String? = null,
    val lookingForRoles: List<String> = emptyList(),
    val requiredSkills: List<String> = emptyList(),
    val maxMembers: Int? = null
)

@Serializable
data class ApplyHackathonTeamRequest(
    val role: String? = null,
    val message: String? = null,
    val skills: List<String> = emptyList()
)

@Serializable
data class RespondHackathonApplicationRequest(
    val action: String
)

@Serializable
data class CollegeCommunity(
    val id: String = "",
    val college: String = "",
    val slug: String = "",
    val description: String? = null,
    val groupId: String = "",
    val emailDomains: List<String> = emptyList(),
    val verificationMode: String = "profile_college",
    val memberCount: Int = 0,
    val isMember: Boolean = false,
    val memberRole: String? = null,
    val verificationStatus: String? = null,
    val canJoin: Boolean = false,
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Serializable
data class CollegeCommunitiesResponse(
    val communities: List<CollegeCommunity> = emptyList()
)

@Serializable
data class CollegeCommunityResponse(
    val community: CollegeCommunity = CollegeCommunity(),
    val created: Boolean = false
)

@Serializable
data class CollegeVerification(
    val id: String = "",
    val userId: String = "",
    val college: String = "",
    val studentEmail: String? = null,
    val status: String = "pending",
    val method: String = "profile_college",
    val verifiedAt: String? = null,
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Serializable
data class CollegeVerificationResponse(
    val verification: CollegeVerification = CollegeVerification()
)

@Serializable
data class CollegeVerificationsResponse(
    val verifications: List<CollegeVerification> = emptyList()
)

@Serializable
data class CreateCollegeCommunityRequest(
    val college: String,
    val description: String? = null,
    val emailDomains: List<String> = emptyList()
)

@Serializable
data class VerifyCollegeStudentRequest(
    val college: String,
    val studentEmail: String? = null
)
