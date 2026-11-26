package com.iptv.glass.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.iptv.glass.model.Channel
import com.iptv.glass.ui.components.ChannelCard
import com.iptv.glass.ui.theme.GlassMuted
import com.iptv.glass.ui.theme.GlassText
import com.iptv.glass.viewmodel.MainViewModel

/** Favourites, persisted locally with DataStore (survives app restarts). */
@Composable
fun FavoritesScreen(
    viewModel: MainViewModel,
    onPlay: (Channel) -> Unit
) {
    val favorites by viewModel.favorites.collectAsState()
    val favChannels = viewModel.favoriteChannels()
    Column(Modifier.fillMaxSize()) {
        Text(
            "Favourites",
            style = MaterialTheme.typography.headlineMedium,
            color = GlassText,
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
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(favChannels, key = { it.id }) { channel ->
                ChannelCard(
                    channel = channel,
                    isFavorite = channel.id in favorites,
                    nowProgram = viewModel.nowProgram(channel),
                    nextProgram = viewModel.nextProgram(channel),
                    onClick = { onPlay(channel) },
                    onFavoriteClick = { viewModel.toggleFavorite(channel) }
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
