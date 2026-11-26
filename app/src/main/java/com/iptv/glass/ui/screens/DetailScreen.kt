package com.iptv.glass.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iptv.glass.model.Channel
import com.iptv.glass.ui.components.ChannelLogo
import com.iptv.glass.ui.components.GlassCard
import com.iptv.glass.ui.theme.GlassMuted
import com.iptv.glass.ui.theme.GlassText
import com.iptv.glass.ui.theme.LiveRed
import com.iptv.glass.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Detail screen for a movie / series title or a live channel. */
@Composable
fun DetailScreen(
    channel: Channel,
    viewModel: MainViewModel,
    onPlay: (Channel) -> Unit,
    onBack: () -> Unit
) {
    val favorites by viewModel.favorites.collectAsState()
    val isFavorite = channel.id in favorites
    val now = viewModel.nowProgram(channel)
    val next = viewModel.nextProgram(channel)
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GlassText)
            }
            Text(
                channel.category,
                color = GlassMuted,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { viewModel.toggleFavorite(channel) }) {
                Icon(
                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favourite",
                    tint = if (isFavorite) LiveRed else GlassText
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        GlassCard {
            ChannelLogo(channel, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).padding(16.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(channel.name, style = MaterialTheme.typography.headlineMedium, color = GlassText, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            if (channel.isLive) "Live channel • HLS" else "Video on demand • ${channel.streamUrl.substringAfterLast('.', "").uppercase()}",
            color = GlassMuted
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onPlay(channel) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = com.iptv.glass.ui.theme.GlassAccent)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (channel.isLive) "Watch Live" else "Play")
        }
        Spacer(Modifier.height(16.dp))
        Text("TV Guide", style = MaterialTheme.typography.titleMedium, color = GlassText)
        Spacer(Modifier.height(8.dp))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    now?.let {
                        "Now (${timeFormat.format(Date(it.startMillis))} – ${timeFormat.format(Date(it.endMillis))}): ${it.title}"
                    } ?: "No EPG info",
                    color = GlassText
                )
                if (now?.description?.isNotBlank() == true) {
                    Spacer(Modifier.height(4.dp))
                    Text(now.description, color = GlassMuted, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    next?.let {
                        "Next (${timeFormat.format(Date(it.startMillis))}): ${it.title}"
                    } ?: "No upcoming programme info",
                    color = GlassMuted
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Stream: ${channel.streamUrl.substringBefore('?')}",
            color = GlassMuted,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
