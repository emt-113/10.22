package com.aizeek.phonepulse.ui.theme

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import kotlin.math.roundToInt

/**
 * "阅读字号" preference: a single reading text size (12–20sp) the user dials
 * on the settings ruler; the history timeline body text follows it live.
 * Compose-state backed, persisted in its own SharedPreferences file — no
 * database impact. [load] is idempotent and cheap, so any entry surface may
 * call it.
 */
object ReadingFontStore {

    const val MIN_SP = 12
    const val MAX_SP = 20
    const val DEFAULT_SP = 14

    private const val PREFS = "reading_font"
    private const val KEY_SP = "reading_sp"

    private val state = mutableStateOf(DEFAULT_SP)
    private var loaded = false

    /** Current committed size in sp (rounded). Read from any composable. */
    val fontSp: Int get() = state.value

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_SP, DEFAULT_SP)
        state.value = sp.coerceIn(MIN_SP, MAX_SP)
    }

    /** Live update while dragging; persisted only when [persist] is true (snap). */
    fun set(sp: Int, persist: Boolean, context: Context? = null) {
        val clamped = sp.coerceIn(MIN_SP, MAX_SP)
        state.value = clamped
        if (persist && context != null) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putInt(KEY_SP, clamped).apply()
        }
    }

    /** Snap helper for the dial: nearest tick, clamped to the ruler range. */
    fun snap(value: Float): Int =
        value.roundToInt().coerceIn(MIN_SP, MAX_SP)
}
