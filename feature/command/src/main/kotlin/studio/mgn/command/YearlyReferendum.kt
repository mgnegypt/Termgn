package studio.mgn.command

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import studio.mgn.design.AssetPlaceholder
import studio.mgn.design.GameButton
import studio.mgn.design.MgnTheme
import studio.mgn.engine.EventResolution
import studio.mgn.engine.ReferendumResult
import studio.mgn.engine.YearlyReport

/** Yearly report: indicator summary, best/worst decision, gems earned. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearlyReportSheet(
    report: YearlyReport,
    best: EventResolution?,
    worst: EventResolution?,
    strings: YearlyStrings,
    onClose: () -> Unit,
) {
    val colors = MgnTheme.colors
    ModalBottomSheet(onDismissRequest = onClose) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "${strings.title} ${report.year}",
                style = MgnTheme.typography.titleLarge,
                color = colors.goldPrimary,
            )
            AssetPlaceholder(
                assetName = "art/yearly_report.png",
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "${strings.score}: ${"%.1f".format(report.nationScore)}",
                style = MgnTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            for (line in report.highlights.drop(1)) {
                Text(
                    text = line,
                    style = MgnTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
            Text(
                text = "${strings.gems}: +${report.gemsAwarded}",
                style = MgnTheme.typography.bodyMedium,
                color = colors.positive,
            )
            best?.let {
                Text(
                    text = "${strings.bestDecision}: ${it.event.title}",
                    style = MgnTheme.typography.bodyMedium,
                    color = colors.positive,
                )
            }
            worst?.let {
                Text(
                    text = "${strings.worstDecision}: ${it.event.title}",
                    style = MgnTheme.typography.bodyMedium,
                    color = colors.negative,
                )
            }
            GameButton(
                text = strings.close,
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

data class YearlyStrings(
    val title: String,
    val score: String,
    val gems: String,
    val bestDecision: String,
    val worstDecision: String,
    val close: String,
)

/** Referendum: animated legitimacy bar against the pass threshold. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferendumSheet(
    result: ReferendumResult,
    threshold: Double,
    animate: Boolean,
    strings: ReferendumStrings,
    onClose: () -> Unit,
) {
    val colors = MgnTheme.colors
    val shown by animateIntAsState(
        targetValue = result.legitimacy.toInt(),
        animationSpec = tween(if (animate) 1200 else 0),
        label = "referendum",
    )
    val displayed = if (animate) shown else result.legitimacy.toInt()
    ModalBottomSheet(onDismissRequest = onClose) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(20.dp),
        ) {
            Text(
                text = strings.title,
                style = MgnTheme.typography.titleLarge,
                color = colors.goldPrimary,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LinearProgressIndicator(
                    progress = { (displayed / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.weight(1f),
                    color = if (result.passed) colors.positive else colors.negative,
                    trackColor = colors.bgPanelElevated,
                )
                Text(
                    text = "$displayed / ${threshold.toInt()}",
                    style = MgnTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                )
            }
            Text(
                text = if (result.passed) strings.passed else strings.failed,
                style = MgnTheme.typography.titleMedium,
                color = if (result.passed) colors.positive else colors.negative,
            )
            GameButton(
                text = strings.close,
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

data class ReferendumStrings(
    val title: String,
    val passed: String,
    val failed: String,
    val close: String,
)

@Preview(name = "Referendum", widthDp = 600, heightDp = 400)
@Composable
private fun ReferendumPreview() {
    MgnTheme {
        ReferendumSheet(
            result = ReferendumResult(year = 2028, passed = true, legitimacy = 62.0),
            threshold = 45.0,
            animate = false,
            strings = ReferendumStrings(
                title = "الاستفتاء",
                passed = "تم تجديد الثقة",
                failed = "فشل الاستفتاء",
                close = "إغلاق",
            ),
            onClose = {},
        )
    }
}
