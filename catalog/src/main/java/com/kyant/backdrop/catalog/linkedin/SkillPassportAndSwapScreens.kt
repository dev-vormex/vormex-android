package com.kyant.backdrop.catalog.linkedin

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.kyant.backdrop.catalog.ui.BasicText
import com.kyant.backdrop.catalog.ui.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.PersonAddAlt1
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.network.SkillsApiService
import com.kyant.backdrop.catalog.network.models.SkillPassportEvidence
import com.kyant.backdrop.catalog.network.models.SkillPassportResponse
import com.kyant.backdrop.catalog.network.models.SkillPassportSkill
import com.kyant.backdrop.catalog.network.models.SkillPerson
import com.kyant.backdrop.catalog.network.models.SkillSwapCreateRequest
import com.kyant.backdrop.catalog.network.models.SkillSwapRequestItem
import com.kyant.backdrop.catalog.network.models.SkillSwapSession
import com.kyant.backdrop.catalog.network.models.SkillSwapStateResponse
import com.kyant.backdrop.catalog.network.models.SkillSwapSuggestion
import com.kyant.backdrop.catalog.network.models.SkillSwapSuggestionsResponse
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class SkillPassportUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val passport: SkillPassportResponse? = null,
    val error: String? = null
)

class SkillPassportViewModel(private val context: Context) : ViewModel() {
    private val applicationContext = context.applicationContext
    private val _uiState = MutableStateFlow(SkillPassportUiState())
    val uiState: StateFlow<SkillPassportUiState> = _uiState.asStateFlow()

    fun load(userId: String = "me", forceRefresh: Boolean = false) {
        if (_uiState.value.isLoading && !forceRefresh) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = _uiState.value.passport == null,
                isRefreshing = _uiState.value.passport != null,
                error = null
            )
            SkillsApiService.getSkillPassport(applicationContext, userId)
                .onSuccess { passport ->
                    _uiState.value = SkillPassportUiState(passport = passport)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = error.message ?: "Could not load Skill Passport"
                    )
                }
        }
    }

    fun linkVerificationProfile(
        userId: String = "me",
        provider: String,
        username: String,
        profileUrl: String? = null
    ) {
        viewModelScope.launch {
            SkillsApiService.upsertVerificationLink(applicationContext, provider, username, profileUrl)
                .onSuccess {
                    Toast.makeText(applicationContext, "Skill verification linked", Toast.LENGTH_SHORT).show()
                    load(userId, forceRefresh = true)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(error = error.message ?: "Could not link verification profile")
                }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SkillPassportViewModel(context) as T
        }
    }
}

data class SkillSwapUiState(
    val isLoadingSuggestions: Boolean = false,
    val isLoadingState: Boolean = false,
    val suggestions: SkillSwapSuggestionsResponse = SkillSwapSuggestionsResponse(),
    val state: SkillSwapStateResponse = SkillSwapStateResponse(),
    val mode: String = "learn",
    val selectedSkill: String? = null,
    val busyIds: Set<String> = emptySet(),
    val error: String? = null,
    val toast: String? = null
)

class SkillSwapViewModel(private val context: Context) : ViewModel() {
    private val applicationContext = context.applicationContext
    private val _uiState = MutableStateFlow(SkillSwapUiState())
    val uiState: StateFlow<SkillSwapUiState> = _uiState.asStateFlow()

    init {
        refreshAll()
    }

    fun refreshAll() {
        loadSuggestions(forceRefresh = true)
        loadState(forceRefresh = true)
    }

    fun setMode(mode: String) {
        if (_uiState.value.mode == mode) return
        _uiState.value = _uiState.value.copy(mode = mode, selectedSkill = null)
        loadSuggestions(forceRefresh = true)
    }

    fun selectSkill(skill: String?) {
        _uiState.value = _uiState.value.copy(selectedSkill = skill)
        loadSuggestions(forceRefresh = true)
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(toast = null)
    }

    fun loadSuggestions(forceRefresh: Boolean = false) {
        if (_uiState.value.isLoadingSuggestions && !forceRefresh) return
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.value = state.copy(isLoadingSuggestions = true, error = null)
            SkillsApiService.getSkillSwapSuggestions(
                context = applicationContext,
                mode = state.mode,
                skill = state.selectedSkill
            ).onSuccess { suggestions ->
                _uiState.value = _uiState.value.copy(
                    isLoadingSuggestions = false,
                    suggestions = suggestions
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoadingSuggestions = false,
                    error = error.message ?: "Could not load skill matches"
                )
            }
        }
    }

    fun loadState(forceRefresh: Boolean = false) {
        if (_uiState.value.isLoadingState && !forceRefresh) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingState = true, error = null)
            SkillsApiService.getSkillSwapState(applicationContext)
                .onSuccess { state ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingState = false,
                        state = state
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingState = false,
                        error = error.message ?: "Could not load requests"
                    )
                }
        }
    }

    fun createRequest(
        suggestion: SkillSwapSuggestion,
        message: String,
        durationMinutes: Int
    ) {
        val busyKey = "request:${suggestion.user.id}:${suggestion.skill}"
        if (_uiState.value.busyIds.contains(busyKey)) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busyIds = _uiState.value.busyIds + busyKey, error = null)
            SkillsApiService.createSkillSwapRequest(
                applicationContext,
                SkillSwapCreateRequest(
                    recipientId = suggestion.user.id,
                    skill = suggestion.skill,
                    message = message.ifBlank { null },
                    requesterGoal = if (suggestion.mode == "teach") "Help a peer practice ${suggestion.skill}" else "Learn ${suggestion.skill}",
                    mode = suggestion.mode,
                    sessionLengthMinutes = durationMinutes
                )
            ).onSuccess {
                _uiState.value = _uiState.value.copy(toast = "Skill Swap request sent")
                loadSuggestions(forceRefresh = true)
                loadState(forceRefresh = true)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(error = error.message ?: "Could not send request")
            }
            _uiState.value = _uiState.value.copy(busyIds = _uiState.value.busyIds - busyKey)
        }
    }

    fun respond(requestId: String, action: String) {
        if (_uiState.value.busyIds.contains(requestId)) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busyIds = _uiState.value.busyIds + requestId, error = null)
            SkillsApiService.respondToSkillSwapRequest(applicationContext, requestId, action)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        toast = if (action == "accept") "Session added" else "Request declined"
                    )
                    loadState(forceRefresh = true)
                    loadSuggestions(forceRefresh = true)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(error = error.message ?: "Could not update request")
                }
            _uiState.value = _uiState.value.copy(busyIds = _uiState.value.busyIds - requestId)
        }
    }

    fun completeSession(sessionId: String, rating: Int, note: String) {
        if (_uiState.value.busyIds.contains(sessionId)) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busyIds = _uiState.value.busyIds + sessionId, error = null)
            SkillsApiService.completeSkillSwapSession(
                context = applicationContext,
                sessionId = sessionId,
                rating = rating,
                note = note.ifBlank { null },
                endorseSkill = true
            ).onSuccess {
                _uiState.value = _uiState.value.copy(toast = "Session completed and endorsed")
                loadState(forceRefresh = true)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(error = error.message ?: "Could not complete session")
            }
            _uiState.value = _uiState.value.copy(busyIds = _uiState.value.busyIds - sessionId)
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SkillSwapViewModel(context) as T
        }
    }
}

@Composable
fun SkillPassportScreen(
    userId: String = "me",
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onNavigateBack: () -> Unit,
    onOpenSkillSwap: () -> Unit = {},
    onOpenProfile: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: SkillPassportViewModel = viewModel(
        key = "skill-passport:$userId",
        factory = SkillPassportViewModel.Factory(context)
    )
    val uiState by viewModel.uiState.collectAsState()
    var verificationProvider by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(userId) {
        viewModel.load(userId)
    }

    verificationProvider?.let { provider ->
        SkillVerificationLinkDialog(
            provider = provider,
            contentColor = contentColor,
            accentColor = accentColor,
            onDismiss = { verificationProvider = null },
            onConfirm = { username, profileUrl ->
                verificationProvider = null
                viewModel.linkVerificationProfile(userId, provider, username, profileUrl)
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        SettingsHeader(
            title = "Skill Passport",
            contentColor = contentColor,
            onBack = onNavigateBack
        )

        when {
            uiState.isLoading && uiState.passport == null -> SkillLoadingState(
                contentColor = contentColor,
                accentColor = accentColor
            )
            uiState.error != null && uiState.passport == null -> SkillErrorState(
                message = uiState.error ?: "Could not load passport",
                contentColor = contentColor,
                accentColor = accentColor,
                onRetry = { viewModel.load(userId, forceRefresh = true) }
            )
            uiState.passport != null -> SkillPassportContent(
                passport = uiState.passport!!,
                contentColor = contentColor,
                accentColor = accentColor,
                onOpenSkillSwap = onOpenSkillSwap,
                onOpenProfile = onOpenProfile,
                onLinkVerification = { provider -> verificationProvider = provider }
            )
        }
    }
}

@Composable
private fun SkillPassportContent(
    passport: SkillPassportResponse,
    contentColor: Color,
    accentColor: Color,
    onOpenSkillSwap: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onLinkVerification: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            SkillPassportHero(
                passport = passport,
                contentColor = contentColor,
                accentColor = accentColor,
                onOpenSkillSwap = onOpenSkillSwap,
                onLinkVerification = onLinkVerification
            )
        }

        if (passport.skills.isNotEmpty()) {
            item {
                SkillSectionTitle("Skills", "${passport.summary.verifiedSkills} verified signals", contentColor)
            }
            items(passport.skills.take(12), key = { it.id + it.name }) { skill ->
                SkillPassportSkillCard(
                    skill = skill,
                    contentColor = contentColor,
                    accentColor = accentColor
                )
            }
        }

        if (passport.recentEvidence.isNotEmpty()) {
            item {
                SkillSectionTitle("Proof", "${passport.recentEvidence.size} recent items", contentColor)
            }
            items(passport.recentEvidence.take(8), key = { it.id }) { evidence ->
                SkillEvidenceRow(
                    evidence = evidence,
                    contentColor = contentColor,
                    accentColor = accentColor
                )
            }
        }

        if (passport.recentEndorsements.isNotEmpty()) {
            item {
                SkillSectionTitle("Endorsements", "${passport.recentEndorsements.size} recent", contentColor)
            }
            items(passport.recentEndorsements, key = { it.id }) { endorsement ->
                SkillEndorsementRow(
                    endorsement = endorsement,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onOpenProfile = onOpenProfile
                )
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SkillPassportHero(
    passport: SkillPassportResponse,
    contentColor: Color,
    accentColor: Color,
    onOpenSkillSwap: () -> Unit,
    onLinkVerification: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkillPassportAvatar(
                name = passport.user.name ?: passport.user.username ?: "Student",
                profileImage = passport.user.profileImage,
                contentColor = contentColor,
                accentColor = accentColor
            )
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicText(
                        passport.user.name ?: "Student",
                        style = TextStyle(contentColor, 22.sp, FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (passport.summary.hasVerifiedSkillsBadge) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Outlined.Verified,
                            contentDescription = null,
                            tint = Color(0xFF22C55E),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                BasicText(
                    passport.user.headline ?: passport.user.college ?: "Skill proof profile",
                    style = TextStyle(contentColor.copy(alpha = 0.62f), 13.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                passport.user.college?.let { college ->
                    Spacer(Modifier.height(3.dp))
                    BasicText(
                        college,
                        style = TextStyle(contentColor.copy(alpha = 0.42f), 11.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            PassportScoreBadge(
                score = passport.summary.passportScore,
                contentColor = contentColor,
                accentColor = accentColor
            )
        }

        Spacer(Modifier.height(22.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SkillPassportMetric("${passport.summary.totalSkills}", "Skills", contentColor)
            SkillPassportMetric("${passport.summary.verifiedSkills}", "Verified", contentColor)
            SkillPassportMetric("${passport.summary.evidenceCount}", "Proof", contentColor)
            SkillPassportMetric("${passport.summary.endorsementsCount}", "Votes", contentColor)
        }

        if (passport.teachingSkills.isNotEmpty() || passport.learningGoals.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(passport.teachingSkills.take(5), key = { "teach:$it" }) {
                    SkillRoleChip("Teaches", it, accentColor, contentColor)
                }
                items(passport.learningGoals.take(5), key = { "learn:$it" }) {
                    SkillRoleChip("Learning", it, Color(0xFF22C55E), contentColor)
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        SkillPrimaryButton(
            text = "Open Skill Swap",
            icon = Icons.Outlined.Groups,
            accentColor = accentColor,
            onClick = onOpenSkillSwap,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SkillSecondaryButton(
                text = "GitHub",
                icon = Icons.Outlined.Code,
                contentColor = contentColor,
                onClick = { onLinkVerification("github") },
                modifier = Modifier.weight(1f)
            )
            SkillSecondaryButton(
                text = "LeetCode",
                icon = Icons.Outlined.Verified,
                contentColor = contentColor,
                onClick = { onLinkVerification("leetcode") },
                modifier = Modifier.weight(1f)
            )
        }
        if (passport.verificationLinks.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                passport.verificationLinks.take(3).forEach { link ->
                    SkillTinyChip("${link.provider.replaceFirstChar { it.uppercase() }} linked", Color(0xFF22C55E), contentColor)
                }
            }
        }
    }
}

@Composable
private fun SkillPassportAvatar(
    name: String,
    profileImage: String?,
    contentColor: Color,
    accentColor: Color
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(accentColor.copy(alpha = 0.14f))
            .border(1.dp, contentColor.copy(alpha = 0.10f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!profileImage.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(profileImage)
                    .crossfade(true)
                    .build(),
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            BasicText(
                name.firstOrNull()?.uppercase() ?: "V",
                style = TextStyle(accentColor, 24.sp, FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun PassportScoreBadge(
    score: Int,
    contentColor: Color,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .width(58.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(accentColor.copy(alpha = 0.12f))
            .padding(vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BasicText(
            score.coerceIn(0, 100).toString(),
            style = TextStyle(accentColor, 18.sp, FontWeight.Bold)
        )
        BasicText("score", style = TextStyle(contentColor.copy(alpha = 0.54f), 10.sp))
    }
}

@Composable
private fun SkillPassportMetric(value: String, label: String, contentColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BasicText(value, style = TextStyle(contentColor, 17.sp, FontWeight.Bold))
        BasicText(label, style = TextStyle(contentColor.copy(alpha = 0.48f), 11.sp))
    }
}

@Composable
private fun SkillVerificationLinkDialog(
    provider: String,
    contentColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (username: String, profileUrl: String?) -> Unit
) {
    var username by rememberSaveable { mutableStateOf("") }
    var profileUrl by rememberSaveable { mutableStateOf("") }
    val providerLabel = provider.replaceFirstChar { it.uppercase() }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(if (contentColor == Color.White) Color(0xFF111827) else Color.White)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BasicText(
                "Link $providerLabel",
                style = TextStyle(contentColor, 20.sp, FontWeight.Bold)
            )
            BasicText(
                "This adds verified evidence to your Skill Passport.",
                style = TextStyle(contentColor.copy(alpha = 0.64f), 13.sp)
            )
            SkillDialogField(
                value = username,
                onValueChange = { username = it },
                placeholder = if (provider == "github") "GitHub username" else "LeetCode username",
                contentColor = contentColor,
                accentColor = accentColor
            )
            SkillDialogField(
                value = profileUrl,
                onValueChange = { profileUrl = it },
                placeholder = "Profile URL optional",
                contentColor = contentColor,
                accentColor = accentColor
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SkillSecondaryButton(
                    text = "Cancel",
                    icon = Icons.Outlined.Close,
                    contentColor = contentColor,
                    modifier = Modifier.weight(1f),
                    onClick = onDismiss
                )
                SkillPrimaryButton(
                    text = "Link",
                    icon = Icons.Outlined.Verified,
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f),
                    enabled = username.isNotBlank(),
                    onClick = { onConfirm(username, profileUrl.takeIf { it.isNotBlank() }) }
                )
            }
        }
    }
}

@Composable
private fun SkillDialogField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    contentColor: Color,
    accentColor: Color
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(contentColor, 15.sp),
        cursorBrush = SolidColor(accentColor),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(contentColor.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        decorationBox = { inner ->
            if (value.isBlank()) {
                BasicText(placeholder, style = TextStyle(contentColor.copy(alpha = 0.42f), 15.sp))
            }
            inner()
        }
    )
}

@Composable
private fun PassportScoreRing(
    score: Int,
    accentColor: Color,
    contentColor: Color
) {
    Box(
        modifier = Modifier.size(96.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = contentColor.copy(alpha = 0.10f),
                startAngle = -210f,
                sweepAngle = 240f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(9.dp.toPx(), 9.dp.toPx()),
                size = Size(size.width - 18.dp.toPx(), size.height - 18.dp.toPx()),
                style = stroke
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(accentColor, Color(0xFF22C55E), accentColor)),
                startAngle = -210f,
                sweepAngle = 240f * (score.coerceIn(0, 100) / 100f),
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(9.dp.toPx(), 9.dp.toPx()),
                size = Size(size.width - 18.dp.toPx(), size.height - 18.dp.toPx()),
                style = stroke
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText(
                score.coerceIn(0, 100).toString(),
                style = TextStyle(contentColor, 24.sp, FontWeight.Bold)
            )
            BasicText("score", style = TextStyle(contentColor.copy(alpha = 0.58f), 11.sp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SkillPassportSkillCard(
    skill: SkillPassportSkill,
    contentColor: Color,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.07f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (skill.verifiedEvidenceCount > 0) Icons.Outlined.Verified else Icons.Outlined.Code,
                    contentDescription = null,
                    tint = if (skill.verifiedEvidenceCount > 0) Color(0xFF22C55E) else contentColor.copy(alpha = 0.74f),
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                BasicText(
                    skill.name,
                    style = TextStyle(contentColor, 15.sp, FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                BasicText(
                    skillPassportSkillSubtitle(skill),
                    style = TextStyle(contentColor.copy(alpha = 0.56f), 11.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicText(
                "${skill.confidenceScore.coerceIn(0, 100)}%",
                style = TextStyle(accentColor, 12.sp, FontWeight.Bold)
            )
        }

        if (skill.canTeach || skill.wantsToLearn || skill.sources.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (skill.canTeach) SkillTinyChip("Can teach", accentColor, contentColor)
                if (skill.wantsToLearn) SkillTinyChip("Learning", Color(0xFF22C55E), contentColor)
                skill.sources.take(3).forEach { source ->
                    SkillTinyChip(sourceLabel(source), contentColor.copy(alpha = 0.32f), contentColor)
                }
            }
        }

        if (skill.evidence.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                skill.evidence.take(2).forEach { evidence ->
                    SkillEvidenceMini(evidence, contentColor, accentColor)
                }
            }
        }
        SkillPassportDivider(contentColor)
    }
}

@Composable
fun SkillSwapScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    initialTab: String = "discover",
    onNavigateBack: () -> Unit,
    onOpenProfile: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: SkillSwapViewModel = viewModel(factory = SkillSwapViewModel.Factory(context))
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by rememberSaveable { mutableStateOf(normalizeSkillSwapTab(initialTab)) }
    var selectedSuggestion by remember { mutableStateOf<SkillSwapSuggestion?>(null) }
    var completingSession by remember { mutableStateOf<SkillSwapSession?>(null) }

    LaunchedEffect(initialTab) {
        selectedTab = normalizeSkillSwapTab(initialTab)
    }

    LaunchedEffect(uiState.toast) {
        uiState.toast?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        SettingsHeader(
            title = "Skill Swap",
            contentColor = contentColor,
            onBack = onNavigateBack
        )

        SkillSwapHero(
            uiState = uiState,
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            onModeChange = viewModel::setMode
        )

        Spacer(Modifier.height(10.dp))

        SkillSwapTabs(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            contentColor = contentColor,
            accentColor = accentColor,
            incomingCount = uiState.state.incoming.size,
            sessionCount = uiState.state.sessions.count { it.status != "completed" }
        )

        AnimatedVisibility(visible = uiState.error != null, enter = fadeIn(), exit = fadeOut()) {
            SkillInlineMessage(
                message = uiState.error ?: "",
                contentColor = contentColor,
                accentColor = accentColor
            )
        }

        when (selectedTab) {
            "discover" -> SkillSwapDiscoverContent(
                uiState = uiState,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onSkillSelected = viewModel::selectSkill,
                onRequest = { selectedSuggestion = it },
                onOpenProfile = onOpenProfile
            )
            "requests" -> SkillSwapRequestsContent(
                uiState = uiState,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onRespond = viewModel::respond,
                onOpenProfile = onOpenProfile
            )
            "sessions" -> SkillSwapSessionsContent(
                uiState = uiState,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onComplete = { completingSession = it },
                onOpenProfile = onOpenProfile
            )
        }
    }

    selectedSuggestion?.let { suggestion ->
        SkillSwapRequestDialog(
            suggestion = suggestion,
            contentColor = contentColor,
            accentColor = accentColor,
            onDismiss = { selectedSuggestion = null },
            onSend = { message, minutes ->
                viewModel.createRequest(suggestion, message, minutes)
                selectedSuggestion = null
            }
        )
    }

    completingSession?.let { session ->
        SkillSwapCompleteDialog(
            session = session,
            contentColor = contentColor,
            accentColor = accentColor,
            onDismiss = { completingSession = null },
            onComplete = { rating, note ->
                viewModel.completeSession(session.id, rating, note)
                completingSession = null
            }
        )
    }
}

private fun normalizeSkillSwapTab(tab: String?): String = when (tab?.lowercase()) {
    "requests" -> "requests"
    "sessions" -> "sessions"
    else -> "discover"
}

@Composable
private fun SkillSwapHero(
    uiState: SkillSwapUiState,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onModeChange: (String) -> Unit
) {
    SkillSurfaceCard(
        backdrop = backdrop,
        contentColor = contentColor,
        accentColor = accentColor,
        tint = accentColor.copy(alpha = 0.08f)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = accentColor)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                BasicText("Skill exchange room", style = TextStyle(contentColor, 18.sp, FontWeight.Bold))
                BasicText(
                    "${uiState.suggestions.suggestions.size} matches · ${uiState.state.incoming.size} incoming",
                    style = TextStyle(contentColor.copy(alpha = 0.64f), 12.sp)
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SkillModeChip("Learn", uiState.mode == "learn", accentColor, contentColor) { onModeChange("learn") }
            SkillModeChip("Teach", uiState.mode == "teach", accentColor, contentColor) { onModeChange("teach") }
        }
    }
}

@Composable
private fun SkillSwapDiscoverContent(
    uiState: SkillSwapUiState,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onSkillSelected: (String?) -> Unit,
    onRequest: (SkillSwapSuggestion) -> Unit,
    onOpenProfile: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (uiState.suggestions.featuredSkills.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        SkillFilterChip(
                            text = "All",
                            selected = uiState.selectedSkill == null,
                            accentColor = accentColor,
                            contentColor = contentColor,
                            onClick = { onSkillSelected(null) }
                        )
                    }
                    items(uiState.suggestions.featuredSkills, key = { it }) { skill ->
                        SkillFilterChip(
                            text = skill,
                            selected = uiState.selectedSkill == skill,
                            accentColor = accentColor,
                            contentColor = contentColor,
                            onClick = { onSkillSelected(skill) }
                        )
                    }
                }
            }
        }

        if (uiState.isLoadingSuggestions && uiState.suggestions.suggestions.isEmpty()) {
            items(4) {
                SkillSwapSkeletonCard(contentColor)
            }
        } else if (uiState.suggestions.suggestions.isEmpty()) {
            item {
                SkillEmptyCard(
                    title = "No skill matches yet",
                    subtitle = "Add can-teach and want-to-learn skills in profile preferences.",
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor
                )
            }
        } else {
            items(uiState.suggestions.suggestions, key = { it.user.id + it.skill + it.mode }) { suggestion ->
                SkillSwapSuggestionCard(
                    suggestion = suggestion,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    isBusy = uiState.busyIds.contains("request:${suggestion.user.id}:${suggestion.skill}"),
                    onRequest = { onRequest(suggestion) },
                    onOpenProfile = { onOpenProfile(suggestion.user.id) }
                )
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SkillSwapRequestsContent(
    uiState: SkillSwapUiState,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onRespond: (String, String) -> Unit,
    onOpenProfile: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (uiState.state.incoming.isEmpty() && uiState.state.outgoing.isEmpty() && !uiState.isLoadingState) {
            item {
                SkillEmptyCard(
                    title = "No open requests",
                    subtitle = "Requests you send or receive will stay here until accepted.",
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor
                )
            }
        }
        if (uiState.state.incoming.isNotEmpty()) {
            item { SkillSectionTitle("Incoming", "${uiState.state.incoming.size} waiting", contentColor) }
            items(uiState.state.incoming, key = { it.id }) { request ->
                SkillSwapRequestCard(
                    request = request,
                    incoming = true,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    isBusy = uiState.busyIds.contains(request.id),
                    onAccept = { onRespond(request.id, "accept") },
                    onDecline = { onRespond(request.id, "decline") },
                    onOpenProfile = onOpenProfile
                )
            }
        }
        if (uiState.state.outgoing.isNotEmpty()) {
            item { SkillSectionTitle("Sent", "${uiState.state.outgoing.size} pending", contentColor) }
            items(uiState.state.outgoing, key = { it.id }) { request ->
                SkillSwapRequestCard(
                    request = request,
                    incoming = false,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    isBusy = false,
                    onAccept = {},
                    onDecline = {},
                    onOpenProfile = onOpenProfile
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SkillSwapSessionsContent(
    uiState: SkillSwapUiState,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onComplete: (SkillSwapSession) -> Unit,
    onOpenProfile: (String) -> Unit
) {
    val active = uiState.state.sessions.filter { it.status != "completed" }
    val completed = uiState.state.sessions.filter { it.status == "completed" }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (uiState.state.sessions.isEmpty() && !uiState.isLoadingState) {
            item {
                SkillEmptyCard(
                    title = "No sessions yet",
                    subtitle = "Accepted swaps become short practice sessions here.",
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor
                )
            }
        }
        if (active.isNotEmpty()) {
            item { SkillSectionTitle("Upcoming", "${active.size} active", contentColor) }
            items(active, key = { it.id }) { session ->
                SkillSwapSessionCard(
                    session = session,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    isBusy = uiState.busyIds.contains(session.id),
                    onComplete = { onComplete(session) },
                    onOpenProfile = onOpenProfile
                )
            }
        }
        if (completed.isNotEmpty()) {
            item { SkillSectionTitle("Completed", "${completed.size} endorsed", contentColor) }
            items(completed, key = { it.id }) { session ->
                SkillSwapSessionCard(
                    session = session,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    isBusy = false,
                    onComplete = {},
                    onOpenProfile = onOpenProfile
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SkillSwapSuggestionCard(
    suggestion: SkillSwapSuggestion,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isBusy: Boolean,
    onRequest: () -> Unit,
    onOpenProfile: () -> Unit
) {
    SkillSurfaceCard(
        backdrop = backdrop,
        contentColor = contentColor,
        accentColor = accentColor
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SkillAvatar(suggestion.user, contentColor, accentColor, onClick = onOpenProfile)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                BasicText(
                    suggestion.user.name ?: "Student",
                    style = TextStyle(contentColor, 16.sp, FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                BasicText(
                    suggestion.matchReason,
                    style = TextStyle(contentColor.copy(alpha = 0.64f), 12.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            SkillConfidencePill(suggestion.matchScore, accentColor, contentColor)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SkillTinyChip(suggestion.skill, accentColor, contentColor)
            if (suggestion.evidenceCount > 0) {
                SkillTinyChip("${suggestion.evidenceCount} proof", Color(0xFF22C55E), contentColor)
            }
            if (suggestion.sharedContext.sameCampus) {
                SkillTinyChip("Same campus", contentColor.copy(alpha = 0.28f), contentColor)
            }
        }
        Spacer(Modifier.height(14.dp))
        SkillPrimaryButton(
            text = if (suggestion.activeRequestStatus == "pending") "Requested" else if (suggestion.mode == "teach") "Offer Help" else "Request Swap",
            icon = if (suggestion.activeRequestStatus == "pending") Icons.Outlined.CheckCircle else Icons.Outlined.PersonAddAlt1,
            accentColor = accentColor,
            enabled = suggestion.activeRequestStatus != "pending" && !isBusy,
            onClick = onRequest,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SkillSwapRequestCard(
    request: SkillSwapRequestItem,
    incoming: Boolean,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isBusy: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onOpenProfile: (String) -> Unit
) {
    val person = if (incoming) request.requester else request.recipient
    SkillSurfaceCard(
        backdrop = backdrop,
        contentColor = contentColor,
        accentColor = accentColor
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SkillAvatar(person, contentColor, accentColor) {
                person?.id?.let(onOpenProfile)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                BasicText(
                    person?.name ?: "Student",
                    style = TextStyle(contentColor, 16.sp, FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                BasicText(
                    "${request.skill} · ${request.sessionLengthMinutes} min",
                    style = TextStyle(contentColor.copy(alpha = 0.64f), 12.sp)
                )
            }
            SkillStatusPill(request.status, accentColor, contentColor)
        }
        if (!request.message.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            BasicText(
                request.message,
                style = TextStyle(contentColor.copy(alpha = 0.76f), 13.sp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (incoming) {
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SkillSecondaryButton(
                    text = "Decline",
                    icon = Icons.Outlined.Close,
                    contentColor = contentColor,
                    onClick = onDecline,
                    enabled = !isBusy,
                    modifier = Modifier.weight(1f)
                )
                SkillPrimaryButton(
                    text = "Accept",
                    icon = Icons.Outlined.Check,
                    accentColor = accentColor,
                    onClick = onAccept,
                    enabled = !isBusy,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SkillSwapSessionCard(
    session: SkillSwapSession,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isBusy: Boolean,
    onComplete: () -> Unit,
    onOpenProfile: (String) -> Unit
) {
    val partner = session.mentor ?: session.learner
    SkillSurfaceCard(
        backdrop = backdrop,
        contentColor = contentColor,
        accentColor = accentColor
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SkillAvatar(partner, contentColor, accentColor) {
                partner?.id?.let(onOpenProfile)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                BasicText(
                    session.skill,
                    style = TextStyle(contentColor, 17.sp, FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                BasicText(
                    "${session.sessionLengthMinutes} min · ${session.status.replaceFirstChar { it.uppercase() }}",
                    style = TextStyle(contentColor.copy(alpha = 0.64f), 12.sp)
                )
            }
            Icon(
                imageVector = if (session.status == "completed") Icons.Outlined.Verified else Icons.Outlined.Schedule,
                contentDescription = null,
                tint = accentColor
            )
        }
        if (session.status != "completed") {
            Spacer(Modifier.height(14.dp))
            SkillPrimaryButton(
                text = "Complete & Endorse",
                icon = Icons.Outlined.StarOutline,
                accentColor = accentColor,
                enabled = !isBusy,
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SkillSwapRequestDialog(
    suggestion: SkillSwapSuggestion,
    contentColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSend: (String, Int) -> Unit
) {
    var message by rememberSaveable {
        mutableStateOf(
            if (suggestion.mode == "teach")
                "I can help you practice ${suggestion.skill}. Want to do a quick swap?"
            else
                "I want to learn ${suggestion.skill}. Can we do a quick swap?"
        )
    }
    var duration by rememberSaveable { mutableIntStateOf(30) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF111827).copy(alpha = 0.96f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            BasicText("Request Skill Swap", style = TextStyle(Color.White, 19.sp, FontWeight.Bold))
            BasicText(
                "${suggestion.user.name ?: "Student"} · ${suggestion.skill}",
                style = TextStyle(Color.White.copy(alpha = 0.68f), 13.sp)
            )
            BasicTextField(
                value = message,
                onValueChange = { message = it.take(280) },
                textStyle = TextStyle(Color.White, 14.sp),
                cursorBrush = SolidColor(accentColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(12.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 30, 45).forEach { minutes ->
                    SkillFilterChip(
                        text = "${minutes}m",
                        selected = duration == minutes,
                        accentColor = accentColor,
                        contentColor = Color.White,
                        onClick = { duration = minutes }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SkillSecondaryButton(
                    text = "Cancel",
                    icon = Icons.Outlined.Close,
                    contentColor = Color.White,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                SkillPrimaryButton(
                    text = "Send",
                    icon = Icons.Outlined.PersonAddAlt1,
                    accentColor = accentColor,
                    onClick = { onSend(message, duration) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SkillSwapCompleteDialog(
    session: SkillSwapSession,
    contentColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit,
    onComplete: (Int, String) -> Unit
) {
    var note by rememberSaveable { mutableStateOf("Great skill swap on ${session.skill}.") }
    var rating by rememberSaveable { mutableIntStateOf(5) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF111827).copy(alpha = 0.96f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            BasicText("Complete Session", style = TextStyle(Color.White, 19.sp, FontWeight.Bold))
            BasicText(
                "Your endorsement will strengthen their Skill Passport.",
                style = TextStyle(Color.White.copy(alpha = 0.68f), 13.sp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..5).forEach { star ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (rating >= star) accentColor.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f))
                            .clickable { rating = star },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.StarOutline,
                            contentDescription = null,
                            tint = if (rating >= star) accentColor else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            BasicTextField(
                value = note,
                onValueChange = { note = it.take(240) },
                textStyle = TextStyle(Color.White, 14.sp),
                cursorBrush = SolidColor(accentColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(94.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(12.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SkillSecondaryButton(
                    text = "Cancel",
                    icon = Icons.Outlined.Close,
                    contentColor = Color.White,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                SkillPrimaryButton(
                    text = "Complete",
                    icon = Icons.Outlined.Verified,
                    accentColor = accentColor,
                    onClick = { onComplete(rating, note) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SkillSurfaceCard(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    tint: Color = Color.Transparent,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = contentColor == Color.White
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(
                Modifier.drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(22.dp) },
                    effects = {
                        vibrancy()
                        blur(14f.dp.toPx())
                        lens(6f.dp.toPx(), 12f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(
                            tint.takeIf { it != Color.Transparent }
                                ?: if (isDark) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.34f)
                        )
                    }
                )
            )
            .border(
                1.dp,
                if (isDark) Color.White.copy(alpha = 0.13f) else Color.Black.copy(alpha = 0.07f),
                shape
            )
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun SkillSectionTitle(title: String, subtitle: String, contentColor: Color) {
    Column(Modifier.padding(top = 10.dp, bottom = 6.dp)) {
        BasicText(title, style = TextStyle(contentColor, 14.sp, FontWeight.SemiBold))
        BasicText(subtitle, style = TextStyle(contentColor.copy(alpha = 0.48f), 11.sp))
    }
}

@Composable
private fun SkillPassportDivider(contentColor: Color) {
    Spacer(
        modifier = Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(contentColor.copy(alpha = 0.08f))
    )
}

@Composable
private fun SkillMetricChip(value: String, label: String, contentColor: Color, accentColor: Color) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(accentColor.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BasicText(value, style = TextStyle(contentColor, 15.sp, FontWeight.Bold))
        BasicText(label, style = TextStyle(contentColor.copy(alpha = 0.58f), 10.sp))
    }
}

@Composable
private fun SkillRoleChip(label: String, value: String, color: Color, contentColor: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.13f))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(label, style = TextStyle(color, 10.sp, FontWeight.SemiBold))
        Spacer(Modifier.width(6.dp))
        BasicText(value, style = TextStyle(contentColor, 12.sp, FontWeight.Medium), maxLines = 1)
    }
}

@Composable
private fun SkillTinyChip(text: String, color: Color, contentColor: Color) {
    BasicText(
        text,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        style = TextStyle(
            color = if (color == contentColor.copy(alpha = 0.32f)) contentColor.copy(alpha = 0.72f) else contentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun SkillConfidencePill(score: Int, accentColor: Color, contentColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(accentColor.copy(alpha = 0.14f))
            .padding(horizontal = 9.dp, vertical = 6.dp)
    ) {
        BasicText(
            "${score.coerceIn(0, 100)}%",
            style = TextStyle(accentColor, 12.sp, FontWeight.Bold)
        )
    }
}

@Composable
private fun SkillStatusPill(status: String, accentColor: Color, contentColor: Color) {
    val color = when (status) {
        "accepted", "completed" -> Color(0xFF22C55E)
        "declined" -> Color(0xFFEF4444)
        else -> accentColor
    }
    BasicText(
        status.replaceFirstChar { it.uppercase() },
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        style = TextStyle(color, 11.sp, FontWeight.SemiBold)
    )
}

@Composable
private fun SkillPrimaryButton(
    text: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) accentColor else accentColor.copy(alpha = 0.38f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        BasicText(text, style = TextStyle(Color.White, 13.sp, FontWeight.SemiBold))
    }
}

@Composable
private fun SkillSecondaryButton(
    text: String,
    icon: ImageVector,
    contentColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(contentColor.copy(alpha = if (enabled) 0.10f else 0.05f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = contentColor.copy(alpha = 0.82f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        BasicText(text, style = TextStyle(contentColor.copy(alpha = 0.86f), 13.sp, FontWeight.SemiBold))
    }
}

@Composable
private fun SkillModeChip(
    text: String,
    selected: Boolean,
    accentColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    SkillFilterChip(text, selected, accentColor, contentColor, onClick)
}

@Composable
private fun SkillFilterChip(
    text: String,
    selected: Boolean,
    accentColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    BasicText(
        text,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) accentColor else contentColor.copy(alpha = 0.09f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        style = TextStyle(
            color = if (selected) Color.White else contentColor.copy(alpha = 0.78f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun SkillSwapTabs(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    contentColor: Color,
    accentColor: Color,
    incomingCount: Int,
    sessionCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SkillFilterChip("Discover", selectedTab == "discover", accentColor, contentColor) { onTabSelected("discover") }
        SkillFilterChip("Requests${if (incomingCount > 0) " $incomingCount" else ""}", selectedTab == "requests", accentColor, contentColor) { onTabSelected("requests") }
        SkillFilterChip("Sessions${if (sessionCount > 0) " $sessionCount" else ""}", selectedTab == "sessions", accentColor, contentColor) { onTabSelected("sessions") }
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun SkillAvatar(
    person: SkillPerson?,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(accentColor.copy(alpha = 0.16f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val profileImage = person?.profileImage
        if (!profileImage.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(profileImage)
                    .crossfade(true)
                    .build(),
                contentDescription = person.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            BasicText(
                person?.name?.firstOrNull()?.uppercase() ?: "V",
                style = TextStyle(accentColor, 18.sp, FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun SkillEvidenceRow(
    evidence: SkillPassportEvidence,
    contentColor: Color,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        SkillEvidenceMini(evidence, contentColor, accentColor)
        SkillPassportDivider(contentColor)
    }
}

@Composable
private fun SkillEvidenceMini(
    evidence: SkillPassportEvidence,
    contentColor: Color,
    accentColor: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(contentColor.copy(alpha = 0.07f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = evidenceIcon(evidence.type),
                contentDescription = null,
                tint = if (evidence.verified) Color(0xFF22C55E) else contentColor.copy(alpha = 0.72f),
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            BasicText(
                evidence.title,
                style = TextStyle(contentColor, 13.sp, FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            BasicText(
                "${evidence.skillName} · ${sourceLabel(evidence.type)}",
                style = TextStyle(contentColor.copy(alpha = 0.58f), 11.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (evidence.verified) {
            Icon(
                Icons.Outlined.Verified,
                contentDescription = null,
                tint = Color(0xFF22C55E),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SkillEndorsementRow(
    endorsement: com.kyant.backdrop.catalog.network.models.SkillPassportEndorsement,
    contentColor: Color,
    accentColor: Color,
    onOpenProfile: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SkillAvatar(endorsement.endorsedBy, contentColor, accentColor) {
                endorsement.endorsedBy?.id?.let(onOpenProfile)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                BasicText(
                    endorsement.skillName,
                    style = TextStyle(contentColor, 15.sp, FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                BasicText(
                    endorsement.endorsedBy?.name ?: "Peer endorsement",
                    style = TextStyle(contentColor.copy(alpha = 0.58f), 12.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            endorsement.rating?.let {
                SkillTinyChip("$it/5", accentColor, contentColor)
            }
        }
        if (!endorsement.note.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            BasicText(
                endorsement.note,
                style = TextStyle(contentColor.copy(alpha = 0.76f), 13.sp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        SkillPassportDivider(contentColor)
    }
}

@Composable
private fun SkillInlineMessage(message: String, contentColor: Color, accentColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(accentColor.copy(alpha = 0.10f))
            .padding(12.dp)
    ) {
        BasicText(message, style = TextStyle(contentColor.copy(alpha = 0.78f), 12.sp))
    }
}

@Composable
private fun SkillEmptyCard(
    title: String,
    subtitle: String,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color
) {
    SkillSurfaceCard(
        backdrop = backdrop,
        contentColor = contentColor,
        accentColor = accentColor
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Outlined.TrackChanges, contentDescription = null, tint = accentColor, modifier = Modifier.size(32.dp))
            BasicText(title, style = TextStyle(contentColor, 16.sp, FontWeight.SemiBold, textAlign = TextAlign.Center))
            BasicText(subtitle, style = TextStyle(contentColor.copy(alpha = 0.62f), 12.sp, textAlign = TextAlign.Center))
        }
    }
}

@Composable
private fun SkillLoadingState(contentColor: Color, accentColor: Color) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = accentColor)
            Spacer(Modifier.height(12.dp))
            BasicText("Loading Skill Passport", style = TextStyle(contentColor.copy(alpha = 0.66f), 13.sp))
        }
    }
}

@Composable
private fun SkillErrorState(
    message: String,
    contentColor: Color,
    accentColor: Color,
    onRetry: () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BasicText(message, style = TextStyle(contentColor.copy(alpha = 0.76f), 14.sp, textAlign = TextAlign.Center))
            SkillPrimaryButton("Retry", Icons.Outlined.CheckCircle, accentColor, onClick = onRetry)
        }
    }
}

@Composable
private fun SkillSwapSkeletonCard(contentColor: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(156.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(contentColor.copy(alpha = 0.07f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(contentColor.copy(alpha = 0.10f)))
        Box(Modifier.fillMaxWidth(0.72f).height(14.dp).clip(RoundedCornerShape(7.dp)).background(contentColor.copy(alpha = 0.10f)))
        Box(Modifier.fillMaxWidth(0.50f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(contentColor.copy(alpha = 0.08f)))
    }
}

private fun skillPassportSkillSubtitle(skill: SkillPassportSkill): String {
    val parts = mutableListOf<String>()
    skill.category?.takeIf { it.isNotBlank() }?.let(parts::add)
    skill.proficiency?.takeIf { it.isNotBlank() }?.let(parts::add)
    parts += "${skill.evidenceCount} proof"
    parts += "${skill.endorsementCount} votes"
    if (skill.verifiedEvidenceCount > 0) parts += "${skill.verifiedEvidenceCount} verified"
    return parts.joinToString(" · ")
}

private fun sourceLabel(source: String): String = when (source) {
    "PROFILE_SKILL" -> "Profile"
    "PROJECT" -> "Project"
    "EXPERIENCE" -> "Experience"
    "CERTIFICATE" -> "Certificate"
    "ACHIEVEMENT" -> "Achievement"
    "GITHUB" -> "GitHub"
    "GITHUB_REPO" -> "Repo"
    else -> source.lowercase().replaceFirstChar { it.uppercase() }
}

private fun evidenceIcon(source: String): ImageVector = when (source) {
    "PROJECT", "GITHUB", "GITHUB_REPO" -> Icons.Outlined.Code
    "CERTIFICATE", "ACHIEVEMENT" -> Icons.Outlined.Verified
    "EXPERIENCE" -> Icons.Outlined.Description
    else -> Icons.Outlined.School
}
