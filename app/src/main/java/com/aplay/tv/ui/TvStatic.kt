package com.aplay.tv.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

/** An old TV with no signal: flickering snow, scan lines, a rolling bar and a glitching "NO SIGNAL". */
@Composable
fun TvStaticError(message: String, onRetry: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(70)
            frame++
        }
    }
    val infinite = rememberInfiniteTransition(label = "tv")
    val roll by infinite.animateFloat(
        0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart), label = "roll",
    )
    val blink by infinite.animateFloat(
        0.35f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "blink",
    )

    Box(modifier.background(Color(0xFF0A0A0A))) {
        Canvas(Modifier.fillMaxSize()) {
            val cols = 56
            val cell = size.width / cols
            val rows = (size.height / cell).toInt() + 1
            val rnd = Random(frame * 7919)
            for (y in 0 until rows) {
                for (x in 0 until cols) {
                    val v = rnd.nextFloat()
                    drawRect(
                        Color(v, v, v, 0.55f),
                        Offset(x * cell, y * cell),
                        Size(cell + 0.6f, cell + 0.6f),
                    )
                }
            }
            // Rolling bright bar
            val barY = size.height * roll
            drawRect(Color(0x33FFFFFF), Offset(0f, barY - size.height * 0.06f), Size(size.width, size.height * 0.12f))
            // Scan lines
            var y = 0f
            while (y < size.height) {
                drawRect(Color(0x55000000), Offset(0f, y), Size(size.width, 1.5f))
                y += 4f
            }
            // Random horizontal glitch tear
            if (frame % 9 < 2) {
                val gy = rnd.nextFloat() * size.height
                translate(left = (rnd.nextFloat() - 0.5f) * 40f) {
                    drawRect(Color(0x66FF2A4D), Offset(0f, gy), Size(size.width, 5f))
                    drawRect(Color(0x6622D3EE), Offset(0f, gy + 5f), Size(size.width, 3f))
                }
            }
            // Dark vignette on the edges
            drawRect(Color(0x66000000), Offset.Zero, Size(size.width, size.height * 0.04f))
            drawRect(Color(0x66000000), Offset(0f, size.height * 0.96f), Size(size.width, size.height * 0.04f))
        }
        val jitter = if (frame % 11 == 0) 3.dp else 0.dp
        Column(
            Modifier.align(Alignment.Center).padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    "NO SIGNAL", color = Color(0xFFFF2A4D), fontSize = 24.sp, fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace, letterSpacing = 3.sp,
                    modifier = Modifier.padding(start = jitter + 2.dp),
                )
                Text(
                    "NO SIGNAL", color = Color(0xFF22D3EE), fontSize = 24.sp, fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace, letterSpacing = 3.sp,
                    modifier = Modifier.padding(end = jitter + 2.dp),
                )
                Text(
                    "NO SIGNAL", color = Color.White.copy(alpha = blink), fontSize = 24.sp, fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace, letterSpacing = 3.sp,
                )
            }
            Text(
                message, color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 2,
                modifier = Modifier.background(Color(0x99000000)).padding(horizontal = 6.dp, vertical = 2.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onRetry) { Text("Try again") }
                TextButton(onClick = onBack) { Text("Back", color = Color.White) }
            }
        }
    }
}
