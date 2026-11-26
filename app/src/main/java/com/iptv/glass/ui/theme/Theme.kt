package com.iptv.glass.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Glassy dark palette — deep navy/violet gradient, frosted white glass.
val GlassBackgroundTop = Color(0xFF0B1026)
val GlassBackgroundMid = Color(0xFF141B3D)
val GlassBackgroundBottom = Color(0xFF0A0A14)
val GlassAccent = Color(0xFF7C5CFF)
val GlassAccent2 = Color(0xFF00D4FF)
val GlassAccentPink = Color(0xFFFF5C8A)
val GlassText = Color(0xFFF4F6FF)
val GlassMuted = Color(0xFF9AA3C7)
val GlassWhite08 = Color.White.copy(alpha = 0.08f)
val GlassWhite12 = Color.White.copy(alpha = 0.12f)
val GlassWhite20 = Color.White.copy(alpha = 0.20f)
val GlassWhite40 = Color.White.copy(alpha = 0.40f)
val LiveRed = Color(0xFFFF3B5C)

val AppGradient: Brush
    get() = Brush.verticalGradient(
        listOf(GlassBackgroundTop, GlassBackgroundMid, GlassBackgroundBottom)
    )

val AccentGradient: Brush
    get() = Brush.horizontalGradient(listOf(GlassAccent, GlassAccent2))

private val DarkColors = darkColorScheme(
    primary = GlassAccent,
    secondary = GlassAccent2,
    tertiary = GlassAccentPink,
    background = GlassBackgroundBottom,
    surface = Color(0xFF171D38),
    onBackground = GlassText,
    onSurface = GlassText,
    error = LiveRed
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 28.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Default, fontSize = 12.sp, color = GlassMuted),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp)
)

@Composable
fun GlassIPTVTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = AppTypography,
        content = content
    )
}
