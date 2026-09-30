package studio.mgn.command

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import studio.mgn.design.GoldFramePanel
import studio.mgn.design.IndicatorBar
import studio.mgn.design.MgnTheme
import studio.mgn.design.IndicatorColor
import studio.mgn.design.color
import studio.mgn.engine.MissionProgress
import studio.mgn.model.GameState

fun indicatorColorOf(name: String): IndicatorColor = when (name) {
    "GREEN" -> IndicatorColor.GREEN
    "BLUE" -> IndicatorColor.BLUE
    "PURPLE" -> IndicatorColor.PURPLE
    "RED" -> IndicatorColor.RED
    "ORANGE" -> IndicatorColor.ORANGE
    "CYAN" -> IndicatorColor.CYAN
    else -> IndicatorColor.YELLOW
}

/** Eight state indicators with animated bars, values and deltas. */
@Composable
fun IndicatorsPanel(
    state: GameState,
    previous: GameState?,
    strings: CommandStrings,
    animate: Boolean,
    onIndicatorClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MgnTheme.colors
    GoldFramePanel(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = strings.indicatorsTitle,
                style = MgnTheme.typography.titleMedium,
                color = colors.goldPrimary,
            )
            for ((key, colorName) in STATE_INDICATORS) {
                val value = state.readKey(key).toFloat()
                val delta = deltaOf(key, state, previous)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        .clickable { onIndicatorClick(key) }
                        .padding(vertical = 2.dp),
                ) {
                    IndicatorBar(
                        label = strings.keyLabels[key] ?: key,
                        value = value,
                        color = indicatorColorOf(colorName).color(),
                        modifier = Modifier.weight(1f),
                    )
                    deltaText(delta)?.let {
                        Text(
                            text = it,
                            style = MgnTheme.typography.labelMedium,
                            color = deltaColor(delta) ?: colors.textSecondary,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Up to 3 active missions with progress bars. */
@Composable
fun MissionsPanel(
    missions: List<MissionProgress>,
    completedCount: Int,
    strings: CommandStrings,
    onShowAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MgnTheme.colors
    GoldFramePanel(modifier = modifier.animateContentSize()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${strings.missionsTitle} ($completedCount)",
                    style = MgnTheme.typography.titleMedium,
                    color = colors.goldPrimary,
                )
                Text(
                    text = strings.showAll,
                    style = MgnTheme.typography.labelMedium,
                    color = colors.goldPrimary,
                    modifier = Modifier.clickable(onClick = onShowAll),
                )
            }
            for (progress in missions) {
                val fraction = (
                    progress.current / progress.mission.goalValue.coerceAtLeast(1e-9)
                    ).toFloat().coerceIn(0f, 1f)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = progress.mission.titleAr,
                            style = MgnTheme.typography.bodyMedium,
                            color = colors.textPrimary,
                        )
                        Text(
                            text = "${formatMissionValue(progress.current)}" +
                                "/${formatMissionValue(progress.mission.goalValue)}",
                            style = MgnTheme.typography.labelMedium,
                            color = colors.textSecondary,
                        )
                    }
                    IndicatorBar(
                        label = progress.mission.descriptionAr,
                        value = fraction * 100f,
                        color = colors.goldPrimary,
                    )
                }
            }
        }
    }
}

private fun formatMissionValue(value: Double): String =
    if (value >= 1000) {
        "%,d".format(java.util.Locale.US, value.toLong())
    } else {
        "%.0f".format(java.util.Locale.US, value)
    }

@Preview(name = "Indicators", widthDp = 320, heightDp = 500)
@Composable
private fun IndicatorsPreview() {
    MgnTheme {
        IndicatorsPanel(
            state = previewState(),
            previous = null,
            strings = previewStrings(),
            animate = false,
            onIndicatorClick = {},
        )
    }
}
