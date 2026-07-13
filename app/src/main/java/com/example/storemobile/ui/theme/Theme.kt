package com.example.storemobile.ui.theme

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

private val JeskoDarkColorScheme = darkColorScheme(
    primary = Jesko.Gold,
    onPrimary = JeskoDarkPalette.bgDark,
    primaryContainer = Jesko.GoldDeep,
    onPrimaryContainer = JeskoDarkPalette.textPrimary,
    secondary = Jesko.Blue,
    onSecondary = JeskoDarkPalette.bgDark,
    tertiary = Jesko.Green,
    onTertiary = JeskoDarkPalette.bgDark,
    background = JeskoDarkPalette.bgDark,
    onBackground = JeskoDarkPalette.textPrimary,
    surface = JeskoDarkPalette.card,
    onSurface = JeskoDarkPalette.textPrimary,
    surfaceVariant = JeskoDarkPalette.input,
    onSurfaceVariant = JeskoDarkPalette.textSecondary,
    outline = JeskoDarkPalette.border,
    outlineVariant = JeskoDarkPalette.inputBorder,
    error = Jesko.Red,
    onError = JeskoDarkPalette.bgDark
)

private val JeskoLightColorScheme = lightColorScheme(
    primary = Jesko.GoldDeep,
    onPrimary = Color.White,
    primaryContainer = Jesko.Gold,
    onPrimaryContainer = JeskoLightPalette.textPrimary,
    secondary = Jesko.BlueDeep,
    onSecondary = Color.White,
    tertiary = Jesko.GreenDeep,
    onTertiary = Color.White,
    background = JeskoLightPalette.bgDark,
    onBackground = JeskoLightPalette.textPrimary,
    surface = JeskoLightPalette.card,
    onSurface = JeskoLightPalette.textPrimary,
    surfaceVariant = JeskoLightPalette.input,
    onSurfaceVariant = JeskoLightPalette.textSecondary,
    outline = JeskoLightPalette.border,
    outlineVariant = JeskoLightPalette.inputBorder,
    error = Jesko.RedDeep,
    onError = Color.White
)

@Composable
fun JeskoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Drive the global brand palette so every `Jesko.*` read reflects the
    // chosen theme (the object is state-backed → automatic recomposition).
    SideEffect { Jesko.applyDark(darkTheme) }

    val colorScheme = if (darkTheme) JeskoDarkColorScheme else JeskoLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = (if (darkTheme) JeskoDarkPalette.bgDark else JeskoLightPalette.bgPanel).toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            // Light bars (dark icons) when the app is in light mode.
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = JeskoTypography,
        shapes = JeskoShapes,
        content = content
    )
}
