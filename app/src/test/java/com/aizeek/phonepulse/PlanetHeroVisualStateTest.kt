package com.aizeek.phonepulse

import com.aizeek.phonepulse.intervention.PlanetEngine
import com.aizeek.phonepulse.ui.components.OrbitArc
import com.aizeek.phonepulse.ui.components.PlanetVisualState
import com.aizeek.phonepulse.ui.components.RestArrival
import com.aizeek.phonepulse.ui.components.heroRestArrival
import com.aizeek.phonepulse.ui.components.planetHeroState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure logic behind the planet hero (2026-10-25): arc-length parametrization of
 * the budget lane, the "刚休息回来" welcome window, and the visual state ladder.
 */
class PlanetHeroVisualStateTest {

    // ---- OrbitArc: arc-length <-> sweep inversion ------------------------------

    @Test
    fun `sweep endpoints and quadrant boundaries are exact`() {
        assertEquals(0f, OrbitArc.sweepFor(0f), 0.01f)
        assertEquals(360f, OrbitArc.sweepFor(1f), 0.01f)
        // Each parametric quadrant of the ellipse is exactly a quarter of its
        // circumference, so the corrected sweep must hit them on the nose.
        assertEquals(90f, OrbitArc.sweepFor(0.25f), 0.5f)
        assertEquals(180f, OrbitArc.sweepFor(0.5f), 0.5f)
        assertEquals(270f, OrbitArc.sweepFor(0.75f), 0.5f)
    }

    @Test
    fun `sweep grows monotonically with progress`() {
        var prev = -1f
        for (k in 0..100) {
            val s = OrbitArc.sweepFor(k / 100f)
            assertTrue("sweep must be non-decreasing at $k", s >= prev)
            prev = s
        }
    }

    @Test
    fun `low progress needs MORE angle than the naive sweep (flat ends are cheap)`() {
        // Naive: 360°·0.10 = 36°. The flat start means 36° of angle covers only
        // ~6% of the orbit, so the corrected sweep must exceed the naive one.
        assertTrue(OrbitArc.sweepFor(0.10f) > 37f)
        // Mirror symmetry: past the first quadrant the corrected sweep runs ahead.
        assertTrue(OrbitArc.sweepFor(0.40f) < 143f)
    }

    // ---- Rest arrival ------------------------------------------------------------

    @Test
    fun `no rest arrival without a gap or with a short one`() {
        assertNull(heroRestArrival(nowMs = 1_000_000L, wakeAtMs = 999_000L, lastScreenOffMs = null))
        assertNull(heroRestArrival(nowMs = 1_000_000L, wakeAtMs = 999_000L, lastScreenOffMs = 2 * 60_000L))
    }

    @Test
    fun `three minute rest inside the window arrives with floored minutes`() {
        val wake = 1_000_000L
        val arrival = heroRestArrival(
            nowMs = wake + 30_000L,
            wakeAtMs = wake,
            lastScreenOffMs = 3 * 60_000L + 45_000L
        )
        assertEquals(3, arrival!!.minutes)
    }

    @Test
    fun `arrival expires once the welcome window has passed`() {
        val wake = 1_000_000L
        assertNotNull(heroRestArrival(wake + 60_000L, wake, 5 * 60_000L))
        assertNull(heroRestArrival(wake + 60_001L, wake, 5 * 60_000L))
    }

    // ---- Hero state priority -------------------------------------------------------

    @Test
    fun `welcome back moment wins even over an over-budget nag`() {
        val state = planetHeroState(
            isScreenOn = true,
            restArrival = RestArrival(5),
            continuousLimitMinutes = 45,
            sessionMinutes = 99,
            budgetMinutes = 100,
            todayMinutes = 99,
            stage = PlanetEngine.Stage.SPROUT
        )
        assertEquals(PlanetVisualState.RESTING, state)
    }

    @Test
    fun `screen off rests, limits and 85pct budget nag, streaks turn good, default steady`() {
        assertEquals(
            PlanetVisualState.RESTING,
            planetHeroState(false, null, null, 0, null, 0, PlanetEngine.Stage.SPROUT)
        )
        assertEquals(
            PlanetVisualState.SUGGEST_REST,
            planetHeroState(true, null, 45, 45, null, 0, PlanetEngine.Stage.SPROUT)
        )
        assertEquals(
            PlanetVisualState.SUGGEST_REST,
            planetHeroState(true, null, null, 0, 200, 170, PlanetEngine.Stage.SPROUT)
        )
        assertEquals(
            PlanetVisualState.GOOD,
            planetHeroState(true, null, null, 0, 200, 20, PlanetEngine.Stage.RUNNING)
        )
        assertEquals(
            PlanetVisualState.STEADY,
            planetHeroState(true, null, null, 0, null, 0, PlanetEngine.Stage.SPROUT)
        )
    }
}
