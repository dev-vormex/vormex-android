package com.kyant.backdrop.catalog.linkedin.crossedpaths

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.kyant.backdrop.catalog.maps.OsmdroidVormexMapRenderer
import com.kyant.backdrop.catalog.network.models.LiveProximityPerson
import com.kyant.backdrop.catalog.network.models.ProximityHistoryItem
import com.kyant.backdrop.catalog.network.models.ProximityLiveData
import com.kyant.backdrop.catalog.network.models.ProximityMarker
import kotlinx.coroutines.delay
import kotlin.random.Random

private val CrossedPurple = Color(0xFF6D5DFB)
private val CrossedIndigo = Color(0xFF4338CA)
private val CrossedMint = Color(0xFF20C997)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrossedPathsScreen(onBack: () -> Unit, onOpenProfile: (String) -> Unit = {}, vm: CrossedPathsViewModel = viewModel()) {
    val context = LocalContext.current
    val state by vm.state.collectAsState()
    var tab by remember { mutableStateOf("live") }
    var mapFailed by remember { mutableStateOf(false) }
    var pendingStart by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf("recent") }
    var relationshipFilter by remember { mutableStateOf("all") }
    var mapExpanded by remember { mutableStateOf(false) }
    var demoProfileId by remember { mutableStateOf<String?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result[Manifest.permission.ACCESS_COARSE_LOCATION] == true || result[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            if (pendingStart) vm.start() else vm.loadLocationPreview()
        }
        pendingStart = false
    }
    fun startExplicitly() {
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (coarse || fine) vm.start() else {
            pendingStart = true
            permission.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
        }
    }
    val backendFilters = when (relationshipFilter) {
        "connected" -> setOf("connected")
        "pending" -> setOf("pending_sent", "pending_received")
        "new" -> setOf("none")
        else -> emptySet()
    }
    LaunchedEffect(Unit) {
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (coarse || fine) vm.loadLocationPreview()
        else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    LaunchedEffect(tab, query, sort, relationshipFilter) { vm.refresh(tab, query.trim(), sort, backendFilters) }
    LaunchedEffect(tab) {
        delay(Random.nextLong(1_000, 8_000))
        while (tab == "live") {
            vm.advanceDemoPreview()
            vm.refresh("live")
            delay(Random.nextLong(13_000, 23_000))
        }
    }
    val openPerson: (String) -> Unit = { userId ->
        if (userId.startsWith("demo-")) demoProfileId = userId else onOpenProfile(userId)
    }

    demoProfileId?.let { id ->
        val livePerson = state.live.people.firstOrNull { it.id == id }
        val historyPerson = state.history.items.firstOrNull { it.user.id == id }?.user
        DemoProfileScreen(
            name = livePerson?.name ?: historyPerson?.name ?: "Demo member",
            headline = livePerson?.headline ?: historyPerson?.headline ?: "Vormex member",
            college = livePerson?.college ?: historyPerson?.college,
            onBack = { demoProfileId = null },
        )
        return
    }
    if (mapExpanded) {
        FullScreenLiveMap(enrichedMarkers(state.live), onBack = { mapExpanded = false }, onPersonClick = openPerson)
        return
    }

    val background = MaterialTheme.colorScheme.background
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { _ ->
        Column(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(CrossedPurple.copy(alpha = .13f), background, background))
            ).windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
        ) {
            CrossedPathsHeader(onBack)
            if (state.loading && state.capabilities == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = CrossedPurple) }
                return@Column
            }
            if (state.capabilities?.flags?.entry != true) {
                UnavailableState(onBack)
                return@Column
            }
            state.summaries.firstOrNull()?.takeIf { state.capabilities?.flags?.summaryNotifications == true }?.let {
                SummaryBanner(it.summaryCount ?: 0) { vm.dismissSummary(it.id) }
            }
            CrossedPathsTabs(tab) { tab = it }
            AnimatedContent(targetState = tab, label = "crossed-paths-tab", modifier = Modifier.weight(1f)) { selected ->
                if (selected == "live") {
                    LiveContent(
                        state = state,
                        mapFailed = mapFailed,
                        onMapFailed = { mapFailed = true },
                        onExpandMap = { mapExpanded = true },
                        onPersonClick = openPerson,
                        pendingStart = pendingStart,
                        onStart = ::startExplicitly,
                        onStop = vm::stop,
                        onResume = vm::resume,
                        onLoadMore = vm::loadMore,
                    )
                } else {
                    HistoryContent(
                        period = selected,
                        query = query, onQuery = { query = it.take(80) }, sort = sort, onSort = { sort = it },
                        relationshipFilter = relationshipFilter, onFilter = { relationshipFilter = it },
                        items = state.history.items, hasMore = state.history.nextCursor != null,
                        loadingMore = state.loadingMore, vm = vm,
                        onPersonClick = openPerson,
                    )
                }
            }
        }
    }
}

@Composable
private fun CrossedPathsHeader(onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        FilledIconButton(onClick = onBack, colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .9f))) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Crossed Paths", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Discover the people your day brought close", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(shape = CircleShape, color = CrossedPurple.copy(alpha = .12f)) {
            Icon(Icons.Outlined.Route, null, Modifier.padding(10.dp), tint = CrossedPurple)
        }
    }
}

@Composable
private fun CrossedPathsTabs(selected: String, onSelect: (String) -> Unit) {
    Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .65f)) {
        Row(Modifier.padding(4.dp)) {
            listOf("live" to "Live now", "today" to "Today", "seven_days" to "7 days").forEach { (key, title) ->
                val active = selected == key
                Surface(
                    Modifier.weight(1f).clickable { onSelect(key) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (active) MaterialTheme.colorScheme.surface else Color.Transparent,
                    shadowElevation = if (active) 2.dp else 0.dp,
                ) {
                    Row(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        if (key == "live") Box(Modifier.size(7.dp).background(if (active) CrossedMint else MaterialTheme.colorScheme.onSurfaceVariant, CircleShape))
                        if (key == "live") Spacer(Modifier.width(6.dp))
                        Text(title, style = MaterialTheme.typography.labelLarge, color = if (active) CrossedPurple else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveContent(
    state: CrossedPathsUiState, mapFailed: Boolean, onMapFailed: () -> Unit, pendingStart: Boolean,
    onExpandMap: () -> Unit, onPersonClick: (String) -> Unit,
    onStart: () -> Unit, onStop: () -> Unit, onResume: () -> Unit, onLoadMore: () -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        if (state.capabilities?.flags?.eventMode == true) item {
            EventModeCard(state.demoEventActive || (state.session != null && state.eventServiceRunning), !state.demoEventActive && state.session != null && !state.eventServiceRunning,
                state.loading || pendingStart, state.error, state.actionMessage, onStart, onStop, onResume)
        }
        if (state.capabilities?.flags?.liveMap == true && !mapFailed) item {
            Box(Modifier.fillMaxWidth().height(340.dp).padding(horizontal = 16.dp, vertical = 8.dp).clip(RoundedCornerShape(28.dp))) {
                OsmdroidVormexMapRenderer(state.capabilities.tile.url.ifBlank { null } ?: "https://tile.openstreetmap.org/{z}/{x}/{y}.png")
                    .Render(enrichedMarkers(state.live), Modifier.fillMaxSize(), onMarkerClick = onPersonClick) { onMapFailed() }
                Surface(Modifier.align(Alignment.TopStart).padding(12.dp), shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface.copy(alpha = .93f)) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(CrossedMint, CircleShape)); Spacer(Modifier.width(7.dp))
                        Text(if (state.live.nearbyCountCapped) "200+ nearby" else "${state.live.nearbyCount} nearby", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Text("© OpenStreetMap contributors", Modifier.align(Alignment.BottomEnd).background(MaterialTheme.colorScheme.surface.copy(alpha = .8f), RoundedCornerShape(topStart = 8.dp)).padding(5.dp), style = MaterialTheme.typography.labelSmall)
                FilledIconButton(onClick = onExpandMap, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .93f))) { Icon(Icons.Outlined.Fullscreen, "Open full-screen map") }
            }
        }
        if (mapFailed) item { InlineNotice("The map is unavailable. Your nearby list is still up to date.") }
        item {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("People around you", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(state.live.totalLabel.takeIf { it.isNotBlank() } ?: state.live.nearbyCount.toString(), color = CrossedPurple, style = MaterialTheme.typography.labelLarge)
            }
        }
        if (state.capabilities?.flags?.liveList != true) item { EmptyState(Icons.Outlined.Radar, "Live list paused", "Nearby people are temporarily unavailable.") }
        else if (state.live.people.isEmpty()) item { EmptyState(Icons.Outlined.Explore, "No one nearby yet", "Start Event Mode and keep moving—new crossings will appear here.") }
        else items(state.live.people, key = { it.id }) { LivePersonCard(it) { onPersonClick(it.id) } }
        if (state.live.nextCursor != null) item { LoadMoreButton(state.loadingMore, onLoadMore) }
    }
}

@Composable
private fun EventModeCard(active: Boolean, canResume: Boolean, busy: Boolean, error: String?, message: String?, onStart: () -> Unit, onStop: () -> Unit, onResume: () -> Unit) {
    val title = if (active) "You're discoverable" else if (canResume) "Session paused" else "Event Mode"
    val subtitle = if (active) "Scanning for opted-in members within 500 m" else if (canResume) "Resume to continue finding nearby people" else "Turn it on at meetups, campuses, and events"
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = CrossedIndigo)) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = Color.White.copy(alpha = .14f)) { Icon(if (active) Icons.Outlined.Radar else Icons.Outlined.NearMe, null, Modifier.padding(11.dp), tint = Color.White) }
                Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); if (active) { Spacer(Modifier.width(8.dp)); Box(Modifier.size(8.dp).background(CrossedMint, CircleShape)) } }
                    Text(subtitle, color = Color.White.copy(alpha = .72f), style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(18.dp))
            if (active) OutlinedButton(onClick = onStop, Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .35f))) { Text("Stop Event Mode") }
            else if (canResume) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onResume, enabled = !busy, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = CrossedIndigo)) { Text("Resume") }
                OutlinedButton(onClick = onStop, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .35f))) { Text("End") }
            } else Button(onClick = onStart, enabled = !busy, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = CrossedIndigo)) {
                if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = CrossedIndigo) else Icon(Icons.Outlined.Bolt, null)
                Spacer(Modifier.width(8.dp)); Text(if (busy) "Starting…" else "Start Event Mode")
            }
            (error ?: message)?.let { status ->
                Spacer(Modifier.height(10.dp))
                Text(status, color = if (error != null) Color(0xFFFFD1D1) else Color.White.copy(alpha = .78f), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun LivePersonCard(person: LiveProximityPerson, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp).clickable(onClick = onClick), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatar(person.profileImage, person.name)
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(person.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(listOfNotNull(person.headline, person.college).joinToString(" · ").ifBlank { "Vormex member" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Surface(shape = RoundedCornerShape(50), color = CrossedPurple.copy(alpha = .1f)) { Text(person.distanceBucket.replace('_', ' '), Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = CrossedPurple, style = MaterialTheme.typography.labelMedium) }
            Spacer(Modifier.width(4.dp)); Icon(Icons.Outlined.ChevronRight, "Open profile", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HistoryContent(period: String, query: String, onQuery: (String) -> Unit, sort: String, onSort: (String) -> Unit, relationshipFilter: String, onFilter: (String) -> Unit, items: List<ProximityHistoryItem>, hasMore: Boolean, loadingMore: Boolean, vm: CrossedPathsViewModel, onPersonClick: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp)) {
        item { HistoryOverview(period, items.size) }
        item { HistoryControls(query, onQuery, sort, onSort, relationshipFilter, onFilter) }
        if (items.isEmpty()) item { EmptyState(Icons.Outlined.Timeline, "No crossings here yet", "People you cross paths with will stay visible for up to 7 days.") }
        else items(items, key = { it.encounterId }) { HistoryPersonCard(it, vm) { onPersonClick(it.user.id) } }
        if (hasMore) item { LoadMoreButton(loadingMore, vm::loadMore) }
    }
}

@Composable
private fun HistoryOverview(period: String, count: Int) {
    val label = if (period == "today") "Today's crossings" else "Your last 7 days"
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
        Row(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(CrossedIndigo, CrossedPurple))).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, color = Color.White.copy(alpha = .78f), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(3.dp))
                Text(if (count == 0) "Ready for new connections" else "$count ${if (count == 1) "person" else "people"} found", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text("Private, approximate, and visible only to you", color = Color.White.copy(alpha = .68f), style = MaterialTheme.typography.bodySmall)
            }
            Surface(shape = CircleShape, color = Color.White.copy(alpha = .14f)) { Icon(Icons.Outlined.HistoryToggleOff, null, Modifier.padding(14.dp).size(27.dp), tint = Color.White) }
        }
    }
}

@Composable
private fun HistoryControls(query: String, onQuery: (String) -> Unit, sort: String, onSort: (String) -> Unit, relationshipFilter: String, onFilter: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Find a crossing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        TextField(query, onQuery, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Name, college, or skill") }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, trailingIcon = { if (query.isNotEmpty()) IconButton({ onQuery("") }) { Icon(Icons.Outlined.Close, "Clear search") } }, shape = RoundedCornerShape(18.dp), colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("all" to "All", "new" to "New", "connected" to "Connected", "pending" to "Pending").forEach { (key, label) -> ModernFilterPill(label, relationshipFilter == key) { onFilter(key) } }
            ModernFilterPill(if (sort == "recent") "Recent first" else "Longest first", true, Icons.Outlined.SwapVert) { onSort(if (sort == "recent") "duration" else "recent") }
        }
    }
}

@Composable
private fun ModernFilterPill(label: String, selected: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, onClick: () -> Unit) {
    Surface(Modifier.clickable(onClick = onClick), shape = RoundedCornerShape(50), color = if (selected) CrossedPurple else MaterialTheme.colorScheme.surface, border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            icon?.let { Icon(it, null, Modifier.size(17.dp), tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurface); Spacer(Modifier.width(5.dp)) }
            Text(label, color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun FullScreenLiveMap(markers: List<com.kyant.backdrop.catalog.network.models.ProximityMarker>, onBack: () -> Unit, onPersonClick: (String) -> Unit) {
    BackHandler(onBack = onBack)
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        OsmdroidVormexMapRenderer("https://tile.openstreetmap.org/{z}/{x}/{y}.png")
            .Render(markers, Modifier.fillMaxSize(), onMarkerClick = onPersonClick) {}
        Row(
            Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledIconButton(onClick = onBack, colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .95f))) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Close map") }
            Spacer(Modifier.width(10.dp))
            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface.copy(alpha = .95f)) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(Color(0xFF4338CA), CircleShape)); Spacer(Modifier.width(6.dp)); Text("You")
                    Spacer(Modifier.width(14.dp)); Surface(Modifier.size(22.dp), CircleShape, CrossedPurple) { Box(contentAlignment = Alignment.Center) { Text("A", color = Color.White, style = MaterialTheme.typography.labelSmall) } }; Spacer(Modifier.width(6.dp)); Text("Nearby profiles")
                }
            }
        }
        Surface(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .95f)) {
            Text("Live presence refreshes automatically · Tap an avatar to open its profile", Modifier.padding(horizontal = 16.dp, vertical = 12.dp), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DemoProfileScreen(name: String, headline: String, college: String?, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(CrossedPurple.copy(alpha = .22f), MaterialTheme.colorScheme.background))).windowInsetsPadding(WindowInsets.safeDrawing)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledIconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
            Spacer(Modifier.width(12.dp)); Text("Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(Modifier.size(104.dp), shape = CircleShape, color = CrossedPurple.copy(alpha = .14f)) { Box(contentAlignment = Alignment.Center) { Text(name.firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.displaySmall, color = CrossedPurple, fontWeight = FontWeight.Bold) } }
            Spacer(Modifier.height(18.dp)); Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp)); Text(headline, color = MaterialTheme.colorScheme.onSurfaceVariant)
            college?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Spacer(Modifier.height(14.dp)); Surface(shape = RoundedCornerShape(50), color = CrossedMint.copy(alpha = .14f)) { Text("Demo profile", Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = CrossedMint, fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.height(28.dp))
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(20.dp)) { Text("About", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); Text("This sample profile demonstrates navigation from Crossed Paths. Real members open their complete Vormex profile with connections, experience, skills, and posts.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        }
    }
}

@Composable
private fun HistoryPersonCard(item: ProximityHistoryItem, vm: CrossedPathsViewModel, onClick: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clickable(onClick = onClick), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfileAvatar(item.user.profileImage, item.user.name); Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.user.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(item.user.headline ?: item.user.college ?: "Vormex member", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box { IconButton(onClick = { menuOpen = true }) { Icon(Icons.Outlined.MoreHoriz, "More actions") }; DropdownMenu(menuOpen, { menuOpen = false }) {
                    DropdownMenuItem({ Text("Hide") }, { menuOpen = false; vm.hide(item.user.id) }, leadingIcon = { Icon(Icons.Outlined.VisibilityOff, null) })
                    DropdownMenuItem({ Text("Remove") }, { menuOpen = false; vm.remove(item.user.id) }, leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null) })
                    if (item.actions.canBlock) DropdownMenuItem({ Text("Block") }, { menuOpen = false; vm.block(item.user.id) }, leadingIcon = { Icon(Icons.Outlined.Block, null) })
                } }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoPill(Icons.Outlined.LocationOn, item.areaLabel, Modifier.weight(1f)); InfoPill(Icons.Outlined.Schedule, formatDuration(item.accumulatedDurationSeconds))
            }
            if (item.actions.canConnect || item.actions.canMessage) { Spacer(Modifier.height(12.dp)); Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (item.actions.canConnect) Button({ vm.connect(item.user.id) }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = CrossedPurple)) { Icon(Icons.Outlined.PersonAdd, null, Modifier.size(18.dp)); Spacer(Modifier.width(7.dp)); Text("Connect") }
                if (item.actions.canMessage) OutlinedButton({ vm.message(item.user.id) }, Modifier.weight(1f)) { Icon(Icons.Outlined.ChatBubbleOutline, null, Modifier.size(18.dp)); Spacer(Modifier.width(7.dp)); Text("Message") }
            } }
        }
    }
}

@Composable private fun ProfileAvatar(url: String?, name: String) {
    Surface(Modifier.size(52.dp), shape = CircleShape, color = CrossedPurple.copy(alpha = .12f)) {
        if (!url.isNullOrBlank()) AsyncImage(url, name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Box(contentAlignment = Alignment.Center) { Text(name.trim().firstOrNull()?.uppercase() ?: "?", color = CrossedPurple, fontWeight = FontWeight.Bold) }
    }
}

@Composable private fun InfoPill(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f)) { Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, Modifier.size(16.dp), tint = CrossedPurple); Spacer(Modifier.width(5.dp)); Text(text, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) } }
}

@Composable private fun SummaryBanner(count: Int, onDismiss: () -> Unit) {
    Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), shape = RoundedCornerShape(18.dp), color = CrossedMint.copy(alpha = .12f)) { Row(Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Celebration, null, tint = CrossedMint); Spacer(Modifier.width(10.dp)); Text("Event complete · $count new ${if (count == 1) "crossing" else "crossings"}", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium); IconButton(onDismiss) { Icon(Icons.Outlined.Close, "Dismiss") } } }
}

@Composable private fun InlineNotice(text: String) { Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Map, null, tint = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.width(8.dp)); Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }

@Composable private fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String) { Column(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 42.dp), horizontalAlignment = Alignment.CenterHorizontally) { Surface(shape = CircleShape, color = CrossedPurple.copy(alpha = .1f)) { Icon(icon, null, Modifier.padding(18.dp).size(30.dp), tint = CrossedPurple) }; Spacer(Modifier.height(14.dp)); Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Spacer(Modifier.height(5.dp)); Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } }

@Composable private fun UnavailableState(onBack: () -> Unit) { Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Surface(shape = CircleShape, color = CrossedPurple.copy(alpha = .1f)) { Icon(Icons.Outlined.LockClock, null, Modifier.padding(22.dp).size(36.dp), tint = CrossedPurple) }; Spacer(Modifier.height(18.dp)); Text("Crossed Paths isn't available yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); Text("We're rolling this experience out gradually. Check back soon.", color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(22.dp)); Button(onBack, colors = ButtonDefaults.buttonColors(containerColor = CrossedPurple)) { Text("Go back") } } }

@Composable private fun LoadMoreButton(loading: Boolean, onClick: () -> Unit) { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { OutlinedButton(onClick, enabled = !loading) { if (loading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp) else Text("Load more") } } }

private fun formatDuration(seconds: Int): String = when { seconds >= 3600 -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"; seconds >= 60 -> "${seconds / 60}m"; else -> "${seconds}s" }

private fun enrichedMarkers(live: ProximityLiveData): List<ProximityMarker> = live.markers.map { marker ->
    val person = live.people.firstOrNull { it.id == marker.userId }
    if (person == null) marker else marker.copy(displayName = person.name, profileImage = person.profileImage)
}
