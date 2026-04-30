package com.example.mp3.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import com.example.mp3.LocalStrings
import com.example.mp3.PlaylistManager
import com.example.mp3.Song
import com.example.mp3.Video
import com.example.mp3.ui.components.CompactAlbumCard
import com.example.mp3.ui.components.VideoList
import java.io.File
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.content.Intent
import android.provider.DocumentsContract

fun getPathFromUri(context: android.content.Context, uri: Uri): String? {
    if (DocumentsContract.isTreeUri(uri)) {
        val documentId = DocumentsContract.getTreeDocumentId(uri)
        if (documentId.startsWith("primary:")) {
            return android.os.Environment.getExternalStorageDirectory().toString() + "/" + documentId.split(":")[1]
        }
    }
    return null
}

@Composable
fun ToolbarActionIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun LibraryTab(
    selectedMusicTab: Int,
    onSelectedMusicTabChange: (Int) -> Unit,
    tabs: List<String>,
    finalSortedSongs: List<Song>,
    player: Player?,
    isPlaying: Boolean,
    favoriteIds: Set<Long>,
    toggleFavorite: (Long) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    songList: List<Song>,
    onSongListChange: (List<Song>) -> Unit,
    videos: List<Video>,
    components: PlayerComponents,
    settings: PlayerSettings,
    onRefreshLibrary: () -> Unit,
    onIgnoreFolder: (String) -> Unit,
    ignoredFolders: Set<String>,
    browsingArtist: String?,
    onBrowsingArtistChange: (String?) -> Unit,
    browsingAlbum: String?,
    onBrowsingAlbumChange: (String?) -> Unit,
    browsingPlaylist: String?,
    onBrowsingPlaylistChange: (String?) -> Unit,
    browsingFolder: String?,
    onBrowsingFolderChange: (String?) -> Unit,
    sortOrder: Int,
    onSortOrderChange: (Int) -> Unit,
    showClearQueueConfirm: Boolean,
    onShowClearQueueConfirmChange: (Boolean) -> Unit,
    onVideoClick: (Video) -> Unit,
    searchQuery: String,
    onCurrentTabChange: (Int) -> Unit,
    songsByArtist: Map<String, List<Song>>,
    songsByAlbum: Map<String, List<Song>>,
    songsByFolder: Map<String, List<Song>>,
    favoriteSongs: List<Song>,
    playlistsNames: Set<String>,
    onPlaylistsNamesChange: (Set<String>) -> Unit,
    onCreatePlaylist: () -> Unit,
    onRenamePlaylist: (String, String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onSetPlaylistImage: (String, String?) -> Unit,
    onEditPlaylist: (String) -> Unit,
    handleUpdateTags: (Song, String, String, String, String, String, String, ByteArray?) -> Unit,
    onVideoFullScreenChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val strings = LocalStrings.current

    Column(modifier = Modifier.fillMaxSize()) {
        // --- CATEGORY TABS ---
        ScrollableTabRow(
            selectedTabIndex = selectedMusicTab,
            edgePadding = 16.dp,
            containerColor = Color.Transparent,
            divider = {},
            indicator = {}
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedMusicTab == index
                val cornerRadius by animateDpAsState(
                    targetValue = if (isSelected) 24.dp else 12.dp,
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "tabCorner"
                )
                
                Tab(
                    selected = isSelected,
                    onClick = {
                        onSelectedMusicTabChange(index)
                        onBrowsingArtistChange(null)
                        onBrowsingAlbumChange(null)
                        onBrowsingPlaylistChange(null)
                        onBrowsingFolderChange(null)
                    },
                    modifier = Modifier
                        .padding(vertical = 12.dp, horizontal = 4.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(cornerRadius))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                        .then(
                            if (isSelected) Modifier.border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(cornerRadius)
                            ) else Modifier
                        ),
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge.copy(
                                letterSpacing = 0.2.sp
                            ),
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        // --- TOOLBAR (Shuffle, View Mode, Sort) ---
        if (selectedMusicTab != 6 && selectedMusicTab != 7 && selectedMusicTab != 8) { // No toolbar for Queue, Videos, Settings
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        player?.stop()
                        player?.clearMediaItems()
                        val shuffled = finalSortedSongs.shuffled()
                        val mediaItems = shuffled.map { it.toMediaItem() }
                        player?.addMediaItems(mediaItems)
                        player?.prepare()
                        player?.play()
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Shuffle, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Shuffle", 
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(Modifier.weight(1f))

                ToolbarActionIconButton(
                    onClick = { settings.onUseGridViewLibraryChange(!settings.useGridViewLibrary) },
                    icon = if (settings.useGridViewLibrary) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView
                )

                Spacer(Modifier.width(12.dp))

                ToolbarActionIconButton(
                    onClick = { onCurrentTabChange(1) },
                    icon = Icons.Default.Search
                )

                Spacer(Modifier.width(12.dp))

                var showSortBottomSheet by remember { mutableStateOf(false) }
                Box {
                    ToolbarActionIconButton(
                        onClick = { showSortBottomSheet = true },
                        icon = Icons.AutoMirrored.Filled.Sort
                    )
                    
                    if (showSortBottomSheet) {
                        ModalBottomSheet(
                            onDismissRequest = { showSortBottomSheet = false },
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 32.dp)
                            ) {
                                Text(
                                    text = "Ordenar por",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(24.dp)
                                )
                                
                                val sortOptions = listOf(
                                    Triple(0, strings.recentlyAdded, Icons.Default.Schedule),
                                    Triple(1, "A-Z", Icons.Default.SortByAlpha),
                                    Triple(2, strings.favorites, Icons.Default.Favorite),
                                    Triple(3, strings.artists, Icons.Default.Person),
                                    Triple(4, strings.albums, Icons.Default.Album)
                                )
                                
                                sortOptions.forEach { (id, label, icon) ->
                                    Surface(
                                        onClick = { 
                                            onSortOrderChange(id)
                                            showSortBottomSheet = false 
                                        },
                                        color = if (sortOrder == id) MaterialTheme.colorScheme.primaryContainer 
                                                else Color.Transparent,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .padding(horizontal = 24.dp, vertical = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = if (sortOrder == id) MaterialTheme.colorScheme.primary 
                                                       else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(Modifier.width(16.dp))
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = if (sortOrder == id) FontWeight.Bold else FontWeight.Normal,
                                                color = if (sortOrder == id) MaterialTheme.colorScheme.onPrimaryContainer 
                                                       else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (sortOrder == id) {
                                                Spacer(Modifier.weight(1f))
                                                Icon(
                                                    Icons.Default.Check,
                                                    null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
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

        // --- CONTENT AREA ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            BackHandler(enabled = browsingArtist != null || browsingAlbum != null || browsingPlaylist != null || browsingFolder != null) {
                onBrowsingArtistChange(null)
                onBrowsingAlbumChange(null)
                onBrowsingPlaylistChange(null)
                onBrowsingFolderChange(null)
            }

            AnimatedContent(
                targetState = selectedMusicTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220, delayMillis = 90)) togetherWith
                            fadeOut(animationSpec = tween(90))
                }, label = "MusicTabTransition"
            ) { targetMusicTab ->
                when (targetMusicTab) {
                    0 -> { // ALL SONGS
                        if (settings.useGridViewLibrary) {
                            val itemsPerRow = 2
                            val rows = finalSortedSongs.chunked(itemsPerRow)
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 140.dp, top = 8.dp)
                            ) {
                                items(rows.size) { rowIndex ->
                                    val rowItems = rows[rowIndex]
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        rowItems.forEach { song ->
                                            CompactAlbumCard(
                                                song = song,
                                                onSongClick = { s ->
                                                    player?.stop()
                                                    player?.clearMediaItems()
                                                    
                                                    val songsToPlay = finalSortedSongs
                                                    val startIndex = songsToPlay.indexOf(s)
                                                    
                                                    val finalOrder = if (startIndex != -1) {
                                                        songsToPlay.subList(startIndex, songsToPlay.size) + songsToPlay.subList(0, startIndex)
                                                    } else {
                                                        listOf(s)
                                                    }
                                                    
                                                    val mediaItems = finalOrder.map { it.toMediaItem() }
                                                    player?.addMediaItems(mediaItems)
                                                    player?.prepare()
                                                    player?.play()
                                                },
                                                getAlbumArt = { settings.getAlbumArt(it) },
                                                settings = settings,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        if (rowItems.size < itemsPerRow) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        } else {
                            player?.let { p ->
                                components.MusicList(
                                    finalSortedSongs,
                                    p,
                                    isPlaying,
                                    MusicListConfig(
                                        activeMediaItem = p.currentMediaItem,
                                        favoriteIds = favoriteIds,
                                        onToggleFavorite = toggleFavorite,
                                        onAddToPlaylist = onAddToPlaylist,
                                        onAddToQueue = { song ->
                                            p.addMediaItem(song.toMediaItem())
                                            Toast.makeText(context, strings.addedToQueue, Toast.LENGTH_SHORT).show()
                                        },
                                        onPlayNext = { song ->
                                            val index = if (p.mediaItemCount == 0) 0 else p.currentMediaItemIndex + 1
                                            p.addMediaItem(index, song.toMediaItem())
                                            Toast.makeText(context, strings.addedToQueue, Toast.LENGTH_SHORT).show()
                                        },
                                        onPlayAll = { songsToPlay, startIndex ->
                                            p.stop()
                                            p.clearMediaItems()
                                            
                                            val finalOrder = if (startIndex != -1) {
                                                songsToPlay.subList(startIndex, songsToPlay.size) + songsToPlay.subList(0, startIndex)
                                            } else {
                                                songsToPlay
                                            }
                                            
                                            val mediaItems = finalOrder.map { it.toMediaItem() }
                                            p.addMediaItems(mediaItems)
                                            p.prepare()
                                            p.play()
                                        },
                                        onDeleteSong = { song ->
                                            try {
                                                val file = File(song.data)
                                                if (file.exists() && file.delete()) {
                                                    onSongListChange(songList.filter { it.id != song.id })
                                                    Toast.makeText(context, strings.libraryUpdated, Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Exception) {}
                                        },
                                        onUpdateTags = handleUpdateTags,
                                        settings = settings
                                    )
                                )
                            }
                        }
                    }

                    1 -> { // ARTISTS
                        AnimatedContent(
                            targetState = browsingArtist,
                            transitionSpec = {
                                if (targetState != null) {
                                    slideInHorizontally { it } + fadeIn() togetherWith
                                            slideOutHorizontally { -it / 2 } + fadeOut()
                                } else {
                                    slideInHorizontally { -it / 2 } + fadeIn() togetherWith
                                            slideOutHorizontally { it } + fadeOut()
                                }.using(SizeTransform(clip = false))
                            }, label = "ArtistDrillDown"
                        ) { currentBrowsingArtist ->
                            if (currentBrowsingArtist == null) {
                                components.ArtistList(
                                    songsByArtist,
                                    player?.currentMediaItem,
                                    { songs -> onBrowsingArtistChange(songs.firstOrNull()?.artist) },
                                    settings,
                                    searchQuery
                                )
                            } else {
                                val artistSongs = (songsByArtist[currentBrowsingArtist] ?: emptyList()).filter {
                                    it.title.contains(searchQuery, ignoreCase = true)
                                }
                                player?.let { p ->
                                    components.SubMusicList(
                                        currentBrowsingArtist,
                                        artistSongs,
                                        p,
                                        isPlaying,
                                        MusicListConfig(
                                            activeMediaItem = p.currentMediaItem,
                                            favoriteIds = favoriteIds,
                                            onToggleFavorite = toggleFavorite,
                                            onAddToQueue = { song ->
                                                p.addMediaItem(song.toMediaItem())
                                                Toast.makeText(context, strings.addedToQueue, Toast.LENGTH_SHORT).show()
                                            },
                                            onPlayNext = { song ->
                                                val index = if (p.mediaItemCount == 0) 0 else p.currentMediaItemIndex + 1
                                                p.addMediaItem(index, song.toMediaItem())
                                                Toast.makeText(context, strings.addedToQueue, Toast.LENGTH_SHORT).show()
                                            },
                                            onPlayAll = { songsToPlay, startIndex ->
                                                p.stop()
                                                p.clearMediaItems()
                                                
                                                val finalOrder = if (startIndex != -1) {
                                                    songsToPlay.subList(startIndex, songsToPlay.size) + songsToPlay.subList(0, startIndex)
                                                } else {
                                                    songsToPlay
                                                }
                                                
                                                val mediaItems = finalOrder.map { it.toMediaItem() }
                                                
                                                p.addMediaItems(mediaItems)
                                                p.prepare()
                                                p.play()
                                            },
                                            onDeleteSong = { song ->
                                                try {
                                                    val file = File(song.data)
                                                    if (file.exists() && file.delete()) {
                                                        onSongListChange(songList.filter { it.id != song.id })
                                                        Toast.makeText(context, strings.libraryUpdated, Toast.LENGTH_SHORT).show()
                                                    }
                                                } catch (e: Exception) {}
                                            },
                                            onUpdateTags = handleUpdateTags,
                                            settings = settings
                                        ),
                                        { onBrowsingArtistChange(null) },
                                        null,
                                        null
                                    )
                                }
                            }
                        }
                    }

                    2 -> { // ALBUMS
                        AnimatedContent(
                            targetState = browsingAlbum,
                            transitionSpec = {
                                if (targetState != null) {
                                    slideInHorizontally { it } + fadeIn() togetherWith
                                            slideOutHorizontally { -it / 2 } + fadeOut()
                                } else {
                                    slideInHorizontally { -it / 2 } + fadeIn() togetherWith
                                            slideOutHorizontally { it } + fadeOut()
                                }.using(SizeTransform(clip = false))
                            }, label = "AlbumDrillDown"
                        ) { currentBrowsingAlbum ->
                            if (currentBrowsingAlbum == null) {
                                components.AlbumList(
                                    songsByAlbum,
                                    player?.currentMediaItem,
                                    { songs -> onBrowsingAlbumChange(songs.firstOrNull()?.album) },
                                    settings,
                                    searchQuery
                                )
                            } else {
                                val albumSongs = (songsByAlbum[currentBrowsingAlbum] ?: emptyList()).filter {
                                    it.title.contains(searchQuery, ignoreCase = true)
                                }
                                player?.let { p ->
                                    components.SubMusicList(
                                        currentBrowsingAlbum,
                                        albumSongs,
                                        p,
                                        isPlaying,
                                        MusicListConfig(
                                            activeMediaItem = p.currentMediaItem,
                                            favoriteIds = favoriteIds,
                                            onToggleFavorite = toggleFavorite,
                                            onAddToQueue = { song ->
                                                p.addMediaItem(song.toMediaItem())
                                                Toast.makeText(context, strings.addedToQueue, Toast.LENGTH_SHORT).show()
                                            },
                                            onPlayNext = { song ->
                                                val index = if (p.mediaItemCount == 0) 0 else p.currentMediaItemIndex + 1
                                                p.addMediaItem(index, song.toMediaItem())
                                                Toast.makeText(context, strings.addedToQueue, Toast.LENGTH_SHORT).show()
                                            },
                                            onPlayAll = { s, startIdx ->
                                                p.stop()
                                                p.clearMediaItems()
                                                
                                                val finalOrder = if (startIdx != -1) {
                                                    s.subList(startIdx, s.size) + s.subList(0, startIdx)
                                                } else {
                                                    s
                                                }
                                                
                                                val mediaItems = finalOrder.map { it.toMediaItem() }
                                                p.addMediaItems(mediaItems)
                                                p.prepare()
                                                p.play()
                                            },
                                            onAddToPlaylist = onAddToPlaylist,
                                            onDeleteSong = { song ->
                                                try {
                                                    val file = File(song.data)
                                                    if (file.exists() && file.delete()) {
                                                        onSongListChange(songList.filter { it.id != song.id })
                                                        Toast.makeText(context, strings.libraryUpdated, Toast.LENGTH_SHORT).show()
                                                    }
                                                } catch (e: Exception) {}
                                            },
                                            onUpdateTags = handleUpdateTags,
                                            settings = settings
                                        ),
                                        { onBrowsingAlbumChange(null) },
                                        null,
                                        null
                                    )
                                }
                            }
                        }
                    }

                    3 -> { // PLAYLISTS
                        AnimatedContent(
                            targetState = browsingPlaylist,
                            transitionSpec = {
                                if (targetState != null) {
                                    slideInHorizontally { it } + fadeIn() togetherWith
                                            slideOutHorizontally { -it / 2 } + fadeOut()
                                } else {
                                    slideInHorizontally { -it / 2 } + fadeIn() togetherWith
                                            slideOutHorizontally { it } + fadeOut()
                                }.using(SizeTransform(clip = false))
                            }, label = "PlaylistDrillDown"
                        ) { currentBrowsingPlaylist ->
                            if (currentBrowsingPlaylist == null) {
                                val filteredPlaylists = if (searchQuery.isEmpty()) playlistsNames
                                else playlistsNames.filter { it.contains(searchQuery, ignoreCase = true) }

                                components.PlaylistList(
                                    filteredPlaylists.toSet(),
                                    { name -> onBrowsingPlaylistChange(name) },
                                    onCreatePlaylist,
                                    { name -> onDeletePlaylist(name) },
                                    { oldName, newName -> onRenamePlaylist(oldName, newName) },
                                    { name, imageUri -> onSetPlaylistImage(name, imageUri) },
                                    songList,
                                    settings
                                )
                            } else {
                                val playlistSongsIds = PlaylistManager.getSongsInPlaylist(context, currentBrowsingPlaylist)
                                val playlistSongs = songList.filter { it.id in playlistSongsIds }
                                    .filter { it.title.contains(searchQuery, ignoreCase = true) }

                                player?.let { p ->
                                    val playlistName = currentBrowsingPlaylist
                                    components.SubMusicList(
                                        playlistName,
                                        playlistSongs,
                                        p,
                                        isPlaying,
                                        MusicListConfig(
                                            activeMediaItem = p.currentMediaItem,
                                            favoriteIds = favoriteIds,
                                            onToggleFavorite = toggleFavorite,
                                            onAddToQueue = { song ->
                                                p.addMediaItem(song.toMediaItem())
                                                Toast.makeText(context, strings.addedToQueue, Toast.LENGTH_SHORT).show()
                                            },
                                            onPlayNext = { song ->
                                                val index = if (p.mediaItemCount == 0) 0 else p.currentMediaItemIndex + 1
                                                p.addMediaItem(index, song.toMediaItem())
                                                Toast.makeText(context, strings.addedToQueue, Toast.LENGTH_SHORT).show()
                                            },
                                            onPlayAll = { s, startIdx ->
                                                p.stop()
                                                p.clearMediaItems()
                                                
                                                val finalOrder = if (startIdx != -1) {
                                                    s.subList(startIdx, s.size) + s.subList(0, startIdx)
                                                } else {
                                                    s
                                                }
                                                
                                                val mediaItems = finalOrder.map { it.toMediaItem() }
                                                p.addMediaItems(mediaItems)
                                                p.prepare()
                                                p.play()
                                            },
                                            onAddToPlaylist = onAddToPlaylist,
                                            onRemoveFromPlaylist = { song ->
                                                PlaylistManager.removeSongFromPlaylist(context, playlistName, song.id)
                                                onBrowsingPlaylistChange(playlistName) // Refresh
                                                Toast.makeText(context, strings.libraryUpdated, Toast.LENGTH_SHORT).show()
                                            },
                                            onDeleteSong = { song ->
                                                try {
                                                    val file = File(song.data)
                                                    if (file.exists() && file.delete()) {
                                                        onSongListChange(songList.filter { it.id != song.id })
                                                        Toast.makeText(context, strings.libraryUpdated, Toast.LENGTH_SHORT).show()
                                                    }
                                                } catch (e: Exception) {}
                                            },
                                            onUpdateTags = handleUpdateTags,
                                            settings = settings
                                        ),
                                        { onBrowsingPlaylistChange(null) },
                                        PlaylistManager.getPlaylistImage(context, playlistName),
                                        { onEditPlaylist(playlistName) }
                                    )
                                }
                            }
                        }
                    }

                    4 -> { // FOLDERS
                        AnimatedContent(
                            targetState = browsingFolder,
                            transitionSpec = {
                                if (targetState != null) {
                                    slideInHorizontally { it } + fadeIn() togetherWith
                                            slideOutHorizontally { -it / 2 } + fadeOut()
                                } else {
                                    slideInHorizontally { -it / 2 } + fadeIn() togetherWith
                                            slideOutHorizontally { it } + fadeOut()
                                }.using(SizeTransform(clip = false))
                            }, label = "FolderDrillDown"
                        ) { currentBrowsingFolder ->
                            if (currentBrowsingFolder == null) {
                                components.FolderList(
                                    songsByFolder,
                                    { songs -> onBrowsingFolderChange(File(songs.first().data).parent) },
                                    onIgnoreFolder,
                                    ignoredFolders,
                                    settings,
                                    searchQuery
                                )
                            } else {
                                val folderSongs = (songsByFolder[currentBrowsingFolder] ?: emptyList()).filter {
                                    it.title.contains(searchQuery, ignoreCase = true)
                                }
                                player?.let { p ->
                                    components.SubMusicList(
                                        currentBrowsingFolder.substringAfterLast("/"),
                                        folderSongs,
                                        p,
                                        isPlaying,
                                        MusicListConfig(
                                            activeMediaItem = p.currentMediaItem,
                                            favoriteIds = favoriteIds,
                                            onToggleFavorite = toggleFavorite,
                                            onAddToQueue = { song ->
                                                p.addMediaItem(song.toMediaItem())
                                                Toast.makeText(context, strings.addedToQueue, Toast.LENGTH_SHORT).show()
                                            },
                                            onPlayNext = { song ->
                                                val index = if (p.mediaItemCount == 0) 0 else p.currentMediaItemIndex + 1
                                                p.addMediaItem(index, song.toMediaItem())
                                                Toast.makeText(context, strings.addedToQueue, Toast.LENGTH_SHORT).show()
                                            },
                                            onPlayAll = { s, startIdx ->
                                                p.stop()
                                                p.clearMediaItems()
                                                
                                                val finalOrder = if (startIdx != -1) {
                                                    s.subList(startIdx, s.size) + s.subList(0, startIdx)
                                                } else {
                                                    s
                                                }
                                                
                                                val mediaItems = finalOrder.map { it.toMediaItem() }
                                                p.addMediaItems(mediaItems)
                                                p.prepare()
                                                p.play()
                                            },
                                            onAddToPlaylist = onAddToPlaylist,
                                            onDeleteSong = { song ->
                                                try {
                                                    val file = File(song.data)
                                                    if (file.exists() && file.delete()) {
                                                        onSongListChange(songList.filter { it.id != song.id })
                                                        Toast.makeText(context, strings.libraryUpdated, Toast.LENGTH_SHORT).show()
                                                    }
                                                } catch (e: Exception) {}
                                            },
                                            onUpdateTags = handleUpdateTags,
                                            settings = settings
                                        ),
                                        { onBrowsingFolderChange(null) },
                                        null,
                                        null
                                    )
                                }
                            }
                        }
                    }

                    5 -> { // FAVORITES
                        player?.let { p ->
                            components.MusicList(
                                favoriteSongs.filter { it.title.contains(searchQuery, ignoreCase = true) },
                                p,
                                isPlaying,
                                MusicListConfig(
                                    activeMediaItem = p.currentMediaItem,
                                    favoriteIds = favoriteIds,
                                    onToggleFavorite = toggleFavorite,
                                    onAddToPlaylist = onAddToPlaylist,
                                    onAddToQueue = { song ->
                                        p.addMediaItem(song.toMediaItem())
                                        Toast.makeText(context, strings.addedToQueue, Toast.LENGTH_SHORT).show()
                                    },
                                    onPlayNext = { song ->
                                        val index = if (p.mediaItemCount == 0) 0 else p.currentMediaItemIndex + 1
                                        p.addMediaItem(index, song.toMediaItem())
                                        Toast.makeText(context, strings.addedToQueue, Toast.LENGTH_SHORT).show()
                                    },
                                    onDeleteSong = { song ->
                                        try {
                                            val file = File(song.data)
                                            if (file.exists() && file.delete()) {
                                                onSongListChange(songList.filter { it.id != song.id })
                                                Toast.makeText(context, strings.libraryUpdated, Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {}
                                    },
                                    onUpdateTags = handleUpdateTags,
                                    settings = settings,
                                    emptyMessage = strings.noFavorites
                                )
                            )
                        }
                    }

                    6 -> { // QUEUE
                        val queueItems = remember(player?.mediaItemCount) {
                            val p = player ?: return@remember emptyList()
                            (0 until p.mediaItemCount).map { p.getMediaItemAt(it) }
                        }
                        val queueSongs = queueItems.mapNotNull { mi -> songList.find { it.id.toString() == mi.mediaId } }

                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Siguiente en la cola",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (queueSongs.isNotEmpty()) {
                                    IconButton(onClick = { onShowClearQueueConfirmChange(true) }) {
                                        Icon(Icons.Default.Delete, contentDescription = strings.clearQueue, tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }

                            player?.let { p ->
                                components.MusicList(
                                    queueSongs,
                                    p,
                                    isPlaying,
                                    MusicListConfig(
                                        activeMediaItem = p.currentMediaItem,
                                        favoriteIds = favoriteIds,
                                        onToggleFavorite = toggleFavorite,
                                        onAddToPlaylist = onAddToPlaylist,
                                        onAddToQueue = { song ->
                                            p.addMediaItem(song.toMediaItem())
                                            Toast.makeText(context, strings.addedToQueue, Toast.LENGTH_SHORT).show()
                                        },
                                        onPlayNext = { song ->
                                            val index = if (p.mediaItemCount == 0) 0 else p.currentMediaItemIndex + 1
                                            p.addMediaItem(index, song.toMediaItem())
                                            Toast.makeText(context, strings.addedToQueue, Toast.LENGTH_SHORT).show()
                                        },
                                        onRemoveFromQueue = { index -> p.removeMediaItem(index) },
                                        onMoveQueueItem = { from, to -> p.moveMediaItem(from, to) },
                                        isDraggable = true,
                                        onDeleteSong = { song ->
                                            try {
                                                val file = File(song.data)
                                                if (file.exists() && file.delete()) {
                                                    onSongListChange(songList.filter { it.id != song.id })
                                                    Toast.makeText(context, strings.libraryUpdated, Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Exception) {}
                                        },
                                        onUpdateTags = handleUpdateTags,
                                        settings = settings,
                                        emptyMessage = "La cola está vacía"
                                    )
                                )
                            }
                        }
                    }

                    7 -> { // VIDEOS
                        val folderPickerLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.OpenDocumentTree()
                        ) { uri: Uri? ->
                            uri?.let {
                                val path = getPathFromUri(context, it)
                                if (path != null) {
                                    settings.onToggleExtraVideoPath(path)
                                } else {
                                    Toast.makeText(context, "No se pudo obtener la ruta de la carpeta", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Mis Videos",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(onClick = { folderPickerLauncher.launch(null) }) {
                                    Icon(Icons.Default.CreateNewFolder, contentDescription = "Añadir carpeta de videos")
                                }
                            }
                            
                            if (settings.extraVideoPaths.isNotEmpty()) {
                                Text(
                                    text = "Carpetas añadidas: ${settings.extraVideoPaths.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }

                            VideoList(
                                videos = videos,
                                onVideoClick = { video ->
                                    if (player?.currentMediaItem?.mediaId != video.id.toString()) {
                                        player?.setMediaItem(video.toMediaItem())
                                        player?.prepare()
                                        player?.play()
                                    }
                                },
                                settings = settings
                            )
                        }
                    }

                    8 -> { // SETTINGS (INTERNAL)
                        // This would need more extraction if we want to keep it here
                    }
                }
            }
        }
    }
}
