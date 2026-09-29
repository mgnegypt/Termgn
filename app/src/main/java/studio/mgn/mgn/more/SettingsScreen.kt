package studio.mgn.mgn.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import studio.mgn.data.settings.GraphicsQuality
import studio.mgn.design.GameButton
import studio.mgn.design.GoldFramePanel
import studio.mgn.design.MgnTheme
import studio.mgn.mgn.BuildConfig
import studio.mgn.mgn.R

/** Full settings screen: audio, haptics, motion, graphics, save, about. */
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val state by viewModel.state.collectAsState()
    SettingsContent(state = state, onEvent = viewModel::onEvent)
}

@Composable
fun SettingsContent(
    state: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MgnTheme.typography.displayLarge,
            color = MgnTheme.colors.goldPrimary,
        )
        GoldFramePanel {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SettingRow(
                    label = stringResource(R.string.settings_music),
                    checked = state.music,
                    onChange = { onEvent(SettingsEvent.Music(it)) },
                )
                SettingRow(
                    label = stringResource(R.string.settings_sfx),
                    checked = state.sfx,
                    onChange = { onEvent(SettingsEvent.Sfx(it)) },
                )
                SettingRow(
                    label = stringResource(R.string.settings_vibration),
                    checked = state.vibration,
                    onChange = { onEvent(SettingsEvent.Vibration(it)) },
                )
                SettingRow(
                    label = stringResource(R.string.settings_reduce_motion),
                    checked = state.reduceMotion,
                    onChange = { onEvent(SettingsEvent.ReduceMotion(it)) },
                )
            }
        }
        GoldFramePanel {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.settings_graphics),
                    style = MgnTheme.typography.titleMedium,
                    color = MgnTheme.colors.textPrimary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GraphicsChip(
                        label = stringResource(R.string.settings_graphics_low),
                        selected = state.graphics == GraphicsQuality.LOW,
                        onClick = { onEvent(SettingsEvent.Graphics(GraphicsQuality.LOW)) },
                    )
                    GraphicsChip(
                        label = stringResource(R.string.settings_graphics_medium),
                        selected = state.graphics == GraphicsQuality.MEDIUM,
                        onClick = { onEvent(SettingsEvent.Graphics(GraphicsQuality.MEDIUM)) },
                    )
                    GraphicsChip(
                        label = stringResource(R.string.settings_graphics_high),
                        selected = state.graphics == GraphicsQuality.HIGH,
                        onClick = { onEvent(SettingsEvent.Graphics(GraphicsQuality.HIGH)) },
                    )
                }
            }
        }
        if (state.hasSave) {
            GameButton(
                text = stringResource(R.string.settings_delete_save),
                onClick = { onEvent(SettingsEvent.DeleteRequested) },
                primary = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        GoldFramePanel {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.settings_about_title),
                    style = MgnTheme.typography.titleMedium,
                    color = MgnTheme.colors.goldPrimary,
                )
                Text(
                    text = stringResource(R.string.settings_about_body),
                    style = MgnTheme.typography.bodyMedium,
                    color = MgnTheme.colors.textSecondary,
                )
                Text(
                    text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                    style = MgnTheme.typography.labelMedium,
                    color = MgnTheme.colors.textSecondary,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (state.showDeleteFirst) {
        AlertDialog(
            onDismissRequest = { onEvent(SettingsEvent.DeleteDismissed) },
            title = { Text(stringResource(R.string.settings_delete_title)) },
            text = { Text(stringResource(R.string.settings_delete_message)) },
            confirmButton = {
                TextButton(onClick = { onEvent(SettingsEvent.DeleteConfirmedFirst) }) {
                    Text(stringResource(R.string.settings_delete_next))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(SettingsEvent.DeleteDismissed) }) {
                    Text(stringResource(R.string.menu_cancel))
                }
            },
        )
    }
    if (state.showDeleteSecond) {
        AlertDialog(
            onDismissRequest = { onEvent(SettingsEvent.DeleteDismissed) },
            title = { Text(stringResource(R.string.settings_delete_title)) },
            text = { Text(stringResource(R.string.settings_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = { onEvent(SettingsEvent.DeleteConfirmedSecond) }) {
                    Text(stringResource(R.string.settings_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(SettingsEvent.DeleteDismissed) }) {
                    Text(stringResource(R.string.menu_cancel))
                }
            },
        )
    }
}

@Composable
private fun SettingRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MgnTheme.typography.bodyLarge,
            color = MgnTheme.colors.textPrimary,
        )
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun GraphicsChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Preview(name = "Settings", widthDp = 900, heightDp = 400)
@Composable
private fun SettingsPreview() {
    MgnTheme {
        SettingsContent(state = SettingsUiState(hasSave = true), onEvent = {})
    }
}
