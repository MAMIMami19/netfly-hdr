package com.nuvio.tv.ui.netflyhdr.cards

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Border
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
import com.nuvio.tv.ui.netflyhdr.NetflyHdrAccent
import com.nuvio.tv.ui.netflyhdr.NetflyHdrDimensions
import com.nuvio.tv.ui.netflyhdr.NetflyHdrMotion
import com.nuvio.tv.ui.netflyhdr.NetflyHdrOutQuad
import kotlinx.coroutines.delay

/**
 * Netfly HDR poster card (NOTE.md §7).
 *
 * - 200×300 poster with a rounded frame; when art is missing the title is
 *   shown as centered text on the theme surface — never a black void.
 * - Focus ring in the Netfly red accent; the poster itself never scales.
 * - Continue Watching badge + 6 px progress bar (30% black track, red bar).
 * - Watched checkmark (34 px circle + ✓).
 * - Animated art on focus: when the item carries a GIF URL, the animated
 *   art overlays the poster once focus settles (focus progress > 0.95),
 *   via [NetflyAnimatedArtImage] (wsrv.nl → DuckDuckGo → original).
 * - Expanded card: when [expandOnFocus] is set, after [expandDelayMs]
 *   (default 3 s) the card grows in place to the 532×300 horizontal card
 *   (0.24 s outQuad) with optional muted trailer playback inside. The row
 *   hosting this card must not clip its children, and siblings shift while
 *   the card is expanded (same behavior as the base card's backdrop expand).
 *
 * @param onRequestTrailer called when the card expands and no trailer URL is
 *   attached yet, so the host row can resolve one (e.g. via TrailerService)
 *   and re-emit the item with [NetflyCardItem.trailerUrl] set.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NetflyPosterCard(
    item: NetflyCardItem,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    showTitleBelow: Boolean = false,
    expandOnFocus: Boolean = false,
    expandDelayMs: Long = NetflyHdrMotion.EXPAND_DELAY_MS,
    expandTrailerEnabled: Boolean = false,
    onRequestTrailer: (NetflyCardItem) -> Unit = {},
    onFocus: (NetflyCardItem) -> Unit = {},
    onClick: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    var isFocused by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    var trailerEnded by remember(item.id) { mutableStateOf(false) }

    // Focus-settle progress (0→1): the animated-art gate mirrors the spec's
    // focusPercent > 0.95.
    val focusProgress by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(durationMillis = NetflyHdrMotion.FOCUS_SETTLE_MS),
        label = "netflyCardFocusProgress"
    )
    val showAnimatedArt =
        item.gifUrl != null && focusProgress > NetflyHdrMotion.FOCUS_SETTLED_THRESHOLD && !isExpanded

    if (expandOnFocus) {
        LaunchedEffect(isFocused, item.id, expandDelayMs) {
            if (!isFocused) {
                isExpanded = false
                trailerEnded = false
                return@LaunchedEffect
            }
            delay(expandDelayMs.coerceAtLeast(0L))
            if (isFocused) {
                isExpanded = true
            }
        }
        LaunchedEffect(isExpanded) {
            if (isExpanded && expandTrailerEnabled && item.trailerUrl.isNullOrBlank()) {
                onRequestTrailer(item)
            }
        }
    } else {
        LaunchedEffect(isFocused) {
            if (!isFocused) isExpanded = false
        }
    }

    val cardWidth = when {
        !expandOnFocus -> NetflyHdrDimensions.posterWidth
        !isFocused && !isExpanded -> NetflyHdrDimensions.posterWidth
        else -> {
            val targetWidth =
                if (isExpanded) NetflyHdrDimensions.expandedCardWidth else NetflyHdrDimensions.posterWidth
            val width by animateDpAsState(
                targetValue = targetWidth,
                animationSpec = tween(
                    durationMillis = NetflyHdrMotion.EXPAND_DURATION_MS,
                    easing = NetflyHdrOutQuad
                ),
                label = "netflyCardWidth"
            )
            width
        }
    }

    val cardShape = remember(isExpanded) {
        RoundedCornerShape(
            if (isExpanded) {
                NetflyHdrDimensions.expandedCardCornerRadius
            } else {
                NetflyHdrDimensions.posterCornerRadius
            }
        )
    }
    // White ring while expanded, red accent ring while focused (§7).
    val focusedRingColor = if (isExpanded) Color.White else NetflyHdrAccent

    Column(
        modifier = modifier.zIndex(if (isFocused || isExpanded) 1f else 0f)
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier
                .width(cardWidth)
                .height(NetflyHdrDimensions.posterHeight)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .onFocusChanged { state ->
                    if (state.isFocused != isFocused) {
                        isFocused = state.isFocused
                        if (state.isFocused) onFocus(item)
                    }
                }
                .semantics { contentDescription = item.name },
            colors = CardDefaults.colors(
                containerColor = colorScheme.surface,
                focusedContainerColor = colorScheme.surface
            ),
            border = CardDefaults.border(
                border = Border.None,
                focusedBorder = Border(
                    border = BorderStroke(NetflyHdrDimensions.focusRingWidth, focusedRingColor),
                    shape = cardShape
                )
            ),
            shape = CardDefaults.shape(shape = cardShape),
            scale = CardDefaults.scale(focusedScale = 1f, pressedScale = 1f)
        ) {
            if (isExpanded) {
                NetflyExpandedCardContent(
                    item = item,
                    trailerEnabled = expandTrailerEnabled,
                    isPlaying = !trailerEnded,
                    onTrailerEnded = { trailerEnded = true },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                NetflyPosterCardContent(
                    item = item,
                    showAnimatedArt = showAnimatedArt
                )
            }
        }
        if (showTitleBelow) {
            Text(
                text = item.name,
                color = if (isFocused) colorScheme.onSurface else colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .width(NetflyHdrDimensions.posterWidth)
                    .padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun NetflyPosterCardContent(
    item: NetflyCardItem,
    showAnimatedArt: Boolean
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    var posterFailed by remember(item.posterUrl) { mutableStateOf(false) }
    val showTitleFallback = item.posterUrl.isNullOrBlank() || posterFailed

    Box(modifier = Modifier.fillMaxSize()) {
        if (showTitleFallback) {
            // Never a black void: title text centered on the theme surface.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.name,
                    color = colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.posterUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                onError = { posterFailed = true },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Animated art overlays the poster once focus settles.
        if (showAnimatedArt && item.gifUrl != null) {
            NetflyAnimatedArtImage(
                gifUrl = item.gifUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Continue Watching badge (top-start).
        if (item.isContinueWatching) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(NetflyHdrAccent)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = stringResource(R.string.continue_watching),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }

        // Watched checkmark (top-end): 34 px circle + ✓.
        if (item.isWatched) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
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

        // Continue Watching progress bar (bottom): 30% black track + red bar.
        if (item.isContinueWatching && item.progressFraction != null) {
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
                        .fillMaxWidth(item.progressFraction.coerceIn(0f, 1f))
                        .background(NetflyHdrAccent)
                )
            }
        }
    }
}
