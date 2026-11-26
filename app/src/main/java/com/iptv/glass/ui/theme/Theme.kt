package com.iptv.glass.ui.theme

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

// Nexa-style palette — dark navy/black background, cyan/teal accent,
// white text, muted grey-blue text. Dark glass cards on top of it.
// (Replaces the earlier Sunset Pink theme.)
val GlassBackgroundTop = Color(0xFF0A0E1A)
val GlassBackgroundMid = Color(0xFF0F1626)
val GlassBackgroundBottom = Color(0xFF070A12)
val GlassAccent = Color(0xFF00D4FF)
val GlassAccent2 = Color(0xFF00B8D4)
val GlassAccentPink = Color(0xFF00D4FF)
val GlassText = Color(0xFFFFFFFF)
val GlassMuted = Color(0xFF8A94A6)
val GlassWhite08 = Color.White.copy(alpha = 0.08f)
val GlassWhite12 = Color.White.copy(alpha = 0.12f)
val GlassWhite20 = Color.White.copy(alpha = 0.20f)
val GlassWhite40 = Color.White.copy(alpha = 0.40f)
val LiveRed = Color(0xFFFF3B5C)

/** Dark card surface used for tiles and rows, like the reference app. */
val NexaCard = Color(0xFF161F33)
val NexaCardBorder = Color(0xFF232F4A)
val NexaTileBackground = Color.White
val NexaBottomBar = Color(0xFF0F1626)
val NexaCyan = Color(0xFF00D4FF)
val NexaCyanDark = Color(0xFF00B8D4)
/** Pure-black top player area, exactly like the reference app's video area. */
val NexaPlayerBlack = Color(0xFF000000)
/** Category-chip background when not selected. */
val NexaChipBackground = Color(0xFF161F33)

/**
 * Monogram fallback palettes (ChannelLogoResolver.paletteIndex 0..3):
 * cyan/blue, pink/orange, teal/green, purple. Each pair is a gradient
 * drawn behind the channel initials when no logo image is available.
 */
val MonogramPalettes: List<Pair<Color, Color>> = listOf(
    Color(0xFF00D4FF) to Color(0xFF2563EB),
    Color(0xFFFF5C8A) to Color(0xFFFF9A3D),
    Color(0xFF00E5A0) to Color(0xFF059669),
    Color(0xFF8B5CF6) to Color(0xFF6366F1)
)

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
    surface = NexaCard,
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
