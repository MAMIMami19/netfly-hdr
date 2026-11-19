package com.nuvio.tv.ui.netflyhdr.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.ui.components.EmptyScreenState
import com.nuvio.tv.ui.components.GridContentCard
import com.nuvio.tv.ui.components.LoadingIndicator
import com.nuvio.tv.ui.components.PosterCardDefaults
import com.nuvio.tv.ui.components.PosterCardStyle
import com.nuvio.tv.ui.screens.search.MIN_SEARCH_QUERY_LENGTH
import com.nuvio.tv.ui.screens.search.SearchEvent
import com.nuvio.tv.ui.screens.search.SearchViewModel

/** Cap on flattened search results shown in the grid (§9 results column). */
private const val MAX_FLATTENED_RESULTS = 60

/**
 * Netfly search (§9): on-screen keyboard on the left, results on the right,
 * recent searches under the keyboard, and a Discover button.
 *
 * Querying, debouncing and the actual search run in the base
 * [SearchViewModel] — this screen only owns the Netfly layout. Results are
 * the base catalog rows flattened into one poster grid (deduplicated by
 * type+id), matching Netfly's results column.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NetflyHdrSearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
    onNavigateToDetail: (String, String, String) -> Unit,
    onOpenDiscover: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val firstKeyFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        repeat(2) { withFrameNanos { } }
        runCatching { firstKeyFocusRequester.requestFocus() }
    }

    val posterCardStyle = remember {
        PosterCardStyle(
            width = 110.dp,
            height = 165.dp,
            cornerRadius = 12.dp,
            focusedBorderWidth = PosterCardDefaults.Style.focusedBorderWidth,
            focusedScale = PosterCardDefaults.Style.focusedScale
        )
    }

    // Flatten the per-addon catalog rows into one deduplicated result list,
    // keeping each item's addon base URL for the detail navigation.
    val flattenedResults = remember(uiState.catalogRows) {
        val seen = HashSet<String>()
        val out = ArrayList<Pair<MetaPreview, String>>()
        for (row in uiState.catalogRows) {
            if (row.isLoading || row.items.isEmpty()) continue
            for (item in row.items) {
                val key = "${item.apiType}:${item.id}"
                if (seen.add(key)) {
                    out += item to row.addonBaseUrl
                    if (out.size >= MAX_FLATTENED_RESULTS) break
                }
            }
            if (out.size >= MAX_FLATTENED_RESULTS) break
        }
        out
    }

    val trimmedQuery = uiState.query.trim()
    val trimmedSubmittedQuery = uiState.submittedQuery.trim()
    val showRecentSearches = trimmedQuery.isEmpty() && uiState.recentSearches.isNotEmpty()

    val appendKey: (String) -> Unit = { key ->
        viewModel.onEvent(SearchEvent.QueryChanged(uiState.query + key))
    }
    val submitCurrentQuery: () -> Unit = {
        viewModel.onEvent(SearchEvent.SubmitSearch)
        viewModel.onEvent(SearchEvent.RememberSearchFromTextInput)
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 48.dp, vertical = 32.dp)
    ) {
        // Left: query display + keyboard + recent searches + Discover button.
        Column(
            modifier = Modifier
                .weight(0.44f)
                .fillMaxHeight()
                .padding(end = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.netflyhdr_search_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Query display box.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.border.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = uiState.query.ifBlank { stringResource(R.string.netflyhdr_search_hint) },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (uiState.query.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            NetflyHdrKeyboard(
                onKey = appendKey,
                onBackspace = {
                    if (uiState.query.isNotEmpty()) {
                        viewModel.onEvent(SearchEvent.QueryChanged(uiState.query.dropLast(1)))
                    }
                },
                onSpace = { appendKey(" ") },
                onClear = { viewModel.onEvent(SearchEvent.QueryChanged("")) },
                onSubmit = submitCurrentQuery,
                firstKeyFocusRequester = firstKeyFocusRequester
            )
            Spacer(modifier = Modifier.height(20.dp))
            if (showRecentSearches) {
                Text(
                    text = stringResource(R.string.netflyhdr_search_recent),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 8.dp)
                ) {
                    items(uiState.recentSearches, key = { it }) { recent ->
                        Button(
                            onClick = {
                                viewModel.onEvent(SearchEvent.QueryChanged(recent))
                                viewModel.onEvent(SearchEvent.SubmitSearch)
                            },
                            colors = ButtonDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedContentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text(text = recent, maxLines = 1)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.onEvent(SearchEvent.ClearRecentSearches) },
                    colors = ButtonDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(text = stringResource(R.string.netflyhdr_search_clear_history))
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onOpenDiscover,
                colors = ButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.netflyhdr_search_discover))
            }
        }

        // Right: results.
        Column(
            modifier = Modifier
                .weight(0.56f)
                .fillMaxHeight()
        ) {
            when {
                flattenedResults.isNotEmpty() -> {
                    if (trimmedSubmittedQuery.length >= MIN_SEARCH_QUERY_LENGTH) {
                        Text(
                            text = stringResource(
                                R.string.netflyhdr_search_results_for,
                                trimmedSubmittedQuery
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(
                            items = flattenedResults,
                            key = { (item, _) -> "${item.apiType}:${item.id}" }
                        ) { (item, addonBaseUrl) ->
                            GridContentCard(
                                item = item,
                                onClick = {
                                    viewModel.prefetchMetaOnFocus(item.id, item.rawType)
                                    onNavigateToDetail(item.id, item.apiType, addonBaseUrl)
                                },
                                posterCardStyle = posterCardStyle,
                                onFocused = {
                                    viewModel.prefetchMetaOnFocus(item.id, item.rawType)
                                }
                            )
                        }
                    }
                }
                uiState.isSearching -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingIndicator()
                    }
                }
                trimmedSubmittedQuery.length >= MIN_SEARCH_QUERY_LENGTH -> {
                    EmptyScreenState(
                        title = stringResource(R.string.netflyhdr_search_no_results_title),
                        subtitle = stringResource(R.string.netflyhdr_search_no_results_subtitle),
                        icon = Icons.Default.Search
                    )
                }
                else -> {
                    EmptyScreenState(
                        title = stringResource(R.string.netflyhdr_search_start_title),
                        subtitle = stringResource(R.string.netflyhdr_search_start_subtitle),
                        icon = Icons.Default.Search
                    )
                }
            }
        }
    }
}
