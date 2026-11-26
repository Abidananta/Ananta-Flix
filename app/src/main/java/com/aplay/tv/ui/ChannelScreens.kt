package com.aplay.tv.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aplay.tv.Channel

const val ALL_CHANNELS = "All"

@Composable
fun ChannelsScreen(
    channels: List<Channel>,
    category: String,
    onCategory: (String) -> Unit,
    query: String,
    favorites: Set<String>,
    onFavorite: (Channel) -> Unit,
    onPlay: (Channel, List<Channel>) -> Unit,
    playingId: String?,
) {
    val categories = listOf(ALL_CHANNELS) + channels.map { it.group }.distinct()
    val active = if (category in categories) category else ALL_CHANNELS
    val visible = channels.filter {
        (active == ALL_CHANNELS || it.group == active) && it.name.contains(query, ignoreCase = true)
    }
    Column(Modifier.fillMaxSize()) {
        // The chips are hidden under the player to leave room for channels.
        if (playingId == null) ChipRow(categories, active, onCategory)
        if (visible.isEmpty()) {
            EmptyMessage("No channels found.")
        } else {
            ChannelGrid(visible, favorites, onFavorite, onPlay, playingId)
        }
    }
}

@Composable
fun FavoritesScreen(
    channels: List<Channel>,
    query: String,
    favorites: Set<String>,
    onFavorite: (Channel) -> Unit,
    onPlay: (Channel, List<Channel>) -> Unit,
    playingId: String?,
) {
    val visible = channels.filter { it.id in favorites && it.name.contains(query, ignoreCase = true) }
    if (visible.isEmpty()) {
        EmptyMessage("No favorites yet.\nTap the heart on a channel to keep it here.")
    } else {
        ChannelGrid(visible, favorites, onFavorite, onPlay, playingId)
    }
}

@Composable
private fun ChannelGrid(
    channels: List<Channel>,
    favorites: Set<String>,
    onFavorite: (Channel) -> Unit,
    onPlay: (Channel, List<Channel>) -> Unit,
    playingId: String?,
) {
    val compact = playingId != null
    LazyVerticalGrid(
        columns = GridCells.Adaptive(if (compact) 76.dp else 104.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(channels, key = { it.id }) { channel ->
            val favorite = channel.id in favorites
            Card(
                modifier = Modifier.clickable { onPlay(channel, channels) },
                border = if (channel.id == playingId) BorderStroke(2.dp, Accent) else null,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Box {
                    Column(
                        Modifier.fillMaxWidth().padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Logo(channel.logo, channel.name, Modifier.fillMaxWidth().aspectRatio(1f))
                        Text(
                            text = channel.name,
                            modifier = Modifier.padding(top = 8.dp),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            minLines = 2,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(
                        onClick = { onFavorite(channel) },
                        modifier = Modifier.align(Alignment.TopEnd).size(if (compact) 28.dp else 36.dp),
                    ) {
                        Icon(
                            imageVector = if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (favorite) "Remove favorite" else "Add favorite",
                            tint = if (favorite) LiveRed else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoriesScreen(channels: List<Channel>, query: String, onOpen: (String) -> Unit) {
    val categories = channels.groupingBy { it.group }.eachCount().toList()
        .filter { it.first.contains(query, ignoreCase = true) }
    if (categories.isEmpty()) {
        EmptyMessage("No categories found.")
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(categories, key = { it.first }) { (name, count) ->
            Card(
                modifier = Modifier.clickable { onOpen(name) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Icon(Icons.Filled.Folder, contentDescription = null, tint = Accent)
                    Text(name, Modifier.padding(top = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "$count channels",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
