package com.nuvio.tv.ui.netflyhdr.cards

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import java.net.URLEncoder

/**
 * Animated art on focus (NOTE.md §7): when the focused item carries a GIF
 * URL, the animated art is shown overlaid on the poster. The URL is fetched
 * through the wsrv.nl proxy, which converts the GIF to animated WebP; if
 * that fails, the DuckDuckGo image proxy is tried, then the original URL.
 *
 * Decoding is handled by the app's Coil ImageLoader, which registers
 * `AnimatedImageDecoder` on API 28+ (animated WebP + GIF) and `GifDecoder`
 * below — no extra setup needed here.
 */

/** wsrv.nl (images.weserv.nl) proxy: converts the GIF to animated WebP. */
fun netflyWsrvAnimatedUrl(gifUrl: String): String {
    val encoded = URLEncoder.encode(gifUrl, "UTF-8")
    return "https://wsrv.nl/?url=$encoded&output=webp"
}

/** DuckDuckGo image proxy fallback. */
fun netflyDuckDuckGoProxyUrl(gifUrl: String): String {
    val encoded = URLEncoder.encode(gifUrl, "UTF-8")
    return "https://proxy.duckduckgo.com/iu/?u=$encoded"
}

/** Ordered candidates for one animated-art URL: wsrv → DuckDuckGo → original. */
fun netflyAnimatedArtCandidates(gifUrl: String): List<String> = listOf(
    netflyWsrvAnimatedUrl(gifUrl),
    netflyDuckDuckGoProxyUrl(gifUrl),
    gifUrl
)

/**
 * Loads animated art with the wsrv → DuckDuckGo → original fallback chain.
 * Advances to the next candidate on load failure; renders nothing once every
 * candidate has failed (the poster underneath stays visible).
 */
@Composable
fun NetflyAnimatedArtImage(
    gifUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val candidates = remember(gifUrl) { netflyAnimatedArtCandidates(gifUrl) }
    var attemptIndex by remember(gifUrl) { mutableIntStateOf(0) }
    if (attemptIndex >= candidates.size) return
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(candidates[attemptIndex])
            .crossfade(false)
            .build(),
        contentDescription = contentDescription,
        contentScale = contentScale,
        onError = {
            if (attemptIndex < candidates.lastIndex) {
                attemptIndex++
            } else {
                // Every candidate failed: stop rendering so the poster shows.
                attemptIndex = candidates.size
            }
        },
        modifier = modifier
    )
}
