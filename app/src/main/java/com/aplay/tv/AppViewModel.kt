package com.aplay.tv

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.UnknownHostException
import java.util.UUID

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val store = Store(application)
    private var job: Job? = null
    private var addJob: Job? = null

    /** Link shown in the "Add playlist" window. Null while the window is closed. */
    var addLink by mutableStateOf<String?>(null)
        private set
    var editing by mutableStateOf<Playlist?>(null)
        private set
    var checking by mutableStateOf(false)
        private set
    var addError by mutableStateOf<String?>(null)
        private set

    val player = PlayerController(application, store)
    var fullscreen by mutableStateOf(false)

    /** Set while a system screen such as the file picker opens, so no floating window appears. */
    var skipPictureInPicture = false

    /** Every channel from every enabled playlist, in the user's order. */
    var channels by mutableStateOf(emptyList<Channel>())
        private set
    /** Channels of each playlist, by playlist key. */
    var sourceChannels by mutableStateOf(emptyMap<String, List<Channel>>())
        private set
    /** All playlists and files, for the server list. */
    var servers by mutableStateOf(emptyList<Server>())
        private set
    /** Key of the playlist chosen in the server list. Empty means all servers. */
    var selectedServer by mutableStateOf(store.selectedServer)
        private set
    var matches by mutableStateOf(emptyList<Match>())
        private set
    var loading by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)

    var playlists by mutableStateOf(store.playlists)
        private set
    var favorites by mutableStateOf(store.favorites)
        private set
    var matchesUrl by mutableStateOf(store.matchesUrl)
        private set
    var pictureInPicture by mutableStateOf(store.pictureInPicture)
        private set
    var dataSaver by mutableStateOf(store.dataSaver)
        private set
    var softwareDecoder by mutableStateOf(store.softwareDecoder)
        private set

    private var lastLoaded = 0L

    init {
        refresh()
        // Playlists are checked again now and then, so edits to an online playlist appear by themselves.
        viewModelScope.launch {
            while (true) {
                delay(AUTO_REFRESH_MS)
                if (!loading) refresh(silent = true)
            }
        }
    }

    /** Called when the app comes back to the screen. Reloads if the lists are a bit old. */
    fun refreshIfStale() {
        if (!loading && System.currentTimeMillis() - lastLoaded > STALE_MS) refresh(silent = true)
    }

    /** Reloads playlists and matches. A silent reload shows no progress bar and no error message. */
    fun refresh(silent: Boolean = false) {
        job?.cancel()
        job = viewModelScope.launch {
            if (!silent) loading = true
            val errors = mutableListOf<String>()
            val loaded = withContext(Dispatchers.IO) { loadChannels(errors) }
            ensureActive()
            val newChannels = orderChannels(loaded.flatMap { it.channels }.distinctBy { it.id }, store.channelOrder)
            if (newChannels != channels) channels = newChannels
            sourceChannels = loaded.associate { it.key to it.channels }
            servers = loaded.map { item ->
                Server(
                    key = item.key,
                    name = item.name,
                    count = item.channels.size,
                    enabled = true,
                    playlist = playlists.firstOrNull { it.source == item.key },
                )
            } + playlists.filter { p -> !p.enabled && loaded.none { it.key == p.source } }
                .map { Server(it.source, it.name, 0, false, it) }
            player.updateQueue(channels)
            val newMatches = withContext(Dispatchers.IO) { loadMatches(errors) }
            ensureActive()
            if (newMatches != matches) matches = newMatches
            loading = false
            lastLoaded = System.currentTimeMillis()
            if (errors.isNotEmpty() && !silent) message = errors.joinToString("\n")
        }
    }

    /** Chooses which playlist the channel list shows. An empty key shows all of them. */
    fun selectServer(key: String) {
        selectedServer = key
        store.selectedServer = key
    }

    /** Channels of the chosen server, or all channels if none is chosen (or it is gone). */
    fun serverChannels(): List<Channel> {
        val own = sourceChannels[selectedServer] ?: return channels
        val rank = channels.withIndex().associate { it.value.id to it.index }
        return own.sortedBy { rank[it.id] ?: Int.MAX_VALUE }
    }

    private class Loaded(val key: String, val name: String, val channels: List<Channel>)

    private fun loadChannels(errors: MutableList<String>): List<Loaded> {
        val sources = buildList {
            if (Config.DEFAULT_PLAYLIST_URL.isNotBlank()) {
                add("Main playlist" to Config.DEFAULT_PLAYLIST_URL)
            }
            addAll(folderPlaylists())
            playlists.filter { it.enabled }.forEach { add(it.name to it.source) }
        }
        if (sources.isEmpty()) {
            val demo = M3uParser.parse(asset("demo/channels.m3u")).map { it.copy(source = "demo") }
            return listOf(Loaded("demo", "Demo", demo))
        }

        return sources.distinctBy { it.second }.map { (name, source) ->
            val list = try {
                M3uParser.load(read(source), name, source).also {
                    if (it.isEmpty()) errors += "$name: no channels found"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errors += "$name: ${e.message ?: "could not load"}"
                emptyList()
            }
            Loaded(source, name, list.distinctBy { it.id }.map { it.copy(source = source) })
        }
    }

    private fun loadMatches(errors: MutableList<String>): List<Match> {
        val url = matchesUrl.ifBlank { Config.DEFAULT_MATCHES_URL }
        val now = System.currentTimeMillis()
        return try {
            parseMatches(if (url.isBlank()) asset("demo/matches.json") else read(url), now)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errors += "Matches: ${e.message ?: "could not load"}"
            emptyList()
        }
    }

    /** Playlists from the project's "playlists" folder: links.txt and any .m3u files. */
    private fun folderPlaylists(): List<Pair<String, String>> {
        val files = getApplication<Application>().assets.list("").orEmpty().sorted()
        val links = if ("links.txt" in files) parsePlaylistLinks(asset("links.txt")) else emptyList()
        val lists = files
            .filter { it.endsWith(".m3u", ignoreCase = true) || it.endsWith(".m3u8", ignoreCase = true) }
            .map { it.substringBeforeLast('.') to "asset:$it" }
        return links + lists
    }

    /** Reads a source; web links are saved so they still open when the network is down. */
    private fun read(source: String): String {
        val app = getApplication<Application>()
        if (source.startsWith("asset:")) return asset(source.removePrefix("asset:"))
        if (source.startsWith("file:")) {
            return File(app.filesDir, source.removePrefix("file:")).readText()
        }
        val cache = File(app.filesDir, "cache_" + UUID.nameUUIDFromBytes(source.toByteArray()))
        return try {
            Net.fetch(source).also { cache.writeText(it) }
        } catch (e: Exception) {
            if (cache.exists()) cache.readText() else throw e
        }
    }

    private fun asset(name: String): String =
        getApplication<Application>().assets.open(name).bufferedReader().use { it.readText() }

    fun showAddPlaylist(link: String = "") {
        dismissPlaylistWindow()
        addLink = link
    }

    fun showEditPlaylist(playlist: Playlist) {
        dismissPlaylistWindow()
        editing = playlist
        addLink = if (playlist.source.startsWith("file:")) "" else playlist.source
    }

    fun dismissPlaylistWindow() {
        addJob?.cancel()
        addLink = null
        editing = null
        checking = false
        addError = null
    }

    /** Adds a new link, or saves changes to the playlist being edited. The link is tested first. */
    fun savePlaylist(name: String, input: String) {
        val old = editing
        if (old != null && old.source.startsWith("file:")) {
            replacePlaylist(old.copy(name = name.trim().ifEmpty { old.name }))
            return
        }
        val link = M3uParser.cleanLink(input)
        if (link == null) {
            addError = "Enter a web link that starts with http:// or https://"
            return
        }
        if (playlists.any { it.source == link && it.id != old?.id }) {
            addError = "This link is already added"
            return
        }
        val title = name.trim().ifEmpty { old?.name ?: "Playlist ${playlists.size + 1}" }
        if (old != null && old.source == link) {
            replacePlaylist(old.copy(name = title))
            return
        }
        addJob?.cancel()
        addJob = viewModelScope.launch {
            checking = true
            addError = null
            try {
                val count = withContext(Dispatchers.IO) {
                    M3uParser.load(read(link), title, link).size
                }
                if (count == 0) {
                    addError = "No channels found. Check that this is an M3U playlist link."
                } else {
                    val playlist = Playlist(old?.id ?: UUID.randomUUID().toString(), title, link)
                    message = "$title: $count channels added"
                    replacePlaylist(playlist)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnknownHostException) {
                addError = "Cannot reach that address. Check the link and your internet."
            } catch (e: Exception) {
                addError = "Could not load the link: ${e.message ?: "unknown problem"}"
            }
            checking = false
        }
    }

    fun importPlaylist(uri: Uri) {
        addJob?.cancel()
        addJob = viewModelScope.launch {
            val app = getApplication<Application>()
            val id = UUID.randomUUID().toString()
            val fileName = "playlist_$id.m3u"
            checking = true
            addError = null
            try {
                val (title, count) = withContext(Dispatchers.IO) {
                    val text = app.contentResolver.openInputStream(uri)?.use { Net.readLimited(it) }
                        ?: throw IOException("The file could not be opened")
                    val found = M3uParser.parse(text).size
                    if (found > 0) File(app.filesDir, fileName).writeText(text)
                    displayName(uri) to found
                }
                if (count == 0) {
                    fail("No channels found in that file")
                } else {
                    val name = title.ifEmpty { "Playlist ${playlists.size + 1}" }
                    message = "$name: $count channels added"
                    replacePlaylist(Playlist(id, name, "file:$fileName"))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fail("Could not read that file")
            }
            checking = false
        }
    }

    /** Shows a problem inside the add window if it is open, otherwise as a short message. */
    private fun fail(text: String) {
        if (addLink != null) addError = text else message = text
    }

    private fun displayName(uri: Uri): String = try {
        getApplication<Application>().contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null }
            .orEmpty()
            .substringBeforeLast('.')
    } catch (e: Exception) {
        ""
    }

    private fun replacePlaylist(playlist: Playlist) {
        val before = playlists.firstOrNull { it.id == playlist.id }
        // If the chosen server's link was edited, keep showing it.
        if (before != null && before.source == selectedServer && before.source != playlist.source) {
            selectServer(playlist.source)
        }
        val exists = before != null
        val updated = if (exists) {
            playlists.map { if (it.id == playlist.id) playlist.copy(enabled = it.enabled) else it }
        } else {
            playlists + playlist
        }
        addLink = null
        editing = null
        checking = false
        addError = null
        savePlaylists(updated)
    }

    fun setPlaylistEnabled(id: String, enabled: Boolean) =
        savePlaylists(playlists.map { if (it.id == id) it.copy(enabled = enabled) else it })

    fun removePlaylist(playlist: Playlist) {
        if (playlist.source.startsWith("file:")) {
            File(getApplication<Application>().filesDir, playlist.source.removePrefix("file:")).delete()
        }
        if (selectedServer == playlist.source) selectServer("")
        savePlaylists(playlists.filterNot { it.id == playlist.id })
    }

    private fun savePlaylists(value: List<Playlist>) {
        playlists = value
        store.playlists = value
        refresh()
    }

    fun moveChannel(firstId: String, secondId: String) {
        channels = swapChannels(channels, firstId, secondId)
        store.channelOrder = channels.map { it.id }
        player.updateQueue(channels)
    }

    fun closePlayer() {
        fullscreen = false
        player.close()
    }

    override fun onCleared() {
        player.close()
    }

    fun toggleFavorite(channel: Channel) {
        favorites = if (channel.id in favorites) favorites - channel.id else favorites + channel.id
        store.favorites = favorites
    }

    fun updateMatchesUrl(url: String) {
        matchesUrl = url.trim()
        store.matchesUrl = matchesUrl
        refresh()
    }

    fun updatePictureInPicture(value: Boolean) {
        pictureInPicture = value
        store.pictureInPicture = value
    }

    fun updateDataSaver(value: Boolean) {
        dataSaver = value
        store.dataSaver = value
    }

    fun updateSoftwareDecoder(value: Boolean) {
        softwareDecoder = value
        store.softwareDecoder = value
    }

    private companion object {
        const val AUTO_REFRESH_MS = 5 * 60_000L
        const val STALE_MS = 60_000L
    }
}
