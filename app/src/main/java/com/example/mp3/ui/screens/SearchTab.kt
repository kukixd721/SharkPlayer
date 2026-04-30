package com.example.mp3.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.absoluteValue
import androidx.media3.common.Player
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.mp3.LocalStrings
import com.example.mp3.Song
import com.example.mp3.SongDetails
import com.example.mp3.ui.components.GenreCard
import com.example.mp3.ui.components.MetadataDetailsDialog
import com.example.mp3.ui.components.SongItemRow
import com.example.mp3.ui.components.SongOptionsMenuContent
import com.example.mp3.ui.components.TagEditorDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTab(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchHistory: List<String> = emptyList(),
    onHistoryItemClick: (String) -> Unit = {},
    onRemoveHistoryItem: (String) -> Unit = {},
    onClearHistory: () -> Unit = {},
    onSearchAction: (String) -> Unit = {},
    songList: List<Song>,
    player: Player,
    currentMediaId: String,
    settings: PlayerSettings,
    browsingGenre: String?,
    onBrowsingGenreChange: (String?) -> Unit,
    getAlbumArt: (String) -> ByteArray?,
    favoriteIds: Set<Long>,
    onToggleFavoriteId: (Long) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onUpdateTags: (Song, String, String, String, String, String, String, ByteArray?) -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current

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

    if (showMenuForSong != null) {
        val song = showMenuForSong!!
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
                isFavorite = song.id in favoriteIds,
                onPlay = {
                    player.stop()
                    player.clearMediaItems()
                    
                    val index = songList.indexOf(song)
                    val finalOrder = if (index != -1) {
                        songList.subList(index, songList.size) + songList.subList(0, index)
                    } else {
                        listOf(song)
                    }
                    
                    val mediaItems = finalOrder.map { it.toMediaItem() }
                    player.addMediaItems(mediaItems)
                    player.prepare()
                    player.play()
                    showMenuForSong = null
                },
                onToggleFavorite = { onToggleFavoriteId(song.id) },
                onAddToQueue = {
                    player.addMediaItem(song.toMediaItem())
                    Toast.makeText(context, strings.addedToQueue, Toast.LENGTH_SHORT).show()
                    showMenuForSong = null
                },
                onPlayNext = {
                    val index = if (player.mediaItemCount == 0) 0 else player.currentMediaItemIndex + 1
                    player.addMediaItem(index, song.toMediaItem())
                    Toast.makeText(context, strings.addedToQueue, Toast.LENGTH_SHORT).show()
                    showMenuForSong = null
                },
                onAddToPlaylist = { 
                    onAddToPlaylist(song)
                    showMenuForSong = null 
                },
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
            onSave = { t, ar, al, y, tr, g, art ->
                onUpdateTags(showTagEditor!!, t, ar, al, y, tr, g, art)
                showTagEditor = null
            }
        )
    }

    if (browsingGenre != null) {
        // Buscamos canciones que coincidan con el género
        val genreSongs = remember(browsingGenre, songList) {
            songList.filter { song ->
                song.genre?.contains(browsingGenre, ignoreCase = true) == true
            }
        }

        // Si no hay canciones con ese género, mostramos un mensaje bonito
        if (genreSongs.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                IconButton(onClick = { onBrowsingGenreChange(null) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "🔍 Buscando canciones de $browsingGenre...",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Asegúrate de tener etiquetas de género en tus archivos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        } else {
            val songsByArtistInGenre = remember(genreSongs) {
                genreSongs.groupBy { it.artist }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                if (settings.backgroundImageUri != null) Color.Transparent else MaterialTheme.colorScheme.surface,
                                if (settings.backgroundImageUri != null) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = settings.backgroundAlpha) else MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        )
                    )
            ) {
                // Header del Género
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                ) {
                    Surface(
                        onClick = { onBrowsingGenreChange(null) },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Text(
                    text = browsingGenre,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                
                Text(
                    text = "${genreSongs.size} canciones encontradas",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 26.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = 16.dp,
                        vertical = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    songsByArtistInGenre.forEach { (artist, artistSongs) ->
                        // ... (el resto del renderizado de artistas y álbumes se mantiene igual)
                        item {
                            Surface(
                                color = if (settings.backgroundImageUri != null) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = settings.backgroundAlpha) else MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(settings.roundnessLarge.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Person,
                                                    null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(
                                            text = artist,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }

                                    val songsByAlbumInArtist = artistSongs.groupBy { it.album }
                                    songsByAlbumInArtist.forEach { (album, albumSongs) ->
                                        val albumArt by produceState<ByteArray?>(
                                            null,
                                            albumSongs.firstOrNull()?.data
                                        ) {
                                            value = withContext(Dispatchers.IO) {
                                                albumSongs.firstOrNull()?.let { getAlbumArt(it.data) }
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(settings.roundnessSmall.dp),
                                                modifier = Modifier.size(64.dp)
                                            ) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(context)
                                                        .data(albumArt)
                                                        .crossfade(true)
                                                        .build(),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = album,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${albumSongs.size} canciones",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    player.stop()
                                                    player.clearMediaItems()
                                                    
                                                    val songsToPlay = albumSongs
                                                    val remainingSongs = songsToPlay.toMutableList()
                                                    
                                                    if (player.shuffleModeEnabled) {
                                                        remainingSongs.shuffle()
                                                    }
                                                    
                                                    val mediaItems = remainingSongs.map { it.toMediaItem() }
                                                    player.addMediaItems(mediaItems)
                                                    player.prepare()
                                                    player.play()
                                                },
                                                colors = IconButtonDefaults.iconButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            ) {
                                                Icon(Icons.Default.PlayArrow, null)
                                            }
                                        }

                                        albumSongs.forEach { song ->
                                            SongItemRow(
                                                song = song,
                                                isSelected = currentMediaId == song.id.toString(),
                                                onClick = {
                                                    player.stop()
                                                    player.clearMediaItems()
                                                    
                                                    val index = albumSongs.indexOf(song)
                                                    val songsToPlay = if (index != -1) {
                                                        albumSongs.subList(index, albumSongs.size) + albumSongs.subList(0, index)
                                                    } else {
                                                        albumSongs
                                                    }
                                                    
                                                    val mediaItems = songsToPlay.map { it.toMediaItem() }
                                                    player.addMediaItems(mediaItems)
                                                    player.prepare()
                                                    player.play()
                                                },
                                                onMenuClick = { showMenuForSong = it },
                                                getAlbumArt = getAlbumArt,
                                                settings = settings,
                                                modifier = Modifier.padding(horizontal = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(140.dp))
                    }
                }
            }
        }
    } else {
            // GÉNEROS DINÁMICOS EXTRAÍDOS DE LA BIBLIOTECA
            val dynamicGenres = remember(songList) {
                songList.mapNotNull { it.genre }
                    .flatMap { it.split(",") }
                    .map { it.trim() }
                    .filter { it.isNotBlank() && it.lowercase() != "unknown" }
                    .distinct()
                    .map { genre ->
                        val emoji = when {
                            genre.contains("rock", true) -> "🎸"
                            genre.contains("pop", true) -> "🎤"
                            genre.contains("hip hop", true) || genre.contains("rap", true) -> "🎧"
                            genre.contains("jazz", true) -> "🎷"
                            genre.contains("classical", true) -> "🎻"
                            genre.contains("electronic", true) || genre.contains("techno", true) || genre.contains("dance", true) -> "🎹"
                            genre.contains("reggeaton", true) || genre.contains("trap", true) || genre.contains("urbano", true) -> "🔥"
                            genre.contains("reggae", true) -> "🍃"
                            genre.contains("country", true) -> "🤠"
                            genre.contains("blues", true) -> "🎷"
                            genre.contains("metal", true) -> "🤘"
                            genre.contains("folk", true) -> "🪕"
                            genre.contains("punk", true) -> "⚡"
                            genre.contains("k-pop", true) -> "✨"
                            genre.contains("lofi", true) -> "☕"
                            genre.contains("soundtrack", true) || genre.contains("anime", true) -> "🎬"
                            else -> "🎵"
                        }
                        
                        val hash = genre.hashCode().absoluteValue
                        val hue = (hash % 360).toFloat()
                        val color = Color.hsv(hue, 0.6f, 0.7f)
                        
                        Triple(genre, color, emoji)
                    }
            }
        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Buscar", style = MaterialTheme.typography.bodyLarge) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, null)
                        }
                    }
                } else null,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = { 
                        onSearchAction(searchQuery)
                        // Esconder teclado si es necesario, aunque en Compose suele manejarse solo con el IME action
                    }
                ),
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = if (settings.backgroundImageUri != null) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = settings.backgroundAlpha) else MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = if (settings.backgroundImageUri != null) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = settings.backgroundAlpha) else MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.Transparent,
                )
            )

            if (searchQuery.isNotEmpty()) {
                // ... (el resto del código de búsqueda se mantiene igual)
                val results = remember(searchQuery, songList) {
                    songList.filter {
                        it.title.contains(searchQuery, ignoreCase = true) ||
                                it.artist.contains(searchQuery, ignoreCase = true) ||
                                it.album.contains(searchQuery, ignoreCase = true)
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 140.dp)
                ) {
                    item {
                        Text(
                            text = "Resultados para \"$searchQuery\"",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (results.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No se encontraron canciones",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(results) { song ->
                            SongItemRow(
                                song = song,
                                isSelected = currentMediaId == song.id.toString(),
                                onClick = {
                                    player.stop()
                                    player.clearMediaItems()
                                    
                                    val index = results.indexOf(song)
                                    val songsToPlay = if (index != -1) {
                                        results.subList(index, results.size) + results.subList(0, index)
                                    } else {
                                        listOf(song)
                                    }
                                    
                                    val mediaItems = songsToPlay.map { it.toMediaItem() }
                                    player.addMediaItems(mediaItems)
                                    player.prepare()
                                    player.play()
                                },
                                onMenuClick = { showMenuForSong = it },
                                getAlbumArt = getAlbumArt,
                                settings = settings,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (searchHistory.isNotEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Búsquedas recientes",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                TextButton(onClick = onClearHistory) {
                                    Text("Borrar todo", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }

                        items(searchHistory, span = { GridItemSpan(2) }) { historyItem ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onHistoryItemClick(historyItem) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    text = historyItem,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { onRemoveHistoryItem(historyItem) }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Eliminar",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        item(span = { GridItemSpan(2) }) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }

                    item(span = { GridItemSpan(2) }) {
                        Text(
                            text = strings.browseByGenre,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                        )
                    }
                    items(dynamicGenres) { (genre, color, emoji) ->
                        GenreCard(
                            genre = genre,
                            color = color,
                            emoji = emoji,
                            onClick = { onBrowsingGenreChange(genre) },
                            settings = settings
                        )
                    }
                    item(span = { GridItemSpan(2) }) {
                        Spacer(modifier = Modifier.height(140.dp))
                    }
                }
            }
        }
    }
}
