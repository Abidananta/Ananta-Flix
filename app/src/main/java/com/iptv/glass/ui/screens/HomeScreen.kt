package com.iptv.glass.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iptv.glass.model.Channel
import com.iptv.glass.ui.components.ChannelCard
import com.iptv.glass.ui.components.GlassCard
import com.iptv.glass.ui.components.GlassChip
import com.iptv.glass.ui.theme.GlassMuted
import com.iptv.glass.ui.theme.GlassText
import com.iptv.glass.ui.theme.GlassWhite20
import com.iptv.glass.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onPlay: (Channel) -> Unit,
    onOpenDetail: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val category by viewModel.selectedCategory.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val channels by viewModel.filteredChannels.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    // Recent list is derived from the recent-ids flow; recompute each composition.
    val recentChannels = viewModel.recentChannels()

    Column(modifier.fillMaxSize()) {
        // Header + search
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            androidx.compose.foundation.layout.Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Glass IPTV", style = MaterialTheme.typography.headlineMedium, color = GlassText)
                    Text(
                        if (uiState.usedFallback) "Offline — showing sample playlist"
                        else "${uiState.channels.size} channels • from GitHub",
                        color = GlassMuted, style = MaterialTheme.typography.bodySmall
                    )
                }
                IconButton(onClick = { viewModel.refresh() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh playlist", tint = GlassText)
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("Search channels, movies…", color = GlassMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GlassMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = GlassText,
                    unfocusedTextColor = GlassText,
                    focusedBorderColor = GlassWhite20,
                    unfocusedBorderColor = GlassWhite20,
                    cursorColor = GlassText
                )
            )
        }

        // Category chips — built from the playlist's real group-titles.
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                GlassChip("All", category == "All") { viewModel.selectedCategory.value = "All" }
            }
            items(categories) { (name, count) ->
                GlassChip("$name ($count)", category == name) { viewModel.selectedCategory.value = name }
            }
        }
        Spacer(Modifier.height(12.dp))

        if (uiState.isLoading) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(48.dp))
                CircularProgressIndicator(color = GlassText)
                Spacer(Modifier.height(12.dp))
                Text("Loading playlist from GitHub…", color = GlassMuted)
            }
            return@Column
        }

        if (channels.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(48.dp))
                Text("No channels found", color = GlassText, fontWeight = FontWeight.SemiBold)
                Text("Try a different search, or check the playlist URL in AppConfig.kt.", color = GlassMuted)
            }
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (recentChannels.isNotEmpty() && query.isBlank() && category == "All") {
                item {
                    Text("Recently watched", style = MaterialTheme.typography.titleMedium, color = GlassText)
                    Spacer(Modifier.height(8.dp))
                }
                items(recentChannels) { channel ->
                    ChannelCard(
                        channel = channel,
                        isFavorite = channel.id in favorites,
                        nowProgram = viewModel.nowProgram(channel),
                        nextProgram = viewModel.nextProgram(channel),
                        onClick = { if (channel.isVod) onOpenDetail(channel) else onPlay(channel) },
                        onFavoriteClick = { viewModel.toggleFavorite(channel) }
                    )
                }
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("All channels", style = MaterialTheme.typography.titleMedium, color = GlassText)
                    Spacer(Modifier.height(8.dp))
                }
            }
            items(channels, key = { it.id }) { channel ->
                ChannelCard(
                    channel = channel,
                    isFavorite = channel.id in favorites,
                    nowProgram = viewModel.nowProgram(channel),
                    nextProgram = viewModel.nextProgram(channel),
                    onClick = { if (channel.isVod) onOpenDetail(channel) else onPlay(channel) },
                    onFavoriteClick = { viewModel.toggleFavorite(channel) }
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
