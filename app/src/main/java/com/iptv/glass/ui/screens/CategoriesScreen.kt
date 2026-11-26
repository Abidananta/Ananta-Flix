package com.iptv.glass.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iptv.glass.ui.components.GlassCard
import com.iptv.glass.ui.components.NexaIcon
import com.iptv.glass.ui.theme.GlassMuted
import com.iptv.glass.ui.theme.GlassText
import com.iptv.glass.ui.theme.NexaCyan
import com.iptv.glass.viewmodel.MainViewModel

/**
 * Categories — every group-title from the playlist with its channel count.
 * Tapping one filters the Channels tab to that category.
 */
@Composable
fun CategoriesScreen(
    viewModel: MainViewModel,
    onOpenCategory: (String) -> Unit
) {
    val categories by viewModel.categories.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Column(Modifier.fillMaxSize()) {
        Text(
            "Categories",
            style = MaterialTheme.typography.headlineMedium,
            color = GlassText,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        if (categories.isEmpty()) {
            Text(
                if (uiState.isLoading) "Loading…" else "No categories yet — the playlist is empty.",
                color = GlassMuted,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            return@Column
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(categories, key = { it.first }) { (name, count) ->
                GlassCard(onClick = { onOpenCategory(name) }) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NexaIcon(Icons.Default.Folder, contentDescription = null, tint = NexaCyan)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(name, color = GlassText, fontWeight = FontWeight.SemiBold)
                            Text(
                                "$count channel${if (count == 1) "" else "s"}",
                                color = GlassMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        NexaIcon(Icons.Default.ChevronRight, contentDescription = null, tint = GlassMuted)
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
