package com.aplay.tv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aplay.tv.AppViewModel

/** Shown on an Android TV until a phone sends the code. */
@Composable
fun TvActivateScreen(model: AppViewModel) {
    LaunchedEffect(Unit) { model.startTvPairing() }
    val code = model.tvCode.chunked(4).joinToString("  ")
    Column(
        Modifier.fillMaxSize().padding(48.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Activate APlay on this TV", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Accent)
        Text(code, fontSize = 64.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        Text(
            "On your phone open APlay, go to More > Activate TV and type this code.",
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CircularProgressIndicator(color = Accent)
        Text("Waiting for your phone...", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
