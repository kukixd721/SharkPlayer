package com.example.mp3.ui.components

import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.mp3.LocalStrings
import com.example.mp3.Song
import com.example.mp3.SongDetails
import com.example.mp3.getAudioMetadata
import com.example.mp3.ui.screens.MusicListConfig
import com.example.mp3.ui.screens.PlayerSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt

@Composable
fun CompactAlbumCard(
    song: Song,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
    getAlbumArt: ((String) -> ByteArray?)? = null,
    settings: PlayerSettings? = null
) {
    Surface(
        onClick = { onSongClick(song) },
        modifier = modifier
            .padding(4.dp)
            .aspectRatio(0.75f),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f)
    ) {
        Column {
            val art by produceState<ByteArray?>(null, song.data) {
                value = withContext(Dispatchers.IO) {
                    getAlbumArt?.invoke(song.data) ?: settings?.getAlbumArt(song.data)
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                val musicIcon = rememberVectorPainter(Icons.Default.MusicNote)
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(art ?: File(song.data))
                        .crossfade(true).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    error = musicIcon,
                    fallback = musicIcon
                )
            }

            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SongItemRow(
    song: Song,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onMenuClick: ((Song) -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    getAlbumArt: ((String) -> ByteArray?)? = null,
    settings: PlayerSettings? = null
) {
    val cornerRadius by animateDpAsState(
        targetValue = if (isSelected) 32.dp else 24.dp,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "cornerRadius"
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(82.dp)
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(cornerRadius),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)) else null
    ) {
        Row(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val art by produceState<ByteArray?>(null, song.data) {
                value = withContext(Dispatchers.IO) { 
                    getAlbumArt?.invoke(song.data) ?: settings?.getAlbumArt(song.data)
                }
            }

            val artCornerRadius by animateDpAsState(
                targetValue = if (isSelected) 27.dp else 14.dp,
                animationSpec = spring(
                    stiffness = Spring.StiffnessMediumLow,
                    dampingRatio = Spring.DampingRatioNoBouncy
                ),
                label = "artCornerRadius"
            )

            Surface(
                modifier = Modifier.size(54.dp),
                shape = RoundedCornerShape(artCornerRadius),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                val musicIcon = rememberVectorPainter(Icons.Default.MusicNote)
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(art ?: File(song.data))
                        .crossfade(true).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    error = musicIcon,
                    fallback = musicIcon
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
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
            } else if (onMenuClick != null) {
                IconButton(
                    onClick = { onMenuClick(song) },
                    modifier = Modifier.size(42.dp)
                ) {
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

@Composable
fun SubMusicList(
    title: String,
    songs: List<Song>,
    player: Player,
    isPlaying: Boolean,
    config: MusicListConfig,
    onBack: () -> Unit,
    subTitle: String? = null,
    onEditClick: (() -> Unit)? = null
) {
    val strings = LocalStrings.current
    val scrollState = rememberLazyListState()
    
    // Altura máxima del header
    val headerHeight = 340.dp
    val density = LocalDensity.current
    val headerHeightPx = with(density) { headerHeight.toPx() }
    
    // Calcular el offset de colapso basado en el scroll
    val scrollOffset = remember { derivedStateOf { 
        if (scrollState.firstVisibleItemIndex == 0) {
            scrollState.firstVisibleItemScrollOffset.toFloat().coerceAtMost(headerHeightPx)
        } else {
            headerHeightPx
        }
    } }

    // Fracción de colapso (0.0 = expandido, 1.0 = colapsado)
    val collapseFraction = remember { derivedStateOf { (scrollOffset.value / headerHeightPx).coerceIn(0f, 1f) } }

    Box(modifier = Modifier.fillMaxSize()) {
        // MusicList al fondo, con padding superior para dejar espacio al header
        MusicList(
            songs = songs,
            player = player,
            isPlaying = isPlaying,
            config = config,
            listState = scrollState,
            headerContent = {
                // Espaciador para que la lista empiece debajo del header expandido
                Spacer(modifier = Modifier.height(headerHeight))
            }
        )

        // Header Immersivo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight - ( (headerHeight - 80.dp) * collapseFraction.value))
                .graphicsLayer {
                    // Sutil efecto de parallax
                    translationY = -scrollOffset.value * 0.5f
                }
        ) {
            // Imagen de fondo con gradiente
            if (subTitle != null && subTitle.startsWith("/")) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(File(subTitle))
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        alpha = 1f - collapseFraction.value
                    }
                )
            } else if (songs.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(songs.first().data)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        alpha = 1f - collapseFraction.value
                    }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
            }

            // Overlay para oscurecer y gradiente inferior
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.4f),
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface.copy(alpha = collapseFraction.value),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
            )

            // Título y acciones (se mueven y escalan con el scroll)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = (24.dp * (1f - collapseFraction.value)).coerceAtLeast(8.dp))
                    .graphicsLayer {
                        val scale = 1f - (collapseFraction.value * 0.3f)
                        scaleX = scale
                        scaleY = scale
                        translationX = collapseFraction.value * with(density) { 48.dp.toPx() }
                    }
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (collapseFraction.value < 0.5f) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.graphicsLayer { alpha = 1f - (collapseFraction.value * 2f).coerceIn(0f, 1f) }
                    ) {
                        Text(
                            text = "${songs.size} ${if (songs.size == 1) strings.songs.dropLast(1) else strings.songs}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        onEditClick?.let {
                            FilledTonalButton(
                                onClick = it,
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(strings.editPlaylist, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Botón atrás fijo o flotante
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(8.dp)
                .align(Alignment.TopStart)
                .background(
                    Color.Black.copy(alpha = (0.3f * (1f - collapseFraction.value)).coerceAtLeast(0f)), 
                    CircleShape
                )
        ) {
            Icon(Icons.Default.ArrowBack, null, tint = if (collapseFraction.value > 0.5f) MaterialTheme.colorScheme.onSurface else Color.White)
        }
    }
}

@Composable
private fun SortChip(
    selected: Boolean,
    label: String,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = if (selected) 4.dp else 0.dp,
        modifier = Modifier.animateContentSize()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(
                    it,
                    null,
                    modifier = Modifier.size(18.dp),
                    tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ArtistGridItem(
    name: String,
    songsCount: Int,
    onClick: () -> Unit,
    getAlbumArt: (String) -> ByteArray?,
    firstSongData: String?,
    modifier: Modifier = Modifier
) {
    val art by produceState<ByteArray?>(null, firstSongData) {
        value = if (firstSongData != null) withContext(Dispatchers.IO) { getAlbumArt(firstSongData) } else null
    }
    val strings = LocalStrings.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth(0.85f),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 2.dp
        ) {
            val personIcon = rememberVectorPainter(Icons.Default.Person)
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(art ?: (if (firstSongData != null) File(firstSongData) else null))
                    .crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                error = personIcon,
                fallback = personIcon
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.1.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "$songsCount ${if (songsCount == 1) strings.song else strings.songs}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AlbumGridItem(
    name: String,
    artist: String,
    onClick: () -> Unit,
    getAlbumArt: (String) -> ByteArray?,
    firstSongData: String?,
    modifier: Modifier = Modifier
) {
    val art by produceState<ByteArray?>(null, firstSongData) {
        value = if (firstSongData != null) withContext(Dispatchers.IO) { getAlbumArt(firstSongData) } else null
    }
    val strings = LocalStrings.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Surface(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 3.dp
        ) {
            val albumIcon = rememberVectorPainter(Icons.Default.Album)
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(art ?: (if (firstSongData != null) File(firstSongData) else null))
                    .crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                error = albumIcon,
                fallback = albumIcon
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.1.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = artist,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ArtistList(
    artists: Map<String, List<Song>>,
    activeMediaItem: MediaItem?,
    onArtistClick: (List<Song>) -> Unit,
    settings: PlayerSettings,
    searchQuery: String
) {
    val strings = LocalStrings.current
    var sortOrder by remember { mutableIntStateOf(0) } // 0: A-Z, 1: Canciones
    
    val sortedArtists = remember(artists, sortOrder) {
        val list = artists.keys.toList()
        if (sortOrder == 0) list.sorted()
        else list.sortedByDescending { artists[it]?.size ?: 0 }
    }
    
    val filteredArtists = remember(sortedArtists, searchQuery) {
        sortedArtists.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SortChip(selected = sortOrder == 0, label = "A-Z", icon = Icons.Default.SortByAlpha, onClick = { sortOrder = 0 })
                SortChip(selected = sortOrder == 1, label = strings.songs, icon = Icons.Default.MusicNote, onClick = { sortOrder = 1 })
            }
        }

        val itemsPerRow = 3
        val rows = filteredArtists.chunked(itemsPerRow)
        items(rows.size) { rowIndex ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rows[rowIndex].forEach { artistName ->
                    val songs = artists[artistName] ?: emptyList()
                    ArtistGridItem(
                        name = artistName,
                        songsCount = songs.size,
                        onClick = { onArtistClick(songs) },
                        getAlbumArt = { settings.getAlbumArt(it) },
                        firstSongData = songs.firstOrNull()?.data,
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(itemsPerRow - rows[rowIndex].size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun AlbumList(
    albums: Map<String, List<Song>>,
    activeMediaItem: MediaItem?,
    onAlbumClick: (List<Song>) -> Unit,
    settings: PlayerSettings,
    searchQuery: String
) {
    val strings = LocalStrings.current
    var sortOrder by remember { mutableIntStateOf(0) } // 0: A-Z, 1: Artistas
    
    val sortedAlbums = remember(albums, sortOrder) {
        val list = albums.keys.toList()
        if (sortOrder == 0) list.sorted()
        else list.sortedBy { albums[it]?.firstOrNull()?.artist ?: "" }
    }
    
    val filteredAlbums = remember(sortedAlbums, searchQuery) {
        sortedAlbums.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SortChip(selected = sortOrder == 0, label = "A-Z", icon = Icons.Default.SortByAlpha, onClick = { sortOrder = 0 })
                SortChip(selected = sortOrder == 1, label = strings.artists, icon = Icons.Default.Person, onClick = { sortOrder = 1 })
            }
        }

        val itemsPerRow = 2
        val rows = filteredAlbums.chunked(itemsPerRow)
        items(rows.size) { rowIndex ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                rows[rowIndex].forEach { albumName ->
                    val songs = albums[albumName] ?: emptyList()
                    AlbumGridItem(
                        name = albumName,
                        artist = songs.firstOrNull()?.artist ?: strings.unknownArtist,
                        onClick = { onAlbumClick(songs) },
                        getAlbumArt = { settings.getAlbumArt(it) },
                        firstSongData = songs.firstOrNull()?.data,
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(itemsPerRow - rows[rowIndex].size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MusicList(
    songs: List<Song>,
    player: Player,
    isPlaying: Boolean,
    config: MusicListConfig,
    listState: LazyListState = rememberLazyListState(),
    headerContent: (@Composable () -> Unit)? = null
) {
    val strings = LocalStrings.current
    val haptic = LocalHapticFeedback.current
    val activeMediaItem = player.currentMediaItem
    
    var draggedItemKey by remember { mutableStateOf<String?>(null) }
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var initialDragIndex by remember { mutableStateOf<Int?>(null) }
    var draggedItemOffset by remember { mutableFloatStateOf(0f) }
    var isSettling by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    // Lista local con claves estables (UUID) para un drag fluido y sin parpadeos
    var localSongsWithKeys by remember { 
        mutableStateOf(songs.mapIndexed { i, s -> s to "key_${s.id}_${UUID.randomUUID()}" }) 
    }
    
    // Sincronizar con la lista externa solo cuando no estemos arrastrando o animando el drop
    LaunchedEffect(songs) {
        if (draggedItemKey == null && !isSettling) {
            val currentSongs = localSongsWithKeys.map { it.first }
            if (currentSongs != songs) {
                localSongsWithKeys = songs.mapIndexed { i, s -> s to "key_${s.id}_${UUID.randomUUID()}" }
            }
        }
    }
    
    var showMenuForSong by remember { mutableStateOf<Pair<Int, Song>?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<Song?>(null) }
    var showTagEditor by remember { mutableStateOf<Song?>(null) }
    
    val context = LocalContext.current
    var songDetailsForEditor by remember { mutableStateOf<SongDetails?>(null) }

    LaunchedEffect(showTagEditor) {
        if (showTagEditor != null) {
            songDetailsForEditor = withContext(Dispatchers.IO) {
                getAudioMetadata(context, showTagEditor!!.data)
            }
        } else {
            songDetailsForEditor = null
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(top = if (headerContent == null) 16.dp else 0.dp, bottom = 180.dp)
    ) {
        headerContent?.let {
            item { it() }
        }

        if (config.isDraggable) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Next Up",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${localSongsWithKeys.size} ${if (localSongsWithKeys.size == 1) strings.song else strings.songs}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(onClick = { 
                            val prevIndex = player.previousMediaItemIndex
                            if (prevIndex != -1) player.seekToDefaultPosition(prevIndex)
                            else player.seekTo(0)
                        }) {
                            Icon(
                                Icons.Default.SkipPrevious,
                                null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        MorphingPlayPauseButton(
                            isPlaying = isPlaying,
                            onClick = { if (isPlaying) player.pause() else player.play() },
                            modifier = Modifier.size(42.dp),
                            iconSize = 20.dp
                        )

                        IconButton(onClick = { 
                            val nextIndex = player.nextMediaItemIndex
                            if (nextIndex != -1) player.seekToDefaultPosition(nextIndex)
                        }) {
                            Icon(
                                Icons.Default.SkipNext,
                                null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        itemsIndexed(
            items = localSongsWithKeys, 
            key = { _, pair -> pair.second }
        ) { index, (song, itemKey) ->
            val currentIndex by rememberUpdatedState(index)
            val isActive = activeMediaItem?.mediaId == song.id.toString()
            val isBeingDragged = itemKey == draggedItemKey

            val currentOnMove by rememberUpdatedState(config.onMoveQueueItem)
            
            val cornerRadius by animateDpAsState(
                targetValue = if (isActive) 32.dp else 24.dp,
                animationSpec = spring(
                    stiffness = Spring.StiffnessMediumLow,
                    dampingRatio = Spring.DampingRatioNoBouncy
                ),
                label = "cornerRadius"
            )

            val dragScale by animateFloatAsState(
                targetValue = if (isBeingDragged) 1.05f else 1f,
                animationSpec = spring(
                    stiffness = Spring.StiffnessMediumLow,
                    dampingRatio = Spring.DampingRatioLowBouncy
                ),
                label = "dragScale"
            )

            val dragElevation by animateFloatAsState(
                targetValue = if (isBeingDragged) 30f else 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "dragElevation"
            )

            Surface(
                onClick = {
                    config.onPlayAll(localSongsWithKeys.map { it.first }, index)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp)
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .let { 
                        if (isBeingDragged) it 
                        else it.animateItem(
                            placementSpec = spring(
                                stiffness = Spring.StiffnessLow,
                                dampingRatio = Spring.DampingRatioNoBouncy
                            )
                        )
                    }
                    .graphicsLayer {
                        translationY = if (isBeingDragged) draggedItemOffset else 0f
                        shadowElevation = dragElevation
                        scaleX = dragScale
                        scaleY = dragScale
                        alpha = if (isBeingDragged) 0.85f else 1f
                    }
                    .zIndex(if (isBeingDragged) 100f else 1f),
                shape = RoundedCornerShape(cornerRadius),
                color = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isBeingDragged) 0.95f else 1f)
                        else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = if (isBeingDragged) 0.8f else 0.7f),
                border = if (isActive) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)) else null
            ) {
                Row(
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (config.isDraggable) {
                        Icon(
                            Icons.Default.DragIndicator,
                            null,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(24.dp)
                                .pointerInput(itemKey) {
                                    detectDragGestures(
                                        onDragStart = { _ ->
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            draggedItemKey = itemKey
                                            draggedItemIndex = currentIndex
                                            initialDragIndex = currentIndex
                                            draggedItemOffset = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            draggedItemOffset += dragAmount.y

                                            val itemHeight = (82.dp).toPx()
                                            val currentIdx = draggedItemIndex ?: currentIndex
                                            val targetIdx = (currentIdx + (draggedItemOffset / itemHeight).roundToInt())
                                                .coerceIn(0, localSongsWithKeys.size - 1)

                                            if (targetIdx != currentIdx) {
                                                val newList = localSongsWithKeys.toMutableList().apply {
                                                    add(targetIdx, removeAt(currentIdx))
                                                }
                                                localSongsWithKeys = newList
                                                draggedItemIndex = targetIdx
                                                draggedItemOffset -= (targetIdx - currentIdx) * itemHeight
                                            }
                                        },
                                        onDragEnd = {
                                            if (draggedItemKey != null) {
                                                isSettling = true
                                                scope.launch {
                                                    // Animación fluida de vuelta a la posición 0
                                                    animate(
                                                        initialValue = draggedItemOffset,
                                                        targetValue = 0f,
                                                        animationSpec = spring(
                                                            stiffness = Spring.StiffnessMediumLow,
                                                            dampingRatio = Spring.DampingRatioLowBouncy
                                                        )
                                                    ) { value, _ ->
                                                        draggedItemOffset = value
                                                    }
                                                    
                                                    val finalIdx = draggedItemIndex ?: currentIndex
                                                    val startIdx = initialDragIndex ?: index
                                                    if (finalIdx != startIdx) {
                                                        currentOnMove(startIdx, finalIdx)
                                                    }
                                                    
                                                    draggedItemKey = null
                                                    draggedItemIndex = null
                                                    initialDragIndex = null
                                                    isSettling = false
                                                }
                                            }
                                        },
                                        onDragCancel = {
                                            if (draggedItemKey != null) {
                                                isSettling = true
                                                scope.launch {
                                                    animate(draggedItemOffset, 0f) { value, _ -> 
                                                        draggedItemOffset = value 
                                                    }
                                                    draggedItemKey = null
                                                    draggedItemIndex = null
                                                    initialDragIndex = null
                                                    isSettling = false
                                                    localSongsWithKeys = songs.mapIndexed { i, s -> s to "key_${s.id}_${UUID.randomUUID()}" }
                                                }
                                            }
                                        }
                                    )
                                },
                            tint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f) 
                                   else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }

                    val art by produceState<ByteArray?>(null, song.data) {
                        value = withContext(Dispatchers.IO) { config.settings.getAlbumArt(song.data) }
                    }

                    val artCornerRadius by animateDpAsState(
                targetValue = if (isActive) 27.dp else 14.dp, // 27dp is ~half of 54dp size for Circle
                animationSpec = spring(
                    stiffness = Spring.StiffnessMediumLow,
                    dampingRatio = Spring.DampingRatioNoBouncy
                ),
                label = "artCornerRadius"
            )

            Surface(
                modifier = Modifier.size(54.dp),
                shape = RoundedCornerShape(artCornerRadius),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                        val musicIcon = rememberVectorPainter(Icons.Default.MusicNote)
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(art ?: File(song.data))
                                .crossfade(true).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            error = musicIcon,
                            fallback = musicIcon
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (isActive && isPlaying) {
                        Icon(
                            Icons.Default.Equalizer,
                            null,
                            tint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp).size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = { showMenuForSong = index to song },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            null,
                            tint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showMenuForSong != null) {
        val (songIndex, song) = showMenuForSong!!
        val isFavorite = song.id in config.favoriteIds
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
                    config.onPlayAll(songs, songIndex)
                    showMenuForSong = null
                },
                onToggleFavorite = { config.onToggleFavorite(song.id) },
                onAddToQueue = { 
                    config.onAddToQueue(song)
                    showMenuForSong = null 
                },
                onPlayNext = { 
                    config.onPlayNext(song)
                    showMenuForSong = null 
                },
                onAddToPlaylist = { 
                    config.onAddToPlaylist(song)
                    showMenuForSong = null 
                },
                onRemoveFromPlaylist = config.onRemoveFromPlaylist?.let { callback ->
                    {
                        callback(song)
                        showMenuForSong = null
                    }
                },
                onDelete = { 
                    showDeleteConfirm = song
                    showMenuForSong = null
                },
                onEditTags = { 
                    showTagEditor = song
                    showMenuForSong = null
                },
                onShowInfo = {
                    songDetailsForInfo = null // Trigger reload if needed
                    showMetadataDialog = true
                },
                onShare = { /* Share logic */ },
                getAlbumArt = { path -> config.settings.getAlbumArt(path) }
            )
        }

        if (showMetadataDialog) {
            LaunchedEffect(song) {
                songDetailsForInfo = withContext(Dispatchers.IO) {
                    getAudioMetadata(context, song.data)
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
            onSave = { t, ar, al, y, tr, g, art ->
                config.onUpdateTags(showTagEditor!!, t, ar, al, y, tr, g, art)
                showTagEditor = null
            }
        )
    }
}

@Composable
fun SongOptionsMenuContent(
    song: Song,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToQueue: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDelete: () -> Unit,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onEditTags: () -> Unit,
    onShowInfo: () -> Unit,
    onShare: () -> Unit,
    getAlbumArt: (String) -> ByteArray?
) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cabecera: Carátula grande y Título
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val art by produceState<ByteArray?>(null, song.data) {
                value = withContext(Dispatchers.IO) { getAlbumArt(song.data) }
            }
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.1f)
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

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                onClick = onEditTags,
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.1f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Fila 1: Play, Like, Share
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Botón Play Principal
            Surface(
                onClick = onPlay,
                modifier = Modifier.weight(1.5f).height(64.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.play, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            // Botón Like
            Surface(
                onClick = onToggleFavorite,
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.05f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        null,
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else Color.White
                    )
                }
            }

            // Botón Share
            Surface(
                onClick = onShare,
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.05f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Share, null, tint = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Fila 2: Add to Queue, Play Next (Cápsulas)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                onClick = onAddToQueue,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFFEADDFF).copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.QueueMusic, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.addToQueue, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Surface(
                onClick = onPlayNext,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFFEADDFF).copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.PlaylistPlay, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.playNext, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Fila 3: Playlist, Remove from Playlist (si aplica), Delete
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                onClick = onAddToPlaylist,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(28.dp),
                color = Color.White.copy(alpha = 0.05f)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.PlaylistAdd, null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.playlist, color = Color.White)
                }
            }

            if (onRemoveFromPlaylist != null) {
                Surface(
                    onClick = onRemoveFromPlaylist,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.PlaylistRemove, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.removeFromPlaylist, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Surface(
                onClick = onDelete,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFFB3261E).copy(alpha = 0.2f)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Delete, null, tint = Color(0xFFF2B8B5))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.deleteSong, color = Color(0xFFF2B8B5))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Fila 4: Info (Botón oscuro ancho)
        Surface(
            onClick = onShowInfo,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color.White.copy(alpha = 0.05f)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Info, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.info, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenreCard(
    genre: String,
    color: Color,
    emoji: String,
    onClick: () -> Unit,
    settings: PlayerSettings
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(32.dp),
        color = color
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Emoji grande de fondo a la derecha
            Text(
                text = emoji,
                fontSize = 110.sp,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 25.dp, y = 20.dp)
                    .graphicsLayer(alpha = 0.15f, rotationZ = -20f)
            )

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopStart),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Círculo con el emoji pequeño
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = emoji, fontSize = 18.sp)
                    }
                }

                Text(
                    text = genre,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.sp
                    ),
                    color = Color.White
                )
            }
        }
    }
}
