package com.aplay.tv.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.DataSaverOn
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Scoreboard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aplay.tv.AppViewModel
import com.aplay.tv.BuildConfig
import com.aplay.tv.Stream

private enum class Dialog { NONE, STREAM, PLAYLISTS, MATCHES }

@Composable
fun MoreScreen(model: AppViewModel, onPlay: (String, List<Stream>, Int) -> Unit) {
    var dialog by rememberSaveable { mutableStateOf(Dialog.NONE) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("PLAYBACK", color = Accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        SettingRow(Icons.Filled.Link, "Network stream", "Play a link with custom headers") {
            dialog = Dialog.STREAM
        }
        SettingRow(Icons.AutoMirrored.Filled.PlaylistAdd, "My playlists", "Add M3U links or files") {
            dialog = Dialog.PLAYLISTS
        }
        SettingRow(Icons.Filled.Scoreboard, "Matches source", "Link to your matches.json") {
            dialog = Dialog.MATCHES
        }
        SwitchRow(
            Icons.Filled.PictureInPictureAlt, "Floating player",
            "Keep playing in a small window when you leave",
            model.pictureInPicture, model::updatePictureInPicture,
        )
        SwitchRow(
            Icons.Filled.DataSaverOn, "Data saver",
            "Lowest quality on mobile data",
            model.dataSaver, model::updateDataSaver,
        )
        SwitchRow(
            Icons.Filled.Memory, "Software decoder",
            "Try this if video is black or broken",
            model.softwareDecoder, model::updateSoftwareDecoder,
        )
        Text(
            "APlay ${BuildConfig.VERSION_NAME}\nOnly add streams that you are allowed to watch.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 12.dp),
        )
    }

    when (dialog) {
        Dialog.NONE -> Unit
        Dialog.STREAM -> StreamDialog(
            onDismiss = { dialog = Dialog.NONE },
            onPlay = {
                dialog = Dialog.NONE
                onPlay("Network stream", listOf(it), 0)
            },
        )
        Dialog.PLAYLISTS -> PlaylistManagerDialog(model) { dialog = Dialog.NONE }
        Dialog.MATCHES -> MatchesDialog(model) { dialog = Dialog.NONE }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit = {},
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(icon, contentDescription = null, tint = Accent)
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            trailing()
        }
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    SettingRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        trailing = { Switch(checked = checked, onCheckedChange = onChange) },
        onClick = { onChange(!checked) },
    )
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun StreamDialog(onDismiss: () -> Unit, onPlay: (Stream) -> Unit) {
    var url by rememberSaveable { mutableStateOf("") }
    var userAgent by rememberSaveable { mutableStateOf("") }
    var referer by rememberSaveable { mutableStateOf("") }
    var origin by rememberSaveable { mutableStateOf("") }
    var cookie by rememberSaveable { mutableStateOf("") }
    var license by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Network stream") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Field("Stream link", url) { url = it }
                Field("User-Agent (optional)", userAgent) { userAgent = it }
                Field("Referer (optional)", referer) { referer = it }
                Field("Origin (optional)", origin) { origin = it }
                Field("Cookie (optional)", cookie) { cookie = it }
                Field("Widevine license link (optional)", license) { license = it }
            }
        },
        confirmButton = {
            TextButton(
                enabled = url.contains("://"),
                onClick = {
                    onPlay(
                        Stream(
                            name = "Network stream",
                            url = url.trim(),
                            userAgent = userAgent.trim(),
                            referer = referer.trim(),
                            origin = origin.trim(),
                            cookie = cookie.trim(),
                            licenseUrl = license.trim(),
                        )
                    )
                },
            ) { Text("Play") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun MatchesDialog(model: AppViewModel, onDismiss: () -> Unit) {
    var url by remember { mutableStateOf(model.matchesUrl) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Matches source") },
        text = { Field("matches.json link", url) { url = it } },
        confirmButton = {
            TextButton(
                onClick = {
                    model.updateMatchesUrl(url)
                    onDismiss()
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
