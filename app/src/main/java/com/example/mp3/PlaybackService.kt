package com.example.mp3

import android.content.Intent
import android.util.Log
import android.media.audiofx.AudioEffect
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.media.audiofx.BassBoost
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

@OptIn(UnstableApi::class)
class PlaybackService : MediaLibraryService() {
    private var mediaLibrarySession: MediaLibrarySession? = null
    private var exoPlayer: ExoPlayer? = null
    private var audioEq: AudioEffect? = null
    private var audioBass: BassBoost? = null
    private var currentAudioSessionId = -1
    
    private var crossfadeEnabled = false
    private var crossfadeDurationMs = 0L

    private val handler = Handler(Looper.getMainLooper())
    private var startTime = 0L
    private var currentSongId: String? = null

    private val checkPositionRunnable = object : Runnable {
        override fun run() {
            checkCrossfade()
            updateStats()
            val nextDelay = if (isNearTransition()) 50L else 1000L
            handler.postDelayed(this, nextDelay)
        }
    }

    private fun isNearTransition(): Boolean {
        val player = exoPlayer ?: return false
        val remaining = player.duration - player.currentPosition
        return remaining in 1..4999
    }

    private fun updateStats() {
        val player = exoPlayer ?: return
        val now = System.currentTimeMillis()
        if (player.isPlaying && currentSongId != null) {
            val duration = (now - startTime) / 1000
            if (duration > 0) {
                StatisticsManager.addTimePlayed(this, currentSongId, duration)
            }
            startTime = now
        }
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        // CONFIGURACIÓN DE RENDERERS PARA MÁXIMA COMPATIBILIDAD
        val renderersFactory = DefaultRenderersFactory(this).apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        }

        val player = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setSkipSilenceEnabled(false)
            .setSeekParameters(androidx.media3.exoplayer.SeekParameters.CLOSEST_SYNC)
            .build()
        
        player.setWakeMode(C.WAKE_MODE_LOCAL)
        exoPlayer = player
        
        // El Listener se encargará de configurar los efectos cuando la sesión esté lista

        mediaLibrarySession = MediaLibrarySession.Builder(this, player, object : MediaLibrarySession.Callback {
            override fun onGetLibraryRoot(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<MediaItem>> {
                val rootItem = MediaItem.Builder()
                    .setMediaId("root")
                    .setMediaMetadata(MediaMetadata.Builder()
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .setTitle("Shark Player")
                        .build())
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

        setMediaNotificationProvider(DefaultMediaNotificationProvider.Builder(this).build())

        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Log.e("SharkPlayer", "Error crítico de playback: ${error.errorCodeName} (${error.errorCode})", error)
                
                when (error.errorCode) {
                    PlaybackException.ERROR_CODE_DECODING_FAILED,
                    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> {
                        Log.w("SharkPlayer", "Falla de decodificación. Intentando recuperación...")
                    }
                    PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED -> {
                        Log.e("SharkPlayer", "AudioTrack fallo. Posible conflicto de efectos. Activando Safe Mode.")
                        safeModeActive = true
                        releaseEffects()
                    }
                }
            }

            @OptIn(UnstableApi::class)
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                    setupAudioEffects(audioSessionId)
                }
            }

            @OptIn(UnstableApi::class)
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    setupAudioEffects(player.audioSessionId)
                    startTime = System.currentTimeMillis()
                    currentSongId = player.currentMediaItem?.mediaId
                    handler.post(checkPositionRunnable)
                } else {
                    updateStats()
                    handler.removeCallbacks(checkPositionRunnable)
                }
            }

            @OptIn(UnstableApi::class)
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateStats()
                currentSongId = mediaItem?.mediaId
                StatisticsManager.markAsRecent(this@PlaybackService, currentSongId)
                startTime = System.currentTimeMillis()
                
                exoPlayer?.volume = 1f
                exoPlayer?.audioSessionId?.let { setupAudioEffects(it) }
            }
        })
    }

    private var effectFailureCount = 0
    private var safeModeActive = false

    @OptIn(UnstableApi::class)
    private fun setupAudioEffects(sessionId: Int) {
        if (sessionId == C.AUDIO_SESSION_ID_UNSET || sessionId <= 0) return
        if (safeModeActive) return

        // Usamos un pequeño delay para asegurar que el AudioTrack esté listo
        handler.removeCallbacksAndMessages("setup_effects")
        handler.postAtTime({
            doSetupAudioEffects(sessionId)
        }, "setup_effects", SystemClock.uptimeMillis() + 400)
    }

    private fun doSetupAudioEffects(sessionId: Int) {
        if (safeModeActive) return
        
        val audioPrefs = getSharedPreferences("audio_settings", MODE_PRIVATE)
        val isEnabled = audioPrefs.getBoolean("eq_enabled", false)

        try {
            if (sessionId != currentAudioSessionId || audioEq == null || audioBass == null) {
                releaseEffects()
                currentAudioSessionId = sessionId
                
                Log.d("SharkAudio", "Iniciando efectos para sesión: $sessionId (Enabled: $isEnabled)")
                
                try {
                    // Prioridad 1 para que tenga precedencia sobre otros efectos globales si los hay
                    val config = DynamicsProcessing.Config.Builder(
                        DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                        2, true, 10, false, 0, false, 0, true
                    ).build()
                    audioEq = DynamicsProcessing(1, sessionId, config)
                } catch (e: Exception) {
                    Log.w("SharkAudio", "DynamicsProcessing no soportado o error de hardware, usando Equalizer standard")
                    try {
                        audioEq = Equalizer(1, sessionId)
                    } catch (e2: Exception) {
                        Log.e("SharkAudio", "Fallo al crear Equalizer standard: ${e2.message}")
                        throw e2
                    }
                }
                
                try {
                    audioBass = BassBoost(1, sessionId)
                } catch (e: Exception) {
                    Log.w("SharkAudio", "BassBoost no soportado o error: ${e.message}")
                }
            }

            applyEffectParameters()
            
            // Resetear contador de fallos si logramos inicializar correctamente
            effectFailureCount = 0
            
        } catch (e: Exception) {
            Log.e("SharkAudio", "Error crítico en configuración de audio: ${e.message}")
            effectFailureCount++
            
            if (effectFailureCount >= 5) {
                Log.e("SharkAudio", "Demasiados fallos consecutivos. Activando Safe Mode.")
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
                
                for (ch in 0 until 2) {
                    val preEq = eq.getPreEqByChannelIndex(ch)
                    preEq.isEnabled = true
                    for (i in 0 until 10) {
                        val level = audioPrefs.getFloat("eq_band_$i", 0f)
                        preEq.setBand(i, DynamicsProcessing.EqBand(true, frequencies[i], level))
                    }
                    eq.setPreEqByChannelIndex(ch, preEq)

                    val limiter = eq.getLimiterByChannelIndex(ch)
                    limiter.isEnabled = true
                    limiter.threshold = audioPrefs.getFloat("limiter_threshold", -0.1f)
                    limiter.ratio = 10f
                    limiter.attackTime = 2f
                    limiter.releaseTime = 50f
                    eq.setLimiterByChannelIndex(ch, limiter)
                }
                eq.setInputGainAllChannelsTo(audioPrefs.getFloat("output_gain", 0f))
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
            it.enabled = isEnabled
            if (isEnabled && it.strengthSupported) {
                it.setStrength(audioPrefs.getInt("bass_strength", 0).toShort())
            }
        }
    }

    private fun releaseEffects() {
        audioEq?.release()
        audioBass?.release()
        audioEq = null
        audioBass = null
    }

    private fun updateCrossfadeSettings() {
        val prefs = getSharedPreferences("app_config", MODE_PRIVATE)
        crossfadeEnabled = prefs.getBoolean("crossfade_enabled", false)
        crossfadeDurationMs = prefs.getInt("crossfade_duration", 5).toLong() * 1000
    }

    private fun checkCrossfade() {
        val player = exoPlayer ?: return
        if (!crossfadeEnabled || crossfadeDurationMs <= 0) {
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
            exoPlayer?.audioSessionId?.let { setupAudioEffects(it) }
        }
        updateCrossfadeSettings()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaLibrarySession

    override fun onDestroy() {
        handler.removeCallbacks(checkPositionRunnable)
        releaseEffects()
        exoPlayer?.release()
        mediaLibrarySession?.release()
        super.onDestroy()
    }
}
