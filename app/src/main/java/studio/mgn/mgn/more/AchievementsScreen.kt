package studio.mgn.mgn.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import studio.mgn.design.GoldFramePanel
import studio.mgn.design.MgnTheme
import studio.mgn.mgn.R

@Composable
fun AchievementsScreen(viewModel: AchievementsViewModel) {
    val state by viewModel.state.collectAsState()
    AchievementsContent(state = state)
}

@Composable
fun AchievementsContent(state: AchievementsUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.achievements_title),
            style = MgnTheme.typography.displayLarge,
            color = MgnTheme.colors.goldPrimary,
        )
        Text(
            text = stringResource(
                R.string.achievements_progress,
                state.unlockedCount,
                state.rows.size,
                state.percent,
            ),
            style = MgnTheme.typography.bodyMedium,
            color = MgnTheme.colors.textSecondary,
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.rows, key = { it.id }) { row ->
                GoldFramePanel {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = row.title,
                                style = MgnTheme.typography.titleMedium,
                                color = MgnTheme.colors.textPrimary,
                            )
                            Text(
                                text = row.description,
                                style = MgnTheme.typography.bodyMedium,
                                color = MgnTheme.colors.textSecondary,
                            )
                        }
                        Text(
                            text = if (row.unlocked) {
                                stringResource(R.string.achievements_unlocked)
                            } else {
                                stringResource(R.string.achievements_locked)
                            },
                            style = MgnTheme.typography.labelMedium,
                            color = if (row.unlocked) {
                                MgnTheme.colors.positive
                            } else {
                                MgnTheme.colors.textSecondary
                            },
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Achievements", widthDp = 900, heightDp = 400)
@Composable
private fun AchievementsPreview() {
    MgnTheme {
        AchievementsContent(
            state = AchievementsUiState(
                isLoading = false,
                rows = listOf(
                    AchievementRow("a", "إنجاز مفتوح", "وصف", 5, true),
                    AchievementRow("b", "إنجاز مقفول", "وصف", 10, false),
                ),
            ),
        )
    }
}
