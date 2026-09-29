package studio.mgn.command

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import studio.mgn.design.MgnTheme
import studio.mgn.design.NavRailItem

/** Landscape side rail with the 9 fixed sections. */
@Composable
fun CommandSideRail(
    selected: CommandSection,
    pendingCount: Int,
    strings: CommandStrings,
    onSelect: (CommandSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(150.dp)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        for (section in CommandSection.entries) {
            val locked = section in LOCKED_SECTIONS
            NavRailItem(
                label = (strings.sectionLabels[section.name] ?: section.name) +
                    if (locked) " • ${strings.lockedSoon}" else "",
                selected = selected == section,
                onClick = { if (!locked) onSelect(section) },
                badge = if (section == CommandSection.DASHBOARD && pendingCount > 0) {
                    "$pendingCount"
                } else {
                    null
                },
            )
        }
    }
}

@Preview(name = "SideRail", widthDp = 200, heightDp = 400)
@Composable
private fun SideRailPreview() {
    MgnTheme {
        CommandSideRail(
            selected = CommandSection.DASHBOARD,
            pendingCount = 2,
            strings = previewStrings(),
            onSelect = {},
        )
    }
}
