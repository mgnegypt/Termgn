package studio.mgn.command

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import studio.mgn.content.ContentPack
import studio.mgn.data.GameRepository
import studio.mgn.data.LoadResult
import studio.mgn.engine.BalanceConfig
import studio.mgn.engine.ConstructionEngine
import studio.mgn.engine.DiplomacyEngine
import studio.mgn.engine.EventEngine
import studio.mgn.engine.EventResolution
import studio.mgn.engine.MissionEngine
import studio.mgn.engine.MissionProgress
import studio.mgn.engine.TurnEngine
import studio.mgn.engine.TurnReport
import studio.mgn.model.GameEvent
import studio.mgn.model.GameState
import studio.mgn.model.LandmarkDef
import kotlin.random.Random

/** Side-rail sections in fixed order. */
enum class CommandSection {
    DASHBOARD,
    ECONOMY,
    DIPLOMACY,
    DEVELOPMENT,
    RESEARCH,
    INTEL,
    HISTORY,
    ACHIEVEMENTS,
    SETTINGS,
}

val LOCKED_SECTIONS = setOf(CommandSection.RESEARCH, CommandSection.INTEL)

/** Sections with real screens in PLAN 3; the rest show placeholders. */
val LIVE_SECTIONS = setOf(CommandSection.DASHBOARD)

/** MVI state for the command screen. */
data class CommandUiState(
    val isLoading: Boolean = true,
    val missing: Boolean = false,
    val state: GameState? = null,
    /** State before the last turn, for delta display. */
    val previous: GameState? = null,
    val lastReport: TurnReport? = null,
    val pending: List<GameEvent> = emptyList(),
    val lastResolution: EventResolution? = null,
    val processing: Boolean = false,
    val showReport: Boolean = false,
    val section: CommandSection = CommandSection.DASHBOARD,
    val selectedIndicator: String? = null,
    val selectedLandmark: String? = null,
    val selectedCountry: String? = null,
    val showDecisions: Boolean = false,
    val missions: List<MissionProgress> = emptyList(),
    val completedMissionCount: Int = 0,
    /** Landmark ids the player can afford and meets requirements for now. */
    val buildable: Set<String> = emptySet(),
    /** Per-choice selectability + effective cash cost for the first pending event. */
    val choiceStates: List<ChoiceUiState> = emptyList(),
)

data class ChoiceUiState(
    val selectable: Boolean,
    val effectiveCostCash: Double,
)

sealed interface CommandEvent {
    data object EndTurn : CommandEvent
    data object DismissReport : CommandEvent
    data class Choose(val eventId: String, val choiceIndex: Int) : CommandEvent
    data object Reroll : CommandEvent
    data class SelectSection(val section: CommandSection) : CommandEvent
    data class ShowIndicator(val key: String?) : CommandEvent
    data class ShowLandmark(val id: String?) : CommandEvent
    data class ShowCountry(val id: String?) : CommandEvent
    data class ShowDecisions(val open: Boolean) : CommandEvent
    data class StartConstruction(val landmarkId: String) : CommandEvent
    data class RushConstruction(val landmarkId: String) : CommandEvent
}

/** One-shot UI signals (navigation is handled by the app layer). */
sealed interface CommandSignal {
    data object DecisionsOpened : CommandSignal
}

/**
 * Command screen logic: owns the live [GameState], runs turns through
 * [TurnEngine], resolves decisions, starts constructions and auto-saves.
 * Composables never touch the engine directly.
 */
class CommandViewModel(
    private val repository: GameRepository,
    private val content: ContentPack,
    private val registrar: SessionRegistrar,
    rng: Random = Random.Default,
) : ViewModel() {

    private val eventEngine = EventEngine(pool = content.events, rng = rng)
    private val turnEngine = TurnEngine(
        eventEngine = eventEngine,
        achievements = content.achievements,
        diplomacyEngine = DiplomacyEngine(rng),
        landmarkCatalog = content.landmarkCatalog,
        missions = content.missions,
        rng = rng,
    )

    private val _state = MutableStateFlow(CommandUiState())
    val state: StateFlow<CommandUiState> = _state.asStateFlow()

    private val _signals = Channel<CommandSignal>(Channel.BUFFERED)
    val signals = _signals.receiveAsFlow()

    init {
        viewModelScope.launch {
            when (val loaded = repository.load()) {
                is LoadResult.Ok -> attach(loaded.state)
                is LoadResult.RecoveredFromBackup -> attach(loaded.state)
                else -> _state.value = CommandUiState(isLoading = false, missing = true)
            }
        }
    }

    private fun attach(state: GameState) {
        registrar.registerSession(state)
        _state.value = CommandUiState(
            isLoading = false,
            state = state,
            missions = MissionEngine.activeMissions(state, content.missions),
            completedMissionCount = state.completedMissions.size,
            buildable = buildableIds(state),
            choiceStates = emptyList(),
        )
    }

    private fun buildableIds(state: GameState): Set<String> =
        content.landmarks
            .filter { ConstructionEngine.canStart(state, it) }
            .map { it.id }
            .toSet()

    private fun choiceStates(
        state: GameState,
        pending: List<GameEvent>,
    ): List<ChoiceUiState> {
        val current = pending.firstOrNull() ?: return emptyList()
        return current.choices.map { choice ->
            ChoiceUiState(
                selectable = eventEngine.isSelectable(choice, state),
                effectiveCostCash = eventEngine.effectiveCostCash(choice),
            )
        }
    }

    fun onEvent(event: CommandEvent) {
        when (event) {
            CommandEvent.EndTurn -> endTurn()
            CommandEvent.DismissReport ->
                _state.value = _state.value.copy(showReport = false)
            is CommandEvent.Choose -> choose(event.eventId, event.choiceIndex)
            CommandEvent.Reroll -> reroll()
            is CommandEvent.SelectSection ->
                _state.value = _state.value.copy(section = event.section)
            is CommandEvent.ShowIndicator ->
                _state.value = _state.value.copy(selectedIndicator = event.key)
            is CommandEvent.ShowLandmark ->
                _state.value = _state.value.copy(selectedLandmark = event.id)
            is CommandEvent.ShowCountry ->
                _state.value = _state.value.copy(selectedCountry = event.id)
            is CommandEvent.ShowDecisions -> {
                _state.value = _state.value.copy(showDecisions = event.open)
                if (event.open) viewModelScope.launch {
                    _signals.send(CommandSignal.DecisionsOpened)
                }
            }
            is CommandEvent.StartConstruction -> construct(event.landmarkId)
            is CommandEvent.RushConstruction -> rush(event.landmarkId)
        }
    }

    private fun endTurn() {
        val s = _state.value
        val current = s.state ?: return
        if (s.processing) return
        if (s.pending.isNotEmpty()) {
            // Decisions cannot be skipped: route to the decisions screen.
            _state.value = s.copy(showDecisions = true)
            return
        }
        _state.value = s.copy(processing = true)
        viewModelScope.launch {
            val report = turnEngine.advance(current)
            val next = report.state
            registrar.session()?.update(next)
            repository.save(next)
            _state.value = _state.value.copy(
                state = next,
                previous = current,
                lastReport = report,
                pending = report.pendingEvents,
                processing = false,
                showReport = true,
                lastResolution = null,
                missions = MissionEngine.activeMissions(next, content.missions),
                completedMissionCount = next.completedMissions.size,
                buildable = buildableIds(next),
                choiceStates = choiceStates(next, report.pendingEvents),
            )
        }
    }

    private fun choose(eventId: String, choiceIndex: Int) {
        val s = _state.value
        val current = s.state ?: return
        val event = s.pending.firstOrNull { it.id == eventId } ?: return
        val choice = event.choices.getOrNull(choiceIndex) ?: return
        if (!eventEngine.isSelectable(choice, current)) return
        viewModelScope.launch {
            val result = eventEngine.applyChoice(current, event, choice)
            val remaining = s.pending.filter { it.id != eventId }
            registrar.session()?.update(result.state)
            repository.save(result.state)
            _state.value = _state.value.copy(
                state = result.state,
                pending = remaining,
                lastResolution = result.resolution,
                missions = MissionEngine.activeMissions(result.state, content.missions),
                completedMissionCount = result.state.completedMissions.size,
                buildable = buildableIds(result.state),
                choiceStates = choiceStates(result.state, remaining),
            )
        }
    }

    private fun reroll() {
        val s = _state.value
        val current = s.state ?: return
        val event = s.pending.firstOrNull() ?: return
        if (current.gems < BalanceConfig.GEMS_PER_DECISION_REROLL) return
        viewModelScope.launch {
            val replacement = eventEngine.reroll(current, event) ?: return@launch
            val paid = current.copy(
                gems = current.gems - BalanceConfig.GEMS_PER_DECISION_REROLL,
            )
            val updated = listOf(replacement) +
                s.pending.filter { it.id != event.id }
            registrar.session()?.update(paid)
            repository.save(paid)
            _state.value = _state.value.copy(state = paid, pending = updated)
        }
    }

    private fun construct(landmarkId: String) {
        val s = _state.value
        val current = s.state ?: return
        val def: LandmarkDef = content.landmarkById(landmarkId) ?: return
        viewModelScope.launch {
            val next = ConstructionEngine.start(current, def) ?: return@launch
            registrar.session()?.update(next)
            repository.save(next)
            _state.value = _state.value.copy(
                state = next,
                selectedLandmark = null,
                buildable = buildableIds(next),
            )
        }
    }

    private fun rush(landmarkId: String) {
        val s = _state.value
        val current = s.state ?: return
        viewModelScope.launch {
            val next = ConstructionEngine.rushWithGems(current, landmarkId, 1)
                ?: return@launch
            registrar.session()?.update(next)
            repository.save(next)
            _state.value = _state.value.copy(state = next)
        }
    }

    /** Minimal surface the app layer provides (session holder access). */
    interface SessionHandle {
        fun update(state: GameState)
    }

    interface SessionRegistrar {
        fun registerSession(initial: GameState)
        fun session(): SessionHandle?
    }
}
