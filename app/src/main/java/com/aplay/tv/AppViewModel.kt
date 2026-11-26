package com.aplay.tv

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val store = Store(application)
    private var job: Job? = null

    var channels by mutableStateOf(emptyList<Channel>())
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

    init {
        refresh()
    }

    fun refresh() {
        job?.cancel()
        job = viewModelScope.launch {
            loading = true
            val errors = mutableListOf<String>()
            val newChannels = withContext(Dispatchers.IO) { loadChannels(errors) }
            ensureActive()
            channels = newChannels
            val newMatches = withContext(Dispatchers.IO) { loadMatches(errors) }
            ensureActive()
            matches = newMatches
            loading = false
            if (errors.isNotEmpty()) message = errors.joinToString("\n")
        }
    }

    private fun loadChannels(errors: MutableList<String>): List<Channel> {
        val sources = buildList {
            if (Config.DEFAULT_PLAYLIST_URL.isNotBlank()) {
                add("Main playlist" to Config.DEFAULT_PLAYLIST_URL)
            }
            playlists.filter { it.enabled }.forEach { add(it.name to it.source) }
        }
        if (sources.isEmpty()) return M3uParser.parse(asset("channels.m3u"))

        return sources.flatMap { (name, source) ->
            try {
                val list = M3uParser.parse(read(source))
                if (list.isEmpty()) errors += "$name: no channels found"
                list
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errors += "$name: ${e.message ?: "could not load"}"
                emptyList()
            }
        }.distinctBy { it.id }
    }

    private fun loadMatches(errors: MutableList<String>): List<Match> {
        val url = matchesUrl.ifBlank { Config.DEFAULT_MATCHES_URL }
        val now = System.currentTimeMillis()
        return try {
            parseMatches(if (url.isBlank()) asset("matches.json") else read(url), now)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errors += "Matches: ${e.message ?: "could not load"}"
            emptyList()
        }
    }

    /** Reads a source; web links are saved so they still open when the network is down. */
    private fun read(source: String): String {
        val app = getApplication<Application>()
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

    fun addPlaylist(name: String, url: String) {
        val id = UUID.randomUUID().toString()
        savePlaylists(playlists + Playlist(id, name.ifBlank { "My playlist" }, url.trim()))
    }

    fun importPlaylist(uri: Uri) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val id = UUID.randomUUID().toString()
            val fileName = "playlist_$id.m3u"
            val copied = withContext(Dispatchers.IO) {
                try {
                    app.contentResolver.openInputStream(uri)?.use { input ->
                        File(app.filesDir, fileName).outputStream().use { input.copyTo(it) }
                    } != null
                } catch (e: Exception) {
                    false
                }
            }
            if (copied) {
                val number = playlists.size + 1
                savePlaylists(playlists + Playlist(id, "Imported file $number", "file:$fileName"))
            } else {
                message = "Could not read that file"
            }
        }
    }

    fun setPlaylistEnabled(id: String, enabled: Boolean) =
        savePlaylists(playlists.map { if (it.id == id) it.copy(enabled = enabled) else it })

    fun removePlaylist(playlist: Playlist) {
        if (playlist.source.startsWith("file:")) {
            File(getApplication<Application>().filesDir, playlist.source.removePrefix("file:")).delete()
        }
        savePlaylists(playlists.filterNot { it.id == playlist.id })
    }

    private fun savePlaylists(value: List<Playlist>) {
        playlists = value
        store.playlists = value
        refresh()
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
}
