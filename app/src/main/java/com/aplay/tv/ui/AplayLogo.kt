package com.aplay.tv.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The APlay symbol: a glassy rounded square with a play triangle, in the app colour. */
@Composable
fun AplayMark(modifier: Modifier = Modifier) {
    val accent = Accent
    Canvas(modifier.aspectRatio(1f)) {
        val s = size.minDimension
        val corner = CornerRadius(s * 0.26f)
        drawRoundRect(
            Brush.linearGradient(
                listOf(accent, lerp(accent, Color.Black, 0.55f)),
                start = Offset(0f, 0f), end = Offset(s, s),
            ),
            cornerRadius = corner,
        )
        // The glass shine on the top half.
        drawRoundRect(
            Brush.verticalGradient(listOf(Color(0x66FFFFFF), Color.Transparent), endY = s * 0.6f),
            cornerRadius = corner,
        )
        drawRoundRect(Color(0x66FFFFFF), cornerRadius = corner, style = Stroke(width = s * 0.025f))
        val play = Path().apply {
            moveTo(s * 0.39f, s * 0.27f)
            lineTo(s * 0.75f, s * 0.5f)
            lineTo(s * 0.39f, s * 0.73f)
            close()
        }
        drawPath(play, Color.White)
    }
}

/** The symbol with the name next to it. Used in the top bar. */
@Composable
fun AplayLogo(height: Dp = 32.dp) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AplayMark(Modifier.size(height))
        Spacer(Modifier.width(8.dp))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = Accent)) { append("A") }
                withStyle(SpanStyle(color = Color.White)) { append("Play") }
            },
            fontSize = (height.value * 0.66f).sp,
            fontWeight = FontWeight.Black,
        )
    }
}
