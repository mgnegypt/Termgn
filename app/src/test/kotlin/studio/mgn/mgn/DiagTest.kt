package studio.mgn.mgn

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import studio.mgn.command.CommandContent
import studio.mgn.command.CommandUiState
import studio.mgn.command.previewPack
import studio.mgn.command.previewState
import studio.mgn.command.previewStrings
import studio.mgn.design.MgnTheme

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DiagTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `print dashboard tree`() {
        compose.setContent {
            MgnTheme {
                CommandContent(
                    ui = CommandUiState(isLoading = false, state = previewState()),
                    content = previewPack(),
                    strings = previewStrings(),
                    reduceMotion = true,
                    onEvent = {},
                )
            }
        }
        compose.onRoot().printToLog("DIAG")
    }
}
