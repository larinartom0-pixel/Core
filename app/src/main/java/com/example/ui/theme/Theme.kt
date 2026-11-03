package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CoreCyan,
    onPrimary = IndustrialDark,
    primaryContainer = IndustrialCard,
    onPrimaryContainer = CoreCyan,
    secondary = CopperAmber,
    onSecondary = IndustrialDark,
    secondaryContainer = IndustrialSurface,
    onSecondaryContainer = CopperAmber,
    background = IndustrialDark,
    onBackground = SlateLight,
    surface = IndustrialSurface,
    onSurface = SlateLight,
    surfaceVariant = IndustrialCard,
    onSurfaceVariant = SlateDim,
    outline = IndustrialBorder,
    error = AlertRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
