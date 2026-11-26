package com.aplay.tv

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Rational
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.aplay.tv.ui.APlayTheme

class PlayerActivity : ComponentActivity() {
    private lateinit var store: Store
    private var player by mutableStateOf<ExoPlayer?>(null)
    private var streams by mutableStateOf(emptyList<Stream>())
    private var current by mutableIntStateOf(0)
    private var heading by mutableStateOf("")
    private var error by mutableStateOf<String?>(null)
    private var controlsVisible by mutableStateOf(true)
    private var resizeMode by mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT)
    private var inPip by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = Store(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemBars()
        load(intent)
        if (savedInstanceState != null) {
            current = savedInstanceState.getInt("current", current)
                .coerceIn(0, (streams.size - 1).coerceAtLeast(0))
        }
        setContent { APlayTheme { Screen() } }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("current", current)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        release()
        load(intent)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) play(current)
    }

    override fun onStart() {
        super.onStart()
        if (player == null) play(current)
    }

    override fun onStop() {
        release()
        super.onStop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (store.pictureInPicture && player?.isPlaying == true) enterPip()
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        inPip = isInPictureInPictureMode
    }

    private fun load(intent: Intent) {
        streams = streamsFromJson(intent.getStringExtra(EXTRA_STREAMS) ?: "[]")
        current = intent.getIntExtra(EXTRA_INDEX, 0)
        heading = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        error = null
    }

    private fun play(index: Int, mimeType: String? = null) {
        val stream = streams.getOrNull(index) ?: return
        release()
        current = index
        error = null

        val headers = buildMap {
            if (stream.referer.isNotEmpty()) put("Referer", stream.referer)
            if (stream.origin.isNotEmpty()) put("Origin", stream.origin)
            if (stream.cookie.isNotEmpty()) put("Cookie", stream.cookie)
        }
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent(stream.userAgent.ifEmpty { "APlay/1.0" })
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(headers)

        val renderers = DefaultRenderersFactory(this).setEnableDecoderFallback(true)
        if (store.softwareDecoder) {
            renderers.setMediaCodecSelector(
                MediaCodecSelector { type, secure, tunneling ->
                    MediaCodecUtil.getDecoderInfos(type, secure, tunneling)
                        .sortedBy { it.hardwareAccelerated }
                }
            )
        }

        val exo = ExoPlayer.Builder(this, renderers)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(this)
                    .setDataSourceFactory(DefaultDataSource.Factory(this, http))
            )
            .build()

        val network = getSystemService(ConnectivityManager::class.java)
        if (store.dataSaver && network.isActiveNetworkMetered) {
            exo.trackSelectionParameters = exo.trackSelectionParameters
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

        exo.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                when {
                    // Live stream fell behind: jump back to the live point.
                    error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW -> {
                        exo.seekToDefaultPosition()
                        exo.prepare()
                    }
                    // Links without a file extension are often HLS.
                    error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED &&
                        mimeType == null -> window.decorView.post {
                        if (player === exo) play(index, MimeTypes.APPLICATION_M3U8)
                    }
                    else -> {
                        controlsVisible = true
                        this@PlayerActivity.error =
                            "This stream could not be played.\n${error.errorCodeName}"
                    }
                }
            }
        })
        exo.setMediaItem(item.build())
        exo.playWhenReady = true
        exo.prepare()
        player = exo
    }

    private fun release() {
        player?.release()
        player = null
    }

    private fun enterPip() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) return
        try {
            enterPictureInPictureMode(
                PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build()
            )
        } catch (e: IllegalStateException) {
            // The phone does not allow a floating window right now.
        }
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    @Composable
    private fun Screen() {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    PlayerView(context).apply {
                        setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                        setControllerVisibilityListener(
                            PlayerView.ControllerVisibilityListener { visibility ->
                                controlsVisible = visibility == View.VISIBLE
                            }
                        )
                    }
                },
                update = { view ->
                    view.player = player
                    view.resizeMode = resizeMode
                    view.useController = !inPip
                },
            )
            if (!inPip) {
                error?.let { message ->
                    Column(
                        Modifier.align(Alignment.Center).background(Color(0xDD000000)).padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(message, color = Color.White, textAlign = TextAlign.Center)
                        Button(onClick = { play(current) }) { Text("Try again") }
                    }
                }
                if (controlsVisible || error != null) TopControls()
            }
        }
    }

    @Composable
    private fun TopControls() {
        Column(Modifier.fillMaxWidth().background(Color(0x99000000)).displayCutoutPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { finish() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
                Text(
                    heading,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = {
                    resizeMode = when (resizeMode) {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                }) {
                    Icon(Icons.Filled.AspectRatio, "Screen size", tint = Color.White)
                }
                IconButton(onClick = { enterPip() }) {
                    Icon(Icons.Filled.PictureInPictureAlt, "Floating window", tint = Color.White)
                }
            }
            if (streams.size > 1) {
                LazyRow(
                    Modifier.padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(streams.size) { index ->
                        FilterChip(
                            selected = index == current,
                            onClick = { play(index) },
                            label = { Text(streams[index].name, color = Color.White) },
                        )
                    }
                }
            }
        }
    }

    companion object {
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_STREAMS = "streams"
        private const val EXTRA_INDEX = "index"

        fun intent(context: Context, title: String, streams: List<Stream>, index: Int): Intent =
            Intent(context, PlayerActivity::class.java)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_STREAMS, streamsToJson(streams))
                .putExtra(EXTRA_INDEX, index)
    }
}
