package studio.mgn.mgn.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import studio.mgn.data.GameRepository
import studio.mgn.data.LoadResult
import studio.mgn.data.saveDateLabel
import studio.mgn.mgn.SessionHolder

data class GameUiState(
    val isLoading: Boolean = true,
    val countryName: String = "",
    val turnNumber: Int = 0,
    val dateLabel: String = "",
    val missing: Boolean = false,
)

/** Minimal surface the Application needs from the game layer. */
fun interface SessionRegistrar {
    fun registerSession(holder: SessionHolder)
}

/**
 * PLAN 2 placeholder game screen logic: loads the save, registers the
 * session for onStop auto-save. Full turn loop arrives in PLAN 3.
 */
class GameViewModel(
    private val repository: GameRepository,
    private val registrar: SessionRegistrar,
) : ViewModel() {

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            when (val loaded = repository.load()) {
                is LoadResult.Ok -> attach(loaded.state)
                is LoadResult.RecoveredFromBackup -> attach(loaded.state)
                else -> _state.value = GameUiState(isLoading = false, missing = true)
            }
        }
    }

    private fun attach(state: studio.mgn.model.GameState) {
        registrar.registerSession(SessionHolder(repository, state))
        _state.value = GameUiState(
            isLoading = false,
            countryName = state.countryName,
            turnNumber = state.turnNumber,
            dateLabel = saveDateLabel(state.inGameDate.toString()),
        )
    }
}
