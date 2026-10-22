package com.aizeek.phonepulse.ui.components

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aizeek.phonepulse.intervention.UsageCoachSettings
import com.aizeek.phonepulse.ui.theme.BorderDark
import com.aizeek.phonepulse.ui.theme.NeonCyan
import com.aizeek.phonepulse.ui.theme.NeonEmerald
import com.aizeek.phonepulse.ui.theme.SurfaceDark
import com.aizeek.phonepulse.ui.theme.TextPrimary
import com.aizeek.phonepulse.ui.theme.TextSecondary
import com.aizeek.phonepulse.ui.theme.TextTertiary
import com.aizeek.phonepulse.ui.theme.AmberWarning

/**
 * Settings card for the usage coach (intervention). All knobs are runtime-tunable:
 * for a demo, set the budget to e.g. 1 minute and the continuous limit to 1 minute
 * to see both alerts fire within a couple of minutes.
 */
@Composable
fun UsageCoachCard() {
    val context = LocalContext.current
    val settings = remember { UsageCoachSettings(context) }
    var enabled by remember { mutableStateOf(settings.current.enabled) }
    var budget by remember { mutableStateOf(settings.current.budgetMinutes?.toString() ?: "") }
    var continuous by remember { mutableStateOf(settings.current.continuousLimitMinutes?.toString() ?: "") }
    var grace by remember { mutableStateOf(settings.current.graceSeconds) }
    var savedTick by remember { mutableStateOf(false) }

    fun persist() {
        // Lower bound 1: the demo path is exactly "budget=1"; do not reject it.
        val budgetVal = budget.trim().toIntOrNull()?.takeIf { it in 1..1440 }
        val contVal = continuous.trim().toIntOrNull()?.takeIf { it in 1..720 }
        settings.save(enabled, budgetVal, contVal, grace)
        savedTick = true
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Alarm, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                Text("使用教练", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Switch(checked = enabled, onCheckedChange = {
                enabled = it
                persist()
            })
        }
        Text(
            "预算与连续提醒相互独立，各有每日冷却；提醒走系统通知。",
            color = TextTertiary, fontSize = 11.sp
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = budget,
                onValueChange = {
                    budget = it.filter { c -> c.isDigit() }.take(4)
                    persist() // live-save: no button dependency for demos
                },
                label = { Text("每日预算(分)") },
                placeholder = { Text("留空关闭", color = TextTertiary, fontSize = 11.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 14.sp)
            )
            OutlinedTextField(
                value = continuous,
                onValueChange = {
                    continuous = it.filter { c -> c.isDigit() }.take(3)
                    persist()
                },
                label = { Text("连续上限(分)") },
                placeholder = { Text("留空关闭", color = TextTertiary, fontSize = 11.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 14.sp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("短熄屏宽限（合并判定）", color = TextSecondary, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(15, 30, 60).forEach { sec ->
                    TextButton(onClick = { grace = sec }, enabled = grace != sec) {
                        Text(
                            "${sec}s",
                            color = if (grace == sec) NeonEmerald else TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (grace == sec) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                if (savedTick) "已保存，立即生效" else "演示技巧：预算填 2、连续填 1，几分钟内即可看到两级提醒",
                color = if (savedTick) NeonEmerald else TextTertiary,
                fontSize = 11.sp,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { persist() }) { Text("保存", color = NeonCyan, fontWeight = FontWeight.Bold) }
        }
        // MIUI gates heads-up + sound at the app level; give users a direct way out.
        TextButton(onClick = {
            val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
            context.startActivity(intent)
        }) {
            Text("收不到悬浮提醒或没声音？点此开启系统通知权限", color = TextSecondary, fontSize = 11.sp)
        }
    }
}
