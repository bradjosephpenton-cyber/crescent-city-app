package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Simple model representing license selection options
data class OptionItem(val title: String, val price: String, val desc: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_scaffold"),
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    CrescentBeatsApp(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrescentBeatsApp(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playbackProgress by viewModel.playbackProgress.collectAsState()
    val currentMillis by viewModel.currentMillis.collectAsState()
    val isLoadingTrack by viewModel.isLoadingTrack.collectAsState()
    val favorites by viewModel.favoritesFlow.collectAsState()
    val estimates by viewModel.estimatesFlow.collectAsState()
    val visualizerBars by viewModel.visualizerBars.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0 = Beats, 1 = Licensing Estimator, 2 = Saved Estimates
    var showPlayerOverlay by remember { mutableStateOf(false) }
    var showBioDialog by remember { mutableStateOf(false) }
    var showCustomBeatDialog by remember { mutableStateOf(false) }

    // Floating UI notifications
    val snackbarHostState = remember { SnackbarHostState() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main Screen Scrollable Layout
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Screen Header: Brand Spotlight banner
            BeatHeader(
                onBioClick = { showBioDialog = true },
                onCustomRequestClick = { showCustomBeatDialog = true }
            )

            // Dynamic Tabs
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Beats Catalog", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                    icon = { Icon(Icons.Default.Album, contentDescription = "Beats Panel") },
                    modifier = Modifier.testTag("tab_beats")
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("License Builder", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                    icon = { Icon(Icons.Default.MonetizationOn, contentDescription = "Licensing Panel") },
                    modifier = Modifier.testTag("tab_licensing")
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = { 
                        BadgedBox(
                            badge = {
                                if (estimates.isNotEmpty()) {
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                        Text(estimates.size.toString(), color = Color.Black)
                                    }
                                }
                            }
                        ) {
                            Text("Saved Estim.", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    },
                    icon = { Icon(Icons.Default.Bookmarks, contentDescription = "Saved Panel") },
                    modifier = Modifier.testTag("tab_saved")
                )
            }

            // Tab Contents
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activeTab) {
                    0 -> BeatsCatalogView(
                        tracks = viewModel.tracks,
                        currentTrackId = currentTrack.id,
                        isPlaying = isPlaying,
                        favorites = favorites,
                        onTrackPlay = { viewModel.playTrack(it) },
                        onFavoriteToggle = { viewModel.toggleFavorite(it) },
                        onSendToEstimator = { track ->
                            activeTab = 1
                        }
                    )
                    1 -> LicensingBuilderView(
                        tracks = viewModel.tracks,
                        preselectedTrackId = currentTrack.id,
                        onSaveEstimate = { trackId, trackName, licType, base, vocal, radio, video, total, notes ->
                            viewModel.saveLicensingEstimate(
                                trackId, trackName, licType, base, vocal, radio, video, total, notes
                            )
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Estimate for '$trackName' saved to Vault!")
                            }
                            activeTab = 2
                        }
                    )
                    2 -> SavedEstimatesView(
                        estimates = estimates,
                        onDeleteEstimate = { viewModel.deleteEstimate(it) },
                        onOpenShareEstimate = { estimate ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Sharing details for: ${estimate.trackName}")
                            }
                        }
                    )
                }
            }

            // Extra height spacing at bottom to avoid overlap by player bar safely
            Spacer(modifier = Modifier.height(72.dp))
        }

        // Floating Bottom Music Progress Player Bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            FloatingPlayerBar(
                track = currentTrack,
                isPlaying = isPlaying,
                isLoading = isLoadingTrack,
                progress = playbackProgress,
                visualizerBars = visualizerBars,
                onPlayPauseClick = { viewModel.togglePlayPause() },
                onBarClick = { showPlayerOverlay = true },
                onSkipNext = { viewModel.playNextTrack() },
                onFavoriteToggle = { viewModel.toggleFavorite(currentTrack.id) },
                isFavorite = favorites.any { it.id == currentTrack.id }
            )
        }

        // Full Screen Player Spotlight Overlay Sheets
        AnimatedVisibility(
            visible = showPlayerOverlay,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)
            ) + fadeOut()
        ) {
            FullPlayerOverlay(
                track = currentTrack,
                isPlaying = isPlaying,
                isLoading = isLoadingTrack,
                progress = playbackProgress,
                currentMillis = currentMillis,
                visualizerBars = visualizerBars,
                onPlayPauseClick = { viewModel.togglePlayPause() },
                onClose = { showPlayerOverlay = false },
                onSkipNext = { viewModel.playNextTrack() },
                onSkipPrev = { viewModel.playPreviousTrack() },
                onSeek = { viewModel.seekToFraction(it) },
                isFavorite = favorites.any { it.id == currentTrack.id },
                onFavoriteToggle = { viewModel.toggleFavorite(currentTrack.id) }
            )
        }

        // Snackbar Alerts Hub
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        )

        // dialog: Creator Bio
        if (showBioDialog) {
            ArtistBioDialog(
                onClose = { showBioDialog = false }
            )
        }

        // dialog: Custom Beats request specification builder
        if (showCustomBeatDialog) {
            CustomBeatRequestDialog(
                onClose = { showCustomBeatDialog = false },
                onSubmit = { requestInfo ->
                    coroutineScope.launch {
                        showCustomBeatDialog = false
                        snackbarHostState.showSnackbar("Custom request submitted to Crescent Studio!")
                    }
                }
            )
        }
    }
}

// ==========================================
// COMPOSABLE VISUAL MODULES
// ==========================================

@Composable
fun BeatHeader(
    onBioClick: () -> Unit,
    onCustomRequestClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .shadow(4.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data("https://cloudimages.soundclick.com/208/pro/album/1/1337361_1225.webp")
                .crossfade(true)
                .build(),
            contentDescription = "Background Studio",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.35f
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("https://cloudimages.soundclick.com/146/images/c/band/crescentcityinstrumentals.webp")
                        .crossfade(true)
                        .build(),
                    contentDescription = "Crescent Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Crescent Instrumentals",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = "Verified Artist",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = "New Orleans Independent • 1,200+ original instrumentals",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onBioClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("header_explore_bio"),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = "Bio", modifier = Modifier.size(12.dp))
                            Text("Artist Bio", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = onCustomRequestClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.Black
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("header_custom_request"),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = "Tune", modifier = Modifier.size(12.dp))
                            Text("Request Beat", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------
// CATALOG VIEW
// ------------------------------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BeatsCatalogView(
    tracks: List<Track>,
    currentTrackId: String,
    isPlaying: Boolean,
    favorites: List<FavoriteTrack>,
    onTrackPlay: (Track) -> Unit,
    onFavoriteToggle: (String) -> Unit,
    onSendToEstimator: (Track) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "FEATURED TRACKS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.2.sp
            )
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "Syncing with SoundClick",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("beats_list"),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(start = 4.dp, top = 4.dp, end = 4.dp, bottom = 16.dp)
        ) {
            items(tracks, key = { it.id }) { track ->
                val isSelected = track.id == currentTrackId
                val isTrackFavorite = favorites.any { it.id == track.id }

                val infiniteTransition = rememberInfiniteTransition()
                val spinAngle by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(4000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    )
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("track_item_${track.id}")
                        .clickable { onTrackPlay(track) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.surface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(track.imageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = track.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.4f))
                            ) {
                                Icon(
                                    imageVector = if (isSelected && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Status Play",
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.Center)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = track.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Beats License from $${String.format("%.2f", track.price)}",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                maxLines = 1
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                track.tags.take(3).forEach { tag ->
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(tag, fontSize = 9.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = track.durationText,
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(end = 4.dp)
                            )

                            IconButton(
                                onClick = { onFavoriteToggle(track.id) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("track_favorite_${track.id}")
                            ) {
                                Icon(
                                    imageVector = if (isTrackFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Toggle Save Track",
                                    tint = if (isTrackFavorite) MaterialTheme.colorScheme.primary else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------
// ESTIMATOR & BUILDER VIEW
// ------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensingBuilderView(
    tracks: List<Track>,
    preselectedTrackId: String,
    onSaveEstimate: (String, String, String, Double, Boolean, Boolean, Boolean, Double, String) -> Unit
) {
    var selectedTrackIndex by remember { mutableStateOf(tracks.indexOfFirst { it.id == preselectedTrackId }.coerceAtLeast(0)) }
    val activeTrack = tracks[selectedTrackIndex]

    var selectedLicenseOption by remember { mutableStateOf(1) }

    var voxelRecordingSelected by remember { mutableStateOf(true) }
    var radioBroadcastSelected by remember { mutableStateOf(false) }
    var videoPlacementsSelected by remember { mutableStateOf(false) }

    var customInquiryNotes by remember { mutableStateOf("") }

    val basePrice = when (selectedLicenseOption) {
        0 -> 2.00   
        1 -> 25.00  
        2 -> 45.00  
        3 -> 150.00 
        else -> 25.00
    }

    val vocalAddonCost = if (voxelRecordingSelected) 15.00 else 0.00
    val radioAddonCost = if (radioBroadcastSelected) 20.00 else 0.00
    val videoAddonCost = if (videoPlacementsSelected) 35.00 else 0.00

    val subtotalPrice = basePrice + vocalAddonCost + radioAddonCost + videoAddonCost
    val finalTotal = if (selectedLicenseOption == 3) subtotalPrice * 0.90 else subtotalPrice 

    var showTrackDropdown by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 24.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "1. SELECT A TRACK FOR YOUR USE CASE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .clickable { showTrackDropdown = true }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("select_track_trigger")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MusicNote, contentDescription = "Active Track", tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = activeTrack.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Expand Catalog", tint = Color.Gray)
                        }

                        DropdownMenu(
                            expanded = showTrackDropdown,
                            onDismissRequest = { showTrackDropdown = false },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            tracks.forEachIndexed { i, track ->
                                DropdownMenuItem(
                                    text = { Text(track.title, color = Color.White, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        selectedTrackIndex = i
                                        showTrackDropdown = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Filled.Album, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    },
                                    modifier = Modifier.testTag("dropdown_item_${track.id}")
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "2. SELECT LICENSE RIGHTS TIER",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            VerticalLicenseSelector(
                selectedIndex = selectedLicenseOption,
                onSelectIndex = { selectedLicenseOption = it }
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "3. ADD OPTIONAL CUSTOM UPGRADES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    CustomUpgradeToggle(
                        title = "Vocal Clearance & Tracking",
                        priceText = "+$15.00",
                        description = "Required to lay down vocal singing/rapping over the beat for sync releases.",
                        checked = voxelRecordingSelected,
                        onCheckedChange = { voxelRecordingSelected = it },
                        modifier = Modifier.testTag("addon_vocals")
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.2f))

                    CustomUpgradeToggle(
                        title = "Commercial Radio Placements",
                        priceText = "+$20.00",
                        description = "Clearance for broadcasting track over FM/AM sync radio stations worldwide.",
                        checked = radioBroadcastSelected,
                        onCheckedChange = { radioBroadcastSelected = it },
                        modifier = Modifier.testTag("addon_radio")
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.2f))

                    CustomUpgradeToggle(
                        title = "Professional Film Music Videos",
                        priceText = "+$35.00",
                        description = "Licensable right to feature audio in sync with YouTube, television, or film productions.",
                        checked = videoPlacementsSelected,
                        onCheckedChange = { videoPlacementsSelected = it },
                        modifier = Modifier.testTag("addon_video")
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = customInquiryNotes,
                onValueChange = { customInquiryNotes = it },
                label = { Text("Client Custom Note/Request Details", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .testTag("estimator_notes"),
                maxLines = 3
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("LICENSE ESTIMATE DETAIL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        if (selectedLicenseOption == 3) {
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("10% DISC.", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    ReceiptRow("Base Tier Rights", "$${String.format("%.2f", basePrice)}")
                    if (voxelRecordingSelected) ReceiptRow("Vocal Tracking Addon", "+$15.00")
                    if (radioBroadcastSelected) ReceiptRow("Radio clearance Addon", "+$20.00")
                    if (videoPlacementsSelected) ReceiptRow("Film/Video clearance Addon", "+$35.00")

                    Divider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ultimate Total Estimate", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            text = "$${String.format("%.2f", finalTotal)}",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            modifier = Modifier.testTag("estimator_total_price")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val licType = when (selectedLicenseOption) {
                                0 -> "MP3 Personal"
                                1 -> "MP3 Independent Lease"
                                2 -> "WAV Premium Lease"
                                3 -> "Exclusive Trackout Buyout"
                                else -> "Custom Lease"
                            }
                            onSaveEstimate(
                                activeTrack.id,
                                activeTrack.title,
                                licType,
                                basePrice,
                                voxelRecordingSelected,
                                radioBroadcastSelected,
                                videoPlacementsSelected,
                                finalTotal,
                                customInquiryNotes
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_estimate_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SAVE ESTIMATE TO VAULT", fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptRow(label: String, valText: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(label, color = Color.Gray, fontSize = 13.sp)
        Text(valText, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
fun VerticalLicenseSelector(
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    val options = listOf(
        OptionItem("MP3 Personal Demo", "$2.00", "Includes basic MP3 preview version, suitable for personal evaluation. No commercial distribution."),
        OptionItem("MP3 Independent Lease", "$25.00", "Delivers clean MP3 beat, instant download. Clear for up to 10K streams, indie releases."),
        OptionItem("WAV Premium Lease", "$45.00", "Clear CD quality WAV files + MP3. Suitable for up to 100K streams, radio, and mix projects."),
        OptionItem("Exclusive Track-out Buyout", "$150.00", "Delivers high-quality separate track stems (WAV). Unlimited streaming and 100% full rights.")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = index == selectedIndex
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectIndex(index) }
                    .border(
                        width = if (isSelected) 2.dp else 0.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.04f) else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelectIndex(index) },
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("radio_license_$index")
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(option.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                            Text(option.price, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, fontSize = 15.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(option.desc, color = Color.Gray, fontSize = 11.sp, lineHeight = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun CustomUpgradeToggle(
    title: String,
    priceText: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(priceText, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(description, color = Color.Gray, fontSize = 11.sp, lineHeight = 14.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = MaterialTheme.colorScheme.primary)
        )
    }
}

// ------------------------------------------
// SAVED INQUIRIES VAULT VIEW
// ------------------------------------------
@Composable
fun SavedEstimatesView(
    estimates: List<SavedEstimate>,
    onDeleteEstimate: (SavedEstimate) -> Unit,
    onOpenShareEstimate: (SavedEstimate) -> Unit
) {
    if (estimates.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .testTag("empty_vault_state"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = "Empty estimates list",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "NO CONFIGURED ESTIMATES",
                fontSize = 14.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Use the Licensing Builder form, toggle custom rights add-ons, and draft your purchase estimate to save it offline here!",
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "YOUR SAVED LICENSE ESTIMATES (${estimates.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("estimates_list"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(estimates) { estimate ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("estimate_item_${estimate.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(estimate.trackName, fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Tier: ${estimate.licenseType}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "$${String.format("%.2f", estimate.totalEstimate)}",
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 16.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (estimate.vocalRecordingAddon) AddonPill("Vocals")
                                if (estimate.radioPlacementsAddon) AddonPill("Radio")
                                if (estimate.musicVideoAddon) AddonPill("Sync Video")
                                if (!estimate.vocalRecordingAddon && !estimate.radioPlacementsAddon && !estimate.musicVideoAddon) {
                                    AddonPill("Flat Lease Track")
                                }
                            }

                            if (estimate.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                        .padding(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Icon(Icons.Filled.Notes, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(estimate.notes, color = Color.LightGray, fontSize = 11.sp, lineHeight = 16.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val sdf = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
                                Text(
                                    text = sdf.format(Date(estimate.timestamp)),
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { onOpenShareEstimate(estimate) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(18.dp))
                                    }

                                    IconButton(
                                        onClick = { onDeleteEstimate(estimate) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("delete_estimate_${estimate.id}")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddonPill(text: String) {
    Box(
        modifier = Modifier
            .background(Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text, fontSize = 9.sp, color = Color.LightGray, fontWeight = FontWeight.Bold)
    }
}

// ------------------------------------------
// FLOATING MINI PLAYER BOTTOM BAR
// ------------------------------------------
@Composable
fun FloatingPlayerBar(
    track: Track,
    isPlaying: Boolean,
    isLoading: Boolean,
    progress: Float,
    visualizerBars: List<Float>,
    onPlayPauseClick: () -> Unit,
    onBarClick: () -> Unit,
    onSkipNext: () -> Unit,
    onFavoriteToggle: () -> Unit,
    isFavorite: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .shadow(16.dp, RoundedCornerShape(12.dp))
            .clickable { onBarClick() }
            .testTag("floating_player_bar"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LinearProgressIndicator(
                progress = progress,
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.TopCenter)
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(track.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Icon",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = track.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "New Orleans LA Studio",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.height(10.dp)
                        ) {
                            visualizerBars.take(5).forEach { barHeight ->
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .fillMaxHeight(barHeight)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier.testTag("floating_player_favorite")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Save track to favorites",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(34.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        IconButton(
                            onClick = onPlayPauseClick,
                            modifier = Modifier.testTag("floating_player_play_pause")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onSkipNext,
                        modifier = Modifier.testTag("floating_player_skip")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Skip track",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}


// ------------------------------------------
// EXPANDED FULL SCREEN PLAYER OVERLAY
// ------------------------------------------
@Composable
fun FullPlayerOverlay(
    track: Track,
    isPlaying: Boolean,
    isLoading: Boolean,
    progress: Float,
    currentMillis: Long,
    visualizerBars: List<Float>,
    onPlayPauseClick: () -> Unit,
    onClose: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrev: () -> Unit,
    onSeek: (Float) -> Unit,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit
) {
    val angleTransition = rememberInfiniteTransition()
    val angleSpin by angleTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("full_player_overlay")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                            Color(0xFF4F378B).copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = Offset(400f, 600f)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("full_player_close")
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimize Player", tint = Color.LightGray, modifier = Modifier.size(32.dp))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NOW PLAYING",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "504 Studio Monitor",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier.testTag("full_player_favorite_toggle")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Save track",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else Color.LightGray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(270.dp)
                        .shadow(32.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color(0xFF070708))
                        .border(6.dp, Color(0xFF1D222B), CircleShape)
                        .rotate(if (isPlaying) angleSpin else 0f)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(color = Color.Black)
                        val center = Offset(size.width / 2, size.height / 2)
                        drawCircle(color = Color.DarkGray.copy(alpha = 0.5f), radius = 110.dp.toPx(), center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
                        drawCircle(color = Color.DarkGray.copy(alpha = 0.5f), radius = 90.dp.toPx(), center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
                        drawCircle(color = Color.DarkGray.copy(alpha = 0.5f), radius = 70.dp.toPx(), center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
                    }

                    Box(
                        modifier = Modifier
                            .size(105.dp)
                            .clip(CircleShape)
                            .align(Alignment.Center)
                            .border(4.dp, Color.Black, CircleShape)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(track.imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = track.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color.Black)
                                .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                .align(Alignment.Center)
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = track.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Crescent City Instrumentals",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("HIP-HOP", fontSize = 9.sp, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = track.description,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier
                        .height(30.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    visualizerBars.forEach { barScale ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height((30 * barScale).dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                        )
                                    )
                                )
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Slider(
                    value = progress,
                    onValueChange = { onSeek(it) },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("full_player_seeker")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val currentSec = currentMillis / 1000
                    val currentFormatted = String.format("%d:%02d", currentSec / 60, currentSec % 60)
                    Text(
                        text = currentFormatted,
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )

                    Text(
                        text = track.durationText,
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onSkipPrev,
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("full_player_prev")
                ) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous Track", tint = Color.White, modifier = Modifier.size(36.dp))
                }

                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { onPlayPauseClick() }
                        .testTag("full_player_play_pause_fab"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(44.dp),
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Status action toggle",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("full_player_next")
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next Track", tint = Color.White, modifier = Modifier.size(36.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}


// ==========================================
// ARTIST BIOGRAPHY DIALOG
// ==========================================
@Composable
fun ArtistBioDialog(
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("bio_dialog"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NEW ORLEANS STUDIO BIO",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close dialog", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data("https://cloudimages.soundclick.com/816/pro/album/1/1337361_21068.jpg")
                            .crossfade(true)
                            .build(),
                        contentDescription = "New Orleans vibe image backdrop",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                )
                            )
                    )
                    Text(
                        text = "Crescent City Production Crew, New Orleans, LA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Based in New Orleans, Louisiana, Crescent City Instrumentals is an independent Hip-Hop crew that has been releasing beats on SoundClick since 2014. Spanning over 1,200 original tracks, their extensive catalog consists of signature hip-hop rhythmics that carry a relentless premium energy and radiant tone.\n\n" +
                            "Productions carry a dancefloor-ready, sync-ready, mainstream quality. The beats have earned 4 top-50 chart placements with the signature track \"Way Of Life 504 Edition\" charting at #18 on the global SoundClick rap charts, accumulating over 144K total plays worldwide.",
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("OK, UNDERSTOOD", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}


// ==========================================
// CUSTOM BEAT SPEC REQUEST BUILDER DIALOG
// ==========================================
@Composable
fun CustomBeatRequestDialog(
    onClose: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var genreState by remember { mutableStateOf("Hip-Hop / Southern Trap") }
    var tempoState by remember { mutableStateOf("94 BPM") }
    var vocalInspirationState by remember { mutableStateOf("Kanye / J. Cole Type Beat") }
    var clientContactState by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onClose
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("custom_request_dialog"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CUSTOM BEAT ORDER SPECS",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close dialog", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Request a bespoke, tailored beat handcrafted by Crescent Instrumentals. We'll consult and send you the custom WAV stem files.",
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = genreState,
                    onValueChange = { genreState = it },
                    label = { Text("Desired Genre Style", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("request_genre"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = tempoState,
                    onValueChange = { tempoState = it },
                    label = { Text("Desired Tempo (BPM)", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("request_tempo"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = vocalInspirationState,
                    onValueChange = { vocalInspirationState = it },
                    label = { Text("Vocal Inspiration (e.g. J. Cole / Kanye)", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("request_inspiration"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = clientContactState,
                    onValueChange = { clientContactState = it },
                    label = { Text("Your Artist Email", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("request_email"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val fullInfo = "Genre: $genreState, Tempo: $tempoState, Inspiration: $vocalInspirationState, Contact: $clientContactState"
                        onSubmit(fullInfo)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("submit_request_button"),
                    shape = RoundedCornerShape(8.dp),
                    enabled = clientContactState.isNotBlank()
                ) {
                    Text("SUBMIT BEAT SPEC INQUIRY", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
