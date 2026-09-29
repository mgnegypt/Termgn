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

        first(store.musicEnabled).let { assertEquals(false, it) }
        first(store.sfxEnabled).let { assertEquals(false, it) }
        first(store.vibrationEnabled).let { assertEquals(false, it) }
        first(store.reduceMotion).let { assertEquals(true, it) }
        first(store.graphicsQuality).let {
            assertEquals(GraphicsQuality.HIGH, it)
        }
    }
}
