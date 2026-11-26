package com.iptv.glass.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iptv.glass.model.Channel
import com.iptv.glass.ui.components.ChannelTileGrid
import com.iptv.glass.ui.theme.GlassMuted
import com.iptv.glass.ui.theme.GlassText
import com.iptv.glass.viewmodel.MainViewModel

/**
 * Matches — honest by design. This app has no match-data feed, so it
 * does NOT invent scores, fixtures or kick-off times. It shows the
 * playlist's Sports-group channels as "Live Sports Channels": tap one
 * to watch it live. When the playlist has no Sports group, it says so.
 */
@Composable
fun MatchesScreen(
    viewModel: MainViewModel,
    onPlay: (Channel) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val sports = viewModel.sportsChannels()

    Column(Modifier.fillMaxSize()) {
        Text(
            "Matches",
            style = MaterialTheme.typography.headlineMedium,
            color = GlassText,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        Text(
            "Live Sports Channels",
            style = MaterialTheme.typography.titleMedium,
            color = GlassText,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Sports channels from your playlist. Tap a channel to watch it live — " +
                "no scores or fixtures are shown, because this app has no match-data feed.",
            color = GlassMuted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(12.dp))
        if (sports.isEmpty()) {
            Text(
                if (uiState.isLoading) "Loading…"
                else "No sports channels in the playlist yet.\n\nAdd entries with " +
                    "group-title=\"Sports\" to your M3U playlist on GitHub and refresh — " +
                    "they will appear here automatically.",
                color = GlassMuted,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            return@Column
        }
        ChannelTileGrid(
            channels = sports,
            onTileClick = { channel ->
                // onPlay (MainActivity) plays inline and goes to Channels.
                onPlay(channel)
            }
        )
    }
}
