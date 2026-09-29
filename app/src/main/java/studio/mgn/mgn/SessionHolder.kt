package studio.mgn.mgn

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import studio.mgn.data.GameRepository
import studio.mgn.model.GameState

/**
 * Holds the active session so the Application can auto-save onStop.
 * Turn-end auto-save (PLAN 3) will call [saveNow] after each turn too.
 */
class SessionHolder(
    private val repository: GameRepository,
    initial: GameState,
    private val playTimeSeconds: () -> Long = { 0 },
) {
    @Volatile
    var state: GameState = initial
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun update(next: GameState) {
        state = next
    }

    fun saveNow() {
        val snapshot = state
        val playtime = playTimeSeconds()
        scope.launch { repository.save(snapshot, playtime) }
    }
}
