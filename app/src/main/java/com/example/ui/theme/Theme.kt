package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = FadxPrimaryLight,
    onPrimary = Color(0xFF0D0B18),
    primaryContainer = FadxPrimaryDark,
    onPrimaryContainer = Color(0xFFEBE8FF),
    secondary = FadxSecondary,
    onSecondary = Color(0xFF003837),
    secondaryContainer = FadxSecondaryDark,
    onSecondaryContainer = Color(0xFFB8FAF8),
    tertiary = FadxAccentCoral,
    onTertiary = Color(0xFF490E0F),
    background = FadxDarkBg,
    onBackground = FadxDarkTextPrimary,
    surface = FadxDarkSurface,
    onSurface = FadxDarkTextPrimary,
    surfaceVariant = FadxDarkSurfaceVariant,
    onSurfaceVariant = FadxDarkTextSecondary,
    outline = FadxDarkBorder,
    outlineVariant = FadxDarkCard
)

private val LightColorScheme = lightColorScheme(
    primary = FadxPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDEAFE),
    onPrimaryContainer = Color(0xFF281E78),
    secondary = FadxSecondaryDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0FBFB),
    onSecondaryContainer = Color(0xFF004947),
    tertiary = FadxAccentCoral,
    onTertiary = Color.White,
    background = FadxLightBg,
    onBackground = FadxLightTextPrimary,
    surface = FadxLightSurface,
    onSurface = FadxLightTextPrimary,
    surfaceVariant = FadxLightSurfaceVariant,
    onSurfaceVariant = FadxLightTextSecondary,
    outline = FadxLightBorder,
    outlineVariant = Color(0xFFDCDFEA)
)

@Composable
fun FadxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our signature Fadx electric violet branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
