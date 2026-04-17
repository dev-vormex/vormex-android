package com.kyant.backdrop.catalog.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class VormexAiChipAction(
    val label: String,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

@Composable
fun VormexAiChipRow(
    actions: List<VormexAiChipAction>,
    contentColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    if (actions.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        actions.forEach { action ->
            val background = if (action.enabled) {
                accentColor.copy(alpha = 0.14f)
            } else {
                contentColor.copy(alpha = 0.07f)
            }
            BasicText(
                text = action.label,
                modifier = Modifier
                    .background(background, RoundedCornerShape(999.dp))
                    .clickable(enabled = action.enabled, onClick = action.onClick)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                style = TextStyle(
                    color = if (action.enabled) contentColor else contentColor.copy(alpha = 0.45f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
fun VormexAiStatusCard(
    message: String,
    contentColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier,
    primaryAction: VormexAiChipAction? = null,
    secondaryAction: VormexAiChipAction? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BasicText(
            text = message,
            style = TextStyle(
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        )
        val actions = listOfNotNull(primaryAction, secondaryAction)
        if (actions.isNotEmpty()) {
            VormexAiChipRow(
                actions = actions,
                contentColor = contentColor,
                accentColor = accentColor
            )
        }
    }
}
