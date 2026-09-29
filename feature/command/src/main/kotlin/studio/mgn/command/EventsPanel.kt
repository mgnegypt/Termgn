package studio.mgn.command

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import studio.mgn.design.AssetPlaceholder
import studio.mgn.design.GameButton
import studio.mgn.design.GoldFramePanel
import studio.mgn.design.MgnTheme
import studio.mgn.model.HistoryEntry
import studio.mgn.model.HistoryTag
import java.time.LocalDate

fun historyDotColor(tag: HistoryTag): Color = when (tag) {
    HistoryTag.CRISIS, HistoryTag.WAR -> Color(0xFFFF5A4E)
    HistoryTag.ACHIEVEMENT -> Color(0xFF4EA1FF)
    HistoryTag.DECISION -> Color(0xFFF2B33D)
    else -> Color(0xFF9AA3AE)
}

/** One history row: thumbnail placeholder + title + snippet + turn + dot. */
@Composable
fun EventRow(
    entry: HistoryEntry,
    imageKey: String,
    modifier: Modifier = Modifier,
) {
    val colors = MgnTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AssetPlaceholder(
            assetName = if (imageKey.isBlank()) "event/default" else imageKey,
            label = "▦",
            modifier = Modifier.size(52.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "●",
                    color = historyDotColor(entry.tag),
                    style = MgnTheme.typography.labelMedium,
                )
                Text(
                    text = entry.title,
                    style = MgnTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            if (entry.detail.isNotBlank()) {
                Text(
                    text = entry.detail.take(80),
                    style = MgnTheme.typography.labelMedium,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            text = "د${entry.turnNumber}",
            style = MgnTheme.typography.labelMedium,
            color = colors.textSecondary,
        )
    }
}

/** Latest-history panel plus the war-council card when decisions pend. */
@Composable
fun EventsPanel(
    history: List<HistoryEntry>,
    pendingCount: Int,
    imageKeyOf: (HistoryEntry) -> String,
    strings: CommandStrings,
    onOpenDecisions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MgnTheme.colors
    GoldFramePanel(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = strings.latestEvents,
                style = MgnTheme.typography.titleMedium,
                color = colors.goldPrimary,
            )
            val recent = history.takeLast(4).reversed()
            if (recent.isEmpty()) {
                Text(
                    text = "—",
                    style = MgnTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
            for (entry in recent) {
                EventRow(entry = entry, imageKey = imageKeyOf(entry))
            }
            if (pendingCount > 0) {
                GameButton(
                    text = "${strings.councilTitle} ($pendingCount)",
                    onClick = onOpenDecisions,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** War-council banner card shown above the events list while deciding. */
@Composable
fun CouncilCard(
    pendingCount: Int,
    strings: CommandStrings,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GoldFramePanel(modifier = modifier, elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "${strings.councilTitle} • $pendingCount",
                style = MgnTheme.typography.titleMedium,
                color = MgnTheme.colors.goldPrimary,
            )
            GameButton(
                text = strings.councilOpen,
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(name = "Events", widthDp = 320, heightDp = 400)
@Composable
private fun EventsPreview() {
    MgnTheme {
        EventsPanel(
            history = listOf(
                HistoryEntry(
                    turnNumber = 12,
                    date = LocalDate.of(2026, 2, 1),
                    title = "موجة جفاف",
                    detail = "تقرير وزارة الزراعة يحذر…",
                    tag = HistoryTag.DECISION,
                ),
                HistoryEntry(
                    turnNumber = 12,
                    date = LocalDate.of(2026, 2, 1),
                    title = "إنجاز: أول معلم",
                    detail = "",
                    tag = HistoryTag.ACHIEVEMENT,
                ),
            ),
            pendingCount = 2,
            imageKeyOf = { "" },
            strings = previewStrings(),
            onOpenDecisions = {},
        )
    }
}
