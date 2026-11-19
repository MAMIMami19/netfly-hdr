package com.nuvio.tv.ui.netflyhdr

import androidx.compose.ui.graphics.Color

/**
 * Route contract for the Netfly HDR browse + hub destinations.
 *
 * The NavRail builder navigates to these exact strings; the NavHost registers
 * them in [com.nuvio.tv.ui.navigation.NuvioNavHost]. Search, Discover and
 * Library keep the base routes ("search", "discover", "library") — only their
 * screen content is swapped for the Netfly HDR presentation.
 */
object NetflyHdrRoutes {
    /** Episode calendar — own nav page (§9). */
    const val CALENDAR = "netflyhdr_calendar"

    /** Recently-watched history page (§9). */
    const val HISTORY = "netflyhdr_history"

    /** Universal Provider Hub — the addon store (§10). */
    const val STORE = "netflyhdr_store"
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
