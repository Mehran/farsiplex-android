package com.mehran.farsiplex.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mehran.farsiplex.R
import com.mehran.farsiplex.ui.theme.Bg0
import com.mehran.farsiplex.ui.theme.Brand
import com.mehran.farsiplex.ui.theme.Text1
import com.mehran.farsiplex.ui.theme.Text2
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Launch screen: the mascot logo pops in, then the welcome line fades up
 * before the app takes over. Skipped entirely after ~2.6s.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.82f) }

    LaunchedEffect(Unit) {
        launch { alpha.animateTo(1f, tween(650)) }
        launch { scale.animateTo(1f, tween(850, easing = FastOutSlowInEasing)) }
        delay(2500)
        onFinished()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF13294B), Bg0),
                    radius = 1200f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = null,
                modifier = Modifier
                    .size(180.dp)
                    .graphicsLayer {
                        this.alpha = alpha.value
                        scaleX = scale.value
                        scaleY = scale.value
                    }
            )
            Spacer(Modifier.height(26.dp))
            Text(
                "Welcome to MehranFarsiPlex",
                color = Text1,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { this.alpha = alpha.value }
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "مهران فارسی‌پلکس · فیلم و سریال ایرانی",
                color = Text2,
                fontSize = 15.sp,
                modifier = Modifier.graphicsLayer { this.alpha = alpha.value }
            )
        }
        Text(
            "FarsiPlex.com",
            color = Brand.copy(alpha = 0.9f),
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 26.dp)
        )
    }
}
