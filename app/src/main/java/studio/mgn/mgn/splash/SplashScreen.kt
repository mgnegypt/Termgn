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
import androidx.compose.ui.unit.dp
import studio.mgn.design.AssetPlaceholder
import studio.mgn.design.GameButton
import studio.mgn.design.MgnTheme

/**
 * Splash: studio logo (Canvas/placeholder art + fade), background content
 * load, then [onReady]. Lottie slot reserved via [lottieAsset].
 */
@Composable
fun SplashScreen(
    viewModel: SplashViewModel,
    onReady: () -> Unit,
    reduceMotion: Boolean = false,
    // Reserved for the Lottie intro file (PLAN 4+). Null = placeholder art.
    lottieAsset: String? = null,
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
                if (lottieAsset != null) {
                    SplashLottieSlot(asset = lottieAsset)
                } else {
                    AssetPlaceholder(
                        assetName = "art/splash_logo.json (Lottie)",
                        label = "MGN STUDIO",
                        modifier = Modifier.size(200.dp),
                    )
                }
            }
            when (state.phase) {
                SplashPhase.LOADING -> Text(
                    text = "جارٍ تجهيز الدولة…",
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
                    GameButton(text = "إعادة المحاولة", onClick = viewModel::retry)
                }
                SplashPhase.READY -> Unit
            }
        }
    }
}
