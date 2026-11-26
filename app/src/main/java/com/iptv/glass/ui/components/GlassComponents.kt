package com.iptv.glass.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.iptv.glass.data.ChannelLogoResolver
import com.iptv.glass.model.Channel
import com.iptv.glass.model.EpgProgram
import com.iptv.glass.ui.theme.GlassMuted
import com.iptv.glass.ui.theme.GlassText
import com.iptv.glass.ui.theme.GlassWhite12
import com.iptv.glass.ui.theme.LiveRed
import com.iptv.glass.ui.theme.MonogramPalettes
import com.iptv.glass.ui.theme.NexaCard
import com.iptv.glass.ui.theme.NexaCardBorder
import com.iptv.glass.ui.theme.NexaChipBackground
import com.iptv.glass.ui.theme.NexaCyan
import com.iptv.glass.ui.theme.NexaPlayerBlack

val GlassShape = RoundedCornerShape(16.dp)
val GlassShapeSmall = RoundedCornerShape(12.dp)

/** Standard icon size used across the whole app — 22dp, like the reference. */
val NexaIconSize: Dp = 22.dp

/** One polished icon: filled Material icon, consistent size and tint. */
@Composable
fun NexaIcon(
    icon: ImageVector,
    contentDescription: String?,
    tint: Color = GlassText,
    size: Dp = NexaIconSize
) {
    Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(size))
}

/** Icon button with the standard 22dp icon (search, reload, share, …). */
@Composable
fun NexaIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    tint: Color = GlassText
) {
    IconButton(onClick = onClick) {
        NexaIcon(icon, contentDescription, tint = tint)
    }
}

/**
 * Small square icon button from the reference action row: 40dp dark
 * rounded square with a border — grey border normally, cyan border
 * for the highlighted Fullscreen button.
 */
@Composable
fun NexaSquareIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    tint: Color = GlassText,
    borderColor: Color = NexaCardBorder
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(NexaCard)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        NexaIcon(icon, contentDescription, tint = tint)
    }
}

/**
 * App icon tile — cyan-bordered rounded square with a cyan play triangle,
 * like the reference app's header icon. Used on the More screen header.
 */
@Composable
fun AppIconTile(modifier: Modifier = Modifier, iconSize: Dp = 28.dp) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Transparent)
            .border(1.5.dp, NexaCyan, RoundedCornerShape(14.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.PlayArrow,
            contentDescription = null,
            tint = NexaCyan,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Settings-row icon tile — 40dp dark rounded square with a cyan 22dp icon,
 * like the reference app's PLAYBACK & STREAMING rows.
 */
@Composable
fun SettingsIconTile(icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(NexaCard)
            .border(1.dp, NexaCardBorder, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        NexaIcon(icon, contentDescription = null, tint = NexaCyan)
    }
}

/** Dark card: navy surface + faint border, like the reference app's panels. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val base = Modifier
        .clip(GlassShape)
        .background(NexaCard)
        .border(1.dp, NexaCardBorder, GlassShape)
    Box(
        modifier = if (onClick != null) base.then(modifier.clickable { onClick() }) else base.then(modifier),
        contentAlignment = Alignment.Center
    ) { content() }
}

/**
 * Category chip — dark pill; selected chip gets a cyan border, cyan text
 * and a faint cyan fill (reference app style). 13sp, 10dp/8dp padding.
 */
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
            .background(if (selected) NexaCyan.copy(alpha = 0.16f) else NexaChipBackground)
            .border(
                1.dp,
                if (selected) NexaCyan else NexaCardBorder,
                CircleShape
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) NexaCyan else GlassText,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

/**
 * Monogram fallback tile: channel initials in bold white on a stable
 * gradient picked from the channel name (see ChannelLogoResolver).
 * Drawn on the same white rounded square as a real logo, so the grid
 * never shows a broken or empty tile.
 */
@Composable
fun MonogramTile(
    channel: Channel,
    modifier: Modifier = Modifier
) {
    val palette = MonogramPalettes[ChannelLogoResolver.paletteIndex(channel.name)]
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(palette.first, palette.second))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                ChannelLogoResolver.monogram(channel.name),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Channel logo on a WHITE rounded square, exactly like the reference
 * app's tiles — logos are designed for a white background.
 *
 * Logo source (see ChannelLogoResolver): playlist `tvg-logo` first,
 * then the iptv-org database looked up by channel name, loaded from the
 * internet with Coil (crossfade, disk + memory cache). While the image
 * is loading — or when no logo exists anywhere — the monogram tile
 * shows instead, never a generic icon and never an empty box.
 */
@Composable
fun ChannelLogo(
    channel: Channel,
    modifier: Modifier = Modifier,
    logoUrl: String? = null
) {
    val url = (logoUrl ?: channel.logoUrl).trim()
    var loadFailed by remember(url) { mutableStateOf(false) }
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Monogram sits underneath: it is the instant placeholder while
        // Coil loads, and the permanent fallback when loading fails.
        MonogramTile(channel, modifier = Modifier.fillMaxSize())
        if (url.isNotBlank() && !loadFailed) {
            AsyncImage(
                model = url,
                contentDescription = channel.name,
                contentScale = ContentScale.Fit,
                onError = { loadFailed = true },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(8.dp)
            )
        }
    }
}

/**
 * The reference app's channel tile, exactly: dark card, white logo
 * square on top, channel name in white below it. No extra badges or
 * buttons inside the tile — tap the tile to play.
 */
@Composable
fun ChannelTile(
    channel: Channel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(GlassShape)
            .background(NexaCard)
            .border(1.dp, NexaCardBorder, GlassShape)
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ChannelLogo(channel, modifier = Modifier.fillMaxWidth().aspectRatio(1f))
        Spacer(Modifier.height(8.dp))
        Text(
            channel.name,
            color = GlassText,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * The tile grid used by every tab: exactly 3 columns on phones and
 * 4 columns on wide/tablet screens, 10dp gaps, 12dp outer padding —
 * the reference app's grid.
 */
@Composable
fun ChannelTileGrid(
    channels: List<Channel>,
    onTileClick: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = if (maxWidth < 600.dp) GridCells.Fixed(3) else GridCells.Fixed(4)
        LazyVerticalGrid(
            columns = columns,
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(channels, key = { it.id }) { channel ->
                ChannelTile(channel = channel) { onTileClick(channel) }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

/**
 * Pure-black top player area, like the reference app's video area at
 * the top of the Channels screen. It is a preview of the channel that
 * is playing (or a hint when nothing is playing yet) — tapping it
 * opens the full player.
 */
@Composable
fun PlayerArea(
    channel: Channel?,
    nowProgram: EpgProgram?,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
            .background(NexaPlayerBlack)
            .clickable(enabled = channel != null) { onPlay() },
        contentAlignment = Alignment.Center
    ) {
        if (channel == null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, NexaCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = NexaCyan,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Tap a channel below to start watching",
                    color = GlassMuted,
                    fontSize = 13.sp
                )
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(NexaCyan),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    channel.name,
                    color = GlassText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(LiveRed))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        (nowProgram?.title ?: "Live") + "  •  Tap for fullscreen",
                        color = GlassMuted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** "NOW PLAYING CHANNEL" bar: cyan left bar, red dot, name, SERVER 1 pill. */
@Composable
fun NowPlayingBar(
    channel: Channel,
    nowProgram: EpgProgram?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(NexaCyan)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(8.dp).clip(CircleShape).background(LiveRed)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "NOW PLAYING CHANNEL",
                        color = NexaCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    channel.name,
                    color = GlassText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    nowProgram?.title ?: "No EPG info",
                    color = GlassMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            // Server selector — only one server exists, labelled honestly.
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(GlassWhite12)
                    .border(1.dp, NexaCardBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(LiveRed))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "SERVER 1",
                        color = GlassText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/** One channel row (kept for the detail-adjacent lists). */
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
                NexaIcon(
                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favourite",
                    tint = if (isFavorite) LiveRed else GlassMuted
                )
            }
            NexaIcon(Icons.Default.PlayArrow, contentDescription = "Play", tint = GlassText)
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

/** Section heading in cyan caps, like "PLAYBACK & STREAMING". */
@Composable
fun NexaSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = NexaCyan,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}
