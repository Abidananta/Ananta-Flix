package com.aplay.tv.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aplay.tv.Match
import com.aplay.tv.MatchStatus
import com.aplay.tv.Stream
import com.aplay.tv.Team
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val ALL_LEAGUES = "All"
private val Muted = Color(0xFF9AA5BD)

@Composable
fun MatchesScreen(matches: List<Match>, onPlay: (String, List<Stream>, Int) -> Unit) {
    var league by rememberSaveable { mutableStateOf(ALL_LEAGUES) }
    var filter by rememberSaveable { mutableStateOf("All") }
    var picked by remember { mutableStateOf<Match?>(null) }
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(15_000)
        }
    }

    val leagues = matches.map { it.league }.distinct()
    val activeLeague = if (league in leagues) league else ALL_LEAGUES
    val day = SimpleDateFormat("yyyyMMdd", Locale.US)
    val today = day.format(Date(now))
    val inLeague = matches.filter { activeLeague == ALL_LEAGUES || it.league == activeLeague }
    val liveCount = inLeague.count { it.status(now) == MatchStatus.LIVE }
    val todayCount = inLeague.count { day.format(Date(it.startMillis)) == today }
    val upcomingCount = inLeague.count { it.status(now) == MatchStatus.UPCOMING }
    val visible = inLeague
        .filter {
            when (filter) {
                "Live" -> it.status(now) == MatchStatus.LIVE
                "Today" -> day.format(Date(it.startMillis)) == today
                "Upcoming" -> it.status(now) == MatchStatus.UPCOMING
                else -> true
            }
        }
        .sortedWith(compareBy<Match>({ it.status(now).ordinal }, { it.startMillis }))

    Column(Modifier.fillMaxSize()) {
        // League logo tiles
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                LeagueTile("All", "", activeLeague == ALL_LEAGUES) { league = ALL_LEAGUES }
            }
            items(leagues) { name ->
                val logo = matches.firstOrNull { it.league == name }?.leagueLogo.orEmpty()
                LeagueTile(name, logo, activeLeague == name) { league = name }
            }
        }
        // Filter chips with counts
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { CountChip("All (${inLeague.size})", filter == "All") { filter = "All" } }
            item { CountChip("Live now ($liveCount)", filter == "Live") { filter = "Live" } }
            item { CountChip("Today's ($todayCount)", filter == "Today") { filter = "Today" } }
            item { CountChip("Upcoming ($upcomingCount)", filter == "Upcoming") { filter = "Upcoming" } }
        }
        if (visible.isEmpty()) {
            EmptyMessage("No matches here right now.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(visible) { match ->
                    MatchCard(match, now) {
                        if (match.servers.size == 1) {
                            onPlay(title(match), match.servers, 0)
                        } else {
                            picked = match
                        }
                    }
                }
            }
        }
    }

    picked?.let { match ->
        AlertDialog(
            onDismissRequest = { picked = null },
            title = { Text(title(match)) },
            text = {
                if (match.servers.isEmpty()) {
                    Text("No stream has been added for this match yet.")
                } else {
                    LazyColumn {
                        items(match.servers.size) { index ->
                            TextButton(
                                onClick = {
                                    picked = null
                                    onPlay(title(match), match.servers, index)
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(match.servers[index].name) }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { picked = null }) { Text("Close") } },
        )
    }
}

private fun title(match: Match) = "${match.home.name} vs ${match.away.name}"

@Composable
private fun LeagueTile(name: String, logo: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.width(72.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(60.dp).clip(shape)
                .background(if (selected) Accent.copy(alpha = 0.16f) else GlassFill)
                .border(if (selected) 2.dp else 1.dp, if (selected) Accent else GlassStroke, shape),
            contentAlignment = Alignment.Center,
        ) {
            if (name == ALL_LEAGUES) {
                AplayMark(Modifier.size(32.dp))
            } else {
                Logo(logo, name, Modifier.size(40.dp))
            }
        }
        Text(
            name, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            color = if (selected) Accent else Muted,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun CountChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Text(
        text,
        color = if (selected) Color.White else Muted,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        fontSize = 13.sp,
        modifier = Modifier.clip(shape)
            .background(if (selected) Accent.copy(alpha = 0.30f) else GlassFill)
            .border(1.dp, if (selected) Accent else GlassStroke, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun MatchCard(match: Match, now: Long, onClick: () -> Unit) {
    val status = match.status(now)
    val start = Date(match.startMillis)
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(GlassFill)
            .border(1.dp, GlassStroke, shape)
            .clickable(onClick = onClick),
    ) {
        // Header
        Row(
            Modifier.fillMaxWidth().background(Color(0x22000000)).padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (match.leagueLogo.isNotBlank()) Logo(match.leagueLogo, match.league, Modifier.size(18.dp))
            Text(
                "${match.league.uppercase()} • ${match.home.name.uppercase()} VS ${match.away.name.uppercase()}",
                fontSize = 11.sp, color = Muted, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            // Left: date & time
            Column(Modifier.width(92.dp)) {
                Text(
                    SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(start),
                    fontSize = 12.sp, color = Muted,
                )
                Text(
                    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(start),
                    fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Accent,
                )
            }
            // Teams
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TeamLine(match.home)
                TeamLine(match.away)
            }
            Box(Modifier.padding(horizontal = 12.dp).width(1.dp).height(52.dp).background(GlassStroke))
            // Status
            Column(Modifier.width(64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = when (status) {
                        MatchStatus.LIVE -> "● LIVE"
                        MatchStatus.UPCOMING -> countdown(match.startMillis - now)
                        MatchStatus.ENDED -> "Ended"
                    },
                    color = when (status) {
                        MatchStatus.LIVE -> LiveRed
                        MatchStatus.UPCOMING -> Accent
                        MatchStatus.ENDED -> Muted
                    },
                    fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 2,
                )
            }
        }
        // Footer: server chips
        Row(
            Modifier.fillMaxWidth().background(Color(0x22000000)).padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            match.servers.take(3).forEach { s ->
                Text(
                    s.name, fontSize = 10.sp, color = Color.White, maxLines = 1,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Accent.copy(alpha = 0.25f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                "${match.servers.size} ${if (match.servers.size == 1) "SERVER" else "SERVERS"}",
                fontSize = 11.sp, color = Muted, fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun TeamLine(team: Team) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Logo(team.logo, team.name, Modifier.size(28.dp))
        Text(team.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun countdown(millis: Long): String {
    val minutes = (millis + 59_999) / 60_000
    return when {
        minutes >= 1440 -> "in ${minutes / 1440}d ${(minutes % 1440) / 60}h"
        minutes >= 60 -> "in ${minutes / 60}h ${minutes % 60}m"
        else -> "in ${minutes}m"
    }
}
