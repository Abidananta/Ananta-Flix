package com.aplay.tv.ui

import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.heightIn
import com.aplay.tv.TvPairing
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

private enum class Dialog { NONE, STREAM, PLAYLISTS, MATCHES, COLOR, TV }

private val BlueTile = Color(0xFF3B82F6)
private val GreenTile = Color(0xFF22C55E)
private val PurpleTile = Color(0xFFA855F7)
private val OrangeTile = Color(0xFFF59E0B)
private val RedTile = Color(0xFFEF4444)
private val TealTile = Color(0xFF14B8A6)
private val PinkTile = Color(0xFFEC4899)

private fun openLink(context: android.content.Context, url: String) {
    if (url.isBlank()) {
        android.widget.Toast.makeText(context, "Link not added yet", android.widget.Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val target = if (url.contains("@") && !url.startsWith("http")) "mailto:$url" else url
        context.startActivity(
            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(target))
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: Exception) {
        android.widget.Toast.makeText(context, "Cannot open link", android.widget.Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun MoreScreen(model: AppViewModel, onPlay: (String, List<Stream>, Int) -> Unit) {
    var dialog by rememberSaveable { mutableStateOf(Dialog.NONE) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // App card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = GlassFill),
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassStroke),
        ) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                AplayMark(Modifier.size(56.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("APlay", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                        Text(
                            "STABLE", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(Accent.copy(alpha = 0.18f))
                                .border(1.dp, Accent.copy(alpha = 0.6f), RoundedCornerShape(50))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                    Text("Version ${BuildConfig.VERSION_NAME}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.Check, null, tint = GreenTile, modifier = Modifier.size(14.dp))
                        Text("Up to date", fontSize = 12.sp, color = GreenTile, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Social tiles
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SocialTile(Modifier.weight(1f), "Telegram", Color(0xFF29A9EB), { Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White) }) {
                openLink(context, com.aplay.tv.Config.TELEGRAM_URL)
            }
            SocialTile(Modifier.weight(1f), "Facebook", Color(0xFF1877F2), { Text("f", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp) }) {
                openLink(context, com.aplay.tv.Config.FACEBOOK_URL)
            }
            SocialTile(Modifier.weight(1f), "Twitter", Color(0xFF111111), { Text("X", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp) }) {
                openLink(context, com.aplay.tv.Config.TWITTER_URL)
            }
            SocialTile(Modifier.weight(1f), "Instagram", Color(0xFFD62976), { Icon(Icons.Filled.CameraAlt, null, tint = Color.White) }) {
                openLink(context, com.aplay.tv.Config.INSTAGRAM_URL)
            }
        }

        SectionHeader("PLAYBACK & STREAMING")
        SettingRow(Icons.Filled.Link, BlueTile, "Network Stream", "Play a link with custom headers",
            trailing = { Chevron() }) { dialog = Dialog.STREAM }
        SettingRow(Icons.AutoMirrored.Filled.PlaylistAdd, GreenTile, "Custom Playlists", "Manage your M3U playlists & files",
            trailing = { Chevron() }) { dialog = Dialog.PLAYLISTS }
        SettingRow(Icons.Filled.PictureInPictureAlt, PurpleTile, "Floating Player (PiP)", "Keep playing in a small window",
            trailing = { OnOffBadge(model.pictureInPicture) }) { model.updatePictureInPicture(!model.pictureInPicture) }
        SettingRow(Icons.Filled.DataSaverOn, OrangeTile, "Data Saver Mode", "Lowest quality on mobile data",
            trailing = { Switch(checked = model.dataSaver, onCheckedChange = model::updateDataSaver) }) {
            model.updateDataSaver(!model.dataSaver)
        }
        SettingRow(Icons.Filled.Memory, RedTile, "Player Decoder", "Try software if video is black or broken",
            trailing = { Pill(if (model.softwareDecoder) "SOFTWARE" else "DEFAULT") }) {
            model.updateSoftwareDecoder(!model.softwareDecoder)
        }
        SettingRow(Icons.Filled.Scoreboard, PinkTile, "Match Schedule", "The schedule from your JSON links",
            trailing = { Chevron() }) { dialog = Dialog.MATCHES }

        SectionHeader("INTERFACE & STARTUP")
        SettingRow(Icons.Filled.Palette, BlueTile, "App Color", "Buttons, lines and highlights",
            trailing = { Box(Modifier.size(22.dp).clip(CircleShape).background(model.accent)) }) {
            dialog = Dialog.COLOR
        }
        SettingRow(Icons.Filled.PlayCircle, TealTile, "Auto-Play on Launch", "Resume your last channel when the app opens",
            trailing = { Switch(checked = model.autoPlay, onCheckedChange = model::updateAutoPlay) }) {
            model.updateAutoPlay(!model.autoPlay)
        }
        if (model.isTv) {
            SettingRow(Icons.Filled.Tv, RedTile, "Deactivate TV", "Forget the phone that activated this TV",
                trailing = { Chevron() }) { model.deactivateTv() }
        } else {
            SettingRow(Icons.Filled.Tv, PurpleTile, "Activate TV", "Type the code shown on your Android TV",
                trailing = { Chevron() }) { dialog = Dialog.TV }
        }

        SectionHeader("COMMUNITY & SUPPORT")
        SettingRow(Icons.AutoMirrored.Filled.Send, Color(0xFF29A9EB), "Join Telegram Channel", "Get updates, new servers & help",
            trailing = { Chevron() }) { openLink(context, com.aplay.tv.Config.TELEGRAM_URL) }
        SettingRow(Icons.Filled.Email, OrangeTile, "Contact & Feedback", "Report a problem or suggest something",
            trailing = { Chevron() }) { openLink(context, com.aplay.tv.Config.CONTACT_URL) }

        Text(
            "Only add streams that you are allowed to watch.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
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
        Dialog.MATCHES -> ScheduleDialog(model) { dialog = Dialog.NONE }
        Dialog.COLOR -> ColorDialog(model) { dialog = Dialog.NONE }
        Dialog.TV -> TvCodeDialog(model) { dialog = Dialog.NONE }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.width(4.dp).height(16.dp).clip(RoundedCornerShape(2.dp)).background(Accent))
        Text(text, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun SocialTile(modifier: Modifier, label: String, color: Color, icon: @Composable () -> Unit, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(GlassFill)
            .border(1.dp, GlassStroke, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(color), contentAlignment = Alignment.Center) { icon() }
        Text(label, fontSize = 11.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Chevron() {
    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun OnOffBadge(on: Boolean) {
    val c = if (on) GreenTile else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        if (on) "ON" else "OFF", color = c, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(c.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun Pill(text: String) {
    Text(
        text, color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(Accent.copy(alpha = 0.16f))
            .border(1.dp, Accent.copy(alpha = 0.5f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit = {},
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = GlassFill),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassStroke),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(tint),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, contentDescription = null, tint = Color.White) }
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            trailing()
        }
    }
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
        text = {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("matches.json links (one per line)") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth(),
            )
        },
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

@Composable
private fun ColorDialog(model: AppViewModel, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("App color") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                AccentChoices.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        row.forEach { (name, color) ->
                            val selected = model.accent == color
                            Box(
                                Modifier.size(52.dp).clip(CircleShape).background(color)
                                    .border(if (selected) 3.dp else 0.dp, Color.White, CircleShape)
                                    .clickable { model.updateAccent(color) },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (selected) Icon(Icons.Filled.Check, contentDescription = name, tint = Color.Black)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun TvCodeDialog(model: AppViewModel, onDismiss: () -> Unit) {
    var code by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Activate TV") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Open APlay on your Android TV. It shows a code. Type it here.", fontSize = 13.sp)
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = TvPairing.clean(it).take(TvPairing.CODE_LENGTH) },
                    label = { Text("TV code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    model.sendToTv(code)
                    onDismiss()
                },
                enabled = code.length == TvPairing.CODE_LENGTH,
            ) { Text("Activate") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ScheduleDialog(model: AppViewModel, onDismiss: () -> Unit) {
    val format = remember { java.text.SimpleDateFormat("EEE d MMM, h:mm a", java.util.Locale.getDefault()) }
    val now = System.currentTimeMillis()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Match schedule") },
        text = {
            if (model.matches.isEmpty()) {
                Text(
                    "No matches yet. Put a JSON link in playlists/links.txt on GitHub and the " +
                        "schedule shows here and in Matches.",
                )
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(model.matches.sortedBy { it.startMillis }) { match ->
                        Column {
                            Text(
                                if (match.away.name.isEmpty()) match.home.name
                                else "${match.home.name} vs ${match.away.name}",
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "${match.league} · ${format.format(java.util.Date(match.startMillis))} · " +
                                    match.status(now).name.lowercase().replaceFirstChar { it.uppercase() },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
