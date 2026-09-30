package studio.mgn.mgn

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import studio.mgn.command.CommandTopBar
import studio.mgn.command.previewState
import studio.mgn.command.previewStrings
import studio.mgn.design.MgnTheme

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class Probe2Test {
    @get:Rule
    val compose = createComposeRule()

    @Test
    @OptIn(ExperimentalMaterial3Api::class)
    fun `probe topbar plus bottomsheetscaffold`() {
        compose.setContent {
            MgnTheme {
                Column(Modifier.fillMaxSize()) {
                    CommandTopBar(
                        state = previewState(),
                        previous = null,
                        strings = previewStrings(),
                        animate = false,
                        onCellClick = {},
                        onEndTurn = {},
                    )
                    BottomSheetScaffold(
                        sheetContent = { Text("sheet") },
                        sheetPeekHeight = 140.dp,
                    ) {
                        Text("body")
                    }
                }
            }
        }
        compose.onNodeWithText("المجد").assertExists()
    }

    @Test
    fun `probe topbar plus plain sibling`() {
        compose.setContent {
            MgnTheme {
                Column(Modifier.fillMaxSize()) {
                    CommandTopBar(
                        state = previewState(),
                        previous = null,
                        strings = previewStrings(),
                        animate = false,
                        onCellClick = {},
                        onEndTurn = {},
                    )
                    Text("sibling")
                }
            }
        }
        compose.onNodeWithText("المجد").assertExists()
    }
}
