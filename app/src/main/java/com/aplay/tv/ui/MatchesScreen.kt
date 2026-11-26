package com.aplay.tv.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

private const val ALL_LEAGUES = "All leagues"
private val FILTERS = listOf("All", "Live", "Today", "Upcoming")

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

    val leagues = listOf(ALL_LEAGUES) + matches.map { it.league }.distinct()
    val activeLeague = if (league in leagues) league else ALL_LEAGUES
    val day = SimpleDateFormat("yyyyMMdd", Locale.US)
    val today = day.format(Date(now))
    val visible = matches
        .filter { activeLeague == ALL_LEAGUES || it.league == activeLeague }
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
        ChipRow(leagues, activeLeague) { league = it }
        ChipRow(FILTERS, filter) { filter = it }
        if (visible.isEmpty()) {
            EmptyMessage("No matches here right now.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
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
private fun MatchCard(match: Match, now: Long, onClick: () -> Unit) {
    val status = match.status(now)
    val start = Date(match.startMillis)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = match.league.uppercase(),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TeamColumn(match.home, Modifier.weight(1f))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(start),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                    Text(
                        text = SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(start),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = when (status) {
                            MatchStatus.LIVE -> "● LIVE"
                            MatchStatus.UPCOMING -> countdown(match.startMillis - now)
                            MatchStatus.ENDED -> "Ended"
                        },
                        color = when (status) {
                            MatchStatus.LIVE -> LiveRed
                            MatchStatus.UPCOMING -> Accent
                            MatchStatus.ENDED -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                }
                TeamColumn(match.away, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (match.servers.size == 1) "1 server" else "${match.servers.size} servers",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TeamColumn(team: Team, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Logo(team.logo, team.name, Modifier.size(52.dp))
        Spacer(Modifier.height(6.dp))
        Text(
            text = team.name,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontSize = 14.sp,
        )
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
