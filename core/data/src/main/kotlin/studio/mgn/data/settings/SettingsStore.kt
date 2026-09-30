package studio.mgn.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(
    name = "mgn_settings",
)

/** Graphics quality levels shown in Settings. */
enum class GraphicsQuality { LOW, MEDIUM, HIGH }

/** Typed access to player settings. Defaults are hearing-friendly and calm. */
class SettingsStore(private val context: Context) {

    private object Keys {
        val MUSIC = booleanPreferencesKey("music_enabled")
        val SFX = booleanPreferencesKey("sfx_enabled")
        val VIBRATION = booleanPreferencesKey("vibration_enabled")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val GRAPHICS = stringPreferencesKey("graphics_quality")
        val MUSIC_VOLUME = floatPreferencesKey("music_volume")
        val SFX_VOLUME = floatPreferencesKey("sfx_volume")
    }

    val musicEnabled: Flow<Boolean> = context.settingsStore.data
        .map { it[Keys.MUSIC] ?: true }
    val sfxEnabled: Flow<Boolean> = context.settingsStore.data
        .map { it[Keys.SFX] ?: true }
    val vibrationEnabled: Flow<Boolean> = context.settingsStore.data
        .map { it[Keys.VIBRATION] ?: true }
    val reduceMotion: Flow<Boolean> = context.settingsStore.data
        .map { it[Keys.REDUCE_MOTION] ?: false }
    val graphicsQuality: Flow<GraphicsQuality> = context.settingsStore.data
        .map {
            try {
                GraphicsQuality.valueOf(it[Keys.GRAPHICS] ?: GraphicsQuality.MEDIUM.name)
            } catch (_: IllegalArgumentException) {
                GraphicsQuality.MEDIUM
            }
        }
    val musicVolume: Flow<Float> = context.settingsStore.data
        .map { (it[Keys.MUSIC_VOLUME] ?: 0.8f).coerceIn(0f, 1f) }
    val sfxVolume: Flow<Float> = context.settingsStore.data
        .map { (it[Keys.SFX_VOLUME] ?: 0.8f).coerceIn(0f, 1f) }

    suspend fun setMusicEnabled(value: Boolean) {
        context.settingsStore.edit { it[Keys.MUSIC] = value }
    }

    suspend fun setSfxEnabled(value: Boolean) {
        context.settingsStore.edit { it[Keys.SFX] = value }
    }

    suspend fun setVibrationEnabled(value: Boolean) {
        context.settingsStore.edit { it[Keys.VIBRATION] = value }
    }

    suspend fun setReduceMotion(value: Boolean) {
        context.settingsStore.edit { it[Keys.REDUCE_MOTION] = value }
    }

    suspend fun setGraphicsQuality(value: GraphicsQuality) {
        context.settingsStore.edit { it[Keys.GRAPHICS] = value.name }
    }

    suspend fun setMusicVolume(value: Float) {
        context.settingsStore.edit { it[Keys.MUSIC_VOLUME] = value.coerceIn(0f, 1f) }
    }

    suspend fun setSfxVolume(value: Float) {
        context.settingsStore.edit { it[Keys.SFX_VOLUME] = value.coerceIn(0f, 1f) }
    }
}
