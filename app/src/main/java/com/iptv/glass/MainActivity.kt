package com.iptv.glass

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.iptv.glass.ui.screens.DetailScreen
import com.iptv.glass.ui.screens.FavoritesScreen
import com.iptv.glass.ui.screens.HomeScreen
import com.iptv.glass.ui.screens.LiveTvScreen
import com.iptv.glass.ui.screens.MoviesScreen
import com.iptv.glass.ui.screens.PlayerScreen
import com.iptv.glass.ui.screens.SeriesScreen
import com.iptv.glass.ui.theme.AppGradient
import com.iptv.glass.ui.theme.GlassIPTVTheme
import com.iptv.glass.ui.theme.GlassMuted
import com.iptv.glass.ui.theme.GlassText
import com.iptv.glass.viewmodel.MainViewModel

sealed class Screen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object Live : Screen("live", "Live TV", Icons.Default.LiveTv)
    data object Movies : Screen("movies", "Movies", Icons.Default.Movie)
    data object Series : Screen("series", "Series", Icons.Default.Tv)
    data object Favorites : Screen("favorites", "Favourites", Icons.Default.Favorite)
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
                    Screen.Home.route, Screen.Live.route, Screen.Movies.route,
                    Screen.Series.route, Screen.Favorites.route
                )

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            GlassBottomBar(navController, currentRoute)
                        }
                    }
                ) { innerPadding ->
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(AppGradient)
                            .padding(innerPadding)
                    ) {
                        NavHost(navController, startDestination = Screen.Home.route) {
                            composable(Screen.Home.route) {
                                HomeScreen(
                                    viewModel = viewModel,
                                    onPlay = { navController.navigate("player/${it.id}") },
                                    onOpenDetail = { navController.navigate("detail/${it.id}") }
                                )
                            }
                            composable(Screen.Live.route) {
                                LiveTvScreen(viewModel) { navController.navigate("player/${it.id}") }
                            }
                            composable(Screen.Movies.route) {
                                MoviesScreen(viewModel) { navController.navigate("detail/${it.id}") }
                            }
                            composable(Screen.Series.route) {
                                SeriesScreen(viewModel) { navController.navigate("detail/${it.id}") }
                            }
                            composable(Screen.Favorites.route) {
                                FavoritesScreen(viewModel) { navController.navigate("player/${it.id}") }
                            }
                            composable("player/{channelId}") { entry ->
                                val channel = viewModel.findChannel(entry.arguments?.getString("channelId").orEmpty())
                                if (channel != null) {
                                    PlayerScreen(channel, viewModel) { navController.popBackStack() }
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
                                        onPlay = { navController.navigate("player/${it.id}") },
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
private fun GlassBottomBar(navController: NavHostController, currentRoute: String?) {
    val tabs = listOf(Screen.Home, Screen.Live, Screen.Movies, Screen.Series, Screen.Favorites)
    NavigationBar(containerColor = Color(0xCC0B1026)) {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = {
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = GlassText,
                    selectedTextColor = GlassText,
                    unselectedIconColor = GlassMuted,
                    unselectedTextColor = GlassMuted,
                    indicatorColor = Color.White.copy(alpha = 0.12f)
                )
            )
        }
    }
}
