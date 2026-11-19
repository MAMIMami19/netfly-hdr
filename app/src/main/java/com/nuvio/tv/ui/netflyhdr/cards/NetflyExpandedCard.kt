package com.nuvio.tv.ui.netflyhdr.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.nuvio.tv.ui.components.TrailerPlayer
import com.nuvio.tv.ui.netflyhdr.NetflyHdrDimensions

/**
 * Expanded-card content (NOTE.md §7): the 532×300 horizontal card a focused
 * poster grows into after [NetflyHdrMotion.EXPAND_DELAY_MS].
 *
 * Backdrop art (or the poster as fallback) fills the card; when a resolved
 * trailer URL is supplied and [trailerEnabled], the trailer plays muted
 * inside the card. The title logo sits bottom-left (title text when no logo
 * art exists). The white focus ring and the card surface are drawn by the
 * hosting [NetflyPosterCard]; the base here is the theme's surface color
 * (the spec's `#1E1E26` is the theme's card surface).
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NetflyExpandedCardContent(
    item: NetflyCardItem,
    trailerEnabled: Boolean,
    isPlaying: Boolean,
    onTrailerEnded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val backdropUrl = item.backdropUrl ?: item.posterUrl
    Box(
        modifier = modifier
            .background(colorScheme.surface)
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
        }
        if (trailerEnabled && !item.trailerUrl.isNullOrBlank() && isPlaying) {
            TrailerPlayer(
                trailerUrl = item.trailerUrl,
                isPlaying = true,
                onEnded = onTrailerEnded,
                onError = onTrailerEnded,
                muted = true,
                modifier = Modifier.fillMaxSize()
            )
        }
        // Bottom scrim so the logo/title stays legible over bright art.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.55f)
                        )
                    )
                )
        )
        // Logo bottom-left (§7); title text when no logo art exists.
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 10.dp, end = 12.dp)
        ) {
            if (!item.logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.logoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(NetflyHdrDimensions.railRowHeight)
                )
            } else {
                Text(
                    text = item.name,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
