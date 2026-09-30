package studio.mgn.command

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import studio.mgn.design.MgnTheme
import studio.mgn.model.HistoryEntry
import studio.mgn.model.HistoryTag
import java.time.LocalDate

enum class HistoryFilter { ALL, ECONOMY, DIPLOMACY, EVENTS }

private fun HistoryTag.filter(): HistoryFilter = when (this) {
    HistoryTag.CONSTRUCTION, HistoryTag.YEARLY_REPORT -> HistoryFilter.ECONOMY
    HistoryTag.WAR, HistoryTag.TREATY -> HistoryFilter.DIPLOMACY
    else -> HistoryFilter.EVENTS
}

/** Filterable timeline grouped by turn. */
@Composable
fun HistoryScreen(
    history: List<HistoryEntry>,
    strings: HistoryStrings,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var filter by remember { mutableStateOf(HistoryFilter.ALL) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BackHeader(title = strings.title, onBack = onBack)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (option in HistoryFilter.entries) {
                FilterChip(
                    selected = filter == option,
                    onClick = { filter = option },
                    modifier = Modifier.heightIn(min = 48.dp),
                    label = { Text(strings.filterName(option)) },
                )
            }
        }
        val shown = history.filter {
            filter == HistoryFilter.ALL || it.tag.filter() == filter
        }.reversed()
        if (shown.isEmpty()) {
            Text(
                text = strings.empty,
                style = MgnTheme.typography.bodyMedium,
                color = MgnTheme.colors.textSecondary,
            )
            return
        }
        var lastTurn = -1
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (entry in shown) {
                if (entry.turnNumber != lastTurn) {
                    lastTurn = entry.turnNumber
                    item(key = "turn-${entry.turnNumber}") {
                        Text(
                            text = "${strings.turnLabel} ${entry.turnNumber}",
                            style = MgnTheme.typography.labelMedium,
                            color = MgnTheme.colors.goldPrimary,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
                item(key = "${entry.turnNumber}-${entry.title}-${entry.detail}") {
                    EventRow(
                        entry = entry,
                        imageKey = "",
                        turnBadge = strings.turnBadge,
                    )
                }
            }
        }
    }
}

data class HistoryStrings(
    val title: String,
    val empty: String,
    val turnLabel: String,
    val back: String,
    val filterAll: String,
    val filterEconomy: String,
    val filterDiplomacy: String,
    val filterEvents: String,
    val turnBadge: (Int) -> String,
)

private fun HistoryStrings.filterName(filter: HistoryFilter): String = when (filter) {
    HistoryFilter.ALL -> filterAll
    HistoryFilter.ECONOMY -> filterEconomy
    HistoryFilter.DIPLOMACY -> filterDiplomacy
    HistoryFilter.EVENTS -> filterEvents
}

@Composable
fun BackHeader(title: String, onBack: () -> Unit, backLabel: String = "→") {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MgnTheme.typography.displayLarge,
            color = MgnTheme.colors.goldPrimary,
        )
        androidx.compose.material3.TextButton(onClick = onBack) {
            Text(backLabel)
        }
    }
}

@Preview(name = "History", widthDp = 900, heightDp = 400)
@Composable
private fun HistoryPreview() {
    MgnTheme {
        HistoryScreen(
            history = listOf(
                HistoryEntry(12, LocalDate.of(2026, 2, 1), "موجة جفاف", "تفاصيل", HistoryTag.DECISION),
                HistoryEntry(12, LocalDate.of(2026, 2, 1), "إعلان حرب: سهران", "أعلنتها سهران", HistoryTag.WAR),
                HistoryEntry(11, LocalDate.of(2026, 1, 1), "اكتمل بناء: المتحف", "", HistoryTag.CONSTRUCTION),
            ),
            strings = HistoryStrings(
                title = "السجل",
                empty = "لا سجل بعد",
                turnLabel = "الدور",
                back = "رجوع",
                filterAll = "الكل",
                filterEconomy = "اقتصاد",
                filterDiplomacy = "دبلوماسية",
                filterEvents = "أحداث",
                turnBadge = { turn -> "د$turn" },
            ),
            onBack = {},
        )
    }
}
