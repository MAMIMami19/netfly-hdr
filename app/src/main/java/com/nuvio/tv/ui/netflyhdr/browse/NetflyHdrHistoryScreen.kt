package com.nuvio.tv.ui.netflyhdr.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.nuvio.tv.domain.model.WatchProgress
import com.nuvio.tv.ui.components.EmptyScreenState
import com.nuvio.tv.ui.components.NuvioDialog
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * History page (§9, route "netflyhdr_history"): the recently-watched list.
 * Selecting an entry opens its detail page. A clear-all action (with a
 * confirm dialog) empties the history.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NetflyHdrHistoryScreen(
    viewModel: NetflyHdrHistoryViewModel = hiltViewModel(),
    onNavigateToDetail: (String, String, String?) -> Unit
) {
    val items by viewModel.items.collectAsState()
    var showClearConfirm by remember { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    val backdropUrl = items.firstOrNull()?.backdrop

    NetflyHdrListScreen(
        title = stringResource(R.string.netflyhdr_history_title),
        subtitle = stringResource(R.string.netflyhdr_history_subtitle),
        backdropUrl = backdropUrl,
        headerActions = {
            if (items.isNotEmpty()) {
                Button(
                    onClick = { showClearConfirm = true },
                    colors = ButtonDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(text = stringResource(R.string.netflyhdr_history_clear))
                }
            }
        }
    ) {
        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                EmptyScreenState(
                    title = stringResource(R.string.netflyhdr_history_empty_title),
                    subtitle = stringResource(R.string.netflyhdr_history_empty_subtitle),
                    icon = Icons.Default.History
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(
                    items = items,
                    key = { "${it.contentId}:${it.videoId}" }
                ) { progress ->
                    val watchedDate = remember(progress.lastWatched) {
                        Instant.ofEpochMilli(progress.lastWatched)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                            .format(dateFormatter)
                    }
                    NetflyHdrListRow(
                        title = historyTitle(progress),
                        subtitle = historySubtitle(progress, watchedDate),
                        onClick = {
                            onNavigateToDetail(
                                progress.contentId,
                                progress.contentType,
                                progress.addonBaseUrl
                            )
                        },
                        leading = {
                            if (!progress.poster.isNullOrBlank()) {
                                AsyncImage(
                                    model = progress.poster,
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

    if (showClearConfirm) {
        NuvioDialog(
            onDismiss = { showClearConfirm = false },
            title = stringResource(R.string.netflyhdr_history_clear_confirm_title),
            subtitle = stringResource(R.string.netflyhdr_history_clear_confirm_body)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        viewModel.clearAll()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                        focusedContainerColor = MaterialTheme.colorScheme.error,
                        focusedContentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(text = stringResource(R.string.netflyhdr_history_clear_yes))
                }
                Button(
                    onClick = { showClearConfirm = false },
                    colors = ButtonDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(text = stringResource(R.string.netflyhdr_common_cancel))
                }
            }
        }
    }
}

@Composable
private fun historyTitle(progress: WatchProgress): String {
    val parts = buildList {
        add(progress.name)
        if (progress.season != null && progress.episode != null) {
            add(
                stringResource(
                    R.string.netflyhdr_calendar_season_episode,
                    progress.season,
                    progress.episode
                )
            )
        }
        progress.episodeTitle?.takeIf { it.isNotBlank() }?.let { add(it) }
    }
    return parts.joinToString(" — ")
}

@Composable
private fun historySubtitle(progress: WatchProgress, watchedDate: String): String {
    val kind = if (progress.contentType.equals("series", ignoreCase = true)) {
        ""
    } else {
        stringResource(R.string.netflyhdr_history_movie) + " · "
    }
    val status = if (progress.isCompleted()) {
        stringResource(R.string.netflyhdr_history_completed)
    } else {
        "${(progress.progressPercentage * 100).toInt()}%"
    }
    return "$kind$status · $watchedDate"
}
