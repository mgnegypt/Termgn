package studio.mgn.mgn

import android.app.Application
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import studio.mgn.audio.AndroidAudioManager
import studio.mgn.audio.AudioManager
import studio.mgn.content.ContentLoader
import studio.mgn.content.ContentPack
import studio.mgn.data.GameRepository
import studio.mgn.data.save.MIGRATION_1_2
import studio.mgn.data.save.MgnDatabase
import studio.mgn.data.settings.SettingsStore

/** Manual composition root (no DI framework in PLAN 2). */
class MgnApp : Application() {
    lateinit var repository: GameRepository
        private set
    lateinit var settings: SettingsStore
        private set

    /** Real audio backend; silent until asset files ship. */
    val audio: AudioManager by lazy { AndroidAudioManager(this) }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val content: ContentPack by lazy { ContentLoader.load() }

    /** Active game session holder for onStop auto-save. */
    var sessionHolder: SessionHolder? = null

    /** Current music mood, restored after backgrounding. */
    var musicMood: studio.mgn.audio.MusicKey? = null

    override fun onCreate() {
        super.onCreate()
        val db = Room.databaseBuilder(this, MgnDatabase::class.java, "mgn.db")
            .addMigrations(MIGRATION_1_2)
            .build()
        settings = SettingsStore(this)
        repository = GameRepository(db, worldProvider = { content.initialWorld() })
        scope.launch { settings.musicEnabled.collect { audio.setMusicEnabled(it) } }
        scope.launch { settings.sfxEnabled.collect { audio.setSfxEnabled(it) } }
        scope.launch { settings.musicVolume.collect { audio.setMusicVolume(it) } }
        scope.launch { settings.sfxVolume.collect { audio.setSfxVolume(it) } }
        scope.launch {
            settings.vibrationEnabled.collect {
                (audio as? AndroidAudioManager)?.setVibrationEnabled(it)
            }
        }
    }

    fun saveNow() {
        sessionHolder?.saveNow()
    }
}
