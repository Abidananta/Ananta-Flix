package com.aplay.tv

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsSoccer
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aplay.tv.ui.ALL_CHANNELS
import com.aplay.tv.ui.APlayTheme
import com.aplay.tv.ui.Accent
import com.aplay.tv.ui.CategoriesScreen
import com.aplay.tv.ui.ChannelsScreen
import com.aplay.tv.ui.FavoritesScreen
import com.aplay.tv.ui.MatchesScreen
import com.aplay.tv.ui.MoreScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent { APlayTheme { App() } }
    }
}

private enum class Tab(val label: String) {
    MATCHES("Matches"),
    CHANNELS("Channels"),
    CATEGORIES("Categories"),
    FAVORITES("Favorites"),
    MORE("More"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun App(model: AppViewModel = viewModel()) {
    val context = LocalContext.current
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    var category by rememberSaveable { mutableStateOf(ALL_CHANNELS) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val tab = Tab.entries[tabIndex]
    val searchable = tab == Tab.CHANNELS || tab == Tab.CATEGORIES || tab == Tab.FAVORITES
    val activeQuery = if (searching && searchable) query else ""

    val play: (String, List<Stream>, Int) -> Unit = { title, streams, index ->
        context.startActivity(PlayerActivity.intent(context, title, streams, index))
    }
    val playChannel: (Channel) -> Unit = { play(it.name, listOf(it.stream), 0) }

    LaunchedEffect(model.message) {
        model.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            model.message = null
        }
    }
    BackHandler(enabled = tabIndex != 0) { tabIndex = 0 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("APlay", color = Accent, fontWeight = FontWeight.Black) },
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
            NavigationBar {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = item == tab,
                        onClick = { tabIndex = item.ordinal },
                        icon = {
                            Icon(
                                when (item) {
                                    Tab.MATCHES -> Icons.Filled.SportsSoccer
                                    Tab.CHANNELS -> Icons.Filled.LiveTv
                                    Tab.CATEGORIES -> Icons.Filled.Category
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
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (model.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (searching && searchable) {
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
                        model.channels, category, { category = it }, activeQuery,
                        model.favorites, model::toggleFavorite, playChannel,
                    )
                    Tab.CATEGORIES -> CategoriesScreen(model.channels, activeQuery) {
                        category = it
                        searching = false
                        tabIndex = Tab.CHANNELS.ordinal
                    }
                    Tab.FAVORITES -> FavoritesScreen(
                        model.channels, activeQuery, model.favorites,
                        model::toggleFavorite, playChannel,
                    )
                    Tab.MORE -> MoreScreen(model, play)
                }
            }
        }
    }
}
