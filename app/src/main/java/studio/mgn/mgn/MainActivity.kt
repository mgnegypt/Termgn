package studio.mgn.mgn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp

/**
 * PLAN 1 placeholder entry point: a dark screen writing "MGN".
 * Real screens arrive in later plans; game logic lives in :core:engine.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MgnPlaceholder() }
    }
}

@Composable
fun MgnPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07070A)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "MGN", color = Color(0xFFD4AF37), fontSize = 30.sp)
    }
}

@Preview
@Composable
fun MgnPlaceholderPreview() {
    MgnPlaceholder()
}
