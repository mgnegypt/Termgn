package studio.mgn.mgn.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import studio.mgn.design.GoldFramePanel
import studio.mgn.design.MgnTheme
import studio.mgn.design.ResourceCounter
import studio.mgn.mgn.R

/** PLAN 2 placeholder: proves setup → save → load → display. */
@Composable
fun GameScreen(viewModel: GameViewModel) {
    val state by viewModel.state.collectAsState()
    GameContent(state = state)
}

@Composable
fun GameContent(state: GameUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.game_title),
            style = MgnTheme.typography.displayLarge,
            color = MgnTheme.colors.goldPrimary,
        )
        Spacer(Modifier.height(16.dp))
        GoldFramePanel {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (state.isLoading) {
                    Text(
                        text = "…",
                        style = MgnTheme.typography.titleLarge,
                        color = MgnTheme.colors.textSecondary,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.game_welcome, state.countryName),
                        style = MgnTheme.typography.titleLarge,
                        color = MgnTheme.colors.textPrimary,
                    )
                    ResourceCounter(value = state.turnNumber, animate = false)
                    Text(
                        text = state.dateLabel,
                        style = MgnTheme.typography.bodyMedium,
                        color = MgnTheme.colors.textSecondary,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.game_autosave_note),
            style = MgnTheme.typography.bodyMedium,
            color = MgnTheme.colors.textSecondary,
        )
        Text(
            text = stringResource(R.string.game_full_coming),
            style = MgnTheme.typography.bodyMedium,
            color = MgnTheme.colors.textSecondary,
        )
    }
}

@Preview(name = "Game", widthDp = 900, heightDp = 400)
@Composable
private fun GamePreview() {
    MgnTheme {
        GameContent(
            state = GameUiState(
                isLoading = false,
                countryName = "المجد",
                turnNumber = 12,
                dateLabel = "2026/01",
            ),
        )
    }
}
