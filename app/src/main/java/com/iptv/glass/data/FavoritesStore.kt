package com.iptv.glass.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "glass_iptv_prefs")

/** Favourites + recently-watched channel ids, persisted locally with DataStore. */
class FavoritesStore(private val context: Context) {

    companion object {
        private val FAVORITES_KEY = stringSetPreferencesKey("favorite_channel_ids")
        private val RECENT_KEY = stringSetPreferencesKey("recent_channel_ids")
        private const val MAX_RECENT = 12
    }

    val favoritesFlow: Flow<Set<String>> =
        context.dataStore.data.map { it[FAVORITES_KEY] ?: emptySet() }

    val recentFlow: Flow<List<String>> =
        context.dataStore.data.map { (it[RECENT_KEY] ?: emptySet()).toList() }

    suspend fun isFavorite(channelId: String): Boolean =
        context.dataStore.data.first()[FAVORITES_KEY]?.contains(channelId) == true

    suspend fun toggleFavorite(channelId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[FAVORITES_KEY] ?: emptySet()
            prefs[FAVORITES_KEY] =
                if (channelId in current) current - channelId else current + channelId
        }
    }

    /** Most-recent first, capped at [MAX_RECENT]. */
    suspend fun markWatched(channelId: String) {
        context.dataStore.edit { prefs ->
            val current = (prefs[RECENT_KEY] ?: emptySet()).toMutableList()
            current.remove(channelId)
            current.add(0, channelId)
            prefs[RECENT_KEY] = current.take(MAX_RECENT).toSet()
        }
    }
}
