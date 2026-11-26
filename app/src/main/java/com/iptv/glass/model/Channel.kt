package com.iptv.glass.model

import com.iptv.glass.AppConfig

/** One entry from the M3U playlist. */
data class Channel(
    /** Stable id: tvg-id when present, otherwise a slug of name + stream URL. */
    val id: String,
    val name: String,
    /** Stream URL — HLS (.m3u8) for live, MP4/MKV for VOD. Query strings are kept as-is. */
    val streamUrl: String,
    val logoUrl: String = "",
    /** Raw group-title from the playlist, e.g. "Family", "Movies", "Sports",
     *  "Live Action", "Documentary", "Drama", "Teen", "Comedy", "Musical",
     *  "Fantasy", "Adventure", "Science Fiction". Empty -> "Live TV". */
    val groupTitle: String = "",
    val tvgId: String = "",
    val tvgName: String = ""
) {
    /** Display category — never blank. */
    val category: String get() = groupTitle.ifBlank { "Live TV" }

    /** True for HLS live streams (.m3u8). All 76 entries in the supplied
     *  playlist are HLS live channels, including the "Movies" group
     *  (live movie channels, not VOD files). */
    val isLive: Boolean
        get() = streamUrl.substringBefore('?').lowercase().endsWith(".m3u8") ||
            streamUrl.contains(".m3u8", ignoreCase = true)

    /** True for a playable file (MP4/MKV/WebM) — a VOD movie / episode. */
    val isVod: Boolean
        get() {
            val base = streamUrl.substringBefore('?').lowercase()
            return base.endsWith(".mp4") || base.endsWith(".mkv") ||
                base.endsWith(".webm") || base.endsWith(".mov")
        }

    /** Movies tab: group-title in AppConfig.MOVIE_GROUPS, or a VOD file
     *  inside a movie group. Live movie channels also land here. */
    val isMovie: Boolean
        get() = groupTitle.lowercase() in AppConfig.MOVIE_GROUPS

    /** Series tab: group-title in AppConfig.SERIES_GROUPS, or a VOD file
     *  inside a series group. Empty for the supplied playlist (no Series
     *  group yet) — the UI shows an empty state in that case. */
    val isSeries: Boolean
        get() = groupTitle.lowercase() in AppConfig.SERIES_GROUPS
}

/** One XMLTV programme (EPG entry) for a channel. */
data class EpgProgram(
    val channelId: String,
    val title: String,
    val description: String = "",
    /** Epoch millis. */
    val startMillis: Long,
    val endMillis: Long
) {
    val isNow: Boolean
        get() {
            val now = System.currentTimeMillis()
            return now in startMillis until endMillis
        }
}
