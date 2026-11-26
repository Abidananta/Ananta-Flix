package com.iptv.glass.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.iptv.glass.AppConfig
import com.iptv.glass.model.Channel
import com.iptv.glass.ui.components.ChannelTileGrid
import com.iptv.glass.ui.components.GlassChip
import com.iptv.glass.ui.components.NexaIcon
import com.iptv.glass.ui.components.NexaIconButton
import com.iptv.glass.ui.components.NowPlayingBar
import com.iptv.glass.ui.components.PlayerArea
import com.iptv.glass.ui.theme.GlassMuted
import com.iptv.glass.ui.theme.GlassText
import com.iptv.glass.ui.theme.GlassWhite20
import com.iptv.glass.ui.theme.NexaCardBorder
import com.iptv.glass.ui.theme.NexaCyan
import com.iptv.glass.viewmodel.MainViewModel

/**
 * Channels — the main tab, laid out exactly like the reference app's
 * Channels screens (screenshots 1 and 3):
 *
 *  - Pure-black player area at the top (the video area).
 *  - Action row under it: "Join Telegram" (cyan outline button, only
 *    when AppConfig.TELEGRAM_URL is set) + Reload + Share + Fullscreen
 *    (small dark squares, Fullscreen with a cyan border).
 *  - "NOW PLAYING CHANNEL" bar with the channel name and a SERVER 1 pill.
 *  - App-name header with search + reload icons (search field opens on tap).
 *  - Category chips in a horizontal row — "All" first, then the
 *    playlist's group-titles, name only (no counts).
 *  - A 3-column grid of white-logo tiles on phones (4 on wide screens),
 *    channel name in white below each logo. Tap a tile to play.
 */
@Composable
fun ChannelsScreen(
    viewModel: MainViewModel,
    onPlay: (Channel) -> Unit,
    onOpenDetail: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val category by viewModel.selectedCategory.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val channels by viewModel.filteredChannels.collectAsState()
    val nowPlaying by viewModel.nowPlaying.collectAsState()
    var showSearch by remember { mutableStateOf(false) }

    Column(modifier.fillMaxSize()) {
        // Black player area at the very top (reference screenshot 1).
        PlayerArea(
            channel = nowPlaying,
            nowProgram = nowPlaying?.let { viewModel.nowProgram(it) },
            onPlay = { nowPlaying?.let { onPlay(it) } }
        )

        // Action row: Join Telegram / Reload / Share / Fullscreen.
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (AppConfig.TELEGRAM_URL.isNotBlank()) {
                OutlinedButton(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, AppConfig.TELEGRAM_URL.toUri())
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, NexaCyan),
                    modifier = Modifier.weight(1f)
                ) {
                    NexaIcon(Icons.Default.Send, contentDescription = null, tint = NexaCyan)
                    Spacer(Modifier.width(6.dp))
                    Text("Join Telegram", color = GlassText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.width(8.dp))
            }
            OutlinedButton(
                onClick = { viewModel.refresh() },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, NexaCardBorder),
                modifier = Modifier.weight(1f)
            ) {
                NexaIcon(Icons.Default.Refresh, contentDescription = null, tint = NexaCyan)
                Spacer(Modifier.width(6.dp))
                Text("Reload", color = GlassText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(8.dp))
            NexaSquareIconButton(
                icon = Icons.Default.Share,
                contentDescription = "Share",
                onClick = {
                    val target = nowPlaying ?: return@NexaSquareIconButton
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Watch ${target.name} on ${AppConfig.APP_NAME}")
                    }
                    context.startActivity(Intent.createChooser(share, "Share channel"))
                }
            )
            Spacer(Modifier.width(8.dp))
            NexaSquareIconButton(
                icon = Icons.Default.Fullscreen,
                contentDescription = "Fullscreen",
                onClick = { nowPlaying?.let { onPlay(it) } },
                tint = NexaCyan,
                borderColor = NexaCyan
            )
        }

        // Now Playing bar — only once a channel has played.
        nowPlaying?.let { playing ->
            NowPlayingBar(
                channel = playing,
                nowProgram = viewModel.nowProgram(playing),
                onClick = { onPlay(playing) },
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Spacer(Modifier.height(10.dp))
        }

        // Header: app name + search + reload (reference screenshot 3).
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                AppConfig.APP_NAME,
                fontSize = 20.sp,
                color = GlassText,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            NexaIconButton(
                icon = Icons.Default.Search,
                contentDescription = "Search",
                onClick = { showSearch = !showSearch }
            )
            NexaIconButton(
                icon = Icons.Default.Refresh,
                contentDescription = "Reload playlist",
                onClick = { viewModel.refresh() }
            )
        }

        if (showSearch) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("Search channels, movies…", color = GlassMuted) },
                leadingIcon = { NexaIcon(Icons.Default.Search, contentDescription = null, tint = GlassMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = GlassText,
                    unfocusedTextColor = GlassText,
                    focusedBorderColor = GlassWhite20,
                    unfocusedBorderColor = GlassWhite20,
                    cursorColor = GlassText
                )
            )
            Spacer(Modifier.height(12.dp))
        }

        // Category chips — "All" first, then the playlist's group-titles.
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                GlassChip("All", category == "All") { viewModel.selectedCategory.value = "All" }
            }
            items(categories) { (name, _) ->
                GlassChip(name, category == name) { viewModel.selectedCategory.value = name }
            }
        }
        Spacer(Modifier.height(12.dp))

        if (uiState.isLoading) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(48.dp))
                CircularProgressIndicator(color = NexaCyan)
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
                Text(
                    "Try a different search, or check the playlist URL in AppConfig.kt.",
                    color = GlassMuted
                )
            }
            return@Column
        }

        // 3-column tile grid on phones, 4 on wide/tablet screens.
        ChannelTileGrid(
            channels = channels,
            onTileClick = { channel ->
                viewModel.setNowPlaying(channel)
                if (channel.isVod) onOpenDetail(channel) else onPlay(channel)
            }
        )
    }
}
