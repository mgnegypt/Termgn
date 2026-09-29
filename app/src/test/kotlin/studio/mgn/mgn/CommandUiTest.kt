package studio.mgn.mgn

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import studio.mgn.command.CommandContent
import studio.mgn.command.CommandEvent
import studio.mgn.command.CommandStrings
import studio.mgn.command.CommandUiState
import studio.mgn.command.DecisionScreen
import studio.mgn.command.HistoryScreen
import studio.mgn.command.HistoryStrings
import studio.mgn.command.previewPack
import studio.mgn.command.previewState
import studio.mgn.command.previewStrings
import studio.mgn.design.MgnTheme

/** Compose UI tests: navigation between screens and empty states. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CommandUiTest {

    @get:Rule
    val compose = createComposeRule()

    private fun strings(): CommandStrings = previewStrings()

    @Test
    fun `dashboard renders state values`() {
        compose.setContent {
            MgnTheme {
                CommandContent(
                    ui = CommandUiState(isLoading = false, state = previewState()),
                    content = previewPack(),
                    strings = strings(),
                    reduceMotion = true,
                    onEvent = {},
                )
            }
        }
        compose.onNodeWithText("المجد").assertIsDisplayed()
        compose.onNodeWithText("إنهاء الدور").assertIsDisplayed()
        compose.onNodeWithText("القيادة").assertIsDisplayed()
    }

    @Test
    fun `empty decisions show the empty banner`() {
        compose.setContent {
            MgnTheme {
                DecisionScreen(
                    pending = emptyList(),
                    gems = 0,
                    rerollCost = 5,
                    choiceStates = emptyList(),
                    lastResolution = null,
                    strings = strings(),
                    onChoose = { _, _ -> },
                    onReroll = {},
                    onClose = {},
                )
            }
        }
        compose.onNodeWithText("لا قرارات معلقة").assertIsDisplayed()
    }

    @Test
    fun `empty history shows the empty note`() {
        compose.setContent {
            MgnTheme {
                HistoryScreen(
                    history = emptyList(),
                    strings = HistoryStrings(
                        title = "السجل",
                        empty = "لا سجل بعد",
                        turnLabel = "الدور",
                        back = "رجوع",
                        filterAll = "الكل",
                        filterEconomy = "اقتصاد",
                        filterDiplomacy = "دبلوماسية",
                        filterEvents = "أحداث",
                    ),
                    onBack = {},
                )
            }
        }
        compose.onNodeWithText("لا سجل بعد").assertIsDisplayed()
        compose.onNodeWithText("الكل").assertIsDisplayed()
    }

    @Test
    fun `decision choices are clickable`() {
        var chosen: Pair<String, Int>? = null
        val pack = previewPack()
        compose.setContent {
            MgnTheme {
                DecisionScreen(
                    pending = pack.events,
                    gems = 25,
                    rerollCost = 5,
                    choiceStates = pack.events.first().choices.map {
                        studio.mgn.command.ChoiceUiState(
                            selectable = true,
                            effectiveCostCash = 0.0,
                        )
                    },
                    lastResolution = null,
                    strings = strings(),
                    onChoose = { id, index -> chosen = id to index },
                    onReroll = {},
                    onClose = {},
                )
            }
        }
        compose.onNodeWithText("خيار أول").assertIsDisplayed()
        compose.onNodeWithText("خيار أول").performClick()
        assert(chosen == "preview_event" to 0) { "choice not routed: $chosen" }
    }

    @Test
    fun `full flow splash to menu to setup to dashboard`() {
        val app = ApplicationProvider.getApplicationContext<MgnApp>()
        compose.setContent {
            MgnTheme {
                studio.mgn.mgn.nav.MgnNav(app = app, reduceMotion = true)
            }
        }
        // Splash loads content, then the menu appears.
        compose.waitUntil(15000) {
            compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isEmpty() &&
                try {
                    compose.onNodeWithText("لعبة جديدة").assertExists()
                    true
                } catch (_: AssertionError) {
                    false
                }
        }
        compose.onNodeWithText("لعبة جديدة").performClick()
        compose.waitUntil(10000) {
            try {
                compose.onNodeWithText("اسم الدولة").assertExists()
                true
            } catch (_: AssertionError) {
                false
            }
        }
        // Wizard: name -> title -> flag -> turn -> axes -> review -> confirm.
        compose.onNode(hasSetTextAction()).performTextInput("المجد")
        compose.onNodeWithText("التالي").performClick()
        compose.waitUntil(10000) {
            try {
                compose.onNodeWithText("لقب الحاكم").assertExists()
                true
            } catch (_: AssertionError) {
                false
            }
        }
        compose.onNode(hasSetTextAction()).performTextInput("رئيس")
        repeat(4) {
            compose.onNodeWithText("التالي").performClick()
        }
        compose.waitUntil(10000) {
            try {
                compose.onNodeWithText("إعلان قيام الدولة").assertExists()
                true
            } catch (_: AssertionError) {
                false
            }
        }
        compose.onNodeWithText("إعلان قيام الدولة").performClick()
        compose.waitUntil(15000) {
            try {
                compose.onNodeWithText("إنهاء الدور").assertExists()
                true
            } catch (_: AssertionError) {
                false
            }
        }
        compose.onNodeWithText("المجد").assertIsDisplayed()
    }
}
