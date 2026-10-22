package com.aizeek.phonepulse.intervention

import java.io.File

/**
 * Resolves the device design capacity without asking the user to type it.
 * Priority: user-entered value > kernel charge_full_design > none.
 * Kernel nodes live under /sys/class/power_supply and are world-readable on the
 * vast majority of Qualcomm/MediaTek devices; missing or restricted nodes simply
 * yield null and the UI falls back to the manual entry.
 */
object DesignCapacityResolver {
    enum class Source { MANUAL, SYSTEM, DEVICE_TABLE, NONE }

    data class Resolution(val capacityMah: Double?, val source: Source)

    const val MIN_MAH = 1000.0
    const val MAX_MAH = 16_000.0

    /**
     * Nominal design capacity per verified device (entry competition devices only).
     * Android 16 locks power_supply sysfs away from apps, so the model table is the
     * practical automatic source on this hardware; the value is the official nominal
     * figure for the model, marked as such in the UI.
     */
    val KNOWN_MODELS: Map<String, Double> = mapOf(
        "23117RK66C" to 5000.0 // Redmi K70 Pro (manet), nominal 5000 mAh / 120W
    )

    fun modelTableCapacity(model: String?): Double? =
        model?.trim()?.let { KNOWN_MODELS[it] }

    val SYSFS_DESIGN_PATHS = listOf(
        "/sys/class/power_supply/battery/charge_full_design",
        "/sys/class/power_supply/bms/charge_full_design",
        "/sys/class/power_supply/battery/battery_charge_full_design"
    )

    val SYSFS_MEASURED_PATHS = listOf(
        "/sys/class/power_supply/battery/charge_full",
        "/sys/class/power_supply/bms/charge_full",
        "/sys/class/power_supply/battery/battery_charge_full"
    )

    /**
     * Kernel reports microamp-hours. Some vendors report milliamp-hours instead;
     * both ranges are accepted, anything outside the physical envelope is rejected.
     */
    fun parseCapacityMah(raw: String): Double? {
        val value = raw.trim().toLongOrNull() ?: return null
        return when (value) {
            in 1_000_000L..16_000_000L -> value / 1000.0
            in 1_000L..16_000L -> value.toDouble()
            else -> null
        }
    }

    fun resolve(manualMah: Double?, sysfsMah: Double?, modelMah: Double? = null): Resolution = when {
        manualMah != null && manualMah.isFinite() && manualMah in MIN_MAH..MAX_MAH ->
            Resolution(manualMah, Source.MANUAL)
        sysfsMah != null && sysfsMah.isFinite() && sysfsMah in MIN_MAH..MAX_MAH ->
            Resolution(sysfsMah, Source.SYSTEM)
        modelMah != null && modelMah.isFinite() && modelMah in MIN_MAH..MAX_MAH ->
            Resolution(modelMah, Source.DEVICE_TABLE)
        else -> Resolution(null, Source.NONE)
    }

    fun readFirst(paths: List<String>, reader: (String) -> String? = ::readNode): Double? =
        paths.firstNotNullOfOrNull { path ->
            reader(path)?.let { parseCapacityMah(it) }
        }

    data class SysfsCapacity(val designMah: Double?, val measuredMah: Double?)

    /** One-shot snapshot of both kernel figures; call from a background dispatcher. */
    fun readSnapshot(designPaths: List<String> = SYSFS_DESIGN_PATHS,
        measuredPaths: List<String> = SYSFS_MEASURED_PATHS,
        reader: (String) -> String? = ::readNode): SysfsCapacity =
        SysfsCapacity(readFirst(designPaths, reader), readFirst(measuredPaths, reader))

    private fun readNode(path: String): String? = try {
        File(path).bufferedReader().use { it.readText().trim() }.takeIf { it.isNotEmpty() }
    } catch (_: Exception) {
        null // Missing or SELinux-restricted node: not an error, just no system evidence.
    }
}
