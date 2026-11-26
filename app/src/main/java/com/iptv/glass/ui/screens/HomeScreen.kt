package com.iptv.glass.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.iptv.glass.model.Channel
import com.iptv.glass.viewmodel.MainViewModel

/**
 * Kept for compatibility — the old Home tab is now the Channels tab.
 * Delegates to [ChannelsScreen], which is the main screen of the app.
 * Tapping a tile plays inline in the top player area; only the
 * Fullscreen button opens the fullscreen player screen.
 */
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onFullscreen: (Channel) -> Unit,
    onOpenDetail: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    ChannelsScreen(viewModel = viewModel, onFullscreen = onFullscreen, onOpenDetail = onOpenDetail, modifier = modifier)
}
