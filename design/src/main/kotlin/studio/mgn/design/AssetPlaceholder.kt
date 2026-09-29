package studio.mgn.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Stand-in for art assets not yet delivered: gradient box + asset name.
 * Every usage site must name the awaited asset so handoff is traceable.
 */
@Composable
fun AssetPlaceholder(
    assetName: String,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val colors = MgnTheme.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    listOf(colors.bgPanelElevated, colors.bgPanel, colors.goldSoft),
                ),
            )
            .border(1.dp, colors.goldSoft, RoundedCornerShape(12.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label ?: assetName,
            style = MgnTheme.typography.bodyMedium,
            color = colors.goldPrimary,
        )
    }
}

@Preview(widthDp = 300)
@Composable
private fun AssetPlaceholderPreview() {
    MgnTheme {
        AssetPlaceholder(assetName = "art/menu_hero.png")
    }
}
