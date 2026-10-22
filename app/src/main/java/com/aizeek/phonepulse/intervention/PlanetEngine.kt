package com.aizeek.phonepulse.intervention

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Planet + weekly insights engine (pure Kotlin, fully testable).
 *
 * The planet mirrors the user's screen-time goal achievement — never battery
 * health (that stays independent per product decision). Stage rules are
 * explainable in-app:
 *   SHINING — last 3 valid days all achieved
 *   RUNNING — last 2 valid days all achieved
 *   SPROUT — otherwise (initial state)
 * A "valid day" is one with a budget set AND enough data; days without data are
 * skipped, never counted as achieved or failed.
 */
object PlanetEngine {
    const val SUGGESTION_BUDGET_HIGH_PCT = 85

    enum class Stage { SPROUT, RUNNING, SHINING }

    data class DayRecord(
        val dateKey: String,        // yyyy-MM-dd
        val screenOnMinutes: Int,   // real accumulated screen-on minutes
        val budgetMinutes: Int?,    // budget that day; null = feature off / no data
        val achieved: Boolean?      // true=in budget, false=over, null=no data
    )

    /**
     * Stage from achievement history. With [todayKey] provided, a stale history
     * (no valid record yesterday or today) falls back to SPROUT so the planet
     * never shines on weeks-old data.
     */
    fun stage(records: List<DayRecord?>, todayKey: String? = null): Stage {
        val valid = records.filterNotNull()
            .filter { it.achieved != null && it.budgetMinutes != null }
            .sortedByDescending { it.dateKey }
        if (valid.isEmpty()) return Stage.SPROUT
        if (todayKey != null && valid.first().dateKey < shiftKey(todayKey, -1)) return Stage.SPROUT
        val achievedHead = valid.takeWhile { it.achieved == true }
        return when {
            achievedHead.size >= 3 -> Stage.SHINING
            achievedHead.size >= 2 -> Stage.RUNNING
            else -> Stage.SPROUT
        }
    }

    /**
     * Consecutive achieved days, counting from the latest *met-or-skipped* run:
     * the newest unmet days are skipped (today may already be over budget) and the
     * streak length before that run is returned. 0 when nothing achieved yet.
     */
    fun consecutiveAchievedDays(records: List<DayRecord?>): Int {
        val valid = records.filterNotNull()
            .filter { it.achieved != null && it.budgetMinutes != null }
            .sortedByDescending { it.dateKey }
        return valid.dropWhile { it.achieved == false }.takeWhile { it.achieved == true }.size
    }

    /** Fixed 7-day slot list ending at [endKey] (inclusive); missing days are null. */
    fun last7Days(records: List<DayRecord>, endKey: String): List<DayRecord?> {
        val byKey = records.associateBy { it.dateKey }
        return (6 downTo 0).map { offset ->
            byKey[shiftKey(endKey, -offset)]
        }
    }

    fun todayKey(now: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))

    fun shiftKey(key: String, days: Int): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = fmt.parse(key) ?: return key
        val shifted = Date(date.time + days * 86_400_000L)
        return fmt.format(shifted)
    }
}

/**
 * Local weekly insights. Real-screen-time based (NOT merged session spans);
 * missing days are "数据不足", never silently counted as achieved/failed;
 * week-over-week comparison only when both weeks have enough data.
 */
object WeeklyInsightsEngine {
    const val MIN_DAYS_FOR_COMPARISON = 3

    data class WeekStats(
        val avgScreenOnMinutes: Int?,
        val daysWithData: Int,
        val achievedDays: Int,
        val daysWithBudget: Int,
        val prevWeekAvgMinutes: Int?,
        /** null when data is insufficient — show "继续记录，生成趋势" instead. */
        val comparisonDeltaMinutes: Int?,
        /** One local rule-based suggestion; null when nothing applies. */
        val suggestion: String?
    )

    fun compute(
        thisWeek: List<PlanetEngine.DayRecord>,
        lastWeek: List<PlanetEngine.DayRecord>,
        todayMinutes: Int? = null,
        todayBudget: Int? = null
    ): WeekStats {
        val validThis = thisWeek.filter { it.screenOnMinutes >= 0 && it.achieved != null }
        val avgThis = validThis.map { it.screenOnMinutes }.takeIf { it.isNotEmpty() }?.average()?.toInt()
        val achieved = validThis.count { it.achieved == true }
        val withBudget = validThis.count { it.budgetMinutes != null }

        val validLast = lastWeek.filter { it.screenOnMinutes >= 0 && it.achieved != null }
        val avgLast = validLast.map { it.screenOnMinutes }.takeIf { it.isNotEmpty() }?.average()?.toInt()

        val enoughThis = validThis.size >= MIN_DAYS_FOR_COMPARISON
        val enoughLast = validLast.size >= MIN_DAYS_FOR_COMPARISON
        val delta = if (enoughThis && enoughLast && avgThis != null && avgLast != null) avgLast - avgThis else null

        val suggestion = ruleSuggestion(validThis, todayMinutes, todayBudget, achieved)

        return WeekStats(
            avgScreenOnMinutes = avgThis,
            daysWithData = validThis.size,
            achievedDays = achieved,
            daysWithBudget = withBudget,
            prevWeekAvgMinutes = avgLast,
            comparisonDeltaMinutes = delta,
            suggestion = suggestion
        )
    }

    /** One suggestion, priority: budget nearly used today > streak encouragement > keep recording. */
    private fun ruleSuggestion(
        thisWeek: List<PlanetEngine.DayRecord>,
        todayMinutes: Int?,
        todayBudget: Int?,
        achievedDays: Int
    ): String? {
        if (todayMinutes != null && todayBudget != null && todayBudget > 0) {
            val pct = todayMinutes * 100.0 / todayBudget
            if (pct >= PlanetEngine.SUGGESTION_BUDGET_HIGH_PCT) {
                return "今天已使用每日预算的 ${pct.toInt()}%，可以考虑安排一次短暂休息。"
            }
        }
        if (achievedDays >= 3) {
            return "最近已连续 $achievedDays 天达成预算目标，保持这个节奏。"
        }
        if (thisWeek.size < 3) return "继续记录，生成趋势。"
        return null
    }
}

/**
 * SharedPreferences-backed day records (no schema migration needed). One flat
 * entry per day; the coach controller archives yesterday's record at day rollover.
 */
class PlanetDayStore(context: Context) {
    private val prefs = context.getSharedPreferences("planet_days", Context.MODE_PRIVATE)

    fun put(record: PlanetEngine.DayRecord) {
        prefs.edit().putString(record.dateKey, encode(record)).apply()
    }

    fun get(dateKey: String): PlanetEngine.DayRecord? =
        prefs.getString(dateKey, null)?.let { decode(dateKey, it) }

    /** Loads [days] ending at [endKey]; missing slots are absent from the result. */
    fun loadRange(endKey: String, days: Int = 7): List<PlanetEngine.DayRecord> {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return (days - 1 downTo 0).mapNotNull { offset ->
            val key = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .format(Date(fmt.parse(endKey)!!.time - offset * 86_400_000L))
            get(key)
        }
    }

    companion object {
        // Format: minutes;budget(null=-1);achieved(null=-1,true=1,false=0)
        internal fun encode(r: PlanetEngine.DayRecord): String =
            "${r.screenOnMinutes};${r.budgetMinutes ?: -1};${when (r.achieved) { true -> 1; false -> 0; null -> -1 }}"

        internal fun decode(dateKey: String, raw: String): PlanetEngine.DayRecord? {
            val parts = raw.split(";")
            if (parts.size != 3) return null
            val minutes = parts[0].toIntOrNull() ?: return null
            val budget = parts[1].toIntOrNull()?.takeIf { it > 0 }
            val achieved = when (parts[2].toIntOrNull()) { 1 -> true; 0 -> false; else -> null }
            return PlanetEngine.DayRecord(dateKey, minutes, budget, achieved)
        }
    }
}
