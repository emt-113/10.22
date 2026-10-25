package com.aizeek.phonepulse.intervention

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Immediacy contract for the coach runtime (2026-10-25): a settings save reaches
 * the published live state within the 300ms reaction window — never waiting for
 * the 5s heartbeat — and a disabled coach publishes null budget/limit so the
 * home hero mirrors the switch instead of freezing on stale values.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UsageCoachRuntimeTest {

    @Test
    fun `saving settings bumps the shared change signal`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val before = UsageCoachSettings.changes.value
        UsageCoachSettings(context)
            .save(enabled = false, budgetMinutes = null, continuousLimitMinutes = null, graceSeconds = 30)
        assertEquals(before + 1, UsageCoachSettings.changes.value)
    }

    @Test
    fun `disabled coach publishes null budget and limit instead of stale values`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settings = UsageCoachSettings(context)
        settings.save(enabled = false, budgetMinutes = 240, continuousLimitMinutes = 45, graceSeconds = 30)
        val controller = startController(context, settings)
        val live = UsageCoachController.liveState().value
        assertFalse(live.enabled)
        assertNull(live.budgetMinutes)
        assertNull(live.continuousLimitMinutes)
        controller.stop()
    }

    @Test
    fun `a settings save reaches live state without waiting for a heartbeat`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settings = UsageCoachSettings(context)
        settings.save(enabled = true, budgetMinutes = 600, continuousLimitMinutes = null, graceSeconds = 30)
        val controller = startController(context, settings)
        assertEquals(600, UsageCoachController.liveState().value.budgetMinutes)

        // Save from the "UI card" instance; the reaction window (300ms) is well
        // below the 5s heartbeat, so the new budget must already be published.
        settings.save(enabled = true, budgetMinutes = 12, continuousLimitMinutes = null, graceSeconds = 30)
        advanceTimeBy(REACTION_WAIT_MS)
        runCurrent()
        assertEquals(12, UsageCoachController.liveState().value.budgetMinutes)
        assertTrue(UsageCoachController.liveState().value.enabled)
        controller.stop()
    }

    @Test
    fun `toggling the coach off hides the budget from live state immediately`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settings = UsageCoachSettings(context)
        settings.save(enabled = true, budgetMinutes = 240, continuousLimitMinutes = 45, graceSeconds = 30)
        val controller = startController(context, settings)
        assertEquals(240, UsageCoachController.liveState().value.budgetMinutes)

        settings.save(enabled = false, budgetMinutes = 240, continuousLimitMinutes = 45, graceSeconds = 30)
        advanceTimeBy(REACTION_WAIT_MS)
        runCurrent()
        val live = UsageCoachController.liveState().value
        assertFalse(live.enabled)
        assertNull(live.budgetMinutes)
        assertNull(live.continuousLimitMinutes)
        controller.stop()
    }

    private fun TestScope.startController(
        context: Context,
        settings: UsageCoachSettings
    ): UsageCoachController {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val controller = UsageCoachController(
            context,
            settings,
            UsageCoachNotifier(context),
            backfillDispatcher = dispatcher
        )
        // Unconfined: start() publishes its first snapshot synchronously, so the
        // assertions below never depend on (nor start) the 5s heartbeat loop.
        controller.start(CoroutineScope(SupervisorJob() + dispatcher))
        return controller
    }

    private companion object {
        // Reaction delay is 300ms; wait comfortably past it in virtual time while
        // staying below the 5s heartbeat so no beat can satisfy the assertion.
        const val REACTION_WAIT_MS = 1_000L
    }
}
