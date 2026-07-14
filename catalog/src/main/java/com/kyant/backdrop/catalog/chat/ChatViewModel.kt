package com.kyant.backdrop.catalog.chat

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kyant.backdrop.catalog.ai.VormexAiGateway
import com.kyant.backdrop.catalog.ai.VormexAiOperationKind
import com.kyant.backdrop.catalog.ai.VormexAiPrepareResult
import com.kyant.backdrop.catalog.ai.VormexAiSuggestionsResult
import com.kyant.backdrop.catalog.ai.VormexAiSurface
import com.kyant.backdrop.catalog.chat.cache.CachedMessagesSnapshot
import com.kyant.backdrop.catalog.chat.cache.ChatCacheRepository
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.ChatSocketManager
import com.kyant.backdrop.catalog.network.GroupsApiService
import com.kyant.backdrop.catalog.network.models.ChatUser
import com.kyant.backdrop.catalog.network.models.Conversation
import com.kyant.backdrop.catalog.network.models.ConversationLastMessage
import com.kyant.backdrop.catalog.network.models.GroupMessageShortcut
import com.kyant.backdrop.catalog.network.models.Message
import com.kyant.backdrop.catalog.network.models.MessageReaction
import com.kyant.backdrop.catalog.network.models.MessagesResponse
import com.kyant.backdrop.catalog.notifications.MessageNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val TAG = "ChatViewModel"
private const val OUTGOING_TYPING_STOP_DELAY_MS = 1_000L
private const val OUTGOING_TYPING_HEARTBEAT_MS = 300L
private const val REFRESH_CONVERSATIONS_DEBOUNCE_MS = 500L
private const val UNREAD_COUNT_DEBOUNCE_MS = 1_000L
private const val REFRESH_CONVERSATIONS_MIN_INTERVAL_MS = 2_000L
private const val LIST_TYPING_TIMEOUT_MS = 4_000L

enum class ChatThreadReadyState {
    CACHED_READY,
    LOADING_WITHOUT_CACHE,
    EMPTY_THREAD,
    MESSAGES_READY
}

data class PeerPresence(
    val isOnline: Boolean,
    val lastActiveAt: String? = null
)

data class ChatUiState(
    val currentUserId: String? = null,
    val conversations: List<Conversation> = emptyList(),
    val groupShortcuts: List<GroupMessageShortcut> = emptyList(),
    val messages: List<Message> = emptyList(),
    val selectedConversation: Conversation? = null,
    val isResolvingConversationOpen: Boolean = false,
    val isLoadingConversations: Boolean = false,
    val isLoadingMessages: Boolean = false,
    val isLoadingMoreMessages: Boolean = false,
    val hasMoreMessages: Boolean = false,
    val messagesNextCursor: String? = null,
    val isSending: Boolean = false,
    val error: String? = null,
    val typingUserId: String? = null,
    val typingConversationIds: Set<String> = emptySet(),
    val peerPresence: Map<String, PeerPresence> = emptyMap(),
    val unreadCount: Int = 0,
    val messageRequestsCount: Int = 0,
    val aiSuggestions: List<String> = emptyList(),
    val isLoadingAiSuggestions: Boolean = false,
    val isPreparingAiSuggestions: Boolean = false,
    val aiStatusMessage: String? = null,
    val aiNeedsPreparation: Boolean = false,
    val aiCanUseCloudFallback: Boolean = false,
    val socketConnected: Boolean = false,
    val replyToMessage: Message? = null,
    val isUploadingAttachment: Boolean = false,
    val attachmentUploadError: String? = null,
    val isRecordingVoice: Boolean = false,
    val threadReadyState: ChatThreadReadyState = ChatThreadReadyState.EMPTY_THREAD,
    val initialDraftMessage: String? = null
)

class ChatViewModel(private val context: Context) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val chatCacheRepository = ChatCacheRepository(context)
    private val aiGateway = VormexAiGateway(context)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var loadConversationsJob: Job? = null
    private var loadGroupShortcutsJob: Job? = null
    private var loadMessagesJob: Job? = null
    private var preloadJob: Job? = null
    private var typingIndicatorTimeoutJob: Job? = null
    private val listTypingTimeoutJobs = mutableMapOf<String, Job>()
    private var outgoingTypingStopJob: Job? = null
    private val readReceiptJobs = mutableMapOf<String, Job>()
    private var hasLoadedConversations: Boolean = false
    private var conversationsLastLoadedAt: Long = 0L
    private var lastPreloadedUserId: String? = null
    private var isOutgoingTyping = false
    private var outgoingTypingConversationId: String? = null
    private var lastOutgoingTypingSentAt: Long = 0L
    private var refreshConversationsJob: Job? = null
    private var lastRefreshConversationsAt: Long = 0L
    private var loadUnreadCountJob: Job? = null
    private val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private var mediaRecorder: MediaRecorder? = null
    private var voiceRecordingFile: File? = null

    init {
        Log.d(TAG, "ChatViewModel init")
        viewModelScope.launch {
            val token = ApiClient.getRealtimeAccessToken(context)
            val userId = ApiClient.getCurrentUserId(context)
            ChatSocketManager.currentUserId = userId
            _uiState.update { it.copy(currentUserId = userId) }
            userId?.let { hydrateCachedConversations(it) }
            if (!token.isNullOrEmpty()) {
                ChatSocketManager.connect(token)
                ChatSocketManager.currentUserId = userId
            } else {
                Log.w(TAG, "No token available, socket not connected")
            }
        }
        collectSocketEvents()
        collectConnectionState()
    }

    fun onSessionChanged(currentUserId: String?) {
        viewModelScope.launch {
            val normalizedUserId = currentUserId?.takeIf { it.isNotBlank() }
            if (_uiState.value.currentUserId != normalizedUserId) {
                resetForSession(normalizedUserId)
            }

            if (normalizedUserId == null) {
                ChatSocketManager.resetSession()
                return@launch
            }

            ChatSocketManager.currentUserId = normalizedUserId
            hydrateCachedConversations(normalizedUserId)
            ApiClient.getRealtimeAccessToken(context)?.takeIf { it.isNotBlank() }?.let { token ->
                ChatSocketManager.connect(token)
                ChatSocketManager.currentUserId = normalizedUserId
            }
        }
    }

    private fun collectConnectionState() {
        viewModelScope.launch {
            ChatSocketManager.connectionStateFlow.collect { state ->
                _uiState.update {
                    it.copy(socketConnected = state == ChatSocketManager.ConnectionState.CONNECTED)
                }
            }
        }
    }

    private fun collectSocketEvents() {
        viewModelScope.launch {
            ChatSocketManager.newMessageFlow.collect { (conversationId, messageJson) ->
                try {
                    val message = json.decodeFromString(Message.serializer(), messageJson)
                    val currentUserId = _uiState.value.currentUserId?.takeIf { it.isNotBlank() }
                        ?: ensureCurrentUserId()
                        ?: ChatSocketManager.currentUserId?.takeIf { it.isNotBlank() }
                    if (!currentUserId.isNullOrBlank() && _uiState.value.currentUserId != currentUserId) {
                        _uiState.update { it.copy(currentUserId = currentUserId) }
                    }
                    val isSelectedConversation = _uiState.value.selectedConversation?.id == conversationId
                    Log.d(
                        TAG,
                        "Realtime message consumed conversation=$conversationId message=${message.id} " +
                            "clientMessageId=${message.clientMessageId.orEmpty()} selected=$isSelectedConversation"
                    )
                    var conversationKnown = false

                    if (isSelectedConversation) {
                        if (message.senderId != currentUserId) {
                            markAsRead()
                        }
                        _uiState.update { state ->
                            val updatedMessages = replaceMatchingPendingMessage(
                                upsertMessage(state.messages, message),
                                message
                            )
                            state.copy(
                                messages = updatedMessages,
                                threadReadyState = resolveThreadReadyState(
                                    messages = updatedMessages,
                                    fromCache = false
                                )
                            )
                        }
                        persistSelectedConversationSnapshot()
                        currentUserId?.let {
                            chatCacheRepository.markConversationRead(it, conversationId)
                        }
                        conversationKnown = applyConversationPreviewFromMessage(
                            conversationId = conversationId,
                            message = message,
                            incrementUnread = false
                        )
                    } else {
                        conversationKnown = applyConversationPreviewFromMessage(
                            conversationId = conversationId,
                            message = message,
                            incrementUnread = true
                        )
                        currentUserId?.let {
                            chatCacheRepository.upsertIncomingMessage(
                                cacheOwnerId = it,
                                conversationId = conversationId,
                                message = message,
                                currentUserId = currentUserId,
                                incrementUnread = true
                            )
                        }
                    }

                    if (conversationKnown) {
                        loadUnreadAndRequestsCount()
                    } else {
                        refreshConversations()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing socket message", e)
                }
            }
        }

        viewModelScope.launch {
            ChatSocketManager.typingFlow.collect { (conversationId, userId, isTyping) ->
                val state = _uiState.value
                if (conversationId.isBlank() || userId == state.currentUserId) {
                    return@collect
                }
                updateListTypingState(conversationId, isTyping)
                if (state.selectedConversation?.id != conversationId) {
                    return@collect
                }
                if (isTyping) {
                    scheduleTypingIndicatorClear(conversationId, userId)
                } else {
                    clearTypingIndicator()
                }
                _uiState.update { currentState ->
                    if (currentState.selectedConversation?.id != conversationId) return@update currentState
                    currentState.copy(typingUserId = if (isTyping) userId else null)
                }
            }
        }

        viewModelScope.launch {
            ChatSocketManager.presenceFlow.collect { event ->
                if (event.userId.isBlank() || event.userId == _uiState.value.currentUserId) {
                    return@collect
                }
                _uiState.update { state ->
                    state.copy(
                        peerPresence = state.peerPresence +
                            (event.userId to PeerPresence(event.isOnline, event.lastActiveAt))
                    )
                }
            }
        }

        viewModelScope.launch {
            ChatSocketManager.messagesDeliveredFlow.collect { event ->
                if (event.conversationId.isBlank()) return@collect
                val currentUserId = ensureCurrentUserId() ?: return@collect
                if (event.deliveredTo == currentUserId) return@collect

                _uiState.update { state ->
                    val updatedMessages = if (state.selectedConversation?.id == event.conversationId) {
                        state.messages.map { message ->
                            if (message.senderId == currentUserId && message.status.equals("SENT", ignoreCase = true)) {
                                message.copy(
                                    status = "DELIVERED",
                                    deliveredAt = event.deliveredAt.takeIf { it.isNotBlank() } ?: message.deliveredAt
                                )
                            } else {
                                message
                            }
                        }
                    } else {
                        state.messages
                    }
                    state.copy(
                        messages = updatedMessages,
                        conversations = state.conversations.map { conversation ->
                            val lastMessage = conversation.lastMessage
                            if (
                                conversation.id == event.conversationId &&
                                lastMessage != null &&
                                lastMessage.senderId == currentUserId &&
                                lastMessage.status.equals("SENT", ignoreCase = true)
                            ) {
                                conversation.copy(lastMessage = lastMessage.copy(status = "DELIVERED"))
                            } else {
                                conversation
                            }
                        }
                    )
                }
                chatCacheRepository.markOwnMessagesAsDeliveredToPeer(
                    cacheOwnerId = currentUserId,
                    conversationId = event.conversationId,
                    currentUserId = currentUserId,
                    deliveredAt = event.deliveredAt
                )
                if (_uiState.value.selectedConversation?.id == event.conversationId) {
                    persistSelectedConversationSnapshot()
                }
            }
        }

        viewModelScope.launch {
            ChatSocketManager.messageDeliveredFlow.collect { event ->
                if (event.messageId.isBlank() || event.conversationId.isBlank()) return@collect
                val currentUserId = ensureCurrentUserId() ?: return@collect

                _uiState.update { state ->
                    val updatedMessages = if (state.selectedConversation?.id == event.conversationId) {
                        state.messages.map { message ->
                            if (
                                message.id == event.messageId &&
                                message.senderId == currentUserId &&
                                message.status.equals("SENT", ignoreCase = true)
                            ) {
                                message.copy(
                                    status = "DELIVERED",
                                    deliveredAt = event.deliveredAt.takeIf { it.isNotBlank() } ?: message.deliveredAt
                                )
                            } else {
                                message
                            }
                        }
                    } else {
                        state.messages
                    }
                    state.copy(
                        messages = updatedMessages,
                        conversations = state.conversations.map { conversation ->
                            val lastMessage = conversation.lastMessage
                            if (
                                conversation.id == event.conversationId &&
                                lastMessage != null &&
                                lastMessage.id == event.messageId &&
                                lastMessage.senderId == currentUserId &&
                                lastMessage.status.equals("SENT", ignoreCase = true)
                            ) {
                                conversation.copy(lastMessage = lastMessage.copy(status = "DELIVERED"))
                            } else {
                                conversation
                            }
                        }
                    )
                }
                chatCacheRepository.markCachedMessageDelivered(
                    cacheOwnerId = currentUserId,
                    conversationId = event.conversationId,
                    messageId = event.messageId,
                    deliveredAt = event.deliveredAt
                )
                if (_uiState.value.selectedConversation?.id == event.conversationId) {
                    persistSelectedConversationSnapshot()
                }
            }
        }

        viewModelScope.launch {
            ChatSocketManager.messagesReadFlow.collect { (conversationId, readByUserId) ->
                val currentUserId = ensureCurrentUserId()
                if (readByUserId == currentUserId) {
                    loadUnreadAndRequestsCount()
                    return@collect
                }

                _uiState.update { state ->
                    val updatedMessages = if (state.selectedConversation?.id == conversationId) {
                        state.messages.map { message ->
                            if (message.senderId == state.currentUserId) {
                                message.copy(status = "READ")
                            } else {
                                message
                            }
                        }
                    } else {
                        state.messages
                    }
                    state.copy(messages = updatedMessages)
                }
                if (!currentUserId.isNullOrBlank()) {
                    chatCacheRepository.markOwnMessagesAsReadByPeer(
                        cacheOwnerId = currentUserId,
                        conversationId = conversationId,
                        currentUserId = currentUserId
                    )
                    if (_uiState.value.selectedConversation?.id == conversationId) {
                        persistSelectedConversationSnapshot()
                    }
                }
                refreshConversations()
            }
        }

        viewModelScope.launch {
            ChatSocketManager.messageDeletedFlow.collect { (messageId, conversationId, _) ->
                _uiState.update { state ->
                    if (state.selectedConversation?.id == conversationId) {
                        val updatedMessages = state.messages.filter { it.id != messageId }
                        state.copy(
                            messages = updatedMessages,
                            threadReadyState = resolveThreadReadyState(
                                messages = updatedMessages,
                                fromCache = false
                            )
                        )
                    } else {
                        state
                    }
                }
                ensureCurrentUserId()?.let { userId ->
                    chatCacheRepository.deleteCachedMessage(userId, conversationId, messageId)
                    if (_uiState.value.selectedConversation?.id == conversationId) {
                        persistSelectedConversationSnapshot()
                    }
                }
                refreshConversations()
            }
        }

        viewModelScope.launch {
            ChatSocketManager.messageEditedFlow.collect { (messageId, conversationId, content) ->
                _uiState.update { state ->
                    if (state.selectedConversation?.id == conversationId) {
                        state.copy(
                            messages = state.messages.map { message ->
                                if (message.id == messageId) message.copy(content = content) else message
                            }
                        )
                    } else {
                        state
                    }
                }
                ensureCurrentUserId()?.let { userId ->
                    chatCacheRepository.updateCachedMessageContent(
                        cacheOwnerId = userId,
                        conversationId = conversationId,
                        messageId = messageId,
                        content = content
                    )
                    if (_uiState.value.selectedConversation?.id == conversationId) {
                        persistSelectedConversationSnapshot()
                    }
                }
                refreshConversations()
            }
        }

        viewModelScope.launch {
            ChatSocketManager.reactionFlow.collect { event ->
                var updatedReactions: List<MessageReaction>? = null
                _uiState.update { state ->
                    if (state.selectedConversation?.id != event.conversationId) return@update state
                    val updatedMessages = state.messages.map { message ->
                        if (message.id != event.messageId) return@map message
                        val existing = message.reactions.firstOrNull { it.userId == event.userId }
                        val nextReactions = when (event.action) {
                            "removed" -> message.reactions.filterNot { it.userId == event.userId }
                            "updated" -> message.reactions.map {
                                if (it.userId == event.userId) it.copy(emoji = event.emoji) else it
                            }
                            else -> {
                                if (existing == null) {
                                    message.reactions + MessageReaction(
                                        id = "local-${event.messageId}-${event.userId}",
                                        userId = event.userId,
                                        emoji = event.emoji
                                    )
                                } else {
                                    message.reactions.map {
                                        if (it.userId == event.userId) it.copy(emoji = event.emoji) else it
                                    }
                                }
                            }
                        }
                        updatedReactions = nextReactions
                        message.copy(reactions = nextReactions)
                    }
                    state.copy(messages = updatedMessages)
                }
                ensureCurrentUserId()?.let { userId ->
                    updatedReactions?.let { reactions ->
                        chatCacheRepository.updateCachedMessageReactions(
                            cacheOwnerId = userId,
                            conversationId = event.conversationId,
                            messageId = event.messageId,
                            reactions = reactions
                        )
                    }
                    if (_uiState.value.selectedConversation?.id == event.conversationId) {
                        persistSelectedConversationSnapshot()
                    }
                }
            }
        }

        viewModelScope.launch {
            ChatSocketManager.allChatsClearedFlow.collect {
                val currentUserId = ensureCurrentUserId()
                stopOutgoingTyping()
                clearTypingIndicator()
                clearListTypingState()
                hasLoadedConversations = true
                conversationsLastLoadedAt = System.currentTimeMillis()
                ChatSocketManager.activeConversationId = null
                _uiState.update { state ->
                    state.copy(
                        conversations = emptyList(),
                        messages = emptyList(),
                        selectedConversation = null,
                        isResolvingConversationOpen = false,
                        isLoadingConversations = false,
                        isLoadingMessages = false,
                        isLoadingMoreMessages = false,
                        hasMoreMessages = false,
                        messagesNextCursor = null,
                        isSending = false,
                        error = null,
                        typingUserId = null,
                        unreadCount = 0,
                        messageRequestsCount = 0,
                        aiSuggestions = emptyList(),
                        isLoadingAiSuggestions = false,
                        replyToMessage = null,
                        isUploadingAttachment = false,
                        attachmentUploadError = null,
                        threadReadyState = ChatThreadReadyState.EMPTY_THREAD
                    )
                }
                currentUserId?.let { userId ->
                    chatCacheRepository.clearAll(userId)
                }
                MessageNotificationManager.clearAll(context)
            }
        }
    }

    fun ensureSocketConnected() {
        viewModelScope.launch {
            val token = ApiClient.getRealtimeAccessToken(context)
            if (!token.isNullOrEmpty()) {
                if (!ChatSocketManager.isConnected()) {
                    ChatSocketManager.connect(token)
                } else {
                    ChatSocketManager.reconnectIfNeeded()
                }
            }
        }
    }

    fun preloadChats(forceRefresh: Boolean = false) {
        if (preloadJob?.isActive == true && !forceRefresh) return

        preloadJob?.cancel()
        preloadJob = viewModelScope.launch {
            val token = ApiClient.getRealtimeAccessToken(context) ?: return@launch
            ChatSocketManager.connect(token)

            val currentUserId = ensureCurrentUserId() ?: return@launch
            lastPreloadedUserId = currentUserId
            loadConversationsInternal(
                cacheOwnerId = currentUserId,
                prefetchRecentMessages = false,
                forceRefresh = forceRefresh
            )
            refreshGroupShortcuts()
        }
    }

    fun ensureConversationsLoaded(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val currentUserId = ensureCurrentUserId() ?: return@launch
            loadConversationsInternal(cacheOwnerId = currentUserId, forceRefresh = forceRefresh)
            refreshGroupShortcuts()
        }
    }

    fun loadConversations() {
        viewModelScope.launch {
            ensureCurrentUserId()?.let { currentUserId ->
                loadConversationsInternal(cacheOwnerId = currentUserId)
                refreshGroupShortcuts()
            }
        }
    }

    fun refreshGroupShortcuts() {
        if (loadGroupShortcutsJob?.isActive == true) return

        loadGroupShortcutsJob = viewModelScope.launch {
            GroupsApiService.getMessageShortcuts(context)
                .onSuccess { shortcuts ->
                    _uiState.update { state ->
                        state.copy(
                            groupShortcuts = shortcuts,
                            error = state.error.takeUnless { state.conversations.isEmpty() && shortcuts.isNotEmpty() }
                        )
                    }
                }
                .onFailure { error ->
                    Log.w(TAG, "Failed to load group message shortcuts", error)
                }
        }
    }

    fun selectConversation(conversation: Conversation?) {
        stopOutgoingTyping()
        _uiState.value.selectedConversation?.let { ChatSocketManager.leaveChat(it.id) }
        clearTypingIndicator()

        ChatSocketManager.activeConversationId = conversation?.id
        conversation?.let { MessageNotificationManager.clearConversationNotification(context, it.id) }

        if (conversation == null) {
            _uiState.update {
                it.copy(
                    selectedConversation = null,
                    isResolvingConversationOpen = false,
                    messages = emptyList(),
                    messagesNextCursor = null,
                    hasMoreMessages = false,
                    typingUserId = null,
                    error = null,
                    isLoadingMessages = false,
                    isLoadingMoreMessages = false,
                    threadReadyState = ChatThreadReadyState.EMPTY_THREAD,
                    initialDraftMessage = null
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                selectedConversation = conversation,
                isResolvingConversationOpen = false,
                messages = emptyList(),
                messagesNextCursor = null,
                hasMoreMessages = false,
                typingUserId = null,
                error = null,
                isLoadingMessages = true,
                isLoadingMoreMessages = false,
                initialDraftMessage = null,
                threadReadyState = ChatThreadReadyState.LOADING_WITHOUT_CACHE
            )
        }

        ChatSocketManager.joinChat(conversation.id)
        ensureSocketConnected()
        ChatSocketManager.checkUserStatus(conversation.otherParticipant.id)

        viewModelScope.launch {
            val currentUserId = ensureCurrentUserId()
            currentUserId?.let { hydrateCachedMessages(it, conversation.id) }
            markConversationReadLocally(conversation.id)
            markAsRead(immediate = true)
            loadMessages(conversation.id)
        }
    }

    fun loadMessages(conversationId: String, cursor: String? = null) {
        if (_uiState.value.selectedConversation?.id != conversationId) return

        loadMessagesJob?.cancel()
        loadMessagesJob = viewModelScope.launch {
            val currentUserId = ensureCurrentUserId() ?: return@launch
            if (cursor == null && _uiState.value.messages.isEmpty()) {
                hydrateCachedMessages(currentUserId, conversationId)
            }
            if (cursor == null) {
                _uiState.update {
                    val hasCachedMessages = it.messages.isNotEmpty()
                    it.copy(
                        isLoadingMessages = !hasCachedMessages,
                        error = null,
                        threadReadyState = if (hasCachedMessages) {
                            it.threadReadyState
                        } else {
                            ChatThreadReadyState.LOADING_WITHOUT_CACHE
                        }
                    )
                }
            } else {
                _uiState.update { it.copy(isLoadingMoreMessages = true, error = null) }
            }

            chatCacheRepository.refreshMessages(
                cacheOwnerId = currentUserId,
                conversationId = conversationId,
                limit = 50,
                cursor = cursor
            )
                .onSuccess { response ->
                    _uiState.update { state ->
                        val updatedMessages = if (cursor == null) {
                            mergeRefreshedMessagesWithLocalState(
                                currentMessages = state.messages,
                                refreshedMessages = response.messages
                            )
                        } else {
                            dedupeAndSortByCreatedAt(state.messages + response.messages)
                        }
                        state.copy(
                            messages = updatedMessages,
                            isLoadingMessages = false,
                            isLoadingMoreMessages = false,
                            hasMoreMessages = resolveHasMoreAfterRefresh(
                                state = state,
                                response = response,
                                cursor = cursor
                            ),
                            messagesNextCursor = resolveNextCursorAfterRefresh(
                                state = state,
                                response = response,
                                cursor = cursor
                            ),
                            error = null,
                            threadReadyState = resolveThreadReadyState(
                                messages = updatedMessages,
                                fromCache = false
                            )
                        )
                    }
                    cacheCurrentMessages(conversationId)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingMessages = false,
                            isLoadingMoreMessages = false,
                            error = if (it.messages.isEmpty()) error.message else null,
                            threadReadyState = if (it.messages.isEmpty()) {
                                ChatThreadReadyState.EMPTY_THREAD
                            } else {
                                it.threadReadyState
                            }
                        )
                    }
                }
        }
    }

    fun loadMoreMessages() {
        val state = _uiState.value
        if (state.isLoadingMessages || state.isLoadingMoreMessages || !state.hasMoreMessages) return

        val conversationId = state.selectedConversation?.id ?: return
        val cursor = state.messagesNextCursor ?: return
        loadMessages(conversationId, cursor)
    }

    fun uploadAndSendMessage(
        uri: Uri,
        fileName: String,
        mimeType: String,
        fileSize: Long? = null,
        durationMs: Long? = null,
        caption: String = "",
        localPreviewUrl: String? = null,
        replyToId: String? = null
    ) {
        val conversation = _uiState.value.selectedConversation ?: return
        val currentUserId = conversation.currentParticipantId(_uiState.value.currentUserId).orEmpty()
        val contentType = when {
            mimeType.startsWith("image/") -> "image"
            mimeType.startsWith("video/") -> "video"
            mimeType.startsWith("audio/") -> "audio"
            else -> "document"
        }
        val pendingLabel = when (contentType) {
            "image" -> "Photo"
            "video" -> "Video"
            "audio" -> "Voice"
            else -> "Document"
        }
        val pendingId = "pending-${System.currentTimeMillis()}"
        val nowIso = isoFormatter.format(Date())
        val optimisticFileSize = fileSize?.coerceAtMost(Int.MAX_VALUE.toLong())?.toInt()
        val optimisticMessage = Message(
            id = pendingId,
            clientMessageId = pendingId,
            conversationId = conversation.id,
            senderId = currentUserId,
            receiverId = conversation.otherParticipant.id,
            content = caption.ifBlank { "Sending $pendingLabel..." },
            contentType = contentType,
            mediaUrl = localPreviewUrl,
            mediaType = contentType,
            fileName = fileName,
            fileSize = optimisticFileSize,
            status = "SENDING",
            createdAt = nowIso,
            updatedAt = nowIso
        )

        _uiState.update { state ->
            state.copy(
                messages = dedupeAndSortByCreatedAt(state.messages + optimisticMessage),
                isUploadingAttachment = true,
                attachmentUploadError = null,
                threadReadyState = ChatThreadReadyState.MESSAGES_READY
            )
        }

        viewModelScope.launch {
            persistSelectedConversationSnapshot()

            ApiClient.uploadChatMedia(
                context = context,
                uri = uri,
                fileName = fileName,
                mimeType = mimeType,
                fileSize = fileSize,
                durationMs = durationMs
            ).onSuccess { upload ->
                val content = caption.ifBlank {
                    when (contentType) {
                        "image" -> "📷 Photo"
                        "video" -> "🎬 Video"
                        "audio" -> "🎤 Voice message"
                        else -> "📎 ${upload.fileName}"
                    }
                }

                sendMessageReliable(
                    conversationId = conversation.id,
                    content = content,
                    contentType = contentType,
                    mediaUrl = upload.mediaUrl,
                    mediaType = upload.mediaType,
                    fileName = upload.fileName,
                    fileSize = upload.fileSize,
                    replyToId = replyToId,
                    clientMessageId = pendingId
                ).onSuccess { message ->
                    reconcileCurrentUserIdFromSentMessage(conversation, message)
                    _uiState.update { state ->
                        val withoutPending = state.messages.filterNot { it.id == pendingId }
                        state.copy(
                            messages = dedupeAndSortByCreatedAt(withoutPending + message),
                            isUploadingAttachment = false,
                            attachmentUploadError = null,
                            threadReadyState = ChatThreadReadyState.MESSAGES_READY
                        )
                    }
                    persistSelectedConversationSnapshot()
                    if (applyConversationPreviewFromMessage(conversation.id, message, incrementUnread = false)) {
                        loadUnreadAndRequestsCount()
                    } else {
                        refreshConversations()
                    }
                }.onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.map { current ->
                                if (current.id == pendingId) {
                                    current.copy(
                                        content = content,
                                        mediaUrl = upload.mediaUrl,
                                        mediaType = upload.mediaType,
                                        fileName = upload.fileName,
                                        fileSize = upload.fileSize,
                                        status = "FAILED",
                                        updatedAt = isoFormatter.format(Date())
                                    )
                                } else {
                                    current
                                }
                            },
                            isUploadingAttachment = false,
                            attachmentUploadError = error.message
                        )
                    }
                    persistSelectedConversationSnapshot()
                }
            }.onFailure { error ->
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages.filterNot { it.id == pendingId },
                        isUploadingAttachment = false,
                        attachmentUploadError = error.message
                    )
                }
                persistSelectedConversationSnapshot()
            }
        }
    }

    fun uploadAndSendMessage(
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        caption: String = "",
        localPreviewUrl: String? = null,
        replyToId: String? = null
    ) {
        val conversation = _uiState.value.selectedConversation ?: return
        val currentUserId = conversation.currentParticipantId(_uiState.value.currentUserId).orEmpty()
        val contentType = when {
            mimeType.startsWith("image/") -> "image"
            mimeType.startsWith("video/") -> "video"
            mimeType.startsWith("audio/") -> "audio"
            else -> "document"
        }
        val pendingLabel = when (contentType) {
            "image" -> "Photo"
            "video" -> "Video"
            "audio" -> "Voice"
            else -> "Document"
        }
        val pendingId = "pending-${System.currentTimeMillis()}"
        val nowIso = isoFormatter.format(Date())
        val optimisticMessage = Message(
            id = pendingId,
            clientMessageId = pendingId,
            conversationId = conversation.id,
            senderId = currentUserId,
            receiverId = conversation.otherParticipant.id,
            content = caption.ifBlank { "Sending $pendingLabel..." },
            contentType = contentType,
            mediaUrl = localPreviewUrl,
            mediaType = contentType,
            fileName = fileName,
            fileSize = fileBytes.size,
            status = "SENDING",
            createdAt = nowIso,
            updatedAt = nowIso
        )

        _uiState.update { state ->
            state.copy(
                messages = dedupeAndSortByCreatedAt(state.messages + optimisticMessage),
                isUploadingAttachment = true,
                attachmentUploadError = null,
                threadReadyState = ChatThreadReadyState.MESSAGES_READY
            )
        }

        viewModelScope.launch {
            persistSelectedConversationSnapshot()

            ApiClient.uploadChatMedia(context, fileBytes, fileName, mimeType)
                .onSuccess { upload ->
                    val content = caption.ifBlank {
                        when (contentType) {
                            "image" -> "📷 Photo"
                            "video" -> "🎬 Video"
                            "audio" -> "🎤 Voice message"
                            else -> "📎 ${upload.fileName}"
                        }
                    }

                    sendMessageReliable(
                        conversationId = conversation.id,
                        content = content,
                        contentType = contentType,
                        mediaUrl = upload.mediaUrl,
                        mediaType = upload.mediaType,
                        fileName = upload.fileName,
                        fileSize = upload.fileSize,
                        replyToId = replyToId,
                        clientMessageId = pendingId
                    ).onSuccess { message ->
                        reconcileCurrentUserIdFromSentMessage(conversation, message)
                        _uiState.update { state ->
                            val withoutPending = state.messages.filterNot { it.id == pendingId }
                            state.copy(
                                messages = dedupeAndSortByCreatedAt(withoutPending + message),
                                isUploadingAttachment = false,
                                attachmentUploadError = null,
                                threadReadyState = ChatThreadReadyState.MESSAGES_READY
                            )
                        }
                        persistSelectedConversationSnapshot()
                        if (applyConversationPreviewFromMessage(conversation.id, message, incrementUnread = false)) {
                            loadUnreadAndRequestsCount()
                        } else {
                            refreshConversations()
                        }
                    }.onFailure { error ->
                        _uiState.update { state ->
                            state.copy(
                                messages = state.messages.map { current ->
                                    if (current.id == pendingId) {
                                        current.copy(
                                            content = content,
                                            mediaUrl = upload.mediaUrl,
                                            mediaType = upload.mediaType,
                                            fileName = upload.fileName,
                                            fileSize = upload.fileSize,
                                            status = "FAILED",
                                            updatedAt = isoFormatter.format(Date())
                                        )
                                    } else {
                                        current
                                    }
                                },
                                isUploadingAttachment = false,
                                attachmentUploadError = error.message
                            )
                        }
                        persistSelectedConversationSnapshot()
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.filterNot { it.id == pendingId },
                            isUploadingAttachment = false,
                            attachmentUploadError = error.message
                        )
                    }
                    persistSelectedConversationSnapshot()
                }
        }
    }

    fun clearAttachmentError() {
        _uiState.update { it.copy(attachmentUploadError = null) }
    }

    fun startVoiceRecording(context: Context) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val file = File(context.cacheDir, "chat_voice_${System.currentTimeMillis()}.m4a")
                    voiceRecordingFile = file
                    val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        MediaRecorder(context)
                    } else {
                        @Suppress("DEPRECATION")
                        MediaRecorder()
                    }.apply {
                        setAudioSource(MediaRecorder.AudioSource.MIC)
                        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                        setOutputFile(file.absolutePath)
                        prepare()
                        start()
                    }
                    mediaRecorder = recorder
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(isRecordingVoice = true) }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start voice recording", e)
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(attachmentUploadError = "Could not start recording") }
                    }
                }
            }
        }
    }

    fun stopVoiceRecordingAndSend(context: Context) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    mediaRecorder?.apply {
                        stop()
                        release()
                    }
                    mediaRecorder = null
                    val file = voiceRecordingFile ?: return@withContext
                    voiceRecordingFile = null
                    val localPreviewUrl = file.toURI().toString()
                    val bytes = file.readBytes()
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(isRecordingVoice = false) }
                        uploadAndSendMessage(
                            fileBytes = bytes,
                            fileName = "voice.m4a",
                            mimeType = "audio/mp4",
                            caption = "",
                            localPreviewUrl = localPreviewUrl,
                            replyToId = _uiState.value.replyToMessage?.id
                        )
                        clearReplyTo()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to stop/send voice", e)
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                isRecordingVoice = false,
                                attachmentUploadError = e.message ?: "Voice message was too short"
                            )
                        }
                    }
                }
            }
        }
    }

    fun cancelVoiceRecording() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    mediaRecorder?.apply {
                        stop()
                        release()
                    }
                    mediaRecorder = null
                    voiceRecordingFile?.delete()
                    voiceRecordingFile = null
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(isRecordingVoice = false) }
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    private suspend fun sendMessageReliable(
        conversationId: String,
        content: String,
        contentType: String = "text",
        mediaUrl: String? = null,
        mediaType: String? = null,
        fileName: String? = null,
        fileSize: Int? = null,
        replyToId: String?,
        clientMessageId: String
    ): Result<Message> {
        val realtimeResult = sendMessageViaRealtime(
            conversationId = conversationId,
            content = content,
            contentType = contentType,
            mediaUrl = mediaUrl,
            mediaType = mediaType,
            fileName = fileName,
            fileSize = fileSize,
            replyToId = replyToId,
            clientMessageId = clientMessageId
        )

        if (realtimeResult.isSuccess) return realtimeResult

        val realtimeError = realtimeResult.exceptionOrNull()
        if (!shouldFallbackToRestSend(realtimeError)) {
            return realtimeResult
        }

        return ApiClient.sendMessage(
            context = context,
            conversationId = conversationId,
            content = content,
            contentType = contentType,
            mediaUrl = mediaUrl,
            mediaType = mediaType,
            fileName = fileName,
            fileSize = fileSize,
            replyToId = replyToId,
            clientMessageId = clientMessageId
        )
    }

    private suspend fun sendMessageViaRealtime(
        conversationId: String,
        content: String,
        contentType: String,
        mediaUrl: String?,
        mediaType: String?,
        fileName: String?,
        fileSize: Int?,
        replyToId: String?,
        clientMessageId: String
    ): Result<Message> {
        val token = ApiClient.getRealtimeAccessToken(context)
            ?: return Result.failure(Exception("Not logged in"))
        ChatSocketManager.currentUserId = ensureCurrentUserId()
        if (!ChatSocketManager.isConnected()) {
            ChatSocketManager.connect(token)
        } else {
            ChatSocketManager.reconnectIfNeeded()
        }
        ChatSocketManager.joinChat(conversationId)

        return ChatSocketManager.sendMessageWithAck(
            conversationId = conversationId,
            content = content,
            contentType = contentType,
            mediaUrl = mediaUrl,
            mediaType = mediaType,
            fileName = fileName,
            fileSize = fileSize,
            replyToId = replyToId,
            clientMessageId = clientMessageId
        ).mapCatching { messageJson ->
            json.decodeFromString(Message.serializer(), messageJson)
        }
    }

    private fun shouldFallbackToRestSend(error: Throwable?): Boolean {
        val message = error?.message.orEmpty().lowercase(Locale.US)
        return message.contains("realtime connection unavailable") ||
            message.contains("not authenticated") ||
            message.contains("socket authentication failed") ||
            message.contains("acknowledgement timed out")
    }

    fun sendMessage(content: String, replyToId: String? = null) {
        val conversation = _uiState.value.selectedConversation ?: return
        if (content.isBlank()) return

        val nowIso = isoFormatter.format(Date())
        val tempMessageId = "local-${System.currentTimeMillis()}"
        val optimisticMessage = Message(
            id = tempMessageId,
            clientMessageId = tempMessageId,
            conversationId = conversation.id,
            senderId = conversation.currentParticipantId(_uiState.value.currentUserId).orEmpty(),
            receiverId = conversation.otherParticipant.id,
            content = content,
            contentType = "text",
            status = "SENDING",
            createdAt = nowIso,
            updatedAt = nowIso
        )

        _uiState.update { state ->
            state.copy(
                messages = dedupeAndSortByCreatedAt(state.messages + optimisticMessage),
                isSending = true,
                error = null,
                threadReadyState = ChatThreadReadyState.MESSAGES_READY
            )
        }

        viewModelScope.launch {
            persistSelectedConversationSnapshot()

            sendMessageReliable(
                conversationId = conversation.id,
                content = content,
                contentType = "text",
                replyToId = replyToId,
                clientMessageId = tempMessageId
            )
                .onSuccess { message ->
                    reconcileCurrentUserIdFromSentMessage(conversation, message)
                    _uiState.update { state ->
                        val withoutTemp = state.messages.filterNot { it.id == tempMessageId }
                        state.copy(
                            messages = dedupeAndSortByCreatedAt(withoutTemp + message),
                            isSending = false,
                            threadReadyState = ChatThreadReadyState.MESSAGES_READY
                        )
                    }
                    persistSelectedConversationSnapshot()
                    if (applyConversationPreviewFromMessage(conversation.id, message, incrementUnread = false)) {
                        loadUnreadAndRequestsCount()
                    } else {
                        refreshConversations()
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.map { message ->
                                if (message.id == tempMessageId) {
                                    message.copy(status = "FAILED", updatedAt = isoFormatter.format(Date()))
                                } else {
                                    message
                                }
                            },
                            isSending = false,
                            error = error.message
                        )
                    }
                    persistSelectedConversationSnapshot()
                }
        }
    }

    fun retryMessage(message: Message) {
        val conversation = _uiState.value.selectedConversation ?: return
        if (!message.status.equals("FAILED", ignoreCase = true)) return
        if (message.contentType.lowercase(Locale.US) != "text" || !message.mediaUrl.isNullOrBlank()) {
            _uiState.update { it.copy(error = "Please resend this attachment.") }
            return
        }

        val retryClientMessageId = message.clientMessageId
            ?.takeIf { it.isNotBlank() }
            ?: "retry-${System.currentTimeMillis()}"
        val nowIso = isoFormatter.format(Date())

        _uiState.update { state ->
            state.copy(
                messages = state.messages.map { current ->
                    if (current.id == message.id) {
                        current.copy(
                            clientMessageId = retryClientMessageId,
                            status = "SENDING",
                            updatedAt = nowIso
                        )
                    } else {
                        current
                    }
                },
                isSending = true,
                error = null
            )
        }

        viewModelScope.launch {
            persistSelectedConversationSnapshot()

            sendMessageReliable(
                conversationId = conversation.id,
                content = message.content,
                contentType = "text",
                replyToId = message.replyToId,
                clientMessageId = retryClientMessageId
            )
                .onSuccess { sentMessage ->
                    reconcileCurrentUserIdFromSentMessage(conversation, sentMessage)
                    _uiState.update { state ->
                        val withoutFailed = state.messages.filterNot { it.id == message.id }
                        state.copy(
                            messages = dedupeAndSortByCreatedAt(withoutFailed + sentMessage),
                            isSending = false,
                            threadReadyState = ChatThreadReadyState.MESSAGES_READY
                        )
                    }
                    persistSelectedConversationSnapshot()
                    if (applyConversationPreviewFromMessage(conversation.id, sentMessage, incrementUnread = false)) {
                        loadUnreadAndRequestsCount()
                    } else {
                        refreshConversations()
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.map { current ->
                                if (current.id == message.id) {
                                    current.copy(status = "FAILED", updatedAt = isoFormatter.format(Date()))
                                } else {
                                    current
                                }
                            },
                            isSending = false,
                            error = error.message
                        )
                    }
                    persistSelectedConversationSnapshot()
                }
        }
    }

    fun sendTyping(isTyping: Boolean) {
        val conversationId = _uiState.value.selectedConversation?.id ?: return

        if (!isTyping) {
            stopOutgoingTyping()
            return
        }

        val now = System.currentTimeMillis()
        val shouldSendHeartbeat =
            !isOutgoingTyping ||
                outgoingTypingConversationId != conversationId ||
                (now - lastOutgoingTypingSentAt) >= OUTGOING_TYPING_HEARTBEAT_MS

        if (shouldSendHeartbeat) {
            ChatSocketManager.sendTyping(conversationId, true)
            lastOutgoingTypingSentAt = now
        }

        isOutgoingTyping = true
        outgoingTypingConversationId = conversationId

        outgoingTypingStopJob?.cancel()
        outgoingTypingStopJob = viewModelScope.launch {
            delay(OUTGOING_TYPING_STOP_DELAY_MS)
            if (_uiState.value.selectedConversation?.id == conversationId) {
                stopOutgoingTyping(conversationId)
            }
        }
    }

    fun markAsRead(immediate: Boolean = false) {
        _uiState.value.selectedConversation?.let { conversation ->
            markConversationReadLocally(conversation.id)
            enqueueReadReceipt(conversation.id, immediate)
        }
    }

    private fun enqueueReadReceipt(conversationId: String, immediate: Boolean) {
        readReceiptJobs.remove(conversationId)?.cancel()
        readReceiptJobs[conversationId] = viewModelScope.launch {
            val waitMillis = ChatReadReceiptPolicy.delayMillis(immediate)
            if (waitMillis > 0) {
                delay(waitMillis)
            }
            ApiClient.markAsRead(context, conversationId)
                .onFailure { ChatSocketManager.markRead(conversationId) }
            ensureCurrentUserId()?.let { userId ->
                chatCacheRepository.markConversationRead(userId, conversationId)
            }
            readReceiptJobs.remove(conversationId)
        }
    }

    fun deleteMessage(messageId: String, forEveryone: Boolean = false) {
        val conversationId = _uiState.value.selectedConversation?.id ?: return
        viewModelScope.launch {
            ApiClient.deleteMessage(context, messageId, forEveryone).onSuccess {
                _uiState.update { state ->
                    val updatedMessages = state.messages.filter { it.id != messageId }
                    state.copy(
                        messages = updatedMessages,
                        threadReadyState = resolveThreadReadyState(
                            messages = updatedMessages,
                            fromCache = false
                        )
                    )
                }
                ensureCurrentUserId()?.let { userId ->
                    chatCacheRepository.deleteCachedMessage(userId, conversationId, messageId)
                }
                persistSelectedConversationSnapshot()
                refreshConversations()
            }
        }
    }

    fun setReplyTo(message: Message?) {
        _uiState.update { it.copy(replyToMessage = message) }
    }

    fun clearReplyTo() {
        _uiState.update { it.copy(replyToMessage = null) }
    }

    fun reactToMessage(messageId: String, emoji: String) {
        viewModelScope.launch {
            ApiClient.addReaction(context, messageId, emoji).onSuccess {
                var updatedReactions: List<MessageReaction>? = null
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages.map { message ->
                            if (message.id == messageId) {
                                val existingReaction = message.reactions.find { it.userId == state.currentUserId }
                                val newReactions = if (existingReaction != null) {
                                    if (existingReaction.emoji == emoji) {
                                        message.reactions.filter { it.userId != state.currentUserId }
                                    } else {
                                        message.reactions.map { reaction ->
                                            if (reaction.userId == state.currentUserId) {
                                                reaction.copy(emoji = emoji)
                                            } else {
                                                reaction
                                            }
                                        }
                                    }
                                } else {
                                    message.reactions + MessageReaction(
                                        id = "local-${System.currentTimeMillis()}",
                                        userId = state.currentUserId.orEmpty(),
                                        emoji = emoji
                                    )
                                }
                                updatedReactions = newReactions
                                message.copy(reactions = newReactions)
                            } else {
                                message
                            }
                        }
                    )
                }
                val conversationId = _uiState.value.selectedConversation?.id
                val currentUserId = ensureCurrentUserId()
                if (!conversationId.isNullOrBlank() && !currentUserId.isNullOrBlank() && updatedReactions != null) {
                    chatCacheRepository.updateCachedMessageReactions(
                        cacheOwnerId = currentUserId,
                        conversationId = conversationId,
                        messageId = messageId,
                        reactions = updatedReactions.orEmpty()
                    )
                }
                persistSelectedConversationSnapshot()
            }
        }
    }

    fun openChatWithUser(userId: String, initialDraft: String? = null) {
        viewModelScope.launch {
            val selectedOtherUserId = _uiState.value.selectedConversation?.otherParticipant?.id
            if (_uiState.value.selectedConversation != null && selectedOtherUserId != userId) {
                beginConversationOpenResolution()
            } else {
                _uiState.update { it.copy(isResolvingConversationOpen = true, error = null) }
            }

            val currentUserId = ensureCurrentUserId()
            val existingConversation = _uiState.value.conversations.firstOrNull { it.otherParticipant.id == userId }
                ?: currentUserId?.let { cacheOwnerId ->
                    runCatching {
                        chatCacheRepository.getCachedConversations(cacheOwnerId)
                    }.getOrNull()?.firstOrNull { it.otherParticipant.id == userId }
                }

            if (existingConversation != null) {
                currentUserId?.let { cacheOwnerId ->
                    chatCacheRepository.upsertConversation(cacheOwnerId, existingConversation)
                }
                _uiState.update { state ->
                    state.copy(
                        conversations = listOf(existingConversation) + state.conversations.filterNot { it.id == existingConversation.id },
                        error = null
                    )
                }
                if (_uiState.value.selectedConversation?.id != existingConversation.id) {
                    selectConversation(existingConversation)
                } else {
                    _uiState.update {
                        it.copy(
                            selectedConversation = existingConversation,
                            isResolvingConversationOpen = false,
                            error = null
                        )
                    }
                }
            }

            ApiClient.getOrCreateConversation(context, userId)
                .onSuccess { conversation ->
                    currentUserId?.let { cacheOwnerId ->
                        chatCacheRepository.upsertConversation(cacheOwnerId, conversation)
                    }
                    _uiState.update { state ->
                        state.copy(
                            conversations = listOf(conversation) + state.conversations.filterNot { it.id == conversation.id }
                        )
                    }
                    if (_uiState.value.selectedConversation?.id != conversation.id) {
                        selectConversation(conversation)
                    } else {
                        _uiState.update { state ->
                            state.copy(
                                selectedConversation = conversation,
                                isResolvingConversationOpen = false,
                                error = null
                            )
                        }
                    }
                    applyInitialDraftIfEmpty(conversation, initialDraft)
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to open chat with user $userId", error)
                    _uiState.update {
                        it.copy(
                            isResolvingConversationOpen = false,
                            error = error.message ?: "Failed to open chat"
                        )
                    }
                }
        }
    }

    fun clearInitialDraftMessage() {
        _uiState.update { it.copy(initialDraftMessage = null) }
    }

    private fun applyInitialDraftIfEmpty(conversation: Conversation, initialDraft: String?) {
        val trimmedDraft = initialDraft?.trim().orEmpty()
        if (trimmedDraft.isBlank() || conversation.lastMessage != null) return

        _uiState.update { state ->
            if (state.selectedConversation?.id == conversation.id) {
                state.copy(initialDraftMessage = trimmedDraft)
            } else {
                state
            }
        }
    }

    fun openConversationById(conversationId: String) {
        val targetConversationId = conversationId.trim()
        if (targetConversationId.isBlank()) return

        if (_uiState.value.selectedConversation?.id != targetConversationId) {
            beginConversationOpenResolution()
        } else {
            _uiState.update { it.copy(isResolvingConversationOpen = true, error = null) }
        }

        MessageNotificationManager.clearConversationNotification(context, targetConversationId)

        _uiState.value.conversations.firstOrNull { it.id == targetConversationId }?.let { conversation ->
            selectConversation(conversation)
        }

        viewModelScope.launch {
            val currentUserId = ensureCurrentUserId()
            val cachedConversation = currentUserId?.let {
                runCatching {
                    chatCacheRepository.getCachedConversation(it, targetConversationId)
                }.getOrNull()
            }
            if (cachedConversation != null) {
                _uiState.update { state ->
                    state.copy(
                        conversations = listOf(cachedConversation) + state.conversations.filterNot { it.id == cachedConversation.id },
                        isResolvingConversationOpen = false,
                        error = null
                    )
                }
                selectConversation(cachedConversation)
            }

            ApiClient.getConversation(context, targetConversationId)
                .onSuccess { conversation ->
                    currentUserId?.let { userId ->
                        chatCacheRepository.upsertConversation(userId, conversation)
                    }
                    _uiState.update { state ->
                        state.copy(
                            conversations = listOf(conversation) + state.conversations.filterNot { it.id == conversation.id },
                            selectedConversation = if (state.selectedConversation?.id == conversation.id) {
                                conversation
                            } else {
                                state.selectedConversation
                            },
                            isResolvingConversationOpen = false,
                            error = null
                        )
                    }
                    if (_uiState.value.selectedConversation?.id != conversation.id) {
                        selectConversation(conversation)
                    }
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to open conversation $targetConversationId", error)
                    _uiState.update {
                        it.copy(
                            isResolvingConversationOpen = false,
                            error = error.message ?: "Failed to open chat"
                        )
                    }
                }
        }
    }

    private fun beginConversationOpenResolution() {
        stopOutgoingTyping()
        _uiState.value.selectedConversation?.let { ChatSocketManager.leaveChat(it.id) }
        loadMessagesJob?.cancel()
        clearTypingIndicator()
        ChatSocketManager.activeConversationId = null

        _uiState.update {
            it.copy(
                selectedConversation = null,
                isResolvingConversationOpen = true,
                messages = emptyList(),
                messagesNextCursor = null,
                hasMoreMessages = false,
                typingUserId = null,
                error = null,
                isLoadingMessages = false,
                isLoadingMoreMessages = false,
                threadReadyState = ChatThreadReadyState.LOADING_WITHOUT_CACHE,
                initialDraftMessage = null
            )
        }
    }

    fun clearLocalMessages() {
        _uiState.update {
            it.copy(
                messages = emptyList(),
                threadReadyState = ChatThreadReadyState.EMPTY_THREAD
            )
        }
    }

    fun deleteCurrentConversation(onSuccess: () -> Unit = {}) {
        val conversation = _uiState.value.selectedConversation ?: return
        viewModelScope.launch {
            ApiClient.deleteConversation(context, conversation.id)
                .onSuccess {
                    ensureCurrentUserId()?.let { userId ->
                        chatCacheRepository.deleteConversation(userId, conversation.id)
                    }
                    selectConversation(null)
                    _uiState.update { state ->
                        state.copy(conversations = state.conversations.filter { it.id != conversation.id })
                    }
                    onSuccess()
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to delete conversation", error)
                    _uiState.update { it.copy(error = "Failed to delete chat: ${error.message}") }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun loadAiSuggestions() {
        loadAiSuggestions(explicitCloudFallback = false)
    }

    fun loadAiSuggestions(explicitCloudFallback: Boolean) {
        val lastMessage = _uiState.value.messages.lastOrNull { it.senderId != _uiState.value.currentUserId }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingAiSuggestions = true,
                    aiStatusMessage = null,
                    aiNeedsPreparation = false,
                    aiCanUseCloudFallback = false
                )
            }
            val messageText = lastMessage?.content
            if (messageText.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        aiSuggestions = generateMockSuggestions(lastMessage?.content),
                        isLoadingAiSuggestions = false
                    )
                }
                return@launch
            }

            when (val result = aiGateway.smartReplies(
                lastIncomingMessage = messageText,
                surface = VormexAiSurface.CHAT,
                allowCloudFallback = explicitCloudFallback
            )) {
                is VormexAiSuggestionsResult.Success -> {
                    _uiState.update {
                        it.copy(
                            aiSuggestions = result.suggestions,
                            isLoadingAiSuggestions = false,
                            aiStatusMessage = null,
                            aiNeedsPreparation = false,
                            aiCanUseCloudFallback = false
                        )
                    }
                }
                is VormexAiSuggestionsResult.NeedsDownload -> {
                    val availability = aiGateway.availability(VormexAiSurface.CHAT)
                    _uiState.update {
                        it.copy(
                            aiSuggestions = emptyList(),
                            isLoadingAiSuggestions = false,
                            aiStatusMessage = result.message,
                            aiNeedsPreparation = true,
                            aiCanUseCloudFallback = availability.cloudAllowed
                        )
                    }
                }
                is VormexAiSuggestionsResult.Blocked -> {
                    _uiState.update {
                        it.copy(
                            aiSuggestions = emptyList(),
                            isLoadingAiSuggestions = false,
                            aiStatusMessage = result.message,
                            aiNeedsPreparation = false,
                            aiCanUseCloudFallback = result.canUseCloudFallback
                        )
                    }
                }
                is VormexAiSuggestionsResult.Failure -> {
                    val availability = aiGateway.availability(VormexAiSurface.CHAT)
                    _uiState.update {
                        it.copy(
                            aiSuggestions = emptyList(),
                            isLoadingAiSuggestions = false,
                            aiStatusMessage = result.message,
                            aiNeedsPreparation = false,
                            aiCanUseCloudFallback = !explicitCloudFallback && availability.cloudAllowed
                        )
                    }
                }
            }
        }
    }

    fun prepareAiSuggestions() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isPreparingAiSuggestions = true,
                    aiStatusMessage = "Preparing on-device AI…",
                    aiNeedsPreparation = false,
                    aiCanUseCloudFallback = false
                )
            }
            when (val result = aiGateway.prepareOnDevice(
                surface = VormexAiSurface.CHAT,
                operation = VormexAiOperationKind.SMART_REPLIES
            )) {
                is VormexAiPrepareResult.Ready -> {
                    _uiState.update {
                        it.copy(
                            isPreparingAiSuggestions = false,
                            aiStatusMessage = "On-device AI is ready.",
                            aiNeedsPreparation = false,
                            aiCanUseCloudFallback = false
                        )
                    }
                    loadAiSuggestions(explicitCloudFallback = false)
                }
                is VormexAiPrepareResult.Failure -> {
                    val availability = aiGateway.availability(VormexAiSurface.CHAT)
                    _uiState.update {
                        it.copy(
                            isPreparingAiSuggestions = false,
                            aiStatusMessage = result.message,
                            aiNeedsPreparation = true,
                            aiCanUseCloudFallback = availability.cloudAllowed
                        )
                    }
                }
            }
        }
    }

    fun clearAiSuggestions() {
        _uiState.update {
            it.copy(
                aiSuggestions = emptyList(),
                aiStatusMessage = null,
                aiNeedsPreparation = false,
                aiCanUseCloudFallback = false
            )
        }
    }

    fun useAiSuggestion(suggestion: String) {
        sendMessage(suggestion)
        clearAiSuggestions()
    }

    private fun generateMockSuggestions(lastMessageContent: String?): List<String> {
        return when {
            lastMessageContent == null -> listOf("Hey! 👋", "How's it going?", "What's up?")
            lastMessageContent.contains("?", ignoreCase = true) -> listOf("Yes, sounds good!", "Let me check", "I'll get back to you")
            lastMessageContent.contains("thanks", ignoreCase = true) ||
                lastMessageContent.contains("thank", ignoreCase = true) -> listOf("You're welcome! 😊", "Anytime!", "Happy to help")
            lastMessageContent.contains("meet", ignoreCase = true) ||
                lastMessageContent.contains("call", ignoreCase = true) -> listOf("Sure, I'm free!", "What time works?", "Let's schedule it")
            lastMessageContent.contains("project", ignoreCase = true) ||
                lastMessageContent.contains("work", ignoreCase = true) -> listOf("I'll look into it", "Sounds interesting!", "Let's discuss more")
            else -> listOf("Got it! 👍", "Sounds good", "Let me know")
        }
    }

    private suspend fun ensureCurrentUserId(): String? {
        val userId = ApiClient.getCurrentUserId(context)
        val normalizedUserId = userId?.takeIf { it.isNotBlank() }
        val existing = _uiState.value.currentUserId

        if (existing != normalizedUserId) {
            resetForSession(normalizedUserId)
        }

        ChatSocketManager.currentUserId = normalizedUserId
        return normalizedUserId
    }

    private fun resetForSession(currentUserId: String?) {
        stopOutgoingTyping()
        clearTypingIndicator()
        listTypingTimeoutJobs.values.forEach { it.cancel() }
        listTypingTimeoutJobs.clear()
        _uiState.value.selectedConversation?.let { ChatSocketManager.leaveChat(it.id) }
        ChatSocketManager.activeConversationId = null
        hasLoadedConversations = false
        conversationsLastLoadedAt = 0L
        lastPreloadedUserId = null
        loadConversationsJob?.cancel()
        loadMessagesJob?.cancel()
        preloadJob?.cancel()
        _uiState.value = ChatUiState(
            currentUserId = currentUserId,
            socketConnected = ChatSocketManager.isConnected()
        )
        ChatSocketManager.currentUserId = currentUserId
        MessageNotificationManager.clearAll(context)
    }

    private fun Conversation.currentParticipantId(currentUserId: String?): String? {
        if (!currentUserId.isNullOrBlank() && (currentUserId == participant1Id || currentUserId == participant2Id)) {
            return currentUserId
        }

        return when (otherParticipant.id) {
            participant1Id -> participant2Id
            participant2Id -> participant1Id
            else -> currentUserId?.takeIf { it.isNotBlank() }
        }
    }

    private fun reconcileCurrentUserIdFromSentMessage(conversation: Conversation, message: Message) {
        val senderId = message.senderId.takeIf { it.isNotBlank() } ?: return
        if (senderId == conversation.otherParticipant.id) return
        if (senderId != conversation.participant1Id && senderId != conversation.participant2Id) return

        ChatSocketManager.currentUserId = senderId
        _uiState.update { state ->
            if (state.currentUserId == senderId) state else state.copy(currentUserId = senderId)
        }
    }

    private fun loadConversationsInternal(
        cacheOwnerId: String,
        prefetchRecentMessages: Boolean = false,
        forceRefresh: Boolean = false
    ) {
        if (loadConversationsJob?.isActive == true) return

        loadConversationsJob = viewModelScope.launch {
            hydrateCachedConversations(cacheOwnerId)

            val hasVisibleConversations = _uiState.value.conversations.isNotEmpty()
            val isFreshEnough =
                hasLoadedConversations &&
                    !forceRefresh &&
                    conversationsLastLoadedAt > 0L &&
                    System.currentTimeMillis() - conversationsLastLoadedAt < 30_000L

            if (isFreshEnough && hasVisibleConversations) {
                loadUnreadAndRequestsCount()
                return@launch
            }

            _uiState.update { it.copy(isLoadingConversations = !hasVisibleConversations, error = null) }
            chatCacheRepository.refreshConversations(cacheOwnerId = cacheOwnerId)
                .onSuccess { response ->
                    hasLoadedConversations = true
                    conversationsLastLoadedAt = System.currentTimeMillis()
                    _uiState.update { state ->
                        val refreshedSelectedConversation = state.selectedConversation?.let { selected ->
                            response.conversations.firstOrNull { it.id == selected.id } ?: selected
                        }
                        state.copy(
                            conversations = mergeRefreshedConversationsWithLocalState(
                                currentConversations = state.conversations,
                                refreshedConversations = response.conversations
                            ),
                            selectedConversation = refreshedSelectedConversation,
                            isLoadingConversations = false,
                            error = null
                        )
                    }
                    if (prefetchRecentMessages) {
                        runCatching {
                            chatCacheRepository.prefetchRecentMessages(
                                cacheOwnerId = cacheOwnerId,
                                conversations = response.conversations
                            )
                        }.onFailure { error ->
                            Log.w(TAG, "Failed to prefetch recent chat messages", error)
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            isLoadingConversations = false,
                            error = if (state.conversations.isEmpty()) error.message else state.error
                        )
                    }
                }
            loadUnreadAndRequestsCount()
            refreshGroupShortcuts()
        }
    }

    private fun refreshConversations() {
        refreshConversationsJob?.cancel()
        refreshConversationsJob = viewModelScope.launch {
            val now = System.currentTimeMillis()
            val elapsed = now - lastRefreshConversationsAt
            if (elapsed < REFRESH_CONVERSATIONS_MIN_INTERVAL_MS) {
                delay(REFRESH_CONVERSATIONS_DEBOUNCE_MS)
            }
            val currentUserId = ensureCurrentUserId() ?: return@launch
            chatCacheRepository.refreshConversations(cacheOwnerId = currentUserId)
                .onSuccess { response ->
                    hasLoadedConversations = true
                    conversationsLastLoadedAt = System.currentTimeMillis()
                    lastRefreshConversationsAt = System.currentTimeMillis()
                    _uiState.update { state ->
                        val refreshedSelectedConversation = state.selectedConversation?.let { selected ->
                            response.conversations.firstOrNull { it.id == selected.id } ?: selected
                        }
                        state.copy(
                            conversations = mergeRefreshedConversationsWithLocalState(
                                currentConversations = state.conversations,
                                refreshedConversations = response.conversations
                            ),
                            selectedConversation = refreshedSelectedConversation
                        )
                    }
                }
            loadUnreadAndRequestsCount()
            refreshGroupShortcuts()
        }
    }

    private suspend fun hydrateCachedConversations(cacheOwnerId: String?) {
        val userId = cacheOwnerId?.takeIf { it.isNotBlank() } ?: return
        val cachedConversations = runCatching {
            chatCacheRepository.getCachedConversations(userId)
        }.getOrElse { error ->
            Log.w(TAG, "Failed to hydrate cached conversations", error)
            emptyList()
        }
        if (cachedConversations.isEmpty()) return

        _uiState.update { state ->
            val refreshedSelectedConversation = state.selectedConversation?.let { selected ->
                cachedConversations.firstOrNull { it.id == selected.id } ?: selected
            }
            state.copy(
                conversations = cachedConversations,
                selectedConversation = refreshedSelectedConversation,
                isLoadingConversations = false,
                error = null
            )
        }
    }

    private suspend fun hydrateCachedMessages(
        cacheOwnerId: String,
        conversationId: String
    ): CachedMessagesSnapshot? {
        val snapshot = runCatching {
            chatCacheRepository.getCachedMessagesSnapshot(cacheOwnerId, conversationId)
        }.getOrElse { error ->
            Log.w(TAG, "Failed to hydrate cached messages", error)
            null
        } ?: return null

        _uiState.update { state ->
            if (state.selectedConversation?.id != conversationId) {
                state
            } else {
                state.copy(
                    messages = dedupeAndSortByCreatedAt(snapshot.messages),
                    isLoadingMessages = false,
                    hasMoreMessages = snapshot.hasMore,
                    messagesNextCursor = snapshot.nextCursor,
                    threadReadyState = resolveThreadReadyState(
                        messages = snapshot.messages,
                        fromCache = true
                    )
                )
            }
        }
        return snapshot
    }

    private fun resolveNextCursorAfterRefresh(
        state: ChatUiState,
        response: MessagesResponse,
        cursor: String?
    ): String? {
        if (cursor != null) return response.nextCursor

        val hasCachedOlderHistory =
            state.messages.size > response.messages.size &&
                !state.messagesNextCursor.isNullOrBlank()

        return if (hasCachedOlderHistory) state.messagesNextCursor else response.nextCursor
    }

    private fun resolveHasMoreAfterRefresh(
        state: ChatUiState,
        response: MessagesResponse,
        cursor: String?
    ): Boolean {
        if (cursor != null) return response.hasMore

        val hasCachedOlderHistory =
            state.messages.size > response.messages.size &&
                !state.messagesNextCursor.isNullOrBlank()

        return if (hasCachedOlderHistory) state.hasMoreMessages else response.hasMore
    }

    private suspend fun cacheCurrentMessages(conversationId: String) {
        val currentUserId = ensureCurrentUserId() ?: return
        val state = _uiState.value
        if (state.selectedConversation?.id != conversationId) return
        chatCacheRepository.cacheConversationMessagesSnapshot(
            cacheOwnerId = currentUserId,
            conversation = state.selectedConversation,
            messages = state.messages,
            nextCursor = state.messagesNextCursor,
            hasMore = state.hasMoreMessages
        )
    }

    private suspend fun persistSelectedConversationSnapshot() {
        val currentUserId = ensureCurrentUserId() ?: return
        val state = _uiState.value
        val conversation = state.selectedConversation ?: return
        chatCacheRepository.cacheConversationMessagesSnapshot(
            cacheOwnerId = currentUserId,
            conversation = conversation,
            messages = state.messages,
            nextCursor = state.messagesNextCursor,
            hasMore = state.hasMoreMessages
        )
    }

    private fun loadUnreadAndRequestsCount() {
        loadUnreadCountJob?.cancel()
        loadUnreadCountJob = viewModelScope.launch {
            delay(UNREAD_COUNT_DEBOUNCE_MS)
            ApiClient.getUnreadCount(context).onSuccess { count ->
                _uiState.update { it.copy(unreadCount = count) }
            }
            ApiClient.getMessageRequestsCount(context).onSuccess { count ->
                _uiState.update { it.copy(messageRequestsCount = count) }
            }
        }
    }

    private fun markConversationReadLocally(conversationId: String) {
        _uiState.update { state ->
            val currentUnread = state.conversations.firstOrNull { it.id == conversationId }?.unreadCount
                ?: state.selectedConversation?.takeIf { it.id == conversationId }?.unreadCount
                ?: 0
            state.copy(
                conversations = state.conversations.map { conversation ->
                    if (conversation.id == conversationId) {
                        conversation.copy(unreadCount = 0)
                    } else {
                        conversation
                    }
                },
                selectedConversation = state.selectedConversation?.let { selected ->
                    if (selected.id == conversationId) selected.copy(unreadCount = 0) else selected
                },
                unreadCount = (state.unreadCount - currentUnread).coerceAtLeast(0)
            )
        }
    }

    private fun upsertMessage(messages: List<Message>, message: Message): List<Message> {
        return dedupeAndSortByCreatedAt(messages.filterNot { it.id == message.id } + message)
    }

    private fun mergeRefreshedConversationsWithLocalState(
        currentConversations: List<Conversation>,
        refreshedConversations: List<Conversation>
    ): List<Conversation> {
        val refreshedIds = refreshedConversations.map { it.id }.toSet()
        return (refreshedConversations + currentConversations.filterNot { it.id in refreshedIds })
            .sortedByDescending { conversation ->
                conversation.lastMessageAt ?: conversation.updatedAt.ifBlank { conversation.createdAt }
            }
    }

    private fun applyConversationPreviewFromMessage(
        conversationId: String,
        message: Message,
        incrementUnread: Boolean
    ): Boolean {
        var foundConversation = false
        _uiState.update { state ->
            val updatedConversations = state.conversations.map { conversation ->
                if (conversation.id != conversationId) return@map conversation

                foundConversation = true
                val shouldIncrementUnread =
                    incrementUnread &&
                        message.senderId != state.currentUserId &&
                        state.selectedConversation?.id != conversationId

                conversation.copy(
                    lastMessage = message.toPreviewMessage(),
                    lastMessageAt = message.createdAt,
                    updatedAt = message.updatedAt,
                    unreadCount = if (shouldIncrementUnread) {
                        conversation.unreadCount + 1
                    } else if (state.selectedConversation?.id == conversationId) {
                        0
                    } else {
                        conversation.unreadCount
                    }
                )
            }

            val conversationsWithInstantInsert = if (foundConversation) {
                updatedConversations
            } else {
                buildInstantConversationFromMessage(
                    state = state,
                    conversationId = conversationId,
                    message = message,
                    incrementUnread = incrementUnread
                )?.let { instantConversation ->
                    listOf(instantConversation) + updatedConversations
                } ?: updatedConversations
            }

            state.copy(
                conversations = conversationsWithInstantInsert.sortedByDescending { conversation ->
                    conversation.lastMessageAt ?: conversation.updatedAt.ifBlank { conversation.createdAt }
                },
                unreadCount = if (
                    incrementUnread &&
                    message.senderId != state.currentUserId &&
                    state.selectedConversation?.id != conversationId
                ) {
                    state.unreadCount + 1
                } else {
                    state.unreadCount
                }
            )
        }

        return foundConversation
    }

    private fun buildInstantConversationFromMessage(
        state: ChatUiState,
        conversationId: String,
        message: Message,
        incrementUnread: Boolean
    ): Conversation? {
        val currentUserId = state.currentUserId?.takeIf { it.isNotBlank() } ?: return null
        val sender = message.sender ?: ChatUser(id = message.senderId)
        val otherParticipant = if (message.senderId == currentUserId) {
            ChatUser(id = message.receiverId)
        } else {
            sender
        }
        val participant1Id = if (message.senderId == currentUserId) {
            message.senderId
        } else {
            message.receiverId.takeIf { it == currentUserId } ?: currentUserId
        }
        val participant2Id = if (participant1Id == message.senderId) {
            message.receiverId
        } else {
            message.senderId
        }
        val shouldIncrementUnread =
            incrementUnread &&
                message.senderId != currentUserId &&
                state.selectedConversation?.id != conversationId

        return Conversation(
            id = conversationId,
            participant1Id = participant1Id,
            participant2Id = participant2Id,
            participant1 = if (participant1Id == sender.id) sender else ChatUser(id = participant1Id),
            participant2 = if (participant2Id == sender.id) sender else ChatUser(id = participant2Id),
            otherParticipant = otherParticipant,
            lastMessage = message.toPreviewMessage(),
            lastMessageAt = message.createdAt,
            unreadCount = if (shouldIncrementUnread) 1 else 0,
            createdAt = message.createdAt,
            updatedAt = message.updatedAt
        )
    }

    private fun Message.toPreviewMessage(): ConversationLastMessage {
        return ConversationLastMessage(
            id = id,
            content = content,
            contentType = contentType,
            senderId = senderId,
            status = status,
            createdAt = createdAt
        )
    }

    private fun scheduleTypingIndicatorClear(conversationId: String, userId: String) {
        typingIndicatorTimeoutJob?.cancel()
        typingIndicatorTimeoutJob = viewModelScope.launch {
            delay(1_500)
            _uiState.update { state ->
                if (state.selectedConversation?.id != conversationId || state.typingUserId != userId) {
                    state
                } else {
                    state.copy(typingUserId = null)
                }
            }
        }
    }

    private fun clearTypingIndicator() {
        typingIndicatorTimeoutJob?.cancel()
        typingIndicatorTimeoutJob = null
    }

    private fun updateListTypingState(conversationId: String, isTyping: Boolean) {
        listTypingTimeoutJobs.remove(conversationId)?.cancel()
        _uiState.update { state ->
            val updatedTypingIds = if (isTyping) {
                state.typingConversationIds + conversationId
            } else {
                state.typingConversationIds - conversationId
            }
            if (updatedTypingIds == state.typingConversationIds) {
                state
            } else {
                state.copy(typingConversationIds = updatedTypingIds)
            }
        }
        if (isTyping) {
            listTypingTimeoutJobs[conversationId] = viewModelScope.launch {
                delay(LIST_TYPING_TIMEOUT_MS)
                listTypingTimeoutJobs.remove(conversationId)
                _uiState.update { state ->
                    if (conversationId in state.typingConversationIds) {
                        state.copy(typingConversationIds = state.typingConversationIds - conversationId)
                    } else {
                        state
                    }
                }
            }
        }
    }

    private fun clearListTypingState() {
        listTypingTimeoutJobs.values.forEach { it.cancel() }
        listTypingTimeoutJobs.clear()
        _uiState.update { state ->
            if (state.typingConversationIds.isEmpty()) {
                state
            } else {
                state.copy(typingConversationIds = emptySet())
            }
        }
    }

    private fun stopOutgoingTyping(conversationId: String? = _uiState.value.selectedConversation?.id) {
        outgoingTypingStopJob?.cancel()
        outgoingTypingStopJob = null

        if (isOutgoingTyping && !conversationId.isNullOrBlank()) {
            ChatSocketManager.sendTyping(conversationId, false)
        }

        isOutgoingTyping = false
        outgoingTypingConversationId = null
        lastOutgoingTypingSentAt = 0L
    }

    private fun resolveThreadReadyState(
        messages: List<Message>,
        fromCache: Boolean
    ): ChatThreadReadyState {
        if (messages.isEmpty()) return ChatThreadReadyState.EMPTY_THREAD
        return if (fromCache) {
            ChatThreadReadyState.CACHED_READY
        } else {
            ChatThreadReadyState.MESSAGES_READY
        }
    }

    private fun replaceMatchingPendingMessage(messages: List<Message>, serverMessage: Message): List<Message> {
        val pending = serverMessage.clientMessageId
            ?.takeIf { it.isNotBlank() }
            ?.let { clientMessageId -> messages.firstOrNull { it.id == clientMessageId } }
            ?: messages.firstOrNull {
                (it.id.startsWith("local-") || it.id.startsWith("pending-")) &&
                it.senderId == serverMessage.senderId &&
                it.conversationId == serverMessage.conversationId &&
                it.content == serverMessage.content
            }
            ?: return messages
        return dedupeAndSortByCreatedAt(messages.filterNot { it.id == pending.id } + serverMessage)
    }

    private fun mergeRefreshedMessagesWithLocalState(
        currentMessages: List<Message>,
        refreshedMessages: List<Message>
    ): List<Message> {
        val refreshedIds = refreshedMessages.map { it.id }.toSet()
        val refreshedClientIds = refreshedMessages
            .mapNotNull { it.clientMessageId?.takeIf(String::isNotBlank) }
            .toSet()
        val localMessagesInFlight = currentMessages.filter { message ->
            val clientMessageId = message.clientMessageId
            val isLocalMessage =
                message.id.startsWith("local-") ||
                    message.id.startsWith("pending-") ||
                    clientMessageId?.startsWith("local-") == true ||
                    clientMessageId?.startsWith("pending-") == true
            val isPendingOrFailed =
                message.status.equals("SENDING", ignoreCase = true) ||
                    message.status.equals("FAILED", ignoreCase = true)

            isLocalMessage &&
                isPendingOrFailed &&
                message.id !in refreshedIds &&
                clientMessageId?.let { it !in refreshedClientIds } != false
        }

        if (refreshedMessages.isEmpty()) {
            return dedupeAndSortByCreatedAt(localMessagesInFlight)
        }

        val oldestRefreshedCreatedAt = refreshedMessages.first().createdAt
        val preservedOlderCachedMessages = currentMessages.filter { message ->
            val clientMessageId = message.clientMessageId
            val isLocalInFlight = localMessagesInFlight.any { it.id == message.id }
            !isLocalInFlight &&
                message.id !in refreshedIds &&
                clientMessageId?.let { it !in refreshedClientIds } != false &&
                message.createdAt < oldestRefreshedCreatedAt
        }

        return dedupeAndSortByCreatedAt(
            preservedOlderCachedMessages + refreshedMessages + localMessagesInFlight
        )
    }

    private fun dedupeAndSortByCreatedAt(messages: List<Message>): List<Message> {
        return messages
            .groupBy { it.id }
            .map { (_, grouped) -> grouped.last() }
            .sortedBy { it.createdAt }
    }

    override fun onCleared() {
        stopOutgoingTyping()
        readReceiptJobs.values.forEach { it.cancel() }
        readReceiptJobs.clear()
        listTypingTimeoutJobs.values.forEach { it.cancel() }
        listTypingTimeoutJobs.clear()
        _uiState.value.selectedConversation?.let { ChatSocketManager.leaveChat(it.id) }
        clearTypingIndicator()
        super.onCleared()
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(context.applicationContext) as T
        }
    }
}
