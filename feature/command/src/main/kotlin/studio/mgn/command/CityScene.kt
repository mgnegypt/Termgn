package studio.mgn.command

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import studio.mgn.content.LandmarkPosition
import studio.mgn.design.MgnTheme
import studio.mgn.model.GameState

/** Background art key by in-game month: day / sunset / night. */
fun cityAssetKey(month: Int): String = when (month) {
    in 6..9 -> "city_day"
    in 3..5, in 10..11 -> "city_sunset"
    else -> "city_night"
}

private fun skyColors(key: String): List<Color> = when (key) {
    "city_day" -> listOf(Color(0xFF1A2A3A), Color(0xFF12161B))
    "city_sunset" -> listOf(Color(0xFF2A1B2E), Color(0xFF3A2413), Color(0xFF12161B))
    else -> listOf(Color(0xFF05070C), Color(0xFF0B0D10))
}

/**
 * City scene: placeholder backdrop (keyed by month) + building signs at
 * relative coords + at most two animated layers (clouds, light flicker).
 * Animations stop when reduceMotion is on or the app leaves the foreground.
 */
@Composable
fun CityScene(
    state: GameState,
    positions: List<LandmarkPosition>,
    signLabel: (String) -> String,
    /** Build progress 0..1 for a queued landmark id, null otherwise. */
    progressOf: (String) -> Float?,
    animate: Boolean,
    onSignClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val lifecycleState by lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    val running = animate && lifecycleState.isAtLeast(Lifecycle.State.RESUMED)

    val key = cityAssetKey(state.inGameDate.monthValue)
    val colors = skyColors(key)

    val drift = if (running) {
        val transition = rememberInfiniteTransition(label = "city-clouds")
        val d by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(30000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "drift",
        )
        d
    } else {
        0f
    }
    val flicker = if (running) {
        val transition = rememberInfiniteTransition(label = "city-lights")
        val f by transition.animateFloat(
            initialValue = 0.55f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "flicker",
        )
        f
    } else {
        1f
    }

    Box(modifier = modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(brush = Brush.verticalGradient(colors))
            // Ground silhouette.
            drawRect(
                color = Color(0xFF0B0D10),
                topLeft = Offset(0f, size.height * 0.78f),
                size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.22f),
            )
            // Layer 1: slow clouds.
            repeat(4) { i ->
                val x = ((i * 0.27f + drift) % 1f) * size.width
                drawOval(
                    color = Color(0xFFF3EBDD).copy(alpha = 0.08f),
                    topLeft = Offset(x, size.height * (0.08f + i * 0.05f)),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.22f, 26f),
                )
            }
            // Layer 2: flickering city lights + simple smoke dots.
            val lit = Color(0xFFF2B33D).copy(alpha = 0.5f * flicker + 0.2f)
            repeat(24) { i ->
                val x = (i * 0.41f % 1f) * size.width
                val y = size.height * (0.6f + (i * 0.13f % 1f) * 0.25f)
                drawCircle(lit, radius = 2.5f, center = Offset(x, y))
            }
        }
        // Asset key tag (visible handoff for the coming backgrounds).
        Text(
            text = key,
            style = MgnTheme.typography.labelMedium,
            color = MgnTheme.colors.textSecondary.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
        )
        // Building signs overlay at true relative positions.
        androidx.compose.foundation.layout.BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
        ) {
            for (pos in positions) {
                val built = state.builtLandmarks.any { it.id == pos.id }
                BuildingSign(
                    label = signLabel(pos.id),
                    active = built || pos.decor,
                    progress = progressOf(pos.id),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(
                            x = (pos.x * maxWidth.value).dp - 28.dp,
                            y = (pos.y * maxHeight.value).dp - 20.dp,
                        ),
                    onClick = { onSignClick(pos.id) },
                )
            }
        }
    }
}
@Composable
fun BuildingSign(
    label: String,
    active: Boolean,
    progress: Float?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MgnTheme.colors
    Column(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (active) "◆" else "◇",
            color = if (active) colors.goldPrimary else colors.textSecondary,
            style = MgnTheme.typography.bodyMedium,
        )
        Text(
            text = label,
            style = MgnTheme.typography.labelMedium,
            color = colors.textPrimary,
        )
        if (progress != null) {
            androidx.compose.material3.LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.size(56.dp, 4.dp),
                color = colors.goldPrimary,
                trackColor = colors.bgPanelElevated,
            )
        }
    }
}

@Preview(name = "City", widthDp = 500, heightDp = 300)
@Composable
private fun CityPreview() {
    MgnTheme {
        CityScene(
            state = previewState(),
            positions = listOf(
                LandmarkPosition("national_museum", 0.3, 0.7, false),
                LandmarkPosition("presidential_palace", 0.5, 0.3, true),
            ),
            signLabel = { it },
            progressOf = { null },
            animate = false,
            onSignClick = {},
        )
    }
}
