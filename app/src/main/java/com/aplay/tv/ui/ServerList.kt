package com.aplay.tv.ui

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
    val chosen = model.servers.firstOrNull { it.key == model.selectedServer && it.enabled }?.key.orEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Servers") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ServerRow(
                    selected = chosen.isEmpty(),
                    title = "All servers",
                    subtitle = "${model.channels.size} channels",
                    onSelect = {
                        model.selectServer("")
                        onDismiss()
                    },
                )
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
        dismissButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    model.showAddPlaylist()
                },
            ) { Text("Add playlist") }
        },
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

private class ServerCard(
    val key: String?,
    val name: String,
    val subtitle: String,
    val logo: String,
    val enabled: Boolean,
    val server: com.aplay.tv.Server?,
)

/** The Servers tab: one card per playlist or file. Tap opens its channels, long press manages it. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ServersScreen(
    model: AppViewModel,
    query: String,
    onOpen: (String) -> Unit,
    onManage: () -> Unit,
) {
    val cards = buildList {
        add(ServerCard(null, "All servers", "${model.channels.size} channels",
            model.channels.firstOrNull { it.logo.isNotBlank() }?.logo.orEmpty(), true, null))
        model.servers.forEach { s ->
            add(ServerCard(
                s.key, s.name,
                if (s.enabled) "${s.count} channels" else "Off",
                model.sourceChannels[s.key]?.firstOrNull { it.logo.isNotBlank() }?.logo.orEmpty(),
                s.enabled, s,
            ))
        }
    }.filter { it.name.contains(query, ignoreCase = true) }

    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(cards, key = { it.key ?: "__all" }) { card ->
            val selected = false
            ServerTile(
                name = card.name,
                subtitle = card.subtitle,
                selected = selected,
                dimmed = !card.enabled,
                onClick = { if (card.enabled) onOpen(card.key.orEmpty()) else onManage() },
                onLongClick = onManage,
            ) {
                if (card.key == null || card.logo.isNotBlank()) {
                    Logo(card.logo, card.name, Modifier.size(56.dp))
                } else {
                    Icon(Icons.Filled.Dns, contentDescription = null, tint = Accent, modifier = Modifier.size(32.dp))
                }
            }
        }
        item(key = "__add") {
            ServerTile("Add playlist", "link or file", false, false,
                onClick = { model.showAddPlaylist() }, onLongClick = {}) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = Accent, modifier = Modifier.size(32.dp))
            }
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
    icon: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, if (selected) Accent else MaterialTheme.colorScheme.outlineVariant, shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(70.dp).clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) { icon() }
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
