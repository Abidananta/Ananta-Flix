package com.aplay.tv.ui

import android.view.LayoutInflater
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List as ChannelListIcon
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.aplay.tv.AppViewModel
import com.aplay.tv.R
import kotlinx.coroutines.delay
import java.util.Locale

private enum class PlayerMenu { NONE, QUALITY, EQUALIZER }

private val Shade = Color(0x99000000)

/** The video with its controls. Used both as the small window and in full screen. */
@Composable
fun PlayerPane(model: AppViewModel, inPip: Boolean, modifier: Modifier = Modifier) {
    val player = model.player
    val playing = player.playing ?: return
    val fullscreen = model.fullscreen
    var controls by remember { mutableStateOf(false) }
    var channelList by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(PlayerMenu.NONE) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    // Goes up on every touch of a control, which restarts the hide timer.
    var touches by remember { mutableIntStateOf(0) }
    val seeking = remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(fullscreen) { channelList = false }
    LaunchedEffect(controls, player.isPlaying, touches, menu, seeking.value != null) {
        if (controls && player.isPlaying && menu == PlayerMenu.NONE && seeking.value == null) {
            delay(4_000)
            controls = false
        }
    }
    BackHandler(enabled = channelList) { channelList = false }

    Box(modifier.background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                LayoutInflater.from(context).inflate(R.layout.player_view, null) as PlayerView
            },
            update = { view ->
                view.player = player.exo
                view.resizeMode = resizeMode
            },
            onRelease = { it.player = null },
        )
        // A tap on the picture shows or hides the controls.
        Box(
            Modifier.fillMaxSize().clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                if (channelList) channelList = false else controls = !controls
            }
        )
        if (!inPip) {
            if (player.isBuffering && !controls) {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = Accent)
            }

            if (controls && !channelList && player.error == null) {
                Controls(
                    model = model,
                    fullscreen = fullscreen,
                    seeking = seeking,
                    onTouch = { touches++ },
                    onResize = {
                        resizeMode = when (resizeMode) {
                            AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                            AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    },
                    onMenu = { menu = it },
                    onChannelList = {
                        controls = false
                        channelList = true
                    },
                )
            }

            player.error?.let { message ->
                Column(
                    Modifier.align(Alignment.Center).background(Shade).padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(message, color = Color.White, fontSize = 13.sp, textAlign = TextAlign.Center)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { player.reload() }) { Text("Try again") }
                        TextButton(
                            onClick = { if (fullscreen) model.fullscreen = false else model.closePlayer() },
                        ) { Text("Back") }
                    }
                }
            }

            if (fullscreen && channelList) {
                ChannelDrawer(model) { channelList = false }
            }
        }
    }

    when (menu) {
        PlayerMenu.NONE -> Unit
        PlayerMenu.QUALITY -> {
            val qualities = remember { player.qualities() }
            val automatic = qualities.none { it.selected }
            OptionsDialog(
                title = "Video quality",
                options = listOf("Auto" to automatic) + qualities.map { "${it.height}p" to it.selected },
                onPick = { player.selectQuality(qualities.getOrNull(it - 1)) },
                onDismiss = { menu = PlayerMenu.NONE },
            )
        }
        PlayerMenu.EQUALIZER -> {
            val presets = remember { player.equalizerPresets() }
            OptionsDialog(
                title = if (presets.isEmpty()) "No equalizer on this phone" else "Equalizer",
                options = listOf("Off" to (player.equalizerPreset < 0)) +
                    presets.mapIndexed { i, name -> name to (player.equalizerPreset == i) },
                onPick = { player.selectEqualizer(it - 1) },
                onDismiss = { menu = PlayerMenu.NONE },
            )
        }
    }
}

@Composable
private fun BoxScope.Controls(
    model: AppViewModel,
    fullscreen: Boolean,
    seeking: MutableState<Float?>,
    onTouch: () -> Unit,
    onResize: () -> Unit,
    onMenu: (PlayerMenu) -> Unit,
    onChannelList: () -> Unit,
) {
    val player = model.player
    val playing = player.playing ?: return
    var position by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var live by remember { mutableStateOf(true) }
    var seekable by remember { mutableStateOf(false) }
    LaunchedEffect(player.exo) {
        while (true) {
            player.exo?.let {
                position = it.currentPosition.coerceAtLeast(0)
                duration = it.duration.coerceAtLeast(0)
                live = it.isCurrentMediaItemLive
                seekable = it.isCurrentMediaItemSeekable
            }
            delay(500)
        }
    }
    val edge = if (fullscreen) 16.dp else 6.dp
    val safe = if (fullscreen) Modifier.displayCutoutPadding() else Modifier

    // Top: back, name, server and live badge
    Row(
        safe.align(Alignment.TopCenter).fillMaxWidth().padding(edge),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RoundButton(Icons.AutoMirrored.Filled.ArrowBack, "Back") {
            if (fullscreen) model.fullscreen = false else model.closePlayer()
        }
        Text(
            playing.title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (playing.streams.size > 1) {
            TextPill(playing.streams[playing.server].name) {
                onTouch()
                player.selectServer((playing.server + 1) % playing.streams.size)
            }
        }
        Text(
            if (live) "● LIVE" else "● VOD",
            color = if (live) LiveRed else Accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clip(RoundedCornerShape(50)).background(Shade)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }

    // Middle: previous, play or pause, next
    Row(
        Modifier.align(Alignment.Center),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (fullscreen) 40.dp else 24.dp),
    ) {
        if (player.canStep) {
            RoundButton(Icons.Filled.SkipPrevious, "Previous channel", 42.dp) {
                onTouch()
                player.step(-1)
            }
        }
        RoundButton(
            icon = if (player.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            label = if (player.isPlaying) "Pause" else "Play",
            size = if (fullscreen) 68.dp else 52.dp,
            background = Accent,
            tint = Color.Black,
        ) {
            onTouch()
            player.togglePlay()
        }
        if (player.canStep) {
            RoundButton(Icons.Filled.SkipNext, "Next channel", 42.dp) {
                onTouch()
                player.step(1)
            }
        }
    }

    // Bottom: time line and tools
    Column(safe.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = edge, vertical = 4.dp)) {
        if (seekable && duration > 0) {
            val fraction = seeking.value ?: (position.toFloat() / duration).coerceIn(0f, 1f)
            Row(verticalAlignment = Alignment.CenterVertically) {
                TimeText((fraction * duration).toLong())
                Slider(
                    value = fraction,
                    onValueChange = { seeking.value = it },
                    onValueChangeFinished = {
                        seeking.value?.let { player.exo?.seekTo((it * duration).toLong()) }
                        seeking.value = null
                        onTouch()
                    },
                    modifier = Modifier.weight(1f).height(28.dp).padding(horizontal = 8.dp),
                )
                TimeText(duration)
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextPill(if (model.softwareDecoder) "SW" else "HW") {
                onTouch()
                model.updateSoftwareDecoder(!model.softwareDecoder)
                player.reload()
            }
            if (fullscreen) {
                RoundButton(Icons.Filled.GraphicEq, "Equalizer") { onMenu(PlayerMenu.EQUALIZER) }
                RoundButton(Icons.AutoMirrored.Filled.ChannelListIcon, "Channel list", onClick = onChannelList)
            }
            RoundButton(Icons.Filled.AspectRatio, "Screen size") {
                onTouch()
                onResize()
            }
            TextPill("AUTO") { onMenu(PlayerMenu.QUALITY) }
            RoundButton(
                if (fullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                if (fullscreen) "Leave full screen" else "Full screen",
            ) {
                onTouch()
                model.fullscreen = !fullscreen
            }
        }
    }
}

@Composable
private fun RoundButton(
    icon: ImageVector,
    label: String,
    size: Dp = 34.dp,
    background: Color = Shade,
    tint: Color = Color.White,
    onClick: () -> Unit,
) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(percent = 35)).background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(size * 0.6f))
    }
}

@Composable
private fun TextPill(text: String, onClick: () -> Unit) {
    Box(
        Modifier.height(34.dp).defaultMinSize(minWidth = 44.dp)
            .clip(RoundedCornerShape(12.dp)).background(Shade)
            .clickable(onClick = onClick).padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun TimeText(millis: Long) {
    val seconds = millis / 1000
    Text(
        String.format(Locale.US, "%02d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60),
        color = Color.White,
        fontSize = 11.sp,
    )
}

@Composable
private fun OptionsDialog(
    title: String,
    options: List<Pair<String, Boolean>>,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                options.forEachIndexed { index, (label, selected) ->
                    TextButton(
                        onClick = {
                            onPick(index)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (selected) "✓ $label" else label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
