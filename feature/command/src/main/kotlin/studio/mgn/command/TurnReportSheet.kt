package studio.mgn.command

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import studio.mgn.design.GameButton
import studio.mgn.design.MgnTheme
import studio.mgn.engine.TurnReport
import studio.mgn.model.GameState

/**
 * End-of-turn report: treasury movement, indicator deltas, new events,
 * achievements and missions. Shown once per turn, then auto-saves.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TurnReportSheet(
    report: TurnReport,
    before: GameState?,
    strings: CommandStrings,
    onClose: () -> Unit,
) {
    val colors = MgnTheme.colors
    val after = report.state
    ModalBottomSheet(onDismissRequest = onClose) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "${strings.reportTitle} ${report.turnNumber}",
                style = MgnTheme.typography.titleLarge,
                color = colors.goldPrimary,
            )
            Text(
                text = "${strings.reportTreasury}: " +
                    "${formatCellValue("treasuryCash", after.treasuryCash)} " +
                    "(${deltaText(deltaOf("treasuryCash", after, before)) ?: "—"})",
                style = MgnTheme.typography.bodyMedium,
                color = colors.textPrimary,
            )
            for (key in listOf("economy", "publicSatisfaction", "legitimacy")) {
                val value = when (key) {
                    "legitimacy" -> after.legitimacy
                    else -> after.readKey(key)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = strings.keyLabels[key] ?: key,
                        style = MgnTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                    Text(
                        text = "${formatCellValue(key, value)} " +
                            "(${deltaText(deltaOf(key, after, before)) ?: "—"})",
                        style = MgnTheme.typography.bodyMedium,
                        color = colors.textPrimary,
                    )
                }
            }
            if (report.completedLandmarks.isNotEmpty()) {
                Text(
                    text = report.completedLandmarks.joinToString("، ") { it.nameAr },
                    style = MgnTheme.typography.bodyMedium,
                    color = colors.positive,
                )
            }
            if (report.pendingEvents.isNotEmpty()) {
                Text(
                    text = "${strings.reportNewEvents}: ${report.pendingEvents.size}",
                    style = MgnTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                )
            }
            if (report.newAchievements.isNotEmpty()) {
                Text(
                    text = "${strings.reportAchievements}: " +
                        report.newAchievements.joinToString("، ") { it.titleAr },
                    style = MgnTheme.typography.bodyMedium,
                    color = colors.positive,
                )
            }
            if (report.newMissions.isNotEmpty()) {
                Text(
                    text = "${strings.reportMissions}: " +
                        report.newMissions.joinToString("، ") { it.titleAr },
                    style = MgnTheme.typography.bodyMedium,
                    color = colors.positive,
                )
            }
            report.yearlyReport?.let {
                Text(
                    text = "${it.highlights.firstOrNull() ?: ""} (+${it.gemsAwarded})",
                    style = MgnTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
            GameButton(
                text = strings.reportClose,
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
