package com.iptv.glass.data

import com.iptv.glass.model.Channel

/**
 * Parses M3U playlists — both the clean single-line form and the messy
 * real-world form used by the supplied playlist.
 *
 * Real-world quirks handled (verified against the configured playlist):
 *
 *  1. #EXTINF split across TWO lines:
 *         #EXTINF:-1 group-title="Family"
 *         tvg-logo="https://…/poster.png",Toffee TV
 *     The tvg-logo attribute and the channel name are on the line AFTER
 *     #EXTINF. A naive line-by-line parser drops the logo and the name.
 *
 *  2. Channel name split across two lines:
 *         #EXTINF:-1 group-title="Sports" tvg-logo="…",WC
 *         Match Recap
 *     or  …,Zee
 *          Bangla
 *     Joining the continuation lines with a space gives "WC Match Recap"
 *     and "Zee Bangla".
 *
 *  3. No tvg-id / tvg-name on most entries — only group-title and
 *     (sometimes) tvg-logo. The id is therefore derived from the name
 *     + stream URL, which is stable across refreshes.
 *
 *  4. Stream URLs carry a query string (?edge-cache-token=…) which must
 *     be preserved in full. URLs are trimmed, never split on '?' or '&'.
 *
 *  5. Blank lines, "#EXTM3U", and "# …" comment lines are ignored.
 *     A stray trailing character on a URL line is trimmed only if it is
 *     whitespace; the token itself is left untouched.
 *
 * Standard single-line form (sample_playlist.m3u) parses identically:
 *     #EXTINF:-1 tvg-id="bbb" tvg-logo="…" group-title="Movies",Big Buck Bunny (Demo)
 *     https://…/BigBuckBunny.mp4
 */
object M3uParser {

    // NOTE: written as an escaped (non-raw) string on purpose — the pattern
    // ends with a double quote, and a raw string cannot end with one:
    // `...*""""` would terminate the raw string one quote early and leave
    // a stray `"` that swallows the following lines (compile error).
    private val ATTR_REGEX = Regex("([a-zA-Z0-9_-]+)=\"([^\"]*)\"")

    fun parse(text: String): List<Channel> {
        val lines = text.lines()
        val channels = mutableListOf<Channel>()
        var i = 0
        while (i < lines.size) {
            val raw = lines[i].trim()
            if (!raw.startsWith("#EXTINF")) {
                i++
                continue
            }
            // --- Join continuation lines ---------------------------------
            // Keep appending lines until we reach a stream URL (http…),
            // the next #EXTINF, or end of file. Continuation lines are
            // plain text: tvg-logo="…",Name  or  just the rest of a name.
            val extInf = StringBuilder(raw)
            var j = i + 1
            var streamUrl: String? = null
            while (j < lines.size) {
                val next = lines[j].trim()
                when {
                    next.isEmpty() -> j++ // skip blank lines inside an entry
                    next.startsWith("http://") || next.startsWith("https://") -> {
                        streamUrl = next
                        j++
                        break
                    }
                    next.startsWith("#EXTINF") || next.startsWith("#EXTM3U") -> break
                    next.startsWith("#") -> j++ // comment line inside an entry
                    else -> {
                        // Continuation of the #EXTINF line: join with a space
                        // so "Zee" + "Bangla" -> "Zee Bangla" and
                        // 'group-title="Family"' + 'tvg-logo="…",Name' stays valid.
                        extInf.append(' ').append(next)
                        j++
                    }
                }
            }
            i = j
            val url = streamUrl?.trim().orEmpty()
            if (url.isBlank()) continue // entry with no stream — skip it

            val header = extInf.toString()
            val attrs = ATTR_REGEX.findAll(header).associate { it.groupValues[1] to it.groupValues[2] }
            val groupTitle = attrs["group-title"].orEmpty().trim()
            val logo = attrs["tvg-logo"].orEmpty().trim()
            val tvgId = attrs["tvg-id"].orEmpty().trim()
            val tvgName = attrs["tvg-name"].orEmpty().trim()

            // Display name = text after the FIRST comma that is outside quotes.
            val name = extractName(header).ifBlank { tvgName }.ifBlank { "Unknown Channel" }

            val id = tvgId.ifNotBlank()
                ?: name.lowercase()
                    .replace(Regex("[^a-z0-9]+"), "-")
                    .trim('-')
                    .ifBlank { "ch" } + "-" + url.hashCode().toString().replace("-", "n")

            channels += Channel(
                id = id,
                name = name,
                streamUrl = url,
                logoUrl = logo,
                groupTitle = groupTitle,
                tvgId = tvgId,
                tvgName = tvgName
            )
        }
        return channels
    }

    /** Text after the first comma outside double quotes, whitespace-collapsed. */
    private fun extractName(header: String): String {
        var inQuotes = false
        for (idx in header.indices) {
            when (header[idx]) {
                '"' -> inQuotes = !inQuotes
                ',' -> if (!inQuotes) {
                    return header.substring(idx + 1)
                        .replace(Regex("\\s+"), " ")
                        .trim()
                }
            }
        }
        return ""
    }

    private fun String.ifNotBlank(): String? = if (isNotBlank()) this else null
}
