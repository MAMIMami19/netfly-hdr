package com.nuvio.tv.ui.netflyhdr.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.ui.components.TrailerPlayer
import com.nuvio.tv.ui.theme.NuvioTheme
import kotlinx.coroutines.delay

/**
 * The Netfly hero (design spec §5) in all 3 modes:
 *
 * - PANEL (default): backdrop 1360×765 at x=560 + solid #0C0C0F panel 562
 *   wide on the left, logo/title/metadata/description/carousel dots.
 * - FULLSCREEN: backdrop fills 1920×1080, content overlaid.
 * - BARE: compact backdrop 1220×686 at x=700 for dense layouts.
 *
 * Spec pixel sizes are halved to dp (1080p canvas at xhdpi density).
 * The trailer video replaces the backdrop when a trailer URL is supplied
 * (muted by default) with a mute toggle; with reduce-motion on, no trailer
 * autoplays. Trailer URLs are resolved by the host (same pattern as the
 * base home: HomeViewModel.requestTrailerPreview populates the URL map).
 */
data class NetflyHeroItem(
    val key: String,
    val title: String,
    val logoUrl: String? = null,
    val backdropUrl: String? = null,
    val description: String? = null,
    val metaLine: String? = null,
    val imdbRating: Float? = null,
    val trailerUrl: String? = null,
    val trailerAudioUrl: String? = null
)

/** Maps a catalog item to a hero item. Trailer URLs come from the host. */
fun MetaPreview.toNetflyHeroItem(
    trailerUrl: String? = null,
    trailerAudioUrl: String? = null
): NetflyHeroItem {
    val metaParts = buildList {
        releaseInfo?.takeIf { it.isNotBlank() }?.let { add(it) }
        runtime?.takeIf { it.isNotBlank() }?.let { add(it) }
        genres.take(3).takeIf { it.isNotEmpty() }?.let { add(it.joinToString(" · ")) }
    }
    return NetflyHeroItem(
        key = id,
        title = name,
        logoUrl = logo,
        backdropUrl = backdropUrl,
        description = description,
        metaLine = metaParts.takeIf { it.isNotEmpty() }?.joinToString("  ·  "),
        imdbRating = imdbRating,
        trailerUrl = trailerUrl,
        trailerAudioUrl = trailerAudioUrl
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NetflyHero(
    item: NetflyHeroItem,
    mode: NetflyHeroMode,
    index: Int = 0,
    count: Int = 1,
    autoplayTrailer: Boolean = true,
    /**
     * Extra bottom inset for the FULLSCREEN mode's text block and mute
     * toggle. The home rows list overlays the bottom of the screen, so the
     * host passes the rows height here to lift the content above it.
     * Ignored by PANEL and BARE, which center their text.
     */
    bottomContentPadding: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    val reduceMotion = LocalNetflyHomePrefs.current.reduceMotion
    var muted by remember(item.key) { mutableStateOf(true) }
    var trailerFirstFrame by remember(item.key) { mutableStateOf(false) }
    val showTrailer = item.trailerUrl != null && autoplayTrailer && !reduceMotion
    val backdropAlpha by animateFloatAsState(
        targetValue = if (showTrailer && trailerFirstFrame) 0f else 1f,
        animationSpec = tween(durationMillis = 400),
        label = "netflyHeroBackdropAlpha"
    )

    val backdrop: @Composable (Modifier) -> Unit = { backdropModifier ->
        Box(modifier = backdropModifier) {
            if (item.backdropUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(
                        androidx.compose.ui.platform.LocalContext.current
                    )
                        .data(item.backdropUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(backdropAlpha)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NuvioTheme.colors.BackgroundCard)
                )
            }
            if (showTrailer) {
                TrailerPlayer(
                    trailerUrl = item.trailerUrl,
                    trailerAudioUrl = item.trailerAudioUrl,
                    isPlaying = true,
                    muted = muted,
                    onEnded = {},
                    onFirstFrameRendered = { trailerFirstFrame = true },
                    cropToFill = true,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    val muteToggle: @Composable (Modifier) -> Unit = { toggleModifier ->
        if (showTrailer && trailerFirstFrame) {
            androidx.tv.material3.Card(
                onClick = { muted = !muted },
                modifier = toggleModifier,
                colors = androidx.tv.material3.CardDefaults.colors(
                    containerColor = Color.Black.copy(alpha = 0.6f),
                    focusedContainerColor = Color.Black.copy(alpha = 0.6f)
                ),
                shape = androidx.tv.material3.CardDefaults.shape(CircleShape)
            ) {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.tv.material3.Icon(
                        imageVector = if (muted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = stringResource(
                            if (muted) {
                                R.string.netfly_hero_unmute_trailer
                            } else {
                                R.string.netfly_hero_mute_trailer
                            }
                        ),
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }

    when (mode) {
        NetflyHeroMode.PANEL -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(383.dp)
                    .background(Color(0xFF0C0C0F))
            ) {
                backdrop(
                    Modifier
                        .align(Alignment.TopEnd)
                        .width(680.dp)
                        .height(383.dp)
                )
                // Left gradient over the backdrop edge.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .width(680.dp)
                        .height(383.dp)
                        .background(
                            Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0f to Color(0xFF0C0C0F),
                                    0.45f to Color(0xFF0C0C0F).copy(alpha = 0.55f),
                                    1f to Color.Transparent
                                )
                            )
                        )
                )
                // Bottom gradient.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.7f to Color.Transparent,
                                    1f to NuvioTheme.colors.Background
                                )
                            )
                        )
                )
                HeroTextBlock(
                    item = item,
                    index = index,
                    count = count,
                    descriptionMaxLines = 3,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(470.dp)
                        .padding(start = 80.dp, end = 16.dp)
                )
                muteToggle(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 24.dp, bottom = 24.dp)
                )
            }
        }

        NetflyHeroMode.FULLSCREEN -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(540.dp)
            ) {
                backdrop(Modifier.fillMaxSize())
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.35f to Color.Transparent,
                                    0.75f to Color.Black.copy(alpha = 0.55f),
                                    1f to NuvioTheme.colors.Background
                                )
                            )
                        )
                )
                HeroTextBlock(
                    item = item,
                    index = index,
                    count = count,
                    descriptionMaxLines = 4,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .width(640.dp)
                        .padding(start = 80.dp, bottom = 32.dp + bottomContentPadding, end = 16.dp)
                )
                muteToggle(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 24.dp, bottom = 24.dp + bottomContentPadding)
                )
            }
        }

        NetflyHeroMode.BARE -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(343.dp)
                    .background(Color(0xFF0C0C0F))
            ) {
                backdrop(
                    Modifier
                        .align(Alignment.TopEnd)
                        .width(610.dp)
                        .height(343.dp)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .width(610.dp)
                        .height(343.dp)
                        .background(
                            Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0f to Color(0xFF0C0C0F),
                                    0.4f to Color(0xFF0C0C0F).copy(alpha = 0.5f),
                                    1f to Color.Transparent
                                )
                            )
                        )
                )
                HeroTextBlock(
                    item = item,
                    index = index,
                    count = count,
                    descriptionMaxLines = 3,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(360.dp)
                        .padding(start = 80.dp, end = 16.dp)
                )
                muteToggle(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 24.dp, bottom = 20.dp)
                )
            }
        }
    }
}

/** Logo (or title) + metadata + description + carousel dots. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HeroTextBlock(
    item: NetflyHeroItem,
    index: Int,
    count: Int,
    descriptionMaxLines: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (item.logoUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(
                    androidx.compose.ui.platform.LocalContext.current
                )
                    .data(item.logoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = item.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(310.dp)
                    .height(90.dp)
            )
        } else {
            Text(
                text = item.title,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (item.imdbRating != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFF5C518))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "IMDb",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                }
                Text(
                    text = String.format("%.1f", item.imdbRating),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            if (item.metaLine != null) {
                Text(
                    text = item.metaLine,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFC9C9D4),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (!item.description.isNullOrBlank()) {
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFE8E8E8),
                maxLines = descriptionMaxLines,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (count > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(count) { dotIndex ->
                    Box(
                        modifier = Modifier
                            .size(width = if (dotIndex == index) 22.dp else 8.dp, height = 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (dotIndex == index) {
                                    NuvioTheme.colors.Secondary
                                } else {
                                    Color.White.copy(alpha = 0.45f)
                                }
                            )
                    )
                }
            }
        }
    }
}

/**
 * Rotating hero host: auto-advances through [items] every [autoAdvanceMs]
 * (disabled with reduce-motion), Left/Right switch items when the hero holds
 * focus, OK opens the current item.
 */
@Composable
fun NetflyHeroCarouselHost(
    items: List<NetflyHeroItem>,
    mode: NetflyHeroMode,
    trailerUrls: Map<String, String> = emptyMap(),
    trailerAudioUrls: Map<String, String> = emptyMap(),
    autoAdvanceMs: Long = 8_000L,
    initialIndex: Int = 0,
    onItemClick: (NetflyHeroItem) -> Unit = {},
    onItemFocus: (NetflyHeroItem) -> Unit = {},
    bottomContentPadding: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val reduceMotion = LocalNetflyHomePrefs.current.reduceMotion
    var index by remember { mutableIntStateOf(initialIndex.coerceIn(items.indices)) }
    val current = items[index.coerceIn(items.indices)]
    val withTrailer = remember(current, trailerUrls, trailerAudioUrls) {
        current.copy(
            trailerUrl = trailerUrls[current.key] ?: current.trailerUrl,
            trailerAudioUrl = trailerAudioUrls[current.key] ?: current.trailerAudioUrl
        )
    }
    var hasFocus by remember { mutableStateOf(false) }

    LaunchedEffect(index, items.size, reduceMotion) {
        if (items.size <= 1 || reduceMotion) return@LaunchedEffect
        delay(autoAdvanceMs)
        index = (index + 1) % items.size
    }
    LaunchedEffect(current.key) {
        onItemFocus(current)
    }

    Box(
        modifier = modifier
            .focusable()
            .onFocusChanged { hasFocus = it.isFocused }
            .onKeyEvent { event ->
                if (!hasFocus || event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> {
                        index = (index + items.size - 1) % items.size
                        true
                    }

                    Key.DirectionRight -> {
                        index = (index + 1) % items.size
                        true
                    }

                    Key.Enter, Key.NumPadEnter -> {
                        onItemClick(current)
                        true
                    }

                    else -> false
                }
            }
    ) {
        NetflyHero(
            item = withTrailer,
            mode = mode,
            index = index,
            count = items.size,
            bottomContentPadding = bottomContentPadding,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
