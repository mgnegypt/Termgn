package studio.mgn.development

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import studio.mgn.design.GameButton
import studio.mgn.design.GoldFramePanel
import studio.mgn.design.MgnTheme
import studio.mgn.model.LandmarkDef

/** Landmarks catalogue with available/building/built tabs. */
@Composable
fun DevelopmentScreen(
    viewModel: DevelopmentViewModel,
    strings: DevelopmentStrings,
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
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = strings.title,
            style = MgnTheme.typography.displayLarge,
            color = MgnTheme.colors.goldPrimary,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp),
        )
        PrimaryTabRow(selectedTabIndex = ui.tab.ordinal) {
            Tab(
                selected = ui.tab == LandmarkTab.AVAILABLE,
                onClick = { viewModel.onEvent(DevelopmentEvent.Tab(LandmarkTab.AVAILABLE)) },
                text = { Text(strings.tabAvailable) },
            )
            Tab(
                selected = ui.tab == LandmarkTab.BUILDING,
                onClick = { viewModel.onEvent(DevelopmentEvent.Tab(LandmarkTab.BUILDING)) },
                text = {
                    Text(
                        "${strings.tabBuilding} (${state.underConstruction.size})",
                    )
                },
            )
            Tab(
                selected = ui.tab == LandmarkTab.BUILT,
                onClick = { viewModel.onEvent(DevelopmentEvent.Tab(LandmarkTab.BUILT)) },
                text = { Text("${strings.tabBuilt} (${state.builtLandmarks.size})") },
            )
        }
        when (ui.tab) {
            LandmarkTab.AVAILABLE -> AvailableList(
                ui = ui,
                strings = strings,
                onBuild = { viewModel.onEvent(DevelopmentEvent.Build(it)) },
            )
            LandmarkTab.BUILDING -> BuildingList(
                ui = ui,
                strings = strings,
                onRush = { viewModel.onEvent(DevelopmentEvent.Rush(it)) },
            )
            LandmarkTab.BUILT -> BuiltList(ui = ui, strings = strings)
        }
    }
}

@Composable
private fun AvailableList(
    ui: DevelopmentUiState,
    strings: DevelopmentStrings,
    onBuild: (String) -> Unit,
) {
    if (ui.available.isEmpty()) {
        EmptyNote(strings.emptyAvailable)
        return
    }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(12.dp),
    ) {
        items(ui.available, key = { it.id }) { def ->
            LandmarkCard(
                def = def,
                strings = strings,
                action = {
                    GameButton(
                        text = "${strings.buildNow} (${fmt(def.costCash)})",
                        onClick = { onBuild(def.id) },
                    )
                },
            )
        }
    }
}

@Composable
private fun BuildingList(
    ui: DevelopmentUiState,
    strings: DevelopmentStrings,
    onRush: (String) -> Unit,
) {
    val queue = ui.state?.underConstruction ?: emptyList()
    if (queue.isEmpty()) {
        EmptyNote(strings.emptyBuilding)
        return
    }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(12.dp),
    ) {
        items(queue, key = { it.id }) { build ->
            val def = ui.catalog[build.id]
            GoldFramePanel {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = def?.nameAr ?: build.id,
                        style = MgnTheme.typography.titleMedium,
                        color = MgnTheme.colors.textPrimary,
                    )
                    val total = (def?.buildTurns ?: build.turnsRemaining).coerceAtLeast(1)
                    LinearProgressIndicator(
                        progress = {
                            (1f - build.turnsRemaining / total.toFloat()).coerceIn(0f, 1f)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        color = MgnTheme.colors.goldPrimary,
                        trackColor = MgnTheme.colors.bgPanelElevated,
                    )
                    Text(
                        text = "${build.turnsRemaining} ${strings.remainingTurns}",
                        style = MgnTheme.typography.labelMedium,
                        color = MgnTheme.colors.textSecondary,
                    )
                    val affordRush = (ui.state?.gems ?: 0) >= ui.rushCostPerTurn
                    GameButton(
                        text = "${strings.rushLabel} " +
                            "(${ui.rushCostPerTurn} ${strings.gemsSuffix})",
                        onClick = { onRush(build.id) },
                        enabled = affordRush,
                        primary = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun BuiltList(ui: DevelopmentUiState, strings: DevelopmentStrings) {
    val built = ui.state?.builtLandmarks ?: emptyList()
    if (built.isEmpty()) {
        EmptyNote(strings.emptyBuilt)
        return
    }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(12.dp),
    ) {
        items(built, key = { it.id }) { landmark ->
            GoldFramePanel {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = ui.catalog[landmark.id]?.nameAr ?: landmark.id,
                        style = MgnTheme.typography.titleMedium,
                        color = MgnTheme.colors.textPrimary,
                    )
                    Text(
                        text = strings.builtLabel,
                        style = MgnTheme.typography.labelMedium,
                        color = MgnTheme.colors.positive,
                    )
                }
            }
        }
    }
}

@Composable
private fun LandmarkCard(
    def: LandmarkDef,
    strings: DevelopmentStrings,
    action: @Composable () -> Unit,
) {
    GoldFramePanel {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = def.nameAr,
                style = MgnTheme.typography.titleMedium,
                color = MgnTheme.colors.textPrimary,
            )
            Text(
                text = def.descriptionAr,
                style = MgnTheme.typography.bodyMedium,
                color = MgnTheme.colors.textSecondary,
            )
            Text(
                text = "${strings.costLabel}: ${fmt(def.costCash)} • " +
                    "${strings.durationLabel}: ${def.buildTurns}",
                style = MgnTheme.typography.labelMedium,
                color = MgnTheme.colors.textSecondary,
            )
            if (def.requirements.isNotEmpty()) {
                Text(
                    text = "${strings.requiresLabel}: " +
                        def.requirements.entries.joinToString("، ") { (key, value) ->
                            "${strings.keyLabels[key] ?: key} ${value.toInt()}"
                        },
                    style = MgnTheme.typography.labelMedium,
                    color = MgnTheme.colors.textSecondary,
                )
            }
            if (def.onCompleteEffects.isNotEmpty()) {
                Text(
                    text = def.onCompleteEffects.entries.joinToString(" • ") { (key, value) ->
                        "${strings.keyLabels[key] ?: key} ${if (value > 0) "+" else ""}$value"
                    },
                    style = MgnTheme.typography.labelMedium,
                    color = MgnTheme.colors.positive,
                )
            }
            action()
        }
    }
}

@Composable
private fun EmptyNote(text: String) {
    Text(
        text = text,
        style = MgnTheme.typography.bodyMedium,
        color = MgnTheme.colors.textSecondary,
        modifier = Modifier.padding(24.dp),
    )
}

private fun fmt(value: Double): String =
    "%,d".format(java.util.Locale.US, value.toLong())

@Preview(name = "Development", widthDp = 900, heightDp = 500)
@Composable
private fun DevelopmentPreview() {
    MgnTheme {
        Text(
            text = "Development preview",
            color = MgnTheme.colors.textSecondary,
            modifier = Modifier.padding(24.dp),
        )
    }
}
