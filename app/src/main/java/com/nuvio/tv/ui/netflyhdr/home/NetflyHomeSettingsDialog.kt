package com.nuvio.tv.ui.netflyhdr.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.ContinueWatchingCardStyle
import com.nuvio.tv.domain.model.DiscoverLocation
import com.nuvio.tv.domain.model.HomeImdbRatingsVisibility
import com.nuvio.tv.ui.netflyhdr.themes.NETFLY_RED
import com.nuvio.tv.ui.netflyhdr.themes.NetflyDialogHeader
import com.nuvio.tv.ui.netflyhdr.themes.NetflyDialogShell
import com.nuvio.tv.ui.netflyhdr.themes.NetflyOptionPill
import com.nuvio.tv.ui.netflyhdr.themes.NetflySectionLabel
import com.nuvio.tv.ui.netflyhdr.themes.NetflyToggleRow
import com.nuvio.tv.ui.screens.detail.requestFocusAfterFrames
import com.nuvio.tv.ui.screens.settings.LayoutSettingsEvent
import com.nuvio.tv.ui.screens.settings.LayoutSettingsViewModel
import com.nuvio.tv.ui.theme.NuvioTheme

/**
 * Netfly Home settings (design spec §4): the 10 home layouts, the hero mode,
 * titles-per-row, featured carousel catalogs and the home display toggles.
 *
 * Layouts and hero mode apply immediately through [NetflyHomeLayoutApplier];
 * the display toggles reuse the base layout preferences (same storage as
 * Layout settings, so both screens stay in sync).
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NetflyHomeSettingsDialog(
    homeViewModel: NetflyHomeViewModel = hiltViewModel(),
    layoutViewModel: LayoutSettingsViewModel = hiltViewModel(),
    onDismiss: () -> Unit
) {
    val netflyState by homeViewModel.state.collectAsStateWithLifecycle()
    val layoutState by layoutViewModel.uiState.collectAsStateWithLifecycle()
    val onLayoutEvent = remember(layoutViewModel) { { event: LayoutSettingsEvent -> layoutViewModel.onEvent(event) } }

    val appliedFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        appliedFocusRequester.requestFocusAfterFrames()
    }

    NetflyDialogShell(onDismiss = onDismiss) {
        NetflyDialogHeader(
            title = stringResource(R.string.netfly_home_settings_title),
            redLabel = stringResource(R.string.netfly_home_layouts_label)
        )
        Spacer(modifier = Modifier.height(16.dp))

        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // The 10 layout cards (5 × 2 grid).
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(330.dp),
                contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                userScrollEnabled = false
            ) {
                items(NetflyHomeLayout.entries, key = { it.name }) { layout ->
                    NetflyHomeLayoutCard(
                        layout = layout,
                        isApplied = layout == netflyState.homeLayout,
                        modifier = if (layout == netflyState.homeLayout) {
                            Modifier.focusRequester(appliedFocusRequester)
                        } else {
                            Modifier
                        },
                        onClick = { homeViewModel.selectLayout(layout) }
                    )
                }
            }

            // Hero mode.
            NetflySectionLabel(text = stringResource(R.string.netfly_home_hero_mode_title))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NetflyHeroMode.entries.forEach { mode ->
                    NetflyOptionPill(
                        text = stringResource(mode.nameRes),
                        selected = mode == netflyState.heroMode,
                        onClick = { homeViewModel.selectHeroMode(mode) }
                    )
                }
            }
            Text(
                text = stringResource(netflyState.heroMode.descriptionRes),
                style = MaterialTheme.typography.bodySmall,
                color = NuvioTheme.colors.TextTertiary
            )

            // Titles per row.
            NetflySectionLabel(text = stringResource(R.string.netfly_home_titles_per_row))
            Text(
                text = stringResource(R.string.netfly_home_titles_per_row_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = NuvioTheme.colors.TextTertiary
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NETFLY_TITLES_PER_ROW_OPTIONS.forEach { count ->
                    NetflyOptionPill(
                        text = count.toString(),
                        selected = count == netflyState.titlesPerRow,
                        onClick = { homeViewModel.setTitlesPerRow(count) }
                    )
                }
            }

            // Featured carousel catalogs (multi-select chips).
            NetflySectionLabel(text = stringResource(R.string.netfly_home_featured_catalogs))
            Text(
                text = stringResource(R.string.netfly_home_featured_catalogs_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = NuvioTheme.colors.TextTertiary
            )
            if (layoutState.availableCatalogs.isEmpty()) {
                Text(
                    text = stringResource(R.string.netfly_home_featured_catalogs_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = NuvioTheme.colors.TextSecondary
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(2.dp)
                ) {
                    items(layoutState.availableCatalogs, key = { it.key }) { catalog ->
                        NetflyOptionPill(
                            text = catalog.name,
                            selected = catalog.key in layoutState.heroCatalogKeys,
                            onClick = {
                                onLayoutEvent(LayoutSettingsEvent.ToggleHeroCatalog(catalog.key))
                            }
                        )
                    }
                }
            }

            // Display toggles (base layout preferences).
            NetflyToggleRow(
                title = stringResource(R.string.netfly_home_poster_names),
                subtitle = stringResource(R.string.netfly_home_poster_names_subtitle),
                checked = layoutState.posterLabelsEnabled,
                onToggle = {
                    onLayoutEvent(
                        LayoutSettingsEvent.SetPosterLabelsEnabled(!layoutState.posterLabelsEnabled)
                    )
                }
            )
            NetflyToggleRow(
                title = stringResource(R.string.netfly_home_hide_unreleased),
                subtitle = stringResource(R.string.netfly_home_hide_unreleased_subtitle),
                checked = layoutState.hideUnreleasedContent,
                onToggle = {
                    onLayoutEvent(
                        LayoutSettingsEvent.SetHideUnreleasedContent(!layoutState.hideUnreleasedContent)
                    )
                }
            )
            NetflyToggleRow(
                title = stringResource(R.string.netfly_home_imdb_cover),
                subtitle = stringResource(R.string.netfly_home_imdb_cover_subtitle),
                checked = layoutState.homeImdbRatingsVisibility == HomeImdbRatingsVisibility.SHOW_ALL,
                onToggle = {
                    onLayoutEvent(
                        LayoutSettingsEvent.SetHomeImdbRatingsVisibility(
                            if (layoutState.homeImdbRatingsVisibility == HomeImdbRatingsVisibility.SHOW_ALL) {
                                HomeImdbRatingsVisibility.HIDE_ALL
                            } else {
                                HomeImdbRatingsVisibility.SHOW_ALL
                            }
                        )
                    )
                }
            )
            NetflyToggleRow(
                title = stringResource(R.string.netfly_home_cw_show),
                subtitle = stringResource(R.string.netfly_home_cw_show_subtitle),
                checked = layoutState.continueWatchingEnabled,
                onToggle = {
                    onLayoutEvent(
                        LayoutSettingsEvent.SetContinueWatchingEnabled(!layoutState.continueWatchingEnabled)
                    )
                }
            )
            if (layoutState.continueWatchingEnabled) {
                NetflySectionLabel(text = stringResource(R.string.netfly_home_cw_style))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NetflyOptionPill(
                        text = stringResource(R.string.netfly_home_cw_style_card),
                        selected = layoutState.continueWatchingCardStyle == ContinueWatchingCardStyle.CARD,
                        onClick = {
                            onLayoutEvent(
                                LayoutSettingsEvent.SetContinueWatchingCardStyle(ContinueWatchingCardStyle.CARD)
                            )
                        }
                    )
                    NetflyOptionPill(
                        text = stringResource(R.string.netfly_home_cw_style_wide),
                        selected = layoutState.continueWatchingCardStyle == ContinueWatchingCardStyle.WIDE,
                        onClick = {
                            onLayoutEvent(
                                LayoutSettingsEvent.SetContinueWatchingCardStyle(ContinueWatchingCardStyle.WIDE)
                            )
                        }
                    )
                    NetflyOptionPill(
                        text = stringResource(R.string.netfly_home_cw_style_poster),
                        selected = layoutState.continueWatchingCardStyle == ContinueWatchingCardStyle.POSTER,
                        onClick = {
                            onLayoutEvent(
                                LayoutSettingsEvent.SetContinueWatchingCardStyle(ContinueWatchingCardStyle.POSTER)
                            )
                        }
                    )
                }
            }

            // Discover location.
            NetflySectionLabel(text = stringResource(R.string.netfly_home_discover_location))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NetflyOptionPill(
                    text = stringResource(R.string.netfly_home_discover_in_search),
                    selected = layoutState.discoverLocation == DiscoverLocation.IN_SEARCH,
                    onClick = {
                        onLayoutEvent(LayoutSettingsEvent.SetDiscoverLocation(DiscoverLocation.IN_SEARCH))
                    }
                )
                NetflyOptionPill(
                    text = stringResource(R.string.netfly_home_discover_in_sidebar),
                    selected = layoutState.discoverLocation == DiscoverLocation.IN_SIDEBAR,
                    onClick = {
                        onLayoutEvent(LayoutSettingsEvent.SetDiscoverLocation(DiscoverLocation.IN_SIDEBAR))
                    }
                )
                NetflyOptionPill(
                    text = stringResource(R.string.netfly_home_discover_off),
                    selected = layoutState.discoverLocation == DiscoverLocation.OFF,
                    onClick = {
                        onLayoutEvent(LayoutSettingsEvent.SetDiscoverLocation(DiscoverLocation.OFF))
                    }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/** One layout card: shape-aware mini preview + name + description. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NetflyHomeLayoutCard(
    layout: NetflyHomeLayout,
    isApplied: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    Card(
        onClick = onClick,
        modifier = modifier.onFocusChanged { isFocused = it.isFocused },
        colors = CardDefaults.colors(
            containerColor = NuvioTheme.colors.BackgroundCard,
            focusedContainerColor = NuvioTheme.colors.BackgroundCard
        ),
        border = CardDefaults.border(
            border = Border(
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isApplied) NETFLY_RED else NuvioTheme.colors.Border
                ),
                shape = RoundedCornerShape(12.dp)
            ),
            focusedBorder = Border(
                border = NuvioTheme.focusRing.border(2.dp),
                shape = RoundedCornerShape(12.dp)
            )
        ),
        shape = CardDefaults.shape(RoundedCornerShape(12.dp)),
        scale = CardDefaults.scale(focusedScale = 1.04f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Mini preview: hero strip (when the layout has a hero) + tiles
            // shaped like the layout's cards.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(NuvioTheme.colors.Background)
                    .padding(6.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (layout.heroEnabled) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (layout.heroFullScreen) 30.dp else 20.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (layout.heroFullScreen) {
                                        NETFLY_RED.copy(alpha = 0.55f)
                                    } else {
                                        NuvioTheme.colors.SurfaceVariant
                                    }
                                )
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        repeat(3) {
                            val tileModifier = when (layout.cardShape) {
                                NetflyCardShape.POSTER -> Modifier.size(width = 20.dp, height = 30.dp)
                                NetflyCardShape.LANDSCAPE -> Modifier.size(width = 34.dp, height = 20.dp)
                                NetflyCardShape.SQUARE -> Modifier.size(24.dp)
                            }
                            Box(
                                modifier = tileModifier
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(NuvioTheme.colors.SurfaceVariant)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(layout.nameRes),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isFocused || isApplied) {
                        NuvioTheme.colors.TextPrimary
                    } else {
                        NuvioTheme.colors.TextSecondary
                    },
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isApplied) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(NETFLY_RED)
                    )
                }
            }
            Text(
                text = stringResource(layout.descriptionRes),
                style = MaterialTheme.typography.bodySmall,
                color = NuvioTheme.colors.TextTertiary,
                maxLines = 1
            )
        }
    }
}
