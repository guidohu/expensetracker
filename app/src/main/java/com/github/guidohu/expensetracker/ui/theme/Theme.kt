package com.github.guidohu.expensetracker.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Hand-tuned turquoise/teal tonal palette (the roles below cover every surface Material3 actually
// draws with — Card, Scaffold, chips, containers — not just primary/secondary; leaving the rest
// on lightColorScheme()'s defaults produces a mismatched purple baseline peeking through).
private val LightColors = lightColorScheme(
    primary = Color(0xFF006971),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF8CF1FB),
    onPrimaryContainer = Color(0xFF002023),
    secondary = Color(0xFF386A60),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFBCECDE),
    onSecondaryContainer = Color(0xFF00201A),
    tertiary = Color(0xFF3D6373),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC1E8FB),
    onTertiaryContainer = Color(0xFF001F29),
    background = Color(0xFFE6F0F1),
    onBackground = Color(0xFF161D1E),
    surface = Color(0xFFE6F0F1),
    onSurface = Color(0xFF161D1E),
    surfaceVariant = Color(0xFFDAE5E6),
    onSurfaceVariant = Color(0xFF3F494A),
    outline = Color(0xFF6F797A),
    outlineVariant = Color(0xFFBEC9CA),
    inverseSurface = Color(0xFF2C3233),
    inverseOnSurface = Color(0xFFEBF2F3),
    inversePrimary = Color(0xFF6DD5E0),
    // Deliberately flat white for the top container tiers (rather than M3's usual tonal ramp) so
    // cards read as crisp, raised white surfaces against the tinted page background — the contrast
    // was missing when this inherited a tint too close to `background`.
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F8F8),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFFFFFFF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6DD5E0),
    onPrimary = Color(0xFF00363B),
    primaryContainer = Color(0xFF004D53),
    onPrimaryContainer = Color(0xFF8CF1FB),
    secondary = Color(0xFFA0D0C1),
    onSecondary = Color(0xFF03372C),
    secondaryContainer = Color(0xFF1E4F43),
    onSecondaryContainer = Color(0xFFBCECDE),
    tertiary = Color(0xFFA5CCDF),
    onTertiary = Color(0xFF073542),
    tertiaryContainer = Color(0xFF244C5A),
    onTertiaryContainer = Color(0xFFC1E8FB),
    background = Color(0xFF0E1415),
    onBackground = Color(0xFFDCE4E5),
    surface = Color(0xFF0E1415),
    onSurface = Color(0xFFDCE4E5),
    surfaceVariant = Color(0xFF3F494A),
    onSurfaceVariant = Color(0xFFBEC9CA),
    outline = Color(0xFF899394),
    outlineVariant = Color(0xFF3F494A),
    inverseSurface = Color(0xFFDCE4E5),
    inverseOnSurface = Color(0xFF2C3233),
    inversePrimary = Color(0xFF006971),
    surfaceContainerLowest = Color(0xFF090F10),
    surfaceContainerLow = Color(0xFF161D1E),
    surfaceContainer = Color(0xFF1A2122),
    surfaceContainerHigh = Color(0xFF242B2B),
    surfaceContainerHighest = Color(0xFF2F3637),
)

@Composable
fun ExpenseTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            view.context.findActivity()?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}

/** Unwraps ContextWrapper layers to find the hosting Activity, or null outside one (e.g. previews). */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
