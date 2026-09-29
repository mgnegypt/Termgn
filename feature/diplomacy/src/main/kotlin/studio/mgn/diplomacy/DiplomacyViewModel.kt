package studio.mgn.diplomacy

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
import studio.mgn.engine.DiplomacyEngine
import studio.mgn.model.GameState
import studio.mgn.model.TreatyType
import studio.mgn.model.WarStance
import kotlin.random.Random

data class CountryRow(
    val id: String,
    val nameAr: String,
    val relation: Double,
    val treaties: List<TreatyType>,
    val atWar: Boolean,
    val sanctioned: Boolean,
    val stance: WarStance,
    val signable: Map<TreatyType, Boolean>,
)

data class DiplomacyUiState(
    val isLoading: Boolean = true,
    val missing: Boolean = false,
    val state: GameState? = null,
    val rows: List<CountryRow> = emptyList(),
    val selectedId: String? = null,
    /** Consequence figures shown in the war confirmation. */
    val warCostPerTurn: Double = BalanceConfig.WAR_COST_PER_TURN,
)

sealed interface DiplomacyEvent {
    data class Select(val id: String?) : DiplomacyEvent
    data class SignTreaty(val id: String, val type: TreatyType) : DiplomacyEvent
    data class BreakTreaty(val id: String, val type: TreatyType) : DiplomacyEvent
    data class DeclareWar(val id: String) : DiplomacyEvent
    data class SetStance(val id: String, val stance: WarStance) : DiplomacyEvent
    data object Refresh : DiplomacyEvent
}

/** Diplomacy screen logic; threat/improve actions do not exist in the engine. */
class DiplomacyViewModel(
    private val repository: GameRepository,
    private val content: ContentPack,
    rng: Random = Random.Default,
) : ViewModel() {

    private val engine = DiplomacyEngine(rng)

    private val _state = MutableStateFlow(DiplomacyUiState())
    val state: StateFlow<DiplomacyUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun onEvent(event: DiplomacyEvent) {
        when (event) {
            is DiplomacyEvent.Select ->
                _state.value = _state.value.copy(selectedId = event.id)
            is DiplomacyEvent.SignTreaty -> mutate { state ->
                engine.signTreaty(state, event.id, event.type) ?: state
            }
            is DiplomacyEvent.BreakTreaty -> mutate { state ->
                engine.breakTreaty(state, event.id, event.type)
            }
            is DiplomacyEvent.DeclareWar -> mutate { state ->
                engine.declareWar(state, event.id)
            }
            is DiplomacyEvent.SetStance -> mutate { state ->
                state.copy(
                    warStances = state.warStances + (event.id to event.stance),
                )
            }
            DiplomacyEvent.Refresh -> refresh()
        }
    }

    private fun mutate(block: (GameState) -> GameState) {
        val current = _state.value.state ?: return
        viewModelScope.launch {
            val next = block(current)
            repository.save(next)
            adopt(next)
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            when (val loaded = repository.load()) {
                is LoadResult.Ok -> adopt(loaded.state)
                is LoadResult.RecoveredFromBackup -> adopt(loaded.state)
                else -> _state.value = DiplomacyUiState(isLoading = false, missing = true)
            }
        }
    }

    private fun adopt(state: GameState) {
        _state.value = DiplomacyUiState(
            isLoading = false,
            state = state,
            rows = state.countries.values.map { country ->
                CountryRow(
                    id = country.countryId,
                    nameAr = country.nameAr,
                    relation = country.relation,
                    treaties = country.treaties.toList(),
                    atWar = country.atWar,
                    sanctioned = country.sanctioned,
                    stance = state.warStances[country.countryId]
                        ?: WarStance.DEFENSIVE,
                    signable = TreatyType.entries.associateWith { type ->
                        engine.canSign(state, country.countryId, type)
                    },
                )
            },
            selectedId = _state.value.selectedId,
            warCostPerTurn = BalanceConfig.WAR_COST_PER_TURN,
        )
    }
}
