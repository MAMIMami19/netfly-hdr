package com.nuvio.tv.ui.netflyhdr

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Netfly HDR accent — the single red of the Netfly design (NOTE.md §2).
 *
 * This is the ONLY hardcoded color in the Netfly HDR nav/cards/detail area:
 * every surface and text color comes from `MaterialTheme.colorScheme`
 * (owned by the theme system), and this constant is used only where the spec
 * demands the Netfly red: focus rings, the active rail item, progress bars
 * and primary actions.
 */
val NetflyHdrAccent: Color = Color(0xFFE50914)

/**
 * Dimensions for the Netfly HDR nav/cards/detail area, taken from the design
 * spec (NOTE.md §§6–8, 14). Spec numbers are Roku 1920×1080 canvas pixels;
 * on a 1080p Android TV screen at xhdpi (density 2.0) 2 spec px = 1 dp, so
 * every spec dimension is halved here. The design stays proportional on
 * other densities because every value scales together.
 */
object NetflyHdrDimensions {
    // Poster card (§7): 200×300 px.
    val posterWidth = 100.dp
    val posterHeight = 150.dp
    val posterCornerRadius = 6.dp
    val posterGap = 13.dp // 26 px row gap.

    // Expanded card (§7): 532×300 px; height matches the poster.
    val expandedCardWidth = 266.dp
    val expandedCardHeight = 150.dp
    val expandedCardCornerRadius = 8.dp

    // Continue Watching progress bar (§7): 6 px tall.
    val progressBarHeight = 3.dp

    // Watched checkmark (§7): 34 px circle.
    val watchedBadgeSize = 17.dp

    // NavRail (§6): 76 px rows, 40×40 px icons, 60 px avatar.
    val railRowHeight = 38.dp
    val railIconSize = 20.dp
    val railAvatarSize = 30.dp
    val railAvatarTopOffset = 95.dp // avatar at y=190 px.
    val railCollapsedWidth = 76.dp
    val railItemSpacing = 5.dp // 10 px between rows.

    // Modern floating panel (§6): 470×1024 px.
    val railPanelWidth = 235.dp
    val railPanelHeight = 512.dp
    val railPanelCornerRadius = 12.dp

    // Floating section pill (§6): 64 px tall.
    val sectionPillHeight = 32.dp

    // Focus ring width.
    val focusRingWidth = 2.dp
}

/** Timing constants for the Netfly HDR nav/cards/detail area. */
object NetflyHdrMotion {
    /** Delay before a focused poster expands (§7, configurable, default 3 s). */
    const val EXPAND_DELAY_MS = 3000L

    /** Expand grow duration (§7): 0.24 s. */
    const val EXPAND_DURATION_MS = 240

    /** How long the floating section pill stays visible after navigating (§6). */
    const val SECTION_PILL_VISIBLE_MS = 3000L

    /** Focus-settle ramp: the animated-art layer appears once focus progress
     *  passes [FOCUS_SETTLED_THRESHOLD], mirroring the spec's
     *  `focusPercent > 0.95` gate (§7). */
    const val FOCUS_SETTLE_MS = 150
    const val FOCUS_SETTLED_THRESHOLD = 0.95f

    /** Rail panel open/close. */
    const val RAIL_PANEL_IN_MS = 300
    const val RAIL_PANEL_OUT_MS = 200
}

/** outQuad easing (1 − (1−t)²) for the expanded-card grow (§7). */
val NetflyHdrOutQuad: Easing = CubicBezierEasing(0.25f, 0.46f, 0.45f, 0.94f)
