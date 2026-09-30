package studio.mgn.mgn

import androidx.compose.runtime.staticCompositionLocalOf
import studio.mgn.audio.AudioManager
import studio.mgn.audio.NoopAudioManager

/** App-wide audio access. Defaults to silent; MainActivity provides the real one. */
val LocalAudio = staticCompositionLocalOf<AudioManager> { NoopAudioManager() }
