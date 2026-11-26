package com.iptv.glass.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

    /** More > Network Stream: play an arbitrary stream URL the user pasted. */
    fun playNetworkStream(url: String): Channel {
        val trimmed = url.trim()
        val channel = Channel(
            id = "network-${trimmed.hashCode()}",
            name = "Network Stream",
            streamUrl = trimmed,
            groupTitle = "Network"
        )
        _networkChannel.value = channel
        _nowPlaying.value = channel
        return channel
    }

    fun setPipEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setPipEnabled(enabled) }
    }

    fun setDataSaverEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setDataSaverEnabled(enabled) }
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
