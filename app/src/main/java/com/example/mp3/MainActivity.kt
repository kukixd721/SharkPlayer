package com.example.mp3

import android.content.Context
import android.Manifest
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import android.view.WindowManager
import android.app.PictureInPictureParams
import android.util.Rational
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.mp3.ui.components.*
import com.example.mp3.ui.screens.*
import com.example.mp3.ui.theme.blendWithNeutral
import com.example.mp3.ui.theme.getTypography
import com.example.mp3.ui.components.formatTime
import com.example.mp3.ui.components.rememberPlayerPosition
import com.google.common.util.concurrent.ListenableFuture

val LocalStrings = staticCompositionLocalOf<AppStrings> { SpanishStrings }

class MainActivity : ComponentActivity() {
    private val viewModel: PlayerViewModel by viewModels()
    private val downloadViewModel: DownloadViewModel by viewModels()
    private lateinit var controllerFuture: ListenableFuture<MediaController>

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (!allGranted) {
            val strings = when (viewModel.selectedLanguage) {
                Language.ES -> SpanishStrings
                Language.EN -> EnglishStrings
            }
            Toast.makeText(this, strings.permissionsRequired, Toast.LENGTH_SHORT).show()
        }
    }

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lanzarPermisos()
        StatisticsManager.performIntegrityCheck(this)

        setContent {
            val context = LocalContext.current
            val strings = when (viewModel.selectedLanguage) {
                Language.ES -> SpanishStrings
                Language.EN -> EnglishStrings
            }

            CompositionLocalProvider(LocalStrings provides strings) {
                SideEffect {
                    if (viewModel.keepScreenOn) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                // color scheme
                val isDark = when (viewModel.themeMode) {
                    1 -> false
                    2 -> true
                    else -> isSystemInDarkTheme()
                }

                // material you
                val curatedPalettes = listOf(
                    Triple(Color(0xFFB4C5FF), Color(0xFFDDE1FF), Color(0xFFFFD9E3)),
                    Triple(Color(0xFFA2AD94), Color(0xFFCFD6C5), Color(0xFFE2E2E2)),
                    Triple(Color(0xFFE4C4A3), Color(0xFFF3E5D8), Color(0xFFD4E4D1)),
                    Triple(Color(0xFFA0D1D1), Color(0xFFC7E6E6), Color(0xFFF4D7D7)),
                    Triple(Color(0xFFE8B7B7), Color(0xFFF5DADA), Color(0xFFDCE2F9)),
                    Triple(Color(0xFF607D8B), Color(0xFF90A4AE), Color(0xFFCFD8DC))
                )

                val colorScheme = when {
                    viewModel.useArtDynamicColor || (viewModel.useImageDynamicColor && viewModel.backgroundImageUri != null) -> {
                        buildCustomColorScheme(
                            Color(viewModel.artPrimaryColor),
                            Color(viewModel.artSecondaryColor),
                            Color(viewModel.artTertiaryColor),
                            isDark,
                            viewModel.paletteStyle,
                            viewModel.useOledMode
                        )
                    }

                    viewModel.selectedCuratedPalette != -1 -> {
                        val palette = curatedPalettes.getOrElse(viewModel.selectedCuratedPalette) { curatedPalettes[0] }
                        buildCustomColorScheme(palette.first, palette.second, palette.third, isDark, viewModel.paletteStyle, viewModel.useOledMode)
                    }

                    else -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && viewModel.useDynamicColor) {
                            val systemScheme = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                            if (viewModel.paletteStyle == PaletteStyle.TONAL_SPOT) {
                                systemScheme
                            } else {
                                val seed = systemScheme.primary
                                buildCustomColorScheme(seed, seed, seed, isDark, viewModel.paletteStyle, viewModel.useOledMode)
                            }
                        } else {
                            val primary = Color(viewModel.customAccentColor)
                            buildCustomColorScheme(primary, primary, primary, isDark, viewModel.paletteStyle, viewModel.useOledMode)
                        }
                    }
                }

                val typography = remember(viewModel.selectedFontName) {
                    getTypography(viewModel.selectedFontName)
                }

                MaterialTheme(
                    colorScheme = colorScheme,
                    typography = typography
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // FONDO DE IMAGEN PERSONALIZADO
                        if (viewModel.backgroundImageUri != null) {
                            val context = LocalContext.current
                            val painter = coil.compose.rememberAsyncImagePainter(
                                model = coil.request.ImageRequest.Builder(context)
                                    .data(viewModel.backgroundImageUri)
                                    .crossfade(true)
                                    .build()
                            )
                            androidx.compose.foundation.Image(
                                painter = painter,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                alpha = viewModel.backgroundAlpha
                            )
                        }

                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = if (viewModel.backgroundImageUri != null) Color.Transparent else MaterialTheme.colorScheme.background
                        ) {
                            if (viewModel.showWelcome) {
                                WelcomeScreen(
                                    state = WelcomeState(
                                        useDynamicColor = viewModel.useDynamicColor,
                                        onDynamicColorChange = { viewModel.updateDynamicColor(it) },
                                        useArtDynamicColor = viewModel.useArtDynamicColor,
                                        onUseArtDynamicColorChange = { viewModel.updateUseArtDynamicColor(it) },
                                        karaokeEnabled = viewModel.isKaraokeEnabled,
                                        onKaraokeChange = { viewModel.updateKaraokeEnabled(it) },
                                        selectedLanguage = viewModel.selectedLanguage,
                                        onLanguageChange = { viewModel.updateLanguage(it) },
                                        isScanning = viewModel.isScanning.value,
                                        scanProgress = viewModel.scanProgress.floatValue,
                                        scanStatus = viewModel.scanStatus.value,
                                        onStartScan = { viewModel.startScan(strings) },
                                        onFinish = { viewModel.updateWelcomeFinished() }
                                    )
                                )
                            } else {
                                val settings = PlayerSettings(
                                    useDynamicColor = viewModel.useDynamicColor,
                                    onDynamicColorChange = { viewModel.updateDynamicColor(it) },
                                    roundnessLarge = viewModel.roundnessLarge,
                                    onRoundnessLargeChange = { viewModel.updateRoundnessLarge(it) },
                                    roundnessMedium = viewModel.roundnessMedium,
                                    onRoundnessMediumChange = { viewModel.updateRoundnessMedium(it) },
                                    roundnessSmall = viewModel.roundnessSmall,
                                    onRoundnessSmallChange = { viewModel.updateRoundnessSmall(it) },
                                    customAccentColor = Color(viewModel.customAccentColor),
                                    onCustomAccentColorChange = { viewModel.updateCustomAccentColor(it.toArgb()) },
                                    customSecondaryColor = Color(viewModel.customSecondaryColor),
                                    onCustomSecondaryColorChange = { viewModel.updateCustomSecondaryColor(it.toArgb()) },
                                    customTertiaryColor = Color(viewModel.customTertiaryColor),
                                    onCustomTertiaryColorChange = { viewModel.updateCustomTertiaryColor(it.toArgb()) },
                                    playbackSpeed = viewModel.playbackSpeed,
                                    onPlaybackSpeedChange = { viewModel.updatePlaybackSpeed(it) },
                                    lyricsFontSize = viewModel.lyricsFontSize,
                                    onLyricsFontSizeChange = { viewModel.updateLyricsFontSize(it) },
                                    keepScreenOn = viewModel.keepScreenOn,
                                    onKeepScreenOnChange = { viewModel.updateKeepScreenOn(it) },
                                    crossfadeDuration = viewModel.crossfadeDuration,
                                    onCrossfadeChange = { viewModel.updateCrossfadeDuration(it) },
                                    crossfadeEnabled = viewModel.crossfadeEnabled,
                                    onCrossfadeEnabledChange = { viewModel.updateCrossfadeEnabled(it) },
                                    karaokeEnabled = viewModel.isKaraokeEnabled,
                                    onKaraokeChange = { viewModel.updateKaraokeEnabled(it) },
                                    selectedFontName = viewModel.selectedFontName,
                                    onFontChange = { viewModel.updateFont(it) },
                                    selectedLanguage = viewModel.selectedLanguage,
                                    onLanguageChange = { viewModel.updateLanguage(it) },
                                    visualizerEnabled = viewModel.visualizerEnabled,
                                    onVisualizerEnabledChange = { viewModel.updateVisualizerEnabled(it) },
                                    showLyricsControls = viewModel.showLyricsControls,
                                    onShowLyricsControlsChange = { viewModel.updateShowLyricsControls(it) },
                                    showLyricsSlider = viewModel.showLyricsSlider,
                                    onShowLyricsSliderChange = { viewModel.updateShowLyricsSlider(it) },
                                    showLyricsMainControls = viewModel.showLyricsMainControls,
                                    onShowLyricsMainControlsChange = { viewModel.updateShowLyricsMainControls(it) },
                                    showLyricsExtraControls = viewModel.showLyricsExtraControls,
                                    onShowLyricsExtraControlsChange = { viewModel.updateShowLyricsExtraControls(it) },
                                    centerLyrics = viewModel.centerLyrics,
                                    onCenterLyricsChange = { viewModel.updateCenterLyrics(it) },
                                    sleepTimerRemaining = viewModel.sleepTimerRemaining,
                                    onSetSleepTimer = { viewModel.startSleepTimer(it) },
                                    onShowLogcat = { viewModel.showLogcat = true },
                                    getTypography = { getTypography(it) },
                                    getAlbumArt = { getAlbumArt(it) },
                                    formatTime = { formatTime(it) },
                                    rememberPlayerPosition = { rememberPlayerPosition(it) },
                                    themeMode = viewModel.themeMode,
                                    onThemeModeChange = { viewModel.updateThemeMode(it) },
                                    visualizerStyle = viewModel.visualizerStyle,
                                    onVisualizerStyleChange = { viewModel.updateVisualizerStyle(it) },
                                    useGridViewHome = viewModel.useGridViewHome,
                                    onUseGridViewHomeChange = { viewModel.updateUseGridViewHome(it) },
                                    useGridViewLibrary = viewModel.useGridViewLibrary,
                                    onUseGridViewLibraryChange = { viewModel.updateUseGridViewLibrary(it) },
                                    useArtDynamicColor = viewModel.useArtDynamicColor,
                                    onUseArtDynamicColorChange = { viewModel.updateUseArtDynamicColor(it) },
                                    paletteStyle = viewModel.paletteStyle,
                                    onPaletteStyleChange = { viewModel.updatePaletteStyle(it) },
                                    artPrimaryColor = Color(viewModel.artPrimaryColor),
                                    artSecondaryColor = Color(viewModel.artSecondaryColor),
                                    artTertiaryColor = Color(viewModel.artTertiaryColor),
                                    autoLyrics = viewModel.autoLyrics,
                                    onAutoLyricsChange = { viewModel.updateAutoLyrics(it) },
                                    useOledMode = viewModel.useOledMode,
                                    onUseOledModeChange = { viewModel.updateUseOledMode(it) },
                                    selectedCuratedPalette = viewModel.selectedCuratedPalette,
                                    onSelectedCuratedPaletteChange = { viewModel.updateSelectedCuratedPalette(it) },
                                    unifiedLyricsBackground = viewModel.unifiedLyricsBackground,
                                    onUnifiedLyricsBackgroundChange = { viewModel.updateUnifiedLyricsBackground(it) },
                                    backgroundImageUri = viewModel.backgroundImageUri,
                                    onBackgroundImageChange = { viewModel.updateBackgroundImage(it) },
                                    backgroundAlpha = viewModel.backgroundAlpha,
                                    onBackgroundAlphaChange = { viewModel.updateBackgroundAlpha(it) },
                                    useImageDynamicColor = viewModel.useImageDynamicColor,
                                    onUseImageDynamicColorChange = { viewModel.updateUseImageDynamicColor(it) },
                                    extraVideoPaths = viewModel.extraVideoPaths,
                                    onToggleExtraVideoPath = { viewModel.toggleExtraVideoPath(it) }
                                )

                                val components = PlayerComponents(
                                    KaraokeView = { lyrics, pos, size, centered, selectionMode, selectedIndices, showShadow, onToggle, onSeek ->
                                        KaraokeView(
                                            lyricsList = lyrics,
                                            currentPositionMs = pos,
                                            fontSize = size.sp,
                                            centered = centered,
                                            selectionMode = selectionMode,
                                            selectedIndices = selectedIndices,
                                            showShadow = showShadow,
                                            onToggleSelection = onToggle,
                                            onLyricClick = onSeek
                                        )
                                    },
                                    SubMusicList = { title, songs, player, isPlaying, config, onBack, cover, onEditClick ->
                                        SubMusicList(title, songs, player, isPlaying, config, onBack, cover, onEditClick)
                                    },
                                    ArtistList = { artists, media, onClick, settings, query ->
                                        ArtistList(artists, media, onClick, settings, query)
                                    },
                                    AlbumList = { albums, media, onClick, settings, query ->
                                        AlbumList(albums, media, onClick, settings, query)
                                    },
                                    FolderList = { folders, onClick, onIgnore, ignored, settings, query ->
                                        FolderList(folders, onClick, onIgnore, ignored, settings, query)
                                    },
                                    PlaylistList = { playlists, onClick, onCreate, onDelete, onRename, onSetImage, songs, settings ->
                                        PlaylistList(playlists, onClick, onCreate, onDelete, onRename, onSetImage, songs, settings)
                                    },
                                    MusicList = ::MusicList,
                                    MetadataRow = ::MetadataRow,
                                    EcualizadorPanel = ::EcualizadorPanel,
                                    MorphingPlayPauseButton = { isPlaying, onClick, modifier, mainColor, iconColor, iconSize, borderWidth ->
                                        MorphingPlayPauseButton(isPlaying, onClick, modifier, mainColor, iconColor, iconSize, borderWidth)
                                    }
                                )

                                PlayerScreen(
                                    songs = viewModel.cancionesState.value,
                                    videos = viewModel.videosState.value,
                                    player = viewModel.mediaController,
                                    settings = settings,
                                    components = components,
                                    downloadViewModel = downloadViewModel,
                                    onRefreshLibrary = { viewModel.loadSongs() },
                                    onIgnoreFolder = { viewModel.toggleIgnoreFolder(it) },
                                    ignoredFolders = viewModel.ignoredFolders.filterNotNull().toSet()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun lanzarPermisos() {
        val permissions = mutableListOf<String>()

        // audio and media permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        // microphone permission (required for audio visualizer)
        permissions.add(Manifest.permission.RECORD_AUDIO)
        
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            verificarOptimizacionBateria()
        }
    }

    private fun verificarOptimizacionBateria() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = android.content.Intent()
            val packageName = packageName
            val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener({
            try {
                if (!controllerFuture.isCancelled) {
                    val controller = controllerFuture.get()
                    viewModel.setMediaController(controller)
                    Log.d("SharkPlayer", "MediaController conectado exitosamente")
                } else {
                    Log.w("SharkPlayer", "MediaController future fue cancelado")
                }
            } catch (e: ExecutionException) {
                Log.e("SharkPlayer", "Error de ejecución al obtener MediaController: ${e.cause?.message}")
            } catch (e: InterruptedException) {
                Log.e("SharkPlayer", "Hilo interrumpido al obtener MediaController: ${e.message}")
                Thread.currentThread().interrupt()
            } catch (e: CancellationException) {
                Log.w("SharkPlayer", "Conexión del controlador cancelada: ${e.message}")
            } catch (e: Throwable) {
                Log.e("SharkPlayer", "Error inesperado al obtener MediaController: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onUserLeaveHint() {
        val player = viewModel.mediaController
        val currentMediaItem = player?.currentMediaItem
        val isVideo = currentMediaItem?.mediaMetadata?.mediaType == androidx.media3.common.MediaMetadata.MEDIA_TYPE_VIDEO 
                      || (currentMediaItem?.mediaId?.startsWith("vid_") == true)

        if (player?.isPlaying == true && isVideo) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        MediaController.releaseFuture(controllerFuture)
    }
}

// --- FUNCIÓN HELPER PARA CONSTRUIR EL ESQUEMA DE COLOR ---
@Composable
fun buildCustomColorScheme(
    pRaw: Color,
    sRaw: Color,
    tRaw: Color,
    isDark: Boolean,
    paletteStyle: PaletteStyle,
    useOledMode: Boolean
): ColorScheme {
    val p = blendWithNeutral(pRaw, isDark, paletteStyle)
    val s = blendWithNeutral(sRaw, isDark, paletteStyle)
    val t = blendWithNeutral(tRaw, isDark, paletteStyle)

    return if (isDark) {
        val backgroundTone = if (useOledMode) Color.Black else lerp(p, Color.Black, 0.82f)
        val surfaceBase = if (useOledMode) Color.Black else lerp(p, Color.Black, 0.75f)

        darkColorScheme(
            primary = lerp(p, Color.White, 0.45f),
            onPrimary = Color.Black,
            primaryContainer = p.copy(alpha = 0.3f),
            onPrimaryContainer = Color.White,
            secondary = lerp(s, Color.White, 0.4f),
            onSecondary = Color.Black,
            secondaryContainer = s.copy(alpha = 0.25f),
            tertiary = t,
            onTertiary = Color.Black,
            background = backgroundTone,
            surface = surfaceBase,
            surfaceVariant = lerp(s, surfaceBase, 0.25f),
            surfaceContainerLowest = if (useOledMode) Color.Black else lerp(backgroundTone, Color.Black, 0.4f),
            surfaceContainerLow = lerp(surfaceBase, p, 0.06f),
            surfaceContainer = lerp(surfaceBase, p, 0.14f),
            surfaceContainerHigh = lerp(surfaceBase, p, 0.24f),
            surfaceContainerHighest = lerp(surfaceBase, p, 0.38f),
            onSurface = Color(0xFFE2E2E2),
            onSurfaceVariant = Color(0xFFC4C7C5),
            outline = p.copy(alpha = 0.35f),
            outlineVariant = p.copy(alpha = 0.18f)
        )
    } else {
        val surfaceBase = lerp(p, Color.White, 0.85f)
        lightColorScheme(
            primary = p,
            onPrimary = Color.White,
            primaryContainer = p.copy(alpha = 0.3f),
            onPrimaryContainer = lerp(p, Color.Black, 0.8f),
            background = lerp(p, Color.White, 0.92f),
            surface = surfaceBase,
            surfaceVariant = lerp(s, surfaceBase, 0.45f),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = lerp(surfaceBase, p, 0.08f),
            surfaceContainer = lerp(surfaceBase, p, 0.15f),
            surfaceContainerHigh = lerp(surfaceBase, p, 0.25f),
            surfaceContainerHighest = lerp(surfaceBase, p, 0.35f),
            onSurface = Color.Black,
            outline = p.copy(alpha = 0.6f)
        )
    }
}
