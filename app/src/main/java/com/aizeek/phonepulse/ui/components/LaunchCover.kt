package com.aizeek.phonepulse.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Canvas
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aizeek.phonepulse.R
import com.aizeek.phonepulse.intervention.PlanetEngine
import com.aizeek.phonepulse.ui.theme.BevelBlue
import com.aizeek.phonepulse.ui.theme.BevelPageBottom
import com.aizeek.phonepulse.ui.theme.BevelPageTop
import com.aizeek.phonepulse.ui.theme.BevelTextPrimary
import com.aizeek.phonepulse.ui.theme.BevelTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Launch cover: one brand moment before the home surface. The pearl planet
 * (same glyph/material as the home hero) breathes in, a glow ring expands
 * around it, an orbit progress arc sweeps once — echoing the "orbit = daily
 * budget" story — and the app name surfaces. The whole cover then fades into
 * the already-composed home screen. Tapping anywhere skips immediately.
 *
 * Pure presentation: no data, no service work, <= ~1.7s total, safe to skip.
 */
@Composable
fun LaunchCover(onFinished: () -> Unit, modifier: Modifier = Modifier) {
    val finished = remember { mutableStateOf(false) }
    fun finish() {
        if (!finished.value) {
            finished.value = true
            onFinished()
        }
    }

    val scale = remember { Animatable(0.55f) }
    val glow = remember { Animatable(0f) }
    val sweep = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        launch { scale.animateTo(1f, tween(850, easing = FastOutSlowInEasing)) }
        launch { glow.animateTo(1f, tween(980, easing = LinearOutSlowInEasing)) }
        launch { sweep.animateTo(1f, tween(1150, easing = LinearEasing)) }
        launch { textAlpha.animateTo(1f, tween(420, delayMillis = 240, easing = LinearOutSlowInEasing)) }
        delay(1180)
        fade.animateTo(0f, tween(520, easing = LinearEasing))
        finish()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BevelPageTop, BevelPageBottom)))
            .pointerInput(Unit) { detectTapGestures { finish() } }
            .testTag("launch_cover")
            .graphicsLayer { alpha = fade.value },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                // Expanding glow ring: the planet "arrives".
                Canvas(modifier = Modifier.size(230.dp)) {
                    val p = glow.value
                    drawCircle(
                        color = BevelBlue.copy(alpha = 0.35f * (1f - p)),
                        radius = size.minDimension / 2f * (0.55f + 0.45f * p),
                        style = Stroke(width = (1f + 2.5f * (1f - p)).dp.toPx())
                    )
                }
                // Orbit progress arc: one full sweep, same story as the home lane.
                Canvas(modifier = Modifier.size(230.dp)) {
                    val stroke = 3.dp.toPx()
                    drawArc(
                        color = BevelBlue.copy(alpha = 0.45f),
                        startAngle = -90f,
                        sweepAngle = 360f * sweep.value,
                        useCenter = false,
                        topLeft = Offset(stroke / 2f, stroke / 2f),
                        size = androidx.compose.ui.geometry.Size(
                            size.width - stroke, size.height - stroke
                        ),
                        style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                }                // The planet itself, breathing in.
                Box(modifier = Modifier.graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    alpha = 0.4f + 0.6f * scale.value.coerceIn(0f, 1f)
                }) {
                    PlanetGlyph(stage = PlanetEngine.Stage.RUNNING, size = 176.dp)
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.app_name),
                color = BevelTextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                modifier = Modifier.graphicsLayer { alpha = textAlpha.value }
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = "让手机更健康 · 让生活更平衡",
                color = BevelTextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.graphicsLayer { alpha = textAlpha.value }
            )
        }
    }
}
