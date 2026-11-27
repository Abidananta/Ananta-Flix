package com.aplay.tv

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import kotlin.coroutines.cancellation.CancellationException

/**
 * Pairs the phone with the TV through ntfy.sh, a free public message service (no account needed).
 * The TV shows a code and listens on a topic made from it. The phone sends the settings to that topic.
 */
object TvPairing {
    private const val HOST = "https://ntfy.sh/"
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    const val CODE_LENGTH = 8
    const val MAX_MESSAGE = 3900

    fun newCode(): String {
        val random = SecureRandom()
        return buildString { repeat(CODE_LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) } }
    }

    /** Keeps only the letters and numbers a code can hold, in capitals. */
    fun clean(input: String): String = input.uppercase().filter { it in ALPHABET }

    private fun topic(code: String) = "aplay-pair-" + code.lowercase()

    /** A message waiting on the TV's topic: its id and text. */
    class Message(val id: String, val text: String)

    /** Waits until a message arrives for [code] (newer than [since]). Retries if the connection drops. */
    suspend fun waitForMessage(code: String, since: String): Message = withContext(Dispatchers.IO) {
        while (true) {
            currentCoroutineContext().ensureActive()
            var connection: HttpURLConnection? = null
            // Closing the connection is what stops a read that is waiting.
            val handle = currentCoroutineContext().job.invokeOnCompletion { connection?.disconnect() }
            try {
                val url = URL("$HOST${topic(code)}/json?since=$since")
                val opened = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 120_000
                    useCaches = false
                }
                connection = opened
                val reader = opened.inputStream.bufferedReader()
                while (true) {
                    val line = reader.readLine() ?: break
                    val item = JSONObject(line)
                    if (item.optString("event") == "message") {
                        return@withContext Message(item.optString("id"), item.optString("message"))
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // No internet or the service is busy: try again shortly.
            } finally {
                handle.dispose()
                connection?.disconnect()
            }
            delay(3_000)
        }
        error("unreachable")
    }

    /** Sends [text] to the TV that shows [code]. */
    suspend fun send(code: String, text: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val connection = URL(HOST + topic(code)).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Content-Type", "text/plain; charset=utf-8")
            connection.outputStream.use { it.write(text.toByteArray()) }
            val ok = connection.responseCode in 200..299
            connection.disconnect()
            ok
        } catch (e: IOException) {
            false
        }
    }
}
