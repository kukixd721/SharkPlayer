package com.example.mp3.ui.components

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.session.MediaController
import androidx.media3.ui.PlayerView
import com.example.mp3.*
import com.example.mp3.ui.screens.PlayerSettings
import com.example.mp3.ui.screens.PlayerComponents
import com.example.mp3.ui.components.formatTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@androidx.media3.common.util.UnstableApi
@Composable
fun VideoPlayerView(
    player: MediaController,
    effectiveVideo: Video?,
    currentSong: Song?,
    showVideoPlayer: Boolean,
    isVideoFullScreen: Boolean,
    onToggleFullScreen: (Boolean) -> Unit,
    onCloseVideo: () -> Unit,
    onRestoreVideo: () -> Unit = {},
    activity: Activity?,
    videoResizeMode: Int,
    onToggleResize: () -> Unit,
    showVideoControls: Boolean,
    onToggleVideoControls: (Boolean) -> Unit,
    showBrightnessOverlay: Boolean,
    brightnessLevel: Int,
    showVolumeOverlay: Boolean,
    volumeLevel: Int,
    showDetailedInfo: Boolean,
    onToggleDetailedInfo: (Boolean) -> Unit,
    songDetails: SongDetails?,
    playbackSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    settings: PlayerSettings,
    components: PlayerComponents,
    strings: AppStrings
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val window = activity?.window

    LaunchedEffect(isVideoFullScreen) {
        if (window != null) {
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            if (isVideoFullScreen) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                // Volver a modo normal solo si estaba en full screen, evitando bugs al abrir/cerrar
                if (activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (window != null) {
                val controller = WindowInsetsControllerCompat(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
            // Solo restaurar si no estamos cerrando la app o algo similar que necesite SPECIFIED
            if (activity?.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }

    BackHandler {
        if (isVideoFullScreen) {
            onToggleFullScreen(false)
        } else if (showVideoPlayer) {
            onCloseVideo()
        }
    }

    // Cargar letras para este video de forma asíncrona para no bloquear la UI
    var videoLyrics by remember { mutableStateOf<List<LyricLine>>(emptyList()) }
    LaunchedEffect(effectiveVideo?.data) {
        if (effectiveVideo == null) {
            videoLyrics = emptyList()
            return@LaunchedEffect
        }
        videoLyrics = withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences("lyrics_cache", Context.MODE_PRIVATE)
            val rawLyrics = prefs.getString(effectiveVideo.data, null)
            rawLyrics?.lines()?.mapNotNull { line ->
                val trimmedLine = line.trim()
                if (trimmedLine.isEmpty()) return@mapNotNull null
                val match = Regex("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{2,3}))?](.*)").find(trimmedLine)
                if (match != null) {
                    val (min, sec, ms, text) = match.destructured
                    val msValue = if (ms.isEmpty()) 0L else ms.padEnd(3, '0').take(3).toLong()
                    val timeMs = min.toLong() * 60000 + sec.toLong() * 1000 + msValue
                    LyricLine(timeMs, text.trim())
                } else null
            }?.sortedBy { it.timeMs } ?: emptyList()
        }
    }

    var thumbnail by remember(effectiveVideo?.data) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(effectiveVideo?.data) {
        if (effectiveVideo != null) {
            thumbnail = withContext(Dispatchers.IO) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        context.contentResolver.loadThumbnail(
                            android.net.Uri.fromFile(File(effectiveVideo.data)),
                            Size(480, 480),
                            null
                        )
                    } else {
                        ThumbnailUtils.createVideoThumbnail(
                            effectiveVideo.data,
                            MediaStore.Video.Thumbnails.MINI_KIND
                        )
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    var showVideoMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = if (isLandscape) Color.Black else MaterialTheme.colorScheme.surface
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Fondo dinámico o gradiente suave
            if (!isLandscape) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (isLandscape) 0.dp else 24.dp)
            ) {
                if (!isLandscape) {
                    Spacer(modifier = Modifier.height(16.dp))
                    // Cabecera tipo "Isla Dinámica" (Igual que LyricsView)
                    Surface(
                        onClick = { showVideoMenu = true },
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(32.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .statusBarsPadding()
                            .align(Alignment.Start)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(6.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                modifier = Modifier.size(52.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                if (thumbnail != null) {
                                    Image(
                                        bitmap = thumbnail!!.asImageBitmap(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.VideoLibrary,
                                        null,
                                        modifier = Modifier.padding(12.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column(modifier = Modifier.padding(end = 12.dp)) {
                                Text(
                                    text = effectiveVideo?.title ?: player.currentMediaItem?.mediaMetadata?.title?.toString() ?: "---",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = effectiveVideo?.artist ?: player.currentMediaItem?.mediaMetadata?.artist?.toString() ?: "---",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            Icon(
                                Icons.Default.MoreHoriz,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Área de Video / Letras
                Box(modifier = Modifier.weight(1f)) {
                    if (showDetailedInfo && songDetails != null) {
                        MetadataDetailsDialog(
                            songDetails = songDetails,
                            currentSong = currentSong,
                            strings = strings,
                            onDismiss = { onToggleDetailedInfo(false) }
                        )
                    }
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Sección Video
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (isLandscape) Modifier.fillMaxHeight() else Modifier.aspectRatio(16f / 9f))
                                .clip(RoundedCornerShape(if (isLandscape) 0.dp else 32.dp))
                                .background(Color.Black)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (showVideoPlayer) {
                                    AndroidView(
                                        factory = { ctx ->
                                            PlayerView(ctx).apply {
                                                this.player = player
                                                useController = false
                                                this.resizeMode = videoResizeMode
                                                setBackgroundColor(android.graphics.Color.BLACK)
                                            }
                                        },
                                        update = { view ->
                                            view.player = player
                                            view.resizeMode = videoResizeMode
                                        },
                                        onRelease = { view ->
                                            view.player = null
                                        },
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable { onToggleVideoControls(!showVideoControls) }
                                    )
                                }

                                // Controles Overlay para Landscape
                                if (isLandscape && showVideoControls) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.4f))
                                    ) {
                                        // Botón para salir de pantalla completa
                                        IconButton(
                                            onClick = { 
                                                onToggleFullScreen(false)
                                            },
                                            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                                        ) {
                                            Icon(Icons.Default.FullscreenExit, null, tint = Color.White, modifier = Modifier.size(32.dp))
                                        }

                                        // Botón para cerrar reproductor
                                        IconButton(
                                            onClick = {
                                                onToggleFullScreen(false)
                                                onCloseVideo()
                                            },
                                            modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                                        }

                                        // Play/Pause central
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .size(80.dp)
                                        ) {
                                            components.MorphingPlayPauseButton(
                                                player.isPlaying,
                                                { if (player.isPlaying) player.pause() else player.play() },
                                                Modifier.fillMaxSize(),
                                                Color.White.copy(alpha = 0.3f),
                                                Color.White,
                                                40.dp,
                                                1.dp
                                            )
                                        }
                                        
                                        // Slider inferior en landscape
                                        var sliderValue by remember { mutableFloatStateOf(0f) }
                                        var isDragging by remember { mutableStateOf(false) }
                                        val currentPos = settings.rememberPlayerPosition(player)
                                        LaunchedEffect(currentPos) { if (!isDragging) sliderValue = currentPos.toFloat() }

                                        Column(
                                            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 32.dp, vertical = 16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(onClick = { showVideoMenu = true }) {
                                                    Icon(Icons.Default.Settings, null, tint = Color.White)
                                                }
                                                
                                                Text(
                                                    text = "${formatTime(sliderValue.toLong())} / ${formatTime(player.duration)}",
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.labelMedium
                                                )

                                                IconButton(onClick = onToggleResize) {
                                                    Icon(Icons.Default.AspectRatio, null, tint = Color.White)
                                                }
                                            }

                                            Slider(
                                                value = sliderValue.coerceIn(0f, player.duration.toFloat().coerceAtLeast(1f)),
                                                onValueChange = { isDragging = true; sliderValue = it },
                                                onValueChangeFinished = { player.seekTo(sliderValue.toLong()); isDragging = false },
                                                valueRange = 0f..player.duration.toFloat().coerceAtLeast(1f),
                                                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White)
                                            )
                                        }
                                    }
                                }
                                
                                // Overlays de Brillo y Volumen
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AnimatedVisibility(visible = showBrightnessOverlay, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                                            VideoSettingOverlay(Icons.Default.BrightnessMedium, brightnessLevel, settings)
                                        }
                                        AnimatedVisibility(visible = showVolumeOverlay, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                                            VideoSettingOverlay(if (volumeLevel == 0) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp, volumeLevel, settings)
                                        }
                                    }
                                }
                            }
                        }

                        if (!isLandscape) {
                            Spacer(modifier = Modifier.height(16.dp))
                            // Sección Letras (Debajo del video en modo retrato)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                shape = RoundedCornerShape(32.dp),
                                border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                if (videoLyrics.isEmpty()) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.MusicNote,
                                            null,
                                            modifier = Modifier.size(64.dp),
                                            tint = settings.customAccentColor
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            strings.noLyricsFound,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                                        )
                                    }
                                } else {
                                    val currentPos = settings.rememberPlayerPosition(player)
                                    components.KaraokeView(
                                        videoLyrics,
                                        currentPos,
                                        settings.lyricsFontSize,
                                        settings.centerLyrics,
                                        false,
                                        emptySet(),
                                        true,
                                        {},
                                        { time -> player.seekTo(time) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Controles de Video (Igual que LyricsView)
                if (!isLandscape) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp, top = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Fila superior: Play + Slider
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            components.MorphingPlayPauseButton(
                                player.isPlaying,
                                { if (player.isPlaying) player.pause() else player.play() },
                                Modifier.size(72.dp),
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.onPrimaryContainer,
                                32.dp,
                                2.dp
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            var sliderValue by remember { mutableFloatStateOf(0f) }
                            var isDragging by remember { mutableStateOf(false) }
                            val currentPos = settings.rememberPlayerPosition(player)
                            LaunchedEffect(currentPos) {
                                if (!isDragging) sliderValue = currentPos.toFloat()
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(72.dp),
                                shape = RoundedCornerShape(32.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Slider(
                                        value = sliderValue.coerceIn(0f, player.duration.toFloat().coerceAtLeast(1f)),
                                        onValueChange = { isDragging = true; sliderValue = it },
                                        onValueChangeFinished = {
                                            player.seekTo(sliderValue.toLong())
                                            isDragging = false
                                        },
                                        valueRange = 0f..player.duration.toFloat().coerceAtLeast(1f),
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary,
                                            inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Fila inferior: Atrás + Resize/Speed + More
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                onClick = {
                                    if (isVideoFullScreen) {
                                        onToggleFullScreen(false)
                                    } else {
                                        onCloseVideo()
                                    }
                                },
                                modifier = Modifier.size(56.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            // Botón de Redimensionado/Pantalla Completa
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp)
                                    .height(56.dp),
                                shape = RoundedCornerShape(32.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                RoundedCornerShape(28.dp)
                                            )
                                            .clickable { onToggleResize() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.AspectRatio, null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clickable {
                                                onToggleFullScreen(true)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Fullscreen, null, tint = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }

                            Surface(
                                onClick = { showVideoMenu = true },
                                modifier = Modifier.size(56.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.MoreVert, null, tint = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showVideoMenu) {
        ModalBottomSheet(
            onDismissRequest = { showVideoMenu = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outline) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp, top = 16.dp)
            ) {
                ListItem(
                    headlineContent = { Text(effectiveVideo?.title ?: "---", fontWeight = FontWeight.Black) },
                    supportingContent = { Text(effectiveVideo?.artist ?: "---", fontWeight = FontWeight.Bold) },
                    leadingContent = {
                        Surface(
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            if (thumbnail != null) {
                                Image(bitmap = thumbnail!!.asImageBitmap(), null, contentScale = ContentScale.Crop)
                            } else {
                                Icon(Icons.Default.VideoLibrary, null, modifier = Modifier.padding(12.dp))
                            }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

                ListItem(
                    modifier = Modifier.clickable { onToggleDetailedInfo(true); showVideoMenu = false },
                    headlineContent = { Text("INFO", fontWeight = FontWeight.Black) },
                    leadingContent = { Icon(Icons.Default.Info, null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent, leadingIconColor = MaterialTheme.colorScheme.primary)
                )

                var showSpeedDialog by remember { mutableStateOf(false) }
                ListItem(
                    modifier = Modifier.clickable { showSpeedDialog = true },
                    headlineContent = { Text("Velocidad de reproducción", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("${playbackSpeed}x") },
                    leadingContent = { Icon(Icons.Default.SlowMotionVideo, null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent, leadingIconColor = MaterialTheme.colorScheme.primary)
                )

                if (showSpeedDialog) {
                    AlertDialog(
                        onDismissRequest = { showSpeedDialog = false },
                        title = { Text("Velocidad") },
                        text = {
                            Column {
                                listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onSpeedChange(speed); showSpeedDialog = false; showVideoMenu = false }
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(selected = playbackSpeed == speed, onClick = null)
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text("${speed}x", fontWeight = if(playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                        },
                        confirmButton = { TextButton(onClick = { showSpeedDialog = false }) { Text("Cerrar") } }
                    )
                }
            }
        }
    }
}
