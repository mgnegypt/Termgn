package studio.mgn.command

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import studio.mgn.design.GoldFramePanel
import studio.mgn.design.MgnTheme
import studio.mgn.model.GameState

/** The 7 top-bar cells in fixed order. */
val TOP_CELLS = listOf(
    "treasuryCash",
    "population",
    "legitimacy",
    "militarySecurity",
    "economy",
    "technology",
    "culture",
)

/** The 8 state indicators in fixed order. */
val STATE_INDICATORS = listOf(
    "economy" to "GREEN",
    "militarySecurity" to "RED",
    "education" to "BLUE",
    "health" to "GREEN",
    "industry" to "ORANGE",
    "agriculture" to "YELLOW",
    "technology" to "CYAN",
    "culture" to "PURPLE",
)

private val DISPLAY_LOCALE = java.util.Locale.US

/** Formats a raw engine value for display (treasury/population grouped). */
fun formatCellValue(key: String, value: Double): String = when (key) {
    "treasuryCash", "population" -> "%,d".format(DISPLAY_LOCALE, value.toLong())
    "legitimacy" -> "%.0f".format(DISPLAY_LOCALE, value)
    else -> "%.0f".format(DISPLAY_LOCALE, value)
}

fun deltaOf(key: String, now: GameState, before: GameState?): Double? {
    if (before == null) return null
    val delta = when (key) {
        "legitimacy" -> now.legitimacy - before.legitimacy
        else -> try {
            now.readKey(key) - before.readKey(key)
        } catch (_: IllegalArgumentException) {
            return null
        }
    }
    return delta
}

fun deltaColor(delta: Double?): Color? = when {
    delta == null || delta == 0.0 -> null
    delta > 0 -> Color(0xFF3DD68C)
    else -> Color(0xFFFF5A4E)
}

fun deltaText(delta: Double?): String? {
    if (delta == null || delta == 0.0) return null
    val sign = if (delta > 0) "+" else ""
    return if (kotlin.math.abs(delta) >= 100) {
        "$sign${"%,d".format(DISPLAY_LOCALE, delta.toLong())}"
    } else {
        "$sign${"%.1f".format(DISPLAY_LOCALE, delta)}"
    }
}

/** Treasury/population count-up; static when [animate] is false. */
@Composable
fun CountUpNumber(value: Long, animate: Boolean, modifier: Modifier = Modifier) {
    val shown by animateIntAsState(
        targetValue = value.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt(),
        animationSpec = tween(500),
        label = "countup",
    )
    Text(
        text = "%,d".format(DISPLAY_LOCALE, if (animate) shown else value),
        style = MgnTheme.typography.titleMedium,
        color = MgnTheme.colors.textPrimary,
        modifier = modifier,
    )
}

/** One tappable top-bar cell with count-up value and colored delta. */
@Composable
fun TopCell(
    label: String,
    key: String,
    value: Double,
    delta: Double?,
    animate: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MgnTheme.colors
    Column(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = 48.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MgnTheme.typography.labelMedium,
            color = colors.textSecondary,
        )
        if (key == "treasuryCash" || key == "population") {
            CountUpNumber(value = value.toLong(), animate = animate)
        } else {
            Text(
                text = formatCellValue(key, value),
                style = MgnTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
        }
        deltaText(delta)?.let {
            Text(
                text = it,
                style = MgnTheme.typography.labelMedium,
                color = deltaColor(delta) ?: colors.textSecondary,
            )
        }
    }
}

/** Top bar: badge+name+ruler+level, 7 cells, date/turn block. */
@Composable
fun CommandTopBar(
    state: GameState,
    previous: GameState?,
    strings: CommandStrings,
    animate: Boolean,
    onCellClick: (String) -> Unit,
    onEndTurn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MgnTheme.colors
    GoldFramePanel(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1.2f)) {
                Text(
                    text = state.countryName,
                    style = MgnTheme.typography.titleLarge,
                    color = colors.goldPrimary,
                )
                Text(
                    text = "${strings.rulerTitlePrefix} ${state.rulerTitle} • " +
                        "${strings.level} ${state.rulerLevel}",
                    style = MgnTheme.typography.labelMedium,
                    color = colors.textSecondary,
                )
                LinearProgressIndicator(
                    progress = { state.rulerLevelProgress },
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .padding(top = 4.dp),
                    color = colors.goldPrimary,
                    trackColor = colors.bgPanelElevated,
                )
            }
            Row(
                modifier = Modifier.weight(4f),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                for (key in TOP_CELLS) {
                    val value = when (key) {
                        "legitimacy" -> state.legitimacy
                        else -> state.readKey(key)
                    }
                    TopCell(
                        label = strings.keyLabels[key] ?: key,
                        key = key,
                        value = value,
                        delta = deltaOf(key, state, previous),
                        animate = animate,
                        onClick = { onCellClick(key) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "%d/%d".format(
                        DISPLAY_LOCALE,
                        state.inGameDate.monthValue,
                        state.inGameDate.year,
                    ),
                    style = MgnTheme.typography.titleMedium,
                    color = colors.textPrimary,
                )
                Text(
                    text = "${strings.turn} ${state.turnNumber}",
                    style = MgnTheme.typography.labelMedium,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

@Preview(name = "TopBar", widthDp = 900, heightDp = 200)
@Composable
private fun TopBarPreview() {
    MgnTheme {
        CommandTopBar(
            state = previewState(),
            previous = null,
            strings = previewStrings(),
            animate = false,
            onCellClick = {},
            onEndTurn = {},
        )
    }
}
