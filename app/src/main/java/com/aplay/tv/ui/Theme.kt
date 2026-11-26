package com.aplay.tv.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/** The colour the user picked in More > App color. Reading it in a composable keeps it live. */
private val accentState = mutableStateOf(Color(0xFF22D3EE))

val Accent: Color get() = accentState.value

fun setAccent(color: Color) {
    accentState.value = color
}

val LiveRed = Color(0xFFFF4D4F)

/** Colours offered in More > App color. */
val AccentChoices = listOf(
    "Cyan" to Color(0xFF22D3EE),
    "Amber" to Color(0xFFFFB020),
    "Red" to Color(0xFFFF4D4F),
    "Green" to Color(0xFF34D399),
    "Purple" to Color(0xFFA78BFA),
    "Blue" to Color(0xFF3B82F6),
    "Pink" to Color(0xFFF472B6),
    "Orange" to Color(0xFFFB923C),
)

private val Colors = darkColorScheme(
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
    val accent = Accent
    val scheme = remember(accent) {
        val container = lerp(Color.Black, accent, 0.35f)
        val onContainer = lerp(accent, Color.White, 0.6f)
        Colors.copy(
            primary = accent,
            onPrimary = if (accent.luminance() > 0.4f) Color(0xFF14110A) else Color.White,
            primaryContainer = container,
            onPrimaryContainer = onContainer,
            secondaryContainer = container,
            onSecondaryContainer = onContainer,
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
