package studio.mgn.design

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Primary/secondary button with glow and press scale. Min touch target 48dp. */
@Composable
fun GameButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    enabled: Boolean = true,
    reduceMotion: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scaleTarget = if (pressed && !reduceMotion) 0.96f else 1f
    val scale by animateFloatAsState(
        targetValue = scaleTarget,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "press",
    )
    val colors = MgnTheme.colors
    val background = if (primary) {
        Brush.horizontalGradient(listOf(colors.goldSoft, colors.goldPrimary))
    } else {
        Brush.horizontalGradient(listOf(colors.bgPanelElevated, colors.bgPanelElevated))
    }
    val contentColor = if (primary) colors.bgBase else colors.goldPrimary
    Box(
        modifier = modifier
            .scale(if (reduceMotion) 1f else scale)
            .clip(RoundedCornerShape(12.dp))
            .background(background, RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = if (primary) Color.Transparent else colors.goldSoft,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .defaultMinSize(minHeight = 48.dp)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MgnTheme.typography.titleMedium,
            color = contentColor.copy(alpha = if (enabled) 1f else 0.5f),
        )
    }
}

/** Panel with a soft gold frame. */
@Composable
fun GoldFramePanel(
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = MgnTheme.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (elevated) colors.bgPanelElevated else colors.bgPanel,
                RoundedCornerShape(16.dp),
            )
            .border(1.dp, colors.goldSoft.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        content()
    }
}

/** Small stat chip: optional icon + value + colored delta. */
@Composable
fun StatChip(
    value: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    icon: ImageVector? = null,
    delta: Double? = null,
) {
    val colors = MgnTheme.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(colors.bgPanelElevated)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.goldPrimary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        Column {
            if (label != null) {
                Text(
                    text = label,
                    style = MgnTheme.typography.labelMedium,
                    color = colors.textSecondary,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    style = MgnTheme.typography.titleMedium,
                    color = colors.textPrimary,
                )
                if (delta != null && delta != 0.0) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = (if (delta > 0) "+" else "") + delta.toString(),
                        style = MgnTheme.typography.bodyMedium,
                        color = if (delta > 0) colors.positive else colors.negative,
                    )
                }
            }
        }
    }
}

/** Labeled 0-100 indicator bar. */
@Composable
fun IndicatorBar(
    label: String,
    value: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val colors = MgnTheme.colors
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MgnTheme.typography.bodyMedium,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value.toInt().toString(),
                style = MgnTheme.typography.bodyMedium,
                color = colors.textPrimary,
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (value / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = colors.bgPanelElevated,
        )
    }
}

/** Number that counts up toward [value]; static text when [animate] is false. */
@Composable
fun ResourceCounter(
    value: Int,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
) {
    val shown by animateIntAsState(
        targetValue = value,
        animationSpec = tween(600),
        label = "count",
    )
    Text(
        text = (if (animate) shown else value).toString(),
        style = MgnTheme.typography.displayLarge,
        color = MgnTheme.colors.goldPrimary,
        modifier = modifier,
    )
}

/** Side-rail navigation item for landscape. */
@Composable
fun NavRailItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    badge: String? = null,
) {
    val colors = MgnTheme.colors
    val background = if (selected) {
        colors.goldPrimary.copy(alpha = 0.15f)
    } else {
        Color.Transparent
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .border(
                1.dp,
                if (selected) colors.goldPrimary else Color.Transparent,
                RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) colors.goldPrimary else colors.textSecondary,
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = label,
            style = MgnTheme.typography.bodyMedium,
            color = if (selected) colors.goldPrimary else colors.textSecondary,
        )
        if (badge != null) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.negative)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = badge,
                    style = MgnTheme.typography.labelMedium,
                    color = Color.White,
                )
            }
        }
    }
}

/** Inline banner for errors, recovery offers and notices. */
@Composable
fun MgnBanner(
    message: String,
    modifier: Modifier = Modifier,
    kind: BannerKind = BannerKind.NEUTRAL,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = MgnTheme.colors
    val border = when (kind) {
        BannerKind.POSITIVE -> colors.positive
        BannerKind.NEGATIVE -> colors.negative
        BannerKind.NEUTRAL -> colors.goldSoft
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.bgPanelElevated)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            style = MgnTheme.typography.bodyMedium,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.width(12.dp))
            GameButton(text = actionLabel, onClick = onAction, primary = false)
        }
    }
}

enum class BannerKind { POSITIVE, NEGATIVE, NEUTRAL }

// ── Previews ──

@Preview(name = "Buttons", widthDp = 400)
@Composable
private fun ButtonsPreview() {
    MgnTheme {
        Column(
            modifier = Modifier
                .background(MgnTheme.colors.bgBase)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GameButton(text = "زر رئيسي", onClick = {})
            GameButton(text = "زر ثانوي", onClick = {}, primary = false)
        }
    }
}

@Preview(name = "Panel+Stats", widthDp = 400)
@Composable
private fun PanelPreview() {
    MgnTheme {
        GoldFramePanel(modifier = Modifier.padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip(value = "250,000", label = "الخزينة", delta = 12000.0)
                IndicatorBar(
                    label = "الاقتصاد",
                    value = 64f,
                    color = IndicatorColor.CYAN.color(),
                )
                ResourceCounter(value = 128)
                MgnBanner(message = "تنبيه تجريبي", kind = BannerKind.NEUTRAL)
            }
        }
    }
}

@Preview(name = "NavRail", widthDp = 300)
@Composable
private fun NavRailPreview() {
    MgnTheme {
        Column(
            modifier = Modifier
                .background(MgnTheme.colors.bgBase)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            NavRailItem(label = "الرئيسية", selected = true, onClick = {})
            NavRailItem(label = "القرارات", selected = false, onClick = {}, badge = "2")
        }
    }
}
