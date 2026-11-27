package com.aplay.tv

import org.json.JSONObject
import android.content.res.Configuration
import android.content.Context
import android.app.UiModeManager
import com.aplay.tv.ui.setAccent
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.UnknownHostException
import org.json.JSONArray
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

    /** True when the app runs on an Android TV. */
    val isTv: Boolean = (getApplication<Application>().getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager)
        ?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION

    /** A TV works only after a phone has sent the code shown on it. Phones are always active. */
    var tvActive by mutableStateOf(!isTv || store.tvActive)
        private set
    val tvCode: String = if (isTv) store.tvCode.ifEmpty { TvPairing.newCode().also { store.tvCode = it } } else ""
    private var tvJob: Job? = null

    /** TV: waits for the phone. When its message arrives the TV is activated. */
    fun startTvPairing() {
        if (!isTv || tvActive || tvJob?.isActive == true) return
        tvJob = viewModelScope.launch {
            var since = "12h"
            while (!tvActive) {
                val received = TvPairing.waitForMessage(tvCode, since)
                since = received.id.ifEmpty { since }
                if (applyTvMessage(received.text)) break
            }
        }
    }

    private fun applyTvMessage(text: String): Boolean {
        return try {
            val data = JSONObject(text)
            if (data.optString("app") != "aplay") return false
            val merged = playlists.toMutableList()
            val incoming = data.optJSONArray("playlists")
            for (i in 0 until (incoming?.length() ?: 0)) {
                val item = incoming?.optJSONObject(i) ?: continue
                val source = item.optString("source")
                if (source.isNotBlank() && merged.none { it.source == source }) {
                    merged += Playlist(UUID.randomUUID().toString(), item.optString("name").ifBlank { "Playlist" }, source)
                }
            }
            playlists = merged
            store.playlists = merged
            data.optString("matches").takeIf { it.isNotBlank() }?.let {
                matchesUrl = it
                store.matchesUrl = it
            }
            if (data.has("accent")) updateAccent(Color(data.getInt("accent")))
            store.tvActive = true
            tvActive = true
            refresh()
            true
        } catch (e: org.json.JSONException) {
            false
        }
    }

    /** Phone: sends the playlists and settings to the TV that shows [rawCode]. */
    fun sendToTv(rawCode: String) {
        val code = TvPairing.clean(rawCode)
        if (code.length != TvPairing.CODE_LENGTH) {
            message = "Type the ${TvPairing.CODE_LENGTH} letters and numbers shown on the TV"
            return
        }
        val list = org.json.JSONArray()
        playlists.filter { it.enabled && it.source.startsWith("http") }.forEach {
            list.put(JSONObject().put("name", it.name).put("source", it.source))
        }
        val text = JSONObject()
            .put("app", "aplay")
            .put("playlists", list)
            .put("matches", matchesUrl)
            .put("accent", accent.toArgb())
            .toString()
        if (text.toByteArray().size > TvPairing.MAX_MESSAGE) {
            message = "Too many playlists to send to the TV. Remove some and try again."
            return
        }
        viewModelScope.launch {
            message = if (TvPairing.send(code, text)) "Sent. Your TV is now active."
            else "Could not reach the TV service. Check the internet."
        }
    }

    /** TV: asks for a new code, so another phone can activate it. */
    fun deactivateTv() {
        store.tvActive = false
        store.tvCode = ""
        message = "TV deactivated. Restart the app to get a new code."
        tvActive = false
    }

    var accent by mutableStateOf(Color(store.accent))
        private set

    fun updateAccent(color: Color) {
        accent = color
        setAccent(color)
        store.accent = color.toArgb()
    }

    init {
        setAccent(accent)
        // First show the saved copies (instant), then check the internet.
        job = viewModelScope.launch {
            loading = true
            reload(silent = true, savedOnly = true)
            reload(silent = false, savedOnly = false)
        }
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
        job = viewModelScope.launch { reload(silent, savedOnly = false) }
    }

    /** True while the saved copy of a link is preferred over the internet. */
    @Volatile
    private var savedOnly = false

    private suspend fun reload(silent: Boolean, savedOnly: Boolean) {
        try {
            reloadUnsafe(silent, savedOnly)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // A bad playlist or file must never close the app.
            loading = false
            if (!silent) message = "Could not load: ${e.javaClass.simpleName} ${e.message.orEmpty()}".trim()
        }
    }

    private suspend fun reloadUnsafe(silent: Boolean, savedOnly: Boolean) {
        this.savedOnly = savedOnly
        if (!silent) loading = true
        val errors = mutableListOf<String>()
        val files = withContext(Dispatchers.IO) { folderFiles() }
        val loaded = withContext(Dispatchers.IO) { loadChannels(errors, files) }
        val newChannels = orderChannels(loaded.flatMap { it.channels }.distinctBy { it.id }, store.channelOrder)
        if (newChannels != channels) channels = newChannels
        val newSources = loaded.associate { it.key to it.channels }
        if (newSources != sourceChannels) sourceChannels = newSources
        val newServers = loaded.map { item ->
            Server(
                key = item.key,
                name = item.name,
                count = item.channels.size,
                enabled = true,
                playlist = playlists.firstOrNull { it.source == item.key },
            )
        } + playlists.filter { p -> !p.enabled && loaded.none { it.key == p.source } }
            .map { Server(it.source, it.name, 0, false, it) }
        if (newServers != servers) servers = newServers
        player.updateQueue(channels)
        val newMatches = withContext(Dispatchers.IO) { loadMatches(errors, files) }
        if (newMatches != matches) matches = newMatches
        if (!savedOnly) {
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

    private suspend fun loadChannels(errors: MutableList<String>, files: List<FolderFile>): List<Loaded> {
        val sources = buildList {
            if (Config.DEFAULT_PLAYLIST_URL.isNotBlank()) {
                add("Main playlist" to Config.DEFAULT_PLAYLIST_URL)
            }
            addAll(folderPlaylists(files))
            playlists.filter { it.enabled }.forEach { add(it.name to it.source) }
        }
        if (sources.isEmpty()) {
            val demo = M3uParser.parse(asset("demo/channels.m3u")).map { it.copy(source = "demo") }
            return listOf(Loaded("demo", "Demo", demo))
        }

        // All playlists are downloaded at the same time.
        val results = coroutineScope {
            sources.distinctBy { it.second }.map { (name, source) ->
                async(Dispatchers.IO) {
                    var problem: String? = null
                    val list = try {
                        loadSource(name, source).also {
                            if (it.isEmpty()) problem = "$name: no channels found"
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        problem = "$name: ${e.message ?: "could not load"}"
                        emptyList()
                    }
                    Loaded(source, name, list.distinctBy { it.id }.map { it.copy(source = source) }) to problem
                }
            }.awaitAll()
        }
        results.forEach { (_, problem) -> if (problem != null) errors += problem }
        return results.map { it.first }
    }

    /**
     * Matches come from every source together: the links saved in More > Matches source
     * (one per line), matches.txt and any .json file in the playlists folder.
     */
    private fun loadMatches(errors: MutableList<String>, files: List<FolderFile>): List<Match> {
        val now = System.currentTimeMillis()
        val sources = mutableListOf<Pair<String, String>>()
        matchesUrl.split(Regex("\\s+")).filter { it.isNotBlank() }.forEach {
            sources += "Matches" to (M3uParser.cleanLink(it) ?: it)
        }
        if (Config.DEFAULT_MATCHES_URL.isNotBlank()) sources += "Matches" to Config.DEFAULT_MATCHES_URL
        for (file in files) {
            val name = file.path.substringAfterLast('/')
            try {
                when {
                    name.equals("matches.txt", ignoreCase = true) ->
                        parsePlaylistLinks(readFile(file)).forEach { (n, l) ->
                            sources += n to (M3uParser.cleanLink(l) ?: l)
                        }
                    name.endsWith(".json", ignoreCase = true) ->
                        sources += name.substringBeforeLast('.') to file.source
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errors += "$name: ${e.message ?: "could not load"}"
            }
        }
        val unique = sources.distinctBy { it.second }
        if (unique.isEmpty()) return parseMatches(asset("demo/matches.json"), now)
        return unique.flatMap { (name, source) ->
            try {
                parseMatches(read(source), now)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errors += "$name: ${e.message ?: "could not load"}"
                emptyList()
            }
        }.distinct().sortedBy { it.startMillis }
    }

    /**
     * Loads one playlist. If it holds no channels but lists links to other playlists
     * (or to single streams), those links are opened too.
     */
    private fun loadSource(name: String, source: String, depth: Int = 0): List<Channel> {
        val text = read(source)
        val direct = M3uParser.load(text, name, source)
        if (direct.isNotEmpty() || depth >= 2) return direct
        val result = mutableListOf<Channel>()
        for ((linkName, raw) in parsePlaylistLinks(text)) {
            val link = M3uParser.cleanLink(raw) ?: continue
            if (M3uParser.isDirectStream(link)) {
                result += Channel(linkName, "", name, Stream(linkName, link))
            } else {
                result += try {
                    loadSource(linkName, link, depth + 1)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }
        return result
    }

    /** Text of links files already read in this reload, so each is downloaded once. */
    private val fileText = java.util.concurrent.ConcurrentHashMap<String, String>()

    private fun readFile(file: FolderFile): String = fileText[file.source] ?: read(file.source)

    /** A file of the playlists folder. [source] can be opened with [read]. */
    private class FolderFile(val path: String, val source: String)

    /** Files of the folder on GitHub, so edits there are seen at once. Throws if GitHub cannot be read. */
    private fun remoteFiles(dir: String, repo: String, branch: String, depth: Int = 0): List<FolderFile> {
        val array = JSONArray(read("https://api.github.com/repos/$repo/contents/$dir?ref=$branch"))
        val result = mutableListOf<FolderFile>()
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val name = item.getString("name")
            val path = item.getString("path")
            when (item.getString("type")) {
                "file" -> {
                    val url = item.optString("download_url")
                    if (url.isNotBlank() && !name.startsWith(".") && !name.startsWith("_")) {
                        result += FolderFile(path.removePrefix(Config.PLAYLIST_FOLDER + "/"), url)
                    }
                }
                "dir" -> if (depth < 2) result += remoteFiles(path, repo, branch, depth + 1)
            }
        }
        return result
    }

    /** All files inside the bundled "playlists" folder, including subfolders. */
    private fun assetFiles(dir: String = ""): List<String> {
        val assets = getApplication<Application>().assets
        return assets.list(dir).orEmpty().sorted().flatMap { entry ->
            val path = if (dir.isEmpty()) entry else "$dir/$entry"
            if (dir.isEmpty() && entry in setOf("demo", "images", "sounds", "webkit")) return@flatMap emptyList()
            if (assets.list(path).orEmpty().isNotEmpty()) assetFiles(path) else listOf(path)
        }
    }

    /**
     * The playlists folder. It is read from GitHub when the app knows the repository, and the
     * copy packed into the APK is used for anything GitHub cannot give.
     *
     * links.txt and matches.txt are fetched straight from the raw file address, which has no
     * request limit. The folder listing (for .m3u and .json files) uses GitHub's API, which only
     * allows a few requests an hour per network, so if it fails the packed files are used.
     */
    private fun folderFiles(): List<FolderFile> {
        fileText.clear()
        val repo = Config.PLAYLIST_REPO.ifBlank { BuildConfig.REPO }
        val branch = Config.PLAYLIST_BRANCH.ifBlank { BuildConfig.BRANCH.ifBlank { "main" } }
        val packed = assetFiles().map { FolderFile(it, "asset:$it") }
        if ('/' !in repo) return packed

        val raw = "https://raw.githubusercontent.com/$repo/$branch/${Config.PLAYLIST_FOLDER}/"
        val links = listOf("links.txt", "matches.txt").map { FolderFile(it, raw + it) }
        val others = try {
            remoteFiles(Config.PLAYLIST_FOLDER, repo, branch)
                .filter { !it.path.equals("links.txt", true) && !it.path.equals("matches.txt", true) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            packed.filter {
                !it.path.equals("links.txt", true) && !it.path.equals("matches.txt", true)
            }
        }
        // A links file that cannot be read (not found, no internet) falls back to the packed one.
        val usable = links.filter { link ->
            try {
                fileText[link.source] = read(link.source)
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
        }
        val missing = links.filter { it !in usable }
            .mapNotNull { m -> packed.firstOrNull { it.path.equals(m.path, true) } }
        return usable + missing + others
    }

    /**
     * Playlists from the folder: every link in links.txt, and every .m3u, .m3u8 or .txt file
     * (also inside subfolders) as its own playlist.
     */
    private fun folderPlaylists(files: List<FolderFile>): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        for (file in files) {
            val name = file.path.substringAfterLast('/')
            val ext = name.substringAfterLast('.', "").lowercase()
            try {
                when {
                    name.equals("links.txt", ignoreCase = true) ->
                        parsePlaylistLinks(readFile(file)).forEach { (n, l) ->
                            result += n to (M3uParser.cleanLink(l) ?: l)
                        }
                    name.equals("matches.txt", ignoreCase = true) -> Unit
                    ext == "m3u" || ext == "m3u8" || ext == "txt" ->
                        result += name.substringBeforeLast('.') to file.source
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // An unreadable links file is skipped.
            }
        }
        return result
    }

    /** Reads a source; web links are saved so they still open when the network is down. */
    private fun read(source: String): String {
        val app = getApplication<Application>()
        if (source.startsWith("asset:")) return asset(source.removePrefix("asset:"))
        if (source.startsWith("file:")) {
            return File(app.filesDir, source.removePrefix("file:")).readText()
        }
        val cache = File(app.filesDir, "cache_" + UUID.nameUUIDFromBytes(source.toByteArray()))
        if (savedOnly && cache.exists()) return cache.readText()
        // GitHub's raw links are cached for minutes; a changing tail makes edits show up at once.
        val address = if (source.contains("raw.githubusercontent.com")) {
            source + (if ('?' in source) "&" else "?") + "t=" + System.currentTimeMillis() / 30_000
        } else source
        return try {
            Net.fetch(address).also { cache.writeText(it) }
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
                    loadSource(title, link).size
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
                    val found = M3uParser.parse(text).size + parsePlaylistLinks(text).size
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
