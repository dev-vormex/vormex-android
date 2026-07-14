package com.kyant.backdrop.catalog.linkedin.groups

import android.net.Uri
import java.util.Locale

private const val GROUP_INVITE_BASE_URL = "https://vormex.in/groups/invite"

fun normalizeGroupInviteVisibility(visibility: String?): String {
    return if (visibility?.trim()?.uppercase(Locale.US) == "MEMBERS") "MEMBERS" else "ADMINS"
}

fun groupInviteUrl(inviteCode: String): String {
    return "$GROUP_INVITE_BASE_URL/${Uri.encode(inviteCode)}"
}

fun canShareGroupInviteLink(memberRole: String?, visibility: String?): Boolean {
    return canManageGroup(memberRole) ||
        (normalizeGroupInviteVisibility(visibility) == "MEMBERS" && normalizeGroupRole(memberRole) == "member")
}
