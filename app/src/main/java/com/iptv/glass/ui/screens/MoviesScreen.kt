package com.iptv.glass.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.iptv.glass.model.Channel
import com.iptv.glass.ui.components.ChannelTileGrid
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
        ChannelTileGrid(
            channels = movies,
            onTileClick = { channel -> onOpenDetail(channel) }
        )
    }
}
