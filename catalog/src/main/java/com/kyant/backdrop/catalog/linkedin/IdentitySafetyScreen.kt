package com.kyant.backdrop.catalog.linkedin

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.media.MediaReadSafety
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.models.IdentitySummary
import com.kyant.backdrop.catalog.network.models.IdentityVerificationRecord
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val IdentityEvidenceMaxBytes = 8L * 1024L * 1024L
private const val StudentEmailResendCooldownSeconds = 60

private enum class IdentityOnboardingPage(
    val title: String,
    val subtitle: String,
    val rawRes: Int
) {
    CollegeEmail(
        title = "College email",
        subtitle = "Enter your institution mailbox. Personal email domains are blocked.",
        rawRes = R.raw.identity_college_email
    ),
    Code(
        title = "Verify code",
        subtitle = "Use the newest 6-digit code sent to your college email.",
        rawRes = R.raw.identity_code_success
    ),
    Proof(
        title = "Student proof",
        subtitle = "Take a fresh photo of your student ID or proof document.",
        rawRes = R.raw.identity_id_scan
    ),
    Status(
        title = "Verification status",
        subtitle = "Your student badge review updates here.",
        rawRes = R.raw.identity_checking_projects_pc
    )
}

private enum class StudentVerificationStage {
    UnderReview,
    Verified,
    Rejected
}

private data class StudentVerificationUiState(
    val stage: StudentVerificationStage,
    val title: String,
    val body: String,
    val progressPercent: Int,
    val verificationId: String? = null,
    val reviewerComment: String? = null
)

@Composable
fun IdentitySafetyScreen(
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val pages = remember { IdentityOnboardingPage.entries.toList() }
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { pages.size }
    )
    val appearance = currentVormexAppearance()
    var studentEmail by remember { mutableStateOf("") }
    var studentCode by remember { mutableStateOf("") }
    var capturedProofUri by remember { mutableStateOf<Uri?>(null) }
    var capturedProofName by remember { mutableStateOf<String?>(null) }
    var pendingProofUri by remember { mutableStateOf<Uri?>(null) }
    var pendingProofName by remember { mutableStateOf<String?>(null) }
    var isSendingStudentCode by remember { mutableStateOf(false) }
    var isVerifyingStudentEmail by remember { mutableStateOf(false) }
    var isUploadingProof by remember { mutableStateOf(false) }
    var isClaimingStudentBadge by remember { mutableStateOf(false) }
    var isStudentEmailVerified by remember { mutableStateOf(false) }
    var isStudentBadgeClaimed by remember { mutableStateOf(false) }
    var showEducationBenefits by remember { mutableStateOf(false) }
    var isLoadingIdentityStatus by remember { mutableStateOf(true) }
    var verificationStatus by remember { mutableStateOf<StudentVerificationUiState?>(null) }
    var studentEmailCooldownSeconds by remember { mutableIntStateOf(0) }
    var codeSuccessAnimationSignal by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun dismissKeyboard() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    fun applyIdentitySummary(identity: IdentitySummary) {
        val nextStatus = identity.studentProofStatus()
        verificationStatus = nextStatus
        if (nextStatus?.stage != StudentVerificationStage.Verified) {
            showEducationBenefits = false
        }
        isStudentEmailVerified = identity.verifications.any {
            it.type.equals("STUDENT_EMAIL", ignoreCase = true) &&
                it.status.equals("VERIFIED", ignoreCase = true)
        }
    }

    fun resetForResubmission() {
        capturedProofUri = null
        capturedProofName = null
        pendingProofUri = null
        pendingProofName = null
        studentCode = ""
        message = null
        error = null
        verificationStatus = null
        isStudentBadgeClaimed = false
        showEducationBenefits = false
        scope.launch {
            pagerState.animateScrollToPage(IdentityOnboardingPage.CollegeEmail.ordinal)
        }
    }

    fun refreshStudentBadgeClaim() {
        scope.launch {
            ApiClient.getCurrentUser(context)
                .onSuccess { user ->
                    isStudentBadgeClaimed = user.profileBadgeStyle.equals("student", ignoreCase = true)
                }
        }
    }

    fun sendStudentCode() {
        if (isSendingStudentCode || studentEmailCooldownSeconds > 0) return
        scope.launch {
            dismissKeyboard()
            isStudentEmailVerified = false
            isSendingStudentCode = true
            error = null
            message = null
            ApiClient.requestStudentEmailVerification(context, studentEmail)
                .onSuccess {
                    message = it.message
                    studentEmailCooldownSeconds = StudentEmailResendCooldownSeconds
                    pagerState.animateScrollToPage(IdentityOnboardingPage.Code.ordinal)
                }
                .onFailure { error = it.message ?: "Could not send verification code." }
            isSendingStudentCode = false
        }
    }

    fun verifyStudentCode() {
        if (isVerifyingStudentEmail) return
        scope.launch {
            dismissKeyboard()
            isVerifyingStudentEmail = true
            error = null
            message = null
            ApiClient.confirmStudentEmailVerification(context, studentEmail, studentCode)
                .onSuccess {
                    studentCode = ""
                    isStudentEmailVerified = true
                    it.identity?.let(::applyIdentitySummary)
                    message = it.message
                    codeSuccessAnimationSignal += 1
                }
                .onFailure { error = it.message ?: "Could not verify student email." }
            isVerifyingStudentEmail = false
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val proofUri = pendingProofUri
        val proofName = pendingProofName ?: "identity-proof.jpg"
        if (!success || proofUri == null) {
            error = "Camera capture cancelled."
            pendingProofUri = null
            pendingProofName = null
            return@rememberLauncherForActivityResult
        }

        capturedProofUri = proofUri
        capturedProofName = proofName
        message = "Photo captured. Hold submit to send it for review."
        error = null
        pendingProofUri = null
        pendingProofName = null
    }

    fun submitProofPhoto() {
        val proofUri = capturedProofUri
        val proofName = capturedProofName ?: "identity-proof.jpg"
        if (proofUri == null || isUploadingProof) {
            return
        }
        scope.launch {
            isUploadingProof = true
            error = null
            message = null
            val result = runCatching {
                val (bytes, fileName) = MediaReadSafety.readMediaBytes(
                    context = context,
                    uri = proofUri,
                    fallbackFileName = proofName,
                    maxBytes = IdentityEvidenceMaxBytes,
                    label = "ID proof photo"
                )
                capturedProofName = fileName
                val uploadRequest = ApiClient.requestIdProofUpload(context).getOrThrow()
                ApiClient.submitIdProof(
                    context = context,
                    verificationId = uploadRequest.verificationId,
                    bytes = bytes,
                    fileName = fileName,
                    mimeType = "image/jpeg"
                ).getOrThrow()
            }
            if (result.isSuccess) {
                val response = result.getOrThrow()
                verificationStatus = StudentVerificationUiState(
                    stage = StudentVerificationStage.UnderReview,
                    title = "Under review",
                    body = "Your student proof was submitted and is waiting for admin review.",
                    progressPercent = 70,
                    verificationId = response.verificationId
                )
                message = response.message
                capturedProofUri = null
                capturedProofName = null
                isUploadingProof = false
                pagerState.animateScrollToPage(IdentityOnboardingPage.Status.ordinal)
                ApiClient.getIdentityStatus(context)
                    .onSuccess { applyIdentitySummary(it.identity) }
                    .onFailure { error = it.message ?: "Could not refresh verification status." }
            } else {
                error = result.exceptionOrNull()?.message ?: "Could not upload ID proof photo."
                isUploadingProof = false
            }
        }
    }

    fun takeProofPhoto() {
        val (uri, fileName) = createIdentityProofPhotoUri(context)
        pendingProofUri = uri
        pendingProofName = fileName
        cameraLauncher.launch(uri)
    }

    fun claimStudentBadge() {
        if (isClaimingStudentBadge || isStudentBadgeClaimed) return
        scope.launch {
            isClaimingStudentBadge = true
            error = null
            message = null
            ApiClient.claimStudentBadge(context)
                .onSuccess { response ->
                    response.identity?.let(::applyIdentitySummary)
                    isStudentBadgeClaimed = response.profileBadgeStyle.equals("student", ignoreCase = true)
                    message = response.message
                }
                .onFailure { error = it.message ?: "Could not claim student badge." }
            isClaimingStudentBadge = false
        }
    }

    LaunchedEffect(studentEmailCooldownSeconds) {
        if (studentEmailCooldownSeconds > 0) {
            delay(1000)
            studentEmailCooldownSeconds -= 1
        }
    }

    LaunchedEffect(Unit) {
        isLoadingIdentityStatus = true
        ApiClient.getIdentityStatus(context)
            .onSuccess { response ->
                applyIdentitySummary(response.identity)
                if (response.identity.studentProofStatus()?.shouldOwnScreen() == true) {
                    pagerState.scrollToPage(IdentityOnboardingPage.Status.ordinal)
                }
                refreshStudentBadgeClaim()
            }
            .onFailure { error = it.message ?: "Could not load verification status." }
        isLoadingIdentityStatus = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(identityBackgroundBrush(appearance))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { dismissKeyboard() })
            }
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
        ) { pageIndex ->
            when (pages[pageIndex]) {
                IdentityOnboardingPage.CollegeEmail -> CollegeEmailPage(
                    studentEmail = studentEmail,
                    cooldownSeconds = studentEmailCooldownSeconds,
                    isSendingCode = isSendingStudentCode,
                    message = message,
                    error = error,
                    onStudentEmailChange = {
                        studentEmail = it
                        isStudentEmailVerified = false
                        codeSuccessAnimationSignal = 0
                    },
                    onSendCode = ::sendStudentCode
                )
                IdentityOnboardingPage.Code -> CodeVerificationPage(
                    studentEmail = studentEmail,
                    studentCode = studentCode,
                    cooldownSeconds = studentEmailCooldownSeconds,
                    isSendingCode = isSendingStudentCode,
                    isVerifying = isVerifyingStudentEmail,
                    isVerified = isStudentEmailVerified,
                    playSuccessAnimationSignal = codeSuccessAnimationSignal,
                    message = message,
                    error = error,
                    onStudentCodeChange = { value ->
                        studentCode = value.filter { it.isDigit() }.take(6)
                    },
                    onVerify = ::verifyStudentCode,
                    onResendCode = ::sendStudentCode,
                    onSuccessAnimationComplete = {
                        scope.launch {
                            pagerState.animateScrollToPage(IdentityOnboardingPage.Proof.ordinal)
                        }
                    }
                )
                IdentityOnboardingPage.Proof -> StudentProofPage(
                    capturedProofUri = capturedProofUri,
                    capturedProofName = capturedProofName,
                    isUploadingProof = isUploadingProof,
                    message = message,
                    error = error,
                    onTakePhoto = ::takeProofPhoto,
                    onSubmitProof = ::submitProofPhoto
                )
                IdentityOnboardingPage.Status -> StudentVerificationStatusPage(
                    status = verificationStatus,
                    isLoading = isLoadingIdentityStatus,
                    isClaimingStudentBadge = isClaimingStudentBadge,
                    isStudentBadgeClaimed = isStudentBadgeClaimed,
                    showEducationBenefits = showEducationBenefits,
                    message = message,
                    error = error,
                    onResubmit = ::resetForResubmission,
                    onNavigateHome = onNavigateHome,
                    onClaimStudentBadge = ::claimStudentBadge,
                    onExploreEducationalBenefits = {
                        message = null
                        error = null
                        showEducationBenefits = true
                    },
                    onBackToStatus = { showEducationBenefits = false },
                    onRefresh = {
                        scope.launch {
                            isLoadingIdentityStatus = true
                            error = null
                            ApiClient.getIdentityStatus(context)
                                .onSuccess {
                                    applyIdentitySummary(it.identity)
                                    refreshStudentBadgeClaim()
                                }
                                .onFailure { error = it.message ?: "Could not refresh verification status." }
                            isLoadingIdentityStatus = false
                        }
                    }
                )
            }
        }

        IdentityBackButton(
            onNavigateBack = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 18.dp, top = 12.dp)
        )

        IdentityPageDots(
            pages = pages.size,
            selectedIndex = pagerState.currentPage,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 18.dp)
        )
    }
}

@Composable
private fun CollegeEmailPage(
    studentEmail: String,
    cooldownSeconds: Int,
    isSendingCode: Boolean,
    message: String?,
    error: String?,
    onStudentEmailChange: (String) -> Unit,
    onSendCode: () -> Unit
) {
    val sendEnabled = !isSendingCode && cooldownSeconds == 0 && studentEmail.isNotBlank()
    IdentityOnboardingCard(page = IdentityOnboardingPage.CollegeEmail) {
        IdentityTextField(
            value = studentEmail,
            onValueChange = onStudentEmailChange,
            label = "College email",
            keyboardType = KeyboardType.Email
        )
        IdentityActionButton(
            label = if (cooldownSeconds > 0) "Resend in ${cooldownSeconds}s" else "Send verification code",
            isLoading = isSendingCode,
            enabled = sendEnabled,
            onClick = onSendCode
        )
        IdentityInlineMessage(message = message, error = error)
    }
}

@Composable
private fun CodeVerificationPage(
    studentEmail: String,
    studentCode: String,
    cooldownSeconds: Int,
    isSendingCode: Boolean,
    isVerifying: Boolean,
    isVerified: Boolean,
    playSuccessAnimationSignal: Int,
    message: String?,
    error: String?,
    onStudentCodeChange: (String) -> Unit,
    onVerify: () -> Unit,
    onResendCode: () -> Unit,
    onSuccessAnimationComplete: () -> Unit
) {
    val verifyEnabled = !isVerified && !isVerifying && studentEmail.isNotBlank() && studentCode.length >= 6
    val resendEnabled = !isVerified && !isSendingCode && cooldownSeconds == 0 && studentEmail.isNotBlank()
    IdentityOnboardingCard(
        page = IdentityOnboardingPage.Code,
        playOnceSignal = playSuccessAnimationSignal,
        onPlayOnceComplete = onSuccessAnimationComplete
    ) {
        Text(
            text = if (studentEmail.isBlank()) "Enter college email on page 1 first." else studentEmail,
            style = MaterialTheme.typography.bodyMedium,
            color = currentVormexAppearance().mutedContentColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        if (isVerified) {
            Text(
                text = "College email verified. Moving to student proof...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            IdentityTextField(
                value = studentCode,
                onValueChange = onStudentCodeChange,
                label = "6-digit code",
                keyboardType = KeyboardType.Number
            )
            IdentityActionButton(
                label = "Verify code",
                isLoading = isVerifying,
                enabled = verifyEnabled,
                onClick = onVerify
            )
            IdentitySecondaryButton(
                label = if (cooldownSeconds > 0) "Resend in ${cooldownSeconds}s" else "Resend code",
                enabled = resendEnabled,
                onClick = onResendCode
            )
        }
        IdentityInlineMessage(message = message, error = error)
    }
}

@Composable
private fun StudentProofPage(
    capturedProofUri: Uri?,
    capturedProofName: String?,
    isUploadingProof: Boolean,
    message: String?,
    error: String?,
    onTakePhoto: () -> Unit,
    onSubmitProof: () -> Unit
) {
    IdentityOnboardingCard(page = IdentityOnboardingPage.Proof) {
        CameraCapturePanel(
            capturedProofUri = capturedProofUri,
            capturedProofName = capturedProofName,
            isUploadingProof = isUploadingProof,
            onTakePhoto = onTakePhoto,
            onSubmitProof = onSubmitProof
        )
        IdentityInlineMessage(message = message, error = error)
    }
}

@Composable
private fun StudentVerificationStatusPage(
    status: StudentVerificationUiState?,
    isLoading: Boolean,
    isClaimingStudentBadge: Boolean,
    isStudentBadgeClaimed: Boolean,
    showEducationBenefits: Boolean,
    message: String?,
    error: String?,
    onResubmit: () -> Unit,
    onNavigateHome: () -> Unit,
    onClaimStudentBadge: () -> Unit,
    onExploreEducationalBenefits: () -> Unit,
    onBackToStatus: () -> Unit,
    onRefresh: () -> Unit
) {
    val isVerifiedBenefitsPage = showEducationBenefits && status?.stage == StudentVerificationStage.Verified
    IdentityOnboardingCard(
        page = IdentityOnboardingPage.Status,
        titleOverride = if (isVerifiedBenefitsPage) "Education benefits" else null,
        subtitleOverride = if (isVerifiedBenefitsPage) {
            "Student-only benefits unlocked by Vormex verification."
        } else {
            null
        }
    ) {
        when {
            isVerifiedBenefitsPage -> {
                StudentEducationBenefitsContent(
                    isStudentBadgeClaimed = isStudentBadgeClaimed,
                    isClaimingStudentBadge = isClaimingStudentBadge,
                    onClaimStudentBadge = onClaimStudentBadge,
                    onNavigateHome = onNavigateHome,
                    onBackToStatus = onBackToStatus
                )
            }
            isLoading && status == null -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp
                )
                Text(
                    text = "Checking verification status...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = currentVormexAppearance().mutedContentColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            status == null -> {
                StudentVerificationProgressPanel(
                    status = StudentVerificationUiState(
                        stage = StudentVerificationStage.Rejected,
                        title = "No proof submitted",
                        body = "Start the student verification flow to submit your current proof.",
                        progressPercent = 0
                    )
                )
                IdentityActionButton(
                    label = "Start verification",
                    isLoading = false,
                    enabled = true,
                    onClick = onResubmit
                )
            }
            else -> {
                StudentVerificationProgressPanel(status = status)
                when (status.stage) {
                    StudentVerificationStage.Rejected -> {
                        IdentityActionButton(
                            label = "Resubmit proof",
                            isLoading = false,
                            enabled = true,
                            onClick = onResubmit
                        )
                    }
                    StudentVerificationStage.UnderReview -> {
                        IdentityActionButton(
                            label = "Go to home",
                            isLoading = false,
                            enabled = true,
                            onClick = onNavigateHome
                        )
                        IdentitySecondaryButton(
                            label = if (isLoading) "Refreshing..." else "Refresh status",
                            enabled = !isLoading,
                            onClick = onRefresh
                        )
                    }
                    StudentVerificationStage.Verified -> {
                        IdentityActionButton(
                            label = "Explore educational benefits",
                            isLoading = false,
                            enabled = true,
                            onClick = onExploreEducationalBenefits
                        )
                    }
                }
            }
        }
        IdentityInlineMessage(message = message, error = error)
    }
}

@Composable
private fun StudentEducationBenefitsContent(
    isStudentBadgeClaimed: Boolean,
    isClaimingStudentBadge: Boolean,
    onClaimStudentBadge: () -> Unit,
    onNavigateHome: () -> Unit,
    onBackToStatus: () -> Unit
) {
    val appearance = currentVormexAppearance()
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        EducationBenefitSectionCard(
            title = "Verification badge",
            body = "Claim the green student badge for your Vormex profile, people cards, chat, and search surfaces.",
            status = if (isStudentBadgeClaimed) "Claimed" else "Available"
        ) {
            if (isStudentBadgeClaimed) {
                ClaimedBenefitIndicator(label = "Student badge claimed")
            } else {
                IdentityActionButton(
                    label = "Claim student badge",
                    isLoading = isClaimingStudentBadge,
                    enabled = !isClaimingStudentBadge,
                    onClick = onClaimStudentBadge
                )
            }
        }

        EducationBenefitSectionCard(
            title = "More student benefits",
            body = "New education benefits for verified students will appear here when Vormex releases them for your account.",
            status = "Coming soon"
        )

        IdentityActionButton(
            label = "Go to home",
            isLoading = false,
            enabled = true,
            onClick = onNavigateHome
        )
        IdentitySecondaryButton(
            label = "Back to verification status",
            enabled = true,
            onClick = onBackToStatus
        )

        Text(
            text = "Benefits are tied to your verified student status.",
            style = MaterialTheme.typography.bodySmall,
            color = appearance.mutedContentColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun EducationBenefitSectionCard(
    title: String,
    body: String,
    status: String,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val appearance = currentVormexAppearance()
    val accentColor = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(appearance.surfaceColor(VormexSurfaceTone.Subtle), shape)
            .border(1.dp, appearance.borderColor(VormexSurfaceTone.Control), shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = appearance.contentColor,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = status,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = appearance.mutedContentColor
        )
        content()
    }
}

@Composable
private fun ClaimedBenefitIndicator(label: String) {
    val appearance = currentVormexAppearance()
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFF22C55E).copy(alpha = if (appearance.isDarkTheme) 0.18f else 0.10f), shape)
            .border(1.dp, Color(0xFF22C55E).copy(alpha = 0.36f), shape)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF22C55E),
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = appearance.contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StudentVerificationProgressPanel(status: StudentVerificationUiState) {
    val appearance = currentVormexAppearance()
    val shape = RoundedCornerShape(8.dp)
    val progressColor = when (status.stage) {
        StudentVerificationStage.Rejected -> warningColor(appearance)
        else -> Color(0xFF22C55E)
    }
    val progress = status.progressPercent.coerceIn(0, 100)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = status.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = appearance.contentColor,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$progress%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = progressColor
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(shape)
                .background(appearance.borderColor(VormexSurfaceTone.Control).copy(alpha = 0.46f), shape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress / 100f)
                    .height(10.dp)
                    .align(Alignment.CenterStart)
                    .background(progressColor, shape)
            )
        }
        Text(
            text = status.body,
            style = MaterialTheme.typography.bodyMedium,
            color = appearance.mutedContentColor
        )
        status.reviewerComment?.takeIf { it.isNotBlank() }?.let { comment ->
            Text(
                text = comment,
                style = MaterialTheme.typography.bodySmall,
                color = appearance.contentColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(appearance.surfaceColor(VormexSurfaceTone.Subtle), shape)
                    .padding(12.dp)
            )
        }
    }
}

@Composable
private fun IdentityBackButton(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appearance = currentVormexAppearance()
    val shape = RoundedCornerShape(8.dp)
    IconButton(
        onClick = onNavigateBack,
        modifier = modifier
            .size(42.dp)
            .clip(shape)
            .background(appearance.surfaceColor(VormexSurfaceTone.Control), shape)
            .border(1.dp, appearance.borderColor(VormexSurfaceTone.Control), shape)
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Back",
            tint = appearance.contentColor
        )
    }
}

@Composable
private fun IdentityOnboardingCard(
    page: IdentityOnboardingPage,
    titleOverride: String? = null,
    subtitleOverride: String? = null,
    playOnceSignal: Int? = null,
    onPlayOnceComplete: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val appearance = currentVormexAppearance()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appearance.surfaceColor(VormexSurfaceTone.Card))
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp)
            .padding(top = 74.dp, bottom = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        IdentityLottieIllustration(
            rawRes = page.rawRes,
            playOnceSignal = playOnceSignal,
            onPlayOnceComplete = onPlayOnceComplete,
            modifier = Modifier.size(148.dp)
        )
        Text(
            text = titleOverride ?: page.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = appearance.contentColor,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitleOverride ?: page.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = appearance.mutedContentColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        content()
    }
}

@Composable
private fun IdentityLottieIllustration(
    rawRes: Int,
    playOnceSignal: Int?,
    onPlayOnceComplete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(rawRes))
    if (playOnceSignal == null) {
        val progress by animateLottieCompositionAsState(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            isPlaying = true
        )
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = modifier,
            alignment = Alignment.Center,
            contentScale = ContentScale.Fit
        )
    } else {
        key(playOnceSignal) {
            var completionSent by remember { mutableStateOf(false) }
            val progress by animateLottieCompositionAsState(
                composition = composition,
                iterations = 1,
                isPlaying = playOnceSignal > 0
            )
            LaunchedEffect(playOnceSignal, progress) {
                if (!completionSent && playOnceSignal > 0 && progress >= 0.99f) {
                    completionSent = true
                    onPlayOnceComplete()
                }
            }
            LottieAnimation(
                composition = composition,
                progress = { if (playOnceSignal > 0) progress else 0f },
                modifier = modifier,
                alignment = Alignment.Center,
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun CameraCapturePanel(
    capturedProofUri: Uri?,
    capturedProofName: String?,
    isUploadingProof: Boolean,
    onTakePhoto: () -> Unit,
    onSubmitProof: () -> Unit
) {
    val context = LocalContext.current
    val appearance = currentVormexAppearance()
    val accentColor = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 166.dp)
            .clip(shape)
            .background(accentColor.copy(alpha = if (appearance.isDarkTheme) 0.12f else 0.08f), shape)
            .border(1.dp, accentColor.copy(alpha = 0.38f), shape)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (capturedProofUri == null) {
            Icon(
                Icons.Outlined.CameraAlt,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(34.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Take a fresh proof photo now",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = appearance.contentColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(14.dp))
            IdentityActionButton(
                label = "Open camera",
                isLoading = false,
                enabled = !isUploadingProof,
                onClick = onTakePhoto
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(capturedProofUri)
                    .crossfade(true)
                    .build(),
                contentDescription = "Student proof preview",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(shape)
                    .border(1.dp, accentColor.copy(alpha = 0.30f), shape),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = capturedProofName ?: "Proof photo ready",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = appearance.contentColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            IdentitySecondaryButton(
                label = "Retake photo",
                enabled = !isUploadingProof,
                onClick = onTakePhoto
            )
            Spacer(Modifier.height(10.dp))
            HoldToSubmitButton(
                enabled = !isUploadingProof,
                isLoading = isUploadingProof,
                label = "Hold to submit",
                onSubmit = onSubmitProof
            )
        }
    }
}

@Composable
private fun HoldToSubmitButton(
    enabled: Boolean,
    isLoading: Boolean,
    label: String,
    onSubmit: () -> Unit
) {
    val appearance = currentVormexAppearance()
    val accentColor = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(8.dp)
    val holdProgress = remember { Animatable(0f) }
    var isHolding by remember { mutableStateOf(false) }
    var submittedForHold by remember { mutableStateOf(false) }

    LaunchedEffect(isHolding, enabled, isLoading, submittedForHold) {
        if (isHolding && enabled && !isLoading && !submittedForHold) {
            holdProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 900, easing = LinearEasing)
            )
            if (isHolding && !submittedForHold) {
                submittedForHold = true
                onSubmit()
            }
        } else if (!isHolding && !isLoading) {
            holdProgress.snapTo(0f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(shape)
            .background(appearance.surfaceColor(VormexSurfaceTone.Control), shape)
            .border(
                1.dp,
                if (enabled) accentColor.copy(alpha = 0.45f) else appearance.borderColor(VormexSurfaceTone.Control),
                shape
            )
            .pointerInput(enabled, isLoading) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    if (!enabled || isLoading) {
                        return@awaitEachGesture
                    }
                    submittedForHold = false
                    isHolding = true
                    waitForUpOrCancellation()
                    isHolding = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(holdProgress.value)
                .height(50.dp)
                .align(Alignment.CenterStart)
                .background(accentColor, shape)
        )
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = appearance.contentColor,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (holdProgress.value > 0.55f) accentOnColor(accentColor) else appearance.contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun IdentityTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType
) {
    val appearance = currentVormexAppearance()
    val accentColor = MaterialTheme.colorScheme.primary
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = appearance.contentColor,
            unfocusedTextColor = appearance.contentColor,
            disabledTextColor = appearance.disabledContentColor,
            focusedContainerColor = appearance.surfaceColor(VormexSurfaceTone.Input),
            unfocusedContainerColor = appearance.surfaceColor(VormexSurfaceTone.Input),
            disabledContainerColor = appearance.surfaceColor(VormexSurfaceTone.Input).copy(alpha = 0.62f),
            cursorColor = accentColor,
            focusedBorderColor = accentColor,
            unfocusedBorderColor = appearance.borderColor(VormexSurfaceTone.Input),
            focusedLabelColor = accentColor,
            unfocusedLabelColor = appearance.mutedContentColor
        )
    )
}

@Composable
private fun IdentityActionButton(
    label: String,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val appearance = currentVormexAppearance()
    val accentColor = MaterialTheme.colorScheme.primary
    val contentColor = accentOnColor(accentColor)
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(shape)
            .background(if (enabled) accentColor else appearance.surfaceColor(VormexSurfaceTone.Control), shape)
            .border(
                1.dp,
                if (enabled) accentColor.copy(alpha = 0.70f) else appearance.borderColor(VormexSurfaceTone.Control),
                shape
            )
            .clickable(enabled = enabled && !isLoading) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = if (enabled) contentColor else appearance.mutedContentColor,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (enabled) contentColor else appearance.disabledContentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun IdentitySecondaryButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appearance = currentVormexAppearance()
    val accentColor = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(shape)
            .background(
                if (enabled) accentColor.copy(alpha = if (appearance.isDarkTheme) 0.12f else 0.08f)
                else appearance.surfaceColor(VormexSurfaceTone.Control),
                shape
            )
            .border(
                1.dp,
                if (enabled) accentColor.copy(alpha = 0.36f) else appearance.borderColor(VormexSurfaceTone.Control),
                shape
            )
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (enabled) accentColor else appearance.disabledContentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun IdentityInlineMessage(message: String?, error: String?) {
    val text = error ?: message ?: return
    val appearance = currentVormexAppearance()
    val color = if (error != null) warningColor(appearance) else MaterialTheme.colorScheme.primary
    val icon = if (error != null) Icons.Outlined.WarningAmber else Icons.Outlined.CheckCircle
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = appearance.contentColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun IdentityPageDots(
    pages: Int,
    selectedIndex: Int,
    modifier: Modifier = Modifier
) {
    val appearance = currentVormexAppearance()
    val accentColor = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pages) { index ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(if (selected) 24.dp else 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) accentColor else appearance.borderColor(VormexSurfaceTone.Control))
            )
        }
    }
}

private fun IdentitySummary.studentProofStatus(): StudentVerificationUiState? {
    val review = latestStudentProofReview() ?: return null
    val normalizedStatus = review.status.uppercase()
    return when {
        normalizedStatus == "VERIFIED" -> StudentVerificationUiState(
            stage = StudentVerificationStage.Verified,
            title = "Verification successful",
            body = "Your student verification is successful. Claim your green student badge and explore education benefits.",
            progressPercent = 100,
            verificationId = review.id
        )
        normalizedStatus == "PENDING" && review.evidenceHasFile -> StudentVerificationUiState(
            stage = StudentVerificationStage.UnderReview,
            title = "Under review",
            body = "Vormex is checking your student proof. Your application is 70% complete while the review is pending.",
            progressPercent = 70,
            verificationId = review.id
        )
        normalizedStatus == "REJECTED" -> StudentVerificationUiState(
            stage = StudentVerificationStage.Rejected,
            title = "Resubmission requested",
            body = "Vormex finished reviewing this request and needs a fresh submission. Read the comment and start again from page 1.",
            progressPercent = 100,
            verificationId = review.id,
            reviewerComment = review.rejectionReason ?: review.reviewNotes
        )
        else -> null
    }
}

private fun IdentitySummary.latestStudentProofReview(): IdentityVerificationRecord? {
    return verifications.firstOrNull { it.type.equals("ID_DOCUMENT", ignoreCase = true) }
}

private fun StudentVerificationUiState.shouldOwnScreen(): Boolean = true

private fun createIdentityProofPhotoUri(context: Context): Pair<Uri, String> {
    val outputDir = File(context.filesDir, "chat_media").apply { mkdirs() }
    val fileName = "identity_proof_${System.currentTimeMillis()}.jpg"
    val file = File(outputDir, fileName)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    return uri to fileName
}

private fun identityBackgroundBrush(appearance: VormexAppearance): Brush {
    val background = appearance.backgroundColor.takeUnless { it.alpha == 0f } ?: appearance.sheetColor
    return Brush.verticalGradient(
        listOf(
            background,
            appearance.surfaceColor(VormexSurfaceTone.Sheet),
            appearance.surfaceColor(VormexSurfaceTone.Subtle)
        )
    )
}

private fun warningColor(appearance: VormexAppearance): Color =
    if (appearance.isDarkTheme) Color(0xFFFFB4AB) else Color(0xFFB3261E)

private fun accentOnColor(color: Color): Color =
    if (color.luminance() > 0.55f) Color(0xFF07110B) else Color.White
