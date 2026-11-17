package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NovaCalcDarkColorScheme = darkColorScheme(
    primary = ElectricBlueBright,
    onPrimary = TextPrimary,
    primaryContainer = DeepCobalt,
    onPrimaryContainer = IceBlue,
    secondary = CyanGlow,
    onSecondary = ObsidianBlack,
    secondaryContainer = MidnightBlue,
    onSecondaryContainer = CyanGlow,
    tertiary = IceBlue,
    onTertiary = ObsidianBlack,
    background = ObsidianBlack,
    onBackground = TextPrimary,
    surface = ObsidianDeep,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianElevated,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorderBright,
    outlineVariant = GlassBorderSubtle,
    error = ErrorRose,
    onError = ObsidianBlack,
    errorContainer = ErrorRoseContainer,
    onErrorContainer = ErrorRose
)

@Composable
fun NovaCalcTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NovaCalcDarkColorScheme,
        typography = NovaCalcTypography,
        content = content
    )
}
