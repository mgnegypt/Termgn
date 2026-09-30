package studio.mgn.setup

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import studio.mgn.design.GameButton
import studio.mgn.design.GoldFramePanel
import studio.mgn.design.MgnBanner
import studio.mgn.design.BannerKind
import studio.mgn.audio.AudioManager
import studio.mgn.audio.NoopAudioManager
import studio.mgn.audio.SoundKey
import studio.mgn.design.MgnTheme
import studio.mgn.model.TurnLength

/**
 * Illustrated state-founding wizard. Every step animates; the flag preview
 * is drawn live on Canvas until art assets are delivered.
 */
@Composable
fun SetupWizardScreen(
    viewModel: SetupViewModel,
    onDone: () -> Unit,
    onBack: () -> Unit,
    reduceMotion: Boolean = false,
    strings: SetupStrings,
    audio: AudioManager = NoopAudioManager(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.done.collectLatest { onDone() }
    }

    SetupWizardContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        reduceMotion = reduceMotion,
        strings = strings,
        audio = audio,
    )
}

/** All user-facing wizard text; the app layer builds it from resources. */
data class SetupStrings(
    val title: String = "تأسيس الدولة",
    val next: String = "التالي",
    val back: String = "السابق",
    val confirm: String = "إعلان قيام الدولة",
    val stepName: String = "اسم الدولة",
    val nameHint: String = "مثال: المجد",
    val stepTitle: String = "لقب الحاكم",
    val rulerHint: String = "مثال: رئيس",
    val stepFlag: String = "الراية",
    val stepTurn: String = "طول الدور",
    val turnDay: String = "يوم",
    val turnWeek: String = "أسبوع",
    val turnMonth: String = "شهر",
    val stepAxes: String = "التوجهات",
    val axisEconomic: String = "اقتصادي",
    val axisSocial: String = "اجتماعي",
    val axisForeign: String = "خارجي",
    val stepReview: String = "إعلان قيام الدولة",
    val reviewLine: (String, String) -> String = { name, title -> "دولة $name — $title" },
    val emptyError: String = "أدخل قيمة أولًا للمتابعة",
)

/** Reads wizard strings from :app resources; :feature:setup owns no strings. */

@Composable
fun SetupWizardContent(
    state: SetupUiState,
    onEvent: (SetupEvent) -> Unit,
    onBack: () -> Unit,
    reduceMotion: Boolean,
    strings: SetupStrings,
    audio: AudioManager = NoopAudioManager(),
) {
    val slide = if (reduceMotion) 0 else 60
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = strings.title,
            style = MgnTheme.typography.displayLarge,
            color = MgnTheme.colors.goldPrimary,
        )
        StepDots(current = state.step.ordinal, total = SetupStep.entries.size)
        AnimatedContent(
            targetState = state.step,
            transitionSpec = {
                (fadeIn() + slideInHorizontally { slide }) togetherWith
                    (fadeOut() + slideOutHorizontally { -slide })
            },
            label = "setup-step",
        ) { step ->
            when (step) {
                SetupStep.NAME -> NameStep(state, onEvent, strings)
                SetupStep.TITLE -> TitleStep(state, onEvent, strings)
                SetupStep.FLAG -> FlagStep(state, onEvent, strings)
                SetupStep.TURN -> TurnStep(state, onEvent, strings)
                SetupStep.AXES -> AxesStep(state, onEvent, strings)
                SetupStep.REVIEW -> ReviewStep(state, strings)
            }
        }
        if (state.showError) {
            MgnBanner(message = strings.emptyError, kind = BannerKind.NEGATIVE)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            GameButton(
                text = strings.back,
                onClick = {
                    audio.play(SoundKey.BUTTON)
                    if (state.step == SetupStep.NAME) onBack() else onEvent(SetupEvent.Back)
                },
                primary = false,
                reduceMotion = reduceMotion,
                modifier = Modifier.weight(1f),
            )
            if (state.step == SetupStep.REVIEW) {
                GameButton(
                    text = if (state.isCreating) "…" else strings.confirm,
                    onClick = {
                        audio.play(SoundKey.REWARD)
                        onEvent(SetupEvent.Confirm)
                    },
                    enabled = !state.isCreating,
                    reduceMotion = reduceMotion,
                    modifier = Modifier.weight(2f),
                )
            } else {
                GameButton(
                    text = strings.next,
                    onClick = {
                        audio.play(SoundKey.BUTTON)
                        onEvent(SetupEvent.Next)
                    },
                    reduceMotion = reduceMotion,
                    modifier = Modifier.weight(2f),
                )
            }
        }
    }
}

@Composable
private fun StepDots(current: Int, total: Int) {
    val colors = MgnTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(total) { i ->
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        if (i == current) colors.goldPrimary else colors.bgPanelElevated,
                    ),
            )
        }
    }
}

@Composable
private fun NameStep(
    state: SetupUiState,
    onEvent: (SetupEvent) -> Unit,
    strings: SetupStrings,
) {
    GoldFramePanel {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(strings.stepName, style = MgnTheme.typography.titleLarge)
            OutlinedTextField(
                value = state.countryName,
                onValueChange = { onEvent(SetupEvent.NameChanged(it)) },
                placeholder = { Text(strings.nameHint) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TitleStep(
    state: SetupUiState,
    onEvent: (SetupEvent) -> Unit,
    strings: SetupStrings,
) {
    GoldFramePanel {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(strings.stepTitle, style = MgnTheme.typography.titleLarge)
            OutlinedTextField(
                value = state.rulerTitle,
                onValueChange = { onEvent(SetupEvent.TitleChanged(it)) },
                placeholder = { Text(strings.rulerHint) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun FlagStep(
    state: SetupUiState,
    onEvent: (SetupEvent) -> Unit,
    strings: SetupStrings,
) {
    GoldFramePanel {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(strings.stepFlag, style = MgnTheme.typography.titleLarge)
            FlagPreview(
                preset = FLAG_PRESETS[state.flagPreset],
                modifier = Modifier.size(180.dp, 120.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FLAG_PRESETS.forEachIndexed { i, _ ->
                    FilterChip(
                        selected = state.flagPreset == i,
                        onClick = { onEvent(SetupEvent.FlagSelected(i)) },
                        modifier = Modifier.heightIn(min = 48.dp),
                        label = { Text("راية ${i + 1}") },
                    )
                }
            }
        }
    }
}

/** Live flag preview drawn on Canvas: field + emblem + symbol. */
@Composable
fun FlagPreview(preset: FlagPreset, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val primary = Color(preset.primary)
        val secondary = Color(preset.secondary)
        drawRect(primary)
        drawRect(
            color = secondary,
            topLeft = Offset(0f, size.height * 0.42f),
            size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.16f),
        )
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.height * 0.22f
        when (preset.symbolId) {
            "crescent_star" -> {
                drawCircle(secondary, r, c)
                drawCircle(primary, r * 0.85f, c + Offset(r * 0.35f, -r * 0.15f))
                drawCircle(secondary, r * 0.22f, c + Offset(r * 0.9f, -r * 0.55f))
            }
            "eagle" -> {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(c.x - r, c.y + r * 0.8f)
                    lineTo(c.x, c.y - r)
                    lineTo(c.x + r, c.y + r * 0.8f)
                    close()
                }
                drawPath(path, secondary)
            }
            else -> {
                drawRect(
                    color = secondary,
                    topLeft = Offset(c.x - r * 0.35f, c.y - r),
                    size = androidx.compose.ui.geometry.Size(r * 0.7f, r * 2f),
                )
            }
        }
    }
}

@Composable
private fun TurnStep(
    state: SetupUiState,
    onEvent: (SetupEvent) -> Unit,
    strings: SetupStrings,
) {
    GoldFramePanel {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(strings.stepTurn, style = MgnTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TurnChip(strings.turnDay, TurnLength.DAY, state, onEvent)
                TurnChip(strings.turnWeek, TurnLength.WEEK, state, onEvent)
                TurnChip(strings.turnMonth, TurnLength.MONTH, state, onEvent)
            }
        }
    }
}

@Composable
private fun TurnChip(
    label: String,
    length: TurnLength,
    state: SetupUiState,
    onEvent: (SetupEvent) -> Unit,
) {
    FilterChip(
        selected = state.turnLength == length,
        onClick = { onEvent(SetupEvent.TurnSelected(length)) },
        modifier = Modifier.heightIn(min = 48.dp),
        label = { Text(label) },
    )
}

@Composable
private fun AxesStep(
    state: SetupUiState,
    onEvent: (SetupEvent) -> Unit,
    strings: SetupStrings,
) {
    GoldFramePanel {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(strings.stepAxes, style = MgnTheme.typography.titleLarge)
            AxisSlider(
                label = strings.axisEconomic,
                value = state.axisEconomic,
                onChange = { onEvent(SetupEvent.AxisChanged(SetupAxis.ECONOMIC, it)) },
            )
            AxisSlider(
                label = strings.axisSocial,
                value = state.axisSocial,
                onChange = { onEvent(SetupEvent.AxisChanged(SetupAxis.SOCIAL, it)) },
            )
            AxisSlider(
                label = strings.axisForeign,
                value = state.axisForeign,
                onChange = { onEvent(SetupEvent.AxisChanged(SetupAxis.FOREIGN, it)) },
            )
        }
    }
}

@Composable
private fun AxisSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Column {
        Text(label, style = MgnTheme.typography.bodyMedium)
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = -100f..100f,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ReviewStep(state: SetupUiState, strings: SetupStrings) {
    val preset = FLAG_PRESETS[state.flagPreset]
    GoldFramePanel(elevated = true) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(strings.stepReview, style = MgnTheme.typography.displayLarge)
            FlagPreview(preset = preset, modifier = Modifier.size(200.dp, 130.dp))
            Text(
                text = strings.reviewLine(state.countryName, state.rulerTitle),
                style = MgnTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Preview(name = "Setup", widthDp = 900, heightDp = 400)
@Composable
private fun SetupPreview() {
    MgnTheme {
        SetupWizardContent(
            state = SetupUiState(countryName = "المجد", step = SetupStep.FLAG),
            onEvent = {},
            onBack = {},
            reduceMotion = true,
            strings = SetupStrings(),
        )
    }
}
