package com.example.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val SportCanvas = Color(0xFF0B0B0E)
val SportSurfaceSecondary = Color(0xFF16161C)
val SportSurfaceTertiary = Color(0xFF22222B)
val SportOnSurface = Color(0xFFF5F5F7)
val SportOnSurfaceSecondary = Color(0xFFE5E5EA)
val SportMuted = Color(0xFF8E8E93)
val SportOrange = Color(0xFFFF6B1A)
val SportOnBrand = Color(0xFF0B0B0E)
val SportSuccess = Color(0xFF2ECC71)
val SportError = Color(0xFFFF453A)
val SportWarning = Color(0xFFFFB020)
val SportInfo = Color(0xFF0A84FF)
val SportBorder = Color(0xFF2A2A33)
val SportBorderStrong = Color(0xFF3A3A46)

private val DarkColorScheme = darkColorScheme(
    primary = SportOrange,
    onPrimary = SportOnBrand,
    secondary = Color(0xFF2A2A33),
    onSecondary = SportOnSurface,
    background = SportCanvas,
    onBackground = SportOnSurface,
    surface = SportCanvas,
    onSurface = SportOnSurface,
    surfaceVariant = SportSurfaceSecondary,
    onSurfaceVariant = SportOnSurfaceSecondary,
    error = SportError,
    onError = Color(0xFF2A0503)
)

@Composable
fun KaBasketTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
