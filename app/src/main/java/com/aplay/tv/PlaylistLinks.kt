package com.aplay.tv

/**
 * Reads playlists/links.txt. Each line is "Name | link" or only a link.
 * Empty lines and lines starting with # are ignored.
 */
fun parsePlaylistLinks(text: String): List<Pair<String, String>> {
    val result = mutableListOf<Pair<String, String>>()
    text.removePrefix("\uFEFF").lineSequence().map { it.trim() }.forEach { line ->
        if (line.isEmpty() || line.startsWith("#")) return@forEach
        val named = '|' in line && "://" !in line.substringBefore('|')
        val name = if (named) line.substringBefore('|').trim() else ""
        val link = if (named) line.substringAfter('|').trim() else line
        if (link.startsWith("http://", true) || link.startsWith("https://", true)) {
            result += name.ifEmpty { "Playlist ${result.size + 1}" } to link
        }
    }
    return result
}
