package studio.mgn.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import studio.mgn.data.GameRepository
import studio.mgn.data.GameSetup
import studio.mgn.model.TurnLength

/** Wizard steps in order. */
enum class SetupStep { NAME, TITLE, FLAG, TURN, AXES, REVIEW }

/** Flag presets: primary/secondary colors + symbol id. */
data class FlagPreset(
    val primary: Int,
    val secondary: Int,
    val symbolId: String,
)

val FLAG_PRESETS = listOf(
    FlagPreset(0xFF0B0B0DL.toInt(), 0xFFD4AF37L.toInt(), "crescent_star"),
    FlagPreset(0xFF0F766EL.toInt(), 0xFFF5F3EEL.toInt(), "eagle"),
    FlagPreset(0xFF7C2D12L.toInt(), 0xFFD4AF37L.toInt(), "tower"),
)

data class SetupUiState(
    val step: SetupStep = SetupStep.NAME,
    val countryName: String = "",
    val rulerTitle: String = "",
    val flagPreset: Int = 0,
    val turnLength: TurnLength = TurnLength.MONTH,
    val axisEconomic: Float = 0f,
    val axisSocial: Float = 0f,
    val axisForeign: Float = 0f,
    val showError: Boolean = false,
    val isCreating: Boolean = false,
)

sealed interface SetupEvent {
    data class NameChanged(val value: String) : SetupEvent
    data class TitleChanged(val value: String) : SetupEvent
    data class FlagSelected(val index: Int) : SetupEvent
    data class TurnSelected(val length: TurnLength) : SetupEvent
    data class AxisChanged(val axis: SetupAxis, val value: Float) : SetupEvent
    data object Next : SetupEvent
    data object Back : SetupEvent
    data object Confirm : SetupEvent
}

enum class SetupAxis { ECONOMIC, SOCIAL, FOREIGN }

/** Illustrated multi-step state founding flow (MVI). */
class SetupViewModel(private val repository: GameRepository) : ViewModel() {

    private val _state = MutableStateFlow(SetupUiState())
    val state: StateFlow<SetupUiState> = _state.asStateFlow()

    private val _done = Channel<Unit>(Channel.BUFFERED)
    val done = _done.receiveAsFlow()

    fun onEvent(event: SetupEvent) {
        val s = _state.value
        when (event) {
            is SetupEvent.NameChanged ->
                _state.value = s.copy(countryName = event.value, showError = false)
            is SetupEvent.TitleChanged ->
                _state.value = s.copy(rulerTitle = event.value, showError = false)
            is SetupEvent.FlagSelected ->
                _state.value = s.copy(flagPreset = event.index.coerceIn(FLAG_PRESETS.indices))
            is SetupEvent.TurnSelected ->
                _state.value = s.copy(turnLength = event.length)
            is SetupEvent.AxisChanged -> _state.value = when (event.axis) {
                SetupAxis.ECONOMIC ->
                    s.copy(axisEconomic = event.value.coerceIn(-100f, 100f))
                SetupAxis.SOCIAL ->
                    s.copy(axisSocial = event.value.coerceIn(-100f, 100f))
                SetupAxis.FOREIGN ->
                    s.copy(axisForeign = event.value.coerceIn(-100f, 100f))
            }
            SetupEvent.Next -> {
                if (!stepValid(s)) {
                    _state.value = s.copy(showError = true)
                } else {
                    _state.value = s.copy(step = nextStep(s.step), showError = false)
                }
            }
            SetupEvent.Back -> {
                _state.value = s.copy(step = prevStep(s.step), showError = false)
            }
            SetupEvent.Confirm -> viewModelScope.launch {
                if (!stepValid(s)) {
                    _state.value = s.copy(showError = true)
                    return@launch
                }
                _state.value = s.copy(isCreating = true)
                val preset = FLAG_PRESETS[s.flagPreset]
                repository.newGame(
                    GameSetup(
                        countryName = s.countryName.trim(),
                        rulerTitle = s.rulerTitle.trim(),
                        turnLength = s.turnLength,
                        flagPrimaryColor = preset.primary,
                        flagSecondaryColor = preset.secondary,
                        flagSymbolId = preset.symbolId,
                        axisEconomic = s.axisEconomic.toDouble(),
                        axisSocial = s.axisSocial.toDouble(),
                        axisForeign = s.axisForeign.toDouble(),
                    ),
                )
                _done.send(Unit)
            }
        }
    }

    private fun stepValid(s: SetupUiState): Boolean = when (s.step) {
        SetupStep.NAME -> s.countryName.isNotBlank()
        SetupStep.TITLE -> s.rulerTitle.isNotBlank()
        SetupStep.REVIEW ->
            s.countryName.isNotBlank() && s.rulerTitle.isNotBlank()
        else -> true
    }

    private fun nextStep(step: SetupStep): SetupStep {
        val all = SetupStep.entries
        return all[(all.indexOf(step) + 1).coerceAtMost(all.lastIndex)]
    }

    private fun prevStep(step: SetupStep): SetupStep {
        val all = SetupStep.entries
        return all[(all.indexOf(step) - 1).coerceAtLeast(0)]
    }
}
