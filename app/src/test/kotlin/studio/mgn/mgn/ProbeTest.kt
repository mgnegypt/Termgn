package studio.mgn.mgn

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import studio.mgn.command.CommandTopBar
import studio.mgn.command.previewState
import studio.mgn.command.previewStrings
import studio.mgn.design.AssetPlaceholder
import studio.mgn.design.MgnTheme

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProbeTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `probe plain text`() {
        compose.setContent { Text("hello probe") }
        compose.onNodeWithText("hello probe").assertExists()
    }

    @Test
    fun `probe themed arabic text`() {
        compose.setContent { MgnTheme { Text("المجد") } }
        compose.onNodeWithText("المجد").assertExists()
    }

    @Test
    fun `probe topbar`() {
        compose.setContent {
            MgnTheme {
                CommandTopBar(
                    state = previewState(),
                    previous = null,
                    strings = previewStrings(),
                    animate = false,
                    onCellClick = {},
                    onEndTurn = {},
                )
            }
        }
        compose.onNodeWithText("المجد").assertExists()
    }

    @Test
    fun `probe asset placeholder`() {
        compose.setContent {
            MgnTheme { AssetPlaceholder(assetName = "x", label = "lbl") }
        }
        compose.onNodeWithText("lbl").assertExists()
    }
}
