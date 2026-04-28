package com.example.mp3.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
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
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                SmallActionChip(Icons.Default.Search, strings.searchSongs, settings, onSearchClick)
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
                    val timePlayed = StatisticsManager.getFormattedTime(context, song.id.toString())
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
                color = MaterialTheme.colorScheme.surfaceContainer,
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
                        initialValue = StatisticsManager.getTotalTimeFormatted(context),
                        key1 = isPlaying,
                        key2 = player?.currentMediaItem
                    ) {
                        value = StatisticsManager.getTotalTimeFormatted(context)
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
        ModalBottomSheet(
            onDismissRequest = { showStatsDetails = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = strings.yourPersonalRhythm,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = strings.yourTopFiveSongs,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))

                val stats = remember(songList) { StatisticsManager.getTopSongsWithTime(context, songList, 5) }
                val maxTime = stats.firstOrNull()?.second ?: 1L

                stats.forEach { (song, time) ->
                    val percentage = (time.toFloat() / maxTime.toFloat()).coerceIn(0.1f, 1f)
                    val formattedTime = StatisticsManager.getFormattedTime(context, song.id.toString())
                    
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = formattedTime,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(6.dp)
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(percentage)
                                    .fillMaxHeight()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary
                                            )
                                        ),
                                        RoundedCornerShape(6.dp)
                                    )
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(48.dp))
                
                Button(
                    onClick = { showStatsDetails = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(settings.roundnessMedium.dp)
                ) {
                    Text("¡Sigue escuchando!")
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
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
        color = MaterialTheme.colorScheme.primaryContainer,
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
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f),
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
