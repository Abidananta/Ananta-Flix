package com.aplay.tv

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object Net {
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
                val code = connection.responseCode
                if (code in 300..399) {
                    val location = connection.getHeaderField("Location")
                        ?: throw IOException("Redirect without a target")
                    url = URL(url, location)
                } else if (code in 200..299) {
                    return connection.inputStream.bufferedReader().use { it.readText() }
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
