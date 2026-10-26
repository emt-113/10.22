package com.aizeek.phonepulse.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.draw.alpha
import com.aizeek.phonepulse.BuildConfig
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aizeek.phonepulse.R
import com.aizeek.phonepulse.battery.BatteryMonitorSettings
import com.aizeek.phonepulse.battery.BatteryMonitorUiState
import com.aizeek.phonepulse.data.LiveBatteryInfo
import com.aizeek.phonepulse.intervention.BatteryHealthFacade
import com.aizeek.phonepulse.intervention.DesignCapacityResolver
import com.aizeek.phonepulse.intervention.PlanetEngine
import com.aizeek.phonepulse.intervention.ReturnGiftEngine
import com.aizeek.phonepulse.intervention.UsageCoachController
import com.aizeek.phonepulse.ui.theme.BevelAmber
import com.aizeek.phonepulse.ui.theme.BevelBlue
import com.aizeek.phonepulse.ui.theme.BevelCard
import com.aizeek.phonepulse.ui.theme.BevelCardBorder
import com.aizeek.phonepulse.ui.theme.BevelCoral
import com.aizeek.phonepulse.ui.theme.BevelCoralDeep
import com.aizeek.phonepulse.ui.theme.BevelCyan
import com.aizeek.phonepulse.ui.theme.BevelHeart
import com.aizeek.phonepulse.ui.theme.BevelMint
import com.aizeek.phonepulse.ui.theme.BevelMintDeep
import com.aizeek.phonepulse.ui.theme.BevelTextPrimary
import com.aizeek.phonepulse.ui.theme.BevelTextSecondary
import com.aizeek.phonepulse.ui.theme.BevelTextTertiary
import com.aizeek.phonepulse.ui.theme.BevelTrack
import com.aizeek.phonepulse.ui.theme.BevelViolet
import com.aizeek.phonepulse.util.TimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Bevel-style light home hub (intervention UI): brand header with settings
 * entry, a 3D-shaded planet whose orbit encodes the real daily budget
 * progress, and compact stat cards (actual screen-on time, battery health
 * with confidence level, charge target). Pure presentation — every value
 * comes from the existing runtime stores (UsageCoachController /
 * BatteryMonitor / BatteryRepository); no algorithm, service or database
 * behavior is touched here.
 */

/** Friendly device names for the health card footer (display only). */
private val FRIENDLY_MODELS = mapOf(
    "23117RK66C" to "K70 Pro"
)

@Composable
fun PlanetHomeSection(
    isScreenOn: Boolean,
    stateStartTimeMs: Long,
    isServiceRunning: Boolean,
    battery: LiveBatteryInfo,
    monitor: BatteryMonitorUiState,
    lastScreenOffDurationMs: Long?,
    onClickPlanet: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCoach: () -> Unit
) {
    val live by UsageCoachController.liveState().collectAsStateWithLifecycle()
    // Re-derived on every recomposition (live state beats every 5s while the screen
    // is on), so the 60s welcome window closes without a dedicated timer.
    val restArrival = heroRestArrival(
        nowMs = System.currentTimeMillis(),
        wakeAtMs = stateStartTimeMs,
        lastScreenOffMs = lastScreenOffDurationMs
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        HomeHeader(onOpenSettings = onOpenSettings)

        PlanetHero(
            todayMinutes = live.todayMinutes,
            budgetMinutes = live.budgetMinutes,
            stage = live.stage,
            sessionMinutes = live.sessionMinutes,
            continuousLimitMinutes = live.continuousLimitMinutes,
            isScreenOn = isScreenOn,
            restArrival = restArrival,
            onClick = onClickPlanet,
            onOpenCoach = onOpenCoach
        )

        RestTipCard(
            budgetMinutes = live.budgetMinutes,
            limitMinutes = live.continuousLimitMinutes,
            sessionMinutes = live.sessionMinutes,
            todayMinutes = live.todayMinutes,
            restArrival = restArrival
        )
        TodayScreenCard(
            isScreenOn = isScreenOn,
            stateStartTimeMs = stateStartTimeMs,
            isServiceRunning = isServiceRunning,
            todayMinutes = live.todayMinutes
        )
        HomeHealthCards(battery = battery, monitor = monitor)
    }
}

// ---- Header ----------------------------------------------------------------

@Composable
private fun HomeHeader(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = stringResource(R.string.app_name),
                color = BevelTextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "让手机更健康 · 让生活更平衡",
                color = BevelTextSecondary,
                fontSize = 12.sp
            )
        }
        Box(
            modifier = Modifier
                .bevelPressable(onClick = onOpenSettings)
                .size(44.dp)
                .shadow(1.dp, CircleShape)
                .clip(CircleShape)
                .background(BevelCard)
                .border(1.dp, BevelCardBorder, CircleShape)
                .testTag("home_settings_entry"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "打开设置",
                tint = BevelTextSecondary,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

// ---- Planet hero -----------------------------------------------------------

/**
 * The planet is the hero visual. Its main orbit ring doubles as the real
 * daily budget progress: the bright satellite travels along the orbit as the
 * day's screen-on time accumulates against the budget. Over budget the ring
 * turns coral. Caption below shows budget / remaining / completion.
 */
@Composable
private fun PlanetHero(
    todayMinutes: Int,
    budgetMinutes: Int?,
    stage: PlanetEngine.Stage,
    sessionMinutes: Int,
    continuousLimitMinutes: Int?,
    isScreenOn: Boolean,
    restArrival: RestArrival?,
    onClick: () -> Unit,
    onOpenCoach: () -> Unit
) {
    val progress = if (budgetMinutes != null && budgetMinutes > 0) {
        todayMinutes.toFloat() / budgetMinutes
    } else null
    val over = progress != null && progress > 1f
    // The arc glides to its new value instead of jumping: every live-data update
    // (5s heartbeat, settings change, screen transition) animates into place.
    val animatedProgress by animateFloatAsState(
        targetValue = (progress ?: 0f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "planet-progress-arc"
    )
    // Coach live state is collected once in PlanetHomeSection and passed down as
    // scalars: three separate collectors (here + RestTipCard) used to triple the
    // recomposition work on every 15s heartbeat with zero visual benefit.
    val actualVisualState = planetHeroState(
        isScreenOn = isScreenOn,
        restArrival = restArrival,
        continuousLimitMinutes = continuousLimitMinutes,
        sessionMinutes = sessionMinutes,
        budgetMinutes = budgetMinutes,
        todayMinutes = todayMinutes,
        stage = stage
    )
    var demoState by remember { mutableStateOf<PlanetVisualState?>(null) }
    val visualState = demoState ?: actualVisualState

    // The pearl scene costs ~20 gradient layers, so it is rasterised once into
    // a bitmap and blitted afterwards. The bitmap is remembered by size/stage:
    // rebuilding it on every recomposition (as drawWithCache does when its
    // block is recreated) costs tens of milliseconds and shows up as jank.
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val motion = rememberInfiniteTransition(label = "planet-motion")
    val orbitPhase by motion.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(visualState.orbitDurationMs, easing = LinearEasing), RepeatMode.Restart),
        label = "planet-orbit-phase"
    )
    val tiltPhase by motion.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(5200, easing = LinearEasing), RepeatMode.Restart),
        label = "planet-orbit-tilt"
    )
    // Perf: the animated overlays used to allocate a Path and a Brush on every
    // animation frame (~60/s -> GC churn that competes with scrolling). The
    // geometry only depends on (size, state, over-budget), so the resources are
    // built once per key here and the wave path is reused via reset(). Pixel
    // output is identical; the draw* functions keep a first-frame fallback.
    val surfaceSpherePath = remember(canvasSize) {
        if (canvasSize.width <= 0 || canvasSize.height <= 0) null else Path().apply {
            val r = minOf(canvasSize.width, canvasSize.height) * 0.315f
            val cx = canvasSize.width / 2f
            val cy = canvasSize.height * 0.47f
            addOval(Rect(cx - r, cy - r, cx + r, cy + r))
        }
    }
    val surfaceFlowPath = remember { Path() }
    val surfaceFlowBrush = remember(canvasSize, visualState) {
        if (canvasSize.width <= 0 || canvasSize.height <= 0) null else Brush.horizontalGradient(
            surfaceFlowColors(visualState),
            startX = canvasSize.width / 2f - minOf(canvasSize.width, canvasSize.height) * 0.315f,
            endX = canvasSize.width / 2f + minOf(canvasSize.width, canvasSize.height) * 0.315f
        )
    }
    val progressBrush = remember(canvasSize, over) {
        if (canvasSize.width <= 0 || canvasSize.height <= 0) null else {
            val r = minOf(canvasSize.width, canvasSize.height) * 0.315f
            val cx = canvasSize.width / 2f
            val cy = canvasSize.height * 0.47f
            val orbitRx = r * 1.66f
            val arcStart = if (over) Color(0xFFF0B9B0) else Color(0xFF7ADCC4)
            val arcEnd = if (over) Color(0xFFF7D5C9) else Color(0xFFA6D6F0)
            Brush.linearGradient(
                colorStops = arrayOf(0.00f to arcStart, 1.00f to arcEnd),
                start = Offset(cx - orbitRx, cy),
                end = Offset(cx + orbitRx, cy)
            )
        }
    }
    Box(modifier = Modifier.fillMaxWidth()) {
        val stageColor = visualState.color
        Text(
            text = "脉搏星球 · ${visualState.title}",
            color = stageColor,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.TopStart)
                .clip(RoundedCornerShape(9.dp))
                .background(stageColor.copy(alpha = 0.10f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
        AnimatedContent(
            targetState = visualState,
            transitionSpec = { fadeIn(tween(700)) togetherWith fadeOut(tween(700)) },
            label = "planet-visual-state"
        ) { animatedState ->
            val animatedLayer = remember(canvasSize, animatedState, isScreenOn) {
                if (canvasSize.width <= 0 || canvasSize.height <= 0) null else PlanetLayerCache.obtain(
                    canvasSize.width, canvasSize.height, animatedState, isScreenOn, density, layoutDirection
                )
            }
            Spacer(
                modifier = Modifier.fillMaxWidth().height(236.dp)
                    .bevelTiltPressable(onClick = onClick)
                    .onSizeChanged { canvasSize = it }
                    .drawBehind {
                        animatedLayer?.let { drawImage(it) }
                        drawPlanetProgress(progress = if (progress != null) animatedProgress else null, over = over, phase = orbitPhase, prebuiltBrush = progressBrush, planetDisk = surfaceSpherePath)
                    drawPlanetOrbitShimmer(orbitPhase, tiltPhase, visualState, planetDisk = surfaceSpherePath)
                    drawPlanetSurfaceFlow(orbitPhase, visualState, spherePath = surfaceSpherePath, flowPath = surfaceFlowPath, prebuiltBrush = surfaceFlowBrush)
                    }.testTag("planet_hero")
            )
        }
        if (BuildConfig.DEBUG) {
            Text(
                text = "${visualState.ordinal + 1}/4 预览",
                color = BevelTextSecondary,
                fontSize = 11.5.sp,
                modifier = Modifier.align(Alignment.TopEnd).clip(RoundedCornerShape(9.dp))
                    .background(BevelCard.copy(alpha = 0.92f)).clickable {
                        demoState = PlanetVisualState.entries[(visualState.ordinal + 1) % PlanetVisualState.entries.size]
                    }.padding(horizontal = 9.dp, vertical = 5.dp)
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .testTag("home_budget_caption"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (budgetMinutes == null) {
            // Day-one state is a designed screen: the empty budget slot invites
            // the one action that starts the loop instead of reading as "—".
            Column(
                modifier = Modifier.bevelPressable(onClick = onOpenCoach, pressedScale = 0.94f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("今日预算", color = BevelTextTertiary, fontSize = 11.5.sp)
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    "未设置 · 去开启",
                    color = BevelBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            HeroStat(label = "今日预算", value = "$budgetMinutes 分钟")
        }
        HeroStat(
            label = if (over) "已超额" else "剩余",
            value = if (budgetMinutes != null && budgetMinutes > 0) {
                val delta = kotlin.math.abs(budgetMinutes - todayMinutes)
                "$delta 分钟"
            } else {
                "—"
            },
            valueColor = if (over) BevelCoral else BevelTextPrimary
        )
        HeroStat(
            label = "目标完成",
            value = if (progress != null) "${(progress * 100).roundToInt()}%" else "—",
            valueColor = when {
                progress == null -> BevelTextTertiary
                over -> BevelCoral
                else -> BevelMint
            }
        )
    }

    // Long-press breath bar: stretch-to-breathe for one stardust (10/day cap).
    BreathPulseRow(modifier = Modifier.padding(horizontal = 6.dp))
}

internal enum class PlanetVisualState(val title: String, val color: Color, val orbitDurationMs: Int) {
    STEADY("平稳状态", Color(0xFF8D8BCB), 18000),
    GOOD("节奏良好", Color(0xFF27BDA9), 8500),
    SUGGEST_REST("建议休息", Color(0xFFD4A958), 24000),
    RESTING("休息中", Color(0xFF8984C8), 36000)
}

/** A real rest (>= ReturnGiftEngine.MIN_GAP_MINUTES) the user just came back from. */
internal data class RestArrival(val minutes: Int)

/** How long the violet "刚休息回来" moment stays visible after the screen wakes. */
internal const val REST_ARRIVAL_WINDOW_MS = 60_000L

/**
 * Pure rest-arrival check: the last completed screen-off gap (short absorbed
 * gaps never write it) must be a real rest AND the screen must have woken
 * within the welcome window. Wall-clock based, matching ScreenStateHolder.
 */
internal fun heroRestArrival(nowMs: Long, wakeAtMs: Long, lastScreenOffMs: Long?): RestArrival? {
    val gap = lastScreenOffMs ?: return null
    if (gap < ReturnGiftEngine.MIN_GAP_MINUTES * 60_000L) return null
    val sinceWake = nowMs - wakeAtMs
    if (sinceWake < 0L || sinceWake > REST_ARRIVAL_WINDOW_MS) return null
    return RestArrival(minutes = (gap / 60_000L).toInt())
}

/** Hero state priority: welcome-back moment first, then the existing ladder. */
internal fun planetHeroState(
    isScreenOn: Boolean,
    restArrival: RestArrival?,
    continuousLimitMinutes: Int?,
    sessionMinutes: Int,
    budgetMinutes: Int?,
    todayMinutes: Int,
    stage: PlanetEngine.Stage
): PlanetVisualState = when {
    restArrival != null -> PlanetVisualState.RESTING
    !isScreenOn -> PlanetVisualState.RESTING
    continuousLimitMinutes != null && sessionMinutes >= continuousLimitMinutes -> PlanetVisualState.SUGGEST_REST
    budgetMinutes != null && budgetMinutes > 0 && todayMinutes * 100 >= budgetMinutes * PlanetEngine.SUGGESTION_BUDGET_HIGH_PCT -> PlanetVisualState.SUGGEST_REST
    stage != PlanetEngine.Stage.SPROUT -> PlanetVisualState.GOOD
    else -> PlanetVisualState.STEADY
}

/**
 * Pearl material palette. Every stage keeps the same translucent-looking
 * material (pastel multi-hue gradient + light shadow), only the tint changes:
 * sprout = mist mint, running = pearl lavender, shining = ice blue + blush.
 */
/**
 * Pearl body + stage tint. The sphere itself is a brand asset: its body always
 * travels the same colour journey (mint -> ice blue -> pearl pink -> lavender),
 * so the planet reads as "the PhonePulse planet" at every stage. Only the
 * atmosphere, the small accents and the glow shift with the stage.
 */
internal data class PlanetPalette(
    val light: Color,
    val mid: Color,
    val mid2: Color,
    val mid3: Color,
    val deep: Color,
    val deep2: Color,
    val accent: Color,
    val glow: Color
)

internal fun planetPalette(state: PlanetVisualState): PlanetPalette = when (state) {
    // Dormant day: mint atmosphere.
    PlanetVisualState.STEADY -> PlanetPalette(
        light = Color(0xFFE6F6F0), mid = Color(0xFF8FDCBE), mid2 = Color(0xFFA9DDEC),
        mid3 = Color(0xFFF7CFE0), deep = Color(0xFFC9B4EE), deep2 = Color(0xFF6F5BC0),
        accent = Color(0xFFCDEFE4), glow = Color(0xFF7FD8C0)
    )
    // Active day: lavender atmosphere.
    PlanetVisualState.GOOD -> PlanetPalette(
        light = Color(0xFFA8FFF0), mid = Color(0xFF35D9CB), mid2 = Color(0xFF35BFE8),
        mid3 = Color(0xFF426FE0), deep = Color(0xFF624DD6), deep2 = Color(0xFF43399B),
        accent = Color(0xFF75F6E4), glow = Color(0xFF48DCCB)
    )
    PlanetVisualState.SUGGEST_REST -> PlanetPalette(
        light = Color(0xFFFFF0D1), mid = Color(0xFFF2D08F), mid2 = Color(0xFFF0BDA7),
        mid3 = Color(0xFFEAC8D5), deep = Color(0xFFC5B7DD), deep2 = Color(0xFF796DAD),
        accent = Color(0xFFFFDCA6), glow = Color(0xFFE8BD79)
    )
    PlanetVisualState.RESTING -> PlanetPalette(
        light = Color(0xFFE8E9FF), mid = Color(0xFFBFC6F8), mid2 = Color(0xFF9FB9ED),
        mid3 = Color(0xFFB7AEEB), deep = Color(0xFF8E84D3), deep2 = Color(0xFF5D578F),
        accent = Color(0xFFC7C5F4), glow = Color(0xFF9C9BE2)
    )
}

/**
 * Perf: single-slot cache for the rasterised pearl backdrop. The remember()
 * that builds the layer dies with the home tab's Crossfade content, so every
 * return to the home tab used to re-rasterise ~20 gradient layers on the main
 * thread at the exact moment of the switch — the largest tab-switch hitch.
 * The cache key fully determines the pixels (size/state/screenOn/density/
 * layoutDirection), so reuse is visually identical. Cost: one bitmap kept
 * alive (~3MB at phone resolution).
 */
private data class PlanetLayerKey(
    val width: Int,
    val height: Int,
    val state: PlanetVisualState,
    val screenOn: Boolean,
    val density: Float,
    val layoutDir: LayoutDirection
)

private object PlanetLayerCache {
    private var key: PlanetLayerKey? = null
    private var layer: ImageBitmap? = null

    fun obtain(
        width: Int,
        height: Int,
        state: PlanetVisualState,
        screenOn: Boolean,
        density: Density,
        layoutDirection: LayoutDirection
    ): ImageBitmap {
        val k = PlanetLayerKey(width, height, state, screenOn, density.density, layoutDirection)
        layer?.takeIf { key == k }?.let { return it }
        val built = buildPlanetLayer(width, height, state, screenOn, density, layoutDirection)
        key = k
        layer = built
        return built
    }
}

    /**
     * Rasterises the static pearl scene into a bitmap once, so the hero can
     * blit it instead of re-drawing ~20 gradient layers on every frame.
     */
    private fun buildPlanetLayer(
        width: Int,
        height: Int,
        visualState: PlanetVisualState,
        screenOn: Boolean,
        density: Density,
        layoutDirection: LayoutDirection
    ): ImageBitmap {
        val layer = ImageBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1))
        val layerCanvas = androidx.compose.ui.graphics.Canvas(layer)
        CanvasDrawScope().draw(
            density = density,
            layoutDirection = layoutDirection,
            canvas = layerCanvas,
            size = Size(width.toFloat(), height.toFloat())
        ) {
            drawPlanetBackdrop(visualState = visualState, screenOn = screenOn)
        }
        return layer
    }

    /**
     * Static half of the hero scene: aura, glass orbit, crescent moon, floating
     * pebbles, particles and the pearl sphere. Rasterising it costs ~20 gradient
     * layers, so it is rendered once into a bitmap and blitted afterwards.
     */
    private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPlanetBackdrop(
        visualState: PlanetVisualState,
        screenOn: Boolean
    ) {
        val w = size.width
        val h = size.height
        val m = minOf(w, h)
        val cx = w / 2f
        val cy = h * 0.47f
        val r = m * 0.315f
        val orbitRx = r * 1.66f
        val orbitRy = r * 0.44f
        val palette = planetPalette(visualState)


        // ---- 1. Aura ---------------------------------------------------------
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to palette.glow.copy(alpha = if (screenOn) 0.30f else 0.20f),
                    0.40f to palette.accent.copy(alpha = 0.16f),
                    0.70f to palette.glow.copy(alpha = 0.07f),
                    1.00f to palette.glow.copy(alpha = 0f)
                ),
                center = Offset(cx, cy),
                radius = r * 2.0f
            ),
            radius = r * 2.0f,
            center = Offset(cx, cy)
        )

        // Breathing halo: one faint concentric outline for rhythm.
        // Drawn as a stroke (cheap) instead of a full-canvas gradient fill.
        drawCircle(
            color = palette.glow.copy(alpha = 0.13f),
            radius = r * 1.42f,
            center = Offset(cx, cy),
            style = Stroke(width = 2.5.dp.toPx())
        )

        // ---- 2. Glass orbit, back half (behind the planet) -------------------
        rotate(degrees = -18f, pivot = Offset(cx, cy)) {
            drawArc(
                brush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.00f to Color(0xFFE6D7F7).copy(alpha = 0f),
                        0.22f to Color(0xFFE6D7F7).copy(alpha = 0.30f),
                        0.50f to Color(0xFFCFE9F7).copy(alpha = 0.22f),
                        0.78f to Color(0xFFF7DCE8).copy(alpha = 0.26f),
                        1.00f to Color(0xFFF7DCE8).copy(alpha = 0f)
                    ),
                    start = Offset(cx - orbitRx, cy),
                    end = Offset(cx + orbitRx, cy)
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(cx - orbitRx, cy - orbitRy),
                size = Size(orbitRx * 2f, orbitRy * 2f),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // ---- 3. Crescent moon -------------------------------------------------
        val moonCx = cx + r * 1.62f
        val moonCy = cy - r * 1.30f
        val moonR = r * 0.22f
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to Color(0xFFFFF6E8).copy(alpha = 0.70f),
                    0.60f to Color(0xFFF7E4CD).copy(alpha = 0.32f),
                    1.00f to Color(0xFFF7E4CD).copy(alpha = 0f)
                ),
                center = Offset(moonCx, moonCy),
                radius = moonR * 1.9f
            ),
            radius = moonR * 1.9f,
            center = Offset(moonCx, moonCy)
        )
        val moonPath = Path().apply {
            val outer = Path().apply {
                addOval(Rect(moonCx - moonR, moonCy - moonR, moonCx + moonR, moonCy + moonR))
            }
            val cut = Path().apply {
                val cr = moonR * 0.94f
                val cc = moonCx + moonR * 0.44f
                addOval(Rect(cc - cr, moonCy - cr, cc + cr, moonCy + cr))
            }
            op(outer, cut, PathOperation.Difference)
        }
        drawPath(moonPath, color = Color(0xFFFFF4E4).copy(alpha = 0.97f))

        // ---- 4. Floating pebbles ---------------------------------------------
        val pebbles = listOf(
            Triple(Offset(cx - r * 1.92f, cy + r * 0.58f), r * 0.085f, Color(0xFFF6C9DC)),
            Triple(Offset(cx + r * 1.86f, cy + r * 0.34f), r * 0.070f, Color(0xFFC8E9DC)),
            Triple(Offset(cx + r * 1.22f, cy - r * 1.02f), r * 0.055f, Color(0xFFD6C6F2))
        )
        pebbles.forEach { (p, pr, tint) ->
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to tint.copy(alpha = 0.95f),
                        0.70f to tint.copy(alpha = 0.50f),
                        1.00f to tint.copy(alpha = 0f)
                    ),
                    center = Offset(p.x - pr * 0.3f, p.y - pr * 0.35f),
                    radius = pr * 1.7f
                ),
                radius = pr * 1.7f,
                center = p
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.55f),
                radius = pr * 0.30f,
                center = Offset(p.x - pr * 0.28f, p.y - pr * 0.30f)
            )
        }

        // ---- 5. Particles + faint constellation -------------------------------
        val dust = listOf(
            0.12f to -1.34f, -1.18f to -0.72f, 1.30f to -0.38f,
            -0.58f to 1.40f, 0.98f to 1.16f, -1.44f to 0.52f, 0.52f to -1.52f
        )
        val dustPoints = dust.map { Offset(cx + it.first * r, cy + it.second * r) }
        drawLine(
            color = palette.glow.copy(alpha = 0.20f),
            start = dustPoints[0],
            end = dustPoints[3],
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = palette.glow.copy(alpha = 0.20f),
            start = dustPoints[3],
            end = dustPoints[6],
            strokeWidth = 1.dp.toPx()
        )
        dustPoints.forEachIndexed { index, p ->
            val point = r * if (index % 3 == 0) 0.028f else 0.019f
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to Color.White.copy(alpha = 0.90f),
                        0.40f to palette.glow.copy(alpha = 0.36f),
                        1.00f to palette.glow.copy(alpha = 0f)
                    ),
                    center = p,
                    radius = point * 2.6f
                ),
                radius = point * 2.6f,
                center = p
            )
            drawCircle(color = Color.White.copy(alpha = 0.92f), radius = point * 0.55f, center = p)
        }

        // ---- 6. The planet ----------------------------------------------------
        val spherePath = Path().apply {
            addOval(Rect(cx - r, cy - r, cx + r, cy + r))
        }

        // Body: the full pearl journey — mint -> ice blue -> pearl pink -> lavender.
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
                start = Offset(cx - r, cy - r),
                end = Offset(cx + r, cy + r)
            ),
            radius = r,
            center = Offset(cx, cy)
        )

        clipPath(spherePath) {
            // Iridescent wash across the lit side.
            drawRect(
                brush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.00f to Color(0xFFCBB6F2).copy(alpha = 0.26f),
                        0.45f to Color(0xFFF3CFE2).copy(alpha = 0.16f),
                        1.00f to Color(0xFFF3CFE2).copy(alpha = 0f)
                    ),
                    start = Offset(cx - r, cy - r),
                    end = Offset(cx + r, cy + r)
                ),
                topLeft = Offset(cx - r, cy - r),
                size = Size(r * 2f, r * 2f)
            )

            // Nacre sheen: thin colour-separated bands — the pearl signature.
            rotate(degrees = -14f, pivot = Offset(cx, cy)) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.White.copy(alpha = 0f),
                            0.11f to Color(0xFFE8D4FF).copy(alpha = 0.34f),
                            0.19f to Color(0xFFE8D4FF).copy(alpha = 0f),
                            0.29f to Color(0xFFD6F2E8).copy(alpha = 0.28f),
                            0.37f to Color(0xFFD6F2E8).copy(alpha = 0f),
                            0.49f to Color(0xFFFFD9E6).copy(alpha = 0.22f),
                            0.57f to Color(0xFFFFD9E6).copy(alpha = 0f),
                            0.71f to Color.White.copy(alpha = 0.18f),
                            0.81f to Color.White.copy(alpha = 0f),
                            1.00f to Color.White.copy(alpha = 0f)
                        ),
                        startY = cy - r,
                        endY = cy + r
                    ),
                    topLeft = Offset(cx - r, cy - r),
                    size = Size(r * 2f, r * 2f)
                )
            }

            // Surface clouds: a pale swirl and a rose storm.
            drawOval(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to palette.light.copy(alpha = 0.60f),
                        1.00f to palette.light.copy(alpha = 0f)
                    ),
                    center = Offset(cx - r * 0.18f, cy + r * 0.22f),
                    radius = r * 0.55f
                ),
                topLeft = Offset(cx - r * 0.72f, cy - r * 0.08f),
                size = Size(r * 1.08f, r * 0.6f)
            )
            drawOval(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to Color(0xFFF9D3E3).copy(alpha = 0.46f),
                        1.00f to Color(0xFFF9D3E3).copy(alpha = 0f)
                    ),
                    center = Offset(cx + r * 0.28f, cy - r * 0.34f),
                    radius = r * 0.42f
                ),
                topLeft = Offset(cx - r * 0.16f, cy - r * 0.62f),
                size = Size(r * 0.88f, r * 0.52f)
            )

            // Key light: a soft sheen instead of a white wash.
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to Color.White.copy(alpha = 0.44f),
                        0.40f to Color.White.copy(alpha = 0.15f),
                        1.00f to Color.White.copy(alpha = 0f)
                    ),
                    center = Offset(cx - r * 0.40f, cy - r * 0.46f),
                    radius = r * 0.88f
                ),
                radius = r * 0.88f,
                center = Offset(cx - r * 0.40f, cy - r * 0.46f)
            )

            // Limb darkening in the body's own deep violet.
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to palette.deep2.copy(alpha = 0f),
                        0.58f to palette.deep2.copy(alpha = 0f),
                        1.00f to palette.deep2.copy(alpha = 0.28f)
                    ),
                    center = Offset(cx, cy),
                    radius = r
                ),
                radius = r,
                center = Offset(cx, cy)
            )

            // Terminator: cool violet shade on the lower right.
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF2E2350).copy(alpha = 0f),
                        Color(0xFF2E2350).copy(alpha = 0.22f)
                    ),
                    start = Offset(cx - r * 1.0f, cy - r * 1.0f),
                    end = Offset(cx + r * 1.25f, cy + r * 1.25f)
                ),
                topLeft = Offset(cx - r, cy - r),
                size = Size(r * 2f, r * 2f)
            )
        }

        // Specular hotspot.
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to Color.White.copy(alpha = 0.88f),
                    0.42f to Color.White.copy(alpha = 0.32f),
                    1.00f to Color.White.copy(alpha = 0f)
                ),
                center = Offset(cx - r * 0.36f, cy - r * 0.42f),
                radius = r * 0.30f
            ),
            radius = r * 0.30f,
            center = Offset(cx - r * 0.36f, cy - r * 0.42f)
        )

        // Rim light along the lit edge + a faint glass edge on the shadow side.
        rotate(degrees = -30f, pivot = Offset(cx, cy)) {
            drawArc(
                color = Color.White.copy(alpha = 0.66f),
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(cx - r, cy - r),
                size = Size(r * 2f, r * 2f),
                style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        drawCircle(
            color = Color.White.copy(alpha = 0.16f),
            radius = r - 0.75.dp.toPx(),
            center = Offset(cx, cy),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // ---- 7. Glass orbit, front half + real budget progress ----------------
        rotate(degrees = -18f, pivot = Offset(cx, cy)) {
            drawArc(
                brush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.00f to Color(0xFFE6D7F7).copy(alpha = 0f),
                        0.22f to Color(0xFFE6D7F7).copy(alpha = 0.44f),
                        0.50f to Color(0xFFCFE9F7).copy(alpha = 0.36f),
                        0.78f to Color(0xFFF7DCE8).copy(alpha = 0.40f),
                        1.00f to Color(0xFFF7DCE8).copy(alpha = 0f)
                    ),
                    start = Offset(cx - orbitRx, cy),
                    end = Offset(cx + orbitRx, cy)
                ),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(cx - orbitRx, cy - orbitRy),
                size = Size(orbitRx * 2f, orbitRy * 2f),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
            // Thin glass highlight along the front rim.
            drawArc(
                color = Color.White.copy(alpha = 0.30f),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(cx - orbitRx, cy - orbitRy + 1.2.dp.toPx()),
                size = Size(orbitRx * 2f, orbitRy * 2f),
                style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
            )
        }

    }

    /**
     * Arc-length parametrization for the budget-lane ellipse (rx = 1.66r, ry = 0.44r).
     * drawArc sweeps by parametric angle, but on a squashed ellipse equal angles are
     * not equal orbit lengths — the flat ends are "cheap", so a naive 360°·progress
     * sweep shows only ~half of the true percentage at low progress. The table below
     * inverts travelled arc length back to a sweep angle. The mapping is
     * scale-invariant (both axes scale with r), so it is built once per process.
     */
    internal object OrbitArc {
        private const val RX = 1.66f
        private const val RY = 0.44f
        private const val STEPS = 4096
        private const val LOOKUP = 1024

        /** sweepTable[k] = sweep angle whose travelled length is k/LOOKUP of the full orbit. */
        private val sweepTable: FloatArray = buildSweepTable()

        fun sweepFor(fraction: Float): Float {
            val u = fraction.coerceIn(0f, 1f) * LOOKUP
            val i = u.toInt().coerceAtMost(LOOKUP - 1)
            return sweepTable[i] + (sweepTable[i + 1] - sweepTable[i]) * (u - i)
        }

        private fun buildSweepTable(): FloatArray {
            // ds/dθ has a 180° period, so integrating the speed from 0 covers the
            // arc that starts at 180° (the lane's start point, the left tip).
            // Trapezoid cumulative length; speed at θ=0 is exactly RY.
            val step = (2.0 * Math.PI / STEPS).toFloat()
            val cum = FloatArray(STEPS + 1)
            var prev = RY
            for (i in 1..STEPS) {
                val t = i * step
                val cur = kotlin.math.sqrt((RX * RX * kotlin.math.sin(t) * kotlin.math.sin(t) +
                    RY * RY * kotlin.math.cos(t) * kotlin.math.cos(t)).toDouble()).toFloat()
                cum[i] = cum[i - 1] + (prev + cur) * 0.5f * step
                prev = cur
            }
            val total = cum[STEPS]
            val table = FloatArray(LOOKUP + 1)
            var cursor = 0
            for (k in 0..LOOKUP) {
                val target = total * k / LOOKUP
                while (cursor < STEPS && cum[cursor + 1] < target) cursor++
                val lo = cum[cursor]
                val hi = if (cursor < STEPS) cum[cursor + 1] else lo
                val span = hi - lo
                val t = if (span > 0f) ((target - lo) / span).coerceIn(0f, 1f) else 0f
                table[k] = (cursor + t) * (360f / STEPS)
            }
            table[LOOKUP] = 360f
            return table
        }
    }

    /** Draws one stroke arc segment; brush wins over color when present. */
    private fun androidx.compose.ui.graphics.drawscope.DrawScope.orbitSegmentArc(
        startAngle: Float,
        sweepAngle: Float,
        topLeft: Offset,
        arcSize: Size,
        style: Stroke,
        alpha: Float,
        brush: Brush?,
        color: Color
    ) {
        if (brush != null) {
            drawArc(
                brush = brush, startAngle = startAngle, sweepAngle = sweepAngle, useCenter = false,
                topLeft = topLeft, size = arcSize, style = style, alpha = alpha
            )
        } else {
            drawArc(
                color = color, startAngle = startAngle, sweepAngle = sweepAngle, useCenter = false,
                topLeft = topLeft, size = arcSize, style = style, alpha = alpha
            )
        }
    }

    /**
     * Draws one orbit-lane arc split at the front/behind boundary: the parametric
     * 180°..360° half is clipped to outside the planet disk (reads as passing
     * behind the planet, like the static glass orbit), the 0°..180° half draws
     * over the planet. Segments are cut at 0°/180°/360° so a trail straddling
     * the boundary renders correctly on both sides.
     */
    private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawOrbitArc(
        startAngle: Float,
        sweepAngle: Float,
        topLeft: Offset,
        arcSize: Size,
        style: Stroke,
        planetDisk: Path?,
        alpha: Float = 1f,
        brush: Brush? = null,
        color: Color = Color.Unspecified
    ) {
        var seg = startAngle.mod(360f)
        var remaining = sweepAngle
        while (remaining > 0.01f) {
            val behind = seg >= 180f
            val run = if (behind) minOf(remaining, 360f - seg) else minOf(remaining, 180f - seg)
            if (behind && planetDisk != null) {
                clipPath(planetDisk, ClipOp.Difference) {
                    orbitSegmentArc(seg, run, topLeft, arcSize, style, alpha, brush, color)
                }
            } else {
                orbitSegmentArc(seg, run, topLeft, arcSize, style, alpha, brush, color)
            }
            seg = (seg + run).mod(360f)
            remaining -= run
        }
    }

    /**
     * Live half: the budget arc (arc-length parametrized so its visible extent
     * equals the true percentage) plus the budget-lane comet — a small pearl
     * satellite with a soft trailing glow that circles continuously, so the
     * middle orbit always reads as moving. Orbit halves that pass behind the
     * planet (parametric angles 180°..360°) are clipped to outside the planet
     * disk, exactly like the static glass orbit, so the lane no longer cuts
     * across the planet's face.
     */
    private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPlanetProgress(
        progress: Float?,
        over: Boolean,
        phase: Float,
        prebuiltBrush: Brush?,
        planetDisk: Path?
    ) {
        val w = size.width
        val h = size.height
        val m = minOf(w, h)
        val cx = w / 2f
        val cy = h * 0.47f
        val r = m * 0.315f
        val orbitRx = r * 1.66f
        val orbitRy = r * 0.44f
        // Pale coral when over budget, mint-to-cyan otherwise: a light trail,
        // never a hard ring.
        val arcStart = if (over) Color(0xFFF0B9B0) else Color(0xFF7ADCC4)
        val arcEnd = if (over) Color(0xFFF7D5C9) else Color(0xFFA6D6F0)
        val bead = if (over) Color(0xFFE0A49A) else Color(0xFF3FC4A6)
        val laneTopLeft = Offset(cx - orbitRx, cy - orbitRy)
        val laneSize = Size(orbitRx * 2f, orbitRy * 2f)

        rotate(degrees = -18f, pivot = Offset(cx, cy)) {
            if (progress != null && progress > 0f) {
                // Arc-length sweep: equal percentages travel equal orbit lengths.
                val sweep = OrbitArc.sweepFor(progress)
                // Steady state reuses the hoisted brush; the fallback only runs on
                // the very first frame before the size state has landed.
                val progressBrush = prebuiltBrush ?: Brush.linearGradient(
                    colorStops = arrayOf(0.00f to arcStart, 1.00f to arcEnd),
                    start = Offset(cx - orbitRx, cy),
                    end = Offset(cx + orbitRx, cy)
                )
                // Soft glow underlay so the arc feels like light, not a stroke.
                drawOrbitArc(
                    startAngle = 180f,
                    sweepAngle = sweep,
                    topLeft = laneTopLeft,
                    arcSize = laneSize,
                    style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round),
                    planetDisk = planetDisk,
                    alpha = 0.12f,
                    brush = progressBrush
                )
                drawOrbitArc(
                    startAngle = 180f,
                    sweepAngle = sweep,
                    topLeft = laneTopLeft,
                    arcSize = laneSize,
                    style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round),
                    planetDisk = planetDisk,
                    brush = progressBrush
                )
                // Real-progress tick: a small quiet marker at the budget point.
                val tick = Math.toRadians((180f + sweep).toDouble())
                val tickPoint = Offset(
                    cx + orbitRx * cos(tick).toFloat(),
                    cy + orbitRy * sin(tick).toFloat()
                )
                if (sweep <= 180f && planetDisk != null) {
                    clipPath(planetDisk, ClipOp.Difference) {
                        drawCircle(color = bead.copy(alpha = 0.12f), radius = 4.dp.toPx(), center = tickPoint)
                        drawCircle(color = bead, radius = 1.7.dp.toPx(), center = tickPoint)
                    }
                } else {
                    drawCircle(color = bead.copy(alpha = 0.12f), radius = 4.dp.toPx(), center = tickPoint)
                    drawCircle(color = bead, radius = 1.7.dp.toPx(), center = tickPoint)
                }
            }
            // Budget-lane comet: a small pearl satellite with a soft trailing
            // glow. It runs 1.16x the tilted rings' speed, is pearl white so
            // it never blends into the mint progress arc, and is drawn even
            // without a budget so the lane never looks dead.
            val lanePhase = phase * 1.16f
            drawOrbitArc(
                startAngle = lanePhase - 36f,
                sweepAngle = 36f,
                topLeft = laneTopLeft,
                arcSize = laneSize,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                planetDisk = planetDisk,
                color = bead.copy(alpha = 0.20f)
            )
            drawOrbitArc(
                startAngle = lanePhase - 15f,
                sweepAngle = 15f,
                topLeft = laneTopLeft,
                arcSize = laneSize,
                style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round),
                planetDisk = planetDisk,
                color = bead.copy(alpha = 0.38f)
            )
            val theta = Math.toRadians(lanePhase.toDouble())
            val satellite = Offset(
                cx + orbitRx * cos(theta).toFloat(),
                cy + orbitRy * sin(theta).toFloat()
            )
            if (lanePhase.mod(360f) >= 180f && planetDisk != null) {
                clipPath(planetDisk, ClipOp.Difference) {
                    drawCircle(bead.copy(alpha = 0.15f), 6.5.dp.toPx(), satellite)
                    drawCircle(Color(0xFFEFFBF8).copy(alpha = 0.92f), 2.4.dp.toPx(), satellite)
                    drawCircle(Color.White, 1.dp.toPx(), satellite)
                }
            } else {
                drawCircle(bead.copy(alpha = 0.15f), 6.5.dp.toPx(), satellite)
                drawCircle(Color(0xFFEFFBF8).copy(alpha = 0.92f), 2.4.dp.toPx(), satellite)
                drawCircle(Color.White, 1.dp.toPx(), satellite)
            }
        }
    }

@Composable
private fun HeroStat(label: String, value: String, valueColor: Color = BevelTextPrimary) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = BevelTextTertiary,
            fontSize = 11.5.sp
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPlanetOrbitShimmer(
    phase: Float,
    tiltPhase: Float,
    state: PlanetVisualState,
    planetDisk: Path?
) {
    val m = minOf(size.width, size.height)
    val cx = size.width / 2f
    val cy = size.height * 0.47f
    val r = m * 0.315f
    val rx = r * 1.88f
    val tiltWave = kotlin.math.sin(Math.toRadians(tiltPhase.toDouble())).toFloat()
    val ry = r * (0.62f + tiltWave * 0.15f)
    val tilt = when (state) {
        PlanetVisualState.STEADY -> 13f
        PlanetVisualState.GOOD -> 24f
        PlanetVisualState.SUGGEST_REST -> 10f
        PlanetVisualState.RESTING -> 7f
    } * tiltWave
    val alpha = when (state) {
        PlanetVisualState.STEADY -> 0.50f
        PlanetVisualState.GOOD -> 0.68f
        PlanetVisualState.SUGGEST_REST -> 0.54f
        PlanetVisualState.RESTING -> 0.38f
    }
    val ringWidth = if (state == PlanetVisualState.GOOD) 2.1f else 1.7f
    rotate(degrees = -24f + tilt, pivot = Offset(cx, cy)) {
        val ringTopLeft = Offset(cx - rx, cy - ry)
        val ringSize = Size(rx * 2f, ry * 2f)
        // Wide, very soft halo keeps the big ring present without a bright line.
        drawOrbitArc(
            startAngle = 0f,
            sweepAngle = 360f,
            topLeft = ringTopLeft,
            arcSize = ringSize,
            style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round),
            planetDisk = planetDisk,
            color = state.color.copy(alpha = alpha * 0.08f)
        )
        // Fine main ring: clearly quieter than the budget lane's progress arc.
        drawOrbitArc(
            startAngle = 0f,
            sweepAngle = 360f,
            topLeft = ringTopLeft,
            arcSize = ringSize,
            style = Stroke(width = ringWidth.dp.toPx(), cap = StrokeCap.Round),
            planetDisk = planetDisk,
            color = state.color.copy(alpha = if (state == PlanetVisualState.GOOD) 0.34f else 0.26f)
        )
        // Travelling glint trails its satellite, echoing the budget-lane comet.
        drawOrbitArc(
            startAngle = phase - 34f,
            sweepAngle = 34f,
            topLeft = ringTopLeft,
            arcSize = ringSize,
            style = Stroke(width = (ringWidth + 1f).dp.toPx(), cap = StrokeCap.Round),
            planetDisk = planetDisk,
            color = state.color.copy(alpha = alpha * 0.55f)
        )
        val theta = Math.toRadians(phase.toDouble())
        val point = Offset(cx + rx * kotlin.math.cos(theta).toFloat(), cy + ry * kotlin.math.sin(theta).toFloat())
        if (phase.mod(360f) >= 180f && planetDisk != null) {
            clipPath(planetDisk, ClipOp.Difference) {
                drawCircle(state.color.copy(alpha = 0.12f), 7.dp.toPx(), point)
                drawCircle(state.color.copy(alpha = 0.24f), 3.6.dp.toPx(), point)
                drawCircle(state.color.copy(alpha = alpha), 2.1.dp.toPx(), point)
                drawCircle(Color.White.copy(alpha = 0.92f), 0.9.dp.toPx(), point)
            }
        } else {
            drawCircle(state.color.copy(alpha = 0.12f), 7.dp.toPx(), point)
            drawCircle(state.color.copy(alpha = 0.24f), 3.6.dp.toPx(), point)
            drawCircle(state.color.copy(alpha = alpha), 2.1.dp.toPx(), point)
            drawCircle(Color.White.copy(alpha = 0.92f), 0.9.dp.toPx(), point)
        }
    }
    rotate(degrees = 22f - tilt * 0.65f, pivot = Offset(cx, cy)) {
        val rx2 = rx * 0.84f
        val ry2 = ry * 0.75f
        val innerTopLeft = Offset(cx - rx2, cy - ry2)
        val innerSize = Size(rx2 * 2f, ry2 * 2f)
        drawOrbitArc(
            startAngle = 0f,
            sweepAngle = 360f,
            topLeft = innerTopLeft,
            arcSize = innerSize,
            style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round),
            planetDisk = planetDisk,
            color = state.color.copy(alpha = alpha * 0.20f)
        )
        // Small white runner with its own shorter trail, 1.3x speed.
        val head = phase * 1.3f
        drawOrbitArc(
            startAngle = head - 26f,
            sweepAngle = 26f,
            topLeft = innerTopLeft,
            arcSize = innerSize,
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round),
            planetDisk = planetDisk,
            color = Color.White.copy(alpha = alpha * 0.40f)
        )
        val theta2 = Math.toRadians(head.toDouble())
        val runner = Offset(cx + rx2 * kotlin.math.cos(theta2).toFloat(), cy + ry2 * kotlin.math.sin(theta2).toFloat())
        if (head.mod(360f) >= 180f && planetDisk != null) {
            clipPath(planetDisk, ClipOp.Difference) {
                drawCircle(Color.White.copy(alpha = alpha * 0.80f), 1.6.dp.toPx(), runner)
            }
        } else {
            drawCircle(Color.White.copy(alpha = alpha * 0.80f), 1.6.dp.toPx(), runner)
        }
    }
}

/** Flow-band colors per visual state; shared by the hoisted brush and the first-frame fallback. */
private fun surfaceFlowColors(state: PlanetVisualState): List<Color> = when (state) {
    PlanetVisualState.STEADY -> listOf(Color.White.copy(alpha = 0f), Color(0xFFBDEBEA).copy(alpha = 0.30f), Color(0xFFD8CBF5).copy(alpha = 0.12f))
    PlanetVisualState.GOOD -> listOf(Color(0xFF63F4D6).copy(alpha = 0f), Color(0xFF77FFE4).copy(alpha = 0.72f), Color(0xFF5BBEFA).copy(alpha = 0.62f), Color(0xFFB6A2FF).copy(alpha = 0f))
    PlanetVisualState.SUGGEST_REST -> listOf(Color(0xFFFFE8B7).copy(alpha = 0f), Color(0xFFFFD28C).copy(alpha = 0.46f), Color(0xFFFFBDAE).copy(alpha = 0.34f), Color(0xFFFFE8B7).copy(alpha = 0f))
    PlanetVisualState.RESTING -> listOf(Color(0xFFD6D8FF).copy(alpha = 0f), Color(0xFFC4C9FF).copy(alpha = 0.28f), Color(0xFFB9D7FF).copy(alpha = 0.22f), Color(0xFFD6D8FF).copy(alpha = 0f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPlanetSurfaceFlow(
    phase: Float,
    state: PlanetVisualState,
    spherePath: Path?,
    flowPath: Path,
    prebuiltBrush: Brush?
) {
    val cx = size.width / 2f
    val cy = size.height * 0.47f
    val r = minOf(size.width, size.height) * 0.315f
    val wave = kotlin.math.sin(Math.toRadians(phase.toDouble())).toFloat() * r * 0.17f
    // Steady state reuses the hoisted sphere path; the fallback only runs on the
    // very first frame before the size state has landed.
    val sphere = spherePath ?: Path().apply { addOval(Rect(cx - r, cy - r, cx + r, cy + r)) }
    clipPath(sphere) {
        flowPath.reset()
        flowPath.moveTo(cx - r, cy - r * 0.12f + wave)
        flowPath.cubicTo(cx - r * 0.42f, cy - r * 0.62f + wave, cx + r * 0.15f, cy + r * 0.58f - wave, cx + r, cy - r * 0.04f - wave)
        val brush = prebuiltBrush ?: Brush.horizontalGradient(surfaceFlowColors(state), startX = cx - r, endX = cx + r)
        drawPath(flowPath, brush, style = Stroke(width = if (state == PlanetVisualState.GOOD) 11.dp.toPx() else 7.dp.toPx(), cap = StrokeCap.Round))
        if (state == PlanetVisualState.GOOD || state == PlanetVisualState.STEADY) {
            drawPath(flowPath, Color.White.copy(alpha = if (state == PlanetVisualState.GOOD) 0.44f else 0.24f), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

// ---- Tip -------------------------------------------------------------------

/** Adaptive local suggestion driven only by real coach state (passed as scalars). */
@Composable
private fun RestTipCard(
    budgetMinutes: Int?,
    limitMinutes: Int?,
    sessionMinutes: Int,
    todayMinutes: Int,
    restArrival: RestArrival?
) {
    val budget = budgetMinutes
    val limit = limitMinutes
    val (title, body) = when {
        restArrival != null ->
            "欢迎回来" to "你离开了 ${restArrival.minutes} 分钟，星球刚陪着你休息了一会儿。"
        limit != null && sessionMinutes >= limit ->
            "该休息一下了" to "你已经连续使用 ${sessionMinutes} 分钟，建议放下手机，休息放松眼睛。"
        budget != null && todayMinutes >= budget ->
            "预算已用完" to "今日 ${budget} 分钟亮屏预算已用完，剩下的时间让眼睛多休息。"
        budget != null && (budget - todayMinutes) in 1..30 ->
            "预算即将到达" to "今日预算还剩 ${budget - todayMinutes} 分钟，注意控制使用节奏。"
        else ->
            "节奏很好" to "今日使用节奏平稳，保持当前状态，星球会随每日目标慢慢点亮。"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bevelCard(RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconChip(color = BevelAmber, icon = Icons.Default.Lightbulb)
        Column {
            Text(
                text = title,
                color = BevelTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = body,
                color = BevelTextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}

// ---- Actual screen-on time card --------------------------------------------

@Composable
private fun TodayScreenCard(
    isScreenOn: Boolean,
    stateStartTimeMs: Long,
    isServiceRunning: Boolean,
    todayMinutes: Int
) {
    val stateColor = if (isScreenOn) BevelMint else BevelViolet
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bevelCard(RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("today_status_card"),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(stateColor.copy(alpha = 0.10f))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isScreenOn) Icons.Default.WbSunny else Icons.Default.Bedtime,
                        contentDescription = null,
                        tint = stateColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = if (isScreenOn) "使用中" else "休眠中",
                        color = stateColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = if (isServiceRunning) BevelTextTertiary else BevelAmber,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = if (isServiceRunning) "守护运行中" else "守护未开启",
                        color = if (isServiceRunning) BevelTextTertiary else BevelAmber,
                        fontSize = 11.5.sp
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                LiveSessionTimer(startTimeMs = stateStartTimeMs, compact = true)
                Text(
                    text = "本次已持续 · 从 ${TimeFormatter.formatShortTime(stateStartTimeMs)} 起",
                    color = BevelTextTertiary,
                    fontSize = 9.5.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "$todayMinutes",
                color = BevelTextPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "分钟",
                color = BevelTextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "今日实际亮屏 · 实时统计",
                color = BevelTextTertiary,
                fontSize = 11.5.sp,
                modifier = Modifier.padding(bottom = 5.dp)
            )
        }
    }
}

/** Isolated ticking timer so only the text recomposes every second. */
@Composable
private fun LiveSessionTimer(startTimeMs: Long, compact: Boolean = false) {
    val elapsedFlow = remember(startTimeMs) {
        flow {
            while (true) {
                emit((System.currentTimeMillis() - startTimeMs).coerceAtLeast(0L))
                delay(1000)
            }
        }
    }
    val elapsedMs by elapsedFlow.collectAsStateWithLifecycle(
        initialValue = (System.currentTimeMillis() - startTimeMs).coerceAtLeast(0L),
        minActiveState = Lifecycle.State.RESUMED
    )
    Text(
        text = TimeFormatter.formatDigitalClock(elapsedMs),
        color = BevelTextPrimary,
        fontSize = if (compact) 17.sp else 24.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace
    )
}

// ---- Health / charge cards ---------------------------------------------------

/** Battery health + charge protection pair. */
@Composable
private fun HomeHealthCards(
    battery: LiveBatteryInfo,
    monitor: BatteryMonitorUiState
) {
    // Same resolution chain as the battery page: manual > sysfs > model table.
    var sysfs by remember { mutableStateOf<DesignCapacityResolver.SysfsCapacity?>(null) }
    LaunchedEffect(Unit) {
        sysfs = withContext(Dispatchers.IO) { DesignCapacityResolver.readSnapshot() }
    }
    val designResolution = remember(monitor.settings.designCapacityMah, sysfs) {
        DesignCapacityResolver.resolve(
            monitor.settings.designCapacityMah, sysfs?.designMah,
            DesignCapacityResolver.modelTableCapacity(android.os.Build.MODEL)
        )
    }
    val displayHealth = remember(monitor, sysfs) {
        val cycles = monitor.cycles + listOfNotNull(monitor.activeCycle)
        BatteryHealthFacade.display(
            monitor.health, cycles, designResolution.capacityMah,
            sysfs?.measuredMah, monitor.settings.calibrationRevision
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BatteryHealthCard(displayHealth, designResolution, Modifier.weight(1f).fillMaxHeight())
        ChargeTargetCard(battery, monitor.settings, Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun BatteryHealthCard(
    displayHealth: BatteryHealthFacade.DisplayHealth,
    designResolution: DesignCapacityResolver.Resolution,
    modifier: Modifier = Modifier
) {
    val pct = displayHealth.healthPct
    // Confidence level follows the estimation source: formal estimates are the
    // high-confidence path, fast estimates are explicitly low confidence.
    val (pill, pillColor) = when {
        displayHealth.anomalous -> "待校准" to BevelAmber
        pct == null -> "估算中" to BevelTextTertiary
        displayHealth.source == BatteryHealthFacade.Source.FORMAL -> "高置信" to BevelMint
        else -> "低置信" to BevelAmber
    }
    val healthWord = when {
        pct == null -> "充电后自动估算"
        pct >= 80 -> "良好"
        pct >= 60 -> "一般"
        else -> "偏低"
    }
    val device = FRIENDLY_MODELS[android.os.Build.MODEL] ?: android.os.Build.MODEL
    val capacityText = designResolution.capacityMah?.roundToInt()?.let { "${it}mAh" } ?: ""

    Column(
        modifier = modifier
            .bevelCard(RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                IconChip(color = BevelHeart, icon = Icons.Default.Favorite)
                Text(
                    text = "电池健康",
                    color = BevelTextSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            StatusPill(text = pill, color = pillColor)
        }
        Spacer(modifier = Modifier.height(2.dp))
        if (pct != null) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${pct.roundToInt()}",
                    color = BevelTextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(text = "%", color = BevelTextSecondary, fontSize = 12.sp)
            }
        } else {
            Text(
                text = "采集中",
                color = BevelTextSecondary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            text = listOf(healthWord, device, capacityText)
                .filter { it.isNotEmpty() }
                .joinToString(" · "),
            color = BevelTextTertiary,
            fontSize = 11.5.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun ChargeTargetCard(
    battery: LiveBatteryInfo,
    settings: BatteryMonitorSettings,
    modifier: Modifier = Modifier
) {
    val level = battery.percentage
    Column(
        modifier = modifier
            .bevelCard(RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                IconChip(color = BevelCyan, icon = Icons.Default.Bolt)
                Text(
                    text = "充电保护",
                    color = BevelTextSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            StatusPill(
                text = if (settings.chargeAlarmEnabled) "已开启" else "未开启",
                color = if (settings.chargeAlarmEnabled) BevelMint else BevelTextTertiary
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "${settings.chargeTargetPct}",
                color = BevelTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(text = "%", color = BevelTextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "目标电量",
                color = BevelTextTertiary,
                fontSize = 11.5.sp,
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }
        ShimmerProgress(
            progress = (level / 100f).coerceIn(0f, 1f),
            color = if (settings.chargeTargetPct in 0..100 && level >= settings.chargeTargetPct) BevelMint else BevelCyan
        )
        Text(
            text = if (level in 0..100) "当前电量 $level%" else "当前电量采集中",
            color = BevelTextTertiary,
            fontSize = 11.5.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun ShimmerProgress(progress: Float, color: Color) {
    val transition = rememberInfiniteTransition(label = "charge-progress-shimmer")
    val sweep by transition.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "charge-progress-sweep"
    )
    Canvas(Modifier.fillMaxWidth().height(6.dp)) {
        val end = size.width * progress
        drawRoundRect(BevelTrack, cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2))
        if (end > 0f) {
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(Color(0xFF63BDF2), color, Color(0xFF9D91EF)), endX = end),
                size = androidx.compose.ui.geometry.Size(end, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
            )
            val highlightX = end * sweep
            if (highlightX in 0f..end) {
                drawCircle(Color.White.copy(alpha = 0.72f), size.height * 0.48f, Offset(highlightX, size.height / 2))
            }
            drawCircle(color, size.height * 0.58f, Offset(end.coerceAtLeast(size.height / 2), size.height / 2))
        }
    }
}

// ---- Shared Bevel primitives ----------------------------------------------

private fun Modifier.bevelCard(shape: RoundedCornerShape): Modifier =
    this
        .shadow(1.5.dp, shape)
        .clip(shape)
        .background(BevelCard)
        .border(1.dp, BevelCardBorder, shape)

@Composable
private fun IconChip(color: Color, icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.13f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(15.dp)
        )
    }
}

@Composable
private fun StatusPill(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

private fun stageName(stage: PlanetEngine.Stage): String = when (stage) {
    PlanetEngine.Stage.SPROUT -> "萌芽"
    PlanetEngine.Stage.RUNNING -> "运转"
    PlanetEngine.Stage.SHINING -> "闪耀"
}

private fun bevelStageColor(stage: PlanetEngine.Stage): Color = when (stage) {
    PlanetEngine.Stage.SPROUT -> BevelTextSecondary
    PlanetEngine.Stage.RUNNING -> BevelViolet
    PlanetEngine.Stage.SHINING -> BevelCyan
}
