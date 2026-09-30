package studio.mgn.data

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import studio.mgn.data.settings.GraphicsQuality
import studio.mgn.data.settings.SettingsStore
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SettingsStoreTest {

    private fun store(): SettingsStore {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        return SettingsStore(context)
    }

    @Test
    fun `settings round trip`() = runTest {
        val store = store()
        store.setMusicEnabled(false)
        store.setSfxEnabled(false)
        store.setVibrationEnabled(false)
        store.setReduceMotion(true)
        store.setGraphicsQuality(GraphicsQuality.HIGH)
        store.setMusicVolume(0.3f)
        store.setSfxVolume(0.4f)

        assertEquals(false, store.musicEnabled.first())
        assertEquals(false, store.sfxEnabled.first())
        assertEquals(false, store.vibrationEnabled.first())
        assertEquals(true, store.reduceMotion.first())
        assertEquals(GraphicsQuality.HIGH, store.graphicsQuality.first())
        assertEquals(0.3f, store.musicVolume.first())
        assertEquals(0.4f, store.sfxVolume.first())
    }
}
