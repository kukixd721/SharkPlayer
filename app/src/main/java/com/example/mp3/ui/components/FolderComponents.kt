package com.example.mp3.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mp3.LocalStrings
import com.example.mp3.Song
import java.io.File

@Composable
fun FolderList(
    folders: Map<String, List<Song>>,
    onFolderClick: (List<Song>) -> Unit,
    onIgnoreFolder: (String) -> Unit,
    ignoredFolders: Set<String>,
    settings: com.example.mp3.ui.screens.PlayerSettings,
    searchQuery: String = ""
) {
    val strings = LocalStrings.current
    if (folders.isEmpty() && ignoredFolders.isEmpty()) {
        EmptyState(
            icon = Icons.Default.FolderOpen,
            message = strings.noFolders
        )
    } else {
        val sortedFolders = remember(folders) {
            folders.keys.toList().sorted()
        }

        val filteredFolders = remember(sortedFolders, searchQuery, ignoredFolders) {
            sortedFolders.filter { folderPath ->
                val folderName = folderPath.substringAfterLast("/")
                val matchesSearch = searchQuery.isEmpty() || folderName.contains(searchQuery, ignoreCase = true)
                matchesSearch
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredFolders) { folderPath ->
                val songs = folders[folderPath] ?: emptyList()
                val songCount = songs.size
                val folderName = folderPath.substringAfterLast("/")
                val isIgnored = ignoredFolders.contains(folderPath)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (isIgnored) 0.6f else 1f)
                        .clickable { onFolderClick(songs) },
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isIgnored) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isIgnored) MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
                ) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = folderName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isIgnored) MaterialTheme.colorScheme.error 
                                        else MaterialTheme.colorScheme.primary // Título con color dinámico primario
                            )
                        },
                        supportingContent = {
                            Text(
                                text = "$folderPath • $songCount ${if (songCount == 1) strings.songs.dropLast(1) else strings.songs}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isIgnored) MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        },
                        leadingContent = {
                            Surface(
                                modifier = Modifier.size(60.dp), // Icono más grande y expresivo
                                shape = RoundedCornerShape(22.dp),
                                color = if (isIgnored) MaterialTheme.colorScheme.errorContainer 
                                        else MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        if (isIgnored) Icons.Default.FolderOff else Icons.Default.Folder,
                                        null,
                                        tint = if (isIgnored) MaterialTheme.colorScheme.onErrorContainer 
                                               else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp) // Icono interno más grande
                                    )
                                }
                            }
                        },
                        trailingContent = {
                            IconButton(onClick = { onIgnoreFolder(folderPath) }) {
                                Icon(
                                    if (isIgnored) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    null,
                                    tint = if (isIgnored) MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                                           else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}
