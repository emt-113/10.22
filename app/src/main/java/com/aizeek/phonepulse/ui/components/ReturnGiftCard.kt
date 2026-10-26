package com.aizeek.phonepulse.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aizeek.phonepulse.intervention.ReturnGiftEngine
import com.aizeek.phonepulse.ui.theme.BevelEaseOut
import com.aizeek.phonepulse.ui.theme.BorderDark
import com.aizeek.phonepulse.ui.theme.ElectricViolet
import com.aizeek.phonepulse.ui.theme.NeonEmerald
import com.aizeek.phonepulse.ui.theme.SurfaceDark
import com.aizeek.phonepulse.ui.theme.SurfaceElevatedDark
import com.aizeek.phonepulse.ui.theme.TextPrimary
import com.aizeek.phonepulse.ui.theme.TextSecondary
import com.aizeek.phonepulse.ui.theme.TextTertiary
import kotlinx.coroutines.delay

/**
 * "Welcome back" overlay: the planet collected stardust while the user rested.
 * Any tap (or system back) dismisses; auto-dismisses after a few seconds so it
 * never traps the user.
 */
@Composable
fun ReturnGiftCard(gift: ReturnGiftEngine.Gift, onDismiss: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(gift) {
        // Arrive with a tick, then grow from 92% with the house curve — the card
        // reads as presented, not printed.
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        shown = true
        delay(8000)
        onDismiss()
    }
    val scale by animateFloatAsState(
        targetValue = if (shown) 1f else 0.92f,
        animationSpec = tween(durationMillis = 280, easing = BevelEaseOut),
        label = "gift-card-in"
    )
    BackHandler(onBack = onDismiss)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.35f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 40.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = scale
                }
                .clip(RoundedCornerShape(26.dp))
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            SurfaceElevatedDark.copy(alpha = 0.98f),
                            SurfaceDark
                        )
                    )
                )
                .border(1.dp, BorderDark, RoundedCornerShape(26.dp))
                .padding(horizontal = 24.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ElectricViolet.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Nightlight,
                    contentDescription = null,
                    tint = ElectricViolet,
                    modifier = Modifier.size(26.dp)
                )
            }
            Text("欢迎回来", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                "你离开了 ${gift.gapMinutes} 分钟，星球在这段时间替你收集了 ${gift.stardustEarned} 颗星尘。",
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center
            )
            Text(
                "累计 ${gift.totalStardust} 颗星尘 · 每一次好好休息都会点亮星球",
                color = TextTertiary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                textAlign = TextAlign.Center
            )
            Text(
                "点按任意位置继续",
                color = NeonEmerald,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
