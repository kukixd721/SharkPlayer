//practicamente son los textos
package com.example.mp3
enum class Language(val code: String, val label: String) {
    ES("es", "Español"),
    EN("en", "English")
}

interface AppStrings {
    val welcomeTitle: String
    val welcomeSubtitle: String
    val selectLanguage: String
    val customizeTheme: String
    val dynamicColor: String
    val dynamicColorDesc: String
    val artDynamicColor: String
    val artDynamicColorDesc: String
    val artDynamicColorOnlyPlayer: String
    val artDynamicColorOnlyPlayerDesc: String
    val karaokeMode: String
    val karaokeModeDesc: String
    val scanLibrary: String
    val scanLibraryDesc: String
    val startScan: String
    val finish: String
    val next: String
    val previous: String
    val settings: String
    val language: String
    val downloadMusic: String
    val downloadMusicDesc: String
    val syncMusic: String
    val syncMusicDesc: String
    val lyricsAnalyzer: String
    val lyricsAnalyzerDesc: String
    val equalizer: String
    val equalizerDesc: String
    val logcatMonitor: String
    val logcatMonitorDesc: String
    val selectFont: String
    val searchSongs: String
    val noSongs: String
    val songsFound: String
    val home: String
    val music: String
    val downloads: String
    val all: String
    val artists: String
    val albums: String
    val favorites: String
    val videos: String
    val appearance: String
    val appearanceDesc: String
    val playback: String
    val playbackDesc: String
    val behavior: String
    val behaviorDesc: String
    val musicManagement: String
    val musicManagementDesc: String
    val aiIntegration: String
    val aiIntegrationDesc: String
    val backupRestore: String
    val backupRestoreDesc: String
    val developerOptions: String
    val developerOptionsDesc: String
    val sound: String
    val library: String
    val advanced: String
    val sharkPlayer: String
    val start: String
    val goodMorning: String
    val goodAfternoon: String
    val goodEvening: String
    val helloNightOwl: String
    val morningSubtitle: String
    val afternoonSubtitle: String
    val eveningSubtitle: String
    val nightSubtitle: String
    val yourPersonalAnthem: String
    val mostPlayed: String
    val musicInNumbers: String
    val totalTime: String
    val artistsInCollection: String
    val songs: String
    val quotes: List<String>
    val analyzingLibrary: String
    val analysisCompleted: String
    val close: String
    val syncLibrary: String
    val searchingNewFiles: String
    val syncFinished: String
    val accept: String
    val configuration: String
    val equalizerPro: String
    val effectsActive: String
    val effectsInactive: String
    val reset: String
    val frequencyAdjustments: String
    val superBass: String
    val year: String
    val genre: String
    val track: String
    val searchingAudio: String
    val analyzingMetadata: String
    val processing: String
    val ready: String
    val systemFont: String
    val libraryUpdated: String
    val unknownArtist: String
    val sleepTimer: String
    val sleepTimerDesc: String
    val minutes: String
    val off: String
    val setTimer: String
    val timerActive: String
    val stopMusicIn: String
    val cancelTimer: String
    val visualCustomization: String
    val roundnessLarge: String
    val roundnessMedium: String
    val roundnessSmall: String
    val accentColor: String
    val secondaryColor: String
    val tertiaryColor: String
    val playbackSpeed: String
    val lyricsFontSize: String
    val blurIntensity: String
    val blurBackground: String
    val blurBackgroundDesc: String
    val keepScreenOn: String
    val keepScreenOnDesc: String
    val crossfade: String
    val crossfadeDesc: String
    val changelog: String
    val versionHistory: String
    val playlists: String
    val createPlaylist: String
    val folderBrowser: String
    val folders: String
    val audioVisualizer: String
    val audioVisualizerDesc: String
    val addToPlaylist: String
    val editPlaylist: String
    val deletePlaylist: String
    val changeCover: String
    val playlistCreated: String
    val playlistDeleted: String
    val songAdded: String
    val enterPlaylistName: String
    val noArtists: String
    val noAlbums: String
    val noPlaylists: String
    val noFolders: String
    val noFavorites: String
    val noVideos: String
    val back: String
    val bitrate: String
    val sampleRate: String
    val fullScreenLyricsControls: String
    val fullScreenLyricsControlsDesc: String
    val showLyricsSlider: String
    val showLyricsMainControls: String
    val showLyricsExtraControls: String
    val centerLyrics: String
    val themeMode: String
    val themeSystem: String
    val themeLight: String
    val themeDark: String
    val visualizerStyle: String
    val styleBars: String
    val styleCircle: String
    val styleWave: String
    val styleDots: String
    val viewMode: String
    val viewGrid: String
    val viewList: String
    val videoAudioUrl: String
    val downloadType: String
    val format: String
    val quality: String
    val formatAndQuality: String
    val searchResults: String
    val original: String
    val searchOrUrl: String
    val recentDownloads: String
    val noActiveDownloads: String
    val downloading: String
    val pending: String
    val completed: String
    val downloadError: String
    val recentlyAdded: String
    val yourPersonalRhythm: String
    val aboutApp: String
    val yourTopFiveSongs: String
    val activateToAdjust: String
    val intensity: String
    val addToQueue: String
    val playNext: String
    val addedToQueue: String
    val addedToPlayNext: String
    val deleteSong: String
    val deleteSongConfirm: String
    val removeFromFavorites: String
    val addToFavorites: String
    val editLyrics: String
    val removeFromQueue: String
    val clearQueue: String
    val clearQueueConfirm: String
    val cancel: String
    val save: String
    val permissionsRequired: String
    val shuffleAll: String
    val noResultsFound: String
    val searchError: String
    val searchingSpotify: String
    val shareStory: String
    val shareStoryDesc: String
    val generatingImage: String
    val autoLyrics: String
    val autoLyricsDesc: String
    val editTags: String
    val title: String
    val artist: String
    val album: String
    val trackNumber: String
    val tagsUpdated: String
    val errorUpdatingTags: String
    val resizeFit: String
    val resizeZoom: String
    val resizeFill: String
    val brightness: String
    val volume: String
    val noArt: String
    val technicalInfo: String
    val noLyricsFound: String
    val paletteStyle: String
    val paletteStyleDesc: String
    val oledBackground: String
    val oledBackgroundDesc: String
    val paletteLavender: String
    val paletteForest: String
    val paletteDune: String
    val paletteReef: String
    val palettePetal: String
    val paletteSlate: String
    val unifiedLyricsBackground: String
    val unifiedLyricsBackgroundDesc: String
    val colorStyle: String
    val colorSource: String
    val wallpaperColors: String
    val albumArtColors: String
    val presetColors: String
    val browseByGenre: String
    val search: String
    val updateEngine: String
    val exploreMusic: String
    val selectFavoritePlatform: String
    val capture: String
    val lossless: String
    val play: String
    val share: String
    val info: String
    val playlist: String
    val song: String
    val dynamicsProcessing: String
    val gain: String
    val limit: String
}


object SpanishStrings : AppStrings {
    override val welcomeTitle = "¡Bienvenido a Shark Player!"
    override val welcomeSubtitle = "Configura tu experiencia antes de empezar"
    override val selectLanguage = "Seleccionar Idioma"
    override val customizeTheme = "Personaliza tu Tema"
    override val dynamicColor = "Colores Dinámicos"
    override val dynamicColorDesc = "Adapta la interfaz a tu fondo de pantalla"
    override val artDynamicColor = "Color dinámico de carátula"
    override val artDynamicColorDesc = "Extrae colores del arte del álbum actual"
    override val artDynamicColorOnlyPlayer = "Solo en el reproductor"
    override val artDynamicColorOnlyPlayerDesc = "Aplica los colores del arte solo a la pantalla de reproducción"
    override val karaokeMode = "Modo Karaoke"
    override val karaokeModeDesc = "Mostrar letras sincronizadas (.lrc)"
    override val scanLibrary = "Escaneo de Biblioteca"
    override val scanLibraryDesc = "Buscando música en tu dispositivo..."
    override val startScan = "Comenzar Escaneo"
    override val finish = "Finalizar"
    override val next = "Siguiente"
    override val previous = "Anterior"
    override val settings = "Ajustes"
    override val language = "Idioma"
    override val downloadMusic = "Descargar Música"
    override val downloadMusicDesc = "Descarga tus canciones favoritas desde la web"
    override val syncMusic = "Sincronizar Música"
    override val syncMusicDesc = "Escanear nuevos archivos locales"
    override val lyricsAnalyzer = "Analizador de Letras"
    override val lyricsAnalyzerDesc = "Busca letras locales (.lrc, .txt)"
    override val equalizer = "Ecualizador"
    override val equalizerDesc = "Ajustes de frecuencia y bajos"
    override val logcatMonitor = "Monitor Logcat"
    override val logcatMonitorDesc = "Ver registros internos del sistema"
    override val selectFont = "Seleccionar Fuente"
    override val searchSongs = "Buscar canciones..."
    override val noSongs = "No se encontraron canciones"
    override val songsFound = "encontradas"
    override val home = "Inicio"
    override val music = "Música"
    override val downloads = "Descargas"
    override val all = "Todas"
    override val artists = "Artistas"
    override val albums = "Álbumes"
    override val favorites = "Favoritos"
    override val videos = "Videos"
    override val appearance = "Apariencia"
    override val appearanceDesc = "Temas, colores y personalización visual"
    override val playback = "Reproducción"
    override val playbackDesc = "Velocidad, crossfade y controles"
    override val behavior = "Comportamiento"
    override val behaviorDesc = "Interacción y gestos de la aplicación"
    override val musicManagement = "Gestión de Música"
    override val musicManagementDesc = "Biblioteca, escaneo y sincronización"
    override val aiIntegration = "Integración IA"
    override val aiIntegrationDesc = "Análisis de letras y sugerencias"
    override val backupRestore = "Copia de Seguridad"
    override val backupRestoreDesc = "Respaldar y restaurar tus datos"
    override val developerOptions = "Opciones de Desarrollador"
    override val developerOptionsDesc = "Herramientas de depuración y registro"
    override val sound = "Sonido"
    override val library = "Biblioteca"
    override val advanced = "Herramientas"
    override val sharkPlayer = "Shark Player"
    override val start = "Comenzar"
    override val goodMorning = "¡Buenos días!"
    override val goodAfternoon = "¡Buenas tardes!"
    override val goodEvening = "¡Buenas noches!"
    override val helloNightOwl = "¡Hola, trasnochador!"
    override val morningSubtitle = "Empieza el día con buen ritmo."
    override val afternoonSubtitle = "Acompaña tu tarde con música."
    override val eveningSubtitle = "Relájate y disfruta la noche."
    override val nightSubtitle = "La música nunca duerme."
    override val yourPersonalAnthem = "Tu himno personal"
    override val mostPlayed = "Lo más escuchado"
    override val musicInNumbers = "Tu música en números"
    override val totalTime = "Tiempo total"
    override val artistsInCollection = "Artistas en tu colección"
    override val songs = "Canciones"
    override val quotes = listOf(
        "¡Que la música te acompañe hoy!",
        "Un buen track para despejar la mente.",
        "Código limpio, volumen al máximo.",
        "Tómate un respiro, te lo ganaste.",
        "Listo para romperla hoy.",
        "La música es la banda sonora de tu vida.",
        "Siente el ritmo, vive el momento.",
        "Tu biblioteca suena increíble hoy."
    )
    override val analyzingLibrary = "Analizando biblioteca..."
    override val analysisCompleted = "Análisis completado"
    override val close = "Cerrar"
    override val syncLibrary = "Sincronizando Biblioteca"
    override val searchingNewFiles = "Buscando nuevos archivos..."
    override val syncFinished = "Sincronización finalizada"
    override val accept = "Aceptar"
    override val configuration = "Configuración"
    override val equalizerPro = "Ecualizador Pro"
    override val effectsActive = "Efectos activos"
    override val effectsInactive = "Efectos desactivados"
    override val reset = "Restablecer"
    override val frequencyAdjustments = "Ajustes de Frecuencia"
    override val superBass = "SuperBass"
    override val year = "Año"
    override val genre = "Género"
    override val track = "Pista"
    override val searchingAudio = "Buscando archivos de audio..."
    override val analyzingMetadata = "Analizando letras y metadatos..."
    override val processing = "Procesando"
    override val ready = "¡Todo listo!"
    override val systemFont = "Sistema"
    override val libraryUpdated = "Biblioteca actualizada"
    override val unknownArtist = "Artista desconocido"
    override val sleepTimer = "Temporizador de Apagado"
    override val sleepTimerDesc = "Detener música automáticamente"
    override val minutes = "minutos"
    override val off = "Desactivado"
    override val setTimer = "Configurar temporizador"
    override val timerActive = "Temporizador activo"
    override val stopMusicIn = "Detener música en"
    override val cancelTimer = "Cancelar temporizador"
    override val visualCustomization = "Personalización Visual"
    override val roundnessLarge = "Redondeo Grande (Tarjetas)"
    override val roundnessMedium = "Redondeo Medio (Listas)"
    override val roundnessSmall = "Redondeo Pequeño (Botones)"
    override val accentColor = "Color de Acento"
    override val secondaryColor = "Color Secundario"
    override val tertiaryColor = "Color Terciario"
    override val playbackSpeed = "Velocidad de Reproducción"
    override val lyricsFontSize = "Tamaño de Letra (Letras)"
    override val blurIntensity = "Intensidad de Desenfoque"
    override val blurBackground = "Fondo con Desenfoque"
    override val blurBackgroundDesc = "Usa la carátula difuminada como fondo"
    override val keepScreenOn = "Mantener Pantalla Encendida"
    override val keepScreenOnDesc = "Evita que la pantalla se bloquee mientras se usa la app"
    override val crossfade = "Crossfade (Desvanecimiento)"
    override val crossfadeDesc = "Tiempo de transición entre canciones"
    override val changelog = "Registro de Cambios"
    override val versionHistory = "Historial de Versiones"
    override val playlists = "Listas de Reproducción"
    override val createPlaylist = "Crear Lista"
    override val folderBrowser = "Explorador de Carpetas"
    override val folders = "Carpetas"
    override val audioVisualizer = "Visualizador de Audio"
    override val audioVisualizerDesc = "Efectos visuales que reaccionan al ritmo"
    override val addToPlaylist = "Añadir a lista"
    override val editPlaylist = "Editar lista"
    override val deletePlaylist = "Eliminar lista"
    override val changeCover = "Cambiar portada"
    override val playlistCreated = "Lista creada con éxito"
    override val playlistDeleted = "Lista eliminada"
    override val songAdded = "Canción añadida a la lista"
    override val enterPlaylistName = "Nombre de la lista"
    override val noArtists = "No se encontraron artistas"
    override val noAlbums = "No se encontraron álbumes"
    override val noPlaylists = "No tienes listas de reproducción"
    override val noFolders = "No se encontraron carpetas"
    override val noFavorites = "Aún no tienes canciones favoritas"
    override val noVideos = "No se encontraron videos"
    override val back = "Volver"
    override val bitrate = "Bitrate"
    override val sampleRate = "Muestreo"
    override val fullScreenLyricsControls = "Controles en Letras"
    override val fullScreenLyricsControlsDesc = "Muestra botones de reproducción en pantalla completa"
    override val showLyricsSlider = "Mostrar Barra de Progreso"
    override val showLyricsMainControls = "Mostrar Controles Principales"
    override val showLyricsExtraControls = "Mostrar Shuffle y Repetir"
    override val centerLyrics = "Centrar Letras"
    override val themeMode = "Modo de Tema"
    override val themeSystem = "Seguir Sistema"
    override val themeLight = "Modo Claro"
    override val themeDark = "Modo Oscuro"
    override val visualizerStyle = "Estilo del Visualizador"
    override val styleBars = "Barras Clásicas"
    override val styleCircle = "Círculo Radiante"
    override val styleWave = "Onda Suave"
    override val styleDots = "Puntos de Energía"
    override val viewMode = "Diseño de Biblioteca"
    override val viewGrid = "Cuadrícula"
    override val viewList = "Lista Detallada"
    override val videoAudioUrl = "URL de Video/Audio"
    override val downloadType = "Tipo de descarga"
    override val format = "Formato"
    override val quality = "Calidad"
    override val formatAndQuality = "Formato y Calidad"
    override val searchResults = "Resultados de búsqueda"
    override val original = "Original"
    override val searchOrUrl = "https://... o Nombre de la canción"
    override val recentDownloads = "Descargas Recientes"
    override val noActiveDownloads = "No hay descargas activas"
    override val downloading = "Descargando..."
    override val pending = "Pendiente"
    override val completed = "Completado"
    override val downloadError = "Error al descargar"
    override val recentlyAdded = "Escuchado recientemente"
    override val yourPersonalRhythm = "Tu ritmo personal"
    override val aboutApp = "Sobre la app"
    override val yourTopFiveSongs = "Las 5 canciones que más han sonado"
    override val activateToAdjust = "Activa el interruptor para ajustar el sonido"
    override val intensity = "intensidad"
    override val addToQueue = "Añadir a la cola"
    override val playNext = "Reproducir siguiente"
    override val addedToQueue = "Añadido a la cola"
    override val addedToPlayNext = "Se reproducirá a continuación"
    override val deleteSong = "Eliminar canción"
    override val deleteSongConfirm = "¿Estás seguro de que quieres eliminar esta canción permanentemente?"
    override val removeFromFavorites = "Quitar de favoritos"
    override val addToFavorites = "Añadir a favoritos"
    override val editLyrics = "Editar Letras"
    override val removeFromQueue = "Quitar de la cola"
    override val clearQueue = "Borrar cola"
    override val clearQueueConfirm = "¿Estás seguro de que quieres borrar toda la cola de reproducción?"
    override val cancel = "Cancelar"
    override val save = "Guardar"
    override val permissionsRequired = "Permisos requeridos"
    override val shuffleAll = "Reproducir Aleatorio"
    override val noResultsFound = "No se encontraron resultados"
    override val searchError = "Error al buscar"
    override val searchingSpotify = "Obteniendo datos de Spotify..."
    override val shareStory = "Compartir Story"
    override val shareStoryDesc = "Crea una imagen para tus redes sociales"
    override val generatingImage = "Generando imagen..."
    override val autoLyrics = "Búsqueda Automática de Letras"
    override val autoLyricsDesc = "Buscar automáticamente letras en LRCLIB si no se encuentran localmente"
    override val editTags = "Editar Etiquetas"
    override val title = "Título"
    override val artist = "Artista"
    override val album = "Álbum"
    override val trackNumber = "Número de Pista"
    override val tagsUpdated = "Etiquetas actualizadas"
    override val errorUpdatingTags = "Error al actualizar etiquetas"
    override val resizeFit = "Ajustar"
    override val resizeZoom = "Zoom"
    override val resizeFill = "Rellenar"
    override val brightness = "Brillo"
    override val volume = "Volumen"
    override val noArt = "Sin Carátula"
    override val technicalInfo = "Información Técnica"
    override val noLyricsFound = "No se encontraron letras"
    override val paletteStyle = "Estilo de Paleta"
    override val paletteStyleDesc = "Efecto de color basado en la carátula"
    override val oledBackground = "Fondo OLED"
    override val oledBackgroundDesc = "Usa negro puro en modo oscuro para ahorrar batería"
    override val paletteLavender = "Lavanda Glacial"
    override val paletteForest = "Bosque de Niebla"
    override val paletteDune = "Duna de Arena"
    override val paletteReef = "Arrecife Calmo"
    override val palettePetal = "Pétalo"
    override val paletteSlate = "Pizarra"
    override val unifiedLyricsBackground = "Fondo de Letras Unificado"
    override val unifiedLyricsBackgroundDesc = "Aplica el fondo animado a toda la pantalla del reproductor"
    override val colorStyle = "Estilo de Color"
    override val colorSource = "Origen del Color"
    override val wallpaperColors = "Fondo de Pantalla"
    override val albumArtColors = "Carátula del Álbum"
    override val presetColors = "Presets de Color"
    override val browseByGenre = "Explorar por Género"
    override val search = "Buscar"
    override val updateEngine = "Actualizar motor"
    override val exploreMusic = "Explorar música"
    override val selectFavoritePlatform = "Selecciona tu plataforma favorita"
    override val capture = "Capturar"
    override val lossless = "Sin pérdida"
    override val play = "Reproducir"
    override val share = "Compartir"
    override val info = "Información"
    override val playlist = "Lista"
    override val song = "Canción"
    override val dynamicsProcessing = "Procesamiento Dinámico"
    override val gain = "Ganancia"
    override val limit = "Límite"
}

object EnglishStrings : AppStrings {
    override val welcomeTitle = "Welcome to Shark!"
    override val welcomeSubtitle = "Configure your experience before starting"
    override val selectLanguage = "Select Language"
    override val customizeTheme = "Customize your Theme"
    override val dynamicColor = "Dynamic Colors"
    override val dynamicColorDesc = "Adapt the interface to your wallpaper"
    override val artDynamicColor = "Dynamic Album Art Color"
    override val artDynamicColorDesc = "Extract colors from the current album art"
    override val artDynamicColorOnlyPlayer = "Only in Player"
    override val artDynamicColorOnlyPlayerDesc = "Apply art colors only to the playback screen"
    override val karaokeMode = "Karaoke Mode"
    override val karaokeModeDesc = "Show synchronized lyrics (.lrc)"
    override val scanLibrary = "Library Scan"
    override val scanLibraryDesc = "Searching for music on your device..."
    override val startScan = "Start Scan"
    override val finish = "Finish"
    override val next = "Next"
    override val previous = "Previous"
    override val settings = "Settings"
    override val language = "Language"
    override val downloadMusic = "Download Music"
    override val downloadMusicDesc = "Download your favorite songs from the web"
    override val syncMusic = "Sync Music"
    override val syncMusicDesc = "Scan for new local files"
    override val lyricsAnalyzer = "Lyrics Analyzer"
    override val lyricsAnalyzerDesc = "Search for local lyrics (.lrc, .txt)"
    override val equalizer = "Equalizer"
    override val equalizerDesc = "Frequency and bass adjustments"
    override val logcatMonitor = "Logcat Monitor"
    override val logcatMonitorDesc = "View internal system logs"
    override val selectFont = "Select Font"
    override val searchSongs = "Search songs..."
    override val noSongs = "No songs found"
    override val songsFound = "found"
    override val home = "Home"
    override val music = "Music"
    override val downloads = "Downloads"
    override val all = "All"
    override val artists = "Artists"
    override val albums = "Albums"
    override val favorites = "Favorites"
    override val videos = "Videos"
    override val appearance = "Appearance"
    override val appearanceDesc = "Themes, colors, and visual customization"
    override val playback = "Playback"
    override val playbackDesc = "Speed, crossfade, and controls"
    override val behavior = "Behavior"
    override val behaviorDesc = "App interaction and gestures"
    override val musicManagement = "Music Management"
    override val musicManagementDesc = "Library, scanning, and synchronization"
    override val aiIntegration = "AI Integration"
    override val aiIntegrationDesc = "Lyrics analysis and suggestions"
    override val backupRestore = "Backup & Restore"
    override val backupRestoreDesc = "Backup and restore your data"
    override val developerOptions = "Developer Options"
    override val developerOptionsDesc = "Debugging tools and logs"
    override val sound = "Sound"
    override val library = "Library"
    override val advanced = "Tools"
    override val sharkPlayer = "Shark Player"
    override val start = "Start"
    override val goodMorning = "Good morning!"
    override val goodAfternoon = "Good afternoon!"
    override val goodEvening = "Good evening!"
    override val helloNightOwl = "Hello, night owl!"
    override val morningSubtitle = "Start the day with a good rhythm."
    override val afternoonSubtitle = "Accompany your afternoon with music."
    override val eveningSubtitle = "Relax and enjoy the night."
    override val nightSubtitle = "The music never sleeps."
    override val yourPersonalAnthem = "Your personal anthem"
    override val mostPlayed = "Most played"
    override val musicInNumbers = "Your music in numbers"
    override val totalTime = "Total time"
    override val artistsInCollection = "Artists in your collection"
    override val songs = "Songs"
    override val quotes = listOf(
        "May music accompany you today!",
        "A good track to clear your mind.",
        "Clean code, volume to the max.",
        "Take a break, you earned it.",
        "Ready to crush it today.",
        "Music is the soundtrack of your life.",
        "Feel the rhythm, live the moment.",
        "Your library sounds amazing today."
    )
    override val analyzingLibrary = "Analyzing library..."
    override val analysisCompleted = "Analysis completed"
    override val close = "Close"
    override val syncLibrary = "Syncing Library"
    override val searchingNewFiles = "Searching for new files..."
    override val syncFinished = "Sync finished"
    override val accept = "Accept"
    override val configuration = "Configuration"
    override val equalizerPro = "Equalizer Pro"
    override val effectsActive = "Effects active"
    override val effectsInactive = "Effects inactive"
    override val reset = "Reset"
    override val frequencyAdjustments = "Frequency Adjustments"
    override val superBass = "Super Bass"
    override val year = "Year"
    override val genre = "Genre"
    override val track = "Track"
    override val searchingAudio = "Searching for audio files..."
    override val analyzingMetadata = "Analyzing lyrics and metadata..."
    override val processing = "Processing"
    override val ready = "All set!"
    override val systemFont = "System"
    override val libraryUpdated = "Library updated"
    override val unknownArtist = "Unknown artist"
    override val sleepTimer = "Sleep Timer"
    override val sleepTimerDesc = "Stop music automatically"
    override val minutes = "minutes"
    override val off = "Off"
    override val setTimer = "Set timer"
    override val timerActive = "Timer active"
    override val stopMusicIn = "Stop music in"
    override val cancelTimer = "Cancel timer"
    override val visualCustomization = "Visual Customization"
    override val roundnessLarge = "Large Roundness (Cards)"
    override val roundnessMedium = "Medium Roundness (Lists)"
    override val roundnessSmall = "Small Roundness (Buttons)"
    override val accentColor = "Accent Color"
    override val secondaryColor = "Secondary Color"
    override val tertiaryColor = "Tertiary Color"
    override val playbackSpeed = "Playback Speed"
    override val lyricsFontSize = "Lyrics Font Size"
    override val blurIntensity = "Blur Intensity"
    override val blurBackground = "Blurred Background"
    override val blurBackgroundDesc = "Use blurred album art as background"
    override val keepScreenOn = "Keep Screen On"
    override val keepScreenOnDesc = "Prevent screen from locking while using the app"
    override val crossfade = "Crossfade"
    override val crossfadeDesc = "Transition time between songs"
    override val changelog = "Changelog"
    override val versionHistory = "Version History"
    override val playlists = "Playlists"
    override val createPlaylist = "Create Playlist"
    override val folderBrowser = "Folder Browser"
    override val folders = "Folders"
    override val audioVisualizer = "Audio Visualizer"
    override val audioVisualizerDesc = "Visual effects that react to the rhythm"
    override val addToPlaylist = "Add to playlist"
    override val editPlaylist = "Edit playlist"
    override val deletePlaylist = "Delete playlist"
    override val changeCover = "Change cover"
    override val playlistCreated = "Playlist created successfully"
    override val playlistDeleted = "Playlist deleted"
    override val songAdded = "Song added to playlist"
    override val enterPlaylistName = "Playlist name"
    override val noArtists = "No artists found"
    override val noAlbums = "No albums found"
    override val noPlaylists = "You don't have any playlists"
    override val noFolders = "No folders found"
    override val noFavorites = "You don't have any favorite songs yet"
    override val noVideos = "No videos found"
    override val back = "Back"
    override val bitrate = "Bitrate"
    override val sampleRate = "Sample Rate"
    override val fullScreenLyricsControls = "Lyrics Controls"
    override val fullScreenLyricsControlsDesc = "Show playback buttons in full screen lyrics"
    override val showLyricsSlider = "Show Progress Bar"
    override val showLyricsMainControls = "Show Main Controls"
    override val showLyricsExtraControls = "Show Shuffle & Repeat"
    override val centerLyrics = "Center Lyrics"
    override val themeMode = "Theme Mode"
    override val themeSystem = "System Default"
    override val themeLight = "Light Mode"
    override val themeDark = "Dark Mode"
    override val visualizerStyle = "Visualizer Style"
    override val styleBars = "Classic Bars"
    override val styleCircle = "Radiant Circle"
    override val styleWave = "Smooth Wave"
    override val styleDots = "Energy Dots"
    override val viewMode = "Library Layout"
    override val viewGrid = "Grid View"
    override val viewList = "Detailed List"
    override val videoAudioUrl = "Video/Audio URL"
    override val downloadType = "Download Type"
    override val format = "Format"
    override val quality = "Quality"
    override val formatAndQuality = "Format and Quality"
    override val searchResults = "Search Results"
    override val original = "Original"
    override val searchOrUrl = "https://... or Song Name"
    override val recentDownloads = "Recent Downloads"
    override val noActiveDownloads = "No active downloads"
    override val downloading = "Downloading..."
    override val pending = "Pending"
    override val completed = "Completed"
    override val downloadError = "Download error"
    override val recentlyAdded = "Recently Heard"
    override val yourPersonalRhythm = "Your personal rhythm"
    override val aboutApp = "About the app"
    override val yourTopFiveSongs = "The 5 songs you've played the most"
    override val activateToAdjust = "Activate the switch to adjust the sound"
    override val intensity = "intensity"
    override val addToQueue = "Add to queue"
    override val playNext = "Play next"
    override val addedToQueue = "Added to queue"
    override val addedToPlayNext = "Will play next"
    override val deleteSong = "Delete song"
    override val deleteSongConfirm = "Are you sure you want to delete this song permanently?"
    override val removeFromFavorites = "Remove from favorites"
    override val addToFavorites = "Add to favorites"
    override val editLyrics = "Edit Lyrics"
    override val removeFromQueue = "Remove from queue"
    override val clearQueue = "Clear queue"
    override val clearQueueConfirm = "Are you sure you want to clear the entire playback queue?"
    override val cancel = "Cancel"
    override val save = "Save"
    override val permissionsRequired = "Permissions required"
    override val shuffleAll = "Shuffle All"
    override val noResultsFound = "No results found"
    override val searchError = "Search error"
    override val searchingSpotify = "Fetching Spotify metadata..."
    override val shareStory = "Share Story"
    override val shareStoryDesc = "Create an image for your social media"
    override val generatingImage = "Generating image..."
    override val autoLyrics = "Automatic Lyrics Search"
    override val autoLyricsDesc = "Automatically search for lyrics on LRCLIB if not found locally"
    override val editTags = "Edit Tags"
    override val title = "Title"
    override val artist = "Artist"
    override val album = "Album"
    override val trackNumber = "Track Number"
    override val tagsUpdated = "Tags updated"
    override val errorUpdatingTags = "Error updating tags"
    override val resizeFit = "Fit"
    override val resizeZoom = "Zoom"
    override val resizeFill = "Fill"
    override val brightness = "Brightness"
    override val volume = "Volume"
    override val noArt = "No Artwork"
    override val technicalInfo = "Technical Information"
    override val noLyricsFound = "No lyrics found"
    override val paletteStyle = "Palette Style"
    override val paletteStyleDesc = "Color effect based on artwork"
    override val oledBackground = "OLED Background"
    override val oledBackgroundDesc = "Use pure black in dark mode to save battery"
    override val paletteLavender = "Glacial Lavender"
    override val paletteForest = "Mist Forest"
    override val paletteDune = "Sand Dune"
    override val paletteReef = "Calm Reef"
    override val palettePetal = "Petal"
    override val paletteSlate = "Slate"
    override val unifiedLyricsBackground = "Unified Lyrics Background"
    override val unifiedLyricsBackgroundDesc = "Apply animated background to the entire player screen"
    override val colorStyle = "Color Style"
    override val colorSource = "Color Source"
    override val wallpaperColors = "Wallpaper Colors"
    override val albumArtColors = "Album Art Colors"
    override val presetColors = "Color Presets"
    override val browseByGenre = "Browse by Genre"
    override val search = "Search"
    override val updateEngine = "Update Engine"
    override val exploreMusic = "Explore Music"
    override val selectFavoritePlatform = "Select your favorite platform"
    override val capture = "Capture"
    override val lossless = "Lossless"
    override val play = "Play"
    override val share = "Share"
    override val info = "Info"
    override val playlist = "Playlist"
    override val song = "Song"
    override val dynamicsProcessing = "Dynamics Processing"
    override val gain = "Gain"
    override val limit = "Limit"
}
