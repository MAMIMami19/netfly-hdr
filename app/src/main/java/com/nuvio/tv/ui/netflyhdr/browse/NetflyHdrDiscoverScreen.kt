package com.nuvio.tv.ui.netflyhdr.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.DiscoverLocation
import com.nuvio.tv.ui.components.EmptyScreenState
import com.nuvio.tv.ui.components.GridContentCard
import com.nuvio.tv.ui.components.LoadingIndicator
import com.nuvio.tv.ui.components.PosterCardDefaults
import com.nuvio.tv.ui.components.PosterCardStyle
import com.nuvio.tv.ui.screens.search.SearchEvent
import com.nuvio.tv.ui.screens.search.SearchViewModel
import com.nuvio.tv.ui.util.localizedContentType
import com.nuvio.tv.ui.util.localizedGenreLabel

/**
 * Netfly Discover (§9): a genre-chips bar over a results grid.
 *
 * Catalog loading, type/genre selection and pagination all live in the base
 * [SearchViewModel] (its discover state); this screen replaces the base
 * dropdown pickers with Netfly's chip bar. Type chips switch the catalog
 * type (the ViewModel auto-picks a catalog for the type); genre chips filter
 * the selected catalog.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NetflyHdrDiscoverScreen(
    viewModel: SearchViewModel = hiltViewModel(),
    onNavigateToDetail: (String, String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.discoverLocation) {
        if (uiState.discoverLocation != DiscoverLocation.OFF) {
            viewModel.ensureDiscoverLoaded()
        }
    }

    val posterCardStyle = remember(uiState.posterCardWidthDp, uiState.posterCardCornerRadiusDp) {
        PosterCardStyle(
            width = uiState.posterCardWidthDp.dp,
            height = (uiState.posterCardWidthDp * 1.5f).toInt().dp,
            cornerRadius = uiState.posterCardCornerRadiusDp.dp,
            focusedBorderWidth = PosterCardDefaults.Style.focusedBorderWidth,
            focusedScale = PosterCardDefaults.Style.focusedScale
        )
    }

    val availableTypes = remember(uiState.discoverCatalogs) {
        uiState.discoverCatalogs.map { it.type }.distinct()
    }
    val selectedCatalog = uiState.discoverCatalogs
        .firstOrNull { it.key == uiState.selectedDiscoverCatalogKey }
    val genres = selectedCatalog?.genres.orEmpty()

    if (uiState.discoverLocation == DiscoverLocation.OFF) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 48.dp, vertical = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.netflyhdr_discover_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))
            EmptyScreenState(
                title = stringResource(R.string.netflyhdr_discover_disabled_title),
                subtitle = stringResource(R.string.netflyhdr_discover_disabled_subtitle),
                icon = Icons.Default.Explore
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 48.dp, vertical = 32.dp)
    ) {
        Text(
            text = stringResource(R.string.netflyhdr_discover_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Type chips.
        if (availableTypes.size > 1) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 8.dp)
            ) {
                items(availableTypes, key = { it }) { type ->
                    val selected = type == uiState.selectedDiscoverType
                    DiscoverChip(
                        label = localizedContentType(context, type),
                        selected = selected,
                        onClick = { viewModel.onEvent(SearchEvent.SelectDiscoverType(type)) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Genre chips bar (§9 DiscoverBar).
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 8.dp)
        ) {
            item(key = "__all__") {
                DiscoverChip(
                    label = stringResource(R.string.netflyhdr_discover_all_genres),
                    selected = uiState.selectedDiscoverGenre == null,
                    onClick = { viewModel.onEvent(SearchEvent.SelectDiscoverGenre(null)) }
                )
            }
            items(genres, key = { it }) { genre ->
                DiscoverChip(
                    label = localizedGenreLabel(genre),
                    selected = genre == uiState.selectedDiscoverGenre,
                    onClick = { viewModel.onEvent(SearchEvent.SelectDiscoverGenre(genre)) }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        when {
            uiState.discoverLoading && uiState.discoverResults.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            }
            uiState.discoverResults.isEmpty() -> {
                EmptyScreenState(
                    title = stringResource(R.string.netflyhdr_discover_no_results),
                    subtitle = stringResource(R.string.netflyhdr_discover_no_results_subtitle),
                    icon = Icons.Default.Explore
                )
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(
                        items = uiState.discoverResults,
                        key = { "${it.apiType}:${it.id}" }
                    ) { item ->
                        GridContentCard(
                            item = item,
                            onClick = {
                                onNavigateToDetail(
                                    item.id,
                                    item.apiType,
                                    selectedCatalog?.addonBaseUrl.orEmpty()
                                )
                            },
                            posterCardStyle = posterCardStyle,
                            onFocused = {
                                viewModel.prefetchMetaOnFocus(item.id, item.rawType)
                            }
                        )
                    }
                    if (uiState.discoverHasMore) {
                        item(key = "__load_more__") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.onEvent(SearchEvent.LoadNextDiscoverResults)
                                    },
                                    colors = ButtonDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        focusedContentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Text(text = stringResource(R.string.netflyhdr_discover_load_more))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DiscoverChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.colors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            focusedContainerColor = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant,
            focusedContentColor = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(text = label, maxLines = 1)
    }
}
