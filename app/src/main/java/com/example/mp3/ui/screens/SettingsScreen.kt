package com.example.mp3.ui.screens

import android.media.audiofx.BassBoost
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mp3.Language
import com.example.mp3.LocalStrings
import com.example.mp3.Song
import com.example.mp3.ui.components.SettingsActionCard
import com.example.mp3.ui.components.SettingsCategoryCard
import com.example.mp3.ui.components.SettingsSwitchCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    padding: PaddingValues,
    settings: PlayerSettings,
    songs: List<Song>,
    onScanClick: (onProgress: (String) -> Unit, onComplete: (List<Song>) -> Unit) -> Unit,
    scanAndSaveLyrics: suspend (android.content.Context, List<Song>, (String) -> Unit) -> Pair<Int, Int>,
    components: PlayerComponents,
    onOpenStyleScreen: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    
    var activeCategory by remember { mutableStateOf<String?>(null) }
    BackHandler(enabled = activeCategory != null) { activeCategory = null }

    // Estados de procesos
    var isScanning by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    val scanLogs = remember { mutableStateListOf<String>() }
    val syncLogs = remember { mutableStateListOf<String>() }
    
    // Estados de diálogos
    var showEqPanel by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showLyricsDialog by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showChangelog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        // --- VISTA PRINCIPAL ---
        AnimatedVisibility(
            visible = activeCategory == null,
            enter = fadeIn() + slideInHorizontally { -it },
            exit = fadeOut() + slideOutHorizontally { -it }
        ) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Text(
                    text = strings.configuration,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(24.dp)
                )

                SettingsCategoryCard(Icons.Default.LibraryMusic, Color(0xFF4CAF50), strings.musicManagement, strings.musicManagementDesc) { activeCategory = "music" }
                SettingsCategoryCard(Icons.Default.Palette, Color(0xFF2196F3), strings.appearance, strings.appearanceDesc) { activeCategory = "appearance" }
                SettingsCategoryCard(Icons.Default.PlayArrow, Color(0xFFFF9800), strings.playback, strings.playbackDesc) { activeCategory = "playback" }
                SettingsCategoryCard(Icons.Default.TouchApp, Color(0xFF9C27B0), strings.behavior, strings.behaviorDesc) { activeCategory = "behavior" }
                SettingsCategoryCard(Icons.Default.AutoAwesome, Color(0xFF00BCD4), strings.aiIntegration, strings.aiIntegrationDesc) { activeCategory = "ai" }
                SettingsCategoryCard(Icons.Default.GraphicEq, Color(0xFFE91E63), strings.equalizer, strings.equalizerDesc) { activeCategory = "audio" }
                SettingsCategoryCard(Icons.Default.DeveloperMode, Color(0xFF607D8B), strings.developerOptions, strings.developerOptionsDesc) { activeCategory = "dev" }
                
                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        // --- SUB-PÁGINAS ---
        AnimatedVisibility(
            visible = activeCategory != null,
            enter = fadeIn() + slideInHorizontally { it },
            exit = fadeOut() + slideOutHorizontally { it }
        ) {
            activeCategory?.let { category ->
                Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    TopAppBar(
                        title = { Text(when(category) {
                            "music" -> strings.musicManagement
                            "appearance" -> strings.appearance
                            "playback" -> strings.playback
                            "behavior" -> strings.behavior
                            "ai" -> strings.aiIntegration
                            "audio" -> strings.equalizer
                            else -> strings.developerOptions
                        }, fontWeight = FontWeight.Bold) },
                        navigationIcon = { IconButton(onClick = { activeCategory = null }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )

                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                        when(category) {
                            "music" -> {
                                SettingsActionCard(Icons.Default.Refresh, strings.syncMusic, strings.syncMusicDesc, { syncLogs.clear(); showSyncDialog = true; isSyncing = true; onScanClick({ syncLogs.add(it) }, { isSyncing = false }) })
                                SettingsActionCard(Icons.Default.Search, strings.lyricsAnalyzer, strings.lyricsAnalyzerDesc, { scope.launch { scanLogs.clear(); showLyricsDialog = true; isScanning = true; scanAndSaveLyrics(context, songs) { scanLogs.add(it) }; isScanning = false } })
                            }
                            "appearance" -> AppearanceSection(strings, settings, { showLanguageDialog = true }, { showFontDialog = true }, onOpenStyleScreen)
                            "playback" -> {
                                Text("${strings.playbackSpeed}: ${String.format("%.2fx", settings.playbackSpeed)}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(16.dp))
                                Slider(value = settings.playbackSpeed, onValueChange = settings.onPlaybackSpeedChange, valueRange = 0.5f..2.0f, steps = 5, modifier = Modifier.padding(horizontal = 16.dp))
                                SettingsSwitchCard(Icons.Default.Audiotrack, strings.crossfade, strings.crossfadeDesc, settings.crossfadeEnabled, settings.onCrossfadeEnabledChange)
                                AnimatedVisibility(settings.crossfadeEnabled) {
                                    Column(Modifier.padding(horizontal = 16.dp)) {
                                        Text("${strings.crossfade}: ${String.format("%.1fs", settings.crossfadeDuration)}", style = MaterialTheme.typography.labelSmall)
                                        Slider(value = settings.crossfadeDuration, onValueChange = settings.onCrossfadeChange, valueRange = 0f..10f)
                                    }
                                }
                            }
                            "behavior" -> {
                                SettingsSwitchCard(Icons.Default.Lightbulb, strings.keepScreenOn, strings.keepScreenOnDesc, settings.keepScreenOn, settings.onKeepScreenOnChange)
                                SettingsSwitchCard(Icons.Default.Lyrics, strings.karaokeMode, strings.karaokeModeDesc, settings.karaokeEnabled, settings.onKaraokeChange)
                            }
                            "ai" -> {
                                SettingsSwitchCard(Icons.Default.AutoAwesome, strings.autoLyrics, strings.autoLyricsDesc, settings.autoLyrics, settings.onAutoLyricsChange)
                            }
                            "audio" -> {
                                SettingsActionCard(
                                    icon = Icons.Default.Tune,
                                    title = strings.equalizerPro,
                                    subtitle = if (settings.playbackSpeed != 1.0f) strings.effectsActive else strings.equalizerDesc,
                                    onClick = { showEqPanel = true }
                                )
                                val timerRemaining = settings.sleepTimerRemaining
                                val timerLabel = if (timerRemaining > 0) {
                                    "${strings.stopMusicIn}: ${timerRemaining / 60 + 1} ${strings.minutes}"
                                } else {
                                    strings.sleepTimerDesc
                                }
                                SettingsActionCard(
                                    icon = Icons.Default.Timer,
                                    title = strings.sleepTimer,
                                    subtitle = timerLabel,
                                    onClick = { showSleepTimerDialog = true }
                                )
                            }
                            "dev" -> {
                                SettingsActionCard(Icons.Default.BugReport, strings.logcatMonitor, strings.logcatMonitorDesc, settings.onShowLogcat)
                                SettingsActionCard(Icons.Default.History, strings.changelog, strings.versionHistory, { showChangelog = true })
                                SettingsActionCard(Icons.Default.Info, strings.aboutApp, "Shark Player v1.7", { })
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }

    // --- DIÁLOGOS ---
    if (showEqPanel) ModalBottomSheet(
        onDismissRequest = { showEqPanel = false },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) { components.EcualizadorPanel(); Spacer(Modifier.height(48.dp)) }
    if (showSleepTimerDialog) {
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text(strings.sleepTimer) },
            confirmButton = { TextButton(onClick = { showSleepTimerDialog = false }) { Text(strings.close) } },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            text = {
                Column {
                    listOf(15, 30, 45, 60, 0).forEach { min ->
                        ListItem(
                            headlineContent = { Text(if(min==0) strings.cancelTimer else "$min ${strings.minutes}") },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { settings.onSetSleepTimer(min * 60L); showSleepTimerDialog = false }
                        )
                    }
                }
            }
        )
    }
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(strings.selectLanguage) },
            confirmButton = { TextButton(onClick = { showLanguageDialog = false }) { Text(strings.close) } },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            text = {
                Column {
                    Language.entries.forEach { lang ->
                        ListItem(
                            headlineContent = { Text(lang.label) },
                            leadingContent = { RadioButton(lang == settings.selectedLanguage, null) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { settings.onLanguageChange(lang); showLanguageDialog = false }
                        )
                    }
                }
            }
        )
    }
    if (showFontDialog) {
        AlertDialog(
            onDismissRequest = { showFontDialog = false },
            title = { Text(strings.selectFont) },
            confirmButton = { TextButton(onClick = { showFontDialog = false }) { Text(strings.close) } },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            text = {
                Column {
                    listOf(strings.systemFont, "Montserrat", "Roboto", "Poppins", "Open Sans").forEach { font ->
                        ListItem(
                            headlineContent = { Text(font, style = settings.getTypography(if(font==strings.systemFont) "Sistema" else font).bodyLarge) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { settings.onFontChange(if(font==strings.systemFont) "Sistema" else font); showFontDialog = false }
                        )
                    }
                }
            }
        )
    }
    if (showLyricsDialog) {
        AlertDialog(
            onDismissRequest = { if(!isScanning) showLyricsDialog = false },
            title = { Text(strings.lyricsAnalyzer) },
            confirmButton = { if(!isScanning) Button(onClick={showLyricsDialog=false}){Text(strings.close)} },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            text = {
                Column {
                    LazyColumn(Modifier.height(200.dp).fillMaxWidth().background(MaterialTheme.colorScheme.primary.copy(0.05f), RoundedCornerShape(12.dp)).padding(8.dp)){
                        items(scanLogs.asReversed()){ Text(it, fontSize = 10.sp) }
                    }
                    if(isScanning) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top=8.dp))
                }
            }
        )
    }
    if (showSyncDialog) {
        AlertDialog(
            onDismissRequest = { if(!isSyncing) showSyncDialog = false },
            title = { Text(strings.syncLibrary) },
            confirmButton = { if(!isSyncing) Button(onClick={showSyncDialog=false}){Text(strings.accept)} },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            text = {
                Column {
                    LazyColumn(Modifier.height(200.dp).fillMaxWidth().background(MaterialTheme.colorScheme.primary.copy(0.05f), RoundedCornerShape(12.dp)).padding(8.dp)){
                        items(syncLogs.asReversed()){ Text(it, fontSize = 10.sp) }
                    }
                    if(isSyncing) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top=8.dp))
                }
            }
        )
    }
    if (showChangelog) {
        AlertDialog(
            onDismissRequest = { showChangelog = false },
            title = { Text(strings.changelog) },
            confirmButton = { TextButton(onClick={showChangelog=false}){Text(strings.accept)} },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()).heightIn(max=400.dp)) {
                    ChangelogItem("v1.7", "Nuevo", listOf("🎨 Reorganización total por ventanas.", "✨ Navegación fluida.", "🛠️ Más opciones de personalización."), true)
                }
            }
        )
    }
}

@Composable
fun AppearanceSection(strings: com.example.mp3.AppStrings, settings: PlayerSettings, onLang: () -> Unit, onFont: () -> Unit, onColorStyle: () -> Unit) {
    var showVisuals by remember { mutableStateOf(false) }
    SettingsActionCard(Icons.Default.Translate, strings.language, settings.selectedLanguage.label, onLang)
    SettingsActionCard(when(settings.themeMode){1->Icons.Default.LightMode; 2->Icons.Default.DarkMode; else->Icons.Default.SettingsSuggest}, strings.themeMode, when(settings.themeMode){1->strings.themeLight; 2->strings.themeDark; else->strings.themeSystem}, { settings.onThemeModeChange((settings.themeMode+1)%3) })
    
    // Opción de Fondo OLED (Solo visible si no es modo claro forzado)
    if (settings.themeMode != 1) {
        SettingsSwitchCard(
            Icons.Default.Contrast,
            strings.oledBackground,
            strings.oledBackgroundDesc,
            settings.useOledMode,
            settings.onUseOledModeChange
        )
    }

    // Nuevo Botón Profesional para Estilo de Color
    SettingsActionCard(
        icon = Icons.Default.ColorLens,
        title = strings.colorStyle,
        subtitle = if(settings.useArtDynamicColor) strings.albumArtColors else strings.wallpaperColors,
        onClick = onColorStyle
    )

    SettingsActionCard(Icons.Default.FontDownload, strings.selectFont, settings.selectedFontName, onFont)
    SettingsSwitchCard(Icons.Default.BarChart, strings.audioVisualizer, strings.audioVisualizerDesc, settings.visualizerEnabled, settings.onVisualizerEnabledChange)
    AnimatedVisibility(settings.visualizerEnabled) { Row(Modifier.horizontalScroll(rememberScrollState()).padding(start=32.dp), horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf(strings.styleBars, strings.styleCircle, strings.styleWave, strings.styleDots).forEachIndexed { i, name -> FilterChip(settings.visualizerStyle==i, {settings.onVisualizerStyleChange(i)}, {Text(name.split(" ").last(), fontSize=10.sp)}) } } }
    SettingsSwitchCard(Icons.Default.SettingsApplications, strings.fullScreenLyricsControls, strings.fullScreenLyricsControlsDesc, settings.showLyricsControls, settings.onShowLyricsControlsChange)
    AnimatedVisibility(settings.showLyricsControls) { Column(Modifier.padding(start=32.dp)) {
        SettingsSwitchCard(Icons.Default.LinearScale, strings.showLyricsSlider, "", settings.showLyricsSlider, settings.onShowLyricsSliderChange, false, true)
        SettingsSwitchCard(Icons.Default.PlayCircle, strings.showLyricsMainControls, "", settings.showLyricsMainControls, settings.onShowLyricsMainControlsChange, false, true)
        SettingsSwitchCard(Icons.Default.Shuffle, strings.showLyricsExtraControls, "", settings.showLyricsExtraControls, settings.onShowLyricsExtraControlsChange, false, true)
        SettingsSwitchCard(Icons.Default.FormatAlignCenter, strings.centerLyrics, "", settings.centerLyrics, settings.onCenterLyricsChange, false, true)
    } }
    SettingsActionCard(if(showVisuals) Icons.Default.ExpandLess else Icons.Default.ExpandMore, strings.visualCustomization, strings.customizeTheme, { showVisuals = !showVisuals })
    AnimatedVisibility(showVisuals) { Column(Modifier.padding(start=16.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(0.2f), RoundedCornerShape(16.dp)).padding(16.dp)) {
        AdjustmentItem(strings.roundnessLarge, settings.roundnessLarge.toFloat(), {settings.onRoundnessLargeChange(it.toInt())}, 0f..50f, "dp")
        AdjustmentItem(strings.roundnessMedium, settings.roundnessMedium.toFloat(), {settings.onRoundnessMediumChange(it.toInt())}, 0f..30f, "dp")
        AdjustmentItem(strings.roundnessSmall, settings.roundnessSmall.toFloat(), {settings.onRoundnessSmallChange(it.toInt())}, 0f..20f, "dp")
        AdjustmentItem(strings.lyricsFontSize, settings.lyricsFontSize.toFloat(), {settings.onLyricsFontSizeChange(it.toInt())}, 16f..48f, "sp")
        SettingsSwitchCard(if(settings.useGridViewHome) Icons.Default.GridView else Icons.AutoMirrored.Filled.ViewList, "${strings.viewMode} (${strings.home})", if(settings.useGridViewHome) strings.viewGrid else strings.viewList, settings.useGridViewHome, settings.onUseGridViewHomeChange, false, true)
        SettingsSwitchCard(if(settings.useGridViewLibrary) Icons.Default.GridView else Icons.AutoMirrored.Filled.ViewList, "${strings.viewMode} (${strings.music})", if(settings.useGridViewLibrary) strings.viewGrid else strings.viewList, settings.useGridViewLibrary, settings.onUseGridViewLibraryChange, false, true)
        Button(onClick={settings.onRoundnessLargeChange(32);settings.onRoundnessMediumChange(16);settings.onRoundnessSmallChange(8);settings.onLyricsFontSizeChange(20)}, Modifier.fillMaxWidth()){ Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(8.dp)); Text(strings.reset) }
    } }
}

@Composable
private fun AdjustmentItem(label: String, value: Float, onValueChange: (Float) -> Unit, range: ClosedFloatingPointRange<Float>, unit: String) {
    Column(Modifier.padding(vertical = 4.dp)) { Text("$label: ${value.toInt()}$unit", style = MaterialTheme.typography.labelSmall); Slider(value, onValueChange, valueRange = range) }
}

@Composable
private fun ChangelogItem(v: String, d: String, changes: List<String>, isLatest: Boolean = false) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = if(isLatest) MaterialTheme.colorScheme.primaryContainer.copy(0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(0.2f)) {
        Column(Modifier.padding(12.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(v, fontWeight = FontWeight.Bold); Text(d, style = MaterialTheme.typography.labelSmall) }; changes.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) } }
    }
}
