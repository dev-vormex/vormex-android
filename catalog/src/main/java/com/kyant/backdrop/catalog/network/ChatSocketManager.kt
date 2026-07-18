package com.kyant.backdrop.catalog.network

import android.util.Log
import com.kyant.backdrop.catalog.BuildConfig
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Manager
import io.socket.client.Socket
import io.socket.engineio.client.Transport
import io.socket.engineio.client.transports.Polling
import io.socket.engineio.client.transports.WebSocket
import io.socket.emitter.Emitter
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.net.URI
import kotlin.coroutines.resume

/**
 * Manages Socket.IO connection for real-time chat with vormex-backend.
 * 
 * Backend expects: handshake.auth.token (JWT), and events:
 * - chat:join, chat:leave (room management)
 * - chat:send_message, chat:new_message, chat:notification  
 * - chat:typing, chat:user_typing
 * - chat:mark_read, chat:messages_read
 * - chat:delete_message, chat:message_deleted
 * - chat:edit_message, chat:message_edited
 * - chat:message_reaction
 * - chat:cleared
 *
 * Configuration:
 * - For local dev with `adb reverse tcp:5000 tcp:5000`: use "http://localhost:5000"
 * - For Android emulator without adb reverse: use "http://10.0.2.2:5000"
 * - For production: use your production WebSocket URL (e.g., "https://api.vormex.com")
 */
object ChatSocketManager {
    private const val TAG = "ChatSocket"
    private const val RECENT_MESSAGE_DEDUPE_LIMIT = 200

    private val SOCKET_URL: String
        get() = BuildConfig.SOCKET_BASE_URL

    private var socket: Socket? = null
    private var currentToken: String? = null
    private var isConnecting = false
    private var isAuthenticated = false
    var currentUserId: String? = null
    private var currentTransportName: String? = null
    private const val SEND_ACK_TIMEOUT_MS = 3_000L
    
    // Track joined rooms to re-join on reconnect
    private val joinedRooms = mutableSetOf<String>()
    private val recentlyProcessedMessageIds = ArrayDeque<String>()
    private val recentMessageIdSet = mutableSetOf<String>()
    
    // Notification callback for showing local notifications
    private var notificationCallback: ((title: String, body: String, data: Map<String, String>) -> Unit)? = null
    
    // Track current active conversation to avoid notifying for messages the user is viewing
    var activeConversationId: String? = null

    /** Emits (conversationId, messageJsonString) for new messages */
    private val _newMessageFlow = MutableSharedFlow<Pair<String, String>>(replay = 0, extraBufferCapacity = 64)
    val newMessageFlow = _newMessageFlow.asSharedFlow()

    private val _typingFlow = MutableSharedFlow<Triple<String, String, Boolean>>(replay = 0, extraBufferCapacity = 5)
    val typingFlow = _typingFlow.asSharedFlow()

    private val _messagesReadFlow = MutableSharedFlow<Pair<String, String>>(replay = 0, extraBufferCapacity = 5)
    val messagesReadFlow = _messagesReadFlow.asSharedFlow()
    
    /**
     * Set callback for showing local notifications
     */
    fun setNotificationCallback(callback: (title: String, body: String, data: Map<String, String>) -> Unit) {
        notificationCallback = callback
    }

    private val _messageDeletedFlow = MutableSharedFlow<Triple<String, String, Boolean>>(replay = 0, extraBufferCapacity = 5)
    val messageDeletedFlow = _messageDeletedFlow.asSharedFlow()

    private val _messageEditedFlow = MutableSharedFlow<Triple<String, String, String>>(replay = 0, extraBufferCapacity = 5)
    val messageEditedFlow = _messageEditedFlow.asSharedFlow()

    private val _reactionFlow = MutableSharedFlow<ReactionEvent>(replay = 0, extraBufferCapacity = 5)
    val reactionFlow = _reactionFlow.asSharedFlow()

    private val _messagesDeliveredFlow = MutableSharedFlow<MessagesDeliveredEvent>(replay = 0, extraBufferCapacity = 8)
    val messagesDeliveredFlow = _messagesDeliveredFlow.asSharedFlow()

    private val _messageDeliveredFlow = MutableSharedFlow<MessageDeliveredEvent>(replay = 0, extraBufferCapacity = 16)
    val messageDeliveredFlow = _messageDeliveredFlow.asSharedFlow()

    private val _presenceFlow = MutableSharedFlow<PresenceEvent>(replay = 0, extraBufferCapacity = 16)
    val presenceFlow = _presenceFlow.asSharedFlow()

    private val _allChatsClearedFlow = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 2)
    val allChatsClearedFlow = _allChatsClearedFlow.asSharedFlow()
    
    /** Connection state flow for UI feedback */
    private val _connectionStateFlow = MutableSharedFlow<ConnectionState>(replay = 1, extraBufferCapacity = 1)
    val connectionStateFlow = _connectionStateFlow.asSharedFlow()
    
    enum class ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, ERROR
    }

    data class ReactionEvent(
        val messageId: String,
        val conversationId: String,
        val userId: String,
        val emoji: String,
        val action: String
    )

    data class MessagesDeliveredEvent(
        val conversationId: String,
        val deliveredTo: String,
        val deliveredAt: String
    )

    data class MessageDeliveredEvent(
        val messageId: String,
        val conversationId: String,
        val deliveredAt: String
    )

    data class PresenceEvent(
        val userId: String,
        val isOnline: Boolean,
        val lastActiveAt: String?
    )

    fun isConnected(): Boolean = socket?.connected() == true && isAuthenticated
    
    fun getConnectionState(): ConnectionState = when {
        socket?.connected() == true && isAuthenticated -> ConnectionState.CONNECTED
        isConnecting || socket?.connected() == true -> ConnectionState.CONNECTING
        else -> ConnectionState.DISCONNECTED
    }

    fun connect(token: String) {
        if (token.isBlank()) return

        if (socket?.connected() == true && currentToken == token && isAuthenticated) {
            Log.d(TAG, "Already connected with same token, skipping")
            return
        }
        if (isConnecting && currentToken == token) {
            Log.d(TAG, "Already connecting with same token, skipping")
            return
        }
        if (socket != null && currentToken == token && socket?.connected() != true) {
            Log.d(TAG, "Re-opening existing socket with same token")
            isConnecting = true
            _connectionStateFlow.tryEmit(ConnectionState.CONNECTING)
            socket?.connect()
            return
        }
        
        Log.d(TAG, "Connecting to socket at $SOCKET_URL")
        disconnect()
        currentToken = token
        isConnecting = true
        isAuthenticated = false
        _connectionStateFlow.tryEmit(ConnectionState.CONNECTING)
        
        try {
            val opts = IO.Options().apply {
                forceNew = true
                reconnection = true
                reconnectionAttempts = 10
                reconnectionDelay = 1000
                reconnectionDelayMax = 5000
                timeout = 20000
                // Prefer WebSocket for latency, with polling available on
                // restrictive mobile/campus networks that block an upgrade.
                transports = arrayOf(WebSocket.NAME, Polling.NAME)
                upgrade = true
                rememberUpgrade = true
                auth = mapOf("token" to token)
            }
            socket = IO.socket(URI.create(SOCKET_URL), opts).apply {
                val manager = io()
                manager.on(Manager.EVENT_TRANSPORT) { args ->
                    val transport = args.firstOrNull() as? Transport
                    currentTransportName = transport?.name
                    Log.d(TAG, "🚇 Socket transport=${currentTransportName ?: "unknown"} socketId=${id().orEmpty()}")
                }
                on(Socket.EVENT_CONNECT) {
                    Log.d(TAG, "✅ Socket connected! ID: ${id()} transport=${currentTransportName ?: "unknown"}")
                    _connectionStateFlow.tryEmit(ConnectionState.CONNECTING)
                }
                on("socket:authenticated") { args ->
                    isConnecting = false
                    isAuthenticated = true
                    parseSocketObject(args.firstOrNull())
                        ?.optString("userId")
                        ?.takeIf { it.isNotBlank() }
                        ?.let { authenticatedUserId ->
                            currentUserId = authenticatedUserId
                        }
                    Log.d(TAG, "✅ Socket authenticated as user ${currentUserId.orEmpty()} transport=${currentTransportName ?: "unknown"}")
                    _connectionStateFlow.tryEmit(ConnectionState.CONNECTED)
                    rejoinAllRooms()
                }
                on("socket:unauthenticated") { args ->
                    isConnecting = false
                    isAuthenticated = false
                    val message = parseSocketObject(args.firstOrNull())
                        ?.optString("message")
                        ?.takeIf { it.isNotBlank() }
                        ?: "Socket authentication failed"
                    Log.e(TAG, "🔴 $message")
                    _connectionStateFlow.tryEmit(ConnectionState.ERROR)
                }
                on(Socket.EVENT_DISCONNECT) { args ->
                    isAuthenticated = false
                    Log.d(TAG, "❌ Socket disconnected: ${args.getOrNull(0)}")
                    _connectionStateFlow.tryEmit(ConnectionState.DISCONNECTED)
                }
                on(Socket.EVENT_CONNECT_ERROR) { args ->
                    isConnecting = false
                    isAuthenticated = false
                    val error = args.getOrNull(0)?.toString() ?: "Unknown error"
                    Log.e(TAG, "🔴 Socket connect error: $error")
                    _connectionStateFlow.tryEmit(ConnectionState.ERROR)
                }
                manager.on(Manager.EVENT_RECONNECT) { args ->
                    Log.d(TAG, "🔄 Socket reconnected: attempt ${args.getOrNull(0)}")
                    isAuthenticated = false
                    _connectionStateFlow.tryEmit(ConnectionState.CONNECTING)
                }
                manager.on(Manager.EVENT_RECONNECT_ATTEMPT) { args ->
                    isConnecting = true
                    Log.d(TAG, "🔄 Socket reconnecting... attempt ${args.getOrNull(0)}")
                    _connectionStateFlow.tryEmit(ConnectionState.CONNECTING)
                }
                manager.on(Manager.EVENT_RECONNECT_FAILED) {
                    isConnecting = false
                    Log.e(TAG, "🔴 Socket reconnection failed after all attempts")
                    _connectionStateFlow.tryEmit(ConnectionState.ERROR)
                }
                
                // Chat events
                on("chat:new_message") { args ->
                    Log.d(TAG, "📩 Received chat:new_message")
                    handleNewMessage(args)
                }
                on("chat:notification") { args ->
                    Log.d(TAG, "🔔 Received chat:notification")
                    handleNotification(args)
                }
                on("chat:user_typing") { args ->
                    handleTyping(args)
                }
                on("chat:messages_read") { args ->
                    handleMessagesRead(args)
                }
                on("chat:messages_delivered") { args ->
                    handleMessagesDelivered(args)
                }
                on("chat:message_delivered") { args ->
                    handleMessageDelivered(args)
                }
                on("user:online") { args ->
                    handlePresence(args, "user:online", fallbackIsOnline = true)
                }
                on("user:offline") { args ->
                    handlePresence(args, "user:offline", fallbackIsOnline = false)
                }
                on("user:status") { args ->
                    handlePresence(args, "user:status", fallbackIsOnline = false)
                }
                on("chat:message_deleted") { args ->
                    handleMessageDeleted(args)
                }
                on("chat:message_edited") { args ->
                    handleMessageEdited(args)
                }
                on("chat:message_reaction") { args ->
                    handleReaction(args)
                }
                on("chat:cleared") {
                    handleAllChatsCleared()
                }
                
                if (BuildConfig.DEBUG) {
                    onAnyIncoming { args ->
                        Log.d(TAG, "🔵 ANY EVENT: ${args.contentToString()}")
                    }
                }
            }
            socket?.connect()
            Log.d(TAG, "Socket.connect() called")
        } catch (e: Exception) {
            isConnecting = false
            Log.e(TAG, "Socket init error", e)
            _connectionStateFlow.tryEmit(ConnectionState.ERROR)
        }
    }
    
    private fun handleNewMessage(args: Array<Any>) {
        Log.d(TAG, "📩 handleNewMessage called with ${args.size} args")
        if (args.isEmpty()) {
            Log.w(TAG, "📩 handleNewMessage: args is empty!")
            return
        }
        try {
            val rawArg = args[0]
            Log.d(TAG, "📩 Raw arg type: ${rawArg::class.java.simpleName}, value: $rawArg")
            val obj = parseSocketObject(rawArg) ?: return
            val conversationId = obj.optString("conversationId")
            val message = obj.optJSONObject("message")
            if (message == null) {
                Log.w(TAG, "📩 message object is null in: $obj")
                return
            }
            val serverEmittedAtMs = obj.optLong("serverEmittedAtMs", 0L)
            val latencyMs = if (serverEmittedAtMs > 0L) System.currentTimeMillis() - serverEmittedAtMs else null
            Log.d(
                TAG,
                "📩 chat:new_message received conversation=$conversationId message=${message.optString("id")} " +
                    "clientMessageId=${message.optString("clientMessageId")} transport=${currentTransportName ?: "unknown"} " +
                    "latencyMs=${latencyMs ?: "unknown"} content=${message.optString("content").take(50)}"
            )
            if (conversationId.isNotEmpty()) {
                emitIncomingMessage(conversationId, message, "event")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling new message", e)
        }
    }

    private fun parseSocketObject(rawArg: Any?): JSONObject? {
        return when (rawArg) {
            is JSONObject -> rawArg
            null -> null
            else -> runCatching { JSONObject(rawArg.toString()) }
                .onFailure { Log.w(TAG, "Could not parse socket payload: $rawArg", it) }
                .getOrNull()
        }
    }

    private fun emitIncomingMessage(conversationId: String, message: JSONObject, source: String) {
        val messageId = message.optString("id")
        if (messageId.isNotBlank() && !rememberIncomingMessageId(messageId)) {
            Log.d(TAG, "📩 Ignoring duplicate incoming message $messageId from $source")
            return
        }

        val emitted = _newMessageFlow.tryEmit(conversationId to message.toString())
        Log.d(TAG, "📩 Flow emit result ($source): $emitted")
        acknowledgeMessageDelivery(conversationId, messageId, message)
        showMessageNotificationIfNeeded(conversationId, message)
    }

    private fun acknowledgeMessageDelivery(conversationId: String, messageId: String, message: JSONObject) {
        val userId = currentUserId?.takeIf { it.isNotBlank() } ?: return
        val senderId = message.optJSONObject("sender")?.optString("id")?.takeIf { it.isNotBlank() }
            ?: message.optString("senderId")
        val receiverId = message.optString("receiverId")
        val targetsCurrentUser = if (receiverId.isNotBlank()) {
            receiverId == userId
        } else {
            senderId.isNotBlank() && senderId != userId
        }
        if (!targetsCurrentUser) return

        if (socket?.connected() != true) {
            Log.d(TAG, "📬 Skipping chat:delivered ack for $messageId - socket not connected")
            return
        }

        val payload = JSONObject().put("conversationId", conversationId)
        messageId.takeIf { it.isNotBlank() }?.let { payload.put("messageId", it) }
        socket?.emit("chat:delivered", payload)
        Log.d(TAG, "📬 Emitted chat:delivered conversation=$conversationId message=$messageId")
    }

    fun emitExternalIncomingMessage(
        conversationId: String,
        messageJson: String,
        source: String = "external"
    ) {
        if (conversationId.isBlank() || messageJson.isBlank()) return
        val message = parseSocketObject(messageJson) ?: return
        emitIncomingMessage(conversationId, message, source)
    }

    private fun rememberIncomingMessageId(messageId: String): Boolean {
        synchronized(recentMessageIdSet) {
            if (!recentMessageIdSet.add(messageId)) {
                return false
            }

            recentlyProcessedMessageIds.addLast(messageId)
            while (recentlyProcessedMessageIds.size > RECENT_MESSAGE_DEDUPE_LIMIT) {
                val removed = recentlyProcessedMessageIds.removeFirst()
                recentMessageIdSet.remove(removed)
            }

            return true
        }
    }
    
    private fun showMessageNotificationIfNeeded(conversationId: String, message: JSONObject) {
        if (activeConversationId == conversationId) {
            Log.d(TAG, "🔕 Skipping notification - user is viewing conversation $conversationId")
            return
        }

        val sender = message.optJSONObject("sender")
        val senderName = sender?.optString("name")?.takeIf { it.isNotBlank() }
            ?: sender?.optString("username")?.takeIf { it.isNotBlank() }
            ?: "Someone"
        val senderId = sender?.optString("id")?.takeIf { it.isNotBlank() }
            ?: message.optString("senderId")
        if (!currentUserId.isNullOrBlank() && senderId == currentUserId) {
            Log.d(TAG, "🔕 Skipping notification for self-sent message $conversationId")
            return
        }
        val senderImage = sender?.optString("profileImage")?.takeIf { it.isNotBlank() } ?: ""

        val contentType = message.optString("contentType", "text")
        val displayContent = when (contentType) {
            "image" -> "\uD83D\uDCF7 Sent a photo"
            "video" -> "\uD83C\uDFA5 Sent a video"
            "file" -> "\uD83D\uDCCE Sent a file"
            "audio" -> "\uD83C\uDFB5 Sent a voice message"
            else -> message.optString("content", "Sent you a message")
        }

        Log.d(TAG, "🔔 Showing notification for message from $senderName (image: ${senderImage.take(30)})")

        notificationCallback?.invoke(
            senderName,
            displayContent,
            mapOf(
                "type" to "message",
                "conversationId" to conversationId,
                "user_id" to senderId,
                "senderImage" to senderImage
            )
        )
    }
    
    private fun handleNotification(args: Array<Any>) {
        Log.d(TAG, "🔔 handleNotification called with ${args.size} args")
        if (args.isEmpty()) {
            Log.w(TAG, "🔔 handleNotification: args is empty!")
            return
        }
        try {
            val rawArg = args[0]
            Log.d(TAG, "🔔 Raw arg type: ${rawArg::class.java.simpleName}")
            val obj = parseSocketObject(rawArg) ?: return

            Log.d(TAG, "🔔 Notification type: ${obj.optString("type")}")
            if (obj.optString("type") == "new_message") {
                val conversationId = obj.optString("conversationId")
                val message = obj.optJSONObject("message")
                if (message == null) {
                    Log.w(TAG, "🔔 message is null in notification")
                    return
                }
                Log.d(TAG, "💬 Message notification for conversation $conversationId")
                if (conversationId.isNotEmpty()) {
                    // Merge top-level sender info into message if message.sender is missing
                    if (!message.has("sender") || message.isNull("sender")) {
                        obj.optJSONObject("sender")?.let { message.put("sender", it) }
                    }
                    emitIncomingMessage(conversationId, message, "notification")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling notification", e)
        }
    }
    
    private fun handleTyping(args: Array<Any>) {
        if (args.isEmpty()) return
        try {
            val obj = parseSocketObject(args[0]) ?: return
            val serverEmittedAtMs = obj.optLong("serverEmittedAtMs", 0L)
            val latencyMs = if (serverEmittedAtMs > 0L) System.currentTimeMillis() - serverEmittedAtMs else null
            Log.d(
                TAG,
                "⌨️ chat:user_typing received conversation=${obj.optString("conversationId")} " +
                    "user=${obj.optString("userId")} isTyping=${obj.optBoolean("isTyping")} " +
                    "transport=${currentTransportName ?: "unknown"} latencyMs=${latencyMs ?: "unknown"}"
            )
            _typingFlow.tryEmit(
                Triple(
                    obj.optString("conversationId"),
                    obj.optString("userId"),
                    obj.optBoolean("isTyping")
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error handling typing", e)
        }
    }
    
    private fun handleMessagesRead(args: Array<Any>) {
        if (args.isEmpty()) return
        try {
            val obj = parseSocketObject(args[0]) ?: return
            _messagesReadFlow.tryEmit(
                obj.optString("conversationId") to obj.optString("readBy")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error handling messages read", e)
        }
    }
    
    private fun handleMessagesDelivered(args: Array<Any>) {
        if (args.isEmpty()) return
        try {
            val obj = parseSocketObject(args[0]) ?: return
            Log.d(
                TAG,
                "📬 chat:messages_delivered conversation=${obj.optString("conversationId")} " +
                    "deliveredTo=${obj.optString("deliveredTo")} transport=${currentTransportName ?: "unknown"}"
            )
            _messagesDeliveredFlow.tryEmit(
                MessagesDeliveredEvent(
                    conversationId = obj.optString("conversationId"),
                    deliveredTo = obj.optString("deliveredTo"),
                    deliveredAt = obj.optString("deliveredAt")
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error handling messages delivered", e)
        }
    }

    private fun handleMessageDelivered(args: Array<Any>) {
        if (args.isEmpty()) return
        try {
            val obj = parseSocketObject(args[0]) ?: return
            Log.d(
                TAG,
                "📬 chat:message_delivered message=${obj.optString("messageId")} " +
                    "conversation=${obj.optString("conversationId")}"
            )
            _messageDeliveredFlow.tryEmit(
                MessageDeliveredEvent(
                    messageId = obj.optString("messageId"),
                    conversationId = obj.optString("conversationId"),
                    deliveredAt = obj.optString("deliveredAt")
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error handling message delivered", e)
        }
    }

    private fun handlePresence(args: Array<Any>, source: String, fallbackIsOnline: Boolean) {
        if (args.isEmpty()) return
        try {
            val obj = parseSocketObject(args[0]) ?: return
            val userId = obj.optString("userId")
            if (userId.isBlank()) return
            val isOnline = obj.optBoolean("isOnline", fallbackIsOnline)
            val lastActiveAt = obj.optString("lastActiveAt")
                .takeIf { it.isNotBlank() && it != "null" }
            Log.d(TAG, "🟢 $source user=$userId isOnline=$isOnline lastActiveAt=${lastActiveAt ?: "unknown"}")
            _presenceFlow.tryEmit(
                PresenceEvent(
                    userId = userId,
                    isOnline = isOnline,
                    lastActiveAt = lastActiveAt
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error handling presence event ($source)", e)
        }
    }

    private fun handleMessageDeleted(args: Array<Any>) {
        if (args.isEmpty()) return
        try {
            val obj = parseSocketObject(args[0]) ?: return
            _messageDeletedFlow.tryEmit(
                Triple(
                    obj.optString("messageId"),
                    obj.optString("conversationId"),
                    obj.optBoolean("forEveryone")
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error handling message deleted", e)
        }
    }
    
    private fun handleMessageEdited(args: Array<Any>) {
        if (args.isEmpty()) return
        try {
            val obj = parseSocketObject(args[0]) ?: return
            _messageEditedFlow.tryEmit(
                Triple(
                    obj.optString("messageId"),
                    obj.optString("conversationId"),
                    obj.optString("content")
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error handling message edited", e)
        }
    }
    
    private fun handleReaction(args: Array<Any>) {
        if (args.isEmpty()) return
        try {
            val obj = parseSocketObject(args[0]) ?: return
            _reactionFlow.tryEmit(
                ReactionEvent(
                    messageId = obj.optString("messageId"),
                    conversationId = obj.optString("conversationId"),
                    userId = obj.optString("userId"),
                    emoji = obj.optString("emoji"),
                    action = obj.optString("action")
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error handling reaction", e)
        }
    }

    private fun handleAllChatsCleared() {
        Log.d(TAG, "Received chat:cleared")
        joinedRooms.toList().forEach { conversationId ->
            socket?.emit("chat:leave", JSONObject().put("conversationId", conversationId))
        }
        joinedRooms.clear()
        activeConversationId = null
        synchronized(recentMessageIdSet) {
            recentlyProcessedMessageIds.clear()
            recentMessageIdSet.clear()
        }
        _allChatsClearedFlow.tryEmit(Unit)
    }

    fun disconnect() {
        Log.d(TAG, "Disconnecting socket")
        socket?.off()
        socket?.io()?.off()
        socket?.disconnect()
        socket = null
        currentToken = null
        isConnecting = false
        isAuthenticated = false
        currentTransportName = null
        _connectionStateFlow.tryEmit(ConnectionState.DISCONNECTED)
    }

    fun resetSession() {
        joinedRooms.clear()
        activeConversationId = null
        currentUserId = null
        synchronized(recentMessageIdSet) {
            recentlyProcessedMessageIds.clear()
            recentMessageIdSet.clear()
        }
        disconnect()
    }
    
    /** Reconnect with the stored token (call on app resume) */
    fun reconnectIfNeeded() {
        val token = currentToken
        if (token != null && (socket?.connected() != true || !isAuthenticated) && !isConnecting) {
            Log.d(TAG, "Reconnecting socket... transport=${currentTransportName ?: "unknown"}")
            connect(token)
        }
    }

    fun joinChat(conversationId: String) {
        Log.d(TAG, "➡️ Joining chat room: $conversationId, socket ready: ${isConnected()}")
        joinedRooms.add(conversationId)
        if (isConnected()) {
            socket?.emit("chat:join", JSONObject().put("conversationId", conversationId))
            Log.d(TAG, "➡️ Emitted chat:join for room: $conversationId")
        } else {
            reconnectIfNeeded()
            Log.d(TAG, "➡️ Socket not connected, room $conversationId will be joined on connect")
        }
    }

    fun leaveChat(conversationId: String) {
        Log.d(TAG, "⬅️ Leaving chat room: $conversationId")
        joinedRooms.remove(conversationId)
        if (socket?.connected() == true) {
            socket?.emit("chat:leave", JSONObject().put("conversationId", conversationId))
        }
    }
    
    private fun rejoinAllRooms() {
        if (joinedRooms.isNotEmpty()) {
            Log.d(TAG, "🔄 Re-joining ${joinedRooms.size} rooms: $joinedRooms")
            joinedRooms.forEach { conversationId ->
                socket?.emit("chat:join", JSONObject().put("conversationId", conversationId))
                Log.d(TAG, "🔄 Re-joined room: $conversationId")
            }
        }
    }

    private fun buildSendMessagePayload(
        conversationId: String,
        content: String,
        contentType: String = "text",
        mediaUrl: String? = null,
        mediaType: String? = null,
        fileName: String? = null,
        fileSize: Int? = null,
        replyToId: String? = null,
        clientMessageId: String? = null
    ): JSONObject {
        return JSONObject()
            .put("conversationId", conversationId)
            .put("content", content)
            .put("contentType", contentType)
            .apply {
                mediaUrl?.takeIf { it.isNotBlank() }?.let { put("mediaUrl", it) }
                mediaType?.takeIf { it.isNotBlank() }?.let { put("mediaType", it) }
                fileName?.takeIf { it.isNotBlank() }?.let { put("fileName", it) }
                fileSize?.let { put("fileSize", it) }
                replyToId?.takeIf { it.isNotBlank() }?.let { put("replyToId", it) }
                clientMessageId?.takeIf { it.isNotBlank() }?.let { put("clientMessageId", it) }
            }
    }

    suspend fun sendMessageWithAck(
        conversationId: String,
        content: String,
        contentType: String = "text",
        mediaUrl: String? = null,
        mediaType: String? = null,
        fileName: String? = null,
        fileSize: Int? = null,
        replyToId: String? = null,
        clientMessageId: String? = null
    ): Result<String> {
        val activeSocket = socket ?: return Result.failure(Exception("Realtime connection unavailable"))
        if (!activeSocket.connected() || !isAuthenticated) {
            return Result.failure(Exception("Realtime connection unavailable"))
        }

        val payload = buildSendMessagePayload(
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

        return try {
            withTimeout(SEND_ACK_TIMEOUT_MS) {
                suspendCancellableCoroutine<Result<String>> { continuation ->
                    activeSocket.emit("chat:send_message", payload, Ack { args ->
                        if (!continuation.isActive) return@Ack

                        val ack = parseSocketObject(args.firstOrNull())
                        val message = ack?.optJSONObject("message")
                        val result = if (ack?.optBoolean("ok") == true && message != null) {
                            Result.success(message.toString())
                        } else {
                            Result.failure(Exception(ack?.optString("error")?.takeIf { it.isNotBlank() } ?: "Failed to send message"))
                        }

                        continuation.resume(result)
                    })
                }
            }
        } catch (e: TimeoutCancellationException) {
            Result.failure(Exception("Realtime send acknowledgement timed out", e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun sendMessage(
        conversationId: String,
        content: String,
        contentType: String = "text",
        replyToId: String? = null,
        clientMessageId: String? = null
    ) {
        val obj = buildSendMessagePayload(
            conversationId = conversationId,
            content = content,
            contentType = contentType,
            replyToId = replyToId,
            clientMessageId = clientMessageId
        )
        Log.d(TAG, "📤 Sending message via socket to $conversationId transport=${currentTransportName ?: "unknown"}")
        socket?.emit("chat:send_message", obj)
    }

    fun sendTyping(conversationId: String, isTyping: Boolean) {
        if (!isConnected()) {
            reconnectIfNeeded()
        }
        Log.d(TAG, "⌨️ chat:typing conversation=$conversationId isTyping=$isTyping transport=${currentTransportName ?: "unknown"}")
        socket?.emit("chat:typing", JSONObject().put("conversationId", conversationId).put("isTyping", isTyping))
    }

    fun markRead(conversationId: String) {
        socket?.emit("chat:mark_read", JSONObject().put("conversationId", conversationId))
    }

    fun checkUserStatus(userId: String) {
        if (userId.isBlank()) return
        val activeSocket = socket ?: return
        Log.d(TAG, "🟢 Emitting user:check_status for user=$userId connected=${activeSocket.connected()}")
        activeSocket.emit("user:check_status", JSONObject().put("userId", userId))
    }
}
