package com.aizeek.phonepulse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aizeek.phonepulse.data.HourlyScreenStat
import com.aizeek.phonepulse.data.ScreenSession
import com.aizeek.phonepulse.ui.components.ActivityBarChart
import com.aizeek.phonepulse.ui.components.BentoStatCards
import com.aizeek.phonepulse.ui.components.CompanionHomeEntry
import com.aizeek.phonepulse.companion.CompanionUiState
import com.aizeek.phonepulse.companion.CompanionPrompt
import com.aizeek.phonepulse.ui.theme.BevelBlue
import com.aizeek.phonepulse.ui.theme.BevelCard
import com.aizeek.phonepulse.ui.theme.BevelCardBorder
import com.aizeek.phonepulse.ui.theme.BevelPageBottom
import com.aizeek.phonepulse.ui.theme.BevelPageTop
import com.aizeek.phonepulse.ui.theme.BevelTextPrimary
import com.aizeek.phonepulse.ui.theme.BevelTextTertiary
import com.aizeek.phonepulse.ui.theme.BevelViolet
import com.aizeek.phonepulse.util.TimeFormatter

/**
 * Home surface (Bevel light style). The intervention hub (planet hero, status
 * strip, budget ring, health/charge cards, tip) is injected via the
 * [planetHeader] slot; all other sections keep their original data sources.
 */
@Composable
fun OverviewScreen(
    isScreenOn: Boolean,
    stateStartTimeMs: Long,
    isServiceRunning: Boolean,
    lastScreenOffDurationMs: Long?,
    lastScreenOnDurationMs: Long?,
    todayTotalScreenOnMs: Long,
    todayTotalScreenOffMs: Long,
    todayWakeCount: Int,
    hourlyStats: List<HourlyScreenStat>,
    recentSessions: List<ScreenSession>,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier,
    companion: CompanionUiState? = null,
    onNavigateToCompanion: () -> Unit = {},
    onCompanionAction: (CompanionPrompt) -> Unit = { onNavigateToCompanion() },
    planetHeader: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BevelPageTop, BevelPageBottom)))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Intervention hub: planet hero + status + budget + health cards.
            item { planetHeader() }

            if (companion != null) item {
                CompanionHomeEntry(companion, onNavigateToCompanion, serviceRunning = isServiceRunning, onAction = onCompanionAction)
            }

            item {
                // Bento Grid Stat Cards
                BentoStatCards(
                    lastScreenOffDurationMs = lastScreenOffDurationMs,
                    lastScreenOnDurationMs = lastScreenOnDurationMs,
                    todayTotalScreenOnMs = todayTotalScreenOnMs,
                    todayTotalScreenOffMs = todayTotalScreenOffMs,
                    todayWakeCount = todayWakeCount
                )
            }

            item {
                // 24-Hour Activity Bar Chart
                ActivityBarChart(
                    hourlyStats = hourlyStats
                )
            }

            item {
                // Recent Sessions Stream Section Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "最近状态日志",
                        color = BevelTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { onNavigateToHistory() }
                            .padding(4.dp)
                    ) {
                        Text(
                            text = "查看全部",
                            color = BevelBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "View all history",
                            tint = BevelBlue,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            // Recent items
            val displaySessions = recentSessions.take(4)
            if (displaySessions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(BevelCard)
                            .border(1.dp, BevelCardBorder, RoundedCornerShape(18.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无状态日志，等待屏幕状态切换记录",
                            color = BevelTextTertiary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(displaySessions.size, key = { displaySessions[it].id }) { index ->
                    val session = displaySessions[index]
                    RecentSessionRow(session = session)
                }
            }

            // Extra bottom clearance so the last card never hides behind the nav bar.
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun RecentSessionRow(
    session: ScreenSession,
    modifier: Modifier = Modifier
) {
    val isScreenOn = session.isScreenOn
    val accentColor = if (isScreenOn) BevelBlue else BevelViolet

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BevelCard)
            .border(1.dp, BevelCardBorder, RoundedCornerShape(18.dp))
            .padding(14.dp)
            .testTag("recent_session_item_${session.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isScreenOn) Icons.Default.WbSunny else Icons.Default.Bedtime,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = if (isScreenOn) "亮屏使用" else "熄屏休眠",
                        color = BevelTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${TimeFormatter.formatTime(session.startTime)} - ${TimeFormatter.formatTime(session.endTime)}",
                        color = BevelTextTertiary,
                        fontSize = 11.sp
                    )
                }
            }

            // Duration Pill
            Text(
                text = TimeFormatter.formatDurationCompact(session.durationMs),
                color = accentColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
