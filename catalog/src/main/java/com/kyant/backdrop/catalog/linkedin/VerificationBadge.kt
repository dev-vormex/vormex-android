package com.kyant.backdrop.catalog.linkedin

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    modifier: Modifier = Modifier,
    size: VerificationBadgeSize = VerificationBadgeSize.Medium
) {
    if (!verified) return

    val context = LocalContext.current
    val style by SettingsPreferences.profileBadgeStyle(context)
        .collectAsState(initial = SettingsPreferences.PROFILE_BADGE_STYLE_STUDENT)
    val badgeColor = when (style) {
        SettingsPreferences.PROFILE_BADGE_STYLE_PROFESSIONAL -> Color(0xFF2563EB)
        SettingsPreferences.PROFILE_BADGE_STYLE_PREMIUM -> Color(0xFFD4A017)
        else -> Color(0xFF16A34A)
    }
    val label = when (style) {
        SettingsPreferences.PROFILE_BADGE_STYLE_PROFESSIONAL -> "Verified working professional"
        SettingsPreferences.PROFILE_BADGE_STYLE_PREMIUM -> "Verified premium user"
        else -> "Verified student"
    }

    Icon(
        imageVector = Icons.Filled.Verified,
        contentDescription = label,
        modifier = modifier.size(size.iconSize),
        tint = badgeColor
    )
}

fun ApiUser.hasVerificationBadge(): Boolean = isVerified
fun ProfileUser.hasVerificationBadge(): Boolean = verified || isVerified
fun Author.hasVerificationBadge(): Boolean = verified || isVerified
fun PersonInfo.hasVerificationBadge(): Boolean = verified || isVerified
fun SmartMatchUser.hasVerificationBadge(): Boolean = verified || isVerified
fun NearbyUser.hasVerificationBadge(): Boolean = verified || isVerified
fun PendingConnectionRequestUser.hasVerificationBadge(): Boolean = verified || isVerified
fun ProfileRelationshipUser.hasVerificationBadge(): Boolean = verified || isVerified
fun MutualConnection.hasVerificationBadge(): Boolean = verified || isVerified
fun ChatUser.hasVerificationBadge(): Boolean = verified || isVerified
fun NotificationActor.hasVerificationBadge(): Boolean = verified || isVerified
fun SharedPostAuthor.hasVerificationBadge(): Boolean = verified || isVerified
fun ReelAuthor.hasVerificationBadge(): Boolean = verified || isVerified
fun LikeUser.hasVerificationBadge(): Boolean = verified || isVerified
fun MentionUser.hasVerificationBadge(): Boolean = verified || isVerified
fun ProfileViewerPerson.hasVerificationBadge(): Boolean = verified || isVerified
fun GroupUser.hasVerificationBadge(): Boolean = verified || isVerified
fun CircleMember.hasVerificationBadge(): Boolean = verified || isVerified
