package com.example.mp3.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Image
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.mp3.LocalStrings
import com.example.mp3.Song
import com.example.mp3.SongDetails
import com.example.mp3.getAlbumArt
import android.graphics.BitmapFactory

@Composable
fun TagEditorDialog(
    song: Song,
    details: SongDetails,
    onDismiss: () -> Unit,
    onSave: (title: String, artist: String, album: String, year: String, track: String, genre: String, artwork: ByteArray?) -> Unit
) {
    var title by remember { mutableStateOf(song.title) }
    var artist by remember { mutableStateOf(song.artist) }
    var album by remember { mutableStateOf(song.album) }
    var year by remember { mutableStateOf(details.year ?: "") }
    var track by remember { mutableStateOf(details.trackNumber ?: "") }
    var genre by remember { mutableStateOf(details.genre ?: "") }
    var artwork by remember { mutableStateOf<ByteArray?>(null) }
    var artworkBitmap by remember { mutableStateOf(getAlbumArt(song.data)?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }) }

    val context = LocalContext.current
    val strings = LocalStrings.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                val bytes = stream.readBytes()
                artwork = bytes
                artworkBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.editTags) },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Selector de carátula
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (artworkBitmap != null) {
                        Image(
                            bitmap = artworkBitmap!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                androidx.compose.material.icons.Icons.Default.Image,
                                null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(strings.noArt, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                val textFieldShape = RoundedCornerShape(16.dp)
                val customTextFieldColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(strings.title) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = textFieldShape,
                    colors = customTextFieldColors
                )
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text(strings.artist) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = textFieldShape,
                    colors = customTextFieldColors
                )
                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    label = { Text(strings.album) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = textFieldShape,
                    colors = customTextFieldColors
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = year,
                        onValueChange = { year = it },
                        label = { Text(strings.year) },
                        modifier = Modifier.weight(1f),
                        shape = textFieldShape,
                        colors = customTextFieldColors
                    )
                    OutlinedTextField(
                        value = track,
                        onValueChange = { track = it },
                        label = { Text(strings.trackNumber) },
                        modifier = Modifier.weight(1f),
                        shape = textFieldShape,
                        colors = customTextFieldColors
                    )
                }
                OutlinedTextField(
                    value = genre,
                    onValueChange = { genre = it },
                    label = { Text(strings.genre) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = textFieldShape,
                    colors = customTextFieldColors
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(title, artist, album, year, track, genre, artwork)
            }) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}