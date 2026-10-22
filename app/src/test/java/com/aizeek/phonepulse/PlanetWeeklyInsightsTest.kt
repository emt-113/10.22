package com.aizeek.phonepulse

import com.aizeek.phonepulse.intervention.PlanetDayStore
import com.aizeek.phonepulse.intervention.PlanetEngine
import com.aizeek.phonepulse.intervention.PlanetEngine.Stage
import com.aizeek.phonepulse.intervention.WeeklyInsightsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanetWeeklyInsightsTest {

    private fun day(key: String, minutes: Int, budget: Int?, achieved: Boolean?) =
        PlanetEngine.DayRecord(key, minutes, budget, achieved)

    private val days = listOf("d1", "d2", "d3", "d4", "d5")

    // ---- stage ----

    @Test
    fun `sprout when no valid days`() {
        assertEquals(Stage.SPROUT, PlanetEngine.stage(emptyList()))
        assertEquals(Stage.SPROUT, PlanetEngine.stage(listOf(day("d1", 100, null, null))))
    }

    @Test
    fun `sprout when latest day unmet`() {
        val records = listOf(day("d1", 100, 200, true), day("d2", 300, 200, false))
        assertEquals(Stage.SPROUT, PlanetEngine.stage(records))
    }

    @Test
    fun `running after two achieved days`() {
        val records = days.mapIndexed { i, k -> day(k, 100, 200, true) }
        assertEquals(Stage.RUNNING, PlanetEngine.stage(records.take(2)))
    }

    @Test
    fun `shining after three achieved days`() {
        val records = days.mapIndexed { i, k -> day(k, 100, 200, true) }
        assertEquals(Stage.SHINING, PlanetEngine.stage(records.take(3)))
    }

    @Test
    fun `unmet latest day resets streak to sprout`() {
        val records = listOf(
            day("d1", 100, 200, true), day("d2", 100, 200, true), day("d3", 300, 200, false))
        assertEquals(Stage.SPROUT, PlanetEngine.stage(records))
    }

    @Test
    fun `days without data are skipped not counted as failure`() {
        val records = listOf(
            day("d1", 100, 200, true), day("d2", 100, 200, true),
            day("d3", 100, null, null), day("d4", 100, 200, true))
        // Valid run d4,d2,d1 all achieved -> SHINING (null-data day skipped, not failed).
        assertEquals(Stage.SHINING, PlanetEngine.stage(records))
    }

    @Test
    fun `consecutive achieved days counted from latest`() {
        val records = listOf(
            day("d1", 100, 200, true), day("d2", 100, 200, true), day("d3", 300, 200, false))
        assertEquals(2, PlanetEngine.consecutiveAchievedDays(records))
    }

    @Test
    fun `seven day slots aligned to end key`() {
        val end = "2026-10-20"
        val records = listOf(day("2026-10-20", 100, 200, true), day("2026-10-18", 90, 200, true))
        val slots = PlanetEngine.last7Days(records, end)
        assertEquals(7, slots.size)
        assertEquals("2026-10-20", slots[6]!!.dateKey)
        assertNull(slots[5])                     // 10-19 missing
        assertEquals("2026-10-18", slots[4]!!.dateKey)
        assertNull(slots[0])                     // 10-14 missing
    }

    @Test
    fun `stale history falls back to sprout`() {
        // Achieved 3 days in a row, but newest record is 10 days old.
        val records = listOf(
            day("2026-10-05", 100, 200, true),
            day("2026-10-06", 100, 200, true),
            day("2026-10-07", 100, 200, true))
        assertEquals(Stage.SHINING, PlanetEngine.stage(records))
        assertEquals(Stage.SPROUT, PlanetEngine.stage(records, todayKey = "2026-10-20"))
        assertEquals(Stage.SHINING, PlanetEngine.stage(records, todayKey = "2026-10-08"))
    }

    @Test
    fun `shift key moves across month boundary`() {
        assertEquals("2026-10-01", PlanetEngine.shiftKey("2026-09-30", 1))
        assertEquals("2026-09-30", PlanetEngine.shiftKey("2026-10-01", -1))
    }

    // ---- weekly insights ----

    private val week = listOf(
        day("a1", 120, 240, true), day("a2", 300, 240, false), day("a3", 180, 240, true))

    @Test
    fun `averages use real screen-on minutes`() {
        val stats = WeeklyInsightsEngine.compute(week, emptyList())
        assertEquals(200, stats.avgScreenOnMinutes) // (120+300+180)/3
        assertEquals(3, stats.daysWithData)
        assertEquals(2, stats.achievedDays)
        assertEquals(3, stats.daysWithBudget)
    }

    @Test
    fun `comparison requires enough data on both weeks`() {
        assertNull(WeeklyInsightsEngine.compute(week, emptyList()).comparisonDeltaMinutes)
        val fullLast = (1..3).map { day("b$it", 250, 240, true) }
        val stats = WeeklyInsightsEngine.compute(week, fullLast) // last avg 250, this 200
        assertEquals(50, stats.comparisonDeltaMinutes) // 50 min less than last week
    }

    @Test
    fun `insufficient data suggests keep recording`() {
        val stats = WeeklyInsightsEngine.compute(listOf(day("a1", 120, 240, true)), emptyList())
        assertEquals("继续记录，生成趋势。", stats.suggestion)
    }

    @Test
    fun `budget nearly used today gets a break suggestion`() {
        val stats = WeeklyInsightsEngine.compute(week, emptyList(), todayMinutes = 210, todayBudget = 240)
        assertTrue(stats.suggestion!!.contains("87%"))
        assertTrue(stats.suggestion!!.contains("休息"))
    }

    @Test
    fun `streak of three gets encouragement`() {
        val streakWeek = (1..4).map { day("c$it", 100, 240, true) }
        val stats = WeeklyInsightsEngine.compute(streakWeek, emptyList(), todayMinutes = 50, todayBudget = 240)
        assertTrue(stats.suggestion!!.contains("连续"))
    }

    // ---- store codec ----

    @Test
    fun `day record codec roundtrip with nulls`() {
        val samples = listOf(
            day("d1", 120, 240, true), day("d2", 300, 240, false),
            day("d3", 90, null, null))
        samples.forEach {
            val decoded = PlanetDayStore.decode(it.dateKey, PlanetDayStore.encode(it))
            assertEquals(it, decoded)
        }
        assertNull(PlanetDayStore.decode("d1", "garbage"))
        assertNull(PlanetDayStore.decode("d1", "1;2"))
    }
}
