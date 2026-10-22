package com.aizeek.phonepulse.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = BgDark,
    primaryContainer = NeonCyanContainer,
    onPrimaryContainer = NeonCyanLight,
    secondary = ElectricViolet,
    onSecondary = BgDark,
    secondaryContainer = ElectricVioletContainer,
    onSecondaryContainer = ElectricVioletLight,
    tertiary = NeonEmerald,
    onTertiary = BgDark,
    background = BgDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevatedDark,
    onSurfaceVariant = TextSecondary,
    outline = BorderDark,
    error = CoralRose,
    onError = TextPrimary
)

/** Bevel pearl light scheme — the single visual language of the whole app. */
private val LightColorScheme = lightColorScheme(
    primary = NeonCyan,
    onPrimary = Color.White,
    primaryContainer = NeonCyanContainer,
    onPrimaryContainer = Color(0xFF14567A),
    secondary = ElectricViolet,
    onSecondary = Color.White,
    secondaryContainer = ElectricVioletContainer,
    onSecondaryContainer = Color(0xFF4B3C8F),
    tertiary = NeonEmerald,
    onTertiary = Color.White,
    background = BgDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevatedDark,
    onSurfaceVariant = TextSecondary,
    outline = BorderDark,
    error = CoralRose,
    onError = Color.White
)

@Composable
fun PhonePulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = BgDark.toArgb()
                window.navigationBarColor = BgDark.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
