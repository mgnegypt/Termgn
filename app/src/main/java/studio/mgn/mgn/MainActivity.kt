package studio.mgn.mgn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.map
import studio.mgn.design.MgnTheme
import studio.mgn.mgn.nav.MgnNav

/**
 * PLAN 2 entry point. Reads reduce-motion once per composition and
 * auto-saves the active session onStop.
 */
class MainActivity : ComponentActivity() {

    private val app: MgnApp get() = application as MgnApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MgnTheme {
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalAudio provides app.audio,
                ) {
                    val reduceMotion by app.settings.reduceMotion.collectAsState(
                        initial = false,
                    )
                    MgnNav(app = app, reduceMotion = reduceMotion)
                }
            }
        }
    }

    override fun onPause() {
        app.audio.setMusic(null)
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        app.audio.setMusic(app.musicMood)
    }

    override fun onStop() {
        app.saveNow()
        super.onStop()
    }
}
