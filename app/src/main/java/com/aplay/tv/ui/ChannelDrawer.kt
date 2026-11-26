package com.aplay.tv.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aplay.tv.AppViewModel

/** Channel strip shown over the full screen video. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BoxScope.ChannelDrawer(model: AppViewModel, onClose: () -> Unit) {
    val playingId = model.player.playing?.channelId
    var favoritesOnly by remember { mutableStateOf(false) }
    var movingId by remember { mutableStateOf<String?>(null) }
    val channels =
        if (favoritesOnly) model.channels.filter { it.id in model.favorites } else model.channels

    Row(
        Modifier.align(Alignment.TopCenter).displayCutoutPadding().padding(top = 16.dp)
            .clip(RoundedCornerShape(50)).background(Color(0xE61D1B19)).padding(4.dp),
    ) {
        DrawerTab("All channels", !favoritesOnly) {
            favoritesOnly = false
            movingId = null
        }
        DrawerTab("Favorites", favoritesOnly) {
            favoritesOnly = true
            movingId = null
        }
    }

    Column(
        Modifier.align(Alignment.BottomCenter).displayCutoutPadding().fillMaxWidth()
            .background(Color(0xB3000000)).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val moving = channels.indexOfFirst { it.id == movingId }
        if (moving >= 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    enabled = moving > 0,
                    onClick = { model.moveChannel(channels[moving].id, channels[moving - 1].id) },
                ) { Text("◀ Move left") }
                TextButton(
                    enabled = moving < channels.lastIndex,
                    onClick = { model.moveChannel(channels[moving].id, channels[moving + 1].id) },
                ) { Text("Move right ▶") }
                TextButton(onClick = { movingId = null }) { Text("Done") }
            }
        } else {
            Text(
                "Press and hold a channel to move it",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        if (channels.isEmpty()) {
            Text(
                if (favoritesOnly) "No favorites yet." else "No channels.",
                color = Color.White,
                modifier = Modifier.padding(24.dp),
            )
        } else {
            // Each tab opens scrolled to the channel that is playing.
            key(favoritesOnly) {
                val start = (channels.indexOfFirst { it.id == playingId } - 2).coerceAtLeast(0)
                LazyRow(
                    state = rememberLazyListState(start),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(channels, key = { _, channel -> channel.id }) { index, channel ->
                        val highlight = when (channel.id) {
                            movingId -> Color.White
                            playingId -> Accent
                            else -> Color.Transparent
                        }
                        Column(
                            Modifier.width(112.dp).combinedClickable(
                                onClick = {
                                    model.player.openChannel(channel, channels)
                                    onClose()
                                },
                                onLongClick = { movingId = channel.id },
                            ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                Modifier.fillMaxWidth().height(72.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xE6262320))
                                    .border(2.dp, highlight, RoundedCornerShape(14.dp)),
                            ) {
                                Logo(channel.logo, channel.name, Modifier.align(Alignment.Center).size(52.dp))
                                Text(
                                    "${index + 1}",
                                    color = Accent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(8.dp),
                                )
                            }
                            Text(
                                channel.name,
                                color = if (channel.id == playingId) Accent else Color.White,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) Color.Black else Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        modifier = Modifier.clip(RoundedCornerShape(50))
            .background(if (selected) Accent else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
    )
}
