package studio.mgn.development

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import studio.mgn.content.ContentPack
import studio.mgn.data.GameRepository
import studio.mgn.data.LoadResult
import studio.mgn.engine.BalanceConfig
import studio.mgn.engine.ConstructionEngine
import studio.mgn.model.GameState
import studio.mgn.model.LandmarkDef

enum class LandmarkTab { AVAILABLE, BUILDING, BUILT }

data class DevelopmentUiState(
    val isLoading: Boolean = true,
    val missing: Boolean = false,
    val state: GameState? = null,
    val tab: LandmarkTab = LandmarkTab.AVAILABLE,
    val available: List<LandmarkDef> = emptyList(),
    val rushCostPerTurn: Int = BalanceConfig.GEMS_PER_CONSTRUCTION_TURN_SKIP,
    /** Full catalogue for name/effect resolution in every tab. */
    val catalog: Map<String, LandmarkDef> = emptyMap(),
)

sealed interface DevelopmentEvent {
    data class Tab(val tab: LandmarkTab) : DevelopmentEvent
    data class Build(val id: String) : DevelopmentEvent
    data class Rush(val id: String) : DevelopmentEvent
    data object Refresh : DevelopmentEvent
}

/** Landmarks catalogue with available/building/built tabs. */
class DevelopmentViewModel(
    private val repository: GameRepository,
    private val content: ContentPack,
) : ViewModel() {

    private val _state = MutableStateFlow(DevelopmentUiState())
    val state: StateFlow<DevelopmentUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun onEvent(event: DevelopmentEvent) {
        when (event) {
            is DevelopmentEvent.Tab ->
                _state.value = _state.value.copy(tab = event.tab)
            is DevelopmentEvent.Build -> mutate { state ->
                val def = content.landmarkById(event.id) ?: return@mutate state
                ConstructionEngine.start(state, def) ?: state
            }
            is DevelopmentEvent.Rush -> mutate { state ->
                ConstructionEngine.rushWithGems(state, event.id, 1) ?: state
            }
            DevelopmentEvent.Refresh -> refresh()
        }
    }

    private fun mutate(block: (GameState) -> GameState) {
        val current = _state.value.state ?: return
        viewModelScope.launch {
            val next = block(current)
            if (next != current) {
                repository.save(next)
            }
            adopt(next)
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            when (val loaded = repository.load()) {
                is LoadResult.Ok -> adopt(loaded.state)
                is LoadResult.RecoveredFromBackup -> adopt(loaded.state)
                else -> _state.value = DevelopmentUiState(isLoading = false, missing = true)
            }
        }
    }

    private fun adopt(state: GameState) {
        _state.value = _state.value.copy(
            isLoading = false,
            state = state,
            available = content.landmarks.filter {
                ConstructionEngine.canStart(state, it)
            },
            catalog = content.landmarkCatalog,
        )
    }
}
