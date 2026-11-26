package com.aplay.tv

import java.net.URLDecoder

object M3uParser {
    private val attribute = Regex("""([A-Za-z0-9_-]+)="([^"]*)"""")

    private val webLink = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)
    private val githubPage = Regex(
        """^https?://(?:www\.)?github\.com/([^/]+)/([^/]+)/blob/(.+)$""",
        RegexOption.IGNORE_CASE,
    )

    /**
     * Turns whatever was pasted or shared into a link that can be downloaded.
     * Returns null if there is no web link in it.
     */
    fun cleanLink(input: String): String? {
        val text = input.trim()
        var link = webLink.find(text)?.value
        if (link == null) {
            // A link typed without https://
            val looksLikeAddress = "://" !in text && text.none { it.isWhitespace() } &&
                '.' in text.substringBefore('/') && '/' in text
            if (!looksLikeAddress) return null
            link = "https://$text"
        }
        // A GitHub file page is changed to the file itself.
        githubPage.find(link)?.let {
            val (user, repo, path) = it.destructured
            return "https://raw.githubusercontent.com/$user/$repo/${path.substringBefore('?')}"
        }
        return link
    }

    private val directExtensions = setOf("ts", "mpd", "mp4", "mkv", "flv", "aac", "mp3", "avi", "webm")

    /** True for a link that is one video or audio stream itself, not a playlist to open. */
    fun isDirectStream(link: String): Boolean =
        link.substringBefore('?').substringAfterLast('.', "").lowercase() in directExtensions

    /** Reads a downloaded link, which is either a channel list or one video stream. */
    fun load(text: String, playlistName: String, source: String): List<Channel> {
        val singleStream = "#EXT-X-STREAM-INF" in text || "#EXT-X-TARGETDURATION" in text
        if (!singleStream) return parse(text)
        if (!source.startsWith("http", ignoreCase = true)) return emptyList()
        return listOf(Channel(playlistName, "", "My links", Stream(playlistName, source)))
    }

    fun parse(text: String): List<Channel> {
        val result = mutableListOf<Channel>()
        var info: String? = null
        var group = ""
        val options = mutableMapOf<String, String>()

        text.removePrefix("﻿").lineSequence().map { it.trim() }.forEach { line ->
            when {
                line.isEmpty() -> Unit
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    info = line
                    group = ""
                    options.clear()
                }
                line.startsWith("#EXTGRP:", ignoreCase = true) -> group = line.substringAfter(':').trim()
                line.startsWith("#EXTVLCOPT:", ignoreCase = true) -> {
                    val option = line.substringAfter(':')
                    if ('=' in option) {
                        options[option.substringBefore('=').trim().lowercase()] =
                            option.substringAfter('=').trim()
                    }
                }
                line.startsWith("#") -> Unit
                else -> {
                    val header = info
                    if (header != null && "://" in line) {
                        result += channel(header, line, group, options)
                    }
                    info = null
                    group = ""
                    options.clear()
                }
            }
        }
        return result.distinctBy { it.id }
    }

    private fun channel(
        info: String,
        rawUrl: String,
        extGroup: String,
        options: Map<String, String>,
    ): Channel {
        val attributes = attribute.findAll(info)
            .associate { it.groupValues[1].lowercase() to it.groupValues[2] }
        val comma = titleComma(info)
        val title = if (comma >= 0) info.substring(comma + 1).trim() else ""
        val name = title.ifEmpty { attributes["tvg-name"].orEmpty() }.ifEmpty { "Channel" }

        // Some playlists append headers to the link: url|User-Agent=x&Referer=y
        val url = rawUrl.substringBefore('|').trim()
        val headers = rawUrl.substringAfter('|', "")
            .split('&')
            .filter { '=' in it }
            .associate { it.substringBefore('=').trim().lowercase() to decode(it.substringAfter('=')) }

        fun header(vararg keys: String): String =
            keys.firstNotNullOfOrNull { headers[it] ?: options["http-$it"] }.orEmpty()

        val group = attributes["group-title"].orEmpty().ifEmpty { extGroup }.ifEmpty { "Other" }

        return Channel(
            name = name,
            logo = attributes["tvg-logo"].orEmpty(),
            group = group,
            stream = Stream(
                name = name,
                url = url,
                userAgent = header("user-agent"),
                referer = header("referer", "referrer"),
                origin = header("origin"),
                cookie = header("cookie"),
            ),
        )
    }

    /** Position of the comma that starts the title, ignoring commas inside quotes. */
    private fun titleComma(line: String): Int {
        var quoted = false
        line.forEachIndexed { index, c ->
            if (c == '"') quoted = !quoted
            if (c == ',' && !quoted) return index
        }
        return line.lastIndexOf(',')
    }

    private fun decode(value: String): String =
        try {
            URLDecoder.decode(value.replace("+", "%2B"), "UTF-8")
        } catch (e: IllegalArgumentException) {
            value
        }
}
