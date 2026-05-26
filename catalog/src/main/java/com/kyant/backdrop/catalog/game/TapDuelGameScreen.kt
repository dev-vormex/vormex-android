package com.kyant.backdrop.catalog.game

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun TapDuelGameScreen(contentColor: Color, onNavigateBack: () -> Unit) {
    var player1Score by remember { mutableIntStateOf(50) }
    var player2Score by remember { mutableIntStateOf(50) }
    var winner by remember { mutableStateOf(0) }
    var countdown by remember { mutableIntStateOf(3) }
    var isPlaying by remember { mutableStateOf(false) }

    LaunchedEffect(countdown, winner) {
        if (winner != 0) return@LaunchedEffect
        if (countdown > 0) {
            delay(1000)
            countdown -= 1
        } else {
            isPlaying = true
        }
    }

    val p1Weight by animateFloatAsState(
        targetValue = player1Score.toFloat() / 100f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "p1Weight"
    )

    val p2Weight by animateFloatAsState(
        targetValue = player2Score.toFloat() / 100f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "p2Weight"
    )

    fun onPlayerTap(player: Int) {
        if (!isPlaying || winner != 0) return
        if (player == 1) {
            player1Score += 2
            player2Score -= 2
        } else {
            player2Score += 2
            player1Score -= 2
        }

        if (player1Score >= 100) {
            player1Score = 100
            player2Score = 0
            winner = 1
            isPlaying = false
        } else if (player2Score >= 100) {
            player2Score = 100
            player1Score = 0
            winner = 2
            isPlaying = false
        }
    }

    fun reset() {
        player1Score = 50
        player2Score = 50
        winner = 0
        countdown = 3
        isPlaying = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Player 2 Area (Top) - Rotated 180 degrees visually for the other player
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(p2Weight.coerceAtLeast(0.01f))
                .background(Color(0xFFF472B6))
                .clickable { onPlayerTap(2) },
            contentAlignment = Alignment.Center
        ) {
            if (winner == 2) {
                Text("YOU WIN!", fontSize = 48.sp, fontWeight = FontWeight.Black, color = contentColor)
            }
        }

        // Divider area (Center)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(Color.White)
        )

        // Player 1 Area (Bottom)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(p1Weight.coerceAtLeast(0.01f))
                .background(Color(0xFF38BDF8))
                .clickable { onPlayerTap(1) },
            contentAlignment = Alignment.Center
        ) {
            if (winner == 1) {
                Text("YOU WIN!", fontSize = 48.sp, fontWeight = FontWeight.Black, color = contentColor)
            }
        }
    }

    // Overlays for Game State
    if (!isPlaying && winner == 0) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (countdown > 0) countdown.toString() else "TAP!",
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Black,
                    color = contentColor
                )
            }
        }
    }

    if (winner != 0) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF38BDF8))
                        .clickable { reset() }
                        .padding(horizontal = 32.dp, vertical = 16.dp)
                ) {
                    Text("Play Again", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFEF4444))
                        .clickable { onNavigateBack() }
                        .padding(horizontal = 32.dp, vertical = 16.dp)
                ) {
                    Text("Exit", fontSize = 20.sp, fontWeight = FontWeight.Medium, color = Color.White)
                }
            }
        }
    }
}
