package com.iptv.glass.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iptv.glass.model.Channel
import com.iptv.glass.ui.components.ChannelTileGrid
import com.iptv.glass.ui.theme.GlassMuted
import com.iptv.glass.ui.theme.GlassText
import com.iptv.glass.viewmodel.MainViewModel

/** Favourites in the same 3-column tile style, persisted with DataStore. */
@Composable
fun FavoritesScreen(
    viewModel: MainViewModel,
    onPlay: (Channel) -> Unit
) {
    val favChannels = viewModel.favoriteChannels()
    Column(Modifier.fillMaxSize()) {
        Text(
            "Favorites",
            style = MaterialTheme.typography.headlineMedium,
            color = GlassText,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        if (favChannels.isEmpty()) {
            Text(
                "No favourites yet.\n\nTap the heart on any channel to save it here.",
                color = GlassMuted,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            return@Column
        }
        ChannelTileGrid(
            channels = favChannels,
            onTileClick = { channel ->
                // onPlay (MainActivity) plays inline and goes to Channels.
                onPlay(channel)
            }
        )
    }
}
