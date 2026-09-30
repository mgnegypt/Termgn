package studio.mgn.mgn.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val musicVolume: Float = 0.8f,
    val sfxVolume: Float = 0.8f,
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
    data class MusicVolume(val level: Float) : SettingsEvent
    data class SfxVolume(val level: Float) : SettingsEvent
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

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            hasSave.value = repository.hasSave()
        }
        viewModelScope.launch {
            settings.musicEnabled.collect { _state.value = _state.value.copy(music = it) }
        }
        viewModelScope.launch {
            settings.sfxEnabled.collect { _state.value = _state.value.copy(sfx = it) }
        }
        viewModelScope.launch {
            settings.vibrationEnabled.collect {
                _state.value = _state.value.copy(vibration = it)
            }
        }
        viewModelScope.launch {
            settings.reduceMotion.collect {
                _state.value = _state.value.copy(reduceMotion = it)
            }
        }
        viewModelScope.launch {
            settings.graphicsQuality.collect {
                _state.value = _state.value.copy(graphics = it)
            }
        }
        viewModelScope.launch {
            settings.musicVolume.collect {
                _state.value = _state.value.copy(musicVolume = it)
            }
        }
        viewModelScope.launch {
            settings.sfxVolume.collect {
                _state.value = _state.value.copy(sfxVolume = it)
            }
        }
        viewModelScope.launch {
            dialogs.collect {
                _state.value = _state.value.copy(
                    showDeleteFirst = it.first,
                    showDeleteSecond = it.second,
                )
            }
        }
        viewModelScope.launch {
            hasSave.collect { _state.value = _state.value.copy(hasSave = it) }
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
            is SettingsEvent.MusicVolume -> viewModelScope.launch {
                settings.setMusicVolume(event.level)
            }
            is SettingsEvent.SfxVolume -> viewModelScope.launch {
                settings.setSfxVolume(event.level)
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
}
