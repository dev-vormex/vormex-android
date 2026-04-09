package com.kyant.backdrop.catalog.network

import android.util.Log
import com.kyant.backdrop.catalog.BuildConfig
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.engineio.client.transports.WebSocket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONObject
import java.net.URI

/**
 * Manages Socket.IO connection for real-time group chat with vormex-backend.
 * 
 * Backend events for groups:
 * - group:join, group:leave (room management)
 * - group:message (send message)
 * - group:new_message (receive new message)
 * - group:typing (send/receive typing indicators)
 * - group:user_typing (typing indicator state)
 * - group:online_count (active members count)
 * - group:message_deleted
 * - group:user_joined, group:user_left
 */
object GroupSocketManager {
    private const val TAG = "GroupSocket"

    private val SOCKET_URL: String
        get() = BuildConfig.SOCKET_BASE_URL

    private var socket: Socket? = null
    private var currentToken: String? = null
    private var isConnecting = false
    
    // Track joined groups to re-join on reconnect
    private val joinedGroups = mutableSetOf<String>()

    /** Emits (groupId, messageJsonString) for new group messages */
    private val _newMessageFlow = MutableSharedFlow<Pair<String, String>>(replay = 0, extraBufferCapacity = 10)
    val newMessageFlow = _newMessageFlow.asSharedFlow()

    /** Emits (groupId, user, isTyping) for typing indicators */
    private val _typingFlow = MutableSharedFlow<GroupTypingEvent>(replay = 0, extraBufferCapacity = 5)
    val typingFlow = _typingFlow.asSharedFlow()

    /** Emits (groupId, userId, messageId) for message deletion events */
    private val _messageDeletedFlow = MutableSharedFlow<Triple<String, String, String>>(replay = 0, extraBufferCapacity = 5)
    val messageDeletedFlow = _messageDeletedFlow.asSharedFlow()

    /** Emits (groupId, userId, isJoining) for user join/leave events */
    private val _userJoinLeaveFlow = MutableSharedFlow<GroupUserEvent>(replay = 0, extraBufferCapacity = 5)
    val userJoinLeaveFlow = _userJoinLeaveFlow.asSharedFlow()

    /** Emits (groupId, count) for online member count updates */
    private val _onlineCountFlow = MutableSharedFlow<Pair<String, Int>>(replay = 0, extraBufferCapacity = 5)
    val onlineCountFlow = _onlineCountFlow.asSharedFlow()
    
    /** Connection state flow for UI feedback */
    private val _connectionStateFlow = MutableSharedFlow<ConnectionState>(replay = 1, extraBufferCapacity = 1)
    val connectionStateFlow = _connectionStateFlow.asSharedFlow()
    
    enum class ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, ERROR
    }

    data class GroupTypingEvent(
        val groupId: String,
        val userId: String,
        val userName: String?,
        val userImage: String?,
        val isTyping: Boolean
    )

    data class GroupUserEvent(
        val groupId: String,
        val userId: String,
        val userName: String?,
        val userImage: String?,
        val isJoining: Boolean
    )

    fun isConnected(): Boolean = socket?.connected() == true
    
    fun getConnectionState(): ConnectionState = when {
        socket?.connected() == true -> ConnectionState.CONNECTED
        isConnecting -> ConnectionState.CONNECTING
        else -> ConnectionState.DISCONNECTED
    }

    fun connect(token: String) {
        if (socket?.connected() == true && currentToken == token) {
            Log.d(TAG, "Already connected with same token, skipping")
            return
        }
        if (isConnecting && currentToken == token) {
            Log.d(TAG, "Already connecting with same token, skipping")
            return
        }
        
        Log.d(TAG, "Connecting to socket at $SOCKET_URL")
        disconnect()
        currentToken = token
        isConnecting = true
        _connectionStateFlow.tryEmit(ConnectionState.CONNECTING)
        
        try {
            val opts = IO.Options().apply {
                forceNew = true
                reconnection = true
                reconnectionAttempts = 10
                reconnectionDelay = 1000
                reconnectionDelayMax = 5000
                timeout = 20000
                transports = arrayOf(WebSocket.NAME, "polling")
                auth = mapOf("token" to token)
            }
            socket = IO.socket(URI.create(SOCKET_URL), opts).apply {
                on(Socket.EVENT_CONNECT) {
                    isConnecting = false
                    Log.d(TAG, "✅ Group socket connected! ID: ${id()}")
                    _connectionStateFlow.tryEmit(ConnectionState.CONNECTED)
                    // Re-join all tracked groups on connect
                    rejoinAllGroups()
                }
                on(Socket.EVENT_DISCONNECT) { args ->
                    Log.d(TAG, "❌ Group socket disconnected: ${args.getOrNull(0)}")
                    _connectionStateFlow.tryEmit(ConnectionState.DISCONNECTED)
                }
                on(Socket.EVENT_CONNECT_ERROR) { args ->
                    isConnecting = false
                    val error = args.getOrNull(0)?.toString() ?: "Unknown error"
                    Log.e(TAG, "🔴 Group socket connect error: $error")
                    _connectionStateFlow.tryEmit(ConnectionState.ERROR)
                }
                on("reconnect") { args ->
                    Log.d(TAG, "🔄 Group socket reconnected: attempt ${args.getOrNull(0)}")
                    _connectionStateFlow.tryEmit(ConnectionState.CONNECTED)
                    rejoinAllGroups()
                }
                on("reconnect_attempt") { args ->
                    Log.d(TAG, "🔄 Group socket reconnecting... attempt ${args.getOrNull(0)}")
                    _connectionStateFlow.tryEmit(ConnectionState.CONNECTING)
                }
                on("reconnect_failed") {
                    Log.e(TAG, "🔴 Group socket reconnection failed after all attempts")
                    _connectionStateFlow.tryEmit(ConnectionState.ERROR)
                }
                
                // Group events
                on("group:new_message") { args ->
                    Log.d(TAG, "📩 Received group:new_message")
                    handleNewMessage(args)
                }
                on("group:typing") { args ->
                    handleTyping(args)
                }
                on("group:user_typing") { args ->
                    handleUserTyping(args)
                }
                on("group:online_count") { args ->
                    handleOnlineCount(args)
                }
                on("group:message_deleted") { args ->
                    handleMessageDeleted(args)
                }
                on("group:user_joined") { args ->
                    handleUserJoined(args)
                }
                on("group:user_left") { args ->
                    handleUserLeft(args)
                }
            }
            socket?.connect()
            Log.d(TAG, "Group socket.connect() called")
        } catch (e: Exception) {
            isConnecting = false
            Log.e(TAG, "Group socket init error", e)
            _connectionStateFlow.tryEmit(ConnectionState.ERROR)
        }
    }

    fun disconnect() {
        Log.d(TAG, "Disconnecting group socket")
        joinedGroups.clear()
        socket?.disconnect()
        socket?.off()
        socket = null
        currentToken = null
        isConnecting = false
        _connectionStateFlow.tryEmit(ConnectionState.DISCONNECTED)
    }

    private fun rejoinAllGroups() {
        Log.d(TAG, "Re-joining ${joinedGroups.size} groups...")
        joinedGroups.forEach { groupId ->
            socket?.emit("group:join", JSONObject().put("groupId", groupId))
        }
    }

    fun joinGroup(groupId: String) {
        Log.d(TAG, "Joining group room: $groupId")
        joinedGroups.add(groupId)
        socket?.emit("group:join", JSONObject().put("groupId", groupId))
    }

    fun leaveGroup(groupId: String) {
        Log.d(TAG, "Leaving group room: $groupId")
        joinedGroups.remove(groupId)
        socket?.emit("group:leave", JSONObject().put("groupId", groupId))
    }

    /**
     * Send a message to a group via socket
     */
    fun sendMessage(
        groupId: String, 
        content: String, 
        tempId: String,
        contentType: String = "text",
        mediaUrl: String? = null,
        mediaType: String? = null,
        replyToId: String? = null
    ) {
        Log.d(TAG, "📤 Sending group message to $groupId: ${content.take(30)}")
        val payload = JSONObject().apply {
            put("groupId", groupId)
            put("content", content)
            put("tempId", tempId)
            put("contentType", contentType)
            mediaUrl?.let { put("mediaUrl", it) }
            mediaType?.let { put("mediaType", it) }
            replyToId?.let { put("replyToId", it) }
        }
        socket?.emit("group:message", payload)
    }

    /**
     * Send typing indicator
     */
    fun sendTyping(groupId: String, isTyping: Boolean) {
        socket?.emit("group:typing", JSONObject().apply {
            put("groupId", groupId)
            put("isTyping", isTyping)
        })
    }

    /**
     * Delete a message
     */
    fun deleteMessage(groupId: String, messageId: String) {
        socket?.emit("group:delete_message", JSONObject().apply {
            put("groupId", groupId)
            put("messageId", messageId)
        })
    }

    /**
     * Add reaction to message
     */
    fun addReaction(groupId: String, messageId: String, emoji: String) {
        socket?.emit("group:message_reaction", JSONObject().apply {
            put("groupId", groupId)
            put("messageId", messageId)
            put("emoji", emoji)
            put("action", "add")
        })
    }

    // Event handlers
    private fun handleNewMessage(args: Array<Any>) {
        if (args.isEmpty()) return
        try {
            val rawArg = args[0]
            val obj = rawArg as? JSONObject ?: JSONObject(rawArg.toString())
            val groupId = obj.optString("groupId")
            val message = obj.optJSONObject("message") ?: obj
            Log.d(TAG, "📩 New message in group $groupId: ${message.optString("content").take(50)}")
            if (groupId.isNotEmpty()) {
                _newMessageFlow.tryEmit(groupId to message.toString())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling group message", e)
        }
    }

    private fun handleTyping(args: Array<Any>) {
        handleUserTyping(args)
    }

    private fun handleUserTyping(args: Array<Any>) {
        if (args.isEmpty()) return
        try {
            val obj = args[0] as? JSONObject ?: JSONObject(args[0].toString())
            val event = GroupTypingEvent(
                groupId = obj.optString("groupId"),
                userId = obj.optString("userId"),
                userName = obj.optString("userName").takeIf { it.isNotEmpty() },
                userImage = obj.optString("userImage").takeIf { it.isNotEmpty() },
                isTyping = obj.optBoolean("isTyping", true)
            )
            _typingFlow.tryEmit(event)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling typing", e)
        }
    }

    private fun handleOnlineCount(args: Array<Any>) {
        if (args.isEmpty()) return
        try {
            val obj = args[0] as? JSONObject ?: JSONObject(args[0].toString())
            val groupId = obj.optString("groupId")
            val count = obj.optInt("count", 0)
            _onlineCountFlow.tryEmit(groupId to count)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling online count", e)
        }
    }

    private fun handleMessageDeleted(args: Array<Any>) {
        if (args.isEmpty()) return
        try {
            val obj = args[0] as? JSONObject ?: JSONObject(args[0].toString())
            _messageDeletedFlow.tryEmit(
                Triple(
                    obj.optString("groupId"),
                    obj.optString("userId"),
                    obj.optString("messageId")
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error handling message deleted", e)
        }
    }

    private fun handleUserJoined(args: Array<Any>) {
        if (args.isEmpty()) return
        try {
            val obj = args[0] as? JSONObject ?: JSONObject(args[0].toString())
            _userJoinLeaveFlow.tryEmit(
                GroupUserEvent(
                    groupId = obj.optString("groupId"),
                    userId = obj.optString("userId"),
                    userName = obj.optString("userName").takeIf { it.isNotEmpty() },
                    userImage = obj.optString("userImage").takeIf { it.isNotEmpty() },
                    isJoining = true
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error handling user joined", e)
        }
    }

    private fun handleUserLeft(args: Array<Any>) {
        if (args.isEmpty()) return
        try {
            val obj = args[0] as? JSONObject ?: JSONObject(args[0].toString())
            _userJoinLeaveFlow.tryEmit(
                GroupUserEvent(
                    groupId = obj.optString("groupId"),
                    userId = obj.optString("userId"),
                    userName = obj.optString("userName").takeIf { it.isNotEmpty() },
                    userImage = obj.optString("userImage").takeIf { it.isNotEmpty() },
                    isJoining = false
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error handling user left", e)
        }
    }
}
