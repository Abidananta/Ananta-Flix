package com.iptv.glass.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.iptv.glass.AppConfig
import com.iptv.glass.data.ChannelLogoResolver
import com.iptv.glass.data.EpgParser
import com.iptv.glass.data.FavoritesStore
import com.iptv.glass.data.IptvRepository
import com.iptv.glass.data.SettingsStore
import com.iptv.glass.model.Channel
import com.iptv.glass.model.EpgProgram
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiState(
    val isLoading: Boolean = true,
    val channels: List<Channel> = emptyList(),
    val epg: List<EpgProgram> = emptyList(),
    val usedFallback: Boolean = false,
    val errorMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = IptvRepository(application)
    private val favoritesStore = FavoritesStore(application)
    private val settingsStore = SettingsStore(application)
    private val logoResolver = ChannelLogoResolver(application)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val favorites: StateFlow<Set<String>> = favoritesStore.favoritesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val recentIds: StateFlow<List<String>> = favoritesStore.recentFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Search text typed in the top bar. */
    val searchQuery = MutableStateFlow("")

    /** Selected category chip — "All" means no filter. */
    val selectedCategory = MutableStateFlow("All")

    /** Channel currently playing (drives the "NOW PLAYING CHANNEL" bar). */
    private val _nowPlaying = MutableStateFlow<Channel?>(null)
    val nowPlaying: StateFlow<Channel?> = _nowPlaying.asStateFlow()

    /** A one-off stream opened from More > Network Stream (not in the playlist). */
    private val _networkChannel = MutableStateFlow<Channel?>(null)

    // ------------------------------------------------------------------
    // Shared inline player (Nexa-style).
    //
    // There is exactly ONE ExoPlayer in the whole app, owned here by the
    // ViewModel (which outlives every screen). Tapping a channel plays it
    // inline in the top player area of the Channels screen; the fullscreen
    // PlayerScreen just binds a different PlayerView to this same player,
    // so going fullscreen never re-buffers or creates a second player.
    // The player is created lazily on the first playChannel() call and
    // released only in onCleared().
    // ------------------------------------------------------------------
    private var exoPlayer: ExoPlayer? = null

    /** The shared player, or null before anything has been played. */
    val sharedPlayer: ExoPlayer?
        get() = exoPlayer

    /** Last playback error message, or null while playing fine. */
    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    /** True when the current stream URL's signed token has expired. */
    private val _tokenExpired = MutableStateFlow(false)
    val tokenExpired: StateFlow<Boolean> = _tokenExpired.asStateFlow()

    // --- Settings from the More screen (persisted in SettingsStore) ---
    val pipEnabled: StateFlow<Boolean> = settingsStore.pipEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val dataSaverEnabled: StateFlow<Boolean> = settingsStore.dataSaverFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val decoder: StateFlow<String> = settingsStore.decoderFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Default")
    val customPlaylistUrl: StateFlow<String> = settingsStore.customPlaylistUrlFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    /** Effective playlist URL: the custom one from More, or the GitHub default. */
    fun effectivePlaylistUrl(): String =
        customPlaylistUrl.value.takeIf { it.isNotBlank() } ?: AppConfig.PLAYLIST_URL

    /** All categories from the playlist, in first-seen order, with counts. */
    val categories: StateFlow<List<Pair<String, Int>>> = combine(_uiState) { states ->
        repository.categories(states[0].channels)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Channels after search + category filtering. */
    val filteredChannels: StateFlow<List<Channel>> =
        combine(_uiState, searchQuery, selectedCategory) { state, query, category ->
            state.channels.filter { channel ->
                (category == "All" || channel.category == category) &&
                    (query.isBlank() || channel.name.contains(query, ignoreCase = true) ||
                        channel.category.contains(query, ignoreCase = true))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val override = settingsStore.customPlaylistUrlFlow.first()
            val result = repository.loadAll(override)
            // Show the channel list immediately — logo lookup must never
            // block it. Logos resolve afterwards, in the background.
            _uiState.value = UiState(
                isLoading = false,
                channels = result.channels,
                epg = result.epg,
                usedFallback = result.usedFallback,
                errorMessage = result.errorMessage
            )
            resolveLogosInBackground(result.channels)
        }
    }

    /**
     * Look up internet logos by channel name (iptv-org database) for
     * every channel that has no playlist `tvg-logo`, then re-emit the
     * channel list with the resolved URLs. Because every screen reads
     * its channels from [uiState], the resolved logos appear everywhere
     * at once (Channels, Categories, Favorites, Matches, Detail,
     * Player). Channels with no logo anywhere keep a blank `logoUrl`
     * and the UI shows the monogram tile for them.
     */
    private fun resolveLogosInBackground(channels: List<Channel>) {
        viewModelScope.launch {
            val resolved = logoResolver.resolveAll(channels)
            if (resolved.isEmpty()) return@launch
            val current = _uiState.value.channels
            _uiState.value = _uiState.value.copy(
                channels = current.map { channel ->
                    resolved[channel.id]?.let { channel.copy(logoUrl = it) } ?: channel
                }
            )
        }
    }

    fun liveChannels(): List<Channel> = _uiState.value.channels.filter { it.isLive }

    /** Movies tab: group-title "Movies" (live movie channels in this playlist)
     *  plus any VOD movie files. */
    fun movieChannels(): List<Channel> =
        _uiState.value.channels.filter { it.isMovie || (it.isVod && !it.isSeries) }

    /** Series tab: group-title "Series". Empty for the supplied playlist —
     *  the screen shows an empty state until the playlist gains a Series group. */
    fun seriesChannels(): List<Channel> = _uiState.value.channels.filter { it.isSeries }

    /**
     * Matches screen: channels whose group-title contains "sport".
     * No scores or fixtures are invented — these are simply the live
     * sports channels from the playlist. Empty when the playlist has
     * no Sports group.
     */
    fun sportsChannels(): List<Channel> =
        _uiState.value.channels.filter {
            it.groupTitle.contains("sport", ignoreCase = true) ||
                it.category.contains("sport", ignoreCase = true)
        }

    fun favoriteChannels(): List<Channel> {
        val fav = favorites.value
        return _uiState.value.channels.filter { it.id in fav }
    }

    fun recentChannels(): List<Channel> {
        val byId = _uiState.value.channels.associateBy { it.id }
        return recentIds.value.mapNotNull { byId[it] }
    }

    fun nowProgram(channel: Channel): EpgProgram? =
        EpgParser.currentProgram(_uiState.value.epg, channel.tvgId.ifBlank { channel.id }, channel.name)

    fun nextProgram(channel: Channel): EpgProgram? =
        EpgParser.nextProgram(_uiState.value.epg, channel.tvgId.ifBlank { channel.id }, channel.name)

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch { favoritesStore.toggleFavorite(channel.id) }
    }

    fun markWatched(channel: Channel) {
        viewModelScope.launch { favoritesStore.markWatched(channel.id) }
    }

    /** Called when playback starts — drives the Now Playing bar. */
    fun setNowPlaying(channel: Channel) {
        _nowPlaying.value = channel
        markWatched(channel)
    }

    /**
     * Play a channel INLINE in the Channels screen's top player area.
     * Never navigates anywhere — callers stay on their screen (or go to
     * the Channels tab) and the video starts in the black player area.
     *
     * Token expiry is checked FIRST: the supplied playlist's URLs carry
     * a signed `edge-cache-token` with `Expires=<epoch>` (about 1 hour).
     * Once that time has passed the CDN refuses the stream, so playback
     * is not even attempted — the player area shows the expiry message
     * instead. No header or player setting can play an expired URL.
     */
    @OptIn(UnstableApi::class)
    fun playChannel(channel: Channel) {
        _playbackError.value = null
        _tokenExpired.value = false
        setNowPlaying(channel)

        val uri = runCatching { Uri.parse(channel.streamUrl.trim()) }.getOrNull()
        if (uri == null || (uri.scheme != "http" && uri.scheme != "https") || uri.host.isNullOrBlank()) {
            _playbackError.value = "Invalid stream URL for this channel."
            return
        }
        if (isTokenExpired(channel.streamUrl)) {
            _tokenExpired.value = true
            _playbackError.value =
                "Stream token expired — playlist GitHub e notun token diye update korun"
            runCatching { exoPlayer?.stop() }
            return
        }

        val player = runCatching { ensurePlayer() }.getOrElse {
            _playbackError.value = it.message ?: "Could not start the player."
            return
        }
        val lower = channel.streamUrl.lowercase()
        val mimeType = when {
            lower.contains(".m3u8") -> MimeTypes.APPLICATION_M3U8
            lower.contains(".mp4") -> MimeTypes.VIDEO_MP4
            lower.contains(".mkv") -> MimeTypes.VIDEO_MATROSKA
            lower.contains(".webm") -> MimeTypes.VIDEO_WEBM
            else -> null
        }
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .apply { if (mimeType != null) setMimeType(mimeType) }
            .build()
        runCatching {
            if (dataSaverEnabled.value) {
                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon().setMaxVideoSizeSd().build()
            }
            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = true
        }.onFailure {
            _playbackError.value = it.message ?: "Could not start the player."
        }
    }

    /** Retry the currently playing channel (used by the player's Retry button). */
    fun retryPlayback() {
        _nowPlaying.value?.let { playChannel(it) }
    }

    /** Build the single shared player, once, on the application context. */
    @OptIn(UnstableApi::class)
    private fun ensurePlayer(): ExoPlayer {
        exoPlayer?.let { return it }
        val appContext = getApplication<Application>().applicationContext
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(
                "Mozilla/5.0 (Linux; Android 13; Mobile) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/120.0 Mobile Safari/537.36 AFLIX/1.0"
            )
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(15_000)
        val player = ExoPlayer.Builder(appContext)
            .setMediaSourceFactory(DefaultMediaSourceFactory(appContext).setDataSourceFactory(httpFactory))
            .build()
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                // A source error on an already-expired token is reported
                // as expiry, not as a generic failure.
                if (isTokenExpired(_nowPlaying.value?.streamUrl.orEmpty())) {
                    _tokenExpired.value = true
                    _playbackError.value =
                        "Stream token expired — playlist GitHub e notun token diye update korun"
                } else {
                    _playbackError.value = error.message ?: "Source error"
                }
            }
        })
        exoPlayer = player
        return player
    }

    /**
     * Signed-token expiry (seconds since epoch) parsed from a stream URL,
     * e.g. `...?edge-cache-token=...&Expires=1795703429` (verified live:
     * every URL in the supplied playlist shares Expires=1795703429,
     * 2026-11-26 14:30 UTC — already past at the time of writing).
     * Null when the URL carries no expiry.
     */
    fun tokenExpiryMillis(url: String): Long? {
        val match = TOKEN_EXPIRY_REGEX.find(url) ?: return null
        val seconds = match.groupValues[1].toLongOrNull() ?: return null
        return seconds * 1000L
    }

    /** True when the URL's signed token exists and is already in the past. */
    fun isTokenExpired(url: String): Boolean {
        val expiry = tokenExpiryMillis(url) ?: return false
        return expiry < System.currentTimeMillis()
    }

    /** More > Network Stream: play an arbitrary stream URL the user pasted, inline. */
    fun playNetworkStream(url: String): Channel {
        val trimmed = url.trim()
        val channel = Channel(
            id = "network-${trimmed.hashCode()}",
            name = "Network Stream",
            streamUrl = trimmed,
            groupTitle = "Network"
        )
        _networkChannel.value = channel
        playChannel(channel)
        return channel
    }

    fun setPipEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setPipEnabled(enabled) }
    }

    fun setDataSaverEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setDataSaverEnabled(enabled)
            // Apply immediately to the shared player if it is playing.
            exoPlayer?.let { player ->
                player.trackSelectionParameters = if (enabled) {
                    player.trackSelectionParameters.buildUpon().setMaxVideoSizeSd().build()
                } else {
                    player.trackSelectionParameters.buildUpon().clearVideoSizeConstraints().build()
                }
            }
        }
    }

    override fun onCleared() {
        runCatching { exoPlayer?.release() }
        exoPlayer = null
        super.onCleared()
    }

    companion object {
        /** `Expires=<epoch seconds>` (also matches `exp=`) in a signed stream URL. */
        private val TOKEN_EXPIRY_REGEX = Regex("(?i)(?:expires|exp)=(\\d{10})")
    }

    fun setCustomPlaylistUrl(url: String) {
        viewModelScope.launch {
            settingsStore.setCustomPlaylistUrl(url)
            refresh()
        }
    }

    fun findChannel(id: String): Channel? =
        _networkChannel.value?.takeIf { it.id == id }
            ?: _uiState.value.channels.firstOrNull { it.id == id }
}
