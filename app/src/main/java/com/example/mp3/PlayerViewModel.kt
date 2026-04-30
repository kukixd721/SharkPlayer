package com.example.mp3

import android.app.Application
import android.content.Context
import android.content.ContentUris
import android.net.Uri
import androidx.compose.runtime.*
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.palette.graphics.Palette
import android.graphics.BitmapFactory
import kotlinx.coroutines.*

enum class PaletteStyle(val displayName: String) {
    TONAL_SPOT("Tonal Spot"),
    VIBRANT("Vibrant"),
    EXPRESSIVE("Expressive"),
    SPRITZ("Spritz"),
    RAINBOW("Rainbow"),
    FRUIT_SALAD("Fruit Salad")
}

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val appPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    private val configPrefs = context.getSharedPreferences("app_config", Context.MODE_PRIVATE)

    // UI State
    var cancionesState = mutableStateOf<List<Song>>(emptyList())
    var videosState = mutableStateOf<List<Video>>(emptyList())
    var isScanning = mutableStateOf(false)
    var scanProgress = mutableFloatStateOf(0f)
    var scanStatus = mutableStateOf("")
    var showLogcat by mutableStateOf(false)

    // Settings State
    var selectedLanguage by mutableStateOf(
        Language.valueOf(appPrefs.getString("selected_language", Language.ES.name) ?: Language.ES.name)
    )
    var selectedFontName by mutableStateOf(appPrefs.getString("selected_font", "Outfit") ?: "Outfit")
    var useDynamicColor by mutableStateOf(appPrefs.getBoolean("use_dynamic_color", true))
    var showWelcome by mutableStateOf(appPrefs.getBoolean("is_first_run", true))
    var isKaraokeEnabled by mutableStateOf(configPrefs.getBoolean("enable_karaoke", true))
    
    var roundnessLarge by mutableIntStateOf(appPrefs.getInt("roundness_large", 28))
    var roundnessMedium by mutableIntStateOf(appPrefs.getInt("roundness_medium", 16))
    var roundnessSmall by mutableIntStateOf(appPrefs.getInt("roundness_small", 8))
    
    var customAccentColor by mutableIntStateOf(appPrefs.getInt("custom_accent_color", android.graphics.Color.BLUE))
    // Nuevos colores para personalización tonal (cuando no hay colores dinámicos)
    var customSecondaryColor by mutableIntStateOf(appPrefs.getInt("custom_secondary_color", android.graphics.Color.GRAY))
    var customTertiaryColor by mutableIntStateOf(appPrefs.getInt("custom_tertiary_color", android.graphics.Color.CYAN))

    var playbackSpeed by mutableFloatStateOf(appPrefs.getFloat("playback_speed", 1.0f))
    var lyricsFontSize by mutableIntStateOf(appPrefs.getInt("lyrics_font_size", 20))
    var keepScreenOn by mutableStateOf(appPrefs.getBoolean("keep_screen_on", false))
    var crossfadeDuration by mutableFloatStateOf(appPrefs.getFloat("crossfade_duration", 0f))
    var crossfadeEnabled by mutableStateOf(appPrefs.getBoolean("crossfade_enabled", false))
    var visualizerEnabled by mutableStateOf(appPrefs.getBoolean("visualizer_enabled", true))
    var showLyricsControls by mutableStateOf(appPrefs.getBoolean("show_lyrics_controls", true))
    var showLyricsSlider by mutableStateOf(appPrefs.getBoolean("show_lyrics_slider", true))
    var showLyricsMainControls by mutableStateOf(appPrefs.getBoolean("show_lyrics_main_controls", true))
    var showLyricsExtraControls by mutableStateOf(appPrefs.getBoolean("show_lyrics_extra_controls", true))
    var centerLyrics by mutableStateOf(appPrefs.getBoolean("center_lyrics", true))
    
    // --- NUEVAS OPCIONES DE PERSONALIZACIÓN ---
    // 0: Sistema, 1: Claro, 2: Oscuro
    var themeMode by mutableIntStateOf(appPrefs.getInt("theme_mode", 0))

    var paletteStyle: PaletteStyle by mutableStateOf(
        PaletteStyle.valueOf(appPrefs.getString("palette_style", PaletteStyle.TONAL_SPOT.name) ?: PaletteStyle.TONAL_SPOT.name)
    )
    // 0: Barras, 1: Círculo, 2: Onda, 3: Puntos
    var visualizerStyle by mutableIntStateOf(appPrefs.getInt("visualizer_style", 0))
    // true: Cuadrícula, false: Lista
    var useGridViewHome by mutableStateOf(appPrefs.getBoolean("use_grid_view_home", false))
    var useGridViewLibrary by mutableStateOf(appPrefs.getBoolean("use_grid_view_library", false))
    // true: Colores basados en la carátula, false: Colores personalizados/sistema
    var useArtDynamicColor by mutableStateOf(appPrefs.getBoolean("use_art_dynamic_color", false))
    var autoLyrics by mutableStateOf(appPrefs.getBoolean("auto_lyrics", true))
    var useOledMode by mutableStateOf(appPrefs.getBoolean("use_oled_mode", false))
    
    // --- NUEVO: PALETAS SELECCIONADAS POR LA IA ---
    // Almacena el índice de la paleta elegida (0 a 5). -1 es "Automático".
    var selectedCuratedPalette by mutableIntStateOf(appPrefs.getInt("selected_curated_palette", -1))
    
    var unifiedLyricsBackground by mutableStateOf(appPrefs.getBoolean("unified_lyrics_background", true))
    
    // --- NUEVO: FONDO PERSONALIZADO ---
    var backgroundImageUri by mutableStateOf(appPrefs.getString("background_image_uri", null))
    var backgroundAlpha by mutableFloatStateOf(appPrefs.getFloat("background_alpha", 0.4f))
    var useImageDynamicColor by mutableStateOf(appPrefs.getBoolean("use_image_dynamic_color", false))
    
    // Colores extraídos de la carátula actual
    var artPrimaryColor by mutableIntStateOf(appPrefs.getInt("art_primary_color", android.graphics.Color.BLUE))
    var artSecondaryColor by mutableIntStateOf(appPrefs.getInt("art_secondary_color", android.graphics.Color.GRAY))
    var artTertiaryColor by mutableIntStateOf(appPrefs.getInt("art_tertiary_color", android.graphics.Color.CYAN))

    var ignoredFolders by mutableStateOf(
        appPrefs.getStringSet("ignored_folders", emptySet()) ?: emptySet()
    )
    
    var extraVideoPaths by mutableStateOf(
        appPrefs.getStringSet("extra_video_paths", emptySet()) ?: emptySet()
    )

    // Playback state tracked in VM
    var sleepTimerRemaining by mutableLongStateOf(0L)
    private var startTime = System.currentTimeMillis()
    private var currentSongId: String? = null
    private var lastExtractedMediaId: String? = null
    private var colorExtractionJob: Job? = null

    private val _mediaController = mutableStateOf<MediaController?>(null)
    val mediaController: MediaController? get() = _mediaController.value

    init {
        if (!showWelcome) {
            loadSongs()
        }
    }

    fun loadSongs() {
        viewModelScope.launch(Dispatchers.IO) {
            // CARGA RELÁMPAGO: Solo lo que MediaStore ya tiene listo
            val music = getAudioFiles(context)
            
            withContext(Dispatchers.Main) {
                cancionesState.value = music
            }

            // TODO lo demás se va al final de la cola, para que no moleste al usuario
            launch(Dispatchers.Default) {
                // Esperamos a que la UI esté bien asentada
                delay(8000) 
                
                if (isActive) {
                    val videos = getVideoFiles(context, extraVideoPaths.toList())
                    withContext(Dispatchers.Main) {
                        videosState.value = videos
                    }
                    // El escaneo de letras es lo que más pesa, lo dejamos para el puro final
                    scanAndSaveLyrics(context, music)
                }
            }
        }
    }

    fun updateUnifiedLyricsBackground(enabled: Boolean) {
        unifiedLyricsBackground = enabled
        appPrefs.edit().putBoolean("unified_lyrics_background", enabled).apply()
    }

    fun startScan(strings: AppStrings) {
        viewModelScope.launch(Dispatchers.IO) {
            isScanning.value = true
            scanStatus.value = strings.searchingAudio
            val iniciales = getAudioFiles(context)

            withContext(Dispatchers.Main) {
                cancionesState.value = iniciales
            }

            scanStatus.value = strings.analyzingMetadata
            val total = iniciales.size
            var procesadas = 0

            iniciales.forEachIndexed { _, song ->
                scanAndSaveLyrics(context, listOf(song))
                procesadas++
                scanProgress.floatValue = procesadas.toFloat() / total
                scanStatus.value = "${strings.processing}: ${song.title}"
            }

            val actualizadas = getAudioFiles(context)
            withContext(Dispatchers.Main) {
                cancionesState.value = actualizadas
                isScanning.value = false
                scanStatus.value = strings.ready
            }
        }
    }

    fun toggleExtraVideoPath(path: String) {
        val current = extraVideoPaths.toMutableSet()
        if (current.contains(path)) {
            current.remove(path)
        } else {
            current.add(path)
        }
        extraVideoPaths = current
        appPrefs.edit { putStringSet("extra_video_paths", current) }
        loadSongs()
    }

    fun toggleIgnoreFolder(path: String) {
        val current = ignoredFolders.toMutableSet()
        if (current.contains(path)) {
            current.remove(path)
        } else {
            current.add(path)
        }
        ignoredFolders = current
        appPrefs.edit { putStringSet("ignored_folders", current) }
        loadSongs()
    }

    fun setMediaController(controller: MediaController?) {
        _mediaController.value = controller
        controller?.let {
            currentSongId = it.currentMediaItem?.mediaId
            it.setPlaybackSpeed(playbackSpeed)
            it.addListener(playerListener)
            // Forzamos la actualización de colores al conectar el controlador
            updateColorsFromMediaItem(it.currentMediaItem)
        }
    }

    private val playerListener = object : Player.Listener {
        @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            configPrefs.edit { putInt("audio_session_id", audioSessionId) }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateColorsFromMediaItem(mediaItem)
        }
    }

    private fun updateColorsFromMediaItem(mediaItem: MediaItem?) {
        if (!useArtDynamicColor || mediaItem == null) return
        
        val songId = mediaItem.mediaId
        // Eliminamos el check restrictivo para permitir actualizaciones de estilo
        lastExtractedMediaId = songId
        
        // CANCELAR cualquier extracción previa
        colorExtractionJob?.cancel()
        
        val mediaUri = mediaItem.requestMetadata.mediaUri?.toString() ?: 
                       mediaItem.localConfiguration?.uri?.toString()
        
        colorExtractionJob = viewModelScope.launch(Dispatchers.IO) {
            val song = cancionesState.value.find { it.id.toString() == songId }
            val path = mediaUri ?: song?.data
            
            var artBytes: ByteArray? = null
            
            // 1. Intentar obtener carátula embebida
            if (path != null) {
                artBytes = getAlbumArt(path)
            }
            
            // 2. Si falla, intentar vía MediaStore (Album ID)
            if (artBytes == null && song != null && song.albumId != 0L) {
                try {
                    val albumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"), 
                        song.albumId
                    )
                    context?.contentResolver?.openInputStream(albumArtUri)?.use { 
                        artBytes = it.readBytes()
                    }
                } catch (e: Exception) { /* No hay carátula */ }
            }

            if (artBytes != null) {
                val bitmap = BitmapFactory.decodeByteArray(artBytes!!, 0, artBytes!!.size)
                if (bitmap != null) {
                    val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, 150, 150, false)
                    val palette = Palette.from(scaledBitmap)
                        .maximumColorCount(32)
                        .addFilter { rgb, hsl -> 
                            // Filtro personalizado: Aceptamos casi todo, pero evitamos 
                            // tonos extremadamente oscuros y sucios que ensucian la UI.
                            hsl[2] > 0.05f && hsl[2] < 0.95f
                        }
                        .generate()
                    
                    // --- EXTRACCIÓN DE IDENTIDAD REAL ---
                    // Buscamos el color que un humano identificaría como "el color del disco"
                    val primarySwatch = palette.vibrantSwatch 
                        ?: palette.mutedSwatch 
                        ?: palette.dominantSwatch
                    
                    val primary = primarySwatch?.rgb ?: customAccentColor
                    
                    // Secundario y Terciario basados en la armonía real de la paleta
                    val secondary = (palette.lightVibrantSwatch ?: palette.lightMutedSwatch ?: primarySwatch)?.rgb ?: primary
                    val tertiary = (palette.darkVibrantSwatch ?: palette.darkMutedSwatch ?: palette.mutedSwatch)?.rgb ?: secondary
                    
                    if (currentCoroutineContext().isActive) {
                        updateColors(primary, secondary, tertiary)
                    }
                }
            } else {
                // Si no hay carátula, usamos los colores personalizados del usuario
                if (currentCoroutineContext().isActive) {
                    updateColors(customAccentColor, customSecondaryColor, customTertiaryColor)
                }
            }
        }
    }

    private suspend fun updateColors(p: Int, s: Int, t: Int) {
        withContext(Dispatchers.Main) {
            artPrimaryColor = p
            artSecondaryColor = s
            artTertiaryColor = t
            
            appPrefs?.edit {
                putInt("art_primary_color", p)
                putInt("art_secondary_color", s)
                putInt("art_tertiary_color", t)
            }
        }
    }

    fun startSleepTimer(minutes: Long) {
        sleepTimerRemaining = minutes * 60
        viewModelScope.launch {
            while (sleepTimerRemaining > 0) {
                delay(1000)
                sleepTimerRemaining -= 1
                if (sleepTimerRemaining == 0L) {
                    mediaController?.pause()
                }
            }
        }
    }

    fun updateLanguage(language: Language) {
        selectedLanguage = language
        appPrefs.edit { putString("selected_language", language.name) }
    }

    fun updateFont(fontName: String) {
        selectedFontName = fontName
        appPrefs.edit { putString("selected_font", fontName) }
    }

    fun updateDynamicColor(enabled: Boolean) {
        useDynamicColor = enabled
        appPrefs.edit { putBoolean("use_dynamic_color", enabled) }
    }

    fun updateWelcomeFinished() {
        showWelcome = false
        appPrefs.edit { putBoolean("is_first_run", false) }
    }

    fun updateKaraokeEnabled(enabled: Boolean) {
        isKaraokeEnabled = enabled
        configPrefs.edit { putBoolean("enable_karaoke", enabled) }
    }

    fun updateRoundnessLarge(value: Int) {
        roundnessLarge = value
        appPrefs.edit { putInt("roundness_large", value) }
    }

    fun updateRoundnessMedium(value: Int) {
        roundnessMedium = value
        appPrefs.edit { putInt("roundness_medium", value) }
    }

    fun updateRoundnessSmall(value: Int) {
        roundnessSmall = value
        appPrefs.edit { putInt("roundness_small", value) }
    }

    fun updateCustomAccentColor(color: Int) {
        customAccentColor = color
        appPrefs.edit { putInt("custom_accent_color", color) }
    }

    // Funciones para actualizar los nuevos colores personalizados (secundario y terciario)
    // Esto permite que el usuario elija exactamente cómo quiere que se vea su tema tonal
    fun updateCustomSecondaryColor(color: Int) {
        customSecondaryColor = color
        appPrefs.edit { putInt("custom_secondary_color", color) }
    }

    fun updateCustomTertiaryColor(color: Int) {
        customTertiaryColor = color
        appPrefs.edit { putInt("custom_tertiary_color", color) }
    }

    fun updatePlaybackSpeed(speed: Float) {
        playbackSpeed = speed
        mediaController?.setPlaybackSpeed(speed)
        appPrefs.edit { putFloat("playback_speed", speed) }
    }

    fun updateLyricsFontSize(size: Int) {
        lyricsFontSize = size
        appPrefs.edit { putInt("lyrics_font_size", size) }
    }

    fun updateKeepScreenOn(enabled: Boolean) {
        keepScreenOn = enabled
        appPrefs.edit { putBoolean("keep_screen_on", enabled) }
    }

    fun updateCrossfadeDuration(duration: Float) {
        crossfadeDuration = duration
        appPrefs.edit { putFloat("crossfade_duration", duration) }
        notifyServiceUpdate()
    }

    fun updateCrossfadeEnabled(enabled: Boolean) {
        crossfadeEnabled = enabled
        appPrefs.edit { putBoolean("crossfade_enabled", enabled) }
        notifyServiceUpdate()
    }

    private fun notifyServiceUpdate() {
        val intent = android.content.Intent(context, PlaybackService::class.java).apply {
            action = "UPDATE_SETTINGS"
        }
        context?.startService(intent)
    }

    fun updateVisualizerEnabled(enabled: Boolean) {
        visualizerEnabled = enabled
        appPrefs.edit { putBoolean("visualizer_enabled", enabled) }
    }

    fun updateShowLyricsControls(enabled: Boolean) {
        showLyricsControls = enabled
        appPrefs.edit { putBoolean("show_lyrics_controls", enabled) }
    }

    fun updateShowLyricsSlider(enabled: Boolean) {
        showLyricsSlider = enabled
        appPrefs.edit { putBoolean("show_lyrics_slider", enabled) }
    }

    fun updateShowLyricsMainControls(enabled: Boolean) {
        showLyricsMainControls = enabled
        appPrefs.edit { putBoolean("show_lyrics_main_controls", enabled) }
    }

    fun updateShowLyricsExtraControls(enabled: Boolean) {
        showLyricsExtraControls = enabled
        appPrefs.edit { putBoolean("show_lyrics_extra_controls", enabled) }
    }

    fun updateCenterLyrics(enabled: Boolean) {
        centerLyrics = enabled
        appPrefs.edit { putBoolean("center_lyrics", enabled) }
    }

    fun updateThemeMode(mode: Int) {
        themeMode = mode
        appPrefs.edit { putInt("theme_mode", mode) }
    }

    fun updatePaletteStyle(style: PaletteStyle) {
        paletteStyle = style
        appPrefs.edit { putString("palette_style", style.name) }
        // Forzar actualización de colores con el nuevo estilo si hay una canción sonando
        mediaController?.currentMediaItem?.let { updateColorsFromMediaItem(it) }
    }

    fun updateVisualizerStyle(style: Int) {
        visualizerStyle = style
        appPrefs.edit { putInt("visualizer_style", style) }
    }

    fun updateUseGridViewHome(enabled: Boolean) {
        useGridViewHome = enabled
        appPrefs.edit { putBoolean("use_grid_view_home", enabled) }
    }

    fun updateUseGridViewLibrary(enabled: Boolean) {
        useGridViewLibrary = enabled
        appPrefs.edit { putBoolean("use_grid_view_library", enabled) }
    }

    fun updateUseArtDynamicColor(enabled: Boolean) {
        useArtDynamicColor = enabled
        appPrefs.edit { putBoolean("use_art_dynamic_color", enabled) }
        if (enabled) {
            updateColorsFromMediaItem(mediaController?.currentMediaItem)
        }
    }

    fun updateAutoLyrics(value: Boolean) {
        autoLyrics = value
        appPrefs.edit { putBoolean("auto_lyrics", value) }
    }

    fun updateUseOledMode(enabled: Boolean) {
        useOledMode = enabled
        appPrefs.edit { putBoolean("use_oled_mode", enabled) }    }

    fun updateSelectedCuratedPalette(index: Int) {
        selectedCuratedPalette = index
        appPrefs.edit { putInt("selected_curated_palette", index) }
        // Si el usuario cambia la paleta manual, desactivamos el color dinámico por carátula
        // para que se note el cambio de "tema" elegido.
        if (useArtDynamicColor) {
            updateUseArtDynamicColor(false)
        }
    }

    fun updateBackgroundImage(uri: String?) {
        backgroundImageUri = uri
        appPrefs.edit { putString("background_image_uri", uri) }
        if (uri != null && useImageDynamicColor) {
            extractColorsFromImage(uri)
        }
    }

    fun updateBackgroundAlpha(alpha: Float) {
        backgroundAlpha = alpha
        appPrefs.edit { putFloat("background_alpha", alpha) }
    }

    fun updateUseImageDynamicColor(enabled: Boolean) {
        useImageDynamicColor = enabled
        appPrefs.edit { putBoolean("use_image_dynamic_color", enabled) }
        if (enabled && backgroundImageUri != null) {
            extractColorsFromImage(backgroundImageUri!!)
        } else if (!enabled && !useArtDynamicColor) {
            // Volver a colores por defecto o sistema si se desactiva
            viewModelScope.launch {
                updateColors(customAccentColor, customSecondaryColor, customTertiaryColor)
            }
        }
    }

    private fun extractColorsFromImage(uriString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val uri = Uri.parse(uriString)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val bitmap = BitmapFactory.decodeStream(input)
                    if (bitmap != null) {
                        val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, 150, 150, false)
                        val palette = Palette.from(scaledBitmap).maximumColorCount(32).generate()
                        
                        val primarySwatch = palette.vibrantSwatch ?: palette.mutedSwatch ?: palette.dominantSwatch
                        val primary = primarySwatch?.rgb ?: customAccentColor
                        val secondary = (palette.lightVibrantSwatch ?: palette.lightMutedSwatch ?: primarySwatch)?.rgb ?: primary
                        val tertiary = (palette.darkVibrantSwatch ?: palette.darkMutedSwatch ?: palette.mutedSwatch)?.rgb ?: secondary
                        
                        updateColors(primary, secondary, tertiary)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
