package com.iptv.glass.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.util.Rational
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.iptv.glass.model.Channel
import com.iptv.glass.ui.theme.LiveRed
import com.iptv.glass.ui.theme.NexaCyan
import com.iptv.glass.viewmodel.MainViewModel

/**
 * Full-screen player (Media3 ExoPlayer) — hardened against crashes.
 *
 * Earlier versions created the player unconditionally in `remember`:
 * a blank / malformed stream URL, or any exception while building the
 * player, crashed the whole app the moment a channel was tapped.
 * This version never crashes on a bad stream:
 *  - the URL is validated first (must be http/https with a host);
 *  - player creation is wrapped in try/catch and the player is nullable;
 *  - a bad stream shows a friendly error screen with Retry / Back
 *    instead of force-closing;
 *  - playback errors are reported through Player.Listener, never thrown.
 *
 * Streaming itself: HLS (.m3u8) live and MP4/MKV VOD, with a browser-like
 * User-Agent and cross-protocol redirects allowed — some CDNs (including
 * the one in the supplied playlist) give an empty reply / 403 to a bare
 * client. If the server still refuses, the error screen says so plainly:
 * the stream link in the playlist may have expired and needs refreshing
 * on GitHub (the edge-cache-token in these URLs is time-limited).
 */
@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    channel: Channel,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val favorites by viewModel.favorites.collectAsState()
    val isFavorite = channel.id in favorites
    val pipEnabled by viewModel.pipEnabled.collectAsState()
    val dataSaver by viewModel.dataSaverEnabled.collectAsState()
    var isFullscreen by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var retryKey by remember { mutableStateOf(0) }

    // Validate the URL before touching ExoPlayer at all.
    val streamUri: Uri? = remember(channel.streamUrl) {
        runCatching { Uri.parse(channel.streamUrl.trim()) }
            .getOrNull()
            ?.takeIf { it.scheme == "http" || it.scheme == "https" }
            ?.takeIf { !it.host.isNullOrBlank() }
    }
    // Show the invalid-URL error immediately, without creating a player.
    androidx.compose.runtime.LaunchedEffect(streamUri) {
        if (streamUri == null) errorMessage = "Invalid stream URL for this channel."
    }

    val exoPlayer: ExoPlayer? = remember(streamUri, dataSaver, retryKey) {
        if (streamUri == null) {
            null
        } else {
            runCatching {
                val appContext = context.applicationContext
                val httpFactory = DefaultHttpDataSource.Factory()
                    .setUserAgent(
                        "Mozilla/5.0 (Linux; Android 13; Mobile) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) " +
                            "Chrome/120.0 Mobile Safari/537.36 AFLIX/1.0"
                    )
                    .setAllowCrossProtocolRedirects(true)
                    .setConnectTimeoutMs(15_000)
                    .setReadTimeoutMs(15_000)
                val lower = channel.streamUrl.lowercase()
                val mimeType = when {
                    lower.contains(".m3u8") -> MimeTypes.APPLICATION_M3U8
                    lower.contains(".mp4") -> MimeTypes.VIDEO_MP4
                    lower.contains(".mkv") -> MimeTypes.VIDEO_MATROSKA
                    lower.contains(".webm") -> MimeTypes.VIDEO_WEBM
                    else -> null
                }
                val mediaItem = MediaItem.Builder()
                    .setUri(streamUri)
                    .apply { if (mimeType != null) setMimeType(mimeType) }
                    .build()
                ExoPlayer.Builder(appContext)
                    .setMediaSourceFactory(DefaultMediaSourceFactory(appContext).setDataSourceFactory(httpFactory))
                    .build()
                    .apply {
                        setMediaItem(mediaItem)
                        if (dataSaver) {
                            trackSelectionParameters = trackSelectionParameters.buildUpon()
                                .setMaxVideoSizeSd()
                                .build()
                        }
                        addListener(object : Player.Listener {
                            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                                errorMessage = error.message
                                    ?: "Playback failed — the stream may be offline or expired."
                            }
                        })
                        prepare()
                        playWhenReady = true
                    }
            }.onFailure { e ->
                errorMessage = e.message ?: "Could not start the player."
            }.getOrNull()
        }
    }

    DisposableEffect(exoPlayer) {
        viewModel.setNowPlaying(channel)
        onDispose {
            runCatching { exoPlayer?.release() }
            runCatching { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (exoPlayer != null && errorMessage == null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Top bar over the video.
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column(Modifier.weight(1f)) {
                Text(channel.name, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(
                    viewModel.nowProgram(channel)?.title ?: "No EPG info",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("SERVER 1", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = { viewModel.toggleFavorite(channel) }) {
                Icon(
                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favourite",
                    tint = if (isFavorite) LiveRed else Color.White
                )
            }
            if (pipEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && exoPlayer != null) {
                IconButton(onClick = {
                    runCatching {
                        activity?.enterPictureInPictureMode(
                            PictureInPictureParams.Builder()
                                .setAspectRatio(Rational(16, 9))
                                .build()
                        )
                    }
                }) {
                    Icon(Icons.Default.PictureInPictureAlt, contentDescription = "Floating player", tint = NexaCyan)
                }
            }
            IconButton(onClick = {
                isFullscreen = !isFullscreen
                runCatching {
                    activity?.requestedOrientation =
                        if (isFullscreen) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            }) {
                Icon(
                    if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = "Fullscreen",
                    tint = Color.White
                )
            }
        }

        errorMessage?.let { message ->
            Box(
                Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Couldn't play this stream", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        message,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Live stream links can expire — refresh the playlist on GitHub and pull to refresh. " +
                            "If it still fails, the server may be blocking this device/region.",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Row {
                        Button(onClick = {
                            errorMessage = null
                            retryKey++
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Retry")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = onBack) {
                            Text("Back")
                        }
                    }
                }
            }
        }
    }
}
