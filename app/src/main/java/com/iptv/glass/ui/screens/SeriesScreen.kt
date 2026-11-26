package com.iptv.glass.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iptv.glass.model.Channel
import com.iptv.glass.ui.components.ChannelLogo
import com.iptv.glass.ui.components.GlassCard
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
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(series, key = { it.id }) { channel ->
                GlassCard(onClick = { onOpenDetail(channel) }) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        ChannelLogo(channel, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            channel.name,
                            color = GlassText,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text("Series", color = GlassMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
