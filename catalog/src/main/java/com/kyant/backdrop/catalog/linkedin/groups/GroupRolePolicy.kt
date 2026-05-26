package com.kyant.backdrop.catalog.linkedin.groups

private val groupRoleRank = mapOf(
    "member" to 1,
    "moderator" to 2,
    "admin" to 3,
    "owner" to 4
)

fun normalizeGroupRole(role: String?): String? {
    val normalized = role?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return null
    return normalized.takeIf { groupRoleRank.containsKey(it) }
}

fun isGroupOwner(role: String?): Boolean = normalizeGroupRole(role) == "owner"

fun canManageGroup(role: String?): Boolean = normalizeGroupRole(role) in setOf("owner", "admin")

fun canModerateGroup(role: String?): Boolean {
    val rank = normalizeGroupRole(role)?.let { groupRoleRank[it] } ?: return false
    return rank >= (groupRoleRank["moderator"] ?: Int.MAX_VALUE)
}

fun groupRoleDisplayName(role: String?): String {
    return when (normalizeGroupRole(role)) {
        "owner" -> "Owner"
        "admin" -> "Admin"
        "moderator" -> "Moderator"
        else -> "Member"
    }
}
