package com.kyant.backdrop.catalog.linkedin

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.catalog.data.SettingsPreferences
import com.kyant.backdrop.catalog.network.models.Author
import com.kyant.backdrop.catalog.network.models.ChatUser
import com.kyant.backdrop.catalog.network.models.LikeUser
import com.kyant.backdrop.catalog.network.models.MentionUser
import com.kyant.backdrop.catalog.network.models.MutualConnection
import com.kyant.backdrop.catalog.network.models.NearbyUser
import com.kyant.backdrop.catalog.network.models.NotificationActor
import com.kyant.backdrop.catalog.network.models.PendingConnectionRequestUser
import com.kyant.backdrop.catalog.network.models.PersonInfo
import com.kyant.backdrop.catalog.network.models.ProfileRelationshipUser
import com.kyant.backdrop.catalog.network.models.ProfileUser
import com.kyant.backdrop.catalog.network.models.ProfileViewerPerson
import com.kyant.backdrop.catalog.network.models.ReelAuthor
import com.kyant.backdrop.catalog.network.models.SharedPostAuthor
import com.kyant.backdrop.catalog.network.models.SmartMatchUser
import com.kyant.backdrop.catalog.network.models.User as ApiUser
import com.kyant.backdrop.catalog.network.models.GroupUser
import com.kyant.backdrop.catalog.network.models.CircleMember

enum class VerificationBadgeSize(val iconSize: Dp) {
    Micro(12.dp),
    Small(15.dp),
    Medium(18.dp),
    Large(23.dp)
}

@Composable
fun VerificationBadge(
    verified: Boolean,
    badgeStyle: String? = null,
    isPremium: Boolean = false,
    modifier: Modifier = Modifier,
    size: VerificationBadgeSize = VerificationBadgeSize.Medium
) {
    val style = resolveVerificationBadgeStyle(badgeStyle, isPremium)
    if (style == null) return

    val badgeColor = when (style) {
        SettingsPreferences.PROFILE_BADGE_STYLE_PROFESSIONAL -> Color(0xFF2563EB)
        SettingsPreferences.PROFILE_BADGE_STYLE_PREMIUM -> Color(0xFFD4A017)
        else -> Color(0xFF16A34A)
    }
    val label = when (style) {
        SettingsPreferences.PROFILE_BADGE_STYLE_PROFESSIONAL -> "Professional badge"
        SettingsPreferences.PROFILE_BADGE_STYLE_PREMIUM -> "Premium badge"
        else -> "Student badge"
    }

    Icon(
        imageVector = Icons.Filled.Verified,
        contentDescription = label,
        modifier = modifier.size(size.iconSize),
        tint = badgeColor
    )
}

fun resolveVerificationBadgeStyle(badgeStyle: String?, isPremium: Boolean = false): String? {
    return when (badgeStyle?.lowercase()) {
        SettingsPreferences.PROFILE_BADGE_STYLE_STUDENT -> SettingsPreferences.PROFILE_BADGE_STYLE_STUDENT
        SettingsPreferences.PROFILE_BADGE_STYLE_PROFESSIONAL -> SettingsPreferences.PROFILE_BADGE_STYLE_PROFESSIONAL
        SettingsPreferences.PROFILE_BADGE_STYLE_PREMIUM -> if (isPremium) {
            SettingsPreferences.PROFILE_BADGE_STYLE_PREMIUM
        } else {
            null
        }
        else -> if (isPremium) SettingsPreferences.PROFILE_BADGE_STYLE_PREMIUM else null
    }
}

fun resolveProfileBadgePreferenceStyle(badgeStyle: String?): String? {
    return when (badgeStyle?.lowercase()) {
        SettingsPreferences.PROFILE_BADGE_STYLE_STUDENT -> SettingsPreferences.PROFILE_BADGE_STYLE_STUDENT
        SettingsPreferences.PROFILE_BADGE_STYLE_PROFESSIONAL -> SettingsPreferences.PROFILE_BADGE_STYLE_PROFESSIONAL
        else -> null
    }
}

fun ApiUser.verificationBadgeStyle(): String? = resolveVerificationBadgeStyle(profileBadgeStyle, isPremium)
fun ProfileUser.verificationBadgeStyle(): String? = resolveVerificationBadgeStyle(profileBadgeStyle, isPremium)
fun Author.verificationBadgeStyle(): String? = resolveVerificationBadgeStyle(profileBadgeStyle, isPremium)
fun PersonInfo.verificationBadgeStyle(): String? = resolveVerificationBadgeStyle(profileBadgeStyle, isPremium)
fun SmartMatchUser.verificationBadgeStyle(): String? = resolveVerificationBadgeStyle(profileBadgeStyle, isPremium)
fun NearbyUser.verificationBadgeStyle(): String? = resolveVerificationBadgeStyle(profileBadgeStyle, isPremium)
fun PendingConnectionRequestUser.verificationBadgeStyle(): String? = resolveVerificationBadgeStyle(profileBadgeStyle, isPremium)
fun ProfileRelationshipUser.verificationBadgeStyle(): String? = resolveVerificationBadgeStyle(profileBadgeStyle, isPremium)
fun MutualConnection.verificationBadgeStyle(): String? = resolveVerificationBadgeStyle(profileBadgeStyle, isPremium)
fun ChatUser.verificationBadgeStyle(): String? = resolveVerificationBadgeStyle(profileBadgeStyle, isPremium)
fun NotificationActor.verificationBadgeStyle(): String? = resolveVerificationBadgeStyle(profileBadgeStyle, isPremium)
fun SharedPostAuthor.verificationBadgeStyle(): String? = resolveVerificationBadgeStyle(profileBadgeStyle, isPremium)
fun ReelAuthor.verificationBadgeStyle(): String? = resolveVerificationBadgeStyle(profileBadgeStyle, isPremium)
fun LikeUser.verificationBadgeStyle(): String? = resolveVerificationBadgeStyle(profileBadgeStyle, isPremium)
fun MentionUser.verificationBadgeStyle(): String? = resolveVerificationBadgeStyle(profileBadgeStyle, isPremium)
fun ProfileViewerPerson.verificationBadgeStyle(): String? = resolveVerificationBadgeStyle(profileBadgeStyle, isPremium)
fun GroupUser.verificationBadgeStyle(): String? = resolveVerificationBadgeStyle(profileBadgeStyle, isPremium)
fun CircleMember.verificationBadgeStyle(): String? = resolveVerificationBadgeStyle(profileBadgeStyle, isPremium)
fun ProfilePeopleListItem.verificationBadgeStyle(): String? = resolveVerificationBadgeStyle(profileBadgeStyle, isPremium)

fun ApiUser.hasVerificationBadge(): Boolean = verificationBadgeStyle() != null
fun ProfileUser.hasVerificationBadge(): Boolean = verificationBadgeStyle() != null
fun Author.hasVerificationBadge(): Boolean = verificationBadgeStyle() != null
fun PersonInfo.hasVerificationBadge(): Boolean = verificationBadgeStyle() != null
fun SmartMatchUser.hasVerificationBadge(): Boolean = verificationBadgeStyle() != null
fun NearbyUser.hasVerificationBadge(): Boolean = verificationBadgeStyle() != null
fun PendingConnectionRequestUser.hasVerificationBadge(): Boolean = verificationBadgeStyle() != null
fun ProfileRelationshipUser.hasVerificationBadge(): Boolean = verificationBadgeStyle() != null
fun MutualConnection.hasVerificationBadge(): Boolean = verificationBadgeStyle() != null
fun ChatUser.hasVerificationBadge(): Boolean = verificationBadgeStyle() != null
fun NotificationActor.hasVerificationBadge(): Boolean = verificationBadgeStyle() != null
fun SharedPostAuthor.hasVerificationBadge(): Boolean = verificationBadgeStyle() != null
fun ReelAuthor.hasVerificationBadge(): Boolean = verificationBadgeStyle() != null
fun LikeUser.hasVerificationBadge(): Boolean = verificationBadgeStyle() != null
fun MentionUser.hasVerificationBadge(): Boolean = verificationBadgeStyle() != null
fun ProfileViewerPerson.hasVerificationBadge(): Boolean = verificationBadgeStyle() != null
fun GroupUser.hasVerificationBadge(): Boolean = verificationBadgeStyle() != null
fun CircleMember.hasVerificationBadge(): Boolean = verificationBadgeStyle() != null
fun ProfilePeopleListItem.hasVerificationBadge(): Boolean = verificationBadgeStyle() != null
