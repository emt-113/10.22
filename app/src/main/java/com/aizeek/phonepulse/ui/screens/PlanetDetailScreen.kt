package com.aizeek.phonepulse.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aizeek.phonepulse.intervention.PlanetDayStore
import com.aizeek.phonepulse.intervention.PlanetEngine
import com.aizeek.phonepulse.intervention.WeeklyInsightsEngine
import com.aizeek.phonepulse.ui.components.PlanetGlyph
import com.aizeek.phonepulse.ui.components.stageColor
import com.aizeek.phonepulse.ui.components.stageLabel
import com.aizeek.phonepulse.ui.theme.BevelAmber
import com.aizeek.phonepulse.ui.theme.BevelPageBottom
import com.aizeek.phonepulse.ui.theme.NeonCyan
import com.aizeek.phonepulse.ui.theme.NeonEmerald
import com.aizeek.phonepulse.ui.theme.SurfaceDark
import com.aizeek.phonepulse.ui.theme.TextPrimary
import com.aizeek.phonepulse.ui.theme.TextSecondary
import com.aizeek.phonepulse.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Planet detail (intervention): stage rules, streak, 7-day real records, and the
 * local weekly insight — all computed from on-device day records.
 */
@Composable
fun PlanetDetailScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val store = remember { PlanetDayStore(context) }
    val today = remember { PlanetEngine.todayKey() }
    val lastWeekEnd = remember { PlanetEngine.shiftKey(today, -7) }
    val thisWeek = remember { store.loadRange(today, 7) }
    val lastWeek = remember { store.loadRange(lastWeekEnd, 7) }
    val records = remember { (0 until 7).map { o -> store.get(PlanetEngine.shiftKey(today, -o)) } }
    val stats = remember { WeeklyInsightsEngine.compute(thisWeek, lastWeek) }
    val stage = remember { PlanetEngine.stage(records, today) }
    val streak = remember { PlanetEngine.consecutiveAchievedDays(records) }

    // System back closes the detail sheet instead of leaving the app.
    BackHandler { onClose() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BevelPageBottom)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("脉搏星球", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "关闭", tint = TextSecondary)
            }
        }

        // Stage hero
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceDark)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PlanetGlyph(stage = stage, size = 72.dp)
            Text(stageLabel(stage, streak), color = stageColor(stage), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "星球反映每日亮屏目标的达成情况，与电池健康无关。规则：最近 2 个有效天全部达成 → 运转；最近 3 个 → 闪耀。",
                color = TextTertiary, fontSize = 11.sp
            )
        }

        // Streak
        StatRow("连续达成", "$streak 天", NeonEmerald)

        // Weekly insight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceDark)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("每周洞察", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            StatRow("本周平均亮屏", stats.avgScreenOnMinutes?.let { "$it 分钟" } ?: "数据不足", NeonCyan)
            StatRow("预算达成", "${stats.achievedDays}/${stats.daysWithBudget} 天", NeonCyan)
            StatRow(
                "与上周对比",
                stats.comparisonDeltaMinutes?.let {
                    if (it >= 0) "多 $it 分钟" else "少 ${-it} 分钟"
                } ?: "继续记录，生成趋势",
                TextSecondary
            )
            if (stats.suggestion != null) {
                Text(stats.suggestion, color = NeonEmerald, fontSize = 12.sp)
            }
        }

        // 7-day records
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceDark)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("最近 7 天", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            records.forEachIndexed { index, record ->
                // Index-based: indexOf() would return the first null for every
                // missing day and label them all with today's date.
                val dayLabel = remember(today, index) {
                    SimpleDateFormat("MM-dd", Locale.getDefault()).format(
                        SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(
                            PlanetEngine.shiftKey(today, -index)
                        ) ?: Date()
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(dayLabel, color = TextSecondary, fontSize = 13.sp)
                    when {
                        record == null -> Text("无记录", color = TextTertiary, fontSize = 13.sp)
                        record.achieved == null -> Text("${record.screenOnMinutes} 分钟 · 未设目标", color = TextSecondary, fontSize = 13.sp)
                        record.achieved -> Text("${record.screenOnMinutes}/${record.budgetMinutes} 分钟 · 达成", color = NeonEmerald, fontSize = 13.sp)
                        else -> Text("${record.screenOnMinutes}/${record.budgetMinutes} 分钟 · 超额", color = BevelAmber, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
