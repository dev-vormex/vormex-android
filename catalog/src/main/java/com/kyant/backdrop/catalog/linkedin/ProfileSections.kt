package com.kyant.backdrop.catalog.linkedin

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
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
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
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
    content: @Composable () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { profileCardBackdropShape() },
                effects = {
                    vibrancy()
                    blur(12f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.08f))
                }
            )
            .padding(16.dp)
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicText(
                        title,
                        style = TextStyle(contentColor, 18.sp, FontWeight.Bold)
                    )
                    count?.let {
                        Spacer(Modifier.width(6.dp))
                        BasicText(
                            "($it)",
                            style = TextStyle(contentColor.copy(alpha = 0.5f), 14.sp)
                        )
                    }
                }
                
                if (isOwner && onAdd != null) {
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.2f))
                            .clickable(onClick = onAdd)
                            .padding(8.dp)
                    ) {
                        BasicText("+", style = TextStyle(accentColor, 16.sp, FontWeight.Bold))
                    }
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            content()
        }
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
    onEditBio: () -> Unit,
    onSaveBio: () -> Unit,
    onCancelEditBio: () -> Unit,
    onBioChange: (String) -> Unit,
    onToggleOpenToWork: (Boolean) -> Unit
) {
    var showFullBio by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val aiGateway = remember { VormexAiGateway(context.applicationContext) }
    val aiScope = rememberCoroutineScope()
    var aiStatus by remember { mutableStateOf<String?>(null) }
    var aiBusyLabel by remember { mutableStateOf<String?>(null) }

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
        onAdd = if (user.bio.isNullOrEmpty() && isOwner) onEditBio else null
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Bio
            if (isEditingBio) {
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
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(accentColor)
                                .clickable(onClick = onSaveBio)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            BasicText("Save", style = TextStyle(Color.White, 14.sp, FontWeight.SemiBold))
                        }
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(contentColor.copy(alpha = 0.1f))
                                .clickable(onClick = onCancelEditBio)
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
                    
                    if (isOwner) {
                        BasicText(
                            "Edit",
                            style = TextStyle(accentColor, 13.sp, FontWeight.Medium),
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clickable(onClick = onEditBio)
                        )
                    }
                }
            }
            
            // Interests
            if (user.interests.isNotEmpty()) {
                Column {
                    BasicText(
                        "Interests",
                        style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp, FontWeight.Medium)
                    )
                    Spacer(Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        user.interests.forEach { interest ->
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(accentColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    BasicText(
                                        interest,
                                        style = TextStyle(accentColor, 12.sp)
                                    )
                                    if (isOwner) {
                                        Spacer(Modifier.width(4.dp))
                                        BasicText(
                                            "×",
                                            style = TextStyle(accentColor.copy(alpha = 0.6f), 14.sp),
                                            modifier = Modifier.clickable { /* Remove interest */ }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            val eduDetails = listOfNotNull(
                user.degree,
                user.branch?.takeIf { it.isNotEmpty() },
                user.currentYear?.let { "Year $it" },
                user.graduationYear?.let { "Class of $it" }
            ).joinToString(" • ")
            if (eduDetails.isNotEmpty()) {
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
            if (isOwner) {
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
    isVisible: Boolean
) {
    val context = LocalContext.current
    
    SectionCard(
        title = "GitHub",
        backdrop = backdrop,
        contentColor = contentColor,
        accentColor = accentColor
    ) {
        val stats = github.stats
        val contributionCalendar = github.contributionCalendar
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
                                    .clickable { /* TODO: Sync */ }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                BasicText("🔄 Sync", style = TextStyle(accentColor, 12.sp))
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

                if (contributionCalendar != null) {
                    GitHubSignalGraph(
                        contributionCalendar = contributionCalendar,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isVisible = isVisible
                    )
                    GitHubContributionGraph(
                        contributionCalendar = contributionCalendar,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isVisible = isVisible
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

                stats?.let { githubStats ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GitHubMetricCard(
                                    modifier = Modifier.weight(1f),
                                    iconRes = R.drawable.ic_code,
                                    value = githubStats.totalPublicRepos.toString(),
                                    label = "Repos",
                                    tint = Color(0xFF60A5FA),
                                    progress = githubMetricProgress(githubStats.totalPublicRepos, 80),
                                    contentColor = contentColor,
                                    isVisible = isVisible,
                                    delayMillis = 260
                                )
                                GitHubMetricCard(
                                    modifier = Modifier.weight(1f),
                                    iconRes = R.drawable.ic_favorite,
                                    value = githubStats.totalStars.toString(),
                                    label = "Stars",
                                    tint = Color(0xFFFBBF24),
                                    progress = githubMetricProgress(githubStats.totalStars, 1500),
                                    contentColor = contentColor,
                                    isVisible = isVisible,
                                    delayMillis = 560
                                )
                            }
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GitHubMetricCard(
                                    modifier = Modifier.weight(1f),
                                    iconRes = R.drawable.ic_share,
                                    value = githubStats.totalForks.toString(),
                                    label = "Forks",
                                    tint = Color(0xFFC084FC),
                                    progress = githubMetricProgress(githubStats.totalForks, 400),
                                    contentColor = contentColor,
                                    isVisible = isVisible,
                                    delayMillis = 860
                                )
                                GitHubMetricCard(
                                    modifier = Modifier.weight(1f),
                                    iconRes = R.drawable.ic_users,
                                    value = githubStats.followers.toString(),
                                    label = "Followers",
                                    tint = Color(0xFF34D399),
                                    progress = githubMetricProgress(githubStats.followers, 1000),
                                    contentColor = contentColor,
                                    isVisible = isVisible,
                                    delayMillis = 1160
                                )
                            }
                        }

                        if (githubStats.topLanguages.isNotEmpty()) {
                            GitHubLanguageOrbit(
                                languages = githubStats.topLanguages.entries
                                    .sortedByDescending { it.value.percentage }
                                    .take(5),
                                contentColor = contentColor,
                                accentColor = accentColor,
                                isVisible = isVisible
                            )
                        }

                        if (githubStats.topRepos.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                BasicText(
                                    "Top Repositories",
                                    style = TextStyle(contentColor.copy(alpha = 0.68f), 12.sp, FontWeight.Medium)
                                )
                                githubStats.topRepos.take(3).forEach { repo ->
                                    GitHubRepositoryCard(
                                        repo = repo,
                                        contentColor = contentColor,
                                        onOpen = {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(repo.url)))
                                        }
                                    )
                                }
                            }
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
                            .clickable { /* TODO: Connect GitHub */ }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        BasicText(
                            "Connect GitHub",
                            style = TextStyle(Color.White, 14.sp, FontWeight.SemiBold)
                        )
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
    accentColor: Color,
    isVisible: Boolean
) {
    val graphRevealProgress by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = GitHubContributionRevealDurationMs,
            delayMillis = GitHubContributionRevealDelayMs,
            easing = LinearEasing
        ),
        label = "githubGraphReveal"
    )
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
            .graphicsLayer {
                alpha = 0.68f + (0.32f * graphRevealProgress)
                translationY = (1f - graphRevealProgress) * 18f
            }
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
                                                .graphicsLayer {
                                                    val activeScale = if (day.contributionCount > 0) {
                                                        0.82f + (0.18f * graphRevealProgress)
                                                    } else {
                                                        1f
                                                    }
                                                    scaleX = activeScale
                                                    scaleY = activeScale
                                                }
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
    val lineColors = listOf(
        Color(0xFF42D9FF),
        Color(0xFFFF5DA2),
        Color(0xFFFFB444)
    )

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
                val gridColor = Color.White.copy(alpha = 0.08f)

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
                        color = gridColor.copy(alpha = 0.74f),
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
                            color = Color.White.copy(alpha = 0.88f),
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
            .background(Color.White.copy(alpha = 0.07f))
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
    
    // Group days by week
    val weeks = days.chunked(7)
    
    Row(
        modifier = Modifier.horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        weeks.forEach { week ->
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                week.forEach { day ->
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(getHeatmapColor(day.level, accentColor))
                    )
                }
            }
        }
    }
}

private fun getHeatmapColor(level: Int, accentColor: Color): Color {
    return when (level) {
        0 -> Color(0xFF1E1E1E)
        1 -> accentColor.copy(alpha = 0.3f)
        2 -> accentColor.copy(alpha = 0.6f)
        3 -> accentColor
        else -> Color(0xFF1E1E1E)
    }
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
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                skills.forEach { userSkill ->
                    SkillChip(
                        skill = userSkill,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isOwner = isOwner,
                        onRemove = { onRemoveSkill(userSkill) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SkillChip(
    skill: UserSkill,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    onRemove: () -> Unit = {}
) {
    val proficiencyColor = when (skill.proficiency?.lowercase()) {
        "expert" -> Color(0xFFFFD700)
        "advanced" -> Color(0xFF22C55E)
        "intermediate" -> Color(0xFF3B82F6)
        else -> contentColor.copy(alpha = 0.6f)
    }
    
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(proficiencyColor.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(
                skill.skill.name,
                style = TextStyle(contentColor, 13.sp)
            )
            skill.proficiency?.let { prof ->
                Spacer(Modifier.width(6.dp))
                BasicText(
                    "• $prof",
                    style = TextStyle(proficiencyColor, 11.sp)
                )
            }
            if (isOwner) {
                Spacer(Modifier.width(6.dp))
                BasicText(
                    "×",
                    style = TextStyle(contentColor.copy(alpha = 0.4f), 14.sp),
                    modifier = Modifier.clickable { onRemove() }
                )
            }
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
                            tint = Color(0xFFFFD66B),
                            background = Color(0xFFFFD66B).copy(alpha = 0.16f)
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
                        accentColor = accentColor,
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
    accentColor: Color,
    isOwner: Boolean,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onToggleFeatured: () -> Unit
) {
    val context = LocalContext.current
    val heroAccent = if (project.featured) Color(0xFFFFD66B) else accentColor
    val cardBorder = if (project.featured) heroAccent.copy(alpha = 0.34f) else Color.White.copy(alpha = 0.08f)
    val subduedSurface = Color.White.copy(alpha = 0.08f)
    val hasLinks = !project.projectUrl.isNullOrBlank() || !project.githubUrl.isNullOrBlank() || !project.otherLinks.isNullOrEmpty()
    
    Box(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { profileCardBackdropShape() },
                effects = {
                    vibrancy()
                    blur(16f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.08f))
                }
            )
            .border(1.dp, cardBorder, ProfileCardShape)
            .clip(ProfileCardShape)
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
                        borderColor = Color.White.copy(alpha = 0.08f),
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
                            borderColor = Color.White.copy(alpha = 0.08f),
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

// ==================== Experience Section ====================

@Composable
fun ExperienceSection(
    experiences: List<Experience>,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    onAddExperience: () -> Unit = {},
    onEditExperience: (Experience) -> Unit = {},
    onViewExperience: (Experience) -> Unit = {}
) {
    SectionCard(
        title = "Experience",
        count = if (experiences.isNotEmpty()) experiences.size else null,
        backdrop = backdrop,
        contentColor = contentColor,
        accentColor = accentColor,
        isOwner = isOwner,
        onAdd = onAddExperience
    ) {
        if (experiences.isEmpty()) {
            // Empty state for owner
            if (isOwner) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_work),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        colorFilter = ColorFilter.tint(contentColor.copy(alpha = 0.3f))
                    )
                    Spacer(Modifier.height(8.dp))
                    BasicText(
                        "No experience yet",
                        style = TextStyle(contentColor.copy(alpha = 0.6f), 14.sp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(accentColor)
                            .clickable { onAddExperience() }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        BasicText(
                            "Add your first experience",
                            style = TextStyle(Color.White, 14.sp, FontWeight.Medium)
                        )
                    }
                }
            } else {
                EmptySectionPlaceholder(
                    icon = "",
                    message = "No experience yet",
                    contentColor = contentColor,
                    iconRes = R.drawable.ic_work
                )
            }
        } else {
            Column {
                experiences.forEachIndexed { index, exp ->
                    ExperienceItem(
                        experience = exp,
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isOwner = isOwner,
                        isLast = index == experiences.lastIndex,
                        onEdit = { onEditExperience(exp) },
                        onView = { onViewExperience(exp) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExperienceItem(
    experience: Experience,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    isLast: Boolean,
    onEdit: () -> Unit,
    onView: () -> Unit
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onView)
    ) {
        // Timeline dot and line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(24.dp)
        ) {
            Box(
                Modifier
                    .size(if (experience.isCurrent) 14.dp else 12.dp)
                    .clip(CircleShape)
                    .background(
                        if (experience.isCurrent) accentColor 
                        else contentColor.copy(alpha = 0.3f)
                    )
                    .then(
                        if (experience.isCurrent) Modifier.border(
                            2.dp,
                            accentColor.copy(alpha = 0.3f),
                            CircleShape
                        ) else Modifier
                    )
            )
            if (!isLast) {
                Box(
                    Modifier
                        .width(2.dp)
                        .height(100.dp)
                        .background(contentColor.copy(alpha = 0.15f))
                )
            }
        }
        
        Spacer(Modifier.width(12.dp))
        
        // Logo or placeholder
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(contentColor.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            if (experience.logo != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(experience.logo)
                        .crossfade(true)
                        .build(),
                    contentDescription = experience.company,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.ic_work),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(contentColor.copy(alpha = 0.4f))
                )
            }
        }
        
        Spacer(Modifier.width(12.dp))
        
        // Content
        Column(
            Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    // Title and type badge
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BasicText(
                            experience.title,
                            style = TextStyle(contentColor, 15.sp, FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(6.dp))
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(getExperienceTypeColor(experience.type).copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            BasicText(
                                experience.type,
                                style = TextStyle(getExperienceTypeColor(experience.type), 10.sp, FontWeight.Medium)
                            )
                        }
                    }
                    
                    // Company
                    BasicText(
                        experience.company,
                        style = TextStyle(contentColor.copy(alpha = 0.8f), 13.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Date and location
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val dateText = buildString {
                            append(formatDate(experience.startDate))
                            append(" — ")
                            if (experience.isCurrent) {
                                append("Present")
                            } else {
                                experience.endDate?.let { append(formatDate(it)) }
                            }
                            // Add duration
                            val duration = calculateDuration(experience.startDate, experience.endDate, experience.isCurrent)
                            if (duration.isNotEmpty()) {
                                append(" · $duration")
                            }
                        }
                        BasicText(
                            dateText,
                            style = TextStyle(contentColor.copy(alpha = 0.5f), 11.sp)
                        )
                    }
                    
                    experience.location?.let { loc ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(R.drawable.ic_location),
                                contentDescription = "Location",
                                modifier = Modifier.size(11.dp),
                                colorFilter = ColorFilter.tint(contentColor.copy(alpha = 0.4f))
                            )
                            Spacer(Modifier.width(2.dp))
                            BasicText(
                                loc,
                                style = TextStyle(contentColor.copy(alpha = 0.5f), 11.sp)
                            )
                        }
                    }
                }
                
                // Edit button for owner
                if (isOwner) {
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(contentColor.copy(alpha = 0.08f))
                            .clickable(onClick = onEdit)
                            .padding(6.dp)
                    ) {
                        BasicText("✎", style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp))
                    }
                }
            }
            
            // Description with expand/collapse
            experience.description?.let { desc ->
                if (desc.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    BasicText(
                        desc,
                        style = TextStyle(contentColor.copy(alpha = 0.7f), 12.sp),
                        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (desc.length > 100) {
                        BasicText(
                            if (isExpanded) "See less" else "Read more",
                            style = TextStyle(accentColor, 11.sp, FontWeight.Medium),
                            modifier = Modifier
                                .clickable { isExpanded = !isExpanded }
                                .padding(top = 2.dp)
                        )
                    }
                }
            }
            
            // Skills
            if (experience.skills.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    experience.skills.take(5).forEach { skill ->
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(accentColor.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            BasicText(
                                skill,
                                style = TextStyle(accentColor, 10.sp)
                            )
                        }
                    }
                    if (experience.skills.size > 5) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(contentColor.copy(alpha = 0.08f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            BasicText(
                                "+${experience.skills.size - 5}",
                                style = TextStyle(contentColor.copy(alpha = 0.5f), 10.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun calculateDuration(startDate: String, endDate: String?, isCurrent: Boolean): String {
    return try {
        val formatter = java.time.format.DateTimeFormatter.ISO_DATE
        val start = java.time.LocalDate.parse(startDate.take(10), formatter)
        val end = if (isCurrent) java.time.LocalDate.now() 
                  else endDate?.let { java.time.LocalDate.parse(it.take(10), formatter) } 
                  ?: return ""
        
        val period = java.time.Period.between(start, end)
        val years = period.years
        val months = period.months
        
        buildString {
            if (years > 0) {
                append("$years yr")
                if (years > 1) append("s")
            }
            if (months > 0) {
                if (years > 0) append(" ")
                append("$months mo")
                if (months > 1) append("s")
            }
            if (years == 0 && months == 0) {
                append("< 1 mo")
            }
        }
    } catch (e: Exception) {
        ""
    }
}

private fun getExperienceTypeColor(type: String): Color {
    return when (type.lowercase()) {
        "full-time" -> Color(0xFF22C55E)
        "internship" -> Color(0xFF3B82F6)
        "part-time" -> Color(0xFFF59E0B)
        "freelance" -> Color(0xFFA855F7)
        "contract" -> Color(0xFFEC4899)
        else -> Color(0xFF6B7280)
    }
}

// ==================== Education Section ====================

@Composable
fun EducationSection(
    education: List<Education>,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    onAddEducation: () -> Unit = {},
    onEditEducation: (Education) -> Unit = {},
    onViewEducation: (Education) -> Unit = {}
) {
    SectionCard(
        title = "Education",
        count = if (education.isNotEmpty()) education.size else null,
        backdrop = backdrop,
        contentColor = contentColor,
        accentColor = accentColor,
        isOwner = isOwner,
        onAdd = onAddEducation
    ) {
        if (education.isEmpty()) {
            // Empty state for owner
            if (isOwner) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_education),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        colorFilter = ColorFilter.tint(contentColor.copy(alpha = 0.3f))
                    )
                    Spacer(Modifier.height(8.dp))
                    BasicText(
                        "No education yet",
                        style = TextStyle(contentColor.copy(alpha = 0.6f), 14.sp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(accentColor)
                            .clickable { onAddEducation() }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        BasicText(
                            "Add your first education",
                            style = TextStyle(Color.White, 14.sp, FontWeight.Medium)
                        )
                    }
                }
            } else {
                EmptySectionPlaceholder(
                    icon = "",
                    message = "No education yet",
                    contentColor = contentColor,
                    iconRes = R.drawable.ic_education
                )
            }
        } else {
            Column {
                education.forEachIndexed { index, edu ->
                    EducationItem(
                        education = edu,
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isOwner = isOwner,
                        isLast = index == education.lastIndex,
                        onEdit = { onEditEducation(edu) },
                        onView = { onViewEducation(edu) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EducationItem(
    education: Education,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    isLast: Boolean,
    onEdit: () -> Unit,
    onView: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val hasLongDescription = (education.description?.length ?: 0) > 100 || education.description?.contains("\n") == true
    val hasLongActivities = (education.activities?.length ?: 0) > 100 || education.activities?.contains("\n") == true
    
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onView)
    ) {
        // Timeline dot and line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(24.dp)
        ) {
            Box(
                Modifier
                    .size(if (education.isCurrent) 14.dp else 12.dp)
                    .clip(CircleShape)
                    .background(
                        if (education.isCurrent) Color(0xFFFFD700)
                        else contentColor.copy(alpha = 0.3f)
                    )
                    .then(
                        if (education.isCurrent) Modifier.border(
                            2.dp,
                            Color(0xFFFFD700).copy(alpha = 0.3f),
                            CircleShape
                        ) else Modifier
                    )
            )
            if (!isLast) {
                Box(
                    Modifier
                        .width(2.dp)
                        .height(100.dp)
                        .background(contentColor.copy(alpha = 0.15f))
                )
            }
        }
        
        Spacer(Modifier.width(12.dp))
        
        // Icon placeholder (no logo for education)
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(contentColor.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_education),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(
                    if (education.isCurrent) Color(0xFFFFD700) else contentColor.copy(alpha = 0.4f)
                )
            )
        }
        
        Spacer(Modifier.width(12.dp))
        
        // Content
        Column(
            Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    // School name
                    BasicText(
                        education.school,
                        style = TextStyle(contentColor, 15.sp, FontWeight.SemiBold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Degree · Field of Study
                    BasicText(
                        "${education.degree} · ${education.fieldOfStudy}",
                        style = TextStyle(contentColor.copy(alpha = 0.8f), 13.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Date range and grade
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val dateText = buildString {
                            append(formatDate(education.startDate))
                            append(" — ")
                            if (education.isCurrent) {
                                append("Present")
                            } else {
                                education.endDate?.let { append(formatDate(it)) }
                            }
                        }
                        BasicText(
                            dateText,
                            style = TextStyle(contentColor.copy(alpha = 0.5f), 11.sp)
                        )
                        
                        education.grade?.let { grade ->
                            if (grade.isNotBlank()) {
                                Spacer(Modifier.width(8.dp))
                                BasicText(
                                    "• Grade: $grade",
                                    style = TextStyle(contentColor.copy(alpha = 0.5f), 11.sp)
                                )
                            }
                        }
                    }
                }
                
                // Edit button for owner
                if (isOwner) {
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(contentColor.copy(alpha = 0.08f))
                            .clickable(onClick = onEdit)
                            .padding(6.dp)
                    ) {
                        BasicText("✎", style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp))
                    }
                }
            }
            
            // Description with expand/collapse
            education.description?.let { desc ->
                if (desc.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    BasicText(
                        desc,
                        style = TextStyle(contentColor.copy(alpha = 0.7f), 12.sp),
                        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (hasLongDescription) {
                        BasicText(
                            if (isExpanded) "See less" else "Read more",
                            style = TextStyle(accentColor, 11.sp, FontWeight.Medium),
                            modifier = Modifier
                                .clickable { isExpanded = !isExpanded }
                                .padding(top = 2.dp)
                        )
                    }
                }
            }
            
            // Activities & Societies
            education.activities?.let { activities ->
                if (activities.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    BasicText(
                        "Activities & Societies",
                        style = TextStyle(contentColor.copy(alpha = 0.5f), 10.sp, FontWeight.Medium)
                    )
                    BasicText(
                        activities,
                        style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp),
                        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (hasLongActivities && !hasLongDescription) {
                        BasicText(
                            if (isExpanded) "See less" else "Read more",
                            style = TextStyle(accentColor, 11.sp, FontWeight.Medium),
                            modifier = Modifier
                                .clickable { isExpanded = !isExpanded }
                                .padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==================== Licenses & Certifications Section ====================

// Predefined color palette for certificate cards (client-side only)
internal val CERTIFICATE_COLORS = listOf(
    Color(0xFF6B7280) to "Neutral",    // Gray
    Color(0xFFEF4444) to "Red",
    Color(0xFFF97316) to "Orange",
    Color(0xFFF59E0B) to "Amber",
    Color(0xFF22C55E) to "Green",
    Color(0xFF3B82F6) to "Blue",
    Color(0xFF6366F1) to "Indigo",
    Color(0xFFA855F7) to "Purple",
    Color(0xFFEC4899) to "Pink"
)

internal fun getCertificateColor(colorHex: String?): Color {
    if (colorHex.isNullOrEmpty()) return CERTIFICATE_COLORS[0].first
    return try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        CERTIFICATE_COLORS[0].first
    }
}

private fun isExpired(expiryDate: String?): Boolean {
    if (expiryDate.isNullOrEmpty()) return false
    return try {
        val expiry = java.time.LocalDate.parse(expiryDate.take(10))
        expiry.isBefore(java.time.LocalDate.now())
    } catch (e: Exception) {
        false
    }
}

@Composable
fun CertificatesSection(
    certificates: List<Certificate>,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    onAddCertificate: () -> Unit = {},
    onEditCertificate: (Certificate) -> Unit = {},
    onViewCertificate: (Certificate) -> Unit = {}
) {
    SectionCard(
        title = "Licenses & Certifications",
        count = if (certificates.isNotEmpty()) certificates.size else null,
        backdrop = backdrop,
        contentColor = contentColor,
        accentColor = accentColor,
        isOwner = isOwner,
        onAdd = onAddCertificate
    ) {
        if (certificates.isEmpty()) {
            // Empty state for owner
            if (isOwner) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_award),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        colorFilter = ColorFilter.tint(contentColor.copy(alpha = 0.3f))
                    )
                    Spacer(Modifier.height(8.dp))
                    BasicText(
                        "No certifications yet",
                        style = TextStyle(contentColor.copy(alpha = 0.6f), 14.sp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(accentColor)
                            .clickable { onAddCertificate() }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        BasicText(
                            "Add your first certification",
                            style = TextStyle(Color.White, 14.sp, FontWeight.Medium)
                        )
                    }
                }
            } else {
                EmptySectionPlaceholder(
                    icon = "",
                    message = "No certifications yet",
                    contentColor = contentColor,
                    iconRes = R.drawable.ic_award
                )
            }
        } else {
            // Grid of certificate cards (1 column on small screens, 2 on larger)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                certificates.forEach { cert ->
                    CertificateCard(
                        certificate = cert,
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isOwner = isOwner,
                        onEdit = { onEditCertificate(cert) },
                        onView = { onViewCertificate(cert) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CertificateCard(
    certificate: Certificate,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    onEdit: () -> Unit,
    onView: () -> Unit
) {
    val context = LocalContext.current
    val cardColor = getCertificateColor(certificate.color)
    val expired = !certificate.doesNotExpire && isExpired(certificate.expiryDate)
    val hasImage = certificate.credentialUrl?.let { isImageUrl(it) } ?: false
    
    Box(
        Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { profileCardBackdropShape() },
                effects = {
                    vibrancy()
                    blur(16f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.06f))
                }
            )
            .clip(ProfileCardShape)
            .clickable(onClick = onView)
            .padding(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left: Certificate image thumbnail or award icon
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(cardColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (hasImage && certificate.credentialUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(certificate.credentialUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = certificate.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.ic_award),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(cardColor)
                    )
                }
            }
            
            // Right: Certificate info
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Name (title)
                BasicText(
                    certificate.name,
                    style = TextStyle(contentColor, 15.sp, FontWeight.SemiBold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Issuing organization (subtitle)
                BasicText(
                    certificate.issuingOrg,
                    style = TextStyle(contentColor.copy(alpha = 0.7f), 13.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Meta: issue date, expiry status
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Issue date
                    BasicText(
                        "Issued ${formatDate(certificate.issueDate)}",
                        style = TextStyle(contentColor.copy(alpha = 0.5f), 11.sp)
                    )
                    
                    // Expiry status
                    when {
                        expired -> {
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                BasicText(
                                    "Expired",
                                    style = TextStyle(Color(0xFFEF4444), 10.sp, FontWeight.Medium)
                                )
                            }
                        }
                        certificate.doesNotExpire -> {
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF22C55E).copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                BasicText(
                                    "No Expiration",
                                    style = TextStyle(Color(0xFF22C55E), 10.sp, FontWeight.Medium)
                                )
                            }
                        }
                        certificate.expiryDate != null -> {
                            BasicText(
                                "• Exp: ${formatDate(certificate.expiryDate)}",
                                style = TextStyle(contentColor.copy(alpha = 0.5f), 11.sp)
                            )
                        }
                    }
                }
                
                // Credential ID (if present)
                certificate.credentialId?.takeIf { it.isNotBlank() }?.let { credentialId ->
                    BasicText(
                        "ID: $credentialId",
                        style = TextStyle(contentColor.copy(alpha = 0.4f), 10.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Action buttons
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // View button (if credentialUrl present)
                certificate.credentialUrl?.let { url ->
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.2f))
                            .clickable(onClick = onView)
                            .padding(6.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_visibility),
                            contentDescription = "View credential",
                            modifier = Modifier.size(14.dp),
                            colorFilter = ColorFilter.tint(accentColor)
                        )
                    }
                }
                
                // Edit button (owner only)
                if (isOwner) {
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(contentColor.copy(alpha = 0.08f))
                            .clickable(onClick = onEdit)
                            .padding(6.dp)
                    ) {
                        BasicText("✎", style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp))
                    }
                }
            }
        }
    }
}

// ==================== Achievements Section ====================

@Composable
fun AchievementsSection(
    achievements: List<Achievement>,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    onAddAchievement: () -> Unit = {},
    onEditAchievement: (Achievement) -> Unit = {},
    onViewAchievement: (Achievement) -> Unit = {}
) {
    val context = LocalContext.current
    
    SectionCard(
        title = "Achievements",
        count = if (achievements.isNotEmpty()) achievements.size else null,
        backdrop = backdrop,
        contentColor = contentColor,
        accentColor = accentColor,
        isOwner = isOwner,
        onAdd = onAddAchievement
    ) {
        if (achievements.isEmpty()) {
            // Empty state for owner
            if (isOwner) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_trophy),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        colorFilter = ColorFilter.tint(contentColor.copy(alpha = 0.3f))
                    )
                    Spacer(Modifier.height(8.dp))
                    BasicText(
                        "No achievements yet",
                        style = TextStyle(contentColor.copy(alpha = 0.6f), 14.sp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(accentColor)
                            .clickable { onAddAchievement() }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        BasicText(
                            "Add your first achievement",
                            style = TextStyle(Color.White, 14.sp, FontWeight.Medium)
                        )
                    }
                }
            } else {
                EmptySectionPlaceholder(
                    icon = "",
                    message = "No achievements yet",
                    contentColor = contentColor,
                    iconRes = R.drawable.ic_trophy
                )
            }
        } else {
            // List of achievement cards
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                achievements.forEach { achievement ->
                    AchievementCard(
                        achievement = achievement,
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isOwner = isOwner,
                        onEdit = { onEditAchievement(achievement) },
                        onView = { onViewAchievement(achievement) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementCard(
    achievement: Achievement,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    onEdit: () -> Unit,
    onView: () -> Unit
) {
    val context = LocalContext.current
    val cardColor = getAchievementColor(achievement.color)
    val hasImage = achievement.certificateUrl?.let { isImageUrl(it) } ?: false
    
    // Get type icon
    val typeIcon = when (achievement.type) {
        "Hackathon" -> R.drawable.ic_target
        "Competition" -> R.drawable.ic_trophy
        "Award" -> R.drawable.ic_medal
        "Scholarship" -> R.drawable.ic_gift
        "Recognition" -> R.drawable.ic_sparkles
        else -> R.drawable.ic_trophy
    }
    
    Box(
        Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { profileCardBackdropShape() },
                effects = {
                    vibrancy()
                    blur(16f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.06f))
                }
            )
            .clip(ProfileCardShape)
            .clickable(onClick = onView)
            .padding(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left: Achievement image thumbnail or type icon
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(cardColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (hasImage && achievement.certificateUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(achievement.certificateUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = achievement.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(typeIcon),
                        contentDescription = achievement.type,
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(cardColor)
                    )
                }
            }
            
            // Right: Achievement info
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Title
                BasicText(
                    achievement.title,
                    style = TextStyle(contentColor, 15.sp, FontWeight.SemiBold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Organization
                BasicText(
                    achievement.organization,
                    style = TextStyle(contentColor.copy(alpha = 0.7f), 13.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Type badge + date
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Type badge
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(cardColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        BasicText(
                            achievement.type,
                            style = TextStyle(cardColor, 10.sp, FontWeight.Medium)
                        )
                    }
                    
                    // Date
                    BasicText(
                        "• ${formatDate(achievement.date)}",
                        style = TextStyle(contentColor.copy(alpha = 0.5f), 11.sp)
                    )
                }
                
                // Description (if present, 2-line clamp)
                achievement.description?.takeIf { it.isNotBlank() }?.let { desc ->
                    Spacer(Modifier.height(4.dp))
                    BasicText(
                        desc,
                        style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Action buttons
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // View button (if certificateUrl present)
                achievement.certificateUrl?.let { url ->
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.2f))
                            .clickable(onClick = onView)
                            .padding(6.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_visibility),
                            contentDescription = "View proof",
                            modifier = Modifier.size(14.dp),
                            colorFilter = ColorFilter.tint(accentColor)
                        )
                    }
                }
                
                // Edit button (owner only)
                if (isOwner) {
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(contentColor.copy(alpha = 0.08f))
                            .clickable(onClick = onEdit)
                            .padding(6.dp)
                    ) {
                        BasicText("✎", style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp))
                    }
                }
            }
        }
    }
}

// ==================== Activity Feed Section ====================

@Composable
fun ActivityFeedSection(
    feedItems: List<FeedItem>,
    currentFilter: String,
    isLoading: Boolean,
    hasMore: Boolean,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isLightTheme: Boolean,
    isOwner: Boolean,
    onFilterChange: (String) -> Unit,
    onLoadMore: () -> Unit,
    onOpenItem: (FeedItem) -> Unit,
    onVotePoll: (String, String) -> Unit,
    onDeletePost: (String) -> Unit
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
            // Filter tabs
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
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) accentColor.copy(alpha = 0.2f)
                                else Color.Transparent
                            )
                            .clickable { onFilterChange(filter) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        BasicText(
                            label,
                            style = TextStyle(
                                if (isSelected) accentColor else contentColor.copy(alpha = 0.6f),
                                13.sp,
                                if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Feed items
            if (feedItems.isEmpty() && !isLoading) {
                EmptySectionPlaceholder(
                    icon = "📭",
                    message = "No activity yet",
                    contentColor = contentColor
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    feedItems.forEach { item ->
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
                    
                    if (isLoading) {
                        Box(
                            Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = accentColor
                            )
                        }
                    }
                    
                    if (hasMore && !isLoading) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(contentColor.copy(alpha = 0.05f))
                                .clickable(onClick = onLoadMore)
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText(
                                "Load more",
                                style = TextStyle(accentColor, 13.sp, FontWeight.Medium)
                            )
                        }
                    }
                }
            }
        }
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
                            text = item.linkTitle ?: item.linkUrl ?: "Open link",
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
