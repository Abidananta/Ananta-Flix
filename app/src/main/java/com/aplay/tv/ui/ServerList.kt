package com.aplay.tv.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aplay.tv.AppViewModel

/** Small button beside the category chips. Shows the chosen server and opens the server list. */
@Composable
fun ServerButton(label: String, onClick: () -> Unit) {
    Row(
        Modifier.padding(start = 12.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(onClick = onClick)
            .padding(start = 10.dp, top = 7.dp, bottom = 7.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Dns, contentDescription = "Servers", tint = Accent)
        Text(
            label,
            Modifier.padding(start = 6.dp).widthIn(max = 110.dp),
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
    }
}

/**
 * Every playlist and file the app knows, shown as servers. Tap one to show only its channels,
 * switch it on or off, or edit it.
 */
@Composable
fun ServerDialog(model: AppViewModel, onDismiss: () -> Unit) {
    val chosen = model.servers.firstOrNull { it.key == model.selectedServer && it.enabled }?.key
        ?: model.servers.firstOrNull { it.enabled }?.key.orEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Servers") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                model.servers.forEach { server ->
                    ServerRow(
                        selected = chosen == server.key,
                        title = server.name,
                        subtitle = when {
                            !server.enabled -> "Off"
                            server.playlist == null -> "${server.count} channels · built in"
                            else -> "${server.count} channels"
                        },
                        enabled = server.enabled,
                        onSelect = {
                            model.selectServer(server.key)
                            onDismiss()
                        },
                        trailing = server.playlist?.let { playlist ->
                            {
                                Switch(
                                    checked = playlist.enabled,
                                    onCheckedChange = { model.setPlaylistEnabled(playlist.id, it) },
                                )
                                IconButton(
                                    onClick = {
                                        onDismiss()
                                        model.showEditPlaylist(playlist)
                                    },
                                ) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
                            }
                        },
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun ServerRow(
    selected: Boolean,
    title: String,
    subtitle: String,
    onSelect: () -> Unit,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = if (enabled) onSelect else null, enabled = enabled)
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing?.invoke()
    }
}

/** The Servers tab: one card per playlist or file. Tap opens its channels, long press manages it. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ServersScreen(
    model: AppViewModel,
    query: String,
    onOpen: (String) -> Unit,
    onManage: () -> Unit,
) {
    val servers = model.servers.filter { it.name.contains(query, ignoreCase = true) }.distinctBy { it.key }
    if (servers.isEmpty()) {
        EmptyMessage("No servers yet.\nAdd links to playlists/links.txt on GitHub.")
        return
    }
    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(servers, key = { it.key }) { server ->
            ServerTile(
                name = server.name,
                subtitle = if (server.enabled) "${server.count} channels" else "Off",
                selected = server.key == model.selectedServer,
                dimmed = !server.enabled,
                onClick = { if (server.enabled) onOpen(server.key) else onManage() },
                onLongClick = onManage,
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ServerTile(
    name: String,
    subtitle: String,
    selected: Boolean,
    dimmed: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(Brush.verticalGradient(listOf(GlassFillStrong, GlassFill)))
            .border(1.dp, if (selected) Accent else GlassStroke, shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The picture follows the server's name: sports -> trophy, movies -> film, series -> dish...
        val (icon, tint) = serverIcon(name)
        Box(
            Modifier.size(70.dp).clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(tint, tint.copy(alpha = 0.55f))))
                .alpha(if (dimmed) 0.4f else 1f)
                .border(1.dp, GlassStroke, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.material3.Icon(icon, null, tint = Color.White, modifier = Modifier.size(38.dp))
        }
        Text(
            name,
            Modifier.padding(top = 10.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 2,
            minLines = 2,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            color = if (dimmed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
        Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

/** Picks a picture and colour for a server from words in its name. */
fun serverIcon(name: String): Pair<androidx.compose.ui.graphics.vector.ImageVector, Color> {
    val n = name.lowercase()
    fun has(vararg words: String) = words.any { n.contains(it) }
    return when {
        has("cricket", "ipl", "t20", "psl", "bpl") -> androidx.compose.material.icons.Icons.Filled.SportsCricket to Color(0xFF16A34A)
        has("football", "soccer", "fifa", "epl", "league", "laliga", "bundesliga") ->
            androidx.compose.material.icons.Icons.Filled.SportsSoccer to Color(0xFF22C55E)
        has("sport", "match", "bein", "espn", "t sports", "gazi", "ten ", "star sports") ->
            androidx.compose.material.icons.Icons.Filled.EmojiEvents to Color(0xFFF59E0B)
        has("movie", "film", "cinema", "hbo", "bollywood", "hollywood", "vod") ->
            androidx.compose.material.icons.Icons.Filled.Movie to Color(0xFFEF4444)
        has("news") -> androidx.compose.material.icons.Icons.Filled.Campaign to Color(0xFF3B82F6)
        has("kid", "cartoon", "anime", "child") -> androidx.compose.material.icons.Icons.Filled.ChildCare to Color(0xFFEC4899)
        has("music", "song", "radio", "fm") -> androidx.compose.material.icons.Icons.Filled.MusicNote to Color(0xFFA855F7)
        has("doc", "discovery", "nature", "geo") -> androidx.compose.material.icons.Icons.Filled.Explore to Color(0xFF14B8A6)
        has("islam", "quran", "deen") -> androidx.compose.material.icons.Icons.Filled.Language to Color(0xFF059669)
        has("series", "drama", "serial", "show", "dish", "tv", "bangla", "bd", "india", "hindi") ->
            androidx.compose.material.icons.Icons.Filled.SatelliteAlt to Color(0xFF6366F1)
        else -> androidx.compose.material.icons.Icons.Filled.SatelliteAlt to Color(0xFF0EA5E9)
    }
}
