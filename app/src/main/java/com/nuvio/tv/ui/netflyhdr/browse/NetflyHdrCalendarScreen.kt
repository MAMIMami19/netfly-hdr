package com.nuvio.tv.ui.netflyhdr.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.nuvio.tv.R
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Episode calendar page (§9, route "netflyhdr_calendar"): list of upcoming
 * episodes — date · title + S/E — grouped by air date. Selecting an entry
 * opens the series detail page.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NetflyHdrCalendarScreen(
    viewModel: NetflyHdrCalendarViewModel = hiltViewModel(),
    onNavigateToDetail: (String, String, String?) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val today = remember { LocalDate.now(ZoneId.systemDefault()) }
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    // Backdrop: the first upcoming show's art, dimmed by the ListScreen.
    val backdropUrl = uiState.days.firstOrNull()
        ?.entries?.firstOrNull()
        ?.backdrop

    NetflyHdrListScreen(
        title = stringResource(R.string.netflyhdr_calendar_title),
        subtitle = stringResource(R.string.netflyhdr_calendar_subtitle),
        backdropUrl = backdropUrl,
        detailLine = stringResource(R.string.netflyhdr_calendar_window)
    ) {
        when {
            uiState.loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.netflyhdr_calendar_loading),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            uiState.error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.layout.Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.netflyhdr_calendar_error),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Button(
                            onClick = { viewModel.load() },
                            colors = ButtonDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedContentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text(text = stringResource(R.string.netflyhdr_common_retry))
                        }
                    }
                }
            }
            uiState.days.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    com.nuvio.tv.ui.components.EmptyScreenState(
                        title = stringResource(R.string.netflyhdr_calendar_empty_title),
                        subtitle = stringResource(R.string.netflyhdr_calendar_empty_subtitle),
                        icon = Icons.Default.CalendarMonth
                    )
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    uiState.days.forEach { day ->
                        item(key = "day_${day.date}") {
                            Text(
                                text = dayLabel(day.date, today, dateFormatter),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                        }
                        items(
                            items = day.entries,
                            key = { "${it.showId}:${it.season}:${it.episode}" }
                        ) { entry ->
                            val seasonEpisode = stringResource(
                                R.string.netflyhdr_calendar_season_episode,
                                entry.season,
                                entry.episode
                            )
                            NetflyHdrListRow(
                                title = "${entry.showName} — $seasonEpisode",
                                subtitle = entry.episodeTitle,
                                onClick = {
                                    onNavigateToDetail(entry.showId, "series", entry.addonBaseUrl)
                                },
                                leading = {
                                    if (!entry.poster.isNullOrBlank()) {
                                        AsyncImage(
                                            model = entry.poster,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun dayLabel(
    date: LocalDate,
    today: LocalDate,
    formatter: DateTimeFormatter
): String {
    return when (date) {
        today -> stringResource(R.string.netflyhdr_calendar_today)
        today.plusDays(1) -> stringResource(R.string.netflyhdr_calendar_tomorrow)
        else -> date.format(formatter)
    }
}
