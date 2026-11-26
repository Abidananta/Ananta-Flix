package com.iptv.glass.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.glass.data.EpgParser
import com.iptv.glass.data.FavoritesStore
import com.iptv.glass.data.IptvRepository
import com.iptv.glass.model.Channel
import com.iptv.glass.model.EpgProgram
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
            val result = repository.loadAll()
            _uiState.value = UiState(
                isLoading = false,
                channels = result.channels,
                epg = result.epg,
                usedFallback = result.usedFallback,
                errorMessage = result.errorMessage
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

    fun findChannel(id: String): Channel? = _uiState.value.channels.firstOrNull { it.id == id }
}
