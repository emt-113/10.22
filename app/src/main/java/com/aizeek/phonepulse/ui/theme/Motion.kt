package com.aizeek.phonepulse.ui.theme

import androidx.compose.animation.core.CubicBezierEasing

/**
 * House motion curve: fast out, long settle, no overshoot. Shared by every
 * programmatic transition (sheets, reveals) so the whole app answers with one
 * voice instead of a mix of default springs.
 */
val BevelEaseOut = CubicBezierEasing(0.32f, 0.86f, 0.28f, 1f)

/** The exit twin of [BevelEaseOut]: accelerate away, never linger. */
val BevelEaseIn = CubicBezierEasing(0.55f, 0.06f, 0.68f, 0.19f)
