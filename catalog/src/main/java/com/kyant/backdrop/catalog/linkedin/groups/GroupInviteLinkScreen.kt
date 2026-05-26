package com.kyant.backdrop.catalog.linkedin.groups

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.network.models.Group
import com.kyant.backdrop.catalog.ui.BasicText

@Composable
fun GroupInviteLinkScreen(
    inviteCode: String,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onNavigateBack: () -> Unit,
    onOpenGroup: (String) -> Unit
) {
    val context = LocalContext.current
    val viewModel: GroupsViewModel = viewModel(factory = GroupsViewModel.Factory(context))
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(inviteCode) {
        viewModel.loadGroupInvitePreview(inviteCode)
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.08f))
                    .clickable(onClick = onNavigateBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = contentColor, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.size(12.dp))
            BasicText(
                "Group Invite",
                style = TextStyle(color = contentColor, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            )
        }

        when {
            uiState.isLoadingInvitePreview -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accentColor)
                }
            }

            uiState.inviteLinkPreview == null -> {
                InviteStatusCard(
                    contentColor = contentColor,
                    title = "Invite link unavailable",
                    message = uiState.inviteLinkError ?: "This invite link may have been reset."
                )
            }

            else -> {
                val preview = uiState.inviteLinkPreview!!
                InviteGroupPreviewCard(
                    group = preview.group,
                    contentColor = contentColor,
                    accentColor = accentColor
                )
                InviteStatusCard(
                    contentColor = contentColor,
                    title = if (preview.requiresApproval) "Approval required" else "Ready to join",
                    message = if (preview.requiresApproval) {
                        "A group admin will review your request."
                    } else {
                        "You can join this group with this invite link."
                    }
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(accentColor, RoundedCornerShape(10.dp))
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = !uiState.isLoadingInvitePreview) {
                            viewModel.joinGroupInviteLink(inviteCode) { response ->
                                if (response.status == "pending") {
                                    Toast.makeText(context, response.message, Toast.LENGTH_SHORT).show()
                                } else {
                                    onOpenGroup(response.groupId)
                                }
                            }
                        }
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        BasicText(
                            if (preview.requiresApproval) "Request to Join" else "Join Group",
                            style = TextStyle(Color.White, 14.sp, FontWeight.SemiBold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InviteGroupPreviewCard(
    group: Group,
    contentColor: Color,
    accentColor: Color
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(contentColor.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .size(52.dp)
                    .background(accentColor.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Group, contentDescription = null, tint = accentColor, modifier = Modifier.size(28.dp))
            }
            Column(Modifier.weight(1f)) {
                BasicText(
                    group.name,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(contentColor, 18.sp, FontWeight.SemiBold)
                )
                BasicText(
                    "${group.memberCount} members",
                    style = TextStyle(contentColor.copy(alpha = 0.62f), 13.sp)
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                when (group.privacy) {
                    "PUBLIC" -> Icons.Default.Public
                    "PRIVATE" -> Icons.Default.Lock
                    else -> Icons.Default.VisibilityOff
                },
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(15.dp)
            )
            BasicText(group.privacy.lowercase().replaceFirstChar { it.titlecase() }, style = TextStyle(accentColor, 12.sp, FontWeight.Medium))
        }
        group.description?.takeIf { it.isNotBlank() }?.let {
            BasicText(it, maxLines = 3, overflow = TextOverflow.Ellipsis, style = TextStyle(contentColor.copy(alpha = 0.72f), 13.sp))
        }
    }
}

@Composable
private fun InviteStatusCard(
    contentColor: Color,
    title: String,
    message: String
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(contentColor.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        BasicText(title, style = TextStyle(contentColor, 15.sp, FontWeight.SemiBold))
        BasicText(message, style = TextStyle(contentColor.copy(alpha = 0.68f), 13.sp))
    }
}
