package com.nuvio.tv.ui.netflyhdr.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.CatalogRow
import com.nuvio.tv.domain.model.ContentType
import com.nuvio.tv.domain.model.LibraryEntry
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.domain.model.isSeriesLikeType
import com.nuvio.tv.ui.components.CatalogRowSection
import com.nuvio.tv.ui.components.EmptyScreenState
import com.nuvio.tv.ui.components.LoadingIndicator
import com.nuvio.tv.ui.components.PosterCardDefaults
import com.nuvio.tv.ui.components.PosterCardStyle
import com.nuvio.tv.ui.screens.library.LibraryViewModel

/**
 * Netfly Library / «My List» (§9): big title + subtitle + the saved items
 * rendered as home-style rows (Movies, Series, Other), plus an explanatory
 * empty message.
 *
 * Items come from the base [LibraryViewModel] (local list or the active
 * tracking source — whichever the library is using); each group is rendered
 * with the same [CatalogRowSection] the home screen uses.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NetflyHdrLibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
    onNavigateToDetail: (String, String, String?) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val watchedMovieIds by viewModel.watchedMovieIds.collectAsState()
    val watchedSeriesIds by viewModel.watchedSeriesIds.collectAsState()

    val moviesTitle = stringResource(R.string.netflyhdr_library_movies)
    val seriesTitle = stringResource(R.string.netflyhdr_library_series)
    val otherTitle = stringResource(R.string.netflyhdr_library_other)

    val rows = remember(uiState.visibleItems, moviesTitle, seriesTitle, otherTitle) {
        buildLibraryRows(uiState.visibleItems, moviesTitle, seriesTitle, otherTitle)
    }

    val posterCardStyle = remember {
        PosterCardStyle(
            cornerRadius = PosterCardDefaults.Style.cornerRadius,
            focusedBorderWidth = PosterCardDefaults.Style.focusedBorderWidth,
            focusedScale = PosterCardDefaults.Style.focusedScale
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 48.dp, vertical = 32.dp)
    ) {
        Text(
            text = stringResource(R.string.netflyhdr_library_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stringResource(R.string.netflyhdr_library_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        when {
            uiState.isLoading && uiState.visibleItems.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            }
            rows.isEmpty() -> {
                EmptyScreenState(
                    title = stringResource(R.string.netflyhdr_library_empty_title),
                    subtitle = stringResource(R.string.netflyhdr_library_empty_subtitle),
                    icon = Icons.Default.Bookmarks
                )
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(rows, key = { it.catalogId }) { row ->
                        CatalogRowSection(
                            catalogRow = row,
                            onItemClick = { id, type, _ ->
                                // The row carries no addon URL (library items come
                                // from mixed sources) — resolve the item's own.
                                val itemBaseUrl = row.items
                                    .firstOrNull { it.id == id }
                                    ?.sourceAddonBaseUrl
                                onNavigateToDetail(id, type, itemBaseUrl)
                            },
                            showSeeAll = false,
                            posterCardStyle = posterCardStyle,
                            showAddonName = false,
                            showCatalogTypeSuffix = false,
                            isItemWatched = { item ->
                                if (isSeriesLikeType(item.apiType)) item.id in watchedSeriesIds
                                else item.id in watchedMovieIds
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun buildLibraryRows(
    items: List<LibraryEntry>,
    moviesTitle: String,
    seriesTitle: String,
    otherTitle: String
): List<CatalogRow> {
    if (items.isEmpty()) return emptyList()
    val movies = ArrayList<LibraryEntry>()
    val series = ArrayList<LibraryEntry>()
    val other = ArrayList<LibraryEntry>()
    for (entry in items) {
        when {
            isSeriesLikeType(entry.type) -> series += entry
            entry.type.equals("movie", ignoreCase = true) -> movies += entry
            else -> other += entry
        }
    }
    val rows = ArrayList<CatalogRow>()
    if (movies.isNotEmpty()) {
        rows += libraryRow("netflyhdr_library_movies", moviesTitle, ContentType.MOVIE, movies)
    }
    if (series.isNotEmpty()) {
        rows += libraryRow("netflyhdr_library_series", seriesTitle, ContentType.SERIES, series)
    }
    if (other.isNotEmpty()) {
        rows += libraryRow("netflyhdr_library_other", otherTitle, ContentType.UNKNOWN, other)
    }
    return rows
}

private fun libraryRow(
    catalogId: String,
    catalogName: String,
    type: ContentType,
    entries: List<LibraryEntry>
): CatalogRow = CatalogRow(
    addonId = "netflyhdr_library",
    addonName = "",
    addonBaseUrl = "",
    catalogId = catalogId,
    catalogName = catalogName,
    type = type,
    items = entries.map { libraryEntryToMetaPreview(it) },
    hasMore = false
)

/**
 * Library entries are not MetaPreviews — build one, preserving the entry's
 * own addon URL so detail navigation resolves against the right source.
 */
private fun libraryEntryToMetaPreview(entry: LibraryEntry): MetaPreview = MetaPreview(
    id = entry.id,
    type = ContentType.fromString(entry.type),
    rawType = entry.type,
    name = entry.name,
    poster = entry.poster,
    posterShape = entry.posterShape,
    background = entry.background,
    logo = entry.logo,
    description = entry.description,
    releaseInfo = entry.releaseInfo,
    imdbRating = entry.imdbRating,
    genres = entry.genres,
    imdbId = entry.imdbId,
    landscapePoster = entry.landscapePoster,
    rawPosterUrl = entry.rawPosterUrl,
    sourceAddonBaseUrl = entry.addonBaseUrl
)
