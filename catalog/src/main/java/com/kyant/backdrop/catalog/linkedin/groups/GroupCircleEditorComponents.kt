package com.kyant.backdrop.catalog.linkedin.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.kyant.backdrop.catalog.ui.BasicText
import com.kyant.backdrop.catalog.ui.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop

@Composable
fun EditorHeader(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    actionEnabled: Boolean = true,
    onBackClick: () -> Unit,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassBackground(backdrop)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(contentColor.copy(alpha = 0.1f), CircleShape)
                .clip(CircleShape)
                .clickable(onClick = onBackClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.size(12.dp))

        Column(Modifier.weight(1f)) {
            BasicText(
                title,
                style = TextStyle(
                    color = contentColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
            BasicText(
                subtitle,
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.58f),
                    fontSize = 12.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (actionLabel != null && onActionClick != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (actionEnabled) accentColor else contentColor.copy(alpha = 0.14f)
                    )
                    .clickable(enabled = actionEnabled, onClick = onActionClick)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    actionLabel,
                    style = TextStyle(
                        color = if (actionEnabled) Color.White else contentColor.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

@Composable
fun EditorSectionCard(
    backdrop: LayerBackdrop,
    contentColor: Color,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassBackground(backdrop, vibrancyAlpha = 0.08f)
            .padding(14.dp)
    ) {
        BasicText(
            title,
            style = TextStyle(
                color = contentColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
        subtitle?.let {
            Spacer(Modifier.height(4.dp))
            BasicText(
                it,
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.58f),
                    fontSize = 12.sp
                )
            )
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
fun EditorTextField(
    contentColor: Color,
    accentColor: Color,
    label: String,
    value: String,
    placeholder: String,
    singleLine: Boolean = true,
    onValueChange: (String) -> Unit
) {
    Column {
        BasicText(
            label,
            style = TextStyle(
                color = contentColor.copy(alpha = 0.72f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(contentColor.copy(alpha = 0.08f))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    color = contentColor,
                    fontSize = 14.sp
                ),
                cursorBrush = SolidColor(accentColor),
                singleLine = singleLine,
                maxLines = if (singleLine) 1 else 5,
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (value.isBlank()) {
                        BasicText(
                            placeholder,
                            style = TextStyle(
                                color = contentColor.copy(alpha = 0.4f),
                                fontSize = 14.sp
                            )
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

@Composable
fun EditorToggleRow(
    contentColor: Color,
    accentColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(contentColor.copy(alpha = 0.06f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            BasicText(
                title,
                style = TextStyle(
                    color = contentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Spacer(Modifier.height(2.dp))
            BasicText(
                subtitle,
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.58f),
                    fontSize = 11.sp
                )
            )
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun EditorActionTile(
    contentColor: Color,
    accentColor: Color,
    title: String,
    subtitle: String,
    actionLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(contentColor.copy(alpha = 0.06f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            BasicText(
                title,
                style = TextStyle(
                    color = contentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Spacer(Modifier.height(2.dp))
            BasicText(
                subtitle,
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.58f),
                    fontSize = 11.sp
                )
            )
        }
        BasicText(
            actionLabel,
            style = TextStyle(
                color = accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
fun EditorStatusBanner(
    backgroundColor: Color,
    contentColor: Color,
    text: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        BasicText(
            text,
            style = TextStyle(
                color = contentColor,
                fontSize = 12.sp
            )
        )
    }
}
