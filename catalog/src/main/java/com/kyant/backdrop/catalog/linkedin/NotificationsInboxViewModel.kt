package com.kyant.backdrop.catalog.linkedin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.models.Notification
import com.kyant.backdrop.catalog.network.models.NotificationsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class NotificationsInboxUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isMarkingAllRead: Boolean = false,
    val unreadOnly: Boolean = false,
    val notifications: List<Notification> = emptyList(),
    val respondingCollabInviteIds: Set<String> = emptySet(),
    val unreadCount: Int = 0,
    val error: String? = null
)

class NotificationsInboxViewModel(
    private val context: Context
) : ViewModel() {
    companion object {
        private const val NOTIFICATION_FETCH_LIMIT = 50
    }

    private val _uiState = MutableStateFlow(NotificationsInboxUiState())
    val uiState: StateFlow<NotificationsInboxUiState> = _uiState.asStateFlow()

    private var allNotifications: List<Notification> = emptyList()
    private var latestCursor: String? = null
    private var cacheOwnerId: String? = null
    private var cacheHydrated = false
    private var refreshJob: Job? = null

    init {
        refresh(showLoader = true)
    }

    fun refresh(showLoader: Boolean = false, forceFull: Boolean = false) {
        if (refreshJob?.isActive == true) return

        refreshJob = viewModelScope.launch {
            hydrateCacheIfNeeded()

            val hasLocalData = allNotifications.isNotEmpty()
            _uiState.value = _uiState.value.copy(
                isLoading = showLoader && !hasLocalData,
                isRefreshing = hasLocalData || !showLoader,
                notifications = visibleNotifications(),
                error = null
            )

            val requestAfterCursor = latestCursor.takeIf { !forceFull && hasLocalData }
            val (notificationsResult, unreadCountResult) = coroutineScope {
                val notificationsDeferred = async {
                    fetchNotificationChanges(requestAfterCursor)
                }
                val unreadCountDeferred = async { ApiClient.getNotificationUnreadCount(context) }
                notificationsDeferred.await() to unreadCountDeferred.await()
            }

            val response = notificationsResult.getOrElse { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    notifications = visibleNotifications(),
                    error = error.message ?: "Failed to load notifications"
                )
                return@launch
            }

            allNotifications = if (requestAfterCursor == null) {
                response.notifications.sortedNewestFirst()
            } else {
                mergeNotifications(allNotifications, response.notifications)
            }
            latestCursor = response.latestCursor ?: latestCursor

            val unreadCount = unreadCountResult.getOrDefault(allNotifications.count { !it.isRead })
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isRefreshing = false,
                notifications = visibleNotifications(),
                unreadCount = unreadCount,
                error = null
            )
            persistCache()
        }
    }

    fun toggleUnreadOnly() {
        _uiState.value = _uiState.value.copy(unreadOnly = !_uiState.value.unreadOnly)
        _uiState.value = _uiState.value.copy(notifications = visibleNotifications())
        refresh()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun markAllAsRead() {
        if (_uiState.value.isMarkingAllRead || _uiState.value.unreadCount == 0) return

        viewModelScope.launch {
            val previousNotifications = allNotifications
            val previousState = _uiState.value
            allNotifications = allNotifications.map { it.copy(isRead = true) }
            _uiState.value = previousState.copy(
                isMarkingAllRead = true,
                notifications = visibleNotifications(),
                unreadCount = 0
            )
            persistCache()

            ApiClient.markAllNotificationsAsRead(context)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isMarkingAllRead = false)
                }
                .onFailure { error ->
                    allNotifications = previousNotifications
                    _uiState.value = previousState.copy(
                        isMarkingAllRead = false,
                        notifications = visibleNotifications(),
                        error = error.message ?: "Failed to mark all as read"
                    )
                    persistCache()
                }
        }
    }

    fun markAsRead(notificationIds: List<String>) {
        val notificationIdSet = notificationIds.toSet()
        if (notificationIdSet.isEmpty()) return

        val unreadIds = allNotifications
            .filter { it.id in notificationIdSet && !it.isRead }
            .map { it.id }

        if (unreadIds.isEmpty()) return

        val unreadIdSet = unreadIds.toSet()
        val previousNotifications = allNotifications
        val previousState = _uiState.value
        allNotifications = allNotifications.map { notification ->
            if (notification.id in unreadIdSet) notification.copy(isRead = true) else notification
        }
        _uiState.value = previousState.copy(
            notifications = visibleNotifications(),
            unreadCount = (previousState.unreadCount - unreadIds.size).coerceAtLeast(0)
        )
        persistCache()

        viewModelScope.launch {
            ApiClient.markNotificationsAsRead(context, unreadIds)
                .onFailure { error ->
                    allNotifications = previousNotifications
                    _uiState.value = previousState.copy(
                        notifications = visibleNotifications(),
                        error = error.message ?: "Failed to mark notification as read"
                    )
                    persistCache()
                }
        }
    }

    fun respondToCollabInvite(notificationId: String, postId: String, accept: Boolean) {
        if (notificationId in _uiState.value.respondingCollabInviteIds) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                respondingCollabInviteIds = _uiState.value.respondingCollabInviteIds + notificationId,
                error = null
            )

            ApiClient.respondToPostCollabInvite(context, postId, accept)
                .onSuccess {
                    refresh(forceFull = true)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "Failed to respond to collaboration invite"
                    )
                }

            _uiState.value = _uiState.value.copy(
                respondingCollabInviteIds = _uiState.value.respondingCollabInviteIds - notificationId
            )
        }
    }

    private suspend fun hydrateCacheIfNeeded() {
        if (cacheHydrated) return
        cacheHydrated = true
        cacheOwnerId = ApiClient.getCurrentUserId(context)

        val cached = withContext(Dispatchers.IO) {
            NotificationsInboxCacheStore.read(context, cacheOwnerId)
        } ?: return

        allNotifications = cached.notifications.sortedNewestFirst()
        latestCursor = cached.latestCursor
        _uiState.value = _uiState.value.copy(
            notifications = visibleNotifications(),
            unreadCount = cached.unreadCount
        )
    }

    private suspend fun fetchNotificationChanges(
        afterCursor: String?
    ): Result<NotificationsResponse> {
        if (afterCursor == null) {
            return ApiClient.getNotifications(
                context = context,
                limit = NOTIFICATION_FETCH_LIMIT,
                unreadOnly = false
            )
        }

        val incoming = mutableListOf<Notification>()
        var cursor = afterCursor
        var latestResponse: NotificationsResponse? = null
        var pageCount = 0
        var hasMoreNewer: Boolean

        do {
            val response = ApiClient.getNotifications(
                context = context,
                limit = NOTIFICATION_FETCH_LIMIT,
                unreadOnly = false,
                afterCursor = cursor
            ).getOrElse { return Result.failure(it) }

            incoming += response.notifications
            latestResponse = response
            pageCount += 1
            hasMoreNewer = response.hasMoreNewer

            val nextCursor = response.latestCursor
            if (nextCursor.isNullOrBlank() || nextCursor == cursor) break
            cursor = nextCursor
        } while (hasMoreNewer && pageCount < 10)

        val response = latestResponse ?: return Result.success(NotificationsResponse())
        return Result.success(
            response.copy(
                notifications = incoming,
                latestCursor = cursor
            )
        )
    }

    private fun visibleNotifications(): List<Notification> =
        if (_uiState.value.unreadOnly) allNotifications.filter { !it.isRead } else allNotifications

    private fun mergeNotifications(
        existing: List<Notification>,
        incoming: List<Notification>
    ): List<Notification> {
        val merged = existing.associateBy { it.id }.toMutableMap()
        incoming.forEach { notification ->
            merged[notification.id] = notification
        }
        return merged.values.toList().sortedNewestFirst()
    }

    private fun List<Notification>.sortedNewestFirst(): List<Notification> =
        sortedWith(compareByDescending<Notification> { it.createdAt }.thenByDescending { it.id })

    private fun persistCache() {
        NotificationsInboxCacheStore.write(
            context = context,
            userId = cacheOwnerId,
            notifications = allNotifications,
            latestCursor = latestCursor,
            unreadCount = _uiState.value.unreadCount
        )
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NotificationsInboxViewModel(context.applicationContext) as T
        }
    }
}
