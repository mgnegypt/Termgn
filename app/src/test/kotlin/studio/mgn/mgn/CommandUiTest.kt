package studio.mgn.mgn

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import studio.mgn.mgn.splash.SplashScreen
import studio.mgn.mgn.splash.SplashViewModel
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
        compose.onAllNodesWithText("المجد")[0].assertExists()
        compose.onNodeWithText("إنهاء الدور").assertExists()
        compose.onNodeWithText("مؤشرات الدولة").assertExists()
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
        compose.onNodeWithText("لا قرارات معلقة").assertExists()
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
                        turnBadge = { turn -> "د$turn" },
                    ),
                    onBack = {},
                )
            }
        }
        compose.onNodeWithText("لا سجل بعد").assertExists()
        compose.onNodeWithText("الكل").assertExists()
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
        compose.onAllNodesWithText("خيار أول")[1].assertExists()
        compose.onAllNodesWithText("خيار أول")[1].performClick()
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
            compose.onAllNodesWithText("إعلان قيام الدولة")
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithText("إعلان قيام الدولة")[1].performClick()
        compose.waitUntil(15000) {
            try {
                compose.onNodeWithText("إنهاء الدور").assertExists()
                true
            } catch (_: AssertionError) {
                false
            }
        }
        compose.onAllNodesWithText("المجد")[0].assertExists()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AccessibilityUiTest {
    @get:Rule
    val compose = createComposeRule()

    private fun strings(): CommandStrings = previewStrings()

    @Test
    fun `dashboard survives 130 percent font scale`() {
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = LocalDensity.current.density,
                    fontScale = 1.3f,
                ),
            ) {
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
        }
        compose.onAllNodesWithText("المجد")[0].assertExists()
        compose.onNodeWithText("إنهاء الدور").assertExists()
    }

    @Test
    fun `splash error state offers retry`() {
        val vm = SplashViewModel(
            loadContent = { throw IllegalStateException("no content") },
            minDelayMs = 0,
        )
        compose.setContent {
            MgnTheme {
                SplashScreen(viewModel = vm, onReady = {}, reduceMotion = true)
            }
        }
        compose.waitUntil(8000) {
            compose.onAllNodesWithText("إعادة المحاولة")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
