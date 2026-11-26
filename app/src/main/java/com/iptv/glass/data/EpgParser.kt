package com.iptv.glass.data

import android.util.Xml
import com.iptv.glass.model.EpgProgram
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Minimal XMLTV parser. Only used when AppConfig.EPG_URL is non-empty.
 * If the EPG URL is empty, malformed, or a channel has no programmes,
 * callers get an empty list and the UI shows "No EPG info" — EPG
 * failure never breaks channel loading or playback.
 *
 * Expected XMLTV shape:
 *   <tv>
 *     <programme start="20261126180000 +0000" stop="20261126190000 +0000" channel="cnn">
 *       <title>News Hour</title>
 *       <desc>…</desc>
 *     </programme>
 *   </tv>
 */
object EpgParser {

    private val DATE_FORMATS = listOf(
        SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US),
        SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
    ).onEach { it.timeZone = TimeZone.getTimeZone("UTC") }

    fun parse(xml: String): List<EpgProgram> {
        val programs = mutableListOf<EpgProgram>()
        if (xml.isBlank()) return programs
        try {
            val parser: XmlPullParser = Xml.newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                setInput(StringReader(xml))
            }
            var event = parser.eventType
            var channelId = ""
            var start = 0L
            var stop = 0L
            var title = ""
            var desc = ""
            var currentTag: String? = null
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                        if (parser.name == "programme") {
                            channelId = parser.getAttributeValue(null, "channel").orEmpty()
                            start = parseDate(parser.getAttributeValue(null, "start"))
                            stop = parseDate(parser.getAttributeValue(null, "stop"))
                            title = ""
                            desc = ""
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val text = parser.text.orEmpty().trim()
                        if (text.isNotEmpty()) {
                            when (currentTag) {
                                "title" -> title = text
                                "desc" -> desc = text
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "programme" && channelId.isNotEmpty() && title.isNotEmpty()) {
                            programs += EpgProgram(channelId, title, desc, start, stop)
                        }
                        currentTag = null
                    }
                }
                event = parser.next()
            }
        } catch (_: Exception) {
            // EPG is best-effort: return whatever was parsed (possibly nothing).
        }
        return programs
    }

    private fun parseDate(raw: String?): Long {
        if (raw.isNullOrBlank()) return 0L
        val cleaned = raw.trim()
        for (format in DATE_FORMATS) {
            try {
                return format.parse(cleaned)?.time ?: 0L
            } catch (_: Exception) {
                // try the next format
            }
        }
        return 0L
    }

    /** Programme airing now for [channelId] (matched on tvg-id, falling back to name). */
    fun currentProgram(programs: List<EpgProgram>, channelId: String, channelName: String): EpgProgram? {
        val now = System.currentTimeMillis()
        return programs.firstOrNull {
            (it.channelId == channelId || it.channelId.equals(channelName, ignoreCase = true)) &&
                now in it.startMillis until it.endMillis
        }
    }

    fun nextProgram(programs: List<EpgProgram>, channelId: String, channelName: String): EpgProgram? {
        val now = System.currentTimeMillis()
        return programs
            .filter {
                (it.channelId == channelId || it.channelId.equals(channelName, ignoreCase = true)) &&
                    it.startMillis > now
            }
            .minByOrNull { it.startMillis }
    }
}
