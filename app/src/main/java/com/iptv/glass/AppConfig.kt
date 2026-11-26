package com.iptv.glass

/**
 * >>> THE ONLY FILE YOU NORMALLY NEED TO EDIT <<<
 *
 * PLAYLIST_URL — the M3U playlist this app loads on start.
 * It must be a *Raw* GitHub URL, e.g.
 *   https://raw.githubusercontent.com/<user>/<repo>/<branch>/<file>.m3u
 * NOT the github.com/.../blob/... page URL (that returns HTML, not M3U).
 *
 * How it works:
 *  - On every app start (and on pull-to-refresh) the app downloads this
 *    file with OkHttp, parses it and shows the channels.
 *  - Edit the file on GitHub and the app picks up the change automatically
 *    — no app update / rebuild is needed.
 *  - If the download fails (offline, bad URL, GitHub down) the app shows
 *    an empty list with the error — there is no sample playlist fallback.
 *
 * The default below is the playlist the user supplied. Replace it with
 * your own playlist URL. Only use a playlist you have the right to use —
 * this app is a player only; it does not supply any channels itself.
 */
object AppConfig {

    const val PLAYLIST_URL =
        "https://raw.githubusercontent.com/Abidananta/toffee/refs/heads/main/toffee_playlist.m3u"

    /**
     * Optional XMLTV EPG (TV guide) URL. Leave empty ("") to disable EPG —
     * the app will simply show "No EPG info" for every channel and will
     * not make any EPG request.
     *
     * Example (replace with a real XMLTV feed that matches your channels'
     * tvg-id values):
     *   "https://example.com/epg.xml"
     */
    /** App identity shown on the More screen (our own name — not a copy of any other app). */
    const val APP_NAME: String = "Glass IPTV"
    const val APP_VERSION: String = "1.0"
    const val APP_TAGLINE: String = "Live TV, Sports & Custom Streams"

    /**
     * Optional Telegram group/channel link for the "Join Telegram" button
     * on the Channels screen. Leave empty ("") and the button is hidden.
     * Put your own link here, e.g. "https://t.me/yourchannel".
     */
    const val TELEGRAM_URL: String = ""

    const val EPG_URL: String = ""

    /** Refresh the playlist automatically this often (in minutes) while the app is open. */
    const val AUTO_REFRESH_MINUTES: Long = 30

    /** HTTP timeouts for the playlist / EPG download. */
    const val CONNECT_TIMEOUT_SECONDS: Long = 20
    const val READ_TIMEOUT_SECONDS: Long = 30

    /** Categories that are treated as "Movies" / "Series" tabs, matched
     *  case-insensitively against group-title (see Channel.kt). The real
     *  playlist uses group-title="Movies" for its live movie channels,
     *  and has no "Series" group yet — the Series tab then shows an
     *  empty state until such a group appears in the playlist. */
    val MOVIE_GROUPS = setOf("movies", "movie", "film", "films", "vod movies")
    val SERIES_GROUPS = setOf("series", "serial", "tv series", "shows", "drama series")
}
