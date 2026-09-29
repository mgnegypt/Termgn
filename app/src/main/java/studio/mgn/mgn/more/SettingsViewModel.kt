package studio.mgn.mgn.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import studio.mgn.data.GameRepository
import studio.mgn.data.settings.GraphicsQuality
import studio.mgn.data.settings.SettingsStore

data class SettingsUiState(
    val music: Boolean = true,
    val sfx: Boolean = true,
    val vibration: Boolean = true,
    val reduceMotion: Boolean = false,
    val graphics: GraphicsQuality = GraphicsQuality.MEDIUM,
    val hasSave: Boolean = false,
    val showDeleteFirst: Boolean = false,
    val showDeleteSecond: Boolean = false,
)

sealed interface SettingsEvent {
    data class Music(val enabled: Boolean) : SettingsEvent
    data class Sfx(val enabled: Boolean) : SettingsEvent
    data class Vibration(val enabled: Boolean) : SettingsEvent
    data class ReduceMotion(val enabled: Boolean) : SettingsEvent
    data class Graphics(val quality: GraphicsQuality) : SettingsEvent
    data object DeleteRequested : SettingsEvent
    data object DeleteConfirmedFirst : SettingsEvent
    data object DeleteConfirmedSecond : SettingsEvent
    data object DeleteDismissed : SettingsEvent
}

/** Settings screen logic; delete uses a double-confirm handshake. */
class SettingsViewModel(
    private val settings: SettingsStore,
    private val repository: GameRepository,
) : ViewModel() {

    private val dialogs = MutableStateFlow(Dialogs())
    private val hasSave = MutableStateFlow(false)

    val state: StateFlow<SettingsUiState> = combine(
        settings.musicEnabled,
        settings.sfxEnabled,
        settings.vibrationEnabled,
        settings.reduceMotion,
        settings.graphicsQuality,
        combine(dialogs, hasSave, ::DialogsWithSave),
    ) { music, sfx, vibration, reduceMotion, graphics, extra ->
        SettingsUiState(
            music = music,
            sfx = sfx,
            vibration = vibration,
            reduceMotion = reduceMotion,
            graphics = graphics,
            hasSave = extra.hasSave,
            showDeleteFirst = extra.dialogs.first,
            showDeleteSecond = extra.dialogs.second,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    init {
        viewModelScope.launch {
            hasSave.value = repository.hasSave()
        }
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.Music -> viewModelScope.launch {
                settings.setMusicEnabled(event.enabled)
            }
            is SettingsEvent.Sfx -> viewModelScope.launch {
                settings.setSfxEnabled(event.enabled)
            }
            is SettingsEvent.Vibration -> viewModelScope.launch {
                settings.setVibrationEnabled(event.enabled)
            }
            is SettingsEvent.ReduceMotion -> viewModelScope.launch {
                settings.setReduceMotion(event.enabled)
            }
            is SettingsEvent.Graphics -> viewModelScope.launch {
                settings.setGraphicsQuality(event.quality)
            }
            SettingsEvent.DeleteRequested ->
                dialogs.value = Dialogs(first = true)
            SettingsEvent.DeleteConfirmedFirst ->
                dialogs.value = Dialogs(second = true)
            SettingsEvent.DeleteConfirmedSecond -> viewModelScope.launch {
                repository.deleteSave()
                hasSave.value = false
                dialogs.value = Dialogs()
            }
            SettingsEvent.DeleteDismissed -> dialogs.value = Dialogs()
        }
    }

    private data class Dialogs(val first: Boolean = false, val second: Boolean = false)

    private data class DialogsWithSave(val dialogs: Dialogs, val hasSave: Boolean)
}
