package com.aizeek.phonepulse

import com.aizeek.phonepulse.intervention.ContinuousSessionTracker
import com.aizeek.phonepulse.intervention.UsageCoachEngine.AlertType
import com.aizeek.phonepulse.intervention.UsageCoachEngine.CoachConfig
import com.aizeek.phonepulse.intervention.UsageCoachEngine.budgetAlert
import com.aizeek.phonepulse.intervention.UsageCoachEngine.continuousAlert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class UsageCoachEngineTest {

    private val config = CoachConfig(dailyBudgetMinutes = 240, continuousLimitMinutes = 45)

    // ---- budget ----

    @Test
    fun `warns at configured budget percentage`() {
        val alert = budgetAlert(195, config, emptySet()) // 81% of 240
        assertEquals(AlertType.BUDGET_WARN, alert!!.type)
        assertEquals("budget_warn_240", alert.cooldownKey)
    }

    @Test
    fun `no warn below threshold`() {
        assertNull(budgetAlert(190, config, emptySet())) // 79% of 240
    }

    @Test
    fun `exact warn boundary triggers`() {
        assertNotNull(budgetAlert(192, config, emptySet())) // exactly 80%
    }

    @Test
    fun `reached fires even if warn was skipped`() {
        val fired = setOf("budget_warn_240")
        val alert = budgetAlert(240, config, fired)
        assertEquals(AlertType.BUDGET_REACHED, alert!!.type)
        // Reaching 100% summarizes regardless: tiny budgets may cross both thresholds.
        assertNotNull(budgetAlert(240, config, emptySet()))
        assertNull(budgetAlert(240, config, fired + "budget_reached_240")) // already fired
    }

    @Test
    fun `warn does not repeat on same day`() {
        assertNull(budgetAlert(200, config, setOf("budget_warn_240")))
    }

    @Test
    fun `disabled or invalid budget yields nothing`() {
        assertNull(budgetAlert(300, CoachConfig(dailyBudgetMinutes = null), emptySet()))
        assertNull(budgetAlert(300, CoachConfig(dailyBudgetMinutes = 0), emptySet()))
        assertNull(budgetAlert(-5, config, emptySet()))
    }

    // ---- continuous usage ----

    @Test
    fun `continuous limit triggers at threshold with once-per-day cooldown`() {
        val alert = continuousAlert(45, config, emptySet())
        assertEquals(AlertType.CONTINUOUS_LIMIT, alert!!.type)
        assertEquals("continuous_45", alert.cooldownKey)
        assertNull(continuousAlert(44, config, emptySet()))
        // Same limit later in the same session must NOT re-fire (no per-minute spam).
        assertNull(continuousAlert(90, config, setOf("continuous_45")))
        assertNull(continuousAlert(120, config, setOf("continuous_45")))
    }

    @Test
    fun `disabled continuous limit yields nothing`() {
        assertNull(continuousAlert(120, CoachConfig(continuousLimitMinutes = null), emptySet()))
        assertNull(continuousAlert(120, CoachConfig(continuousLimitMinutes = 0), emptySet()))
    }

    // ---- continuous session tracker (30s grace, < grace merges) ----

    @Test
    fun `short screen-off gap keeps the session alive`() {
        val tracker = ContinuousSessionTracker()
        val t0 = 0L
        tracker.onScreenOn(t0)
        tracker.onScreenOff(t0 + 20 * 60_000L)          // 20 min use
        tracker.onScreenOn(t0 + 20 * 60_000L + 10_000L) // 10s gap (< 30s grace): resume
        // Span includes the short gap (agreed contract): 20min + 10s + 25min = 45min.
        assertEquals(45, tracker.currentSessionMinutes(t0 + 20 * 60_000L + 10_000L + 25 * 60_000L))
        assertEquals(t0, tracker.sessionStartMs())
    }

    @Test
    fun `suspended session expires once grace passes without re-light`() {
        val tracker = ContinuousSessionTracker()
        tracker.onScreenOn(0L)
        tracker.onScreenOff(20 * 60_000L)
        // No re-light: 5 minutes later the session no longer counts as ongoing.
        assertEquals(0, tracker.currentSessionMinutes(20 * 60_000L + 5 * 60_000L))
    }

    @Test
    fun `gap of exactly the grace value ends the session`() {
        val tracker = ContinuousSessionTracker()
        tracker.onScreenOn(0L)
        tracker.onScreenOff(20 * 60_000L)
        tracker.onScreenOn(20 * 60_000L + 30_000L) // exactly 30s: ends, fresh session
        assertEquals(0, tracker.currentSessionMinutes(20 * 60_000L + 30_000L))
        assertEquals(20 * 60_000L + 30_000L, tracker.sessionStartMs())
    }

    @Test
    fun `gap longer than grace starts a fresh session`() {
        val tracker = ContinuousSessionTracker()
        tracker.onScreenOn(0L)
        tracker.onScreenOff(30 * 60_000L)
        tracker.onScreenOn(30 * 60_000L + 60_000L) // 60s gap
        assertEquals(0, tracker.currentSessionMinutes(30 * 60_000L + 60_000L))
        assertEquals(5, tracker.currentSessionMinutes(30 * 60_000L + 60_000L + 5 * 60_000L))
        assertEquals(30 * 60_000L + 60_000L, tracker.sessionStartMs())
    }

    @Test
    fun `missed screen-off event still resumes within grace`() {
        // OEM throttled the SCREEN_OFF broadcast: only an on-event arrives 10s later.
        val tracker = ContinuousSessionTracker()
        tracker.onScreenOn(0L)
        tracker.onScreenOn(10_000L) // no OFF in between (broadcast swallowed)
        assertEquals(10, tracker.currentSessionMinutes(10 * 60_000L))
        assertEquals(0L, tracker.sessionStartMs())
    }

    @Test
    fun `late on-screen event after long outage starts fresh session`() {
        val tracker = ContinuousSessionTracker()
        tracker.onScreenOn(0L)
        // Caller missed the screen-off event (process death): 1h later screen turns on again.
        tracker.onScreenOn(60 * 60_000L)
        assertEquals(1, tracker.currentSessionMinutes(60 * 60_000L + 1 * 60_000L))
        assertEquals(60 * 60_000L, tracker.sessionStartMs())
    }

    @Test
    fun `idle tracker reports zero`() {
        val tracker = ContinuousSessionTracker()
        assertEquals(0, tracker.currentSessionMinutes(1000L))
        tracker.onScreenOn(0L)
        tracker.endSession()
        assertEquals(0, tracker.currentSessionMinutes(60 * 60_000L))
    }
}
