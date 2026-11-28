package com.aplay.tv

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import android.os.Build
import com.aplay.tv.ui.AplayLogo
import androidx.compose.material3.TopAppBarDefaults
import com.aplay.tv.ui.TvActivateScreen
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
        // If the app ever crashes, the reason is saved and shown the next time it opens.
        val crashFile = java.io.File(filesDir, "crash.txt")
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                crashFile.writeText(android.util.Log.getStackTraceString(error).take(5000))
            } catch (e: Throwable) {
                // nothing more can be done
            }
            previous?.uncaughtException(thread, error)
        }
        val lastCrash = try {
            crashFile.takeIf { it.exists() }?.readText().also { crashFile.delete() }
        } catch (e: Throwable) {
            null
        }
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
            // Keep the floating-window shape up to date, and open it when the button is pressed.
            LaunchedEffect(model.player.isPlaying, model.player.playing?.channelId, model.pictureInPicture) {
                updatePip()
            }
            LaunchedEffect(model.pipRequest) { if (model.pipRequest > 0) enterPip() }
            APlayTheme {
                if (model.isTv && !model.tvActive) TvActivateScreen(model) else App(model, inPip)
                var crash by remember { mutableStateOf(lastCrash) }
                crash?.let { report ->
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { crash = null },
                        title = { Text("The app closed last time") },
                        text = {
                            Text(
                                report,
                                fontSize = 10.sp,
                                modifier = Modifier.verticalScroll(rememberScrollState()),
                            )
                        },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = {
                                val clipboard = getSystemService(android.content.ClipboardManager::class.java)
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("crash", report))
                                crash = null
                            }) { Text("Copy") }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(onClick = { crash = null }) { Text("Close") }
                        },
                    )
                }
            }
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
        // From Android 12 the window enters by itself (see pipParams); older phones enter here.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S &&
            model.pictureInPicture && model.player.isPlaying && !model.skipPictureInPicture
        ) {
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

    /** The floating window takes the shape of the video, and opens by itself when the app is left. */
    private fun pipParams(): PictureInPictureParams {
        val size = model.player.exo?.videoSize
        val ratio = if (size != null && size.width > 0 && size.height > 0) {
            (size.width.toFloat() / size.height).coerceIn(0.42f, 2.39f)
        } else {
            16f / 9f
        }
        val builder = PictureInPictureParams.Builder().setAspectRatio(Rational((ratio * 1000).toInt(), 1000))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(
                model.pictureInPicture && model.player.isPlaying && !model.skipPictureInPicture
            )
        }
        return builder.build()
    }

    private fun updatePip() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) return
        try {
            setPictureInPictureParams(pipParams())
        } catch (e: IllegalStateException) {
            // The phone does not allow it right now.
        }
    }

    private fun enterPip() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) return
        try {
            if (enterPictureInPictureMode(pipParams())) inPip = true
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
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val tab = Tab.entries[tabIndex]
    // Channels has its own search field under the Now Playing card.
    val searchable = tab == Tab.SERVERS || tab == Tab.FAVORITES
    val activeQuery = if (searching && searchable) query else ""
    val playing = model.player.playing
    // Only the video is shown in full screen and in the floating window.
    val videoOnly = playing != null && (model.fullscreen || inPip)
    // In the settings menu the video shrinks to a small floating window.
    val mini = playing != null && !videoOnly && tab == Tab.MORE

    val play: (String, List<Stream>, Int) -> Unit = model.player::open
    val playChannel: (Channel, List<Channel>) -> Unit = { c, list ->
        model.rememberChannel(c.id)
        model.player.openChannel(c, list)
    }
    LaunchedEffect(model.channels.size, model.autoPlay) {
        if (model.autoPlay && !model.autoPlayDone && model.channels.isNotEmpty()) {
            model.autoPlayDone = true
            val last = model.lastChannelId()
            val target = model.channels.firstOrNull { it.id == last }
            if (target != null) {
                val list = model.channels.filter { it.source == target.source }.ifEmpty { listOf(target) }
                model.player.openChannel(target, list)
            }
        }
    }

    LaunchedEffect(model.message) {
        model.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            model.message = null
        }
    }
    BackHandler(enabled = playing != null || tabIndex != 0) {
        when {
            model.fullscreen -> model.fullscreen = false
            mini -> tabIndex = 0
            playing != null -> model.closePlayer()
            tab == Tab.CHANNELS -> tabIndex = Tab.SERVERS.ordinal
            else -> tabIndex = 0
        }
    }

    AddPlaylistDialog(model)
    if (showServers) ServerDialog(model) { showServers = false }

    // The Channels tab shows only the chosen server (the first one until a server is chosen).
    val currentServer = model.servers.firstOrNull { it.key == model.selectedServer && it.enabled }
        ?: model.servers.firstOrNull { it.enabled }
    val serverChannels = remember(model.channels, model.sourceChannels, currentServer?.key) {
        val own = currentServer?.let { model.sourceChannels[it.key] }
        if (own == null) model.channels else {
            val rank = model.channels.withIndex().associate { it.value.id to it.index }
            own.sortedBy { rank[it.id] ?: Int.MAX_VALUE }
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            // The player window takes the place of the title bar.
            if (playing == null) TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    scrolledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                ),
                title = {
                    if (tab == Tab.CHANNELS && currentServer != null) {
                        Text(currentServer.name, fontWeight = FontWeight.Black, maxLines = 1)
                    } else {
                        AplayLogo(32.dp)
                    }
                },
                navigationIcon = {
                    if (tab == Tab.CHANNELS) IconButton(onClick = { tabIndex = Tab.SERVERS.ordinal }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Servers")
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
                    IconButton(onClick = model::refresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
        bottomBar = {
            if (!videoOnly) AplayNavBar(tab) { tabIndex = it.ordinal }
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
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        when (tab) {
                            Tab.MATCHES -> MatchesScreen(model.matches, play)
                            Tab.CHANNELS -> ChannelsScreen(
                                serverChannels, category, { category = it }, "",
                                model.favorites, model::toggleFavorite, playChannel,
                                playing?.let { it.channelId ?: "" },
                            )
                            Tab.SERVERS -> ServersScreen(
                                model, activeQuery,
                                onOpen = {
                                    model.selectServer(it)
                                    category = ALL_CHANNELS
                                    searching = false
                                    query = ""
                                    tabIndex = Tab.CHANNELS.ordinal
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

private fun tabIcon(tab: Tab): ImageVector = when (tab) {
    Tab.MATCHES -> Icons.Filled.SportsSoccer
    Tab.CHANNELS -> Icons.Filled.LiveTv
    Tab.SERVERS -> Icons.Filled.Dns
    Tab.FAVORITES -> Icons.Filled.Favorite
    Tab.MORE -> Icons.Filled.Settings
}

/** A floating, rounded bar. The chosen tab sits in a glowing pill in the app colour. */
@Composable
private fun AplayNavBar(selected: Tab, onSelect: (Tab) -> Unit) {
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val idle = androidx.compose.ui.graphics.Color(0xFF9AA5BD)
    Box(Modifier.fillMaxWidth().background(androidx.compose.ui.graphics.Color(0xFF0B0F1A))) {
        Row(
            Modifier.fillMaxWidth().clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            androidx.compose.ui.graphics.Color(0xF2171E31),
                            androidx.compose.ui.graphics.Color(0xF20D1220),
                        )
                    )
                )
                .border(1.dp, androidx.compose.ui.graphics.Color(0xFF2A3350), shape)
                .navigationBarsPadding()
                .padding(start = 4.dp, end = 4.dp, top = 10.dp, bottom = 8.dp),
        ) {
            Tab.entries.forEach { item ->
                val active = item == selected
                Column(
                    Modifier.weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(item) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.width(66.dp).height(36.dp).clip(RoundedCornerShape(18.dp))
                            .then(
                                if (active) Modifier
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Accent.copy(alpha = 0.45f),
                                                androidx.compose.ui.graphics.Color(0xFF2B2F6E).copy(alpha = 0.6f),
                                            )
                                        )
                                    )
                                    .border(1.dp, Accent, RoundedCornerShape(18.dp))
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            tabIcon(item),
                            contentDescription = item.label,
                            tint = if (active) androidx.compose.ui.graphics.Color.White else idle,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Text(
                        item.label.uppercase(),
                        Modifier.padding(top = 4.dp),
                        color = if (active) androidx.compose.ui.graphics.Color.White else idle,
                        fontSize = 10.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}
