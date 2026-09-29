package studio.mgn.data.save

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1: saves(id, payload_json, schema_version, country_name, turn_number,
 * date_iso, saved_at).
 * v2: adds playtime_seconds (default 0).
 */
@Database(
    entities = [SaveSlot::class],
    version = DB_SCHEMA_VERSION,
    exportSchema = false,
)
abstract class MgnDatabase : RoomDatabase() {
    abstract fun saveDao(): SaveDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE saves ADD COLUMN playtime_seconds INTEGER NOT NULL DEFAULT 0",
        )
    }
}
