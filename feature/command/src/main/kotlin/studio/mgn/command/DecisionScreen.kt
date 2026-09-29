package studio.mgn.command

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import studio.mgn.design.GameButton
import studio.mgn.design.GoldFramePanel
import studio.mgn.design.MgnBanner
import studio.mgn.design.BannerKind
import studio.mgn.design.MgnTheme
import studio.mgn.engine.EventResolution
import studio.mgn.model.EventChoice
import studio.mgn.model.GameEvent

/**
 * Full-scene decision screen: pending queue, current event with choice
 * cards + effect previews, gem reroll, animated result.
 */
@Composable
fun DecisionScreen(
    pending: List<GameEvent>,
    gems: Int,
    rerollCost: Int,
    choiceStates: List<ChoiceUiState>,
    lastResolution: EventResolution?,
    strings: CommandStrings,
    onChoose: (eventId: String, choiceIndex: Int) -> Unit,
    onReroll: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MgnTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${strings.decisionsTitle} (${pending.size})",
                style = MgnTheme.typography.displayLarge,
                color = colors.goldPrimary,
            )
            GameButton(
                text = strings.continueLabel,
                onClick = onClose,
                primary = false,
            )
        }
        val current = pending.firstOrNull()
        if (current == null) {
            MgnBanner(
                message = strings.decisionsEmpty,
                kind = BannerKind.POSITIVE,
            )
        } else {
            EventScene(
                event = current,
                choiceStates = choiceStates,
                strings = strings,
                onChoose = { index -> onChoose(current.id, index) },
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GameButton(
                    text = "${strings.reroll} ($rerollCost ${strings.gemsSuffix})",
                    onClick = onReroll,
                    enabled = gems >= rerollCost,
                    primary = false,
                )
                if (gems < rerollCost) {
                    Text(
                        text = strings.notEnoughGems,
                        style = MgnTheme.typography.labelMedium,
                        color = colors.negative,
                    )
                }
            }
            if (pending.size > 1) {
                Text(
                    text = pending.drop(1).joinToString(" • ") { it.title },
                    style = MgnTheme.typography.labelMedium,
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        AnimatedVisibility(
            visible = lastResolution != null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            lastResolution?.let { resolution ->
                GoldFramePanel(elevated = true) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = strings.chooseResult,
                            style = MgnTheme.typography.labelMedium,
                            color = colors.goldPrimary,
                        )
                        Text(
                            text = resolution.choice.resultText,
                            style = MgnTheme.typography.bodyMedium,
                            color = colors.textPrimary,
                        )
                        for ((key, value) in resolution.appliedEffects) {
                            Text(
                                text = "${strings.keyLabels[key] ?: key}: " +
                                    "${deltaText(value)}",
                                style = MgnTheme.typography.labelMedium,
                                color = deltaColor(value) ?: colors.textSecondary,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun EventScene(
    event: GameEvent,
    choiceStates: List<ChoiceUiState>,
    strings: CommandStrings,
    onChoose: (Int) -> Unit,
) {
    val colors = MgnTheme.colors
    GoldFramePanel(elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AssetPlaceholder(
                assetName = if (event.imageKey.isBlank()) {
                    "event/default"
                } else {
                    event.imageKey
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .size(160.dp),
            )
            Text(
                text = event.title,
                style = MgnTheme.typography.titleLarge,
                color = colors.textPrimary,
            )
            Text(
                text = event.description,
                style = MgnTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
            event.choices.forEachIndexed { index, choice ->
                val ui = choiceStates.getOrNull(index)
                ChoiceCard(
                    choice = choice,
                    enabled = ui?.selectable ?: false,
                    effectiveCost = ui?.effectiveCostCash ?: 0.0,
                    strings = strings,
                    gemsSuffix = strings.gemsSuffix,
                    onClick = { onChoose(index) },
                )
            }
        }
    }
}

@Composable
private fun ChoiceCard(
    choice: EventChoice,
    enabled: Boolean,
    effectiveCost: Double,
    strings: CommandStrings,
    gemsSuffix: String,
    onClick: () -> Unit,
) {
    val colors = MgnTheme.colors
    GoldFramePanel {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = choice.label,
                style = MgnTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            if (choice.costCash > 0 || choice.costGems > 0) {
                Text(
                    text = "${strings.costLabel}: " +
                        "${"%,d".format(java.util.Locale.US, effectiveCost.toLong())}" +
                        if (choice.costGems > 0) {
                            " + ${choice.costGems} $gemsSuffix"
                        } else {
                            ""
                        },
                    style = MgnTheme.typography.labelMedium,
                    color = colors.textSecondary,
                )
            }
            if (choice.stateEffects.isNotEmpty()) {
                Text(
                    text = strings.effectsPreview,
                    style = MgnTheme.typography.labelMedium,
                    color = colors.goldPrimary,
                )
                for ((key, value) in choice.stateEffects) {
                    Text(
                        text = "${strings.keyLabels[key] ?: key}: ${deltaText(value)}",
                        style = MgnTheme.typography.labelMedium,
                        color = deltaColor(value) ?: colors.textSecondary,
                    )
                }
            }
            GameButton(
                text = choice.label,
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(name = "Decisions", widthDp = 900, heightDp = 500)
@Composable
private fun DecisionsPreview() {
    MgnTheme {
        val pack = previewPack()
        DecisionScreen(
            pending = pack.events.take(2),
            gems = 25,
            rerollCost = 5,
            choiceStates = pack.events.first().choices.map {
                ChoiceUiState(selectable = true, effectiveCostCash = 1000.0)
            },
            lastResolution = null,
            strings = previewStrings(),
            onChoose = { _, _ -> },
            onReroll = {},
            onClose = {},
        )
    }
}
