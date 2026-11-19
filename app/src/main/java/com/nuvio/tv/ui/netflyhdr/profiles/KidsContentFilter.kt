package com.nuvio.tv.ui.netflyhdr.profiles

import com.nuvio.tv.domain.model.CatalogRow
import com.nuvio.tv.domain.model.MetaPreview

/**
 * Kids age-rating filter (design §11).
 *
 * Ratings arrive as TMDB certifications on [MetaPreview.ageRating] — MPAA
 * (`PG-13`, `R`), TV (`TV-MA`, `TV-14`), BBFC (`12A`, `15`) or plain numbers
 * (`12`, `16`). [minimumAgeForRating] maps a rating string to the youngest
 * age it is suitable for; unknown or missing ratings are ALLOWED (most
 * catalog items carry no rating at all, so hiding them would empty the home
 * rows — the limit only ever hides titles positively rated above the cap).
 *
 * ENFORCEMENT HOOK — home/search pipelines apply the filter next to the
 * existing unreleased filter, e.g. in `HomeViewModelCatalogPipeline`:
 * ```
 * val kidsMaxAge = kidsSettings.takeIf { it.enabled }?.maxAgeYears
 * val rows = rawRows.map { row ->
 *     var filtered = if (hideUnreleased) row.filterReleasedItems(today) else row
 *     if (kidsMaxAge != null) filtered = filtered.filterKidsItems(kidsMaxAge)
 *     filtered
 * }
 * ```
 * The hero and search rows use the same one-line call.
 */
object KidsContentFilter {

    private val NUMBER_REGEX = Regex("""\d{1,2}""")

    private val KNOWN_RATINGS = mapOf(
        // No age floor.
        "G" to 0,
        "TV-Y" to 0,
        "TV-G" to 0,
        "U" to 0,
        "UC" to 0,
        "E" to 0,
        "TP" to 0,
        "ALL" to 0,
        "0" to 0,
        // Parental guidance: treated as 7+.
        "PG" to 7,
        "TV-Y7" to 7,
        "TV-Y7-FV" to 7,
        "TV-PG" to 7,
        // Adults only.
        "R" to 17,
        "TV-MA" to 17,
        "NC-17" to 18,
        "X" to 18,
        "XXX" to 18,
        "18" to 18
    )

    /**
     * Youngest suitable age for a rating string, or null when the rating is
     * missing or unrecognized (treated as allowed by [isAllowed]).
     */
    fun minimumAgeForRating(rating: String?): Int? {
        val normalized = rating?.trim()?.uppercase()?.replace(" ", "").orEmpty()
        if (normalized.isEmpty()) return null
        KNOWN_RATINGS[normalized]?.let { return it }
        // "PG-13" -> 13, "12A" -> 12, "TV-14" -> 14, "16+" -> 16.
        return NUMBER_REGEX.find(normalized)?.value?.toIntOrNull()
    }

    /** True when a title rated [rating] may show on a profile capped at [maxAge]. */
    fun isAllowed(rating: String?, maxAge: Int): Boolean {
        val minimumAge = minimumAgeForRating(rating) ?: return true
        return minimumAge <= maxAge
    }
}

fun MetaPreview.isAllowedForKids(maxAge: Int): Boolean =
    KidsContentFilter.isAllowed(ageRating, maxAge)

fun List<MetaPreview>.filterKidsItems(maxAge: Int): List<MetaPreview> =
    filter { it.isAllowedForKids(maxAge) }

fun CatalogRow.filterKidsItems(maxAge: Int): CatalogRow {
    val filtered = items.filterKidsItems(maxAge)
    return if (filtered.size == items.size) this else copy(items = filtered)
}
