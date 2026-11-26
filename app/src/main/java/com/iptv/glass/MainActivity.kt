package com.iptv.glass

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.iptv.glass.ui.components.NexaIcon
import com.iptv.glass.ui.components.NexaIconSize
import com.iptv.glass.ui.screens.CategoriesScreen
import com.iptv.glass.ui.screens.ChannelsScreen
import com.iptv.glass.ui.screens.DetailScreen
import com.iptv.glass.ui.screens.FavoritesScreen
import com.iptv.glass.ui.screens.MatchesScreen
import com.iptv.glass.ui.screens.MoreScreen
import com.iptv.glass.ui.screens.PlayerScreen
import com.iptv.glass.ui.theme.AppGradient
import com.iptv.glass.ui.theme.GlassIPTVTheme
import com.iptv.glass.ui.theme.GlassMuted
import com.iptv.glass.ui.theme.GlassText
import com.iptv.glass.ui.theme.NexaBottomBar
import com.iptv.glass.ui.theme.NexaCyan
import com.iptv.glass.viewmodel.MainViewModel

/**
 * Bottom navigation, modelled on the reference app:
 * MATCHES, CHANNELS, CATEGORIES, FAVORITES, MORE.
 * Channels is the start (main) tab. The selected tab's icon sits on a
 * cyan rounded pill, like the reference screenshots.
 */
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Matches : Screen("matches", "MATCHES", Icons.Default.Shield)
    data object Channels : Screen("channels", "CHANNELS", Icons.Default.LiveTv)
    data object Categories : Screen("categories", "CATEGORIES", Icons.Default.Layers)
    data object Favorites : Screen("favorites", "FAVORITES", Icons.Default.Favorite)
    data object More : Screen("more", "MORE", Icons.Default.Tune)
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            GlassIPTVTheme {
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route
                val showBottomBar = currentRoute in listOf(
                    Screen.Matches.route, Screen.Channels.route, Screen.Categories.route,
                    Screen.Favorites.route, Screen.More.route
                )

                // Inline playback (Nexa-style): every Play action starts
                // the stream in the Channels screen's top player area and
                // goes to that tab — no new window opens. Only the
                // Fullscreen button opens the fullscreen player screen.
                val playInline: (com.iptv.glass.model.Channel) -> Unit = { channel ->
                    viewModel.playChannel(channel)
                    navController.navigate(Screen.Channels.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                val openFullscreen: (com.iptv.glass.model.Channel) -> Unit = { channel ->
                    navController.navigate("player/${channel.id}")
                }

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NexaBottomBar(navController, currentRoute)
                        }
                    }
                ) { innerPadding ->
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(AppGradient)
                            .padding(innerPadding)
                    ) {
                        NavHost(navController, startDestination = Screen.Channels.route) {
                            composable(Screen.Matches.route) {
                                MatchesScreen(viewModel, onPlay = playInline)
                            }
                            composable(Screen.Channels.route) {
                                ChannelsScreen(
                                    viewModel = viewModel,
                                    onFullscreen = openFullscreen,
                                    onOpenDetail = { navController.navigate("detail/${it.id}") }
                                )
                            }
                            composable(Screen.Categories.route) {
                                CategoriesScreen(viewModel) { category ->
                                    viewModel.selectedCategory.value = category
                                    navController.navigate(Screen.Channels.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                            composable(Screen.Favorites.route) {
                                FavoritesScreen(viewModel, onPlay = playInline)
                            }
                            composable(Screen.More.route) {
                                // playNetworkStream() already starts inline
                                // playback in the ViewModel; just go to the
                                // Channels tab so the video is visible.
                                MoreScreen(viewModel) { _ ->
                                    navController.navigate(Screen.Channels.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                            // Fullscreen ONLY: reuses the shared player that
                            // is already playing inline — no re-buffering.
                            composable("player/{channelId}") { entry ->
                                val channel = viewModel.findChannel(entry.arguments?.getString("channelId").orEmpty())
                                if (channel != null) {
                                    PlayerScreen(viewModel, channel) { navController.popBackStack() }
                                } else {
                                    Text("Channel not found", color = GlassText)
                                }
                            }
                            composable("detail/{channelId}") { entry ->
                                val channel = viewModel.findChannel(entry.arguments?.getString("channelId").orEmpty())
                                if (channel != null) {
                                    DetailScreen(
                                        channel = channel,
                                        viewModel = viewModel,
                                        onPlay = playInline,
                                        onBack = { navController.popBackStack() }
                                    )
                                } else {
                                    Text("Channel not found", color = GlassText)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NexaBottomBar(navController: NavHostController, currentRoute: String?) {
    val tabs = listOf(
        Screen.Matches, Screen.Channels, Screen.Categories, Screen.Favorites, Screen.More
    )
    NavigationBar(containerColor = NexaBottomBar) {
        tabs.forEach { tab ->
            val selected = currentRoute == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Box(
                        modifier = if (selected) {
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(NexaCyan.copy(alpha = 0.22f))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        } else {
                            Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        }
                    ) {
                        NexaIcon(
                            tab.icon,
                            contentDescription = tab.label,
                            tint = if (selected) NexaCyan else GlassMuted,
                            size = NexaIconSize
                        )
                    }
                },
                label = {
                    Text(
                        tab.label,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NexaCyan,
                    selectedTextColor = GlassText,
                    unselectedIconColor = GlassMuted,
                    unselectedTextColor = GlassMuted,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
