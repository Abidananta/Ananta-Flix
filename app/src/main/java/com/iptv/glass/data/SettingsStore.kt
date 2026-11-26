package com.iptv.glass.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "glass_iptv_settings")

/**
 * Playback + playlist settings from the More screen, persisted locally.
 * - Floating Player (PiP): when on, the player offers a picture-in-picture button.
 * - Data Saver: when on, the player caps video quality to SD (lowest rendition).
 * - Player Decoder: "Default" (ExoPlayer). Stored so a future decoder choice has a home.
 * - Custom Playlist URL: overrides AppConfig.PLAYLIST_URL when set; empty = use the default.
 */
class SettingsStore(private val context: Context) {

    companion object {
        private val PIP_KEY = booleanPreferencesKey("pip_enabled")
        private val DATA_SAVER_KEY = booleanPreferencesKey("data_saver_enabled")
        private val DECODER_KEY = stringPreferencesKey("player_decoder")
        private val CUSTOM_PLAYLIST_KEY = stringPreferencesKey("custom_playlist_url")
    }

    val pipEnabledFlow: Flow<Boolean> =
        context.settingsDataStore.data.map { it[PIP_KEY] ?: false }

    val dataSaverFlow: Flow<Boolean> =
        context.settingsDataStore.data.map { it[DATA_SAVER_KEY] ?: false }

    val decoderFlow: Flow<String> =
        context.settingsDataStore.data.map { it[DECODER_KEY] ?: "Default" }

    val customPlaylistUrlFlow: Flow<String> =
        context.settingsDataStore.data.map { it[CUSTOM_PLAYLIST_KEY] ?: "" }

    suspend fun setPipEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[PIP_KEY] = enabled }
    }

    suspend fun setDataSaverEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[DATA_SAVER_KEY] = enabled }
    }

    suspend fun setDecoder(decoder: String) {
        context.settingsDataStore.edit { it[DECODER_KEY] = decoder }
    }

    suspend fun setCustomPlaylistUrl(url: String) {
        context.settingsDataStore.edit { it[CUSTOM_PLAYLIST_KEY] = url.trim() }
    }
}
