package com.kyant.backdrop.catalog.linkedin

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.ui.BasicText
import kotlinx.coroutines.delay

private val OtpInk = Color(0xFF101828)
private val OtpSurface = Color(0xFFFFFFFF)
private val OtpLine = Color(0xFFE4EAF2)
private val OtpError = Color(0xFFE11D48)
private val OtpSuccess = Color(0xFF16A34A)

@Composable
internal fun LiquidGlassEmailVerificationSheet(
    contentColor: Color,
    accentColor: Color,
    email: String,
    isLoading: Boolean,
    isSuccess: Boolean,
    error: String?,
    onVerify: (String) -> Unit,
    onResend: () -> Unit,
    onLoginClick: () -> Unit,
    onClearError: () -> Unit,
    onSuccessAnimationFinished: () -> Unit
) {
    var code by rememberSaveable(email) { mutableStateOf("") }
    var showFieldError by rememberSaveable(email) { mutableStateOf(false) }
    var successExitStarted by remember(email) { mutableStateOf(false) }
    var successExpanded by remember(email) { mutableStateOf(false) }
    val sheetOffset = remember(email) { Animatable(1.16f) }
    val successProgress = remember(email) { Animatable(0f) }
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val appearance = currentVormexAppearance()
    val resolvedTextColor = if (appearance.isDarkTheme) {
        appearance.contentColor
    } else {
        contentColor.takeUnless { it == Color.Transparent } ?: OtpInk
    }
    val sheetBaseColor = if (appearance.isDarkTheme) appearance.controlColor else OtpSurface
    val sheetBottomColor = if (appearance.isDarkTheme) appearance.backgroundColor else Color(0xFFFAFBFD)
    val borderColor = if (appearance.isDarkTheme) appearance.controlBorderColor else OtpLine
    val statusTopPx = WindowInsets.statusBars.getTop(density)
    val screenHeight = configuration.screenHeightDp.dp + with(density) { statusTopPx.toDp() }
    val collapsedHeight = if (screenHeight < 680.dp) screenHeight * 0.70f else 404.dp
    val sheetHeight = collapsedHeight + (screenHeight - collapsedHeight) * successProgress.value
    val topRadius = 34.dp * (1f - successProgress.value)
    val sheetShape = RoundedCornerShape(topStart = topRadius, topEnd = topRadius)
    val travelPx = with(density) { screenHeight.toPx() }
    val keyboardBottomPx = WindowInsets.ime.getBottom(density)
    val navigationBottomPx = WindowInsets.navigationBars.getBottom(density)
    val keyboardLiftTargetPx = (keyboardBottomPx - navigationBottomPx)
        .coerceAtLeast(0)
        .toFloat()
        .coerceAtMost(with(density) { (screenHeight * 0.42f).toPx() })
    val keyboardLiftPx by animateFloatAsState(
        targetValue = if (isSuccess) 0f else keyboardLiftTargetPx,
        animationSpec = tween(durationMillis = 460, easing = FastOutSlowInEasing),
        label = "otpSheetKeyboardLift"
    )
    val hasError = showFieldError || error != null
    val isPositive = isSuccess || successProgress.value > 0f
    val feedbackTargetColor = when {
        isPositive -> OtpSuccess
        hasError -> OtpError
        else -> borderColor
    }
    val feedbackColor by animateColorAsState(
        targetValue = feedbackTargetColor,
        label = "otpSheetFeedbackColor"
    )
    val feedbackIntensity by animateFloatAsState(
        targetValue = if (isPositive || hasError) 1f else 0f,
        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
        label = "otpSheetFeedbackIntensity"
    )
    val inputAccentColor by animateColorAsState(
        targetValue = when {
            hasError -> OtpError
            isPositive -> OtpSuccess
            else -> accentColor
        },
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "otpInputAccent"
    )
    val successComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.otp_verified_success))
    val playSuccessAnimation = isSuccess && successExpanded && !successExitStarted
    val successAnimationState = animateLottieCompositionAsState(
        composition = successComposition,
        restartOnPlay = true,
        iterations = 1,
        isPlaying = playSuccessAnimation
    )
    val successAnimationProgress = successAnimationState.progress

    LaunchedEffect(email) {
        successExitStarted = false
        successExpanded = false
        showFieldError = false
        successProgress.snapTo(0f)
        sheetOffset.snapTo(1.16f)
        sheetOffset.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            keyboardController?.hide()
            showFieldError = false
            successExpanded = false
            successProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1120, easing = FastOutSlowInEasing)
            )
            successExpanded = true
        }
    }

    LaunchedEffect(isSuccess, successExpanded, successComposition) {
        val composition = successComposition
        if (isSuccess && successExpanded && composition != null && !successExitStarted) {
            val animationDurationMs = composition.duration.toLong().coerceIn(1400L, 7000L)
            delay(animationDurationMs + 520L)
            successExitStarted = true
            sheetOffset.animateTo(
                targetValue = 1.42f,
                animationSpec = tween(durationMillis = 2300, easing = FastOutSlowInEasing)
            )
            onSuccessAnimationFinished()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color.Black.copy(
                        alpha = (
                            0.20f *
                                (1f - sheetOffset.value.coerceIn(0f, 1f)) *
                                (1f - successProgress.value * 0.65f)
                            ).coerceIn(0f, 0.20f)
                    )
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(sheetHeight)
                .graphicsLayer {
                    translationY = (travelPx * sheetOffset.value) - (keyboardLiftPx * (1f - successProgress.value))
                    shadowElevation = with(density) { 30.dp.toPx() }
                    shape = sheetShape
                    clip = false
                }
                .clip(sheetShape)
                .background(Brush.verticalGradient(listOf(sheetBaseColor, sheetBottomColor)), sheetShape)
                .border(10.dp, feedbackColor.copy(alpha = 0.07f * feedbackIntensity), sheetShape)
                .border(5.dp, feedbackColor.copy(alpha = 0.16f * feedbackIntensity), sheetShape)
                .border(
                    width = if (feedbackIntensity > 0f) 2.dp else 1.dp,
                    color = if (feedbackIntensity > 0f) {
                        feedbackColor.copy(alpha = 0.88f * feedbackIntensity)
                    } else {
                        borderColor
                    },
                    shape = sheetShape
                )
        ) {
            if (!isSuccess) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .widthIn(min = 42.dp, max = 42.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(resolvedTextColor.copy(alpha = 0.16f))
                    )

                    AuthSectionLabel(
                        title = "Confirm your email",
                        subtitle = "Enter the 6-digit code sent to $email.",
                        contentColor = contentColor
                    )

                    AuthInputField(
                        value = code,
                        onValueChange = { next ->
                            code = next.filter { it.isDigit() }.take(6)
                            showFieldError = false
                            onClearError()
                        },
                        label = "Verification code",
                        hint = "000000",
                        iconRes = if (hasError) R.drawable.ic_warning else R.drawable.ic_check,
                        accentColor = inputAccentColor,
                        keyboardType = KeyboardType.NumberPassword
                    )

                    error?.let { AuthInlineError(message = it) }

                    AuthPrimaryButton(
                        label = "Verify",
                        isLoading = isLoading,
                        enabled = !isLoading,
                        accentColor = accentColor,
                        onClick = {
                            if (code.length == 6) {
                                onVerify(code)
                            } else {
                                showFieldError = true
                            }
                        }
                    )

                    AuthTextActionRow(
                        prompt = "Didn't get it?",
                        action = "Resend code",
                        accentColor = accentColor,
                        contentColor = contentColor,
                        onClick = {
                            if (!isLoading) onResend()
                        }
                    )

                    AuthTextActionRow(
                        prompt = "Already verified?",
                        action = "Sign in",
                        accentColor = accentColor,
                        contentColor = contentColor,
                        onClick = onLoginClick
                    )
                }
            } else {
                if (successExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        OtpVerifiedLottie(
                            composition = successComposition,
                            progress = successAnimationProgress,
                            modifier = Modifier.size(138.dp)
                        )
                        BasicText(
                            "Welcome to Vormex",
                            modifier = Modifier.padding(top = 16.dp),
                            style = TextStyle(
                                color = resolvedTextColor,
                                fontSize = 22.sp,
                                lineHeight = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        BasicText(
                            "Your email is verified. Set up your profile so people can discover your work, skills, and story.",
                            modifier = Modifier
                                .widthIn(max = 300.dp)
                                .padding(horizontal = 18.dp, vertical = 6.dp),
                            style = TextStyle(
                                color = resolvedTextColor.copy(alpha = 0.66f),
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OtpVerifiedLottie(
    composition: LottieComposition?,
    progress: Float,
    modifier: Modifier = Modifier
) {
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier,
        alignment = Alignment.Center,
        contentScale = ContentScale.Fit,
        clipToCompositionBounds = false
    )
}
