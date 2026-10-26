package com.aizeek.phonepulse.intervention

import kotlin.math.sin

/**
 * Long-press breath exercise: holding the breath button for [HOLD_TARGET_MS]
 * turns the pill into a live breathing bar and pays one stardust. Pure math so
 * the award decision and the hold curve stay unit-testable; persistence lives
 * in [ReturnGiftStore.grantBreathBonus] so the bonus lands in the same ledger
 * as rest-gift stardust.
 */
object BreathPulseEngine {

    /** Full hold length. Below this, release = cancel (no award). */
    const val HOLD_TARGET_MS = 3_000L

    /** Anti-farm ceiling: completed breaths paid per calendar day. */
    const val DAILY_CAP = 10

    /** 0..1 through the hold, clamped so an overlong press never overshoots. */
    fun holdProgress(elapsedMs: Long): Float =
        (elapsedMs.toFloat() / HOLD_TARGET_MS).coerceIn(0f, 1f)

    fun isComplete(elapsedMs: Long): Boolean = elapsedMs >= HOLD_TARGET_MS

    /**
     * The breathing curve drawn while holding: a smooth sine so the bar reads
     * as inhale/exhale rather than a noise waveform. [phase] in radians.
     */
    fun wave(phase: Float): Float = sin(phase)

    /**
     * Day-capped award decision. Returns [Award.earned] = 1 while under the
     * cap (or 0 once today's cap is reached) plus the new daily counter, so
     * the UI can confirm "已达成/今日已达上限" from one call.
     */
    fun award(storedDayKey: String, storedCount: Int, todayKey: String): Award {
        val todayCount = if (storedDayKey == todayKey) storedCount else 0
        return Award(
            dayKey = todayKey,
            todayCount = todayCount + 1,
            earned = if (todayCount < DAILY_CAP) 1 else 0
        )
    }

    data class Award(val dayKey: String, val todayCount: Int, val earned: Int)
}
