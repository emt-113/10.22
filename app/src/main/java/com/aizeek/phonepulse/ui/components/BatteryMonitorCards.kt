package com.aizeek.phonepulse.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aizeek.phonepulse.battery.*
import com.aizeek.phonepulse.ui.theme.*
import com.aizeek.phonepulse.util.TimeFormatter
import java.util.Locale

@Composable
private fun MonitorCard(
    title: String,
    icon: ImageVector? = null,
    iconTint: Color = NeonCyan,
    badge: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        SurfaceElevatedDark.copy(alpha = 0.95f),
                        SurfaceDark
                    )
                )
            )
            .border(1.dp, BorderDark, RoundedCornerShape(26.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (icon != null) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(iconTint.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                        }
                    }
                    Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                badge?.invoke()
            }
            content()
        }
    }
}

@Composable
private fun BentoTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = NeonCyan,
    subtext: @Composable (() -> Unit)? = null,
    accentColor: Color = TextPrimary
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark.copy(alpha = 0.7f))
            .border(1.dp, BorderDark.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(13.dp))
                }
                Text(label, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Text(value, color = accentColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            subtext?.invoke()
        }
    }
}

@Composable
private fun MonitorCallout(
    text: String,
    icon: ImageVector = Icons.Default.Info,
    tint: Color = NeonCyan
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(tint.copy(alpha = 0.08f))
            .border(0.5.dp, tint.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
        Text(text, color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
    }
}

private fun number(value: Double): String = String.format(Locale.getDefault(), "%.0f", value)
private fun percent(value: Double): String = String.format(Locale.getDefault(), "%.1f", value)
private fun duration(value: Long?): String = value?.let { TimeFormatter.formatDurationChinese(it) } ?: "暂无可靠估算"
private fun measuredCharge(cycle: BatteryCycle): String =
    if (cycle.measuredMs > 0) "${number(cycle.netMah)} mAh" else "暂无有效测量"
private fun bucketCharge(cycle: BatteryCycle, screenOn: Boolean): String {
    val duration = if (screenOn) cycle.screenOnMeasuredMs else cycle.screenOffMeasuredMs
    // Pre-v4 records lack bucket-duration evidence; do not infer it from the accumulated charge.
    return if (duration > 0 && cycle.screenOnMeasuredMs + cycle.screenOffMeasuredMs == cycle.measuredMs)
        "${number(if (screenOn) cycle.screenOnMah else cycle.screenOffMah)} mAh" else "暂无有效测量"
}
private fun coverage(cycle: BatteryCycle): String {
    val total = cycle.measuredMs.toDouble() + cycle.missingMs
    return if (total > 0) "${percent(cycle.measuredMs / total * 100)}%" else "暂无测量"
}

/** Human-readable explanation of where the current prediction comes from. */
private fun estimatesSourceText(source: String?, charging: Boolean): String? = when (source) {
    "SESSION" -> "依据本次充电实测速度，随采样动态刷新。"
    "SOC_HISTORY" -> if (charging)
        "依据近 30 天同电量段的历史充电速度；本次充电数据积累后估算会自动校准。"
    else "依据近 30 天历史耗电速度，随采样动态刷新。"
    "SOC_HISTORY_SESSION" -> "结合本次实测与近 30 天历史速度，随采样动态刷新。"
    "MAH_HISTORY" -> "依据近 30 天实测耗电速度，随采样动态刷新。"
    "HISTORY" -> "依据近 30 天历史耗电速度，随采样动态刷新。"
    else -> null
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BatteryMeasurementCard(state: BatteryMonitorUiState) {
    val measuring = state.isRunning && state.error == null
    val currentFormatted = state.currentUa?.takeIf { it.isFinite() }?.let {
        String.format(Locale.getDefault(), "%+.0f mA", it / 1000)
    } ?: "暂不可用"
    val isPositive = state.currentUa != null && state.currentUa > 0
    val isNegative = state.currentUa != null && state.currentUa < 0
    val currentColor = when {
        isPositive -> NeonEmerald
        isNegative -> NeonCyan
        else -> TextSecondary
    }

    MonitorCard(
        title = "实时监测",
        icon = Icons.Default.Speed,
        iconTint = NeonCyan,
        badge = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (measuring) NeonEmerald.copy(alpha = 0.12f) else SurfaceDark)
                    .border(0.5.dp, if (measuring) NeonEmerald.copy(alpha = 0.3f) else BorderDark, RoundedCornerShape(20.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (measuring) NeonEmerald else TextTertiary)
                )
                Text(
                    text = if (measuring) "持续监测中" else if (state.isRunning) "采样待恢复" else "监测未运行 · 开启保活守护可持续记录",
                    color = if (measuring) NeonEmerald else TextTertiary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    ) {
        // Hero Current Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            currentColor.copy(alpha = 0.12f),
                            SurfaceDark.copy(alpha = 0.8f)
                        )
                    )
                )
                .border(1.dp, currentColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "净电流 $currentFormatted",
                        color = if (state.currentUa != null && state.currentUa > 0) NeonEmerald else NeonCyan,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceDark.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(12.dp))
                        Text(
                            text = state.lastSampleTime?.let { TimeFormatter.formatSampleUpdateTime(it) } ?: "等待首次采样",
                            color = TextTertiary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                Text("正值充电，负值放电。", color = TextSecondary, fontSize = 12.sp)

            }
        }

        if (state.lastSampleTime != null && state.currentUa == null) {
            MonitorCallout(
                text = "电流读数不可用",
                icon = Icons.Default.Info,
                tint = AmberWarning
            )
        }
        state.error?.let {
            MonitorCallout(text = "监测提示：$it", icon = Icons.Default.BatteryAlert, tint = AmberWarning)
        }

        // Active Session Section
        state.activeCycle?.let { cycle ->
            HorizontalDivider(color = BorderDark, thickness = 0.8.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        if (cycle.charging) Icons.Default.Bolt else Icons.Default.BatteryFull,
                        contentDescription = null,
                        tint = if (cycle.charging) NeonEmerald else NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "本次${if (cycle.charging) "充电" else "放电"} ${cycle.startPct}% → ${cycle.endPct}%",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (cycle.charging) NeonEmerald.copy(0.12f) else NeonCyan.copy(0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    val delta = cycle.endPct - cycle.startPct
                    Text(
                        text = "${if (delta > 0) "+" else ""}$delta%",
                        color = if (cycle.charging) NeonEmerald else NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 2x2 Bento Grid of Active Session Metrics
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BentoTile(
                        label = "已测净电荷",
                        value = measuredCharge(cycle),
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.ElectricBolt,
                        iconTint = if (cycle.charging) NeonEmerald else NeonCyan,
                        accentColor = if (cycle.charging) NeonEmerald else NeonCyan,
                        subtext = {
                            Text(
                                "已测净电荷 ${measuredCharge(cycle)} · 覆盖率 ${coverage(cycle)}",
                                color = TextTertiary,
                                fontSize = 12.sp
                            )
                        }
                    )
                    BentoTile(
                        label = "有效测量时长",
                        value = duration(cycle.measuredMs),
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Schedule,
                        iconTint = ElectricViolet,
                        subtext = {
                            Text(
                                "测量 ${duration(cycle.measuredMs)} · 缺失 ${duration(cycle.missingMs)}",
                                color = TextTertiary,
                                fontSize = 12.sp
                            )
                        }
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BentoTile(
                        label = "亮屏电荷",
                        value = bucketCharge(cycle, true),
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.WbSunny,
                        iconTint = AmberWarning,
                        subtext = {
                            if (cycle.measuredMs > 0) {
                                Text(
                                    "亮屏 ${bucketCharge(cycle, true)} · 熄屏 ${bucketCharge(cycle, false)}",
                                    color = TextTertiary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    )
                    BentoTile(
                        label = "熄屏电荷",
                        value = bucketCharge(cycle, false),
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Nightlight,
                        iconTint = ElectricVioletLight
                    )
                }
            }

            // A legacy cycle may contain a fabricated zero before any measurement.
            if (cycle.charging && cycle.to100Mah != null && cycle.measuredMs > 0 && cycle.startPct < 100) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(NeonEmerald.copy(alpha = 0.08f))
                        .border(1.dp, NeonEmerald.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(15.dp))
                            Text("首次达到 100% 时累计 ${number(cycle.to100Mah)} mAh", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (cycle.netMah.isFinite() && cycle.to100Mah.isFinite()) {
                            Text("此后净充入 ${number(cycle.netMah - cycle.to100Mah)} mAh", color = NeonEmerald, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        } ?: MonitorCallout(
            text = "尚无正在记录的充放电会话",
            icon = Icons.Default.Info,
            tint = TextSecondary
        )
    }
}

@Composable
fun BatteryHealthCard(state: BatteryMonitorUiState,
    display: com.aizeek.phonepulse.intervention.BatteryHealthFacade.DisplayHealth? = null,
    designResolution: com.aizeek.phonepulse.intervention.DesignCapacityResolver.Resolution? = null) {
    val health = state.health
    val consistent = health.confidence == BatteryHealthConfidence.CONSISTENT
    val points = remember(state.cycles, state.activeCycle, state.settings.calibrationRevision) {
        (state.cycles + listOfNotNull(state.activeCycle))
            .mapNotNull { BatteryEstimates.healthSample(it, state.settings) }
            .distinctBy { it.id to it.startTime }.sortedBy { it.startTime }
    }

    MonitorCard(
        title = "容量与电池健康",
        icon = Icons.Default.Favorite,
        iconTint = NeonEmerald
    ) {
        // Hero Health Ring & Key Metrics Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            NeonEmerald.copy(alpha = 0.12f),
                            SurfaceDark.copy(alpha = 0.8f)
                        )
                    )
                )
                .border(1.dp, NeonEmerald.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                    val shownPct = display?.healthPct ?: health.healthPct
                    Text(
                        text = "容量保持率 ${shownPct?.let { "${percent(it)}%" } ?: "待估算"}",
                        color = NeonEmerald,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val shownCapacity = display?.capacityMah ?: health.capacityMah
                    Text(
                        text = "估算实际容量 ${shownCapacity?.let { "${number(it)} mAh" } ?: "暂无可靠估算"}",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    val designLabel = when (designResolution?.source) {
                        com.aizeek.phonepulse.intervention.DesignCapacityResolver.Source.MANUAL ->
                            "${number(designResolution.capacityMah!!)} mAh"
                        com.aizeek.phonepulse.intervention.DesignCapacityResolver.Source.SYSTEM ->
                            "系统报告 ${number(designResolution.capacityMah!!)} mAh"
                        com.aizeek.phonepulse.intervention.DesignCapacityResolver.Source.DEVICE_TABLE ->
                            "机型标称 ${number(designResolution.capacityMah!!)} mAh"
                        else -> "尚未设置"
                    }
                    Text(
                        text = "设计容量 $designLabel",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    val evidenceLabel = when (display?.source) {
                        com.aizeek.phonepulse.intervention.BatteryHealthFacade.Source.FORMAL ->
                            "有效样本 ${display.formalAcceptedCount} 次"
                        com.aizeek.phonepulse.intervention.BatteryHealthFacade.Source.FAST ->
                            "快速证据 ${display.fastSampleCount} 次"
                        else -> "有效样本 ${health.acceptedCount} 次"
                    }
                    Text(
                        text = evidenceLabel,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    if (health.acceptedCount > 0) {
                        Text("短会话 ${health.shortSampleCount} 次 · 大跨度 ${health.referenceSampleCount} 次", color = TextSecondary, fontSize = 12.sp)
                        Text("近期电量区间覆盖 ${health.socCoveragePct} 个百分点", color = TextSecondary, fontSize = 12.sp)
                    }
                }

                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.size(56.dp),
                        color = SurfaceElevatedDark,
                        strokeWidth = 6.dp
                    )
                    CircularProgressIndicator(
                        progress = { (((display?.healthPct ?: health.healthPct) ?: 0.0) / 100.0).coerceIn(0.0, 1.0).toFloat() },
                        modifier = Modifier.size(56.dp),
                        color = NeonEmerald,
                        strokeWidth = 6.dp
                    )
                    Text(
                        text = (display?.healthPct ?: health.healthPct)?.let { "${number(it)}%" } ?: "--",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if ((display?.healthPct ?: health.healthPct)?.let { it > 125 || it < 60 } == true) {
            MonitorCallout(
                text = "容量异常，请核对设计容量和校准设置。",
                icon = Icons.Default.BatteryAlert,
                tint = AmberWarning
            )
        }
        if (display?.anomalous == true) {
            MonitorCallout(
                text = "本次估算超出物理合理范围，可能为传感器异常或设计容量有误，结果仅供参考。",
                icon = Icons.Default.BatteryAlert,
                tint = AmberWarning
            )
        }

        // Stability & Assessment Row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BentoTile(
                label = "样本离散度",
                value = health.spreadPct?.let { "${percent(it)}%" } ?: "待更多样本",
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                iconTint = ElectricViolet,
                subtext = {
                    Text("样本离散程度 ${health.spreadPct?.let { "${percent(it)}%" } ?: "待更多样本"}", color = TextTertiary, fontSize = 12.sp)
                }
            )
            BentoTile(
                label = "一致性评估",
                value = when (health.confidence) {
                    BatteryHealthConfidence.CONSISTENT -> "样本较一致"
                    BatteryHealthConfidence.PRELIMINARY -> "初步估算"
                    BatteryHealthConfidence.INSUFFICIENT -> "等待样本"
                },
                modifier = Modifier.weight(1f),
                icon = if (consistent) Icons.Default.CheckCircle else Icons.Default.Info,
                iconTint = if (consistent) NeonEmerald else TextSecondary,
                accentColor = if (consistent) NeonEmerald else TextPrimary
            )
        }

        val shownCapacity = display?.capacityMah ?: health.capacityMah
        if (shownCapacity == null) {
            MonitorCallout(
                text = "暂无容量估算，请继续记录充电。",
                icon = Icons.Default.Info,
                tint = TextTertiary
            )
            state.cycles.firstOrNull { it.charging }?.let { latest ->
                val reason = when {
                    latest.excluded -> "已由你排除"
                    latest.calibrationRevision != state.settings.calibrationRevision -> "校准设置已变更，需要新样本"
                    else -> latest.rejectionReason?.let { rejectionLabel(it) }
                }
                if (reason != null) MonitorCallout(
                    text = "最近充电未计入：$reason",
                    icon = Icons.Default.Info,
                    tint = TextTertiary
                )
            }
        } else if (display?.source == com.aizeek.phonepulse.intervention.BatteryHealthFacade.Source.FAST) {
            MonitorCallout(
                text = "以上为快速估算（依据少量充电证据或系统电池数据），一次较长的完整充电可提升精度。",
                icon = Icons.Default.Info,
                tint = TextTertiary
            )
        }
        if (health.downWeightedCount > 0) {
            MonitorCallout(
                text = "异常样本 ${health.downWeightedCount} 次",
                icon = Icons.Default.Info,
                tint = AmberWarning
            )
        }
        if (designResolution?.source == com.aizeek.phonepulse.intervention.DesignCapacityResolver.Source.NONE) {
            MonitorCallout(
                text = "未能自动读取设计容量，请在“养护”页手动填写设备标称容量。",
                icon = Icons.Default.Tune,
                tint = NeonCyan
            )
        }

        // Capacity Trend Chart
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("容量趋势", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                if (points.isNotEmpty()) {
                    Text(
                        "按记录顺序 · ${number(points.first().estimatedCapacityMah!!)} → ${number(points.last().estimatedCapacityMah!!)} mAh",
                        color = NeonEmerald,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (points.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark.copy(alpha = 0.5f))
                        .border(1.dp, BorderDark.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("容量趋势：等待有效充电记录", color = TextTertiary, fontSize = 12.sp)
                }
            } else {
                val description = "容量趋势，${points.size} 次有效充电记录：" + points.joinToString("；") {
                    "${TimeFormatter.formatDateTime(it.startTime)}，${number(it.estimatedCapacityMah!!)} mAh"
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark.copy(alpha = 0.7f))
                        .border(1.dp, BorderDark.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Canvas(Modifier.fillMaxSize().semantics { contentDescription = description }) {
                        val values = points.map { it.estimatedCapacityMah!!.toFloat() }
                        val min = values.min() * .95f
                        val max = (values.max() * 1.05f).coerceAtLeast(min + 1)
                        val inset = 8.dp.toPx()
                        val plotW = (size.width - inset * 2).coerceAtLeast(1f)
                        val plotH = (size.height - inset * 2).coerceAtLeast(1f)

                        val coords = values.mapIndexed { index, value ->
                            Offset(
                                if (values.size == 1) size.width / 2 else inset + index.toFloat() / (values.size - 1) * plotW,
                                size.height - inset - (value - min) / (max - min) * plotH
                            )
                        }

                        // Gradient Area Fill
                        if (coords.size >= 2) {
                            val path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(coords.first().x, coords.first().y)
                                coords.drop(1).forEach { lineTo(it.x, it.y) }
                                lineTo(coords.last().x, size.height)
                                lineTo(coords.first().x, size.height)
                                close()
                            }
                            drawPath(
                                path,
                                Brush.verticalGradient(
                                    listOf(NeonEmerald.copy(alpha = 0.2f), Color.Transparent),
                                    startY = 0f,
                                    endY = size.height
                                )
                            )
                        }

                        // Trend Line
                        coords.zipWithNext().forEach { (a, b) ->
                            drawLine(NeonEmerald, a, b, 2.5.dp.toPx(), StrokeCap.Round)
                        }
                        // Dots
                        coords.forEach {
                            drawCircle(NeonEmerald.copy(alpha = 0.3f), 6.dp.toPx(), it)
                            drawCircle(NeonEmerald, 3.dp.toPx(), it)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BatteryMaintenanceCard(
    state: BatteryMonitorUiState,
    onSave: (BatteryMonitorSettings) -> Unit,
    onMute: () -> Unit
) {
    var editing by rememberSaveable { mutableStateOf(false) }

    MonitorCard(
        title = "充电养护与预计时间",
        icon = Icons.Default.Shield,
        iconTint = ElectricVioletLight
    ) {
        // Smart Target Charge Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            ElectricViolet.copy(alpha = 0.12f),
                            SurfaceDark.copy(alpha = 0.8f)
                        )
                    )
                )
                .border(1.dp, ElectricViolet.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("目标电量提醒", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("达到 ${state.settings.chargeTargetPct}% 时提醒", color = ElectricVioletLight, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    Switch(
                        checked = state.settings.chargeAlarmEnabled,
                        onCheckedChange = { onSave(state.settings.copy(chargeAlarmEnabled = it)) },
                        modifier = Modifier.testTag("battery_alarm_switch").semantics { contentDescription = "目标电量提醒" },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextPrimary,
                            checkedTrackColor = ElectricViolet,
                            uncheckedTrackColor = SurfaceElevatedDark
                        )
                    )
                }

                // Target Progress indicator
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { state.settings.chargeTargetPct / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = ElectricViolet,
                        trackColor = SurfaceElevatedDark
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("0%", color = TextTertiary, fontSize = 11.5.sp)
                        Text("养护目标 ${state.settings.chargeTargetPct}%", color = ElectricVioletLight, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        Text("100%", color = TextTertiary, fontSize = 11.5.sp)
                    }
                }

                Text("提醒需要允许通知；不会自动停止充电。", color = TextSecondary, fontSize = 12.sp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { editing = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = NeonCyan)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("编辑设置", fontSize = 13.sp)
                    }

                    state.activeCycle?.takeIf { it.charging && state.settings.chargeAlarmEnabled }?.let {
                        if (it.alarmMuted) {
                            Text("本次充电提醒已静音", color = TextTertiary, fontSize = 12.sp)
                        } else {
                            TextButton(
                                onClick = onMute,
                                colors = ButtonDefaults.textButtonColors(contentColor = AmberWarning)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.VolumeOff, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("本次充电静音", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // Remaining Time Predictions Section
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("续航与充电预测", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            val charging = state.activeCycle?.charging
            if (charging == true) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BentoTile(
                        label = "距目标 ${state.settings.chargeTargetPct}%",
                        value = duration(state.estimates.toTargetMs),
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Bolt,
                        iconTint = ElectricVioletLight,
                        accentColor = ElectricVioletLight
                    )
                    BentoTile(
                        label = "距充满 100%",
                        value = duration(state.estimates.toFullMs),
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.BatteryChargingFull,
                        iconTint = NeonEmerald,
                        accentColor = NeonEmerald
                    )
                }
                val noEstimate = state.estimates.toTargetMs == null && state.estimates.toFullMs == null
                val note = if (noEstimate) "正在积累本次充电数据，稍后会自动给出预估时间。"
                else estimatesSourceText(state.estimates.source, charging = true)
                note?.let {
                    Text(
                        text = it,
                        color = TextTertiary,
                        fontSize = 11.5.sp,
                        lineHeight = 14.sp
                    )
                }
            } else if (charging == false) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BentoTile(
                        label = "持续亮屏",
                        value = duration(state.estimates.screenOnRemainingMs),
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.WbSunny,
                        iconTint = NeonCyan,
                        accentColor = NeonCyan
                    )
                    BentoTile(
                        label = "持续待机",
                        value = duration(state.estimates.screenOffRemainingMs),
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Nightlight,
                        iconTint = ElectricVioletLight,
                        accentColor = ElectricVioletLight
                    )
                    BentoTile(
                        label = "综合日常",
                        value = duration(state.estimates.mixedRemainingMs),
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Speed,
                        iconTint = NeonEmerald,
                        accentColor = NeonEmerald
                    )
                }
                Text(
                    text = estimatesSourceText(state.estimates.source, charging = false)
                        ?: "正在积累耗电数据，稍后会自动给出预估时间。",
                    color = TextTertiary,
                    fontSize = 11.5.sp,
                    lineHeight = 14.sp
                )
            } else {
                MonitorCallout(
                    text = "预计时间：等待有效充放电测量",
                    icon = Icons.Default.Info,
                    tint = TextSecondary
                )
            }
        }
    }

    if (editing) {
        BatterySettingsDialog(state.settings, { editing = false }) { onSave(it); editing = false }
    }
}

@Composable
private fun BatterySettingsDialog(
    settings: BatteryMonitorSettings,
    onDismiss: () -> Unit,
    onSave: (BatteryMonitorSettings) -> Unit
) {
    var design by rememberSaveable { mutableStateOf(settings.designCapacityMah?.let(::number) ?: "") }
    var target by rememberSaveable { mutableStateOf(settings.chargeTargetPct.toString()) }
    var scale by rememberSaveable { mutableStateOf(settings.currentScale.toString()) }
    var enabled by rememberSaveable { mutableStateOf(settings.chargeAlarmEnabled) }
    var invert by rememberSaveable { mutableStateOf(settings.invertCurrent) }
    var cells by rememberSaveable { mutableStateOf(settings.cellFactor) }
    var advanced by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        modifier = Modifier.testTag("battery_settings_dialog"),
        onDismissRequest = onDismiss,
        title = { Text("电池设置", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                Text("不确定的项目保持默认即可，不影响正常使用。", color = TextSecondary, fontSize = 12.sp)
                Column(
                    modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = design,
                        onValueChange = { design = it },
                        label = { Text("设计容量（mAh，可留空）") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag("battery_design_input")
                    )
                    Text(
                        "设备官方标称的电池容量，会影响健康度估算。不确定就留空，App 会尝试自动识别（系统读数或内置机型表）。",
                        color = TextTertiary, fontSize = 12.sp, lineHeight = 17.sp
                    )

                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it },
                        label = { Text("提醒目标（50–100%）") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("battery_target_input")
                    )
                    Text(
                        "充电到这个电量时提醒你，避免长时间满电；建议设 80%，有利于延长电池寿命。",
                        color = TextTertiary, fontSize = 12.sp, lineHeight = 17.sp
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = enabled, onCheckedChange = { enabled = it })
                        Text("开启目标提醒", color = TextPrimary, fontSize = 13.sp)
                    }

                    TextButton(onClick = { advanced = !advanced }) {
                        Text(if (advanced) "收起测量校准" else "测量校准（高级）", color = NeonCyan)
                    }

                    if (advanced) {
                        Text(
                            "多数手机不需要动这几项；只有当电流读数明显不对（方向相反或大小偏差固定倍数）时才调整。修改后会开始新的记录会话，旧记录不会重新计算。",
                            color = TextTertiary, fontSize = 12.sp, lineHeight = 17.sp
                        )
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = invert, onCheckedChange = { invert = it })
                                Text("反转电流方向", color = TextPrimary, fontSize = 13.sp)
                            }
                            Text(
                                "如果充电时显示负数、放电时显示正数（方向反了），勾上即可纠正。",
                                color = TextTertiary, fontSize = 12.sp, lineHeight = 17.sp,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                        OutlinedTextField(
                            value = scale,
                            onValueChange = { scale = it },
                            label = { Text("电流倍率（0.001–1000）") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth().testTag("battery_scale_input")
                        )
                        Text(
                            "正常保持 1.0；仅当电流数值整体偏大或偏小固定倍数时才调整。",
                            color = TextTertiary, fontSize = 12.sp, lineHeight = 17.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = cells == 1, onClick = { cells = 1 })
                            Text("不换算", color = TextPrimary, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(16.dp))
                            RadioButton(selected = cells == 2, onClick = { cells = 2 })
                            Text("双电芯 ×2", color = TextPrimary, fontSize = 13.sp)
                        }
                        Text(
                            "普通手机选『不换算』；少数内置双电芯的机型读数需 ×2 才等于整块电池，不确定就选不换算。",
                            color = TextTertiary, fontSize = 12.sp, lineHeight = 17.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val capacity = design.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
                val targetValue = target.trim().toIntOrNull()
                val scaleValue = scale.trim().toDoubleOrNull()
                error = when {
                    design.isNotBlank() && (capacity == null || !capacity.isFinite() || capacity !in 500.0..30000.0) -> "设计容量需为 500–30000 mAh，或留空"
                    targetValue == null || targetValue !in 50..100 -> "目标电量需为 50–100%"
                    scaleValue == null || !scaleValue.isFinite() || scaleValue !in 0.001..1000.0 -> "电流倍率需为 0.001–1000"
                    else -> null
                }
                if (error == null) onSave(
                    settings.copy(
                        designCapacityMah = capacity,
                        chargeTargetPct = targetValue!!,
                        chargeAlarmEnabled = enabled,
                        currentScale = scaleValue!!,
                        invertCurrent = invert,
                        cellFactor = cells
                    )
                )
            }) { Text("保存", color = NeonCyan) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = TextTertiary) }
        }
    )
}

@Composable
fun BatterySessionsCard(cycles: List<BatteryCycle>, onExclude: (Long, Boolean) -> Unit) {
    MonitorCard(
        title = "最近充放电记录",
        icon = Icons.Default.Schedule,
        iconTint = NeonCyan,
        badge = {
            if (cycles.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("${cycles.take(10).size} 条记录", color = TextTertiary, fontSize = 12.sp)
                }
            }
        }
    ) {
        if (cycles.isEmpty()) {
            MonitorCallout(
                text = "暂无会话记录，持续监测后会自动记录。",
                icon = Icons.Default.Info,
                tint = TextSecondary
            )
        }

        cycles.sortedByDescending { it.startTime }.take(10).forEach { cycle ->
            var expanded by rememberSaveable(cycle.id) { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(SurfaceDark.copy(alpha = 0.85f))
                    .border(1.dp, BorderDark, RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Clickable Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = !expanded }
                            .testTag("battery_cycle_${cycle.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (cycle.charging) NeonEmerald.copy(alpha = 0.12f)
                                        else NeonCyan.copy(alpha = 0.12f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (cycle.charging) Icons.Default.Bolt else Icons.Default.BatteryFull,
                                    contentDescription = null,
                                    tint = if (cycle.charging) NeonEmerald else NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "${if (cycle.charging) "充电" else "放电"} ${cycle.startPct}% → ${cycle.endPct}%${if (cycle.endTime == null) " · 进行中" else ""}",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${TimeFormatter.formatDateTime(cycle.startTime)} · ${if (expanded) "收起详情" else "展开详情"}",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Status Pill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceElevatedDark.copy(alpha = 0.5f))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(if (cycle.excluded) AmberWarning else TextTertiary)
                        )
                        Text(
                            text = if (cycle.excluded) "已由你排除，不计入健康估算" else rejectionLabel(cycle.rejectionReason),
                            color = if (cycle.excluded) AmberWarning else TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    // Expandable Details
                    AnimatedVisibility(
                        visible = expanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            HorizontalDivider(color = BorderDark, thickness = 0.5.dp)

                            // 2-column detail grid
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("结束 ${cycle.endTime?.let(TimeFormatter::formatDateTime) ?: "尚未结束"}", color = TextSecondary, fontSize = 12.sp)
                                Text("净电荷 ${measuredCharge(cycle)} · 覆盖率 ${coverage(cycle)}", color = TextSecondary, fontSize = 12.sp)
                                Text("有效测量 ${duration(cycle.measuredMs)} · 缺失 ${duration(cycle.missingMs)}", color = TextSecondary, fontSize = 12.sp)
                                if (cycle.measuredMs > 0) {
                                    Text("亮屏 ${bucketCharge(cycle, true)} · 熄屏 ${bucketCharge(cycle, false)}", color = TextSecondary, fontSize = 12.sp)
                                }
                                Text("时钟观测挂起 ${duration(cycle.deepSleepMs)} · ≥80% 观测区间 ${duration(cycle.highSocMs)}", color = TextSecondary, fontSize = 12.sp)
                                Text("采样峰值温度 ${cycle.maxTemperatureC?.let { "${percent(it)}°C" } ?: "未知"}", color = TextSecondary, fontSize = 12.sp)
                                Text("本次容量估算 ${cycle.estimatedCapacityMah?.let { "${number(it)} mAh" } ?: "暂无可靠估算"}", color = TextSecondary, fontSize = 12.sp)
                            }

                            if (cycle.to100Mah != null && cycle.measuredMs > 0 && cycle.startPct < 100) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(NeonEmerald.copy(0.08f))
                                        .padding(10.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("首次达到 100% 时累计 ${number(cycle.to100Mah)} mAh", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        if (cycle.netMah.isFinite() && cycle.to100Mah.isFinite()) {
                                            Text("此后净充入 ${number(cycle.netMah - cycle.to100Mah)} mAh", color = NeonEmerald, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }

                            if (cycle.endTime != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = { onExclude(cycle.id, !cycle.excluded) }
                                    ) {
                                        Text(
                                            text = if (cycle.excluded) "重新纳入此记录" else "排除此记录",
                                            color = if (cycle.excluded) NeonCyan else CoralRose,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun rejectionLabel(reason: String?): String = when (reason) {
    null -> "满足条件的充电记录可用于容量估算"
    "NOT_CHARGING", "not_charging" -> "放电记录，不用于容量估算"
    "SMALL_SOC_CHANGE", "INSUFFICIENT_SOC", "insufficient_soc", "small_soc_delta" -> "电量变化不足，需要更大充电跨度"
    "LOW_COVERAGE", "low_coverage", "insufficient_coverage" -> "测量缺失较多，暂不用于容量估算"
    "SOC_DISCONTINUITY", "soc_discontinuity" -> "电量出现跳变，暂不用于容量估算"
    "INVALID_CAPACITY", "invalid_capacity" -> "容量读数异常，暂不用于容量估算"
    "INCOMPLETE", "incomplete" -> "记录不完整，暂不用于容量估算"
    "REBOOT" -> "设备重启中断了测量，暂不用于容量估算"
    "CALIBRATION_CHANGED" -> "测量校准已调整，旧会话已结束"
    "INCOMPLETE_RESTORE" -> "恢复时缺少完整记录，暂不用于容量估算"
    "NO_MEASUREMENT" -> "暂无有效电荷测量，暂不用于容量估算"
    "UNQUALIFIED_FULL_PREFIX" -> "满充时未保留合格测量，暂不用于容量估算"
    "NON_POSITIVE_CHARGE" -> "未测得有效净充入电荷，暂不用于容量估算"
    else -> "此记录不满足可靠容量估算条件"
}
