package com.iptv.glass.data

import android.content.Context
import com.iptv.glass.model.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Resolves a channel logo from the internet by channel NAME.
 *
 * Priority:
 *  1. `tvg-logo` from the playlist itself (already on the [Channel]).
 *  2. The public iptv-org database (https://iptv-org.github.io):
 *     - `api/channels.json` — an array of
 *       `{"id": "DeeptoTV.bd", "name": "Deepto TV",
 *       "alt_names": ["দীপ্ত টিভি"], "country": "BD", ...}`.
 *       NOTE: this file has NO logo field — logos live in the second file.
 *     - `api/logos.json` — an array of
 *       `{"channel": "DeeptoTV.bd", "in_use": true, "format": "PNG",
 *       "width": 512, "url": "https://i.imgur.com/…png"}`.
 *     The resolver joins the two by channel id: normalised name (and every
 *     alt_name) -> id -> logo URL. Verified live 2026-11-26: 31,899 channels,
 *     35,725 logos, including Bangladeshi channels (Deepto TV, Channel i,
 *     Independent TV, BTV News, Jamuna TV, Sony PIX, PTV Sports, …).
 *  3. No logo anywhere -> the UI shows a monogram tile (see [monogram] /
 *     [paletteIndex]) instead of a generic icon. That fallback is rendered
 *     by the UI layer, never by this class.
 *
 * Caching: the joined name -> logo map is kept in memory and also saved as
 * one compact JSON file in the app cache dir, with its fetch time in
 * SharedPreferences. It is re-fetched at most once per 24 hours, and
 * never on the main thread. If either API is unreachable the resolver
 * fails gracefully (empty map / null) and the channel list still loads —
 * logo lookup never blocks playlist loading.
 *
 * No logo URLs are hard-coded here and no stream URLs are involved.
 */
class ChannelLogoResolver(
    private val context: Context,
    private val client: OkHttpClient = defaultClient()
) {

    companion object {
        const val CHANNELS_URL = "https://iptv-org.github.io/api/channels.json"
        const val LOGOS_URL = "https://iptv-org.github.io/api/logos.json"

        /** Re-fetch the iptv-org database at most once per day. */
        const val CACHE_MAX_AGE_MILLIS: Long = 24L * 60 * 60 * 1000
        const val CACHE_FILE_NAME = "iptv_org_logo_index.json"
        private const val PREFS_NAME = "channel_logo_resolver"
        private const val KEY_LAST_FETCH_MILLIS = "last_fetch_millis"

        private val QUALITY_TOKENS = setOf("hd", "fhd", "uhd", "sd", "4k", "8k", "plus")

        /**
         * Normalise a channel name for lookup: lowercase, strip quality
         * tokens (HD/SD/4K/…), strip punctuation, collapse whitespace.
         * Bengali letters (U+0980–U+09FF) are kept, so alt_names such as
         * "দীপ্ত টিভি" normalise the same way as Latin names.
         * "Deepto TV HD" -> "deepto tv"; "Sony PIX HD" -> "sony pix".
         */
        fun normalizeName(raw: String): String {
            val lower = raw.lowercase()
            val kept = StringBuilder()
            for (ch in lower) {
                kept.append(
                    when {
                        ch.isLetterOrDigit() -> ch
                        ch in '\u0980'..'\u09FF' -> ch
                        ch == ' ' || ch == '-' || ch == '_' -> ' '
                        else -> ' '
                    }
                )
            }
            val tokens = kept.toString()
                .split(' ')
                .filter { it.isNotBlank() && it !in QUALITY_TOKENS }
            return tokens.joinToString(" ")
        }

        /** Candidate lookup keys for a name, most specific first. */
        fun candidateKeys(name: String): List<String> {
            val full = normalizeName(name)
            if (full.isBlank()) return emptyList()
            val keys = linkedSetOf(full)
            val tokens = full.split(' ')
            // "deepto tv" -> also try "deepto"; "channel i" -> also "i" is too
            // short, so only drop "tv"/"channel" when something remains.
            val withoutTv = tokens.filter { it != "tv" }.joinToString(" ")
            if (withoutTv.isNotBlank() && withoutTv != full) keys += withoutTv
            val withoutChannel = tokens.filter { it != "channel" }.joinToString(" ")
            if (withoutChannel.isNotBlank() && withoutChannel != full) keys += withoutChannel
            val compact = full.replace(" ", "")
            if (compact.isNotBlank() && compact != full) keys += compact
            return keys.toList()
        }

        /**
         * Monogram for the fallback tile: first letter of up to two words.
         * Bengali-aware (first code point of each word). "Deepto TV" -> "DT",
         * "BTV" -> "BT", "Channel i" -> "CI", "" -> "?".
         */
        fun monogram(name: String): String {
            val words = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            if (words.isEmpty()) return "?"
            if (words.size == 1) {
                val w = words[0]
                return w.take(2).uppercase()
            }
            val first = words[0].first().toString()
            val second = words[1].first().toString()
            return (first + second).uppercase()
        }

        /** Stable gradient palette 0..3, picked from the name hash. */
        fun paletteIndex(name: String): Int {
            val hash = normalizeName(name).hashCode()
            return (if (hash == Int.MIN_VALUE) 0 else kotlin.math.abs(hash)) % 4
        }

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    private val mutex = Mutex()

    /** In-memory joined map: normalised name -> logo URL. Null = not loaded yet. */
    @Volatile
    private var index: Map<String, String>? = null

    @Volatile
    private var indexLoadedAtMillis: Long = 0L

    private val cacheFile: File get() = File(context.cacheDir, CACHE_FILE_NAME)

    private fun lastFetchMillis(): Long =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_FETCH_MILLIS, 0L)

    private fun saveLastFetch(millis: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putLong(KEY_LAST_FETCH_MILLIS, millis).apply()
    }

    /**
     * Logo for one channel. Playlist `tvg-logo` wins; otherwise the
     * iptv-org lookup by name (and `tvg-id` when present). Null when no
     * logo is known anywhere — the UI then shows the monogram tile.
     */
    suspend fun resolve(channel: Channel): String? {
        if (channel.logoUrl.isNotBlank()) return channel.logoUrl
        return resolveByName(channel.name, channel.tvgId)
    }

    /** Logo URL for a channel name, or null when unknown. Never throws. */
    suspend fun resolveByName(name: String, tvgId: String = ""): String? {
        if (name.isBlank() && tvgId.isBlank()) return null
        return try {
            ensureIndex()
            val map = index ?: return null
            // A tvg-id like "DeeptoTV.bd" is itself a strong key: the
            // index also stores the id (lowercased) as a key.
            val idKey = tvgId.trim().lowercase()
            if (idKey.isNotBlank()) map[idKey]?.let { return it }
            for (key in candidateKeys(name)) {
                map[key]?.let { return it }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Resolve logos for a whole channel list. Returns channel-id -> logo
     * URL for the channels that had no playlist logo but were found in
     * the iptv-org database. Runs on IO, never blocks list loading —
     * callers show the list first and apply the result afterwards.
     */
    suspend fun resolveAll(channels: List<Channel>): Map<String, String> {
        val out = mutableMapOf<String, String>()
        for (channel in channels) {
            if (channel.logoUrl.isNotBlank()) continue
            resolveByName(channel.name, channel.tvgId)?.let { out[channel.id] = it }
        }
        return out
    }

    /** Load the joined index (memory -> disk cache -> network), at most once a day. */
    private suspend fun ensureIndex() {
        val now = System.currentTimeMillis()
        val current = index
        if (current != null && now - indexLoadedAtMillis < CACHE_MAX_AGE_MILLIS) return
        mutex.withLock {
            val rechecked = index
            if (rechecked != null && now - indexLoadedAtMillis < CACHE_MAX_AGE_MILLIS) return
            // 1) Disk cache, if fresh.
            val diskAgeOk = now - lastFetchMillis() < CACHE_MAX_AGE_MILLIS
            if (diskAgeOk) {
                loadIndexFromDisk()?.let {
                    index = it
                    indexLoadedAtMillis = lastFetchMillis()
                    return
                }
            }
            // 2) Network: fetch both iptv-org files and join them.
            val fresh = fetchIndexFromNetwork()
            if (fresh != null) {
                index = fresh
                indexLoadedAtMillis = now
                saveIndexToDisk(fresh)
                saveLastFetch(now)
            } else if (index == null) {
                // 3) Network failed: use a stale disk copy rather than nothing.
                loadIndexFromDisk()?.let {
                    index = it
                    indexLoadedAtMillis = lastFetchMillis()
                } ?: run { index = emptyMap() }
            }
        }
    }

    // Block body (not an expression body): `return` is used below, and
    // returns are not allowed inside an expression-body function.
    private fun loadIndexFromDisk(): Map<String, String>? {
        return try {
            val file = cacheFile
            if (!file.exists()) return null
            val json = JSONObject(file.readText())
            val map = mutableMapOf<String, String>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = json.optString(key)
            }
            map.ifEmpty { null }
        } catch (_: Exception) {
            null
        }
    }

    private fun saveIndexToDisk(map: Map<String, String>) {
        try {
            val json = JSONObject()
            for ((key, value) in map) json.put(key, value)
            cacheFile.writeText(json.toString())
        } catch (_: Exception) {
            // Disk cache is best-effort only.
        }
    }

    /**
     * Fetch channels.json + logos.json and join them into
     * normalised-name -> logo URL. Countries are preferred in the order
     * BD, IN, then any other, so "BTV" resolves to the Bangladeshi
     * channel rather than a same-named channel elsewhere.
     */
    private suspend fun fetchIndexFromNetwork(): Map<String, String>? =
        withContext(Dispatchers.IO) {
            try {
                val channelsJson = downloadText(CHANNELS_URL)
                val logosJson = downloadText(LOGOS_URL)

                // id -> best logo URL (PNG first, then JPEG, then others;
                // SVG last — Coil has no SVG decoder by default).
                val logoById = mutableMapOf<String, Pair<Int, Int>>() // id -> (score, width)
                val logoUrlById = mutableMapOf<String, String>()
                val logos = JSONArray(logosJson)
                for (i in 0 until logos.length()) {
                    val entry = logos.optJSONObject(i) ?: continue
                    if (!entry.optBoolean("in_use", true)) continue
                    val channelId = entry.optString("channel").trim()
                    val url = entry.optString("url").trim()
                    if (channelId.isBlank() || url.isBlank()) continue
                    val format = entry.optString("format").uppercase()
                    val score = when (format) {
                        "PNG" -> 3
                        "JPEG", "JPG" -> 2
                        "WEBP" -> 2
                        "SVG" -> 0
                        else -> 1
                    }
                    val width = entry.optInt("width", 0)
                    val current = logoById[channelId]
                    if (current == null || score > current.first ||
                        (score == current.first && width > current.second)
                    ) {
                        logoById[channelId] = score to width
                        logoUrlById[channelId] = url
                    }
                }

                // normalised name -> channel id, preferring BD, then IN.
                fun countryRank(country: String): Int = when (country.uppercase()) {
                    "BD" -> 0
                    "IN" -> 1
                    else -> 2
                }
                val idByName = mutableMapOf<String, Pair<Int, String>>() // key -> (rank, id)
                fun putNameKey(key: String, id: String, rank: Int) {
                    if (key.isBlank()) return
                    val current = idByName[key]
                    if (current == null || rank < current.first) idByName[key] = rank to id
                }
                val channels = JSONArray(channelsJson)
                for (i in 0 until channels.length()) {
                    val entry = channels.optJSONObject(i) ?: continue
                    val id = entry.optString("id").trim()
                    if (id.isBlank()) continue
                    val rank = countryRank(entry.optString("country"))
                    putNameKey(id.lowercase(), id, rank)
                    for (key in candidateKeys(entry.optString("name"))) putNameKey(key, id, rank)
                    val altNames = entry.optJSONArray("alt_names")
                    if (altNames != null) {
                        for (j in 0 until altNames.length()) {
                            for (key in candidateKeys(altNames.optString(j))) {
                                putNameKey(key, id, rank)
                            }
                        }
                    }
                }

                val joined = mutableMapOf<String, String>()
                for ((key, pair) in idByName) {
                    logoUrlById[pair.second]?.let { joined[key] = it }
                }
                joined.ifEmpty { null }
            } catch (_: Exception) {
                null
            }
        }

    private fun downloadText(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "AFLIX/1.0 (Android)")
            .header("Accept", "application/json")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")
            return response.body?.string().orEmpty()
        }
    }
}
