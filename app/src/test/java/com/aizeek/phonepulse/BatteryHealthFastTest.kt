package com.aizeek.phonepulse

import com.aizeek.phonepulse.battery.BatteryCycle
import com.aizeek.phonepulse.battery.BatteryHealthEstimate
import com.aizeek.phonepulse.intervention.BatteryHealthFacade
import com.aizeek.phonepulse.intervention.DesignCapacityResolver
import com.aizeek.phonepulse.intervention.FastHealthEstimator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryHealthFastTest {

    private fun cycle(
        id: Long,
        startPct: Int,
        endPct: Int,
        netMah: Double,
        charging: Boolean = true,
        excluded: Boolean = false,
        revision: Int = 0,
        rejectionReason: String? = null,
        socDiscontinuity: Boolean = false,
        measuredMs: Long = 3_600_000,
        missingMs: Long = 0
    ) = BatteryCycle(id = id, charging = charging, startTime = id, endTime = id + 10,
        lastTime = id + 10, lastElapsedMs = 0, startPct = startPct, endPct = endPct,
        netMah = netMah, measuredMs = measuredMs, missingMs = missingMs,
        rejectionReason = rejectionReason, excluded = excluded,
        calibrationRevision = revision, socDiscontinuity = socDiscontinuity)

    // ---- FastHealthEstimator ----

    @Test
    fun `short span session is accepted by fast estimator`() {
        val sample = FastHealthEstimator.sample(cycle(1, 80, 92, 336.0), 0)
        assertEquals(2800.0, sample!!.capacityMah, 0.001)
    }

    @Test
    fun `span below minimum is rejected`() {
        assertNull(FastHealthEstimator.sample(cycle(1, 90, 96, 168.0), 0))
    }

    @Test
    fun `tolerated rejection reasons pass through`() {
        listOf("SMALL_SOC_CHANGE", "INCOMPLETE_RESTORE", null).forEach { reason ->
            assertEquals(2800.0,
                FastHealthEstimator.sample(cycle(1, 80, 92, 336.0, rejectionReason = reason), 0)!!.capacityMah,
                0.001)
        }
        assertNull(FastHealthEstimator.sample(cycle(1, 80, 92, 336.0, rejectionReason = "REBOOT"), 0))
    }

    @Test
    fun `low coverage session is rejected`() {
        assertNull(FastHealthEstimator.sample(cycle(1, 80, 92, 336.0,
            measuredMs = 600_000, missingMs = 4_000_000), 0))
    }

    @Test
    fun `discharge nonpositive and mismatched revision cycles are rejected`() {
        assertNull(FastHealthEstimator.sample(cycle(1, 92, 80, -336.0), 0))
        assertNull(FastHealthEstimator.sample(cycle(1, 80, 92, 336.0, revision = 1), 0))
        assertNull(FastHealthEstimator.sample(cycle(1, 80, 92, 336.0, socDiscontinuity = true), 0))
        assertNull(FastHealthEstimator.sample(cycle(1, 80, 92, 336.0, excluded = true), 0))
    }

    @Test
    fun `estimate takes median of samples`() {
        val cycles = listOf(
            cycle(1, 50, 66, 720.0),   // 4500
            cycle(2, 20, 36, 700.0),   // 4375
            cycle(3, 60, 76, 740.0)    // 4625
        )
        val estimate = FastHealthEstimator.estimate(cycles, 0)!!
        assertEquals(4500.0, estimate.capacityMah, 0.001)
        assertEquals(3, estimate.sampleCount)
    }

    @Test
    fun `no eligible samples yields null`() {
        assertNull(FastHealthEstimator.estimate(emptyList(), 0))
        assertNull(FastHealthEstimator.estimate(listOf(cycle(1, 95, 99, 100.0)), 0))
    }

    // ---- BatteryHealthFacade ----

    private val formal = BatteryHealthEstimate(capacityMah = 4000.0, acceptedCount = 2)

    @Test
    fun `formal evidence wins and divides by manual design capacity`() {
        val display = BatteryHealthFacade.display(formal, emptyList(), 5000.0, null, 0)
        assertEquals(BatteryHealthFacade.Source.FORMAL, display.source)
        assertEquals(4000.0, display.capacityMah!!, 0.001)
        assertEquals(80.0, display.healthPct!!, 0.001)
    }

    @Test
    fun `system measured capacity used when formal missing`() {
        val display = BatteryHealthFacade.display(
            BatteryHealthEstimate(), emptyList(), 4400.0, 3960.0, 0)
        assertEquals(BatteryHealthFacade.Source.FAST, display.source)
        assertEquals(90.0, display.healthPct!!, 0.001)
    }

    @Test
    fun `fast sampled estimate is third`() {
        val cycles = listOf(cycle(1, 80, 92, 336.0)) // 2800 mAh
        val display = BatteryHealthFacade.display(
            BatteryHealthEstimate(), cycles, 4000.0, null, 0)
        assertEquals(BatteryHealthFacade.Source.FAST, display.source)
        assertEquals(2800.0, display.capacityMah!!, 0.001)
        assertEquals(70.0, display.healthPct!!, 0.001)
        assertEquals(1, display.fastSampleCount)
    }

    @Test
    fun `no evidence and no design capacity yields none`() {
        val display = BatteryHealthFacade.display(BatteryHealthEstimate(), emptyList(), null, null, 0)
        assertEquals(BatteryHealthFacade.Source.NONE, display.source)
        assertNull(display.capacityMah)
        assertNull(display.healthPct)
    }

    @Test
    fun `absurd ratio is flagged as anomalous, never hidden`() {
        // capacity 4000 against design 10000 would read 40%: below the plausible floor.
        // The value stays visible but is flagged; real severe degradation must not be suppressed.
        val display = BatteryHealthFacade.display(formal, emptyList(), 10_000.0, null, 0)
        assertTrue(display.anomalous)
        assertEquals(40.0, display.healthPct!!, 0.001)
        assertEquals(4000.0, display.capacityMah!!, 0.001)
    }

    @Test
    fun `plausible ratio is not flagged`() {
        val display = BatteryHealthFacade.display(formal, emptyList(), 5000.0, null, 0)
        assertEquals(80.0, display.healthPct!!, 0.001)
        assertEquals(false, display.anomalous)
    }

    // ---- DesignCapacityResolver ----

    @Test
    fun `parses microamp-hour kernel values`() {
        assertEquals(4400.0, DesignCapacityResolver.parseCapacityMah("4400000")!!, 0.001)
        assertEquals(5000.0, DesignCapacityResolver.parseCapacityMah(" 5000000\n")!!, 0.001)
    }

    @Test
    fun `parses milliamp-hour vendor values`() {
        assertEquals(4500.0, DesignCapacityResolver.parseCapacityMah("4500")!!, 0.001)
    }

    @Test
    fun `rejects garbage and out-of-envelope values`() {
        assertNull(DesignCapacityResolver.parseCapacityMah(""))
        assertNull(DesignCapacityResolver.parseCapacityMah("n/a"))
        assertNull(DesignCapacityResolver.parseCapacityMah("999"))
        assertNull(DesignCapacityResolver.parseCapacityMah("17000000"))
    }

    @Test
    fun `manual beats system, system beats model table`() {
        assertEquals(DesignCapacityResolver.Source.MANUAL,
            DesignCapacityResolver.resolve(5000.0, 4400.0, 5500.0).source)
        assertEquals(4400.0, DesignCapacityResolver.resolve(null, 4400.0, 5500.0).capacityMah!!, 0.001)
        assertEquals(DesignCapacityResolver.Source.DEVICE_TABLE,
            DesignCapacityResolver.resolve(null, null, 5500.0).source)
        assertEquals(DesignCapacityResolver.Source.NONE,
            DesignCapacityResolver.resolve(null, null, null).source)
        assertEquals(DesignCapacityResolver.Source.NONE,
            DesignCapacityResolver.resolve(0.0, null, null).source)
    }

    @Test
    fun `model table resolves the team device`() {
        assertEquals(5000.0, DesignCapacityResolver.modelTableCapacity("23117RK66C")!!, 0.001)
        assertNull(DesignCapacityResolver.modelTableCapacity("unknown-model"))
        assertNull(DesignCapacityResolver.modelTableCapacity(null))
    }

    @Test
    fun `readFirst tolerates unreadable nodes`() {
        val value = DesignCapacityResolver.readFirst(listOf("/nonexistent/a", "/nonexistent/b")) { null }
        assertNull(value)
    }
}
