package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = StudioPurplePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4C1D95),
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary = SecondaryCyanLight,
    onSecondary = Color(0xFF082F49),
    secondaryContainer = Color(0xFF164E63),
    onSecondaryContainer = Color(0xFFCFFAFE),
    tertiary = AccentAmber,
    onTertiary = Color.Black,
    background = StudioDarkBg,
    onBackground = Color(0xFFF8FAFC),
    surface = StudioCardBg,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = StudioCardHover,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = StudioBorder,
    error = AccentRose,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = StudioPurplePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF4C1D95),
    secondary = SecondaryCyan,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0369A1),
    tertiary = AccentAmber,
    onTertiary = Color.Black,
    background = StudioLilacBg,
    onBackground = Color(0xFF1E1B4B),
    surface = StudioLilacSurface,
    onSurface = Color(0xFF1E1B4B),
    surfaceVariant = Color(0xFFF5F3FF),
    onSurfaceVariant = Color(0xFF6B7280),
    outline = StudioLilacBorder,
    error = AccentRose,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent cinematic look
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
