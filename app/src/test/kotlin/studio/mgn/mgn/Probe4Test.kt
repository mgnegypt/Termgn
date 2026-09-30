package studio.mgn.mgn

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import studio.mgn.command.CityScene
import studio.mgn.command.CommandTopBar
import studio.mgn.command.EndTurnBar
import studio.mgn.command.EventsPanel
import studio.mgn.command.IndicatorsPanel
import studio.mgn.command.WorldMap
import studio.mgn.command.previewPack
import studio.mgn.command.previewState
import studio.mgn.command.previewStrings
import studio.mgn.design.MgnTheme

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class Probe4Test {
    @get:Rule
    val compose = createComposeRule()

    private fun topbar() = compose.setContent {
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
                CityScene(
                    state = previewState(),
                    positions = previewPack().landmarkPositions,
                    signLabel = { it },
                    progressOf = { null },
                    animate = false,
                    onSignClick = {},
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                )
            }
        }
    }

    @Test
    fun `combo topbar city`() {
        topbar()
        compose.onNodeWithText("المجد").assertExists()
    }

    @Test
    fun `combo topbar map`() {
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
                    WorldMap(
                        countries = previewState().countries.values.toList(),
                        playerName = "المجد",
                        selectedId = null,
                        onSelect = {},
                    )
                }
            }
        }
        compose.onNodeWithText("المجد").assertExists()
    }

    @Test
    fun `combo topbar events indicators endturn`() {
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
                    EventsPanel(
                        history = emptyList(),
                        pendingCount = 0,
                        imageKeyOf = { "" },
                        strings = previewStrings(),
                        onOpenDecisions = {},
                    )
                    IndicatorsPanel(
                        state = previewState(),
                        previous = null,
                        strings = previewStrings(),
                        animate = false,
                        onIndicatorClick = {},
                    )
                    EndTurnBar(
                        processing = false,
                        pendingCount = 0,
                        strings = previewStrings(),
                        reduceMotion = true,
                        onEndTurn = {},
                    )
                }
            }
        }
        compose.onNodeWithText("المجد").assertExists()
    }
}
