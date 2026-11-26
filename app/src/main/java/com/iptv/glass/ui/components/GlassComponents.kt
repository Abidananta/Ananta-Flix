package com.iptv.glass.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.iptv.glass.model.Channel
import com.iptv.glass.model.EpgProgram
import com.iptv.glass.ui.theme.AccentGradient
import com.iptv.glass.ui.theme.GlassMuted
import com.iptv.glass.ui.theme.GlassText
import com.iptv.glass.ui.theme.GlassWhite12
import com.iptv.glass.ui.theme.GlassWhite20
import com.iptv.glass.ui.theme.LiveRed

val GlassShape = RoundedCornerShape(20.dp)
val GlassShapeSmall = RoundedCornerShape(14.dp)

/** Frosted-glass card: translucent white fill + faint white border + soft shadow. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val base = Modifier
        .clip(GlassShape)
        .background(
            Brush.linearGradient(
                listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.05f))
            )
        )
        .border(1.dp, GlassWhite20, GlassShape)
    Box(
        modifier = if (onClick != null) base.then(modifier.clickable { onClick() }) else base.then(modifier),
        contentAlignment = Alignment.Center
    ) { content() }
}

/** Translucent pill chip, filled with the accent gradient when selected. */
@Composable
fun GlassChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (selected) AccentGradient else GlassWhite12)
            .border(1.dp, if (selected) Color.Transparent else GlassWhite20, CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = GlassText, style = MaterialTheme.typography.labelLarge)
    }
}

/** Channel logo loaded with Coil; falls back to a LiveTv icon on empty/error URL. */
@Composable
fun ChannelLogo(
    channel: Channel,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center
    ) {
        if (channel.logoUrl.isNotBlank()) {
            AsyncImage(
                model = channel.logoUrl,
                contentDescription = channel.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(6.dp)
            )
        } else {
            Icon(Icons.Default.LiveTv, contentDescription = null, tint = GlassMuted, modifier = Modifier.size(28.dp))
        }
    }
}

/** One channel row/card used by Home, Live TV, Search and Favourites. */
@Composable
fun ChannelCard(
    channel: Channel,
    isFavorite: Boolean,
    nowProgram: EpgProgram?,
    nextProgram: EpgProgram?,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChannelLogo(channel, modifier = Modifier.size(56.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        channel.name,
                        color = GlassText,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (channel.isLive) {
                        Spacer(Modifier.width(8.dp))
                        LiveBadge()
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(channel.category, color = GlassMuted, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(2.dp))
                // EPG: current programme, or "No EPG info" when EPG is off / absent.
                Text(
                    text = nowProgram?.let { "Now: ${it.title}" }
                        ?: nextProgram?.let { "Next: ${it.title}" }
                        ?: "No EPG info",
                    color = GlassMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favourite",
                    tint = if (isFavorite) LiveRed else GlassMuted
                )
            }
            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = GlassText)
        }
    }
}

@Composable
fun LiveBadge() {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(LiveRed)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("LIVE", color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}
