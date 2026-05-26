package com.kyant.backdrop.catalog.game

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.ArcadeSocketManager
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun OnlineTicTacToeGameScreen(contentColor: Color, onNavigateBack: () -> Unit = {}) {
    val context = LocalContext.current
    var isConnecting by remember { mutableStateOf(true) }
    var currentRoom by remember { mutableStateOf<JSONObject?>(null) }
    var gameState by remember { mutableStateOf<JSONObject?>(null) }
    var board by remember { mutableStateOf(Array(3) { Array(3) { Player.NONE } }) }
    var currentTurn by remember { mutableStateOf(Player.X) }
    var winner by remember { mutableStateOf<Player?>(null) }
    var isDraw by remember { mutableStateOf(false) }

    val connectionState by ArcadeSocketManager.connectionState.collectAsState()

    // Connect and Matchmake
    LaunchedEffect(Unit) {
        val token = ApiClient.getToken(context)
        if (token != null) {
            ArcadeSocketManager.connect(token)
            // Wait a brief moment for connection, then quick match
            delay(500)
            ArcadeSocketManager.quickMatch("tic_tac_toe")
        }
    }

    LaunchedEffect(connectionState) {
        if (connectionState == ArcadeSocketManager.ConnectionState.CONNECTED) {
            isConnecting = false
        }
    }

    LaunchedEffect(Unit) {
        ArcadeSocketManager.roomStateFlow.collect { room ->
            currentRoom = room
            if (room.optString("status") == "waiting") {
                ArcadeSocketManager.setReady(room.getString("id"), true)
            }
        }
    }

    LaunchedEffect(Unit) {
        ArcadeSocketManager.gameStateFlow.collect { stateObj ->
            val type = stateObj.optString("type")
            if (type == "move") {
                val data = stateObj.optJSONObject("data")
                if (data != null) {
                    val row = data.getInt("row")
                    val col = data.getInt("col")
                    val playerStr = data.getString("player")
                    val player = if (playerStr == "X") Player.X else Player.O
                    board[row][col] = player

                    // Switch turn
                    currentTurn = if (player == Player.X) Player.O else Player.X

                    // Check win/draw locally based on sync
                    checkWinSync(board)?.let {
                        winner = it
                    }
                    if (winner == null && board.all { r -> r.all { cell -> cell != Player.NONE } }) {
                        isDraw = true
                    }
                }
            }
        }
    }


    val myRole = currentRoom?.optString("currentUserRole") // "host" or "guest"
    val myPlayer = if (myRole == "host") Player.X else if (myRole == "guest") Player.O else Player.NONE
    val isMyTurn = myPlayer == currentTurn && myPlayer != Player.NONE

    val statusText = when {
        isConnecting -> "Connecting to Server..."
        currentRoom == null -> "Matchmaking..."
        currentRoom?.optString("status") == "waiting" -> "Waiting for Opponent..."
        winner != null -> if (winner == myPlayer) "You Win!" else "You Lose!"
        isDraw -> "It's a Draw!"
        isMyTurn -> "Your Turn"
        else -> "Opponent's Turn"
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 32.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        currentRoom?.optString("id")?.let { ArcadeSocketManager.leaveRoom(it) }
                        ArcadeSocketManager.disconnect()
                        onNavigateBack()
                    }
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = contentColor
                )
            }
            Text(
                text = "Vormex Connect (Online)",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = statusText,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        if (currentRoom?.optString("status") == "waiting" || isConnecting || currentRoom == null) {
            CircularProgressIndicator(color = contentColor)
        } else {
            // Game Board
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(contentColor.copy(alpha = 0.1f))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                for (row in 0..2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        for (col in 0..2) {
                            val cell = board[row][col]
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(contentColor.copy(alpha = 0.05f))
                                    .clickable(enabled = cell == Player.NONE && isMyTurn && winner == null) {
                                        board[row][col] = myPlayer
                                        currentTurn = if (myPlayer == Player.X) Player.O else Player.X

                                        currentRoom?.optString("id")?.let { roomId ->
                                            val moveData = JSONObject()
                                                .put("row", row)
                                                .put("col", col)
                                                .put("player", if (myPlayer == Player.X) "X" else "O")
                                            ArcadeSocketManager.sendInput(roomId, "move", moveData)
                                        }

                                        checkWinSync(board)?.let { winner = it }
                                        if (winner == null && board.all { r -> r.all { c -> c != Player.NONE } }) {
                                            isDraw = true
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (cell != Player.NONE) {
                                    Text(
                                        text = if (cell == Player.X) "X" else "O",
                                        fontSize = 48.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (cell == Player.X) Color(0xFF38BDF8) else Color(0xFFF472B6)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        AnimatedVisibility(visible = winner != null || isDraw) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF38BDF8))
                    .clickable {
                        currentRoom?.optString("id")?.let { ArcadeSocketManager.leaveRoom(it) }
                        ArcadeSocketManager.disconnect()
                        onNavigateBack()
                    }
                    .padding(horizontal = 32.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Leave Game",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

private fun checkWinSync(b: Array<Array<Player>>): Player? {
    for (i in 0..2) {
        if (b[i][0] != Player.NONE && b[i][0] == b[i][1] && b[i][1] == b[i][2]) return b[i][0]
        if (b[0][i] != Player.NONE && b[0][i] == b[1][i] && b[1][i] == b[2][i]) return b[0][i]
    }
    if (b[0][0] != Player.NONE && b[0][0] == b[1][1] && b[1][1] == b[2][2]) return b[0][0]
    if (b[0][2] != Player.NONE && b[0][2] == b[1][1] && b[1][1] == b[2][0]) return b[0][2]
    return null
}
