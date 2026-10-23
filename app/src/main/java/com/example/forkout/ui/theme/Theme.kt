package com.example.forkout.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ForkoutColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = SurfaceHigh,
    onPrimaryContainer = Primary,
    secondary = Primary,
    onSecondary = OnPrimary,
    secondaryContainer = SurfaceHigh,
    onSecondaryContainer = OnSurface,
    tertiary = Primary,
    onTertiary = OnPrimary,
    background = Background,
    onBackground = OnSurface,
    surface = Background,
    onSurface = OnSurface,
    surfaceVariant = SurfaceHigh,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceContainerLowest = Background,
    surfaceContainerLow = Surface,
    surfaceContainer = Surface,
    surfaceContainerHigh = SurfaceHigh,
    surfaceContainerHighest = SurfaceHigh,
    outline = OnSurfaceVariant,
    outlineVariant = SurfaceHigh,
    error = Error,
    onError = OnError,
)

private val ForkoutAmoledColorScheme = ForkoutColorScheme.copy(
    background = BackgroundAmoled,
    surface = BackgroundAmoled,
    surfaceContainerLowest = BackgroundAmoled,
)

/**
 * Forkout is dark-only. [amoled] switches the background to true black.
 */
@Composable
fun ForkoutTheme(
    amoled: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (amoled) ForkoutAmoledColorScheme else ForkoutColorScheme,
        typography = Typography,
        content = content
    )
}
