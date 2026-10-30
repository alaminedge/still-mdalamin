package com.still.app

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF7DD3A8), onPrimary = Color(0xFF06150E),
    primaryContainer = Color(0xFF1B2B24), onPrimaryContainer = Color(0xFFBFEBD6),
    secondaryContainer = Color(0xFF1F2A25), onSecondaryContainer = Color(0xFFE6E6E6),
    background = Color(0xFF0B0B0C), onBackground = Color(0xFFECECEC),
    surface = Color(0xFF0B0B0C), onSurface = Color(0xFFECECEC),
    surfaceVariant = Color(0xFF1A1A1D), onSurfaceVariant = Color(0xFF9A9AA2),
    surfaceContainer = Color(0xFF111113), surfaceContainerHigh = Color(0xFF18181B),
    surfaceContainerHighest = Color(0xFF222226),
    outline = Color(0xFF3A3A40), outlineVariant = Color(0xFF26262B),
    error = Color(0xFFFF8A80), onError = Color(0xFF2B0A07)
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF2F7A5A), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9EEE3), onPrimaryContainer = Color(0xFF0F2E21),
    secondaryContainer = Color(0xFFE3EDE7), onSecondaryContainer = Color(0xFF111111),
    background = Color(0xFFF6F5F2), onBackground = Color(0xFF111111),
    surface = Color(0xFFF6F5F2), onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFECEAE5), onSurfaceVariant = Color(0xFF6B6B70),
    surfaceContainer = Color(0xFFFFFFFF), surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFF0EEE9),
    outline = Color(0xFFBDBBB5), outlineVariant = Color(0xFFE2E0DA),
    error = Color(0xFFB3261E), onError = Color(0xFFFFFFFF)
)

private val StillType = Typography().copy(
    displayLarge = TextStyle(fontSize = 64.sp, fontWeight = FontWeight.Light, letterSpacing = (-2).sp),
    displayMedium = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Light, letterSpacing = (-1).sp),
    headlineLarge = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.SemiBold),
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
)

@Composable
fun StillTheme(content: @Composable () -> Unit) {
    val dark = when (Store.themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val view = LocalView.current
    SideEffect {
        val w = (view.context as? Activity)?.window
        if (w != null) {
            val ctl = WindowCompat.getInsetsController(w, view)
            ctl.isAppearanceLightStatusBars = !dark
            ctl.isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        typography = StillType,
        content = content
    )
}
