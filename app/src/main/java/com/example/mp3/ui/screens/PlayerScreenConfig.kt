package com.example.mp3.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.TextUnit
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import android.media.audiofx.BassBoost
import androidx.compose.material3.Typography
import com.example.mp3.Language
import com.example.mp3.LyricLine
import com.example.mp3.Song
import com.example.mp3.PaletteStyle

data class PlayerSettings(
    val useDynamicColor: Boolean,
    val onDynamicColorChange: (Boolean) -> Unit,
    val roundnessLarge: Int,
    val onRoundnessLargeChange: (Int) -> Unit,
    val roundnessMedium: Int,
    val onRoundnessMediumChange: (Int) -> Unit,
    val roundnessSmall: Int,
    val onRoundnessSmallChange: (Int) -> Unit,
    val customAccentColor: Color,
    val onCustomAccentColorChange: (Color) -> Unit,
    val customSecondaryColor: Color,
    val onCustomSecondaryColorChange: (Color) -> Unit,
    val customTertiaryColor: Color,
    val onCustomTertiaryColorChange: (Color) -> Unit,
    val playbackSpeed: Float,
    val onPlaybackSpeedChange: (Float) -> Unit,
    val lyricsFontSize: Int,
    val onLyricsFontSizeChange: (Int) -> Unit,
    val keepScreenOn: Boolean,
    val onKeepScreenOnChange: (Boolean) -> Unit,
    val crossfadeDuration: Float,
    val onCrossfadeChange: (Float) -> Unit,
    val crossfadeEnabled: Boolean,
    val onCrossfadeEnabledChange: (Boolean) -> Unit,
    val karaokeEnabled: Boolean,
    val onKaraokeChange: (Boolean) -> Unit,
    val selectedFontName: String,
    val onFontChange: (String) -> Unit,
    val selectedLanguage: Language,
    val onLanguageChange: (Language) -> Unit,
    val visualizerEnabled: Boolean,
    val onVisualizerEnabledChange: (Boolean) -> Unit,
    val showLyricsControls: Boolean,
    val onShowLyricsControlsChange: (Boolean) -> Unit,
    val showLyricsSlider: Boolean,
    val onShowLyricsSliderChange: (Boolean) -> Unit,
    val showLyricsMainControls: Boolean,
    val onShowLyricsMainControlsChange: (Boolean) -> Unit,
    val showLyricsExtraControls: Boolean,
    val onShowLyricsExtraControlsChange: (Boolean) -> Unit,
    val centerLyrics: Boolean,
    val onCenterLyricsChange: (Boolean) -> Unit,
    val sleepTimerRemaining: Long,
    val onSetSleepTimer: (Long) -> Unit,
    val onShowLogcat: () -> Unit,
    val getTypography: (String) -> Typography,
    val getAlbumArt: (String) -> ByteArray?,
    val formatTime: (Long) -> String,
    val rememberPlayerPosition: @Composable (MediaController?) -> Long,
    val themeMode: Int,
    val onThemeModeChange: (Int) -> Unit,
    val visualizerStyle: Int,
    val onVisualizerStyleChange: (Int) -> Unit,
    val useGridViewHome: Boolean,
    val onUseGridViewHomeChange: (Boolean) -> Unit,
    val useGridViewLibrary: Boolean,
    val onUseGridViewLibraryChange: (Boolean) -> Unit,
    val useArtDynamicColor: Boolean,
    val onUseArtDynamicColorChange: (Boolean) -> Unit,
    val paletteStyle: PaletteStyle,
    val onPaletteStyleChange: (PaletteStyle) -> Unit,
    val artPrimaryColor: Color,
    val artSecondaryColor: Color,
    val artTertiaryColor: Color,
    val autoLyrics: Boolean,
    val onAutoLyricsChange: (Boolean) -> Unit,
    val useOledMode: Boolean,
    val onUseOledModeChange: (Boolean) -> Unit,
    val selectedCuratedPalette: Int,
    val onSelectedCuratedPaletteChange: (Int) -> Unit,
    val unifiedLyricsBackground: Boolean,
    val onUnifiedLyricsBackgroundChange: (Boolean) -> Unit
)

data class MusicListConfig(
    val activeMediaItem: MediaItem?,
    val favoriteIds: Set<Long>,
    val onToggleFavorite: (Long) -> Unit,
    val onAddToPlaylist: (Song) -> Unit = {},
    val onAddToQueue: (Song) -> Unit = {},
    val onPlayNext: (Song) -> Unit = {},
    val onRemoveFromQueue: (Int) -> Unit = {},
    val onMoveQueueItem: (Int, Int) -> Unit = { _, _ -> },
    val isDraggable: Boolean = false,
    val onDeleteSong: (Song) -> Unit = {},
    val onUpdateTags: (Song, String, String, String, String, String, String, ByteArray?) -> Unit = { _, _, _, _, _, _, _, _ -> },
    val onPlayAll: (List<Song>, Int) -> Unit = { _, _ -> },
    val settings: PlayerSettings,
    val emptyMessage: String? = null
)

data class PlayerComponents(
    val KaraokeView: @Composable (
        List<LyricLine>,
        Long,
        Int,
        Boolean,
        Boolean,
        Set<Int>,
        Boolean,
        (Int) -> Unit,
        ((Long) -> Unit)?
    ) -> Unit,
    val SubMusicList: @Composable (
        artistOrAlbum: String,
        songs: List<Song>,
        player: Player,
        isPlaying: Boolean,
        config: MusicListConfig,
        onBack: () -> Unit,
        customCover: String?,
        onEditClick: (() -> Unit)?
    ) -> Unit,
    val ArtistList: @Composable (artists: Map<String, List<Song>>, currentMediaItem: MediaItem?, onArtistClick: (List<Song>) -> Unit, settings: PlayerSettings, searchQuery: String) -> Unit,
    val AlbumList: @Composable (albums: Map<String, List<Song>>, currentMediaItem: MediaItem?, onAlbumClick: (List<Song>) -> Unit, settings: PlayerSettings, searchQuery: String) -> Unit,
    val FolderList: @Composable (folders: Map<String, List<Song>>, onFolderClick: (List<Song>) -> Unit, onIgnoreFolder: (String) -> Unit, ignoredFolders: Set<String>, settings: PlayerSettings, searchQuery: String) -> Unit,
    val PlaylistList: @Composable (
        playlists: Set<String>,
        onPlaylistClick: (String) -> Unit,
        onCreatePlaylist: () -> Unit,
        onDeletePlaylist: (String) -> Unit,
        onRenamePlaylist: (String, String) -> Unit,
        onSetPlaylistImage: (String, String?) -> Unit,
        allSongs: List<Song>,
        settings: PlayerSettings
    ) -> Unit,
    val MusicList: @Composable (songs: List<Song>, player: Player, isPlaying: Boolean, config: MusicListConfig) -> Unit,
    val MetadataRow: @Composable (ImageVector, String, String) -> Unit,
    val EcualizadorPanel: @Composable () -> Unit,
    val MorphingPlayPauseButton: @Composable (
        isPlaying: Boolean,
        onClick: () -> Unit,
        modifier: androidx.compose.ui.Modifier,
        mainColor: androidx.compose.ui.graphics.Color,
        iconColor: androidx.compose.ui.graphics.Color,
        iconSize: androidx.compose.ui.unit.Dp,
        borderWidth: androidx.compose.ui.unit.Dp
    ) -> Unit
)
