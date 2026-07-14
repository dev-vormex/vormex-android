package com.kyant.backdrop.catalog.linkedin

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.network.models.Achievement
import com.kyant.backdrop.catalog.network.models.Certificate
import com.kyant.backdrop.catalog.network.models.Education
import com.kyant.backdrop.catalog.network.models.Experience
import com.kyant.backdrop.catalog.ui.BasicText
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ==================== Profile Showcase Sections ====================
// Experience / Education / Licenses & Certifications / Achievements.
// One surface card per section, timeline entries, collapsed to
// VX_COLLAPSED_ENTRIES with a "Show all" footer for long lists.

private const val VX_COLLAPSED_ENTRIES = 3
private const val VX_SEE_MORE_THRESHOLD = 130

private val VxTileShape = RoundedCornerShape(10.dp)

// ==================== Section chrome ====================

@Composable
private fun VxSectionCard(
    title: String,
    count: Int,
    iconRes: Int,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    onAdd: () -> Unit,
    content: @Composable () -> Unit
) {
    val appearance = currentVormexAppearance()
    Column(
        Modifier
            .fillMaxWidth()
            .background(appearance.cardColor)
            .padding(horizontal = 18.dp, vertical = 20.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText(
                    title,
                    style = TextStyle(contentColor, 20.sp, FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (count > 0) {
                    BasicText(
                        "($count)",
                        style = TextStyle(contentColor.copy(alpha = 0.55f), 14.sp),
                        maxLines = 1
                    )
                }
            }

            if (isOwner) {
                VxAddPill(accentColor = accentColor, onClick = onAdd)
            }
        }

        Spacer(Modifier.height(16.dp))

        content()
    }
}

@Composable
private fun VxAddPill(
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.ic_vx_plus),
            contentDescription = "Add",
            modifier = Modifier.size(20.dp),
            colorFilter = ColorFilter.tint(accentColor)
        )
    }
}

@Composable
private fun VxShowAllFooter(
    expanded: Boolean,
    totalCount: Int,
    noun: String,
    contentColor: Color,
    onToggle: () -> Unit
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "vxShowAllChevron"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clickable(onClick = onToggle)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                if (expanded) "Show less" else "Show all $totalCount $noun",
                style = TextStyle(contentColor.copy(alpha = 0.82f), 14.sp, FontWeight.SemiBold)
            )
            Image(
                painter = painterResource(R.drawable.ic_vx_chevron_down),
                contentDescription = null,
                modifier = Modifier
                    .size(13.dp)
                    .graphicsLayer { rotationZ = chevronRotation },
                colorFilter = ColorFilter.tint(contentColor.copy(alpha = 0.6f))
            )
        }
    }
}

// ==================== Entry building blocks ====================

@Composable
private fun VxTimelineEntry(
    isLast: Boolean,
    contentColor: Color,
    onClick: () -> Unit,
    tile: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            tile()
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp, bottom = 2.dp)
                        .width(1.5.dp)
                        .weight(1f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(contentColor.copy(alpha = 0.1f))
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 18.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun VxLogoTile(
    iconRes: Int,
    imageUrl: String?,
    contentColor: Color,
    accentColor: Color
) {
    val context = LocalContext.current
    if (!imageUrl.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(VxTileShape)
                .border(1.dp, contentColor.copy(alpha = 0.08f), VxTileShape),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        Box(
            modifier = Modifier.size(46.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                colorFilter = ColorFilter.tint(accentColor.copy(alpha = 0.9f))
            )
        }
    }
}

@Composable
private fun VxEntryTitle(text: String, contentColor: Color) {
    BasicText(
        text,
        style = TextStyle(
            color = contentColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 20.sp
        ),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun VxEntrySubtitle(text: String, contentColor: Color) {
    BasicText(
        text,
        style = TextStyle(contentColor.copy(alpha = 0.78f), 13.sp, FontWeight.Medium, lineHeight = 17.sp),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun VxEntryMeta(text: String, contentColor: Color) {
    BasicText(
        text,
        style = TextStyle(contentColor.copy(alpha = 0.52f), 12.sp, lineHeight = 16.sp),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun VxStatusChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.13f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        BasicText(
            text,
            style = TextStyle(color, 11.sp, FontWeight.SemiBold),
            maxLines = 1
        )
    }
}

@Composable
private fun VxEntryMenu(
    entityLabel: String,
    contentColor: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    Box {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .clickable { showMenu = true }
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_more),
                contentDescription = "$entityLabel options",
                modifier = Modifier.size(15.dp),
                colorFilter = ColorFilter.tint(contentColor.copy(alpha = 0.55f))
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                leadingIcon = {
                    Image(
                        painter = painterResource(R.drawable.ic_vx_edit),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        colorFilter = ColorFilter.tint(Color(0xFF374151))
                    )
                },
                text = { BasicText("Edit", style = TextStyle(Color(0xFF1F2937), 13.sp, FontWeight.Medium)) },
                onClick = {
                    showMenu = false
                    onEdit()
                }
            )
            DropdownMenuItem(
                leadingIcon = {
                    Image(
                        painter = painterResource(R.drawable.ic_vx_trash),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        colorFilter = ColorFilter.tint(Color(0xFFDC2626))
                    )
                },
                text = { BasicText("Delete", style = TextStyle(Color(0xFFDC2626), 13.sp, FontWeight.Medium)) },
                onClick = {
                    showMenu = false
                    showConfirm = true
                }
            )
        }
    }

    if (showConfirm) {
        VxDeleteConfirmDialog(
            entityLabel = entityLabel,
            onConfirm = {
                showConfirm = false
                onDelete()
            },
            onDismiss = { showConfirm = false }
        )
    }
}

@Composable
private fun VxDeleteConfirmDialog(
    entityLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .padding(20.dp)
        ) {
            BasicText(
                "Delete $entityLabel?",
                style = TextStyle(Color(0xFF111827), 16.sp, FontWeight.Bold)
            )
            Spacer(Modifier.height(6.dp))
            BasicText(
                "This will permanently remove it from your profile.",
                style = TextStyle(Color(0xFF6B7280), 13.sp, lineHeight = 18.sp)
            )
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFFF3F4F6))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    BasicText(
                        "Cancel",
                        style = TextStyle(Color(0xFF374151), 13.sp, FontWeight.SemiBold)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFFDC2626))
                        .clickable(onClick = onConfirm)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    BasicText(
                        "Delete",
                        style = TextStyle(Color.White, 13.sp, FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

@Composable
private fun VxOutlineButton(
    label: String,
    contentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .border(1.2.dp, contentColor.copy(alpha = 0.32f), RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(
            label,
            style = TextStyle(contentColor.copy(alpha = 0.85f), 12.5.sp, FontWeight.SemiBold),
            maxLines = 1
        )
        Image(
            painter = painterResource(R.drawable.ic_vx_arrow_up_right),
            contentDescription = null,
            modifier = Modifier.size(11.dp),
            colorFilter = ColorFilter.tint(contentColor.copy(alpha = 0.7f))
        )
    }
}

@Composable
private fun VxExpandableText(
    text: String,
    contentColor: Color,
    accentColor: Color,
    collapsedLines: Int = 3
) {
    var expanded by remember(text) { mutableStateOf(false) }
    val needsToggle = text.length > VX_SEE_MORE_THRESHOLD || text.contains('\n')
    Column {
        BasicText(
            text,
            style = TextStyle(contentColor.copy(alpha = 0.74f), 13.sp, lineHeight = 19.sp),
            maxLines = if (expanded) Int.MAX_VALUE else collapsedLines,
            overflow = TextOverflow.Ellipsis
        )
        if (needsToggle) {
            BasicText(
                if (expanded) "See less" else "See more",
                style = TextStyle(accentColor, 12.5.sp, FontWeight.SemiBold),
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { expanded = !expanded }
                    .padding(top = 4.dp, bottom = 2.dp)
            )
        }
    }
}

@Composable
private fun VxSkillsLine(
    skills: List<String>,
    contentColor: Color
) {
    if (skills.isEmpty()) return
    val shown = skills.take(4).joinToString(" · ")
    val extra = skills.size - 4
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.ic_vx_sparkles),
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            colorFilter = ColorFilter.tint(contentColor.copy(alpha = 0.55f))
        )
        BasicText(
            "Skills: $shown" + if (extra > 0) " +$extra" else "",
            style = TextStyle(contentColor.copy(alpha = 0.66f), 12.sp, FontWeight.Medium, lineHeight = 16.sp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun VxEmptyState(
    iconRes: Int,
    title: String,
    subtitle: String,
    actionLabel: String,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(contentColor.copy(alpha = 0.03f))
            .padding(horizontal = 18.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(30.dp),
            colorFilter = ColorFilter.tint(accentColor.copy(alpha = 0.9f))
        )
        Spacer(Modifier.height(12.dp))
        BasicText(
            title,
            style = TextStyle(contentColor, 14.5.sp, FontWeight.SemiBold)
        )
        Spacer(Modifier.height(4.dp))
        BasicText(
            subtitle,
            style = TextStyle(contentColor.copy(alpha = 0.58f), 12.sp, lineHeight = 17.sp),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        if (isOwner) {
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(accentColor.copy(alpha = 0.12f))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_vx_plus),
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    colorFilter = ColorFilter.tint(accentColor)
                )
                BasicText(
                    actionLabel,
                    style = TextStyle(accentColor, 12.5.sp, FontWeight.SemiBold)
                )
            }
        }
    }
}

// ==================== Date helpers ====================

private fun vxFormatDate(dateString: String): String {
    return try {
        val date = LocalDate.parse(dateString.take(10))
        date.format(DateTimeFormatter.ofPattern("MMM yyyy"))
    } catch (e: Exception) {
        dateString.take(10)
    }
}

private fun vxRangeText(startDate: String, endDate: String?, isCurrent: Boolean): String {
    val start = vxFormatDate(startDate)
    val end = if (isCurrent) "Present" else endDate?.let { vxFormatDate(it) }.orEmpty()
    return listOf(start, end).filter { it.isNotBlank() }.joinToString(" – ")
}

private fun vxDurationText(startDate: String, endDate: String?, isCurrent: Boolean): String {
    return try {
        val formatter = DateTimeFormatter.ISO_DATE
        val start = LocalDate.parse(startDate.take(10), formatter)
        val end = if (isCurrent) LocalDate.now()
        else endDate?.let { LocalDate.parse(it.take(10), formatter) } ?: return ""

        val period = java.time.Period.between(start, end)
        val years = period.years
        val months = period.months

        buildString {
            if (years > 0) {
                append("$years yr")
                if (years > 1) append("s")
            }
            if (months > 0) {
                if (years > 0) append(" ")
                append("$months mo")
                if (months > 1) append("s")
            }
            if (years == 0 && months == 0) {
                append("less than 1 mo")
            }
        }
    } catch (e: Exception) {
        ""
    }
}

private fun vxIsExpired(expiryDate: String?): Boolean {
    if (expiryDate.isNullOrEmpty()) return false
    return try {
        LocalDate.parse(expiryDate.take(10)).isBefore(LocalDate.now())
    } catch (e: Exception) {
        false
    }
}

// Vivid-but-readable semantic colors, tuned per theme brightness.
private fun vxPositiveColor(contentColor: Color): Color =
    if (contentColor.luminance() > 0.5f) Color(0xFF4ADE80) else Color(0xFF15803D)

private fun vxNegativeColor(contentColor: Color): Color =
    if (contentColor.luminance() > 0.5f) Color(0xFFF87171) else Color(0xFFDC2626)

// ==================== Experience ====================

@Composable
fun ExperienceSection(
    experiences: List<Experience>,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    onAddExperience: () -> Unit = {},
    onEditExperience: (Experience) -> Unit = {},
    onViewExperience: (Experience) -> Unit = {},
    onDeleteExperience: (Experience) -> Unit = {}
) {
    var showAll by remember { mutableStateOf(false) }
    val visible = if (showAll) experiences else experiences.take(VX_COLLAPSED_ENTRIES)

    VxSectionCard(
        title = "Experience",
        count = experiences.size,
        iconRes = R.drawable.ic_vx_briefcase,
        backdrop = backdrop,
        contentColor = contentColor,
        accentColor = accentColor,
        isOwner = isOwner,
        onAdd = onAddExperience
    ) {
        if (experiences.isEmpty()) {
            VxEmptyState(
                iconRes = R.drawable.ic_vx_briefcase,
                title = "Build your career story",
                subtitle = if (isOwner) "Add roles, internships, freelance work, and the skills you used." else "This profile has not shared work experience yet.",
                actionLabel = "Add experience",
                contentColor = contentColor,
                accentColor = accentColor,
                isOwner = isOwner,
                onAction = onAddExperience
            )
        } else {
            Column {
                visible.forEachIndexed { index, exp ->
                    VxExperienceEntry(
                        experience = exp,
                        isLast = index == visible.lastIndex,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isOwner = isOwner,
                        onEdit = { onEditExperience(exp) },
                        onView = { onViewExperience(exp) },
                        onDelete = { onDeleteExperience(exp) }
                    )
                }
                if (experiences.size > VX_COLLAPSED_ENTRIES) {
                    VxShowAllFooter(
                        expanded = showAll,
                        totalCount = experiences.size,
                        noun = if (experiences.size == 1) "experience" else "experiences",
                        contentColor = contentColor,
                        onToggle = { showAll = !showAll }
                    )
                }
            }
        }
    }
}

@Composable
private fun VxExperienceEntry(
    experience: Experience,
    isLast: Boolean,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    onEdit: () -> Unit,
    onView: () -> Unit,
    onDelete: () -> Unit
) {
    val dateLine = remember(experience.startDate, experience.endDate, experience.isCurrent) {
        val range = vxRangeText(experience.startDate, experience.endDate, experience.isCurrent)
        val duration = vxDurationText(experience.startDate, experience.endDate, experience.isCurrent)
        listOf(range, duration).filter { it.isNotBlank() }.joinToString(" · ")
    }
    val subtitle = listOf(experience.company, experience.type)
        .filter { it.isNotBlank() }
        .joinToString(" · ")

    VxTimelineEntry(
        isLast = isLast,
        contentColor = contentColor,
        onClick = onView,
        tile = {
            VxLogoTile(
                iconRes = R.drawable.ic_vx_briefcase,
                imageUrl = experience.logo,
                contentColor = contentColor,
                accentColor = accentColor
            )
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(Modifier.weight(1f)) {
                    VxEntryTitle(experience.title, contentColor)
                }
                if (experience.isCurrent) {
                    VxStatusChip("Current", accentColor)
                }
                if (isOwner) {
                    VxEntryMenu("experience", contentColor, onEdit, onDelete)
                }
            }
            if (subtitle.isNotBlank()) {
                VxEntrySubtitle(subtitle, contentColor)
            }
            if (dateLine.isNotBlank()) {
                VxEntryMeta(dateLine, contentColor)
            }
            experience.location?.takeIf { it.isNotBlank() }?.let { loc ->
                VxEntryMeta(loc, contentColor)
            }
            experience.description?.takeIf { it.isNotBlank() }?.let { desc ->
                Spacer(Modifier.height(4.dp))
                VxExpandableText(desc, contentColor, accentColor)
            }
            if (experience.skills.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                VxSkillsLine(experience.skills, contentColor)
            }
        }
    }
}

// ==================== Education ====================

@Composable
fun EducationSection(
    education: List<Education>,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    onAddEducation: () -> Unit = {},
    onEditEducation: (Education) -> Unit = {},
    onViewEducation: (Education) -> Unit = {},
    onDeleteEducation: (Education) -> Unit = {}
) {
    var showAll by remember { mutableStateOf(false) }
    val visible = if (showAll) education else education.take(VX_COLLAPSED_ENTRIES)

    VxSectionCard(
        title = "Education",
        count = education.size,
        iconRes = R.drawable.ic_vx_graduation,
        backdrop = backdrop,
        contentColor = contentColor,
        accentColor = accentColor,
        isOwner = isOwner,
        onAdd = onAddEducation
    ) {
        if (education.isEmpty()) {
            VxEmptyState(
                iconRes = R.drawable.ic_vx_graduation,
                title = "Add your academic path",
                subtitle = if (isOwner) "Show schools, degrees, fields of study, grades, and societies." else "This profile has not shared education details yet.",
                actionLabel = "Add education",
                contentColor = contentColor,
                accentColor = accentColor,
                isOwner = isOwner,
                onAction = onAddEducation
            )
        } else {
            Column {
                visible.forEachIndexed { index, edu ->
                    VxEducationEntry(
                        education = edu,
                        isLast = index == visible.lastIndex,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isOwner = isOwner,
                        onEdit = { onEditEducation(edu) },
                        onView = { onViewEducation(edu) },
                        onDelete = { onDeleteEducation(edu) }
                    )
                }
                if (education.size > VX_COLLAPSED_ENTRIES) {
                    VxShowAllFooter(
                        expanded = showAll,
                        totalCount = education.size,
                        noun = "schools",
                        contentColor = contentColor,
                        onToggle = { showAll = !showAll }
                    )
                }
            }
        }
    }
}

@Composable
private fun VxEducationEntry(
    education: Education,
    isLast: Boolean,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    onEdit: () -> Unit,
    onView: () -> Unit,
    onDelete: () -> Unit
) {
    val programText = listOf(education.degree, education.fieldOfStudy)
        .filter { it.isNotBlank() }
        .joinToString(", ")
    val dateLine = remember(education.startDate, education.endDate, education.isCurrent) {
        vxRangeText(education.startDate, education.endDate, education.isCurrent)
    }

    VxTimelineEntry(
        isLast = isLast,
        contentColor = contentColor,
        onClick = onView,
        tile = {
            VxLogoTile(
                iconRes = R.drawable.ic_vx_graduation,
                imageUrl = education.logo,
                contentColor = contentColor,
                accentColor = accentColor
            )
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(Modifier.weight(1f)) {
                    VxEntryTitle(education.school, contentColor)
                }
                if (education.isCurrent) {
                    VxStatusChip("Studying", accentColor)
                }
                if (isOwner) {
                    VxEntryMenu("education", contentColor, onEdit, onDelete)
                }
            }
            if (programText.isNotBlank()) {
                VxEntrySubtitle(programText, contentColor)
            }
            if (dateLine.isNotBlank()) {
                VxEntryMeta(dateLine, contentColor)
            }
            education.grade?.takeIf { it.isNotBlank() }?.let { grade ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_vx_star),
                        contentDescription = null,
                        modifier = Modifier.size(11.dp),
                        colorFilter = ColorFilter.tint(contentColor.copy(alpha = 0.5f))
                    )
                    VxEntryMeta("Grade: $grade", contentColor)
                }
            }
            education.description?.takeIf { it.isNotBlank() }?.let { desc ->
                Spacer(Modifier.height(4.dp))
                VxExpandableText(desc, contentColor, accentColor)
            }
            education.activities?.takeIf { it.isNotBlank() }?.let { activities ->
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_vx_users),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(12.dp),
                        colorFilter = ColorFilter.tint(contentColor.copy(alpha = 0.55f))
                    )
                    BasicText(
                        "Activities: $activities",
                        style = TextStyle(contentColor.copy(alpha = 0.66f), 12.sp, lineHeight = 17.sp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ==================== Licenses & Certifications ====================

@Composable
fun CertificatesSection(
    certificates: List<Certificate>,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    onAddCertificate: () -> Unit = {},
    onEditCertificate: (Certificate) -> Unit = {},
    onViewCertificate: (Certificate) -> Unit = {},
    onDeleteCertificate: (Certificate) -> Unit = {}
) {
    var showAll by remember { mutableStateOf(false) }
    val visible = if (showAll) certificates else certificates.take(VX_COLLAPSED_ENTRIES)

    VxSectionCard(
        title = "Licenses & Certifications",
        count = certificates.size,
        iconRes = R.drawable.ic_vx_certificate,
        backdrop = backdrop,
        contentColor = contentColor,
        accentColor = accentColor,
        isOwner = isOwner,
        onAdd = onAddCertificate
    ) {
        if (certificates.isEmpty()) {
            VxEmptyState(
                iconRes = R.drawable.ic_vx_certificate,
                title = "Show verified learning",
                subtitle = if (isOwner) "Add certifications, license numbers, and credential links." else "This profile has not shared licenses or certifications yet.",
                actionLabel = "Add certification",
                contentColor = contentColor,
                accentColor = accentColor,
                isOwner = isOwner,
                onAction = onAddCertificate
            )
        } else {
            Column {
                visible.forEachIndexed { index, cert ->
                    VxCertificateEntry(
                        certificate = cert,
                        isLast = index == visible.lastIndex,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isOwner = isOwner,
                        onEdit = { onEditCertificate(cert) },
                        onView = { onViewCertificate(cert) },
                        onDelete = { onDeleteCertificate(cert) }
                    )
                }
                if (certificates.size > VX_COLLAPSED_ENTRIES) {
                    VxShowAllFooter(
                        expanded = showAll,
                        totalCount = certificates.size,
                        noun = "certifications",
                        contentColor = contentColor,
                        onToggle = { showAll = !showAll }
                    )
                }
            }
        }
    }
}

@Composable
private fun VxCertificateEntry(
    certificate: Certificate,
    isLast: Boolean,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    onEdit: () -> Unit,
    onView: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val expired = !certificate.doesNotExpire && vxIsExpired(certificate.expiryDate)
    val credentialUrl = certificate.credentialUrl?.takeIf { it.isNotBlank() }
    val hasImage = credentialUrl?.let { isImageUrl(it) } ?: false
    val dateLine = buildList {
        add("Issued ${vxFormatDate(certificate.issueDate)}")
        when {
            expired -> {}
            certificate.doesNotExpire -> add("No expiry")
            certificate.expiryDate != null -> add("Expires ${vxFormatDate(certificate.expiryDate)}")
        }
    }.joinToString(" · ")

    VxTimelineEntry(
        isLast = isLast,
        contentColor = contentColor,
        onClick = onView,
        tile = {
            VxLogoTile(
                iconRes = R.drawable.ic_vx_certificate,
                imageUrl = if (hasImage) credentialUrl else null,
                contentColor = contentColor,
                accentColor = accentColor
            )
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(Modifier.weight(1f)) {
                    VxEntryTitle(certificate.name, contentColor)
                }
                if (expired) {
                    VxStatusChip("Expired", vxNegativeColor(contentColor))
                }
                if (isOwner) {
                    VxEntryMenu("certification", contentColor, onEdit, onDelete)
                }
            }
            VxEntrySubtitle(certificate.issuingOrg, contentColor)
            if (dateLine.isNotBlank()) {
                VxEntryMeta(dateLine, contentColor)
            }
            certificate.credentialId?.takeIf { it.isNotBlank() }?.let { id ->
                VxEntryMeta("Credential ID $id", contentColor)
            }
            credentialUrl?.let { url ->
                Spacer(Modifier.height(7.dp))
                VxOutlineButton(
                    label = "Show credential",
                    contentColor = contentColor,
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                )
            }
        }
    }
}

// ==================== Achievements ====================

@Composable
fun AchievementsSection(
    achievements: List<Achievement>,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    onAddAchievement: () -> Unit = {},
    onEditAchievement: (Achievement) -> Unit = {},
    onViewAchievement: (Achievement) -> Unit = {},
    onDeleteAchievement: (Achievement) -> Unit = {}
) {
    var showAll by remember { mutableStateOf(false) }
    val visible = if (showAll) achievements else achievements.take(VX_COLLAPSED_ENTRIES)

    VxSectionCard(
        title = "Achievements",
        count = achievements.size,
        iconRes = R.drawable.ic_vx_trophy,
        backdrop = backdrop,
        contentColor = contentColor,
        accentColor = accentColor,
        isOwner = isOwner,
        onAdd = onAddAchievement
    ) {
        if (achievements.isEmpty()) {
            VxEmptyState(
                iconRes = R.drawable.ic_vx_trophy,
                title = "Celebrate the wins",
                subtitle = if (isOwner) "Add hackathons, awards, scholarships, and recognitions with proof links." else "This profile has not shared achievements yet.",
                actionLabel = "Add achievement",
                contentColor = contentColor,
                accentColor = accentColor,
                isOwner = isOwner,
                onAction = onAddAchievement
            )
        } else {
            Column {
                visible.forEachIndexed { index, achievement ->
                    VxAchievementEntry(
                        achievement = achievement,
                        isLast = index == visible.lastIndex,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isOwner = isOwner,
                        onEdit = { onEditAchievement(achievement) },
                        onView = { onViewAchievement(achievement) },
                        onDelete = { onDeleteAchievement(achievement) }
                    )
                }
                if (achievements.size > VX_COLLAPSED_ENTRIES) {
                    VxShowAllFooter(
                        expanded = showAll,
                        totalCount = achievements.size,
                        noun = "achievements",
                        contentColor = contentColor,
                        onToggle = { showAll = !showAll }
                    )
                }
            }
        }
    }
}

private fun vxAchievementIcon(type: String): Int = when (type.lowercase()) {
    "hackathon" -> R.drawable.ic_vx_target
    "competition" -> R.drawable.ic_vx_trophy
    "award" -> R.drawable.ic_vx_star
    "scholarship" -> R.drawable.ic_vx_gift
    "recognition" -> R.drawable.ic_vx_sparkles
    else -> R.drawable.ic_vx_trophy
}

@Composable
private fun VxAchievementEntry(
    achievement: Achievement,
    isLast: Boolean,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    onEdit: () -> Unit,
    onView: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val proofUrl = achievement.certificateUrl?.takeIf { it.isNotBlank() }
    val hasImage = proofUrl?.let { isImageUrl(it) } ?: false
    val typeLabel = achievement.type
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

    VxTimelineEntry(
        isLast = isLast,
        contentColor = contentColor,
        onClick = onView,
        tile = {
            VxLogoTile(
                iconRes = vxAchievementIcon(achievement.type),
                imageUrl = if (hasImage) proofUrl else null,
                contentColor = contentColor,
                accentColor = accentColor
            )
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(Modifier.weight(1f)) {
                    VxEntryTitle(achievement.title, contentColor)
                }
                VxStatusChip(typeLabel, accentColor)
                if (isOwner) {
                    VxEntryMenu("achievement", contentColor, onEdit, onDelete)
                }
            }
            VxEntrySubtitle(achievement.organization, contentColor)
            VxEntryMeta(vxFormatDate(achievement.date), contentColor)
            achievement.description?.takeIf { it.isNotBlank() }?.let { desc ->
                Spacer(Modifier.height(4.dp))
                VxExpandableText(desc, contentColor, accentColor)
            }
            proofUrl?.let { url ->
                Spacer(Modifier.height(7.dp))
                VxOutlineButton(
                    label = "Show proof",
                    contentColor = contentColor,
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                )
            }
        }
    }
}
