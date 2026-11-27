package com.aplay.tv.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
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

/** Frosted-glass look: a see-through white fill with a thin light edge. */
val GlassFill = Color(0x1FFFFFFF)
val GlassFillStrong = Color(0x2EFFFFFF)
val GlassStroke = Color(0x33FFFFFF)

private val Colors = darkColorScheme(
    background = Color(0xFF0B0F1A),
    onBackground = Color(0xFFEEF1F8),
    surface = Color(0xFF0D1322),
    onSurface = Color(0xFFEEF1F8),
    surfaceVariant = Color(0xFF232A40),
    onSurfaceVariant = Color(0xFFA3ABBF),
    surfaceContainer = Color(0xFF171C2B),
    surfaceContainerHigh = Color(0xFF1C2236),
    surfaceContainerHighest = Color(0xFF232A40),
    outline = Color(0xFF3A4259),
    outlineVariant = Color(0x33FFFFFF),
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
    MaterialTheme(colorScheme = scheme) {
        // A deep blue background with a soft glow of the app colour, behind everything.
        Box(
            Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF0E1527), Color(0xFF070910))))
                .drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            listOf(accent.copy(alpha = 0.22f), Color.Transparent),
                            center = Offset(size.width * 0.9f, 0f),
                            radius = size.width * 0.9f,
                        )
                    )
                    drawRect(
                        Brush.radialGradient(
                            listOf(accent.copy(alpha = 0.10f), Color.Transparent),
                            center = Offset(0f, size.height * 0.85f),
                            radius = size.width,
                        )
                    )
                },
        ) {
            CompositionLocalProvider(LocalContentColor provides scheme.onBackground, content = content)
        }
    }
}
