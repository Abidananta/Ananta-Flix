package com.aplay.tv.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Accent = Color(0xFFFFB020)
val LiveRed = Color(0xFFFF4D4F)

private val Colors = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF1A1200),
    primaryContainer = Color(0xFF4A3500),
    onPrimaryContainer = Color(0xFFFFE0A3),
    secondaryContainer = Color(0xFF4A3500),
    onSecondaryContainer = Color(0xFFFFE0A3),
    background = Color(0xFF121110),
    onBackground = Color(0xFFF2EEE8),
    surface = Color(0xFF121110),
    onSurface = Color(0xFFF2EEE8),
    surfaceVariant = Color(0xFF262320),
    onSurfaceVariant = Color(0xFFA39E96),
    surfaceContainer = Color(0xFF1D1B19),
    surfaceContainerHigh = Color(0xFF262320),
    surfaceContainerHighest = Color(0xFF302C28),
    outline = Color(0xFF4A453F),
)

@Composable
fun APlayTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
