package com.aizeek.phonepulse.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

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
