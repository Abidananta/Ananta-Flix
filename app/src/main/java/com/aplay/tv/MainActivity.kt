package com.aplay.tv

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.Rational
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.aplay.tv.ui.ALL_CHANNELS
import com.aplay.tv.ui.APlayTheme
import com.aplay.tv.ui.Accent
import com.aplay.tv.ui.AddPlaylistDialog
import com.aplay.tv.ui.ChannelsScreen
import com.aplay.tv.ui.FavoritesScreen
import com.aplay.tv.ui.MatchesScreen
import com.aplay.tv.ui.MoreScreen
import com.aplay.tv.ui.NowPlayingBar
import com.aplay.tv.ui.PlayerPane
import com.aplay.tv.ui.ServerDialog
import com.aplay.tv.ui.ServersScreen

class MainActivity : ComponentActivity() {
    private val model: AppViewModel by viewModels()
    private var inPip by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        if (savedInstanceState == null) receive(intent)
        setContent {
            // Full screen turns the phone sideways and hides the system bars.
            val fullscreen = model.fullscreen
            LaunchedEffect(fullscreen) { applyScreenMode(fullscreen) }
            APlayTheme { App(model, inPip) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        receive(intent)
    }

    /** A link shared to the app, or a playlist file opened with it. */
    private fun receive(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
                model.fullscreen = false
                model.showAddPlaylist(M3uParser.cleanLink(text) ?: text.trim())
            }
            Intent.ACTION_VIEW -> {
                val file = intent.data ?: return
                model.fullscreen = false
                model.importPlaylist(file)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        model.player.resume()
        model.refreshIfStale()
    }

    override fun onResume() {
        super.onResume()
        model.skipPictureInPicture = false
    }

    override fun onStop() {
        if (!isChangingConfigurations) model.player.suspend()
        super.onStop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && model.fullscreen) applyScreenMode(true)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (model.pictureInPicture && model.player.isPlaying && !model.skipPictureInPicture) {
            enterPip()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        inPip = isInPictureInPictureMode
    }

    private fun applyScreenMode(fullscreen: Boolean) {
        // Phones stay upright outside full screen; tablets and TVs rotate freely.
        requestedOrientation = when {
            fullscreen -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            resources.configuration.smallestScreenWidthDp < 600 ->
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        WindowInsetsControllerCompat(window, window.decorView).apply {
            if (fullscreen) {
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.systemBars())
            } else {
                show(WindowInsetsCompat.Type.systemBars())
            }
        }
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
}

private enum class Tab(val label: String) {
    MATCHES("Matches"),
    CHANNELS("Channels"),
    SERVERS("Servers"),
    FAVORITES("Favorites"),
    MORE("More"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun App(model: AppViewModel, inPip: Boolean) {
    val context = LocalContext.current
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    var category by rememberSaveable { mutableStateOf(ALL_CHANNELS) }
    var showServers by rememberSaveable { mutableStateOf(false) }
    // The server whose channels are open in the Servers tab. Null shows the server cards, "" is all servers.
    var openServer by rememberSaveable { mutableStateOf<String?>(null) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val tab = Tab.entries[tabIndex]
    val inServer = tab == Tab.SERVERS && openServer != null
    val searchable = tab == Tab.CHANNELS || tab == Tab.SERVERS || tab == Tab.FAVORITES
    val activeQuery = if (searching && searchable) query else ""
    val playing = model.player.playing
    // Only the video is shown in full screen and in the floating window.
    val videoOnly = playing != null && (model.fullscreen || inPip)
    // In the settings menu the video shrinks to a small floating window.
    val mini = playing != null && !videoOnly && tab == Tab.MORE

    val play: (String, List<Stream>, Int) -> Unit = model.player::open
    val playChannel: (Channel, List<Channel>) -> Unit = model.player::openChannel

    LaunchedEffect(model.message) {
        model.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            model.message = null
        }
    }
    BackHandler(enabled = playing != null || tabIndex != 0 || openServer != null) {
        when {
            model.fullscreen -> model.fullscreen = false
            mini -> tabIndex = 0
            playing != null -> model.closePlayer()
            inServer -> openServer = null
            else -> tabIndex = 0
        }
    }

    AddPlaylistDialog(model)
    if (showServers) ServerDialog(model) { showServers = false }

    val serverTitle = model.servers.firstOrNull { it.key == openServer }?.name ?: "All servers"
    val serverChannels = remember(model.channels, model.sourceChannels, openServer) {
        val own = openServer?.let { model.sourceChannels[it] }
        if (own == null) model.channels else {
            val rank = model.channels.withIndex().associate { it.value.id to it.index }
            own.sortedBy { rank[it.id] ?: Int.MAX_VALUE }
        }
    }

    Scaffold(
        topBar = {
            // The player window takes the place of the title bar.
            if (playing == null) TopAppBar(
                title = {
                    if (inServer) Text(serverTitle, fontWeight = FontWeight.Black, maxLines = 1)
                    else Text("APlay", color = Accent, fontWeight = FontWeight.Black)
                },
                navigationIcon = {
                    if (inServer) IconButton(onClick = { openServer = null }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (searchable) {
                        IconButton(onClick = {
                            searching = !searching
                            query = ""
                        }) {
                            Icon(
                                if (searching) Icons.Filled.Close else Icons.Filled.Search,
                                contentDescription = "Search",
                            )
                        }
                    }
                    IconButton(onClick = { model.showAddPlaylist() }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add playlist")
                    }
                    IconButton(onClick = model::refresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
        bottomBar = {
            if (!videoOnly) NavigationBar {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = item == tab,
                        onClick = {
                            if (item == Tab.SERVERS) openServer = null
                            tabIndex = item.ordinal
                        },
                        icon = {
                            Icon(
                                when (item) {
                                    Tab.MATCHES -> Icons.Filled.SportsSoccer
                                    Tab.CHANNELS -> Icons.Filled.LiveTv
                                    Tab.SERVERS -> Icons.Filled.Dns
                                    Tab.FAVORITES -> Icons.Filled.Favorite
                                    Tab.MORE -> Icons.Filled.Settings
                                },
                                contentDescription = item.label,
                            )
                        },
                        label = { Text(item.label, maxLines = 1, softWrap = false) },
                    )
                }
            }
        },
    ) { padding ->
        BoxWithConstraints(
            Modifier.fillMaxSize().padding(if (videoOnly) PaddingValues(0.dp) else padding),
        ) {
            val windowHeight = minOf(maxWidth * 9f / 16f, maxHeight * 0.5f)
            Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                // The video is drawn on top (below), so it is not rebuilt when its size changes.
                if (playing != null && !videoOnly && !mini) {
                    Spacer(Modifier.fillMaxWidth().height(windowHeight))
                }
                if (!videoOnly) {
                    if (!mini) NowPlayingBar(model)
                    if (model.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (searching && searchable && playing == null) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text("Search") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        )
                    }
                    // Shown while only the demo channels are loaded.
                    if (tab == Tab.CHANNELS && playing == null && model.playlists.isEmpty() &&
                        Config.DEFAULT_PLAYLIST_URL.isBlank()
                    ) {
                        Button(
                            onClick = { model.showAddPlaylist() },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        ) { Text("+ Add your playlist") }
                    }
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        when (tab) {
                            Tab.MATCHES -> MatchesScreen(model.matches, play)
                            Tab.CHANNELS -> ChannelsScreen(
                                model.channels, category, { category = it }, activeQuery,
                                model.favorites, model::toggleFavorite, playChannel,
                                playing?.let { it.channelId ?: "" },
                            )
                            Tab.SERVERS -> if (inServer) ChannelsScreen(
                                serverChannels, category, { category = it }, activeQuery,
                                model.favorites, model::toggleFavorite, playChannel,
                                playing?.let { it.channelId ?: "" },
                            ) else ServersScreen(
                                model, activeQuery,
                                onOpen = {
                                    category = ALL_CHANNELS
                                    searching = false
                                    query = ""
                                    openServer = it
                                },
                                onManage = { showServers = true },
                            )
                            Tab.FAVORITES -> FavoritesScreen(
                                model.channels, activeQuery, model.favorites,
                                model::toggleFavorite, playChannel,
                                playing?.let { it.channelId ?: "" },
                            )
                            Tab.MORE -> MoreScreen(model, play)
                        }
                    }
                }
            }
            if (playing != null) {
                val miniShape = RoundedCornerShape(12.dp)
                val miniModifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)
                    .width(200.dp).aspectRatio(16f / 9f).clip(miniShape)
                PlayerPane(
                    model, inPip || mini,
                    when {
                        videoOnly -> Modifier.fillMaxSize()
                        mini -> miniModifier
                        else -> Modifier.align(Alignment.TopStart).fillMaxWidth().height(windowHeight)
                    },
                )
                if (mini) {
                    // Tap the small window to open the full player; the cross closes it.
                    Box(miniModifier.clickable { tabIndex = Tab.CHANNELS.ordinal }) {
                        IconButton(
                            onClick = { model.closePlayer() },
                            modifier = Modifier.align(Alignment.TopEnd).size(32.dp),
                        ) {
                            Icon(
                                Icons.Filled.Close, contentDescription = "Close player",
                                tint = androidx.compose.ui.graphics.Color.White,
                            )
                        }
                    }
                }
            }
            }
        }
    }
}
