package com.aplay.tv

import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object Net {
    private const val LIMIT = 20_000_000

    /** Reads a playlist, stopping if it is far too large (for example an endless video link). */
    fun readLimited(input: InputStream): String {
        val reader = input.bufferedReader()
        val text = StringBuilder()
        val buffer = CharArray(16_384)
        while (true) {
            val count = reader.read(buffer)
            if (count < 0) return text.toString()
            text.append(buffer, 0, count)
            if (text.length > LIMIT) throw IOException("Too large to be a playlist")
        }
    }

    /** Downloads a text file, following redirects (including http to https). */
    fun fetch(address: String): String {
        var url = URL(address)
        repeat(6) {
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 15_000
                connection.readTimeout = 30_000
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("User-Agent", "APlay/1.0")
                // Always ask for the newest copy, so an edited playlist shows up.
                connection.useCaches = false
                connection.setRequestProperty("Cache-Control", "no-cache")
                val code = connection.responseCode
                if (code in 300..399) {
                    val location = connection.getHeaderField("Location")
                        ?: throw IOException("Redirect without a target")
                    url = URL(url, location)
                } else if (code in 200..299) {
                    return connection.inputStream.use { readLimited(it) }
                } else {
                    throw IOException("Server answered $code")
                }
            } finally {
                connection.disconnect()
            }
        }
        throw IOException("Too many redirects")
    }
}
