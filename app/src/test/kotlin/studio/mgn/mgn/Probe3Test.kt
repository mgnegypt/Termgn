package studio.mgn.mgn

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
import studio.mgn.command.EventsPanel
import studio.mgn.command.IndicatorsPanel
import studio.mgn.command.MissionsPanel
import studio.mgn.command.WorldMap
import studio.mgn.command.previewPack
import studio.mgn.command.previewState
import studio.mgn.command.previewStrings
import studio.mgn.design.MgnTheme
import studio.mgn.engine.MissionEngine

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class Probe3Test {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `probe city`() {
        compose.setContent {
            MgnTheme {
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
        compose.onNodeWithText("city_night").assertExists()
    }

    @Test
    fun `probe map`() {
        compose.setContent {
            MgnTheme {
                WorldMap(
                    countries = previewState().countries.values.toList(),
                    playerName = "المجد",
                    selectedId = null,
                    onSelect = {},
                )
            }
        }
        compose.onNodeWithText("المجد").assertExists()
    }

    @Test
    fun `probe events`() {
        compose.setContent {
            MgnTheme {
                EventsPanel(
                    history = emptyList(),
                    pendingCount = 0,
                    imageKeyOf = { "" },
                    strings = previewStrings(),
                    onOpenDecisions = {},
                )
            }
        }
        compose.onNodeWithText("آخر الأحداث").assertExists()
    }

    @Test
    fun `probe indicators missions`() {
        val pack = previewPack()
        compose.setContent {
            MgnTheme {
                IndicatorsPanel(
                    state = previewState(),
                    previous = null,
                    strings = previewStrings(),
                    animate = false,
                    onIndicatorClick = {},
                )
                MissionsPanel(
                    missions = MissionEngine.activeMissions(previewState(), pack.missions),
                    completedCount = 0,
                    strings = previewStrings(),
                    onShowAll = {},
                )
            }
        }
        compose.onNodeWithText("مؤشرات الدولة").assertExists()
    }
}
