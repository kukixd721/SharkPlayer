package com.example.mp3.ui.screens

import android.content.Context
import android.content.pm.ActivityInfo
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.edit
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.ui.AspectRatioFrameLayout
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.mp3.*
import com.example.mp3.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@androidx.media3.common.util.UnstableApi
@Composable
fun FullPlayerSheet(
    player: MediaController,
    currentSong: Song?,
    currentVideo: Video?,
    settings: PlayerSettings,
    components: PlayerComponents,
    strings: AppStrings,
    onDismiss: () -> Unit,
    sheetState: SheetState,
    // Hoisted states/callbacks from PlayerScreen
    isLyricsFullScreen: Boolean,
    onLyricsFullScreenChange: (Boolean) -> Unit,
    showQueueDialog: Boolean,
    onShowQueueDialogChange: (Boolean) -> Unit,
    showEqualizerDialog: Boolean,
    onShowEqualizerDialogChange: (Boolean) -> Unit,
    onlineLyricsResults: List<LyricResult>,
    onOnlineLyricsResultsChange: (List<LyricResult>) -> Unit,
    showLyricsSelectionDialog: Boolean,
    onShowLyricsSelectionDialogChange: (Boolean) -> Unit,
    isSearchingLyrics: Boolean,
    onIsSearchingLyricsChange: (Boolean) -> Unit,
    // Physics/Animation states from PlayerScreen
    isPlaying: Boolean,
    currentPosition: Long,
    totalDuration: Long,
    isShuffleOn: Boolean,
    repeatMode: Int,
    albumScale: Float,
    albumCornerRadius: androidx.compose.ui.unit.Dp,
    offsetX: Animatable<Float, AnimationVector1D>,
    videos: List<Video> = emptyList(),
    // New parameters to resolve PlayerScreen issues
    showDetailedInfo: Boolean,
    onToggleDetailedInfo: (Boolean) -> Unit,
    playbackSpeed: Float,
    onSpeedChange: (Float) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = {
            if (isLyricsFullScreen) {
                onLyricsFullScreenChange(false)
            } else {
                onDismiss()
            }
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        scrimColor = Color.Black.copy(alpha = 0.32f),
        tonalElevation = 0.dp,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(
            topStart = settings.roundnessLarge.dp,
            topEnd = settings.roundnessLarge.dp
        )
    ) {
        val playerColorScheme = MaterialTheme.colorScheme

        MaterialTheme(
            colorScheme = playerColorScheme,
            typography = MaterialTheme.typography
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                // Prioridad máxima al botón de atrás cuando las letras están abiertas
                BackHandler(enabled = isLyricsFullScreen) {
                    onLyricsFullScreenChange(false)
                }

                val currentMedia = player.currentMediaItem
                val albumArt by produceState<ByteArray?>(null, currentSong) {
                    value = withContext(Dispatchers.IO) {
                        currentSong?.let { settings.getAlbumArt(it.data) }
                    }
                }

                val effectiveMedia = currentSong ?: currentVideo
                val songDetails by produceState<SongDetails>(
                    SongDetails(),
                    effectiveMedia
                ) {
                    value = withContext(Dispatchers.IO) {
                        effectiveMedia?.let { media ->
                            val data = when (media) {
                                is Song -> media.data
                                is Video -> media.data
                                else -> ""
                            }
                            if (data.isBlank()) return@withContext SongDetails()

                            var details = getAudioMetadata(context, data)
                            if (media is Song && details.lyrics.isNullOrBlank() && settings.autoLyrics) {
                                try {
                                    val cleanTitle =
                                        media.title.replace(
                                            Regex("(?i)\\(.*?\\)|\\[.*?\\]"),
                                            ""
                                        )
                                            .replace(
                                                Regex("(?i)official video|video oficial|lyric video|full hd|4k|audio|high quality|hq|visualizer|remastered"),
                                                ""
                                            )
                                            .trim()
                                    val artist =
                                        if (media.artist != "<unknown>") media.artist else ""

                                    val queryUrl = if (artist.isNotBlank()) {
                                        "https://lrclib.net/api/search?artist_name=${
                                            java.net.URLEncoder.encode(
                                                artist,
                                                "UTF-8"
                                            )
                                        }&track_name=${
                                            java.net.URLEncoder.encode(
                                                cleanTitle,
                                                "UTF-8"
                                            )
                                        }"
                                    } else {
                                        "https://lrclib.net/api/search?q=${
                                            java.net.URLEncoder.encode(
                                                cleanTitle,
                                                "UTF-8"
                                            )
                                        }"
                                    }

                                    val url = java.net.URL(queryUrl)
                                    val connection =
                                        url.openConnection() as java.net.HttpURLConnection
                                    connection.requestMethod = "GET"
                                    connection.connectTimeout = 4000
                                    connection.readTimeout = 4000

                                    if (connection.responseCode == 200) {
                                        val response =
                                            connection.inputStream.bufferedReader()
                                                .readText()
                                        val jsonArray =
                                            org.json.JSONArray(response)
                                        if (jsonArray.length() > 0) {
                                            val item =
                                                jsonArray.getJSONObject(0)
                                            val syncedLyrics =
                                                item.optString("syncedLyrics")
                                            val plainLyrics =
                                                item.optString("plainLyrics")
                                            val foundLyrics =
                                                if (!syncedLyrics.isNullOrBlank() && syncedLyrics != "null") syncedLyrics else plainLyrics

                                            if (!foundLyrics.isNullOrBlank() && foundLyrics != "null") {
                                                details = details.copy(
                                                    lyrics = foundLyrics,
                                                    lyricsSource = "online"
                                                )
                                                // Guardar en caché
                                                val lPrefs =
                                                    context.getSharedPreferences(
                                                        "lyrics_cache",
                                                        Context.MODE_PRIVATE
                                                    )
                                                lPrefs.edit {
                                                    putString(
                                                        data,
                                                        foundLyrics
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            details
                        } ?: SongDetails()
                    }
                }

                var lyrics by remember(currentSong, songDetails.lyrics) {
                    mutableStateOf(currentSong?.lyrics ?: songDetails.lyrics)
                }

                // Efecto para intentar embeber letras automáticamente si se encontraron online
                LaunchedEffect(lyrics) {
                    if (!lyrics.isNullOrBlank() && currentSong != null && songDetails.lyricsSource == "online") {
                        withContext(Dispatchers.IO) {
                            embedLyricsToFile(currentSong.data, lyrics!!)
                        }
                    }
                }
                var isEditingLyrics by remember { mutableStateOf(false) }
                var selectedLyricLines by remember { mutableStateOf(setOf<Int>()) }
                var isSelectionMode by remember { mutableStateOf(false) }
                var editedLyricsText by remember { mutableStateOf("") }
                // var showDetailedInfo by remember { mutableStateOf(false) } // Removed as it is now passed as parameter

                // --- DIÁLOGOS DE METADATOS Y LETRAS ---
                if (showDetailedInfo) {
                    MetadataDetailsDialog(
                        songDetails = songDetails,
                        currentSong = currentSong,
                        strings = strings,
                        onDismiss = { onToggleDetailedInfo(false) }
                    )
                }

                // Internal playback speed state for VideoPlayerView compatibility
                // var playbackSpeed by remember { mutableFloatStateOf(player.playbackParameters.speed) } // Removed as it is now passed as parameter
                var videoResizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
                var isVideoFullScreen by remember { mutableStateOf(false) }
                var forceCloseVideo by remember { mutableStateOf(false) }
                var showVideoControls by remember { mutableStateOf(true) }
                var showBrightnessOverlay by remember { mutableStateOf(false) }
                var brightnessLevel by remember { mutableIntStateOf(0) }
                var showVolumeOverlay by remember { mutableStateOf(false) }
                var volumeLevel by remember { mutableIntStateOf(0) }

                val isVideoByMetadata =
                    currentMedia?.mediaMetadata?.mediaType == androidx.media3.common.MediaMetadata.MEDIA_TYPE_VIDEO
                val isVideoPlaying = currentVideo != null
                val showVideoPlayer =
                    (isVideoFullScreen || isVideoPlaying || isVideoByMetadata) && (isVideoPlaying || isVideoByMetadata) && !forceCloseVideo

                LaunchedEffect(currentMedia?.mediaId) {
                    forceCloseVideo = false
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (settings.unifiedLyricsBackground) {
                        AnimatedMeshGradient(
                            modifier = Modifier.fillMaxSize(),
                            primaryColor = MaterialTheme.colorScheme.primary,
                            secondaryColor = MaterialTheme.colorScheme.secondary,
                            tertiaryColor = MaterialTheme.colorScheme.tertiary,
                            blurRadius = 80.dp
                        )
                    }

                    AnimatedContent(
                        targetState = isLyricsFullScreen,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(400)) togetherWith fadeOut(
                                animationSpec = tween(400)
                            )
                        },
                        label = "LyricsTransition",
                        modifier = Modifier.fillMaxSize()
                    ) { fullScreen ->
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = if (settings.unifiedLyricsBackground) Color.Transparent else MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            if (fullScreen) {
                                LyricsView(
                                    player = player,
                                    currentSong = currentSong,
                                    lyrics = lyrics,
                                    onLyricsChange = { lyrics = it },
                                    onClose = { onLyricsFullScreenChange(false) },
                                    isSelectionMode = isSelectionMode,
                                    onSelectionModeChange = { isSelectionMode = it },
                                    selectedLyricLines = selectedLyricLines,
                                    onSelectedLinesChange = { selectedLyricLines = it },
                                    showDetailedInfo = showDetailedInfo,
                                    onToggleDetailedInfo = {
                                        onToggleDetailedInfo(!showDetailedInfo)
                                    },
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
                                    onShowQueue = {
                                        onShowQueueDialogChange(true)
                                    }
                                )
                            } else {
                                // --- VISTA NORMAL DEL REPRODUCTOR ---
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 24.dp)
                                        .verticalScroll(rememberScrollState()),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Aquí se maneja si mostrar el Video o el Album Art
                                    if (showVideoPlayer) {
                                        if (!isVideoFullScreen) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(16f / 9f)
                                                    .clip(RoundedCornerShape(settings.roundnessMedium.dp))
                                                    .background(Color.Black)
                                                    .clickable {
                                                        showVideoControls = true
                                                    }
                                            ) {
                                                if (showVideoControls) {
                                                    VideoControlsOverlay(
                                                        player = player,
                                                        video = currentVideo ?: Video(
                                                            0,
                                                            currentMedia?.mediaMetadata?.title?.toString()
                                                                ?: "",
                                                            "",
                                                            "",
                                                            0,
                                                            ""
                                                        ),
                                                        onClose = {
                                                            showVideoControls = false
                                                        },
                                                        onToggleResize = {
                                                            videoResizeMode =
                                                                if (videoResizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT)
                                                                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT
                                                        },
                                                        onToggleFullScreen = {
                                                            isVideoFullScreen = true
                                                            showVideoControls = true
                                                        },
                                                        playbackSpeed = playbackSpeed,
                                                        onSpeedChange = {
                                                            player.setPlaybackSpeed(it)
                                                            onSpeedChange(it)
                                                        },
                                                        settings = settings
                                                    )
                                                }
                                            }
                                        } else {
                                            Spacer(
                                                modifier = Modifier
                                                    .aspectRatio(16f / 9f)
                                                    .fillMaxWidth()
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .aspectRatio(1f)
                                                .fillMaxWidth(0.85f)
                                                .offset {
                                                    IntOffset(
                                                        offsetX.value.roundToInt(),
                                                        0
                                                    )
                                                }
                                                .graphicsLayer {
                                                    scaleX = albumScale
                                                    scaleY = albumScale
                                                    rotationZ = offsetX.value / 25f
                                                }
                                                .pointerInput(Unit) {
                                                    detectTapGestures(
                                                        onDoubleTap = {
                                                            if (isPlaying) player.pause() else player.play()
                                                        },
                                                        onLongPress = {
                                                            val currentSong_ = currentSong
                                                            if (currentSong_ != null) {
                                                                val currentColors =
                                                                    context.getSharedPreferences(
                                                                        "app_settings",
                                                                        Context.MODE_PRIVATE
                                                                    )
                                                                val p = currentColors.getInt(
                                                                    "art_primary_color",
                                                                    Color.Blue.toArgb()
                                                                )
                                                                val s = currentColors.getInt(
                                                                    "art_secondary_color",
                                                                    Color.Gray.toArgb()
                                                                )

                                                                scope.launch {
                                                                    Toast.makeText(
                                                                        context,
                                                                        strings.generatingImage,
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                    val art =
                                                                        withContext(
                                                                            Dispatchers.IO
                                                                        ) {
                                                                            settings.getAlbumArt(
                                                                                currentSong_.data
                                                                            )
                                                                        }
                                                                    val snippet =
                                                                        if (!lyrics.isNullOrBlank()) {
                                                                            val currentPos =
                                                                                currentPosition
                                                                            if (lyrics!!.contains(
                                                                                    Regex(
                                                                                        "\\[\\d{2}:\\d{2}"
                                                                                    )
                                                                                )
                                                                            ) {
                                                                                val parsed =
                                                                                    parseSincronizedLyrics(
                                                                                        lyrics!!
                                                                                    )
                                                                                val activeLine =
                                                                                    parsed.indexOfLast { it.timeMs <= currentPos }
                                                                                        .coerceAtLeast(
                                                                                            0
                                                                                        )
                                                                                parsed.subList(
                                                                                    activeLine,
                                                                                    (activeLine + 3).coerceAtMost(
                                                                                        parsed.size
                                                                                    )
                                                                                )
                                                                                    .joinToString(
                                                                                        "\n"
                                                                                    ) { it.text }
                                                                            } else {
                                                                                lyrics!!.split(
                                                                                    "\n"
                                                                                )
                                                                                    .take(
                                                                                        3
                                                                                    )
                                                                                    .joinToString(
                                                                                        "\n"
                                                                                    )
                                                                            }
                                                                        } else null
                                                                    StoryGenerator.generateAndShare(
                                                                        context,
                                                                        currentSong_,
                                                                        art,
                                                                        p,
                                                                        s,
                                                                        snippet,
                                                                        currentPosition
                                                                    )
                                                                }
                                                            }
                                                        },
                                                        onTap = {
                                                            onLyricsFullScreenChange(!isLyricsFullScreen)
                                                        }
                                                    )
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth(0.92f)
                                                    .aspectRatio(1f)
                                                    .graphicsLayer {
                                                        scaleX = albumScale
                                                        scaleY = albumScale
                                                    }
                                                    .pointerInput(Unit) {
                                                        detectHorizontalDragGestures(
                                                            onHorizontalDrag = { _, dragAmount ->
                                                                scope.launch {
                                                                    offsetX.snapTo(
                                                                        offsetX.value + dragAmount
                                                                    )
                                                                }
                                                            },
                                                            onDragEnd = {
                                                                scope.launch {
                                                                    if (offsetX.value > 150f) {
                                                                        player.seekToPrevious()
                                                                    } else if (offsetX.value < -150f) {
                                                                        player.seekToNext()
                                                                    }
                                                                    offsetX.animateTo(
                                                                        0f,
                                                                        spring(stiffness = Spring.StiffnessLow)
                                                                    )
                                                                }
                                                            },
                                                            onDragCancel = {
                                                                scope.launch {
                                                                    offsetX.animateTo(
                                                                        0f,
                                                                        spring(stiffness = Spring.StiffnessLow)
                                                                    )
                                                                }
                                                            }
                                                        )
                                                    },
                                                shape = RoundedCornerShape(
                                                    32.dp
                                                ),
                                                shadowElevation = 0.dp,
                                                border = BorderStroke(
                                                    2.dp,
                                                    MaterialTheme.colorScheme.outline
                                                )
                                            ) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(LocalContext.current)
                                                        .data(
                                                            albumArt
                                                                ?: currentSong?.data?.let { File(it) })
                                                        .crossfade(true).build(),
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop,
                                                    error = rememberVectorPainter(Icons.Default.MusicNote)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth()
                                                .padding(horizontal = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.Start,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = currentMedia?.mediaMetadata?.title?.toString()
                                                        ?: strings.noSongs,
                                                    style = MaterialTheme.typography.headlineMedium.copy(
                                                        fontWeight = FontWeight.Black,
                                                        letterSpacing = (-1).sp,
                                                        fontSize = 28.sp,
                                                        lineHeight = 34.sp
                                                    ),
                                                    textAlign = TextAlign.Start,
                                                    modifier = Modifier.basicMarquee(),
                                                    maxLines = 1
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = currentMedia?.mediaMetadata?.artist?.toString()
                                                        ?: strings.unknownArtist,
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 18.sp,
                                                        letterSpacing = 0.sp
                                                    ),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    textAlign = TextAlign.Start,
                                                    modifier = Modifier.alpha(0.9f)
                                                )
                                            }

                                            IconButton(
                                                onClick = { onToggleDetailedInfo(true) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Info,
                                                    contentDescription = "Metadata",
                                                    tint = MaterialTheme.colorScheme.primary.copy(
                                                        alpha = 0.6f
                                                    ),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        var sliderValue by remember { mutableFloatStateOf(0f) }
                                        var isDragging by remember { mutableStateOf(false) }

                                        LaunchedEffect(currentPosition) {
                                            if (!isDragging) {
                                                sliderValue = currentPosition.toFloat()
                                            }
                                        }

                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val audioSessionId = remember {
                                                context.getSharedPreferences(
                                                    "app_config",
                                                    Context.MODE_PRIVATE
                                                ).getInt("audio_session_id", 0)
                                            }

                                            if (settings.visualizerEnabled) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(180.dp)
                                                        .alpha(0.35f),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (audioSessionId > 0 && isPlaying) {
                                                        AudioVisualizer(
                                                            audioSessionId = audioSessionId,
                                                            modifier = Modifier.fillMaxSize(),
                                                            color = MaterialTheme.colorScheme.primary,
                                                            visualizerStyle = settings.visualizerStyle
                                                        )
                                                    }
                                                }
                                            }

                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                val wavePhase by rememberInfiniteTransition(label = "wave")
                                                    .animateFloat(
                                                        initialValue = 0f,
                                                        targetValue = 2f * Math.PI.toFloat(),
                                                        animationSpec = infiniteRepeatable(
                                                            animation = tween(
                                                                1500,
                                                                easing = LinearEasing
                                                            ),
                                                            repeatMode = RepeatMode.Restart
                                                        ),
                                                        label = "phase"
                                                    )

                                                Column(
                                                    modifier = Modifier.fillMaxWidth()
                                                        .padding(horizontal = 8.dp)
                                                ) {
                                                    val primaryColor =
                                                        MaterialTheme.colorScheme.primary
                                                    Slider(
                                                        value = sliderValue.coerceIn(
                                                            0f,
                                                            totalDuration.toFloat()
                                                                .coerceAtLeast(1f)
                                                        ),
                                                        onValueChange = {
                                                            isDragging = true
                                                            sliderValue = it
                                                        },
                                                        onValueChangeFinished = {
                                                            player.seekTo(sliderValue.toLong())
                                                            isDragging = false
                                                        },
                                                        valueRange = 0f..totalDuration.toFloat()
                                                            .coerceAtLeast(1f),
                                                        modifier = Modifier.fillMaxWidth(),
                                                        thumb = {
                                                            Box(
                                                                modifier = Modifier
                                                                    .width(4.dp)
                                                                    .height(28.dp)
                                                                    .background(
                                                                        primaryColor,
                                                                        RoundedCornerShape(2.dp)
                                                                    )
                                                            )
                                                        },
                                                        track = { sliderState ->
                                                            val fraction =
                                                                sliderState.value / sliderState.valueRange.endInclusive
                                                            Canvas(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .height(48.dp)
                                                            ) {
                                                                val width = size.width
                                                                val centerY = size.height / 2
                                                                val activeWidth = width * fraction

                                                                drawLine(
                                                                    color = primaryColor.copy(alpha = 0.2f),
                                                                    start = Offset(
                                                                        activeWidth,
                                                                        centerY
                                                                    ),
                                                                    end = Offset(width, centerY),
                                                                    strokeWidth = 2.dp.toPx(),
                                                                    cap = StrokeCap.Round
                                                                )

                                                                val path = Path()
                                                                path.moveTo(0f, centerY)

                                                                if (isPlaying && !isDragging) {
                                                                    val waveLength = 100f
                                                                    val amplitude = 6f
                                                                    for (x in 0..activeWidth.toInt()) {
                                                                        val relativeX = x.toFloat()
                                                                        val y =
                                                                            centerY + amplitude * sin(
                                                                                relativeX / waveLength * 2 * Math.PI.toFloat() + wavePhase
                                                                            )
                                                                        path.lineTo(relativeX, y)
                                                                    }
                                                                } else {
                                                                    path.lineTo(
                                                                        activeWidth,
                                                                        centerY
                                                                    )
                                                                }

                                                                drawPath(
                                                                    path = path,
                                                                    color = primaryColor,
                                                                    style = Stroke(
                                                                        width = 4.dp.toPx(),
                                                                        cap = StrokeCap.Round
                                                                    )
                                                                )
                                                            }
                                                        }
                                                    )
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth()
                                                            .padding(horizontal = 8.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            formatTime(if (isDragging) sliderValue.toLong() else currentPosition),
                                                            style = MaterialTheme.typography.labelMedium.copy(
                                                                fontWeight = FontWeight.Bold,
                                                                fontFeatureSettings = "tnum",
                                                                letterSpacing = 0.5.sp
                                                            ),
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        Text(
                                                            formatTime(totalDuration),
                                                            style = MaterialTheme.typography.labelMedium.copy(
                                                                fontWeight = FontWeight.Bold,
                                                                fontFeatureSettings = "tnum",
                                                                letterSpacing = 0.5.sp
                                                            ),
                                                            color = MaterialTheme.colorScheme.primary.copy(
                                                                alpha = 0.6f
                                                            )
                                                        )
                                                    }
                                                }

                                                Column(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 24.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        IconButton(onClick = {
                                                            player.shuffleModeEnabled = !isShuffleOn
                                                        }) {
                                                            Icon(
                                                                imageVector = if (isShuffleOn) Icons.Default.ShuffleOn else Icons.Default.Shuffle,
                                                                contentDescription = null,
                                                                tint = if (isShuffleOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(
                                                                    alpha = 0.6f
                                                                ),
                                                                modifier = Modifier.size(24.dp)
                                                            )
                                                        }

                                                        IconButton(onClick = {
                                                            player.repeatMode =
                                                                when (repeatMode) {
                                                                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                                                                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                                                                    else -> Player.REPEAT_MODE_OFF
                                                                }
                                                        }) {
                                                            Icon(
                                                                imageVector = when (repeatMode) {
                                                                    Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                                                                    Player.REPEAT_MODE_ALL -> Icons.Default.Repeat
                                                                    else -> Icons.Default.Repeat
                                                                },
                                                                contentDescription = null,
                                                                tint = if (repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(
                                                                    alpha = 0.6f
                                                                ),
                                                                modifier = Modifier.size(24.dp)
                                                            )
                                                        }

                                                        IconButton(onClick = {
                                                            onToggleDetailedInfo(
                                                                true
                                                            )
                                                        }) {
                                                            Icon(
                                                                Icons.Default.Info,
                                                                null,
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(24.dp)
                                                            )
                                                        }

                                                        IconButton(onClick = {
                                                            onShowQueueDialogChange(
                                                                true
                                                            )
                                                        }) {
                                                            Icon(
                                                                Icons.AutoMirrored.Filled.PlaylistPlay,
                                                                null,
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(32.dp)
                                                            )
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(24.dp))

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.Center,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        IconButton(
                                                            onClick = { player.seekToPrevious() },
                                                            modifier = Modifier.size(64.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.SkipPrevious,
                                                                null,
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(40.dp)
                                                            )
                                                        }

                                                        Spacer(modifier = Modifier.width(24.dp))

                                                        components.MorphingPlayPauseButton(
                                                            isPlaying,
                                                            { if (isPlaying) player.pause() else player.play() },
                                                            Modifier.requiredSize(88.dp),
                                                            MaterialTheme.colorScheme.primary,
                                                            MaterialTheme.colorScheme.onPrimary,
                                                            42.dp,
                                                            2.dp
                                                        )

                                                        Spacer(modifier = Modifier.width(28.dp))

                                                        IconButton(
                                                            onClick = { player.seekToNext() },
                                                            modifier = Modifier.size(60.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.SkipNext,
                                                                null,
                                                                modifier = Modifier.size(36.dp),
                                                                tint = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(12.dp))

                                                val hasLyrics = !lyrics.isNullOrBlank()

                                                Row(
                                                    modifier = Modifier.fillMaxWidth()
                                                        .padding(horizontal = 8.dp),
                                                    horizontalArrangement = Arrangement.Center,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Surface(
                                                        onClick = { onLyricsFullScreenChange(true) },
                                                        modifier = Modifier.height(56.dp)
                                                            .width(140.dp),
                                                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                                        shape = RoundedCornerShape(28.dp),
                                                        border = BorderStroke(
                                                            2.dp,
                                                            MaterialTheme.colorScheme.outline
                                                        )
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxSize(),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceEvenly
                                                        ) {
                                                            Icon(
                                                                imageVector = if (hasLyrics) Icons.Default.Lyrics else Icons.Default.MusicNote,
                                                                contentDescription = null,
                                                                tint = if (hasLyrics) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(
                                                                    alpha = 0.6f
                                                                ),
                                                                modifier = Modifier.size(24.dp)
                                                            )

                                                            VerticalDivider(
                                                                modifier = Modifier.height(20.dp)
                                                                    .width(1.dp),
                                                                color = MaterialTheme.colorScheme.primary.copy(
                                                                    alpha = 0.15f
                                                                )
                                                            )

                                                            Box(
                                                                modifier = Modifier.size(40.dp),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                if (isSearchingLyrics) {
                                                                    CircularProgressIndicator(
                                                                        modifier = Modifier.size(18.dp),
                                                                        strokeWidth = 2.dp
                                                                    )
                                                                } else {
                                                                    IconButton(
                                                                        onClick = {
                                                                            currentSong?.let { song ->
                                                                                scope.launch {
                                                                                    onIsSearchingLyricsChange(
                                                                                        true
                                                                                    )
                                                                                    try {
                                                                                        val cleanTitle =
                                                                                            song.title.replace(
                                                                                                Regex(
                                                                                                    "(?i)\\(.*?\\)|\\[.*?\\]"
                                                                                                ),
                                                                                                ""
                                                                                            ).trim()
                                                                                        val artist =
                                                                                            if (song.artist != "<unknown>") song.artist else ""
                                                                                        val queryUrl =
                                                                                            "https://lrclib.net/api/search?artist_name=${
                                                                                                java.net.URLEncoder.encode(
                                                                                                    artist,
                                                                                                    "UTF-8"
                                                                                                )
                                                                                            }&track_name=${
                                                                                                java.net.URLEncoder.encode(
                                                                                                    cleanTitle,
                                                                                                    "UTF-8"
                                                                                                )
                                                                                            }"
                                                                                        val response =
                                                                                            withContext(
                                                                                                Dispatchers.IO
                                                                                            ) {
                                                                                                java.net.URL(
                                                                                                    queryUrl
                                                                                                )
                                                                                                    .readText()
                                                                                            }
                                                                                        val jsonArray =
                                                                                            org.json.JSONArray(
                                                                                                response
                                                                                            )
                                                                                        val results =
                                                                                            mutableListOf<LyricResult>()
                                                                                        for (i in 0 until jsonArray.length()) {
                                                                                            val item =
                                                                                                jsonArray.getJSONObject(
                                                                                                    i
                                                                                                )
                                                                                            results.add(
                                                                                                LyricResult(
                                                                                                    trackName = item.getString(
                                                                                                        "trackName"
                                                                                                    ),
                                                                                                    artistName = item.getString(
                                                                                                        "artistName"
                                                                                                    ),
                                                                                                    albumName = item.optString(
                                                                                                        "albumName",
                                                                                                        ""
                                                                                                    ),
                                                                                                    syncedLyrics = item.optString(
                                                                                                        "syncedLyrics",
                                                                                                        ""
                                                                                                    ),
                                                                                                    plainLyrics = item.optString(
                                                                                                        "plainLyrics",
                                                                                                        ""
                                                                                                    )
                                                                                                )
                                                                                            )
                                                                                        }
                                                                                        onOnlineLyricsResultsChange(
                                                                                            results
                                                                                        )
                                                                                        onShowLyricsSelectionDialogChange(
                                                                                            true
                                                                                        )
                                                                                    } catch (e: Exception) {
                                                                                        Toast.makeText(
                                                                                            context,
                                                                                            "Error: ${e.message}",
                                                                                            Toast.LENGTH_SHORT
                                                                                        ).show()
                                                                                    } finally {
                                                                                        onIsSearchingLyricsChange(
                                                                                            false
                                                                                        )
                                                                                    }
                                                                                }
                                                                            }
                                                                        },
                                                                        modifier = Modifier.size(32.dp)
                                                                    ) {
                                                                        Icon(
                                                                            Icons.Default.Search,
                                                                            null,
                                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                            modifier = Modifier.size(
                                                                                22.dp
                                                                            )
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(32.dp))

                                                if (isEditingLyrics) {
                                                    AlertDialog(
                                                        onDismissRequest = {
                                                            isEditingLyrics = false
                                                        },
                                                        title = { Text(strings.editLyrics) },
                                                        text = {
                                                            OutlinedTextField(
                                                                value = editedLyricsText,
                                                                onValueChange = {
                                                                    editedLyricsText = it
                                                                },
                                                                modifier = Modifier.fillMaxWidth()
                                                                    .height(300.dp),
                                                                placeholder = { Text("[00:00.00] Lyrics...") }
                                                            )
                                                        },
                                                        confirmButton = {
                                                            Button(onClick = {
                                                                currentSong?.let { song ->
                                                                    lyrics = editedLyricsText
                                                                    song.lyrics = editedLyricsText
                                                                    context.getSharedPreferences(
                                                                        "lyrics_cache",
                                                                        Context.MODE_PRIVATE
                                                                    )
                                                                        .edit {
                                                                            putString(
                                                                                song.data,
                                                                                editedLyricsText
                                                                            )
                                                                        }
                                                                }
                                                                isEditingLyrics = false
                                                            }) {
                                                                Text(strings.save)
                                                            }
                                                        },
                                                        dismissButton = {
                                                            TextButton(onClick = {
                                                                isEditingLyrics = false
                                                            }) {
                                                                Text(strings.close)
                                                            }
                                                        }
                                                    )
                                                }

                                                if (showLyricsSelectionDialog) {
                                                    LyricsSelectionDialog(
                                                        results = onlineLyricsResults,
                                                        onDismiss = {
                                                            onShowLyricsSelectionDialogChange(
                                                                false
                                                            )
                                                        },
                                                        onLyricSelected = { selectedLyric ->
                                                            currentSong?.let { song ->
                                                                lyrics = selectedLyric
                                                                song.lyrics = selectedLyric
                                                                context.getSharedPreferences(
                                                                    "lyrics_cache",
                                                                    Context.MODE_PRIVATE
                                                                )
                                                                    .edit {
                                                                        putString(
                                                                            song.data,
                                                                            selectedLyric
                                                                        )
                                                                    }
                                                                scope.launch(Dispatchers.IO) {
                                                                    embedLyricsToFile(
                                                                        song.data,
                                                                        selectedLyric
                                                                    )
                                                                }
                                                            }
                                                            onShowLyricsSelectionDialogChange(false)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                val effectiveVideo = currentVideo ?: if (isVideoByMetadata) {
                                    val title =
                                        player.currentMediaItem?.mediaMetadata?.title?.toString()
                                    videos.find { it.title == title }
                                } else null

                                androidx.compose.animation.AnimatedVisibility(
                                    visible = showVideoPlayer && !forceCloseVideo,
                                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                                    modifier = Modifier.zIndex(200f)
                                ) {
                                    val activity = (context as? android.app.Activity)
                                        ?: (context as? android.content.ContextWrapper)?.baseContext as? android.app.Activity

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
                                        onRestoreVideo = { forceCloseVideo = false },
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
                                        onToggleDetailedInfo = onToggleDetailedInfo,
                                        songDetails = songDetails,
                                        playbackSpeed = playbackSpeed,
                                        onSpeedChange = onSpeedChange,
                                        settings = settings,
                                        components = components,
                                        strings = strings
                                    )
                                }

                                if (showEqualizerDialog) {
                                    ModalBottomSheet(
                                        onDismissRequest = { onShowEqualizerDialogChange(false) },
                                        sheetState = rememberModalBottomSheetState(
                                            skipPartiallyExpanded = true
                                        ),
                                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                                    ) {
                                        components.EcualizadorPanel()
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