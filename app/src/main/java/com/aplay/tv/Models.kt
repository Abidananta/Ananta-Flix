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
    /** The playlist this channel came from (its link or file). Not part of the channel's identity. */
    val source: String = "",
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

/** One source of channels, shown in the server list: a link, a file, or a built-in playlist. */
data class Server(
    val key: String,
    val name: String,
    val count: Int,
    val enabled: Boolean,
    /** Null for playlists that come with the build (Config.kt, the playlists folder). */
    val playlist: Playlist?,
)

data class NowPlaying(
    val title: String,
    val streams: List<Stream>,
    val server: Int,
    /** Channels that the previous and next buttons move through. */
    val queue: List<Channel> = emptyList(),
    val channelId: String? = null,
)

/** Puts channels in the saved order. Channels without a saved place keep their order, at the end. */
fun orderChannels(channels: List<Channel>, order: List<String>): List<Channel> {
    val rank = order.withIndex().associate { it.value to it.index }
    return channels.sortedBy { rank[it.id] ?: Int.MAX_VALUE }
}

fun swapChannels(channels: List<Channel>, firstId: String, secondId: String): List<Channel> {
    val first = channels.indexOfFirst { it.id == firstId }
    val second = channels.indexOfFirst { it.id == secondId }
    if (first < 0 || second < 0) return channels
    return channels.toMutableList().also {
        it[first] = channels[second]
        it[second] = channels[first]
    }
}
