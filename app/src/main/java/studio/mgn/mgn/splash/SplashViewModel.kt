package studio.mgn.mgn.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import studio.mgn.content.ContentPack

/** MVI state for the splash screen. */
data class SplashUiState(
    val phase: SplashPhase = SplashPhase.LOADING,
    val eventCount: Int = 0,
    val error: String? = null,
)

enum class SplashPhase { LOADING, READY, ERROR }

/** Loads and validates content off the main thread, then signals readiness. */
class SplashViewModel(
    private val loadContent: suspend () -> ContentPack,
    private val minDelayMs: Long = 800,
) : ViewModel() {

    private val _state = MutableStateFlow(SplashUiState())
    val state: StateFlow<SplashUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val started = System.currentTimeMillis()
            try {
                val pack = loadContent()
                val elapsed = System.currentTimeMillis() - started
                if (elapsed < minDelayMs) delay(minDelayMs - elapsed)
                _state.value = SplashUiState(
                    phase = SplashPhase.READY,
                    eventCount = pack.events.size,
                )
            } catch (t: Throwable) {
                _state.value = SplashUiState(
                    phase = SplashPhase.ERROR,
                    error = t.message ?: "load_failed",
                )
            }
        }
    }

    fun retry() {
        _state.value = SplashUiState()
        viewModelScope.launch {
            try {
                val pack = loadContent()
                _state.value = SplashUiState(
                    phase = SplashPhase.READY,
                    eventCount = pack.events.size,
                )
            } catch (t: Throwable) {
                _state.value = SplashUiState(
                    phase = SplashPhase.ERROR,
                    error = t.message ?: "load_failed",
                )
            }
        }
    }
}
