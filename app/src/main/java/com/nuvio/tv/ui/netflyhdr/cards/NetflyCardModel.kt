package com.nuvio.tv.ui.netflyhdr.cards

import com.nuvio.tv.domain.model.ContentType
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.domain.model.WatchProgress

/**
 * Card model for the Netfly HDR poster / expanded cards (NOTE.md §7).
 *
 * This is deliberately a plain UI model, not the domain [MetaPreview]: rows
 * build it from whatever source they have (catalog meta, Continue Watching,
 * My List) and the card only renders what it is given — no fake data.
 *
 * ### Animated art ([gifUrl])
 * Netfly shows animated art over the focused poster when the focused item
 * carries a `gif` URL (§7). No Stremio/Cinemeta/TMDB meta field carries
 * animated art today, so there is no automatic source: callers fill
 * [gifUrl] when they can resolve one — e.g. a future addon
 * `behaviorHints` extension, a curated per-title asset map, or a fanart
 * lookup — and leave it `null` otherwise. `null` disables the animated
 * layer entirely; the poster is always the fallback.
 */
data class NetflyCardItem(
    val id: String,
    val type: ContentType,
    val name: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val logoUrl: String?,
    /** Animated-art (GIF) URL for the focus layer, or null when unknown. */
    val gifUrl: String? = null,
    val year: String? = null,
    val imdbRating: Float? = null,
    val genres: List<String> = emptyList(),
    val runtime: String? = null,
    val seasonCount: Int? = null,
    /** Continue Watching progress 0..1, or null when the item has none. */
    val progressFraction: Float? = null,
    val isContinueWatching: Boolean = false,
    val isWatched: Boolean = false,
    /** Resolved playable trailer URL (TrailerService), or null. */
    val trailerUrl: String? = null,
    val sourceAddonBaseUrl: String? = null
) {
    val apiType: String
        get() = type.toApiString()
}

/**
 * Adapts catalog metadata to a card. [progress] attaches Continue Watching
 * state; [gifUrl] is passed through by callers that resolve animated art
 * (see [NetflyCardItem.gifUrl]).
 */
fun MetaPreview.toNetflyCardItem(
    progress: WatchProgress? = null,
    isWatched: Boolean = false,
    gifUrl: String? = null,
    trailerUrl: String? = null
): NetflyCardItem {
    val progressFraction = progress
        ?.takeIf { it.duration > 0 }
        ?.let { (it.position.toFloat() / it.duration.toFloat()).coerceIn(0f, 1f) }
    return NetflyCardItem(
        id = id,
        type = type,
        name = name,
        posterUrl = poster,
        backdropUrl = backdropUrl,
        logoUrl = logo,
        gifUrl = gifUrl,
        year = releaseInfo,
        imdbRating = imdbRating,
        genres = genres,
        runtime = runtime,
        seasonCount = seasonCount,
        progressFraction = progressFraction,
        isContinueWatching = progress != null,
        isWatched = isWatched,
        trailerUrl = trailerUrl,
        sourceAddonBaseUrl = sourceAddonBaseUrl
    )
}

/** Adapts a Continue Watching entry to a card (progress always attached). */
fun WatchProgress.toNetflyCardItem(
    isWatched: Boolean = false,
    gifUrl: String? = null
): NetflyCardItem {
    val progressFraction = if (duration > 0) {
        (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    } else {
        null
    }
    return NetflyCardItem(
        id = contentId,
        type = ContentType.fromString(contentType),
        name = name,
        posterUrl = poster,
        backdropUrl = backdrop,
        logoUrl = logo,
        gifUrl = gifUrl,
        progressFraction = progressFraction,
        isContinueWatching = true,
        isWatched = isWatched
    )
}
