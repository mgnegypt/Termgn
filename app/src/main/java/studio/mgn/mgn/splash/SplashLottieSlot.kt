package studio.mgn.mgn.splash

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import studio.mgn.design.AssetPlaceholder

/**
 * Lottie intro slot. With no asset file shipped yet ([asset] is null in
 * PLAN 2), renders the named placeholder so the handoff stays visible.
 */
@Composable
fun SplashLottieSlot(asset: String?) {
    if (asset == null) {
        AssetPlaceholder(
            assetName = "art/splash_logo.json (Lottie)",
            label = "MGN STUDIO",
            modifier = Modifier.size(200.dp),
        )
        return
    }
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset(asset))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
    )
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = Modifier.size(200.dp),
    )
}
