package studio.mgn.command

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import studio.mgn.design.GoldFramePanel
import studio.mgn.design.MgnTheme
import studio.mgn.model.Achievement
import studio.mgn.model.CmpOp
import studio.mgn.model.Condition

/** In-game achievements grid with lock state and detail dialog. */
@Composable
fun AchievementsGrid(
    achievements: List<Achievement>,
    unlocked: Set<String>,
    strings: AchievementsGridStrings,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var detailId by remember { mutableStateOf<String?>(null) }
    val detail = achievements.firstOrNull { it.id == detailId }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BackHeader(title = strings.title, onBack = onBack)
        Text(
            text = strings.progress(unlocked.size, achievements.size),
            style = MgnTheme.typography.bodyMedium,
            color = MgnTheme.colors.textSecondary,
        )
        if (achievements.isEmpty()) {
            Text(
                text = strings.empty,
                style = MgnTheme.typography.bodyMedium,
                color = MgnTheme.colors.textSecondary,
            )
            return
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(220.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(achievements, key = { it.id }) { achievement ->
                val open = achievement.id in unlocked
                GoldFramePanel {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                            .clickable { detailId = achievement.id }
                            .alpha(if (open) 1f else 0.55f),
                    ) {
                        Text(
                            text = (if (open) "◆ " else "◇ ") + achievement.titleAr,
                            style = MgnTheme.typography.titleMedium,
                            color = if (open) {
                                MgnTheme.colors.goldPrimary
                            } else {
                                MgnTheme.colors.textSecondary
                            },
                        )
                        Text(
                            text = "+${achievement.gemReward} ${strings.gemsSuffix}",
                            style = MgnTheme.typography.labelMedium,
                            color = MgnTheme.colors.textSecondary,
                        )
                    }
                }
            }
        }
    }
    detail?.let {
        AlertDialog(
            onDismissRequest = { detailId = null },
            title = { Text(it.titleAr) },
            text = { Text(it.descriptionAr) },
            confirmButton = {
                TextButton(onClick = { detailId = null }) {
                    Text(strings.close)
                }
            },
        )
    }
}

data class AchievementsGridStrings(
    val title: String,
    val empty: String,
    val close: String,
    val gemsSuffix: String,
    val progress: (Int, Int) -> String,
)

@Preview(name = "AchievementsGrid", widthDp = 900, heightDp = 400)
@Composable
private fun AchievementsGridPreview() {
    MgnTheme {
        AchievementsGrid(
            achievements = listOf(
                Achievement(
                    "a",
                    "إنجاز مفتوح",
                    "وصف",
                    Condition.Cmp("economy", CmpOp.GTE, 80.0),
                    5,
                ),
            ),
            unlocked = setOf("a"),
            strings = AchievementsGridStrings(
                title = "الإنجازات",
                empty = "لا إنجازات",
                close = "إغلاق",
                gemsSuffix = "جواهر",
                progress = { a, b -> "$a/$b" },
            ),
            onBack = {},
        )
    }
}
