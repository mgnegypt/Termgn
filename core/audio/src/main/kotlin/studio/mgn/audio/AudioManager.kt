package studio.mgn.audio

/** One-shot interface effects (SoundPool, OGG files in assets/audio/sfx). */
enum class SoundKey(val file: String) {
    BUTTON("click"),
    PANEL("panel"),
    ACHIEVEMENT("achievement"),
    REWARD("reward"),
    DANGER("danger"),
    BUILD("build"),
    TURN_END("turn_end"),
    ERROR("error"),
}

/** Looping music moods (Media3, OGG files in assets/audio/music). */
enum class MusicKey(val file: String) {
    MENU("menu"),
    CALM("calm"),
    TENSION("tension"),
    WAR("war"),
    VICTORY("victory"),
    COLLAPSE("collapse"),
}

/** Haptic patterns. */
enum class HapticStrength { LIGHT, MEDIUM, HEAVY }

/**
 * Single audio surface for the whole game. Implementations stay silent when
 * asset files are missing — never throw, never block.
 */
interface AudioManager {
    fun play(key: SoundKey)
    fun setMusic(key: MusicKey?)
    fun setMusicEnabled(enabled: Boolean)
    fun setSfxEnabled(enabled: Boolean)
    fun setMusicVolume(level: Float)
    fun setSfxVolume(level: Float)
    fun vibrate(strength: HapticStrength)
    fun release()
}

/** Silent implementation used until real wiring, and in tests. */
class NoopAudioManager : AudioManager {
    var lastSound: SoundKey? = null
        private set
    var lastMusic: MusicKey? = null
        private set
    var lastHaptic: HapticStrength? = null
        private set
    var musicVolume: Float = 1f
        private set
    var sfxVolume: Float = 1f
        private set

    override fun play(key: SoundKey) {
        lastSound = key
    }

    override fun setMusic(key: MusicKey?) {
        lastMusic = key
    }

    override fun setMusicEnabled(enabled: Boolean) = Unit
    override fun setSfxEnabled(enabled: Boolean) = Unit
    override fun setMusicVolume(level: Float) {
        musicVolume = level.coerceIn(0f, 1f)
    }

    override fun setSfxVolume(level: Float) {
        sfxVolume = level.coerceIn(0f, 1f)
    }

    override fun vibrate(strength: HapticStrength) {
        lastHaptic = strength
    }

    override fun release() = Unit
}
