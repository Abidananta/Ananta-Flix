package com.aplay.tv.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aplay.tv.AppViewModel
import com.aplay.tv.Playlist

/** The "Add playlist" window. Keep this on screen at all times so the file picker can answer. */
@Composable
fun AddPlaylistDialog(model: AppViewModel) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) model.importPlaylist(uri)
    }
    val link = model.addLink ?: return
    val editing = model.editing
    val isFile = editing?.source?.startsWith("file:") == true
    var name by rememberSaveable(editing?.id, link) { mutableStateOf(editing?.name.orEmpty()) }
    var url by rememberSaveable(editing?.id, link) { mutableStateOf(link) }
    val clipboard = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = model::dismissPlaylistWindow,
        title = { Text(if (editing == null) "Add playlist" else "Edit playlist") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!isFile) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("Playlist link") },
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedButton(
                        onClick = { clipboard.getText()?.let { url = it.text.trim() } },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Paste copied link") }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (editing == null) {
                    OutlinedButton(
                        enabled = !model.checking,
                        onClick = {
                            model.skipPictureInPicture = true
                            picker.launch(arrayOf("*/*"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Choose a file from phone") }
                }
                if (model.checking) {
                    Text("Checking the playlist…", fontSize = 12.sp)
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                model.addError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !model.checking && (isFile || url.isNotBlank()),
                onClick = { model.savePlaylist(name, url) },
            ) { Text(if (editing == null) "Add" else "Save") }
        },
        dismissButton = {
            TextButton(onClick = model::dismissPlaylistWindow) { Text("Cancel") }
        },
    )
}

/** List of saved playlists: switch on or off, edit, delete. */
@Composable
fun PlaylistManagerDialog(model: AppViewModel, onDismiss: () -> Unit) {
    var deleting by remember { mutableStateOf<Playlist?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("My playlists") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (model.playlists.isEmpty()) Text("No playlist added yet.")
                model.playlists.forEach { playlist ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            playlist.name,
                            Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
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
                        IconButton(onClick = { deleting = playlist }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                }
                Button(
                    onClick = {
                        onDismiss()
                        model.showAddPlaylist()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Add new playlist") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )

    deleting?.let { playlist ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete playlist?") },
            text = { Text(playlist.name) },
            confirmButton = {
                TextButton(
                    onClick = {
                        model.removePlaylist(playlist)
                        deleting = null
                    },
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}
