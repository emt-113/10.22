package com.aizeek.phonepulse.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Legacy token set — re-pointed to the Bevel pearl palette (2026-10-20).
 *
 * The names are kept unchanged on purpose: the whole UI (25+ files) reads
 * these tokens, so re-pointing the values migrates every screen to the light
 * shell in one move without touching a single call site. The old neon/dark
 * values only survive where a screen still needs real contrast.
 */

// Page / surface / border — warm white shell.
val BgDark = Color(0xFFFBFAF6)
val SurfaceDark = Color(0xFFFFFFFF)
val SurfaceElevatedDark = Color(0xFFFCFBF7)
val SurfaceHighlightDark = Color(0xFFF3F1EA)
val BorderDark = Color(0xFFEAE8E0)

// Accents — mint primary, ice blue secondary, soft violet support.
val NeonCyan = Color(0xFF3FA6DD)
val NeonCyanLight = Color(0xFF8FCBEB)
val NeonCyanContainer = Color(0xFFE3F0F9)

val ElectricViolet = Color(0xFF9C8FE0)
val ElectricVioletLight = Color(0xFFC0B6EE)
val ElectricVioletContainer = Color(0xFFEFEAFB)

val NeonEmerald = Color(0xFF2EC5A2)
val NeonEmeraldContainer = Color(0xFFDCF5EE)

val AmberWarning = Color(0xFFE8A13C)
val CoralRose = Color(0xFFE0705F)

// Text — dark text on the light shell.
val TextPrimary = Color(0xFF242C33)
val TextSecondary = Color(0xFF5F6B76)
val TextTertiary = Color(0xFF66707A)
