package com.aplay.tv

data class Stream(
    val name: String,
    val url: String,
    val userAgent: String = "",
    val referer: String = "",
    val origin: String = "",
    val cookie: String = "",
    val licenseUrl: String = "",
)

data class Channel(
    val name: String,
    val logo: String,
    val group: String,
    val stream: Stream,
) {
    val id: String get() = stream.url + "|" + name
}

data class Team(val name: String, val logo: String)

enum class MatchStatus { LIVE, UPCOMING, ENDED }

data class Match(
    val league: String,
    val leagueLogo: String,
    val home: Team,
    val away: Team,
    val startMillis: Long,
    val durationMinutes: Int,
    val servers: List<Stream>,
) {
    fun status(now: Long): MatchStatus = when {
        now < startMillis -> MatchStatus.UPCOMING
        now < startMillis + durationMinutes * 60_000L -> MatchStatus.LIVE
        else -> MatchStatus.ENDED
    }
}

data class Playlist(
    val id: String,
    val name: String,
    /** A web link, or "file:<name>" for a playlist imported from the phone. */
    val source: String,
    val enabled: Boolean = true,
)
