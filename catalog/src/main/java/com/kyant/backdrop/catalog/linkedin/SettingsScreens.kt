package com.kyant.backdrop.catalog.linkedin

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.kyant.backdrop.catalog.ui.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.BuildConfig
import com.kyant.backdrop.catalog.ads.VormexAdsManager
import com.kyant.backdrop.catalog.components.LiquidToggle
import com.kyant.backdrop.catalog.data.SettingsPreferences
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.GrowthApiService
import com.kyant.backdrop.catalog.network.models.ProfileUpdateRequest
import com.kyant.backdrop.catalog.notifications.PushTokenRegistrar
import com.kyant.backdrop.catalog.ui.VormexFontOptions
import kotlinx.coroutines.launch
import kotlin.math.abs

private data class HelpFaqItemData(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val answer: String
)

private const val ACCOUNT_DELETION_URL = "https://vormex.in/vormex-delete-account"

@Composable
private fun SettingsScreenContainer(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsIconBadge(
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(accentColor.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun Modifier.settingsSurface(
    contentColor: Color,
    cornerRadius: Dp = 18.dp,
    containerColor: Color? = null,
    outlineColor: Color? = null
): Modifier = composed {
    val appearance = currentVormexAppearance()
    val isDarkSurface = appearance.isDarkTheme
    val resolvedOutline = outlineColor ?: if (appearance.isGlassTheme) {
        if (isDarkSurface) {
            Color.White.copy(alpha = 0.14f)
        } else {
            Color.White.copy(alpha = 0.42f)
        }
    } else {
        appearance.cardBorderColor
    }
    val shape = RoundedCornerShape(cornerRadius)

    val base = this.clip(shape)

    val withBackground = if (containerColor != null) {
        base.background(containerColor)
    } else if (!appearance.isGlassTheme) {
        base.background(appearance.cardColor)
    } else {
        base.background(
            brush = Brush.verticalGradient(
                colors = if (isDarkSurface) {
                    listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.07f)
                    )
                } else {
                    listOf(
                        Color.White.copy(alpha = 0.34f),
                        Color.White.copy(alpha = 0.18f)
                    )
                }
            )
        )
    }

    withBackground
        .border(1.dp, resolvedOutline, shape)
}

// ==================== NOTIFICATION SETTINGS SCREEN ====================

@Composable
fun NotificationSettingsScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var permissionRefreshKey by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current

    fun refreshPermissionState() {
        permissionRefreshKey++
    }

    fun showPermissionDeniedToast(label: String) {
        Toast.makeText(
            context,
            "$label permission denied. You can enable it from app settings.",
            Toast.LENGTH_SHORT
        ).show()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissionState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        refreshPermissionState()
        if (granted) {
            PushTokenRegistrar.syncCurrentToken(context)
        } else {
            showPermissionDeniedToast("Notifications")
        }
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        refreshPermissionState()
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!granted) showPermissionDeniedToast("Location")
    }
    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        refreshPermissionState()
        if (!granted) showPermissionDeniedToast("Microphone")
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        refreshPermissionState()
        if (!granted) showPermissionDeniedToast("Camera")
    }
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        refreshPermissionState()
        if (!granted) showPermissionDeniedToast("Contacts")
    }

    // Check if system notifications are enabled
    val systemNotificationsEnabled = remember(permissionRefreshKey) {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    val notificationPermissionGranted = remember(permissionRefreshKey) {
        isRuntimePermissionGranted(context, Manifest.permission.POST_NOTIFICATIONS) ||
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
    }
    val locationPermissionGranted = remember(permissionRefreshKey) {
        isRuntimePermissionGranted(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
            isRuntimePermissionGranted(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    }
    val microphonePermissionGranted = remember(permissionRefreshKey) {
        isRuntimePermissionGranted(context, Manifest.permission.RECORD_AUDIO)
    }
    val cameraPermissionGranted = remember(permissionRefreshKey) {
        isRuntimePermissionGranted(context, Manifest.permission.CAMERA)
    }
    val contactsPermissionGranted = remember(permissionRefreshKey) {
        isRuntimePermissionGranted(context, Manifest.permission.READ_CONTACTS)
    }
    
    // Collect all notification preferences
    val pushEnabled by SettingsPreferences.pushNotificationsEnabled(context).collectAsState(initial = true)
    val dailyDigestEnabled by SettingsPreferences.dailyDigestEnabled(context).collectAsState(initial = true)
    val dailyDigestTime by SettingsPreferences.dailyDigestTime(context).collectAsState(initial = "09:00")
    val matchAlertsEnabled by SettingsPreferences.matchAlertsEnabled(context).collectAsState(initial = true)
    val messageNotificationsEnabled by SettingsPreferences.messageNotificationsEnabled(context).collectAsState(initial = true)
    val connectionNotificationsEnabled by SettingsPreferences.connectionNotificationsEnabled(context).collectAsState(initial = true)
    val likeNotificationsEnabled by SettingsPreferences.likeNotificationsEnabled(context).collectAsState(initial = true)
    val commentNotificationsEnabled by SettingsPreferences.commentNotificationsEnabled(context).collectAsState(initial = true)
    val streakRemindersEnabled by SettingsPreferences.streakRemindersEnabled(context).collectAsState(initial = true)
    val weeklySummaryEnabled by SettingsPreferences.weeklySummaryEnabled(context).collectAsState(initial = true)
    val showProfileLocation by SettingsPreferences.showProfileLocation(context).collectAsState(initial = true)

    SettingsScreenContainer(backdrop = backdrop, contentColor = contentColor, accentColor = accentColor) {
            SettingsHeader(
                title = "Permissions & notifications",
                contentColor = contentColor,
                onBack = onNavigateBack
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
            item {
                SettingsSectionHeader("App permissions", contentColor)
            }
            
            item {
                NotificationActionRow(
                    title = "Notifications",
                    subtitle = if (systemNotificationsEnabled) {
                        "Allowed for push alerts and message updates"
                    } else {
                        "Required for push alerts and message updates"
                    },
                    icon = if (systemNotificationsEnabled) {
                        Icons.Outlined.NotificationsActive
                    } else {
                        Icons.Outlined.NotificationsOff
                    },
                    contentColor = contentColor,
                    accentColor = accentColor,
                    trailingText = if (systemNotificationsEnabled) "Allowed" else "Allow",
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationPermissionGranted) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            openAppNotificationSettings(context)
                        }
                    }
                )
            }

            item {
                NotificationActionRow(
                    title = "Location",
                    subtitle = if (locationPermissionGranted) {
                        "Allowed for nearby people and device-based profile location"
                    } else {
                        "Needed for nearby people and location autofill"
                    },
                    icon = Icons.Outlined.LocationOn,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    trailingText = if (locationPermissionGranted) "Allowed" else "Allow",
                    onClick = {
                        if (locationPermissionGranted) {
                            openAppPermissionSettings(context)
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    }
                )
            }

            item {
                NotificationActionRow(
                    title = "Microphone",
                    subtitle = if (microphonePermissionGranted) {
                        "Allowed for voice notes, AI voice, and dictation"
                    } else {
                        "Needed for voice notes, AI voice, and dictation"
                    },
                    icon = Icons.Outlined.Mic,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    trailingText = if (microphonePermissionGranted) "Allowed" else "Allow",
                    onClick = {
                        if (microphonePermissionGranted) {
                            openAppPermissionSettings(context)
                        } else {
                            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                )
            }

            item {
                NotificationActionRow(
                    title = "Camera",
                    subtitle = if (cameraPermissionGranted) {
                        "Allowed for identity checks and future in-app capture"
                    } else {
                        "Needed for identity checks and in-app capture"
                    },
                    icon = Icons.Outlined.PhotoCamera,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    trailingText = if (cameraPermissionGranted) "Allowed" else "Allow",
                    onClick = {
                        if (cameraPermissionGranted) {
                            openAppPermissionSettings(context)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                )
            }

            item {
                NotificationActionRow(
                    title = "Contacts",
                    subtitle = if (contactsPermissionGranted) {
                        "Allowed for finding people you already know"
                    } else {
                        "Needed to find people from your contacts"
                    },
                    icon = Icons.Outlined.Contacts,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    trailingText = if (contactsPermissionGranted) "Allowed" else "Allow",
                    onClick = {
                        if (contactsPermissionGranted) {
                            openAppPermissionSettings(context)
                        } else {
                            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        }
                    }
                )
            }

            item {
                NotificationActionRow(
                    title = "Android app permissions",
                    subtitle = "Open system settings to manage every Vormex permission",
                    icon = Icons.Outlined.AdminPanelSettings,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    trailingText = "Manage",
                    onClick = {
                        openAppPermissionSettings(context)
                    }
                )
            }

            item {
                SettingsSectionHeader("Profile location", contentColor)
            }

            item {
                NotificationLiquidToggleItem(
                    title = "Show location on profile",
                    subtitle = "Control whether your profile shows your live/current location",
                    icon = Icons.Outlined.MyLocation,
                    checked = showProfileLocation,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onCheckedChange = {
                        coroutineScope.launch {
                            SettingsPreferences.setShowProfileLocation(context, it)
                        }
                    }
                )
            }

            item {
                SettingsSectionHeader("Notifications", contentColor)
            }
            
            item {
                NotificationLiquidToggleItem(
                    title = "Push notifications",
                    subtitle = "Master switch for all app alerts",
                    icon = Icons.Outlined.NotificationsNone,
                    checked = pushEnabled,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onCheckedChange = { 
                        coroutineScope.launch { 
                            SettingsPreferences.setPushNotificationsEnabled(context, it)
                            PushTokenRegistrar.setPushEnabled(context, it)
                        }
                    }
                )
            }
            
            // Activity notifications
            item {
                SettingsSectionHeader("Activity", contentColor)
            }
            
            item {
                NotificationLiquidToggleItem(
                    title = "Messages",
                    subtitle = "New messages and chat requests",
                    icon = Icons.Outlined.MarkChatUnread,
                    checked = messageNotificationsEnabled && pushEnabled,
                    enabled = pushEnabled,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onCheckedChange = { 
                        coroutineScope.launch { 
                            SettingsPreferences.setMessageNotificationsEnabled(context, it) 
                        }
                    }
                )
            }
            
            item {
                NotificationLiquidToggleItem(
                    title = "Connection requests",
                    subtitle = "Connection requests and acceptances",
                    icon = Icons.Outlined.PersonAddAlt,
                    checked = connectionNotificationsEnabled && pushEnabled,
                    enabled = pushEnabled,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onCheckedChange = { 
                        coroutineScope.launch { 
                            SettingsPreferences.setConnectionNotificationsEnabled(context, it) 
                        }
                    }
                )
            }
            
            item {
                NotificationLiquidToggleItem(
                    title = "Likes",
                    subtitle = "When someone likes your posts",
                    icon = Icons.Outlined.FavoriteBorder,
                    checked = likeNotificationsEnabled && pushEnabled,
                    enabled = pushEnabled,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onCheckedChange = { 
                        coroutineScope.launch { 
                            SettingsPreferences.setLikeNotificationsEnabled(context, it) 
                        }
                    }
                )
            }
            
            item {
                NotificationLiquidToggleItem(
                    title = "Comments",
                    subtitle = "When someone comments on your posts",
                    icon = Icons.Outlined.ModeComment,
                    checked = commentNotificationsEnabled && pushEnabled,
                    enabled = pushEnabled,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onCheckedChange = { 
                        coroutineScope.launch { 
                            SettingsPreferences.setCommentNotificationsEnabled(context, it) 
                        }
                    }
                )
            }
            
            // Match alerts
            item {
                SettingsSectionHeader("Discovery", contentColor)
            }
            
            item {
                NotificationLiquidToggleItem(
                    title = "Match Alerts",
                    subtitle = "Daily matches and recommendations",
                    icon = Icons.Outlined.TravelExplore,
                    checked = matchAlertsEnabled && pushEnabled,
                    enabled = pushEnabled,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onCheckedChange = { 
                        coroutineScope.launch { 
                            SettingsPreferences.setMatchAlertsEnabled(context, it) 
                        }
                    }
                )
            }
            
            // Engagement reminders
            item {
                SettingsSectionHeader("Reminders", contentColor)
            }
            
            item {
                NotificationLiquidToggleItem(
                    title = "Streak Reminders",
                    subtitle = "Don't lose your streak!",
                    icon = Icons.Outlined.LocalFireDepartment,
                    checked = streakRemindersEnabled && pushEnabled,
                    enabled = pushEnabled,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onCheckedChange = { 
                        coroutineScope.launch { 
                            SettingsPreferences.setStreakRemindersEnabled(context, it) 
                        }
                    }
                )
            }
            
            // Digests
            item {
                SettingsSectionHeader("Digests & Summaries", contentColor)
            }
            
            item {
                NotificationLiquidToggleItem(
                    title = "Daily Digest",
                    subtitle = "Daily summary at $dailyDigestTime",
                    icon = Icons.AutoMirrored.Outlined.Article,
                    checked = dailyDigestEnabled && pushEnabled,
                    enabled = pushEnabled,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onCheckedChange = { 
                        coroutineScope.launch { 
                            SettingsPreferences.setDailyDigestEnabled(context, it) 
                        }
                    }
                )
            }
            
            item {
                NotificationLiquidToggleItem(
                    title = "Weekly Summary",
                    subtitle = "Your week in review",
                    icon = Icons.Outlined.Insights,
                    checked = weeklySummaryEnabled && pushEnabled,
                    enabled = pushEnabled,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onCheckedChange = { 
                        coroutineScope.launch { 
                            SettingsPreferences.setWeeklySummaryEnabled(context, it) 
                        }
                    }
                )
            }
            
            item { Spacer(Modifier.height(80.dp)) }
            }
    }
}

@Composable
private fun NotificationActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    contentColor: Color,
    accentColor: Color,
    trailingText: String = "",
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NotificationPlainIcon(icon = icon, accentColor = accentColor)
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            BasicText(
                title,
                style = TextStyle(contentColor, 14.sp, FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            BasicText(
                subtitle,
                style = TextStyle(contentColor.copy(alpha = 0.58f), 11.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (trailingText.isNotBlank()) {
            Spacer(Modifier.width(10.dp))
            BasicText(
                trailingText,
                style = TextStyle(accentColor, 11.sp, FontWeight.SemiBold),
                maxLines = 1
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = contentColor.copy(alpha = 0.34f),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun NotificationPlainIcon(
    icon: ImageVector,
    accentColor: Color,
    alpha: Float = 1f
) {
    Box(
        modifier = Modifier.size(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor.copy(alpha = 0.88f * alpha),
            modifier = Modifier.size(21.dp)
        )
    }
}

@Composable
private fun NotificationLiquidToggleItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val alpha = if (enabled) 1f else 0.45f
    val subtitleColor = contentColor.copy(alpha = 0.58f * alpha)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 2.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NotificationPlainIcon(icon = icon, accentColor = accentColor, alpha = alpha)
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            BasicText(
                title,
                style = TextStyle(contentColor.copy(alpha = alpha), 14.sp, FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotEmpty()) {
                BasicText(
                    subtitle,
                    style = TextStyle(subtitleColor, 11.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!enabled) {
                BasicText(
                    "Disabled while push notifications are off",
                    style = TextStyle(accentColor.copy(alpha = 0.7f), 11.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.alpha(alpha)) {
            LiquidToggle(
                selected = { checked },
                onSelect = { if (enabled) onCheckedChange(it) },
                backdrop = backdrop,
                trackWidth = 58.dp,
                trackHeight = 26.dp,
                thumbWidth = 36.dp,
                thumbHeight = 22.dp
            )
        }
    }
}

// ==================== PRIVACY SETTINGS SCREEN ====================

@Composable
fun PrivacySettingsScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Collect privacy preferences
    val profileVisibility by SettingsPreferences.profileVisibility(context).collectAsState(initial = "public")
    val whoCanMessage by SettingsPreferences.whoCanMessage(context).collectAsState(initial = "everyone")
    val showOnlineStatus by SettingsPreferences.showOnlineStatus(context).collectAsState(initial = true)
    val showActivityStatus by SettingsPreferences.showActivityStatus(context).collectAsState(initial = true)
    val showProfileViews by SettingsPreferences.showProfileViews(context).collectAsState(initial = true)
    val discoverableByEmail by SettingsPreferences.discoverableByEmail(context).collectAsState(initial = true)
    val discoverableByPhone by SettingsPreferences.discoverableByPhone(context).collectAsState(initial = false)
    val showProfileLocation by SettingsPreferences.showProfileLocation(context).collectAsState(initial = true)
    val privacyOptionsRequired by VormexAdsManager.privacyOptionsRequired.collectAsState()
    var useLocationForDiscovery by rememberSaveable { mutableStateOf(true) }
    var isSavingLocationPrivacy by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        ApiClient.getCurrentLocation(context)
            .onSuccess { location ->
                SettingsPreferences.setShowProfileLocation(context, location.shareLocationPublic)
                useLocationForDiscovery = location.locationPermission
            }
    }

    SettingsScreenContainer(backdrop = backdrop, contentColor = contentColor, accentColor = accentColor) {
        SettingsHeader(
            title = "Privacy",
            contentColor = contentColor,
            onBack = onNavigateBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.padding(start = 2.dp, end = 2.dp, bottom = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BasicText(
                        "Choose what Vormex can show about you.",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    BasicText(
                        "Changes save immediately and location privacy is synced with your account.",
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.58f),
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    )
                }
            }

            item {
                SettingsOptionItem(
                    title = "Who can see your profile",
                    subtitle = when (profileVisibility) {
                        "public" -> "Everyone"
                        "connections" -> "Only connections"
                        "private" -> "Only you"
                        else -> "Everyone"
                    },
                    icon = Icons.Outlined.Visibility,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    options = listOf(
                        "public" to "Everyone",
                        "connections" to "Only connections",
                        "private" to "Only you"
                    ),
                    selectedOption = profileVisibility,
                    flat = true,
                    onOptionSelected = { 
                        coroutineScope.launch { 
                            SettingsPreferences.setProfileVisibility(context, it) 
                        }
                    }
                )
            }

            item {
                SettingsOptionItem(
                    title = "Who can message you",
                    subtitle = when (whoCanMessage) {
                        "everyone" -> "Everyone"
                        "connections" -> "Only connections"
                        "none" -> "No one"
                        else -> "Everyone"
                    },
                    icon = Icons.Outlined.ChatBubbleOutline,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    options = listOf(
                        "everyone" to "Everyone",
                        "connections" to "Only connections",
                        "none" to "No one"
                    ),
                    selectedOption = whoCanMessage,
                    flat = true,
                    onOptionSelected = { 
                        coroutineScope.launch { 
                            SettingsPreferences.setWhoCanMessage(context, it) 
                        }
                    }
                )
            }

            item {
                SettingsSectionHeader("Activity", contentColor)
            }

            item {
                NotificationLiquidToggleItem(
                    title = "Online status",
                    subtitle = if (showOnlineStatus) "People can see when you're online" else "Your online state stays hidden",
                    icon = Icons.Outlined.Lens,
                    checked = showOnlineStatus,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onCheckedChange = { 
                        coroutineScope.launch { 
                            SettingsPreferences.setShowOnlineStatus(context, it) 
                        }
                    }
                )
            }

            item {
                NotificationLiquidToggleItem(
                    title = "Activity status",
                    subtitle = if (showActivityStatus) "Shows recent active time" else "Recent active time is hidden",
                    icon = Icons.Outlined.Schedule,
                    checked = showActivityStatus,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onCheckedChange = { 
                        coroutineScope.launch { 
                            SettingsPreferences.setShowActivityStatus(context, it) 
                        }
                    }
                )
            }

            item {
                NotificationLiquidToggleItem(
                    title = "Profile views",
                    subtitle = if (showProfileViews) "Profile view insights stay enabled" else "Profile view insights are off",
                    icon = Icons.Outlined.Visibility,
                    checked = showProfileViews,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onCheckedChange = { 
                        coroutineScope.launch { 
                            SettingsPreferences.setShowProfileViews(context, it) 
                        }
                    }
                )
            }

            item {
                SettingsSectionHeader("Location", contentColor)
            }

            item {
                NotificationLiquidToggleItem(
                    title = "Show profile location",
                    subtitle = if (showProfileLocation) {
                        "Your profile can show your saved/current location"
                    } else {
                        "Your profile location is hidden from others"
                    },
                    icon = Icons.Outlined.MyLocation,
                    checked = showProfileLocation,
                    enabled = !isSavingLocationPrivacy,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onCheckedChange = { enabled ->
                        if (!isSavingLocationPrivacy) {
                            val previous = showProfileLocation
                            coroutineScope.launch {
                                isSavingLocationPrivacy = true
                                SettingsPreferences.setShowProfileLocation(context, enabled)
                                ApiClient.updateLocationSettings(context, shareLocationPublic = enabled)
                                    .onFailure {
                                        SettingsPreferences.setShowProfileLocation(context, previous)
                                        Toast.makeText(context, "Couldn't update profile location privacy.", Toast.LENGTH_SHORT).show()
                                    }
                                isSavingLocationPrivacy = false
                            }
                        }
                    }
                )
            }

            item {
                NotificationLiquidToggleItem(
                    title = "Nearby discovery",
                    subtitle = if (useLocationForDiscovery) {
                        "Your location can be used for nearby people"
                    } else {
                        "Nearby people will not use your location"
                    },
                    icon = Icons.Outlined.TravelExplore,
                    checked = useLocationForDiscovery,
                    enabled = !isSavingLocationPrivacy,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onCheckedChange = { enabled ->
                        if (!isSavingLocationPrivacy) {
                            val previous = useLocationForDiscovery
                            coroutineScope.launch {
                                isSavingLocationPrivacy = true
                                useLocationForDiscovery = enabled
                                ApiClient.updateLocationSettings(context, locationPermission = enabled)
                                    .onFailure {
                                        useLocationForDiscovery = previous
                                        Toast.makeText(context, "Couldn't update location discovery.", Toast.LENGTH_SHORT).show()
                                    }
                                isSavingLocationPrivacy = false
                            }
                        }
                    }
                )
            }

            item {
                SettingsSectionHeader("Discoverability", contentColor)
            }

            item {
                NotificationLiquidToggleItem(
                    title = "Email lookup",
                    subtitle = if (discoverableByEmail) "People can find you by email" else "Email lookup is off",
                    icon = Icons.Outlined.AlternateEmail,
                    checked = discoverableByEmail,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onCheckedChange = { 
                        coroutineScope.launch { 
                            SettingsPreferences.setDiscoverableByEmail(context, it) 
                        }
                    }
                )
            }

            item {
                NotificationLiquidToggleItem(
                    title = "Phone lookup",
                    subtitle = if (discoverableByPhone) "People can find you by phone" else "Phone lookup is off",
                    icon = Icons.Outlined.PhoneAndroid,
                    checked = discoverableByPhone,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onCheckedChange = { 
                        coroutineScope.launch { 
                            SettingsPreferences.setDiscoverableByPhone(context, it) 
                        }
                    }
                )
            }

            item {
                SettingsSectionHeader("Account data", contentColor)
            }

            if (privacyOptionsRequired) {
                item {
                    NotificationActionRow(
                        title = "Ad privacy choices",
                        subtitle = "Manage ad consent and privacy options",
                        icon = Icons.Outlined.Tune,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onClick = {
                            VormexAdsManager.showPrivacyOptions(context)
                        }
                    )
                }
            }

            item {
                NotificationActionRow(
                    title = "Delete account",
                    subtitle = "Request account and associated data deletion",
                    icon = Icons.Outlined.DeleteOutline,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = {
                        openUrl(context, ACCOUNT_DELETION_URL)
                    }
                )
            }
            
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ==================== APPEARANCE SETTINGS SCREEN ====================

@Composable
fun AppearanceSettingsScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onNavigateBack: () -> Unit,
    canAccessProfileCustomization: Boolean = false,
    onThemeChange: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Collect appearance preferences
    val themeMode by SettingsPreferences.themeMode(context).collectAsState(initial = DefaultThemeModeKey)
    val glassBackgroundKey by SettingsPreferences.glassBackgroundPreset(context)
        .collectAsState(initial = DefaultGlassBackgroundPresetKey)
    val reduceAnimations by SettingsPreferences.reduceAnimations(context).collectAsState(initial = false)
    val fontFamily by SettingsPreferences.fontFamily(context)
        .collectAsState(initial = SettingsPreferences.FONT_FAMILY_SYSTEM)
    val profileTheme by SettingsPreferences.profileTheme(context)
        .collectAsState(initial = DefaultProfileThemeKey)
    val showReelsOnHome by SettingsPreferences.showReelsOnHome(context).collectAsState(initial = false)

    SettingsScreenContainer(backdrop = backdrop, contentColor = contentColor, accentColor = accentColor) {
        SettingsHeader(
            title = "Appearance",
            contentColor = contentColor,
            onBack = onNavigateBack
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Section
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    BasicText(
                        "Choose Theme",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    
                    BasicText(
                        "Select how Vormex looks to you",
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    )
                }
            }
            
            // Theme Preview Cards
            item {
                val selectedThemeKey = VormexThemeMode.fromKey(themeMode).key
                val themeOptions = listOf(
                    "glass" to "Glass",
                    "warm_paper" to "Warm Paper",
                    "black_green" to "Black Green",
                    "light" to "White",
                    "dark" to "Dark"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    themeOptions.forEach { (themeKey, themeName) ->
                        ThemePreviewCard(
                            modifier = Modifier.width(132.dp),
                            themeName = themeName,
                            themeKey = themeKey,
                            isSelected = selectedThemeKey == themeKey,
                            contentColor = contentColor,
                            backdrop = backdrop,
                            accentColor = accentColor,
                            onClick = {
                                coroutineScope.launch {
                                    SettingsPreferences.setThemeMode(context, themeKey)
                                    onThemeChange(themeKey)
                                }
                            }
                        )
                    }
                }
            }

            if (themeMode == "glass") {
                item {
                    SettingsSubsectionHeader(
                        title = "Glass Background",
                        subtitle = "Pick the scene behind every glass surface in the app.",
                        contentColor = contentColor
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GlassBackgroundPresets.forEach { preset ->
                            GlassBackgroundChoiceCard(
                                preset = preset,
                                isSelected = glassBackgroundKey == preset.key,
                                contentColor = contentColor,
                                backdrop = backdrop,
                                accentColor = accentColor,
                                previewAccentColor = accentColor,
                                onClick = {
                                    coroutineScope.launch {
                                        SettingsPreferences.setGlassBackgroundPreset(context, preset.key)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            item {
                SettingsOptionItem(
                    title = "Font Family",
                    subtitle = "Change the typeface used across Vormex",
                    icon = Icons.Outlined.TextFields,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    options = VormexFontOptions.map { it.key to it.label },
                    selectedOption = fontFamily,
                    onOptionSelected = { nextFontFamily ->
                        coroutineScope.launch {
                            SettingsPreferences.setFontFamily(context, nextFontFamily)
                        }
                    }
                )
            }

            if (canAccessProfileCustomization) {
                item {
                    SettingsOptionItem(
                        title = "Profile Theme",
                        subtitle = "Choose how your public profile is styled for everyone",
                        icon = Icons.Outlined.SportsEsports,
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        options = VormexProfileThemeOptions.map { it.key to it.label },
                        selectedOption = profileTheme,
                        onOptionSelected = { nextTheme ->
                            val normalizedTheme = normalizeProfileThemeKey(nextTheme)
                            coroutineScope.launch {
                                SettingsPreferences.setProfileTheme(context, normalizedTheme)
                                val explicitNullFields = if (normalizedTheme == DefaultProfileThemeKey) {
                                    setOf("profileTheme")
                                } else {
                                    emptySet()
                                }
                                val request = ProfileUpdateRequest(
                                    profileTheme = normalizedTheme.takeUnless { it == DefaultProfileThemeKey }
                                )
                                ApiClient.updateProfile(context, request, explicitNullFields)
                                    .onSuccess {
                                        Toast.makeText(context, "Profile theme updated", Toast.LENGTH_SHORT).show()
                                    }
                                    .onFailure { error ->
                                        SettingsPreferences.setProfileTheme(context, profileTheme)
                                        Toast.makeText(
                                            context,
                                            error.message ?: "Couldn't update profile theme",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }
                        }
                    )
                }
            }

            item {
                SettingsSwitchItem(
                    title = "Show Reels on Home Page",
                    subtitle = "Display the trending reels row above your home feed",
                    icon = Icons.Outlined.VideoLibrary,
                    checked = showReelsOnHome,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onCheckedChange = {
                        coroutineScope.launch {
                            SettingsPreferences.setShowReelsOnHome(context, it)
                        }
                    }
                )
            }

            // Accessibility Section
            item {
                BasicText(
                    "Accessibility",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            
            item {
                SettingsSwitchItem(
                    title = "Reduce Animations",
                    subtitle = "Minimize motion effects for better accessibility",
                    icon = Icons.Outlined.Tune,
                    checked = reduceAnimations,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onCheckedChange = { 
                        coroutineScope.launch { 
                            SettingsPreferences.setReduceAnimations(context, it) 
                        }
                    }
                )
            }
            
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun ThemePreviewCard(
    modifier: Modifier = Modifier,
    themeName: String,
    themeKey: String,
    isSelected: Boolean,
    contentColor: Color,
    backdrop: LayerBackdrop,
    accentColor: Color,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else Color.Transparent,
        label = "borderColor"
    )
    
    Column(
        modifier = modifier
            .settingsSurface(
                contentColor = contentColor,
                cornerRadius = 20.dp
            )
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Theme Preview Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    when (themeKey) {
                        "glass" -> Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF64B5F6),
                                Color(0xFF42A5F5),
                                Color(0xFF26C6DA),
                                Color(0xFF4DD0E1)
                            )
                        )
                        "warm_paper" -> Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFFFCF7),
                                Color(0xFFF7F2EA),
                                Color(0xFFEAD8C3)
                            )
                        )
                        "black_green" -> Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF020604),
                                Color(0xFF0B1F13),
                                Color(0xFF22C55E)
                            )
                        )
                        "light" -> Brush.linearGradient(
                            colors = listOf(Color.White, Color(0xFFF5F5F5))
                        )
                        "dark" -> Brush.linearGradient(
                            colors = listOf(Color(0xFF1A1A1A), Color.Black)
                        )
                        "midnight_neon" -> Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF050A12),
                                Color(0xFF083B5C),
                                Color(0xFF12C8FF)
                            )
                        )
                        "soft_graphite" -> Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFF8F9FB),
                                Color(0xFFE7EAEE),
                                Color(0xFFB8C1CC)
                            )
                        )
                        "emerald_focus" -> Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFF2FBF7),
                                Color(0xFFDDF8EA),
                                Color(0xFF18A66F)
                            )
                        )
                        else -> Brush.linearGradient(
                            colors = listOf(Color(0xFF64B5F6), Color(0xFF26C6DA))
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            // Mini phone UI preview
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Status bar mockup
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            when (themeKey) {
                                "dark", "midnight_neon" -> Color.White.copy(alpha = 0.2f)
                                "black_green" -> Color(0xFF8EF7B2).copy(alpha = 0.24f)
                                "warm_paper" -> Color(0xFF2F2A24).copy(alpha = 0.12f)
                                else -> Color.Black.copy(alpha = 0.1f)
                            }
                        )
                )
                
                Spacer(Modifier.height(4.dp))
                
                // Content preview cards
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when (themeKey) {
                                    "dark", "midnight_neon" -> Color.White.copy(alpha = 0.15f)
                                    "glass" -> Color.White.copy(alpha = 0.4f)
                                    "black_green" -> Color(0xFF36D072).copy(alpha = 0.22f)
                                    "warm_paper" -> Color(0xFFC96442).copy(alpha = 0.18f)
                                    "soft_graphite" -> Color.Black.copy(alpha = 0.12f)
                                    "emerald_focus" -> Color(0xFF047857).copy(alpha = 0.18f)
                                    else -> Color.Black.copy(alpha = 0.08f)
                                }
                            )
                    )
                    Spacer(Modifier.height(4.dp))
                }
                
                Spacer(Modifier.weight(1f))
                
                // Nav bar mockup
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    repeat(5) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when (themeKey) {
                                        "dark", "midnight_neon" -> Color.White.copy(alpha = 0.3f)
                                        "black_green" -> Color(0xFF36D072).copy(alpha = 0.54f)
                                        "warm_paper" -> Color(0xFFC96442).copy(alpha = 0.42f)
                                        "emerald_focus" -> Color(0xFF047857).copy(alpha = 0.34f)
                                        else -> Color.Black.copy(alpha = 0.2f)
                                    }
                                )
                        )
                    }
                }
            }
        }
        
        // Theme name and icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = when (themeKey) {
                    "glass" -> Icons.Outlined.BlurOn
                    "warm_paper" -> Icons.Outlined.Palette
                    "black_green" -> Icons.Outlined.DarkMode
                    "light" -> Icons.Outlined.LightMode
                    "dark" -> Icons.Outlined.DarkMode
                    "midnight_neon" -> Icons.Outlined.DarkMode
                    "soft_graphite" -> Icons.Outlined.Palette
                    "emerald_focus" -> Icons.Outlined.Palette
                    else -> Icons.Outlined.Palette
                },
                contentDescription = null,
                tint = if (isSelected) accentColor else contentColor.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(4.dp))
            BasicText(
                themeName,
                style = TextStyle(
                    color = if (isSelected) accentColor else contentColor,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // Selection indicator
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        } else {
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SettingsSubsectionHeader(
    title: String,
    subtitle: String,
    contentColor: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        BasicText(
            title,
            style = TextStyle(
                color = contentColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        )
        BasicText(
            subtitle,
            style = TextStyle(
                color = contentColor.copy(alpha = 0.62f),
                fontSize = 13.sp
            )
        )
    }
}

@Composable
private fun GlassBackgroundChoiceCard(
    preset: GlassBackgroundPreset,
    isSelected: Boolean,
    contentColor: Color,
    backdrop: LayerBackdrop,
    accentColor: Color,
    previewAccentColor: Color,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else Color.Transparent,
        label = "glassBackgroundBorder"
    )

    Column(
        modifier = Modifier
            .width(152.dp)
            .settingsSurface(
                contentColor = contentColor,
                cornerRadius = 20.dp
            )
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(186.dp)
                .clip(RoundedCornerShape(14.dp))
        ) {
            GlassBackgroundPreview(
                modifier = Modifier.fillMaxSize(),
                presetKey = preset.key,
                accentColor = previewAccentColor
            )
        }

        BasicText(
            preset.name,
            style = TextStyle(
                color = contentColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        )

        BasicText(
            preset.description,
            style = TextStyle(
                color = contentColor.copy(alpha = 0.62f),
                fontSize = 12.sp
            )
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(accentColor.copy(alpha = 0.16f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                BasicText(
                    "Selected",
                    style = TextStyle(
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

// ==================== HELP & FAQ SCREEN ====================

@Composable
fun HelpScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var expandedFaqId by rememberSaveable { mutableStateOf<String?>(null) }
    val faqs = remember {
        listOf(
            HelpFaqItemData(
                id = "post",
                icon = Icons.Outlined.Edit,
                title = "How to create a post",
                answer = "Use the center create tab to publish text, images, videos, links, polls, or articles. After posting, go back to Home and your content should appear without reloading the entire app."
            ),
            HelpFaqItemData(
                id = "connect",
                icon = Icons.Outlined.PersonAddAlt,
                title = "How to connect with others",
                answer = "Open Find, choose Smart, For You, Campus, or Nearby, then tap Connect on people who look relevant. The app now ranks active and more complete profiles higher to make this section feel more useful."
            ),
            HelpFaqItemData(
                id = "notifications",
                icon = Icons.Outlined.NotificationsNone,
                title = "Why notifications may not arrive",
                answer = "Check Android system notifications first, then make sure push notifications are enabled inside Vormex. If the app was force-stopped from Android Settings, Firebase will stay blocked until the app is opened again."
            ),
            HelpFaqItemData(
                id = "goals",
                icon = Icons.Outlined.TrackChanges,
                title = "Understanding weekly goals",
                answer = "Weekly goals track activity like connections and posting. Progress, streaks, and retention cards now stay cached for a few minutes, so reopening these sections should feel much faster."
            ),
            HelpFaqItemData(
                id = "password",
                icon = Icons.Outlined.Lock,
                title = "How to change password",
                answer = "If you signed up with email and password, use the account flow on web or contact support from this screen and ask for a password reset. Include the email tied to your Vormex account."
            ),
            HelpFaqItemData(
                id = "account",
                icon = Icons.Outlined.DeleteOutline,
                title = "How to delete your account",
                answer = "Open Privacy settings and choose Delete account, or visit $ACCOUNT_DELETION_URL. Send the request from the email linked to your account and include your username plus a clear confirmation."
            )
        )
    }
    val filteredFaqs = remember(query) {
        val normalizedQuery = query.trim().lowercase()
        if (normalizedQuery.isBlank()) {
            faqs
        } else {
            faqs.filter {
                it.title.lowercase().contains(normalizedQuery) ||
                    it.answer.lowercase().contains(normalizedQuery)
            }
        }
    }
    SettingsScreenContainer(backdrop = backdrop, contentColor = contentColor, accentColor = accentColor) {
        SettingsHeader(
            title = "Help & FAQ",
            contentColor = contentColor,
            onBack = onNavigateBack
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                HelpIntroCard(
                    contentColor = contentColor
                )
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = contentColor.copy(alpha = 0.48f),
                            modifier = Modifier.size(19.dp)
                        )
                    },
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Clear search",
                                    tint = contentColor.copy(alpha = 0.48f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    placeholder = {
                        Text("Search help topics")
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = contentColor.copy(alpha = 0.34f),
                        unfocusedBorderColor = contentColor.copy(alpha = 0.16f),
                        focusedTextColor = contentColor,
                        unfocusedTextColor = contentColor,
                        cursorColor = accentColor,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedPlaceholderColor = contentColor.copy(alpha = 0.45f),
                        unfocusedPlaceholderColor = contentColor.copy(alpha = 0.45f)
                    )
                )
            }

            item {
                HelpSectionTitle(
                    title = "Quick fixes",
                    detail = "3 shortcuts",
                    contentColor = contentColor
                )
            }

            item {
                HelpActionCard(
                    title = "Notification troubleshooting",
                    subtitle = "Open Android notification settings for Vormex",
                    icon = Icons.Outlined.NotificationsActive,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = { openAppNotificationSettings(context) }
                )
                HelpRowDivider(contentColor)
            }

            item {
                HelpActionCard(
                    title = "Copy app diagnostics",
                    subtitle = "Version, device, Android, and backend endpoint",
                    icon = Icons.Outlined.Science,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = {
                        copyText(
                            context = context,
                            label = "Vormex diagnostics",
                            text = buildDiagnosticsText()
                        )
                    }
                )
                HelpRowDivider(contentColor)
            }

            item {
                HelpActionCard(
                    title = "Contact support",
                    subtitle = "Open email with support details filled in",
                    icon = Icons.Outlined.Email,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = {
                        launchEmail(
                            context = context,
                            to = "support@vormex.in",
                            subject = "Help Request - Vormex Android",
                            body = "Hi Vormex Support,\n\nI need help with:\n\n${buildDiagnosticsText()}"
                        )
                    }
                )
            }

            item {
                HelpSectionTitle(
                    title = "FAQ",
                    detail = "${filteredFaqs.size} topic${if (filteredFaqs.size == 1) "" else "s"}",
                    contentColor = contentColor
                )
            }

            if (filteredFaqs.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp)) {
                        BasicText(
                            "No help topics matched \"$query\". Try a shorter phrase like notifications, post, password, or account.",
                            style = TextStyle(contentColor.copy(alpha = 0.68f), 13.sp)
                        )
                    }
                }
            } else {
                items(filteredFaqs, key = { it.id }) { faq ->
                    HelpFaqCard(
                        faq = faq,
                        expanded = expandedFaqId == faq.id,
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onClick = {
                            expandedFaqId = if (expandedFaqId == faq.id) null else faq.id
                        }
                    )
                    if (faq != filteredFaqs.last()) {
                        HelpRowDivider(contentColor)
                    }
                }
            }

            item {
                HelpSectionTitle(
                    title = "Need more help?",
                    detail = "Support",
                    contentColor = contentColor
                )
            }
            
            item {
                HelpActionCard(
                    title = "Visit Help Center",
                    subtitle = "Full documentation and guides",
                    icon = Icons.Outlined.Language,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    trailingIcon = Icons.AutoMirrored.Outlined.ArrowForward,
                    onClick = {
                        openUrl(context, "https://vormex.in/help")
                    }
                )
                HelpRowDivider(contentColor)
            }
            
            item {
                HelpActionCard(
                    title = "Contact Support",
                    subtitle = "Get help from our team",
                    icon = Icons.Outlined.SupportAgent,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    trailingIcon = Icons.AutoMirrored.Outlined.ArrowForward,
                    onClick = {
                        launchEmail(
                            context = context,
                            to = "support@vormex.in",
                            subject = "Help Request - Vormex App",
                            body = buildDiagnosticsText()
                        )
                    }
                )
            }
            
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ==================== ABOUT SCREEN ====================

@Composable
fun AboutScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    SettingsScreenContainer(backdrop = backdrop, contentColor = contentColor, accentColor = accentColor) {
        SettingsHeader(
            title = "About",
            contentColor = contentColor,
            onBack = onNavigateBack
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            AboutBrandIntro(contentColor = contentColor)

            HelpSectionTitle(
                title = "Legal",
                detail = "Policies",
                contentColor = contentColor
            )

            HelpActionCard(
                title = "Terms of Service",
                subtitle = "Rules for using Vormex",
                icon = Icons.Outlined.Gavel,
                contentColor = contentColor,
                accentColor = accentColor,
                onClick = {
                    openUrl(context, "https://vormex.in/terms")
                }
            )
            HelpRowDivider(contentColor)

            HelpActionCard(
                title = "Privacy Policy",
                subtitle = "How your data is handled",
                icon = Icons.Outlined.Policy,
                contentColor = contentColor,
                accentColor = accentColor,
                onClick = {
                    openUrl(context, "https://vormex.in/privacy")
                }
            )

            HelpSectionTitle(
                title = "Connect",
                detail = "Links",
                contentColor = contentColor
            )

            HelpActionCard(
                title = "Follow us on Twitter",
                subtitle = "@VormexApp",
                icon = Icons.Outlined.AlternateEmail,
                contentColor = contentColor,
                accentColor = accentColor,
                onClick = {
                    openUrl(context, "https://twitter.com/VormexApp")
                }
            )
            HelpRowDivider(contentColor)

            HelpActionCard(
                title = "Visit our website",
                subtitle = "vormex.in",
                icon = Icons.Outlined.Language,
                contentColor = contentColor,
                accentColor = accentColor,
                onClick = {
                    openUrl(context, "https://vormex.in")
                }
            )
            
            // Copyright
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    "© 2026 Vormex. All rights reserved.",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.4f),
                        fontSize = 12.sp
                    )
                )
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

// ==================== INVITE FRIENDS SCREEN ====================

@Composable
fun InviteFriendsScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onNavigateBack: () -> Unit,
    referralCode: String = "VORMEX2026"
) {
    val context = LocalContext.current
    var resolvedReferralCode by remember { mutableStateOf(referralCode) }
    var resolvedInviteLink by remember { mutableStateOf("https://vormex.in/login?mode=signup&ref=$referralCode") }
    var isLoadingReferral by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        GrowthApiService.getReferralShareLinks(context)
            .onSuccess { shareLinks ->
                resolvedReferralCode = shareLinks.code.ifBlank { referralCode }
                resolvedInviteLink = shareLinks.link.ifBlank {
                    "https://vormex.in/login?mode=signup&ref=$resolvedReferralCode"
                }
                isLoadingReferral = false
            }
            .onFailure {
                GrowthApiService.getReferralCode(context)
                    .onSuccess { code ->
                        resolvedReferralCode = code
                        resolvedInviteLink = "https://vormex.in/login?mode=signup&ref=$code"
                    }
                isLoadingReferral = false
            }
    }

    val inviteMessage = remember(resolvedReferralCode, resolvedInviteLink) {
        buildInviteMessage(resolvedReferralCode, resolvedInviteLink)
    }

    SettingsScreenContainer(backdrop = backdrop, contentColor = contentColor, accentColor = accentColor) {
        SettingsHeader(
            title = "Invite Friends",
            contentColor = contentColor,
            onBack = onNavigateBack
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            InviteIntroBlock(contentColor = contentColor)

            HelpSectionTitle(
                title = "Referral",
                detail = if (isLoadingReferral) "Loading" else "Ready",
                contentColor = contentColor
            )

            InviteReferralBlock(
                referralCode = resolvedReferralCode,
                inviteLink = resolvedInviteLink,
                isLoading = isLoadingReferral,
                contentColor = contentColor
            )
            HelpRowDivider(contentColor, start = 18.dp)

            HelpActionCard(
                title = "Copy invite link",
                subtitle = if (isLoadingReferral) "Loading your share link..." else resolvedInviteLink,
                icon = Icons.Outlined.Link,
                contentColor = contentColor,
                accentColor = accentColor,
                onClick = {
                    copyText(context, "Invite link", resolvedInviteLink)
                }
            )
            HelpRowDivider(contentColor)

            HelpActionCard(
                title = "Copy referral code",
                subtitle = resolvedReferralCode,
                icon = Icons.Outlined.ContentCopy,
                contentColor = contentColor,
                accentColor = accentColor,
                onClick = {
                    copyText(context, "Referral code", resolvedReferralCode)
                }
            )
            HelpRowDivider(contentColor)

            HelpActionCard(
                title = "Copy invite message",
                subtitle = "Ready to paste anywhere",
                icon = Icons.Outlined.Drafts,
                contentColor = contentColor,
                accentColor = accentColor,
                onClick = {
                    copyText(context, "Invite message", inviteMessage)
                }
            )

            HelpSectionTitle(
                title = "Share via",
                detail = "3 options",
                contentColor = contentColor
            )

            HelpActionCard(
                title = "Message",
                subtitle = "Send with SMS",
                icon = Icons.Outlined.Sms,
                contentColor = contentColor,
                accentColor = accentColor,
                onClick = {
                    shareInvite(context, "sms", inviteMessage)
                }
            )
            HelpRowDivider(contentColor)

            HelpActionCard(
                title = "Email",
                subtitle = "Send a ready email",
                icon = Icons.Outlined.Email,
                contentColor = contentColor,
                accentColor = accentColor,
                onClick = {
                    shareInvite(context, "email", inviteMessage)
                }
            )
            HelpRowDivider(contentColor)

            HelpActionCard(
                title = "Share sheet",
                subtitle = "Choose WhatsApp, Instagram, or any app",
                icon = Icons.Outlined.Share,
                contentColor = contentColor,
                accentColor = accentColor,
                onClick = {
                    shareInvite(context, "share", inviteMessage)
                }
            )

            HelpSectionTitle(
                title = "What friends receive",
                detail = "Preview",
                contentColor = contentColor
            )
            SettingsFlatNote(
                text = "A direct signup link, your referral code, and a short note about networking, creators, and opportunities on Vormex.",
                contentColor = contentColor
            )

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun AboutBrandIntro(
    contentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 2.dp, top = 8.dp, end = 2.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(42.dp),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                "V",
                style = TextStyle(
                    color = contentColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            BasicText(
                "Vormex",
                style = TextStyle(
                    color = contentColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            BasicText(
                "Built for dreamers, creators, and networkers",
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.62f),
                    fontSize = 12.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun InviteIntroBlock(
    contentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 2.dp, top = 8.dp, end = 2.dp, bottom = 14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        HelpPlainIcon(
            icon = Icons.Outlined.GroupAdd,
            contentColor = contentColor,
            modifier = Modifier.size(26.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            BasicText(
                "Invite friends and grow together",
                style = TextStyle(
                    color = contentColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
            BasicText(
                "Share Vormex with people who should discover better connections and opportunities.",
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.62f),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            )
        }
    }
}

@Composable
private fun InviteReferralBlock(
    referralCode: String,
    inviteLink: String,
    isLoading: Boolean,
    contentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BasicText(
                "Your code",
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.58f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            BasicText(
                if (isLoading) "Syncing" else "Active",
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.52f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        Box(
            modifier = Modifier
                .border(1.dp, contentColor.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            BasicText(
                referralCode,
                style = TextStyle(
                    color = contentColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        BasicText(
            inviteLink,
            style = TextStyle(
                color = contentColor.copy(alpha = 0.54f),
                fontSize = 12.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SettingsFlatNote(
    text: String,
    contentColor: Color
) {
    BasicText(
        text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        style = TextStyle(
            color = contentColor.copy(alpha = 0.68f),
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
    )
}

@Composable
private fun ContactIntroBlock(
    contentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 2.dp, top = 8.dp, end = 2.dp, bottom = 14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        HelpPlainIcon(
            icon = Icons.Outlined.SupportAgent,
            contentColor = contentColor,
            modifier = Modifier.size(26.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            BasicText(
                "Support toolkit",
                style = TextStyle(
                    color = contentColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
            BasicText(
                "Send a bug report, suggest a feature, or copy diagnostics before you write in.",
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.62f),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            )
        }
    }
}

private fun shareInvite(context: Context, method: String, shareText: String) {
    when (method) {
        "share" -> {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            launchIntentSafely(context, Intent.createChooser(intent, "Share via"))
        }
        "email" -> {
            launchEmail(
                context = context,
                to = "",
                subject = "Join me on Vormex!",
                body = shareText
            )
        }
        "sms" -> {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:")
                putExtra("sms_body", shareText)
            }
            launchIntentSafely(context, intent)
        }
    }
}

// ==================== CONTACT US SCREEN ====================

@Composable
fun ContactScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    
    SettingsScreenContainer(backdrop = backdrop, contentColor = contentColor, accentColor = accentColor) {
        SettingsHeader(
            title = "Contact Us",
            contentColor = contentColor,
            onBack = onNavigateBack
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                ContactIntroBlock(contentColor = contentColor)
            }

            item {
                HelpSectionTitle(
                    title = "Quick actions",
                    detail = "Before writing",
                    contentColor = contentColor
                )
            }

            item {
                HelpActionCard(
                    title = "Open help center",
                    subtitle = "Read onboarding and troubleshooting guides",
                    icon = Icons.Outlined.Language,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = {
                        openUrl(context, "https://vormex.in/help")
                    }
                )
            }

            item {
                HelpSectionTitle(
                    title = "Get in touch",
                    detail = "Email",
                    contentColor = contentColor
                )
            }
            
            item {
                HelpActionCard(
                    title = "Email Support",
                    subtitle = "support@vormex.in",
                    icon = Icons.Outlined.Email,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = {
                        launchEmail(
                            context = context,
                            to = "support@vormex.in",
                            subject = "Support Request - Vormex Android",
                            body = "Hi Vormex Support,\n\nI need help with:\n\n${buildDiagnosticsText()}"
                        )
                    }
                )
                HelpRowDivider(contentColor)
            }
            
            item {
                HelpActionCard(
                    title = "Report a Bug",
                    subtitle = "Help us improve",
                    icon = Icons.Outlined.BugReport,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = {
                        launchEmail(
                            context = context,
                            to = "bugs@vormex.in",
                            subject = "Bug Report - Vormex App v${getAppVersion()}",
                            body = "Describe the issue:\n\nExpected:\n\nActual:\n\nSteps to reproduce:\n\n${buildDiagnosticsText()}"
                        )
                    }
                )
                HelpRowDivider(contentColor)
            }
            
            item {
                HelpActionCard(
                    title = "Feature Request",
                    subtitle = "Suggest new features",
                    icon = Icons.Outlined.TipsAndUpdates,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = {
                        launchEmail(
                            context = context,
                            to = "feedback@vormex.in",
                            subject = "Feature Request - Vormex App",
                            body = "I would love to see:\n\nWhy this helps:\n\n${buildDiagnosticsText()}"
                        )
                    }
                )
            }

            item {
                HelpSectionTitle(
                    title = "Social media",
                    detail = "Follow",
                    contentColor = contentColor
                )
            }

            item {
                HelpActionCard(
                    title = "Twitter / X",
                    subtitle = "@VormexApp",
                    icon = Icons.Outlined.AlternateEmail,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = {
                        openUrl(context, "https://twitter.com/VormexApp")
                    }
                )
                HelpRowDivider(contentColor)
            }

            item {
                HelpActionCard(
                    title = "Instagram",
                    subtitle = "@vormex.app",
                    icon = Icons.Outlined.CameraAlt,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = {
                        openUrl(context, "https://instagram.com/vormex.app")
                    }
                )
                HelpRowDivider(contentColor)
            }

            item {
                HelpActionCard(
                    title = "LinkedIn",
                    subtitle = "Vormex",
                    icon = Icons.Outlined.BusinessCenter,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = {
                        openUrl(context, "https://linkedin.com/company/vormex")
                    }
                )
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun HelpIntroCard(
    contentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 2.dp, top = 8.dp, end = 2.dp, bottom = 14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        HelpPlainIcon(
            icon = Icons.AutoMirrored.Outlined.HelpOutline,
            contentColor = contentColor,
            modifier = Modifier.size(26.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            BasicText(
                "Find answers faster",
                style = TextStyle(
                    color = contentColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
            BasicText(
                "Search common questions, run quick fixes, or send diagnostics when support needs context.",
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.62f),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            )
        }
    }
}

@Composable
private fun HelpSectionTitle(
    title: String,
    detail: String,
    contentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 2.dp, top = 14.dp, end = 2.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        BasicText(
            title,
            style = TextStyle(
                color = contentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
        BasicText(
            detail,
            style = TextStyle(
                color = contentColor.copy(alpha = 0.52f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HelpActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    contentColor: Color,
    accentColor: Color,
    trailingIcon: ImageVector = Icons.AutoMirrored.Outlined.ArrowForward,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 13.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HelpPlainIcon(
                icon = icon,
                contentColor = contentColor,
                modifier = Modifier.size(26.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                BasicText(
                    title,
                    style = TextStyle(contentColor, 15.sp, FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                BasicText(
                    subtitle,
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.58f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.38f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun HelpPlainIcon(
    icon: ImageVector,
    contentColor: Color,
    modifier: Modifier = Modifier,
    accentColor: Color = contentColor
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor.copy(alpha = 0.82f),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun HelpRowDivider(
    contentColor: Color,
    start: Dp = 58.dp,
    end: Dp = 18.dp
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = start, end = end)
            .height(1.dp)
            .background(contentColor.copy(alpha = 0.10f))
    )
}

@Composable
private fun HelpFaqCard(
    faq: HelpFaqItemData,
    expanded: Boolean,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 13.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HelpPlainIcon(
                        icon = faq.icon,
                        contentColor = contentColor,
                        modifier = Modifier.size(26.dp)
                    )
                    BasicText(
                        faq.title,
                        style = TextStyle(contentColor, 15.sp, FontWeight.SemiBold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Outlined.Remove else Icons.Outlined.Add,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.48f),
                    modifier = Modifier.size(18.dp)
                )
            }

            if (expanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 40.dp)
                        .height(1.dp)
                        .background(contentColor.copy(alpha = 0.08f))
                )
                BasicText(
                    faq.answer,
                    modifier = Modifier.padding(start = 40.dp),
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.70f),
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun SavedMetric(
    icon: ImageVector,
    value: Int,
    contentColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor.copy(alpha = 0.52f),
            modifier = Modifier.size(14.dp)
        )
        BasicText(
            value.toString(),
            style = TextStyle(contentColor.copy(alpha = 0.52f), 12.sp)
        )
    }
}

// ==================== SAVED POSTS SCREEN ====================

private enum class SavedContentTab(
    val title: String,
    val emptyTitle: String,
    val emptyBody: String
) {
    Profiles(
        title = "Profile",
        emptyTitle = "No saved profiles",
        emptyBody = "Profiles you bookmark will show up here."
    ),
    Reels(
        title = "Reels",
        emptyTitle = "No saved reels",
        emptyBody = "Reels you bookmark will show up here."
    ),
    Posts(
        title = "Posts",
        emptyTitle = "No saved posts",
        emptyBody = "Posts you bookmark will show up here."
    )
}

private fun SavedContentTab.savedIndex(): Int =
    when (this) {
        SavedContentTab.Profiles -> 0
        SavedContentTab.Reels -> 1
        SavedContentTab.Posts -> 2
    }

private fun savedContentTabAt(index: Int): SavedContentTab =
    when (((index % 3) + 3) % 3) {
        1 -> SavedContentTab.Reels
        2 -> SavedContentTab.Posts
        else -> SavedContentTab.Profiles
    }

private fun SavedContentTab.circularOffsetFrom(selectedTab: SavedContentTab): Int {
    val rawOffset = savedIndex() - selectedTab.savedIndex()
    return when {
        rawOffset > 1 -> rawOffset - 3
        rawOffset < -1 -> rawOffset + 3
        else -> rawOffset
    }
}

@Composable
fun SavedPostsScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onNavigateBack: () -> Unit,
    onNavigateToPost: (String) -> Unit = {},
    onNavigateToReel: (String) -> Unit = {},
    onNavigateToProfile: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: com.kyant.backdrop.catalog.linkedin.posts.PostsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = com.kyant.backdrop.catalog.linkedin.posts.PostsViewModel.Factory(context)
    )
    val uiState by viewModel.uiState.collectAsState()
    var savedReels by remember { mutableStateOf<List<com.kyant.backdrop.catalog.network.models.Reel>>(emptyList()) }
    var isLoadingSavedReels by remember { mutableStateOf(false) }
    var isLoadingMoreSavedReels by remember { mutableStateOf(false) }
    var savedReelsNextCursor by remember { mutableStateOf<String?>(null) }
    var hasMoreSavedReels by remember { mutableStateOf(false) }
    var savedProfiles by remember { mutableStateOf<List<com.kyant.backdrop.catalog.network.models.SavedProfileItem>>(emptyList()) }
    var isLoadingSavedProfiles by remember { mutableStateOf(false) }
    var isLoadingMoreSavedProfiles by remember { mutableStateOf(false) }
    var savedProfilesNextCursor by remember { mutableStateOf<String?>(null) }
    var hasMoreSavedProfiles by remember { mutableStateOf(false) }
    val savedListState = rememberLazyListState()
    var selectedTabName by rememberSaveable { mutableStateOf(SavedContentTab.Profiles.name) }
    var hasAutoSelectedSavedTab by rememberSaveable { mutableStateOf(false) }
    val selectedTab = when (selectedTabName) {
        SavedContentTab.Reels.name -> SavedContentTab.Reels
        SavedContentTab.Posts.name -> SavedContentTab.Posts
        else -> SavedContentTab.Profiles
    }

    fun loadSavedReels(refresh: Boolean = false) {
        if (refresh) {
            if (isLoadingSavedReels) return
        } else {
            if (
                isLoadingSavedReels ||
                isLoadingMoreSavedReels ||
                !hasMoreSavedReels ||
                savedReelsNextCursor == null
            ) return
        }

        scope.launch {
            if (refresh) {
                isLoadingSavedReels = true
                savedReels = emptyList()
                savedReelsNextCursor = null
                hasMoreSavedReels = false
            } else {
                isLoadingMoreSavedReels = true
            }

            ApiClient.getMySavedReels(
                context = context,
                cursor = if (refresh) null else savedReelsNextCursor,
                limit = 20
            )
                .onSuccess { response ->
                    val existing = if (refresh) emptyList() else savedReels
                    savedReels = (existing + response.reels).distinctBy { it.id }
                    savedReelsNextCursor = response.nextCursor
                    hasMoreSavedReels = response.hasMore
                    isLoadingSavedReels = false
                    isLoadingMoreSavedReels = false
                }
                .onFailure {
                    isLoadingSavedReels = false
                    isLoadingMoreSavedReels = false
                }
        }
    }

    fun loadSavedProfiles(refresh: Boolean = false) {
        if (refresh) {
            if (isLoadingSavedProfiles) return
        } else {
            if (
                isLoadingSavedProfiles ||
                isLoadingMoreSavedProfiles ||
                !hasMoreSavedProfiles ||
                savedProfilesNextCursor == null
            ) return
        }

        scope.launch {
            if (refresh) {
                isLoadingSavedProfiles = true
                savedProfiles = emptyList()
                savedProfilesNextCursor = null
                hasMoreSavedProfiles = false
            } else {
                isLoadingMoreSavedProfiles = true
            }

            ApiClient.getSavedProfiles(
                context = context,
                cursor = if (refresh) null else savedProfilesNextCursor,
                limit = 20
            )
                .onSuccess { response ->
                    val existing = if (refresh) emptyList() else savedProfiles
                    savedProfiles = (existing + response.profiles).distinctBy { it.id }
                    savedProfilesNextCursor = response.nextCursor
                    hasMoreSavedProfiles = response.hasMore
                    isLoadingSavedProfiles = false
                    isLoadingMoreSavedProfiles = false
                }
                .onFailure {
                    isLoadingSavedProfiles = false
                    isLoadingMoreSavedProfiles = false
                }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadSavedPosts(refresh = true)
        loadSavedReels(refresh = true)
        loadSavedProfiles(refresh = true)
    }

    val isInitialSavedLoading =
        (uiState.isLoadingSaved || isLoadingSavedReels || isLoadingSavedProfiles) &&
            uiState.savedPosts.isEmpty() &&
            savedReels.isEmpty() &&
            savedProfiles.isEmpty()

    LaunchedEffect(
        isInitialSavedLoading,
        savedProfiles.size,
        savedReels.size,
        uiState.savedPosts.size
    ) {
        if (!hasAutoSelectedSavedTab && !isInitialSavedLoading) {
            selectedTabName = when {
                savedProfiles.isNotEmpty() -> SavedContentTab.Profiles.name
                savedReels.isNotEmpty() -> SavedContentTab.Reels.name
                uiState.savedPosts.isNotEmpty() -> SavedContentTab.Posts.name
                else -> SavedContentTab.Profiles.name
            }
            hasAutoSelectedSavedTab = true
        }
    }

    LaunchedEffect(
        savedListState,
        selectedTab,
        savedProfiles.size,
        savedReels.size,
        uiState.savedPosts.size,
        hasMoreSavedProfiles,
        hasMoreSavedReels,
        uiState.hasSavedMore,
        isLoadingMoreSavedProfiles,
        isLoadingMoreSavedReels,
        uiState.isLoadingMoreSaved
    ) {
        snapshotFlow {
            val layout = savedListState.layoutInfo
            val lastVisibleIndex = layout.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisibleIndex to layout.totalItemsCount
        }.collect { (lastVisibleIndex, totalItems) ->
            if (totalItems > 0 && lastVisibleIndex >= totalItems - 5) {
                when (selectedTab) {
                    SavedContentTab.Profiles -> {
                        if (hasMoreSavedProfiles && !isLoadingMoreSavedProfiles) {
                            loadSavedProfiles(refresh = false)
                        }
                    }
                    SavedContentTab.Reels -> {
                        if (hasMoreSavedReels && !isLoadingMoreSavedReels) {
                            loadSavedReels(refresh = false)
                        }
                    }
                    SavedContentTab.Posts -> {
                        if (uiState.hasSavedMore && !uiState.isLoadingMoreSaved) {
                            viewModel.loadSavedPosts(refresh = false)
                        }
                    }
                }
            }
        }
    }

    SettingsScreenContainer(backdrop = backdrop, contentColor = contentColor, accentColor = accentColor) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                SettingsHeader(
                    title = "Saved",
                    contentColor = contentColor,
                    onBack = onNavigateBack
                )

                Box(modifier = Modifier.fillMaxSize()) {
                    if (isInitialSavedLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = accentColor)
                        }
                    } else {
                        SavedContentStage(
                            selectedTab = selectedTab,
                            listState = savedListState,
                            savedProfiles = savedProfiles,
                            savedReels = savedReels,
                            savedPosts = uiState.savedPosts,
                            hasMoreSavedProfiles = hasMoreSavedProfiles,
                            hasMoreSavedReels = hasMoreSavedReels,
                            hasMoreSavedPosts = uiState.hasSavedMore,
                            isLoadingMoreSavedProfiles = isLoadingMoreSavedProfiles,
                            isLoadingMoreSavedReels = isLoadingMoreSavedReels,
                            isLoadingMoreSavedPosts = uiState.isLoadingMoreSaved,
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            onProfileClick = { savedProfile -> onNavigateToProfile(savedProfile.user.id) },
                            onProfileUnsave = { savedProfile ->
                                scope.launch {
                                    ApiClient.toggleProfileSave(context, savedProfile.user.id)
                                        .onSuccess {
                                            savedProfiles = savedProfiles.filterNot { it.id == savedProfile.id }
                                        }
                                }
                            },
                            onLoadMoreProfiles = { loadSavedProfiles(refresh = false) },
                            onReelClick = { reel -> onNavigateToReel(reel.id) },
                            onReelUnsave = { reel ->
                                scope.launch {
                                    ApiClient.toggleReelSave(context, reel.id)
                                        .onSuccess {
                                            savedReels = savedReels.filterNot { it.id == reel.id }
                                        }
                                }
                            },
                            onLoadMoreReels = { loadSavedReels(refresh = false) },
                            onPostClick = { post -> onNavigateToPost(post.id) },
                            onPostUnsave = { post -> viewModel.toggleSave(post.id) },
                            onLoadMorePosts = { viewModel.loadSavedPosts(refresh = false) }
                        )
                    }
                }
            }

            SavedContentWheelSelector(
                selectedTab = selectedTab,
                contentColor = contentColor,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 0.dp),
                onTabSelected = { tab -> selectedTabName = tab.name }
            )
        }
    }
}

private fun String?.savedPreviewSnippet(fallback: String): String =
    this?.trim()?.takeIf { it.isNotBlank() } ?: fallback

@Composable
private fun SavedContentStage(
    selectedTab: SavedContentTab,
    listState: LazyListState,
    savedProfiles: List<com.kyant.backdrop.catalog.network.models.SavedProfileItem>,
    savedReels: List<com.kyant.backdrop.catalog.network.models.Reel>,
    savedPosts: List<com.kyant.backdrop.catalog.network.models.FullPost>,
    hasMoreSavedProfiles: Boolean,
    hasMoreSavedReels: Boolean,
    hasMoreSavedPosts: Boolean,
    isLoadingMoreSavedProfiles: Boolean,
    isLoadingMoreSavedReels: Boolean,
    isLoadingMoreSavedPosts: Boolean,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onProfileClick: (com.kyant.backdrop.catalog.network.models.SavedProfileItem) -> Unit,
    onProfileUnsave: (com.kyant.backdrop.catalog.network.models.SavedProfileItem) -> Unit,
    onLoadMoreProfiles: () -> Unit,
    onReelClick: (com.kyant.backdrop.catalog.network.models.Reel) -> Unit,
    onReelUnsave: (com.kyant.backdrop.catalog.network.models.Reel) -> Unit,
    onLoadMoreReels: () -> Unit,
    onPostClick: (com.kyant.backdrop.catalog.network.models.FullPost) -> Unit,
    onPostUnsave: (com.kyant.backdrop.catalog.network.models.FullPost) -> Unit,
    onLoadMorePosts: () -> Unit
) {
    AnimatedContent(
        targetState = selectedTab,
        label = "savedContentStage",
        transitionSpec = {
            val forward = targetState.savedIndex() >= initialState.savedIndex()
            val enterOffset = if (forward) { width: Int -> width / 6 } else { width: Int -> -width / 6 }
            val exitOffset = if (forward) { width: Int -> -width / 8 } else { width: Int -> width / 8 }
            (slideInHorizontally(initialOffsetX = enterOffset) + fadeIn()) togetherWith
                (slideOutHorizontally(targetOffsetX = exitOffset) + fadeOut())
        },
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 72.dp)
    ) { tab ->
        when (tab) {
            SavedContentTab.Profiles -> SavedProfilesList(
                listState = listState,
                profiles = savedProfiles,
                hasMore = hasMoreSavedProfiles,
                isLoadingMore = isLoadingMoreSavedProfiles,
                contentColor = contentColor,
                accentColor = accentColor,
                onProfileClick = onProfileClick,
                onUnsave = onProfileUnsave,
                onLoadMore = onLoadMoreProfiles
            )
            SavedContentTab.Reels -> SavedReelsList(
                listState = listState,
                reels = savedReels,
                hasMore = hasMoreSavedReels,
                isLoadingMore = isLoadingMoreSavedReels,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onReelClick = onReelClick,
                onUnsave = onReelUnsave,
                onLoadMore = onLoadMoreReels
            )
            SavedContentTab.Posts -> SavedPostsList(
                listState = listState,
                posts = savedPosts,
                hasMore = hasMoreSavedPosts,
                isLoadingMore = isLoadingMoreSavedPosts,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onPostClick = onPostClick,
                onUnsave = onPostUnsave,
                onLoadMore = onLoadMorePosts
            )
        }
    }
}

@Composable
private fun SavedContentWheelSelector(
    selectedTab: SavedContentTab,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onTabSelected: (SavedContentTab) -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    val appearance = currentVormexAppearance()
    val selectorShape = RoundedCornerShape(18.dp)

    fun selectTab(tab: SavedContentTab, withHaptic: Boolean) {
        if (tab == selectedTab) return
        if (withHaptic) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        onTabSelected(tab)
    }

    fun selectRelative(step: Int) {
        selectTab(savedContentTabAt(selectedTab.savedIndex() + step), withHaptic = true)
    }

    Box(
        modifier = modifier
            .shadow(10.dp, selectorShape, clip = false)
            .width(62.dp)
            .height(126.dp)
            .clip(selectorShape)
            .background(appearance.cardColor.copy(alpha = if (appearance.isGlassTheme) 0.82f else 0.98f))
            .border(1.dp, appearance.cardBorderColor, selectorShape)
            .pointerInput(selectedTab) {
                var dragDistance = 0f
                var snappedDuringDrag = false
                detectVerticalDragGestures(
                    onDragStart = {
                        dragDistance = 0f
                        snappedDuringDrag = false
                    },
                    onVerticalDrag = { _, dragAmount ->
                        dragDistance += dragAmount
                        if (!snappedDuringDrag && abs(dragDistance) > 24f) {
                            snappedDuringDrag = true
                            selectRelative(if (dragDistance < 0f) 1 else -1)
                        }
                    },
                    onDragEnd = {
                        dragDistance = 0f
                        snappedDuringDrag = false
                    },
                    onDragCancel = {
                        dragDistance = 0f
                        snappedDuringDrag = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        SavedContentWheelCard(
            tab = SavedContentTab.Profiles,
            offsetFromSelected = SavedContentTab.Profiles.circularOffsetFrom(selectedTab),
            contentColor = contentColor,
            onClick = { selectTab(SavedContentTab.Profiles, withHaptic = true) },
            modifier = Modifier.align(Alignment.Center)
        )
        SavedContentWheelCard(
            tab = SavedContentTab.Reels,
            offsetFromSelected = SavedContentTab.Reels.circularOffsetFrom(selectedTab),
            contentColor = contentColor,
            onClick = { selectTab(SavedContentTab.Reels, withHaptic = true) },
            modifier = Modifier.align(Alignment.Center)
        )
        SavedContentWheelCard(
            tab = SavedContentTab.Posts,
            offsetFromSelected = SavedContentTab.Posts.circularOffsetFrom(selectedTab),
            contentColor = contentColor,
            onClick = { selectTab(SavedContentTab.Posts, withHaptic = true) },
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun SavedContentWheelCard(
    tab: SavedContentTab,
    offsetFromSelected: Int,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val distance = abs(offsetFromSelected)
    val isActive = offsetFromSelected == 0
    val targetScale = when (distance) {
        0 -> 1f
        1 -> 0.8f
        else -> 0.6f
    }
    val targetAlpha = when (distance) {
        0 -> 1f
        1 -> 0.58f
        else -> 0.3f
    }
    val targetOffsetY = when {
        offsetFromSelected < 0 -> (-39).dp
        offsetFromSelected > 0 -> 39.dp
        else -> 0.dp
    }
    val cardScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f),
        label = "savedWheelScale"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 360f),
        label = "savedWheelAlpha"
    )
    val offsetY by animateDpAsState(
        targetValue = targetOffsetY,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f),
        label = "savedWheelOffset"
    )
    val shape = RoundedCornerShape(14.dp)
    val surfaceColor = if (isActive) contentColor.copy(alpha = 0.09f) else Color.Transparent
    val borderColor = if (isActive) contentColor.copy(alpha = 0.24f) else Color.Transparent

    Box(
        modifier = modifier
            .offset(y = offsetY)
            .zIndex(if (isActive) 3f else 1f - distance.toFloat())
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
                alpha = cardAlpha
            }
            .width(54.dp)
            .height(32.dp)
            .clip(shape)
            .background(surfaceColor)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 5.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            tab.title,
            style = TextStyle(
                color = if (isActive) contentColor else contentColor.copy(alpha = 0.62f),
                fontSize = if (isActive) 10.sp else 9.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                textAlign = TextAlign.Center
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SavedContentEmptyState(
    tab: SavedContentTab,
    contentColor: Color,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .settingsSurface(contentColor = contentColor, cornerRadius = 18.dp)
            .padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = when (tab) {
                SavedContentTab.Profiles -> Icons.Outlined.PersonOutline
                SavedContentTab.Reels -> Icons.Outlined.PlayCircleOutline
                SavedContentTab.Posts -> Icons.AutoMirrored.Outlined.Article
            },
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(28.dp)
        )
        BasicText(
            tab.emptyTitle,
            style = TextStyle(
                color = contentColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        )
        BasicText(
            tab.emptyBody,
            style = TextStyle(
                color = contentColor.copy(alpha = 0.62f),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                textAlign = TextAlign.Center
            )
        )
    }
}

@Composable
private fun SavedProfilesList(
    listState: LazyListState,
    profiles: List<com.kyant.backdrop.catalog.network.models.SavedProfileItem>,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    contentColor: Color,
    accentColor: Color,
    onProfileClick: (com.kyant.backdrop.catalog.network.models.SavedProfileItem) -> Unit,
    onUnsave: (com.kyant.backdrop.catalog.network.models.SavedProfileItem) -> Unit,
    onLoadMore: () -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (profiles.isEmpty()) {
            item {
                SavedContentEmptyState(
                    tab = SavedContentTab.Profiles,
                    contentColor = contentColor,
                    accentColor = accentColor
                )
            }
        } else {
            items(
                items = profiles,
                key = { profile -> profile.id }
            ) { savedProfile ->
                SavedProfileRow(
                    item = savedProfile,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = { onProfileClick(savedProfile) },
                    onUnsave = { onUnsave(savedProfile) }
                )
            }
        }

        if (hasMore || isLoadingMore) {
            item {
                SavedLoadMoreRow(
                    tab = SavedContentTab.Profiles,
                    isLoadingMore = isLoadingMore,
                    accentColor = accentColor,
                    onLoadMore = onLoadMore
                )
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun SavedReelsList(
    listState: LazyListState,
    reels: List<com.kyant.backdrop.catalog.network.models.Reel>,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onReelClick: (com.kyant.backdrop.catalog.network.models.Reel) -> Unit,
    onUnsave: (com.kyant.backdrop.catalog.network.models.Reel) -> Unit,
    onLoadMore: () -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (reels.isEmpty()) {
            item {
                SavedContentEmptyState(
                    tab = SavedContentTab.Reels,
                    contentColor = contentColor,
                    accentColor = accentColor
                )
            }
        } else {
            items(
                items = reels,
                key = { reel -> reel.id }
            ) { reel ->
                SavedReelItem(
                    reel = reel,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = { onReelClick(reel) },
                    onUnsave = { onUnsave(reel) }
                )
            }
        }

        if (hasMore || isLoadingMore) {
            item {
                SavedLoadMoreRow(
                    tab = SavedContentTab.Reels,
                    isLoadingMore = isLoadingMore,
                    accentColor = accentColor,
                    onLoadMore = onLoadMore
                )
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun SavedPostsList(
    listState: LazyListState,
    posts: List<com.kyant.backdrop.catalog.network.models.FullPost>,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onPostClick: (com.kyant.backdrop.catalog.network.models.FullPost) -> Unit,
    onUnsave: (com.kyant.backdrop.catalog.network.models.FullPost) -> Unit,
    onLoadMore: () -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (posts.isEmpty()) {
            item {
                SavedContentEmptyState(
                    tab = SavedContentTab.Posts,
                    contentColor = contentColor,
                    accentColor = accentColor
                )
            }
        } else {
            items(
                items = posts,
                key = { post -> post.id }
            ) { post ->
                SavedPostItem(
                    post = post,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = { onPostClick(post) },
                    onUnsave = { onUnsave(post) }
                )
            }
        }

        if (hasMore || isLoadingMore) {
            item {
                SavedLoadMoreRow(
                    tab = SavedContentTab.Posts,
                    isLoadingMore = isLoadingMore,
                    accentColor = accentColor,
                    onLoadMore = onLoadMore
                )
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun SavedLoadMoreRow(
    tab: SavedContentTab,
    isLoadingMore: Boolean,
    accentColor: Color,
    onLoadMore: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = !isLoadingMore, onClick = onLoadMore)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoadingMore) {
            CircularProgressIndicator(
                color = accentColor,
                strokeWidth = 2.dp,
                modifier = Modifier.size(22.dp)
            )
        } else {
            BasicText(
                "Load more ${tab.title.lowercase()}",
                style = TextStyle(accentColor, 13.sp, FontWeight.SemiBold)
            )
        }
    }
}

private fun com.kyant.backdrop.catalog.network.models.FullPost.savedPreviewImageUrl(): String? =
    when {
        !videoThumbnail.isNullOrBlank() -> videoThumbnail
        mediaUrls.isNotEmpty() -> mediaUrls.firstOrNull()
        !articleCoverImage.isNullOrBlank() -> articleCoverImage
        !linkImage.isNullOrBlank() -> linkImage
        !documentThumbnail.isNullOrBlank() -> documentThumbnail
        !celebrationGifUrl.isNullOrBlank() -> celebrationGifUrl
        else -> null
    }

private fun com.kyant.backdrop.catalog.network.models.FullPost.savedPreviewText(): String =
    listOf(content, articleTitle, linkTitle, documentName)
        .firstOrNull { !it.isNullOrBlank() }
        .savedPreviewSnippet(
            when {
                type.equals("VIDEO", ignoreCase = true) || !videoUrl.isNullOrBlank() -> "Video post"
                type.equals("IMAGE", ignoreCase = true) || mediaUrls.isNotEmpty() -> "Photo post"
                type.equals("POLL", ignoreCase = true) -> "Poll post"
                type.equals("ARTICLE", ignoreCase = true) -> "Article post"
                else -> "Saved post"
            }
        )

@Composable
private fun SavedMediaThumbnail(
    imageUrl: String?,
    isVideo: Boolean,
    fallbackIcon: ImageVector,
    contentColor: Color,
    accentColor: Color,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(contentColor.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (isVideo) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.42f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayCircleOutline,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        } else {
            Icon(
                imageVector = fallbackIcon,
                contentDescription = null,
                tint = accentColor.copy(alpha = 0.78f),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun SavedProfileRow(
    item: com.kyant.backdrop.catalog.network.models.SavedProfileItem,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit,
    onUnsave: () -> Unit
) {
    val user = item.user
    val title = user.name.ifBlank { user.username }
    val subtitle = listOfNotNull(
        user.headline?.takeIf { it.isNotBlank() },
        user.college?.takeIf { it.isNotBlank() }
    ).joinToString(" • ")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .settingsSurface(contentColor = contentColor, cornerRadius = 16.dp)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                if (!user.profileImage.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(user.profileImage)
                            .crossfade(true)
                            .build(),
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.PersonOutline,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    BasicText(
                        title,
                        style = TextStyle(contentColor, 14.sp, FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    VerificationBadge(
                        verified = user.hasVerificationBadge(),
                        badgeStyle = user.verificationBadgeStyle(),
                        size = VerificationBadgeSize.Small
                    )
                }

                if (subtitle.isNotBlank()) {
                    BasicText(
                        subtitle,
                        style = TextStyle(contentColor.copy(alpha = 0.68f), 12.sp),
                        modifier = Modifier.padding(top = 4.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                BasicText(
                    "@${user.username}",
                    style = TextStyle(contentColor.copy(alpha = 0.48f), 11.sp),
                    modifier = Modifier.padding(top = 5.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onUnsave)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Bookmark,
                    contentDescription = "Unsave profile",
                    tint = contentColor.copy(alpha = 0.72f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SavedReelItem(
    reel: com.kyant.backdrop.catalog.network.models.Reel,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit,
    onUnsave: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .settingsSurface(contentColor = contentColor, cornerRadius = 16.dp)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            SavedMediaThumbnail(
                imageUrl = reel.thumbnailUrl ?: reel.previewGifUrl,
                isVideo = true,
                fallbackIcon = Icons.Outlined.PlayCircleOutline,
                contentColor = contentColor,
                accentColor = accentColor,
                contentDescription = "Reel thumbnail",
                modifier = Modifier.size(width = 72.dp, height = 96.dp)
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    reel.author.name ?: "Unknown",
                    style = TextStyle(contentColor, 14.sp, FontWeight.SemiBold)
                )

                BasicText(
                    (reel.caption ?: reel.title).savedPreviewSnippet("Saved reel"),
                    style = TextStyle(contentColor.copy(alpha = 0.7f), 13.sp),
                    modifier = Modifier.padding(top = 4.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SavedMetric(icon = Icons.Outlined.FavoriteBorder, value = reel.likesCount, contentColor = contentColor)
                    SavedMetric(icon = Icons.Outlined.ChatBubbleOutline, value = reel.commentsCount, contentColor = contentColor)
                    SavedMetric(icon = Icons.Outlined.PlayCircleOutline, value = reel.viewsCount, contentColor = contentColor)
                }
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onUnsave)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Bookmark,
                    contentDescription = "Unsave",
                    tint = contentColor.copy(alpha = 0.72f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SavedPostItem(
    post: com.kyant.backdrop.catalog.network.models.FullPost,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit,
    onUnsave: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .settingsSurface(contentColor = contentColor, cornerRadius = 16.dp)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            val isVideoPost = post.type.equals("VIDEO", ignoreCase = true) || !post.videoUrl.isNullOrBlank()
            SavedMediaThumbnail(
                imageUrl = post.savedPreviewImageUrl(),
                isVideo = isVideoPost,
                fallbackIcon = if (isVideoPost) Icons.Outlined.PlayCircleOutline else Icons.Outlined.Image,
                contentColor = contentColor,
                accentColor = accentColor,
                contentDescription = "Post thumbnail",
                modifier = Modifier.size(width = 88.dp, height = 74.dp)
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    post.author.name ?: "Unknown",
                    style = TextStyle(contentColor, 14.sp, FontWeight.SemiBold)
                )
                
                BasicText(
                    post.savedPreviewText(),
                    style = TextStyle(contentColor.copy(alpha = 0.7f), 13.sp),
                    modifier = Modifier.padding(top = 4.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SavedMetric(icon = Icons.Outlined.FavoriteBorder, value = post.likesCount, contentColor = contentColor)
                    SavedMetric(icon = Icons.Outlined.ChatBubbleOutline, value = post.commentsCount, contentColor = contentColor)
                }
            }
            
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onUnsave)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Bookmark,
                    contentDescription = "Unsave",
                    tint = contentColor.copy(alpha = 0.72f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ==================== LOGOUT DIALOG ====================

@Composable
fun LogoutConfirmationDialog(
    contentColor: Color,
    accentColor: Color,
    title: String = "Log Out",
    message: String = "Are you sure you want to log out? You'll need to sign in again to access your account.",
    confirmText: String = "Log Out",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val dialogSurfaceColor = MaterialTheme.colorScheme.surface
    val dialogTextColor = MaterialTheme.colorScheme.onSurface

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                title,
                color = dialogTextColor,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                message,
                color = dialogTextColor.copy(alpha = 0.74f)
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = accentColor)
            ) {
                Text("Cancel")
            }
        },
        containerColor = dialogSurfaceColor
    )
}

// ==================== REUSABLE SETTINGS COMPONENTS ====================

@Composable
fun SettingsHeader(
    title: String,
    contentColor: Color,
    onBack: () -> Unit
) {
    val isDarkSurface = contentColor == Color.White
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (isDarkSurface) Color.White.copy(alpha = 0.12f)
                    else Color.Black.copy(alpha = 0.05f)
                )
                .clickable(onClick = onBack)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(Modifier.width(8.dp))
        
        BasicText(
            title,
            style = TextStyle(contentColor, 20.sp, FontWeight.Bold)
        )
    }
}

@Composable
fun SettingsSectionHeader(
    title: String,
    contentColor: Color
) {
    BasicText(
        title,
        style = TextStyle(
            color = contentColor.copy(alpha = 0.5f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        ),
        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val alpha = if (enabled) 1f else 0.5f
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .settingsSurface(contentColor = contentColor, cornerRadius = 18.dp)
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                SettingsIconBadge(icon = icon, accentColor = accentColor)
                Spacer(Modifier.width(16.dp))
                Column {
                    BasicText(
                        title,
                        style = TextStyle(contentColor.copy(alpha = alpha), 16.sp, FontWeight.Medium)
                    )
                    if (subtitle.isNotEmpty()) {
                        BasicText(
                            subtitle,
                            style = TextStyle(contentColor.copy(alpha = 0.5f * alpha), 12.sp)
                        )
                    }
                }
            }

            Box(modifier = Modifier.alpha(alpha)) {
                LiquidToggle(
                    selected = { checked },
                    onSelect = { if (enabled) onCheckedChange(it) },
                    backdrop = backdrop
                )
            }
        }
    }
}

@Composable
fun SettingsActionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    trailingText: String = "",
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .settingsSurface(contentColor = contentColor, cornerRadius = 18.dp)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                SettingsIconBadge(icon = icon, accentColor = accentColor)
                Spacer(Modifier.width(16.dp))
                Column {
                    BasicText(
                        title,
                        style = TextStyle(contentColor, 16.sp, FontWeight.Medium)
                    )
                    if (subtitle.isNotEmpty()) {
                        BasicText(
                            subtitle,
                            style = TextStyle(contentColor.copy(alpha = 0.5f), 12.sp)
                        )
                    }
                }
            }
            
            if (trailingText.isNotEmpty()) {
                BasicText(
                    trailingText,
                    style = TextStyle(
                        color = accentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

@Composable
fun SettingsNavigationItem(
    title: String,
    icon: ImageVector,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    subtitle: String = "",
    trailing: String = "",
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .settingsSurface(contentColor = contentColor, cornerRadius = 18.dp)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                SettingsIconBadge(icon = icon, accentColor = accentColor)
                Spacer(Modifier.width(16.dp))
                Column {
                    BasicText(
                        title,
                        style = TextStyle(contentColor, 16.sp, FontWeight.Medium)
                    )
                    if (subtitle.isNotEmpty()) {
                        BasicText(
                            subtitle,
                            style = TextStyle(contentColor.copy(alpha = 0.5f), 12.sp)
                        )
                    }
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (trailing.isNotEmpty()) {
                    BasicText(
                        trailing,
                        style = TextStyle(contentColor.copy(alpha = 0.5f), 14.sp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.34f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun SettingsOptionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    options: List<Pair<String, String>>,
    selectedOption: String,
    flat: Boolean = false,
    onOptionSelected: (String) -> Unit
) {
    val isDarkSurface = contentColor == Color.White
    val containerModifier = if (flat) {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 2.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .settingsSurface(contentColor = contentColor, cornerRadius = 18.dp)
            .padding(16.dp)
    }
    Box(
        modifier = containerModifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (flat) {
                    NotificationPlainIcon(icon = icon, accentColor = accentColor)
                    Spacer(Modifier.width(12.dp))
                } else {
                    SettingsIconBadge(icon = icon, accentColor = accentColor)
                    Spacer(Modifier.width(16.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    BasicText(
                        title,
                        style = TextStyle(
                            color = contentColor,
                            fontSize = if (flat) 14.sp else 16.sp,
                            fontWeight = if (flat) FontWeight.SemiBold else FontWeight.Medium
                        )
                    )
                    BasicText(
                        subtitle,
                        style = TextStyle(
                            color = contentColor.copy(alpha = if (flat) 0.58f else 0.5f),
                            fontSize = if (flat) 11.sp else 12.sp
                        )
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { (value, label) ->
                    val isSelected = value == selectedOption
                    val pillBackground = if (flat) {
                        if (isSelected) accentColor.copy(alpha = 0.16f)
                        else contentColor.copy(alpha = 0.05f)
                    } else {
                        if (isSelected) accentColor.copy(alpha = 0.18f)
                        else if (isDarkSurface) Color.White.copy(alpha = 0.10f)
                        else Color.White.copy(alpha = 0.20f)
                    }
                    val pillBorder = if (isSelected) {
                        accentColor.copy(alpha = 0.36f)
                    } else if (isDarkSurface) {
                        Color.White.copy(alpha = 0.14f)
                    } else {
                        Color.White.copy(alpha = 0.34f)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(pillBackground)
                            .then(
                                if (flat) Modifier
                                else Modifier.border(1.dp, pillBorder, RoundedCornerShape(999.dp))
                            )
                            .clickable { onOptionSelected(value) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            BasicText(
                                label,
                                style = TextStyle(
                                    color = if (isSelected) accentColor else contentColor.copy(alpha = 0.78f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== HELPER FUNCTIONS ====================

private fun isRuntimePermissionGranted(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

private fun openAppNotificationSettings(context: Context) {
    val intent = Intent().apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        } else {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.parse("package:${context.packageName}")
        }
    }
    launchIntentSafely(context, intent)
}

private fun openAppPermissionSettings(context: Context) {
    launchIntentSafely(
        context,
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    )
}

private fun openUrl(context: Context, url: String) {
    launchIntentSafely(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

private fun launchEmail(
    context: Context,
    to: String,
    subject: String,
    body: String
) {
    val mailTarget = if (to.isBlank()) "mailto:" else "mailto:$to"
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse(mailTarget)
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    launchIntentSafely(context, intent, failureMessage = "No email app available on this device.")
}

private fun launchIntentSafely(
    context: Context,
    intent: Intent,
    failureMessage: String = "No app available to handle this action."
) {
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, failureMessage, Toast.LENGTH_SHORT).show()
    }
}

private fun copyText(
    context: Context,
    label: String,
    text: String
) {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
}

private fun buildInviteMessage(
    referralCode: String,
    inviteLink: String = "https://vormex.in/login?mode=signup&ref=$referralCode"
): String {
    return "Join me on Vormex! Connect with professionals, share your journey, and discover opportunities. Use my code $referralCode and join here: $inviteLink"
}

private fun buildDiagnosticsText(): String {
    return buildString {
        appendLine("App Version: ${getAppVersion()}")
        appendLine("Package: ${BuildConfig.APPLICATION_ID}")
        appendLine("Backend: ${BuildConfig.API_BASE_URL}")
        appendLine("Device: ${Build.MODEL}")
        appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
    }.trim()
}

private fun getAppVersion(): String {
    return try {
        BuildConfig.VERSION_NAME
    } catch (e: Exception) {
        "1.0.0"
    }
}
