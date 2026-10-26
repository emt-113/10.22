package com.aizeek.phonepulse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aizeek.phonepulse.BuildConfig
import com.aizeek.phonepulse.R
import androidx.compose.ui.res.stringResource
import com.aizeek.phonepulse.ui.theme.BevelAmberDeep
import com.aizeek.phonepulse.ui.theme.BevelMintDeep
import com.aizeek.phonepulse.ui.theme.BorderDark
import com.aizeek.phonepulse.ui.theme.CoralRose
import com.aizeek.phonepulse.ui.theme.NeonCyan
import com.aizeek.phonepulse.ui.theme.NeonEmerald
import com.aizeek.phonepulse.ui.theme.SurfaceDark
import com.aizeek.phonepulse.ui.theme.SurfaceElevatedDark
import com.aizeek.phonepulse.ui.theme.TextPrimary
import com.aizeek.phonepulse.ui.theme.TextSecondary
import com.aizeek.phonepulse.ui.theme.TextTertiary
import com.aizeek.phonepulse.ui.components.FontSizeDialCard
import com.aizeek.phonepulse.update.UpdatePhase
import com.aizeek.phonepulse.update.UpdateRepository
import com.aizeek.phonepulse.update.UpdateState
import java.util.Locale

@Composable
fun SettingsScreen(
    updateRepository: UpdateRepository,
    onCheckUpdates: () -> Unit,
    isServiceRunning: Boolean,
    isBatteryIgnoring: Boolean,
    hasUsagePermission: Boolean,
    hasNotificationPermission: Boolean,
    onToggleService: (Boolean) -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onRequestUsagePermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestAutoStart: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val updateState by updateRepository.state.collectAsStateWithLifecycle(minActiveState = Lifecycle.State.RESUMED)
    var expandedVendorGuide by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Column {
                Text(
                    text = "系统设置",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
            }
        }

        // Section 1: Version and Updates Card
        item {
            VersionUpdateCard(
                state = updateState,
                onCheckUpdates = onCheckUpdates
            )
        }

        // Section 2: KeepAlive and Protection Center Header
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "后台保活与系统权限",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // KeepAlive items (Health Score, Core Protections, Vendor Guide)
        keepAliveItems(
            isServiceRunning = isServiceRunning,
            isBatteryIgnoring = isBatteryIgnoring,
            hasUsagePermission = hasUsagePermission,
            hasNotificationPermission = hasNotificationPermission,
            onToggleService = onToggleService,
            onRequestBatteryOptimization = onRequestBatteryOptimization,
            onRequestUsagePermission = onRequestUsagePermission,
            onRequestNotificationPermission = onRequestNotificationPermission,
            onRequestAutoStart = onRequestAutoStart,
            expandedVendorGuide = expandedVendorGuide,
            onToggleVendorGuide = { expandedVendorGuide = !expandedVendorGuide }
        )

        // Section 2.5: Notification preferences — user chooses how reminders
        // appear (heads-up / lock screen / sound / vibration) in system settings.
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "通知与提醒",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        item {
            NotificationPrefsCard()
        }

        // Reading font size ruler (persists; drives the history timeline text).
        item {
            FontSizeDialCard()
        }

        // Section 3: About & Local Storage Privacy
        item {
            AboutAppCard()
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun VersionUpdateCard(
    state: UpdateState,
    onCheckUpdates: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(SurfaceDark)
            .border(1.dp, if (state.hasUpdate) CoralRose.copy(alpha = 0.5f) else BorderDark, RoundedCornerShape(22.dp))
            .padding(18.dp)
            .testTag("settings_version_card")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Top Row: App Icon + App Name + Version Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NeonCyan.copy(alpha = 0.15f))
                            .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.app_name),
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SurfaceElevatedDark)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "v${BuildConfig.VERSION_NAME}",
                                    color = NeonCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "版本更新",
                            color = TextTertiary,
                            fontSize = 12.sp
                        )
                    }
                }

                if (state.hasUpdate) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(CoralRose)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .testTag("update_badge")
                    ) {
                        Text(
                            text = "NEW",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Middle section: Status description & Action
            when {
                state.hasUpdate -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(CoralRose.copy(alpha = 0.10f))
                            .border(1.dp, CoralRose.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🎉 发现新版本：${state.release?.version?.name ?: ""}",
                            color = CoralRose,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (!state.release?.title.isNullOrBlank()) {
                            Text(
                                text = state.release?.title ?: "",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                maxLines = 2
                            )
                        }
                        Button(
                            onClick = onCheckUpdates,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CoralRose,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_update_btn")
                        ) {
                            Text(
                                text = if (state.phase == UpdatePhase.READY) "立即安装更新" else "查看更新详情并下载",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                state.phase == UpdatePhase.CHECKING -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = NeonCyan,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "正在检查最新发布版本…",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
                state.isDownloading -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "正在下载新版本…",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "${formatBytes(state.downloadedBytes)} / ${formatBytes(state.totalBytes)}",
                                color = NeonCyan,
                                fontSize = 12.sp
                            )
                        }
                        if (state.totalBytes > 0) {
                            LinearProgressIndicator(
                                progress = { (state.downloadedBytes.toFloat() / state.totalBytes).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth(),
                                color = NeonCyan
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = NeonCyan
                            )
                        }
                    }
                }
                state.phase == UpdatePhase.READY -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "安装包已准备就绪",
                            color = BevelMintDeep,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Button(
                            onClick = onCheckUpdates,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonEmerald.copy(alpha = 0.14f),
                                contentColor = BevelMintDeep
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("立即安装", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = BevelMintDeep,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "当前已是最新版本",
                                color = BevelMintDeep,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        OutlinedButton(
                            onClick = onCheckUpdates,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = NeonCyan
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("check_updates_button")
                        ) {
                            Text("检查更新", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

        }
    }
}

@Composable
private fun AboutAppCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderDark, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.settings_about_title, stringResource(R.string.app_name)),
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "使用记录与电量数据仅保存在本机，不会上传。",
                color = TextTertiary,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}

private fun formatBytes(bytes: Long): String = String.format(Locale.getDefault(), "%.1f MB", bytes.coerceAtLeast(0) / 1_048_576.0)

/**
 * 通知偏好卡（方案二）：不在应用内复刻系统开关，而是引导用户到系统通知设置，
 * 自行选择悬浮提醒 / 锁屏显示 / 声音 / 振动。常驻状态通知默认静音 + 锁屏可见。
 */
@Composable
private fun NotificationPrefsCard() {
    val context = androidx.compose.ui.platform.LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderDark, RoundedCornerShape(22.dp))
            .padding(18.dp)
            .testTag("settings_notification_prefs")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonCyan.copy(alpha = 0.15f))
                        .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "通知方式由你选择",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "悬浮 · 锁屏 · 声音 · 振动，按你的习惯设置",
                        color = TextTertiary,
                        fontSize = 12.sp
                    )
                }
            }
            Text(
                text = "常驻状态通知已默认静音不打扰，并支持在锁屏上直接查看当前亮屏/熄屏状态。" +
                    "教练提醒与充电提醒的通知方式，在系统通知设置里自行开启或关闭。",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
            Button(
                onClick = {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan.copy(alpha = 0.14f),
                    contentColor = NeonCyan
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_notification_settings_btn")
            ) {
                Text("前往系统通知设置", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            // Per-channel shortcuts: every OEM buries notification settings somewhere
            // different, so jump straight into each channel's own system page.
            NotificationChannelJumpButton(
                "使用教练提醒 · 频道直达",
                com.aizeek.phonepulse.intervention.UsageCoachNotifier.CHANNEL_ID,
                "open_coach_channel_btn"
            )
            NotificationChannelJumpButton(
                "充电目标提醒 · 频道直达",
                com.aizeek.phonepulse.battery.BatteryChargeAlarm.CHANNEL,
                "open_charge_channel_btn"
            )
            NotificationChannelJumpButton(
                "屏幕状态监测 · 频道直达",
                com.aizeek.phonepulse.service.ScreenTrackerService.CHANNEL_ID,
                "open_monitor_channel_btn"
            )
        }
    }
}

@Composable
private fun NotificationChannelJumpButton(label: String, channelId: String, tag: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    OutlinedButton(
        onClick = {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val intent = android.content.Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                    .putExtra(android.provider.Settings.EXTRA_CHANNEL_ID, channelId)
                context.startActivity(intent)
            } else {
                context.startActivity(
                    android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                )
            }
        },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
    ) {
        Text(label, fontSize = 13.sp, color = TextSecondary)
    }
}
