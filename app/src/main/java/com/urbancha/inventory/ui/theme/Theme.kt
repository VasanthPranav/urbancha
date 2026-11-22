package com.urbancha.inventory.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Every custom colour, in a light and a dark version. Must match ios/Sources/UrbanCha/Theme.swift. */
@Immutable
data class Palette(
    val teaGreen: Color,
    val teaGreenSoft: Color,
    val saffron: Color,
    val saffronSoft: Color,
    val saffronText: Color,
    val ground: Color,
    val surface: Color,
    val chip: Color,
    val ink: Color,
    val text: Color,
    val muted: Color,
    val line: Color,
    val danger: Color,
    val dangerSoft: Color,
)

val LightPalette = Palette(
    teaGreen = Color(0xFF1F4D3A),
    teaGreenSoft = Color(0xFFE3EEE7),
    saffron = Color(0xFFE8A33D),
    saffronSoft = Color(0xFFFBEBD0),
    saffronText = Color(0xFF6B4000),
    ground = Color(0xFFF6F5F1),
    surface = Color(0xFFFFFFFF),
    chip = Color(0xFFEDEDED),
    ink = Color(0xFF1A1D1B),
    text = Color(0xFF1A1D1B),
    muted = Color(0xFF5B635E),
    line = Color(0xFFE4E2DC),
    danger = Color(0xFFB42318),
    dangerSoft = Color(0xFFFDECEA),
)

val DarkPalette = Palette(
    teaGreen = Color(0xFF3F9269),
    teaGreenSoft = Color(0xFF15241C),
    saffron = Color(0xFFE8A33D),
    saffronSoft = Color(0xFF3A2C14),
    saffronText = Color(0xFFF0B95E),
    ground = Color(0xFF121614),
    surface = Color(0xFF1C211E),
    chip = Color(0xFF2C322F),
    ink = Color(0xFF1A1D1B), // text on saffron stays dark in both modes
    text = Color(0xFFE8EBE9),
    muted = Color(0xFF9AA39D),
    line = Color(0xFF2C322F),
    danger = Color(0xFFF27064),
    dangerSoft = Color(0xFF3A1A17),
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

// Read like plain constants in composable code; they follow the Light / Dark setting.
val TeaGreen: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.teaGreen
val TeaGreenSoft: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.teaGreenSoft
val Saffron: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.saffron
val SaffronSoft: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.saffronSoft
val SaffronText: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.saffronText
val Ground: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.ground
/** Panels, rows and bars (white in Light mode). */
val SurfaceBg: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.surface
/** Unselected pills. */
val ChipBg: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.chip
/** Text on saffron, e.g. "New sale". Stays dark in Dark mode. */
val Ink: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.ink
val Muted: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.muted
val Line: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.line
val Danger: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.danger
val DangerSoft: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.dangerSoft

enum class ThemeMode(val label: String) { System("System"), Light("Light"), Dark("Dark") }

@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}

private fun scheme(p: Palette, dark: Boolean) = (if (dark) darkColorScheme() else lightColorScheme()).copy(
    primary = p.teaGreen,
    onPrimary = Color.White,
    primaryContainer = p.teaGreenSoft,
    onPrimaryContainer = p.teaGreen,
    secondary = p.saffron,
    onSecondary = p.ink,
    secondaryContainer = p.saffronSoft,
    onSecondaryContainer = p.saffronText,
    background = p.ground,
    onBackground = p.text,
    surface = p.surface,
    onSurface = p.text,
    surfaceVariant = p.ground,
    onSurfaceVariant = p.muted,
    surfaceContainer = p.surface,
    surfaceContainerHigh = p.surface,
    surfaceContainerHighest = p.surface,
    surfaceContainerLow = p.surface,
    outline = p.line,
    outlineVariant = p.line,
    error = p.danger,
    errorContainer = p.dangerSoft,
)

@Composable
fun UrbanChaTheme(dark: Boolean = false, content: @Composable () -> Unit) {
    val palette = if (dark) DarkPalette else LightPalette
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = scheme(palette, dark), content = content)
    }
}
