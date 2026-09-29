package studio.mgn.command

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import studio.mgn.design.GameButton
import studio.mgn.design.MgnTheme
import studio.mgn.engine.TurnFinance
import studio.mgn.model.DiplomacyState
import studio.mgn.model.GameState
import studio.mgn.model.LandmarkDef

/** Detail sheet for one top-bar cell: value, last-turn delta, breakdown. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndicatorSheet(
    key: String,
    state: GameState,
    previous: GameState?,
    finance: TurnFinance?,
    strings: CommandStrings,
    onDismiss: () -> Unit,
) {
    val colors = MgnTheme.colors
    val value = when (key) {
        "legitimacy" -> state.legitimacy
        else -> state.readKey(key)
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(20.dp),
        ) {
            Text(
                text = strings.keyLabels[key] ?: key,
                style = MgnTheme.typography.titleLarge,
                color = colors.goldPrimary,
            )
            Text(
                text = formatCellValue(key, value),
                style = MgnTheme.typography.displayLarge,
                color = colors.textPrimary,
            )
            deltaText(deltaOf(key, state, previous))?.let {
                Text(
                    text = it,
                    style = MgnTheme.typography.bodyMedium,
                    color = deltaColor(deltaOf(key, state, previous))
                        ?: colors.textSecondary,
                )
            }
            if (key == "treasuryCash" && finance != null) {
                for ((label, amount) in finance.incomeBreakdown) {
                    SheetLine(label = label, value = "+${"%,d".format(amount.toLong())}")
                }
                for ((label, amount) in finance.expenseBreakdown) {
                    SheetLine(label = label, value = "-${"%,d".format(amount.toLong())}")
                }
            }
        }
    }
}

@Composable
private fun SheetLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MgnTheme.typography.bodyMedium,
            color = MgnTheme.colors.textSecondary,
        )
        Text(
            text = value,
            style = MgnTheme.typography.bodyMedium,
            color = MgnTheme.colors.textPrimary,
        )
    }
}

/** Landmark sheet: status, effects, requirements, build/rush actions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandmarkSheet(
    def: LandmarkDef?,
    isDecor: Boolean,
    decorNote: String,
    isBuilt: Boolean,
    turnsRemaining: Int?,
    buildTurns: Int,
    canBuild: Boolean,
    canRush: Boolean,
    rushCost: Int,
    strings: CommandStrings,
    onBuild: () -> Unit,
    onRush: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MgnTheme.colors
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(20.dp),
        ) {
            Text(
                text = def?.nameAr ?: "",
                style = MgnTheme.typography.titleLarge,
                color = colors.goldPrimary,
            )
            if (isDecor) {
                Text(
                    text = decorNote,
                    style = MgnTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            } else {
                Text(
                    text = def?.descriptionAr ?: "",
                    style = MgnTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
                Text(
                    text = if (isBuilt) {
                        strings.builtLabel
                    } else if (turnsRemaining != null) {
                        "${strings.underConstructionLabel}: " +
                            "$turnsRemaining/${buildTurns} ${strings.remainingTurns}"
                    } else {
                        "${strings.costLabel}: " +
                            "${"%,d".format((def?.costCash ?: 0.0).toLong())}"
                    },
                    style = MgnTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                )
                if (!isBuilt && turnsRemaining == null) {
                    GameButton(
                        text = strings.buildNow,
                        onClick = onBuild,
                        enabled = canBuild,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (turnsRemaining != null) {
                    GameButton(
                        text = "${strings.rushLabel} ($rushCost ${strings.gemsSuffix})",
                        onClick = onRush,
                        enabled = canRush,
                        primary = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/** Country card sheet: relation, treaties. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountrySheet(
    country: DiplomacyState,
    strings: CommandStrings,
    treatyLabel: (String) -> String,
    onDismiss: () -> Unit,
) {
    val colors = MgnTheme.colors
    val status = relationStatusOf(country)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(20.dp),
        ) {
            Text(
                text = country.nameAr,
                style = MgnTheme.typography.titleLarge,
                color = status.color(),
            )
            Text(
                text = "${strings.relationLabel}: " +
                    "${"%.0f".format(java.util.Locale.US, country.relation)}",
                style = MgnTheme.typography.bodyMedium,
                color = colors.textPrimary,
            )
            val treaties = country.treaties.map { treatyLabel(it.name) }
            Text(
                text = if (treaties.isEmpty()) {
                    "${strings.treatiesLabel}: ${strings.noTreaties}"
                } else {
                    "${strings.treatiesLabel}: ${treaties.joinToString("، ")}"
                },
                style = MgnTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
        }
    }
}
