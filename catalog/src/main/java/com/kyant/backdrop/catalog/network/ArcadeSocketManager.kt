package com.kyant.backdrop.catalog.network

import android.util.Log
import com.kyant.backdrop.catalog.BuildConfig
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.engineio.client.transports.WebSocket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.net.URI

object ArcadeSocketManager {
    private const val TAG = "ArcadeSocket"
    private val SOCKET_URL: String get() = BuildConfig.SOCKET_BASE_URL

    private var socket: Socket? = null
    private var currentToken: String? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()

    private val _roomStateFlow = MutableSharedFlow<JSONObject>(replay = 1, extraBufferCapacity = 5)
    val roomStateFlow = _roomStateFlow.asSharedFlow()

    private val _gameStateFlow = MutableSharedFlow<JSONObject>(replay = 0, extraBufferCapacity = 10)
    val gameStateFlow = _gameStateFlow.asSharedFlow()

    private val _errorFlow = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 5)
    val errorFlow = _errorFlow.asSharedFlow()

    enum class ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, ERROR
    }

    fun connect(token: String) {
        if (socket?.connected() == true && currentToken == token) return

        disconnect()
        currentToken = token
        _connectionState.value = ConnectionState.CONNECTING

        try {
            val opts = IO.Options().apply {
                forceNew = true
                reconnection = true
                transports = arrayOf(WebSocket.NAME, "polling")
                auth = mapOf("token" to token)
            }
            socket = IO.socket(URI.create(SOCKET_URL), opts).apply {
                on(Socket.EVENT_CONNECT) {
                    Log.d(TAG, "Connected to Arcade Socket")
                    _connectionState.value = ConnectionState.CONNECTED
                }
                on(Socket.EVENT_DISCONNECT) {
                    _connectionState.value = ConnectionState.DISCONNECTED
                }
                on(Socket.EVENT_CONNECT_ERROR) {
                    _connectionState.value = ConnectionState.ERROR
                }
                on("arcade:room_updated") { args ->
                    val data = args[0] as? JSONObject ?: runCatching { JSONObject(args[0].toString()) }.getOrNull()
                    data?.optJSONObject("room")?.let { _roomStateFlow.tryEmit(it) }
                }
                on("arcade:joined") { args ->
                    val data = args[0] as? JSONObject ?: runCatching { JSONObject(args[0].toString()) }.getOrNull()
                    data?.optJSONObject("room")?.let { _roomStateFlow.tryEmit(it) }
                }
                on("arcade:state") { args ->
                    val data = args[0] as? JSONObject ?: runCatching { JSONObject(args[0].toString()) }.getOrNull()
                    data?.let { _gameStateFlow.tryEmit(it) }
                }
                on("arcade:error") { args ->
                    val data = args[0] as? JSONObject ?: runCatching { JSONObject(args[0].toString()) }.getOrNull()
                    data?.optString("message")?.let { _errorFlow.tryEmit(it) }
                }
            }
            socket?.connect()
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.ERROR
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket = null
        currentToken = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    fun joinRoom(roomId: String? = null, inviteCode: String? = null) {
        val payload = JSONObject()
        roomId?.let { payload.put("roomId", it) }
        inviteCode?.let { payload.put("inviteCode", it) }
        socket?.emit("arcade:join_room", payload)
    }

    fun quickMatch(gameType: String) {
        val payload = JSONObject().put("gameType", gameType)
        socket?.emit("arcade:quick_match", payload)
    }

    fun setReady(roomId: String, ready: Boolean) {
        val payload = JSONObject()
            .put("roomId", roomId)
            .put("ready", ready)
        socket?.emit("arcade:ready", payload)
    }

    fun sendInput(roomId: String, type: String, data: Any) {
        val payload = JSONObject()
            .put("roomId", roomId)
            .put("type", type)
            .put("data", data)
        socket?.emit("arcade:input", payload)
    }

    fun leaveRoom(roomId: String) {
        val payload = JSONObject().put("roomId", roomId)
        socket?.emit("arcade:leave_room", payload)
    }
}
