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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
 * Movies: channels whose group-title is "Movies" (live movie channels in
 * the supplied playlist) plus any VOD movie files. Tap opens the detail
 * screen; Play starts the stream.
 */
@Composable
fun MoviesScreen(
    viewModel: MainViewModel,
    onOpenDetail: (Channel) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val movies = viewModel.movieChannels()
    Column(Modifier.fillMaxSize()) {
        Text(
            "Movies",
            style = MaterialTheme.typography.headlineMedium,
            color = GlassText,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        if (movies.isEmpty()) {
            Text(
                if (uiState.isLoading) "Loading…" else "No movies yet — add a group-title=\"Movies\" group to your playlist.",
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
            items(movies, key = { it.id }) { channel ->
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
                        Text(
                            if (channel.isLive) "Live movie channel" else "Movie • VOD",
                            color = GlassMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
