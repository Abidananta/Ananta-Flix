package com.aplay.tv

import java.net.URLDecoder

object M3uParser {
    private val attribute = Regex("""([A-Za-z0-9_-]+)="([^"]*)"""")

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
