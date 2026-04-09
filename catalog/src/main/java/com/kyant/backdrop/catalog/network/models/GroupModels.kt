package com.kyant.backdrop.catalog.network.models

import kotlinx.serialization.Serializable

// ==================== Group Models ====================

enum class GroupPrivacy(val value: String) {
    PUBLIC("PUBLIC"),
    PRIVATE("PRIVATE"),
    SECRET("SECRET")
}

enum class GroupMemberRole(val value: String) {
    OWNER("owner"),
    ADMIN("admin"),
    MODERATOR("moderator"),
    MEMBER("member")
}

@Serializable
data class Group(
    val id: String,
    val name: String,
    val slug: String? = null,
    val description: String? = null,
    val coverImage: String? = null,
    val iconImage: String? = null,
    val privacy: String = "PUBLIC", // PUBLIC, PRIVATE, SECRET
    val category: String? = null,
    val tags: List<String> = emptyList(),
    val rules: List<String> = emptyList(),
    val memberCount: Int = 0,
    val postCount: Int = 0,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val createdById: String? = null,
    val isMember: Boolean = false,
    val memberRole: String? = null, // owner, admin, moderator, member
    val allowMemberPosts: Boolean = true,
    val requirePostApproval: Boolean = false
)

@Serializable
data class GroupUser(
    val id: String,
    val username: String? = null,
    val name: String? = null,
    val profileImage: String? = null,
    val headline: String? = null
)

@Serializable
data class GroupMember(
    val id: String,
    val groupId: String,
    val userId: String,
    val user: GroupUser,
    val role: String = "member", // owner, admin, moderator, member
    val joinedAt: String? = null,
    val mutedUntil: String? = null
)

@Serializable
data class GroupMessage(
    val id: String,
    val groupId: String,
    val senderId: String,
    val sender: GroupUser,
    val content: String,
    val contentType: String = "text", // text, image, video, file
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val fileName: String? = null,
    val fileSize: Int? = null,
    val replyToId: String? = null,
    val replyTo: GroupMessageReply? = null,
    val reactions: List<GroupMessageReaction> = emptyList(),
    val isDeleted: Boolean = false,
    val createdAt: String,
    val updatedAt: String? = null
)

@Serializable
data class GroupMessageReply(
    val id: String,
    val content: String,
    val senderId: String,
    val sender: GroupUser? = null
)

@Serializable
data class GroupMessageReaction(
    val id: String,
    val messageId: String,
    val userId: String,
    val emoji: String,
    val createdAt: String? = null
)

@Serializable
data class GroupPost(
    val id: String,
    val groupId: String,
    val authorId: String,
    val author: GroupUser,
    val content: String,
    val mediaUrls: List<String> = emptyList(),
    val mediaType: String? = null,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isPinned: Boolean = false,
    val isApproved: Boolean = true,
    val isLiked: Boolean = false,
    val createdAt: String,
    val updatedAt: String? = null
)

@Serializable
data class GroupInvite(
    val id: String,
    val groupId: String,
    val invitedUserId: String,
    val invitedUser: GroupUser? = null,
    val invitedBy: GroupUser? = null,
    val status: String = "pending", // pending, accepted, declined, expired
    val message: String? = null,
    val createdAt: String? = null,
    val respondedAt: String? = null,
    val group: Group? = null
)

@Serializable
data class GroupCategory(
    val name: String,
    val count: Int = 0
)

// ==================== API Request/Response Models ====================

@Serializable
data class CreateGroupRequest(
    val name: String,
    val description: String? = null,
    val privacy: String = "PUBLIC",
    val category: String? = null,
    val tags: List<String> = emptyList(),
    val coverImage: String? = null,
    val iconImage: String? = null,
    val rules: List<String> = emptyList()
)

@Serializable
data class UpdateGroupRequest(
    val name: String? = null,
    val description: String? = null,
    val privacy: String? = null,
    val category: String? = null,
    val tags: List<String>? = null,
    val coverImage: String? = null,
    val iconImage: String? = null,
    val rules: List<String>? = null
)

@Serializable
data class GroupsResponse(
    val groups: List<Group>,
    val pagination: PaginationMeta? = null
)

@Serializable
data class PaginationMeta(
    val page: Int = 1,
    val limit: Int = 20,
    val total: Int = 0,
    val totalPages: Int = 1
)

@Serializable
data class GroupMembersResponse(
    val members: List<GroupMember>,
    val pagination: PaginationMeta? = null
)

@Serializable
data class JoinGroupResponse(
    val status: String, // "joined" or "pending"
    val message: String
)

@Serializable
data class SendGroupMessageRequest(
    val content: String,
    val contentType: String = "text",
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val fileName: String? = null,
    val fileSize: Int? = null,
    val replyToId: String? = null
)

@Serializable
data class GroupMessagesResponse(
    val messages: List<GroupMessage> = emptyList()
)

@Serializable
data class GroupPostsResponse(
    val posts: List<GroupPost>,
    val pagination: PaginationMeta? = null
)

@Serializable
data class CreateGroupPostRequest(
    val content: String,
    val mediaUrls: List<String> = emptyList(),
    val mediaType: String? = null
)

@Serializable
data class GroupCategoriesResponse(
    val categories: List<GroupCategory>
)

@Serializable
data class GroupInvitesResponse(
    val invites: List<GroupInvite>
)

@Serializable
data class UpdateMemberRoleRequest(
    val role: String
)

// ==================== Circle Models ====================

@Serializable
data class Circle(
    val id: String,
    val name: String,
    val slug: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val coverImageUrl: String? = null,
    val emoji: String? = null,
    val category: String? = null,
    val campus: String? = null,
    val tags: List<String> = emptyList(),
    val type: String? = null,
    val isPrivate: Boolean = false,
    val memberCount: Int = 0,
    val activeMembers: Int = 0,
    val postsCount: Int = 0,
    val weeklyActivity: Int = 0,
    val isMember: Boolean = false,
    val myRole: String? = null,
    val createdAt: String? = null
)

@Serializable
data class CircleMember(
    val id: String,
    val name: String? = null,
    val username: String? = null,
    val profileImage: String? = null,
    val headline: String? = null,
    val college: String? = null,
    val role: String? = null,
    val xpInCircle: Int = 0,
    val joinedAt: String? = null
)

@Serializable
data class CirclePost(
    val id: String,
    val circleId: String,
    val authorId: String,
    val content: String,
    val type: String? = null,
    val mediaUrls: List<String> = emptyList(),
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isPinned: Boolean = false,
    val author: CircleMember? = null,
    val createdAt: String? = null
)

@Serializable
data class CirclesResponse(
    val circles: List<Circle>,
    val pagination: PaginationMeta? = null
)

@Serializable
data class CircleMembersResponse(
    val members: List<CircleMember>,
    val pagination: PaginationMeta? = null
)

@Serializable
data class CirclePostsResponse(
    val posts: List<CirclePost>,
    val pagination: PaginationMeta? = null
)

@Serializable
data class CreateCircleRequest(
    val name: String,
    val description: String? = null,
    val category: String? = null,
    val campus: String? = null,
    val tags: List<String> = emptyList(),
    val emoji: String? = null,
    val isPrivate: Boolean = false
)

@Serializable
data class JoinCircleResponse(
    val message: String,
    val requiresUpgrade: Boolean = false
)

@Serializable
data class CreateCirclePostRequest(
    val content: String,
    val type: String? = null,
    val mediaUrls: List<String> = emptyList()
)

// Circle Categories
data class CircleCategory(
    val id: String,
    val name: String,
    val emoji: String
) {
    companion object {
        val ALL_CATEGORIES = listOf(
            CircleCategory("coding", "Coding", "💻"),
            CircleCategory("design", "Design", "🎨"),
            CircleCategory("cp", "Competitive Programming", "🏆"),
            CircleCategory("web_dev", "Web Development", "🌐"),
            CircleCategory("business", "Business", "🚀"),
            CircleCategory("security", "Cybersecurity", "🔒"),
            CircleCategory("mobile", "Mobile Dev", "📱"),
            CircleCategory("career", "Career", "💼"),
            CircleCategory("content", "Content Creation", "🎬"),
            CircleCategory("ai_ml", "AI/ML", "🤖"),
            CircleCategory("data_science", "Data Science", "📊"),
            CircleCategory("research", "Research", "🔬"),
            CircleCategory("devops", "DevOps", "☁️"),
            CircleCategory("gaming", "Gaming", "🎮"),
            CircleCategory("music", "Music", "🎵")
        )
    }
}
