package com.example.mp3.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.media3.session.MediaController
import com.example.mp3.*
import com.example.mp3.ui.screens.PlayerSettings
import com.example.mp3.ui.screens.PlayerComponents
import com.example.mp3.ui.components.AudioVisualizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Context
import android.widget.Toast
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.mp3.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsView(
    player: MediaController,
    currentSong: Song?,
    lyrics: String?,
    onLyricsChange: (String) -> Unit,
    onClose: () -> Unit,
    isSelectionMode: Boolean,
    onSelectionModeChange: (Boolean) -> Unit,
    selectedLyricLines: Set<Int>,
    onSelectedLinesChange: (Set<Int>) -> Unit,
    showDetailedInfo: Boolean,
    onToggleDetailedInfo: () -> Unit,
    isEditingLyrics: Boolean,
    onEditLyrics: (Boolean) -> Unit,
    currentPosition: Long,
    totalDuration: Long,
    isShuffleOn: Boolean,
    repeatMode: Int,
    songDetails: SongDetails?,
    settings: PlayerSettings,
    components: PlayerComponents,
    strings: AppStrings,
    onShowQueue: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val art by produceState<ByteArray?>(null, currentSong?.data) {
        value = withContext(Dispatchers.IO) {
            currentSong?.let { settings.getAlbumArt(it.data) }
        }
    }
    var showLyricsMenu by remember { mutableStateOf(false) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset = if (source == NestedScrollSource.UserInput) {
                available.copy(x = 0f)
            } else {
                Offset.Zero
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            // Cabecera tipo "Isla Dinámica" / Cápsula Morfológica (Sólida y Expresiva)
            Surface(
                onClick = { showLyricsMenu = true },
                color = if (settings.backgroundImageUri != null) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = settings.backgroundAlpha) else Color(0xFF1E1E1E),
                shape = RoundedCornerShape(32.dp),
                tonalElevation = 0.dp,
                shadowElevation = if (settings.backgroundImageUri != null) 0.dp else 8.dp,
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
                        color = Color.Black
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(art ?: currentSong?.data)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            error = painterResource(R.drawable.ic_launcher_foreground)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.padding(end = 12.dp)) {
                        Text(
                            text = currentSong?.title ?: "---",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentSong?.artist ?: "---",
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
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                if (!lyrics.isNullOrBlank()) {
                    val curPos = settings.rememberPlayerPosition(player)
                    if (settings.karaokeEnabled && lyrics.contains(Regex("\\[\\d{2}:\\d{2}"))) {
                        val parsedLyrics = remember(lyrics) {
                            parseSincronizedLyrics(lyrics)
                        }
                        components.KaraokeView(
                            parsedLyrics,
                            curPos,
                            settings.lyricsFontSize,
                            settings.centerLyrics,
                            isSelectionMode,
                            selectedLyricLines,
                            settings.unifiedLyricsBackground,
                            { index ->
                                val newSet = if (selectedLyricLines.contains(index)) {
                                    selectedLyricLines - index
                                } else {
                                    if (selectedLyricLines.size < 5) selectedLyricLines + index else selectedLyricLines
                                }
                                onSelectedLinesChange(newSet)
                            },
                            { time ->
                                if (!isSelectionMode) player.seekTo(time)
                            }
                        )
                    } else {
                        val lines = remember(lyrics) { lyrics.split("\n") }
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 32.dp, horizontal = 8.dp)
                        ) {
                            itemsIndexed(lines) { index, line ->
                                val isSelected = selectedLyricLines.contains(index)
                                Text(
                                    text = line,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = isSelectionMode) {
                                            val newSet = if (isSelected) {
                                                selectedLyricLines - index
                                            } else {
                                                if (selectedLyricLines.size < 5) selectedLyricLines + index else selectedLyricLines
                                            }
                                            onSelectedLinesChange(newSet)
                                        }
                                        .padding(vertical = 8.dp),
                                    textAlign = if (settings.centerLyrics) TextAlign.Center else TextAlign.Start,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.ExtraBold,
                                        fontSize = settings.lyricsFontSize.sp,
                                        lineHeight = (settings.lyricsFontSize * 1.5f).sp,
                                        letterSpacing = (-0.5).sp,
                                        shadow = if (settings.unifiedLyricsBackground) Shadow(
                                            color = Color.Black.copy(alpha = 0.5f),
                                            offset = Offset(2f, 2f),
                                            blurRadius = 8f
                                        ) else null
                                    ),
                                    color = if (isSelected && isSelectionMode)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        if (isSelectionMode && !isSelected)
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) // Material 3 disabled alpha
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // --- DIÁLOGO DE METADATOS TÉCNICOS ---
                if (showDetailedInfo && songDetails != null) {
                    MetadataDetailsDialog(
                        songDetails = songDetails,
                        currentSong = currentSong,
                        strings = strings,
                        onDismiss = onToggleDetailedInfo
                    )
                }
            }

            // Controles rápidos abajo (Solid Expressive Refactor)
            if (settings.showLyricsControls) {
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
                        // Play/Pause Circular Gigante (Sólido)
                        Surface(
                            onClick = { if (player.isPlaying) player.pause() else player.play() },
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape,
                            color = Color.White
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (player.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = Color.Black
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Slider en Cápsula (Sólida)
                        var sliderValue by remember { mutableFloatStateOf(0f) }
                        var isDragging by remember { mutableStateOf(false) }
                        LaunchedEffect(currentPosition) {
                            if (!isDragging) sliderValue = currentPosition.toFloat()
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(72.dp),
                            shape = RoundedCornerShape(32.dp),
                            color = if (settings.backgroundImageUri != null) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = settings.backgroundAlpha) else Color(0xFF1E1E1E),
                            tonalElevation = 0.dp,
                            shadowElevation = if (settings.backgroundImageUri != null) 0.dp else 4.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Slider(
                                    value = sliderValue.coerceIn(0f, totalDuration.toFloat().coerceAtLeast(1f)),
                                    onValueChange = { isDragging = true; sliderValue = it },
                                    onValueChangeFinished = {
                                        player.seekTo(sliderValue.toLong())
                                        isDragging = false
                                    },
                                    valueRange = 0f..totalDuration.toFloat().coerceAtLeast(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color.White,
                                        inactiveTrackColor = Color.DarkGray
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Fila inferior: Atrás + Toggle + More
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Botón Atrás Circular (Sólido)
                        Surface(
                            onClick = onClose,
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape,
                            color = if (settings.backgroundImageUri != null) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = settings.backgroundAlpha) else Color(0xFF1E1E1E),
                            tonalElevation = 0.dp,
                            shadowElevation = if (settings.backgroundImageUri != null) 0.dp else 4.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                            }
                        }

                        // Selector Synced/Static (Sólido)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                                .height(56.dp),
                            shape = RoundedCornerShape(32.dp),
                            color = if (settings.backgroundImageUri != null) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = settings.backgroundAlpha) else Color(0xFF1E1E1E),
                            tonalElevation = 0.dp,
                            shadowElevation = if (settings.backgroundImageUri != null) 0.dp else 4.dp
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
                                            if (settings.karaokeEnabled) Color.White else Color.Transparent,
                                            RoundedCornerShape(28.dp)
                                        )
                                        .clickable { settings.onKaraokeChange(true) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Synced",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Black,
                                        color = if (settings.karaokeEnabled) Color.Black else Color.White
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(
                                            if (!settings.karaokeEnabled) Color.White else Color.Transparent,
                                            RoundedCornerShape(28.dp)
                                        )
                                        .clickable { settings.onKaraokeChange(false) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Static",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Black,
                                        color = if (!settings.karaokeEnabled) Color.Black else Color.White
                                    )
                                }
                            }
                        }

                        // Botón Opciones Circular (Sólido)
                        Surface(
                            onClick = { showLyricsMenu = true },
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape,
                            color = if (settings.backgroundImageUri != null) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = settings.backgroundAlpha) else Color(0xFF1E1E1E),
                            tonalElevation = 0.dp,
                            shadowElevation = if (settings.backgroundImageUri != null) 0.dp else 4.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.MoreVert, null, tint = Color.White)
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }

        if (showLyricsMenu) {
            ModalBottomSheet(
                onDismissRequest = { showLyricsMenu = false },
                containerColor = if (settings.backgroundImageUri != null) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = settings.backgroundAlpha) else MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 0.dp,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outline) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 48.dp, top = 16.dp)
                ) {
                    ListItem(
                        headlineContent = { Text(currentSong?.title ?: "---", fontWeight = FontWeight.Black) },
                        supportingContent = { Text(currentSong?.artist ?: "---", fontWeight = FontWeight.Bold) },
                        leadingContent = {
                            Surface(
                                modifier = Modifier.size(56.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(art ?: currentSong?.data).build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent,
                            headlineColor = MaterialTheme.colorScheme.onSurface,
                            supportingColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    ListItem(
                        modifier = Modifier.clickable { onEditLyrics(true); showLyricsMenu = false },
                        headlineContent = { Text("Editar letras", fontWeight = FontWeight.Bold) },
                        leadingContent = { Icon(Icons.Default.Edit, null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent, headlineColor = MaterialTheme.colorScheme.onSurface, leadingIconColor = MaterialTheme.colorScheme.primary)
                    )

                    ListItem(
                        modifier = Modifier.clickable { onSelectionModeChange(!isSelectionMode); showLyricsMenu = false },
                        headlineContent = { Text(if (isSelectionMode) "Desactivar selección" else "Seleccionar para historia", fontWeight = FontWeight.Bold) },
                        leadingContent = { Icon(if (isSelectionMode) Icons.Default.CheckCircle else Icons.Default.FormatQuote, null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent, headlineColor = MaterialTheme.colorScheme.onSurface, leadingIconColor = MaterialTheme.colorScheme.primary)
                    )

                    ListItem(
                        modifier = Modifier.clickable { 
                            showLyricsMenu = false
                            onToggleDetailedInfo()
                        },
                        headlineContent = { Text("INFO", fontWeight = FontWeight.Black) },
                        leadingContent = { Icon(Icons.Default.Info, null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent, headlineColor = MaterialTheme.colorScheme.onSurface, leadingIconColor = MaterialTheme.colorScheme.primary)
                    )

                    ListItem(
                        modifier = Modifier.clickable {
                            showLyricsMenu = false
                            val currentSong_ = currentSong
                            if (currentSong_ != null) {
                                val currentColors = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                                val p = currentColors.getInt("art_primary_color", Color.Blue.toArgb())
                                val s = currentColors.getInt("art_secondary_color", Color.Gray.toArgb())
                                scope.launch {
                                    Toast.makeText(context, strings.generatingImage, Toast.LENGTH_SHORT).show()
                                    val artBytes = withContext(Dispatchers.IO) { settings.getAlbumArt(currentSong_.data) }
                                    val snippet = if (!lyrics.isNullOrBlank()) {
                                        val isLrc = lyrics.contains(Regex("\\[\\d{2}:\\d{2}"))
                                        if (isSelectionMode && selectedLyricLines.isNotEmpty()) {
                                            val lines = if (isLrc) {
                                                val parsed = parseSincronizedLyrics(lyrics)
                                                selectedLyricLines.sorted().map { parsed[it].text }
                                            } else {
                                                lyrics.split("\n").let { l -> selectedLyricLines.sorted().map { l[it] } }
                                            }
                                            lines.joinToString("\n")
                                        } else {
                                            val curPos = currentPosition
                                            if (isLrc) {
                                                val parsed = parseSincronizedLyrics(lyrics)
                                                val activeLine = parsed.indexOfLast { it.timeMs <= curPos }.coerceAtLeast(0)
                                                parsed.subList(activeLine, (activeLine + 3).coerceAtMost(parsed.size)).joinToString("\n") { it.text }
                                            } else {
                                                lyrics.split("\n").take(3).joinToString("\n")
                                            }
                                        }
                                    } else null
                                    StoryGenerator.generateAndShare(context, currentSong_, artBytes, p, s, snippet, currentPosition)
                                }
                            }
                        },
                        headlineContent = { Text("Compartir en historia", fontWeight = FontWeight.Bold) },
                        leadingContent = { Icon(Icons.Default.Share, null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent, headlineColor = MaterialTheme.colorScheme.onSurface, leadingIconColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}
