package com.example.mp3.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.mp3.LocalStrings
import com.example.mp3.Song
import com.example.mp3.ui.screens.PlayerSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ControlBar(
    player: Player,
    isPlaying: Boolean,
    currentPosition: Long,
    totalDuration: Long,
    isShuffleOn: Boolean,
    repeatMode: Int,
    songList: List<Song>,
    settings: PlayerSettings,
    onTitleClick: () -> Unit,
    onCollapse: () -> Unit,
    getAlbumArt: (String) -> ByteArray?,
    forceCloseVideo: Boolean = false,
    onRestoreVideo: () -> Unit = {}
) {
    val strings = LocalStrings.current
    val currentMedia = player.currentMediaItem
    val currentSong = songList.find { it.title == currentMedia?.mediaMetadata?.title.toString() }

    val albumArt by produceState<ByteArray?>(null, currentSong) {
        value = withContext(Dispatchers.IO) {
            currentSong?.let { getAlbumArt(it.data) }
        }
    }

    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    val containerCornerRadius by animateDpAsState(
        targetValue = if (isPlaying) 44.dp else 24.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ControlBarCornerRadius"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .clickable { onTitleClick() },
        shape = RoundedCornerShape(containerCornerRadius),
        color = if (settings.backgroundImageUri != null) surfaceColor.copy(alpha = settings.backgroundAlpha) else surfaceColor,
        tonalElevation = 0.dp,
        shadowElevation = if (settings.backgroundImageUri != null) 0.dp else 8.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center // Mejor centrado para el contenido reducido
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ... (Vinyl animation unchanged)
                val infiniteTransition = rememberInfiniteTransition(label = "VinylRotation")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(12000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "Rotation"
                )

                Box(
                    modifier = Modifier.size(54.dp), // Reducido de 65.dp
                    contentAlignment = Alignment.Center
                ) {
                    val currentRotation = if (isPlaying) rotation else 0f
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(rotationZ = currentRotation),
                        shape = CircleShape,
                        color = Color(0xFF121212)
                    ) {}

                    Surface(
                        modifier = Modifier
                            .size(36.dp) // Reducido de 42.dp
                            .graphicsLayer(rotationZ = currentRotation),
                        shape = CircleShape
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(albumArt ?: currentSong?.data?.let { java.io.File(it) })
                                .crossfade(true).build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            error = rememberVectorPainter(Icons.Default.MusicNote)
                        )
                    }

                    Surface(
                        modifier = Modifier.size(3.dp), // Reducido de 4.dp
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface
                    ) {}
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentMedia?.mediaMetadata?.title?.toString() ?: strings.sharkPlayer,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp), // Reducido de 16.sp
                        fontWeight = FontWeight.ExtraBold,
                        color = onSurfaceColor,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                    Text(
                        text = currentMedia?.mediaMetadata?.artist?.toString() ?: strings.unknownArtist,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), // Reducido de 12.sp
                        fontWeight = FontWeight.Bold,
                        color = onSurfaceColor.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(0.dp) // Espaciado más compacto
                ) {
                    if (player.currentMediaItem?.mediaMetadata?.mediaType == MediaMetadata.MEDIA_TYPE_VIDEO) {
                        IconButton(onClick = {
                            if (forceCloseVideo) {
                                player.stop() // "Cerrar de verdad"
                            } else {
                                onCollapse() // "Minimizar"
                            }
                        }) {
                            Icon(
                                if (forceCloseVideo) Icons.Default.Close else Icons.Default.VideoLibrary,
                                null,
                                modifier = Modifier.size(24.dp),
                                tint = if (forceCloseVideo) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(onClick = { player.seekToPrevious() }) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            null,
                            modifier = Modifier.size(24.dp), // Reducido de 28.dp
                            tint = secondaryColor
                        )
                    }

                    MorphingPlayPauseButton(
                        isPlaying = isPlaying,
                        onClick = { if (isPlaying) player.playWhenReady = !player.playWhenReady else player.play() },
                        modifier = Modifier.size(46.dp),
                        mainColor = MaterialTheme.colorScheme.primary,
                        iconColor = MaterialTheme.colorScheme.onPrimary,
                        iconSize = 24.dp,
                        borderWidth = 0.dp
                    )

                    IconButton(onClick = { player.seekToNext() }) {
                        Icon(
                            Icons.Default.SkipNext,
                            null,
                            modifier = Modifier.size(24.dp), // Reducido de 28.dp
                            tint = secondaryColor
                        )
                    }
                }
            }

            var sliderValue by remember { mutableFloatStateOf(0f) }
            var isDragging by remember { mutableStateOf(false) }

            LaunchedEffect(currentPosition) {
                if (!isDragging) {
                    sliderValue = currentPosition.toFloat()
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 2.dp, bottom = 4.dp) // Padding más ajustado
            ) {
                Slider(
                    value = sliderValue.coerceIn(0f, totalDuration.coerceAtLeast(0L).toFloat()),
                    onValueChange = {
                        isDragging = true
                        sliderValue = it
                    },
                    onValueChangeFinished = {
                        player.seekTo(sliderValue.toLong())
                        isDragging = false
                    },
                    valueRange = 0f..totalDuration.coerceAtLeast(0L).toFloat(),
                    modifier = Modifier.fillMaxWidth().height(16.dp), // Reducido de 24.dp
                    colors = SliderDefaults.colors(
                        thumbColor = primaryColor,
                        activeTrackColor = primaryColor,
                        inactiveTrackColor = primaryColor.copy(alpha = 0.15f)
                    )
                )
            }
        }
    }

}
