package com.aplay.tv

import android.content.Context
import android.media.audiofx.Equalizer
import android.net.ConnectivityManager
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

data class Quality(val height: Int, val group: Tracks.Group, val track: Int, val selected: Boolean)

/** Owns the video player, so it keeps running when the screen changes between window and full screen. */
class PlayerController(private val context: Context, private val store: Store) {
    var exo by mutableStateOf<ExoPlayer?>(null)
        private set
    var playing by mutableStateOf<NowPlaying?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var isBuffering by mutableStateOf(false)
        private set
    var equalizerPreset by mutableIntStateOf(-1)
        private set
    private var equalizer: Equalizer? = null
    private val main = Handler(Looper.getMainLooper())

    fun open(title: String, streams: List<Stream>, server: Int) {
        if (server !in streams.indices) return
        playing = NowPlaying(title, streams, server)
        start()
    }

    fun openChannel(channel: Channel, queue: List<Channel>) {
        playing = NowPlaying(channel.name, listOf(channel.stream), 0, queue, channel.id)
        start()
    }

    fun selectServer(index: Int) {
        val current = playing ?: return
        if (index !in current.streams.indices) return
        playing = current.copy(server = index)
        start()
    }

    val canStep: Boolean get() = (playing?.queue?.size ?: 0) > 1

    /** Moves to the previous (-1) or next (+1) channel. */
    fun step(by: Int) {
        val current = playing ?: return
        if (current.queue.size < 2) return
        val index = current.queue.indexOfFirst { it.id == current.channelId }
        val next = (index + by).mod(current.queue.size)
        openChannel(current.queue[next], current.queue)
    }

    /** Keeps previous and next in step with the saved channel order. */
    fun updateQueue(ordered: List<Channel>) {
        val current = playing ?: return
        if (current.queue.isEmpty()) return
        val ids = current.queue.map { it.id }.toSet()
        playing = current.copy(queue = ordered.filter { it.id in ids })
    }

    fun togglePlay() {
        val player = exo ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
            player.play()
        }
    }

    fun reload() {
        if (playing != null) start()
    }

    fun close() {
        release()
        playing = null
        error = null
    }

    /** Frees the player while the app is not visible. */
    fun suspend() = release()

    fun resume() {
        if (exo == null && playing != null) start()
    }

    private fun start(mimeType: String? = null) {
        val current = playing ?: return
        val stream = current.streams.getOrNull(current.server) ?: return
        release()
        error = null
        isBuffering = true

        val headers = buildMap {
            if (stream.referer.isNotEmpty()) put("Referer", stream.referer)
            if (stream.origin.isNotEmpty()) put("Origin", stream.origin)
            if (stream.cookie.isNotEmpty()) put("Cookie", stream.cookie)
        }
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent(stream.userAgent.ifEmpty { "APlay/1.0" })
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(headers)

        val renderers = DefaultRenderersFactory(context).setEnableDecoderFallback(true)
        if (store.softwareDecoder) {
            renderers.setMediaCodecSelector(
                MediaCodecSelector { type, secure, tunneling ->
                    MediaCodecUtil.getDecoderInfos(type, secure, tunneling)
                        .sortedBy { it.hardwareAccelerated }
                }
            )
        }

        val player = ExoPlayer.Builder(context, renderers)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(context)
                    .setDataSourceFactory(DefaultDataSource.Factory(context, http))
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        player.setAudioAttributes(AudioAttributes.DEFAULT, true)

        val network = context.getSystemService(ConnectivityManager::class.java)
        if (store.dataSaver && network.isActiveNetworkMetered) {
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setForceLowestBitrate(true)
                .build()
        }

        val item = MediaItem.Builder().setUri(stream.url)
        if (mimeType != null) item.setMimeType(mimeType)
        if (stream.licenseUrl.isNotEmpty()) {
            item.setDrmConfiguration(
                MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                    .setLicenseUri(stream.licenseUrl)
                    .setLicenseRequestHeaders(headers)
                    .build()
            )
        }

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                this@PlayerController.isPlaying = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
            }

            override fun onPlayerError(error: PlaybackException) {
                if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    // Live stream fell behind: jump back to the live point.
                    player.seekToDefaultPosition()
                    player.prepare()
                } else if (error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED &&
                    mimeType == null
                ) {
                    // Links without a file extension are often HLS.
                    main.post { if (exo === player) start(MimeTypes.APPLICATION_M3U8) }
                } else {
                    isBuffering = false
                    this@PlayerController.error =
                        "This stream could not be played.\n${error.errorCodeName}"
                }
            }
        })
        player.setMediaItem(item.build())
        player.playWhenReady = true
        player.prepare()
        exo = player
        applyEqualizer()
    }

    private fun release() {
        equalizer?.release()
        equalizer = null
        exo?.release()
        exo = null
        isPlaying = false
        isBuffering = false
    }

    /** Video qualities offered by the current stream, best first. */
    fun qualities(): List<Quality> {
        val player = exo ?: return emptyList()
        val chosen = player.trackSelectionParameters.overrides
        return player.currentTracks.groups
            .filter { it.type == C.TRACK_TYPE_VIDEO }
            .flatMap { group ->
                (0 until group.length)
                    .filter { group.isTrackSupported(it) && group.getTrackFormat(it).height > 0 }
                    .map { track ->
                        val selected = chosen[group.mediaTrackGroup]?.trackIndices?.contains(track) == true
                        Quality(group.getTrackFormat(track).height, group, track, selected)
                    }
            }
            .sortedByDescending { it.height }
            .distinctBy { it.height }
    }

    /** Pass null to let the player choose the quality by itself. */
    fun selectQuality(quality: Quality?) {
        val player = exo ?: return
        val builder = player.trackSelectionParameters
            .buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
        if (quality != null) {
            builder.addOverride(TrackSelectionOverride(quality.group.mediaTrackGroup, quality.track))
        }
        player.trackSelectionParameters = builder.build()
    }

    /** Names of the sound presets on this phone. Empty if it has no equalizer. */
    fun equalizerPresets(): List<String> {
        val session = exo?.audioSessionId ?: return emptyList()
        if (session == C.AUDIO_SESSION_ID_UNSET) return emptyList()
        return try {
            val effect = equalizer ?: Equalizer(0, session)
            val names = (0 until effect.numberOfPresets.toInt())
                .map { effect.getPresetName(it.toShort()) }
            if (effect !== equalizer) effect.release()
            names
        } catch (e: RuntimeException) {
            emptyList()
        }
    }

    /** -1 turns the equalizer off. */
    fun selectEqualizer(preset: Int) {
        equalizerPreset = preset
        applyEqualizer()
    }

    private fun applyEqualizer() {
        equalizer?.release()
        equalizer = null
        val session = exo?.audioSessionId ?: return
        if (equalizerPreset < 0 || session == C.AUDIO_SESSION_ID_UNSET) return
        equalizer = try {
            Equalizer(0, session).also {
                it.usePreset(equalizerPreset.toShort())
                it.setEnabled(true)
            }
        } catch (e: RuntimeException) {
            null
        }
    }
}
