package studio.mgn.design

import android.util.Log
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

private const val TAG = "GameAnimation"

/** Well-known animation keys (files live in assets/animations/<key>.json). */
object AnimationKeys {
    const val SPLASH_LOGO = "splash_logo"
    const val ACHIEVEMENT_UNLOCK = "achievement_unlock"
    const val TURN_REPORT = "turn_report"
    const val TURN_TRANSITION = "turn_transition"
    const val ENDING_VICTORY = "ending_victory"
    const val ENDING_COLLAPSE = "ending_collapse"
    const val ENDING_CONTINUATION = "ending_continuation"
    const val CITY_SMOKE = "city_smoke"
}

/**
 * Single entry point for every Lottie animation. Swapping a file never
 * touches a screen. A missing file renders [AssetPlaceholder] (plus a
 * debug-only logcat warning), and [animate]=false freezes the first frame.
 */
@Composable
fun GameAnimation(
    key: String,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    iterations: Int = 1,
) {
    val context = LocalContext.current
    val exists = remember(key) {
        try {
            context.assets.list("animations")?.contains("$key.json") == true
        } catch (_: Exception) {
            false
        }
    }
    if (!exists) {
        if (BuildConfig.DEBUG) {
            Log.w(TAG, "missing animation asset: animations/$key.json")
        }
        AssetPlaceholder(
            assetName = "animations/$key.json",
            modifier = modifier,
        )
        return
    }
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Asset("animations/$key.json"),
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = iterations,
        isPlaying = animate,
    )
    LottieAnimation(
        composition = composition,
        progress = { if (animate) progress else 0f },
        modifier = modifier,
    )
}

/** Fixed-size variant used for spots and banners. */
@Composable
fun GameAnimationSpot(
    key: String,
    animate: Boolean = true,
    sizeDp: Int = 200,
) {
    GameAnimation(
        key = key,
        animate = animate,
        modifier = Modifier.size(sizeDp.dp),
    )
}
