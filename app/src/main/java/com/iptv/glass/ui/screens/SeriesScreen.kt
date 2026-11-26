package com.iptv.glass.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.iptv.glass.model.Channel
import com.iptv.glass.ui.components.ChannelTileGrid
import com.iptv.glass.ui.theme.GlassMuted
import com.iptv.glass.ui.theme.GlassText
import com.iptv.glass.viewmodel.MainViewModel

/**
 * Series: channels whose group-title is "Series".
 * The supplied playlist has no Series group yet, so this screen shows
 * an empty state until the playlist gains one — it is not an error.
 */
@Composable
fun SeriesScreen(
    viewModel: MainViewModel,
    onOpenDetail: (Channel) -> Unit
) {
    val series = viewModel.seriesChannels()
    Column(Modifier.fillMaxSize()) {
        Text(
            "Series",
            style = MaterialTheme.typography.headlineMedium,
            color = GlassText,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        if (series.isEmpty()) {
            Text(
                "No series yet.\n\nAdd entries with group-title=\"Series\" to your M3U playlist on GitHub and pull to refresh — they will appear here automatically.",
                color = GlassMuted,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            return@Column
        }
        ChannelTileGrid(
            channels = series,
            onTileClick = { channel -> onOpenDetail(channel) }
        )
    }
}
