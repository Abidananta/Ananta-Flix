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
 * and (optionally) loads the XMLTV EPG. Falls back to the bundled
 * sample_playlist.m3u (assets) when the network fetch fails, so the
 * app always opens — even offline.
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
        /** True when the bundled sample was used instead of GitHub. */
        val usedFallback: Boolean,
        val errorMessage: String? = null
    )

    suspend fun loadAll(): LoadResult = withContext(Dispatchers.IO) {
        var usedFallback = false
        var error: String? = null
        val m3uText: String = try {
            downloadText(AppConfig.PLAYLIST_URL)
                .takeIf { it.contains("#EXTINF") }
                ?: throw IllegalStateException("Downloaded file is not an M3U playlist")
        } catch (e: Exception) {
            usedFallback = true
            error = e.message
            context.assets.open("sample_playlist.m3u").bufferedReader().use { it.readText() }
        }

        val channels = M3uParser.parse(m3uText)

        val epg: List<EpgProgram> = if (AppConfig.EPG_URL.isBlank()) {
            emptyList() // EPG disabled — UI shows "No EPG info"
        } else {
            try {
                EpgParser.parse(downloadText(AppConfig.EPG_URL))
            } catch (_: Exception) {
                emptyList()
            }
        }
        LoadResult(channels, epg, usedFallback, error)
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
