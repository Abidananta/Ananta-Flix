package com.aplay.tv.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.material3.IconButton
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.background
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

private val NpCard = Color(0xB3141824)
private val NpBorder = Color(0xFF262B3A)
private val NpPill = Color(0xFF1E2433)
private val NpPillBorder = Color(0xFF2E3547)
private val NpPillText = Color(0xFFA7B0C4)

/** The channel that is playing: a dark card with a cyan bar and the server on the right. */
@Composable
fun NowPlayingBar(model: AppViewModel) {
    val context = LocalContext.current
    val player = model.player
    val playing = player.playing ?: return
    var servers by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)

    Box(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(
            Modifier.fillMaxWidth().clip(shape).background(NpCard)
                .border(1.dp, NpBorder, shape)
                .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(4.dp).height(40.dp).clip(RoundedCornerShape(2.dp)).background(Accent))
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(LiveRed))
                    Text(
                        "NOW PLAYING CHANNEL",
                        Modifier.padding(start = 8.dp),
                        color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    playing.title,
                    Modifier.padding(top = 4.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (Config.TELEGRAM_URL.isNotBlank()) {
                IconButton(
                    onClick = {
                        open(context, Intent(Intent.ACTION_VIEW, Uri.parse(Config.TELEGRAM_URL)))
                    },
                ) { Icon(Icons.AutoMirrored.Filled.Send, "Telegram", tint = Accent) }
            }
            Box {
                Row(
                    Modifier.clip(RoundedCornerShape(10.dp)).background(NpPill)
                        .border(1.dp, NpPillBorder, RoundedCornerShape(10.dp))
                        .clickable { servers = true }
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(LiveRed))
                    Text(
                        (if (playing.streams.size > 1) playing.streams[playing.server].name else "Server 1")
                            .uppercase(),
                        Modifier.padding(start = 8.dp),
                        color = NpPillText, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                    )
                }
                DropdownMenu(expanded = servers, onDismissRequest = { servers = false }) {
                    playing.streams.forEachIndexed { index, stream ->
                        DropdownMenuItem(
                            text = { Text(if (playing.streams.size > 1) stream.name else "Server 1") },
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

private fun open(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No app can open this", Toast.LENGTH_SHORT).show()
    }
}
