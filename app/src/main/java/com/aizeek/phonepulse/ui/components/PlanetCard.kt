package com.aizeek.phonepulse.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aizeek.phonepulse.intervention.PlanetEngine
import com.aizeek.phonepulse.intervention.UsageCoachController
import com.aizeek.phonepulse.ui.theme.BevelCyan
import com.aizeek.phonepulse.ui.theme.BevelViolet
import com.aizeek.phonepulse.ui.theme.NeonCyan
import com.aizeek.phonepulse.ui.theme.NeonEmerald
import com.aizeek.phonepulse.ui.theme.SurfaceDark
import com.aizeek.phonepulse.ui.theme.TextPrimary
import com.aizeek.phonepulse.ui.theme.TextSecondary
import com.aizeek.phonepulse.ui.theme.TextTertiary

/**
 * Planet card (intervention): live daily screen-on progress, current continuous
 * session, planet stage and streak. Stage mirrors goal achievement only — never
 * battery health. Tap opens the detail page (weekly insights + history).
 */
@Composable
fun PlanetCard(onClick: () -> Unit) {
    val live by UsageCoachController.liveState().collectAsStateWithLifecycle()
    val context = LocalContext.current

    val budget = live.budgetMinutes
    val progress = if (budget != null && budget > 0) {
        (live.todayMinutes.toFloat() / budget).coerceIn(0f, 1f)
    } else null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceDark)
            .clickable { onClick() }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PlanetGlyph(stage = live.stage, size = 44.dp)
                Column {
                    Text("脉搏星球", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        stageLabel(live.stage, live.streakDays),
                        color = stageColor(live.stage), fontSize = 12.sp
                    )
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "详情", tint = TextTertiary)
        }

        // Ring: today's screen-on minutes against budget (or total when no budget).
        Box(contentAlignment = Alignment.Center) {
            ProgressRing(progress = progress ?: (live.todayMinutes.coerceAtMost(1) / 1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${live.todayMinutes}",
                    color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold
                )
                Text(
                    if (budget != null) "今日亮屏 · 预算 ${budget} 分钟" else "今日亮屏分钟",
                    color = TextSecondary, fontSize = 11.sp
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MetricChip("当前连续会话", "${live.sessionMinutes} 分钟", NeonCyan)
            MetricChip(
                "连续达成",
                "${live.streakDays} 天",
                if (live.streakDays > 0) NeonEmerald else TextSecondary
            )
            MetricChip(
                "连续使用上限",
                live.continuousLimitMinutes?.let { "$it 分钟" } ?: "未开启",
                TextSecondary
            )
        }

        if (!live.enabled) {
            Text(
                "教练未开启：在下方「使用教练」中打开开关，星球将随每日目标成长。",
                color = TextTertiary, fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun MetricChip(label: String, value: String, valueColor: Color) {
    Column {
        Text(label, color = TextTertiary, fontSize = 10.sp)
        Text(value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

/** Pearl planet glyph, same material as the home hero so both read as one planet. */
@Composable
internal fun PlanetGlyph(stage: PlanetEngine.Stage, size: androidx.compose.ui.unit.Dp) {
    val visualState = when (stage) {
        PlanetEngine.Stage.SPROUT -> PlanetVisualState.STEADY
        PlanetEngine.Stage.RUNNING, PlanetEngine.Stage.SHINING -> PlanetVisualState.GOOD
    }
    val palette = planetPalette(visualState)
    Canvas(modifier = Modifier.size(size)) {
        val r = this.size.minDimension / 2
        val core = r * 0.55f
        // Body: the same pearl journey as the home hero.
        drawCircle(
            brush = Brush.linearGradient(
                colorStops = arrayOf(
                    0.00f to palette.light,
                    0.20f to palette.mid,
                    0.40f to palette.mid2,
                    0.60f to palette.mid3,
                    0.80f to palette.deep,
                    1.00f to palette.deep2
                ),
                start = Offset(r - core, r - core),
                end = Offset(r + core, r + core)
            ),
            radius = core,
            center = Offset(r, r)
        )
        // Key light + blush.
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to Color.White.copy(alpha = 0.62f),
                    0.45f to Color.White.copy(alpha = 0.18f),
                    1.00f to Color.White.copy(alpha = 0f)
                ),
                center = Offset(r - core * 0.38f, r - core * 0.42f),
                radius = core * 1.0f
            ),
            radius = core * 1.0f,
            center = Offset(r - core * 0.38f, r - core * 0.42f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to Color(0xFFF6D2E2).copy(alpha = 0.45f),
                    1.00f to Color(0xFFF6D2E2).copy(alpha = 0f)
                ),
                center = Offset(r + core * 0.36f, r + core * 0.42f),
                radius = core * 0.95f
            ),
            radius = core * 0.95f,
            center = Offset(r + core * 0.36f, r + core * 0.42f)
        )
        // Pearl rim.
        drawCircle(
            color = Color.White.copy(alpha = 0.45f),
            radius = core - core * 0.06f,
            center = Offset(r, r),
            style = Stroke(width = r * 0.03f)
        )
        if (stage != PlanetEngine.Stage.SPROUT) {
            drawCircle(
                color = palette.glow.copy(alpha = 0.45f),
                radius = r * 0.85f,
                center = Offset(r, r),
                style = Stroke(width = r * 0.10f)
            )
        }
        if (stage == PlanetEngine.Stage.SHINING) {
            drawCircle(
                color = palette.glow.copy(alpha = 0.7f),
                radius = r * 0.7f,
                center = Offset(r, r),
                style = Stroke(width = r * 0.05f)
            )
        }
    }
}

@Composable
private fun ProgressRing(progress: Float) {
    val track = TextTertiary.copy(alpha = 0.25f)
    val gradient = Brush.sweepGradient(listOf(NeonCyan, NeonEmerald, NeonCyan))
    Canvas(modifier = Modifier.size(96.dp)) {
        val stroke = 10.dp.toPx()
        val inset = stroke / 2
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(
            color = track,
            startAngle = -90f, sweepAngle = 360f, useCenter = false,
            topLeft = Offset(inset, inset), size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        if (progress > 0f) {
            drawArc(
                brush = gradient,
                startAngle = -90f, sweepAngle = 360f * progress.coerceIn(0f, 1f), useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
    }
}

internal fun stageLabel(stage: PlanetEngine.Stage, streak: Int): String = when (stage) {
    PlanetEngine.Stage.SPROUT -> if (streak > 0) "萌芽 · 昨日达成 $streak 天" else "萌芽 · 完成今日目标点亮星球"
    PlanetEngine.Stage.RUNNING -> "运转 · 连续达成 $streak 天"
    PlanetEngine.Stage.SHINING -> "闪耀 · 连续达成 $streak 天"
}

internal fun stageColor(stage: PlanetEngine.Stage): Color = when (stage) {
    PlanetEngine.Stage.SPROUT -> TextSecondary
    PlanetEngine.Stage.RUNNING -> BevelViolet
    PlanetEngine.Stage.SHINING -> BevelCyan
}
