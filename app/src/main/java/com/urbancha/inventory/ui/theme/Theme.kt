package com.urbancha.inventory.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val TeaGreen = Color(0xFF1F4D3A)
val TeaGreenSoft = Color(0xFFE3EEE7)
val Saffron = Color(0xFFE8A33D)
val SaffronSoft = Color(0xFFFBEBD0)
val SaffronText = Color(0xFF6B4000)
val Ground = Color(0xFFF6F5F1)
val Ink = Color(0xFF1A1D1B)
val Muted = Color(0xFF5B635E)
val Line = Color(0xFFE4E2DC)
val Danger = Color(0xFFB42318)
val DangerSoft = Color(0xFFFDECEA)

private val colors = lightColorScheme(
    primary = TeaGreen,
    onPrimary = Color.White,
    primaryContainer = TeaGreenSoft,
    onPrimaryContainer = TeaGreen,
    secondary = Saffron,
    onSecondary = Ink,
    secondaryContainer = SaffronSoft,
    onSecondaryContainer = SaffronText,
    background = Ground,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Ground,
    onSurfaceVariant = Muted,
    outline = Line,
    outlineVariant = Line,
    error = Danger,
    errorContainer = DangerSoft,
)

@Composable
fun UrbanChaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}
