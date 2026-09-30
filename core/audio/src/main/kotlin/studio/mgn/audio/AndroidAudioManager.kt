package studio.mgn.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager as SystemAudioManager
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val CROSSFADE_MS = 1500L
private const val FADE_STEPS = 15

/**
 * Real implementation: Media3 ExoPlayer for looping music (1.5s crossfade),
 * SoundPool for effects, framework Vibrator for haptics. Every asset load is
 * guarded — missing OGG files mean silence, never a crash.
 */
class AndroidAudioManager(
    context: Context,
    private var vibrationEnabled: Boolean = true,
) : AudioManager {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * The player is created on first use and remembered as unavailable if
     * construction fails (e.g. stripped test runtimes): music then stays
     * silent instead of crashing. Throwable (not just Exception) is caught
     * on purpose — linkage failures are Errors.
     */
    private var playerOrNull: ExoPlayer? = null
    private var playerFailed = false

    private fun player(): ExoPlayer? {
        playerOrNull?.let { return it }
        if (playerFailed) return null
        return try {
            ExoPlayer.Builder(appContext)
                .setMediaSourceFactory(
                    DefaultMediaSourceFactory(DefaultDataSource.Factory(appContext)),
                )
                .build()
                .also {
                    it.repeatMode = Player.REPEAT_MODE_ONE
                    playerOrNull = it
                }
        } catch (_: Throwable) {
            playerFailed = true
            null
        }
    }

    private val soundPool: SoundPool by lazy {
        SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .build()
    }

    private val soundIds = mutableMapOf<SoundKey, Int>()
    private var musicEnabled = true
    private var sfxEnabled = true
    private var musicVolume = 1f
    private var sfxVolume = 1f
    private var currentMusic: MusicKey? = null
    private var fadeJob: kotlinx.coroutines.Job? = null

    private val systemAudio =
        appContext.getSystemService(SystemAudioManager::class.java)

    private val focusListener =
        SystemAudioManager.OnAudioFocusChangeListener { change ->
            if (change == SystemAudioManager.AUDIOFOCUS_LOSS ||
                change == SystemAudioManager.AUDIOFOCUS_LOSS_TRANSIENT
            ) {
                scope.launch(Dispatchers.Main.immediate) {
                    try {
                        playerOrNull?.pause()
                    } catch (_: Throwable) {
                    }
                }
            }
        }

    private val focusRequest: AudioFocusRequest by lazy {
        AudioFocusRequest.Builder(SystemAudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setOnAudioFocusChangeListener(focusListener)
            .build()
    }

    fun setVibrationEnabled(enabled: Boolean) {
        vibrationEnabled = enabled
    }

    override fun play(key: SoundKey) {
        if (!sfxEnabled) return
        scope.launch(Dispatchers.IO) {
            try {
                val id = soundIds.getOrPut(key) {
                    val fd = appContext.assets.openFd("audio/sfx/${key.file}.ogg")
                    soundPool.load(fd, 1).also { fd.close() }
                }
                if (id != 0) soundPool.play(id, sfxVolume, sfxVolume, 1, 0, 1f)
            } catch (_: Throwable) {
                // Missing asset: stay silent.
            }
        }
    }

    @OptIn(UnstableApi::class)
    override fun setMusic(key: MusicKey?) {
        if (key == currentMusic) return
        fadeJob?.cancel()
        val previous = currentMusic
        currentMusic = key
        if (key == null || !musicEnabled) {
            fadeOutAndPause()
            return
        }
                scope.launch(Dispatchers.Main.immediate) {
                    try {
                        val focus = systemAudio.requestAudioFocus(focusRequest)
                        if (focus != SystemAudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                            return@launch
                        }
                        val exo = player() ?: return@launch
                        exo.setMediaItem(
                            MediaItem.fromUri("asset:///audio/music/${key.file}.ogg"),
                        )
                        exo.prepare()
                        if (previous == null) {
                            exo.volume = musicVolume
                            exo.play()
                        } else {
                            crossfade()
                        }
                    } catch (_: Throwable) {
                        // Missing asset: stay silent.
                    }
                }
    }

    private fun fadeOutAndPause() {
        fadeJob = scope.launch {
            val exo = player() ?: return@launch
            val start = exo.volume
            repeat(FADE_STEPS) { step ->
                exo.volume = start * (1 - (step + 1) / FADE_STEPS.toFloat())
                delay(CROSSFADE_MS / FADE_STEPS)
            }
            exo.pause()
            exo.volume = musicVolume
        }
    }

    private fun crossfade() {
        fadeJob = scope.launch {
            val exo = player() ?: return@launch
            exo.volume = 0f
            exo.play()
            repeat(FADE_STEPS) { step ->
                exo.volume = musicVolume * ((step + 1) / FADE_STEPS.toFloat())
                delay(CROSSFADE_MS / FADE_STEPS)
            }
            exo.volume = musicVolume
        }
    }

    override fun setMusicEnabled(enabled: Boolean) {
        musicEnabled = enabled
        if (!enabled) {
            fadeJob?.cancel()
            playerOrNull?.pause()
        } else if (currentMusic != null) {
            setMusic(currentMusic)
        }
    }

    override fun setSfxEnabled(enabled: Boolean) {
        sfxEnabled = enabled
    }

    override fun setMusicVolume(level: Float) {
        musicVolume = level.coerceIn(0f, 1f)
        if (fadeJob?.isActive != true) playerOrNull?.volume = musicVolume
    }

    override fun setSfxVolume(level: Float) {
        sfxVolume = level.coerceIn(0f, 1f)
    }

    override fun vibrate(strength: HapticStrength) {
        if (!vibrationEnabled) return
        try {
            val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = appContext.getSystemService(VibratorManager::class.java)
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Vibrator::class.java)
            }
            val (timings, amplitudes) = when (strength) {
                HapticStrength.LIGHT -> longArrayOf(0, 20) to intArrayOf(0, 80)
                HapticStrength.MEDIUM -> longArrayOf(0, 40) to intArrayOf(0, 160)
                HapticStrength.HEAVY -> longArrayOf(0, 60, 60, 60) to intArrayOf(0, 255, 0, 200)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(abs(timings.sum()))
            }
        } catch (_: Throwable) {
            // No vibrator: ignore.
        }
    }

    override fun release() {
        fadeJob?.cancel()
        try {
            systemAudio.abandonAudioFocusRequest(focusRequest)
        } catch (_: Throwable) {
        }
        try {
            playerOrNull?.release()
        } catch (_: Throwable) {
        }
        try {
            soundPool.release()
        } catch (_: Throwable) {
        }
    }
}
