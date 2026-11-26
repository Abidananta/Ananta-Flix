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

fun streamFromJson(o: JSONObject, fallbackName: String = "Stream"): Stream = Stream(
    name = o.text("name").ifEmpty { fallbackName },
    url = o.text("url"),
    userAgent = o.text("userAgent"),
    referer = o.text("referer"),
    origin = o.text("origin"),
    cookie = o.text("cookie"),
    licenseUrl = o.text("licenseUrl"),
)

fun streamsToJson(streams: List<Stream>): String =
    JSONArray().also { array -> streams.forEach { array.put(it.toJson()) } }.toString()

fun streamsFromJson(json: String): List<Stream> =
    JSONArray(json).objects().map { streamFromJson(it) }

/** Reads the matches file. A match with a missing or wrong start time is skipped. */
fun parseMatches(json: String, now: Long): List<Match> {
    val root = JSONObject(json.removePrefix("﻿"))
    return root.getJSONArray("matches").objects().mapNotNull { o ->
        val start: Long = if (o.has("startInMinutes")) {
            now + o.optLong("startInMinutes") * 60_000L
        } else {
            try {
                OffsetDateTime.parse(o.text("start")).toInstant().toEpochMilli()
            } catch (e: java.time.format.DateTimeParseException) {
                return@mapNotNull null
            }
        }
        val home = o.optJSONObject("home") ?: JSONObject()
        val away = o.optJSONObject("away") ?: JSONObject()
        Match(
            league = o.text("league").ifEmpty { "Other" },
            leagueLogo = o.text("leagueLogo"),
            home = Team(home.text("name"), home.text("logo")),
            away = Team(away.text("name"), away.text("logo")),
            startMillis = start,
            durationMinutes = o.optInt("durationMinutes", 120),
            servers = o.optJSONArray("servers").objects()
                .mapIndexed { i, s -> streamFromJson(s, "Server ${i + 1}") }
                .filter { it.url.isNotEmpty() },
        )
    }
}
