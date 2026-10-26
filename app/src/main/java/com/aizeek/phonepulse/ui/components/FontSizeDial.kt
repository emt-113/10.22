package com.aizeek.phonepulse.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aizeek.phonepulse.ui.theme.BevelBlue
import com.aizeek.phonepulse.ui.theme.BevelCard
import com.aizeek.phonepulse.ui.theme.BevelCardBorder
import com.aizeek.phonepulse.ui.theme.BevelEaseOut
import com.aizeek.phonepulse.ui.theme.BevelMint
import com.aizeek.phonepulse.ui.theme.BevelTextPrimary
import com.aizeek.phonepulse.ui.theme.BevelTextSecondary
import com.aizeek.phonepulse.ui.theme.BevelTextTertiary
import com.aizeek.phonepulse.ui.theme.BevelTrack
import com.aizeek.phonepulse.ui.theme.ReadingFontStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** Tick spacing of the ruler in dp; drag sensitivity derives from it. */
private const val TICK_SPACING_DP = 46

/**
 * "阅读字号" ruler (settings): a horizontal scale the user flicks like a
 * dial — the current value sits centered and highlighted, the sides fade out,
 * the preview text follows the finger in real time, and on release the value
 * snaps to the nearest tick (with a haptic tick on settle). The committed size
 * drives the history timeline reading text.
 */
@Composable
fun FontSizeDialCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    remember { ReadingFontStore.load(context); true }

    // Raw value under the finger (float); the store's int value follows it so
    // the preview re-flows every frame of the drag.
    var rawValue by remember { mutableFloatStateOf(ReadingFontStore.fontSp.toFloat()) }
    var settleJob by remember { mutableStateOf<Job?>(null) }

    fun settle() {
        val target = ReadingFontStore.snap(rawValue)
        ReadingFontStore.set(target, persist = true, context = context)
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        settleJob = scope.launch {
            val anim = Animatable(rawValue)
            anim.animateTo(target.toFloat(), tween(220, easing = BevelEaseOut)) { rawValue = value }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(BevelCard)
            .border(1.dp, BevelCardBorder, RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(BevelBlue.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatSize,
                        contentDescription = null,
                        tint = BevelBlue,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Text("阅读字号", color = BevelTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(
                text = "${ReadingFontStore.fontSp} sp",
                color = BevelBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Live preview: real product copy, re-flows while the ruler moves.
        Text(
            text = "预览 · 预算 = 每天累计使用上限，用到 80% 预告一次，超限后每超 1 分钟催一次，息屏即止。",
            color = BevelTextSecondary,
            fontSize = ReadingFontStore.fontSp.sp,
            lineHeight = (ReadingFontStore.fontSp + 6).sp
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            settleJob?.cancel()
                        },
                        onDragEnd = { settle() },
                        onDragCancel = { settle() }
                    ) { change, dragAmount ->
                        change.consume()
                        val spacingPx = TICK_SPACING_DP.dp.toPx()
                        rawValue = (rawValue - dragAmount / spacingPx)
                            .coerceIn(ReadingFontStore.MIN_SP.toFloat(), ReadingFontStore.MAX_SP.toFloat())
                        ReadingFontStore.set(rawValue.roundToInt(), persist = false)
                    }
                }
        ) {
            val centerX = size.width / 2f
            val spacing = TICK_SPACING_DP.dp.toPx()
            val centerTickY = size.height * 0.46f

            // Track behind the ticks.
            drawRoundRect(
                color = BevelTrack,
                topLeft = Offset(0f, size.height * 0.80f),
                size = Size(size.width, 3.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx()),
                style = Fill
            )

            for (tick in ReadingFontStore.MIN_SP..ReadingFontStore.MAX_SP) {
                val x = centerX + (tick - rawValue) * spacing
                val distance = abs(x - centerX)
                val fade = (1f - distance / (spacing * 3.2f)).coerceIn(0f, 1f)
                if (fade <= 0.02f) continue
                val isCenter = distance < spacing * 0.5f
                val tickColor = if (isCenter) BevelMint else BevelTextTertiary

                drawLine(
                    color = tickColor.copy(alpha = if (isCenter) 1f else 0.55f * fade),
                    start = Offset(x, centerTickY - if (isCenter) 12.dp.toPx() else 7.dp.toPx()),
                    end = Offset(x, centerTickY + if (isCenter) 12.dp.toPx() else 7.dp.toPx()),
                    strokeWidth = if (isCenter) 2.6.dp.toPx() else 1.6.dp.toPx()
                )

                val label = textMeasurer.measure(
                    text = "$tick",
                    style = TextStyle(
                        color = if (isCenter) BevelMint else BevelTextTertiary,
                        fontSize = if (isCenter) 12.sp else 10.sp,
                        fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal
                    )
                )
                drawText(
                    textLayoutResult = label,
                    topLeft = Offset(x - label.size.width / 2f, centerTickY - 36.dp.toPx()),
                    alpha = if (isCenter) 1f else 0.7f * fade
                )
            }
        }

        Text(
            text = "左右拖动刻度，松手自动吸附 · 生效于「屏幕状态明细」阅读文本",
            color = BevelTextTertiary,
            fontSize = 11.5.sp
        )
    }
}
