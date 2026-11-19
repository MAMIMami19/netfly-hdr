package com.nuvio.tv.ui.netflyhdr.home

import androidx.annotation.StringRes
import com.nuvio.tv.R
import com.nuvio.tv.data.local.LayoutPreferenceDataStore
import com.nuvio.tv.domain.model.HomeLayout

/** Card shape a Netfly home layout renders. */
enum class NetflyCardShape {
    POSTER,
    LANDSCAPE,
    SQUARE
}

/**
 * The 10 Netfly home layouts (design spec §4).
 *
 * Each layout maps onto the base home machinery: one of the three base
 * [HomeLayout] renderers plus poster metrics, the landscape-posters toggle,
 * the hero section and the fullscreen-hero flag. The mapping is applied by
 * [NetflyHomeLayoutApplier] through [LayoutPreferenceDataStore], so the
 * layouts are real behaviour — not just labels.
 *
 * Poster size 200×300 is the spec's own row size (§4); Compact reuses the
 * base default 126×189.
 */
enum class NetflyHomeLayout(
    @StringRes val nameRes: Int,
    @StringRes val descriptionRes: Int,
    val baseLayout: HomeLayout,
    val posterWidthDp: Int,
    val posterHeightDp: Int,
    val landscapePosters: Boolean,
    val heroEnabled: Boolean,
    val heroFullScreen: Boolean,
    val cardShape: NetflyCardShape,
    /** Rows visible on one screen in the Roku reference (informational). */
    val visibleRows: Int,
    /** Whether the layout shows a featured carousel strip. */
    val hasCarousel: Boolean
) {
    CINEMATIC(
        R.string.netfly_layout_cinematic,
        R.string.netfly_layout_cinematic_desc,
        baseLayout = HomeLayout.MODERN,
        posterWidthDp = 200,
        posterHeightDp = 300,
        landscapePosters = false,
        heroEnabled = true,
        heroFullScreen = false,
        cardShape = NetflyCardShape.POSTER,
        visibleRows = 2,
        hasCarousel = false
    ),
    CLASSIC(
        R.string.netfly_layout_classic,
        R.string.netfly_layout_classic_desc,
        baseLayout = HomeLayout.CLASSIC,
        posterWidthDp = 200,
        posterHeightDp = 300,
        landscapePosters = false,
        heroEnabled = true,
        heroFullScreen = false,
        cardShape = NetflyCardShape.POSTER,
        visibleRows = 4,
        hasCarousel = true
    ),
    GRID(
        R.string.netfly_layout_grid,
        R.string.netfly_layout_grid_desc,
        baseLayout = HomeLayout.GRID,
        posterWidthDp = 200,
        posterHeightDp = 300,
        landscapePosters = false,
        heroEnabled = true,
        heroFullScreen = false,
        cardShape = NetflyCardShape.POSTER,
        visibleRows = 4,
        hasCarousel = false
    ),
    COMPACT(
        R.string.netfly_layout_compact,
        R.string.netfly_layout_compact_desc,
        baseLayout = HomeLayout.MODERN,
        posterWidthDp = 126,
        posterHeightDp = 189,
        landscapePosters = false,
        heroEnabled = true,
        heroFullScreen = false,
        cardShape = NetflyCardShape.POSTER,
        visibleRows = 4,
        hasCarousel = false
    ),
    LANDSCAPE(
        R.string.netfly_layout_landscape,
        R.string.netfly_layout_landscape_desc,
        // Spec style is "classic", but landscape cards only exist in the
        // Modern renderer — Modern + landscape posters is the faithful
        // rendering on this base.
        baseLayout = HomeLayout.MODERN,
        posterWidthDp = 200,
        posterHeightDp = 300,
        landscapePosters = true,
        heroEnabled = true,
        heroFullScreen = false,
        cardShape = NetflyCardShape.LANDSCAPE,
        visibleRows = 4,
        hasCarousel = false
    ),
    SQUARE(
        R.string.netfly_layout_square,
        R.string.netfly_layout_square_desc,
        baseLayout = HomeLayout.GRID,
        posterWidthDp = 200,
        posterHeightDp = 200,
        landscapePosters = false,
        heroEnabled = true,
        heroFullScreen = false,
        cardShape = NetflyCardShape.SQUARE,
        visibleRows = 4,
        hasCarousel = false
    ),
    MOSAIC(
        R.string.netfly_layout_mosaic,
        R.string.netfly_layout_mosaic_desc,
        baseLayout = HomeLayout.GRID,
        posterWidthDp = 160,
        posterHeightDp = 160,
        landscapePosters = false,
        heroEnabled = true,
        heroFullScreen = false,
        cardShape = NetflyCardShape.SQUARE,
        visibleRows = 5,
        hasCarousel = true
    ),
    SPOTLIGHT(
        R.string.netfly_layout_spotlight,
        R.string.netfly_layout_spotlight_desc,
        baseLayout = HomeLayout.MODERN,
        posterWidthDp = 200,
        posterHeightDp = 300,
        landscapePosters = true,
        heroEnabled = true,
        heroFullScreen = true,
        cardShape = NetflyCardShape.LANDSCAPE,
        visibleRows = 3,
        hasCarousel = true
    ),
    MINIMAL(
        R.string.netfly_layout_minimal,
        R.string.netfly_layout_minimal_desc,
        baseLayout = HomeLayout.MODERN,
        posterWidthDp = 200,
        posterHeightDp = 300,
        landscapePosters = true,
        heroEnabled = false,
        heroFullScreen = false,
        cardShape = NetflyCardShape.LANDSCAPE,
        visibleRows = 3,
        hasCarousel = false
    ),
    IMMERSIVE(
        R.string.netfly_layout_immersive,
        R.string.netfly_layout_immersive_desc,
        baseLayout = HomeLayout.MODERN,
        posterWidthDp = 200,
        posterHeightDp = 300,
        landscapePosters = true,
        heroEnabled = true,
        heroFullScreen = true,
        cardShape = NetflyCardShape.LANDSCAPE,
        visibleRows = 2,
        hasCarousel = false
    );

    companion object {
        val DEFAULT = CINEMATIC

        fun fromName(name: String?): NetflyHomeLayout =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

/** Hero presentation mode (design spec §5). */
enum class NetflyHeroMode(
    @StringRes val nameRes: Int,
    @StringRes val descriptionRes: Int
) {
    /** Backdrop 1360×765 + solid side panel (the default). */
    PANEL(
        R.string.netfly_home_hero_mode_panel,
        R.string.netfly_home_hero_mode_panel_desc
    ),

    /** Backdrop fills the whole 1920×1080 screen. */
    FULLSCREEN(
        R.string.netfly_home_hero_mode_fullscreen,
        R.string.netfly_home_hero_mode_fullscreen_desc
    ),

    /** Compact backdrop 1220×686 for dense layouts. */
    BARE(
        R.string.netfly_home_hero_mode_bare,
        R.string.netfly_home_hero_mode_bare_desc
    );

    companion object {
        val DEFAULT = PANEL

        fun fromName(name: String?): NetflyHeroMode =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

/** Titles-per-row options (design spec §4). */
val NETFLY_TITLES_PER_ROW_OPTIONS = listOf(20, 40, 60, 100)

/** Default titles per row when the user has not chosen one. */
const val NETFLY_TITLES_PER_ROW_DEFAULT = 40

object NetflyHomeLayoutApplier {

    /**
     * Applies a Netfly layout to the base home machinery. Every write goes
     * through [LayoutPreferenceDataStore], so the change survives restarts
     * and is per-profile like all other layout prefs.
     *
     * Apply the hero mode AFTER the layout: both touch the fullscreen-hero
     * flag, and the explicit hero-mode choice wins.
     */
    suspend fun applyLayout(
        layout: NetflyHomeLayout,
        layoutPrefs: LayoutPreferenceDataStore
    ) {
        layoutPrefs.setLayout(layout.baseLayout)
        layoutPrefs.setPosterCardWidthDp(layout.posterWidthDp)
        layoutPrefs.setPosterCardHeightDp(layout.posterHeightDp)
        layoutPrefs.setModernLandscapePostersEnabled(layout.landscapePosters)
        layoutPrefs.setHeroSectionEnabled(layout.heroEnabled)
        layoutPrefs.setModernHeroFullScreenBackdropEnabled(layout.heroFullScreen)
    }

    /**
     * Applies a hero mode on top of the current layout. Only the fullscreen
     * flag has a base equivalent: FULLSCREEN turns the fullscreen hero
     * backdrop on, PANEL and BARE turn it off (BARE's compact 1220×686
     * rendering lives in [NetflyHero][com.nuvio.tv.ui.netflyhdr.home.NetflyHero]).
     */
    suspend fun applyHeroMode(
        mode: NetflyHeroMode,
        layoutPrefs: LayoutPreferenceDataStore
    ) {
        layoutPrefs.setModernHeroFullScreenBackdropEnabled(mode == NetflyHeroMode.FULLSCREEN)
    }
}
