package studio.mgn.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** MGN STUDIO identity: luxurious dark, black + gold. */
object MgnTokens {
    val BgBase = Color(0xFF0B0D10)
    val BgPanel = Color(0xFF12161B)
    val BgPanelElevated = Color(0xFF1A2027)
    val GoldPrimary = Color(0xFFF2B33D)
    val GoldSoft = Color(0xFFB8862B)
    val TextPrimary = Color(0xFFF3EBDD)
    val TextSecondary = Color(0xFF9AA3AE)
    val Positive = Color(0xFF3DD68C)
    val Negative = Color(0xFFFF5A4E)

    val IndicatorGreen = Color(0xFF3DD68C)
    val IndicatorBlue = Color(0xFF4EA1FF)
    val IndicatorPurple = Color(0xFFA06BFF)
    val IndicatorRed = Color(0xFFFF5A4E)
    val IndicatorOrange = Color(0xFFFF9E4E)
    val IndicatorCyan = Color(0xFF4ED6FF)
    val IndicatorYellow = Color(0xFFFFD94E)
}

/** One token per game indicator, in dashboard order. */
enum class IndicatorColor { GREEN, BLUE, PURPLE, RED, ORANGE, CYAN, YELLOW }

fun IndicatorColor.color(): Color = when (this) {
    IndicatorColor.GREEN -> MgnTokens.IndicatorGreen
    IndicatorColor.BLUE -> MgnTokens.IndicatorBlue
    IndicatorColor.PURPLE -> MgnTokens.IndicatorPurple
    IndicatorColor.RED -> MgnTokens.IndicatorRed
    IndicatorColor.ORANGE -> MgnTokens.IndicatorOrange
    IndicatorColor.CYAN -> MgnTokens.IndicatorCyan
    IndicatorColor.YELLOW -> MgnTokens.IndicatorYellow
}

@Immutable
data class MgnColors(
    val bgBase: Color = MgnTokens.BgBase,
    val bgPanel: Color = MgnTokens.BgPanel,
    val bgPanelElevated: Color = MgnTokens.BgPanelElevated,
    val goldPrimary: Color = MgnTokens.GoldPrimary,
    val goldSoft: Color = MgnTokens.GoldSoft,
    val textPrimary: Color = MgnTokens.TextPrimary,
    val textSecondary: Color = MgnTokens.TextSecondary,
    val positive: Color = MgnTokens.Positive,
    val negative: Color = MgnTokens.Negative,
)

@Immutable
data class MgnSpacing(
    val xs: androidx.compose.ui.unit.Dp = 4.dp,
    val sm: androidx.compose.ui.unit.Dp = 8.dp,
    val md: androidx.compose.ui.unit.Dp = 12.dp,
    val lg: androidx.compose.ui.unit.Dp = 16.dp,
    val xl: androidx.compose.ui.unit.Dp = 24.dp,
    val xxl: androidx.compose.ui.unit.Dp = 32.dp,
)

val LocalMgnColors = staticCompositionLocalOf { MgnColors() }
val LocalMgnSpacing = staticCompositionLocalOf { MgnSpacing() }

val DisplayFontFamily = FontFamily(
    Font(R.font.reem_kufi, FontWeight.Normal),
    Font(R.font.cairo, FontWeight.Bold),
)
val BodyFontFamily = FontFamily(
    Font(R.font.tajawal_regular, FontWeight.Normal),
    Font(R.font.tajawal_bold, FontWeight.Bold),
)

private val MgnTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 38.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)

private val MgnShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
)

/**
 * App theme. Forces RTL layout direction (Arabic-first) and provides
 * MGN colors, fonts, shapes and spacing below MaterialTheme.
 */
@Composable
fun MgnTheme(
    content: @Composable () -> Unit,
) {
    // The game is dark-only by design.
    val scheme = darkColorScheme(
        primary = MgnTokens.GoldPrimary,
        onPrimary = MgnTokens.BgBase,
        secondary = MgnTokens.GoldSoft,
        background = MgnTokens.BgBase,
        surface = MgnTokens.BgPanel,
        onBackground = MgnTokens.TextPrimary,
        onSurface = MgnTokens.TextPrimary,
        error = MgnTokens.Negative,
    )
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalMgnColors provides MgnColors(),
        LocalMgnSpacing provides MgnSpacing(),
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = MgnTypography,
            shapes = MgnShapes,
            content = content,
        )
    }
}

/** Convenient accessors inside [MgnTheme]. */
object MgnTheme {
    val colors: MgnColors
        @Composable get() = LocalMgnColors.current
    val spacing: MgnSpacing
        @Composable get() = LocalMgnSpacing.current
    val typography: Typography
        @Composable get() = MaterialTheme.typography
}
