package studio.mgn.data.save

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Current Room schema version. */
const val DB_SCHEMA_VERSION = 2

/** Payload (GameState JSON) schema version. */
const val PAYLOAD_SCHEMA_VERSION = 1

const val CURRENT_SAVE_ID = 0
const val BACKUP_SAVE_ID = 1

/**
 * One save row. Id 0 is the current save, id 1 the last known-good backup.
 * `playTimeSeconds` was added in schema v2 (see MIGRATION_1_2).
 */
@Entity(tableName = "saves")
data class SaveSlot(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "payload_json") val payloadJson: String,
    @ColumnInfo(name = "schema_version") val schemaVersion: Int,
    @ColumnInfo(name = "country_name") val countryName: String,
    @ColumnInfo(name = "turn_number") val turnNumber: Int,
    @ColumnInfo(name = "date_iso") val dateIso: String,
    @ColumnInfo(name = "saved_at") val savedAt: Long,
    @ColumnInfo(name = "playtime_seconds", defaultValue = "0")
    val playTimeSeconds: Long = 0,
)
