package com.nuvio.tv.ui.netflyhdr

import java.net.URLEncoder

/**
 * Route contract for the Netfly HDR destinations.
 *
 * [Store], [Calendar] and [History] are owned by the builder of the
 * Store / Episode Calendar / History pages: their composables will be
 * registered under exactly these route strings after merge. Until then the
 * NavRail guards those entries (see [com.nuvio.tv.ui.netflyhdr.nav.NetflyHdrNavRailScaffold])
 * so tapping one safely no-ops instead of crashing navigation.
 *
 * [Detail] is the Netfly HDR detail page (this area), registered in
 * `NuvioNavHost`. It reuses [com.nuvio.tv.ui.screens.detail.MetaDetailsViewModel],
 * which reads `itemId` / `itemType` / `addonBaseUrl` from the SavedStateHandle,
 * so the route carries the same arguments as the base `detail/` route.
 */
object NetflyHdrRoutes {
    const val Store = "netflyhdr_store"
    const val Calendar = "netflyhdr_calendar"
    const val History = "netflyhdr_history"

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
