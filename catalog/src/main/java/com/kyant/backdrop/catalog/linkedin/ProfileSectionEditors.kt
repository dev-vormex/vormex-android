package com.kyant.backdrop.catalog.linkedin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.remember
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
import com.kyant.backdrop.catalog.ui.BasicText
import com.kyant.backdrop.catalog.ui.BasicTextField

@Composable
private fun ProfileEditorHeader(
    title: String,
    isSaving: Boolean,
    contentColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(currentVormexAppearance().navigationColor)
            .border(1.dp, currentVormexAppearance().navigationBorderColor)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(
            "×",
            style = TextStyle(contentColor, 30.sp, FontWeight.Light),
            modifier = Modifier.clickable(enabled = !isSaving, onClick = onDismiss)
        )
        BasicText(title, style = TextStyle(contentColor, 19.sp, FontWeight.Bold))
        Spacer(Modifier.size(30.dp))
    }
}

@Composable
private fun ProfileEditorSaveBar(
    isSaving: Boolean,
    contentColor: Color,
    accentColor: Color,
    onSave: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(currentVormexAppearance().navigationColor)
            .border(1.dp, currentVormexAppearance().navigationBorderColor)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(999.dp))
                .background(if (isSaving) accentColor.copy(alpha = 0.55f) else accentColor)
                .clickable(enabled = !isSaving, onClick = onSave)
                .padding(vertical = 13.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                }
                BasicText(
                    if (isSaving) "Saving" else "Save",
                    style = TextStyle(Color.White, 16.sp, FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun EditAboutScreen(
    value: String,
    isSaving: Boolean,
    error: String?,
    contentColor: Color,
    accentColor: Color,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(currentVormexAppearance().backgroundColor)
    ) {
        ProfileEditorHeader("Edit about", isSaving, contentColor, accentColor, onDismiss, onSave)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BasicText(
                "Write about your experience, strengths, achievements, and the work you want to do.",
                style = TextStyle(contentColor.copy(alpha = 0.62f), 14.sp, lineHeight = 21.sp)
            )
            BasicTextField(
                value = value,
                onValueChange = { onValueChange(it.take(2_600)) },
                textStyle = TextStyle(contentColor, 16.sp, lineHeight = 23.sp),
                cursorBrush = SolidColor(accentColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(contentColor.copy(alpha = 0.035f))
                    .border(1.dp, contentColor.copy(alpha = 0.26f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                error?.let {
                    BasicText(it, style = TextStyle(Color(0xFFDC2626), 12.sp, FontWeight.Medium))
                } ?: Spacer(Modifier.size(1.dp))
                BasicText(
                    "${value.length}/2600",
                    style = TextStyle(contentColor.copy(alpha = 0.45f), 11.sp)
                )
            }
        }
        ProfileEditorSaveBar(isSaving, contentColor, accentColor, onSave)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditInterestsScreen(
    interests: List<String>,
    isSaving: Boolean,
    error: String?,
    contentColor: Color,
    accentColor: Color,
    onInterestsChange: (List<String>) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    var newInterest by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    fun addInterest() {
        val value = newInterest.trim()
        when {
            value.length !in 2..30 -> localError = "Interests must be 2–30 characters"
            interests.size >= 10 -> localError = "You can add up to 10 interests"
            interests.any { it.equals(value, ignoreCase = true) } -> localError = "That interest is already added"
            else -> {
                onInterestsChange(interests + value)
                newInterest = ""
                localError = null
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(currentVormexAppearance().backgroundColor)
    ) {
        ProfileEditorHeader("Edit interests", isSaving, contentColor, accentColor, onDismiss, onSave)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BasicText(
                "Add topics you care about. These improve profile discovery and matching.",
                style = TextStyle(contentColor.copy(alpha = 0.62f), 14.sp, lineHeight = 21.sp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = newInterest,
                    onValueChange = {
                        newInterest = it.take(30)
                        localError = null
                    },
                    textStyle = TextStyle(contentColor, 15.sp),
                    cursorBrush = SolidColor(accentColor),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(contentColor.copy(alpha = 0.05f))
                        .border(1.dp, contentColor.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 13.dp)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor)
                        .clickable(enabled = !isSaving, onClick = ::addInterest)
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    BasicText("Add", style = TextStyle(Color.White, 14.sp, FontWeight.Bold))
                }
            }
            (localError ?: error)?.let {
                BasicText(it, style = TextStyle(Color(0xFFDC2626), 12.sp, FontWeight.Medium))
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                interests.forEach { interest ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(accentColor.copy(alpha = 0.10f))
                            .border(1.dp, accentColor.copy(alpha = 0.22f), RoundedCornerShape(999.dp))
                            .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicText(interest, style = TextStyle(contentColor, 13.sp, FontWeight.Medium))
                        BasicText(
                            "×",
                            style = TextStyle(contentColor.copy(alpha = 0.58f), 17.sp),
                            modifier = Modifier.clickable(enabled = !isSaving) {
                                onInterestsChange(interests.filterNot { it == interest })
                            }
                        )
                    }
                }
            }
            BasicText(
                "${interests.size}/10 interests",
                style = TextStyle(contentColor.copy(alpha = 0.45f), 11.sp)
            )
        }
        ProfileEditorSaveBar(isSaving, contentColor, accentColor, onSave)
    }
}
