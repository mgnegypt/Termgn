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
import studio.mgn.model.GameEnding
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
    /** Ordered overlays after a turn: report, then yearly, then referendum. */
    val overlays: List<ReportOverlay> = emptyList(),
    val ending: GameEndingScreen? = null,
    /** Golden unlock toast on the dashboard until the next turn. */
    val showUnlockToast: Boolean = false,
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
    val referendumThreshold: Double = BalanceConfig.REFERENDUM_PASS_THRESHOLD,
)

data class ChoiceUiState(
    val selectable: Boolean,
    val effectiveCostCash: Double,
)

sealed interface CommandEvent {
    data object EndTurn : CommandEvent
    data object DismissReport : CommandEvent
    data object DismissEnding : CommandEvent
    data object GoToMenu : CommandEvent
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
    data object NavigateMenu : CommandSignal
    data object NavigateEconomy : CommandSignal
    data object NavigateDiplomacy : CommandSignal
    data object NavigateDevelopment : CommandSignal
    data object NavigateSettings : CommandSignal
}

/** Which overlay sits above the dashboard after a turn. */
enum class ReportOverlay { REPORT, YEARLY, REFERENDUM }

/** Which ending screen is on display, if any. */
enum class GameEndingScreen { VICTORY, COLLAPSE, CONTINUATION }

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

    // Session-only reign tracking for ending summaries (not persisted).
    private var yearResolutions = mutableListOf<EventResolution>()
    private var resolutionYear: Int? = null
    private var allResolutions = mutableListOf<EventResolution>()
    private var bestTurn: Pair<Int, Double>? = null
    private var worstTurn: Pair<Int, Double>? = null
    private var continuationShown = false
    private var victoryShown = false

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
        resolutionYear = state.inGameDate.year
        var next = state
        // A loaded save already past an ending keeps the persisted marker.
        if ((next.isCollapsed || next.hasWon) && next.ended == null) {
            next = next.copy(
                ended = if (next.isCollapsed) {
                    GameEnding.COLLAPSE
                } else {
                    GameEnding.VICTORY
                },
            )
            viewModelScope.launch { repository.save(next) }
        }
        _state.value = CommandUiState(
            isLoading = false,
            state = next,
            missions = MissionEngine.activeMissions(next, content.missions),
            completedMissionCount = next.completedMissions.size,
            buildable = buildableIds(next),
            choiceStates = emptyList(),
            ending = endingFor(next),
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
            CommandEvent.DismissReport -> dismissOverlay()
            CommandEvent.DismissEnding ->
                _state.value = _state.value.copy(ending = null)
            CommandEvent.GoToMenu -> viewModelScope.launch {
                _signals.send(CommandSignal.NavigateMenu)
            }
            is CommandEvent.Choose -> choose(event.eventId, event.choiceIndex)
            CommandEvent.Reroll -> reroll()
            is CommandEvent.SelectSection -> when (event.section) {
                CommandSection.ECONOMY -> signal(CommandSignal.NavigateEconomy)
                CommandSection.DIPLOMACY -> signal(CommandSignal.NavigateDiplomacy)
                CommandSection.DEVELOPMENT -> signal(CommandSignal.NavigateDevelopment)
                CommandSection.SETTINGS -> signal(CommandSignal.NavigateSettings)
                else -> _state.value = _state.value.copy(section = event.section)
            }
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

    private fun signal(target: CommandSignal) {
        viewModelScope.launch { _signals.send(target) }
    }

    private fun endTurn() {
        val s = _state.value
        val current = s.state ?: return
        if (s.processing) return
        if (current.ended == GameEnding.COLLAPSE) return
        if (s.pending.isNotEmpty()) {
            // Decisions cannot be skipped: route to the decisions screen.
            _state.value = s.copy(showDecisions = true)
            return
        }
        _state.value = s.copy(processing = true)
        viewModelScope.launch {
            val report = turnEngine.advance(current)
            var next = report.state
            trackTurnDelta(current, next)
            if (next.inGameDate.year != current.inGameDate.year) {
                yearResolutions = mutableListOf()
                resolutionYear = next.inGameDate.year
            }
            val overlays = buildList {
                add(ReportOverlay.REPORT)
                if (report.yearlyReport != null) add(ReportOverlay.YEARLY)
                if (report.referendum != null) add(ReportOverlay.REFERENDUM)
            }
            next = maybeEnd(next)
            registrar.session()?.update(next)
            repository.save(next)
            _state.value = _state.value.copy(
                state = next,
                previous = current,
                lastReport = report,
                pending = report.pendingEvents,
                processing = false,
                showReport = true,
                overlays = overlays,
                showUnlockToast = report.newAchievements.isNotEmpty() ||
                    report.newMissions.isNotEmpty(),
                lastResolution = null,
                missions = MissionEngine.activeMissions(next, content.missions),
                completedMissionCount = next.completedMissions.size,
                buildable = buildableIds(next),
                choiceStates = choiceStates(next, report.pendingEvents),
                ending = endingFor(next),
            )
        }
    }

    private fun dismissOverlay() {
        val s = _state.value
        val remaining = s.overlays.drop(1)
        _state.value = s.copy(
            overlays = remaining,
            showReport = remaining.isNotEmpty(),
            showUnlockToast = false,
        )
    }

    /** Best/worst turn by nation-score swing, for ending summaries. */
    private fun trackTurnDelta(before: GameState, after: GameState) {
        val swing = after.nationScore - before.nationScore
        val turn = after.turnNumber
        if (bestTurn == null || swing > bestTurn!!.second) bestTurn = turn to swing
        if (worstTurn == null || swing < worstTurn!!.second) worstTurn = turn to swing
    }

    fun bestDecision(): EventResolution? =
        yearResolutions.maxByOrNull { it.appliedEffects.values.sum() }

    fun worstDecision(): EventResolution? =
        yearResolutions.minByOrNull { it.appliedEffects.values.sum() }

    fun bestTurnInfo(): Pair<Int, Double>? = bestTurn
    fun worstTurnInfo(): Pair<Int, Double>? = worstTurn

    fun allResolutions(): List<EventResolution> = allResolutions.toList()

    /**
     * Persists victory/collapse once, decides which ending screen to show.
     * Collapse blocks further play; victory and continuation allow it.
     */
    private fun maybeEnd(next: GameState): GameState {
        var out = next
        if (next.isCollapsed && next.ended == null) {
            out = next.copy(ended = GameEnding.COLLAPSE)
        } else if (next.hasWon && next.ended == null) {
            out = next.copy(ended = GameEnding.VICTORY)
        }
        return out
    }

    private fun endingFor(next: GameState): GameEndingScreen? = when {
        next.isCollapsed -> GameEndingScreen.COLLAPSE
        next.hasWon && next.ended == GameEnding.VICTORY && !victoryShown -> {
            victoryShown = true
            GameEndingScreen.VICTORY
        }
        !continuationShown &&
            (next.inGameDate.year >= 2035 || next.turnNumber >= 120) -> {
            continuationShown = true
            GameEndingScreen.CONTINUATION
        }
        else -> _state.value.ending
    }

    private fun choose(eventId: String, choiceIndex: Int) {
        val s = _state.value
        val current = s.state ?: return
        if (current.ended == GameEnding.COLLAPSE) return
        val event = s.pending.firstOrNull { it.id == eventId } ?: return
        val choice = event.choices.getOrNull(choiceIndex) ?: return
        if (!eventEngine.isSelectable(choice, current)) return
        viewModelScope.launch {
            val result = eventEngine.applyChoice(current, event, choice)
            if (resolutionYear != result.state.inGameDate.year) {
                yearResolutions = mutableListOf()
                resolutionYear = result.state.inGameDate.year
            }
            yearResolutions.add(result.resolution)
            allResolutions.add(result.resolution)
            val remaining = s.pending.filter { it.id != eventId }
            var next = result.state
            next = maybeEnd(next)
            registrar.session()?.update(next)
            repository.save(next)
            _state.value = _state.value.copy(
                state = next,
                pending = remaining,
                lastResolution = result.resolution,
                missions = MissionEngine.activeMissions(next, content.missions),
                completedMissionCount = next.completedMissions.size,
                buildable = buildableIds(next),
                choiceStates = choiceStates(next, remaining),
                ending = endingFor(next),
            )
        }
    }

    private fun reroll() {
        val s = _state.value
        val current = s.state ?: return
        if (current.ended == GameEnding.COLLAPSE) return
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
            _state.value = _state.value.copy(
                state = paid,
                pending = updated,
                choiceStates = choiceStates(paid, updated),
            )
        }
    }

    private fun construct(landmarkId: String) {
        val s = _state.value
        val current = s.state ?: return
        if (current.ended == GameEnding.COLLAPSE) return
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
        if (current.ended == GameEnding.COLLAPSE) return
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
