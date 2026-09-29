package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val FizaDarkColorScheme = darkColorScheme(
    primary = NeonMagenta,
    onPrimary = TextPrimary,
    primaryContainer = CyberSurfaceVariant,
    onPrimaryContainer = NeonPink,
    secondary = NeonCyan,
    onSecondary = CyberBlack,
    secondaryContainer = CyberSurface,
    onSecondaryContainer = NeonCyanBright,
    tertiary = NeonPurple,
    onTertiary = TextPrimary,
    background = CyberBlack,
    onBackground = TextPrimary,
    surface = CyberDark,
    onSurface = TextPrimary,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = NeonMagenta.copy(alpha = 0.3f)
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FizaDarkColorScheme,
        typography = Typography,
        content = content
    )
}
