package studio.mgn.mgn.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import studio.mgn.mgn.R
import studio.mgn.design.GameButton
import studio.mgn.design.MgnTheme

/**
 * Splash: studio logo (Lottie via GameAnimation, placeholder until the file
 * ships) + fade, background content load, then [onReady].
 */
@Composable
fun SplashScreen(
    viewModel: SplashViewModel,
    onReady: () -> Unit,
    reduceMotion: Boolean = false,
    loadingText: String = stringResource(R.string.splash_loading),
    retryText: String = stringResource(R.string.splash_retry),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.phase) {
        if (state.phase == SplashPhase.READY) onReady()
    }

    val colors = MgnTheme.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgBase),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AnimatedVisibility(
                visible = true,
                enter = if (reduceMotion) fadeIn() else fadeIn(),
                exit = fadeOut(),
            ) {
                studio.mgn.design.GameAnimation(
                    key = studio.mgn.design.AnimationKeys.SPLASH_LOGO,
                    animate = !reduceMotion,
                    modifier = Modifier.size(200.dp),
                )
            }
            when (state.phase) {
                SplashPhase.LOADING -> Text(
                    text = loadingText,
                    style = MgnTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
                SplashPhase.ERROR -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = state.error ?: "",
                        style = MgnTheme.typography.bodyMedium,
                        color = colors.negative,
                    )
                    GameButton(text = retryText, onClick = viewModel::retry)
                }
                SplashPhase.READY -> Unit
            }
        }
    }
}
