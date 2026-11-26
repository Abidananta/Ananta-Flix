package com.iptv.glass.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.os.Build
import android.util.Rational
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.iptv.glass.model.Channel
import com.iptv.glass.ui.components.SharedPlayerView
import com.iptv.glass.ui.theme.LiveRed
import com.iptv.glass.ui.theme.NexaCyan
import com.iptv.glass.viewmodel.MainViewModel

/**
 * Fullscreen player — opened ONLY from the Fullscreen button on the
 * Channels screen (action row) or the Now Playing bar.
 *
 * It does NOT create its own ExoPlayer. It binds a PlayerView to the
 * ViewModel's single shared player, which is already playing inline in
 * the Channels screen's top player area — so going fullscreen never
 * re-buffers, never restarts the stream, and never creates a second
 * player. Pressing Back returns to the Channels screen with playback
 * continuing inline in the top player area.
 *
 * The player is never released here; it lives in the ViewModel and is
 * released only when the app's ViewModel is cleared.
 */
@Composable
fun PlayerScreen(
    viewModel: MainViewModel,
    channel: Channel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val favorites by viewModel.favorites.collectAsState()
    val isFavorite = channel.id in favorites
    val pipEnabled by viewModel.pipEnabled.collectAsState()
    val playbackError by viewModel.playbackError.collectAsState()
    val tokenExpired by viewModel.tokenExpired.collectAsState()
    val player = viewModel.sharedPlayer

    // Landscape while fullscreen; restore on exit. The shared player
    // keeps playing through this — only the orientation changes.
    DisposableEffect(Unit) {
        runCatching { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        onDispose {
            runCatching { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (player != null && playbackError == null) {
            SharedPlayerView(viewModel, modifier = Modifier.fillMaxSize())
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
            if (pipEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && player != null) {
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
            IconButton(onClick = onBack) {
                Icon(Icons.Default.FullscreenExit, contentDescription = "Exit fullscreen", tint = Color.White)
            }
        }

        if (tokenExpired || playbackError != null) {
            Box(
                Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (tokenExpired) "Stream token expired" else "Couldn't play this stream",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        playbackError ?: "Source error",
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Live stream links can expire — refresh the playlist on GitHub " +
                            "with a new token and pull to refresh.",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Row {
                        Button(onClick = { viewModel.retryPlayback() }) {
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
