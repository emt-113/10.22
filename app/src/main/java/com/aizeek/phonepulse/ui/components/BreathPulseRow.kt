package com.aizeek.phonepulse.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.withFrameNanos
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aizeek.phonepulse.intervention.BreathPulseEngine
import com.aizeek.phonepulse.intervention.ReturnGiftStore
import com.aizeek.phonepulse.ui.theme.BevelCard
import com.aizeek.phonepulse.ui.theme.BevelCardBorder
import com.aizeek.phonepulse.ui.theme.BevelCoral
import com.aizeek.phonepulse.ui.theme.BevelMint
import com.aizeek.phonepulse.ui.theme.BevelTextPrimary
import com.aizeek.phonepulse.ui.theme.BevelTextTertiary
import com.aizeek.phonepulse.ui.theme.BevelViolet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class BreathPhase { IDLE, HOLDING, DONE }

private fun todayKey(): String =
    SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

/**
 * Bottom dual control of the planet hero: an air button + a "breath" pill.
 * Long-press the air button and the pill stretches horizontally into a live
 * breathing bar (sine waveform), the plus rotates 135° into a cancel button,
 * and releasing early springs everything back. Holding through 3s pays one
 * stardust (BreathPulseEngine, capped at 10/day) into the same ledger as the
 * rest gifts.
 */
@Composable
fun BreathPulseRow(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val store = remember { ReturnGiftStore(context) }

    var phase by remember { mutableStateOf(BreathPhase.IDLE) }
    var holding by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var wavePhase by remember { mutableStateOf(0f) }
    var breathCount by remember { mutableIntStateOf(store.breathCount(todayKey())) }
    var awardMsg by remember { mutableStateOf("") }

    // Morph metrics in px: the pill's natural width <-> the full row width.
    // The pill width is frozen at its FIRST natural measurement — re-measuring
    // it while the animated width constraint is applied creates a feedback
    // loop that squeezes the pill into an invisible sliver on cold start.
    var pillNaturalPx by remember { mutableStateOf(0) }
    var rowMaxPx by remember { mutableStateOf(0) }
    var morphReady by remember { mutableStateOf(false) }
    val morph = remember { Animatable(0f) }
    LaunchedEffect(pillNaturalPx, rowMaxPx, phase) {
        val target = if (phase == BreathPhase.IDLE) pillNaturalPx else rowMaxPx
        if (target <= 0) return@LaunchedEffect
        if (!morphReady) {
            morphReady = true
            morph.snapTo(target.toFloat())
        } else {
            morph.animateTo(target.toFloat(), spring(dampingRatio = 0.85f, stiffness = 230f))
        }
    }
    val buttonScale by animateFloatAsState(
        targetValue = if (holding) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 420f),
        label = "breath-button-scale"
    )
    val plusRotation by animateFloatAsState(
        targetValue = if (phase != BreathPhase.IDLE) 135f else 0f,
        animationSpec = tween(240),
        label = "breath-plus-rotation"
    )

    fun completeHold() {
        val award = store.grantBreathBonus(todayKey())
        breathCount = award.todayCount
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        awardMsg = if (award.earned > 0) {
            "呼吸完成 · 星尘 +1（今日 ${award.todayCount}/${BreathPulseEngine.DAILY_CAP}）"
        } else {
            "呼吸完成 · 今日已达上限（${BreathPulseEngine.DAILY_CAP}/${BreathPulseEngine.DAILY_CAP}）"
        }
        phase = BreathPhase.DONE
    }

    // Hold loop: drives progress + the waveform every frame; completion while
    // the finger is still down pays immediately, release before 3s cancels.
    // The 120ms gate keeps a quick tap from flashing the bar.
    LaunchedEffect(holding) {
        if (!holding) {
            if (phase == BreathPhase.HOLDING) {
                phase = BreathPhase.IDLE
                progress = 0f
            }
            return@LaunchedEffect
        }
        kotlinx.coroutines.delay(120)
        if (!holding) return@LaunchedEffect
        phase = BreathPhase.HOLDING
        val start = android.os.SystemClock.elapsedRealtime()
        while (true) {
            val elapsed = android.os.SystemClock.elapsedRealtime() - start
            progress = BreathPulseEngine.holdProgress(elapsed)
            wavePhase += 0.16f
            if (BreathPulseEngine.isComplete(elapsed)) {
                completeHold()
                break
            }
            withFrameNanos { }
        }
    }
    LaunchedEffect(phase) {
        if (phase == BreathPhase.DONE) {
            kotlinx.coroutines.delay(1700)
            phase = BreathPhase.IDLE
            progress = 0f
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Air button: press-and-hold target, with a progress ring while holding.
        Box(
            modifier = Modifier
                .size(44.dp)
                .graphicsLayer {
                    scaleX = buttonScale
                    scaleY = buttonScale
                }
                .clip(CircleShape)
                .background(BevelViolet.copy(alpha = 0.14f))
                .border(1.dp, BevelViolet.copy(alpha = 0.4f), CircleShape)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            if (phase == BreathPhase.IDLE) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                holding = true
                            }
                            tryAwaitRelease()
                            holding = false
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(44.dp)) {
                if (progress > 0f) {
                    drawArc(
                        color = BevelViolet,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.Air,
                contentDescription = "长按呼吸",
                tint = BevelViolet,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.size(10.dp))

        // Morphing zone: pill <-> bar. The pill's natural width animates out to
        // the full row width, which is what makes the group read as stretching.
        Box(
            modifier = Modifier
                .weight(1f)
                .onSizeChanged { rowMaxPx = it.width },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier.then(
                    if (!morphReady) Modifier
                    else Modifier.width(with(density) { morph.value.toDp() })
                )
            ) {
                Crossfade(
                    targetState = phase,
                    animationSpec = tween(180),
                    label = "breath-crossfade"
                ) { current ->
                    when (current) {
                        BreathPhase.IDLE -> Row(
                            modifier = Modifier
                                .onSizeChanged {
                                    if (!morphReady && pillNaturalPx == 0) pillNaturalPx = it.width
                                }
                                .clip(RoundedCornerShape(14.dp))
                                .background(BevelCard)
                                .border(1.dp, BevelCardBorder, RoundedCornerShape(14.dp))
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = BevelViolet,
                                modifier = Modifier
                                    .graphicsLayer { rotationZ = plusRotation }
                                    .size(14.dp)
                            )
                            Text(
                                text = "长按呼吸 · 星尘 +1",
                                color = BevelTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.size(2.dp))
                            Text(
                                text = "今日 $breathCount/${BreathPulseEngine.DAILY_CAP}",
                                color = BevelTextTertiary,
                                fontSize = 10.5.sp
                            )
                        }

                        BreathPhase.HOLDING -> Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(BevelViolet.copy(alpha = 0.10f))
                                .border(1.dp, BevelViolet.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BreathWaveform(
                                progress = progress,
                                phase = wavePhase,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${(progress * 30).roundToInt() / 10f}s",
                                color = BevelViolet,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        holding = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "取消",
                                    tint = BevelCoral,
                                    modifier = Modifier
                                        .graphicsLayer { rotationZ = plusRotation }
                                        .size(16.dp)
                                )
                            }
                        }

                        BreathPhase.DONE -> Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(BevelMint.copy(alpha = 0.12f))
                                .border(1.dp, BevelMint.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = BevelMint,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = awardMsg,
                                color = BevelTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Live sine waveform; amplitude and spread grow with the hold progress. */
@Composable
private fun BreathWaveform(
    progress: Float,
    phase: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.height(20.dp)) {
        val amp = size.height / 2f * (0.4f + 0.6f * progress)
        val steps = 44
        val path = Path()
        for (i in 0..steps) {
            val t = i / steps.toFloat()
            val x = size.width * t
            val envelope = sin(kotlin.math.PI * t).toFloat()
            val y = size.height / 2f +
                BreathPulseEngine.wave(phase + t * 6.2832f * 2.2f) * amp * (0.3f + 0.7f * envelope)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = BevelViolet.copy(alpha = 0.25f + 0.75f * progress),
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
