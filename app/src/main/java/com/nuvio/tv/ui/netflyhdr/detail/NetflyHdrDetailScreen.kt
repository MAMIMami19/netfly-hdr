package com.nuvio.tv.ui.netflyhdr.detail

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.ContentType
import com.nuvio.tv.domain.model.Meta
import com.nuvio.tv.domain.model.MetaCastMember
import com.nuvio.tv.domain.model.Video
import com.nuvio.tv.domain.model.WatchProgress
import com.nuvio.tv.ui.components.ErrorState
import com.nuvio.tv.ui.components.LoadingIndicator
import com.nuvio.tv.ui.components.LocalPlaybackAvailability
import com.nuvio.tv.ui.components.TrailerPlayer
import com.nuvio.tv.ui.netflyhdr.NetflyHdrAccent
import com.nuvio.tv.ui.netflyhdr.NetflyHdrDimensions
import com.nuvio.tv.ui.netflyhdr.cards.NetflyPosterCard
import com.nuvio.tv.ui.netflyhdr.cards.toNetflyCardItem
import com.nuvio.tv.ui.screens.detail.MetaDetailsEvent
import com.nuvio.tv.ui.screens.detail.MetaDetailsUiState
import com.nuvio.tv.ui.screens.detail.MetaDetailsViewModel
import com.nuvio.tv.ui.screens.detail.resolveHeroPlaybackVideo
import com.nuvio.tv.ui.util.localizedGenreLabel

/**
 * Netfly HDR detail page (NOTE.md §8), on route [com.nuvio.tv.ui.netflyhdr.NetflyHdrRoutes.Detail].
 *
 * Fullscreen hero + pill button row (with icons) + status line + bottom zones
 * navigable by arrows: buttons, seasons, episodes, cast, tabs (more like this
 * / about), organizers (collection). Focus moves between zones with the
 * default DPAD traversal of the single LazyColumn.
 *
 * Data comes from the shared [MetaDetailsViewModel] (same as the base detail
 * page), so playback, library, watched state and trailers behave identically.
 * Only real metadata is shown — IMDb, year, runtime and genres appear only
 * when the loaded meta actually carries them; no quality badges are invented.
 *
 * Fullscreen trailer: the "trailer playing · press any key to return" hint
 * is shown while the trailer plays; any key (or BACK) returns to the page.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NetflyHdrDetailScreen(
    viewModel: MetaDetailsViewModel = hiltViewModel(),
    onBackPress: () -> Unit,
    onNavigateToCastDetail: (personId: Int, personName: String, preferCrew: Boolean) -> Unit = { _, _, _ -> },
    onNavigateToDetail: (itemId: String, itemType: String, addonBaseUrl: String?) -> Unit = { _, _, _ -> },
    onPlayClick: (
        videoId: String,
        contentType: String,
        contentId: String,
        title: String,
        poster: String?,
        backdrop: String?,
        logo: String?,
        season: Int?,
        episode: Int?,
        episodeName: String?,
        genres: String?,
        year: String?,
        runtime: Int?,
        contentLanguage: String?
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
    onPlayManuallyClick: (
        videoId: String,
        contentType: String,
        contentId: String,
        title: String,
        poster: String?,
        backdrop: String?,
        logo: String?,
        season: Int?,
        episode: Int?,
        episodeName: String?,
        genres: String?,
        year: String?,
        runtime: Int?,
        contentLanguage: String?
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colorScheme = MaterialTheme.colorScheme
    val isTrailerOverlayVisible =
        uiState.isTrailerPlaying && !uiState.trailerUrl.isNullOrBlank()

    BackHandler {
        if (isTrailerOverlayVisible) {
            viewModel.onEvent(MetaDetailsEvent.OnTrailerBackPress)
        } else {
            onBackPress()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        when {
            uiState.isLoading && uiState.meta == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator(color = NetflyHdrAccent)
                }
            }

            uiState.error != null && uiState.meta == null -> {
                ErrorState(
                    message = uiState.error ?: stringResource(R.string.error_generic),
                    onRetry = { viewModel.onEvent(MetaDetailsEvent.OnRetry) },
                    modifier = Modifier.fillMaxSize()
                )
            }

            uiState.meta != null -> {
                NetflyHdrDetailContent(
                    meta = uiState.meta!!,
                    uiState = uiState,
                    viewModel = viewModel,
                    onNavigateToCastDetail = onNavigateToCastDetail,
                    onNavigateToDetail = onNavigateToDetail,
                    onPlayClick = onPlayClick,
                    onPlayManuallyClick = onPlayManuallyClick
                )
            }
        }

        if (isTrailerOverlayVisible) {
            NetflyHdrTrailerOverlay(
                trailerUrl = uiState.trailerUrl!!,
                trailerAudioUrl = uiState.trailerAudioUrl,
                onAnyKey = { viewModel.onEvent(MetaDetailsEvent.OnTrailerBackPress) },
                onEnded = { viewModel.onEvent(MetaDetailsEvent.OnTrailerEnded) }
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NetflyHdrDetailContent(
    meta: Meta,
    uiState: MetaDetailsUiState,
    viewModel: MetaDetailsViewModel,
    onNavigateToCastDetail: (personId: Int, personName: String, preferCrew: Boolean) -> Unit,
    onNavigateToDetail: (itemId: String, itemType: String, addonBaseUrl: String?) -> Unit,
    onPlayClick: (
        videoId: String, contentType: String, contentId: String, title: String,
        poster: String?, backdrop: String?, logo: String?, season: Int?, episode: Int?,
        episodeName: String?, genres: String?, year: String?, runtime: Int?,
        contentLanguage: String?
    ) -> Unit,
    onPlayManuallyClick: (
        videoId: String, contentType: String, contentId: String, title: String,
        poster: String?, backdrop: String?, logo: String?, season: Int?, episode: Int?,
        episodeName: String?, genres: String?, year: String?, runtime: Int?,
        contentLanguage: String?
    ) -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val playbackAvailability = LocalPlaybackAvailability.current
    val playFocusRequester = remember { FocusRequester() }

    val isSeries = remember(meta.type, meta.videos) {
        meta.type == ContentType.SERIES || meta.videos.isNotEmpty()
    }
    val genresString = remember(meta.genres) {
        meta.genres.takeIf { it.isNotEmpty() }
            ?.joinToString(" • ") { localizedGenreLabel(context, it) }
    }
    val yearString = remember(meta.releaseInfo) {
        meta.releaseInfo?.trim()?.takeIf { it.isNotBlank() }
    }
    val heroVideo = remember(meta, uiState.nextToWatch, uiState.episodesForSeason) {
        resolveHeroPlaybackVideo(
            meta = meta,
            nextToWatch = uiState.nextToWatch,
            episodesForSeason = uiState.episodesForSeason
        )
    }

    val playEpisode: (Video) -> Unit = playEpisode@{ video ->
        if (!playbackAvailability.canStream(meta.apiType, video.id, meta.id, video)) {
            Toast.makeText(context, R.string.playback_unavailable_message, Toast.LENGTH_SHORT).show()
            return@playEpisode
        }
        onPlayClick(
            video.id,
            meta.apiType,
            meta.id,
            meta.name,
            video.thumbnail ?: meta.poster,
            meta.backdropUrl,
            meta.logo,
            video.season,
            video.episode,
            video.title,
            null,
            null,
            video.runtime,
            meta.resolveContentLanguage()
        )
    }
    val playEpisodeManually: (Video) -> Unit = playEpisodeManually@{ video ->
        if (!playbackAvailability.canStream(meta.apiType, video.id, meta.id, video)) {
            Toast.makeText(context, R.string.playback_unavailable_message, Toast.LENGTH_SHORT).show()
            return@playEpisodeManually
        }
        onPlayManuallyClick(
            video.id,
            meta.apiType,
            meta.id,
            meta.name,
            video.thumbnail ?: meta.poster,
            meta.backdropUrl,
            meta.logo,
            video.season,
            video.episode,
            video.title,
            null,
            null,
            video.runtime,
            meta.resolveContentLanguage()
        )
    }
    val playTitle: (String) -> Unit = playTitle@{ videoId ->
        if (!playbackAvailability.canStream(meta.apiType, videoId, meta.id)) {
            Toast.makeText(context, R.string.playback_unavailable_message, Toast.LENGTH_SHORT).show()
            return@playTitle
        }
        onPlayClick(
            videoId,
            meta.apiType,
            meta.id,
            meta.name,
            meta.poster,
            meta.backdropUrl,
            meta.logo,
            null,
            null,
            null,
            genresString,
            yearString,
            null,
            meta.resolveContentLanguage()
        )
    }
    val playTitleManually: (String) -> Unit = playTitleManually@{ videoId ->
        if (!playbackAvailability.canStream(meta.apiType, videoId, meta.id)) {
            Toast.makeText(context, R.string.playback_unavailable_message, Toast.LENGTH_SHORT).show()
            return@playTitleManually
        }
        onPlayManuallyClick(
            videoId,
            meta.apiType,
            meta.id,
            meta.name,
            meta.poster,
            meta.backdropUrl,
            meta.logo,
            null,
            null,
            null,
            genresString,
            yearString,
            null,
            meta.resolveContentLanguage()
        )
    }
    val onHeroPlayClick: () -> Unit = {
        if (heroVideo != null) playEpisode(heroVideo) else playTitle(meta.id)
    }
    val onHeroPlayManuallyClick: () -> Unit = {
        if (heroVideo != null) playEpisodeManually(heroVideo) else playTitleManually(meta.id)
    }

    // Play button label from real progress state only.
    val nextToWatch = uiState.nextToWatch
    val playLabel = when {
        nextToWatch?.isResume == true -> stringResource(R.string.netflyhdr_detail_resume)
        isSeries && nextToWatch?.nextSeason != null && nextToWatch.nextEpisode != null ->
            stringResource(
                R.string.hero_play_episode,
                nextToWatch.nextSeason,
                nextToWatch.nextEpisode
            )
        else -> stringResource(R.string.hero_play)
    }
    val trailerAvailable =
        uiState.trailerButtonEnabled || !uiState.trailerUrl.isNullOrBlank()

    // Initial focus goes to the Play pill once meta is loaded.
    LaunchedEffect(meta.id) {
        repeat(2) { withFrameNanos { } }
        runCatching { playFocusRequester.requestFocus() }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 48.dp)
    ) {
        // Zone: hero (fullscreen-width backdrop + logo/title + real metadata).
        item {
            NetflyHdrDetailHero(meta = meta)
        }

        // Zone: pill button row.
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = NetflyHdrDetailMargins.CONTENT_START, top = 16.dp, end = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NetflyHdrDetailPillButton(
                    label = playLabel,
                    icon = Icons.Filled.PlayArrow,
                    primary = true,
                    focusRequester = playFocusRequester,
                    onClick = onHeroPlayClick
                )
                if (trailerAvailable) {
                    NetflyHdrDetailPillButton(
                        label = stringResource(R.string.hero_play_trailer),
                        icon = Icons.Filled.Movie,
                        onClick = { viewModel.onEvent(MetaDetailsEvent.OnTrailerButtonClick) }
                    )
                }
                NetflyHdrDetailPillButton(
                    label = if (uiState.isInLibrary) {
                        stringResource(R.string.netflyhdr_detail_in_my_list)
                    } else {
                        stringResource(R.string.netflyhdr_detail_add_to_my_list)
                    },
                    icon = if (uiState.isInLibrary) Icons.Filled.Check else Icons.Filled.Add,
                    enabled = !uiState.defaultLibraryTogglePending,
                    onClick = { viewModel.onEvent(MetaDetailsEvent.OnToggleLibrary) }
                )
                if (!isSeries) {
                    NetflyHdrDetailPillButton(
                        label = if (uiState.isMovieWatched) {
                            stringResource(R.string.netflyhdr_detail_watched)
                        } else {
                            stringResource(R.string.netflyhdr_detail_mark_watched)
                        },
                        icon = Icons.Filled.Check,
                        enabled = !uiState.isMovieWatchedPending,
                        onClick = { viewModel.onEvent(MetaDetailsEvent.OnToggleMovieWatched) }
                    )
                }
                NetflyHdrDetailPillButton(
                    label = stringResource(R.string.play_manually),
                    icon = Icons.Filled.PlayArrow,
                    onClick = onHeroPlayManuallyClick
                )
            }
        }

        // Status line: real state only (list membership, next episode, status).
        val statusSegments = buildList {
            if (uiState.isInLibrary) {
                add(context.getString(R.string.netflyhdr_detail_in_my_list))
            }
            if (isSeries && nextToWatch?.nextSeason != null && nextToWatch.nextEpisode != null) {
                val nextTitle = uiState.episodesForSeason.firstOrNull {
                    it.season == nextToWatch.nextSeason && it.episode == nextToWatch.nextEpisode
                }?.title?.trim()?.takeIf { it.isNotBlank() }
                val nextLabel = context.getString(
                    R.string.netflyhdr_detail_next_episode,
                    nextToWatch.nextSeason,
                    nextToWatch.nextEpisode
                )
                add(if (nextTitle != null) "$nextLabel · $nextTitle" else nextLabel)
            }
            meta.status?.trim()?.takeIf { it.isNotBlank() }?.let { add(it) }
        }
        if (statusSegments.isNotEmpty()) {
            item {
                Text(
                    text = statusSegments.joinToString("  •  "),
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = NetflyHdrDetailMargins.CONTENT_START,
                            top = 12.dp,
                            end = 24.dp
                        )
                )
            }
        }

        // Zone: seasons (series only).
        if (isSeries && uiState.seasons.isNotEmpty()) {
            item {
                NetflyHdrDetailZoneHeader(
                    text = stringResource(R.string.netflyhdr_detail_seasons)
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(
                        start = NetflyHdrDetailMargins.CONTENT_START,
                        end = 24.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.seasons, key = { it }) { season ->
                        NetflyHdrSeasonChip(
                            season = season,
                            selected = season == uiState.selectedSeason,
                            onClick = {
                                viewModel.onEvent(MetaDetailsEvent.OnSeasonSelected(season))
                            }
                        )
                    }
                }
            }
        }

        // Zone: episodes (series only).
        if (isSeries) {
            item {
                NetflyHdrDetailZoneHeader(
                    text = stringResource(R.string.netflyhdr_detail_episodes)
                )
            }
            if (uiState.episodesForSeason.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.netflyhdr_detail_no_episodes),
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(
                            start = NetflyHdrDetailMargins.CONTENT_START,
                            end = 24.dp
                        )
                    )
                }
            } else {
                items(uiState.episodesForSeason, key = { it.id }) { episode ->
                    NetflyHdrEpisodeRow(
                        episode = episode,
                        progress = uiState.episodeProgressMap[episode.season to episode.episode],
                        isWatched = uiState.watchedEpisodes.contains(
                            episode.season to episode.episode
                        ),
                        onClick = { playEpisode(episode) },
                        modifier = Modifier.padding(
                            start = NetflyHdrDetailMargins.CONTENT_START,
                            end = 24.dp,
                            bottom = 8.dp
                        )
                    )
                }
            }
        }

        // Zone: cast.
        if (meta.castMembers.isNotEmpty()) {
            item {
                NetflyHdrDetailZoneHeader(
                    text = stringResource(R.string.detail_tab_cast)
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(
                        start = NetflyHdrDetailMargins.CONTENT_START,
                        end = 24.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(meta.castMembers, key = { it.name }) { member ->
                        NetflyHdrCastItem(
                            member = member,
                            onClick = {
                                val tmdbId = member.tmdbId
                                if (tmdbId != null) {
                                    onNavigateToCastDetail(tmdbId, member.name, false)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Zone: tabs — more like this.
        if (uiState.moreLikeThis.isNotEmpty()) {
            item {
                NetflyHdrDetailZoneHeader(
                    text = stringResource(R.string.detail_tab_more_like_this)
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(
                        start = NetflyHdrDetailMargins.CONTENT_START,
                        end = 24.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(NetflyHdrDimensions.posterGap)
                ) {
                    items(uiState.moreLikeThis, key = { it.id }) { preview ->
                        NetflyPosterCard(
                            item = preview.toNetflyCardItem(
                                isWatched = uiState.relatedWatchedStatus[preview.id] == true
                            ),
                            onClick = {
                                onNavigateToDetail(
                                    preview.id,
                                    preview.apiType,
                                    preview.sourceAddonBaseUrl
                                )
                            }
                        )
                    }
                }
            }
        }

        // Zone: tabs — about (real facts only).
        item {
            NetflyHdrDetailZoneHeader(
                text = stringResource(R.string.netflyhdr_detail_about)
            )
        }
        item {
            NetflyHdrDetailAbout(meta = meta)
        }

        // Zone: organizers — collection.
        if (uiState.collection.isNotEmpty()) {
            item {
                NetflyHdrDetailZoneHeader(
                    text = uiState.collectionName?.trim()?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.netflyhdr_detail_collection)
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(
                        start = NetflyHdrDetailMargins.CONTENT_START,
                        end = 24.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(NetflyHdrDimensions.posterGap)
                ) {
                    items(uiState.collection, key = { it.id }) { preview ->
                        NetflyPosterCard(
                            item = preview.toNetflyCardItem(
                                isWatched = uiState.relatedWatchedStatus[preview.id] == true
                            ),
                            onClick = {
                                onNavigateToDetail(
                                    preview.id,
                                    preview.apiType,
                                    preview.sourceAddonBaseUrl
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

private object NetflyHdrDetailMargins {
    // Left margin 152 px (§14).
    val CONTENT_START = 76.dp
}

@Composable
private fun NetflyHdrDetailHero(meta: Meta) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val backdropUrl = meta.background ?: meta.poster
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
    ) {
        if (!backdropUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(backdropUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorScheme.surfaceVariant)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            colorScheme.background.copy(alpha = 0.85f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(
                    start = NetflyHdrDetailMargins.CONTENT_START,
                    end = 24.dp,
                    bottom = 8.dp
                )
        ) {
            if (!meta.logo.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(meta.logo)
                        .crossfade(true)
                        .build(),
                    contentDescription = meta.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(60.dp)
                )
            } else {
                Text(
                    text = meta.name,
                    color = colorScheme.onSurface,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // Metadata line: only values the loaded meta actually carries.
            val metaSegments = buildList {
                meta.imdbRating?.let { rating ->
                    add("IMDb ${"%.1f".format(rating)}")
                }
                meta.releaseInfo?.trim()?.takeIf { it.isNotBlank() }?.let { add(it) }
                meta.runtime?.trim()?.takeIf { it.isNotBlank() }?.let { add(it) }
                meta.genres.takeIf { it.isNotEmpty() }?.let { genres ->
                    add(
                        genres.take(3).joinToString(" • ") {
                            localizedGenreLabel(context, it)
                        }
                    )
                }
            }
            if (metaSegments.isNotEmpty()) {
                Text(
                    text = metaSegments.joinToString("  •  "),
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (!meta.description.isNullOrBlank()) {
                Text(
                    text = meta.description,
                    color = colorScheme.onSurface,
                    fontSize = 14.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .padding(top = 8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NetflyHdrDetailPillButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val pillShape = RoundedCornerShape(50)
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.then(
            if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier
        ),
        colors = ButtonDefaults.colors(
            containerColor = if (primary) NetflyHdrAccent else colorScheme.surfaceVariant,
            focusedContainerColor = if (primary) NetflyHdrAccent else colorScheme.surfaceVariant,
            contentColor = if (primary) Color.White else colorScheme.onSurface,
            focusedContentColor = if (primary) Color.White else colorScheme.onSurface
        ),
        shape = ButtonDefaults.shape(shape = pillShape),
        border = ButtonDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(NetflyHdrDimensions.focusRingWidth, NetflyHdrAccent),
                shape = pillShape
            )
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun NetflyHdrDetailZoneHeader(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = NetflyHdrDetailMargins.CONTENT_START,
                top = 24.dp,
                bottom = 8.dp,
                end = 24.dp
            )
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NetflyHdrSeasonChip(
    season: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val pillShape = RoundedCornerShape(50)
    Button(
        onClick = onClick,
        colors = ButtonDefaults.colors(
            containerColor = if (selected) NetflyHdrAccent else colorScheme.surfaceVariant,
            focusedContainerColor = if (selected) NetflyHdrAccent else colorScheme.surfaceVariant,
            contentColor = if (selected) Color.White else colorScheme.onSurface,
            focusedContentColor = if (selected) Color.White else colorScheme.onSurface
        ),
        shape = ButtonDefaults.shape(shape = pillShape),
        border = ButtonDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(NetflyHdrDimensions.focusRingWidth, NetflyHdrAccent),
                shape = pillShape
            )
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.netflyhdr_detail_season_number, season),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NetflyHdrEpisodeRow(
    episode: Video,
    progress: WatchProgress?,
    isWatched: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val rowShape = RoundedCornerShape(NetflyHdrDimensions.posterCornerRadius)
    val progressFraction = progress
        ?.takeIf { it.duration > 0 }
        ?.let { (it.position.toFloat() / it.duration.toFloat()).coerceIn(0f, 1f) }
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        colors = CardDefaults.colors(
            containerColor = colorScheme.surface,
            focusedContainerColor = colorScheme.surfaceVariant
        ),
        border = CardDefaults.border(
            border = Border.None,
            focusedBorder = Border(
                border = BorderStroke(NetflyHdrDimensions.focusRingWidth, NetflyHdrAccent),
                shape = rowShape
            )
        ),
        shape = CardDefaults.shape(shape = rowShape),
        scale = CardDefaults.scale(focusedScale = 1f, pressedScale = 1f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!episode.thumbnail.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(episode.thumbnail)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(96.dp, 52.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(96.dp, 52.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = episode.episode?.let {
                                stringResource(R.string.netflyhdr_detail_episode_number, it)
                            } ?: "",
                            color = colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = buildString {
                            if (episode.season != null && episode.episode != null) {
                                append("S${episode.season} E${episode.episode} · ")
                            }
                            append(episode.title)
                        },
                        color = colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!episode.overview.isNullOrBlank()) {
                        Text(
                            text = episode.overview,
                            color = colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                episode.runtime?.let { runtime ->
                    Text(
                        text = stringResource(R.string.netflyhdr_detail_minutes, runtime),
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                if (isWatched) {
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(NetflyHdrDimensions.watchedBadgeSize)
                            .clip(CircleShape)
                            .background(NetflyHdrAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
            if (progressFraction != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(NetflyHdrDimensions.progressBarHeight)
                        .background(Color.Black.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressFraction)
                            .background(NetflyHdrAccent)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NetflyHdrCastItem(
    member: MetaCastMember,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(72.dp)
            // Cast without a TMDB id has nowhere to go — keep it unfocusable.
            .focusProperties { canFocus = member.tmdbId != null },
        colors = CardDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent
        ),
        border = CardDefaults.border(border = Border.None),
        shape = CardDefaults.shape(shape = RoundedCornerShape(8.dp)),
        scale = CardDefaults.scale(focusedScale = 1f, pressedScale = 1f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!member.photo.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(member.photo)
                        .crossfade(true)
                        .build(),
                    contentDescription = member.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = member.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        color = colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = member.name,
                color = colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp)
            )
            if (!member.character.isNullOrBlank()) {
                Text(
                    text = member.character,
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun NetflyHdrDetailAbout(meta: Meta) {
    val colorScheme = MaterialTheme.colorScheme
    val facts = buildList {
        if (meta.director.isNotEmpty()) {
            add(stringResource(R.string.netflyhdr_detail_director) to meta.director.joinToString(", "))
        }
        if (meta.writer.isNotEmpty()) {
            add(stringResource(R.string.netflyhdr_detail_writer) to meta.writer.joinToString(", "))
        }
        if (!meta.country.isNullOrBlank()) {
            add(stringResource(R.string.netflyhdr_detail_country) to meta.country)
        }
        if (!meta.language.isNullOrBlank()) {
            add(stringResource(R.string.netflyhdr_detail_language) to meta.language)
        }
        if (!meta.status.isNullOrBlank()) {
            add(stringResource(R.string.netflyhdr_detail_status) to meta.status)
        }
        if (meta.networks.isNotEmpty()) {
            add(
                stringResource(R.string.netflyhdr_detail_network) to
                    meta.networks.joinToString(", ") { it.name }
            )
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = NetflyHdrDetailMargins.CONTENT_START, end = 24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (!meta.description.isNullOrBlank()) {
            Text(
                text = meta.description,
                color = colorScheme.onSurface,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        facts.forEach { (label, value) ->
            Row {
                Text(
                    text = label,
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.width(90.dp)
                )
                Text(
                    text = value,
                    color = colorScheme.onSurface,
                    fontSize = 13.sp
                )
            }
        }
    }
}

/**
 * Fullscreen trailer with the "trailer playing · press any key to return"
 * hint. The overlay takes focus on appear; any key down returns to the page
 * (BACK is handled by the screen-level BackHandler).
 */
@Composable
private fun NetflyHdrTrailerOverlay(
    trailerUrl: String,
    trailerAudioUrl: String?,
    onAnyKey: () -> Unit,
    onEnded: () -> Unit
) {
    val overlayFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { overlayFocusRequester.requestFocus() }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(overlayFocusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key != Key.Back) {
                    onAnyKey()
                    true
                } else {
                    false
                }
            }
    ) {
        TrailerPlayer(
            trailerUrl = trailerUrl,
            trailerAudioUrl = trailerAudioUrl,
            isPlaying = true,
            onEnded = onEnded,
            onError = onEnded,
            muted = false,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.netflyhdr_trailer_playing_hint),
                color = Color.White,
                fontSize = 14.sp,
                maxLines = 1
            )
        }
    }
}
