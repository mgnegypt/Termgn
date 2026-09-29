package studio.mgn.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import studio.mgn.data.save.BACKUP_SAVE_ID
import studio.mgn.data.save.CURRENT_SAVE_ID
import studio.mgn.data.save.MgnDatabase
import studio.mgn.data.save.PAYLOAD_SCHEMA_VERSION
import studio.mgn.data.save.SaveSlot
import studio.mgn.model.DiplomacyState
import studio.mgn.model.GameState
import studio.mgn.model.TurnLength
import java.time.LocalDate

/** Input collected by the setup wizard. */
data class GameSetup(
    val countryName: String,
    val rulerTitle: String,
    val turnLength: TurnLength = TurnLength.MONTH,
    val flagPrimaryColor: Int = 0xFF0B0B0DL.toInt(),
    val flagSecondaryColor: Int = 0xFFD4AF37L.toInt(),
    val flagSymbolId: String = "crescent_star",
    val axisEconomic: Double = 0.0,
    val axisSocial: Double = 0.0,
    val axisForeign: Double = 0.0,
)

/** Lightweight summary shown in the menu without parsing the full state. */
data class SaveSummary(
    val countryName: String,
    val turnNumber: Int,
    val dateIso: String,
    val savedAt: Long,
)

/** Result of attempting to load a save. Never throws on corrupt data. */
sealed interface LoadResult {
    data object NoSave : LoadResult
    data class Ok(val state: GameState, val summary: SaveSummary) : LoadResult

    /** Current row is corrupt but the last-good backup parsed fine. */
    data class RecoveredFromBackup(val state: GameState, val summary: SaveSummary) : LoadResult

    /** Both rows are unreadable; the UI must offer "delete save". */
    data object Corrupted : LoadResult
}

/**
 * Owns persistence. Keeps the last known-good backup so a corrupt write
 * never crashes the game and never silently wipes progress.
 */
class GameRepository(
    private val db: MgnDatabase,
    private val worldProvider: () -> Map<String, DiplomacyState>,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    suspend fun hasSave(): Boolean = withContext(ioDispatcher) {
        db.saveDao().get(CURRENT_SAVE_ID) != null
    }

    suspend fun peekSummary(): SaveSummary? = withContext(ioDispatcher) {
        val row = db.saveDao().get(CURRENT_SAVE_ID)
            ?: db.saveDao().get(BACKUP_SAVE_ID)
            ?: return@withContext null
        SaveSummary(row.countryName, row.turnNumber, row.dateIso, row.savedAt)
    }

    suspend fun newGame(setup: GameSetup): GameState = withContext(ioDispatcher) {
        val state = GameState.newGame(
            countryName = setup.countryName,
            rulerTitle = setup.rulerTitle,
            turnLength = setup.turnLength,
            flagPrimaryColor = setup.flagPrimaryColor,
            flagSecondaryColor = setup.flagSecondaryColor,
            flagSymbolId = setup.flagSymbolId,
            countries = worldProvider(),
            axisEconomic = setup.axisEconomic,
            axisSocial = setup.axisSocial,
            axisForeign = setup.axisForeign,
        )
        persist(state, playTimeSeconds = 0)
        state
    }

    /** Called at end of turn and onStop. Shifts the previous good save aside. */
    suspend fun save(state: GameState, playTimeSeconds: Long = 0) {
        withContext(ioDispatcher) { persist(state, playTimeSeconds) }
    }

    suspend fun deleteSave() = withContext(ioDispatcher) {
        db.saveDao().deleteAll()
    }

    suspend fun load(): LoadResult = withContext(ioDispatcher) {
        val current = db.saveDao().get(CURRENT_SAVE_ID)
        if (current == null) {
            // No current row: a lone backup still counts as recoverable.
            val backup = db.saveDao().get(BACKUP_SAVE_ID)
                ?.let { parse(it) }
            return@withContext if (backup != null) {
                LoadResult.RecoveredFromBackup(backup.first, backup.second)
            } else {
                LoadResult.NoSave
            }
        }
        val parsed = parse(current)
        if (parsed != null) return@withContext LoadResult.Ok(parsed.first, parsed.second)

        val backup = db.saveDao().get(BACKUP_SAVE_ID)?.let { parse(it) }
        if (backup != null) {
            LoadResult.RecoveredFromBackup(backup.first, backup.second)
        } else {
            LoadResult.Corrupted
        }
    }

    private suspend fun persist(state: GameState, playTimeSeconds: Long) {
        val dao = db.saveDao()
        // Shift the previous current aside only if it still parses.
        val previous = dao.get(CURRENT_SAVE_ID)
        if (previous != null && parses(previous.payloadJson)) {
            dao.upsert(previous.copy(id = BACKUP_SAVE_ID))
        }
        dao.upsert(
            SaveSlot(
                id = CURRENT_SAVE_ID,
                payloadJson = json.encodeToString(GameState.serializer(), state),
                schemaVersion = PAYLOAD_SCHEMA_VERSION,
                countryName = state.countryName,
                turnNumber = state.turnNumber,
                dateIso = state.inGameDate.toString(),
                savedAt = System.currentTimeMillis(),
                playTimeSeconds = playTimeSeconds,
            ),
        )
    }

    private fun parses(payload: String): Boolean = try {
        json.decodeFromString(GameState.serializer(), payload)
        true
    } catch (_: Exception) {
        false
    }

    private fun parse(row: SaveSlot): Pair<GameState, SaveSummary>? = try {
        val state = json.decodeFromString(GameState.serializer(), row.payloadJson)
        state to SaveSummary(row.countryName, row.turnNumber, row.dateIso, row.savedAt)
    } catch (_: Exception) {
        null
    }
}

/** Display helpers shared by menu and setup summaries. */
fun saveDateLabel(iso: String): String = try {
    val d = LocalDate.parse(iso)
    "%04d/%02d".format(d.year, d.monthValue)
} catch (_: Exception) {
    iso
}
