package com.iptv.glass.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.PictureInPictureAlt
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.iptv.glass.model.Channel
import com.iptv.glass.ui.theme.LiveRed
import com.iptv.glass.ui.theme.NexaCyan
import com.iptv.glass.viewmodel.MainViewModel

/**
 * Full-screen player (Media3 ExoPlayer).
 * - HLS (.m3u8) live and MP4/MKV VOD; the built-in PlayerView controller
 *   supplies play/pause and seek (VOD).
 * - "SERVER 1" pill in the top bar — only one server exists, labelled honestly.
 * - Floating Player (PiP): shown only when the More-screen toggle is on.
 *   Tapping it enters picture-in-picture (Android 8+, activity must allow it —
 *   see AndroidManifest: supportsPictureInPicture).
 * - Data Saver: when on, track selection is capped to SD (640x360), i.e.
 *   the lowest rendition the stream offers.
 * The player is released in DisposableEffect.onDispose — never leaked.
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

    val exoPlayer = remember(channel.streamUrl, dataSaver) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(channel.streamUrl))
            if (dataSaver) {
                // Data Saver Mode: force the lowest (SD) rendition.
                trackSelectionParameters = trackSelectionParameters.buildUpon()
                    .setMaxVideoSizeSd()
                    .build()
            }
            prepare()
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    errorMessage = error.message ?: "Playback failed — the stream may be offline or expired."
                }
            })
        }
    }

    DisposableEffect(exoPlayer) {
        viewModel.setNowPlaying(channel)
        onDispose {
            exoPlayer.release()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
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
            // Server selector — one server only, labelled honestly.
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
            if (pipEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                IconButton(onClick = {
                    activity?.enterPictureInPictureMode(
                        PictureInPictureParams.Builder()
                            .setAspectRatio(Rational(16, 9))
                            .build()
                    )
                }) {
                    Icon(Icons.Default.PictureInPictureAlt, contentDescription = "Floating player", tint = NexaCyan)
                }
            }
            IconButton(onClick = {
                isFullscreen = !isFullscreen
                activity?.requestedOrientation =
                    if (isFullscreen) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
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
                    Text(message, color = Color.White.copy(alpha = 0.8f))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Live stream links can expire — refresh the playlist on GitHub and pull to refresh.",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
