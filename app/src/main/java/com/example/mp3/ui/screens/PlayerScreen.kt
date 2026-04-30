package com.example.mp3.ui.screens

import android.content.Context
import android.content.pm.ActivityInfo
import android.app.Activity
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.content.Intent
import android.provider.Settings
import com.example.mp3.*
import com.example.mp3.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.example.mp3.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@androidx.media3.common.util.UnstableApi
@Composable
fun PlayerScreen(
    songs: List<Song>,
    videos: List<Video> = emptyList(),
    player: MediaController?,
    settings: PlayerSettings,
    components: PlayerComponents,
    downloadViewModel: DownloadViewModel,
    onRefreshLibrary: () -> Unit = {},
    onIgnoreFolder: (String) -> Unit = {},
    ignoredFolders: Set<String> = emptySet()
) {
    val strings = LocalStrings.current
    // Escuchar cambios en el ViewModel de descarga para refrescar la lista
    LaunchedEffect(downloadViewModel.isLibraryUpdating) {
        if (downloadViewModel.isLibraryUpdating) {
            // Un pequeño delay extra aquí asegura que el MediaStore haya procesado la inserción
            // antes de que hagamos la consulta de archivos de audio.
            delay(800)
            onRefreshLibrary()
            downloadViewModel.isLibraryUpdating = false
        }
    }

    var currentMediaId by remember { mutableStateOf(player?.currentMediaItem?.mediaId ?: "") }
    var queueUpdateCounter by remember { mutableIntStateOf(0) }

    var showLyricsSelectionDialog by remember { mutableStateOf(false) }
    var showEqualizerDialog by remember { mutableStateOf(false) }
    var onlineLyricsResults by remember { mutableStateOf<List<LyricResult>>(emptyList()) }
    var isSearchingLyrics by remember { mutableStateOf(false) }

    // Listener para actualizar el estado cuando cambia la canción
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentMediaId = mediaItem?.mediaId ?: ""
            }

            override fun onEvents(player: Player, events: Player.Events) {
                if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                    currentMediaId = player.currentMediaItem?.mediaId ?: ""
                }
                if (events.contains(Player.EVENT_TIMELINE_CHANGED)) {
                    queueUpdateCounter++
                }
            }
        }
        player?.addListener(listener)
        onDispose {
            player?.removeListener(listener)
        }
    }

    val currentSong = remember(currentMediaId, songs) {
        songs.find { it.id.toString() == currentMediaId }
    }

    val currentVideo = remember(currentMediaId, videos) {
        videos.find { it.id.toString() == currentMediaId }
    }

    val isVideoPlaying = currentVideo != null

    if (player == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        val context = LocalContext.current
        var currentPosition by remember { mutableLongStateOf(0L) }
        var totalDuration by remember { mutableLongStateOf(0L) }
        var songList by remember { mutableStateOf(songs) }
        var isPlaying by remember { mutableStateOf(false) }

        // --- ANIMACIONES GLOBALES DE CARÁTULA (MATERIAL 3 EXPRESSIVE) ---
        val albumCornerRadius by animateDpAsState(
            targetValue = if (isPlaying) 48.dp else 24.dp,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "AlbumCorners"
        )

        var searchQuery by remember { mutableStateOf("") }
        var isLyricsFullScreen by rememberSaveable { mutableStateOf(false) }
        var isSheetOpen by rememberSaveable { mutableStateOf(false) }
        var isVideoFullScreen by rememberSaveable { mutableStateOf(false) }

        // --- ESTADOS DE VIDEO Y METADATOS (Hoisted) ---
        var playbackSpeed by remember { mutableFloatStateOf(1f) }
        val isVideoByMetadata =
            player.currentMediaItem?.mediaMetadata?.mediaType == MediaMetadata.MEDIA_TYPE_VIDEO
        var forceCloseVideo by remember { mutableStateOf(false) }

        // RESET de estados al cambiar de canción para evitar que desaparezca o se quede en pantalla completa
        LaunchedEffect(currentMediaId) {
            forceCloseVideo = false
            if (!isVideoPlaying && !isVideoByMetadata) {
                isVideoFullScreen = false
            }
        }

        val showVideoPlayer = (isVideoPlaying || isVideoByMetadata)

        fun findActivity(context: Context): Activity? {
            var ctx = context
            while (ctx is ContextWrapper) {
                if (ctx is Activity) return ctx
                ctx = ctx.baseContext
            }
            return null
        }
        val activity = remember(context) { findActivity(context) }

        val effectiveVideo = remember(currentMediaId, isVideoByMetadata) {
            currentVideo ?: if (isVideoByMetadata) {
                val title = player.currentMediaItem?.mediaMetadata?.title?.toString()
                videos.find { it.title == title }
            } else null
        }

        LaunchedEffect(playbackSpeed) {
            player.setPlaybackSpeed(playbackSpeed)
        }

        // Estados para overlays de brillo y volumen
        var showBrightnessOverlay by remember { mutableStateOf(false) }
        var brightnessLevel by remember { mutableIntStateOf(0) }
        var showVolumeOverlay by remember { mutableStateOf(false) }
        var volumeLevel by remember { mutableIntStateOf(0) }

        var showDetailedInfo by remember { mutableStateOf(false) }

        val effectiveMedia = currentSong ?: effectiveVideo
        val songDetails by produceState<SongDetails>(SongDetails(), effectiveMedia) {
            value = withContext(Dispatchers.IO) {
                val media = effectiveMedia
                val data = when (media) {
                    is Song -> media.data
                    is Video -> media.data
                    else -> ""
                }
                if (data.isBlank()) return@withContext SongDetails()

                var details = getAudioMetadata(context, data)
                if (media is Song && details.lyrics.isNullOrBlank() && settings.autoLyrics) {
                    // Intentar buscar en LRCLIB si el usuario tiene activada la búsqueda automática
                    try {
                        val cleanTitle =
                            media.title.replace(
                                Regex("(?i)\\(.*?\\)|\\[.*?\\]"),
                                ""
                            ).trim()
                        val artist = if (media.artist != "<unknown>") media.artist else ""
                        val queryUrl = "https://lrclib.net/api/search?artist_name=${java.net.URLEncoder.encode(artist, "UTF-8")}&track_name=${java.net.URLEncoder.encode(cleanTitle, "UTF-8")}"
                        val response = java.net.URL(queryUrl).readText()
                        val jsonArray = org.json.JSONArray(response)
                        if (jsonArray.length() > 0) {
                            val item = jsonArray.getJSONObject(0)
                            val bestLyrics = item.optString("syncedLyrics", "").ifEmpty { item.optString("plainLyrics", "") }
                            details = details.copy(
                                lyrics = bestLyrics,
                                lyricsSource = "online"
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                details
            }
        }

        var lyrics by remember(effectiveMedia, songDetails.lyrics) {
            mutableStateOf((effectiveMedia as? Song)?.lyrics ?: songDetails.lyrics)
        }
        var isEditingLyrics by rememberSaveable { mutableStateOf(false) }
        var selectedLyricLines by rememberSaveable { mutableStateOf(setOf<Int>()) }
        var isSelectionMode by rememberSaveable { mutableStateOf(false) }
        var editedLyricsText by rememberSaveable { mutableStateOf("") }
        // ----------------------------------------------

        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { newValue ->
                // Bloqueamos el cierre de la hoja si las letras están en pantalla completa
                if (isLyricsFullScreen && newValue == SheetValue.Hidden) false else true
            }
        )
        var showQueueDialog by remember { mutableStateOf(false) }
        var showClearQueueConfirm by remember { mutableStateOf(false) }

        var isShuffleOn by remember { mutableStateOf(false) }
        var repeatMode by remember { mutableIntStateOf(Player.REPEAT_MODE_OFF) }
        var lastUserAddedIndex by remember { mutableIntStateOf(-1) }

        // Limpiar el índice de inserción si la canción actual cambia y no es una de las agregadas manualmente
        LaunchedEffect(player.currentMediaItemIndex) {
            if (lastUserAddedIndex != -1 && player.currentMediaItemIndex > lastUserAddedIndex) {
                lastUserAddedIndex = -1
            }
        }
        var currentTab by remember { mutableIntStateOf(0) }
        val pagerState = rememberPagerState(pageCount = { 4 })

        LaunchedEffect(currentTab) {
            if (currentTab < 4 && pagerState.currentPage != currentTab) {
                pagerState.animateScrollToPage(currentTab)
            }
        }

        LaunchedEffect(pagerState.settledPage) {
            if (currentTab < 4 && currentTab != pagerState.settledPage) {
                currentTab = pagerState.settledPage
            }
        }
        val prefs = remember { context.getSharedPreferences("prefs", Context.MODE_PRIVATE) }
        var sortOrder by rememberSaveable { mutableIntStateOf(0) } // 0: Recientes, 1: A-Z, 2: Favoritos, 3: Artista, 4: Álbum
        var searchHistory by rememberSaveable { 
            val savedHistory = prefs.getString("search_history", "") ?: ""
            mutableStateOf(if (savedHistory.isEmpty()) emptyList<String>() else savedHistory.split("|"))
        }

        fun addToSearchHistory(query: String) {
            if (query.isBlank()) return
            val currentList = searchHistory.toMutableList()
            currentList.remove(query)
            currentList.add(0, query)
            val newList = currentList.take(10)
            searchHistory = newList
            prefs.edit { putString("search_history", newList.joinToString("|")) }
        }

        fun removeFromSearchHistory(query: String) {
            val newList = searchHistory.filter { it != query }
            searchHistory = newList
            prefs.edit { putString("search_history", newList.joinToString("|")) }
        }

        fun clearSearchHistory() {
            searchHistory = emptyList()
            prefs.edit { remove("search_history") }
        }
        var showStyleScreen by remember { mutableStateOf(false) }
        var showMenu by remember { mutableStateOf(false) }
        var showAboutDialog by remember { mutableStateOf(false) }
        var selectedMusicTab by remember { mutableIntStateOf(0) }

        var browsingArtist by remember { mutableStateOf<String?>(null) }
        var browsingAlbum by remember { mutableStateOf<String?>(null) }
        var browsingPlaylist by remember { mutableStateOf<String?>(null) }
        var browsingFolder by remember { mutableStateOf<String?>(null) }
        var browsingGenre by remember { mutableStateOf<String?>(null) }

        val isSubViewActive = browsingArtist != null || browsingAlbum != null ||
                browsingPlaylist != null || browsingFolder != null ||
                browsingGenre != null

        var isPlayerVisible by remember { mutableStateOf(true) }
        var videoResizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
        var showVideoControls by remember { mutableStateOf(true) }
        LaunchedEffect(showVideoControls, player.isPlaying) {
            if (showVideoControls && player.isPlaying) {
                delay(3500)
                showVideoControls = false
            }
        }
        val scope = rememberCoroutineScope()
        val globalSnackbarHostState = remember { SnackbarHostState() }

        // Escuchar eventos de descarga de forma global
        LaunchedEffect(downloadViewModel) {
            downloadViewModel.downloadEvents.collect { songTitle ->
                globalSnackbarHostState.showSnackbar(
                    message = "¡Lista para escuchar! $songTitle",
                    duration = SnackbarDuration.Short
                )
            }
        }

        // --- ANIMACIONES DE LA CARÁTULA ---
        val offsetX = remember { Animatable(0f) }
        val albumScale by animateFloatAsState(
            targetValue = if (isPlaying) 1f else 0.85f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "AlbumScale"
        )


        LaunchedEffect(showVideoPlayer, isVideoFullScreen) {
            // Manejado dentro de VideoPlayerView para mayor consistencia
        }

        var showFirstRunDialog by remember {
            val prefsApp = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            mutableStateOf(prefsApp.getBoolean("is_first_run", true))
        }

        var favoriteIds by remember {
            val savedFavs = prefs.getStringSet("favs", emptySet()) ?: emptySet()
            mutableStateOf(savedFavs.mapNotNull { it.toLongOrNull() }.toSet())
        }

        // Playlist & Selection State (Hoisted)
        var showCreatePlaylistDialog by remember { mutableStateOf(false) }
        var showEditPlaylistDialog by remember { mutableStateOf(false) }
        var playlistToEdit by remember { mutableStateOf<String?>(null) }
        var newPlaylistName by remember { mutableStateOf("") }
        var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }

        val toggleFavorite: (Long) -> Unit = { id ->
            val newSet =
                if (id in favoriteIds) favoriteIds - id else favoriteIds + id
            favoriteIds = newSet
            prefs.edit {
                putStringSet(
                    "favs",
                    newSet.map { it.toString() }.toSet()
                )
            }
        }

        val handleUpdateTags: (Song, String, String, String, String, String, String, ByteArray?) -> Unit =
            { song, title, artist, album, year, track, genre, artwork ->
                scope.launch(Dispatchers.IO) {
                    val success = updateSongTags(
                        song.data,
                        title,
                        artist,
                        album,
                        year,
                        track,
                        genre,
                        artwork
                    )
                    withContext(Dispatchers.Main) {
                        if (success) {
                            Toast.makeText(context, strings.tagsUpdated, Toast.LENGTH_SHORT).show()
                            onRefreshLibrary()
                        } else {
                            Toast.makeText(context, strings.errorUpdatingTags, Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            }

        val favoriteSongs =
            remember(songList, favoriteIds) { songList.filter { it.id in favoriteIds } }

        val librarySongs = remember(songList, ignoredFolders, sortOrder, favoriteIds) {
            val filtered = songList.filter { song ->
                !ignoredFolders.any { ignored -> song.data.startsWith(ignored) }
            }
            when (sortOrder) {
                0 -> filtered.sortedByDescending { it.id } // Asumiendo que ID mayor es más reciente
                1 -> filtered.sortedBy { it.title.lowercase() }
                2 -> filtered.filter { it.id in favoriteIds }.sortedBy { it.title.lowercase() }
                3 -> filtered.sortedBy { it.artist.lowercase() }
                4 -> filtered.sortedBy { it.album.lowercase() }
                else -> filtered
            }
        }

        val filteredSongs = remember(searchQuery, songList, ignoredFolders) {
            songList.filter { song ->
                val matchesSearch = song.title.contains(searchQuery, ignoreCase = true) ||
                        song.artist.contains(searchQuery, ignoreCase = true) ||
                        song.album.contains(searchQuery, ignoreCase = true)

                val isIgnored = ignoredFolders.any { ignored -> song.data.startsWith(ignored) }

                matchesSearch && !isIgnored
            }
        }

        val songsByArtist = remember(librarySongs) { librarySongs.groupBy { it.artist } }
        val songsByAlbum = remember(librarySongs) { librarySongs.groupBy { it.album } }
        val songsByFolder = remember(songList) { // Usamos songList para mostrar TODAS las carpetas (incluidas las ignoradas)
            songList.groupBy { song ->
                val file = File(song.data)
                file.parent ?: "Unknown"
            }
        }
        var playlistsNames by remember { mutableStateOf(PlaylistManager.getPlaylistNames(context)) }

        val tabs = listOf(
            LocalStrings.current.all,
            LocalStrings.current.artists,
            LocalStrings.current.albums,
            LocalStrings.current.playlists,
            LocalStrings.current.folders,
            LocalStrings.current.favorites,
            "Cola",
            LocalStrings.current.videos
        )



        LaunchedEffect(songs) {
            songList = songs
        }

        LaunchedEffect(player, isPlayerVisible) {
            while (true) {
                if (isPlayerVisible || isVideoPlaying) {
                    currentPosition = player.currentPosition.coerceAtLeast(0L)
                    totalDuration = if (player.duration > 0) player.duration else 0L
                    isPlaying = player.isPlaying
                    isShuffleOn = player.shuffleModeEnabled
                    repeatMode = player.repeatMode

                    currentMediaId = player.currentMediaItem?.mediaId ?: ""
                }
                delay(500)
            }
        }

        LaunchedEffect(Unit) {
            val music = withContext(Dispatchers.IO) { getAudioFiles(context) }
            songList = music
        }

        LaunchedEffect(downloadViewModel.isLibraryUpdating) {
            if (downloadViewModel.isLibraryUpdating) {
                delay(800)
                onRefreshLibrary()
                downloadViewModel.isLibraryUpdating = false
            }
        }

        if (showFirstRunDialog) {
            AlertDialog(
                onDismissRequest = { },
                icon = {
                    Surface(
                        modifier = Modifier.size(100.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shadowElevation = 8.dp
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.shark),
                            contentDescription = "Shark Logo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                },
                title = {
                    Text(
                        strings.welcomeTitle,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                text = {
                    Text(
                        strings.welcomeSubtitle,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                val canciones =
                                    withContext(Dispatchers.IO) { getAudioFiles(context) }
                                songList = canciones

                                val prefsApp = context.getSharedPreferences(
                                    "app_settings",
                                    Context.MODE_PRIVATE
                                )
                                prefsApp.edit { putBoolean("is_first_run", false) }

                                showFirstRunDialog = false
                                Toast.makeText(context, strings.syncFinished, Toast.LENGTH_SHORT)
                                    .show()
                                Unit
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f))
                    ) {
                        Text(strings.startScan, fontWeight = FontWeight.ExtraBold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showFirstRunDialog = false
                        Unit
                    }) {
                        Text(
                            strings.close,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                shape = RoundedCornerShape(32.dp)
            )
        }

        // --- DIÁLOGO "SOBRE LA APP" ---
        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = {
                    showAboutDialog = false
                    Unit
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.shark),
                            contentDescription = null,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Shark Player", fontWeight = FontWeight.Bold)
                    }
                },
                icon = {
                    Surface(
                        modifier = Modifier.size(80.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shadowElevation = 4.dp
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.shark),
                            contentDescription = "Shark Logo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                },
                text = {
                    Column {
                        Text(
                            "Versión 1.6",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Un reproductor de música moderno diseñado para ofrecer la mejor experiencia visual y auditiva.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Incluye transiciones suaves, crossfade inteligente y un diseño moderno basado en Material 3 Expressive.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            "Desarrollado por:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Kukardog 🚬", // Basado en tu nombre de usuario
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Código limpio, volumen al máximo.",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showAboutDialog = false
                        Unit
                    }) {
                        Text(strings.close, fontWeight = FontWeight.Bold)
                    }
                },
                shape = RoundedCornerShape(32.dp)
            )
        }

        if (showQueueDialog) {
            ModalBottomSheet(
                onDismissRequest = {
                    showQueueDialog = false
                    Unit
                },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                shape = RoundedCornerShape(
                    topStart = settings.roundnessMedium.dp,
                    topEnd = settings.roundnessMedium.dp
                )
            ) {
                Column(modifier = Modifier.fillMaxHeight(0.9f)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            strings.music,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { showClearQueueConfirm = true }) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                strings.clearQueue,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    val mediaItems = remember(player.mediaItemCount, queueUpdateCounter, player.shuffleModeEnabled) {
                        val timeline = player.currentTimeline
                        if (timeline.isEmpty) {
                            emptyList()
                        } else {
                            val items = mutableListOf<MediaItem>()
                            if (player.shuffleModeEnabled) {
                                // Reconstruir el orden aleatorio real
                                var index = timeline.getFirstWindowIndex(true)
                                while (index != -1) {
                                    items.add(player.getMediaItemAt(index))
                                    index = timeline.getNextWindowIndex(index, Player.REPEAT_MODE_OFF, true)
                                }
                                items
                            } else {
                                (0 until player.mediaItemCount).map { player.getMediaItemAt(it) }
                            }
                        }
                    }
                    val songListByMediaId =
                        remember(songList) { songList.associateBy { it.id.toString() } }


                    components.MusicList(
                        mediaItems.map { item ->
                            songListByMediaId[item.mediaId] ?: Song(
                                id = item.mediaId.toLongOrNull() ?: 0L,
                                title = item.mediaMetadata.title?.toString() ?: "",
                                artist = item.mediaMetadata.artist?.toString() ?: "",
                                album = item.mediaMetadata.albumTitle?.toString() ?: "",
                                data = item.requestMetadata.mediaUri?.toString() ?: "",
                                duration = 0L,
                                albumId = 0L
                            )
                        },
                        player,
                        isPlaying,
                        MusicListConfig(
                            activeMediaItem = player.currentMediaItem,
                            favoriteIds = favoriteIds,
                            onToggleFavorite = toggleFavorite,
                            onAddToPlaylist = { songToAddToPlaylist = it },
                            onPlayNext = { song ->
                                val index = if (player.mediaItemCount == 0) 0 else player.currentMediaItemIndex + 1
                                player.addMediaItem(index, song.toMediaItem())
                                Toast.makeText(context, strings.addedToQueue, Toast.LENGTH_SHORT).show()
                            },
                            onAddToQueue = { song ->
                                player.addMediaItem(song.toMediaItem())
                                Toast.makeText(context, strings.addedToQueue, Toast.LENGTH_SHORT).show()
                            },
                            onRemoveFromQueue = { index -> player.removeMediaItem(index) },
                            onMoveQueueItem = { from, to -> player.moveMediaItem(from, to) },
                            onUpdateTags = handleUpdateTags,
                            settings = settings,
                            isDraggable = true
                        )
                    )
                }
            }
        }

        if (showClearQueueConfirm) {
            AlertDialog(
                onDismissRequest = {
                    showClearQueueConfirm = false
                    Unit
                },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                title = {
                    Text(
                        strings.clearQueue,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                text = {
                    Text(
                        strings.clearQueueConfirm,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            player.clearMediaItems()
                            showClearQueueConfirm = false
                            Unit
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(strings.clearQueue)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showClearQueueConfirm = false
                        Unit
                    }) {
                        Text(
                            strings.cancel,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                    }
                }
            )
        }

        // --- DIÁLOGOS DE LISTAS DE REPRODUCCIÓN (Hoisted) ---
        if (showCreatePlaylistDialog) {
            var creatingName by remember { mutableStateOf("") }
            var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
            val imageLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
            ) { uri: android.net.Uri? ->
                selectedImageUri = uri
            }

            AlertDialog(
                onDismissRequest = {
                    showCreatePlaylistDialog = false
                    selectedImageUri = null
                },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(32.dp),
                title = { 
                    Text(
                        strings.createPlaylist, 
                        style = MaterialTheme.typography.headlineSmall, 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ) 
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Vista previa de imagen seleccionada
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                .clickable { imageLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedImageUri != null) {
                                AsyncImage(
                                    model = selectedImageUri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.AddPhotoAlternate, 
                                        null, 
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        strings.changeCover,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        OutlinedTextField(
                            value = creatingName,
                            onValueChange = { creatingName = it },
                            label = { Text(strings.enterPlaylistName) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (creatingName.isNotBlank()) {
                                PlaylistManager.createPlaylist(context, creatingName)
                                selectedImageUri?.let { uri ->
                                    PlaylistManager.setPlaylistImage(context, creatingName, uri.toString())
                                }
                                playlistsNames = PlaylistManager.getPlaylistNames(context)
                                showCreatePlaylistDialog = false
                                selectedImageUri = null
                                Toast.makeText(context, strings.playlistCreated, Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(strings.accept)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showCreatePlaylistDialog = false
                        selectedImageUri = null
                    }) {
                        Text(strings.cancel)
                    }
                }
            )
        }

        if (showEditPlaylistDialog && playlistToEdit != null) {
            var editedName by remember { mutableStateOf(playlistToEdit ?: "") }
            AlertDialog(
                onDismissRequest = {
                    showEditPlaylistDialog = false
                    Unit
                },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                title = { Text(strings.editPlaylist, color = MaterialTheme.colorScheme.primary) },
                text = {
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        label = { Text(strings.enterPlaylistName) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.primary,
                            unfocusedTextColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editedName.isNotBlank() && editedName != playlistToEdit) {
                                PlaylistManager.renamePlaylist(
                                    context,
                                    playlistToEdit!!,
                                    editedName
                                )
                                playlistsNames = PlaylistManager.getPlaylistNames(context)
                                showEditPlaylistDialog = false
                                Unit
                                playlistToEdit = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(strings.accept)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showEditPlaylistDialog = false
                        Unit
                    }) {
                        Text(
                            strings.close,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                    }
                }
            )
        }

        if (songToAddToPlaylist != null) {
            ModalBottomSheet(
                onDismissRequest = {
                    songToAddToPlaylist = null
                },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                dragHandle = { BottomSheetDefaults.DragHandle() },
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        strings.addToPlaylist,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (playlistsNames.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                strings.noPlaylists,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 400.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(playlistsNames.toList().sorted()) { playlistName ->
                                val playlistSongIds = PlaylistManager.getSongsInPlaylist(context, playlistName)
                                
                                Surface(
                                    onClick = {
                                        songToAddToPlaylist?.let { song ->
                                            PlaylistManager.addSongToPlaylist(
                                                context,
                                                playlistName,
                                                song.id
                                            )
                                            scope.launch {
                                                globalSnackbarHostState.showSnackbar(strings.songAdded)
                                            }
                                        }
                                        songToAddToPlaylist = null
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainer
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.PlaylistPlay,
                                            null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                playlistName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                "${playlistSongIds.size} ${if (playlistSongIds.size == 1) strings.song else strings.songs}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    FilledTonalButton(
                        onClick = {
                            showCreatePlaylistDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.createPlaylist)
                    }
                }
            }
        }


        // --- MAIN APP UI ---
        Scaffold(
            containerColor = if (settings.backgroundImageUri != null) Color.Transparent else MaterialTheme.colorScheme.surface,
            snackbarHost = {
                SnackbarHost(hostState = globalSnackbarHostState) { data ->
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 12.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp), // Forma simétrica
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary, // Color sólido
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                null,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = data.visuals.message,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            },
            topBar = {
                if (currentTab != 4) {
                    TopAppBar(
                        title = {
                            Text(
                                text = when (currentTab) {
                                    0 -> strings.home
                                    1 -> strings.search
                                    2 -> "Library"
                                    3 -> "Downloads"
                                    else -> strings.settings
                                },
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        ),
                        navigationIcon = {
                        },
                        actions = {
                            Surface(
                                onClick = {
                                    context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                                },
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(40.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Bluetooth,
                                        contentDescription = strings.connectBluetooth,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            IconButton(onClick = {
                                currentTab = 4
                                browsingGenre = null
                            }) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = strings.settings,
                                    tint = if (currentTab == 4) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (currentTab != 4) {
                    NavigationBar(
                        containerColor = if (settings.backgroundImageUri != null) Color.Transparent else MaterialTheme.colorScheme.surface,
                        tonalElevation = if (settings.backgroundImageUri != null) 0.dp else 3.dp
                    ) {
                        NavigationBarItem(
                            selected = currentTab == 0,
                            onClick = {
                                currentTab = 0
                                browsingGenre = null
                            },
                            icon = {
                                Icon(
                                    Icons.Default.Home,
                                    contentDescription = strings.home,
                                    modifier = Modifier.size(30.dp)
                                )
                            },
                            label = {
                                Text(
                                    strings.home,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        )
                        NavigationBarItem(
                            selected = currentTab == 1,
                            onClick = {
                                currentTab = 1
                                browsingGenre = null
                            },
                            icon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = strings.search,
                                    modifier = Modifier.size(30.dp)
                                )
                            },
                            label = {
                                Text(
                                    strings.search,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        )
                        NavigationBarItem(
                            selected = currentTab == 2,
                            onClick = {
                                currentTab = 2
                                browsingGenre = null
                            },
                            icon = {
                                Icon(
                                    Icons.Default.LibraryMusic,
                                    contentDescription = "Library",
                                    modifier = Modifier.size(30.dp)
                                )
                            },
                            label = {
                                Text(
                                    "Library",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        )
                        NavigationBarItem(
                            selected = currentTab == 3,
                            onClick = {
                                currentTab = 3
                                browsingGenre = null
                            },
                            icon = {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = "Downloads",
                                    modifier = Modifier.size(30.dp)
                                )
                            },
                            label = {
                                Text(
                                    "Downloads",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        )
                    }
                }
            },
            floatingActionButton = {
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isPlayerVisible && currentTab != 4 && currentTab != 3,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    ExtendedFloatingActionButton(
                        onClick = { isPlayerVisible = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(settings.roundnessMedium.dp),
                        icon = { Icon(Icons.Default.PlayArrow, null) },
                        text = { Text(strings.sharkPlayer) }
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (settings.backgroundImageUri != null) Color.Transparent else MaterialTheme.colorScheme.background)
            ) {
                // 1. El contenido de las pestañas (Home, Search, etc.)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    AnimatedContent(
                        targetState = if (currentTab == 4) 4 else 0,
                        transitionSpec = {
                            val springSpec = spring<IntOffset>(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                            if (targetState == 4) {
                                (slideInHorizontally(springSpec) { it } + fadeIn()).togetherWith(
                                    slideOutHorizontally(springSpec) { -it } + fadeOut())
                            } else {
                                (slideInHorizontally(springSpec) { -it } + fadeIn()).togetherWith(
                                    slideOutHorizontally(springSpec) { it } + fadeOut())
                            }.using(
                                SizeTransform(clip = false)
                            )
                        }, label = "MainSettingsTransition"
                    ) { targetState ->
                        if (targetState == 4) {
                            BackHandler { currentTab = 0 }
                            SettingsScreen(
                                padding = PaddingValues(0.dp),
                                settings = settings,
                                songs = songs,
                                onScanClick = { onProgress, onComplete ->
                                    scope.launch {
                                        val result = withContext(Dispatchers.IO) {
                                            getAudioFiles(context, onProgress)
                                        }
                                        onComplete(result)
                                    }
                                },
                                scanAndSaveLyrics = { ctx, list, onProgress ->
                                    scanAndSaveLyrics(ctx, list, onProgress)
                                },
                                components = components,
                                onOpenStyleScreen = { showStyleScreen = true }
                            )
                        } else {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                                userScrollEnabled = !isSubViewActive,
                                beyondViewportPageCount = 1
                            ) { page ->
                                when (page) {
                                    0 -> {
                                        val homeSongList = remember(songs) { songs }
                                        HomeScreen(
                                            songList = homeSongList,
                                            player = player,
                                            favoriteIds = favoriteIds,
                                            onToggleFavoriteId = toggleFavorite,
                                            onSongClick = { song ->
                                                player.stop()
                                                player.clearMediaItems()
                                                
                                                val index = songList.indexOf(song)
                                                val finalOrder = if (index != -1) {
                                                    songList.subList(index, songList.size) + songList.subList(0, index)
                                                } else {
                                                    listOf(song)
                                                }
                                                
                                                val mediaItems = finalOrder.map { s -> s.toMediaItem() }
                                                player.addMediaItems(mediaItems)
                                                player.prepare()
                                                player.play()
                                            },
                                            onSearchClick = {
                                                currentTab = 1
                                                selectedMusicTab = 0
                                                browsingGenre = null
                                            },
                                            onEqualizerClick = {
                                                showEqualizerDialog = true
                                            },
                                            onDownloadClick = {
                                                currentTab = 3
                                                browsingGenre = null
                                            },
                                            onUpdateTags = handleUpdateTags,
                                            settings = settings,
                                            getAlbumArt = { settings.getAlbumArt(it) },
                                        )
                                    }

                                    1 -> {
                                        if (browsingGenre != null) {
                                            val genreSongs = remember(browsingGenre, songList) {
                                                songList.filter {
                                                    it.genre?.contains(
                                                        browsingGenre!!,
                                                        ignoreCase = true
                                                    ) == true
                                                }
                                            }
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
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 16.dp, vertical = 24.dp)
                                                ) {
                                                    Surface(
                                                        onClick = { browsingGenre = null },
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
                                                    text = browsingGenre!!,
                                                    style = MaterialTheme.typography.displaySmall,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
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
                                                        item {
                                                            Surface(
                                                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                                shape = RoundedCornerShape(settings.roundnessLarge.dp),
                                                                modifier = Modifier.fillMaxWidth()
                                                            ) {
                                                                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                                                                    Row(
                                                                        verticalAlignment = Alignment.CenterVertically,
                                                                        modifier = Modifier.padding(
                                                                            horizontal = 16.dp,
                                                                            vertical = 8.dp
                                                                        )
                                                                    ) {
                                                                        Surface(
                                                                            shape = CircleShape,
                                                                            color = MaterialTheme.colorScheme.primary.copy(
                                                                                alpha = 0.2f
                                                                            ),
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

                                                                    val songsByAlbumInArtist =
                                                                        artistSongs.groupBy { it.album }
                                                                    songsByAlbumInArtist.forEach { (album, albumSongs) ->
                                                                        val albumArt by produceState<ByteArray?>(
                                                                            null,
                                                                            albumSongs.firstOrNull()?.data
                                                                        ) {
                                                                            value = withContext(Dispatchers.IO) {
                                                                                albumSongs.firstOrNull()
                                                                                    ?.let { settings.getAlbumArt(it.data) }
                                                                            }
                                                                        }

                                                                        Row(
                                                                            verticalAlignment = Alignment.CenterVertically,
                                                                            modifier = Modifier
                                                                                .fillMaxWidth()
                                                                                .padding(
                                                                                    horizontal = 16.dp,
                                                                                    vertical = 12.dp
                                                                                )
                                                                        ) {
                                                                            Surface(
                                                                                shape = RoundedCornerShape(settings.roundnessSmall.dp),
                                                                                modifier = Modifier.size(64.dp)
                                                                            ) {
                                                                                AsyncImage(
                                                                                    model = ImageRequest.Builder(
                                                                                        LocalContext.current
                                                                                    )
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
                                                                                    player.setMediaItems(
                                                                                        albumSongs.map { it.toMediaItem() })
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
                                                                                getAlbumArt = { data -> getAlbumArt(data) },
                                                                                onClick = {
                                                                                    player.setMediaItem(song.toMediaItem())
                                                                                    player.prepare()
                                                                                    player.play()
                                                                                },
                                                                                trailingContent = {
                                                                                    Icon(
                                                                                        Icons.Default.MoreVert,
                                                                                        null,
                                                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                                                    )
                                                                                },
                                                                                settings = settings
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
                                        } else {
                                            SearchTab(
                                                searchQuery = searchQuery,
                                                onSearchQueryChange = { searchQuery = it },
                                                searchHistory = searchHistory,
                                                onHistoryItemClick = { 
                                                    searchQuery = it
                                                    addToSearchHistory(it)
                                                },
                                                onRemoveHistoryItem = { removeFromSearchHistory(it) },
                                                onClearHistory = { clearSearchHistory() },
                                                onSearchAction = { addToSearchHistory(it) },
                                                songList = songList,
                                                player = player,
                                                currentMediaId = currentMediaId,
                                                settings = settings,
                                                browsingGenre = browsingGenre,
                                                onBrowsingGenreChange = { browsingGenre = it },
                                                getAlbumArt = { settings.getAlbumArt(it) },
                                                favoriteIds = favoriteIds,
                                                onToggleFavoriteId = toggleFavorite,
                                                onAddToPlaylist = { songToAddToPlaylist = it },
                                                onUpdateTags = handleUpdateTags
                                            )
                                        }
                                    }

                                    2 -> {
                                        LibraryTab(
                                            selectedMusicTab = selectedMusicTab,
                                            onSelectedMusicTabChange = {
                                                selectedMusicTab = it
                                                browsingArtist = null
                                                browsingAlbum = null
                                                browsingPlaylist = null
                                                browsingFolder = null
                                            },
                                            tabs = tabs,
                                            finalSortedSongs = librarySongs,
                                            player = player,
                                            isPlaying = isPlaying,
                                            favoriteIds = favoriteIds,
                                            toggleFavorite = toggleFavorite,
                                            onAddToPlaylist = { songToAddToPlaylist = it },
                                            songList = songList,
                                            onSongListChange = { songList = it },
                                            videos = videos,
                                            components = components,
                                            settings = settings,
                                            onRefreshLibrary = {
                                                scope.launch {
                                                    val result = withContext(Dispatchers.IO) { getAudioFiles(context) }
                                                    songList = result
                                                }
                                            },
                                            onIgnoreFolder = onIgnoreFolder,
                                            ignoredFolders = ignoredFolders,
                                            browsingArtist = browsingArtist,
                                            onBrowsingArtistChange = { browsingArtist = it },
                                            browsingAlbum = browsingAlbum,
                                            onBrowsingAlbumChange = { browsingAlbum = it },
                                            browsingPlaylist = browsingPlaylist,
                                            onBrowsingPlaylistChange = { browsingPlaylist = it },
                                            browsingFolder = browsingFolder,
                                            onBrowsingFolderChange = { browsingFolder = it },
                                            sortOrder = sortOrder,
                                            onSortOrderChange = { sortOrder = it },
                                            showClearQueueConfirm = showClearQueueConfirm,
                                            onShowClearQueueConfirmChange = { showClearQueueConfirm = it },
                                            onVideoClick = { video ->
                                                if (player?.currentMediaItem?.mediaId != video.id.toString()) {
                                                    player?.setMediaItem(video.toMediaItem())
                                                    player?.prepare()
                                                    player?.play()
                                                }
                                                isPlayerVisible = true
                                                forceCloseVideo = false // Asegurar que se muestre si estaba cerrado
                                                // isVideoFullScreen = true // Eliminado para evitar pantalla completa automática
                                            },
                                            searchQuery = "",
                                            onCurrentTabChange = { currentTab = it },
                                            songsByArtist = songsByArtist,
                                            songsByAlbum = songsByAlbum,
                                            songsByFolder = songsByFolder,
                                            favoriteSongs = favoriteSongs,
                                            playlistsNames = playlistsNames,
                                            onPlaylistsNamesChange = { playlistsNames = it },
                                            onCreatePlaylist = { showCreatePlaylistDialog = true },
                                            onRenamePlaylist = { old, new ->
                                                PlaylistManager.renamePlaylist(context, old, new)
                                                playlistsNames = PlaylistManager.getPlaylistNames(context)
                                            },
                                            onDeletePlaylist = { name ->
                                                PlaylistManager.deletePlaylist(context, name)
                                                playlistsNames = PlaylistManager.getPlaylistNames(context)
                                                Toast.makeText(context, strings.playlistDeleted, Toast.LENGTH_SHORT).show()
                                            },
                                            onSetPlaylistImage = { name, uri ->
                                                PlaylistManager.setPlaylistImage(context, name, uri)
                                                playlistsNames = emptySet()
                                                playlistsNames = PlaylistManager.getPlaylistNames(context).toSet()
                                            },
                                            onEditPlaylist = { name ->
                                                playlistToEdit = name
                                                showEditPlaylistDialog = true
                                            },
                                            handleUpdateTags = handleUpdateTags,
                                            onVideoFullScreenChange = { isVideoFullScreen = it }
                                        )
                                    }

                                    3 -> {
                                        DownloadScreen(
                                            paddingValues = PaddingValues(0.dp),
                                            settings = settings,
                                            viewModel = downloadViewModel
                                        )
                                    }
                                }
                            }
                        }
                    }
                }


                // 2. El Reproductor (ControlBar) - FUERA del Box con padding para control total
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = paddingValues.calculateBottomPadding()) // Pegado a la NavigationBar
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isPlayerVisible && currentTab != 4 && currentTab != 3 && !isVideoFullScreen && !isLyricsFullScreen,
                        enter = slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + fadeIn(animationSpec = tween(300)),
                        exit = slideOutVertically(
                            targetOffsetY = { it / 2 },
                            animationSpec = spring(stiffness = Spring.StiffnessMedium)
                        ) + fadeOut(animationSpec = tween(200))
                    ) {
                        ControlBar(
                            player = player,
                            isPlaying = isPlaying,
                            currentPosition = currentPosition,
                            totalDuration = totalDuration,
                            isShuffleOn = isShuffleOn,
                            repeatMode = repeatMode,
                            songList = songList,
                            settings = settings,
                            onTitleClick = {
                                if (isVideoPlaying || isVideoByMetadata) {
                                    forceCloseVideo = false
                                    isPlayerVisible = true
                                    isVideoFullScreen = true // Se abre a pantalla completa al presionar la barra (acción explícita)
                                } else {
                                    isSheetOpen = true
                                }
                            },
                            onCollapse = {
                                if (isVideoPlaying || isVideoByMetadata) {
                                    forceCloseVideo = true
                                } else {
                                    isPlayerVisible = false
                                }
                            },
                            getAlbumArt = { settings.getAlbumArt(it) },
                            forceCloseVideo = forceCloseVideo
                        )
                    }
                }
            }

            if (showEqualizerDialog) {
                androidx.compose.ui.window.Dialog(
                    onDismissRequest = { showEqualizerDialog = false },
                    properties = androidx.compose.ui.window.DialogProperties(
                        usePlatformDefaultWidth = false
                    )
                ) {
                    EcualizadorPanel(onClose = { showEqualizerDialog = false })
                }
            }

            if (isSheetOpen) {
                FullPlayerSheet(
                    player = player,
                    currentSong = currentSong,
                    currentVideo = effectiveVideo,
                    settings = settings,
                    components = components,
                    strings = strings,
                    onDismiss = { isSheetOpen = false },
                    sheetState = sheetState,
                    isLyricsFullScreen = isLyricsFullScreen,
                    onLyricsFullScreenChange = { isLyricsFullScreen = it },
                    showQueueDialog = showQueueDialog,
                    onShowQueueDialogChange = { showQueueDialog = it },
                    showEqualizerDialog = showEqualizerDialog,
                    onShowEqualizerDialogChange = { showEqualizerDialog = it },
                    onlineLyricsResults = onlineLyricsResults,
                    onOnlineLyricsResultsChange = { onlineLyricsResults = it },
                    showLyricsSelectionDialog = showLyricsSelectionDialog,
                    onShowLyricsSelectionDialogChange = { showLyricsSelectionDialog = it },
                    showDetailedInfo = showDetailedInfo,
                    onToggleDetailedInfo = { showDetailedInfo = it },
                    playbackSpeed = playbackSpeed,
                    onSpeedChange = {
                        playbackSpeed = it
                        player.setPlaybackSpeed(it)
                    },
                    isSearchingLyrics = isSearchingLyrics,
                    onIsSearchingLyricsChange = { isSearchingLyrics = it },
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    totalDuration = totalDuration,
                    isShuffleOn = isShuffleOn,
                    repeatMode = repeatMode,
                    albumScale = albumScale,
                    albumCornerRadius = albumCornerRadius,
                    offsetX = offsetX,
                    videos = videos,
                    // Nuevos parámetros pasados
                    songDetails = songDetails,
                    lyrics = lyrics,
                    onLyricsChange = { lyrics = it },
                    isEditingLyrics = isEditingLyrics,
                    onEditLyricsChange = { isEditingLyrics = it },
                    selectedLyricLines = selectedLyricLines,
                    onSelectedLyricLinesChange = { selectedLyricLines = it },
                    isSelectionMode = isSelectionMode,
                    onSelectionModeChange = { isSelectionMode = it },
                    editedLyricsText = editedLyricsText,
                    onEditedLyricsTextChange = { editedLyricsText = it }
                )
            }

            // --- PANTALLA DE LETRAS FIJA (Overlay superior como Dialog) ---
            if (isLyricsFullScreen) {
                Dialog(
                    onDismissRequest = { isLyricsFullScreen = false },
                    properties = DialogProperties(
                        usePlatformDefaultWidth = false,
                        dismissOnBackPress = true,
                        dismissOnClickOutside = false
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (settings.backgroundImageUri != null) Color.Transparent else MaterialTheme.colorScheme.surface)
                    ) {
                        if (settings.unifiedLyricsBackground) {
                            AnimatedMeshGradient(
                                modifier = Modifier.fillMaxSize(),
                                primaryColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                secondaryColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                                tertiaryColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f),
                                alpha = 0.5f,
                                blurRadius = 120.dp
                            )
                        }

                        LyricsView(
                            player = player,
                            currentSong = currentSong,
                            lyrics = lyrics,
                            onLyricsChange = { lyrics = it },
                            onClose = { isLyricsFullScreen = false },
                            isSelectionMode = isSelectionMode,
                            onSelectionModeChange = { isSelectionMode = it },
                            selectedLyricLines = selectedLyricLines,
                            onSelectedLinesChange = { selectedLyricLines = it },
                            showDetailedInfo = showDetailedInfo,
                            onToggleDetailedInfo = { showDetailedInfo = !showDetailedInfo },
                            isEditingLyrics = isEditingLyrics,
                            onEditLyrics = { isEditingLyrics = it },
                            currentPosition = currentPosition,
                            totalDuration = totalDuration,
                            isShuffleOn = isShuffleOn,
                            repeatMode = repeatMode,
                            songDetails = songDetails,
                            settings = settings,
                            components = components,
                            strings = strings,
                            onShowQueue = { showQueueDialog = true }
                        )
                    }
                }
            }

                    AnimatedVisibility(
                        visible = showVideoPlayer && !forceCloseVideo,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                        modifier = Modifier.zIndex(200f) // Aseguramos prioridad de dibujo
                    ) {
                        VideoPlayerView(
                            player = player,
                            effectiveVideo = effectiveVideo,
                            currentSong = currentSong,
                            showVideoPlayer = showVideoPlayer,
                            isVideoFullScreen = isVideoFullScreen,
                            onToggleFullScreen = { isVideoFullScreen = it },
                            onCloseVideo = {
                                isVideoFullScreen = false
                                if (isVideoPlaying || isVideoByMetadata) {
                                    forceCloseVideo = true
                                }
                            },
                            activity = activity,
                            videoResizeMode = videoResizeMode,
                            onToggleResize = {
                                videoResizeMode =
                                    if (videoResizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT)
                                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT
                            },
                            showVideoControls = showVideoControls,
                            onToggleVideoControls = { showVideoControls = it },
                            showBrightnessOverlay = showBrightnessOverlay,
                            brightnessLevel = brightnessLevel,
                            showVolumeOverlay = showVolumeOverlay,
                            volumeLevel = volumeLevel,
                            showDetailedInfo = showDetailedInfo,
                            onToggleDetailedInfo = { showDetailedInfo = it },
                            songDetails = songDetails,
                            playbackSpeed = playbackSpeed,
                            onSpeedChange = {
                                playbackSpeed = it
                                player.setPlaybackSpeed(it)
                            },
                            settings = settings,
                            components = components,
                            strings = strings
                        )
                    }

                    AnimatedVisibility(
                        visible = showStyleScreen,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                        modifier = Modifier.zIndex(1000f) // Por encima de TODO
                    ) {
                        BackHandler { showStyleScreen = false }
                        StyleScreen(
                            settings = settings,
                            onBack = { showStyleScreen = false }
                        )
                    }
                }
            }
        }
