package com.kyant.backdrop.catalog.linkedin

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.kyant.backdrop.catalog.ui.BasicText
import com.kyant.backdrop.catalog.ui.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.ai.VormexAiChipAction
import com.kyant.backdrop.catalog.ai.VormexAiChipRow
import com.kyant.backdrop.catalog.ai.VormexAiGateway
import com.kyant.backdrop.catalog.ai.VormexAiRewriteStyle
import com.kyant.backdrop.catalog.ai.VormexAiStatusCard
import com.kyant.backdrop.catalog.ai.VormexAiSurface
import com.kyant.backdrop.catalog.ai.VormexAiTextResult
import com.kyant.backdrop.catalog.network.models.*
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// ==================== Section Card Wrapper ====================

@Composable
private fun SectionCard(
    title: String,
    count: Int? = null,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean = false,
    onAdd: (() -> Unit)? = null,
    actionIconRes: Int? = null,
    content: @Composable () -> Unit
) {
    val appearance = currentVormexAppearance()
    Column(
        Modifier
            .fillMaxWidth()
            .background(appearance.cardColor)
            .padding(horizontal = 18.dp, vertical = 20.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    title,
                    style = TextStyle(contentColor, 20.sp, FontWeight.Bold)
                )
                count?.let {
                    Spacer(Modifier.width(6.dp))
                    BasicText(
                        "($it)",
                        style = TextStyle(contentColor.copy(alpha = 0.55f), 14.sp)
                    )
                }
            }

            if (isOwner && onAdd != null) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onAdd),
                    contentAlignment = Alignment.Center
                ) {
                    if (actionIconRes != null) {
                        Image(
                            painter = painterResource(actionIconRes),
                            contentDescription = "Edit $title",
                            modifier = Modifier.size(20.dp),
                            colorFilter = ColorFilter.tint(contentColor)
                        )
                    } else {
                        BasicText("+", style = TextStyle(contentColor, 25.sp, FontWeight.Normal))
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        content()
    }
}

// ==================== About Section ====================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AboutSection(
    user: ProfileUser,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    isEditingBio: Boolean,
    editedBio: String,
    isSavingBio: Boolean = false,
    bioEditError: String? = null,
    onEditBio: () -> Unit,
    onSaveBio: () -> Unit,
    onCancelEditBio: () -> Unit,
    onBioChange: (String) -> Unit,
    onToggleOpenToWork: (Boolean) -> Unit
) {
    var showFullBio by remember { mutableStateOf(false) }
    var showAllInterests by remember(user.interests) { mutableStateOf(false) }
    val context = LocalContext.current
    val aiGateway = remember { VormexAiGateway(context.applicationContext) }
    val aiScope = rememberCoroutineScope()
    var aiStatus by remember { mutableStateOf<String?>(null) }
    var aiBusyLabel by remember { mutableStateOf<String?>(null) }
    val arrangedInterests = remember(user.interests) {
        user.interests
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase(Locale.getDefault()) }
    }

    fun runBioAi(label: String, block: suspend () -> VormexAiTextResult) {
        aiScope.launch {
            aiBusyLabel = "$label…"
            aiStatus = null
            when (val result = block()) {
                is VormexAiTextResult.Success -> {
                    onBioChange(result.text)
                    aiStatus = when (result.source) {
                        com.kyant.backdrop.catalog.ai.VormexAiSource.LOCAL -> "$label updated on-device."
                        com.kyant.backdrop.catalog.ai.VormexAiSource.CLOUD -> "$label updated with cloud AI."
                    }
                }
                is VormexAiTextResult.NeedsDownload -> aiStatus = result.message
                is VormexAiTextResult.Blocked -> aiStatus = result.message
                is VormexAiTextResult.Failure -> aiStatus = result.message
            }
            aiBusyLabel = null
        }
    }

    SectionCard(
        title = "About",
        backdrop = backdrop,
        contentColor = contentColor,
        accentColor = accentColor,
        isOwner = isOwner,
        onAdd = if (isOwner) onEditBio else null,
        actionIconRes = R.drawable.ic_vx_edit
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Bio
            if (false && isEditingBio) {
                Column {
                    BasicTextField(
                        value = editedBio,
                        onValueChange = onBioChange,
                        textStyle = TextStyle(contentColor, 14.sp),
                        cursorBrush = SolidColor(accentColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(contentColor.copy(alpha = 0.05f))
                            .padding(12.dp)
                            .height(100.dp)
                    )

                    Spacer(Modifier.height(10.dp))

                    VormexAiChipRow(
                        actions = listOf(
                            VormexAiChipAction(
                                label = "Professional",
                                enabled = editedBio.isNotBlank(),
                                onClick = {
                                    runBioAi("Professional rewrite") {
                                        aiGateway.rewrite(
                                            text = editedBio,
                                            style = VormexAiRewriteStyle.PROFESSIONAL,
                                            surface = VormexAiSurface.PROFILE,
                                            allowCloudFallback = true
                                        )
                                    }
                                }
                            ),
                            VormexAiChipAction(
                                label = "Shorter",
                                enabled = editedBio.isNotBlank(),
                                onClick = {
                                    runBioAi("Shorter rewrite") {
                                        aiGateway.rewrite(
                                            text = editedBio,
                                            style = VormexAiRewriteStyle.SHORTER,
                                            surface = VormexAiSurface.PROFILE,
                                            allowCloudFallback = true
                                        )
                                    }
                                }
                            ),
                            VormexAiChipAction(
                                label = "Clearer",
                                enabled = editedBio.isNotBlank(),
                                onClick = {
                                    runBioAi("Clearer rewrite") {
                                        aiGateway.rewrite(
                                            text = editedBio,
                                            style = VormexAiRewriteStyle.CLEARER,
                                            surface = VormexAiSurface.PROFILE,
                                            allowCloudFallback = true
                                        )
                                    }
                                }
                            ),
                            VormexAiChipAction(
                                label = "Proofread",
                                enabled = editedBio.isNotBlank(),
                                onClick = {
                                    runBioAi("Proofread") {
                                        aiGateway.proofread(
                                            text = editedBio,
                                            surface = VormexAiSurface.PROFILE,
                                            allowCloudFallback = true
                                        )
                                    }
                                }
                            )
                        ),
                        contentColor = contentColor,
                        accentColor = accentColor
                    )

                    aiBusyLabel?.let { busy ->
                        Spacer(Modifier.height(10.dp))
                        VormexAiStatusCard(
                            message = busy,
                            contentColor = contentColor,
                            accentColor = accentColor
                        )
                    }

                    aiStatus?.let { status ->
                        Spacer(Modifier.height(10.dp))
                        VormexAiStatusCard(
                            message = status,
                            contentColor = contentColor,
                            accentColor = accentColor
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    bioEditError?.let { error ->
                        BasicText(
                            error,
                            style = TextStyle(Color(0xFFEF4444), 12.sp, FontWeight.Medium)
                        )
                        Spacer(Modifier.height(4.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSavingBio) {
                                        accentColor.copy(alpha = 0.62f)
                                    } else {
                                        accentColor
                                    }
                                )
                                .clickable(enabled = !isSavingBio, onClick = onSaveBio)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSavingBio) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                }
                                BasicText(
                                    if (isSavingBio) "Saving" else "Save",
                                    style = TextStyle(Color.White, 14.sp, FontWeight.SemiBold)
                                )
                            }
                        }
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(contentColor.copy(alpha = 0.1f))
                                .clickable(enabled = !isSavingBio, onClick = onCancelEditBio)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            BasicText("Cancel", style = TextStyle(contentColor, 14.sp))
                        }
                    }
                }
            } else if (!user.bio.isNullOrEmpty()) {
                Column {
                    val displayBio = if (showFullBio || user.bio.length <= 200) {
                        user.bio
                    } else {
                        user.bio.take(200) + "..."
                    }

                    BasicText(
                        displayBio,
                        style = TextStyle(contentColor.copy(alpha = 0.9f), 14.sp)
                    )

                    if (user.bio.length > 200) {
                        BasicText(
                            if (showFullBio) "Show less" else "Show more",
                            style = TextStyle(accentColor, 13.sp, FontWeight.Medium),
                            modifier = Modifier.clickable { showFullBio = !showFullBio }
                        )
                    }

                    if (false && isOwner) {
                        BasicText(
                            "Edit",
                            style = TextStyle(accentColor, 13.sp, FontWeight.Medium),
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clickable(onClick = onEditBio)
                        )
                    }
                }
            } else {
                BasicText(
                    if (isOwner) "Add an about summary so people know what you do." else "No about information yet.",
                    style = TextStyle(contentColor.copy(alpha = 0.58f), 14.sp)
                )
            }

            // Interests
            if (false && arrangedInterests.isNotEmpty()) {
                InterestsPanel(
                    interests = arrangedInterests,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    expanded = showAllInterests,
                    onToggleExpanded = { showAllInterests = !showAllInterests }
                )
            }

            val eduDetails = listOfNotNull(
                user.degree,
                user.branch?.takeIf { it.isNotEmpty() },
                user.currentYear?.let { "Year $it" },
                user.graduationYear?.let { "Class of $it" }
            ).joinToString(" • ")
            if (false && eduDetails.isNotEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(contentColor.copy(alpha = 0.05f))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.ic_education),
                            contentDescription = "Education",
                            modifier = Modifier.size(24.dp),
                            colorFilter = ColorFilter.tint(contentColor)
                        )
                        Spacer(Modifier.width(12.dp))
                        BasicText(
                            eduDetails,
                            style = TextStyle(contentColor, 14.sp, FontWeight.Medium)
                        )
                    }
                }
            }

            // Open to opportunities toggle (owner only)
            if (false && isOwner) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (user.isOpenToOpportunities) Color(0xFF22C55E).copy(alpha = 0.15f)
                            else contentColor.copy(alpha = 0.05f)
                        )
                        .clickable { onToggleOpenToWork(!user.isOpenToOpportunities) }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        BasicText(
                            "#OpenToWork",
                            style = TextStyle(
                                if (user.isOpenToOpportunities) Color(0xFF22C55E) else contentColor,
                                14.sp,
                                FontWeight.Medium
                            )
                        )
                    }
                    Box(
                        Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (user.isOpenToOpportunities) Color(0xFF22C55E)
                                else contentColor.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (user.isOpenToOpportunities) {
                            BasicText("✓", style = TextStyle(Color.White, 14.sp, FontWeight.Bold))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InterestsSection(
    interests: List<String>,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    onEditInterests: () -> Unit
) {
    val normalizedInterests = remember(interests) {
        interests
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale.getDefault()) }
    }
    SectionCard(
        title = "Interests",
        backdrop = backdrop,
        contentColor = contentColor,
        accentColor = accentColor,
        isOwner = isOwner,
        onAdd = if (isOwner) onEditInterests else null,
        actionIconRes = R.drawable.ic_vx_edit
    ) {
        if (normalizedInterests.isEmpty()) {
            BasicText(
                if (isOwner) "Add interests to help people discover what you care about." else "No interests added yet.",
                style = TextStyle(contentColor.copy(alpha = 0.58f), 14.sp)
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                normalizedInterests.chunked(3).forEach { columnInterests ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        columnInterests.forEach { interest ->
                            DefaultInterestChip(
                                text = interest,
                                contentColor = contentColor,
                                accentColor = accentColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InterestsPanel(
    interests: List<String>,
    contentColor: Color,
    accentColor: Color,
    expanded: Boolean,
    onToggleExpanded: () -> Unit
) {
    val collapsedCount = 9
    val visibleInterests = if (expanded || interests.size <= collapsedCount) {
        interests
    } else {
        interests.take(collapsedCount)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    "Interests",
                    style = TextStyle(contentColor.copy(alpha = 0.86f), 14.sp, FontWeight.SemiBold)
                )
            }

            if (interests.size > collapsedCount) {
                BasicText(
                    if (expanded) "Show less" else "Show all",
                    style = TextStyle(accentColor, 11.sp, FontWeight.SemiBold),
                    modifier = Modifier
                        .clickable(onClick = onToggleExpanded)
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                )
            }
        }

        if (visibleInterests.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                visibleInterests.forEach { interest ->
                    DefaultInterestChip(
                        text = interest,
                        contentColor = contentColor,
                        accentColor = accentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun DefaultInterestChip(
    text: String,
    contentColor: Color,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(contentColor.copy(alpha = 0.06f))
            .border(
                width = 1.dp,
                color = contentColor.copy(alpha = 0.075f),
                shape = RoundedCornerShape(9.dp)
            )
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(2.dp)
                .height(12.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(accentColor.copy(alpha = 0.72f))
        )
        Spacer(Modifier.width(8.dp))
        BasicText(
            text = text,
            style = TextStyle(
                color = contentColor.copy(alpha = 0.78f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 150.dp)
        )
    }
}

// ==================== GitHub Section ====================

private const val GitHubMetricAnimationDurationMs = 2600
private const val GitHubMetricRevealDurationMs = 1700
private const val GitHubContributionRevealDurationMs = 3200
private const val GitHubContributionRevealDelayMs = 520
private const val GitHubLanguageOrbitDurationMs = 3600
private const val GitHubLanguageOrbitDelayMs = 680
private const val GitHubLanguageBarDurationMs = 2200
private const val GitHubLanguageBarRevealDurationMs = 1600
private const val GitHubSignalGraphDurationMs = 4200
private const val GitHubSignalGraphDelayMs = 260
private const val GitHubInteractiveModelMotionDurationMs = 420

@Composable
fun GitHubSection(
    github: GitHubProfile,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    isConnecting: Boolean = false,
    isSyncing: Boolean = false,
    isDisconnecting: Boolean = false,
    actionError: String? = null,
    onConnect: () -> Unit = {},
    onSync: () -> Unit = {},
    onDisconnect: () -> Unit = {}
) {
    val context = LocalContext.current

    SectionCard(
        title = "GitHub",
        backdrop = backdrop,
        contentColor = contentColor,
        accentColor = accentColor
    ) {
        val contributionCalendar = github.contributionCalendar
        actionError?.takeIf { it.isNotBlank() }?.let { error ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(ProfileCardShape)
                    .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                BasicText(
                    error,
                    style = TextStyle(Color(0xFFEF4444), 12.sp, FontWeight.Medium)
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        if (github.connected) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // GitHub profile link
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!github.avatarUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = github.avatarUrl,
                                contentDescription = "GitHub Avatar",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Column {
                            BasicText(
                                "@${github.username ?: "github"}",
                                style = TextStyle(contentColor, 14.sp, FontWeight.Medium)
                            )
                            github.lastSyncedAt?.let {
                                BasicText(
                                    "Last synced: ${formatGitHubSyncDate(it)}",
                                    style = TextStyle(contentColor.copy(alpha = 0.5f), 10.sp)
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isOwner) {
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(accentColor.copy(alpha = 0.2f))
                                    .clickable(enabled = !isSyncing && !isDisconnecting, onClick = onSync)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isSyncing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(12.dp),
                                            strokeWidth = 1.4.dp,
                                            color = accentColor
                                        )
                                        Spacer(Modifier.width(6.dp))
                                    }
                                    BasicText(
                                        if (isSyncing) "Syncing" else "Sync",
                                        style = TextStyle(accentColor, 12.sp, FontWeight.Medium)
                                    )
                                }
                            }
                        }

                        github.profileUrl?.let { url ->
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(contentColor.copy(alpha = 0.1f))
                                    .clickable {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                BasicText("View", style = TextStyle(contentColor, 12.sp))
                            }
                        }
                    }
                }

                if (isOwner) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                                .clickable(enabled = !isDisconnecting && !isSyncing, onClick = onDisconnect)
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isDisconnecting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 1.4.dp,
                                        color = Color(0xFFEF4444)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                }
                                BasicText(
                                    if (isDisconnecting) "Disconnecting" else "Disconnect",
                                    style = TextStyle(Color(0xFFEF4444), 12.sp, FontWeight.Medium)
                                )
                            }
                        }
                    }
                }

                if (contributionCalendar != null) {
                    GitHubContributionGraph(
                        contributionCalendar = contributionCalendar,
                        contentColor = contentColor,
                        accentColor = accentColor
                    )
                } else {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(ProfileCardShape)
                            .background(contentColor.copy(alpha = 0.05f))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            BasicText(
                                "Contribution graph is syncing",
                                style = TextStyle(contentColor, 13.sp, FontWeight.Medium)
                            )
                            BasicText(
                                if (isOwner) {
                                    "Sync GitHub once and the contribution graph will appear on your profile."
                                } else {
                                    "This profile has GitHub connected, but the contribution graph is not available yet."
                                },
                                style = TextStyle(contentColor.copy(alpha = 0.58f), 11.sp)
                            )
                        }
                    }
                }

            }
        } else if (isOwner) {
            // Not connected - show connect button
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(ProfileCardShape)
                    .background(contentColor.copy(alpha = 0.05f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(R.drawable.ic_github),
                        contentDescription = "GitHub",
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    BasicText(
                        "Connect GitHub",
                        style = TextStyle(contentColor, 14.sp, FontWeight.Medium)
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF24292E))
                            .clickable(enabled = !isConnecting, onClick = onConnect)
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isConnecting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 1.6.dp,
                                    color = Color.White
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            BasicText(
                                if (isConnecting) "Opening GitHub" else "Connect GitHub",
                                style = TextStyle(Color.White, 14.sp, FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GitHubMetricCard(
    modifier: Modifier = Modifier,
    iconRes: Int,
    value: String,
    label: String,
    tint: Color,
    progress: Float,
    contentColor: Color,
    isVisible: Boolean,
    delayMillis: Int = 0
) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (isVisible) progress.coerceIn(0f, 1f) else 0f,
        animationSpec = tween(
            durationMillis = GitHubMetricAnimationDurationMs,
            delayMillis = delayMillis,
            easing = FastOutSlowInEasing
        ),
        label = "githubMetricRing$label"
    )
    val revealProgress by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = GitHubMetricRevealDurationMs,
            delayMillis = max(0, delayMillis / 2),
            easing = FastOutSlowInEasing
        ),
        label = "githubMetricReveal$label"
    )
    Box(
        modifier
            .graphicsLayer {
                alpha = 0.4f + (0.6f * revealProgress)
                translationY = (1f - revealProgress) * 28f
            }
            .clip(ProfileCardShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        tint.copy(alpha = 0.12f),
                        contentColor.copy(alpha = 0.04f)
                    )
                )
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = size.minDimension * 0.11f
                    drawArc(
                        color = contentColor.copy(alpha = 0.10f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = tint,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(contentColor.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(iconRes),
                        contentDescription = label,
                        modifier = Modifier.size(15.dp),
                        colorFilter = ColorFilter.tint(tint)
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column {
                BasicText(value, style = TextStyle(contentColor, 16.sp, FontWeight.Bold))
                BasicText(label, style = TextStyle(contentColor.copy(alpha = 0.6f), 10.sp))
            }
        }
    }
}

@Composable
private fun GitHubContributionGraph(
    contributionCalendar: GitHubContributionCalendar,
    contentColor: Color,
    accentColor: Color
) {
    val scrollState = rememberScrollState()
    var selectedDay by remember(contributionCalendar) { mutableStateOf<GitHubContributionDay?>(null) }
    val allDays = remember(contributionCalendar) {
        contributionCalendar.weeks.flatMap { week ->
            week.contributionDays.sortedBy { it.weekday }
        }
    }
    val activeDays = remember(allDays) { allDays.count { it.contributionCount > 0 } }
    val peakDay = remember(allDays) { allDays.maxByOrNull { it.contributionCount } }
    val highlightedDay = selectedDay ?: peakDay
    val monthSegments = contributionCalendar.months.ifEmpty {
        contributionCalendar.weeks.firstOrNull()?.let { listOf(GitHubContributionMonth(totalWeeks = contributionCalendar.weeks.size, name = "This year")) }
            ?: emptyList()
    }
    val palette = contributionCalendar.colors.ifEmpty {
        listOf(
            accentColor.copy(alpha = 0.25f).toHexColor(),
            accentColor.copy(alpha = 0.45f).toHexColor(),
            accentColor.copy(alpha = 0.7f).toHexColor(),
            accentColor.toHexColor()
        )
    }
    val cellSize = 12.dp
    val cellGap = 3.dp
    val monthSlotWidth = 15
    val dayLabelWidth = 24.dp

    Box(
        Modifier
            .fillMaxWidth()
            .clip(ProfileCardShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.12f),
                        contentColor.copy(alpha = 0.03f)
                    )
                )
            )
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    BasicText(
                        "Contribution Graph",
                        style = TextStyle(contentColor, 13.sp, FontWeight.SemiBold)
                    )
                    BasicText(
                        "Visible to anyone who visits this profile",
                        style = TextStyle(contentColor.copy(alpha = 0.56f), 10.sp)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    BasicText(
                        contributionCalendar.totalContributions.toString(),
                        style = TextStyle(contentColor, 20.sp, FontWeight.Bold)
                    )
                    BasicText(
                        "last year",
                        style = TextStyle(contentColor.copy(alpha = 0.56f), 10.sp)
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GitHubGraphInfoChip(
                    modifier = Modifier.weight(1f),
                    value = activeDays.toString(),
                    label = "Active days",
                    contentColor = contentColor
                )
                GitHubGraphInfoChip(
                    modifier = Modifier.weight(1f),
                    value = (peakDay?.contributionCount ?: 0).toString(),
                    label = "Peak day",
                    contentColor = contentColor
                )
                GitHubGraphInfoChip(
                    modifier = Modifier.weight(1f),
                    value = when {
                        contributionCalendar.contributionYears.isEmpty() -> "1"
                        else -> contributionCalendar.contributionYears.size.toString()
                    },
                    label = "Years",
                    contentColor = contentColor
                )
            }

            Column(
                modifier = Modifier.horizontalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Spacer(Modifier.width(dayLabelWidth))
                    monthSegments.forEach { month ->
                        Box(
                            Modifier
                                .width((month.totalWeeks.coerceAtLeast(1) * monthSlotWidth).dp)
                                .padding(end = cellGap)
                        ) {
                            BasicText(
                                month.name.take(3),
                                style = TextStyle(contentColor.copy(alpha = 0.52f), 10.sp)
                            )
                        }
                    }
                }

                Row {
                    Column(
                        modifier = Modifier.width(dayLabelWidth),
                        verticalArrangement = Arrangement.spacedBy(cellGap)
                    ) {
                        repeat(7) { index ->
                            Box(
                                Modifier.height(cellSize),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                val label = when (index) {
                                    0 -> "M"
                                    2 -> "W"
                                    4 -> "F"
                                    else -> ""
                                }
                                if (label.isNotEmpty()) {
                                    BasicText(
                                        label,
                                        style = TextStyle(contentColor.copy(alpha = 0.42f), 9.sp)
                                    )
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(cellGap)) {
                        contributionCalendar.weeks.forEach { week ->
                            Column(verticalArrangement = Arrangement.spacedBy(cellGap)) {
                                week.contributionDays
                                    .sortedBy { it.weekday }
                                    .forEach { day ->
                                        val squareColor = contributionColorOrDefault(
                                            hex = day.color,
                                            fallback = accentColor,
                                            alpha = when {
                                                day.contributionCount <= 0 -> 0.08f
                                                else -> 1f
                                            }
                                        )
                                        Box(
                                            Modifier
                                                .size(cellSize)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(
                                                    if (day.contributionCount <= 0) {
                                                        contentColor.copy(alpha = 0.08f)
                                                    } else {
                                                        squareColor
                                                    }
                                                )
                                                .border(
                                                    width = 0.8.dp,
                                                    color = if (selectedDay == day) {
                                                        contentColor.copy(alpha = 0.42f)
                                                    } else {
                                                        contentColor.copy(alpha = 0.06f)
                                                    },
                                                    shape = RoundedCornerShape(3.dp)
                                                )
                                                .clickable { selectedDay = day }
                                        )
                                    }
                            }
                        }
                    }
                }
            }

            highlightedDay?.let { day ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(contentColor.copy(alpha = 0.05f))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        BasicText(
                            "${day.contributionCount} contributions",
                            style = TextStyle(contentColor, 13.sp, FontWeight.Medium)
                        )
                        BasicText(
                            formatGitHubContributionDate(day.date),
                            style = TextStyle(contentColor.copy(alpha = 0.56f), 10.sp)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BasicText("Less", style = TextStyle(contentColor.copy(alpha = 0.46f), 10.sp))
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(contentColor.copy(alpha = 0.08f))
                )
                palette.takeLast(4).forEach { hex ->
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(contributionColorOrDefault(hex, accentColor))
                    )
                }
                BasicText("More", style = TextStyle(contentColor.copy(alpha = 0.46f), 10.sp))
            }
        }
    }
}

@Composable
private fun GitHubLanguageOrbit(
    languages: List<Map.Entry<String, LanguageStat>>,
    contentColor: Color,
    accentColor: Color,
    isVisible: Boolean
) {
    val density = LocalDensity.current
    val chartProgress by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = GitHubLanguageOrbitDurationMs,
            delayMillis = GitHubLanguageOrbitDelayMs,
            easing = LinearEasing
        ),
        label = "githubLanguageOrbit"
    )
    val totalShare = languages.sumOf { it.value.percentage }.takeIf { it > 0.0 } ?: 1.0
    val leadLanguage = languages.firstOrNull()
    val dragHintColor = accentColor.copy(alpha = 0.74f)
    val horizontalTravelPx = remember(density) { with(density) { 210.dp.toPx() } }
    val verticalTravelPx = remember(density) { with(density) { 132.dp.toPx() } }
    val maxOffsetX = remember(density) { with(density) { 14.dp.toPx() } }
    val maxOffsetY = remember(density) { with(density) { 9.dp.toPx() } }
    var rotationTarget by remember(languages) { mutableStateOf(0f) }
    var tiltTarget by remember(languages) { mutableStateOf(0f) }
    var offsetXTarget by remember(languages) { mutableStateOf(0f) }
    var offsetYTarget by remember(languages) { mutableStateOf(0f) }
    val animatedRotation by animateFloatAsState(
        targetValue = if (isVisible) rotationTarget else 0f,
        animationSpec = tween(
            durationMillis = GitHubInteractiveModelMotionDurationMs,
            easing = FastOutSlowInEasing
        ),
        label = "githubPieRotation"
    )
    val animatedTilt by animateFloatAsState(
        targetValue = if (isVisible) tiltTarget else 0f,
        animationSpec = tween(
            durationMillis = GitHubInteractiveModelMotionDurationMs,
            easing = FastOutSlowInEasing
        ),
        label = "githubPieTilt"
    )
    val animatedOffsetX by animateFloatAsState(
        targetValue = if (isVisible) offsetXTarget else 0f,
        animationSpec = tween(
            durationMillis = GitHubInteractiveModelMotionDurationMs,
            easing = FastOutSlowInEasing
        ),
        label = "githubPieOffsetX"
    )
    val animatedOffsetY by animateFloatAsState(
        targetValue = if (isVisible) offsetYTarget else 0f,
        animationSpec = tween(
            durationMillis = GitHubInteractiveModelMotionDurationMs,
            easing = FastOutSlowInEasing
        ),
        label = "githubPieOffsetY"
    )
    val orbitSlices = remember(languages, totalShare, animatedRotation, chartProgress) {
        buildGitHubOrbitSlices(
            languages = languages,
            totalShare = totalShare,
            rotationDegrees = animatedRotation,
            revealProgress = chartProgress
        )
    }
    val focusSlice = remember(orbitSlices, leadLanguage) {
        orbitSlices.maxByOrNull { it.frontness }
            ?: leadLanguage?.let {
                GitHubOrbitSliceSpec(
                    name = it.key,
                    percentage = it.value.percentage,
                    color = getLanguageColor(it.key),
                    startAngle = -90f,
                    fullSweep = 0f,
                    animatedSweep = 0f,
                    midAngle = -90f,
                    frontness = 0f
                )
            }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = 0.46f + (0.54f * chartProgress)
                translationY = (1f - chartProgress) * 34f
            }
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    BasicText(
                        "3D Language Breakdown",
                        style = TextStyle(contentColor, 13.sp, FontWeight.SemiBold)
                    )
                    BasicText(
                        "Drag the model to rotate and tilt it",
                        style = TextStyle(contentColor.copy(alpha = 0.56f), 10.sp)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    BasicText(
                        focusSlice?.name ?: leadLanguage?.key ?: "GitHub",
                        style = TextStyle(contentColor, 14.sp, FontWeight.Bold)
                    )
                    BasicText(
                        focusSlice?.percentage?.toInt()?.let { "$it% front view" }
                            ?: leadLanguage?.value?.percentage?.toInt()?.let { "$it% lead" }
                            ?: "",
                        style = TextStyle(contentColor.copy(alpha = 0.56f), 10.sp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(234.dp)
                    .pointerInput(languages, isVisible) {
                        detectDragGestures(
                            onDragEnd = {
                                rotationTarget = normalizeRotationDegrees(rotationTarget)
                            },
                            onDragCancel = {
                                rotationTarget = normalizeRotationDegrees(rotationTarget)
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            rotationTarget = normalizeRotationDegrees(
                                rotationTarget + ((dragAmount.x / horizontalTravelPx) * 170f)
                            )
                            tiltTarget = (
                                tiltTarget - (dragAmount.y / verticalTravelPx)
                            ).coerceIn(-1f, 1f)
                            offsetXTarget = (
                                offsetXTarget + (dragAmount.x * 0.05f)
                            ).coerceIn(-maxOffsetX, maxOffsetX)
                            offsetYTarget = (
                                offsetYTarget + (dragAmount.y * 0.04f)
                            ).coerceIn(-maxOffsetY, maxOffsetY)
                        }
                    }
            ) {
                Canvas(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp)) {
                    val pieWidth = size.width * 0.78f
                    val tiltFactor = ((animatedTilt + 1f) / 2f).coerceIn(0f, 1f)
                    val pieHeight = size.height * (0.38f + (tiltFactor * 0.18f))
                    val topLeft = Offset(
                        x = ((size.width - pieWidth) / 2f) + animatedOffsetX,
                        y = (size.height * 0.20f) + animatedOffsetY
                    )
                    val pieSize = Size(pieWidth, pieHeight)
                    val depth = size.height * (0.13f + (tiltFactor * 0.12f))
                    val baseShadowTopLeft = Offset(
                        x = topLeft.x + (pieWidth * 0.05f) + (animatedOffsetX * 0.08f),
                        y = topLeft.y + pieHeight + (depth * 0.9f) + (animatedOffsetY * 0.18f)
                    )
                    val shadowStrength = 0.12f + (0.12f * chartProgress)
                    val depthOrder = orbitSlices.sortedBy { it.frontness }
                    val rotationEnergy = (animatedRotation.absoluteValue / 180f).coerceIn(0f, 1f)

                    drawOval(
                        color = Color.Black.copy(alpha = shadowStrength),
                        topLeft = baseShadowTopLeft,
                        size = Size(pieWidth * 0.9f, pieHeight * 0.36f)
                    )

                    depthOrder.forEachIndexed { index, slice ->
                        val frontBias = ((slice.frontness + 1f) / 2f).coerceIn(0f, 1f)
                        val leadingBoost = if (slice.name == focusSlice?.name) 3.5f else 0f
                        val sliceLift = (
                            if (index == depthOrder.lastIndex) 7f else 4.5f
                        ) + (rotationEnergy * 4f) + (frontBias * 6f) + leadingBoost
                        val sliceOffsetX = cos(Math.toRadians(slice.midAngle.toDouble())).toFloat() * sliceLift
                        val sliceOffsetY = sin(Math.toRadians(slice.midAngle.toDouble())).toFloat() * (sliceLift * 0.42f)
                        val topOffset = Offset(topLeft.x + sliceOffsetX, topLeft.y + sliceOffsetY)
                        val sideDepth = depth * (0.24f + (0.76f * frontBias))
                        val sideColor = slice.color.darken(0.36f + ((1f - frontBias) * 0.08f))
                        val topColor = slice.color.lighten(0.06f + (frontBias * 0.10f))

                        for (layer in sideDepth.toInt() downTo 2 step 3) {
                            drawArc(
                                color = sideColor.copy(alpha = 0.22f + (0.34f * frontBias)),
                                startAngle = slice.startAngle,
                                sweepAngle = slice.animatedSweep,
                                useCenter = true,
                                topLeft = Offset(topOffset.x, topOffset.y + layer),
                                size = pieSize
                            )
                        }

                        drawArc(
                            color = topColor,
                            startAngle = slice.startAngle,
                            sweepAngle = slice.animatedSweep,
                            useCenter = true,
                            topLeft = topOffset,
                            size = pieSize
                        )
                        drawArc(
                            color = topColor.lighten(0.18f).copy(alpha = 0.32f + (0.22f * frontBias)),
                            startAngle = slice.startAngle + 2f,
                            sweepAngle = (slice.animatedSweep - 4f).coerceAtLeast(0f),
                            useCenter = true,
                            topLeft = Offset(topOffset.x, topOffset.y + 4f),
                            size = Size(pieSize.width, pieSize.height * 0.86f)
                        )
                        drawArc(
                            color = Color.White.copy(alpha = 0.10f + (0.12f * frontBias)),
                            startAngle = slice.startAngle,
                            sweepAngle = slice.animatedSweep,
                            useCenter = false,
                            topLeft = topOffset,
                            size = pieSize,
                            style = Stroke(width = 1.6f, cap = StrokeCap.Round)
                        )
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GitHubOrbitInfoChip(
                    modifier = Modifier.weight(1f),
                    title = "Front Slice",
                    value = focusSlice?.let { "${it.name} ${it.percentage.toInt()}%" } ?: "GitHub",
                    tint = focusSlice?.color ?: dragHintColor,
                    contentColor = contentColor
                )
                GitHubOrbitInfoChip(
                    modifier = Modifier.weight(1f),
                    title = "Control",
                    value = "Drag to rotate and tilt",
                    tint = dragHintColor,
                    contentColor = contentColor
                )
            }

            leadLanguage?.let { primary ->
                BasicText(
                    "Leading language: ${primary.key} at ${primary.value.percentage.toInt()}%",
                    style = TextStyle(contentColor.copy(alpha = 0.52f), 10.sp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                languages.forEachIndexed { index, (lang, stat) ->
                    GitHubLanguageBar(
                        language = lang,
                        percentage = stat.percentage,
                        color = getLanguageColor(lang),
                        contentColor = contentColor,
                        isVisible = isVisible,
                        delayMillis = 980 + (index * 180)
                    )
                }
            }
        }
    }
}

@Composable
private fun GitHubOrbitInfoChip(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    tint: Color,
    contentColor: Color
) {
    Box(
        modifier
            .clip(ProfileCardShape)
            .background(contentColor.copy(alpha = 0.05f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(tint)
                )
                BasicText(
                    title,
                    style = TextStyle(contentColor.copy(alpha = 0.54f), 10.sp, FontWeight.Medium)
                )
            }
            BasicText(
                value,
                style = TextStyle(contentColor, 12.sp, FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GitHubGraphInfoChip(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    contentColor: Color
) {
    Box(
        modifier
            .clip(ProfileCardShape)
            .background(contentColor.copy(alpha = 0.05f))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText(
                value,
                style = TextStyle(contentColor, 14.sp, FontWeight.SemiBold)
            )
            BasicText(
                label,
                style = TextStyle(contentColor.copy(alpha = 0.52f), 10.sp)
            )
        }
    }
}

@Composable
private fun GitHubLanguageBar(
    language: String,
    percentage: Double,
    color: Color,
    contentColor: Color,
    isVisible: Boolean,
    delayMillis: Int = 0
) {
    val animatedWidth by animateFloatAsState(
        targetValue = if (isVisible) (percentage / 100.0).toFloat().coerceIn(0f, 1f) else 0f,
        animationSpec = tween(
            durationMillis = GitHubLanguageBarDurationMs,
            delayMillis = delayMillis,
            easing = FastOutSlowInEasing
        ),
        label = "githubLanguageBar$language"
    )
    val revealProgress by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = GitHubLanguageBarRevealDurationMs,
            delayMillis = max(0, delayMillis / 2),
            easing = FastOutSlowInEasing
        ),
        label = "githubLanguageBarReveal$language"
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = 0.4f + (0.6f * revealProgress)
                    translationY = (1f - revealProgress) * 18f
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(Modifier.width(6.dp))
                BasicText(
                    language,
                    style = TextStyle(contentColor, 12.sp, FontWeight.Medium)
                )
            }
            BasicText(
                "${percentage.toInt()}%",
                style = TextStyle(contentColor.copy(alpha = 0.56f), 11.sp)
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(contentColor.copy(alpha = 0.08f))
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedWidth)
                    .clip(RoundedCornerShape(999.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun GitHubSignalGraph(
    contributionCalendar: GitHubContributionCalendar,
    contentColor: Color,
    accentColor: Color,
    isVisible: Boolean
) {
    val weeklySignals = remember(contributionCalendar) {
        contributionCalendar.weeks.map { week ->
            val orderedDays = week.contributionDays.sortedBy { it.weekday }
            GitHubWeeklySignal(
                total = orderedDays.sumOf { it.contributionCount },
                activeDays = orderedDays.count { it.contributionCount > 0 },
                peakDay = orderedDays.maxOfOrNull { it.contributionCount } ?: 0
            )
        }
    }
    val contributionSeries = remember(weeklySignals) {
        normalizeGraphSeries(smoothGraphSeries(weeklySignals.map { it.total.toFloat() }))
    }
    val activeSeries = remember(weeklySignals) {
        normalizeGraphSeries(weeklySignals.map { it.activeDays.toFloat() })
    }
    val peakSeries = remember(weeklySignals) {
        normalizeGraphSeries(smoothGraphSeries(weeklySignals.map { it.peakDay.toFloat() }))
    }
    val highlightWeek = remember(weeklySignals) {
        weeklySignals.maxByOrNull { it.total }
    }
    val revealProgress by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = GitHubSignalGraphDurationMs,
            delayMillis = GitHubSignalGraphDelayMs,
            easing = LinearEasing
        ),
        label = "githubSignalGraph"
    )
    val monthLabels = remember(contributionCalendar) {
        contributionCalendar.months
            .filterIndexed { index, _ -> index % 2 == 0 }
            .map { it.name.take(3) }
            .takeLast(6)
            .ifEmpty { listOf("Jan", "Mar", "May", "Jul", "Sep", "Now") }
    }
    // Light content color means a dark surface: keep the neon palette there,
    // switch to deeper tones on light themes where neon washes out.
    val isDarkSurface = contentColor.luminance() > 0.5f
    val lineColors = if (isDarkSurface) {
        listOf(
            Color(0xFF42D9FF),
            Color(0xFFFF5DA2),
            Color(0xFFFFB444)
        )
    } else {
        listOf(
            Color(0xFF0284C7),
            Color(0xFFDB2777),
            Color(0xFFD97706)
        )
    }

    Box(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = 0.42f + (0.58f * revealProgress)
                translationY = (1f - revealProgress) * 38f
            }
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    BasicText(
                        "Signal Graph",
                        style = TextStyle(contentColor, 13.sp, FontWeight.SemiBold)
                    )
                    BasicText(
                        "Weekly activity, active days, and bursts",
                        style = TextStyle(contentColor.copy(alpha = 0.56f), 10.sp)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    BasicText(
                        (highlightWeek?.total ?: 0).toString(),
                        style = TextStyle(contentColor, 18.sp, FontWeight.Bold)
                    )
                    BasicText(
                        "best week",
                        style = TextStyle(contentColor.copy(alpha = 0.54f), 10.sp)
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GitHubSignalLegendPill(
                    modifier = Modifier.weight(1f),
                    label = "Flow",
                    color = lineColors[0],
                    contentColor = contentColor
                )
                GitHubSignalLegendPill(
                    modifier = Modifier.weight(1f),
                    label = "Active",
                    color = lineColors[1],
                    contentColor = contentColor
                )
                GitHubSignalLegendPill(
                    modifier = Modifier.weight(1f),
                    label = "Peaks",
                    color = lineColors[2],
                    contentColor = contentColor
                )
            }

            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(188.dp)
            ) {
                val leftPadding = 8.dp.toPx()
                val rightPadding = 8.dp.toPx()
                val topPadding = 12.dp.toPx()
                val bottomPadding = 24.dp.toPx()
                val chartWidth = size.width - leftPadding - rightPadding
                val chartHeight = size.height - topPadding - bottomPadding
                val baselineY = topPadding + chartHeight
                val gridColor = contentColor.copy(alpha = 0.10f)

                for (index in 0..4) {
                    val y = topPadding + ((chartHeight / 4f) * index)
                    drawLine(
                        color = gridColor,
                        start = Offset(leftPadding, y),
                        end = Offset(size.width - rightPadding, y),
                        strokeWidth = 1f
                    )
                }

                for (index in 0..6) {
                    val x = leftPadding + ((chartWidth / 6f) * index)
                    drawLine(
                        color = gridColor.copy(alpha = 0.06f),
                        start = Offset(x, topPadding),
                        end = Offset(x, baselineY),
                        strokeWidth = 1f
                    )
                }

                fun toPoints(series: List<Float>): List<Offset> {
                    if (series.isEmpty()) return emptyList()
                    return series.mapIndexed { index, value ->
                        val x = if (series.size == 1) {
                            leftPadding + (chartWidth / 2f)
                        } else {
                            leftPadding + ((chartWidth / (series.lastIndex).coerceAtLeast(1)) * index)
                        }
                        val y = baselineY - (value.coerceIn(0f, 1f) * chartHeight * 0.92f)
                        Offset(x, y)
                    }
                }

                val primaryPoints = trimGraphPoints(toPoints(contributionSeries), revealProgress)
                val activePoints = trimGraphPoints(toPoints(activeSeries), revealProgress)
                val peakPoints = trimGraphPoints(toPoints(peakSeries), revealProgress)

                if (primaryPoints.isNotEmpty()) {
                    drawPath(
                        path = buildAreaPath(primaryPoints, baselineY),
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                lineColors[0].copy(alpha = 0.28f),
                                Color.Transparent
                            ),
                            startY = topPadding,
                            endY = baselineY
                        )
                    )
                }

                listOf(
                    primaryPoints to lineColors[0],
                    activePoints to lineColors[1],
                    peakPoints to lineColors[2]
                ).forEachIndexed { index, (points, color) ->
                    if (points.size >= 2) {
                        drawPath(
                            path = buildSmoothGraphPath(points),
                            color = color,
                            style = Stroke(
                                width = if (index == 0) 5f else 3.6f,
                                cap = StrokeCap.Round
                            )
                        )
                    }

                    points.lastOrNull()?.let { point ->
                        drawCircle(
                            color = color.copy(alpha = 0.28f),
                            radius = 12f + (index * 2f),
                            center = point
                        )
                        drawCircle(
                            color = contentColor.copy(alpha = 0.88f),
                            radius = 4.2f,
                            center = point
                        )
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                monthLabels.forEach { month ->
                    BasicText(
                        month,
                        style = TextStyle(contentColor.copy(alpha = 0.44f), 10.sp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GitHubSignalLegendPill(
    modifier: Modifier = Modifier,
    label: String,
    color: Color,
    contentColor: Color
) {
    Row(
        modifier
            .clip(RoundedCornerShape(999.dp))
            .background(contentColor.copy(alpha = 0.07f))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        BasicText(
            label,
            style = TextStyle(contentColor.copy(alpha = 0.72f), 10.sp, FontWeight.Medium)
        )
    }
}

@Composable
private fun GitHubRepositoryCard(
    repo: TopRepo,
    contentColor: Color,
    onOpen: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(ProfileCardShape)
            .background(contentColor.copy(alpha = 0.05f))
            .clickable(onClick = onOpen)
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    BasicText(
                        repo.name,
                        style = TextStyle(contentColor, 13.sp, FontWeight.Medium)
                    )
                    repo.description?.takeIf { it.isNotBlank() }?.let { desc ->
                        BasicText(
                            desc,
                            style = TextStyle(contentColor.copy(alpha = 0.58f), 11.sp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.End) {
                    BasicText("⭐ ${repo.stars}", style = TextStyle(contentColor.copy(alpha = 0.64f), 11.sp))
                    BasicText("🍴 ${repo.forks}", style = TextStyle(contentColor.copy(alpha = 0.64f), 11.sp))
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repo.language?.takeIf { it.isNotBlank() }?.let { language ->
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(getLanguageColor(language).copy(alpha = 0.18f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        BasicText(
                            language,
                            style = TextStyle(getLanguageColor(language), 10.sp, FontWeight.Medium)
                        )
                    }
                }
                repo.updatedAt?.takeIf { it.isNotBlank() }?.let { updatedAt ->
                    BasicText(
                        "Updated ${formatGitHubRepoDate(updatedAt)}",
                        style = TextStyle(contentColor.copy(alpha = 0.46f), 10.sp)
                    )
                }
            }
        }
    }
}

private data class GitHubWeeklySignal(
    val total: Int,
    val activeDays: Int,
    val peakDay: Int
)

private data class GitHubOrbitSliceSpec(
    val name: String,
    val percentage: Double,
    val color: Color,
    val startAngle: Float,
    val fullSweep: Float,
    val animatedSweep: Float,
    val midAngle: Float,
    val frontness: Float
)

private fun normalizeRotationDegrees(value: Float): Float {
    val normalized = value % 360f
    return when {
        normalized > 180f -> normalized - 360f
        normalized < -180f -> normalized + 360f
        else -> normalized
    }
}

private fun buildGitHubOrbitSlices(
    languages: List<Map.Entry<String, LanguageStat>>,
    totalShare: Double,
    rotationDegrees: Float,
    revealProgress: Float
): List<GitHubOrbitSliceSpec> {
    val collapsedStartAngle = -120f + (30f * revealProgress) + rotationDegrees
    var startAngle = collapsedStartAngle
    return languages.map { (language, stat) ->
        val share = (stat.percentage / totalShare).toFloat().coerceIn(0f, 1f)
        val fullSweep = (360f * share).coerceAtLeast(8f)
        val animatedSweep = fullSweep * revealProgress
        val midAngle = startAngle + (animatedSweep / 2f)
        val frontness = sin(Math.toRadians(midAngle.toDouble())).toFloat()
        GitHubOrbitSliceSpec(
            name = language,
            percentage = stat.percentage,
            color = getLanguageColor(language),
            startAngle = startAngle,
            fullSweep = fullSweep,
            animatedSweep = animatedSweep,
            midAngle = midAngle,
            frontness = frontness
        ).also {
            startAngle += fullSweep
        }
    }
}

private fun smoothGraphSeries(values: List<Float>): List<Float> {
    if (values.size < 3) return values
    return values.mapIndexed { index, _ ->
        val start = max(0, index - 1)
        val end = min(values.lastIndex, index + 1)
        values.subList(start, end + 1).average().toFloat()
    }
}

private fun normalizeGraphSeries(values: List<Float>): List<Float> {
    if (values.isEmpty()) return emptyList()
    val minValue = values.minOrNull() ?: 0f
    val maxValue = values.maxOrNull() ?: 0f
    val range = maxValue - minValue
    if (range == 0f) return values.map { 0.5f }
    return values.map { ((it - minValue) / range).coerceIn(0f, 1f) }
}

private fun trimGraphPoints(points: List<Offset>, progress: Float): List<Offset> {
    if (points.isEmpty()) return emptyList()
    if (points.size == 1) return points
    val clampedProgress = progress.coerceIn(0f, 1f)
    if (clampedProgress <= 0f) return listOf(points.first())
    if (clampedProgress >= 1f) return points

    val rawIndex = clampedProgress * (points.lastIndex)
    val fullIndex = rawIndex.toInt()
    val remainder = rawIndex - fullIndex
    val visiblePoints = points.take(fullIndex + 1).toMutableList()

    if (fullIndex < points.lastIndex) {
        val start = points[fullIndex]
        val end = points[fullIndex + 1]
        visiblePoints += Offset(
            x = start.x + ((end.x - start.x) * remainder),
            y = start.y + ((end.y - start.y) * remainder)
        )
    }

    return visiblePoints
}

private fun buildSmoothGraphPath(points: List<Offset>): Path {
    return Path().apply {
        if (points.isEmpty()) return@apply
        moveTo(points.first().x, points.first().y)
        if (points.size == 1) return@apply
        for (index in 1 until points.size) {
            val previous = points[index - 1]
            val current = points[index]
            val controlX = (previous.x + current.x) / 2f
            cubicTo(
                controlX,
                previous.y,
                controlX,
                current.y,
                current.x,
                current.y
            )
        }
    }
}

private fun buildAreaPath(points: List<Offset>, baselineY: Float): Path {
    return buildSmoothGraphPath(points).apply {
        if (points.isNotEmpty()) {
            lineTo(points.last().x, baselineY)
            lineTo(points.first().x, baselineY)
            close()
        }
    }
}

private fun Color.mixWith(other: Color, amount: Float): Color {
    val fraction = amount.coerceIn(0f, 1f)
    return Color(
        red = red + ((other.red - red) * fraction),
        green = green + ((other.green - green) * fraction),
        blue = blue + ((other.blue - blue) * fraction),
        alpha = alpha + ((other.alpha - alpha) * fraction)
    )
}

private fun Color.lighten(amount: Float): Color = mixWith(Color.White, amount)

private fun Color.darken(amount: Float): Color = mixWith(Color.Black, amount)

private fun getLanguageColor(language: String): Color {
    return when (language.lowercase()) {
        "kotlin" -> Color(0xFFA97BFF)
        "java" -> Color(0xFFB07219)
        "javascript" -> Color(0xFFF1E05A)
        "typescript" -> Color(0xFF3178C6)
        "python" -> Color(0xFF3572A5)
        "rust" -> Color(0xFFDEA584)
        "go" -> Color(0xFF00ADD8)
        "swift" -> Color(0xFFFA7343)
        "c++" -> Color(0xFFF34B7D)
        "c" -> Color(0xFF555555)
        else -> Color(0xFF858585)
    }
}

private fun githubMetricProgress(value: Int, softCap: Int): Float {
    if (value <= 0 || softCap <= 0) return 0f
    val numerator = ln((value + 1).toFloat())
    val denominator = ln((softCap + 1).toFloat())
    if (denominator == 0f) return 0f
    return (numerator / denominator).coerceIn(0.08f, 1f)
}

private fun contributionColorOrDefault(hex: String?, fallback: Color, alpha: Float = 1f): Color {
    val parsed = runCatching {
        if (hex.isNullOrBlank()) fallback else Color(android.graphics.Color.parseColor(hex))
    }.getOrElse { fallback }
    return parsed.copy(alpha = alpha.coerceIn(0f, 1f))
}

private fun Color.toHexColor(): String {
    val red = (red * 255).toInt().coerceIn(0, 255)
    val green = (green * 255).toInt().coerceIn(0, 255)
    val blue = (blue * 255).toInt().coerceIn(0, 255)
    return String.format("#%02X%02X%02X", red, green, blue)
}

// ==================== Activity Calendar Section ====================

@Composable
fun ActivityCalendarSection(
    heatmap: List<ActivityHeatmapDay>,
    stats: ProfileStats,
    availableYears: List<Int>,
    selectedYear: Int?,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onYearChange: (Int) -> Unit
) {
    var showYearDropdown by remember { mutableStateOf(false) }

    SectionCard(
        title = "Activity",
        backdrop = backdrop,
        contentColor = contentColor,
        accentColor = accentColor
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header with year selector and streak
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Streak info
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            StreakFireLottie(modifier = Modifier.size(20.dp))
                            BasicText(
                                "${stats.currentStreak}",
                                style = TextStyle(contentColor, 14.sp, FontWeight.Bold)
                            )
                        }
                        BasicText("Current", style = TextStyle(contentColor.copy(alpha = 0.5f), 10.sp))
                    }
                    Column {
                        BasicText("🏆 ${stats.longestStreak}", style = TextStyle(contentColor, 14.sp, FontWeight.Bold))
                        BasicText("Longest", style = TextStyle(contentColor.copy(alpha = 0.5f), 10.sp))
                    }
                }

                // Year selector
                if (availableYears.isNotEmpty()) {
                    Box {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(contentColor.copy(alpha = 0.1f))
                                .clickable { showYearDropdown = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BasicText(
                                    "${selectedYear ?: availableYears.firstOrNull() ?: ""}",
                                    style = TextStyle(contentColor, 13.sp)
                                )
                                Spacer(Modifier.width(4.dp))
                                BasicText("▼", style = TextStyle(contentColor.copy(alpha = 0.5f), 10.sp))
                            }
                        }

                        DropdownMenu(
                            expanded = showYearDropdown,
                            onDismissRequest = { showYearDropdown = false }
                        ) {
                            availableYears.forEach { year ->
                                DropdownMenuItem(
                                    text = { BasicText("$year", style = TextStyle(contentColor)) },
                                    onClick = {
                                        onYearChange(year)
                                        showYearDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Activity heatmap grid
            if (heatmap.isNotEmpty()) {
                ActivityHeatmapGrid(
                    days = heatmap,
                    accentColor = accentColor,
                    contentColor = contentColor
                )
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(ProfileCardShape)
                        .background(contentColor.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        "No activity data available",
                        style = TextStyle(contentColor.copy(alpha = 0.5f), 13.sp)
                    )
                }
            }

            // Legend
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText("Less", style = TextStyle(contentColor.copy(alpha = 0.5f), 10.sp))
                listOf(0, 1, 2, 3).forEach { level ->
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(getHeatmapColor(level, accentColor))
                    )
                }
                BasicText("More", style = TextStyle(contentColor.copy(alpha = 0.5f), 10.sp))
            }
        }
    }
}

@Composable
private fun ActivityHeatmapGrid(
    days: List<ActivityHeatmapDay>,
    accentColor: Color,
    contentColor: Color
) {
    val scrollState = rememberScrollState()
    var selectedDay by remember(days) { mutableStateOf<ActivityHeatmapDay?>(null) }

    // Group days by week
    val weeks = days.chunked(7)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            weeks.forEach { week ->
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    week.forEach { day ->
                        val level = activityHeatmapLevel(day)
                        val shape = RoundedCornerShape(2.dp)
                        Box(
                            Modifier
                                .size(12.dp)
                                .clip(shape)
                                .background(getHeatmapColor(level, accentColor))
                                .border(
                                    width = 0.8.dp,
                                    color = if (selectedDay?.date == day.date) {
                                        contentColor.copy(alpha = 0.5f)
                                    } else {
                                        contentColor.copy(alpha = 0.05f)
                                    },
                                    shape = shape
                                )
                                .then(
                                    if (day.date.isNotBlank()) {
                                        Modifier.pointerInput(day.date, day.activityCount, day.isActive, day.level) {
                                            detectTapGestures(
                                                onLongPress = { selectedDay = day }
                                            )
                                        }
                                    } else {
                                        Modifier
                                    }
                                )
                        )
                    }
                }
            }
        }

        selectedDay?.takeIf { it.date.isNotBlank() }?.let { day ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(contentColor.copy(alpha = 0.05f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText(
                    formatActivityHeatmapDate(day.date),
                    style = TextStyle(contentColor, 12.sp, FontWeight.SemiBold)
                )
                BasicText(
                    activityHeatmapCountLabel(day),
                    style = TextStyle(contentColor.copy(alpha = 0.58f), 11.sp)
                )
            }
        }
    }
}

private fun activityHeatmapLevel(day: ActivityHeatmapDay): Int {
    if (day.level > 0) return day.level.coerceIn(1, 3)
    if (day.activityCount > 0) return when (day.activityCount) {
        in 1..3 -> 1
        in 4..9 -> 2
        else -> 3
    }
    return if (day.isActive) 1 else 0
}

private fun getHeatmapColor(level: Int, accentColor: Color): Color {
    return when (level) {
        0 -> accentColor.copy(alpha = 0.08f)
        1 -> accentColor.copy(alpha = 0.3f)
        2 -> accentColor.copy(alpha = 0.6f)
        3 -> accentColor
        else -> accentColor.copy(alpha = 0.08f)
    }
}

private fun activityHeatmapCountLabel(day: ActivityHeatmapDay): String {
    if (!day.isActive && day.activityCount <= 0) return "No activity"
    if (day.activityCount <= 0) return "Active day"
    return "${day.activityCount} activit${if (day.activityCount == 1) "y" else "ies"}"
}

// ==================== Skills Section ====================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SkillsSection(
    skills: List<UserSkill>,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    onAddSkill: () -> Unit = {},
    onRemoveSkill: (UserSkill) -> Unit = {}
) {
    SectionCard(
        title = "Skills & Expertise",
        backdrop = backdrop,
        contentColor = contentColor,
        accentColor = accentColor,
        isOwner = isOwner,
        onAdd = onAddSkill
    ) {
        if (skills.isEmpty()) {
            EmptySectionPlaceholder(
                icon = "💡",
                message = "No skills yet",
                contentColor = contentColor
            )
        } else {
            AdaptiveSkillRows(
                skills = skills,
                contentColor = contentColor,
                accentColor = accentColor,
                isOwner = isOwner,
                onRemoveSkill = onRemoveSkill
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdaptiveSkillRows(
    skills: List<UserSkill>,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    onRemoveSkill: (UserSkill) -> Unit
) {
    val skillColumns = remember(skills) { skills.chunked(3) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        skillColumns.forEach { columnSkills ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                columnSkills.forEach { skill ->
                    SkillCompactChip(
                        skill = skill,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isOwner = isOwner,
                        onRemove = { onRemoveSkill(skill) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SkillAdaptiveRow(
    skills: List<UserSkill>,
    rowIndex: Int,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    onRemoveSkill: (UserSkill) -> Unit
) {
    var isVisible by remember(skills) { mutableStateOf(false) }
    LaunchedEffect(skills) {
        isVisible = true
    }
    val rowAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 420,
            delayMillis = rowIndex * 80,
            easing = FastOutSlowInEasing
        ),
        label = "skillRowAlpha"
    )

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = rowAlpha
            },
        horizontalArrangement = Arrangement.spacedBy(
            8.dp,
            Alignment.Start
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        skills.forEach { skill ->
            SkillCompactChip(
                skill = skill,
                contentColor = contentColor,
                accentColor = accentColor,
                isOwner = isOwner,
                onRemove = { onRemoveSkill(skill) }
            )
        }
    }
}

@Composable
private fun SkillCompactChip(
    skill: UserSkill,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    onRemove: () -> Unit = {}
) {
    val proficiency = skill.proficiency?.takeIf { it.isNotBlank() }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(contentColor.copy(alpha = 0.06f))
            .border(
                width = 1.dp,
                color = contentColor.copy(alpha = 0.075f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(2.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(accentColor.copy(alpha = 0.74f))
        )
        Spacer(Modifier.width(8.dp))
        BasicText(
            skill.skill.name,
            style = TextStyle(
                color = contentColor.copy(alpha = 0.86f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 132.dp)
        )
        proficiency?.let { prof ->
            Spacer(Modifier.width(7.dp))
            BasicText(
                prof,
                style = TextStyle(
                    color = accentColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
        if (isOwner) {
            Spacer(Modifier.width(7.dp))
            BasicText(
                "×",
                style = TextStyle(contentColor.copy(alpha = 0.42f), 14.sp),
                modifier = Modifier.clickable { onRemove() }
            )
        }
    }
}

// ==================== Projects Section ====================

@Composable
fun ProjectsSection(
    projects: List<Project>,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    onAddProject: () -> Unit = {},
    onEditProject: (Project) -> Unit = {},
    onViewProject: (Project) -> Unit = {},
    onToggleFeatured: (Project) -> Unit = {}
) {
    val orderedProjects = remember(projects) {
        projects.sortedWith(
            compareByDescending<Project> { it.featured }
                .thenByDescending { it.isCurrent }
                .thenByDescending { it.startDate }
        )
    }
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { orderedProjects.size }
    )

    SectionCard(
        title = "Projects & Work",
        backdrop = backdrop,
        contentColor = contentColor,
        accentColor = accentColor,
        isOwner = isOwner,
        onAdd = onAddProject
    ) {
        if (projects.isEmpty()) {
            // Empty state for owner
            if (isOwner) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BasicText("✨", style = TextStyle(fontSize = 32.sp))
                    Spacer(Modifier.height(8.dp))
                    BasicText(
                        "No work yet",
                        style = TextStyle(contentColor.copy(alpha = 0.6f), 14.sp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(accentColor)
                            .clickable { onAddProject() }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        BasicText(
                            "Add Your First Work",
                            style = TextStyle(Color.White, 14.sp, FontWeight.Medium)
                        )
                    }
                }
            } else {
                EmptySectionPlaceholder(
                    icon = "✨",
                    message = "No work yet",
                    contentColor = contentColor
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        BasicText(
                            text = if (orderedProjects.size > 1) "Swipe through projects" else "Project spotlight",
                            style = TextStyle(
                                color = contentColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        BasicText(
                            text = if (orderedProjects.size > 1) "Move right to explore the rest"
                            else "A richer card for this work",
                            style = TextStyle(
                                color = contentColor.copy(alpha = 0.58f),
                                fontSize = 11.sp
                            )
                        )
                    }

                    if (orderedProjects.size > 1) {
                        ProjectPagerCounter(
                            currentPage = pagerState.currentPage,
                            totalPages = orderedProjects.size,
                            accentColor = accentColor,
                            contentColor = contentColor
                        )
                    } else if (orderedProjects.firstOrNull()?.featured == true) {
                        ProjectBadge(
                            iconRes = R.drawable.ic_sparkles,
                            label = "Featured",
                            tint = Color(0xFF0A66C2),
                            background = Color(0xFF0A66C2).copy(alpha = 0.12f)
                        )
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    pageSpacing = 14.dp,
                    contentPadding = PaddingValues(end = if (orderedProjects.size > 1) 28.dp else 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(430.dp)
                ) { page ->
                    val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
                    val settledOffset = pageOffset.coerceIn(0f, 1f)
                    val motionProgress = 1f - settledOffset

                    ProjectCard(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = 0.7f + (0.3f * motionProgress)
                                scaleX = 0.94f + (0.06f * motionProgress)
                                scaleY = 0.96f + (0.04f * motionProgress)
                                translationY = (1f - motionProgress) * 22f
                            },
                        project = orderedProjects[page],
                        backdrop = backdrop,
                        contentColor = contentColor,
                        isOwner = isOwner,
                        onView = { onViewProject(orderedProjects[page]) },
                        onEdit = { onEditProject(orderedProjects[page]) },
                        onToggleFeatured = { onToggleFeatured(orderedProjects[page]) }
                    )
                }

                if (orderedProjects.size > 1) {
                    ProjectPagerIndicator(
                        currentPage = pagerState.currentPage,
                        totalPages = orderedProjects.size,
                        accentColor = accentColor,
                        contentColor = contentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectPagerCounter(
    currentPage: Int,
    totalPages: Int,
    accentColor: Color,
    contentColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(accentColor.copy(alpha = 0.16f))
            .border(1.dp, accentColor.copy(alpha = 0.24f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        BasicText(
            text = "${currentPage + 1}/$totalPages",
            style = TextStyle(
                color = if (totalPages > 1) accentColor else contentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
private fun ProjectPagerIndicator(
    currentPage: Int,
    totalPages: Int,
    accentColor: Color,
    contentColor: Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalPages) { index ->
            val isSelected = index == currentPage
            val width by animateFloatAsState(
                targetValue = if (isSelected) 24f else 8f,
                animationSpec = tween(durationMillis = 260),
                label = "projectPagerWidth$index"
            )
            val alpha by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0.28f,
                animationSpec = tween(durationMillis = 260),
                label = "projectPagerAlpha$index"
            )

            Box(
                modifier = Modifier
                    .width(width.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (isSelected) accentColor.copy(alpha = alpha)
                        else contentColor.copy(alpha = alpha)
                    )
            )
        }
    }
}

@Composable
private fun ProjectBadge(
    iconRes: Int,
    label: String,
    tint: Color,
    background: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .border(1.dp, tint.copy(alpha = 0.24f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = label,
            modifier = Modifier.size(12.dp),
            colorFilter = ColorFilter.tint(tint)
        )
        BasicText(
            text = label,
            style = TextStyle(
                color = tint,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
private fun ProjectMetaChip(
    iconRes: Int,
    text: String,
    tint: Color,
    background: Color,
    contentColor: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .border(1.dp, tint.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = text,
            modifier = Modifier.size(12.dp),
            colorFilter = ColorFilter.tint(tint)
        )
        BasicText(
            text = text,
            style = TextStyle(
                color = contentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ProjectHeaderAction(
    iconRes: Int,
    tint: Color,
    background: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(background)
            .border(1.dp, tint.copy(alpha = 0.18f), CircleShape)
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            colorFilter = ColorFilter.tint(tint)
        )
    }
}

@Composable
private fun ProjectActionButton(
    iconRes: Int,
    label: String,
    tint: Color,
    background: Color,
    borderColor: Color = tint.copy(alpha = 0.24f),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = label,
            modifier = Modifier.size(14.dp),
            colorFilter = ColorFilter.tint(tint)
        )
        BasicText(
            text = label,
            style = TextStyle(
                color = tint,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

private fun projectTimelineLabel(project: Project): String {
    val endLabel = if (project.isCurrent) "Present" else project.endDate?.let { formatDate(it) }.orEmpty()
    return listOf(formatDate(project.startDate), endLabel)
        .filter { it.isNotBlank() }
        .joinToString(" — ")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectCard(
    modifier: Modifier = Modifier,
    project: Project,
    backdrop: LayerBackdrop,
    contentColor: Color,
    isOwner: Boolean,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onToggleFeatured: () -> Unit
) {
    val context = LocalContext.current
    val heroAccent = Color(0xFF0A66C2)
    val cardBorder = contentColor.copy(alpha = 0.12f)
    val subduedSurface = contentColor.copy(alpha = 0.06f)
    val hasLinks = !project.projectUrl.isNullOrBlank() || !project.githubUrl.isNullOrBlank() || !project.otherLinks.isNullOrEmpty()

    Box(
        modifier
            .vormexSurface(
                backdrop = backdrop,
                tone = VormexSurfaceTone.Subtle,
                cornerRadius = ProfileCardCornerRadius,
                blurRadius = 16.dp,
                lensRadius = 0.dp,
                lensDepth = 0.dp,
                borderColor = cardBorder
            )
            .clickable(onClick = onView)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(188.dp)
            ) {
                if (project.images.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(project.images.first())
                            .crossfade(true)
                            .build(),
                        contentDescription = project.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        heroAccent.copy(alpha = 0.44f),
                                        Color.White.copy(alpha = 0.08f),
                                        contentColor.copy(alpha = 0.1f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(
                                when {
                                    !project.githubUrl.isNullOrBlank() -> R.drawable.ic_code
                                    !project.projectUrl.isNullOrBlank() -> R.drawable.ic_globe
                                    else -> R.drawable.ic_work
                                }
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(42.dp),
                            colorFilter = ColorFilter.tint(heroAccent)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.12f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.42f)
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (project.featured) {
                            ProjectBadge(
                                iconRes = R.drawable.ic_sparkles,
                                label = "Featured",
                                tint = heroAccent,
                                background = Color.Black.copy(alpha = 0.3f)
                            )
                        }
                        if (project.isCurrent) {
                            ProjectBadge(
                                iconRes = R.drawable.ic_check,
                                label = "Active",
                                tint = Color.White,
                                background = Color.White.copy(alpha = 0.14f)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (isOwner) {
                            ProjectHeaderAction(
                                iconRes = R.drawable.ic_sparkles,
                                tint = if (project.featured) heroAccent else Color.White,
                                background = Color.Black.copy(alpha = 0.26f),
                                onClick = onToggleFeatured
                            )
                            ProjectHeaderAction(
                                iconRes = R.drawable.ic_edit,
                                tint = Color.White,
                                background = Color.Black.copy(alpha = 0.26f),
                                onClick = onEdit
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BasicText(
                        text = project.name,
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    project.role?.takeIf { it.isNotBlank() }?.let { role ->
                        BasicText(
                            text = role,
                            style = TextStyle(
                                color = Color.White.copy(alpha = 0.82f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    project.role?.takeIf { it.isNotBlank() }?.let { role ->
                        ProjectMetaChip(
                            iconRes = R.drawable.ic_work,
                            text = role,
                            tint = heroAccent,
                            background = heroAccent.copy(alpha = 0.14f),
                            contentColor = contentColor
                        )
                    }

                    ProjectMetaChip(
                        iconRes = R.drawable.ic_calendar,
                        text = projectTimelineLabel(project),
                        tint = contentColor.copy(alpha = 0.72f),
                        background = subduedSurface,
                        contentColor = contentColor
                    )

                    if (project.images.isNotEmpty()) {
                        ProjectMetaChip(
                            iconRes = R.drawable.ic_image,
                            text = "${project.images.size} image${if (project.images.size > 1) "s" else ""}",
                            tint = contentColor.copy(alpha = 0.72f),
                            background = subduedSurface,
                            contentColor = contentColor
                        )
                    }

                    if (!project.otherLinks.isNullOrEmpty()) {
                        ProjectMetaChip(
                            iconRes = R.drawable.ic_link,
                            text = "${project.otherLinks.size} link${if (project.otherLinks.size > 1) "s" else ""}",
                            tint = contentColor.copy(alpha = 0.72f),
                            background = subduedSurface,
                            contentColor = contentColor
                        )
                    }
                }

                if (project.description.isNotBlank()) {
                    BasicText(
                        text = project.description,
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.82f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        ),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (project.techStack.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        project.techStack.take(5).forEach { tag ->
                            ProjectMetaChip(
                                iconRes = R.drawable.ic_code,
                                text = tag,
                                tint = heroAccent,
                                background = heroAccent.copy(alpha = 0.12f),
                                contentColor = contentColor
                            )
                        }

                        val remaining = project.techStack.size - 5
                        if (remaining > 0) {
                            ProjectMetaChip(
                                iconRes = R.drawable.ic_sparkles,
                                text = "+$remaining",
                                tint = contentColor.copy(alpha = 0.72f),
                                background = subduedSurface,
                                contentColor = contentColor
                            )
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProjectActionButton(
                        iconRes = R.drawable.ic_visibility,
                        label = "View",
                        tint = contentColor,
                        background = subduedSurface,
                        borderColor = contentColor.copy(alpha = 0.12f),
                        onClick = onView
                    )

                    project.projectUrl?.takeIf { it.isNotBlank() }?.let { url ->
                        ProjectActionButton(
                            iconRes = R.drawable.ic_open_in_browser,
                            label = "Live",
                            tint = heroAccent,
                            background = heroAccent.copy(alpha = 0.14f),
                            onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        )
                    }

                    project.githubUrl?.takeIf { it.isNotBlank() }?.let { url ->
                        ProjectActionButton(
                            iconRes = R.drawable.ic_github,
                            label = "Source",
                            tint = contentColor,
                            background = subduedSurface,
                            borderColor = contentColor.copy(alpha = 0.12f),
                            onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        )
                    }

                    if (project.projectUrl.isNullOrBlank() && project.githubUrl.isNullOrBlank() && hasLinks) {
                        project.otherLinks?.firstOrNull()?.url?.let { url ->
                            ProjectActionButton(
                                iconRes = R.drawable.ic_link,
                                label = "Links",
                                tint = heroAccent,
                                background = heroAccent.copy(alpha = 0.14f),
                                onClick = {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== Activity Feed Section ====================

@Composable
fun ActivityFeedHeaderSection(
    currentFilter: String,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onFilterChange: (String) -> Unit
) {
    val filters = listOf(
        "all" to "All",
        "posts" to "Posts",
        "articles" to "Articles",
        "videos" to "Reels"
    )

    SectionCard(
        title = "Activity",
        backdrop = backdrop,
        contentColor = contentColor,
        accentColor = accentColor
    ) {
        Column {
            val selectedTextColor =
                if (accentColor.luminance() > 0.6f) Color(0xFF111111) else Color.White
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { (filter, label) ->
                    val isSelected = currentFilter == filter
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                if (isSelected) accentColor
                                else contentColor.copy(alpha = 0.06f)
                            )
                            .border(
                                1.dp,
                                if (isSelected) Color.Transparent else contentColor.copy(alpha = 0.1f),
                                RoundedCornerShape(999.dp)
                            )
                            .clickable { onFilterChange(filter) }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        BasicText(
                            label,
                            style = TextStyle(
                                if (isSelected) selectedTextColor else contentColor.copy(alpha = 0.65f),
                                13.sp,
                                if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityFeedGridSection(
    items: List<FeedItem>,
    isLoading: Boolean,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    onOpenItem: (FeedItem) -> Unit,
    onDeletePost: (String) -> Unit
) {
    val gridItems = remember(items) { items.mapNotNull(::activityGridItemFor) }

    ActivityFeedSurface {
        when {
            gridItems.isNotEmpty() -> {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    gridItems.chunked(3).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            rowItems.forEach { gridItem ->
                                ActivityFeedGridTile(
                                    gridItem = gridItem,
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    isOwner = isOwner,
                                    onOpenItem = onOpenItem,
                                    onDeletePost = onDeletePost,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            repeat(3 - rowItems.size) {
                                Spacer(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                )
                            }
                        }
                    }
                }
            }
            !isLoading -> {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(ProfileCardShape)
                        .background(contentColor.copy(alpha = 0.05f))
                ) {
                    EmptySectionPlaceholder(
                        icon = "",
                        iconRes = R.drawable.ic_vx_gallery,
                        message = "No activity found for this filter",
                        contentColor = contentColor
                    )
                }
            }
        }
    }
}

@Composable
fun ActivityFeedItemSection(
    item: FeedItem,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    onOpenItem: (FeedItem) -> Unit,
    onVotePoll: (String, String) -> Unit,
    onDeletePost: (String) -> Unit
) {
    ActivityFeedSurface {
        FeedItemCard(
            item = item,
            contentColor = contentColor,
            accentColor = accentColor,
            isOwner = isOwner,
            onOpenItem = onOpenItem,
            onVotePoll = onVotePoll,
            onDeletePost = onDeletePost
        )
    }
}

private data class ActivityGridItem(
    val item: FeedItem,
    val thumbnailUrl: String?,
    val mediaCount: Int,
    val isVideo: Boolean,
    val defaultVideoId: String?
)

private fun activityGridItemFor(item: FeedItem): ActivityGridItem? {
    val imageUrls = item.images.orEmpty().filter { it.isNotBlank() }
    val mediaUrls = item.mediaUrls.orEmpty().filter { it.isNotBlank() }
    val mediaItems = when {
        imageUrls.isNotEmpty() -> imageUrls
        mediaUrls.isNotEmpty() -> mediaUrls
        else -> emptyList()
    }
    val hasDefaultVideo = findDefaultPostVideo(item.defaultVideoId) != null
    val isVideo =
        item.contentType == "short_video" ||
            item.postType?.equals("VIDEO", ignoreCase = true) == true ||
            item.entityType?.equals("reel", ignoreCase = true) == true ||
            !item.videoUrl.isNullOrBlank() ||
            !item.videoThumbnail.isNullOrBlank() ||
            hasDefaultVideo
    val thumbnailUrl = when {
        !item.videoThumbnail.isNullOrBlank() -> item.videoThumbnail
        mediaItems.isNotEmpty() -> mediaItems.first()
        !item.celebrationGifUrl.isNullOrBlank() -> item.celebrationGifUrl
        else -> null
    }

    return ActivityGridItem(
        item = item,
        thumbnailUrl = thumbnailUrl,
        mediaCount = mediaItems.size.coerceAtLeast(if (isVideo || thumbnailUrl != null) 1 else 0),
        isVideo = isVideo,
        defaultVideoId = item.defaultVideoId?.takeIf { hasDefaultVideo }
    )
}

@Composable
private fun ActivityFeedGridTile(
    gridItem: ActivityGridItem,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    onOpenItem: (FeedItem) -> Unit,
    onDeletePost: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val item = gridItem.item

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(contentColor.copy(alpha = 0.06f))
            .clickable { onOpenItem(item) }
    ) {
        if (gridItem.thumbnailUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(gridItem.thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = if (gridItem.isVideo) "Video thumbnail" else "Post thumbnail",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else if (gridItem.defaultVideoId != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                DefaultPostVideoPlayer(
                    defaultVideoId = gridItem.defaultVideoId,
                    modifier = Modifier.fillMaxSize(),
                    reduceAnimations = true,
                    accentColor = accentColor,
                    height = null,
                    contentScale = ContentScale.Crop
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(accentColor.copy(alpha = 0.14f))
                    .padding(10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                BasicText(
                    text = when (item.contentType) {
                        "article" -> "ARTICLE"
                        "short_video" -> "REEL"
                        else -> "POST"
                    },
                    style = TextStyle(accentColor, 10.sp, FontWeight.Bold, letterSpacing = 0.7.sp)
                )
                BasicText(
                    text = item.title?.takeIf { it.isNotBlank() } ?: item.content,
                    style = TextStyle(contentColor, 13.sp, FontWeight.SemiBold, lineHeight = 16.sp),
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.20f),
                        0.45f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.18f)
                    )
                )
        )

        if (gridItem.mediaCount > 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_vx_layers),
                    contentDescription = "Multiple media",
                    modifier = Modifier.size(13.dp),
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }
        }

    }
}

@Composable
fun ActivityFeedLoadingSection(
    contentColor: Color,
    accentColor: Color
) {
    ActivityFeedSurface {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(ProfileCardShape)
                .background(contentColor.copy(alpha = 0.05f))
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = accentColor
            )
        }
    }
}

@Composable
private fun ActivityFeedSurface(
    content: @Composable () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        content()
    }
}

@Composable
private fun FeedItemCard(
    item: FeedItem,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    onOpenItem: (FeedItem) -> Unit,
    onVotePoll: (String, String) -> Unit,
    onDeletePost: (String) -> Unit
) {
    val canDelete = item.entityType == "post"
    val canOpen = item.entityType == "post" || item.entityType == "reel"
    val mediaItems = when {
        !item.images.isNullOrEmpty() -> item.images
        !item.mediaUrls.isNullOrEmpty() -> item.mediaUrls
        else -> emptyList()
    }
    val previewMediaItems = when {
        !item.videoThumbnail.isNullOrBlank() && mediaItems.isEmpty() -> listOf(item.videoThumbnail)
        !item.celebrationGifUrl.isNullOrBlank() && mediaItems.isEmpty() -> listOf(item.celebrationGifUrl)
        else -> mediaItems
    }
    val hasDefaultVideo = findDefaultPostVideo(item.defaultVideoId) != null
    var showMenu by remember { mutableStateOf(false) }

    val typeIconRes: Int? = when (item.contentType) {
        "post" -> R.drawable.ic_post
        "article" -> R.drawable.ic_article
        "short_video" -> R.drawable.ic_video
        else -> null
    }
    val typeIconText = when (item.contentType) {
        "forum_question" -> "?"
        "forum_answer" -> "A"
        else -> ""
    }

    Box(
        Modifier
            .fillMaxWidth()
            .clip(ProfileCardShape)
            .background(contentColor.copy(alpha = 0.05f))
            .clickable(enabled = canOpen) { onOpenItem(item) }
            .padding(12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (typeIconRes != null) {
                    Image(
                        painter = painterResource(typeIconRes),
                        contentDescription = item.contentType,
                        modifier = Modifier.size(16.dp),
                        colorFilter = ColorFilter.tint(contentColor)
                    )
                } else {
                    BasicText(typeIconText, style = TextStyle(contentColor, 14.sp, FontWeight.Bold))
                }
                Spacer(Modifier.width(8.dp))
                item.title?.let { title ->
                    BasicText(
                        title,
                        style = TextStyle(contentColor, 14.sp, FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                } ?: run {
                    BasicText(
                        item.content.take(50) + if (item.content.length > 50) "..." else "",
                        style = TextStyle(contentColor, 14.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (isOwner && canDelete) {
                    Box {
                        BasicText(
                            "⋯",
                            style = TextStyle(contentColor.copy(alpha = 0.7f), 18.sp, FontWeight.SemiBold),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { showMenu = true }
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { BasicText("Delete post", style = TextStyle(Color(0xFFDC2626), 13.sp)) },
                                onClick = {
                                    showMenu = false
                                    onDeletePost(item.id)
                                }
                            )
                        }
                    }
                }
            }

            if (item.title != null && item.content.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                BasicText(
                    item.content.take(100) + if (item.content.length > 100) "..." else "",
                    style = TextStyle(contentColor.copy(alpha = 0.7f), 12.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (item.postType?.equals("CELEBRATION", ignoreCase = true) == true) {
                Spacer(Modifier.height(8.dp))
                ActivityCelebrationPreview(
                    celebrationType = item.celebrationType,
                    celebrationBadge = item.celebrationBadge,
                    contentColor = contentColor,
                    accentColor = accentColor
                )
            }

            if (item.postType?.equals("POLL", ignoreCase = true) == true && item.pollOptions.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                ActivityPollPreview(
                    options = item.pollOptions,
                    endsAt = item.pollEndsAt,
                    userVotedOptionId = item.userVotedOptionId,
                    showResultsBeforeVote = item.showResultsBeforeVote,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onVote = { optionId -> onVotePoll(item.id, optionId) }
                )
            } else if (!item.linkUrl.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(contentColor.copy(alpha = 0.04f))
                        .border(1.dp, contentColor.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        BasicText(
                            text = item.linkTitle ?: item.linkUrl.orEmpty(),
                            style = TextStyle(contentColor, 13.sp, FontWeight.Medium),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        item.linkDomain?.let { domain ->
                            BasicText(
                                text = domain,
                                style = TextStyle(accentColor, 12.sp)
                            )
                        }
                        item.linkDescription?.takeIf { it.isNotBlank() }?.let { description ->
                            BasicText(
                                text = description,
                                style = TextStyle(contentColor.copy(alpha = 0.65f), 12.sp),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Display media preview (images/videos)
            if (previewMediaItems.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                val isVideo =
                    item.contentType == "short_video" ||
                    item.postType?.equals("VIDEO", ignoreCase = true) == true ||
                    item.entityType == "reel"
                val imageCount = previewMediaItems.size

                if (imageCount == 1) {
                    // Single image/video
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(previewMediaItems.first())
                                .crossfade(true)
                                .build(),
                            contentDescription = if (isVideo) "Video thumbnail" else "Post image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Video play icon overlay
                        if (isVideo) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.9f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    BasicText(
                                        "▶",
                                        style = TextStyle(
                                            color = Color.Black,
                                            fontSize = 20.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Multiple images - show grid preview
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Show first 2-3 images
                        previewMediaItems.take(3).forEachIndexed { index, imageUrl ->
                            val isLastVisible = index == 2 || index == previewMediaItems.lastIndex
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(imageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Post image ${index + 1}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )

                                // Show count overlay on last image if more images
                                if (isLastVisible && imageCount > 3) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.6f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        BasicText(
                                            "+${imageCount - 3}",
                                            style = TextStyle(
                                                color = Color.White,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (hasDefaultVideo) {
                Spacer(Modifier.height(8.dp))
                DefaultPostVideoPlayer(
                    defaultVideoId = item.defaultVideoId,
                    height = 150.dp,
                    accentColor = accentColor,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicText("❤️", style = TextStyle(fontSize = 12.sp))
                    Spacer(Modifier.width(4.dp))
                    BasicText("${item.likesCount}", style = TextStyle(contentColor.copy(alpha = 0.6f), 11.sp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicText("💬", style = TextStyle(fontSize = 12.sp))
                    Spacer(Modifier.width(4.dp))
                    BasicText("${item.commentsCount}", style = TextStyle(contentColor.copy(alpha = 0.6f), 11.sp))
                }
                if (item.viewsCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BasicText("👁", style = TextStyle(fontSize = 12.sp))
                        Spacer(Modifier.width(4.dp))
                        BasicText("${item.viewsCount}", style = TextStyle(contentColor.copy(alpha = 0.6f), 11.sp))
                    }
                }
                Spacer(Modifier.weight(1f))
                BasicText(
                    formatDate(item.createdAt),
                    style = TextStyle(contentColor.copy(alpha = 0.5f), 10.sp)
                )
            }
        }
    }
}

@Composable
private fun ActivityCelebrationPreview(
    celebrationType: String?,
    celebrationBadge: String?,
    contentColor: Color,
    accentColor: Color
) {
    val label = when (celebrationType?.uppercase(Locale.US)) {
        "NEW_JOB" -> "New job"
        "PROMOTION" -> "Promotion"
        "GRADUATION" -> "Graduation"
        "CERTIFICATION" -> "Certification"
        "WORK_ANNIVERSARY" -> "Work anniversary"
        "BIRTHDAY" -> "Birthday"
        else ->
            celebrationType?.replace('_', ' ')?.lowercase(Locale.US)?.split(' ')?.joinToString(" ") { w ->
                w.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase(Locale.US) else c.toString() }
            }?.takeIf { it.isNotBlank() } ?: "Celebration"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(accentColor.copy(alpha = 0.12f))
            .border(1.dp, accentColor.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BasicText("🎉", style = TextStyle(fontSize = 18.sp))
            BasicText(
                label,
                style = TextStyle(contentColor, 14.sp, FontWeight.SemiBold)
            )
        }
        celebrationBadge?.takeIf { it.isNotBlank() }?.let { badge ->
            BasicText(
                badge,
                style = TextStyle(contentColor.copy(alpha = 0.78f), 12.sp)
            )
        }
    }
}

@Composable
private fun ActivityPollPreview(
    options: List<PollOption>,
    endsAt: String?,
    userVotedOptionId: String?,
    showResultsBeforeVote: Boolean,
    contentColor: Color,
    accentColor: Color,
    onVote: (String) -> Unit
) {
    val hasVoted = userVotedOptionId != null
    val showResults = hasVoted || showResultsBeforeVote
    val isPollEnded = isProfilePollExpired(endsAt)
    val totalVotes = options.sumOf { it.votes }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.take(4).forEach { option ->
            val isSelected = option.id == userVotedOptionId
            val percentage = if (showResults) option.percentage else 0.0

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(contentColor.copy(alpha = 0.05f))
                    .clickable(enabled = !hasVoted && !isPollEnded) { onVote(option.id) }
            ) {
                if (showResults) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((percentage / 100.0).toFloat().coerceIn(0f, 1f))
                            .height(42.dp)
                            .background(
                                if (isSelected) accentColor.copy(alpha = 0.22f)
                                else contentColor.copy(alpha = 0.06f)
                            )
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        text = option.text,
                        style = TextStyle(
                            color = if (isSelected) accentColor else contentColor,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (showResults) {
                        BasicText(
                            text = "${percentage.toInt()}%",
                            style = TextStyle(contentColor.copy(alpha = 0.6f), 11.sp, FontWeight.Medium)
                        )
                    }
                }
            }
        }

        BasicText(
            text = if (isPollEnded) {
                "$totalVotes vote${if (totalVotes == 1) "" else "s"} • Poll ended"
            } else {
                "$totalVotes vote${if (totalVotes == 1) "" else "s"}"
            },
            style = TextStyle(contentColor.copy(alpha = 0.55f), 11.sp)
        )
    }
}

private fun isProfilePollExpired(endsAt: String?): Boolean {
    if (endsAt.isNullOrBlank()) return false

    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX"
    )

    for (pattern in formats) {
        val parser = SimpleDateFormat(pattern, Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val parsed = runCatching { parser.parse(endsAt) }.getOrNull()
        if (parsed != null) {
            return parsed.time < System.currentTimeMillis()
        }
    }

    return false
}

// ==================== Helper Components ====================

@Composable
private fun EmptySectionPlaceholder(
    icon: String,
    message: String,
    contentColor: Color,
    iconRes: Int? = null  // Optional drawable resource
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (iconRes != null) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = message,
                    modifier = Modifier.size(32.dp),
                    colorFilter = ColorFilter.tint(contentColor.copy(alpha = 0.5f))
                )
            } else {
                BasicText(icon, style = TextStyle(fontSize = 32.sp))
            }
            Spacer(Modifier.height(8.dp))
            BasicText(
                message,
                style = TextStyle(contentColor.copy(alpha = 0.5f), 13.sp)
            )
        }
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val date = LocalDate.parse(dateString.take(10))
        date.format(DateTimeFormatter.ofPattern("MMM yyyy"))
    } catch (e: Exception) {
        dateString.take(10)
    }
}

private fun formatActivityHeatmapDate(dateString: String): String {
    return try {
        val date = LocalDate.parse(dateString.take(10))
        date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    } catch (e: Exception) {
        dateString.take(10)
    }
}

private fun formatGitHubSyncDate(dateString: String): String {
    return try {
        val normalized = dateString.replace("Z", "+00:00")
        val instant = java.time.OffsetDateTime.parse(normalized)
        instant.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    } catch (e: Exception) {
        formatDate(dateString)
    }
}

private fun formatGitHubContributionDate(dateString: String): String {
    return try {
        val date = LocalDate.parse(dateString.take(10))
        date.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy"))
    } catch (e: Exception) {
        dateString.take(10)
    }
}

private fun formatGitHubRepoDate(dateString: String): String {
    return try {
        val normalized = dateString.replace("Z", "+00:00")
        val instant = java.time.OffsetDateTime.parse(normalized)
        instant.format(DateTimeFormatter.ofPattern("MMM d"))
    } catch (e: Exception) {
        formatDate(dateString)
    }
}
