package com.kyant.backdrop.catalog.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import kotlinx.coroutines.delay

enum class Player { X, O, NONE }

@Composable
fun TicTacToeGameScreen(contentColor: Color, onNavigateBack: () -> Unit = {}) {
    var board by remember { mutableStateOf(Array(3) { Array(3) { Player.NONE } }) }
    var currentPlayer by remember { mutableStateOf(Player.X) }
    var winner by remember { mutableStateOf<Player?>(null) }
    var isDraw by remember { mutableStateOf(false) }

    fun checkWinner() {
        // Rows
        for (i in 0..2) {
            if (board[i][0] != Player.NONE && board[i][0] == board[i][1] && board[i][1] == board[i][2]) winner = board[i][0]
        }
        // Cols
        for (i in 0..2) {
            if (board[0][i] != Player.NONE && board[0][i] == board[1][i] && board[1][i] == board[2][i]) winner = board[0][i]
        }
        // Diagonals
        if (board[0][0] != Player.NONE && board[0][0] == board[1][1] && board[1][1] == board[2][2]) winner = board[0][0]
        if (board[0][2] != Player.NONE && board[0][2] == board[1][1] && board[1][1] == board[2][0]) winner = board[0][2]

        // Check Draw
        if (winner == null && board.flatten().none { it == Player.NONE }) {
            isDraw = true
        }
    }

    fun resetGame() {
        board = Array(3) { Array(3) { Player.NONE } }
        currentPlayer = Player.X
        winner = null
        isDraw = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)), // Dark premium background
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onNavigateBack() }
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = contentColor
                    )
                }
                Text(
                    text = "Vormex Connect",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Current Turn Indicator
            AnimatedVisibility(visible = winner == null && !isDraw) {
                Text(
                    text = "Player ${currentPlayer.name}'s Turn",
                    fontSize = 20.sp,
                    color = if (currentPlayer == Player.X) Color(0xFF38BDF8) else Color(0xFFF472B6),
                    fontWeight = FontWeight.Medium
                )
            }

            // Game Board
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1E293B))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (row in 0..2) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (col in 0..2) {
                                GameCell(
                                    player = board[row][col],
                                    onClick = {
                                        if (board[row][col] == Player.NONE && winner == null && !isDraw) {
                                            val newBoard = board.map { it.clone() }.toTypedArray()
                                            newBoard[row][col] = currentPlayer
                                            board = newBoard
                                            checkWinner()
                                            if (winner == null) {
                                                currentPlayer = if (currentPlayer == Player.X) Player.O else Player.X
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Game Over State
            AnimatedVisibility(
                visible = winner != null || isDraw,
                enter = fadeIn() + scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                exit = fadeOut() + scaleOut()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (winner != null) "Player ${winner?.name} Wins!" else "It's a Draw!",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981) // Emerald Green for success
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF38BDF8))
                            .clickable { resetGame() }
                            .padding(horizontal = 32.dp, vertical = 16.dp)
                    ) {
                        Text(text = "Play Again", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun GameCell(player: Player, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (player != Player.NONE) 1f else 0.8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cell_scale"
    )

    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF334155))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (player != Player.NONE) {
            Text(
                text = player.name,
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = if (player == Player.X) Color(0xFF38BDF8) else Color(0xFFF472B6),
                modifier = Modifier.scale(scale)
            )
        }
    }
}
