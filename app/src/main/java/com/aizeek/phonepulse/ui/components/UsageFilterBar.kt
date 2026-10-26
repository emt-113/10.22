package com.aizeek.phonepulse.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aizeek.phonepulse.ui.theme.BevelBlue
import com.aizeek.phonepulse.ui.theme.BevelCard
import com.aizeek.phonepulse.ui.theme.BevelCardBorder
import com.aizeek.phonepulse.ui.theme.BevelMint
import com.aizeek.phonepulse.ui.theme.BevelTextPrimary
import com.aizeek.phonepulse.ui.theme.BevelTextSecondary
import com.aizeek.phonepulse.ui.theme.BevelViolet
import com.aizeek.phonepulse.ui.theme.BevelEaseIn
import com.aizeek.phonepulse.ui.theme.BevelEaseOut
import kotlinx.coroutines.launch

/** Foreground-usage intensity band, computed from the real per-app durations. */
enum class UsageBand(val label: String, val minMinutes: Long) {
    HIGH("高频 ≥30 分", 30),
    MID("中频 5–29 分", 5),
    LOW("低频 <5 分", 0)
}

/** Pure filter rules for the usage list — unit-tested, no Compose involved. */
object UsageFilterEngine {

    fun bandOf(foregroundMs: Long): UsageBand {
        val minutes = foregroundMs / 60_000L
        return when {
            minutes >= UsageBand.HIGH.minMinutes -> UsageBand.HIGH
            minutes >= UsageBand.MID.minMinutes -> UsageBand.MID
            else -> UsageBand.LOW
        }
    }

    /** Empty selection = no band filter. Several selected bands are an OR. */
    fun matchesBands(selected: Set<UsageBand>, foregroundMs: Long): Boolean =
        selected.isEmpty() || bandOf(foregroundMs) in selected

    /** Empty tag list = no tag filter. Several tags are an OR across names. */
    fun matchesTags(tags: Collection<String>, appName: String, packageName: String): Boolean =
        tags.isEmpty() || tags.any { tag ->
            appName.contains(tag, ignoreCase = true) || packageName.contains(tag, ignoreCase = true)
        }
}

/**
 * The filter toggle for the app-usage header: a pill with a live badge whose
 * count bounces (spring overshoot) whenever the selection changes.
 */
@Composable
fun UsageFilterButton(
    selectedCount: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    // Count bounce: snap to 1.35x then let a bouncy spring settle it back —
    // a plain fade would read as a number that changed, not a number that counted.
    val badgeScale = remember { Animatable(1f) }
    var lastCount by remember { mutableIntStateOf(selectedCount) }
    LaunchedEffect(selectedCount) {
        if (selectedCount != lastCount) {
            lastCount = selectedCount
            launch {
                badgeScale.snapTo(1.35f)
                badgeScale.animateTo(1f, spring(dampingRatio = 0.38f, stiffness = 380f))
            }
        }
    }
    val bg by animateColorAsState(
        if (expanded || selectedCount > 0) BevelBlue.copy(alpha = 0.14f) else BevelCard,
        tween(180), label = "filter-btn-bg"
    )
    val border by animateColorAsState(
        if (expanded || selectedCount > 0) BevelBlue.copy(alpha = 0.65f) else BevelCardBorder,
        tween(180), label = "filter-btn-border"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onToggle()
            }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = Icons.Default.FilterList,
            contentDescription = "筛选",
            tint = BevelBlue,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = "筛选",
            color = BevelTextPrimary,
            fontSize = 12.sp,
            fontWeight = if (selectedCount > 0) androidx.compose.ui.text.font.FontWeight.SemiBold
            else androidx.compose.ui.text.font.FontWeight.Medium
        )
        AnimatedVisibility(visible = selectedCount > 0, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .graphicsLayer { scaleX = badgeScale.value; scaleY = badgeScale.value }
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(BevelBlue),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$selectedCount",
                    color = androidx.compose.ui.graphics.Color.White,
                    fontSize = 9.5.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
        }
    }
}

/**
 * The multi-select filter panel. On open the tags start stacked at the row's
 * center and spread into their laid-out places (staggered), so the panel
 * unfolds instead of being printed on. Each tag: a check that grows out of the
 * left edge, a tint that wipes across the full width, and neighbours that make
 * room smoothly because the chip width itself animates.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UsageFilterPanel(
    visible: Boolean,
    selected: Set<UsageBand>,
    onSelectionChange: (Set<UsageBand>) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(tween<IntSize>(420, easing = BevelEaseOut)) + fadeIn(tween(160)),
        exit = shrinkVertically(tween<IntSize>(280, easing = BevelEaseIn)) + fadeOut(tween(140)),
        modifier = modifier
    ) {
        val haptic = LocalHapticFeedback.current
        val bands = UsageBand.entries
        var flowSize by remember { mutableStateOf(IntSize.Zero) }
        val spreads = remember { bands.map { Animatable(0f) } }
        LaunchedEffect(visible) {
            if (visible) {
                spreads.forEachIndexed { index, anim ->
                    launch {
                        kotlinx.coroutines.delay(index * 42L)
                        anim.animateTo(1f, tween(420, easing = BevelEaseOut))
                    }
                }
            } else {
                spreads.forEach { it.snapTo(0f) }
            }
        }

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .onSizeChanged { flowSize = it },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            bands.forEachIndexed { index, band ->
                var chipOrigin by remember { mutableStateOf(Offset.Zero) }
                val isSelected = band in selected
                val spread = spreads[index].value
                val ready = flowSize != IntSize.Zero && chipOrigin != Offset.Zero
                val accent = when (band) {
                    UsageBand.HIGH -> BevelMint
                    UsageBand.MID -> BevelBlue
                    UsageBand.LOW -> BevelViolet
                }

                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            // Stacked -> spread: slide from the row's centre while fading in.
                            translationX = (flowSize.width / 2f - chipOrigin.x) * (1f - spread)
                            translationY = (flowSize.height / 2f - chipOrigin.y) * (1f - spread)
                            alpha = if (!ready) 0f else 0.2f + 0.8f * spread
                        }
                        .onGloballyPositioned {
                            chipOrigin = it.positionInParent() +
                                Offset(it.size.width / 2f, it.size.height / 2f)
                        }
                ) {
                    UsageFilterChip(
                        label = band.label,
                        accent = accent,
                        selected = isSelected,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectionChange(
                                if (isSelected) selected - band else selected + band
                            )
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = selected.isNotEmpty(),
                enter = fadeIn(tween(150)) + expandHorizontally(tween<IntSize>(200, easing = BevelEaseOut)),
                exit = fadeOut(tween(120)) + shrinkHorizontally(tween<IntSize>(160, easing = BevelEaseIn))
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectionChange(emptySet()) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        tint = BevelTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text("重置", color = BevelTextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun UsageFilterChip(
    label: String,
    accent: androidx.compose.ui.graphics.Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        if (selected) BevelCard else BevelCard, tween(160), label = "chip-bg"
    )
    val border by animateColorAsState(
        if (selected) accent else BevelCardBorder, tween(160), label = "chip-border"
    )
    val fillFraction by animateFloatAsState(
        if (selected) 1f else 0f,
        tween(260, easing = BevelEaseOut), label = "chip-fill"
    )
    val checkSlot by animateDpAsState(
        if (selected) 17.dp else 0.dp,
        tween(220, easing = BevelEaseOut), label = "chip-check-slot"
    )
    val checkScale by animateFloatAsState(
        if (selected) 1f else 0.3f,
        tween(220, easing = BevelEaseOut), label = "chip-check-scale"
    )
    val labelColor by animateColorAsState(
        if (selected) accent else BevelTextSecondary, tween(160), label = "chip-label"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // The selected tint wipes from the left edge across the full chip width.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(13.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fillFraction)
                    .background(accent.copy(alpha = 0.16f))
            )
        }
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.width(checkSlot),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = checkScale
                            scaleY = checkScale
                        }
                        .size(13.dp)
                )
            }
            Text(
                text = label,
                color = labelColor,
                fontSize = 12.sp,
                fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.SemiBold
                else androidx.compose.ui.text.font.FontWeight.Medium
            )
        }
    }
}
