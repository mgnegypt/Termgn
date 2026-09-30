package studio.mgn.economy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import studio.mgn.design.BannerKind
import studio.mgn.design.GameButton
import studio.mgn.design.GoldFramePanel
import studio.mgn.design.IndicatorBar
import studio.mgn.design.MgnBanner
import studio.mgn.audio.AudioManager
import studio.mgn.audio.NoopAudioManager
import studio.mgn.audio.SoundKey
import studio.mgn.design.MgnTheme
import studio.mgn.model.GameState
import java.util.Locale

/** Economy screen: dials with live preview, investment, loans, chart. */
@Composable
fun EconomyScreen(
    viewModel: EconomyViewModel,
    strings: EconomyStrings,
    reduceMotion: Boolean,
    audio: AudioManager = NoopAudioManager(),
) {
    val ui by viewModel.state.collectAsState()
    val state = ui.state
    if (ui.isLoading || state == null) {
        Text(
            text = "…",
            color = MgnTheme.colors.textSecondary,
            modifier = Modifier.padding(24.dp),
        )
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = strings.title,
            style = MgnTheme.typography.displayLarge,
            color = MgnTheme.colors.goldPrimary,
        )
        DialsPanel(ui = ui, strings = strings, onEvent = viewModel::onEvent)
        InvestPanel(
            options = ui.investOptions,
            strings = strings,
            investError = ui.investError,
            audio = audio,
            onEvent = viewModel::onEvent,
        )
        LoanPanel(
            state = state,
            debtRatio = ui.debtRatio,
            financeInterest = ui.finance?.debtInterest ?: 0.0,
            strings = strings,
            audio = audio,
            onEvent = viewModel::onEvent,
        )
        ChartPanel(
            treasury = state.treasuryHistory,
            scores = state.scoreHistory,
            strings = strings,
        )
    }
}

@Composable
private fun DialsPanel(
    ui: EconomyUiState,
    strings: EconomyStrings,
    onEvent: (EconomyEvent) -> Unit,
) {
    val changed = ui.preview != null && ui.finance != null &&
        (ui.preview.totalIncome != ui.finance.totalIncome ||
            ui.preview.totalExpenses != ui.finance.totalExpenses)
    GoldFramePanel {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = strings.dialsTitle,
                style = MgnTheme.typography.titleMedium,
                color = MgnTheme.colors.goldPrimary,
            )
            DialRow(
                label = strings.taxLabel,
                value = ui.draftTax,
                suffix = strings.percentSuffix,
                onChange = { onEvent(EconomyEvent.Draft(tax = it, military = null, subsidy = null)) },
            )
            DialRow(
                label = strings.militaryLabel,
                value = ui.draftMilitary,
                suffix = strings.percentSuffix,
                onChange = {
                    onEvent(EconomyEvent.Draft(tax = null, military = it, subsidy = null))
                },
            )
            DialRow(
                label = strings.subsidyLabel,
                value = ui.draftSubsidy,
                suffix = strings.percentSuffix,
                onChange = {
                    onEvent(EconomyEvent.Draft(tax = null, military = null, subsidy = it))
                },
            )
            ui.preview?.let { preview ->
                Text(
                    text = "${strings.previewLabel}: " +
                        "${strings.incomeLabel} ${fmt(preview.totalIncome)} • " +
                        "${strings.expensesLabel} ${fmt(preview.totalExpenses)} • " +
                        "${strings.netLabel} ${fmtSigned(preview.net)}",
                    style = MgnTheme.typography.bodyMedium,
                    color = MgnTheme.colors.textSecondary,
                )
            }
            GameButton(
                text = strings.confirmDials,
                onClick = { onEvent(EconomyEvent.ConfirmDials) },
                enabled = changed,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DialRow(
    label: String,
    value: Float,
    suffix: String,
    onChange: (Float) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MgnTheme.typography.bodyMedium,
            color = MgnTheme.colors.textPrimary,
            modifier = Modifier.width(110.dp),
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 0f..1f,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${(value * 100).toInt()}$suffix",
            style = MgnTheme.typography.labelMedium,
            color = MgnTheme.colors.textSecondary,
            modifier = Modifier.width(48.dp),
        )
    }
}

@Composable
private fun InvestPanel(
    options: List<InvestOption>,
    strings: EconomyStrings,
    investError: Boolean,
    audio: AudioManager,
    onEvent: (EconomyEvent) -> Unit,
) {
    GoldFramePanel {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = strings.investTitle,
                style = MgnTheme.typography.titleMedium,
                color = MgnTheme.colors.goldPrimary,
            )
            if (investError) {
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    audio.play(SoundKey.ERROR)
                }
                MgnBanner(
                    message = strings.investBlocked,
                    kind = BannerKind.NEGATIVE,
                )
            }
            for (option in options) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IndicatorBar(
                        label = strings.keyLabels[option.sector] ?: option.sector,
                        value = option.level.toFloat(),
                        color = MgnTheme.colors.goldPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    GameButton(
                        text = "+3 (${fmt(option.cost)})",
                        onClick = {
                            audio.play(SoundKey.BUTTON)
                            onEvent(EconomyEvent.Invest(option.sector))
                        },
                        enabled = option.affordable,
                        primary = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoanPanel(
    state: GameState,
    debtRatio: Double,
    financeInterest: Double,
    strings: EconomyStrings,
    audio: AudioManager,
    onEvent: (EconomyEvent) -> Unit,
) {
    var amount by remember { mutableStateOf("50000") }
    GoldFramePanel {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = strings.loanTitle,
                style = MgnTheme.typography.titleMedium,
                color = MgnTheme.colors.goldPrimary,
            )
            Text(
                text = "${strings.debtLabel}: ${fmt(state.debt)} • " +
                    "${strings.interestLabel}: ${fmt(financeInterest)}",
                style = MgnTheme.typography.bodyMedium,
                color = MgnTheme.colors.textPrimary,
            )
            if (EconomyViewModel.warnDebt(debtRatio)) {
                MgnBanner(
                    message = strings.debtWarning,
                    kind = BannerKind.NEGATIVE,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter(Char::isDigit).take(9) },
                    placeholder = { Text(strings.loanAmountHint) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                GameButton(
                    text = strings.borrowAction,
                    onClick = {
                        audio.play(SoundKey.BUTTON)
                        onEvent(EconomyEvent.Loan(amount.toDoubleOrNull() ?: 0.0))
                    },
                    primary = false,
                )
                GameButton(
                    text = strings.repayAction,
                    onClick = {
                        audio.play(SoundKey.BUTTON)
                        onEvent(EconomyEvent.Repay(amount.toDoubleOrNull() ?: 0.0))
                    },
                    primary = false,
                )
            }
        }
    }
}

/** Line chart of treasury and score history (Canvas, last ≤30 turns). */
@Composable
fun TreasuryChart(
    treasury: List<Double>,
    scores: List<Double>,
    strings: EconomyStrings,
    modifier: Modifier = Modifier,
) {
    val colors = MgnTheme.colors
    Column(modifier = modifier) {
        Text(
            text = strings.chartTitle,
            style = MgnTheme.typography.titleMedium,
            color = colors.goldPrimary,
        )
        if (treasury.size < 2) {
            Text(
                text = strings.chartEmpty,
                style = MgnTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
            return
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
        ) {
            fun seriesPath(values: List<Double>, color: Color) {
                val min = values.min()
                val max = values.max()
                val span = (max - min).coerceAtLeast(1e-9)
                val path = Path()
                values.forEachIndexed { i, v ->
                    val x = size.width * i / (values.size - 1).coerceAtLeast(1)
                    val y = size.height - size.height * ((v - min) / span).toFloat() * 0.9f - 4f
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, color, style = Stroke(width = 3f))
                // End dot.
                val last = values.last()
                val lx = size.width
                val ly = size.height - size.height * ((last - min) / span).toFloat() * 0.9f - 4f
                drawCircle(color, 5f, Offset(lx - 5f, ly))
            }
            seriesPath(treasury, Color(0xFFF2B33D))
            if (scores.size >= 2) seriesPath(scores.map { it * 10000 }, Color(0xFF4ED6FF))
        }
    }
}

@Composable
private fun ChartPanel(
    treasury: List<Double>,
    scores: List<Double>,
    strings: EconomyStrings,
) {
    GoldFramePanel {
        TreasuryChart(treasury = treasury, scores = scores, strings = strings)
    }
}

private fun fmt(value: Double): String = "%,d".format(Locale.US, value.toLong())

private fun fmtSigned(value: Double): String {
    val sign = if (value >= 0) "+" else ""
    return "$sign${"%,d".format(Locale.US, value.toLong())}"
}

@Preview(name = "Economy", widthDp = 900, heightDp = 600)
@Composable
private fun EconomyPreview() {
    MgnTheme {
        Column(
            modifier = Modifier
                .background(MgnTheme.colors.bgBase)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            TreasuryChart(
                treasury = listOf(250000.0, 280000.0, 265000.0, 310000.0, 340000.0),
                scores = listOf(43.0, 45.5, 47.0, 49.2, 51.0),
                strings = previewEconomyStrings(),
            )
        }
    }
}

private fun previewEconomyStrings(): EconomyStrings = EconomyStrings(
    title = "الاقتصاد",
    dialsTitle = "السياسات",
    taxLabel = "الضرائب",
    militaryLabel = "العسكر",
    subsidyLabel = "الدعم",
    incomeLabel = "الدخل",
    expensesLabel = "الإنفاق",
    netLabel = "الصافي",
    previewLabel = "المعاينة",
    confirmDials = "تأكيد",
    investTitle = "الاستثمار",
    investBlocked = "لا يكفي الرصيد",
    loanTitle = "القروض",
    loanAmountHint = "المبلغ",
    borrowAction = "اقتراض",
    repayAction = "سداد",
    debtLabel = "الدين",
    interestLabel = "الفائدة",
    debtWarning = "الدين يقترب من حد الخطر",
    chartTitle = "تاريخ الخزينة",
    chartEmpty = "بعد أول دورين يظهر المخطط",
    percentSuffix = "٪",
    keyLabels = emptyMap(),
)
