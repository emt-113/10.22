package com.aizeek.phonepulse.intervention

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * "Welcome back" moment (user-requested game feedback): when the user returns to
 * the app after being away long enough for the eyes to rest, the planet has
 * "collected stardust" during the break — a small payoff that reinforces the
 * rest instead of punishing it. Pure rule engine + SharedPreferences store;
 * no database schema impact.
 */
object ReturnGiftEngine {
    /** Rests shorter than this do not count (a glance away is not a break). */
    const val MIN_GAP_MINUTES = 3

    /** Stardust per gap: 1 for any valid rest + 1 per full 5 minutes, capped. */
    const val MAX_STARDUST_PER_GAP = 4

    data class Gift(
        val gapMinutes: Int,
        val stardustEarned: Int,
        val totalStardust: Int
    )

    /** null when the absence is too short to count as a rest. */
    fun compute(gapMinutes: Int, currentTotal: Int): Gift? {
        if (gapMinutes < MIN_GAP_MINUTES) return null
        val earned = (gapMinutes / 5 + 1).coerceAtMost(MAX_STARDUST_PER_GAP)
        return Gift(gapMinutes, earned, currentTotal + earned)
    }
}

/**
 * SharedPreferences-backed leave/arrival bookkeeping. [recordLeave] on app
 * background, [onArrive] on app foreground; the stardust total persists across
 * installs of the same data and feeds the popup copy.
 */
class ReturnGiftStore(context: Context) {
    private val prefs = context.getSharedPreferences("planet_gift", Context.MODE_PRIVATE)

    fun recordLeave(nowMs: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_LAST_LEAVE, nowMs).apply()
    }

    /** Called on app foreground; returns a gift when the absence counts as a rest. */
    fun onArrive(nowMs: Long = System.currentTimeMillis()): ReturnGiftEngine.Gift? {
        val left = prefs.getLong(KEY_LAST_LEAVE, 0L)
        if (left <= 0L) return null
        val gapMinutes = ((nowMs - left) / 60_000L).toInt()
        if (gapMinutes < ReturnGiftEngine.MIN_GAP_MINUTES) return null
        val gift = ReturnGiftEngine.compute(gapMinutes, totalStardust()) ?: return null
        prefs.edit().putInt(KEY_TOTAL, gift.totalStardust).apply()
        return gift
    }

    fun totalStardust(): Int = prefs.getInt(KEY_TOTAL, 0)

    private companion object {
        const val KEY_LAST_LEAVE = "last_leave_ms"
        const val KEY_TOTAL = "stardust_total"
    }
}

/** App-level bus between the MainActivity lifecycle and the Compose overlay. */
object ReturnGiftCenter {
    private val gift = MutableStateFlow<ReturnGiftEngine.Gift?>(null)

    fun gift(): StateFlow<ReturnGiftEngine.Gift?> = gift.asStateFlow()

    fun post(value: ReturnGiftEngine.Gift) {
        gift.value = value
    }

    fun clear() {
        gift.value = null
    }
}
