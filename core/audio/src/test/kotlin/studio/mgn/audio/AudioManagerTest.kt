package studio.mgn.audio

import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AudioManagerTest {

    @Test
    fun `noop records calls and clamps volumes`() {
        val audio = NoopAudioManager()
        audio.play(SoundKey.BUTTON)
        audio.setMusic(MusicKey.MENU)
        audio.vibrate(HapticStrength.HEAVY)
        audio.setMusicVolume(2f)
        audio.setSfxVolume(-1f)
        assertEquals(SoundKey.BUTTON, audio.lastSound)
        assertEquals(MusicKey.MENU, audio.lastMusic)
        assertEquals(HapticStrength.HEAVY, audio.lastHaptic)
        assertEquals(1f, audio.musicVolume)
        assertEquals(0f, audio.sfxVolume)
    }

    @Test
    fun `every key maps to an asset file name`() {
        assertTrue(SoundKey.entries.all { it.file.isNotBlank() })
        assertTrue(MusicKey.entries.all { it.file.isNotBlank() })
        assertEquals(
            SoundKey.entries.size,
            SoundKey.entries.map { it.file }.toSet().size,
            "duplicate sfx file",
        )
        assertEquals(
            MusicKey.entries.size,
            MusicKey.entries.map { it.file }.toSet().size,
            "duplicate music file",
        )
    }

    @Test
    fun `android manager stays silent without asset files`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val audio = AndroidAudioManager(context)
        // No assets ship yet: every call must be a silent no-op, never a crash.
        audio.play(SoundKey.BUTTON)
        audio.setMusic(MusicKey.MENU)
        audio.setMusic(null)
        audio.vibrate(HapticStrength.LIGHT)
        audio.setVibrationEnabled(false)
        audio.vibrate(HapticStrength.HEAVY)
        audio.setMusicVolume(0.5f)
        audio.setSfxVolume(0.5f)
        audio.release()
        // Reaching here without an exception is the assertion.
    }
}
