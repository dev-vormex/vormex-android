package com.kyant.backdrop.catalog.linkedin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.ui.BasicText
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import kotlin.math.roundToInt

private val TalentNodeWidth = 224.dp
private val TalentNodeHeight = 124.dp

@Composable
fun TalentEngineScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onNavigateBack: () -> Unit
) {
    var selectedMode by rememberSaveable { mutableStateOf(TalentEngineMode.TALENT) }
    var selectedTrack by rememberSaveable { mutableStateOf(TalentEngineTrack.DEVELOPERS) }
    var showFullscreen by rememberSaveable { mutableStateOf(false) }
    val graph = remember(selectedMode, selectedTrack) {
        buildTalentEngineGraph(selectedMode, selectedTrack)
    }
    var selectedNodeId by rememberSaveable(selectedMode, selectedTrack) {
        mutableStateOf<String?>(null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
    ) {
        SettingsHeader(
            title = "Talent Engine",
            contentColor = contentColor,
            onBack = onNavigateBack
        )

        TalentEngineTopPanel(
            graph = graph,
            contentColor = contentColor,
            accentColor = accentColor,
            backdrop = backdrop,
            selectedMode = selectedMode,
            selectedTrack = selectedTrack,
            onModeChange = { selectedMode = it },
            onTrackChange = { selectedTrack = it }
        )

        Spacer(Modifier.height(12.dp))

        TalentEngineCanvas(
            graph = graph,
            selectedNodeId = selectedNodeId,
            contentColor = contentColor,
            accentColor = accentColor,
            onSelectNode = { selectedNodeId = it },
            onOpenFullscreen = { showFullscreen = true },
            modifier = Modifier.weight(1f)
        )
    }

    if (showFullscreen) {
        TalentEngineFullscreenDialog(
            graph = graph,
            selectedNodeId = selectedNodeId,
            contentColor = contentColor,
            accentColor = accentColor,
            onSelectNode = { selectedNodeId = it },
            onDismiss = { showFullscreen = false }
        )
    }
}

@Composable
private fun TalentEngineTopPanel(
    graph: TalentEngineGraph,
    contentColor: Color,
    accentColor: Color,
    backdrop: LayerBackdrop,
    selectedMode: TalentEngineMode,
    selectedTrack: TalentEngineTrack,
    onModeChange: (TalentEngineMode) -> Unit,
    onTrackChange: (TalentEngineTrack) -> Unit
) {
    val surfaceColor = contentColor.copy(alpha = if (contentColor.luminance() > 0.5f) 0.08f else 0.06f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(18.dp) },
                effects = {
                    vibrancy()
                    blur(8.dp.toPx())
                    lens(14.dp.toPx(), 24.dp.toPx())
                },
                onDrawSurface = { drawRect(surfaceColor) }
            )
            .border(1.dp, contentColor.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    graph.title,
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                BasicText(
                    graph.subtitle,
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.66f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                TalentStatPill("Acceptance", graph.acceptanceRate, contentColor, accentColor)
                Spacer(Modifier.height(6.dp))
                TalentStatPill("Cohort", graph.cohortSize, contentColor, toneColor(TalentEngineTone.GREEN, accentColor))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TalentEngineSelector(
                label = "Mode",
                options = TalentEngineMode.values().map { it.label },
                selectedIndex = TalentEngineMode.values().indexOf(selectedMode),
                contentColor = contentColor,
                accentColor = accentColor,
                modifier = Modifier.weight(1f),
                onSelect = { onModeChange(TalentEngineMode.values()[it]) }
            )
            TalentEngineSelector(
                label = "Track",
                options = TalentEngineTrack.values().map { it.label },
                selectedIndex = TalentEngineTrack.values().indexOf(selectedTrack),
                contentColor = contentColor,
                accentColor = accentColor,
                modifier = Modifier.weight(1f),
                onSelect = { onTrackChange(TalentEngineTrack.values()[it]) }
            )
        }
    }
}

@Composable
private fun TalentEngineCanvas(
    graph: TalentEngineGraph,
    selectedNodeId: String?,
    contentColor: Color,
    accentColor: Color,
    onSelectNode: (String) -> Unit,
    onOpenFullscreen: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val rawBounds = remember(graph) { calculateTalentGraphBounds(graph) }
    val layerBounds = remember(rawBounds) {
        TalentGraphBounds(0f, 0f, rawBounds.width, rawBounds.height)
    }
    val nodeOffsets = remember { mutableStateMapOf<String, Offset>() }
    var zoom by remember { mutableFloatStateOf(1f) }
    var offsetPx by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(graph.mode, graph.track) {
        nodeOffsets.clear()
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(contentColor.copy(alpha = if (contentColor.luminance() > 0.5f) 0.045f else 0.035f))
            .border(1.dp, contentColor.copy(alpha = 0.10f), RoundedCornerShape(22.dp))
    ) {
        val containerWidth = maxWidth.value
        val containerHeight = maxHeight.value

        fun fitCanvas() {
            val viewport = calculateTalentFitViewport(containerWidth, containerHeight, layerBounds)
            zoom = viewport.zoom
            offsetPx = with(density) {
                Offset(viewport.offsetX.dp.toPx(), viewport.offsetY.dp.toPx())
            }
        }

        LaunchedEffect(graph.mode, graph.track, containerWidth, containerHeight) {
            fitCanvas()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(graph.mode, graph.track, zoom) {
                    detectTransformGestures { centroid, pan, gestureZoom, _ ->
                        val oldZoom = zoom
                        val newZoom = clampTalentCanvasZoom(oldZoom * gestureZoom)
                        val scaleChange = newZoom / oldZoom
                        offsetPx = Offset(
                            x = centroid.x - (centroid.x - offsetPx.x) * scaleChange + pan.x,
                            y = centroid.y - (centroid.y - offsetPx.y) * scaleChange + pan.y
                        )
                        zoom = newZoom
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0f, 0f)
                        translationX = offsetPx.x
                        translationY = offsetPx.y
                        scaleX = zoom
                        scaleY = zoom
                    }
                    .size(layerBounds.width.dp, layerBounds.height.dp)
            ) {
                TalentEngineEdges(
                    graph = graph,
                    rawBounds = rawBounds,
                    nodeOffsets = nodeOffsets,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    modifier = Modifier.fillMaxSize()
                )

                graph.stickyNotes.forEach { note ->
                    TalentStickyNoteCard(
                        note = note,
                        rawBounds = rawBounds,
                        contentColor = contentColor
                    )
                }

                graph.nodes.forEach { node ->
                    TalentEngineNodeCard(
                        node = node,
                        rawBounds = rawBounds,
                        customOffset = nodeOffsets[node.id] ?: Offset.Zero,
                        isSelected = node.id == selectedNodeId,
                        zoom = zoom,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onOffsetChange = { nodeOffsets[node.id] = it },
                        onSelect = { onSelectNode(node.id) }
                    )
                }
            }
        }

        TalentCanvasControls(
            zoom = zoom,
            contentColor = contentColor,
            accentColor = accentColor,
            onZoomIn = { zoom = clampTalentCanvasZoom(zoom * 1.16f) },
            onZoomOut = { zoom = clampTalentCanvasZoom(zoom / 1.16f) },
            onFit = { fitCanvas() },
            onReset = {
                nodeOffsets.clear()
                fitCanvas()
            },
            onOpenFullscreen = onOpenFullscreen,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
        )

    }
}

@Composable
private fun TalentEngineFullscreenDialog(
    graph: TalentEngineGraph,
    selectedNodeId: String?,
    contentColor: Color,
    accentColor: Color,
    onSelectNode: (String) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)
    val backgroundColor = if (contentColor.luminance() > 0.5f) {
        Color(0xFF07111F)
    } else {
        Color(0xFFF8FAFC)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(8.dp)
        ) {
            TalentEngineCanvas(
                graph = graph,
                selectedNodeId = selectedNodeId,
                contentColor = contentColor,
                accentColor = accentColor,
                onSelectNode = onSelectNode,
                modifier = Modifier.fillMaxSize()
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.10f))
                    .border(1.dp, contentColor.copy(alpha = 0.12f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close fullscreen",
                    tint = contentColor
                )
            }
        }
    }
}

@Composable
private fun TalentEngineEdges(
    graph: TalentEngineGraph,
    rawBounds: TalentGraphBounds,
    nodeOffsets: Map<String, Offset>,
    contentColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val nodeById = graph.nodes.associateBy { it.id }
    Canvas(modifier = modifier) {
        val gridSpacing = 72.dp.toPx()
        var gridX = 0f
        while (gridX <= size.width) {
            drawLine(
                color = contentColor.copy(alpha = 0.035f),
                start = Offset(gridX, 0f),
                end = Offset(gridX, size.height),
                strokeWidth = 1f
            )
            gridX += gridSpacing
        }
        var gridY = 0f
        while (gridY <= size.height) {
            drawLine(
                color = contentColor.copy(alpha = 0.035f),
                start = Offset(0f, gridY),
                end = Offset(size.width, gridY),
                strokeWidth = 1f
            )
            gridY += gridSpacing
        }

        graph.edges.forEach { edge ->
            val from = nodeById[edge.fromNodeId] ?: return@forEach
            val to = nodeById[edge.toNodeId] ?: return@forEach
            val fromOffset = nodeOffsets[from.id] ?: Offset.Zero
            val toOffset = nodeOffsets[to.id] ?: Offset.Zero
            val fromX = (from.position.x - rawBounds.left + fromOffset.x).dp.toPx()
            val fromY = (from.position.y - rawBounds.top + fromOffset.y).dp.toPx()
            val toX = (to.position.x - rawBounds.left + toOffset.x).dp.toPx()
            val toY = (to.position.y - rawBounds.top + toOffset.y).dp.toPx()
            val start = Offset(fromX + TalentNodeWidth.toPx(), fromY + TalentNodeHeight.toPx() * 0.5f)
            val end = Offset(toX, toY + TalentNodeHeight.toPx() * 0.5f)
            val bend = ((end.x - start.x) * 0.52f).coerceAtLeast(44.dp.toPx())
            val path = Path().apply {
                moveTo(start.x, start.y)
                cubicTo(start.x + bend, start.y, end.x - bend, end.y, end.x, end.y)
            }
            drawPath(
                path = path,
                color = accentColor.copy(alpha = 0.42f),
                style = Stroke(width = 3.dp.toPx())
            )
            drawCircle(
                color = accentColor.copy(alpha = 0.76f),
                radius = 4.dp.toPx(),
                center = end
            )
        }
    }
}

@Composable
private fun TalentEngineNodeCard(
    node: TalentEngineNode,
    rawBounds: TalentGraphBounds,
    customOffset: Offset,
    isSelected: Boolean,
    zoom: Float,
    contentColor: Color,
    accentColor: Color,
    onOffsetChange: (Offset) -> Unit,
    onSelect: () -> Unit
) {
    val nodeAccent = toneColor(node.tone, accentColor)
    val surfaceColor = if (contentColor.luminance() > 0.5f) {
        Color(0xFF101826).copy(alpha = 0.94f)
    } else {
        Color.White.copy(alpha = 0.94f)
    }
    val x = node.position.x - rawBounds.left + customOffset.x
    val y = node.position.y - rawBounds.top + customOffset.y
    Box(
        modifier = Modifier
            .offset { IntOffset(x.dp.roundToPx(), y.dp.roundToPx()) }
            .size(TalentNodeWidth, TalentNodeHeight)
            .clip(RoundedCornerShape(18.dp))
            .background(surfaceColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) nodeAccent else contentColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onSelect)
            .pointerInput(node.id, zoom) {
                detectDragGestures(
                    onDragStart = { onSelect() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onOffsetChange(
                            customOffset + Offset(
                                x = dragAmount.x / density / zoom,
                                y = dragAmount.y / density / zoom
                            )
                        )
                    }
                )
            }
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(nodeAccent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(nodeAccent)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    BasicText(
                        node.title,
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    BasicText(
                        node.stage.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = TextStyle(
                            color = nodeAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            BasicText(
                node.subtitle,
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.74f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TalentMiniMetric(node.metrics.firstOrNull()?.value ?: node.status, nodeAccent, contentColor)
                TalentMiniMetric(node.status, contentColor.copy(alpha = 0.18f), contentColor)
            }
        }
    }
}

@Composable
private fun TalentStickyNoteCard(
    note: TalentEngineStickyNote,
    rawBounds: TalentGraphBounds,
    contentColor: Color
) {
    val x = note.position.x - rawBounds.left
    val y = note.position.y - rawBounds.top
    Column(
        modifier = Modifier
            .offset { IntOffset(x.dp.roundToPx(), y.dp.roundToPx()) }
            .width(note.width.dp)
            .heightIn(min = note.estimatedHeight.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFF3B0).copy(alpha = 0.96f))
            .border(1.dp, Color(0xFFE9C75F), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        BasicText(
            note.title,
            style = TextStyle(
                color = Color(0xFF3B2E06),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(Modifier.height(4.dp))
        BasicText(
            note.body,
            style = TextStyle(
                color = Color(0xFF3B2E06).copy(alpha = 0.84f),
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        )
    }
}

@Composable
private fun TalentCanvasControls(
    zoom: Float,
    contentColor: Color,
    accentColor: Color,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onFit: () -> Unit,
    onReset: () -> Unit,
    onOpenFullscreen: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(contentColor.copy(alpha = if (contentColor.luminance() > 0.5f) 0.10f else 0.07f))
            .border(1.dp, contentColor.copy(alpha = 0.10f), RoundedCornerShape(999.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TalentIconControl(Icons.Outlined.ZoomOut, contentColor, onZoomOut)
        TalentIconControl(Icons.Outlined.ZoomIn, contentColor, onZoomIn)
        TalentIconControl(Icons.Outlined.CenterFocusStrong, contentColor, onFit)
        TalentIconControl(Icons.Outlined.RestartAlt, contentColor, onReset)
        if (onOpenFullscreen != null) {
            TalentIconControl(Icons.Outlined.OpenInFull, contentColor, onOpenFullscreen)
        }
        BasicText(
            "${(zoom * 100f).roundToInt()}%",
            modifier = Modifier.padding(horizontal = 10.dp),
            style = TextStyle(
                color = accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun TalentIconControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentColor: Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(34.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun TalentEngineSelector(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    contentColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit
) {
    Column(modifier = modifier) {
        BasicText(
            label,
            style = TextStyle(
                color = contentColor.copy(alpha = 0.52f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(999.dp))
                .background(contentColor.copy(alpha = 0.07f))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            options.forEachIndexed { index, option ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (selected) accentColor else Color.Transparent)
                        .clickable { onSelect(index) }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        option,
                        style = TextStyle(
                            color = if (selected) Color.White else contentColor.copy(alpha = 0.72f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun TalentStatPill(
    label: String,
    value: String,
    contentColor: Color,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(accentColor.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(
            label,
            style = TextStyle(
                color = contentColor.copy(alpha = 0.62f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        )
        Spacer(Modifier.width(6.dp))
        BasicText(
            value,
            style = TextStyle(
                color = accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun TalentMiniMetric(
    label: String,
    backgroundColor: Color,
    contentColor: Color
) {
    BasicText(
        label,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(backgroundColor.copy(alpha = if (backgroundColor.luminance() > 0.6f) 0.20f else 1f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        style = TextStyle(
            color = contentColor.copy(alpha = 0.78f),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun TalentStatusBadge(
    status: String,
    accentColor: Color,
    contentColor: Color
) {
    BasicText(
        status,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(accentColor.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        style = TextStyle(
            color = accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun TalentDetailMetric(
    metric: TalentEngineMetric,
    contentColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val metricColor = toneColor(metric.tone, accentColor)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(metricColor.copy(alpha = 0.12f))
            .padding(10.dp)
    ) {
        BasicText(
            metric.label,
            style = TextStyle(contentColor.copy(alpha = 0.56f), 10.sp, FontWeight.Medium),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        BasicText(
            metric.value,
            style = TextStyle(metricColor, 13.sp, FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TalentInfoLine(
    label: String,
    value: String,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.widthIn(min = 0.dp)
    ) {
        BasicText(
            label,
            style = TextStyle(contentColor.copy(alpha = 0.52f), 10.sp, FontWeight.Medium),
            maxLines = 1
        )
        BasicText(
            value,
            style = TextStyle(contentColor.copy(alpha = 0.78f), 12.sp, FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun toneColor(tone: TalentEngineTone, accentColor: Color): Color {
    return when (tone) {
        TalentEngineTone.BLUE -> accentColor
        TalentEngineTone.GREEN -> Color(0xFF1FA97A)
        TalentEngineTone.AMBER -> Color(0xFFE49A2A)
        TalentEngineTone.PURPLE -> Color(0xFF8B5CF6)
        TalentEngineTone.ROSE -> Color(0xFFE05272)
    }
}
