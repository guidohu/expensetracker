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

// Hand-tuned green/teal tonal palette (the roles below cover every surface Material3 actually
// draws with — Card, Scaffold, chips, containers — not just primary/secondary; leaving the rest
// on lightColorScheme()'s defaults produces a mismatched purple baseline peeking through).
private val LightColors = lightColorScheme(
    primary = Color(0xFF2E6B34),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB3F1AE),
    onPrimaryContainer = Color(0xFF002204),
    secondary = Color(0xFF386A60),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFBCECDE),
    onSecondaryContainer = Color(0xFF00201A),
    tertiary = Color(0xFF3D6373),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC1E8FB),
    onTertiaryContainer = Color(0xFF001F29),
    background = Color(0xFFF7FBF1),
    onBackground = Color(0xFF181D17),
    surface = Color(0xFFF7FBF1),
    onSurface = Color(0xFF181D17),
    surfaceVariant = Color(0xFFDEE5D8),
    onSurfaceVariant = Color(0xFF424940),
    outline = Color(0xFF72796F),
    outlineVariant = Color(0xFFC2C9BC),
    inverseSurface = Color(0xFF2D322C),
    inverseOnSurface = Color(0xFFEEF2E7),
    inversePrimary = Color(0xFF98D593),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F5EB),
    surfaceContainer = Color(0xFFEBEFE5),
    surfaceContainerHigh = Color(0xFFE5E9E0),
    surfaceContainerHighest = Color(0xFFDFE4DA),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF98D593),
    onPrimary = Color(0xFF00390A),
    primaryContainer = Color(0xFF0C5017),
    onPrimaryContainer = Color(0xFFB3F1AE),
    secondary = Color(0xFFA0D0C1),
    onSecondary = Color(0xFF03372C),
    secondaryContainer = Color(0xFF1E4F43),
    onSecondaryContainer = Color(0xFFBCECDE),
    tertiary = Color(0xFFA5CCDF),
    onTertiary = Color(0xFF073542),
    tertiaryContainer = Color(0xFF244C5A),
    onTertiaryContainer = Color(0xFFC1E8FB),
    background = Color(0xFF10140F),
    onBackground = Color(0xFFDFE4DA),
    surface = Color(0xFF10140F),
    onSurface = Color(0xFFDFE4DA),
    surfaceVariant = Color(0xFF424940),
    onSurfaceVariant = Color(0xFFC2C9BC),
    outline = Color(0xFF8C9388),
    outlineVariant = Color(0xFF424940),
    inverseSurface = Color(0xFFDFE4DA),
    inverseOnSurface = Color(0xFF2D322C),
    inversePrimary = Color(0xFF2E6B34),
    surfaceContainerLowest = Color(0xFF0B0F0A),
    surfaceContainerLow = Color(0xFF181D17),
    surfaceContainer = Color(0xFF1C211B),
    surfaceContainerHigh = Color(0xFF262B25),
    surfaceContainerHighest = Color(0xFF313630),
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
