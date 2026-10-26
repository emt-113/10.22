package com.aizeek.phonepulse.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch

/**
 * Press feedback for pearl-surface tappables: the card scales down slightly
 * while the finger is down and springs back on release — no ripple, the scale
 * IS the answer. Place it EARLY in the modifier chain so the layer wraps the
 * whole visual (background, border, content) and the hit area scales with it.
 */
@Composable
fun Modifier.bevelPressable(
    onClick: () -> Unit,
    pressedScale: Float = 0.97f
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = tween(durationMillis = 140),
        label = "bevel-press-scale"
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
}

/**
 * The depth version of [bevelPressable]: while the finger is down the card
 * tilts a few degrees toward the touch point and shrinks a touch, and a soft
 * light spot gathers under the finger; on release everything springs back
 * (a spring, not a tween — the overshoot IS the "弹性复原"). Place it early
 * in the modifier chain like [bevelPressable].
 */
@Composable
fun Modifier.bevelTiltPressable(
    onClick: () -> Unit,
    pressedScale: Float = 0.97f,
    maxTiltDegrees: Float = 3.4f,
    spotAlpha: Float = 0.34f
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    // Touch point in 0..1 node space, tracked for the whole press so a drifting
    // thumb keeps steering the tilt and the light spot.
    var pressNorm by remember { mutableStateOf(Offset(0.5f, 0.5f)) }

    val tiltX by animateFloatAsState(
        targetValue = if (pressed) (pressNorm.y - 0.5f) * 2f * maxTiltDegrees else 0f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 340f),
        label = "bevel-tilt-x"
    )
    val tiltY by animateFloatAsState(
        targetValue = if (pressed) (0.5f - pressNorm.x) * 2f * maxTiltDegrees else 0f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 340f),
        label = "bevel-tilt-y"
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bevel-tilt-scale"
    )

    // The light spot starts at the card centre, converges toward the finger
    // while pressed, and fades before re-centering on release.
    val spotX = remember { Animatable(0.5f) }
    val spotY = remember { Animatable(0.5f) }
    val spotA = remember { Animatable(0f) }
    LaunchedEffect(pressed, pressNorm) {
        if (pressed) {
            launch { spotX.animateTo(pressNorm.x, spring(dampingRatio = 0.72f, stiffness = 240f)) }
            launch { spotY.animateTo(pressNorm.y, spring(dampingRatio = 0.72f, stiffness = 240f)) }
        } else {
            spotA.animateTo(0f, tween(180))
            spotX.snapTo(0.5f)
            spotY.snapTo(0.5f)
        }
    }
    LaunchedEffect(pressed) {
        if (pressed) spotA.animateTo(spotAlpha, tween(150))
    }

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            rotationX = tiltX
            rotationY = tiltY
            cameraDistance = 22f * density
        }
        .drawWithContent {
            drawContent()
            val a = spotA.value
            if (a > 0.01f) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = a),
                            Color.White.copy(alpha = 0f)
                        ),
                        center = Offset(
                            spotX.value * size.width,
                            spotY.value * size.height
                        ),
                        radius = size.minDimension * 0.85f
                    )
                )
            }
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                fun track(change: androidx.compose.ui.input.pointer.PointerInputChange) {
                    if (size.width > 0 && size.height > 0) {
                        pressNorm = Offset(
                            (change.position.x / size.width).coerceIn(0f, 1f),
                            (change.position.y / size.height).coerceIn(0f, 1f)
                        )
                    }
                }
                track(currentEvent.changes.first())
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull() ?: break
                    if (!change.pressed) break
                    track(change)
                }
            }
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}
