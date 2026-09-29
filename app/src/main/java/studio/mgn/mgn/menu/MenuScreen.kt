package studio.mgn.mgn.menu

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import studio.mgn.design.AssetPlaceholder
import studio.mgn.design.GameButton
import studio.mgn.design.GoldFramePanel
import studio.mgn.design.MgnBanner
import studio.mgn.design.BannerKind
import studio.mgn.design.MgnTheme
import studio.mgn.mgn.R

/**
 * Main menu: layered Canvas background (sky + parallax clouds + glow),
 * new/continue/achievements/settings actions, save summary and guards.
 */
@Composable
fun MenuScreen(
    viewModel: MenuViewModel,
    onNavigateSetup: () -> Unit,
    onNavigateGame: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAchievements: () -> Unit,
    reduceMotion: Boolean = false,
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.nav.collectLatest { target ->
            when (target) {
                MenuNav.SETUP -> onNavigateSetup()
                MenuNav.GAME -> onNavigateGame()
            }
        }
    }

    MenuContent(
        state = state,
        onEvent = viewModel::onEvent,
        onOpenSettings = onOpenSettings,
        onOpenAchievements = onOpenAchievements,
        reduceMotion = reduceMotion,
    )
}

@Composable
fun MenuContent(
    state: MenuUiState,
    onEvent: (MenuEvent) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAchievements: () -> Unit,
    reduceMotion: Boolean,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        MenuBackground(reduceMotion = reduceMotion)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "MGN",
                style = MgnTheme.typography.displayLarge,
                color = MgnTheme.colors.goldPrimary,
            )
            Text(
                text = "MGN STUDIO",
                style = MgnTheme.typography.labelMedium,
                color = MgnTheme.colors.textSecondary,
            )
            Spacer(Modifier.height(16.dp))

            if (state.recoveredFromBackup) {
                MgnBanner(
                    message = stringResource(R.string.menu_recovered_message),
                    kind = BannerKind.POSITIVE,
                )
                Spacer(Modifier.height(8.dp))
            }
            if (state.corrupted) {
                MgnBanner(
                    message = stringResource(R.string.menu_corrupt_message),
                    kind = BannerKind.NEGATIVE,
                    actionLabel = stringResource(R.string.menu_corrupt_delete),
                    onAction = { onEvent(MenuEvent.DeleteSaveClicked) },
                )
                Spacer(Modifier.height(8.dp))
            }

            if (state.summary != null && !state.corrupted) {
                GoldFramePanel {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.summary.countryName,
                            style = MgnTheme.typography.titleLarge,
                            color = MgnTheme.colors.textPrimary,
                        )
                        Text(
                            text = stringResource(
                                R.string.menu_turn_label,
                                state.summary.turnNumber,
                                state.summary.dateIso,
                            ),
                            style = MgnTheme.typography.bodyMedium,
                            color = MgnTheme.colors.textSecondary,
                        )
                        Spacer(Modifier.height(8.dp))
                        GameButton(
                            text = stringResource(R.string.menu_continue),
                            onClick = { onEvent(MenuEvent.ContinueClicked) },
                            reduceMotion = reduceMotion,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GameButton(
                    text = stringResource(R.string.menu_new_game),
                    onClick = { onEvent(MenuEvent.NewGameClicked) },
                    reduceMotion = reduceMotion,
                    modifier = Modifier.weight(1f),
                )
                GameButton(
                    text = stringResource(R.string.menu_achievements),
                    onClick = onOpenAchievements,
                    primary = false,
                    reduceMotion = reduceMotion,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(12.dp))
            GameButton(
                text = stringResource(R.string.menu_settings),
                onClick = onOpenSettings,
                primary = false,
                reduceMotion = reduceMotion,
                modifier = Modifier.width(220.dp),
            )
        }
    }

    if (state.showOverwriteConfirm) {
        AlertDialog(
            onDismissRequest = { onEvent(MenuEvent.OverwriteDismissed) },
            title = { Text(stringResource(R.string.menu_overwrite_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.menu_overwrite_message,
                        state.summary?.countryName ?: "",
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = { onEvent(MenuEvent.OverwriteConfirmed) }) {
                    Text(stringResource(R.string.menu_overwrite_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(MenuEvent.OverwriteDismissed) }) {
                    Text(stringResource(R.string.menu_cancel))
                }
            },
        )
    }
}

/**
 * Temporary layered backdrop until art assets are delivered.
 * Sky gradient + drifting cloud blobs + horizon glow, all on Canvas.
 */
@Composable
fun MenuBackground(reduceMotion: Boolean) {
    val colors = MgnTheme.colors
    val drift = if (reduceMotion) {
        0f
    } else {
        val transition = rememberInfiniteTransition(label = "clouds")
        val d1 by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(24000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "layer1",
        )
        d1
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF0B0D10), Color(0xFF1A2027), Color(0xFF2A2113)),
            ),
        )
        // Horizon glow.
        drawCircle(
            color = Color(0xFFF2B33D).copy(alpha = 0.12f),
            radius = size.width * 0.45f,
            center = Offset(size.width * 0.5f, size.height * 0.95f),
        )
        // Two parallax cloud layers.
        cloudLayer(
            count = 5,
            baseY = size.height * 0.25f,
            speed = drift,
            alpha = 0.10f,
            spread = size.width,
        )
        cloudLayer(
            count = 7,
            baseY = size.height * 0.45f,
            speed = (drift * 1.7f) % 1f,
            alpha = 0.07f,
            spread = size.width,
        )
    }
}

private fun DrawScope.cloudLayer(
    count: Int,
    baseY: Float,
    speed: Float,
    alpha: Float,
    spread: Float,
) {
    repeat(count) { i ->
        val x = ((i * 0.23f + speed) % 1f) * spread
        drawOval(
            color = Color(0xFFF3EBDD).copy(alpha = alpha),
            topLeft = Offset(x, baseY + (i % 3) * 18f),
            size = androidx.compose.ui.geometry.Size(150f + (i % 4) * 40f, 42f),
        )
    }
}

@Preview(name = "Menu", widthDp = 900, heightDp = 400)
@Composable
private fun MenuPreview() {
    MgnTheme {
        Box(Modifier.background(MgnTheme.colors.bgBase)) {
            AssetPlaceholder(assetName = "menu preview")
        }
        MenuContent(
            state = MenuUiState(isLoading = false),
            onEvent = {},
            onOpenSettings = {},
            onOpenAchievements = {},
            reduceMotion = true,
        )
    }
}
