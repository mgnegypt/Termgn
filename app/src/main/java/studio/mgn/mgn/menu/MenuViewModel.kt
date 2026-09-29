package studio.mgn.mgn.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import studio.mgn.data.GameRepository
import studio.mgn.data.LoadResult
import studio.mgn.data.SaveSummary

/** One-shot navigation targets from the menu. */
enum class MenuNav { SETUP, GAME }

/** MVI state for the main menu. */
data class MenuUiState(
    val isLoading: Boolean = true,
    val summary: SaveSummary? = null,
    val showOverwriteConfirm: Boolean = false,
    /** Current row unreadable and no backup: offer delete. */
    val corrupted: Boolean = false,
    val recoveredFromBackup: Boolean = false,
)

sealed interface MenuEvent {
    data object Refresh : MenuEvent
    data object NewGameClicked : MenuEvent
    data object OverwriteConfirmed : MenuEvent
    data object OverwriteDismissed : MenuEvent
    data object ContinueClicked : MenuEvent
    data object DeleteSaveClicked : MenuEvent
}

/** Menu logic: summaries, overwrite guard, corrupt-save handling. */
class MenuViewModel(private val repository: GameRepository) : ViewModel() {

    private val _state = MutableStateFlow(MenuUiState())
    val state: StateFlow<MenuUiState> = _state.asStateFlow()

    private val _nav = Channel<MenuNav>(Channel.BUFFERED)
    val nav = _nav.receiveAsFlow()

    init {
        refresh()
    }

    fun onEvent(event: MenuEvent) {
        when (event) {
            MenuEvent.Refresh -> refresh()
            MenuEvent.NewGameClicked -> viewModelScope.launch {
                if (repository.hasSave()) {
                    _state.value = _state.value.copy(showOverwriteConfirm = true)
                } else {
                    _nav.send(MenuNav.SETUP)
                }
            }
            MenuEvent.OverwriteConfirmed -> viewModelScope.launch {
                repository.deleteSave()
                _state.value = MenuUiState(isLoading = false)
                _nav.send(MenuNav.SETUP)
            }
            MenuEvent.OverwriteDismissed -> {
                _state.value = _state.value.copy(showOverwriteConfirm = false)
            }
            MenuEvent.ContinueClicked -> viewModelScope.launch {
                _nav.send(MenuNav.GAME)
            }
            MenuEvent.DeleteSaveClicked -> viewModelScope.launch {
                repository.deleteSave()
                refresh()
            }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _state.value = MenuUiState(isLoading = true)
            val summary = repository.peekSummary()
            val load = repository.load()
            _state.value = MenuUiState(
                isLoading = false,
                summary = summary,
                corrupted = load is LoadResult.Corrupted,
                recoveredFromBackup = load is LoadResult.RecoveredFromBackup,
            )
        }
    }
}
