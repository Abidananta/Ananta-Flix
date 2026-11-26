package com.aplay.tv

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Everything the user saves on the phone. */
class Store(context: Context) {
    private val prefs = context.getSharedPreferences("aplay", Context.MODE_PRIVATE)

    var playlists: List<Playlist>
        get() {
            val array = JSONArray(prefs.getString("playlists", "[]"))
            return (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                Playlist(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    source = o.getString("source"),
                    enabled = o.optBoolean("enabled", true),
                )
            }
        }
        set(value) {
            val array = JSONArray()
            value.forEach {
                array.put(
                    JSONObject()
                        .put("id", it.id)
                        .put("name", it.name)
                        .put("source", it.source)
                        .put("enabled", it.enabled)
                )
            }
            prefs.edit().putString("playlists", array.toString()).apply()
        }

    var favorites: Set<String>
        get() = prefs.getStringSet("favorites", emptySet()).orEmpty().toSet()
        set(value) = prefs.edit().putStringSet("favorites", value).apply()

    var channelOrder: List<String>
        get() {
            val array = JSONArray(prefs.getString("channelOrder", "[]"))
            return (0 until array.length()).map { array.getString(it) }
        }
        set(value) {
            val array = JSONArray()
            value.forEach { array.put(it) }
            prefs.edit().putString("channelOrder", array.toString()).apply()
        }

    /** The playlist chosen in the server list. Empty means all servers. */
    var selectedServer: String
        get() = prefs.getString("selectedServer", "").orEmpty()
        set(value) = prefs.edit().putString("selectedServer", value).apply()

    var matchesUrl: String
        get() = prefs.getString("matchesUrl", "").orEmpty()
        set(value) = prefs.edit().putString("matchesUrl", value).apply()

    var pictureInPicture: Boolean
        get() = prefs.getBoolean("pip", true)
        set(value) = prefs.edit().putBoolean("pip", value).apply()

    var dataSaver: Boolean
        get() = prefs.getBoolean("dataSaver", false)
        set(value) = prefs.edit().putBoolean("dataSaver", value).apply()

    var softwareDecoder: Boolean
        get() = prefs.getBoolean("softwareDecoder", false)
        set(value) = prefs.edit().putBoolean("softwareDecoder", value).apply()
}
