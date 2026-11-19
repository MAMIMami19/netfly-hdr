package com.nuvio.tv.ui.netflyhdr.home

import androidx.compose.runtime.staticCompositionLocalOf
import com.nuvio.tv.domain.model.CatalogRow
import com.nuvio.tv.ui.screens.home.CarouselRowLookups
import com.nuvio.tv.ui.screens.home.ModernHomePresentationState
import com.nuvio.tv.ui.util.asStable

/**
 * The Netfly home preferences the home screen renders with. Provided once by
 * MainActivity from [com.nuvio.tv.ui.netflyhdr.themes.NetflyHdrDataStore] so
 * Home, the hero and the cards all read the same values.
 */
data class NetflyHomePrefs(
    val layout: NetflyHomeLayout = NetflyHomeLayout.DEFAULT,
    val heroMode: NetflyHeroMode = NetflyHeroMode.DEFAULT,
    val titlesPerRow: Int = NETFLY_TITLES_PER_ROW_DEFAULT,
    val reduceMotion: Boolean = false
)

val LocalNetflyHomePrefs = staticCompositionLocalOf { NetflyHomePrefs() }

/**
 * Display-caps Classic / Grid catalog rows to [titlesPerRow] items. The
 * ViewModel's own lists are untouched — load-more keeps working, and the cap
 * re-applies on every render.
 */
fun trimCatalogRows(
    rows: List<CatalogRow>,
    titlesPerRow: Int
): List<CatalogRow> {
    if (titlesPerRow <= 0) return rows
    var changed = false
    val trimmed = rows.map { row ->
        if (row.items.size > titlesPerRow) {
            changed = true
            row.copy(items = row.items.take(titlesPerRow))
        } else {
            row
        }
    }
    return if (changed) trimmed else rows
}

/**
 * Display-caps a Modern home presentation to [titlesPerRow] items per row,
 * keeping the row lookups consistent with the trimmed rows:
 * [CarouselRowLookups.rowByKey] points at the trimmed rows and
 * [CarouselRowLookups.itemIdentitiesByRow] is trimmed in the same order, so
 * focus relocation can never land past the end of a rendered row. The
 * active-item sets are left alone — a superset there is harmless.
 */
fun trimModernPresentation(
    presentation: ModernHomePresentationState,
    titlesPerRow: Int
): ModernHomePresentationState {
    if (titlesPerRow <= 0) return presentation
    val rows = presentation.rows.list
    if (rows.none { it.items.size > titlesPerRow }) return presentation

    val trimmedRows = rows.map { row ->
        if (row.items.size > titlesPerRow) {
            row.copy(items = row.items.take(titlesPerRow).asStable())
        } else {
            row
        }
    }
    val trimmedByKey = trimmedRows.associateBy { it.key }
    val lookups = presentation.lookups
    val trimmedLookups = CarouselRowLookups(
        rowIndexByKey = lookups.rowIndexByKey,
        rowByKey = lookups.rowByKey.mapValues { (key, row) ->
            trimmedByKey[key] ?: row
        }.asStable(),
        rowKeyByGlobalRowIndex = lookups.rowKeyByGlobalRowIndex,
        firstHeroPreviewByRow = lookups.firstHeroPreviewByRow,
        fallbackBackdropByRow = lookups.fallbackBackdropByRow,
        activeRowKeys = lookups.activeRowKeys,
        activeItemKeysByRow = lookups.activeItemKeysByRow,
        itemIdentitiesByRow = lookups.itemIdentitiesByRow.mapValues { (_, identities) ->
            identities.take(titlesPerRow).asStable()
        }.asStable(),
        activeCatalogItemIds = lookups.activeCatalogItemIds
    )
    return presentation.copy(
        rows = trimmedRows.asStable(),
        lookups = trimmedLookups
    )
}
