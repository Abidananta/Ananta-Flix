package com.iptv.glass.data

import android.content.Context
import com.iptv.glass.AppConfig
import com.iptv.glass.model.Channel
import com.iptv.glass.model.EpgProgram
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Loads the playlist from the GitHub Raw URL in AppConfig, parses it,
 * and (optionally) loads the XMLTV EPG. There is NO sample playlist
 * fallback: if the download fails, the app shows an empty list with
 * the error message, so the user knows to check the playlist URL.
 */
class IptvRepository(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(AppConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(AppConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    data class LoadResult(
        val channels: List<Channel>,
        val epg: List<EpgProgram>,
        /** Kept for compatibility; always false — no sample fallback. */
        val usedFallback: Boolean = false,
        val errorMessage: String? = null
    )

    /**
     * [playlistUrlOverride] — when non-blank (set from More > Custom Playlists),
     * that URL is fetched instead of [AppConfig.PLAYLIST_URL].
     */
    suspend fun loadAll(playlistUrlOverride: String? = null): LoadResult = withContext(Dispatchers.IO) {
        var error: String? = null
        val playlistUrl = playlistUrlOverride?.takeIf { it.isNotBlank() } ?: AppConfig.PLAYLIST_URL
        val m3uText: String? = try {
            downloadText(playlistUrl)
                .takeIf { it.contains("#EXTINF") }
                ?: throw IllegalStateException("Downloaded file is not an M3U playlist")
        } catch (e: Exception) {
            error = e.message
            null
        }

        val channels = if (m3uText == null) emptyList() else M3uParser.parse(m3uText)

        val epg: List<EpgProgram> = if (AppConfig.EPG_URL.isBlank()) {
            emptyList() // EPG disabled — UI shows "No EPG info"
        } else {
            try {
                EpgParser.parse(downloadText(AppConfig.EPG_URL))
            } catch (_: Exception) {
                emptyList()
            }
        }
        LoadResult(channels, epg, usedFallback = false, errorMessage = error)
    }

    /** Distinct categories (group-title values) in first-seen order, with counts. */
    fun categories(channels: List<Channel>): List<Pair<String, Int>> =
        channels.groupingBy { it.category }.eachCount().toList()
            .sortedByDescending { it.second }

    private fun downloadText(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "GlassIPTV/1.0 (Android)")
            .header("Accept", "*/*")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")
            return response.body?.string().orEmpty()
        }
    }
}
