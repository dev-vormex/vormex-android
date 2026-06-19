package com.kyant.backdrop.catalog.linkedin

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.kyant.backdrop.catalog.ui.BasicText
import com.kyant.backdrop.catalog.ui.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.R
import kotlinx.coroutines.launch

private val AuthInk = Color(0xFF101828)
private val AuthSurface = Color(0xFFFFFFFF)
private val AuthField = Color(0xFFF4F7FB)
private val AuthLine = Color(0xFFE4EAF2)
private val AuthBrandFontFamily = FontFamily(Font(R.font.kaushan_script))

@Composable
internal fun LiquidGlassLoginScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isLoading: Boolean,
    isGoogleLoading: Boolean,
    error: String?,
    savedAccountsCount: Int,
    onLogin: (String, String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onForgotPassword: (String) -> Unit,
    onSignUpClick: () -> Unit,
    onOpenSavedAccounts: () -> Unit,
    onClearError: () -> Unit
) {
    val context = LocalContext.current
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showEmailLogin by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(error) {
        if (!error.isNullOrBlank()) {
            showEmailLogin = true
        }
    }

    LiquidGlassAuthFrame(
        accentColor = accentColor,
        contentColor = contentColor,
        isForm = showEmailLogin
    ) {
        if (!showEmailLogin) {
            error?.let { AuthInlineError(message = it) }

            AuthPrimaryButton(
                label = "Create an account",
                isLoading = false,
                enabled = !isLoading && !isGoogleLoading,
                accentColor = accentColor,
                onClick = onSignUpClick
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AuthSocialButton(
                    label = "Email",
                    contentColor = contentColor,
                    enabled = !isLoading && !isGoogleLoading,
                    modifier = Modifier.weight(1f),
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_email),
                            contentDescription = "Email",
                            tint = currentAuthTextColor(contentColor),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    onClick = { showEmailLogin = true }
                )

                AuthSocialButton(
                    label = "Google",
                    contentColor = contentColor,
                    enabled = !isLoading && !isGoogleLoading,
                    isLoading = isGoogleLoading,
                    modifier = Modifier.weight(1f),
                    leadingContent = {
                        Image(
                            painter = painterResource(R.drawable.ic_google),
                            contentDescription = "Google",
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    onClick = onGoogleSignIn
                )
            }

            AuthTextActionRow(
                prompt = "Already have an account?",
                action = "Sign in",
                accentColor = accentColor,
                contentColor = contentColor,
                onClick = { showEmailLogin = true }
            )
            AuthSavedAccountsAction(
                savedAccountsCount = savedAccountsCount,
                accentColor = accentColor,
                contentColor = contentColor,
                onClick = onOpenSavedAccounts
            )
        } else {
            AuthSectionLabel(
                title = "Sign in",
                subtitle = "Use your Vormex account to continue.",
                contentColor = contentColor
            )

            AuthInputField(
                value = email,
                onValueChange = {
                    email = it
                    onClearError()
                },
                label = "Email",
                hint = "you@example.com",
                iconRes = R.drawable.ic_email,
                accentColor = accentColor,
                keyboardType = KeyboardType.Email
            )

            AuthInputField(
                value = password,
                onValueChange = {
                    password = it
                    onClearError()
                },
                label = "Password",
                hint = "Enter your password",
                iconRes = R.drawable.ic_lock,
                accentColor = accentColor,
                isPassword = true,
                keyboardType = KeyboardType.Password
            )

            error?.let { AuthInlineError(message = it) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                BasicText(
                    "Forgot password?",
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable {
                            val trimmedEmail = email.trim()
                            if (trimmedEmail.isBlank()) {
                                Toast.makeText(context, "Enter your email first", Toast.LENGTH_SHORT).show()
                            } else {
                                onForgotPassword(trimmedEmail)
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    style = TextStyle(accentColor, 14.sp, FontWeight.Medium)
                )
            }

            AuthPrimaryButton(
                label = "Sign in",
                isLoading = isLoading,
                enabled = !isLoading,
                accentColor = accentColor,
                onClick = {
                    if (email.isNotBlank() && password.isNotBlank()) {
                        onLogin(email, password)
                    }
                }
            )

            AuthDivider(contentColor = contentColor)

            AuthGoogleButton(
                contentColor = contentColor,
                isLoading = isGoogleLoading,
                isEnabled = !isGoogleLoading && !isLoading,
                onClick = onGoogleSignIn
            )

            AuthTextActionRow(
                prompt = "New to Vormex?",
                action = "Create account",
                accentColor = accentColor,
                contentColor = contentColor,
                onClick = onSignUpClick
            )
            AuthSavedAccountsAction(
                savedAccountsCount = savedAccountsCount,
                accentColor = accentColor,
                contentColor = contentColor,
                onClick = onOpenSavedAccounts
            )
        }
    }
}

@Composable
internal fun LiquidGlassSignUpScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isLoading: Boolean,
    isGoogleLoading: Boolean,
    error: String?,
    savedAccountsCount: Int,
    onSignUp: (email: String, password: String, name: String, username: String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onLoginClick: () -> Unit,
    onOpenSavedAccounts: () -> Unit,
    onClearError: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordError by rememberSaveable { mutableStateOf<String?>(null) }

    LiquidGlassAuthFrame(
        accentColor = accentColor,
        contentColor = contentColor,
        isForm = true
    ) {
        AuthSectionLabel(
            title = "Create an account",
            subtitle = "Start with your name, handle, and email.",
            contentColor = contentColor
        )

        AuthInputField(
            value = name,
            onValueChange = {
                name = it
                onClearError()
            },
            label = "Full name",
            hint = "Your name",
            iconRes = R.drawable.ic_profile,
            accentColor = accentColor
        )

        AuthInputField(
            value = username,
            onValueChange = {
                username = it.lowercase().filter { char -> char.isLetterOrDigit() || char == '_' }
                onClearError()
            },
            label = "Username",
            hint = "vormex_handle",
            iconRes = R.drawable.ic_mention,
            accentColor = accentColor,
            supportingText = "Letters, numbers, and underscores only"
        )

        AuthInputField(
            value = email,
            onValueChange = {
                email = it
                onClearError()
            },
            label = "Email",
            hint = "you@example.com",
            iconRes = R.drawable.ic_email,
            accentColor = accentColor,
            keyboardType = KeyboardType.Email
        )

        AuthInputField(
            value = password,
            onValueChange = {
                password = it
                onClearError()
                passwordError = null
            },
            label = "Password",
            hint = "At least 8 characters",
            iconRes = R.drawable.ic_lock,
            accentColor = accentColor,
            isPassword = true,
            keyboardType = KeyboardType.Password
        )

        AuthPasswordStrength(
            password = password,
            accentColor = accentColor,
            contentColor = contentColor
        )

        AuthInputField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                passwordError = null
            },
            label = "Confirm password",
            hint = "Repeat password",
            iconRes = R.drawable.ic_check,
            accentColor = accentColor,
            isPassword = true,
            keyboardType = KeyboardType.Password
        )

        passwordError?.let { AuthInlineError(message = it) }
        error?.let { AuthInlineError(message = it) }

        AuthPrimaryButton(
            label = "Create an account",
            isLoading = isLoading,
            enabled = !isLoading,
            accentColor = accentColor,
            onClick = {
                when {
                    name.isBlank() -> passwordError = "Name is required"
                    username.isBlank() -> passwordError = "Username is required"
                    username.length < 3 -> passwordError = "Username must be at least 3 characters"
                    email.isBlank() -> passwordError = "Email is required"
                    password.length < 8 -> passwordError = "Password must be at least 8 characters"
                    password != confirmPassword -> passwordError = "Passwords do not match"
                    !isLoading -> onSignUp(email.trim(), password, name.trim(), username.trim())
                }
            }
        )

        AuthDivider(contentColor = contentColor)

        AuthGoogleButton(
            contentColor = contentColor,
            isLoading = isGoogleLoading,
            isEnabled = !isGoogleLoading && !isLoading,
            onClick = onGoogleSignIn
        )

        AuthTextActionRow(
            prompt = "Already have an account?",
            action = "Sign in",
            accentColor = accentColor,
            contentColor = contentColor,
            onClick = onLoginClick
        )
        AuthSavedAccountsAction(
            savedAccountsCount = savedAccountsCount,
            accentColor = accentColor,
            contentColor = contentColor,
            onClick = onOpenSavedAccounts
        )
    }
}

@Composable
internal fun LiquidGlassEmailVerificationScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    email: String,
    isLoading: Boolean,
    error: String?,
    onVerify: (String) -> Unit,
    onResend: () -> Unit,
    onLoginClick: () -> Unit,
    onClearError: () -> Unit
) {
    var code by rememberSaveable { mutableStateOf("") }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }

    LiquidGlassAuthFrame(
        accentColor = accentColor,
        contentColor = contentColor,
        isForm = true
    ) {
        AuthSectionLabel(
            title = "Verify email",
            subtitle = "Enter the 6-digit code sent to $email.",
            contentColor = contentColor
        )

        AuthInputField(
            value = code,
            onValueChange = { next ->
                code = next.filter { it.isDigit() }.take(6)
                localError = null
                onClearError()
            },
            label = "Verification code",
            hint = "000000",
            iconRes = R.drawable.ic_check,
            accentColor = accentColor,
            keyboardType = KeyboardType.Number
        )

        localError?.let { AuthInlineError(message = it) }
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
                    localError = "Enter the 6-digit code"
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
}

@Composable
private fun LiquidGlassAuthFrame(
    accentColor: Color,
    contentColor: Color,
    isForm: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    val fade = remember { Animatable(0f) }
    val contentRise = remember { Animatable(18f) }
    val scrollState = rememberScrollState()
    val appearance = currentVormexAppearance()
    val backgroundColor = currentAuthBackgroundColor()

    LaunchedEffect(Unit) {
        launch { fade.animateTo(1f, tween(durationMillis = 520)) }
        contentRise.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = 300f))
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        val isCompactHeight = maxHeight < 720.dp
        val horizontalPadding = if (maxWidth < 380.dp) 24.dp else 26.dp
        val topGap = when {
            isForm -> if (isCompactHeight) 18.dp else 34.dp
            isCompactHeight -> maxHeight * 0.22f
            else -> maxHeight * 0.30f
        }
        val spacing = if (isCompactHeight) 18.dp else 24.dp

        if (!appearance.isDarkTheme) {
            AuthQuietBackground(accentColor = accentColor)
        }

        if (isForm) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .displayCutoutPadding()
                    .imePadding()
                    .navigationBarsPadding()
                    .verticalScroll(scrollState)
                    .padding(horizontal = horizontalPadding, vertical = 18.dp)
                    .graphicsLayer {
                        alpha = fade.value
                        translationY = contentRise.value
                    },
                verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 12.dp else 14.dp)
            ) {
                Spacer(modifier = Modifier.height(topGap))
                AuthEditorialHero(
                    accentColor = accentColor,
                    contentColor = contentColor,
                    compact = true
                )
                Spacer(modifier = Modifier.height(4.dp))
                content()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .displayCutoutPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = horizontalPadding, vertical = 18.dp)
                    .graphicsLayer {
                        alpha = fade.value
                        translationY = contentRise.value
                    }
            ) {
                Spacer(modifier = Modifier.height(topGap))
                AuthEditorialHero(
                    accentColor = accentColor,
                    contentColor = contentColor,
                    compact = false
                )
                Spacer(modifier = Modifier.weight(1f))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                        .align(Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun AuthQuietBackground(accentColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(currentAuthBackgroundColor())
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(190.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            accentColor.copy(alpha = 0.05f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun AuthEditorialHero(
    accentColor: Color,
    contentColor: Color,
    compact: Boolean
) {
    val resolvedContentColor = currentAuthTextColor(contentColor)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                BasicText(
                    "vormex",
                    style = TextStyle(
                        color = resolvedContentColor,
                        fontSize = if (compact) 28.sp else 32.sp,
                        lineHeight = if (compact) 32.sp else 36.sp,
                        fontFamily = AuthBrandFontFamily
                    )
                )
                Box(
                    modifier = Modifier
                        .padding(bottom = 7.dp)
                        .size(if (compact) 6.dp else 7.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
            }
            BasicText(
                "A quieter professional network".uppercase(),
                style = TextStyle(
                    color = resolvedContentColor.copy(alpha = 0.52f),
                    fontSize = if (compact) 10.sp else 11.sp,
                    lineHeight = if (compact) 12.sp else 14.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        BasicText(
            buildAnnotatedString {
                append("The work, ")
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Normal))
                append("not")
                pop()
                append(" the noise.")
            },
            style = TextStyle(
                color = resolvedContentColor,
                fontSize = if (compact) 28.sp else 34.sp,
                lineHeight = if (compact) 32.sp else 38.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
            )
        )

        BasicText(
            "A place to share craft, find collaborators, and read the people you actually want to hear from.",
            style = TextStyle(
                color = resolvedContentColor.copy(alpha = 0.70f),
                fontSize = if (compact) 14.sp else 15.sp,
                lineHeight = if (compact) 20.sp else 22.sp
            )
        )
    }
}

@Composable
private fun currentAuthBackgroundColor(): Color {
    val appearance = currentVormexAppearance()
    return when {
        appearance.isDarkTheme -> appearance.backgroundColor
        appearance.backgroundColor == Color.Transparent -> AuthSurface
        else -> appearance.backgroundColor
    }
}

@Composable
private fun currentAuthTextColor(fallback: Color): Color {
    val appearance = currentVormexAppearance()
    return when {
        appearance.isDarkTheme -> appearance.contentColor
        fallback == Color.Transparent -> AuthInk
        else -> fallback
    }
}

@Composable
private fun currentAuthControlColor(): Color {
    val appearance = currentVormexAppearance()
    return if (appearance.isDarkTheme) appearance.controlColor else AuthSurface.copy(alpha = 0.86f)
}

@Composable
private fun currentAuthBorderColor(): Color {
    val appearance = currentVormexAppearance()
    return if (appearance.isDarkTheme) appearance.controlBorderColor else AuthLine
}

@Composable
internal fun AuthSectionLabel(
    title: String,
    subtitle: String,
    contentColor: Color
) {
    val resolvedContentColor = currentAuthTextColor(contentColor)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        BasicText(
            title,
            style = TextStyle(
                color = resolvedContentColor,
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
        BasicText(
            subtitle,
            style = TextStyle(
                color = resolvedContentColor.copy(alpha = 0.62f),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        )
    }
}

@Composable
internal fun AuthPrimaryButton(
    label: String,
    isLoading: Boolean,
    enabled: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = {
            if (!isLoading && enabled) {
                onClick()
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = accentColor,
            contentColor = Color.White,
            disabledContainerColor = accentColor.copy(alpha = 0.42f),
            disabledContentColor = Color.White.copy(alpha = 0.70f)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            BasicText(
                label,
                style = TextStyle(Color.White, 14.sp, FontWeight.SemiBold)
            )
        }
    }
}

@Composable
private fun AuthSocialButton(
    label: String,
    contentColor: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    leadingContent: @Composable () -> Unit,
    onClick: () -> Unit
) {
    val resolvedContentColor = currentAuthTextColor(contentColor)
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .height(46.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.56f }
            .clip(shape)
            .background(currentAuthControlColor(), shape)
            .border(1.dp, currentAuthBorderColor(), shape)
            .clickable(enabled = enabled && !isLoading, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = resolvedContentColor,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                leadingContent()
                BasicText(
                    label,
                    style = TextStyle(
                        color = resolvedContentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

@Composable
private fun AuthSavedAccountsAction(
    savedAccountsCount: Int,
    accentColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    if (savedAccountsCount <= 0) return
    val prompt = if (savedAccountsCount == 1) {
        "1 saved account"
    } else {
        "$savedAccountsCount saved accounts"
    }
    AuthTextActionRow(
        prompt = prompt,
        action = "Switch",
        accentColor = accentColor,
        contentColor = contentColor,
        onClick = onClick
    )
}

@Composable
internal fun AuthTextActionRow(
    prompt: String,
    action: String,
    accentColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(
            prompt,
            style = TextStyle(
                color = currentAuthTextColor(contentColor).copy(alpha = 0.62f),
                fontSize = 13.sp
            )
        )
        BasicText(
            action,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            style = TextStyle(
                color = accentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
internal fun AuthInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    hint: String? = null,
    iconRes: Int,
    accentColor: Color,
    supportingText: String? = null,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val colors = MaterialTheme.colorScheme
    val appearance = currentVormexAppearance()
    val resolvedTextColor = currentAuthTextColor(AuthInk)
    val resolvedMutedColor = resolvedTextColor.copy(alpha = 0.58f)
    val idleFieldColor = if (appearance.isDarkTheme) appearance.inputColor else AuthField
    val focusedFieldColor = if (appearance.isDarkTheme) appearance.controlColor else AuthSurface
    val idleBorderColor = if (appearance.isDarkTheme) appearance.inputBorderColor else AuthLine
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val fieldShape = RoundedCornerShape(12.dp)
    val fieldFill by animateColorAsState(
        targetValue = if (isFocused) {
            focusedFieldColor
        } else {
            idleFieldColor
        },
        label = "authFieldFill"
    )
    val fieldBorder by animateColorAsState(
        targetValue = if (isFocused) {
            accentColor.copy(alpha = 0.75f)
        } else {
            idleBorderColor
        },
        label = "authFieldBorder"
    )
    val visualTransformation =
        if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        BasicText(
            label,
            style = TextStyle(
                color = if (isFocused) accentColor else resolvedMutedColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        )

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = TextStyle(
                color = resolvedTextColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            ),
            cursorBrush = SolidColor(accentColor),
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(fieldShape)
                        .background(fieldFill, fieldShape)
                        .border(1.dp, fieldBorder, fieldShape)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFocused) {
                                    accentColor.copy(alpha = 0.16f)
                                } else {
                                    currentAuthControlColor()
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = null,
                            tint = if (isFocused) accentColor else resolvedMutedColor,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (value.isEmpty() && hint != null) {
                            BasicText(
                                hint,
                                style = TextStyle(
                                    color = resolvedMutedColor.copy(alpha = 0.72f),
                                    fontSize = 15.sp
                                )
                            )
                        }
                        innerTextField()
                    }

                    if (isPassword) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable { passwordVisible = !passwordVisible },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_visibility),
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = if (passwordVisible) accentColor else resolvedMutedColor,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
        )

        supportingText?.let {
            BasicText(
                it,
                modifier = Modifier.padding(horizontal = 4.dp),
                style = TextStyle(
                    color = colors.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            )
        }
    }
}

@Composable
private fun AuthPasswordStrength(
    password: String,
    accentColor: Color,
    contentColor: Color
) {
    if (password.isBlank()) return

    val score = listOf(
        password.length >= 8,
        password.any { it.isUpperCase() },
        password.any { it.isDigit() },
        password.any { !it.isLetterOrDigit() }
    ).count { it }
    val label = when {
        score <= 1 -> "Weak"
        score == 2 -> "Fair"
        score == 3 -> "Good"
        else -> "Strong"
    }
    val activeColor = when {
        score <= 1 -> Color(0xFFEF4444)
        score == 2 -> Color(0xFFF59E0B)
        else -> accentColor
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (index < score) activeColor else contentColor.copy(alpha = 0.12f)
                        )
                )
            }
        }
        BasicText(
            "Password strength: $label",
            style = TextStyle(
                color = activeColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun AuthDivider(contentColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(contentColor.copy(alpha = 0.18f))
        )
        BasicText(
            "or",
            style = TextStyle(
                color = contentColor.copy(alpha = 0.52f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(contentColor.copy(alpha = 0.18f))
        )
    }
}

@Composable
private fun AuthGoogleButton(
    contentColor: Color,
    isLoading: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    AuthSocialButton(
        label = "Continue with Google",
        contentColor = contentColor,
        enabled = isEnabled,
        isLoading = isLoading,
        modifier = Modifier.fillMaxWidth(),
        leadingContent = {
            Image(
                painter = painterResource(R.drawable.ic_google),
                contentDescription = "Google",
                modifier = Modifier.size(18.dp)
            )
        },
        onClick = onClick
    )
}

@Composable
internal fun AuthInlineError(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFFFF1F2).copy(alpha = 0.92f))
            .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        BasicText(
            message,
            style = TextStyle(
                color = Color(0xFFB91C1C),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        )
    }
}
