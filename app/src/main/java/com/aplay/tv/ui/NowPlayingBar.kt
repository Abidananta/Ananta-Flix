package com.aplay.tv.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aplay.tv.AppViewModel
import com.aplay.tv.Config

/** Buttons and channel name under the small player window. */
@Composable
fun NowPlayingBar(model: AppViewModel) {
    val context = LocalContext.current
    val player = model.player
    val playing = player.playing ?: return
    var servers by remember { mutableStateOf(false) }

    Column(
        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (Config.TELEGRAM_URL.isNotBlank()) {
                FilledTonalButton(
                    onClick = {
                        open(context, Intent(Intent.ACTION_VIEW, Uri.parse(Config.TELEGRAM_URL)))
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, null, Modifier.size(18.dp))
                    Text(" Telegram", maxLines = 1)
                }
            }
            OutlinedButton(
                onClick = { player.reload() },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                Icon(Icons.Filled.Refresh, null, Modifier.size(18.dp))
                Text(" Reload", maxLines = 1)
            }
            FilledTonalIconButton(
                onClick = {
                    val text = "Watching ${playing.title} on APlay ${Config.TELEGRAM_URL}".trim()
                    val send = Intent(Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, text)
                    model.skipPictureInPicture = true
                    open(context, Intent.createChooser(send, "Share"))
                },
            ) { Icon(Icons.Filled.Share, "Share") }
            FilledTonalIconButton(onClick = { model.fullscreen = true }) {
                Icon(Icons.Filled.Fullscreen, "Full screen")
            }
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("● NOW PLAYING", color = LiveRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        playing.title,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box {
                    OutlinedButton(onClick = { servers = true }) {
                        Text(
                            if (playing.streams.size > 1) {
                                "${playing.streams[playing.server].name} ▾"
                            } else {
                                "Server 1"
                            },
                            maxLines = 1,
                        )
                    }
                    DropdownMenu(expanded = servers, onDismissRequest = { servers = false }) {
                        playing.streams.forEachIndexed { index, stream ->
                            DropdownMenuItem(
                                text = {
                                    Text(if (playing.streams.size > 1) stream.name else "Server 1")
                                },
                                onClick = {
                                    servers = false
                                    player.selectServer(index)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun open(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No app can open this", Toast.LENGTH_SHORT).show()
    }
}
