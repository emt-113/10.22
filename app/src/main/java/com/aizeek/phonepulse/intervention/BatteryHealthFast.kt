package com.aizeek.phonepulse.intervention

import com.aizeek.phonepulse.battery.BatteryCycle
import com.aizeek.phonepulse.battery.BatteryHealthEstimate
import kotlin.math.abs

/**
 * Fast health evidence for sessions the strict estimator rejects.
 * Read-only over persisted/active cycles; never mutates battery/ module state.
 */
object FastHealthEstimator {
    const val MIN_SPAN = 8
    const val MIN_COVERAGE = 0.7
    private const val MAX_SAMPLES = 30

    data class FastSample(val capacityMah: Double, val span: Int, val weight: Double)

    data class FastEstimate(val capacityMah: Double, val sampleCount: Int, val maxSpan: Int)

    /** Same reasons the strict estimator tolerates, reused verbatim so no history is reclassified. */
    private fun toleratedRejection(reason: String?): Boolean =
        reason == null || reason == "SMALL_SOC_CHANGE" || reason == "INCOMPLETE_RESTORE"

    fun sample(cycle: BatteryCycle, calibrationRevision: Int): FastSample? {
        if (!cycle.charging || cycle.excluded || cycle.calibrationRevision != calibrationRevision) return null
        if (cycle.startPct !in 0..100 || cycle.endPct !in 0..100) return null
        val span = cycle.endPct - cycle.startPct
        if (span < MIN_SPAN) return null
        if (!toleratedRejection(cycle.rejectionReason)) return null
        if (cycle.socDiscontinuity) return null
        if (!cycle.netMah.isFinite() || cycle.netMah <= 0) return null
        val duration = cycle.measuredMs.toDouble() + cycle.missingMs
        if (duration <= 0) return null
        val coverage = cycle.measuredMs / duration
        if (coverage < MIN_COVERAGE) return null
        val capacity = cycle.netMah * 100.0 / span
        if (!capacity.isFinite() || capacity <= 0) return null
        // Quadratic in span like the strict engine: small spans must not dominate.
        return FastSample(capacity, span, span.toDouble() * span * coverage)
    }

    fun estimate(cycles: List<BatteryCycle>, calibrationRevision: Int): FastEstimate? {
        val samples = cycles.asSequence()
            .mapNotNull { sample(it, calibrationRevision) }
            .take(MAX_SAMPLES)
            .toList()
        if (samples.isEmpty()) return null
        val sorted = samples.map { it.capacityMah }.sorted()
        val median = if (sorted.size % 2 == 1) sorted[sorted.size / 2]
        else sorted[sorted.size / 2 - 1] / 2 + sorted[sorted.size / 2] / 2
        if (!median.isFinite() || median <= 0) return null
        return FastEstimate(median, samples.size, samples.maxOf { it.span })
    }
}

/**
 * Single display entry for the health screen. Priority:
 * 1. FORMAL — strict estimator accepted real sessions.
 * 2. FAST(system charge_full) — the kernel's own measured full-charge figure.
 * 3. FAST(sampled) — relaxed median over short charging sessions.
 * healthPct always divides by the resolved design capacity (manual first, then sysfs).
 */
object BatteryHealthFacade {
    enum class Source { FORMAL, FAST, NONE }

    data class DisplayHealth(
        val capacityMah: Double? = null,
        val healthPct: Double? = null,
        val source: Source = Source.NONE,
        val formalAcceptedCount: Int = 0,
        val fastSampleCount: Int = 0,
        val maxFastSpan: Int = 0,
        /** Estimate falls outside the physical envelope; shown as-is but flagged. */
        val anomalous: Boolean = false
    )

    /** Physical-envelope bounds used only for flagging, never for hiding. */
    const val PLAUSIBLE_MIN_PCT = 45.0
    const val PLAUSIBLE_MAX_PCT = 140.0

    fun display(
        formal: BatteryHealthEstimate,
        cycles: List<BatteryCycle>,
        designCapacityMah: Double?,
        systemMeasuredMah: Double?,
        calibrationRevision: Int
    ): DisplayHealth {
        val design = designCapacityMah?.takeIf { it.isFinite() && it > 0 }
        // Never hide: an implausible ratio is flagged (sensor fault or wrong design
        // capacity) so the user sees it and can fix inputs, but a genuinely degraded
        // battery below the envelope is never silently suppressed.
        val healthPct: (Double) -> Double? = { capacity ->
            design?.let { capacity / it * 100.0 }?.takeIf { it.isFinite() }
        }
        val anomaly: (Double?) -> Boolean = { pct ->
            pct != null && (pct < PLAUSIBLE_MIN_PCT || pct > PLAUSIBLE_MAX_PCT)
        }
        formal.capacityMah?.takeIf { it.isFinite() && it > 0 }?.let {
            val pct = healthPct(it)
            return DisplayHealth(
                capacityMah = it, healthPct = pct, source = Source.FORMAL,
                formalAcceptedCount = formal.acceptedCount, anomalous = anomaly(pct)
            )
        }
        systemMeasuredMah?.takeIf { it.isFinite() && it > 0 }?.let {
            val pct = healthPct(it)
            return DisplayHealth(
                capacityMah = it, healthPct = pct, source = Source.FAST,
                fastSampleCount = 1, anomalous = anomaly(pct)
            )
        }
        FastHealthEstimator.estimate(cycles, calibrationRevision)?.let { fast ->
            val pct = healthPct(fast.capacityMah)
            return DisplayHealth(
                capacityMah = fast.capacityMah, healthPct = pct,
                source = Source.FAST, fastSampleCount = fast.sampleCount,
                maxFastSpan = fast.maxSpan, anomalous = anomaly(pct)
            )
        }
        return DisplayHealth()
    }
}
