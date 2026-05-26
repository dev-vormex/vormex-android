package com.kyant.backdrop.catalog.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Gamepad
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class GameType { NONE, TIC_TAC_TOE, TIC_TAC_TOE_ONLINE, TAP_DUEL }

@Composable
fun GamesHubScreen(
    contentColor: Color,
    onNavigateBack: () -> Unit,
    onSelectGame: (GameType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(top = 48.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 32.dp)
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
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Vormex Arcade",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }

        GameCard(
            title = "Vormex Connect (Local)",
            subtitle = "Classic Tic-Tac-Toe locally with a friend.",
            icon = Icons.Outlined.Gamepad,
            color = Color(0xFF38BDF8),
            textColor = contentColor,
            onClick = { onSelectGame(GameType.TIC_TAC_TOE) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        GameCard(
            title = "Vormex Connect (Online)",
            subtitle = "Matchmake and play with anyone worldwide.",
            icon = Icons.Outlined.SportsEsports,
            color = Color(0xFF8B5CF6),
            textColor = contentColor,
            onClick = { onSelectGame(GameType.TIC_TAC_TOE_ONLINE) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        GameCard(
            title = "Tap Duel",
            subtitle = "Fast-paced screen-tapping competition.",
            icon = Icons.Outlined.TouchApp,
            color = Color(0xFFF472B6),
            textColor = contentColor,
            onClick = { onSelectGame(GameType.TAP_DUEL) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        GameCard(
            title = "Spy Fall",
            subtitle = "Coming soon. Real-time multiplayer.",
            icon = Icons.Outlined.SportsEsports,
            color = Color(0xFF94A3B8),
            textColor = contentColor,
            onClick = { /* Coming soon */ }
        )
    }
}

@Composable
fun GameCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable { onClick() }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = textColor.copy(alpha = 0.6f)
            )
        }
    }
}
