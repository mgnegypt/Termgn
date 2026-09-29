package studio.mgn.mgn

import android.app.Application
import androidx.room.Room
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

    val content: ContentPack by lazy { ContentLoader.load() }

    /** Active game session holder for onStop auto-save. */
    var sessionHolder: SessionHolder? = null

    override fun onCreate() {
        super.onCreate()
        val db = Room.databaseBuilder(this, MgnDatabase::class.java, "mgn.db")
            .addMigrations(MIGRATION_1_2)
            .build()
        settings = SettingsStore(this)
        repository = GameRepository(db, worldProvider = { content.initialWorld() })
    }

    fun saveNow() {
        sessionHolder?.saveNow()
    }
}
