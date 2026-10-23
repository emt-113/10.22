package com.aizeek.phonepulse.intervention

/**
 * Usage coach rule engine (phase 1): daily screen-on budget + continuous usage reminder.
 *
 * Pure Kotlin and stateless per call: the caller owns persistence of fired keys
 * (per-day reset) and passes them back in, so every decision is unit-testable and
 * replayable.
 *
 * Reminder cadence (user-requested contract, covered by tests):
 *   - Budget: a single 80% warn per day, then once the budget is REACHED the
 *     coach nags — one reminder per extra screen-on minute until the screen
 *     goes off (screen-on accumulation pauses while off, so the nag pauses
 *     with it and resumes only when usage keeps growing).
 *   - Continuous: one reminder per minute over the limit within the session;
 *     a real screen-off (>= grace) ends the session and the nag with it.
 *
 * Statistical contract (fixed rule, covered by tests): a screen-off gap shorter
 * than [CONTINUOUS_GRACE_MS] keeps the continuous session alive; a gap of exactly
 * the grace value or longer ends it.
 */
object UsageCoachEngine {
    const val CONTINUOUS_GRACE_MS = 30_000L
    const val DEFAULT_CONTINUOUS_LIMIT_MIN = 45

    enum class AlertType { BUDGET_WARN, BUDGET_REACHED, CONTINUOUS_LIMIT }

    data class CoachConfig(
        /** Daily screen-on budget in minutes; null disables budget alerts. */
        val dailyBudgetMinutes: Int? = null,
        /** Warn at this percentage of the budget (0-100). */
        val budgetWarnPct: Int = 80,
        /** Continuous usage limit in minutes; null or <= 0 disables the reminder. */
        val continuousLimitMinutes: Int? = DEFAULT_CONTINUOUS_LIMIT_MIN
    )

    data class CoachAlert(
        val type: AlertType,
        val title: String,
        val message: String,
        /** Cooldown key the caller must persist for the rest of the day. */
        val cooldownKey: String
    )

    /**
     * Budget decision. [todayScreenOnMinutes] is the accumulated real screen-on
     * time for the calendar day; [firedKeys] are cooldown keys already shown today.
     */
    fun budgetAlert(
        todayScreenOnMinutes: Int,
        config: CoachConfig,
        firedKeys: Set<String>
    ): CoachAlert? {
        val budget = config.dailyBudgetMinutes
        if (budget == null || budget <= 0 || todayScreenOnMinutes < 0) return null
        val warnKey = "budget_warn_$budget"
        if (todayScreenOnMinutes >= budget) {
            // Over budget = nag mode: one reminder per extra screen-on minute.
            // The key embeds the minute value, so the same minute never repeats
            // while a new minute always fires — "every minute until screen off".
            val nagKey = "budget_reached_${budget}_$todayScreenOnMinutes"
            if (nagKey in firedKeys) return null
            val over = todayScreenOnMinutes - budget
            return CoachAlert(
                type = AlertType.BUDGET_REACHED,
                title = "今日亮屏已超预算",
                message = "今日亮屏 $todayScreenOnMinutes 分钟，已超预算 $over 分钟。放下手机休息一下，息屏即可结束提醒。",
                cooldownKey = nagKey
            )
        }
        val warnAt = budget * config.budgetWarnPct / 100.0
        if (todayScreenOnMinutes >= warnAt && warnAt > 0) {
            if (warnKey in firedKeys) return null
            return CoachAlert(
                type = AlertType.BUDGET_WARN,
                title = "今日亮屏预算已用 ${config.budgetWarnPct}%",
                message = "今日亮屏 $todayScreenOnMinutes 分钟，预算 $budget 分钟。还有 ${budget - todayScreenOnMinutes} 分钟余量。",
                cooldownKey = warnKey
            )
        }
        return null
    }

    /**
     * Continuous usage decision. [currentSessionMinutes] is the elapsed span of the
     * ongoing continuous session (short screen-off gaps included), 0 when idle.
     * Nags once per minute over the limit; the key embeds the minute value, so a
     * long session keeps reminding instead of staying silent after the first hit.
     */
    fun continuousAlert(
        currentSessionMinutes: Int,
        config: CoachConfig,
        firedKeys: Set<String>
    ): CoachAlert? {
        val limit = config.continuousLimitMinutes
        if (limit == null || limit <= 0 || currentSessionMinutes < limit) return null
        val key = "continuous_${limit}_$currentSessionMinutes"
        if (key in firedKeys) return null
        return CoachAlert(
            type = AlertType.CONTINUOUS_LIMIT,
            title = "已连续使用 $currentSessionMinutes 分钟",
            message = "该休息啦：远眺 20 秒，或起身倒杯水。本轮每分钟会再提醒一次，息屏即可结束。",
            cooldownKey = key
        )
    }
}

/**
 * Tracks the ongoing continuous session from raw screen moments.
 *
 * State machine: on screen-off the session is *suspended* (not ended) — whether the
 * grace window expires can only be known in the future. A re-light within the grace
 * window resumes the same session (start unchanged, gap included in the span, per
 * the agreed "连续使用时段" contract); a re-light at or beyond the grace value ends
 * the old session and starts a fresh one. A late on-screen event with no matching
 * off (process death) also starts fresh. One instance per day; call [reset] at
 * midnight. Durations use the monotonic clock provided by the caller.
 */
class ContinuousSessionTracker(private val graceMs: Long = UsageCoachEngine.CONTINUOUS_GRACE_MS) {
    private var sessionStartMs: Long? = null
    private var suspendedOffMs: Long? = null
    private var lastOnMs: Long? = null

    /**
     * Feed a screen-on transition observed at [timestampMs] (monotonic clock).
     * Resumes the session when either a short screen-off was recorded, or when the
     * screen-off event was missed entirely (OEM broadcast throttling) but the gap
     * since the last on-event is still within grace.
     */
    fun onScreenOn(timestampMs: Long) {
        val off = suspendedOffMs
        val last = lastOnMs
        val resuming = sessionStartMs != null && (
            (off != null && timestampMs - off < graceMs) ||
                (off == null && last != null && timestampMs - last < graceMs)
            )
        if (!resuming) sessionStartMs = timestampMs
        suspendedOffMs = null
        lastOnMs = timestampMs
    }

    /** Feed a screen-off transition; suspends the session pending the grace window. */
    fun onScreenOff(timestampMs: Long) {
        if (sessionStartMs == null) return
        suspendedOffMs = timestampMs
    }

    /**
     * Elapsed continuous session span in minutes (short screen-off gaps included);
     * 0 when no session is active or the grace window expired while screen was off.
     */
    fun currentSessionMinutes(nowMs: Long): Int {
        val start = sessionStartMs ?: return 0
        val off = suspendedOffMs
        if (off != null && nowMs - off >= graceMs) return 0
        return ((nowMs - start) / 60_000L).toInt().coerceAtLeast(0)
    }

    fun sessionStartMs(): Long? = sessionStartMs

    fun endSession() {
        sessionStartMs = null
        suspendedOffMs = null
        lastOnMs = null
    }

    fun reset() = endSession()
}
