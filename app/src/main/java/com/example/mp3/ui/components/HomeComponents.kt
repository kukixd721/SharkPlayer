package com.example.mp3.ui.components

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.example.mp3.*
import com.example.mp3.R
import com.example.mp3.ui.screens.PlayerSettings
import com.example.mp3.LocalStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    songList: List<Song>,
    player: Player?,
    favoriteIds: Set<Long>,
    onToggleFavoriteId: (Long) -> Unit,
    onSongClick: (Song) -> Unit,
    onSearchClick: () -> Unit,
    onEqualizerClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onUpdateTags: (Song, String, String, String, String, String, String, ByteArray?) -> Unit,
    settings: PlayerSettings,
    getAlbumArt: (String) -> ByteArray?
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    
    val currentMediaId by produceState<String?>(initialValue = player?.currentMediaItem?.mediaId, key1 = player?.currentMediaItem) {
        value = player?.currentMediaItem?.mediaId
    }

    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    
    val quadruple = when (hour) {
        in 6..12 -> Quadruple(strings.goodMorning, strings.morningSubtitle, Icons.Default.WbSunny, Color(0xFFFFB300))
        in 13..18 -> Quadruple(strings.goodAfternoon, strings.afternoonSubtitle, Icons.Default.WbCloudy, Color(0xFF64B5F6))
        in 19..22 -> Quadruple(strings.goodEvening, strings.eveningSubtitle, Icons.Default.NightsStay, Color(0xFF9575CD))
        else -> Quadruple(strings.helloNightOwl, strings.nightSubtitle, Icons.Default.Bedtime, Color(0xFFCE93D8))
    }
    val greeting = quadruple.first
    val subtitle = quadruple.second
    val icon = quadruple.third
    val iconColor = quadruple.fourth

    val quote = remember { strings.quotes.random() }
    
    var refreshTrigger by remember { mutableStateOf(0) }
    LaunchedEffect(player?.isPlaying) {
        if (player?.isPlaying == true) {
            while (true) {
                delay(10000)
                refreshTrigger++
            }
        }
    }
    
    val topSongs by produceState<List<Song>>(initialValue = emptyList(), songList, refreshTrigger) {
        value = withContext(Dispatchers.IO) {
            StatisticsManager.getTopSongs(context, songList, 5)
        }
    }
    val recentSongs by produceState<List<Song>>(initialValue = emptyList(), songList, currentMediaId) {
        value = withContext(Dispatchers.IO) {
            StatisticsManager.getRecentSongs(context, songList, 10)
        }
    }

    var showStatsDetails by remember { mutableStateOf(false) }
    var showMenuForSong by remember { mutableStateOf<Song?>(null) }
    var showTagEditor by remember { mutableStateOf<Song?>(null) }
    var songDetailsForEditor by remember { mutableStateOf<SongDetails?>(null) }

    LaunchedEffect(showTagEditor) {
        if (showTagEditor != null) {
            songDetailsForEditor = withContext(Dispatchers.IO) {
                com.example.mp3.getAudioMetadata(context, showTagEditor!!.data)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 80.dp)
    ) {
        // --- HEADER ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = iconColor
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = greeting,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = subtitle,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )

                // --- MOTIVATIONAL QUOTE (Right under title) ---
                Text(
                    text = "\"$quote\"",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 12.dp),
                    textAlign = TextAlign.Start
                )
            }
        }


        // --- QUICK ACTIONS ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                modifier = Modifier.weight(1f),
                title = strings.search,
                icon = Icons.Default.Search,
                color = MaterialTheme.colorScheme.primary,
                onClick = onSearchClick
            )
            QuickActionCard(
                modifier = Modifier.weight(1f),
                title = strings.downloads,
                icon = Icons.Default.Download,
                color = MaterialTheme.colorScheme.secondary,
                onClick = onDownloadClick
            )
            QuickActionCard(
                modifier = Modifier.weight(1f),
                title = strings.equalizer,
                icon = Icons.Default.GraphicEq,
                color = MaterialTheme.colorScheme.tertiary,
                onClick = onEqualizerClick
            )
        }

        Spacer(Modifier.height(24.dp)) // Espacio adicional para que no roce

        // --- FEATURED TOP SONG ---
        if (topSongs.isNotEmpty()) {
            val featuredSong = topSongs.first()
            val albumArt = remember(featuredSong.data) { getAlbumArt(featuredSong.data) }
            val topSongStats by produceState<StatisticsManager.DetailedSongStat?>(initialValue = null, featuredSong, refreshTrigger) {
                value = withContext(Dispatchers.IO) {
                    StatisticsManager.getTopSongsDetailed(context, songList, 1, "All Time").firstOrNull()
                }
            }
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(260.dp)
                    .clickable { onSongClick(featuredSong) },
                shape = RoundedCornerShape(32.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (albumArt != null) {
                        AsyncImage(
                            model = albumArt,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                                    )
                                )
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(80.dp), tint = Color.White.copy(alpha = 0.5f))
                        }
                    }
                    
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(24.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "TU CANCIÓN FAVORITA",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFFD700),
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = featuredSong.title,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = featuredSong.artist,
                            fontSize = 20.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        topSongStats?.let { stats ->
                            Text(
                                text = "${StatisticsManager.formatMillis(stats.timeMs)} escuchados • ${stats.plays} reproducciones",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD700).copy(alpha = 0.9f),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                    
                    // Floating Play Icon
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(24.dp)
                            .size(56.dp)
                            .background(Color(0xFFFFD700), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(32.dp))
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }

        // --- TOP SONGS (CARDS HORIZONTALES) ---
        val detailedTopSongs by produceState<List<StatisticsManager.DetailedSongStat>>(initialValue = emptyList(), songList, refreshTrigger) {
            value = withContext(Dispatchers.IO) {
                StatisticsManager.getTopSongsDetailed(context, songList, 5, "All Time")
            }
        }

        if (detailedTopSongs.size > 1) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    text = strings.yourTopFiveSongs,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
                
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(detailedTopSongs.drop(1)) { stat ->
                        Box(modifier = Modifier.width(220.dp)) { // Aumentado considerablemente
                            CompactAlbumCard(
                                song = stat.song,
                                onSongClick = { onSongClick(stat.song) },
                                getAlbumArt = getAlbumArt,
                                settings = settings,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp) // Forzamos altura para que los textos respiren
                            )
                            
                            // Badge de tiempo
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                            ) {
                                Text(
                                    text = StatisticsManager.formatMillis(stat.timeMs),
                                    fontSize = 11.sp, // Un pelín más grande
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- RECENTLY PLAYED ---
        if (recentSongs.isNotEmpty()) {
            Column(modifier = Modifier.padding(top = 28.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.recentlyAdded, // Assuming this is reused for recently played
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    recentSongs.take(5).forEach { song ->
                        SongItemRow(
                            song = song,
                            isSelected = currentMediaId == song.id.toString(),
                            onClick = { onSongClick(song) },
                            getAlbumArt = getAlbumArt,
                            settings = settings,
                            onMenuClick = { showMenuForSong = it }
                        )
                    }
                }
            }
        }

        // --- STATS OVERVIEW (Bottom) ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .clickable { showStatsDetails = true },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1.6f)) {
                    Text(
                        text = strings.listeningStats,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    val isPlaying = player?.isPlaying ?: false
                    val totalTimeStr by produceState(
                        initialValue = "...",
                        key1 = isPlaying,
                        key2 = player?.currentMediaItem,
                        key3 = refreshTrigger
                    ) {
                        value = withContext(Dispatchers.IO) {
                            StatisticsManager.getTotalTimeFormatted(context)
                        }
                    }

                    Text(
                        text = totalTimeStr,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = strings.totalTime,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.QueryStats,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }

    // Sheets & Dialogs
    if (showStatsDetails) {
        StatisticsBottomSheet(
            songList = songList,
            onDismiss = { showStatsDetails = false },
            getAlbumArt = getAlbumArt
        )
    }

    if (showMenuForSong != null) {
        val isFavorite = showMenuForSong!!.id in favoriteIds
        ModalBottomSheet(
            onDismissRequest = { showMenuForSong = null },
            containerColor = Color(0xFF121212),
            contentColor = Color.White
        ) {
            SongOptionsMenuContent(
                song = showMenuForSong!!,
                isFavorite = isFavorite,
                onPlay = { onSongClick(showMenuForSong!!); showMenuForSong = null },
                onToggleFavorite = { onToggleFavoriteId(showMenuForSong!!.id); showMenuForSong = null },
                onAddToQueue = { /* Implement if needed */ showMenuForSong = null },
                onPlayNext = { /* Implement if needed */ showMenuForSong = null },
                onAddToPlaylist = { /* Implement if needed */ showMenuForSong = null },
                onEditTags = { showTagEditor = showMenuForSong; showMenuForSong = null },
                onDelete = { /* Implement if needed */ showMenuForSong = null },
                onShowInfo = { /* Implement if needed */ showMenuForSong = null },
                onShare = { /* Implement if needed */ showMenuForSong = null },
                getAlbumArt = getAlbumArt
            )
        }
    }

    if (showTagEditor != null && songDetailsForEditor != null) {
        TagEditorDialog(
            song = showTagEditor!!,
            details = songDetailsForEditor!!,
            onDismiss = { showTagEditor = null; songDetailsForEditor = null },
            onSave = { title, artist, album, year, track, genre, art ->
                onUpdateTags(showTagEditor!!, title, artist, album, year, track, genre, art)
                showTagEditor = null
                songDetailsForEditor = null
            }
        )
    }
}

@Composable
fun QuickActionCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(30.dp))
            Spacer(Modifier.height(8.dp))
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = color)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsBottomSheet(
    songList: List<Song>,
    onDismiss: () -> Unit,
    getAlbumArt: (String) -> ByteArray?
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    var selectedPeriod by remember { mutableStateOf("All Time") }
    val periods = listOf(strings.today, strings.thisWeek, strings.thisMonth, strings.thisYear, strings.allTime)

    // State for statistics data
    val stats by produceState(initialValue = emptyMap<String, Any>(), selectedPeriod) {
        value = withContext(Dispatchers.IO) { StatisticsManager.getGeneralStats(context, selectedPeriod) }
    }
    val topDetailedSongs by produceState(initialValue = emptyList<StatisticsManager.DetailedSongStat>(), selectedPeriod) {
        value = withContext(Dispatchers.IO) { StatisticsManager.getTopSongsDetailed(context, songList, 5, selectedPeriod) }
    }
    val topArtists by produceState(initialValue = emptyList<StatisticsManager.DetailedStat>(), selectedPeriod) {
        value = withContext(Dispatchers.IO) { StatisticsManager.getTopArtistsDetailed(context, songList, 5, selectedPeriod) }
    }
    val topGenres by produceState(initialValue = emptyList<Pair<String, Int>>(), selectedPeriod) {
        value = withContext(Dispatchers.IO) { StatisticsManager.getTopGenres(context, songList, 5, selectedPeriod) }
    }
    val habits by produceState(initialValue = IntArray(24), selectedPeriod) {
        value = withContext(Dispatchers.IO) { StatisticsManager.getHourlyHabits(context, selectedPeriod) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = strings.listeningStats,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            // Period Selector
            SecondaryTabRow(
                selectedTabIndex = periods.indexOf(selectedPeriod),
                containerColor = Color.Transparent,
                divider = {},
                indicator = {
                    if (periods.indexOf(selectedPeriod) != -1) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(periods.indexOf(selectedPeriod)),
                            color = MaterialTheme.colorScheme.primary,
                            height = 3.dp
                        )
                    }
                }
            ) {
                periods.forEach { period ->
                    val selected = period == selectedPeriod
                    Tab(
                        selected = selected,
                        onClick = { selectedPeriod = period },
                        text = {
                            Text(
                                text = period,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Spacer(Modifier.height(24.dp))

                // Summary Cards with better styling
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = strings.plays,
                        value = stats["total_plays"]?.toString() ?: "0",
                        icon = Icons.Default.PlayArrow,
                        color = MaterialTheme.colorScheme.primary
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = strings.totalTime,
                        value = StatisticsManager.formatMillis(stats["total_time_ms"] as? Long ?: 0L),
                        icon = Icons.Default.Schedule,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(Modifier.height(32.dp))

                // Top Content Sections
                if (topDetailedSongs.isNotEmpty()) {
                    StatsSectionTitle(strings.yourTopFiveSongs)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(8.dp)
                    ) {
                        topDetailedSongs.forEachIndexed { index, item ->
                            DetailedStatItem(
                                rank = index + 1,
                                title = item.song.title,
                                subtitle = item.song.artist,
                                value = StatisticsManager.formatMillis(item.timeMs),
                                image = remember(item.song.data) { getAlbumArt(item.song.data) }
                            )
                            if (index < topDetailedSongs.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }

                if (topArtists.isNotEmpty()) {
                    Spacer(Modifier.height(32.dp))
                    StatsSectionTitle(strings.topArtists)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(8.dp)
                    ) {
                        topArtists.forEachIndexed { index, item ->
                            DetailedStatItem(
                                rank = index + 1,
                                title = item.name,
                                subtitle = "${item.plays} ${strings.plays.lowercase()}",
                                value = StatisticsManager.formatMillis(item.timeMs)
                            )
                            if (index < topArtists.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }

                if (topGenres.isNotEmpty()) {
                    Spacer(Modifier.height(32.dp))
                    StatsSectionTitle(strings.topGenres)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            topGenres.forEachIndexed { index, (genre, plays) ->
                                GenreStatBar(genre, plays, topGenres.first().second)
                                if (index < topGenres.size - 1) Spacer(Modifier.height(16.dp))
                            }
                        }
                    }
                }

                // Listening Habits (Bar Chart)
                Spacer(Modifier.height(32.dp))
                StatsSectionTitle(strings.listeningHabits)
                Text(
                    text = strings.yourPersonalRhythm,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Box(modifier = Modifier.padding(20.dp)) {
                        HabitsChart(habits)
                    }
                }

                Spacer(Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, label: String, value: String, icon: ImageVector, color: Color) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = value, 
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label, 
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StatsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
    )
}

@Composable
fun DetailedStatItem(rank: Int, title: String, subtitle: String, value: String, image: ByteArray? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.width(36.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = rank.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
        }
        
        if (image != null) {
            AsyncImage(
                model = image,
                contentDescription = null,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = title, 
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1, 
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle, 
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = value, 
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun GenreStatBar(genre: String, plays: Int, maxPlays: Int) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(), 
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = genre, 
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$plays ${if (plays == 1) "play" else "plays"}", 
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))
        
        val progress = if (maxPlays > 0) plays.toFloat() / maxPlays else 0f
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
    }
}

@Composable
fun HabitsChart(habits: IntArray) {
    val max = habits.maxOrNull() ?: 1
    val primaryColor = MaterialTheme.colorScheme.primary
    
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            habits.forEachIndexed { index, count ->
                val heightFactor = if (max > 0) count.toFloat() / max else 0f
                val isPeak = count == max && count > 0
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(heightFactor.coerceAtLeast(0.08f))
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = if (isPeak) listOf(primaryColor, primaryColor.copy(alpha = 0.7f))
                                         else listOf(primaryColor.copy(alpha = 0.5f), primaryColor.copy(alpha = 0.2f))
                            )
                        )
                )
            }
        }
        
        Spacer(Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(), 
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("00h", "06h", "12h", "18h", "23h").forEach { label ->
                Text(
                    text = label, 
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}


data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
