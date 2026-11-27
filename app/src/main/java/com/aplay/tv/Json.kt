package com.aplay.tv

import org.json.JSONArray
import org.json.JSONObject
import java.time.OffsetDateTime

private fun JSONObject.text(key: String): String =
    if (isNull(key)) "" else optString(key).trim()

private fun JSONArray?.objects(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

fun Stream.toJson(): JSONObject = JSONObject()
    .put("name", name)
    .put("url", url)
    .put("userAgent", userAgent)
    .put("referer", referer)
    .put("origin", origin)
    .put("cookie", cookie)
    .put("licenseUrl", licenseUrl)

/** Accepts the app's own names and the ones used by common match feeds (channel_name, stream_url...). */
fun streamFromJson(o: JSONObject, fallbackName: String = "Stream"): Stream {
    val kid = o.text("kid")
    val key = o.text("key")
    // A ClearKey pair is kept in the licence field as "clearkey:<kid>:<key>" (hex).
    val license = o.text("licenseUrl").ifEmpty {
        if (kid.isNotEmpty() && key.isNotEmpty()) "clearkey:$kid:$key" else ""
    }
    return Stream(
        name = o.text("name").ifEmpty { o.text("channel_name") }.ifEmpty { fallbackName },
        url = o.text("url").ifEmpty { o.text("stream_url") },
        userAgent = o.text("userAgent").ifEmpty { o.text("user_agent") },
        referer = o.text("referer"),
        origin = o.text("origin"),
        cookie = o.text("cookie"),
        licenseUrl = license,
    )
}

fun streamsToJson(streams: List<Stream>): String =
    JSONArray().also { array -> streams.forEach { array.put(it.toJson()) } }.toString()

fun streamsFromJson(json: String): List<Stream> =
    JSONArray(json).objects().map { streamFromJson(it) }

private val startKeys = listOf("start", "startTime", "start_time", "time", "date", "kickoff", "datetime")
private val listKeys = listOf("matches", "events", "data", "fixtures", "items", "games")

/** The start of a match in milliseconds, from an ISO date, a plain date and time, or a number. */
private fun startOf(o: JSONObject, now: Long): Long? {
    if (o.has("startInMinutes")) return now + o.optLong("startInMinutes") * 60_000L
    val raw = startKeys.firstNotNullOfOrNull { key -> o.opt(key)?.takeIf { it != JSONObject.NULL } } ?: return null
    if (raw is Number) return if (raw.toLong() > 10_000_000_000L) raw.toLong() else raw.toLong() * 1000
    val text = raw.toString().trim()
    text.toLongOrNull()?.let { return if (it > 10_000_000_000L) it else it * 1000 }
    try {
        return OffsetDateTime.parse(text).toInstant().toEpochMilli()
    } catch (e: java.time.format.DateTimeParseException) {
        // try the next form
    }
    try {
        return java.time.Instant.parse(text).toEpochMilli()
    } catch (e: java.time.format.DateTimeParseException) {
        // try the next form
    }
    // Day first, as in "09/12/2026 11:00:00 AM". Such feeds are written in Bangladesh time.
    val clean = text.replace(Regex("\\s+"), " ")
    for (pattern in listOf("dd/MM/yyyy hh:mm:ss a", "dd/MM/yyyy hh:mm a", "dd/MM/yyyy HH:mm:ss", "dd/MM/yyyy HH:mm")) {
        try {
            return java.time.LocalDateTime
                .parse(clean, java.time.format.DateTimeFormatter.ofPattern(pattern, java.util.Locale.US))
                .atZone(java.time.ZoneId.of("Asia/Dhaka")).toInstant().toEpochMilli()
        } catch (e: java.time.format.DateTimeParseException) {
            // try the next form
        }
    }
    return try {
        // No time zone written: use the phone's own.
        java.time.LocalDateTime.parse(clean.replace(' ', 'T'))
            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    } catch (e: java.time.format.DateTimeParseException) {
        null
    }
}

private fun teamOf(o: JSONObject, vararg keys: String): Team {
    val value = keys.firstNotNullOfOrNull { key -> o.opt(key)?.takeIf { it != JSONObject.NULL } }
    return when (value) {
        is JSONObject -> Team(value.text("name").ifEmpty { value.text("title") }, value.text("logo"))
        null -> Team("", "")
        else -> Team(value.toString().trim(), "")
    }
}

private fun serversOf(o: JSONObject): List<Stream> {
    val array = listOf("servers", "streams", "links", "channels").firstNotNullOfOrNull { o.optJSONArray(it) }
    val streams = if (array == null) emptyList() else (0 until array.length()).mapNotNull { i ->
        when (val item = array.opt(i)) {
            is JSONObject -> streamFromJson(item, "Server ${i + 1}")
            is String -> Stream("Server ${i + 1}", item.trim())
            else -> null
        }
    }
    val single = o.text("url").ifEmpty { o.text("stream") }
    return (if (streams.isEmpty() && single.isNotEmpty()) listOf(Stream("Server 1", single)) else streams)
        .filter { it.url.isNotEmpty() }
}

/**
 * Reads the matches file. It can be a list, or an object holding a list called "matches"
 * (or events, data...). A match whose start time cannot be read is skipped.
 */
fun parseMatches(json: String, now: Long): List<Match> {
    val text = json.trim().removePrefix("\uFEFF")
    val array = if (text.startsWith("[")) {
        JSONArray(text)
    } else {
        val root = JSONObject(text)
        listKeys.firstNotNullOfOrNull { root.optJSONArray(it) }
            ?: throw IllegalArgumentException("no \"matches\" list found in the JSON")
    }
    return array.objects().mapNotNull { o ->
        // Some feeds keep the teams and the start time inside "eventInfo".
        val info = o.optJSONObject("eventInfo")
        val start = startOf(o, now) ?: info?.let { startOf(it, now) } ?: return@mapNotNull null
        var home = teamOf(o, "home", "homeTeam", "team1")
        var away = teamOf(o, "away", "awayTeam", "team2")
        if (info != null && home.name.isEmpty()) home = Team(info.text("teamA"), info.text("teamAFlag"))
        if (info != null && away.name.isEmpty()) away = Team(info.text("teamB"), info.text("teamBFlag"))
        if (home.name.isEmpty() && away.name.isEmpty()) {
            val title = o.text("event_name").ifEmpty { o.text("title") }
            home = Team(title, "")
        }
        var duration = o.optInt("durationMinutes", 120)
        // The feed says it is live: keep it live even if it runs longer than expected.
        if (o.text("status").equals("LIVE", ignoreCase = true) && now >= start) {
            duration = maxOf(duration, ((now - start) / 60_000L).toInt() + 60)
        }
        Match(
            league = o.text("league").ifEmpty { o.text("competition") }
                .ifEmpty { info?.text("eventName").orEmpty() }
                .ifEmpty { o.text("Category") }.ifEmpty { "Other" },
            leagueLogo = o.text("leagueLogo").ifEmpty { info?.text("event_logo").orEmpty() },
            home = home,
            away = away,
            startMillis = start,
            durationMinutes = duration,
            servers = serversOf(o),
        )
    }
}
