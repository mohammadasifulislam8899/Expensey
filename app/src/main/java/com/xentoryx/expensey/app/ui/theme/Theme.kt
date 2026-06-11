package com.xentoryx.expensey.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─── Custom Dark Color Scheme (Crush Dark Fitness Theme) ──────────────────────
private val CrushDarkColorScheme = darkColorScheme(
    primary = CrushLavender,
    secondary = CrushYellow,
    tertiary = CrushPink,
    background = CrushBg,
    surface = CrushCardBg,
    onPrimary = CrushBg,
    onSecondary = CrushBg,
    onBackground = CrushTextPrimary,
    onSurface = CrushTextPrimary,
    surfaceVariant = CrushInputBg,
    outline = CrushBorder,
    onSurfaceVariant = CrushTextSecondary
)

// ─── Custom Light Color Scheme (Playful Light Theme) ──────────────────────────
private val PlayfulLightColorScheme = lightColorScheme(
    primary = PlayfulLavender,
    secondary = PlayfulYellow,
    tertiary = PlayfulPink,
    background = PlayfulBg,
    surface = PlayfulCardBg,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = PlayfulTextPrimary,
    onSurface = PlayfulTextPrimary,
    surfaceVariant = PlayfulInputBg,
    outline = PlayfulBorder,
    onSurfaceVariant = PlayfulTextSecondary
)

@Composable
fun ExpenseyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set dynamicColor to false by default to enforce our premium, high-fidelity design system
    // across all screens rather than having dynamic wallpaper colors override it!
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> CrushDarkColorScheme
        else -> PlayfulLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                // Enforce edge-to-edge transparent system bars
                WindowCompat.setDecorFitsSystemWindows(it, false)

                // Clear colors to full transparent so our background gradient/shapes show through
                it.statusBarColor = android.graphics.Color.TRANSPARENT
                it.navigationBarColor = android.graphics.Color.TRANSPARENT

                // Set system bar text/icon contrast correctly based on theme
                val controller = WindowCompat.getInsetsController(it, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Maps to the standard Typography of your project
        content = content
    )
}