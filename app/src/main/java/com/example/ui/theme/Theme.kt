package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val PrecisionDarkColorScheme = darkColorScheme(
    primary = LaserCyan,
    onPrimary = ObsidianBg,
    primaryContainer = Color(0xFF003642),
    onPrimaryContainer = LaserCyan,
    secondary = PrecisionAmber,
    onSecondary = ObsidianBg,
    secondaryContainer = Color(0xFF3E2C00),
    onSecondaryContainer = PrecisionAmberLight,
    tertiary = LevelEmerald,
    onTertiary = ObsidianBg,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = SlateSurface,
    onSurface = TextPrimary,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = WarningCoral,
    outline = SlateCardBorder
)

@Composable
fun MyApplicationTheme(
    rtl: Boolean = true,
    content: @Composable () -> Unit
) {
    val direction = if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        MaterialTheme(
            colorScheme = PrecisionDarkColorScheme,
            typography = Typography,
            content = content
        )
    }
}
