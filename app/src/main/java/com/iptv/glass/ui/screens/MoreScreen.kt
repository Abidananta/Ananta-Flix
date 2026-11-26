package com.iptv.glass.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iptv.glass.AppConfig
import com.iptv.glass.model.Channel
import com.iptv.glass.ui.components.AppIconTile
import com.iptv.glass.ui.components.GlassCard
import com.iptv.glass.ui.components.NexaSectionTitle
import com.iptv.glass.ui.components.SettingsIconTile
import com.iptv.glass.ui.theme.GlassMuted
import com.iptv.glass.ui.theme.GlassText
import com.iptv.glass.ui.theme.NexaCyan
import com.iptv.glass.viewmodel.MainViewModel

/**
 * More — modelled on the reference app's More screen, but with OUR app
 * identity (Glass IPTV v1.0) and no copied social handles: there is no
 * social-links row, because the user has not supplied any.
 *
 * Playback & Streaming rows are real:
 * - Network Stream: paste any stream URL and play it.
 * - Custom Playlists: view / edit the playlist URL (saved locally, overrides AppConfig).
 * - Floating Player (PiP): toggle; the player shows a PiP button when on.
 * - Data Saver: toggle; the player caps quality to SD when on.
 * - Player Decoder: "Default" (ExoPlayer) — the only decoder shipped.
 */
@Composable
fun MoreScreen(
    viewModel: MainViewModel,
    onPlayNetworkStream: (Channel) -> Unit
) {
    val pipEnabled by viewModel.pipEnabled.collectAsState()
    val dataSaver by viewModel.dataSaverEnabled.collectAsState()
    val decoder by viewModel.decoder.collectAsState()
    val customUrl by viewModel.customPlaylistUrl.collectAsState()

    var showNetworkDialog by remember { mutableStateOf(false) }
    var networkUrl by remember { mutableStateOf("") }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var playlistUrl by remember { mutableStateOf(viewModel.effectivePlaylistUrl()) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        // App header card.
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                AppIconTile(modifier = Modifier.padding(end = 12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(AppConfig.APP_NAME, color = GlassText, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.width(8.dp))
                        Text("STABLE", color = NexaCyan, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(AppConfig.APP_TAGLINE, color = GlassMuted, style = MaterialTheme.typography.bodySmall)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("v${AppConfig.APP_VERSION}", color = GlassText, fontWeight = FontWeight.SemiBold)
                    Text("Up to date", color = NexaCyan, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        NexaSectionTitle("PLAYBACK & STREAMING")
        Spacer(Modifier.height(10.dp))

        SettingsRow(
            icon = Icons.Default.SignalCellularAlt,
            title = "Network Stream",
            subtitle = "Play any stream URL",
            onClick = { showNetworkDialog = true }
        )
        Spacer(Modifier.height(10.dp))
        SettingsRow(
            icon = Icons.Default.List,
            title = "Custom Playlists",
            subtitle = "Manage M3U playlists & local channel lists",
            onClick = {
                playlistUrl = viewModel.effectivePlaylistUrl()
                showPlaylistDialog = true
            }
        )
        Spacer(Modifier.height(10.dp))
        SettingsRow(
            icon = Icons.Default.PictureInPictureAlt,
            title = "Floating Player (PiP)",
            subtitle = if (pipEnabled) "Picture-in-Picture mode is active" else "Picture-in-Picture mode is off",
            trailing = {
                Column(horizontalAlignment = Alignment.End) {
                    Switch(
                        checked = pipEnabled,
                        onCheckedChange = { viewModel.setPipEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = NexaCyan)
                    )
                    Text(
                        if (pipEnabled) "ON" else "OFF",
                        color = if (pipEnabled) NexaCyan else GlassMuted,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
        Spacer(Modifier.height(10.dp))
        SettingsRow(
            icon = Icons.Default.DataUsage,
            title = "Data Saver Mode",
            subtitle = "Forces lowest stream resolution on mobile data",
            trailing = {
                Switch(
                    checked = dataSaver,
                    onCheckedChange = { viewModel.setDataSaverEnabled(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = NexaCyan)
                )
            }
        )
        Spacer(Modifier.height(10.dp))
        SettingsRow(
            icon = Icons.Default.Speed,
            title = "Player Decoder",
            subtitle = "Select video rendering engine",
            trailing = {
                Text("DEFAULT", color = NexaCyan, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "Playlist: ${viewModel.effectivePlaylistUrl()}",
            color = GlassMuted,
            style = MaterialTheme.typography.bodySmall
        )
        if (decoder != "Default") {
            Text("Decoder: $decoder", color = GlassMuted, style = MaterialTheme.typography.bodySmall)
        }
    }

    if (showNetworkDialog) {
        AlertDialog(
            onDismissRequest = { showNetworkDialog = false },
            title = { Text("Network Stream", color = GlassText) },
            text = {
                Column {
                    Text("Paste a stream URL (.m3u8 or .mp4) to play it.", color = GlassMuted)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = networkUrl,
                        onValueChange = { networkUrl = it },
                        placeholder = { Text("https://…/stream.m3u8", color = GlassMuted) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = networkUrl.isNotBlank(),
                    onClick = {
                        val channel = viewModel.playNetworkStream(networkUrl)
                        networkUrl = ""
                        showNetworkDialog = false
                        onPlayNetworkStream(channel)
                    }
                ) { Text("Play", color = NexaCyan) }
            },
            dismissButton = {
                TextButton(onClick = { showNetworkDialog = false }) { Text("Cancel", color = GlassMuted) }
            }
        )
    }

    if (showPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showPlaylistDialog = false },
            title = { Text("Custom Playlists", color = GlassText) },
            text = {
                Column {
                    Text(
                        "GitHub Raw M3U URL. Leave it as the default, or paste your own. " +
                            "It is saved on this device and used on the next refresh.",
                        color = GlassMuted
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = playlistUrl,
                        onValueChange = { playlistUrl = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setCustomPlaylistUrl(playlistUrl)
                        showPlaylistDialog = false
                    }
                ) { Text("Save", color = NexaCyan) }
            },
            dismissButton = {
                TextButton(onClick = { showPlaylistDialog = false }) { Text("Cancel", color = GlassMuted) }
            }
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    GlassCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsIconTile(icon)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(title, color = GlassText, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = GlassMuted, style = MaterialTheme.typography.bodySmall)
            }
            if (trailing != null) {
                trailing()
            } else {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = GlassMuted)
            }
        }
    }
}
