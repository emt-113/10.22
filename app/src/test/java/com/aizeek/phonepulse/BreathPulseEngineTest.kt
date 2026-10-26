package com.aizeek.phonepulse

import com.aizeek.phonepulse.intervention.BreathPulseEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BreathPulseEngineTest {

    // --- hold progress -------------------------------------------------------

    @Test
    fun `progress ramps linearly and clamps at both ends`() {
        assertEquals(0f, BreathPulseEngine.holdProgress(0L))
        assertEquals(0.5f, BreathPulseEngine.holdProgress(BreathPulseEngine.HOLD_TARGET_MS / 2))
        assertEquals(1f, BreathPulseEngine.holdProgress(BreathPulseEngine.HOLD_TARGET_MS))
        // An over-long press never overshoots the ring; negative time clamps to 0.
        assertEquals(1f, BreathPulseEngine.holdProgress(BreathPulseEngine.HOLD_TARGET_MS * 10))
        assertEquals(0f, BreathPulseEngine.holdProgress(-5L))
    }

    @Test
    fun `completion is exactly at the target, not before`() {
        assertFalse(BreathPulseEngine.isComplete(BreathPulseEngine.HOLD_TARGET_MS - 1))
        assertTrue(BreathPulseEngine.isComplete(BreathPulseEngine.HOLD_TARGET_MS))
    }

    @Test
    fun `waveform stays within a unit sine envelope`() {
        val v = BreathPulseEngine.wave(1.234f)
        assertTrue(v in -1f..1f)
        assertEquals(kotlin.math.sin(1.234f), v, 1e-6f)
    }

    // --- day-capped award ----------------------------------------------------

    @Test
    fun `first breath of a new day earns one`() {
        val award = BreathPulseEngine.award("20261026", 10, "20261027")
        assertEquals(1, award.earned)
        assertEquals(1, award.todayCount)
        assertEquals("20261027", award.dayKey)
    }

    @Test
    fun `same day increments the counter`() {
        val award = BreathPulseEngine.award("20261027", 3, "20261027")
        assertEquals(1, award.earned)
        assertEquals(4, award.todayCount)
    }

    @Test
    fun `daily cap stops earning but keeps counting`() {
        val award = BreathPulseEngine.award("20261027", BreathPulseEngine.DAILY_CAP, "20261027")
        assertEquals(0, award.earned)
        assertEquals(BreathPulseEngine.DAILY_CAP + 1, award.todayCount)
    }

    @Test
    fun `missing stored day behaves like a fresh day`() {
        val award = BreathPulseEngine.award("", 0, "20261027")
        assertEquals(1, award.earned)
    }
}
