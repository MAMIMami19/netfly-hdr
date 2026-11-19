package com.nuvio.tv.ui.netflyhdr

import androidx.compose.ui.graphics.Color
import java.net.URLEncoder

/**
 * Route contract for the Netfly HDR destinations.
 *
 * Two naming conventions live side by side here (both builders' code
 * references its own): PascalCase ([Store], [Calendar], [History]) from the
 * NavRail builder and UPPER_SNAKE ([STORE], [CALENDAR], [HISTORY]) from the
 * browse/hub builder. The route strings are identical — do not rename one
 * set without updating its call sites.
 *
 * [Detail] is the Netfly HDR detail page, registered in
 * `NuvioNavHost`. It reuses [com.nuvio.tv.ui.screens.detail.MetaDetailsViewModel],
 * which reads `itemId` / `itemType` / `addonBaseUrl` from the SavedStateHandle,
 * so the route carries the same arguments as the base `detail/` route.
 *
 * Search, Discover and Library keep the base routes ("search", "discover",
 * "library") — only their screen content is swapped for the Netfly HDR
 * presentation.
 */
object NetflyHdrRoutes {
    const val Store = "netflyhdr_store"
    const val Calendar = "netflyhdr_calendar"
    const val History = "netflyhdr_history"

    /** Episode calendar — own nav page (§9). */
    const val CALENDAR = "netflyhdr_calendar"

    /** Recently-watched history page (§9). */
    const val HISTORY = "netflyhdr_history"

    /** Universal Provider Hub — the addon store (§10). */
    const val STORE = "netflyhdr_store"

    /** Route pattern for the Netfly HDR detail page. */
    const val Detail = "netflyhdr_detail/{itemId}/{itemType}?addonBaseUrl={addonBaseUrl}"

    fun detail(
        itemId: String,
        itemType: String,
        addonBaseUrl: String? = null
    ): String {
        fun encode(value: String): String =
            URLEncoder.encode(value, "UTF-8").replace("+", "%20")
        val encodedAddon = addonBaseUrl?.let { encode(it) } ?: ""
        return "netflyhdr_detail/${encode(itemId)}/${encode(itemType)}?addonBaseUrl=$encodedAddon"
    }
}

/**
 * Netfly red — the single hardcoded accent the design spec demands (§2).
 * Every other surface/text color comes from MaterialTheme.colorScheme
 * (owned by the theme builder); this constant is only for focus rings,
 * badges and the red marks the spec calls for.
 */
val NetflyHdrRed = Color(0xFFE50914)

/**
 * Store badge text color, per §10 ("شارة سفلية بلون #E5A8A8").
 * A light red-pink reserved for the Install / Installed / Unavailable /
 * Search / Add / Manage / Refresh badges on store cards.
 */
val NetflyHdrBadgePink = Color(0xFFE5A8A8)
