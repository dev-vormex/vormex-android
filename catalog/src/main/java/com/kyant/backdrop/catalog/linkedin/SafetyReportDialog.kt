package com.kyant.backdrop.catalog.linkedin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kyant.backdrop.catalog.ui.BasicText
import com.kyant.backdrop.catalog.ui.BasicTextField

private data class SafetyReportReasonOption(
    val id: String,
    val label: String,
    val description: String
)

private val SafetyReportReasonOptions = listOf(
    SafetyReportReasonOption("spam", "Spam", "Unwanted commercial content or repeated messages"),
    SafetyReportReasonOption("harassment", "Harassment", "Bullying, threats, or repeated unwanted contact"),
    SafetyReportReasonOption("hate_speech", "Hate speech", "Hateful or discriminatory content"),
    SafetyReportReasonOption("violence", "Violence", "Violent or graphic content"),
    SafetyReportReasonOption("inappropriate", "Inappropriate", "Adult or inappropriate content"),
    SafetyReportReasonOption("misinformation", "Misinformation", "False or misleading information"),
    SafetyReportReasonOption("impersonation", "Impersonation", "Pretending to be someone else"),
    SafetyReportReasonOption("other", "Other", "Something else Trust & Safety should review")
)

@Composable
fun SafetyReportDialog(
    title: String,
    subtitle: String,
    contentColor: Color,
    accentColor: Color,
    blockLabel: String? = null,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (reason: String, details: String, blockTarget: Boolean) -> Unit
) {
    var selectedReason by rememberSaveable { mutableStateOf(SafetyReportReasonOptions.first().id) }
    var details by rememberSaveable { mutableStateOf("") }
    var blockTarget by rememberSaveable { mutableStateOf(false) }
    val mutedColor = contentColor.copy(alpha = 0.64f)

    Dialog(
        onDismissRequest = {
            if (!isSubmitting) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF111111).copy(alpha = 0.94f))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 620.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                BasicText(title, style = TextStyle(Color.White, 20.sp, FontWeight.Bold))
                BasicText(subtitle, style = TextStyle(Color.White.copy(alpha = 0.68f), 13.sp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SafetyReportReasonOptions.forEach { option ->
                        val selected = selectedReason == option.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) accentColor.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.06f)
                                )
                                .border(
                                    1.dp,
                                    if (selected) accentColor.copy(alpha = 0.48f) else Color.White.copy(alpha = 0.08f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable(enabled = !isSubmitting) { selectedReason = option.id }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            BasicText(
                                if (selected) "*" else "o",
                                style = TextStyle(if (selected) accentColor else mutedColor, 16.sp, FontWeight.Bold)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                BasicText(option.label, style = TextStyle(Color.White, 14.sp, FontWeight.SemiBold))
                                BasicText(option.description, style = TextStyle(Color.White.copy(alpha = 0.58f), 12.sp))
                            }
                        }
                    }
                }

                BasicTextField(
                    value = details,
                    onValueChange = { details = it.take(1000) },
                    enabled = !isSubmitting,
                    textStyle = TextStyle(Color.White, 14.sp),
                    cursorBrush = SolidColor(accentColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 92.dp),
                    decorationBox = { inner ->
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.07f))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            if (details.isBlank()) {
                                BasicText(
                                    "Add details for the moderator",
                                    style = TextStyle(Color.White.copy(alpha = 0.42f), 14.sp)
                                )
                            }
                            inner()
                        }
                    }
                )

                blockLabel?.let { label ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (blockTarget) accentColor.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f))
                            .clickable(enabled = !isSubmitting) { blockTarget = !blockTarget }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BasicText(
                            if (blockTarget) "*" else "o",
                            style = TextStyle(if (blockTarget) accentColor else mutedColor, 16.sp, FontWeight.Bold)
                        )
                        BasicText(label, style = TextStyle(Color.White, 14.sp, FontWeight.SemiBold))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable(enabled = !isSubmitting) { onDismiss() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText("Cancel", style = TextStyle(Color.White.copy(alpha = 0.82f), 14.sp, FontWeight.SemiBold))
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor)
                            .clickable(enabled = !isSubmitting) {
                                onSubmit(selectedReason, details.trim(), blockTarget)
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            BasicText("Submit", style = TextStyle(Color.White, 14.sp, FontWeight.Bold))
                        }
                    }
                }
            }
        }
    }
}
