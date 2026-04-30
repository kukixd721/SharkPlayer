package com.example.mp3

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.Futures
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class PlaybackService : MediaLibraryService() {

    private var mediaLibrarySession: MediaLibrarySession? = null
    private var exoPlayer: ExoPlayer? = null
    private var audioEq: AudioEffect? = null
    private var audioBass: BassBoost? = null
    private var audioVirtualizer: Virtualizer? = null
    private var audioLoudness: LoudnessEnhancer? = null
    
    private var currentAudioSessionId: Int = C.AUDIO_SESSION_ID_UNSET

    private var crossfadeEnabled: Boolean = false
    private var crossfadeDurationMs: Long = 2000

    private val handler = Handler(Looper.getMainLooper())
    private val statsThread = HandlerThread("StatsThread").apply { start() }
    private val statsBackgroundHandler = Handler(statsThread.looper)

    private var startTime: Long = 0
    private var currentSongId: String? = null
    private var currentSongTitle: String = "Unknown"
    private var currentSongArtist: String = "Unknown"
    private var currentSongAlbum: String = "Unknown"
    private var currentSongGenre: String = "Unknown"
    private var currentSong: Song? = null

    private val checkPositionRunnable = object : Runnable {
        private var lastStatsUpdate = 0L
        override fun run() {
            checkCrossfade()
            
            // Actualizar estadísticas cada 20 segundos de reproducción continua
            val now = System.currentTimeMillis()
            if (now - lastStatsUpdate > 20000) {
                updateStats()
                lastStatsUpdate = now
            }
            
            handler.postDelayed(this, 500)
        }
    }

    private fun updateStats(force: Boolean = false) {
        val songId = currentSongId ?: return
        val player = exoPlayer ?: return
        
        // Registramos si está reproduciendo O si es una llamada forzada (ej. al pausar o cambiar de canción)
        if (player.isPlaying || force) {
            val now = System.currentTimeMillis()
            if (startTime > 0) {
                val duration = now - startTime
                if (duration > 500) { // Guardar incluso fragmentos cortos (> 0.5s)
                    val title = currentSongTitle
                    val artist = currentSongArtist
                    val album = currentSongAlbum
                    val genre = currentSongGenre
                    
                    statsBackgroundHandler.post {
                        StatisticsManager.trackPlay(this, songId, title, artist, album, genre, duration, isNewPlay = false)
                    }
                }
            }
            startTime = if (player.isPlaying) now else 0
        }
    }

    override fun onCreate() {
        super.onCreate()
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        mediaLibrarySession = MediaLibrarySession.Builder(this, exoPlayer!!, object : MediaLibrarySession.Callback {
            override fun onGetLibraryRoot(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<MediaItem>> {
                val rootItem = MediaItem.Builder()
                    .setMediaId("root")
                    .build()
                return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
            }

            override fun onGetChildren(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                parentId: String,
                page: Int,
                pageSize: Int,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
                return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.of(), params))
            }
        }).build()

        exoPlayer?.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (currentAudioSessionId != audioSessionId) {
                    currentAudioSessionId = audioSessionId
                    setupAudioEffects(audioSessionId)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    startTime = System.currentTimeMillis()
                    handler.post(checkPositionRunnable)
                } else {
                    updateStats(force = true)
                    handler.removeCallbacks(checkPositionRunnable)
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateStats(force = true)
                
                currentSongId = mediaItem?.mediaId
                if (mediaItem != null) {
                    val metadata = mediaItem.mediaMetadata
                    currentSongTitle = metadata.title?.toString() ?: "Unknown"
                    currentSongArtist = metadata.artist?.toString() ?: "Unknown"
                    currentSongAlbum = metadata.albumTitle?.toString() ?: "Unknown"
                    currentSongGenre = metadata.genre?.toString() ?: "Unknown"

                    statsBackgroundHandler.post {
                        StatisticsManager.trackPlay(this@PlaybackService, currentSongId!!, currentSongTitle, currentSongArtist, currentSongAlbum, currentSongGenre, 0L, isNewPlay = true)
                    }

                    val scope = CoroutineScope(Dispatchers.IO)
                    scope.launch {
                        val allSongs = getAudioFiles(this@PlaybackService) { }
                        currentSong = allSongs.find { it.id.toString() == mediaItem.mediaId }
                    }
                } else {
                    currentSongId = null
                    currentSongTitle = "Unknown"
                    currentSongArtist = "Unknown"
                    currentSongAlbum = "Unknown"
                    currentSongGenre = "Unknown"
                    currentSong = null
                }
                startTime = System.currentTimeMillis()
            }
        })
        
        StatisticsManager.startSession(this)
        updateCrossfadeSettings()
    }

    private var effectFailureCount = 0
    private var safeModeActive = false

    private fun setupAudioEffects(sessionId: Int) {
        if (safeModeActive || sessionId == C.AUDIO_SESSION_ID_UNSET) return
        
        handler.post {
            try {
                doSetupAudioEffects(sessionId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun doSetupAudioEffects(sessionId: Int) {
        releaseEffects()
        try {
            val dpConfig = DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                2, // Canales
                true, 10, // Pre-EQ
                false, 0, // Multi-band
                false, 0, // Post-EQ
                true // Limiter
            ).build()
            
            audioEq = DynamicsProcessing(0, sessionId, dpConfig)
            audioBass = BassBoost(0, sessionId)
            audioVirtualizer = Virtualizer(0, sessionId)
            audioLoudness = LoudnessEnhancer(sessionId)

            applyEffectParameters()
        } catch (e: Exception) {
            e.printStackTrace()
            effectFailureCount++
            if (effectFailureCount > 3) {
                safeModeActive = true
            }
            releaseEffects()
        }
    }

    private fun applyEffectParameters() {
        val audioPrefs = getSharedPreferences("audio_settings", MODE_PRIVATE)
        val isEnabled = audioPrefs.getBoolean("eq_enabled", false)

        audioEq?.let { eq ->
            if (eq is DynamicsProcessing) {
                val frequencies = floatArrayOf(31f, 62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)
                val masterGain = audioPrefs.getFloat("master_gain", 1.0f)
                val balance = audioPrefs.getFloat("audio_balance", 0f)
                val limiterEnabled = audioPrefs.getBoolean("limiter_enabled", true)

                val leftGain = if (balance > 0) 1f - balance else 1f
                val rightGain = if (balance < 0) 1f + balance else 1f
                
                val masterGainDb = if (masterGain > 0) 20f * kotlin.math.log10(masterGain.toDouble()).toFloat() else -100f
                eq.setInputGainAllChannelsTo(masterGainDb)

                for (ch in 0 until 2) {
                    val channelGain = if (ch == 0) leftGain else rightGain
                    val channelBalanceDb = if (channelGain > 0) 20f * kotlin.math.log10(channelGain.toDouble()).toFloat() else -100f

                    val preEq = eq.getPreEqByChannelIndex(ch)
                    preEq.isEnabled = true
                    for (i in 0 until 10) {
                        val level = audioPrefs.getFloat("eq_band_$i", 0f)
                        preEq.setBand(i, DynamicsProcessing.EqBand(true, frequencies[i], level + channelBalanceDb))
                    }
                    eq.setPreEqByChannelIndex(ch, preEq)

                    val limiter = eq.getLimiterByChannelIndex(ch)
                    limiter.isEnabled = limiterEnabled
                    limiter.threshold = -0.1f
                    limiter.ratio = 12f
                    limiter.attackTime = 1f
                    limiter.releaseTime = 60f
                    eq.setLimiterByChannelIndex(ch, limiter)
                }
                eq.enabled = isEnabled
            } else if (eq is Equalizer) {
                val bands = eq.numberOfBands.toInt()
                for (i in 0 until 10) {
                    if (i < bands) {
                        val level = audioPrefs.getFloat("eq_band_$i", 0f)
                        eq.setBandLevel(i.toShort(), (level * 100).toInt().toShort())
                    }
                }
                eq.enabled = isEnabled
            }
        }

        audioBass?.let {
            val effectEnabled = isEnabled && audioPrefs.getBoolean("bass_strength_enabled", true)
            it.enabled = effectEnabled
            if (effectEnabled && it.strengthSupported) {
                it.setStrength(audioPrefs.getInt("bass_strength", 0).toShort())
            }
        }

        audioVirtualizer?.let {
            val effectEnabled = isEnabled && audioPrefs.getBoolean("virtualizer_strength_enabled", true)
            it.enabled = effectEnabled
            if (effectEnabled && it.strengthSupported) {
                it.setStrength(audioPrefs.getInt("virtualizer_strength", 0).toShort())
            }
        }

        audioLoudness?.let {
            val effectEnabled = isEnabled && audioPrefs.getBoolean("loudness_gain_enabled", true)
            it.enabled = effectEnabled
            if (effectEnabled) {
                it.setTargetGain(audioPrefs.getInt("loudness_gain", 0))
            }
        }
    }

    private fun releaseEffects() {
        audioEq?.release()
        audioEq = null
        audioBass?.release()
        audioBass = null
        audioVirtualizer?.release()
        audioVirtualizer = null
        audioLoudness?.release()
        audioLoudness = null
    }

    private fun updateCrossfadeSettings() {
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        crossfadeEnabled = prefs.getBoolean("crossfade_enabled", false)
        crossfadeDurationMs = prefs.getLong("crossfade_duration", 2000)
    }

    private fun checkCrossfade() {
        val player = exoPlayer ?: return
        if (!crossfadeEnabled || player.mediaItemCount <= 1) {
            if (player.volume != 1f) player.volume = 1f
            return
        }

        val duration = player.duration
        val position = player.currentPosition

        if (duration <= 0) return

        val remaining = duration - position

        when {
            position < crossfadeDurationMs -> {
                val volume = position.toFloat() / crossfadeDurationMs
                player.volume = volume.coerceIn(0f, 1f)
            }
            remaining < crossfadeDurationMs -> {
                val volume = remaining.toFloat() / crossfadeDurationMs
                player.volume = volume.coerceIn(0f, 1f)
            }
            else -> {
                if (player.volume != 1f) player.volume = 1f
            }
        }
    }

    @OptIn(UnstableApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "UPDATE_EQ") {
            if (audioEq != null) {
                applyEffectParameters()
            } else {
                exoPlayer?.audioSessionId?.let { setupAudioEffects(it) }
            }
        }
        updateCrossfadeSettings()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaLibrarySession

    override fun onDestroy() {
        updateStats(force = true)
        statsBackgroundHandler.post {
            StatisticsManager.endSession(this)
            statsThread.quitSafely()
        }
        handler.removeCallbacks(checkPositionRunnable)
        releaseEffects()
        exoPlayer?.release()
        mediaLibrarySession?.release()
        super.onDestroy()
    }
}
