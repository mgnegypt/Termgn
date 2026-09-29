package studio.mgn.command

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import studio.mgn.design.AssetPlaceholder
import studio.mgn.design.GameButton
import studio.mgn.design.MgnTheme
import studio.mgn.model.GameState

/** Reign summary shared by all three endings. */
data class ReignSummary(
    val turns: Int,
    val score: Double,
    val achievements: Int,
    val achievementsTotal: Int,
    val bestDecisionTitle: String?,
    val worstDecisionTitle: String?,
    val bestTurn: Pair<Int, Double>?,
    val worstTurn: Pair<Int, Double>?,
)

/** Victory / collapse / continuation ending screen. */
@Composable
fun EndingScreen(
    ending: GameEndingScreen,
    state: GameState,
    summary: ReignSummary,
    strings: EndingStrings,
    canContinue: Boolean,
    onMenu: () -> Unit,
    onNewGame: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MgnTheme.colors
    val titleColor = when (ending) {
        GameEndingScreen.VICTORY -> colors.positive
        GameEndingScreen.COLLAPSE -> colors.negative
        GameEndingScreen.CONTINUATION -> colors.goldPrimary
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AssetPlaceholder(
            assetName = when (ending) {
                GameEndingScreen.VICTORY -> "art/ending_victory.png"
                GameEndingScreen.COLLAPSE -> "art/ending_collapse.png"
                GameEndingScreen.CONTINUATION -> "art/ending_continuation.png"
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = when (ending) {
                GameEndingScreen.VICTORY -> strings.victoryTitle
                GameEndingScreen.COLLAPSE -> strings.collapseTitle
                GameEndingScreen.CONTINUATION -> strings.continuationTitle
            },
            style = MgnTheme.typography.displayLarge,
            color = titleColor,
        )
        Text(
            text = "${state.countryName} • ${strings.turnsLabel}: ${summary.turns} • " +
                "${strings.scoreLabel}: ${"%.1f".format(summary.score)}",
            style = MgnTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
        Text(
            text = "${strings.achievementsLabel}: " +
                "${summary.achievements}/${summary.achievementsTotal}",
            style = MgnTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
        summary.bestDecisionTitle?.let {
            Text(
                text = "${strings.bestDecision}: $it",
                style = MgnTheme.typography.bodyMedium,
                color = colors.positive,
            )
        }
        summary.worstDecisionTitle?.let {
            Text(
                text = "${strings.worstDecision}: $it",
                style = MgnTheme.typography.bodyMedium,
                color = colors.negative,
            )
        }
        summary.bestTurn?.let { (turn, swing) ->
            Text(
                text = "${strings.bestTurn}: $turn (${"%+.1f".format(swing)})",
                style = MgnTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
        }
        summary.worstTurn?.let { (turn, swing) ->
            Text(
                text = "${strings.worstTurn}: $turn (${"%+.1f".format(swing)})",
                style = MgnTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GameButton(
                text = strings.menu,
                onClick = onMenu,
                primary = false,
                modifier = Modifier.weight(1f),
            )
            GameButton(
                text = strings.newGame,
                onClick = onNewGame,
                primary = false,
                modifier = Modifier.weight(1f),
            )
        }
        if (canContinue) {
            GameButton(
                text = strings.continuePlaying,
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

data class EndingStrings(
    val victoryTitle: String,
    val collapseTitle: String,
    val continuationTitle: String,
    val turnsLabel: String,
    val scoreLabel: String,
    val achievementsLabel: String,
    val bestDecision: String,
    val worstDecision: String,
    val bestTurn: String,
    val worstTurn: String,
    val menu: String,
    val newGame: String,
    val continuePlaying: String,
)

@Preview(name = "Ending", widthDp = 900, heightDp = 500)
@Composable
private fun EndingPreview() {
    MgnTheme {
        EndingScreen(
            ending = GameEndingScreen.VICTORY,
            state = previewState(),
            summary = ReignSummary(
                turns = 130,
                score = 87.0,
                achievements = 10,
                achievementsTotal = 18,
                bestDecisionTitle = "تصدير الفائض",
                worstDecisionTitle = "رفض المطالب",
                bestTurn = 40 to 2.5,
                worstTurn = 61 to -3.1,
            ),
            strings = EndingStrings(
                victoryTitle = "نصر عظيم",
                collapseTitle = "انهيار الدولة",
                continuationTitle = "عقد من الحكم",
                turnsLabel = "الأدوار",
                scoreLabel = "التقييم",
                achievementsLabel = "الإنجازات",
                bestDecision = "أفضل قرار",
                worstDecision = "أسوأ قرار",
                bestTurn = "أفضل دور",
                worstTurn = "أسوأ دور",
                menu = "القائمة",
                newGame = "لعبة جديدة",
                continuePlaying = "متابعة اللعب",
            ),
            canContinue = true,
            onMenu = {},
            onNewGame = {},
            onContinue = {},
        )
    }
}
