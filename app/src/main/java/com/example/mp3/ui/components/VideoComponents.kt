package com.example.mp3.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.mp3.LocalStrings
import com.example.mp3.Video
import com.example.mp3.ui.screens.PlayerSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun VideoSettingOverlay(icon: ImageVector, value: Int, settings: PlayerSettings) {
    Surface(
        color = Color.Black.copy(alpha = 0.6f),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$value%",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoControlsOverlay(
    player: Player?,
    video: Video,
    onClose: () -> Unit,
    onToggleResize: () -> Unit,
    onToggleFullScreen: () -> Unit,
    playbackSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    onShowMetadata: (() -> Unit)? = null,
    settings: PlayerSettings
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    var isPlaying by remember { mutableStateOf(player?.isPlaying ?: false) }
    var currentPosition by remember { mutableLongStateOf(player?.currentPosition ?: 0L) }
    var duration by remember { mutableLongStateOf(player?.duration?.coerceAtLeast(0L) ?: 0L) }
    var isDragging by remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val playPauseScale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = tween(100), label = "PlayPauseScale"
    )

    LaunchedEffect(player) {
        player?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                duration = player.duration.coerceAtLeast(0L)
            }
        })
    }

    LaunchedEffect(isPlaying, isDragging) {
        if (isPlaying && !isDragging) {
            while (true) {
                currentPosition = player?.currentPosition ?: 0L
                delay(1000)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.7f),
                        Color.Transparent,
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.7f)
                    )
                )
            )
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(settings.roundnessSmall.dp))
            ) {
                Icon(Icons.Default.Close, null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = video.artist,
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }

            IconButton(onClick = onToggleResize) {
                Icon(Icons.Default.AspectRatio, null, tint = Color.White)
            }

            if (onShowMetadata != null) {
                IconButton(onClick = onShowMetadata) {
                    Icon(Icons.Default.Info, null, tint = Color.White)
                }
            }

            var showSpeedMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { showSpeedMenu = true }) {
                    Icon(Icons.Default.SlowMotionVideo, null, tint = Color.White)
                }
                DropdownMenu(
                    expanded = showSpeedMenu,
                    onDismissRequest = { showSpeedMenu = false },
                    modifier = Modifier.background(
                        MaterialTheme.colorScheme.surfaceContainerHigh, 
                        RoundedCornerShape(settings.roundnessMedium.dp)
                    )
                ) {
                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                        DropdownMenuItem(
                            text = { Text("${speed}x", fontWeight = if(playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                onSpeedChange(speed)
                                showSpeedMenu = false
                            },
                            leadingIcon = {
                                if (playbackSpeed == speed) Icon(Icons.Default.Check, null)
                            }
                        )
                    }
                }
            }
        }

        // Center Controls
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            IconButton(
                onClick = { player?.seekBack() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Replay10, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }

            Surface(
                onClick = { if (isPlaying) player?.pause() else player?.play() },
                modifier = Modifier
                    .size(72.dp)
                    .graphicsLayer {
                        scaleX = playPauseScale
                        scaleY = playPauseScale
                    },
                shape = RoundedCornerShape(if (isPlaying) 36.dp else 22.dp),
                color = Color.White,
                interactionSource = interactionSource
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null,
                        tint = Color.Black,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            IconButton(
                onClick = { player?.seekForward() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Forward10, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }

        // Bottom Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(settings.roundnessSmall.dp))
                ) {
                    Text(
                        text = formatVideoTime(currentPosition),
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Text(
                        text = formatVideoTime(duration),
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                IconButton(onClick = onToggleFullScreen) {
                    Icon(Icons.Default.Fullscreen, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val sliderValue = currentPosition.toFloat()
            val totalDurationFloat = duration.toFloat().coerceAtLeast(1f)

            // Usamos el efecto de onda si está reproduciendo
            val wavePhase by rememberInfiniteTransition(label = "wave").animateFloat(
                initialValue = 0f,
                targetValue = 2f * Math.PI.toFloat(),
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "phase"
            )

            Slider(
                value = sliderValue.coerceIn(0f, totalDurationFloat),
                onValueChange = {
                    isDragging = true
                    currentPosition = it.toLong()
                },
                onValueChangeFinished = {
                    player?.seekTo(currentPosition)
                    isDragging = false
                },
                valueRange = 0f..totalDurationFloat,
                modifier = Modifier.fillMaxWidth(),
                thumb = {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(24.dp)
                            .background(Color.White, RoundedCornerShape(2.dp))
                    )
                },
                track = { sliderState ->
                    val fraction = sliderState.value / sliderState.valueRange.endInclusive
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                    ) {
                        val width = size.width
                        val centerY = size.height / 2
                        val activeWidth = width * fraction

                        // Track Inactivo
                        drawLine(
                            color = Color.White.copy(alpha = 0.24f),
                            start = androidx.compose.ui.geometry.Offset(activeWidth, centerY),
                            end = androidx.compose.ui.geometry.Offset(width, centerY),
                            strokeWidth = 4.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )

                        // Track Activo (Wavy)
                        val path = androidx.compose.ui.graphics.Path()
                        path.moveTo(0f, centerY)

                        if (isPlaying && !isDragging) {
                            val waveLength = 100f
                            val amplitude = 6f
                            for (x in 0..activeWidth.toInt()) {
                                val relativeX = x.toFloat()
                                val y = centerY + amplitude * kotlin.math.sin(
                                    (relativeX / waveLength * 2 * Math.PI.toFloat() + wavePhase).toDouble()
                                ).toFloat()
                                path.lineTo(relativeX, y)
                            }
                        } else {
                            path.lineTo(activeWidth, centerY)
                        }

                        drawPath(
                            path = path,
                            color = Color.White,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 6.dp.toPx(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        )
                    }
                }
            )
        }
    }
}


private fun formatVideoTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

@Composable
fun VideoList(
    videos: List<Video>,
    onVideoClick: (Video) -> Unit,
    settings: PlayerSettings
) {
    val strings = LocalStrings.current
    
    if (videos.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No se encontraron videos",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 120.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(videos) { video ->
                VideoCard(video = video, onClick = { onVideoClick(video) }, settings = settings)
            }
        }
    }
}

@Composable
fun VideoCard(
    video: Video,
    onClick: () -> Unit,
    settings: PlayerSettings
) {
    val context = LocalContext.current
    var thumbnail by remember(video.data, video.thumbnailUri) { mutableStateOf<Any?>(video.thumbnailUri) }

    LaunchedEffect(video.data, video.thumbnailUri) {
        if (video.thumbnailUri == null) {
            thumbnail = withContext(Dispatchers.IO) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        context.contentResolver.loadThumbnail(
                            android.net.Uri.fromFile(java.io.File(video.data)),
                            Size(480, 480),
                            null
                        )
                    } else {
                        ThumbnailUtils.createVideoThumbnail(
                            video.data,
                            MediaStore.Video.Thumbnails.MINI_KIND
                        )
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(settings.roundnessMedium.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (thumbnail != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(thumbnail)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Overlay de duración
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                Text(
                    text = formatDuration(video.duration),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            // Icono de Play central
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(settings.roundnessSmall.dp))
                    .padding(8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = video.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
        if (video.resolution != null) {
            Text(
                text = video.resolution,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
