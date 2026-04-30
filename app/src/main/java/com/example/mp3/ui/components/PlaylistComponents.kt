package com.example.mp3.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.mp3.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PlaylistList(
    playlists: Set<String>,
    onPlaylistClick: (String) -> Unit,
    onCreatePlaylist: () -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onRenamePlaylist: (String, String) -> Unit,
    onSetPlaylistImage: (String, String?) -> Unit,
    songs: List<Song>,
    settings: com.example.mp3.ui.screens.PlayerSettings
) {
    val context = LocalContext.current
    val strings = LocalStrings.current

    // EL LAUNCHER DEBE IR AQUÍ (Fuera de cualquier loop o condición)
    var currentTargetPlaylist by remember { mutableStateOf<String?>(null) }
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            currentTargetPlaylist?.let { name ->
                onSetPlaylistImage(name, it.toString())
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ... (resto del código igual hasta el LazyColumn)
        // Cabecera con título y botón de crear
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    strings.playlists,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "${playlists.size} ${if (playlists.size == 1) strings.playlists.dropLast(1) else strings.playlists}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            IconButton(
                onClick = onCreatePlaylist,
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(28.dp))
            ) {
                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }

        if (playlists.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.AutoMirrored.Filled.PlaylistPlay, 
                        null, 
                        modifier = Modifier.size(80.dp), 
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = strings.noPlaylists,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            val itemsPerRow = 2
            val rows = playlists.toList().sorted().chunked(itemsPerRow)
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                rows.forEach { rowPlaylists ->
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            rowPlaylists.forEach { playlistName ->
                                val playlistSongsIds = remember(playlistName) {
                                    PlaylistManager.getSongsInPlaylist(context, playlistName)
                                }
                                val playlistSongs = remember(playlistSongsIds, songs) {
                                    songs.filter { it.id in playlistSongsIds }
                                }
                                val songCount = playlistSongs.size
                                // Agregamos 'playlists' como clave para que se refresque cuando el set cambie
                                val customImagePath = remember(playlistName, playlists) {
                                    PlaylistManager.getPlaylistImage(context, playlistName)
                                }

                                var showOptions by remember { mutableStateOf(false) }
                                var showRenameDialog by remember { mutableStateOf(false) }

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(28.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            RoundedCornerShape(28.dp)
                                        )
                                        .clickable { onPlaylistClick(playlistName) }
                                ) {
                                    // Mosaico de Portada Premium
                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(28.dp))
                                    ) {
                                        if (customImagePath != null) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(File(customImagePath))
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else if (playlistSongs.isEmpty()) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.PlaylistPlay, 
                                                null, 
                                                modifier = Modifier.size(48.dp).align(Alignment.Center),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                                            )
                                        } else {
                                            // Diseño de mosaico (2x2) si hay suficientes canciones
                                            val displaySongs = playlistSongs.take(4)
                                            Column(modifier = Modifier.fillMaxSize()) {
                                                Row(modifier = Modifier.weight(1f)) {
                                                    displaySongs.getOrNull(0)?.let { s ->
                                                        AsyncImage(
                                                            model = ImageRequest.Builder(LocalContext.current).data(s.data).crossfade(true).build(),
                                                            contentDescription = null,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.weight(1f).fillMaxHeight()
                                                        )
                                                    }
                                                    displaySongs.getOrNull(1)?.let { s ->
                                                        AsyncImage(
                                                            model = ImageRequest.Builder(LocalContext.current).data(s.data).crossfade(true).build(),
                                                            contentDescription = null,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.weight(1f).fillMaxHeight()
                                                        )
                                                    }
                                                }
                                                if (displaySongs.size > 2) {
                                                    Row(modifier = Modifier.weight(1f)) {
                                                        displaySongs.getOrNull(2)?.let { s ->
                                                            AsyncImage(
                                                                model = ImageRequest.Builder(LocalContext.current).data(s.data).crossfade(true).build(),
                                                                contentDescription = null,
                                                                contentScale = ContentScale.Crop,
                                                                modifier = Modifier.weight(1f).fillMaxHeight()
                                                            )
                                                        }
                                                        displaySongs.getOrNull(3)?.let { s ->
                                                            AsyncImage(
                                                                model = ImageRequest.Builder(LocalContext.current).data(s.data).crossfade(true).build(),
                                                                contentDescription = null,
                                                                contentScale = ContentScale.Crop,
                                                                modifier = Modifier.weight(1f).fillMaxHeight()
                                                            )
                                                        }
                                                    }
                                                } else if (displaySongs.size == 1) {
                                                    // Si solo hay 1, se expande
                                                }
                                            }
                                        }

                                        // Botón de opciones discreto
                                        IconButton(
                                            onClick = { showOptions = true },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(8.dp)
                                                .size(32.dp)
                                                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.MoreVert, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface)
                                        }

                                        if (showOptions) {
                                            DropdownMenu(
                                                expanded = showOptions,
                                                onDismissRequest = { showOptions = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text(strings.editPlaylist) },
                                                    onClick = {
                                                        showOptions = false
                                                        showRenameDialog = true
                                                    },
                                                    leadingIcon = { Icon(Icons.Default.Edit, null) }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(strings.changeCover) },
                                                    onClick = {
                                                        showOptions = false
                                                        currentTargetPlaylist = playlistName
                                                        launcher.launch("image/*")
                                                    },
                                                    leadingIcon = { Icon(Icons.Default.Image, null) }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(strings.deletePlaylist, color = MaterialTheme.colorScheme.error) },
                                                    onClick = {
                                                        showOptions = false
                                                        onDeletePlaylist(playlistName)
                                                    },
                                                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                                                )
                                            }
                                        }
                                    }

                                    if (showRenameDialog) {
                                        var editingName by remember { mutableStateOf(playlistName) }
                                        AlertDialog(
                                            onDismissRequest = { showRenameDialog = false },
                                            title = { 
                                                Column {
                                                    Text(strings.editPlaylist, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                                    Text(strings.enterPlaylistName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            },
                                            shape = RoundedCornerShape(32.dp),
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            text = {
                                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                                    OutlinedTextField(
                                                        value = editingName,
                                                        onValueChange = { editingName = it },
                                                        placeholder = { Text(playlistName) },
                                                        singleLine = true,
                                                        modifier = Modifier.fillMaxWidth(),
                                                        shape = RoundedCornerShape(16.dp),
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                            unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                                                            unfocusedLabelColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                                        )
                                                    )
                                                    
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    
                                                    // Botón rápido para cambiar imagen dentro del diálogo de edición
                                                    FilledTonalButton(
                                                        onClick = {
                                                            currentTargetPlaylist = playlistName
                                                            launcher.launch("image/*")
                                                        },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        shape = RoundedCornerShape(16.dp)
                                                    ) {
                                                        Icon(Icons.Default.Image, null)
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(strings.changeCover)
                                                    }
                                                }
                                            },
                                            confirmButton = {
                                                Button(
                                                    onClick = {
                                                        if (editingName.isNotBlank() && editingName != playlistName) {
                                                            onRenamePlaylist(playlistName, editingName)
                                                        }
                                                        showRenameDialog = false
                                                    },
                                                    shape = RoundedCornerShape(16.dp)
                                                ) {
                                                    Text(strings.save)
                                                }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = { showRenameDialog = false }) {
                                                    Text(strings.cancel)
                                                }
                                            }
                                        )
                                    }

                                    Column(
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Text(
                                            text = playlistName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "$songCount ${if (songCount == 1) strings.songs.dropLast(1) else strings.songs}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            // Rellenar espacios
                            repeat(itemsPerRow - rowPlaylists.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

