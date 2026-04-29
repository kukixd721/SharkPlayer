package com.example.mp3.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
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
import coil.request.ImageRequest
import com.example.mp3.*
import com.example.mp3.R
import com.example.mp3.ui.screens.PlayerSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
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
    settings: PlayerSettings,
    getAlbumArt: (String) -> ByteArray?
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    
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
    
    val topSongs by produceState<List<Song>>(initialValue = emptyList(), songList) {
        value = withContext(Dispatchers.IO) {
            StatisticsManager.getTopSongs(context, songList, 5)
        }
    }
    val recentSongs by produceState<List<Song>>(initialValue = emptyList(), songList) {
        value = withContext(Dispatchers.IO) {
            StatisticsManager.getRecentSongs(context, songList, 10)
        }
    }

    var isRecentExpanded by remember { mutableStateOf(true) }
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

    val currentMediaId by produceState<String?>(initialValue = player?.currentMediaItem?.mediaId, key1 = player?.currentMediaItem) {
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                value = mediaItem?.mediaId
            }
        }
        player?.addListener(listener)
        awaitDispose {
            player?.removeListener(listener)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 36.dp, bottom = 24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-1).sp,
                                fontSize = 42.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().basicMarquee()
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(settings.roundnessSmall.dp),
                        color = iconColor.copy(alpha = 0.12f),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.padding(12.dp),
                            tint = iconColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (settings.backgroundImageUri != null) MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(
                        iconColor,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(
                                iconColor.copy(alpha = 0.05f),
                                Color.Transparent
                            )))
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = iconColor.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = quote,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                lineHeight = 24.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SmallActionChip(Icons.Default.Search, strings.search, settings, onSearchClick)
                SmallActionChip(Icons.Default.Equalizer, strings.equalizer, settings, onEqualizerClick)
            }
        }


        if (topSongs.isNotEmpty()) {
            item {
                Text(
                    text = strings.yourPersonalAnthem,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 24.dp, bottom = 16.dp, top = 24.dp)
                )
                
                val heroSong = topSongs.first()
                HeroSongCard(heroSong, onSongClick, getAlbumArt, settings = settings)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = strings.mostPlayed,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 24.dp, bottom = 12.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            if (settings.useGridViewHome) {
                item {
                    val remainingSongs = topSongs.drop(1)
                    val itemsPerRow = 2
                    val rows = remainingSongs.chunked(itemsPerRow)
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rows.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowItems.forEach { song ->
                                    CompactAlbumCard(
                                        song = song,
                                        onSongClick = onSongClick,
                                        getAlbumArt = getAlbumArt,
                                        settings = settings,
                                        modifier = Modifier
                                            .weight(1f)
                                            .animateContentSize()
                                    )
                                }
                                if (rowItems.size < itemsPerRow) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            } else {
                items(topSongs.drop(1)) { song ->
                    val timePlayed by produceState<String>(initialValue = "...", song.id) {
                        value = withContext(Dispatchers.IO) {
                            StatisticsManager.getFormattedTime(context, song.id.toString())
                        }
                    }
                    SongItemRow(
                        song = song, 
                        onClick = onSongClick, 
                        getAlbumArt = getAlbumArt,
                        onMenuClick = { showMenuForSong = it },
                        settings = settings,
                        isSelected = currentMediaId == song.id.toString(),
                        modifier = Modifier.animateContentSize(),
                        trailingContent = {
                            Text(
                                text = timePlayed,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (currentMediaId == song.id.toString()) Color.White else MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .background(
                                        if (currentMediaId == song.id.toString()) Color.White.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), 
                                        RoundedCornerShape(settings.roundnessSmall.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isRecentExpanded = !isRecentExpanded }
                    .padding(start = 24.dp, top = 40.dp, bottom = 16.dp, end = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = strings.recentlyAdded,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (isRecentExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        if (isRecentExpanded) {
            if (settings.useGridViewHome) {
                item {
                    val itemsPerRow = 2
                    val rows = recentSongs.chunked(itemsPerRow)
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rows.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowItems.forEach { song ->
                                    CompactAlbumCard(
                                        song = song,
                                        onSongClick = onSongClick,
                                        getAlbumArt = getAlbumArt,
                                        settings = settings,
                                        modifier = Modifier
                                            .weight(1f)
                                            .animateContentSize()
                                    )
                                }
                                if (rowItems.size < itemsPerRow) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            } else {
                items(recentSongs) { song ->
                    SongItemRow(
                        song, 
                        onSongClick, 
                        getAlbumArt, 
                        onMenuClick = { showMenuForSong = it },
                        settings = settings,
                        isSelected = currentMediaId == song.id.toString(),
                        modifier = Modifier.animateContentSize()
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(48.dp))
            Surface(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .clickable { showStatsDetails = true },
                color = if (settings.backgroundImageUri != null) MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(settings.roundnessLarge.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = strings.musicInNumbers,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                    
                    val isPlaying = player?.isPlaying ?: false
                    val totalTime by produceState(
                        initialValue = "...",
                        key1 = isPlaying,
                        key2 = player?.currentMediaItem
                    ) {
                        value = withContext(Dispatchers.IO) {
                            StatisticsManager.getTotalTimeFormatted(context)
                        }
                    }
                    val totalSongsCount = songList.size
                    val totalArtists = remember(songList) { songList.map { it.artist }.distinct().size }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(Icons.Default.History, totalTime, strings.totalTime, Modifier.weight(1f), MaterialTheme.colorScheme.primary, settings = settings)
                        StatCard(Icons.Default.Face, totalArtists.toString(), strings.artists, Modifier.weight(1.2f), MaterialTheme.colorScheme.secondary, settings = settings)
                        StatCard(Icons.Default.LibraryMusic, totalSongsCount.toString(), strings.songs, Modifier.weight(1f), MaterialTheme.colorScheme.tertiary, settings = settings)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showMenuForSong != null) {
        val song = showMenuForSong!!
        val isFavorite = song.id in favoriteIds 
        var showMetadataDialog by remember { mutableStateOf(false) }
        var songDetailsForInfo by remember { mutableStateOf<SongDetails?>(null) }

        ModalBottomSheet(
            onDismissRequest = { showMenuForSong = null },
            containerColor = Color(0xFF121212),
            contentColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            SongOptionsMenuContent(
                song = song,
                isFavorite = isFavorite,
                onPlay = {
                    onSongClick(song)
                    showMenuForSong = null
                },
                onToggleFavorite = { 
                    onToggleFavoriteId(song.id)
                },
                onAddToQueue = { showMenuForSong = null },
                onPlayNext = { showMenuForSong = null },
                onAddToPlaylist = { showMenuForSong = null },
                onDelete = { showMenuForSong = null },
                onEditTags = { 
                    showTagEditor = song
                    showMenuForSong = null
                },
                onShowInfo = {
                    showMetadataDialog = true
                },
                onShare = { /* Share logic */ },
                getAlbumArt = { getAlbumArt(it) }
            )
        }

        if (showMetadataDialog) {
            LaunchedEffect(song) {
                songDetailsForInfo = withContext(Dispatchers.IO) {
                    com.example.mp3.getAudioMetadata(context, song.data)
                }
            }
            
            songDetailsForInfo?.let { details ->
                MetadataDetailsDialog(
                    songDetails = details,
                    currentSong = song,
                    strings = LocalStrings.current,
                    onDismiss = { showMetadataDialog = false }
                )
            }
        }
    }

    if (showTagEditor != null && songDetailsForEditor != null) {
        TagEditorDialog(
            song = showTagEditor!!,
            details = songDetailsForEditor!!,
            onDismiss = { showTagEditor = null },
            onSave = { _, _, _, _, _, _, _ ->
                showTagEditor = null
            }
        )
    }

    if (showStatsDetails) {
        val allTimeText = strings.allTime
        var selectedFilter by remember { mutableStateOf(allTimeText) }

        val topSongsWithTime by produceState<List<StatisticsManager.DetailedSongStat>>(emptyList(), songList, selectedFilter) {
            value = withContext(Dispatchers.IO) {
                val period = when (selectedFilter) {
                    strings.today -> "Today"
                    strings.thisWeek -> "This Week"
                    strings.thisMonth -> "This Month"
                    strings.thisYear -> "This Year"
                    else -> "All Time"
                }
                StatisticsManager.getTopSongsDetailed(context, songList, 10, period)
            }
        }
        val topArtistsWithTime by produceState<List<StatisticsManager.DetailedStat>>(emptyList(), songList, selectedFilter) {
            value = withContext(Dispatchers.IO) {
                val period = when (selectedFilter) {
                    strings.today -> "Today"
                    strings.thisWeek -> "This Week"
                    strings.thisMonth -> "This Month"
                    strings.thisYear -> "This Year"
                    else -> "All Time"
                }
                StatisticsManager.getTopArtistsDetailed(context, songList, 5, period)
            }
        }
        val topAlbumsWithTime by produceState<List<StatisticsManager.DetailedStat>>(emptyList(), songList, selectedFilter) {
            value = withContext(Dispatchers.IO) {
                val period = when (selectedFilter) {
                    strings.today -> "Today"
                    strings.thisWeek -> "This Week"
                    strings.thisMonth -> "This Month"
                    strings.thisYear -> "This Year"
                    else -> "All Time"
                }
                StatisticsManager.getTopAlbumsDetailed(context, songList, 5, period)
            }
        }
        val hourlyHabits by produceState<IntArray?>(null, selectedFilter) {
            value = withContext(Dispatchers.IO) {
                val period = when (selectedFilter) {
                    strings.today -> "Today"
                    strings.thisWeek -> "This Week"
                    strings.thisMonth -> "This Month"
                    strings.thisYear -> "This Year"
                    else -> "All Time"
                }
                StatisticsManager.getHourlyHabits(context, period)
            }
        }
        val general by produceState<Map<String, Any>>(emptyMap(), selectedFilter) {
            value = withContext(Dispatchers.IO) {
                val period = when (selectedFilter) {
                    strings.today -> "Today"
                    strings.thisWeek -> "This Week"
                    strings.thisMonth -> "This Month"
                    strings.thisYear -> "This Year"
                    else -> "All Time"
                }
                StatisticsManager.getGeneralStats(context, period)
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showStatsDetails = false },
            containerColor = Color(0xFF0F1115),
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.listeningStats,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Filtros (Today, This Week...)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf(strings.today, strings.thisWeek, strings.thisMonth, strings.thisYear, strings.allTime)
                    filters.forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.White.copy(alpha = 0.05f),
                                labelColor = Color.White.copy(alpha = 0.6f),
                                selectedContainerColor = Color(0xFF64B5F6).copy(alpha = 0.8f),
                                selectedLabelColor = Color.Black
                            ),
                            border = null,
                            shape = CircleShape
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                // Resumen arriba (Listening / Plays)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val totalTimeMs = general["total_time_ms"]?.toString()?.toLongOrNull() ?: 0L
                    val totalPlays = general["total_plays"]?.toString()?.toIntOrNull() ?: 0
                    
                    SummaryCard(
                        title = strings.listening, 
                        value = StatisticsManager.formatMillis(totalTimeMs),
                        modifier = Modifier.weight(1f),
                        containerColor = Color(0xFF2D4356),
                        icon = Icons.Default.Headset
                    )
                    SummaryCard(
                        title = strings.plays, 
                        value = totalPlays.toString(),
                        modifier = Modifier.weight(1f),
                        containerColor = Color(0xFFD2C1F3),
                        icon = Icons.Default.PlayArrow
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
                
                // Timeline Section
                Text(strings.listeningTimeline, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text(
                    strings.listeningTimelineDesc, 
                    style = MaterialTheme.typography.bodySmall, 
                    color = Color.White.copy(alpha = 0.6f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Surface(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    val totalPlays = general["total_plays"]?.toString()?.toIntOrNull() ?: 0
                    if (totalPlays == 0 || hourlyHabits == null) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.White.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(strings.noDataYet, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(strings.noDataDesc, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f))
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(16.dp), 
                            verticalAlignment = Alignment.Bottom, 
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val maxHour = hourlyHabits!!.maxOrNull()?.takeIf { it > 0 } ?: 1
                            hourlyHabits!!.forEachIndexed { _, count ->
                                val hPerc by animateFloatAsState(
                                    targetValue = (count.toFloat() / maxHour.toFloat()).coerceIn(0.1f, 1f),
                                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                                    label = "barHeight"
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(hPerc)
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(Color(0xFF64B5F6), Color(0xFF64B5F6).copy(alpha = 0.3f))
                                            ), 
                                            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 2.dp, bottomEnd = 2.dp)
                                        )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Top Categories
                Text(strings.topCategories, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text(
                    strings.compareListening,
                    style = MaterialTheme.typography.bodySmall, 
                    color = Color.White.copy(alpha = 0.6f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                var categoryTab by remember { mutableStateOf(strings.song) }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(strings.song, strings.album, strings.artist, strings.genre).forEach { tab ->
                        FilterChip(
                            selected = categoryTab == tab,
                            onClick = { categoryTab = tab },
                            label = { Text(tab) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.White.copy(alpha = 0.05f),
                                labelColor = Color.White.copy(alpha = 0.6f),
                                selectedContainerColor = Color(0xFF64B5F6).copy(alpha = 0.9f),
                                selectedLabelColor = Color.Black
                            ),
                            border = null,
                            shape = CircleShape
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedContent(
                    targetState = categoryTab,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(220, delayMillis = 90)) + 
                         scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)))
                        .togetherWith(fadeOut(animationSpec = tween(90)))
                    },
                    label = "categoryTransition"
                ) { targetTab ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            when (targetTab) {
                                strings.song -> {
                                    if (topSongsWithTime.isEmpty()) NoDataView(strings.noTopTracks)
                                    else topSongsWithTime.take(5).forEachIndexed { index, stat ->
                                        StatListItem(index + 1, stat.song.title, "${stat.song.artist} • ${stat.plays} ${strings.plays.lowercase()}", StatisticsManager.formatMillis(stat.timeMs))
                                    }
                                }
                                strings.artist -> {
                                    if (topArtistsWithTime.isEmpty()) NoDataView(strings.noTopArtists)
                                    else topArtistsWithTime.forEachIndexed { index, stat ->
                                        StatListItem(index + 1, stat.name, "${stat.plays} ${strings.plays.lowercase()}", StatisticsManager.formatMillis(stat.timeMs))
                                    }
                                }
                                strings.album -> {
                                    if (topAlbumsWithTime.isEmpty()) NoDataView(strings.noTopAlbums)
                                    else topAlbumsWithTime.forEachIndexed { index, stat ->
                                        StatListItem(index + 1, stat.name, "${stat.plays} ${strings.plays.lowercase()}", StatisticsManager.formatMillis(stat.timeMs))
                                    }
                                }
                                strings.genre -> {
                                    val topGenres by produceState<List<Pair<String, Int>>>(emptyList(), songList, selectedFilter) {
                                        value = withContext(Dispatchers.IO) {
                                            val period = when (selectedFilter) {
                                                strings.today -> "Today"
                                                strings.thisWeek -> "This Week"
                                                strings.thisMonth -> "This Month"
                                                strings.thisYear -> "This Year"
                                                else -> "All Time"
                                            }
                                            StatisticsManager.getTopGenres(context, songList, 5, period)
                                        }
                                    }
                                    if (topGenres.isEmpty()) NoDataView(strings.noTopGenres)
                                    else topGenres.forEachIndexed { index, (genre, plays) ->
                                        StatListItem(index + 1, genre, "$plays ${strings.plays.lowercase()}", "")
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Listening Habits (Detailed)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(strings.listeningHabits, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        val totalSessions = general["total_sessions"]?.toString()?.toIntOrNull() ?: 0
                        val avgSessionMs = general["avg_session_ms"]?.toString()?.toLongOrNull() ?: 0L
                        val longestSessionMs = general["longest_session_ms"]?.toString()?.toLongOrNull() ?: 0L
                        
                        HabitRow(Icons.Default.History, strings.totalSessions, totalSessions.toString())
                        HabitRow(Icons.Default.Hearing, strings.avgSession, StatisticsManager.formatMillis(avgSessionMs))
                        HabitRow(Icons.Default.FlashOn, strings.longestSession, StatisticsManager.formatMillis(longestSessionMs))
                        HabitRow(Icons.Default.BarChart, strings.sessionsPerDay, String.format(Locale.US, "%.1f", totalSessions.toFloat() / 7f))
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun SummaryCard(title: String, value: String, modifier: Modifier, containerColor: Color, icon: ImageVector) {
    val isLight = containerColor.luminance() > 0.5f
    val contentColor = if (isLight) Color.Black else Color.White
    
    Surface(
        modifier = modifier.height(130.dp),
        color = containerColor,
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title, 
                    style = MaterialTheme.typography.labelLarge, 
                    fontWeight = FontWeight.Bold,
                    color = contentColor.copy(alpha = 0.6f)
                )
                Icon(
                    icon, 
                    contentDescription = null, 
                    modifier = Modifier.size(18.dp), 
                    tint = contentColor.copy(alpha = 0.4f)
                )
            }
            Text(
                value, 
                style = MaterialTheme.typography.headlineMedium, 
                fontWeight = FontWeight.ExtraBold, 
                color = contentColor
            )
        }
    }
}

private fun Color.luminance(): Float {
    return 0.2126f * red + 0.7152f * green + 0.0722f * blue
}

@Composable
fun StatListItem(rank: Int, title: String, subtitle: String, trailing: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(rank.toString(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
        }
        if (trailing.isNotEmpty()) {
            Text(trailing, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun HabitRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = Color.White.copy(alpha = 0.7f))
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun NoDataView(text: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Info, null, modifier = Modifier.size(32.dp), tint = Color.White.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(8.dp))
        Text(text, color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.bodySmall)
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun HeroSongCard(
    song: Song, 
    onClick: (Song) -> Unit, 
    getAlbumArt: (String) -> ByteArray?,
    settings: PlayerSettings
) {
    val art by produceState<ByteArray?>(null, song.data) { value = withContext(Dispatchers.IO) { getAlbumArt(song.data) } }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .padding(horizontal = 24.dp)
            .clickable { onClick(song) },
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 20.dp),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Box {
            AsyncImage(
                model = art ?: File(song.data),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))))
            )
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)
            ) {
                Text(song.title, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(8.dp))
            }
        }
    }
}

@Composable
fun CompactAlbumCard(
    song: Song, 
    onSongClick: (Song) -> Unit, 
    getAlbumArt: (String) -> ByteArray?,
    settings: PlayerSettings,
    modifier: Modifier = Modifier
) {
    val art by produceState<ByteArray?>(null, song.data) { value = withContext(Dispatchers.IO) { getAlbumArt(song.data) } }
    
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onSongClick(song) }
            .padding(4.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth(),
            shadowElevation = 2.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(art ?: File(song.data))
                    .crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = song.title,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.1.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SmallActionChip(icon: ImageVector, label: String, settings: PlayerSettings, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (settings.backgroundImageUri != null) MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                label, 
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun StatCard(
    icon: ImageVector, 
    value: String, 
    label: String, 
    modifier: Modifier = Modifier, 
    accentColor: Color = MaterialTheme.colorScheme.primary,
    settings: PlayerSettings
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = if (settings.backgroundImageUri != null) MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 20.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = accentColor.copy(alpha = 0.2f),
                modifier = Modifier.size(44.dp)
            ) {
                Icon(icon, null, modifier = Modifier.padding(10.dp), tint = accentColor)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                value, 
                style = MaterialTheme.typography.headlineMedium, 
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                label, 
                style = MaterialTheme.typography.labelMedium, 
                color = MaterialTheme.colorScheme.primary, 
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun SongItemRow(
    song: Song,
    onClick: (Song) -> Unit,
    getAlbumArt: ((String) -> ByteArray?)? = null,
    onMenuClick: ((Song) -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    settings: PlayerSettings,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false
) {
    val art by produceState<ByteArray?>(null, song.data) {
        value = withContext(Dispatchers.IO) { getAlbumArt?.invoke(song.data) }
    }

    val cornerRadius by animateDpAsState(
        targetValue = if (isSelected) 40.dp else 12.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "cornerRadius"
    )

    val imageCornerRadius by animateDpAsState(
        targetValue = if (isSelected) 40.dp else 10.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "imageCornerRadius"
    )

    Surface(
        onClick = { onClick(song) },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(cornerRadius),
        color = if (isSelected) {
            if (settings.backgroundImageUri != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.primaryContainer
        } else {
            if (settings.backgroundImageUri != null) MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f)
        },
    ) {
        Row(
            modifier = Modifier
                .padding(if (isSelected) 6.dp else 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(imageCornerRadius),
                modifier = Modifier.size(52.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(art ?: song.data)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    error = painterResource(R.drawable.ic_launcher_foreground)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (trailingContent != null) {
                trailingContent()
            } else {
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f) else Color.Transparent,
                    modifier = Modifier.size(42.dp),
                    onClick = { onMenuClick?.invoke(song) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.MoreVert,
                            null,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
