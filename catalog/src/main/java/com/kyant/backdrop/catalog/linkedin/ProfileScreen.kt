package com.kyant.backdrop.catalog.linkedin

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.kyant.backdrop.catalog.ui.BasicText
import com.kyant.backdrop.catalog.ui.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.draw.shadow
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.Matrix
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.CachePolicy
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.catalog.data.SettingsPreferences
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.components.LiquidSlider
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.models.*
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.io.ByteArrayOutputStream
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.foundation.border
import kotlin.math.max
import kotlin.math.min
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.roundToInt

private const val ProfileGitHubSectionKey = "profile_github_section"
private val RetroGameCream = Color(0xFFF3EFE3)
private val RetroGameInk = Color(0xFF111111)
private val RetroGameYellow = Color(0xFFFFD414)
private val RetroGameRed = Color(0xFFFF3B30)
private val RetroGameBlue = Color(0xFF3F6FFF)
private val RetroGameGreen = Color(0xFF28D17C)

// ==================== Main Profile Screen ====================

@Composable
fun ProfileScreen(
    userId: String? = null,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    profileViewModel: ProfileViewModel? = null,
    onNavigateBack: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onMessage: (String) -> Unit = {},
    showStartConversation: Boolean = false,
    isPreparingConversationStarter: Boolean = false,
    onStartConversation: (String) -> Unit = {},
    onOpenFeedItem: (FeedItem) -> Unit = {},
    onOpenProfile: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val screenViewModel = profileViewModel ?: viewModel(
        key = "profile:${userId ?: "me"}",
        factory = ProfileViewModel.Factory(context)
    )
    val uiState by screenViewModel.uiState.collectAsState()
    
    // Theme detection
    val themeMode by SettingsPreferences.themeMode(context).collectAsState(initial = DefaultThemeModeKey)
    val reduceAnimations by SettingsPreferences.reduceAnimations(context).collectAsState(initial = false)
    val profileThemePreference by SettingsPreferences.profileTheme(context).collectAsState(initial = DefaultProfileThemeKey)
    val showProfileLocation by SettingsPreferences.showProfileLocation(context).collectAsState(initial = true)
    val appearance = currentVormexAppearance(themeMode)
    val isGlassTheme = appearance.isGlassTheme
    val isDarkTheme = appearance.isDarkTheme
    val profileUser = uiState.profile?.user
    val canUseProfileTheme =
        profileUser?.canAccessProfileCustomization == true || profileUser?.isPremium == true
    val activeProfileThemeKey = normalizeProfileThemeKey(
        if (uiState.isOwner && canUseProfileTheme) {
            profileThemePreference
        } else {
            profileUser?.profileTheme
        }
    )
    val isGameProfileTheme = activeProfileThemeKey == GameRetroProfileThemeKey
    
    // Project screen state
    var showAddProject by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<Project?>(null) }
    var projectDetailProject by remember { mutableStateOf<Project?>(null) }
    var projectDetailVisible by remember { mutableStateOf(false) }
    var showReportProfileDialog by remember { mutableStateOf(false) }
    var isSubmittingProfileReport by remember { mutableStateOf(false) }
    var isBlockingProfileUser by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun dismissProjectDetail(afterDismiss: (() -> Unit)? = null) {
        scope.launch {
            projectDetailVisible = false
            delay(220)
            projectDetailProject = null
            afterDismiss?.invoke()
        }
    }

    fun targetProfileUserId(): String? {
        val targetUser = uiState.profile?.user ?: return null
        return targetUser.id.takeIf { it.isNotBlank() && !uiState.isOwner }
    }

    fun submitProfileReport(reason: String, details: String, blockUser: Boolean) {
        val targetUserId = targetProfileUserId() ?: return
        if (isSubmittingProfileReport) return
        scope.launch {
            isSubmittingProfileReport = true
            ApiClient.reportUser(
                context = context,
                userId = targetUserId,
                reason = reason,
                description = details.ifBlank { null },
                blockUser = blockUser
            ).onSuccess {
                Toast.makeText(
                    context,
                    if (blockUser) {
                        "Report sent and user blocked."
                    } else {
                        "Thanks - we received your report."
                    },
                    Toast.LENGTH_LONG
                ).show()
                showReportProfileDialog = false
                if (blockUser) onNavigateBack()
            }.onFailure { error ->
                Toast.makeText(
                    context,
                    error.message ?: "Could not submit report",
                    Toast.LENGTH_LONG
                ).show()
            }
            isSubmittingProfileReport = false
        }
    }

    fun blockProfileUser() {
        val targetUserId = targetProfileUserId() ?: return
        if (isBlockingProfileUser) return
        scope.launch {
            isBlockingProfileUser = true
            ApiClient.blockUser(
                context = context,
                userId = targetUserId,
                reason = "Blocked from profile"
            ).onSuccess {
                Toast.makeText(context, "User blocked.", Toast.LENGTH_LONG).show()
                onNavigateBack()
            }.onFailure { error ->
                Toast.makeText(
                    context,
                    error.message ?: "Could not block user",
                    Toast.LENGTH_LONG
                ).show()
            }
            isBlockingProfileUser = false
        }
    }
    
    // Experience screen state
    var showAddExperience by remember { mutableStateOf(false) }
    var editingExperience by remember { mutableStateOf<Experience?>(null) }
    
    // Education screen state
    var showAddEducation by remember { mutableStateOf(false) }
    var editingEducation by remember { mutableStateOf<Education?>(null) }
    
    // Certificate screen state
    var showAddCertificate by remember { mutableStateOf(false) }
    var editingCertificate by remember { mutableStateOf<Certificate?>(null) }
    var viewingCertificate by remember { mutableStateOf<Certificate?>(null) }
    
    // Achievement screen state
    var showAddAchievement by remember { mutableStateOf(false) }
    var editingAchievement by remember { mutableStateOf<Achievement?>(null) }
    var viewingAchievement by remember { mutableStateOf<Achievement?>(null) }
    
    // Skills screen state
    var showAddSkill by remember { mutableStateOf(false) }
    
    // Handle back button for profile overlays, or navigate back from profile
    BackHandler(
        enabled = uiState.isEditingProfile ||
                showAddProject || editingProject != null || projectDetailProject != null ||
                showAddExperience || editingExperience != null ||
                showAddEducation || editingEducation != null ||
                showAddCertificate || editingCertificate != null || viewingCertificate != null ||
                showAddAchievement || editingAchievement != null || viewingAchievement != null ||
                showAddSkill ||
                true // Always enabled to handle back from profile screen
    ) {
        when {
            // Close any open dialogs/overlays first
            uiState.isEditingProfile -> screenViewModel.cancelEditingProfile()
            showAddProject -> showAddProject = false
            editingProject != null -> editingProject = null
            projectDetailProject != null -> dismissProjectDetail()
            showAddExperience -> showAddExperience = false
            editingExperience != null -> editingExperience = null
            showAddEducation -> showAddEducation = false
            editingEducation != null -> editingEducation = null
            showAddCertificate -> showAddCertificate = false
            editingCertificate != null -> editingCertificate = null
            viewingCertificate != null -> viewingCertificate = null
            showAddAchievement -> showAddAchievement = false
            editingAchievement != null -> editingAchievement = null
            viewingAchievement != null -> viewingAchievement = null
            showAddSkill -> showAddSkill = false
            // If no overlays open, navigate back from profile
            else -> onNavigateBack()
        }
    }
    
    LaunchedEffect(userId) {
        screenViewModel.loadProfile(userId)
    }
    
    Box(Modifier.fillMaxSize()) {
        // Prefer cached profile while refreshing (stale-while-revalidate). Checking isLoading first
        // would flash the skeleton on every tab revisit because loadProfile(forceRefresh) sets loading.
        when {
            uiState.profile != null -> {
                ProfileContent(
                    uiState = uiState,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    isGlassTheme = isGlassTheme,
                    isDarkTheme = isDarkTheme,
                    profileThemeOverride = profileThemePreference,
                    showOwnerProfileLocation = showProfileLocation,
                    onEditProfile = { screenViewModel.startEditingProfile() },
                    onConnect = { screenViewModel.sendConnectionRequest() },
                    onCancelRequest = { screenViewModel.cancelConnectionRequest() },
                    onAcceptRequest = { screenViewModel.acceptConnectionRequest() },
                    onRejectRequest = { screenViewModel.rejectConnectionRequest() },
                    onRemoveConnection = { screenViewModel.removeConnection() },
                    onToggleFollow = { screenViewModel.toggleFollow() },
                    onToggleProfileSave = { screenViewModel.toggleProfileSave() },
                    onFilterChange = { screenViewModel.setFeedFilter(it) },
                    onLoadMore = { screenViewModel.loadMoreFeed() },
                    onYearChange = { screenViewModel.loadActivityForYear(it) },
                    onEditBio = { screenViewModel.startEditingBio() },
                    onSaveBio = { screenViewModel.saveEditedBio() },
                    onCancelEditBio = { screenViewModel.cancelEditingBio() },
                    onBioChange = { screenViewModel.updateEditedBio(it) },
                    onToggleOpenToWork = { screenViewModel.updateOpenToOpportunities(it) },
                    onOpenConnections = { screenViewModel.openConnectionsSheet() },
                    onOpenFollowers = { screenViewModel.openFollowersSheet() },
                    onMessage = onMessage,
                    showStartConversation = showStartConversation,
                    isPreparingConversationStarter = isPreparingConversationStarter,
                    onStartConversation = onStartConversation,
                    onOpenProfile = onOpenProfile,
                    onOpenFeedItem = onOpenFeedItem,
                    onVotePoll = { postId, optionId -> screenViewModel.votePoll(postId, optionId) },
                    onUploadAvatar = { screenViewModel.uploadAvatar(it) },
                    onUploadBanner = { screenViewModel.uploadBanner(it) },
                    onReportUser = { showReportProfileDialog = true },
                    onBlockUser = { blockProfileUser() },
                    onConnectGitHub = {
                        screenViewModel.startGitHubOAuth { authUrl ->
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)))
                            }.onFailure {
                                screenViewModel.reportGitHubBrowserOpenFailure(
                                    "Couldn't open GitHub authorization"
                                )
                            }
                        }
                    },
                    onSyncGitHub = { screenViewModel.syncGitHubStats() },
                    onDisconnectGitHub = { screenViewModel.disconnectGitHub() },
                    // Project callbacks
                    onAddProject = { showAddProject = true },
                    onEditProject = { editingProject = it },
                    onViewProject = {
                        projectDetailProject = it
                        projectDetailVisible = true
                    },
                    onToggleProjectFeatured = { screenViewModel.toggleProjectFeatured(it.id) },
                    // Experience callbacks
                    onAddExperience = { showAddExperience = true },
                    onEditExperience = { editingExperience = it },
                    onViewExperience = { /* Experiences view in place */ },
                    // Education callbacks
                    onAddEducation = { showAddEducation = true },
                    onEditEducation = { editingEducation = it },
                    onViewEducation = { /* Education view in place */ },
                    // Certificate callbacks
                    onAddCertificate = { showAddCertificate = true },
                    onEditCertificate = { editingCertificate = it },
                    onViewCertificate = { viewingCertificate = it },
                    // Achievement callbacks
                    onAddAchievement = { showAddAchievement = true },
                    onEditAchievement = { editingAchievement = it },
                    onViewAchievement = { viewingAchievement = it },
                    // Skills callbacks
                    onAddSkill = { showAddSkill = true },
                    onRemoveSkill = { skill -> screenViewModel.removeLocalSkill(skill.id) },
                    onDeleteFeedPost = { postId ->
                        screenViewModel.deleteFeedPost(
                            postId = postId,
                            onSuccess = {},
                            onError = { }
                        )
                    }
                )
            }
            uiState.error != null -> {
                ProfileError(
                    error = uiState.error!!,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onRetry = { screenViewModel.retry() }
                )
            }
            uiState.isLoading -> {
                ProfileLoadingContent(
                    contentColor = contentColor,
                    accentColor = accentColor,
                    visitLoaderGiftId = uiState.visitLoaderGiftIdHint,
                    reduceAnimations = reduceAnimations
                )
            }
            else -> {
                ProfileLoadingContent(
                    contentColor = contentColor,
                    accentColor = accentColor,
                    visitLoaderGiftId = uiState.visitLoaderGiftIdHint,
                    reduceAnimations = reduceAnimations
                )
            }
        }

        if (uiState.peopleSheet.isVisible) {
            ProfilePeopleSheetDialog(
                sheetState = uiState.peopleSheet,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                isGlassTheme = isGlassTheme,
                isDarkTheme = isDarkTheme,
                onDismiss = { screenViewModel.dismissPeopleSheet() },
                onRetry = { screenViewModel.retryPeopleSheet() },
                onLoadMore = { screenViewModel.loadMorePeopleSheet() },
                onPersonClick = onOpenProfile?.let { callback ->
                    { personId ->
                        screenViewModel.dismissPeopleSheet()
                        callback(personId)
                    }
                }
            )
        }

        if (showReportProfileDialog) {
            SafetyReportDialog(
                title = "Report profile",
                subtitle = "Tell Trust & Safety what is wrong with this profile.",
                contentColor = contentColor,
                accentColor = accentColor,
                blockLabel = "Also block this user",
                isSubmitting = isSubmittingProfileReport,
                onDismiss = {
                    if (!isSubmittingProfileReport) showReportProfileDialog = false
                },
                onSubmit = ::submitProfileReport
            )
        }

        val profileEditDraft = uiState.profileEditDraft
        if (uiState.isEditingProfile && profileEditDraft != null) {
            EditProfileScreen(
                draft = profileEditDraft,
                isSaving = uiState.isSavingProfile,
                error = uiState.profileEditError,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                isGameProfileTheme = isGameProfileTheme,
                onDraftChange = { screenViewModel.updateProfileEditDraft { _ -> it } },
                onDraftTransform = { transform -> screenViewModel.updateProfileEditDraft(transform) },
                onDismiss = { screenViewModel.cancelEditingProfile() },
                onSave = { screenViewModel.saveProfileEdits() }
            )
        }
        
        // Add Project Screen
        if (showAddProject) {
            AddEditProjectScreen(
                project = null,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                isGameProfileTheme = isGameProfileTheme,
                onSave = { showAddProject = false },
                onDelete = null,
                onCancel = { showAddProject = false }
            )
        }
        
        // Edit Project Screen
        editingProject?.let { project ->
            AddEditProjectScreen(
                project = project,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                isGameProfileTheme = isGameProfileTheme,
                onSave = { editingProject = null },
                onDelete = {
                    screenViewModel.deleteProject(
                        projectId = project.id,
                        onSuccess = { editingProject = null },
                        onError = { /* Show error toast */ }
                    )
                },
                onCancel = { editingProject = null }
            )
        }
        
        AnimatedVisibility(
            visible = projectDetailProject != null && projectDetailVisible,
            enter = fadeIn(animationSpec = tween(180)) + scaleIn(
                initialScale = 0.94f,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            ),
            exit = fadeOut(animationSpec = tween(180)) + scaleOut(
                targetScale = 0.98f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            )
        ) {
            projectDetailProject?.let { project ->
                val orderedProjects = remember(uiState.profile?.projects) {
                    uiState.profile?.projects.orEmpty().sortedWith(
                        compareByDescending<Project> { it.featured }
                            .thenByDescending { it.isCurrent }
                            .thenByDescending { it.startDate }
                    )
                }.ifEmpty { listOf(project) }
                val initialProjectPage = orderedProjects.indexOfFirst { it.id == project.id }.coerceAtLeast(0)

                key(project.id, orderedProjects.size) {
                    val projectPagerState = rememberPagerState(
                        initialPage = initialProjectPage,
                        pageCount = { orderedProjects.size }
                    )

                    HorizontalPager(
                        state = projectPagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val pageProject = orderedProjects[page]
                        ProjectDetailScreen(
                            project = pageProject,
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            isGameProfileTheme = isGameProfileTheme,
                            isOwner = uiState.isOwner,
                            onEdit = {
                                dismissProjectDetail {
                                    editingProject = pageProject
                                }
                            },
                            onBack = { dismissProjectDetail() }
                        )
                    }
                }
            }
        }
        // Add Experience Screen
        if (showAddExperience) {
            AddEditExperienceScreen(
                experience = null,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onSave = { savedExperience ->
                    screenViewModel.addExperience(savedExperience)
                    showAddExperience = false
                },
                onDelete = null,
                onCancel = { showAddExperience = false }
            )
        }
        
        // Add Skill Screen
        if (showAddSkill) {
            AddSkillDialog(
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onSave = { name, proficiency ->
                    screenViewModel.addLocalSkill(name = name, proficiency = proficiency)
                    showAddSkill = false
                },
                onCancel = { showAddSkill = false }
            )
        }
        
        // Edit Experience Screen
        editingExperience?.let { experience ->
            AddEditExperienceScreen(
                experience = experience,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onSave = { savedExperience ->
                    screenViewModel.updateExperience(savedExperience)
                    editingExperience = null
                },
                onDelete = {
                    screenViewModel.deleteExperience(
                        experienceId = experience.id,
                        onSuccess = { editingExperience = null },
                        onError = { /* Show error toast */ }
                    )
                },
                onCancel = { editingExperience = null }
            )
        }
        
        // Add Education Screen
        if (showAddEducation) {
            AddEditEducationScreen(
                education = null,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onSave = { savedEducation ->
                    screenViewModel.addEducation(savedEducation)
                    showAddEducation = false
                },
                onDelete = null,
                onCancel = { showAddEducation = false }
            )
        }
        
        // Edit Education Screen
        editingEducation?.let { education ->
            AddEditEducationScreen(
                education = education,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onSave = { savedEducation ->
                    screenViewModel.updateEducation(savedEducation)
                    editingEducation = null
                },
                onDelete = {
                    screenViewModel.deleteEducation(
                        educationId = education.id,
                        onSuccess = { editingEducation = null },
                        onError = { /* Show error toast */ }
                    )
                },
                onCancel = { editingEducation = null }
            )
        }
        
        // Add Certificate Screen
        if (showAddCertificate) {
            AddEditCertificateScreen(
                certificate = null,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onSave = { savedCertificate ->
                    screenViewModel.addCertificate(savedCertificate)
                    showAddCertificate = false
                },
                onDelete = null,
                onCancel = { showAddCertificate = false }
            )
        }
        
        // Edit Certificate Screen
        editingCertificate?.let { certificate ->
            AddEditCertificateScreen(
                certificate = certificate,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onSave = { savedCertificate ->
                    screenViewModel.updateCertificate(savedCertificate)
                    editingCertificate = null
                },
                onDelete = {
                    screenViewModel.deleteCertificate(
                        certificateId = certificate.id,
                        onSuccess = { editingCertificate = null },
                        onError = { /* Show error toast */ }
                    )
                },
                onCancel = { editingCertificate = null }
            )
        }
        
        // View Certificate Detail Modal
        viewingCertificate?.let { certificate ->
            CertificateDetailModal(
                certificate = certificate,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                certificates = uiState.profile?.certificates.orEmpty(),
                onDismiss = { viewingCertificate = null }
            )
        }
        
        // Add Achievement Screen
        if (showAddAchievement) {
            AddEditAchievementScreen(
                achievement = null,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onSave = { savedAchievement ->
                    screenViewModel.addAchievement(savedAchievement)
                    showAddAchievement = false
                },
                onDelete = null,
                onCancel = { showAddAchievement = false }
            )
        }
        
        // Edit Achievement Screen
        editingAchievement?.let { achievement ->
            AddEditAchievementScreen(
                achievement = achievement,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onSave = { savedAchievement ->
                    screenViewModel.updateAchievement(savedAchievement)
                    editingAchievement = null
                },
                onDelete = {
                    screenViewModel.deleteAchievement(
                        achievementId = achievement.id,
                        onSuccess = { editingAchievement = null },
                        onError = { /* Show error toast */ }
                    )
                },
                onCancel = { editingAchievement = null }
            )
        }
        
        // View Achievement Detail Modal
        viewingAchievement?.let { achievement ->
            AchievementDetailModal(
                achievement = achievement,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                achievements = uiState.profile?.achievements.orEmpty(),
                onDismiss = { viewingAchievement = null }
            )
        }
    }
}

@Composable
private fun AddSkillDialog(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onSave: (String, String?) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var proficiency by remember { mutableStateOf("") }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable { onCancel() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(20f.dp) },
                    effects = {
                        vibrancy()
                        blur(16f.dp.toPx())
                    },
                    onDrawSurface = { drawRect(Color.White.copy(alpha = 0.14f)) }
                )
                .padding(20.dp)
                .clickable(enabled = false) { }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BasicText(
                    "Add skill",
                    style = TextStyle(contentColor, 18.sp, FontWeight.SemiBold)
                )

                // Skill name
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    textStyle = TextStyle(contentColor, 14.sp),
                    cursorBrush = SolidColor(accentColor),
                    singleLine = true,
                    decorationBox = { inner ->
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(contentColor.copy(alpha = 0.06f))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            if (name.isEmpty()) {
                                BasicText(
                                    "Skill name (e.g. Kotlin, UI/UX)",
                                    style = TextStyle(contentColor.copy(alpha = 0.4f), 14.sp)
                                )
                            }
                            inner()
                        }
                    }
                )

                // Proficiency (optional)
                BasicTextField(
                    value = proficiency,
                    onValueChange = { proficiency = it },
                    textStyle = TextStyle(contentColor, 14.sp),
                    cursorBrush = SolidColor(accentColor),
                    singleLine = true,
                    decorationBox = { inner ->
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(contentColor.copy(alpha = 0.06f))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            if (proficiency.isEmpty()) {
                                BasicText(
                                    "Proficiency (e.g. Beginner, Intermediate, Expert)",
                                    style = TextStyle(contentColor.copy(alpha = 0.4f), 14.sp)
                                )
                            }
                            inner()
                        }
                    }
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    BasicText(
                        "Cancel",
                        style = TextStyle(contentColor.copy(alpha = 0.7f), 14.sp),
                        modifier = Modifier
                            .clickable { onCancel() }
                            .padding(8.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (name.isNotBlank()) accentColor
                                else accentColor.copy(alpha = 0.4f)
                            )
                            .clickable(enabled = name.isNotBlank()) {
                                onSave(name, proficiency.takeIf { it.isNotBlank() })
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        BasicText(
                            "Save",
                            style = TextStyle(Color.White, 14.sp, FontWeight.Medium)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditProfileScreen(
    draft: ProfileEditDraft,
    isSaving: Boolean,
    error: String?,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGameProfileTheme: Boolean = false,
    onDraftChange: (ProfileEditDraft) -> Unit,
    onDraftTransform: ((ProfileEditDraft) -> ProfileEditDraft) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val appearance = currentVormexAppearance()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isResolvingDeviceLocation by remember { mutableStateOf(false) }
    var deviceLocationMessage by remember { mutableStateOf<String?>(null) }
    var deviceLocationError by remember { mutableStateOf<String?>(null) }

    fun applyDeviceLocation() {
        if (isResolvingDeviceLocation) return
        deviceLocationMessage = null
        deviceLocationError = null
        isResolvingDeviceLocation = true
        scope.launch {
            getProfileLocationLabelFromDevice(context)
                .onSuccess { label ->
                    onDraftTransform { current -> current.copy(location = label) }
                    deviceLocationMessage = "Location updated from device"
                }
                .onFailure { failure ->
                    deviceLocationError = failure.message ?: "Couldn't get device location"
                }
            isResolvingDeviceLocation = false
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            hasProfileLocationPermission(context)
        if (granted) {
            applyDeviceLocation()
        } else {
            deviceLocationMessage = null
            deviceLocationError = "Location permission denied"
        }
    }

    fun requestDeviceLocation() {
        if (hasProfileLocationPermission(context)) {
            applyDeviceLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isGameProfileTheme) {
                    Modifier.background(RetroGameCream)
                } else if (appearance.isGlassTheme) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(0f.dp) },
                        effects = {
                            vibrancy()
                            blur(22f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(Color.Black.copy(alpha = if (appearance.isDarkTheme) 0.42f else 0.24f))
                        }
                    )
                } else {
                    Modifier.background(appearance.backgroundColor)
                }
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isGameProfileTheme) {
                            RetroGameCream
                        } else if (appearance.isGlassTheme) {
                            Color.Black.copy(alpha = 0.14f)
                        } else {
                            appearance.navigationColor
                        }
                    )
                    .border(
                        if (isGameProfileTheme) 2.dp else 1.dp,
                        if (isGameProfileTheme) RetroGameInk else appearance.navigationBorderColor,
                        RoundedCornerShape(0.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (isGameProfileTheme) {
                    RetroGameMiniButton(
                        label = "CANCEL",
                        background = Color.White,
                        onClick = onDismiss
                    )
                } else {
                    BasicText(
                        "Cancel",
                        style = TextStyle(
                            contentColor.copy(alpha = if (isSaving) 0.35f else 0.76f),
                            14.sp,
                            FontWeight.Medium
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = !isSaving, onClick = onDismiss)
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                }

                BasicText(
                    if (isGameProfileTheme) "EDIT / PROFILE" else "Edit Profile",
                    style = TextStyle(
                        if (isGameProfileTheme) RetroGameInk else contentColor,
                        if (isGameProfileTheme) 14.sp else 18.sp,
                        FontWeight.Black,
                        letterSpacing = if (isGameProfileTheme) 1.sp else 0.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (isGameProfileTheme) {
                    RetroGameMiniButton(
                        label = if (isSaving) "SAVING" else "SAVE",
                        background = if (isSaving) RetroGameRed.copy(alpha = 0.62f) else RetroGameRed,
                        content = Color.White,
                        onClick = onSave
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSaving) accentColor.copy(alpha = 0.62f) else accentColor)
                            .clickable(enabled = !isSaving, onClick = onSave)
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            }
                            BasicText(
                                if (isSaving) "Saving" else "Save",
                                style = TextStyle(Color.White, 14.sp, FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 112.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                error?.let {
                    item {
                        BasicText(
                            it,
                            style = TextStyle(Color(0xFFEF4444), 12.sp, FontWeight.Medium)
                        )
                    }
                }

                item {
                    ProfileEditTextField(
                        label = "Name",
                        value = draft.name,
                        placeholder = "Your full name",
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isGameProfileTheme = isGameProfileTheme,
                        onValueChange = { onDraftChange(draft.copy(name = it)) }
                    )
                }
                item {
                    ProfileEditTextField(
                        label = "Headline",
                        value = draft.headline,
                        placeholder = "Student, Android developer, founder...",
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isGameProfileTheme = isGameProfileTheme,
                        onValueChange = { onDraftChange(draft.copy(headline = it)) }
                    )
                }
                item {
                    ProfileEditOpenToWorkRow(
                        checked = draft.isOpenToOpportunities,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isGameProfileTheme = isGameProfileTheme,
                        onToggle = {
                            onDraftChange(
                                draft.copy(isOpenToOpportunities = !draft.isOpenToOpportunities)
                            )
                        }
                    )
                }
                item {
                    ProfileEditTextField(
                        label = "About",
                        value = draft.bio,
                        placeholder = "Tell people what you are building, learning, or looking for",
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isGameProfileTheme = isGameProfileTheme,
                        singleLine = false,
                        minHeight = 116.dp,
                        onValueChange = { onDraftChange(draft.copy(bio = it)) }
                    )
                }
                item {
                    ProfileEditLocationField(
                        value = draft.location,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isGameProfileTheme = isGameProfileTheme,
                        isResolvingDeviceLocation = isResolvingDeviceLocation,
                        deviceLocationMessage = deviceLocationMessage,
                        deviceLocationError = deviceLocationError,
                        onUseDeviceLocation = ::requestDeviceLocation,
                        onValueChange = {
                            deviceLocationMessage = null
                            deviceLocationError = null
                            onDraftChange(draft.copy(location = it))
                        }
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ProfileEditTextField(
                            label = "College",
                            value = draft.college,
                            placeholder = "College name",
                            contentColor = contentColor,
                            accentColor = accentColor,
                            isGameProfileTheme = isGameProfileTheme,
                            modifier = Modifier.weight(1f),
                            onValueChange = { onDraftChange(draft.copy(college = it)) }
                        )
                        ProfileEditTextField(
                            label = "Branch",
                            value = draft.branch,
                            placeholder = "CSE, ECE...",
                            contentColor = contentColor,
                            accentColor = accentColor,
                            isGameProfileTheme = isGameProfileTheme,
                            modifier = Modifier.weight(1f),
                            onValueChange = { onDraftChange(draft.copy(branch = it)) }
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ProfileEditTextField(
                            label = "Degree",
                            value = draft.degree,
                            placeholder = "B.Tech",
                            contentColor = contentColor,
                            accentColor = accentColor,
                            isGameProfileTheme = isGameProfileTheme,
                            modifier = Modifier.weight(1f),
                            onValueChange = { onDraftChange(draft.copy(degree = it)) }
                        )
                        ProfileEditTextField(
                            label = "Current Year",
                            value = draft.currentYear,
                            placeholder = "1-5",
                            contentColor = contentColor,
                            accentColor = accentColor,
                            isGameProfileTheme = isGameProfileTheme,
                            modifier = Modifier.weight(1f),
                            onValueChange = { value ->
                                onDraftChange(
                                    draft.copy(currentYear = value.filter { it.isDigit() }.take(1))
                                )
                            }
                        )
                    }
                }
                item {
                    ProfileEditTextField(
                        label = "Graduation Year",
                        value = draft.graduationYear,
                        placeholder = "2027",
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isGameProfileTheme = isGameProfileTheme,
                        onValueChange = { value ->
                            onDraftChange(
                                draft.copy(graduationYear = value.filter { it.isDigit() }.take(4))
                            )
                        }
                    )
                }
                item {
                    ProfileEditTextField(
                        label = "LinkedIn",
                        value = draft.linkedinUrl,
                        placeholder = "https://linkedin.com/in/username",
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isGameProfileTheme = isGameProfileTheme,
                        onValueChange = { onDraftChange(draft.copy(linkedinUrl = it)) }
                    )
                }
                item {
                    ProfileEditTextField(
                        label = "GitHub",
                        value = draft.githubProfileUrl,
                        placeholder = "https://github.com/username",
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isGameProfileTheme = isGameProfileTheme,
                        onValueChange = { onDraftChange(draft.copy(githubProfileUrl = it)) }
                    )
                }
                item {
                    ProfileEditTextField(
                        label = "Portfolio",
                        value = draft.portfolioUrl,
                        placeholder = "https://your-site.com",
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isGameProfileTheme = isGameProfileTheme,
                        onValueChange = { onDraftChange(draft.copy(portfolioUrl = it)) }
                    )
                }
                item {
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfileEditLocationField(
    value: String,
    contentColor: Color,
    accentColor: Color,
    isGameProfileTheme: Boolean = false,
    isResolvingDeviceLocation: Boolean,
    deviceLocationMessage: String?,
    deviceLocationError: String?,
    onUseDeviceLocation: () -> Unit,
    onValueChange: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                if (isGameProfileTheme) "// LOCATION" else "Location",
                style = TextStyle(
                    if (isGameProfileTheme) RetroGameInk else contentColor.copy(alpha = 0.62f),
                    12.sp,
                    if (isGameProfileTheme) FontWeight.Black else FontWeight.SemiBold,
                    letterSpacing = if (isGameProfileTheme) 0.8.sp else 0.sp
                )
            )
            Row(
                modifier = Modifier
                    .clip(if (isGameProfileTheme) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp))
                    .background(
                        if (isGameProfileTheme) RetroGameYellow
                        else accentColor.copy(alpha = if (isResolvingDeviceLocation) 0.12f else 0.18f)
                    )
                    .border(
                        if (isGameProfileTheme) 2.dp else 0.dp,
                        if (isGameProfileTheme) RetroGameInk else Color.Transparent,
                        RoundedCornerShape(0.dp)
                    )
                    .clickable(enabled = !isResolvingDeviceLocation, onClick = onUseDeviceLocation)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isResolvingDeviceLocation) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(13.dp),
                        color = if (isGameProfileTheme) RetroGameInk else accentColor,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_location),
                        contentDescription = null,
                        tint = if (isGameProfileTheme) RetroGameInk else accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
                BasicText(
                    if (isResolvingDeviceLocation) "Locating" else "Use device",
                    style = TextStyle(
                        if (isGameProfileTheme) RetroGameInk else accentColor,
                        12.sp,
                        if (isGameProfileTheme) FontWeight.Black else FontWeight.SemiBold
                    )
                )
            }
        }

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(if (isGameProfileTheme) RetroGameInk else contentColor, 14.sp),
            cursorBrush = SolidColor(if (isGameProfileTheme) RetroGameRed else accentColor),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(if (isGameProfileTheme) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp))
                        .background(if (isGameProfileTheme) Color.White.copy(alpha = 0.72f) else contentColor.copy(alpha = 0.06f))
                        .border(
                            if (isGameProfileTheme) 2.dp else 1.dp,
                            if (isGameProfileTheme) RetroGameInk else contentColor.copy(alpha = 0.10f),
                            if (isGameProfileTheme) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        BasicText(
                            "City, State",
                            style = TextStyle(
                                if (isGameProfileTheme) RetroGameInk.copy(alpha = 0.42f) else contentColor.copy(alpha = 0.36f),
                                14.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    inner()
                }
            }
        )

        deviceLocationError?.let {
            BasicText(
                it,
                style = TextStyle(Color(0xFFEF4444), 12.sp, FontWeight.Medium)
            )
        } ?: deviceLocationMessage?.let {
            BasicText(
                it,
                style = TextStyle(
                    if (isGameProfileTheme) RetroGameInk.copy(alpha = 0.62f) else contentColor.copy(alpha = 0.56f),
                    12.sp,
                    FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun ProfileEditTextField(
    label: String,
    value: String,
    placeholder: String,
    contentColor: Color,
    accentColor: Color,
    isGameProfileTheme: Boolean = false,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minHeight: androidx.compose.ui.unit.Dp = 48.dp,
    onValueChange: (String) -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        BasicText(
            if (isGameProfileTheme) "// ${label.uppercase(Locale.US)}" else label,
            style = TextStyle(
                if (isGameProfileTheme) RetroGameInk else contentColor.copy(alpha = 0.62f),
                12.sp,
                if (isGameProfileTheme) FontWeight.Black else FontWeight.SemiBold,
                letterSpacing = if (isGameProfileTheme) 0.8.sp else 0.sp
            )
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(if (isGameProfileTheme) RetroGameInk else contentColor, 14.sp),
            cursorBrush = SolidColor(if (isGameProfileTheme) RetroGameRed else accentColor),
            singleLine = singleLine,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = minHeight)
                        .clip(if (isGameProfileTheme) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp))
                        .background(if (isGameProfileTheme) Color.White.copy(alpha = 0.72f) else contentColor.copy(alpha = 0.06f))
                        .border(
                            if (isGameProfileTheme) 2.dp else 1.dp,
                            if (isGameProfileTheme) RetroGameInk else contentColor.copy(alpha = 0.10f),
                            if (isGameProfileTheme) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart
                ) {
                    if (value.isEmpty()) {
                        BasicText(
                            placeholder,
                            style = TextStyle(
                                if (isGameProfileTheme) RetroGameInk.copy(alpha = 0.42f) else contentColor.copy(alpha = 0.36f),
                                14.sp
                            ),
                            maxLines = if (singleLine) 1 else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    inner()
                }
            }
        )
    }
}

@Composable
private fun ProfileEditOpenToWorkRow(
    checked: Boolean,
    contentColor: Color,
    accentColor: Color,
    isGameProfileTheme: Boolean = false,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(if (isGameProfileTheme) RoundedCornerShape(0.dp) else RoundedCornerShape(14.dp))
            .background(
                if (isGameProfileTheme && checked) RetroGameGreen
                else if (isGameProfileTheme) Color.White.copy(alpha = 0.72f)
                else if (checked) Color(0xFF22C55E).copy(alpha = 0.15f)
                else contentColor.copy(alpha = 0.06f)
            )
            .border(
                if (isGameProfileTheme) 2.dp else 0.dp,
                if (isGameProfileTheme) RetroGameInk else Color.Transparent,
                RoundedCornerShape(0.dp)
            )
            .clickable(onClick = onToggle)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BasicText(
                "Open to work",
                style = TextStyle(
                    if (isGameProfileTheme) RetroGameInk else contentColor,
                    14.sp,
                    if (isGameProfileTheme) FontWeight.Black else FontWeight.SemiBold
                )
            )
            BasicText(
                if (checked) "Shown as #OpenToWork on your profile" else "Hidden from your profile header",
                style = TextStyle(
                    if (isGameProfileTheme) RetroGameInk.copy(alpha = 0.68f) else contentColor.copy(alpha = 0.54f),
                    12.sp
                )
            )
        }
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(24.dp)
                .clip(if (isGameProfileTheme) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp))
                .background(
                    if (isGameProfileTheme && checked) RetroGameYellow
                    else if (isGameProfileTheme) Color.White
                    else if (checked) Color(0xFF22C55E)
                    else contentColor.copy(alpha = 0.16f)
                )
                .border(
                    if (isGameProfileTheme) 2.dp else 0.dp,
                    if (isGameProfileTheme) RetroGameInk else Color.Transparent,
                    RoundedCornerShape(0.dp)
                )
                .padding(3.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(if (isGameProfileTheme) RoundedCornerShape(0.dp) else CircleShape)
                    .background(
                        if (isGameProfileTheme && checked) RetroGameInk
                        else if (isGameProfileTheme) RetroGameRed
                        else if (checked) Color.White
                        else accentColor.copy(alpha = 0.72f)
                    )
            )
        }
    }
}

private fun hasProfileLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
}

private suspend fun getProfileLocationLabelFromDevice(context: Context): Result<String> {
    if (!hasProfileLocationPermission(context)) {
        return Result.failure(IllegalStateException("Location permission denied"))
    }

    val location = getProfileDeviceLocation(context)
        ?: return Result.failure(IllegalStateException("Couldn't get device location"))
    val label = reverseGeocodeProfileLocation(
        context = context,
        latitude = location.latitude,
        longitude = location.longitude
    )

    return if (label.isNullOrBlank()) {
        Result.failure(IllegalStateException("Couldn't resolve your city"))
    } else {
        Result.success(label)
    }
}

@SuppressLint("MissingPermission")
private suspend fun getProfileDeviceLocation(context: Context): Location? =
    suspendCancellableCoroutine { continuation ->
        val client = LocationServices.getFusedLocationProviderClient(context)
        var callback: LocationCallback? = null

        fun finish(location: Location?) {
            if (continuation.isActive) {
                continuation.resume(location)
            }
        }

        fun requestFreshLocation() {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4_000L)
                .setMaxUpdates(1)
                .build()
            val freshCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    client.removeLocationUpdates(this)
                    finish(result.lastLocation)
                }
            }
            callback = freshCallback
            runCatching {
                client.requestLocationUpdates(
                    request,
                    freshCallback,
                    Looper.getMainLooper()
                ).addOnFailureListener {
                    finish(null)
                }
            }.onFailure {
                finish(null)
            }
        }

        runCatching {
            client.lastLocation
                .addOnSuccessListener { lastLocation ->
                    if (lastLocation != null) {
                        finish(lastLocation)
                    } else {
                        requestFreshLocation()
                    }
                }
                .addOnFailureListener {
                    requestFreshLocation()
                }
        }.onFailure {
            finish(null)
        }

        continuation.invokeOnCancellation {
            callback?.let { client.removeLocationUpdates(it) }
        }
    }

private suspend fun reverseGeocodeProfileLocation(
    context: Context,
    latitude: Double,
    longitude: Double
): String? = withContext(Dispatchers.IO) {
    runCatching {
        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine<List<android.location.Address>?> { continuation ->
                geocoder.getFromLocation(latitude, longitude, 1) { result ->
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                }
            }
        } else {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(latitude, longitude, 1)
        }

        addresses
            ?.firstOrNull()
            ?.let { address ->
                listOfNotNull(
                    address.locality ?: address.subAdminArea ?: address.subLocality,
                    address.adminArea,
                    address.countryName
                )
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .joinToString(", ")
                    .takeIf { it.isNotBlank() }
            }
    }.getOrNull()
}

@Composable
private fun ProfileContent(
    uiState: ProfileUiState,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    isDarkTheme: Boolean,
    profileThemeOverride: String? = null,
    showOwnerProfileLocation: Boolean = true,
    onEditProfile: () -> Unit = {},
    onConnect: () -> Unit,
    onCancelRequest: () -> Unit,
    onAcceptRequest: () -> Unit,
    onRejectRequest: () -> Unit,
    onRemoveConnection: () -> Unit,
    onToggleFollow: () -> Unit,
    onToggleProfileSave: () -> Unit,
    onFilterChange: (String) -> Unit,
    onLoadMore: () -> Unit,
    onYearChange: (Int) -> Unit,
    onEditBio: () -> Unit,
    onSaveBio: () -> Unit,
    onCancelEditBio: () -> Unit,
    onBioChange: (String) -> Unit,
    onToggleOpenToWork: (Boolean) -> Unit,
    onOpenConnections: () -> Unit,
    onOpenFollowers: () -> Unit,
    onMessage: (String) -> Unit,
    showStartConversation: Boolean,
    isPreparingConversationStarter: Boolean,
    onStartConversation: (String) -> Unit,
    onOpenProfile: ((String) -> Unit)? = null,
    onOpenFeedItem: (FeedItem) -> Unit,
    onVotePoll: (String, String) -> Unit,
    onUploadAvatar: (ByteArray) -> Unit,
    onUploadBanner: (ByteArray) -> Unit,
    onReportUser: () -> Unit = {},
    onBlockUser: () -> Unit = {},
    onConnectGitHub: () -> Unit = {},
    onSyncGitHub: () -> Unit = {},
    onDisconnectGitHub: () -> Unit = {},
    // Project callbacks
    onAddProject: () -> Unit = {},
    onEditProject: (Project) -> Unit = {},
    onViewProject: (Project) -> Unit = {},
    onToggleProjectFeatured: (Project) -> Unit = {},
    // Experience callbacks
    onAddExperience: () -> Unit = {},
    onEditExperience: (Experience) -> Unit = {},
    onViewExperience: (Experience) -> Unit = {},
    // Education callbacks
    onAddEducation: () -> Unit = {},
    onEditEducation: (Education) -> Unit = {},
    onViewEducation: (Education) -> Unit = {},
    // Certificate callbacks
    onAddCertificate: () -> Unit = {},
    onEditCertificate: (Certificate) -> Unit = {},
    onViewCertificate: (Certificate) -> Unit = {},
    // Achievement callbacks
    onAddAchievement: () -> Unit = {},
    onEditAchievement: (Achievement) -> Unit = {},
    onViewAchievement: (Achievement) -> Unit = {},
    // Skills callbacks
    onAddSkill: () -> Unit = {},
    onRemoveSkill: (UserSkill) -> Unit = {},
    onDeleteFeedPost: (String) -> Unit = {}
) {
    val profile = uiState.profile!!
    val profileThemeKey = if (uiState.isOwner) {
        normalizeProfileThemeKey(profileThemeOverride ?: profile.user.profileTheme)
    } else {
        normalizeProfileThemeKey(profile.user.profileTheme)
    }
    val isGameProfileTheme = profileThemeKey == GameRetroProfileThemeKey
    val listState = rememberLazyListState()
    val isGitHubItemVisible by remember(listState, profile.github.connected, uiState.isOwner) {
        derivedStateOf {
            if (!(profile.github.connected || uiState.isOwner)) return@derivedStateOf false

            val layoutInfo = listState.layoutInfo
            val githubItem = layoutInfo.visibleItemsInfo.firstOrNull { it.key == ProfileGitHubSectionKey }
                ?: return@derivedStateOf false

            val viewportStart = layoutInfo.viewportStartOffset
            val viewportEnd = layoutInfo.viewportEndOffset
            val visibleTop = max(githubItem.offset, viewportStart)
            val visibleBottom = min(githubItem.offset + githubItem.size, viewportEnd)
            val visibleHeight = (visibleBottom - visibleTop).coerceAtLeast(0)
            val visibleRatio = visibleHeight.toFloat() / githubItem.size.coerceAtLeast(1)
            visibleRatio >= 0.22f
        }
    }
    var shouldAnimateGitHubSection by remember(
        profile.user.username,
        profile.github.username,
        profile.github.lastSyncedAt,
        profile.github.contributionCalendar?.totalContributions,
        uiState.isOwner
    ) { mutableStateOf(false) }

    LaunchedEffect(isGitHubItemVisible) {
        if (isGitHubItemVisible) {
            shouldAnimateGitHubSection = true
        }
    }
    
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .then(if (isGameProfileTheme) Modifier.background(RetroGameCream) else Modifier),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Header Section
        item {
            if (isGameProfileTheme) {
                RetroGameProfileThemeSection(
                    user = profile.user,
                    stats = profile.stats,
                    isOwner = uiState.isOwner,
                    showLocation = !uiState.isOwner || showOwnerProfileLocation,
                    showStartConversation = showStartConversation,
                    isPreparingConversationStarter = isPreparingConversationStarter,
                    onEditProfile = onEditProfile,
                    onMessage = onMessage,
                    onStartConversation = onStartConversation,
                    onOpenConnections = onOpenConnections,
                    onOpenFollowers = onOpenFollowers,
                    isProfileSaved = profile.viewerContext.isProfileSaved,
                    profileSaveActionInProgress = uiState.profileSaveActionInProgress,
                    onToggleProfileSave = onToggleProfileSave
                )
            } else {
                ProfileHeader(
                    user = profile.user,
                    stats = profile.stats,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    isGlassTheme = isGlassTheme,
                    isDarkTheme = isDarkTheme,
                    isOwner = uiState.isOwner,
                    showLocation = !uiState.isOwner || showOwnerProfileLocation,
                    connectionStatus = uiState.connectionStatus,
                    isFollowing = uiState.isFollowing,
                    isFollowedBy = uiState.isFollowedBy,
                    connectionActionInProgress = uiState.connectionActionInProgress,
                    followActionInProgress = uiState.followActionInProgress,
                    isProfileSaved = profile.viewerContext.isProfileSaved,
                    profileSaveActionInProgress = uiState.profileSaveActionInProgress,
                    mutualConnections = uiState.mutualConnections,
                    mutualConnectionsCount = uiState.mutualConnectionsCount,
                    isUploadingAvatar = uiState.isUploadingAvatar,
                    isUploadingBanner = uiState.isUploadingBanner,
                    onEditProfile = onEditProfile,
                    onConnect = onConnect,
                    onCancelRequest = onCancelRequest,
                    onAcceptRequest = onAcceptRequest,
                    onRejectRequest = onRejectRequest,
                    onRemoveConnection = onRemoveConnection,
                    onToggleFollow = onToggleFollow,
                    onToggleProfileSave = onToggleProfileSave,
                    onOpenConnections = onOpenConnections,
                    onOpenFollowers = onOpenFollowers,
                    onMessage = onMessage,
                    showStartConversation = showStartConversation,
                    isPreparingConversationStarter = isPreparingConversationStarter,
                    onStartConversation = onStartConversation,
                    onOpenMutualProfile = onOpenProfile,
                    onUploadAvatar = onUploadAvatar,
                    onUploadBanner = onUploadBanner,
                    onReportUser = onReportUser,
                    onBlockUser = onBlockUser
                )
            }
        }
        
        // About Section
        if (!isGameProfileTheme) item {
            Spacer(Modifier.height(12.dp))
            AboutSection(
                user = profile.user,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                isOwner = uiState.isOwner,
                isEditingBio = uiState.isEditingBio,
                editedBio = uiState.editedBio,
                isSavingBio = uiState.isSavingBio,
                bioEditError = uiState.bioEditError,
                onEditBio = onEditBio,
                onSaveBio = onSaveBio,
                onCancelEditBio = onCancelEditBio,
                onBioChange = onBioChange,
                onToggleOpenToWork = onToggleOpenToWork
            )
        }
        
        // GitHub Stats Section
        if (!isGameProfileTheme && (profile.github.connected || uiState.isOwner)) {
            item(key = ProfileGitHubSectionKey) {
                Spacer(Modifier.height(12.dp))
                GitHubSection(
                    github = profile.github,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    isOwner = uiState.isOwner,
                    isVisible = shouldAnimateGitHubSection,
                    isConnecting = uiState.isGitHubConnecting,
                    isSyncing = uiState.isGitHubSyncing,
                    isDisconnecting = uiState.isGitHubDisconnecting,
                    actionError = uiState.githubActionError,
                    onConnect = onConnectGitHub,
                    onSync = onSyncGitHub,
                    onDisconnect = onDisconnectGitHub
                )
            }
        }
        
        // Activity Calendar Section
        if (!isGameProfileTheme) item {
            Spacer(Modifier.height(12.dp))
            ActivityCalendarSection(
                heatmap = uiState.activityHeatmap,
                stats = profile.stats,
                availableYears = uiState.availableYears,
                selectedYear = uiState.selectedYear,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onYearChange = onYearChange
            )
        }
        
        // Skills Section
        if (profile.skills.isNotEmpty() || uiState.isOwner) {
            item {
                Spacer(Modifier.height(12.dp))
                if (isGameProfileTheme) {
                    RetroGameSkillsSection(
                        skills = profile.skills,
                        isOwner = uiState.isOwner,
                        onAddSkill = onAddSkill,
                        onRemoveSkill = onRemoveSkill
                    )
                } else {
                    SkillsSection(
                        skills = profile.skills,
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isOwner = uiState.isOwner,
                        onAddSkill = onAddSkill,
                        onRemoveSkill = onRemoveSkill
                    )
                }
            }
        }
        
        // Projects Section
        if (profile.projects.isNotEmpty() || uiState.isOwner) {
            item {
                Spacer(Modifier.height(12.dp))
                if (isGameProfileTheme) {
                    RetroGameProjectsSection(
                        projects = profile.projects,
                        isOwner = uiState.isOwner,
                        onAddProject = onAddProject,
                        onEditProject = onEditProject,
                        onViewProject = onViewProject,
                        onToggleFeatured = onToggleProjectFeatured
                    )
                } else {
                    ProjectsSection(
                        projects = profile.projects,
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isOwner = uiState.isOwner,
                        onAddProject = onAddProject,
                        onEditProject = onEditProject,
                        onViewProject = onViewProject,
                        onToggleFeatured = onToggleProjectFeatured
                    )
                }
            }
        }
        
        // Experience Section
        if (profile.experiences.isNotEmpty() || uiState.isOwner) {
            item {
                Spacer(Modifier.height(12.dp))
                if (isGameProfileTheme) {
                    RetroGameExperienceSection(
                        experiences = profile.experiences,
                        isOwner = uiState.isOwner,
                        onAddExperience = onAddExperience,
                        onEditExperience = onEditExperience,
                        onViewExperience = onViewExperience
                    )
                } else {
                    ExperienceSection(
                        experiences = profile.experiences,
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isOwner = uiState.isOwner,
                        onAddExperience = onAddExperience,
                        onEditExperience = onEditExperience,
                        onViewExperience = onViewExperience
                    )
                }
            }
        }
        
        // Education Section
        if (profile.education.isNotEmpty() || uiState.isOwner) {
            item {
                Spacer(Modifier.height(12.dp))
                if (isGameProfileTheme) {
                    RetroGameEducationSection(
                        education = profile.education,
                        isOwner = uiState.isOwner,
                        onAddEducation = onAddEducation,
                        onEditEducation = onEditEducation,
                        onViewEducation = onViewEducation
                    )
                } else {
                    EducationSection(
                        education = profile.education,
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isOwner = uiState.isOwner,
                        onAddEducation = onAddEducation,
                        onEditEducation = onEditEducation,
                        onViewEducation = onViewEducation
                    )
                }
            }
        }
        
        // Certificates Section
        if (profile.certificates.isNotEmpty() || uiState.isOwner) {
            item {
                Spacer(Modifier.height(12.dp))
                if (isGameProfileTheme) {
                    RetroGameCertificatesSection(
                        certificates = profile.certificates,
                        isOwner = uiState.isOwner,
                        onAddCertificate = onAddCertificate,
                        onEditCertificate = onEditCertificate,
                        onViewCertificate = onViewCertificate
                    )
                } else {
                    CertificatesSection(
                        certificates = profile.certificates,
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isOwner = uiState.isOwner,
                        onAddCertificate = onAddCertificate,
                        onEditCertificate = onEditCertificate,
                        onViewCertificate = onViewCertificate
                    )
                }
            }
        }
        
        // Achievements Section
        if (profile.achievements.isNotEmpty() || uiState.isOwner) {
            item {
                Spacer(Modifier.height(12.dp))
                if (isGameProfileTheme) {
                    RetroGameAchievementsSection(
                        achievements = profile.achievements,
                        isOwner = uiState.isOwner,
                        onAddAchievement = onAddAchievement,
                        onEditAchievement = onEditAchievement,
                        onViewAchievement = onViewAchievement
                    )
                } else {
                    AchievementsSection(
                        achievements = profile.achievements,
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isOwner = uiState.isOwner,
                        onAddAchievement = onAddAchievement,
                        onEditAchievement = onEditAchievement,
                        onViewAchievement = onViewAchievement
                    )
                }
            }
        }
        
        // Activity Feed Section
        item(key = "profile_activity_header", contentType = "profile_activity_header") {
            Spacer(Modifier.height(12.dp))
            if (isGameProfileTheme) {
                RetroGameActivityFeedHeaderSection(
                    currentFilter = uiState.feedFilter,
                    onFilterChange = onFilterChange
                )
            } else {
                ActivityFeedHeaderSection(
                    currentFilter = uiState.feedFilter,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onFilterChange = onFilterChange
                )
            }
        }

        item(key = "profile_activity_grid", contentType = "profile_activity_grid") {
            Spacer(Modifier.height(12.dp))
            if (isGameProfileTheme) {
                RetroGameActivityFeedGridSection(
                    items = uiState.feedItems,
                    isLoading = uiState.isLoadingFeed,
                    isOwner = uiState.isOwner,
                    onOpenItem = onOpenFeedItem,
                    onDeletePost = onDeleteFeedPost
                )
            } else {
                ActivityFeedGridSection(
                    items = uiState.feedItems,
                    isLoading = uiState.isLoadingFeed,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    isOwner = uiState.isOwner,
                    onOpenItem = onOpenFeedItem,
                    onDeletePost = onDeleteFeedPost
                )
            }
        }

        if (uiState.isLoadingFeed) {
            item(key = "profile_activity_loading", contentType = "profile_activity_status") {
                Spacer(Modifier.height(12.dp))
                if (isGameProfileTheme) {
                    RetroGameActivityFeedLoadingSection()
                } else {
                    ActivityFeedLoadingSection(
                        contentColor = contentColor,
                        accentColor = accentColor
                    )
                }
            }
        }

        if (uiState.feedHasMore && !uiState.isLoadingFeed) {
            item(key = "profile_activity_load_more", contentType = "profile_activity_status") {
                Spacer(Modifier.height(12.dp))
                if (isGameProfileTheme) {
                    RetroGameActivityFeedLoadMoreSection(onLoadMore = onLoadMore)
                } else {
                    ActivityFeedLoadMoreSection(
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onLoadMore = onLoadMore
                    )
                }
            }
        }
    }
}

// ==================== Retro Game Profile Theme ====================

@Composable
private fun RetroGameProfileThemeSection(
    user: ProfileUser,
    stats: ProfileStats,
    isOwner: Boolean,
    showLocation: Boolean,
    showStartConversation: Boolean,
    isPreparingConversationStarter: Boolean,
    onEditProfile: () -> Unit,
    onMessage: (String) -> Unit,
    onStartConversation: (String) -> Unit,
    onOpenConnections: () -> Unit,
    onOpenFollowers: () -> Unit,
    isProfileSaved: Boolean = false,
    profileSaveActionInProgress: Boolean = false,
    onToggleProfileSave: () -> Unit = {}
) {
    val context = LocalContext.current
    val profileUrl = "https://vormex.com/@${user.username}"
    val primaryLabel = when {
        isOwner -> "EDIT PROFILE ->"
        showStartConversation && isPreparingConversationStarter -> "STARTING..."
        showStartConversation -> "START CHAT ->"
        else -> "MESSAGE ->"
    }
    val headline = user.headline?.takeIf { it.isNotBlank() } ?: "Vormex profile player"
    val locationLine = listOfNotNull(
        if (showLocation) user.location?.takeIf { it.isNotBlank() }?.uppercase(Locale.US) else null,
        user.college?.takeIf { it.isNotBlank() }?.uppercase(Locale.US)
    ).joinToString("  ·  ").ifBlank { "VORMEX" }
    val xpTarget = (stats.xp + stats.xpToNextLevel).coerceAtLeast(1)
    val xpProgress = (stats.xp.toFloat() / xpTarget.toFloat()).coerceIn(0.08f, 1f)

    fun openUrl(url: String?) {
        if (url.isNullOrBlank()) return
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    fun shareProfile() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, profileUrl)
        }
        context.startActivity(Intent.createChooser(intent, "Share profile"))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RetroGameCream)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText("VORMEX", style = TextStyle(RetroGameInk, 16.sp, FontWeight.Black))
            BasicText(
                "PROFILE / 01",
                style = TextStyle(RetroGameInk, 11.sp, FontWeight.Black),
                modifier = Modifier
                    .border(2.dp, RetroGameInk, RoundedCornerShape(0.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .retroGamePanel(RetroGameYellow)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                val profileImageUrl = user.profileImageUrl()
                if (profileImageUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(profileImageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Profile photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    BasicText(
                        profileInitials(user.name),
                        style = TextStyle(RetroGameInk, 32.sp, FontWeight.Black)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                BasicText(
                    "// HUMAN_${user.id.take(3).uppercase(Locale.US)}",
                    style = TextStyle(RetroGameInk, 11.sp, FontWeight.Black, letterSpacing = 1.sp)
                )
                BasicText(
                    user.name.uppercase(Locale.US),
                    style = TextStyle(RetroGameInk, 22.sp, FontWeight.Black, lineHeight = 22.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (user.isOpenToOpportunities) {
                    BasicText(
                        "✓ OPEN TO WORK",
                        style = TextStyle(RetroGameInk, 10.sp, FontWeight.Black),
                        modifier = Modifier
                            .background(RetroGameGreen)
                            .border(2.dp, RetroGameInk, RoundedCornerShape(0.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                BasicText(
                    "@${user.username}",
                    style = TextStyle(RetroGameInk, 12.sp, FontWeight.Black),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, RetroGameInk, RoundedCornerShape(0.dp))
                .background(Color.White.copy(alpha = 0.70f))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BasicText(
                headline,
                style = TextStyle(RetroGameInk, 14.sp, FontWeight.Medium, lineHeight = 19.sp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            BasicText(
                "⌾ $locationLine",
                style = TextStyle(RetroGameInk, 11.sp, FontWeight.Black, letterSpacing = 0.8.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RetroGameButton(
                label = primaryLabel,
                background = RetroGameRed,
                content = Color.White,
                modifier = Modifier.weight(1f),
                onClick = {
                    when {
                        isOwner -> onEditProfile()
                        showStartConversation -> onStartConversation(user.id)
                        else -> onMessage(user.id)
                    }
                }
            )
            if (!isOwner) {
                RetroGameButton(
                    label = if (isProfileSaved) "SAVED" else "SAVE",
                    background = if (isProfileSaved) RetroGameYellow else Color.White,
                    content = RetroGameInk,
                    modifier = Modifier.width(76.dp),
                    onClick = {
                        if (!profileSaveActionInProgress) {
                            onToggleProfileSave()
                        }
                    }
                )
            }
            RetroGameButton(
                label = "SHARE",
                background = Color.White,
                content = RetroGameInk,
                modifier = Modifier.width(86.dp),
                onClick = ::shareProfile
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, RetroGameInk, RoundedCornerShape(0.dp))
                .background(Color.White.copy(alpha = 0.70f))
        ) {
            RetroGameStatCell(
                value = formatNumber(stats.connectionsCount),
                label = "CONNECTIONS",
                modifier = Modifier.weight(1f),
                onClick = onOpenConnections
            )
            RetroGameStatCell(
                value = formatNumber(stats.followersCount),
                label = "FOLLOWERS",
                background = RetroGameYellow,
                modifier = Modifier.weight(1f),
                onClick = onOpenFollowers
            )
            RetroGameStatCell(
                value = formatNumber(stats.totalPosts),
                label = "POSTS",
                modifier = Modifier.weight(1f)
            )
            RetroGameStatCell(
                value = formatNumber(stats.totalLikesReceived),
                label = "LIKES",
                modifier = Modifier.weight(1f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, RetroGameInk, RoundedCornerShape(0.dp))
                .background(RetroGameBlue)
        ) {
            BasicText(
                "// PROGRESSION",
                style = TextStyle(Color.White, 11.sp, FontWeight.Black, letterSpacing = 1.2.sp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
            )
            Row {
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(76.dp)
                        .background(RetroGameYellow)
                        .border(2.dp, RetroGameInk, RoundedCornerShape(0.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        BasicText("LVL", style = TextStyle(RetroGameInk, 10.sp, FontWeight.Black))
                        BasicText("${stats.level}", style = TextStyle(RetroGameInk, 34.sp, FontWeight.Black))
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        BasicText("XP ${formatNumber(stats.xp)}", style = TextStyle(Color.White, 11.sp, FontWeight.Black))
                        BasicText("+${formatNumber(stats.xpToNextLevel)}", style = TextStyle(Color.White, 11.sp, FontWeight.Black))
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .background(Color.White)
                            .border(2.dp, RetroGameInk, RoundedCornerShape(0.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(xpProgress)
                                .fillMaxHeight()
                                .background(RetroGameRed)
                        )
                    }
                    BasicText(
                        "STREAK · ${stats.currentStreak}D (${stats.longestStreak} BEST)",
                        style = TextStyle(Color.White, 11.sp, FontWeight.Black),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RetroGameLinkButton(
                label = "in LINKEDIN",
                url = user.linkedinUrl,
                modifier = Modifier.weight(1f),
                onClick = ::openUrl
            )
            RetroGameLinkButton(
                label = "<> GITHUB",
                url = user.githubProfileUrl,
                modifier = Modifier.weight(1f),
                onClick = ::openUrl
            )
            RetroGameLinkButton(
                label = "↗ PORTFOLIO",
                url = user.portfolioUrl,
                background = RetroGameYellow,
                modifier = Modifier.weight(1f),
                onClick = ::openUrl
            )
        }

        BasicText(
            "// ABOUT",
            style = TextStyle(RetroGameInk, 12.sp, FontWeight.Black, letterSpacing = 1.4.sp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, RetroGameInk, RoundedCornerShape(0.dp))
                .background(Color.White.copy(alpha = 0.70f))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BasicText(
                user.bio?.takeIf { it.isNotBlank() } ?: "No bio added yet.",
                style = TextStyle(RetroGameInk, 14.sp, FontWeight.Medium, lineHeight = 20.sp),
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )
            if (isOwner) {
                RetroGameButton(
                    label = "SHOW MORE ->",
                    background = RetroGameInk,
                    content = Color.White,
                    onClick = onEditProfile
                )
            }
        }
    }
}

@Composable
private fun RetroGameSection(
    title: String,
    count: Int? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .retroGamePanel(Color.White.copy(alpha = 0.72f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                buildString {
                    append("// ")
                    append(title.uppercase(Locale.US))
                    count?.let { append(" / $it") }
                },
                style = TextStyle(RetroGameInk, 12.sp, FontWeight.Black, letterSpacing = 1.2.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (actionLabel != null && onAction != null) {
                RetroGameMiniButton(
                    label = actionLabel,
                    background = RetroGameYellow,
                    onClick = onAction
                )
            }
        }
        content()
    }
}

@Composable
private fun RetroGameMiniButton(
    label: String,
    background: Color,
    modifier: Modifier = Modifier,
    content: Color = RetroGameInk,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(30.dp)
            .retroGamePanel(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            label,
            style = TextStyle(content, 10.sp, FontWeight.Black),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RetroGameEntryCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(if (highlighted) RetroGameYellow.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.70f))
            .border(2.dp, RetroGameInk, RoundedCornerShape(0.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}

@Composable
private fun RetroGameEmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, RetroGameInk, RoundedCornerShape(0.dp))
            .background(Color.White.copy(alpha = 0.58f))
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            message.uppercase(Locale.US),
            style = TextStyle(
                color = RetroGameInk.copy(alpha = 0.58f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
                textAlign = TextAlign.Center
            )
        )
    }
}

@Composable
private fun RetroGamePill(
    label: String,
    background: Color = Color.White,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .background(background)
            .border(2.dp, RetroGameInk, RoundedCornerShape(0.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 9.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            label.uppercase(Locale.US),
            style = TextStyle(RetroGameInk, 10.sp, FontWeight.Black, letterSpacing = 0.4.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RetroGameSkillsSection(
    skills: List<UserSkill>,
    isOwner: Boolean,
    onAddSkill: () -> Unit,
    onRemoveSkill: (UserSkill) -> Unit
) {
    RetroGameSection(
        title = "Skill Inventory",
        count = skills.size,
        actionLabel = if (isOwner) "+ ADD" else null,
        onAction = if (isOwner) onAddSkill else null
    ) {
        if (skills.isEmpty()) {
            RetroGameEmptyState("No skills equipped yet")
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                skills.forEach { userSkill ->
                    Row(
                        modifier = Modifier
                            .background(RetroGameBlue.copy(alpha = 0.12f))
                            .border(2.dp, RetroGameInk, RoundedCornerShape(0.dp))
                            .padding(start = 9.dp, end = if (isOwner) 5.dp else 9.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Column {
                            BasicText(
                                userSkill.skill.name.uppercase(Locale.US),
                                style = TextStyle(RetroGameInk, 10.sp, FontWeight.Black),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 150.dp)
                            )
                            val meta = listOfNotNull(
                                userSkill.proficiency?.takeIf { it.isNotBlank() },
                                userSkill.yearsOfExp?.let { "$it YRS" }
                            ).joinToString(" / ")
                            if (meta.isNotBlank()) {
                                BasicText(
                                    meta.uppercase(Locale.US),
                                    style = TextStyle(RetroGameInk.copy(alpha = 0.62f), 8.sp, FontWeight.Black),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (isOwner) {
                            BasicText(
                                "X",
                                style = TextStyle(RetroGameRed, 10.sp, FontWeight.Black),
                                modifier = Modifier
                                    .clickable { onRemoveSkill(userSkill) }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
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
private fun RetroGameProjectsSection(
    projects: List<Project>,
    isOwner: Boolean,
    onAddProject: () -> Unit,
    onEditProject: (Project) -> Unit,
    onViewProject: (Project) -> Unit,
    onToggleFeatured: (Project) -> Unit
) {
    RetroGameSection(
        title = "Project Quests",
        count = projects.size,
        actionLabel = if (isOwner) "+ ADD" else null,
        onAction = if (isOwner) onAddProject else null
    ) {
        if (projects.isEmpty()) {
            RetroGameEmptyState("No project quests posted")
        } else {
            val orderedProjects = remember(projects) {
                projects.sortedWith(
                    compareByDescending<Project> { it.featured }.thenByDescending { it.startDate }
                )
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(end = 22.dp)
            ) {
                items(orderedProjects.size) { index ->
                    val project = orderedProjects[index]
                    Box(modifier = Modifier.width(284.dp)) {
                        RetroGameEntryCard(
                            highlighted = project.featured,
                            onClick = { onViewProject(project) }
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    BasicText(
                                        "QUEST_${(index + 1).toString().padStart(2, '0')}",
                                        style = TextStyle(RetroGameInk.copy(alpha = 0.62f), 9.sp, FontWeight.Black, letterSpacing = 0.8.sp)
                                    )
                                    BasicText(
                                        project.name.uppercase(Locale.US),
                                        style = TextStyle(RetroGameInk, 16.sp, FontWeight.Black, lineHeight = 18.sp),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (project.featured) {
                                    RetroGamePill("Featured", background = RetroGameYellow)
                                }
                            }
                            project.role?.takeIf { it.isNotBlank() }?.let { role ->
                                BasicText(
                                    role,
                                    style = TextStyle(RetroGameInk, 12.sp, FontWeight.Black),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicText(
                                project.description,
                                style = TextStyle(RetroGameInk.copy(alpha = 0.82f), 13.sp, FontWeight.Medium, lineHeight = 18.sp),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            BasicText(
                                retroDateRange(project.startDate, project.endDate, project.isCurrent),
                                style = TextStyle(RetroGameInk.copy(alpha = 0.62f), 10.sp, FontWeight.Black)
                            )
                            if (project.techStack.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    project.techStack.take(6).forEach { tech ->
                                        RetroGamePill(tech, background = Color.White)
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                RetroGameMiniButton("VIEW", RetroGameBlue, content = Color.White) { onViewProject(project) }
                                if (isOwner) {
                                    RetroGameMiniButton("EDIT", Color.White) { onEditProject(project) }
                                    RetroGameMiniButton(
                                        if (project.featured) "UNPIN" else "PIN",
                                        RetroGameYellow
                                    ) { onToggleFeatured(project) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RetroGameExperienceSection(
    experiences: List<Experience>,
    isOwner: Boolean,
    onAddExperience: () -> Unit,
    onEditExperience: (Experience) -> Unit,
    onViewExperience: (Experience) -> Unit
) {
    RetroGameSection(
        title = "Career Missions",
        count = experiences.size,
        actionLabel = if (isOwner) "+ ADD" else null,
        onAction = if (isOwner) onAddExperience else null
    ) {
        if (experiences.isEmpty()) {
            RetroGameEmptyState("No missions unlocked")
        } else {
            experiences.forEachIndexed { index, experience ->
                RetroGameEntryCard(onClick = { onViewExperience(experience) }) {
                    BasicText(
                        "MISSION_${(index + 1).toString().padStart(2, '0')}",
                        style = TextStyle(RetroGameInk.copy(alpha = 0.62f), 9.sp, FontWeight.Black, letterSpacing = 0.8.sp)
                    )
                    BasicText(
                        experience.title.uppercase(Locale.US),
                        style = TextStyle(RetroGameInk, 16.sp, FontWeight.Black, lineHeight = 18.sp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    BasicText(
                        "${experience.company} / ${experience.type}",
                        style = TextStyle(RetroGameInk, 12.sp, FontWeight.Black),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    BasicText(
                        retroDateRange(experience.startDate, experience.endDate, experience.isCurrent),
                        style = TextStyle(RetroGameInk.copy(alpha = 0.62f), 10.sp, FontWeight.Black)
                    )
                    experience.description?.takeIf { it.isNotBlank() }?.let { description ->
                        BasicText(
                            description,
                            style = TextStyle(RetroGameInk.copy(alpha = 0.82f), 13.sp, FontWeight.Medium, lineHeight = 18.sp),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (experience.skills.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            experience.skills.take(6).forEach { skill ->
                                RetroGamePill(skill, background = RetroGameYellow.copy(alpha = 0.60f))
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RetroGameMiniButton("VIEW", RetroGameBlue, content = Color.White) { onViewExperience(experience) }
                        if (isOwner) {
                            RetroGameMiniButton("EDIT", Color.White) { onEditExperience(experience) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RetroGameEducationSection(
    education: List<Education>,
    isOwner: Boolean,
    onAddEducation: () -> Unit,
    onEditEducation: (Education) -> Unit,
    onViewEducation: (Education) -> Unit
) {
    RetroGameSection(
        title = "Academy Log",
        count = education.size,
        actionLabel = if (isOwner) "+ ADD" else null,
        onAction = if (isOwner) onAddEducation else null
    ) {
        if (education.isEmpty()) {
            RetroGameEmptyState("No academy records")
        } else {
            education.forEachIndexed { index, item ->
                RetroGameEntryCard(onClick = { onViewEducation(item) }) {
                    BasicText(
                        "CLASS_${(index + 1).toString().padStart(2, '0')}",
                        style = TextStyle(RetroGameInk.copy(alpha = 0.62f), 9.sp, FontWeight.Black, letterSpacing = 0.8.sp)
                    )
                    BasicText(
                        item.school.uppercase(Locale.US),
                        style = TextStyle(RetroGameInk, 16.sp, FontWeight.Black, lineHeight = 18.sp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    BasicText(
                        "${item.degree} / ${item.fieldOfStudy}",
                        style = TextStyle(RetroGameInk, 12.sp, FontWeight.Black),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    BasicText(
                        retroDateRange(item.startDate, item.endDate, item.isCurrent),
                        style = TextStyle(RetroGameInk.copy(alpha = 0.62f), 10.sp, FontWeight.Black)
                    )
                    item.description?.takeIf { it.isNotBlank() }?.let { description ->
                        BasicText(
                            description,
                            style = TextStyle(RetroGameInk.copy(alpha = 0.82f), 13.sp, FontWeight.Medium, lineHeight = 18.sp),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RetroGameMiniButton("VIEW", RetroGameBlue, content = Color.White) { onViewEducation(item) }
                        if (isOwner) {
                            RetroGameMiniButton("EDIT", Color.White) { onEditEducation(item) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RetroGameCertificatesSection(
    certificates: List<Certificate>,
    isOwner: Boolean,
    onAddCertificate: () -> Unit,
    onEditCertificate: (Certificate) -> Unit,
    onViewCertificate: (Certificate) -> Unit
) {
    RetroGameSection(
        title = "Power Badges",
        count = certificates.size,
        actionLabel = if (isOwner) "+ ADD" else null,
        onAction = if (isOwner) onAddCertificate else null
    ) {
        if (certificates.isEmpty()) {
            RetroGameEmptyState("No power badges collected")
        } else {
            certificates.forEachIndexed { index, certificate ->
                RetroGameEntryCard(onClick = { onViewCertificate(certificate) }) {
                    BasicText(
                        "BADGE_${(index + 1).toString().padStart(2, '0')}",
                        style = TextStyle(RetroGameInk.copy(alpha = 0.62f), 9.sp, FontWeight.Black, letterSpacing = 0.8.sp)
                    )
                    BasicText(
                        certificate.name.uppercase(Locale.US),
                        style = TextStyle(RetroGameInk, 16.sp, FontWeight.Black, lineHeight = 18.sp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    BasicText(
                        certificate.issuingOrg,
                        style = TextStyle(RetroGameInk, 12.sp, FontWeight.Black),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    BasicText(
                        "ISSUED ${retroCompactDate(certificate.issueDate)}",
                        style = TextStyle(RetroGameInk.copy(alpha = 0.62f), 10.sp, FontWeight.Black)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RetroGameMiniButton("VIEW", RetroGameBlue, content = Color.White) { onViewCertificate(certificate) }
                        if (isOwner) {
                            RetroGameMiniButton("EDIT", Color.White) { onEditCertificate(certificate) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RetroGameAchievementsSection(
    achievements: List<Achievement>,
    isOwner: Boolean,
    onAddAchievement: () -> Unit,
    onEditAchievement: (Achievement) -> Unit,
    onViewAchievement: (Achievement) -> Unit
) {
    RetroGameSection(
        title = "Trophy Shelf",
        count = achievements.size,
        actionLabel = if (isOwner) "+ ADD" else null,
        onAction = if (isOwner) onAddAchievement else null
    ) {
        if (achievements.isEmpty()) {
            RetroGameEmptyState("No trophies claimed")
        } else {
            achievements.forEachIndexed { index, achievement ->
                RetroGameEntryCard(onClick = { onViewAchievement(achievement) }) {
                    BasicText(
                        "TROPHY_${(index + 1).toString().padStart(2, '0')}",
                        style = TextStyle(RetroGameInk.copy(alpha = 0.62f), 9.sp, FontWeight.Black, letterSpacing = 0.8.sp)
                    )
                    BasicText(
                        achievement.title.uppercase(Locale.US),
                        style = TextStyle(RetroGameInk, 16.sp, FontWeight.Black, lineHeight = 18.sp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    BasicText(
                        "${achievement.organization} / ${achievement.type}",
                        style = TextStyle(RetroGameInk, 12.sp, FontWeight.Black),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    BasicText(
                        retroCompactDate(achievement.date),
                        style = TextStyle(RetroGameInk.copy(alpha = 0.62f), 10.sp, FontWeight.Black)
                    )
                    achievement.description?.takeIf { it.isNotBlank() }?.let { description ->
                        BasicText(
                            description,
                            style = TextStyle(RetroGameInk.copy(alpha = 0.82f), 13.sp, FontWeight.Medium, lineHeight = 18.sp),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RetroGameMiniButton("VIEW", RetroGameBlue, content = Color.White) { onViewAchievement(achievement) }
                        if (isOwner) {
                            RetroGameMiniButton("EDIT", Color.White) { onEditAchievement(achievement) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RetroGameActivityFeedHeaderSection(
    currentFilter: String,
    onFilterChange: (String) -> Unit
) {
    val filters = listOf(
        "all" to "All",
        "posts" to "Posts",
        "articles" to "Articles",
        "videos" to "Reels"
    )

    RetroGameSection(title = "Activity Grid") {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { (filter, label) ->
                val isSelected = currentFilter == filter
                RetroGamePill(
                    label = label,
                    background = if (isSelected) RetroGameYellow else Color.White,
                    onClick = { onFilterChange(filter) }
                )
            }
        }
    }
}

@Composable
private fun RetroGameActivityFeedGridSection(
    items: List<FeedItem>,
    isLoading: Boolean,
    isOwner: Boolean,
    onOpenItem: (FeedItem) -> Unit,
    onDeletePost: (String) -> Unit
) {
    val gridItems = remember(items) { items.mapNotNull(::retroGameActivityGridItemFor) }

    RetroGameSection(
        title = "Posts",
        count = gridItems.size
    ) {
        when {
            gridItems.isNotEmpty() -> {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    gridItems.chunked(3).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            rowItems.forEach { gridItem ->
                                RetroGameActivityGridTile(
                                    gridItem = gridItem,
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
            !isLoading -> RetroGameEmptyState("No image or video posts yet")
        }
    }
}

@Composable
private fun RetroGameActivityGridTile(
    gridItem: RetroGameActivityGridItem,
    isOwner: Boolean,
    onOpenItem: (FeedItem) -> Unit,
    onDeletePost: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val item = gridItem.item
    val canDelete = isOwner && item.entityType?.equals("post", ignoreCase = true) == true
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(Color.White.copy(alpha = 0.70f))
            .border(2.dp, RetroGameInk, RoundedCornerShape(0.dp))
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
            DefaultPostVideoPlayer(
                defaultVideoId = gridItem.defaultVideoId,
                modifier = Modifier.fillMaxSize(),
                reduceAnimations = true,
                accentColor = RetroGameRed,
                height = null,
                contentScale = ContentScale.Crop
            )
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.22f),
                        0.55f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.28f)
                    )
                )
        )

        if (gridItem.isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(42.dp)
                    .background(RetroGameRed)
                    .border(2.dp, RetroGameInk, RoundedCornerShape(0.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_play_arrow),
                    contentDescription = "Video",
                    modifier = Modifier.size(22.dp),
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }
        }

        if (gridItem.mediaCount > 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .background(RetroGameYellow)
                    .border(2.dp, RetroGameInk, RoundedCornerShape(0.dp))
                    .padding(5.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_image),
                    contentDescription = "Multiple media",
                    modifier = Modifier.size(14.dp),
                    colorFilter = ColorFilter.tint(RetroGameInk)
                )
            }
        }

        if (canDelete) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color.White)
                        .border(2.dp, RetroGameInk, RoundedCornerShape(0.dp))
                        .clickable { showMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_more),
                        contentDescription = "Post options",
                        modifier = Modifier.size(17.dp),
                        colorFilter = ColorFilter.tint(RetroGameInk)
                    )
                }

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
}

@Composable
private fun RetroGameActivityFeedLoadingSection() {
    RetroGameSection(title = "Loading") {
        Box(
            Modifier
                .fillMaxWidth()
                .border(2.dp, RetroGameInk, RoundedCornerShape(0.dp))
                .background(Color.White.copy(alpha = 0.70f))
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = RetroGameRed
            )
        }
    }
}

@Composable
private fun RetroGameActivityFeedLoadMoreSection(onLoadMore: () -> Unit) {
    RetroGameSection(title = "More Posts") {
        RetroGameButton(
            label = "LOAD MORE ->",
            background = RetroGameInk,
            content = Color.White,
            modifier = Modifier.fillMaxWidth(),
            onClick = onLoadMore
        )
    }
}

private data class RetroGameActivityGridItem(
    val item: FeedItem,
    val thumbnailUrl: String?,
    val mediaCount: Int,
    val isVideo: Boolean,
    val defaultVideoId: String?
)

private fun retroGameActivityGridItemFor(item: FeedItem): RetroGameActivityGridItem? {
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

    if (thumbnailUrl == null && !hasDefaultVideo) return null

    return RetroGameActivityGridItem(
        item = item,
        thumbnailUrl = thumbnailUrl,
        mediaCount = mediaItems.size.coerceAtLeast(if (isVideo || thumbnailUrl != null) 1 else 0),
        isVideo = isVideo,
        defaultVideoId = item.defaultVideoId?.takeIf { hasDefaultVideo }
    )
}

private fun retroCompactDate(value: String?): String =
    value
        ?.takeIf { it.isNotBlank() }
        ?.take(10)
        ?.replace("-", ".")
        ?: "NOW"

private fun retroDateRange(startDate: String?, endDate: String?, isCurrent: Boolean): String {
    val start = retroCompactDate(startDate)
    val end = if (isCurrent) "PRESENT" else retroCompactDate(endDate)
    return "$start - $end"
}

@Composable
private fun RetroGameButton(
    label: String,
    background: Color,
    content: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .retroGamePanel(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            label,
            style = TextStyle(content, 13.sp, FontWeight.Black),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RetroGameLinkButton(
    label: String,
    url: String?,
    background: Color = Color.White,
    modifier: Modifier = Modifier,
    onClick: (String?) -> Unit
) {
    Box(
        modifier = modifier
            .height(34.dp)
            .retroGamePanel(if (url.isNullOrBlank()) Color(0xFFE8E2D4) else background)
            .clickable(enabled = !url.isNullOrBlank()) { onClick(url) },
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            label,
            style = TextStyle(
                color = RetroGameInk.copy(alpha = if (url.isNullOrBlank()) 0.45f else 1f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RowScope.RetroGameStatCell(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    background: Color = Color.White.copy(alpha = 0.70f),
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .height(80.dp)
            .background(background)
            .border(1.dp, RetroGameInk, RoundedCornerShape(0.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 4.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BasicText(value, style = TextStyle(RetroGameInk, 22.sp, FontWeight.Black))
        Spacer(Modifier.height(4.dp))
        BasicText(
            label,
            style = TextStyle(RetroGameInk, 9.sp, FontWeight.Black, letterSpacing = 0.7.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun Modifier.retroGamePanel(background: Color): Modifier =
    this
        .drawBehind {
            val shadowOffset = 4.dp.toPx()
            drawRect(
                color = RetroGameInk,
                topLeft = Offset(shadowOffset, shadowOffset),
                size = size
            )
        }
        .background(background)
        .border(2.dp, RetroGameInk, RoundedCornerShape(0.dp))

// ==================== Profile Header ====================

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ProfileHeader(
    user: ProfileUser,
    stats: ProfileStats,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    isDarkTheme: Boolean,
    isOwner: Boolean,
    showLocation: Boolean,
    connectionStatus: String,
    isFollowing: Boolean,
    isFollowedBy: Boolean,
    connectionActionInProgress: Boolean,
    followActionInProgress: Boolean,
    isProfileSaved: Boolean,
    profileSaveActionInProgress: Boolean,
    mutualConnections: List<MutualConnection>,
    mutualConnectionsCount: Int,
    isUploadingAvatar: Boolean = false,
    isUploadingBanner: Boolean = false,
    onEditProfile: () -> Unit = {},
    onConnect: () -> Unit,
    onCancelRequest: () -> Unit,
    onAcceptRequest: () -> Unit,
    onRejectRequest: () -> Unit,
    onRemoveConnection: () -> Unit,
    onToggleFollow: () -> Unit,
    onToggleProfileSave: () -> Unit,
    onOpenConnections: () -> Unit,
    onOpenFollowers: () -> Unit,
    onMessage: (String) -> Unit,
    showStartConversation: Boolean,
    isPreparingConversationStarter: Boolean,
    onStartConversation: (String) -> Unit,
    onOpenMutualProfile: ((String) -> Unit)? = null,
    onUploadAvatar: (ByteArray) -> Unit = {},
    onUploadBanner: (ByteArray) -> Unit = {},
    onReportUser: () -> Unit = {},
    onBlockUser: () -> Unit = {}
) {
    val context = LocalContext.current
    var showShareMenu by remember { mutableStateOf(false) }
    var shareMenuAnchorBounds by remember { mutableStateOf<Rect?>(null) }
    val profileMenuAppearance = currentVormexAppearance(
        fallbackThemeMode = when {
            isGlassTheme -> VormexThemeMode.Glass.key
            isDarkTheme -> VormexThemeMode.Dark.key
            else -> VormexThemeMode.Light.key
        }
    )
    val profileMenuContainerColor = profileMenuAppearance.overlayColor
    val profileMenuContentColor = profileMenuAppearance.contentColor
    val profileMenuMutedContentColor = profileMenuAppearance.mutedContentColor
    val profileMenuDangerColor = if (profileMenuAppearance.isDarkTheme) Color(0xFFF87171) else Color(0xFFDC2626)
    val profileSaveMenuColor = if (isProfileSaved) accentColor else profileMenuContentColor
    val profileBodySurfaceColor = if (isGlassTheme) Color.Transparent else profileMenuAppearance.cardColor
    val profileBodyContentColor = profileMenuAppearance.contentColor
    val profileBodyMutedContentColor = profileMenuAppearance.mutedContentColor
    val profileBodySubtleColor = if (isGlassTheme) {
        profileMenuAppearance.controlColor
    } else {
        profileMenuAppearance.subtleColor
    }
    val profileBodyBorderColor = profileMenuAppearance.cardBorderColor
    val profileAvatarRingColor = if (isGlassTheme) {
        Color.White.copy(alpha = 0.72f)
    } else {
        profileMenuAppearance.cardColor
    }
    
    // Image editor state
    var showAvatarEditor by remember { mutableStateOf(false) }
    var showBannerEditor by remember { mutableStateOf(false) }
    var showAvatarPreview by remember { mutableStateOf(false) }
    var selectedImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    
    // Cache key to force image refresh
    var avatarCacheKey by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var bannerCacheKey by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val avatarContainerSize = 108.dp
    val bannerImageUrl = user.bannerImageUrl?.takeIf { it.isNotBlank() }
    
    // Image pickers
    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                val bytes = stream.readBytes()
                selectedImageBytes = bytes
                showAvatarEditor = true
            }
        }
    }
    
    val bannerPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                val bytes = stream.readBytes()
                selectedImageBytes = bytes
                showBannerEditor = true
            }
        }
    }
    
    // Avatar Editor Dialog
    if (showAvatarEditor && selectedImageBytes != null) {
        ImageEditorDialog(
            imageBytes = selectedImageBytes!!,
            isForAvatar = true,
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            onSave = { bytes ->
                onUploadAvatar(bytes)
                avatarCacheKey = System.currentTimeMillis() // Force cache refresh
                showAvatarEditor = false
                selectedImageBytes = null
            },
            onDismiss = {
                showAvatarEditor = false
                selectedImageBytes = null
            }
        )
    }
    
    // Banner Editor Dialog
    if (showBannerEditor && selectedImageBytes != null) {
        ImageEditorDialog(
            imageBytes = selectedImageBytes!!,
            isForAvatar = false,
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            onSave = { bytes ->
                onUploadBanner(bytes)
                bannerCacheKey = System.currentTimeMillis() // Force cache refresh
                showBannerEditor = false
                selectedImageBytes = null
            },
            onDismiss = {
                showBannerEditor = false
                selectedImageBytes = null
            }
        )
    }

    if (showAvatarPreview) {
        ProfileAvatarPreviewDialog(
            user = user,
            avatarCacheKey = avatarCacheKey,
            onDismiss = { showAvatarPreview = false }
        )
    }
    
    Box(
        Modifier
            .fillMaxWidth()
            .then(
                when {
                    isGlassTheme -> Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { profileCardBackdropShape() },
                        effects = {
                            vibrancy()
                            blur(16f.dp.toPx())
                            lens(8f.dp.toPx(), 16f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(profileMenuAppearance.cardColor)
                        }
                    )
                    isDarkTheme -> Modifier
                        .clip(ProfileCardShape)
                        .background(profileMenuAppearance.cardColor)
                    else -> Modifier
                        .clip(ProfileCardShape)
                        .background(profileMenuAppearance.cardColor)
                }
            )
    ) {
        Column {
            // Banner
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Color.Transparent)
            ) {
                Image(
                    painter = painterResource(R.drawable.profile_default_banner_vx),
                    contentDescription = "Default banner",
                    contentScale = ContentScale.Crop,
                    alignment = androidx.compose.ui.BiasAlignment(horizontalBias = 0.65f, verticalBias = 0f),
                    modifier = Modifier.fillMaxSize()
                )

                bannerImageUrl?.let { url ->
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(url)
                            .memoryCacheKey("banner_${user.id}_$bannerCacheKey")
                            .diskCachePolicy(CachePolicy.DISABLED)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Edit cover button (owner only)
                if (isOwner) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable(enabled = !isUploadingBanner) { 
                                bannerPicker.launch("image/*") 
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        if (isUploadingBanner) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.ic_camera),
                                    contentDescription = "Edit cover",
                                    modifier = Modifier.size(14.dp),
                                    colorFilter = ColorFilter.tint(Color.White)
                                )
                                BasicText(
                                    "Edit cover",
                                    style = TextStyle(Color.White, 12.sp)
                                )
                            }
                        }
                    }
                }
            }
            
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(profileBodySurfaceColor)
                    .padding(horizontal = 16.dp)
            ) {
                // Avatar Row
                Row(
                    Modifier
                        .fillMaxWidth()
                        .offset(y = (-54).dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(avatarContainerSize)
                            .clickable { showAvatarPreview = true }
                            .graphicsLayer { clip = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier
                                .size(avatarContainerSize)
                                .shadow(
                                    elevation = 5.dp,
                                    shape = CircleShape,
                                    clip = false,
                                    ambientColor = Color.Black.copy(alpha = 0.14f),
                                    spotColor = Color.Black.copy(alpha = 0.18f)
                                )
                                .clip(CircleShape)
                                .background(profileAvatarRingColor)
                                .border(1.dp, profileBodyBorderColor, CircleShape)
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.8f)),
                                contentAlignment = Alignment.Center
                            ) {
                                val profileImageUrl = user.profileImageUrl()
                                if (profileImageUrl != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(profileImageUrl)
                                            .memoryCacheKey("avatar_${user.id}_$avatarCacheKey")
                                            .diskCachePolicy(CachePolicy.DISABLED)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    val initials = user.name
                                        .split(" ")
                                        .mapNotNull { it.firstOrNull()?.uppercase() }
                                        .take(2)
                                        .joinToString("")
                                    BasicText(
                                        initials,
                                        style = TextStyle(Color.White, 28.sp, FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Action buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isOwner) {
                            // Edit Profile button
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(accentColor)
                                    .clickable(onClick = onEditProfile)
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                BasicText(
                                    "Edit Profile",
                                    style = TextStyle(Color.White, 14.sp, FontWeight.SemiBold)
                                )
                            }

                            Box(
                                Modifier
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.14f))
                                    .clickable(enabled = !isUploadingAvatar) {
                                        avatarPicker.launch("image/*")
                                    }
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isUploadingAvatar) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = accentColor,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(R.drawable.ic_camera),
                                        contentDescription = "Edit avatar",
                                        modifier = Modifier.size(16.dp),
                                        colorFilter = ColorFilter.tint(accentColor)
                                    )
                                }
                            }
                            
                            // Share button
                            Box {
                                Box(
                                    Modifier
                                        .clip(CircleShape)
                                        .background(profileBodySubtleColor)
                                        .onGloballyPositioned { shareMenuAnchorBounds = it.boundsInWindow() }
                                        .clickable {
                                            showShareMenu = true
                                        }
                                        .padding(10.dp)
                                ) {
                                    BasicText("↗", style = TextStyle(profileBodyContentColor, 16.sp))
                                }

                                if (isGlassTheme) {
                                    GlassDropdownMenu(
                                        expanded = showShareMenu,
                                        onDismissRequest = { showShareMenu = false },
                                        backdrop = backdrop,
                                        contentColor = profileMenuContentColor,
                                        anchorBounds = shareMenuAnchorBounds
                                    ) {
                                        GlassMenuItem(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Profile URL", "https://vormex.com/@${user.username}"))
                                                showShareMenu = false
                                            },
                                            contentColor = profileMenuContentColor,
                                            text = "Copy profile link"
                                        )
                                        GlassMenuItem(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, "Check out ${user.name}'s profile on Vormex: https://vormex.com/@${user.username}")
                                                }
                                                context.startActivity(Intent.createChooser(intent, "Share profile"))
                                                showShareMenu = false
                                            },
                                            contentColor = profileMenuContentColor,
                                            text = "Share profile"
                                        )
                                    }
                                } else {
                                    DropdownMenu(
                                        expanded = showShareMenu,
                                        onDismissRequest = { showShareMenu = false },
                                        containerColor = profileMenuContainerColor
                                    ) {
                                        DropdownMenuItem(
                                            text = { BasicText("Copy profile link", style = TextStyle(profileMenuContentColor)) },
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Profile URL", "https://vormex.com/@${user.username}"))
                                                showShareMenu = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { BasicText("Share profile", style = TextStyle(profileMenuContentColor)) },
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, "Check out ${user.name}'s profile on Vormex: https://vormex.com/@${user.username}")
                                                }
                                                context.startActivity(Intent.createChooser(intent, "Share profile"))
                                                showShareMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            val primaryActionText = when {
                                isPreparingConversationStarter -> "Writing..."
                                showStartConversation -> "Start conversation"
                                else -> "Message"
                            }
                            val primaryActionIcon = if (showStartConversation) R.drawable.vormex_logo else R.drawable.ic_message
                            val startConversationOrange = Color(0xFFFF8A3D)
                            val startConversationDeepOrange = if (isDarkTheme) Color(0xFFFFB071) else Color(0xFF9A3412)
                            val startConversationShape = RoundedCornerShape(24.dp)
                            val primaryActionBackground = if (showStartConversation) {
                                Modifier
                                    .shadow(
                                        elevation = if (isPreparingConversationStarter) 7.dp else 12.dp,
                                        shape = startConversationShape,
                                        ambientColor = Color(0xFF9A5725).copy(alpha = if (isDarkTheme) 0.38f else 0.22f)
                                    )
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = if (isDarkTheme) {
                                                listOf(Color(0xFF301A10), Color(0xFF482312), Color(0xFF6B2E12))
                                            } else {
                                                listOf(Color(0xFFFFFBF6), Color(0xFFFFE4C7), Color(0xFFFFB071))
                                            }
                                        ),
                                        shape = startConversationShape
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = startConversationOrange.copy(alpha = if (isPreparingConversationStarter) 0.28f else 0.48f),
                                        shape = startConversationShape
                                    )
                            } else {
                                when {
                                    isGlassTheme -> Modifier.drawBackdrop(
                                        backdrop = backdrop,
                                        shape = { RoundedRectangle(20f.dp) },
                                        effects = {
                                            vibrancy()
                                            blur(12f.dp.toPx())
                                            lens(6f.dp.toPx(), 12f.dp.toPx())
                                        },
                                        onDrawSurface = {
                                            drawRect(Color.White.copy(alpha = 0.15f))
                                        }
                                    )
                                    isDarkTheme -> Modifier.background(profileBodySubtleColor)
                                    else -> Modifier.background(profileBodySubtleColor)
                                }
                            }
                            val primaryActionContentColor = if (showStartConversation) startConversationDeepOrange else profileBodyContentColor
                            val primaryActionShape = if (showStartConversation) startConversationShape else RoundedCornerShape(20.dp)
                            val primaryActionPreBackgroundClip =
                                if (showStartConversation) Modifier else Modifier.clip(primaryActionShape)
                            val primaryActionPostBackgroundClip =
                                if (showStartConversation) Modifier.clip(primaryActionShape) else Modifier

                            // Message / Vormex opener button
                            Box(
                                Modifier
                                    .then(primaryActionPreBackgroundClip)
                                    .then(primaryActionBackground)
                                    .then(primaryActionPostBackgroundClip)
                                    .clickable(enabled = !isPreparingConversationStarter) {
                                        if (showStartConversation) {
                                            onStartConversation(user.id)
                                        } else {
                                            onMessage(user.id)
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isPreparingConversationStarter) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            color = primaryActionContentColor,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Image(
                                            painter = painterResource(primaryActionIcon),
                                            contentDescription = primaryActionText,
                                            modifier = if (showStartConversation) {
                                                Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = if (isDarkTheme) 0.14f else 0.82f))
                                                    .border(1.dp, startConversationOrange.copy(alpha = 0.26f), CircleShape)
                                                    .padding(3.dp)
                                            } else {
                                                Modifier.size(16.dp)
                                            },
                                            colorFilter = if (showStartConversation) null else ColorFilter.tint(primaryActionContentColor)
                                        )
                                    }
                                    BasicText(
                                        primaryActionText,
                                        style = TextStyle(primaryActionContentColor, 14.sp, FontWeight.SemiBold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            
                            // Menu button (Follow + Share profile)
                            Box {
                                Box(
                                    Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .then(
                                            when {
                                                isGlassTheme -> Modifier.drawBackdrop(
                                                    backdrop = backdrop,
                                                    shape = { RoundedRectangle(18f.dp) },
                                                    effects = {
                                                        vibrancy()
                                                        blur(12f.dp.toPx())
                                                        lens(6f.dp.toPx(), 12f.dp.toPx())
                                                    },
                                                    onDrawSurface = {
                                                        drawRect(Color.White.copy(alpha = 0.15f))
                                                    }
                                                )
                                                isDarkTheme -> Modifier.background(profileBodySubtleColor)
                                                else -> Modifier.background(profileBodySubtleColor)
                                            }
                                        )
                                        .onGloballyPositioned { shareMenuAnchorBounds = it.boundsInWindow() }
                                        .clickable {
                                            showShareMenu = true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.ic_more),
                                        contentDescription = "More options",
                                        modifier = Modifier.size(20.dp),
                                        colorFilter = ColorFilter.tint(profileBodyContentColor)
                                    )
                                }
                                
                                if (isGlassTheme) {
                                    GlassDropdownMenu(
                                        expanded = showShareMenu,
                                        onDismissRequest = { showShareMenu = false },
                                        backdrop = backdrop,
                                        contentColor = profileMenuContentColor,
                                        anchorBounds = shareMenuAnchorBounds
                                    ) {
                                        GlassMenuItem(
                                            onClick = {},
                                            contentColor = profileMenuContentColor,
                                            text = when (connectionStatus) {
                                                "connected" -> "Connection: Connected"
                                                "pending_sent" -> "Connection: Request sent"
                                                "pending_received" -> "Connection: Request received"
                                                else -> "Connection: Not connected"
                                            },
                                            textColor = profileMenuMutedContentColor,
                                            enabled = false
                                        )
                                        GlassMenuDivider(contentColor = profileMenuContentColor)
                                        when (connectionStatus) {
                                            "connected" -> {
                                                GlassMenuItem(
                                                    onClick = {
                                                        onRemoveConnection()
                                                        showShareMenu = false
                                                    },
                                                    contentColor = profileMenuContentColor,
                                                    text = "Remove Connection",
                                                    textColor = profileMenuDangerColor,
                                                    enabled = !connectionActionInProgress
                                                )
                                            }
                                            "pending_sent" -> {
                                                GlassMenuItem(
                                                    onClick = {
                                                        onCancelRequest()
                                                        showShareMenu = false
                                                    },
                                                    contentColor = profileMenuContentColor,
                                                    text = "Cancel Request",
                                                    enabled = !connectionActionInProgress
                                                )
                                            }
                                            "pending_received" -> {
                                                GlassMenuItem(
                                                    onClick = {
                                                        onAcceptRequest()
                                                        showShareMenu = false
                                                    },
                                                    contentColor = profileMenuContentColor,
                                                    text = "Accept Request",
                                                    enabled = !connectionActionInProgress
                                                )
                                                GlassMenuItem(
                                                    onClick = {
                                                        onRejectRequest()
                                                        showShareMenu = false
                                                    },
                                                    contentColor = profileMenuContentColor,
                                                    text = "Ignore Request",
                                                    enabled = !connectionActionInProgress
                                                )
                                            }
                                            else -> {
                                                GlassMenuItem(
                                                    onClick = {
                                                        onConnect()
                                                        showShareMenu = false
                                                    },
                                                    contentColor = profileMenuContentColor,
                                                    text = "Connect",
                                                    enabled = !connectionActionInProgress
                                                )
                                            }
                                        }
                                        GlassMenuItem(
                                            onClick = {
                                                onToggleFollow()
                                                showShareMenu = false
                                            },
                                            contentColor = profileMenuContentColor,
                                            leadingIcon = {
                                                Image(
                                                    painter = painterResource(R.drawable.ic_users),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp),
                                                    colorFilter = ColorFilter.tint(profileMenuContentColor)
                                                )
                                            },
                                            text = if (isFollowing) "Unfollow" else "Follow",
                                            enabled = !followActionInProgress
                                        )
                                        GlassMenuItem(
                                            onClick = {
                                                onToggleProfileSave()
                                                showShareMenu = false
                                            },
                                            contentColor = profileMenuContentColor,
                                            leadingIcon = {
                                                Image(
                                                    painter = painterResource(
                                                        if (isProfileSaved) R.drawable.ic_bookmark else R.drawable.ic_bookmark_outline
                                                    ),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp),
                                                    colorFilter = ColorFilter.tint(profileSaveMenuColor)
                                                )
                                            },
                                            text = if (isProfileSaved) "Saved profile" else "Save profile",
                                            textColor = profileSaveMenuColor,
                                            enabled = !profileSaveActionInProgress
                                        )
                                        GlassMenuItem(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Profile URL", "https://vormex.com/@${user.username}"))
                                                showShareMenu = false
                                            },
                                            contentColor = profileMenuContentColor,
                                            leadingIcon = {
                                                Image(
                                                    painter = painterResource(R.drawable.ic_link),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp),
                                                    colorFilter = ColorFilter.tint(profileMenuContentColor)
                                                )
                                            },
                                            text = "Copy profile link"
                                        )
                                        GlassMenuItem(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, "Check out ${user.name}'s profile on Vormex: https://vormex.com/@${user.username}")
                                                }
                                                context.startActivity(Intent.createChooser(intent, "Share profile"))
                                                showShareMenu = false
                                            },
                                            contentColor = profileMenuContentColor,
                                            leadingIcon = {
                                                Image(
                                                    painter = painterResource(R.drawable.ic_share),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp),
                                                    colorFilter = ColorFilter.tint(profileMenuContentColor)
                                                )
                                            },
                                            text = "Share profile"
                                        )
                                        if (!isOwner) {
                                            GlassMenuDivider(contentColor = profileMenuContentColor)
                                            GlassMenuItem(
                                                onClick = {
                                                    showShareMenu = false
                                                    onReportUser()
                                                },
                                                contentColor = profileMenuContentColor,
                                                text = "Report profile",
                                                textColor = profileMenuDangerColor
                                            )
                                            GlassMenuItem(
                                                onClick = {
                                                    showShareMenu = false
                                                    onBlockUser()
                                                },
                                                contentColor = profileMenuContentColor,
                                                text = "Block user",
                                                textColor = profileMenuDangerColor
                                            )
                                        }
                                    }
                                } else {
                                    DropdownMenu(
                                        expanded = showShareMenu,
                                        onDismissRequest = { showShareMenu = false },
                                        containerColor = profileMenuContainerColor
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                BasicText(
                                                    text = when (connectionStatus) {
                                                        "connected" -> "Connection: Connected"
                                                        "pending_sent" -> "Connection: Request sent"
                                                        "pending_received" -> "Connection: Request received"
                                                        else -> "Connection: Not connected"
                                                    },
                                                    style = TextStyle(profileMenuMutedContentColor, 13.sp)
                                                )
                                            },
                                            enabled = false,
                                            onClick = {}
                                        )
                                        when (connectionStatus) {
                                            "connected" -> {
                                                DropdownMenuItem(
                                                    text = {
                                                        BasicText(
                                                            "Remove Connection",
                                                            style = TextStyle(profileMenuDangerColor, 14.sp)
                                                        )
                                                    },
                                                    enabled = !connectionActionInProgress,
                                                    onClick = {
                                                        onRemoveConnection()
                                                        showShareMenu = false
                                                    }
                                                )
                                            }
                                            "pending_sent" -> {
                                                DropdownMenuItem(
                                                    text = {
                                                        BasicText(
                                                            "Cancel Request",
                                                            style = TextStyle(profileMenuContentColor, 14.sp)
                                                        )
                                                    },
                                                    enabled = !connectionActionInProgress,
                                                    onClick = {
                                                        onCancelRequest()
                                                        showShareMenu = false
                                                    }
                                                )
                                            }
                                            "pending_received" -> {
                                                DropdownMenuItem(
                                                    text = {
                                                        BasicText(
                                                            "Accept Request",
                                                            style = TextStyle(profileMenuContentColor, 14.sp)
                                                        )
                                                    },
                                                    enabled = !connectionActionInProgress,
                                                    onClick = {
                                                        onAcceptRequest()
                                                        showShareMenu = false
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = {
                                                        BasicText(
                                                            "Ignore Request",
                                                            style = TextStyle(profileMenuContentColor, 14.sp)
                                                        )
                                                    },
                                                    enabled = !connectionActionInProgress,
                                                    onClick = {
                                                        onRejectRequest()
                                                        showShareMenu = false
                                                    }
                                                )
                                            }
                                            else -> {
                                                DropdownMenuItem(
                                                    text = {
                                                        BasicText(
                                                            "Connect",
                                                            style = TextStyle(profileMenuContentColor, 14.sp)
                                                        )
                                                    },
                                                    enabled = !connectionActionInProgress,
                                                    onClick = {
                                                        onConnect()
                                                        showShareMenu = false
                                                    }
                                                )
                                            }
                                        }
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Image(
                                                        painter = painterResource(R.drawable.ic_users),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp),
                                                        colorFilter = ColorFilter.tint(profileMenuContentColor)
                                                    )
                                                    BasicText(
                                                        if (isFollowing) "Unfollow" else "Follow",
                                                        style = TextStyle(profileMenuContentColor, 14.sp)
                                                    )
                                                }
                                            },
                                            enabled = !followActionInProgress,
                                            onClick = {
                                                onToggleFollow()
                                                showShareMenu = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Image(
                                                        painter = painterResource(
                                                            if (isProfileSaved) R.drawable.ic_bookmark else R.drawable.ic_bookmark_outline
                                                    ),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp),
                                                    colorFilter = ColorFilter.tint(profileSaveMenuColor)
                                                )
                                                    BasicText(
                                                        if (isProfileSaved) "Saved profile" else "Save profile",
                                                        style = TextStyle(profileSaveMenuColor, 14.sp, FontWeight.SemiBold)
                                                    )
                                                }
                                            },
                                            enabled = !profileSaveActionInProgress,
                                            onClick = {
                                                onToggleProfileSave()
                                                showShareMenu = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Image(
                                                        painter = painterResource(R.drawable.ic_link),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp),
                                                        colorFilter = ColorFilter.tint(profileMenuContentColor)
                                                    )
                                                    BasicText("Copy profile link", style = TextStyle(profileMenuContentColor, 14.sp))
                                                }
                                            },
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Profile URL", "https://vormex.com/@${user.username}"))
                                                showShareMenu = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Image(
                                                        painter = painterResource(R.drawable.ic_share),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp),
                                                        colorFilter = ColorFilter.tint(profileMenuContentColor)
                                                    )
                                                    BasicText("Share profile", style = TextStyle(profileMenuContentColor, 14.sp))
                                                }
                                            },
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, "Check out ${user.name}'s profile on Vormex: https://vormex.com/@${user.username}")
                                                }
                                                context.startActivity(Intent.createChooser(intent, "Share profile"))
                                                showShareMenu = false
                                            }
                                        )
                                        if (!isOwner) {
                                            DropdownMenuItem(
                                                text = {
                                                    BasicText(
                                                        "Report profile",
                                                        style = TextStyle(profileMenuDangerColor, 14.sp, FontWeight.SemiBold)
                                                    )
                                                },
                                                onClick = {
                                                    showShareMenu = false
                                                    onReportUser()
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = {
                                                    BasicText(
                                                        "Block user",
                                                        style = TextStyle(profileMenuDangerColor, 14.sp, FontWeight.SemiBold)
                                                    )
                                                },
                                                onClick = {
                                                    showShareMenu = false
                                                    onBlockUser()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Name and badges
                Row(
                    Modifier.offset(y = (-24).dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BasicText(
                        user.name,
                        style = TextStyle(profileBodyContentColor, 22.sp, FontWeight.Bold)
                    )

                    if (user.hasVerificationBadge()) {
                        ProfileVerificationBadge(badgeStyle = user.verificationBadgeStyle())
                    }
                }

                if (user.isOpenToOpportunities) {
                    Box(
                        Modifier
                            .offset(y = (-20).dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF22C55E).copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        BasicText(
                            "#OpenToWork",
                            style = TextStyle(Color(0xFF22C55E), 12.sp, FontWeight.Medium)
                        )
                    }
                }
                
                // Username
                BasicText(
                    "@${user.username}",
                    style = TextStyle(profileBodyMutedContentColor, 14.sp),
                    modifier = Modifier.offset(y = (-20).dp)
                )
                
                // Headline
                user.headline?.let { headline ->
                    BasicText(
                        headline,
                        style = TextStyle(profileBodyContentColor, 14.sp),
                        modifier = Modifier.offset(y = (-12).dp)
                    )
                }
                
                // Location, College, Branch
                Row(
                    Modifier.offset(y = (-8).dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (showLocation) user.location?.let { location ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(R.drawable.ic_location),
                                contentDescription = "Location",
                                modifier = Modifier.size(14.dp),
                                colorFilter = ColorFilter.tint(profileBodyMutedContentColor)
                            )
                            Spacer(Modifier.width(4.dp))
                            BasicText(
                                location,
                                style = TextStyle(profileBodyMutedContentColor, 12.sp)
                            )
                        }
                    }
                    
                    if (!user.college.isNullOrEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(R.drawable.ic_education),
                                contentDescription = "Education",
                                modifier = Modifier.size(14.dp),
                                colorFilter = ColorFilter.tint(profileBodyMutedContentColor)
                            )
                            Spacer(Modifier.width(4.dp))
                            BasicText(
                                user.college,
                                style = TextStyle(profileBodyMutedContentColor, 12.sp)
                            )
                        }
                    }
                }
                
                // Social links
                Row(
                    Modifier.offset(y = (-4).dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    user.linkedinUrl?.let { url ->
                        SocialLinkChip(
                            icon = "in",
                            label = "LinkedIn",
                            url = url,
                            accentColor = accentColor
                        )
                    }
                    user.githubProfileUrl?.let { url ->
                        SocialLinkChip(
                            icon = "⌘",
                            label = "GitHub",
                            url = url,
                            accentColor = profileBodyContentColor
                        )
                    }
                    user.portfolioUrl?.let { url ->
                        SocialLinkChip(
                            icon = "",
                            label = "Portfolio",
                            url = url,
                            accentColor = accentColor,
                            iconRes = R.drawable.ic_link
                        )
                    }
                }
                
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatItem(
                        value = formatNumber(stats.connectionsCount),
                        label = "connections",
                        contentColor = profileBodyContentColor,
                        onClick = onOpenConnections,
                        modifier = Modifier.weight(1f)
                    )
                    StatItem(
                        value = formatNumber(stats.followersCount),
                        label = "followers",
                        contentColor = profileBodyContentColor,
                        onClick = onOpenFollowers,
                        modifier = Modifier.weight(1f)
                    )
                    StatItem(
                        value = formatNumber(stats.totalPosts),
                        label = "posts",
                        contentColor = profileBodyContentColor,
                        modifier = Modifier.weight(1f)
                    )
                    StatItem(
                        value = formatNumber(stats.totalLikesReceived),
                        label = "likes",
                        contentColor = profileBodyContentColor,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                ProfileGamificationRow(
                    stats = stats,
                    backdrop = backdrop,
                    contentColor = profileBodyContentColor,
                    accentColor = accentColor,
                    isGlassTheme = isGlassTheme,
                    isDarkTheme = isDarkTheme
                )
                
                if (!isOwner && (mutualConnectionsCount > 0 || isFollowedBy)) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (mutualConnectionsCount > 0) {
                            BasicText(
                                "$mutualConnectionsCount mutual connections",
                                style = TextStyle(profileBodyMutedContentColor, 12.sp)
                            )

                            if (mutualConnections.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    mutualConnections.forEach { mutual ->
                                        MutualConnectionChip(
                                            mutual = mutual,
                                            contentColor = profileBodyContentColor,
                                            accentColor = accentColor,
                                            onClick = onOpenMutualProfile?.let { callback ->
                                                { callback(mutual.id) }
                                            }
                                        )
                                    }

                                    val remainingMutuals =
                                        (mutualConnectionsCount - mutualConnections.size).coerceAtLeast(0)
                                    if (remainingMutuals > 0) {
                                        Box(
                                            Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(profileBodySubtleColor)
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            BasicText(
                                                "+$remainingMutuals more",
                                                style = TextStyle(
                                                    color = profileBodyMutedContentColor,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (isFollowedBy) {
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(profileBodySubtleColor)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                BasicText(
                                    "Follows you",
                                    style = TextStyle(profileBodyMutedContentColor, 11.sp)
                                )
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ProfileVerificationBadge(
    badgeStyle: String?,
    modifier: Modifier = Modifier
) {
    VerificationBadge(
        verified = true,
        badgeStyle = badgeStyle,
        modifier = modifier,
        size = VerificationBadgeSize.Large
    )
}

@Composable
private fun ProfileAvatarPreviewDialog(
    user: ProfileUser,
    avatarCacheKey: Long,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val profileUrl = remember(user.username) { "https://vormex.com/@${user.username}" }
    val avatarUrl = user.profileImageUrl()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp, start = 18.dp, end = 18.dp)
                    .align(Alignment.TopCenter)
            ) {
                Image(
                    painter = painterResource(R.drawable.vormex_logo),
                    contentDescription = "Vormex",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .align(Alignment.Center)
                )

                ProfileAvatarPreviewIconButton(
                    iconRes = R.drawable.ic_close,
                    contentDescription = "Close",
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 96.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    Modifier
                        .size(286.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(avatarUrl)
                                .memoryCacheKey("avatar_preview_${user.id}_$avatarCacheKey")
                                .diskCachePolicy(CachePolicy.DISABLED)
                                .crossfade(true)
                                .build(),
                            contentDescription = "${user.name} profile picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        BasicText(
                            profileInitials(user.name),
                            style = TextStyle(Color.White, 72.sp, FontWeight.Bold)
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))

                BasicText(
                    user.name,
                    style = TextStyle(Color.White, 22.sp, FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                BasicText(
                    "@${user.username}",
                    style = TextStyle(Color.White.copy(alpha = 0.58f), 14.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 30.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileAvatarPreviewActionButton(
                    iconRes = R.drawable.ic_share,
                    label = "Share",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Check out ${user.name}'s profile on Vormex: $profileUrl"
                            )
                        }
                        context.startActivity(Intent.createChooser(intent, "Share profile"))
                    }
                )
                ProfileAvatarPreviewActionButton(
                    iconRes = R.drawable.ic_copy,
                    label = "Copy",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Profile URL", profileUrl))
                        Toast.makeText(context, "Profile link copied", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
private fun ProfileAvatarPreviewIconButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(18.dp),
            colorFilter = ColorFilter.tint(Color.White)
        )
    }
}

@Composable
private fun ProfileAvatarPreviewActionButton(
    iconRes: Int,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.13f))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = label,
            modifier = Modifier.size(18.dp),
            colorFilter = ColorFilter.tint(Color.White)
        )
        Spacer(Modifier.width(8.dp))
        BasicText(
            label,
            style = TextStyle(Color.White, 14.sp, FontWeight.SemiBold),
            maxLines = 1
        )
    }
}

private fun profileInitials(name: String): String {
    return name
        .split(" ")
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .take(2)
        .joinToString("")
        .ifBlank { "V" }
}

private fun ProfileUser.profileImageUrl(): String? {
    return avatar?.takeIf { it.isNotBlank() }
        ?: profileImage?.takeIf { it.isNotBlank() }
}

@Composable
private fun MutualConnectionChip(
    mutual: MutualConnection,
    contentColor: Color,
    accentColor: Color,
    onClick: (() -> Unit)? = null
) {
    val label = mutual.name?.takeIf { it.isNotBlank() }
        ?: mutual.username?.takeIf { it.isNotBlank() }?.let { "@$it" }
        ?: "Vormex member"

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(contentColor.copy(alpha = 0.08f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            if (!mutual.avatar.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(mutual.avatar)
                        .crossfade(true)
                        .build(),
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                BasicText(
                    mutual.name?.firstOrNull()?.uppercase()
                        ?: mutual.username?.firstOrNull()?.uppercase()
                        ?: "?",
                    style = TextStyle(accentColor, 11.sp, FontWeight.Bold)
                )
            }
        }

        BasicText(
            label,
            modifier = Modifier.widthIn(max = 132.dp),
            style = TextStyle(
                color = contentColor.copy(alpha = 0.84f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        VerificationBadge(
            verified = mutual.hasVerificationBadge(),
            badgeStyle = mutual.verificationBadgeStyle(),
            size = VerificationBadgeSize.Micro
        )
    }
}

@Composable
private fun SocialLinkChip(
    icon: String,
    label: String,
    url: String,
    accentColor: Color,
    iconRes: Int? = null  // Optional drawable resource
) {
    val context = LocalContext.current
    
    Box(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(accentColor.copy(alpha = 0.15f))
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            }
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (iconRes != null) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = label,
                    modifier = Modifier.size(14.dp),
                    colorFilter = ColorFilter.tint(accentColor)
                )
            } else {
                BasicText(icon, style = TextStyle(accentColor, 12.sp, FontWeight.Bold))
            }
            Spacer(Modifier.width(4.dp))
            BasicText(label, style = TextStyle(accentColor, 12.sp))
        }
    }
}

@Composable
private fun RowScope.StatItem(
    value: String,
    label: String,
    contentColor: Color,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BasicText(
            value,
            style = TextStyle(contentColor, 16.sp, FontWeight.Bold)
        )
        BasicText(
            label,
            style = TextStyle(contentColor.copy(alpha = 0.6f), 11.sp)
        )
    }
}

@Composable
private fun ProfilePeopleSheetDialog(
    sheetState: ProfilePeopleSheetState,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onPersonClick: ((String) -> Unit)? = null
) {
    val kind = sheetState.kind ?: return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.42f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.72f)
                    .padding(horizontal = 12.dp, vertical = 16.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .then(
                        when {
                            isGlassTheme -> Modifier.drawBackdrop(
                                backdrop = backdrop,
                                shape = { RoundedRectangle(28f.dp) },
                                effects = {
                                    vibrancy()
                                    blur(18f.dp.toPx())
                                    lens(10f.dp.toPx(), 18f.dp.toPx())
                                },
                                onDrawSurface = {
                                    drawRect(Color.White.copy(alpha = 0.12f))
                                }
                            )
                            isDarkTheme -> Modifier.background(Color(0xFF171717))
                            else -> Modifier.background(Color.White)
                        }
                    )
                    .clickable(enabled = false) { }
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(42.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(contentColor.copy(alpha = 0.18f))
                )

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        BasicText(
                            kind.title,
                            style = TextStyle(contentColor, 20.sp, FontWeight.Bold)
                        )
                        BasicText(
                            if (sheetState.isLoading && sheetState.total == 0) "Loading people..."
                            else "${formatNumber(sheetState.total)} people",
                            style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(contentColor.copy(alpha = 0.08f))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = "Close",
                            modifier = Modifier.size(18.dp),
                            colorFilter = ColorFilter.tint(contentColor)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when {
                        sheetState.isLoading && sheetState.people.isEmpty() -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = accentColor,
                                    strokeWidth = 3.dp
                                )
                            }
                        }

                        sheetState.error != null && sheetState.people.isEmpty() -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                BasicText(
                                    "Could not load ${kind.title.lowercase()}",
                                    style = TextStyle(
                                        color = contentColor,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center
                                    )
                                )
                                Spacer(Modifier.height(8.dp))
                                BasicText(
                                    sheetState.error,
                                    style = TextStyle(
                                        color = contentColor.copy(alpha = 0.62f),
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )
                                )
                                Spacer(Modifier.height(14.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(accentColor)
                                        .clickable(onClick = onRetry)
                                        .padding(horizontal = 18.dp, vertical = 10.dp)
                                ) {
                                    BasicText(
                                        "Try again",
                                        style = TextStyle(Color.White, 13.sp, FontWeight.SemiBold)
                                    )
                                }
                            }
                        }

                        sheetState.people.isEmpty() -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                BasicText(
                                    kind.emptyTitle,
                                    style = TextStyle(
                                        color = contentColor,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center
                                    )
                                )
                                if (kind.emptyBody.isNotBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    BasicText(
                                        kind.emptyBody,
                                        style = TextStyle(
                                            color = contentColor.copy(alpha = 0.62f),
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    )
                                }
                            }
                        }

                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(sheetState.people, key = { it.id }) { person ->
                                    ProfilePeopleRow(
                                        person = person,
                                        contentColor = contentColor,
                                        accentColor = accentColor,
                                        onClick = onPersonClick?.let { callback ->
                                            { callback(person.id) }
                                        }
                                    )
                                }

                                if (sheetState.isLoading) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = accentColor,
                                                strokeWidth = 2.5.dp
                                            )
                                        }
                                    }
                                } else if (sheetState.error != null) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(18.dp))
                                                .background(contentColor.copy(alpha = 0.06f))
                                                .clickable(onClick = onRetry)
                                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                        ) {
                                            BasicText(
                                                "Retry loading more",
                                                style = TextStyle(accentColor, 13.sp, FontWeight.Medium),
                                                modifier = Modifier.align(Alignment.Center)
                                            )
                                        }
                                    }
                                } else if (sheetState.hasMore) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(18.dp))
                                                .background(accentColor.copy(alpha = 0.12f))
                                                .clickable(onClick = onLoadMore)
                                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                        ) {
                                            BasicText(
                                                "Load more",
                                                style = TextStyle(accentColor, 13.sp, FontWeight.SemiBold),
                                                modifier = Modifier.align(Alignment.Center)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfilePeopleRow(
    person: ProfilePeopleListItem,
    contentColor: Color,
    accentColor: Color,
    onClick: (() -> Unit)? = null
) {
    val secondaryLine = when {
        !person.headline.isNullOrBlank() -> person.headline
        !person.college.isNullOrBlank() -> person.college
        else -> "Vormex member"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ProfileCardShape)
            .background(contentColor.copy(alpha = 0.06f))
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            if (!person.profileImage.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(person.profileImage)
                        .crossfade(true)
                        .build(),
                    contentDescription = person.name ?: "Profile",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val initials = (person.name ?: person.username ?: "U")
                    .split(" ")
                    .mapNotNull { it.firstOrNull()?.uppercase() }
                    .take(2)
                    .joinToString("")
                BasicText(
                    initials,
                    style = TextStyle(accentColor, 15.sp, FontWeight.Bold)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BasicText(
                    text = person.name ?: person.username ?: "Unknown member",
                    style = TextStyle(contentColor, 14.sp, FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                VerificationBadge(
                    verified = person.hasVerificationBadge(),
                    badgeStyle = person.verificationBadgeStyle(),
                    size = VerificationBadgeSize.Small
                )

                if (person.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E))
                    )
                }
            }

            person.username?.takeIf { it.isNotBlank() }?.let { username ->
                BasicText(
                    text = "@$username",
                    style = TextStyle(contentColor.copy(alpha = 0.52f), 11.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            BasicText(
                text = secondaryLine,
                style = TextStyle(contentColor.copy(alpha = 0.7f), 12.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (onClick != null) {
            BasicText(
                "View",
                style = TextStyle(accentColor, 12.sp, FontWeight.Medium)
            )
        }
    }
}

// ==================== Image Editor Dialog ====================

@Composable
private fun ImageEditorDialog(
    imageBytes: ByteArray,
    isForAvatar: Boolean, // true for avatar (square), false for banner (landscape)
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onSave: (ByteArray) -> Unit,
    onDismiss: () -> Unit
) {
    val bitmap = remember(imageBytes) {
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
    
    if (bitmap == null) {
        onDismiss()
        return
    }
    
    // Zoom and pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    
    // Animated values for smooth transitions
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "scale"
    )
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "offsetX"
    )
    val animatedOffsetY by animateFloatAsState(
        targetValue = offsetY,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "offsetY"
    )
    
    // Reset function
    fun resetTransform() {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }
    
    // Crop and save function
    fun cropAndSave() {
        try {
            val bitmapWidth = bitmap.width
            val bitmapHeight = bitmap.height
            
            // Target dimensions
            val targetWidth = if (isForAvatar) 400 else 1500
            val targetHeight = if (isForAvatar) 400 else 500
            val targetAspect = targetWidth.toFloat() / targetHeight
            
            // At scale 1f, the entire image is visible (ContentScale.Fit)
            // When user zooms in (scale > 1f), they select a portion of the image
            // The visible portion is 1/scale of the original dimensions
            
            // Calculate the crop dimensions based on target aspect ratio and zoom
            val imageAspect = bitmapWidth.toFloat() / bitmapHeight
            
            // Determine base crop size (at scale 1f, this would be the full image with target aspect)
            val baseCropWidth: Float
            val baseCropHeight: Float
            
            if (imageAspect > targetAspect) {
                // Image is wider than target - crop width
                baseCropHeight = bitmapHeight.toFloat()
                baseCropWidth = baseCropHeight * targetAspect
            } else {
                // Image is taller than target - crop height
                baseCropWidth = bitmapWidth.toFloat()
                baseCropHeight = baseCropWidth / targetAspect
            }
            
            // Apply zoom - zooming in means we take a smaller portion
            val cropWidth = (baseCropWidth / animatedScale).roundToInt().coerceAtLeast(1)
            val cropHeight = (baseCropHeight / animatedScale).roundToInt().coerceAtLeast(1)
            
            // Calculate center position with pan offset
            // Pan offset is in screen coordinates, convert to bitmap coordinates
            val panScaleFactor = minOf(baseCropWidth / bitmapWidth, baseCropHeight / bitmapHeight) / animatedScale
            val centerX = (bitmapWidth / 2f - animatedOffsetX * panScaleFactor).roundToInt()
            val centerY = (bitmapHeight / 2f - animatedOffsetY * panScaleFactor).roundToInt()
            
            // Calculate crop bounds ensuring they stay within image
            val cropX = (centerX - cropWidth / 2).coerceIn(0, (bitmapWidth - cropWidth).coerceAtLeast(0))
            val cropY = (centerY - cropHeight / 2).coerceIn(0, (bitmapHeight - cropHeight).coerceAtLeast(0))
            val finalCropW = cropWidth.coerceAtMost(bitmapWidth - cropX)
            val finalCropH = cropHeight.coerceAtMost(bitmapHeight - cropY)
            
            // Ensure valid crop dimensions
            if (finalCropW <= 0 || finalCropH <= 0) {
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, if (isForAvatar) 80 else 90, outputStream)
                onSave(outputStream.toByteArray())
                return
            }
            
            // Create cropped bitmap
            val croppedBitmap = Bitmap.createBitmap(bitmap, cropX, cropY, finalCropW, finalCropH)
            
            // Scale to final target size
            val finalBitmap = Bitmap.createScaledBitmap(croppedBitmap, targetWidth, targetHeight, true)
            
            // Compress
            val outputStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, if (isForAvatar) 80 else 90, outputStream)
            
            // Cleanup
            if (croppedBitmap != bitmap) croppedBitmap.recycle()
            if (finalBitmap != croppedBitmap) finalBitmap.recycle()
            
            onSave(outputStream.toByteArray())
        } catch (e: Exception) {
            // Fallback: just compress original
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, if (isForAvatar) 80 else 90, outputStream)
            onSave(outputStream.toByteArray())
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Glass Header
                Box(
                    Modifier
                        .fillMaxWidth()
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(20f.dp) },
                            effects = {
                                vibrancy()
                                blur(16f.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(Color.White.copy(alpha = 0.08f))
                            }
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cancel button with glass style
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                                .clickable { onDismiss() }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            BasicText(
                                "Cancel",
                                style = TextStyle(Color.White, 14.sp, FontWeight.Medium)
                            )
                        }
                        
                        // Title
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            BasicText(
                                if (isForAvatar) "Edit Profile Photo" else "Edit Cover Photo",
                                style = TextStyle(Color.White, 16.sp, FontWeight.SemiBold)
                            )
                            BasicText(
                                "Pinch to zoom • Drag to position",
                                style = TextStyle(Color.White.copy(alpha = 0.5f), 11.sp)
                            )
                        }
                        
                        // Save button
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(accentColor)
                                .clickable { cropAndSave() }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            BasicText(
                                "Save",
                                style = TextStyle(Color.White, 14.sp, FontWeight.SemiBold)
                            )
                        }
                    }
                }
                
                // Image Editor Area
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF0A0A0A)),
                    contentAlignment = Alignment.Center
                ) {
                    // Editor container with crop overlay
                    Box(
                        Modifier
                            .then(
                                if (isForAvatar) {
                                    Modifier.size(300.dp)
                                } else {
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .aspectRatio(3f / 1f)
                                }
                            )
                            .clip(if (isForAvatar) CircleShape else RoundedCornerShape(16.dp))
                            .background(Color(0xFF1A1A1A)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Transformable image
                        Box(
                            Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(1f, 4f)
                                        offsetX += pan.x
                                        offsetY += pan.y
                                        
                                        // Limit pan based on scale
                                        val maxOffset = 300f * (scale - 1f)
                                        offsetX = offsetX.coerceIn(-maxOffset, maxOffset)
                                        offsetY = offsetY.coerceIn(-maxOffset, maxOffset)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Editable image",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = animatedScale
                                        scaleY = animatedScale
                                        translationX = animatedOffsetX
                                        translationY = animatedOffsetY
                                    }
                            )
                        }
                        
                        // Crop overlay grid (rule of thirds)
                        if (scale > 1f) {
                            Canvas(Modifier.fillMaxSize()) {
                                val strokeWidth = 1f
                                val color = Color.White.copy(alpha = 0.3f)
                                
                                // Vertical lines
                                drawLine(
                                    color = color,
                                    start = Offset(size.width / 3, 0f),
                                    end = Offset(size.width / 3, size.height),
                                    strokeWidth = strokeWidth
                                )
                                drawLine(
                                    color = color,
                                    start = Offset(size.width * 2 / 3, 0f),
                                    end = Offset(size.width * 2 / 3, size.height),
                                    strokeWidth = strokeWidth
                                )
                                
                                // Horizontal lines
                                drawLine(
                                    color = color,
                                    start = Offset(0f, size.height / 3),
                                    end = Offset(size.width, size.height / 3),
                                    strokeWidth = strokeWidth
                                )
                                drawLine(
                                    color = color,
                                    start = Offset(0f, size.height * 2 / 3),
                                    end = Offset(size.width, size.height * 2 / 3),
                                    strokeWidth = strokeWidth
                                )
                            }
                        }
                        
                        // Border glow effect
                        Box(
                            Modifier
                                .fillMaxSize()
                                .then(
                                    if (isForAvatar) {
                                        Modifier.border(2.dp, accentColor.copy(alpha = 0.6f), CircleShape)
                                    } else {
                                        Modifier.border(2.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                                    }
                                )
                        )
                    }
                    
                    // Hint text below avatar
                    if (isForAvatar) {
                        BasicText(
                            "Your photo will appear as a circle",
                            style = TextStyle(Color.White.copy(alpha = 0.5f), 12.sp),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 24.dp)
                        )
                    }
                }
                
                // Glass Controls Panel
                val sliderBackdrop = rememberLayerBackdrop()
                Box(
                    Modifier
                        .fillMaxWidth()
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(20f.dp) },
                            effects = {
                                vibrancy()
                                blur(16f.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(Color.White.copy(alpha = 0.08f))
                            }
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Zoom label and value
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicText(
                                "Zoom",
                                style = TextStyle(Color.White, 14.sp, FontWeight.Medium)
                            )
                            BasicText(
                                "${(animatedScale * 100).roundToInt()}%",
                                style = TextStyle(accentColor, 14.sp, FontWeight.SemiBold)
                            )
                        }
                        
                        // Liquid Glass Slider for zoom
                        LiquidSlider(
                            value = { scale },
                            onValueChange = { newScale ->
                                scale = newScale
                                // Reset offset when zooming out to prevent image going out of bounds
                                val maxOffset = 300f * (newScale - 1f)
                                offsetX = offsetX.coerceIn(-maxOffset, maxOffset)
                                offsetY = offsetY.coerceIn(-maxOffset, maxOffset)
                            },
                            valueRange = 1f..4f,
                            visibilityThreshold = 0.01f,
                            backdrop = sliderBackdrop,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(Modifier.height(4.dp))
                        
                        // Action buttons row
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Reset button
                            Box(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.1f))
                                    .clickable { resetTransform() }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BasicText(
                                        "↻",
                                        style = TextStyle(Color.White, 16.sp)
                                    )
                                    BasicText(
                                        "Reset",
                                        style = TextStyle(Color.White, 14.sp, FontWeight.Medium)
                                    )
                                }
                            }
                            
                            // Fit button
                            Box(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.1f))
                                    .clickable { 
                                        scale = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BasicText(
                                        "⊡",
                                        style = TextStyle(Color.White, 16.sp)
                                    )
                                    BasicText(
                                        "Fit",
                                        style = TextStyle(Color.White, 14.sp, FontWeight.Medium)
                                    )
                                }
                            }
                        }
                        
                        // Size recommendation
                        BasicText(
                            if (isForAvatar) "Recommended: 400 × 400 px" else "Recommended: 1500 × 500 px",
                            style = TextStyle(Color.White.copy(alpha = 0.4f), 11.sp)
                        )
                    }
                }
            }
        }
    }
}

// ==================== Helper Functions ====================

private fun formatNumber(num: Int): String {
    return when {
        num >= 1_000_000 -> "${num / 1_000_000}M"
        num >= 1_000 -> "${num / 1_000}K"
        else -> num.toString()
    }
}

// ==================== Loading (lightweight; avoids heavy shimmer + blur skeleton) ====================

@Composable
private fun ProfileLoadingContent(
    contentColor: Color,
    accentColor: Color,
    visitLoaderGiftId: String? = null,
    reduceAnimations: Boolean = false
) {
    val rawRes = ProfileLoaderGifts.resolvedVisitorRawRes(visitLoaderGiftId)
    val showLottie = !reduceAnimations
    Box(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (showLottie) {
                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(rawRes))
                val progress by animateLottieCompositionAsState(
                    composition = composition,
                    iterations = LottieConstants.IterateForever,
                    isPlaying = true
                )
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.size(120.dp),
                    alignment = Alignment.Center,
                    contentScale = ContentScale.Fit,
                    clipToCompositionBounds = true
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = accentColor,
                    strokeWidth = 3.dp
                )
            }
            BasicText(
                "Loading profile…",
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.72f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

// ==================== Error State ====================

@Composable
private fun ProfileError(
    error: String,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onRetry: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BasicText("😕", style = TextStyle(fontSize = 48.sp))
            
            BasicText(
                when {
                    error.contains("404") || error.contains("not found", ignoreCase = true) -> "User not found"
                    error.contains("403") || error.contains("private", ignoreCase = true) -> "This profile is private"
                    else -> "Failed to load profile"
                },
                style = TextStyle(contentColor, 18.sp, FontWeight.Bold)
            )
            
            BasicText(
                error,
                style = TextStyle(contentColor.copy(alpha = 0.6f), 14.sp),
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor)
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                BasicText(
                    "Retry",
                    style = TextStyle(Color.White, 14.sp, FontWeight.SemiBold)
                )
            }
        }
    }
}

@Composable
private fun ProfileGamificationRow(
    stats: ProfileStats,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    isDarkTheme: Boolean
) {
    val streakColor = profileStreakAccentColor(stats.currentStreak, contentColor)

    val labelStyle = TextStyle(
        color = contentColor.copy(alpha = 0.45f),
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .then(
                when {
                    isGlassTheme -> Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { profileCardBackdropShape() },
                        effects = {
                            vibrancy()
                            blur(12f.dp.toPx())
                            lens(6f.dp.toPx(), 12f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(Color.White.copy(alpha = 0.08f))
                        }
                    )
                    isDarkTheme -> Modifier
                        .clip(ProfileCardShape)
                        .background(Color.White.copy(alpha = 0.06f))
                    else -> Modifier
                        .clip(ProfileCardShape)
                        .background(Color.Black.copy(alpha = 0.035f))
                }
            )
            .border(1.dp, contentColor.copy(alpha = 0.1f), ProfileCardShape)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Level
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BasicText("LEVEL", style = labelStyle)
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFFFE566), Color(0xFFFFA500))
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    BasicText(
                        "${stats.level}",
                        style = TextStyle(
                            color = Color(0xFF1A1408),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            GamificationVDivider(contentColor)

            // XP
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BasicText("EXPERIENCE", style = labelStyle)
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BasicText(
                        "⚡",
                        style = TextStyle(
                            color = accentColor.copy(alpha = 0.85f),
                            fontSize = 15.sp
                        )
                    )
                    BasicText(
                        formatNumber(stats.xp),
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                BasicText(
                    "next +${formatNumber(stats.xpToNextLevel)} XP",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.42f),
                        fontSize = 9.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            GamificationVDivider(contentColor)

            // Streak
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BasicText("STREAK", style = labelStyle)
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (stats.currentStreak > 0) {
                        StreakFireLottie(modifier = Modifier.size(36.dp))
                        Spacer(Modifier.width(4.dp))
                    } else {
                        BasicText("❄️", style = TextStyle(fontSize = 14.sp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Column(horizontalAlignment = Alignment.Start) {
                        BasicText(
                            text = if (stats.currentStreak > 0) {
                                "${stats.currentStreak} day${if (stats.currentStreak > 1) "s" else ""}"
                            } else {
                                "No streak"
                            },
                            style = TextStyle(
                                color = streakColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (stats.currentStreak > 0 && stats.longestStreak > stats.currentStreak) {
                            BasicText(
                                "Best ${stats.longestStreak}",
                                style = TextStyle(
                                    color = contentColor.copy(alpha = 0.42f),
                                    fontSize = 9.sp
                                )
                            )
                        } else if (stats.totalActiveDays > 0) {
                            BasicText(
                                "${formatNumber(stats.totalActiveDays)} active days",
                                style = TextStyle(
                                    color = contentColor.copy(alpha = 0.42f),
                                    fontSize = 9.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GamificationVDivider(contentColor: Color) {
    Box(
        Modifier
            .width(1.dp)
            .height(46.dp)
            .background(contentColor.copy(alpha = 0.12f))
    )
}

private fun profileStreakAccentColor(currentStreak: Int, contentColor: Color): Color = when {
    currentStreak >= 30 -> Color(0xFFFF4500)
    currentStreak >= 7 -> Color(0xFFFF8C00)
    currentStreak >= 3 -> Color(0xFFFFD700)
    currentStreak > 0 -> Color(0xFFFFA500)
    else -> contentColor.copy(alpha = 0.55f)
}
