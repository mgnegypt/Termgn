package studio.mgn.diplomacy

import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import studio.mgn.command.WorldMap
import studio.mgn.command.color
import studio.mgn.command.relationStatusOf
import studio.mgn.design.GameButton
import studio.mgn.design.GoldFramePanel
import studio.mgn.audio.AudioManager
import studio.mgn.audio.HapticStrength
import studio.mgn.audio.NoopAudioManager
import studio.mgn.audio.SoundKey
import studio.mgn.design.MgnTheme
import studio.mgn.model.DiplomacyState
import studio.mgn.model.TreatyType
import studio.mgn.model.WarStance

/**
 * Full-screen diplomacy: zoomable world map, selected-country card with
 * relation meter, treaties, war stance and a confirmed war declaration.
 */
@Composable
fun DiplomacyScreen(
    viewModel: DiplomacyViewModel,
    strings: DiplomacyStrings,
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
    Row(modifier = Modifier.fillMaxSize()) {
        var scale by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
        val transform = rememberTransformableState { zoom, pan, _ ->
            scale = (scale * zoom).coerceIn(1f, 3f)
            offset += pan
        }
        WorldMap(
            countries = state.countries.values.toList(),
            playerName = state.countryName,
            selectedId = ui.selectedId,
            onSelect = { viewModel.onEvent(DiplomacyEvent.Select(it)) },
            modifier = Modifier
                .weight(1.4f)
                .fillMaxHeight()
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                .transformable(transform)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                ),
        )
        CountryPanel(
            ui = ui,
            strings = strings,
            audio = audio,
            onEvent = viewModel::onEvent,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
        )
    }
}

@Composable
private fun CountryPanel(
    ui: studio.mgn.diplomacy.DiplomacyUiState,
    strings: DiplomacyStrings,
    audio: AudioManager,
    onEvent: (DiplomacyEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val row = ui.rows.firstOrNull { it.id == ui.selectedId }
    if (row == null) {
        GoldFramePanel(modifier = modifier) {
            Text(
                text = "—",
                style = MgnTheme.typography.bodyMedium,
                color = MgnTheme.colors.textSecondary,
            )
        }
        return
    }
    var confirmWar by remember(row.id) { mutableStateOf(false) }
    GoldFramePanel(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = row.nameAr,
                style = MgnTheme.typography.titleLarge,
                color = MgnTheme.colors.goldPrimary,
            )
            if (row.atWar) {
                Text(
                    text = strings.atWarLabel,
                    style = MgnTheme.typography.bodyMedium,
                    color = MgnTheme.colors.negative,
                )
            }
            if (row.sanctioned) {
                Text(
                    text = strings.sanctionedLabel,
                    style = MgnTheme.typography.bodyMedium,
                    color = MgnTheme.colors.negative,
                )
            }
            Text(
                text = "${strings.relationLabel}: ${row.relation.toInt()}",
                style = MgnTheme.typography.bodyMedium,
                color = MgnTheme.colors.textPrimary,
            )
            LinearProgressIndicator(
                progress = { ((row.relation + 100) / 200).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = relationStatusOf(
                    DiplomacyState(
                        countryId = row.id,
                        nameAr = row.nameAr,
                        behavior = studio.mgn.model.AiBehavior.NEUTRAL,
                        relation = row.relation,
                        atWar = row.atWar,
                        treaties = row.treaties.toSet(),
                    ),
                ).color(),
                trackColor = MgnTheme.colors.bgPanelElevated,
            )
            Text(
                text = if (row.treaties.isEmpty()) {
                    "${strings.treatiesLabel}: —"
                } else {
                    "${strings.treatiesLabel}: " +
                        row.treaties.joinToString("، ") { treatyName(it, strings) }
                },
                style = MgnTheme.typography.bodyMedium,
                color = MgnTheme.colors.textSecondary,
            )
            for (type in TreatyType.entries) {
                if (type in row.treaties) {
                    GameButton(
                        text = "${strings.breakTreaty}: ${treatyName(type, strings)}",
                        onClick = { onEvent(DiplomacyEvent.BreakTreaty(row.id, type)) },
                        primary = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else if (!row.atWar) {
                    GameButton(
                        text = "${strings.signTreaty}: ${treatyName(type, strings)}",
                        onClick = {
                            audio.play(SoundKey.REWARD)
                            onEvent(DiplomacyEvent.SignTreaty(row.id, type))
                        },
                        enabled = row.signable[type] == true,
                        primary = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            if (row.atWar) {
                StancePicker(
                    current = row.stance,
                    strings = strings,
                    onPick = { onEvent(DiplomacyEvent.SetStance(row.id, it)) },
                )
            } else {
                GameButton(
                    text = strings.declareWar,
                    onClick = { confirmWar = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
    if (confirmWar) {
        AlertDialog(
            onDismissRequest = { confirmWar = false },
            title = { Text(strings.warConfirmTitle) },
            text = {
                Text(
                    strings.warConfirmMessage + "\n" +
                        "${strings.warCostNote}: ${ui.warCostPerTurn.toLong()}",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmWar = false
                        audio.play(SoundKey.DANGER)
                        audio.vibrate(HapticStrength.MEDIUM)
                        onEvent(DiplomacyEvent.DeclareWar(row.id))
                    },
                ) {
                    Text(strings.warConfirmOk)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmWar = false }) {
                    Text(strings.cancel)
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StancePicker(
    current: WarStance,
    strings: DiplomacyStrings,
    onPick: (WarStance) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(
            text = strings.stanceLabel,
            style = MgnTheme.typography.labelMedium,
            color = MgnTheme.colors.textSecondary,
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
        ) {
            OutlinedTextField(
                value = stanceName(current, strings),
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(strings.stanceDefensive) },
                    onClick = {
                        expanded = false
                        onPick(WarStance.DEFENSIVE)
                    },
                )
                DropdownMenuItem(
                    text = { Text(strings.stanceOffensive) },
                    onClick = {
                        expanded = false
                        onPick(WarStance.OFFENSIVE)
                    },
                )
                DropdownMenuItem(
                    text = { Text(strings.stanceNegotiate) },
                    onClick = {
                        expanded = false
                        onPick(WarStance.NEGOTIATE)
                    },
                )
            }
        }
    }
}

@Preview(name = "Diplomacy", widthDp = 900, heightDp = 400)
@Composable
private fun DiplomacyPreview() {
    MgnTheme {
        Text(
            text = "Diplomacy preview",
            color = MgnTheme.colors.textSecondary,
            modifier = Modifier.padding(24.dp),
        )
    }
}
