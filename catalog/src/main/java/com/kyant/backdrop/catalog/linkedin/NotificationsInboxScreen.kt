package com.kyant.backdrop.catalog.linkedin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.network.models.Notification
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val DAY_IN_MILLIS = 24L * 60L * 60L * 1000L

private enum class NotificationBucket {
    TODAY,
    YESTERDAY,
    THIS_WEEK,
    EARLIER
}

private enum class NotificationDestinationKind {
    Conversation,
    Post,
    Reel,
    Profile,
    Network,
    GrowthHub
}

private data class NotificationSection(
    val id: String,
    val title: String,
    val subtitle: String,
    val notifications: List<NotificationFeedItem>
)

private data class NotificationFeedItem(
    val id: String,
    val notification: Notification,
    val notificationIds: List<String>,
    val displayName: String,
    val actionText: String
)

private data class NotificationDestination(
    val kind: NotificationDestinationKind,
    val targetId: String? = null,
    val label: String,
    val hint: String,
    val badge: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsInboxScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onNavigateBack: () -> Unit,
    onUnreadCountChanged: (Int) -> Unit = {},
    onOpenProfile: (String) -> Unit = {},
    onOpenPost: (String) -> Unit = {},
    onOpenReel: (String) -> Unit = {},
    onOpenConversation: (String) -> Unit = {},
    onOpenNetwork: () -> Unit = {},
    onOpenGrowthHub: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: NotificationsInboxViewModel =
        viewModel(factory = NotificationsInboxViewModel.Factory(context))
    val uiState by viewModel.uiState.collectAsState()
    val refreshState = rememberPullToRefreshState()

    val sections = buildNotificationSections(uiState.notifications)

    LaunchedEffect(uiState.unreadCount) {
        onUnreadCountChanged(uiState.unreadCount)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            SettingsHeader(
                title = "Notifications",
                contentColor = contentColor,
                onBack = onNavigateBack
            )

            Spacer(Modifier.height(6.dp))

            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refresh() },
                state = refreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    uiState.isLoading && uiState.notifications.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = accentColor)
                        }
                    }

                    uiState.notifications.isEmpty() -> {
                        EmptyNotificationsState(
                            backdrop = backdrop,
                            contentColor = contentColor,
                            error = uiState.error,
                            onRetry = { viewModel.refresh(showLoader = true) }
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            item {
                                if (uiState.error != null) {
                                    NotificationsInlineError(
                                        message = uiState.error ?: "Something went wrong",
                                        backdrop = backdrop,
                                        contentColor = contentColor,
                                        onRetry = {
                                            viewModel.clearError()
                                            viewModel.refresh(showLoader = true)
                                        }
                                    )
                                }
                            }

                            sections.forEach { section ->
                                item(key = "section_${section.id}") {
                                    NotificationSectionHeader(
                                        section = section,
                                        contentColor = contentColor,
                                        accentColor = accentColor
                                    )
                                }

                                items(
                                    items = section.notifications,
                                    key = { item -> "${section.id}_${item.id}" }
                                ) { item ->
                                    NotificationInboxCard(
                                        feedItem = item,
                                        contentColor = contentColor,
                                        accentColor = accentColor,
                                        onClick = {
                                            viewModel.markAsRead(item.notificationIds)
                                            routeNotification(
                                                notification = item.notification,
                                                onOpenProfile = onOpenProfile,
                                                onOpenPost = onOpenPost,
                                                onOpenReel = onOpenReel,
                                                onOpenConversation = onOpenConversation,
                                                onOpenNetwork = onOpenNetwork,
                                                onOpenGrowthHub = onOpenGrowthHub
                                            )
                                        }
                                    )
                                }
                            }

                            item {
                                Spacer(Modifier.height(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InboxActionPill(
    label: String,
    active: Boolean,
    accentColor: Color,
    contentColor: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val background = if (active) accentColor.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.1f)
    val border = if (active) accentColor.copy(alpha = 0.45f) else contentColor.copy(alpha = 0.12f)
    val textColor = if (enabled) {
        if (active) accentColor else contentColor
    } else {
        contentColor.copy(alpha = 0.35f)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(999.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        BasicText(
            text = label,
            style = TextStyle(
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
private fun NotificationSectionHeader(
    section: NotificationSection,
    contentColor: Color,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BasicText(
                text = section.title,
                style = TextStyle(
                    color = contentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
            if (section.subtitle.isNotBlank()) {
                BasicText(
                    text = section.subtitle,
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.58f),
                        fontSize = 10.sp
                    )
                )
            }
        }

        NotificationMetaPill(
            text = section.notifications.size.toString(),
            background = accentColor.copy(alpha = 0.14f),
            borderColor = accentColor.copy(alpha = 0.24f),
            textColor = accentColor
        )
    }
}

@Composable
private fun NotificationInboxCard(
    feedItem: NotificationFeedItem,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    val notification = feedItem.notification
    val tone = notificationTone(notification.type, accentColor)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            NotificationAvatar(
                notification = notification,
                displayName = feedItem.displayName,
                tone = tone
            )

            Spacer(Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        text = feedItem.displayName,
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 13.sp,
                            fontWeight = if (notification.isRead) FontWeight.Medium else FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                BasicText(
                    text = feedItem.actionText,
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(12.dp))

            BasicText(
                text = relativeTimestamp(notification.createdAt),
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.44f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 50.dp)
                .height(1.dp)
                .background(contentColor.copy(alpha = 0.08f))
        )
    }
}

@Composable
private fun NotificationAvatar(
    notification: Notification,
    displayName: String,
    tone: Color
) {
    val profileImage = notification.actor?.profileImage

    if (!profileImage.isNullOrBlank()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(profileImage)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
        )
    } else {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(tone.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = avatarInitials(displayName),
                style = TextStyle(
                    color = tone,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@Composable
private fun NotificationMetaPill(
    text: String,
    background: Color,
    borderColor: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .border(1.dp, borderColor, RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                color = textColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
private fun NotificationsInlineError(
    message: String,
    backdrop: LayerBackdrop,
    contentColor: Color,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(18.dp) },
                effects = {
                    vibrancy()
                    blur(10f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color(0xFFFF8A80).copy(alpha = 0.12f))
                }
            )
            .border(
                width = 1.dp,
                color = Color(0xFFFF8A80).copy(alpha = 0.22f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                BasicText(
                    text = "Inbox needs another try",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                BasicText(
                    text = message,
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                )
            }

            Spacer(Modifier.width(12.dp))

            InboxActionPill(
                label = "Retry",
                active = true,
                accentColor = Color(0xFFFF8A80),
                contentColor = contentColor,
                onClick = onRetry
            )
        }
    }
}

@Composable
private fun EmptyNotificationsState(
    backdrop: LayerBackdrop,
    contentColor: Color,
    error: String?,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(24.dp) },
                    effects = {
                        vibrancy()
                        blur(16f.dp.toPx())
                        lens(6f.dp.toPx(), 12f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(Color.White.copy(alpha = 0.12f))
                    }
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BasicText(
                    text = if (error != null) "Notification inbox hit a snag" else "Your inbox is quiet",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                BasicText(
                    text = error ?: "Comments and other activity will appear here as they come in.",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.66f),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (error != null) {
                        InboxActionPill(
                            label = "Retry",
                            active = true,
                            accentColor = Color(0xFFFF8A80),
                            contentColor = contentColor,
                            onClick = onRetry
                        )
                    }
                    NotificationMetaPill(
                        text = "Pull to refresh",
                        background = Color.White.copy(alpha = 0.08f),
                        borderColor = Color.White.copy(alpha = 0.1f),
                        textColor = contentColor.copy(alpha = 0.62f)
                    )
                }
            }
        }
    }
}

private fun buildNotificationSections(
    notifications: List<Notification>
): List<NotificationSection> {
    if (notifications.isEmpty()) return emptyList()

    val sections = mutableListOf<NotificationSection>()

    val today = buildNotificationFeedItems(
        notifications.filter { notificationBucket(it.createdAt) == NotificationBucket.TODAY }
    )
    val yesterday = buildNotificationFeedItems(
        notifications.filter { notificationBucket(it.createdAt) == NotificationBucket.YESTERDAY }
    )
    val thisWeek = buildNotificationFeedItems(
        notifications.filter { notificationBucket(it.createdAt) == NotificationBucket.THIS_WEEK }
    )
    val earlier = buildNotificationFeedItems(
        notifications.filter { notificationBucket(it.createdAt) == NotificationBucket.EARLIER }
    )

    if (today.isNotEmpty()) {
        sections += NotificationSection(
            id = "today",
            title = "Today",
            subtitle = "",
            notifications = today
        )
    }
    if (yesterday.isNotEmpty()) {
        sections += NotificationSection(
            id = "yesterday",
            title = "Yesterday",
            subtitle = "",
            notifications = yesterday
        )
    }
    if (thisWeek.isNotEmpty()) {
        sections += NotificationSection(
            id = "week",
            title = "This week",
            subtitle = "",
            notifications = thisWeek
        )
    }
    if (earlier.isNotEmpty()) {
        sections += NotificationSection(
            id = "earlier",
            title = "Earlier",
            subtitle = "",
            notifications = earlier
        )
    }

    return if (sections.isEmpty()) {
        listOf(
            NotificationSection(
                id = "all",
                title = "All activity",
                subtitle = "",
                notifications = buildNotificationFeedItems(notifications)
            )
        )
    } else {
        sections
    }
}

private fun buildNotificationFeedItems(
    notifications: List<Notification>
): List<NotificationFeedItem> {
    if (notifications.isEmpty()) return emptyList()

    val groupedNotifications = linkedMapOf<String, MutableList<Notification>>()

    notifications.forEach { notification ->
        val key = notificationGroupKey(notification)
        groupedNotifications.getOrPut(key) { mutableListOf() }.add(notification)
    }

    return groupedNotifications.map { (key, group) ->
        val representative = group.first()
        NotificationFeedItem(
            id = key,
            notification = representative,
            notificationIds = group.map { it.id },
            displayName = notificationDisplayName(representative),
            actionText = notificationActionSummary(group)
        )
    }
}

private fun notificationGroupKey(notification: Notification): String {
    val actorKey = notification.actor?.id
        ?: notification.dataValue("viewerId")
        ?: notification.dataValue("userId")
        ?: notification.dataValue("actorId")

    if (actorKey.isNullOrBlank()) {
        return "single:${notification.id}"
    }

    val action = notificationActionKey(notification.type)
    val target = notificationTargetKey(notification)

    return if (action == "message") {
        "actor:$actorKey:$action:${notification.dataValue("conversationId") ?: notification.id}"
    } else {
        "actor:$actorKey:$action:$target"
    }
}

private fun notificationActionKey(type: String): String {
    val normalized = type.lowercase(Locale.getDefault())
    return when {
        normalized == "connection_request" -> "connect_request"
        normalized == "connection_accepted" -> "connect_accepted"
        normalized == "profile_view" -> "profile_view"
        normalized == "follow" -> "follow"
        normalized == "message" || "message" in normalized -> "message"
        "like" in normalized -> "like"
        "comment_reply" in normalized -> "reply"
        "comment" in normalized -> "comment"
        "mention" in normalized -> "mention"
        "share" in normalized -> "share"
        else -> normalized
    }
}

private fun notificationTargetKey(notification: Notification): String {
    val normalized = notification.type.lowercase(Locale.getDefault())

    return when {
        notification.reel?.id != null || "reel" in normalized -> "reel"
        notification.post?.id != null -> "post"
        "reply" in normalized -> "comment"
        "connection" in normalized -> "connection"
        "profile" in normalized -> "profile"
        else -> "generic"
    }
}

private fun notificationDisplayName(notification: Notification): String {
    val actorName = notification.actor?.name?.trim().orEmpty()
    if (actorName.isNotBlank()) return actorName

    val username = notification.actor?.username?.trim().orEmpty()
    if (username.isNotBlank()) return username

    return if (notificationUsesVormexBranding(notification)) "Vormex" else "Someone"
}

private fun notificationActionSummary(notifications: List<Notification>): String {
    val notification = notifications.first()
    val count = notifications.size

    return when (notificationActionKey(notification.type)) {
        "connect_request" -> "wants to connect"
        "connect_accepted" -> "accepted your connection"
        "follow" -> "followed you"
        "profile_view" -> if (count > 1) {
            "viewed your profile $count times"
        } else {
            "viewed your profile"
        }
        "message" -> quantityCopy(
            count = count,
            singular = "sent you a message",
            plural = "sent $count messages"
        )
        "like" -> when (notificationTargetKey(notification)) {
            "reel" -> quantityCopy(count, "liked your reel", "liked $count of your reels")
            "post" -> quantityCopy(count, "liked your post", "liked $count of your posts")
            else -> quantityCopy(count, "liked your update", "liked $count of your updates")
        }
        "comment" -> when (notificationTargetKey(notification)) {
            "reel" -> quantityCopy(count, "commented on your reel", "commented on $count of your reels")
            "post" -> quantityCopy(count, "commented on your post", "commented on $count of your posts")
            else -> quantityCopy(count, "commented on your update", "commented on $count of your updates")
        }
        "reply" -> quantityCopy(
            count = count,
            singular = "replied to your comment",
            plural = "replied to $count of your comments"
        )
        "mention" -> when (notificationTargetKey(notification)) {
            "reel" -> quantityCopy(count, "mentioned you in a reel", "mentioned you in $count reels")
            "post" -> quantityCopy(count, "mentioned you in a post", "mentioned you in $count posts")
            else -> quantityCopy(count, "mentioned you", "mentioned you $count times")
        }
        "share" -> when (notificationTargetKey(notification)) {
            "reel" -> quantityCopy(count, "shared your reel", "shared $count of your reels")
            "post" -> quantityCopy(count, "shared your post", "shared $count of your posts")
            else -> quantityCopy(count, "shared your update", "shared $count of your updates")
        }
        else -> fallbackNotificationActionSummary(notification)
    }
}

private fun quantityCopy(
    count: Int,
    singular: String,
    plural: String
): String {
    return if (count > 1) plural else singular
}

private fun fallbackNotificationActionSummary(notification: Notification): String {
    val displayName = notificationDisplayName(notification)
    val cleanedBody = trimActorPrefix(notificationDisplayBody(notification.body), displayName)
    if (cleanedBody.isNotBlank()) return cleanedBody

    val cleanedTitle = trimActorPrefix(notificationDisplayTitle(notification.title), displayName)
    if (cleanedTitle.isNotBlank()) return cleanedTitle

    return notificationTypeLabel(notification.type)
}

private fun trimActorPrefix(
    text: String,
    displayName: String
): String {
    if (text.isBlank() || displayName.isBlank()) return text

    val withoutActor = text.replaceFirst(
        Regex("^${Regex.escape(displayName)}\\s+", RegexOption.IGNORE_CASE),
        ""
    ).trim()

    return withoutActor.ifBlank { text }
}

private fun notificationBucket(createdAt: String): NotificationBucket {
    val parsedDate = parseNotificationDate(createdAt) ?: return NotificationBucket.EARLIER
    val now = Calendar.getInstance()
    val item = Calendar.getInstance().apply { time = parsedDate }

    if (sameDay(item, now)) return NotificationBucket.TODAY

    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    if (sameDay(item, yesterday)) return NotificationBucket.YESTERDAY

    val dayDiff = ((startOfDayMillis(now) - startOfDayMillis(item)) / DAY_IN_MILLIS).toInt()
    return if (dayDiff in 2..6) NotificationBucket.THIS_WEEK else NotificationBucket.EARLIER
}

private fun relativeTimestamp(createdAt: String): String {
    val parsedDate = parseNotificationDate(createdAt) ?: return compactTimestamp(createdAt)
    val diff = (System.currentTimeMillis() - parsedDate.time).coerceAtLeast(0L)

    return when {
        diff < 60_000L -> "Now"
        diff < 60L * 60L * 1000L -> "${diff / 60_000L}m"
        diff < DAY_IN_MILLIS -> "${diff / (60L * 60L * 1000L)}h"
        diff < 2L * DAY_IN_MILLIS -> "Yesterday"
        diff < 7L * DAY_IN_MILLIS -> "${diff / DAY_IN_MILLIS}d"
        else -> SimpleDateFormat("MMM d", Locale.US).format(parsedDate)
    }
}

private fun parseNotificationDate(createdAt: String): Date? {
    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSX",
        "yyyy-MM-dd'T'HH:mm:ssX"
    )

    patterns.forEach { pattern ->
        runCatching {
            SimpleDateFormat(pattern, Locale.US).parse(createdAt)
        }.getOrNull()?.let { return it }
    }

    return null
}

private fun sameDay(first: Calendar, second: Calendar): Boolean {
    return first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
        first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
}

private fun startOfDayMillis(calendar: Calendar): Long {
    return (calendar.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun notificationTypeLabel(type: String): String {
    val normalized = type.lowercase(Locale.getDefault())
    return when {
        normalized == "admin_announcement" -> "Announcement"
        normalized == "connection_request" -> "Connection request"
        normalized == "connection_accepted" -> "Connection accepted"
        normalized == "profile_view" -> "Profile views"
        normalized == "comment_reply" -> "Reply"
        normalized == "reel_comment_reply" -> "Reel reply"
        normalized == "people_you_know_joined" -> "People you know"
        normalized == "reel_view_milestone" -> "Reel milestone"
        normalized == "xp_earned" -> "XP earned"
        normalized == "streak_milestone" -> "Streak milestone"
        normalized == "streak_lost" -> "Streak update"
        else -> normalized
            .split("_")
            .joinToString(" ") { word ->
                word.replaceFirstChar { char -> char.titlecase(Locale.getDefault()) }
            }
    }
}

private fun notificationDisplayTitle(title: String): String {
    val trimmed = title.trim()
    val cleaned = cleanNotificationCopy(trimmed)
    return cleaned.ifBlank { trimmed.replace(Regex("^[^\\p{L}\\p{N}]+\\s*"), "") }
}

private fun notificationDisplayBody(body: String): String {
    return cleanNotificationCopy(body)
}

private fun cleanNotificationCopy(text: String): String {
    val original = text.trim()
    if (original.isBlank()) return ""

    var cleaned = original.replace(Regex("^[^\\p{L}\\p{N}]+\\s*"), "")
    listOf(
        Regex("^there is a new\\s+", RegexOption.IGNORE_CASE),
        Regex("^there's a new\\s+", RegexOption.IGNORE_CASE),
        Regex("^you have a new\\s+", RegexOption.IGNORE_CASE),
        Regex("^you got a new\\s+", RegexOption.IGNORE_CASE),
        Regex("^you have\\s+", RegexOption.IGNORE_CASE),
        Regex("^new\\s+(?=(message|comment|reply|request|connection|follower|mention|like|reel|notification)s?\\b)", RegexOption.IGNORE_CASE)
    ).forEach { pattern ->
        cleaned = cleaned.replace(pattern, "")
    }

    listOf(
        Regex("\\bon Vormex\\b", RegexOption.IGNORE_CASE),
        Regex("\\btap to (?:open|view|see|reply)\\b", RegexOption.IGNORE_CASE),
        Regex("\\bcheck it out\\b", RegexOption.IGNORE_CASE),
        Regex("\\bright now\\b", RegexOption.IGNORE_CASE)
    ).forEach { pattern ->
        cleaned = cleaned.replace(pattern, "")
    }

    cleaned = cleaned
        .replace(Regex("\\s+"), " ")
        .replace(Regex("\\s+([,.:!?])"), "$1")
        .replace(Regex("^[,.:;\\-\\s]+"), "")
        .replace(Regex("[,.:;\\-\\s]+$"), "")
        .trim()

    val normalized = cleaned.ifBlank { original }
    return normalized.replaceFirstChar { char ->
        if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
    }
}

private fun notificationTone(type: String, accentColor: Color): Color {
    val normalized = type.lowercase(Locale.getDefault())
    return when {
        "admin" in normalized -> Color(0xFF2563EB)
        "message" in normalized -> Color(0xFF4FC3F7)
        "mention" in normalized -> Color(0xFFFFB74D)
        "profile" in normalized -> Color(0xFF26A69A)
        "follow" in normalized || "connection" in normalized -> Color(0xFF66BB6A)
        "comment" in normalized -> Color(0xFF29B6F6)
        "reel" in normalized -> Color(0xFFFF8A65)
        "streak" in normalized || "xp" in normalized -> Color(0xFFFFC107)
        else -> accentColor
    }
}

private fun resolveNotificationDestination(notification: Notification): NotificationDestination {
    val normalizedType = notification.type.lowercase(Locale.getDefault())
    val conversationId = notification.dataValue("conversationId")
    val postId = notification.post?.id ?: notification.dataValue("postId")
    val reelId = notification.reel?.id ?: notification.dataValue("reelId")
    val actorId = notification.actor?.id
        ?: notification.dataValue("viewerId")
        ?: notification.dataValue("userId")
        ?: notification.dataValue("actorId")

    return when {
        notificationUsesVormexBranding(notification) || normalizedType == "admin_announcement" -> NotificationDestination(
            kind = NotificationDestinationKind.GrowthHub,
            label = "Open Vormex",
            hint = "Read the latest update from the Vormex team",
            badge = "Vormex"
        )

        !conversationId.isNullOrBlank() || "message" in normalizedType -> NotificationDestination(
            kind = NotificationDestinationKind.Conversation,
            targetId = conversationId,
            label = "Open chat",
            hint = "Jump back into the conversation",
            badge = "Chat"
        )

        !postId.isNullOrBlank() && "mention" in normalizedType -> NotificationDestination(
            kind = NotificationDestinationKind.Post,
            targetId = postId,
            label = "See mention",
            hint = "Open the post where you were mentioned",
            badge = "Post"
        )

        !postId.isNullOrBlank() && "comment" in normalizedType -> NotificationDestination(
            kind = NotificationDestinationKind.Post,
            targetId = postId,
            label = "View comments",
            hint = "Open the post and its discussion",
            badge = "Post"
        )

        !postId.isNullOrBlank() -> NotificationDestination(
            kind = NotificationDestinationKind.Post,
            targetId = postId,
            label = "Open post",
            hint = "See the post behind this update",
            badge = "Post"
        )

        !reelId.isNullOrBlank() && ("comment" in normalizedType || "mention" in normalizedType) -> NotificationDestination(
            kind = NotificationDestinationKind.Reel,
            targetId = reelId,
            label = "View reel thread",
            hint = "Open the reel and its reactions",
            badge = "Reel"
        )

        !reelId.isNullOrBlank() -> NotificationDestination(
            kind = NotificationDestinationKind.Reel,
            targetId = reelId,
            label = "Open reel",
            hint = "Watch the reel behind this alert",
            badge = "Reel"
        )

        "streak" in normalizedType || "xp" in normalizedType -> NotificationDestination(
            kind = NotificationDestinationKind.GrowthHub,
            label = "Open growth hub",
            hint = "Check streaks, XP, and rewards",
            badge = "Growth"
        )

        normalizedType == "people_you_know_joined" -> NotificationDestination(
            kind = NotificationDestinationKind.Network,
            label = "Find people",
            hint = "See who joined and grow your network",
            badge = "Network"
        )

        "connection" in normalizedType && !actorId.isNullOrBlank() -> NotificationDestination(
            kind = NotificationDestinationKind.Profile,
            targetId = actorId,
            label = "View profile",
            hint = if ("request" in normalizedType) {
                "See the person behind this request"
            } else {
                "Open the profile linked to this connection update"
            },
            badge = "Profile"
        )

        "connection" in normalizedType -> NotificationDestination(
            kind = NotificationDestinationKind.Network,
            label = "Open network",
            hint = "Manage your connection activity",
            badge = "Network"
        )

        !actorId.isNullOrBlank() -> NotificationDestination(
            kind = NotificationDestinationKind.Profile,
            targetId = actorId,
            label = "View profile",
            hint = "Open the person behind this alert",
            badge = "Profile"
        )

        else -> NotificationDestination(
            kind = NotificationDestinationKind.Network,
            label = "Open network",
            hint = "See more activity from your network",
            badge = "Network"
        )
    }
}

private fun notificationUsesVormexBranding(notification: Notification): Boolean {
    return notification.type.lowercase(Locale.getDefault()) == "admin_announcement" ||
        notification.dataValue("branding") == "vormex" ||
        notification.dataValue("senderType") == "admin"
}

private fun avatarInitials(displayName: String): String {
    val parts = displayName
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }

    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts.first().take(1).uppercase(Locale.getDefault())
        else -> {
            val first = parts.first().take(1)
            val last = parts.last().take(1)
            (first + last).uppercase(Locale.getDefault())
        }
    }
}

private fun compactTimestamp(createdAt: String): String {
    val date = createdAt.substringBefore('T')
    return if (date.length >= 10) date.substring(5) else createdAt.take(10)
}

private fun routeNotification(
    notification: Notification,
    onOpenProfile: (String) -> Unit,
    onOpenPost: (String) -> Unit,
    onOpenReel: (String) -> Unit,
    onOpenConversation: (String) -> Unit,
    onOpenNetwork: () -> Unit,
    onOpenGrowthHub: () -> Unit
) {
    val destination = resolveNotificationDestination(notification)

    when (destination.kind) {
        NotificationDestinationKind.Conversation -> {
            destination.targetId?.let(onOpenConversation) ?: onOpenNetwork()
        }
        NotificationDestinationKind.Post -> {
            destination.targetId?.let(onOpenPost) ?: onOpenNetwork()
        }
        NotificationDestinationKind.Reel -> {
            destination.targetId?.let(onOpenReel) ?: onOpenNetwork()
        }
        NotificationDestinationKind.Profile -> {
            destination.targetId?.let(onOpenProfile) ?: onOpenNetwork()
        }
        NotificationDestinationKind.Network -> onOpenNetwork()
        NotificationDestinationKind.GrowthHub -> onOpenGrowthHub()
    }
}

private fun Notification.dataValue(key: String): String? {
    return data?.get(key)?.jsonPrimitive?.contentOrNull
}
