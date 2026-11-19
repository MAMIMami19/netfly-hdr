package com.nuvio.tv.ui.netflyhdr.ambient

/**
 * Netfly HDR ambient mode (design §12) — spec constants and verification map.
 *
 * VERIFIED 2026-11-19: the NuvioTV 1.2.0-beta.2 base ALREADY implements the
 * full §12 ambient mode, so this package adds no second implementation —
 * duplicating it would create two competing idle timers. What the base has:
 *
 * | Spec (§12) | Base implementation | Status |
 * |---|---|---|
 * | After N idle minutes, default 5 | `AmbientModeSettings.DEFAULT_TIMEOUT_MINUTES = 5` | ✓ |
 * | On/off toggle + duration picker in settings | `AmbientModeSettings` screen (toggle + 1/2/5/10/15/30 min) | ✓ |
 * | Per-profile settings | `AmbientModeDataStore` is profile-scoped | ✓ |
 * | Fullscreen slideshow of catalog backdrops | `AmbientSlideshow` + `AmbientSlideLoader` (up to 30 slides, TMDB-enriched) | ✓ |
 * | 1920x1080 art | backdrops loaded at screen pixel size | ✓ |
 * | Slow Ken Burns zoom 1.0 → 1.06 over ~13.6s | `AmbientMotion` (1.0 → 1.06 is the first motion; full cycle ≈ 13.0s) | ✓ |
 * | Bottom gradient + logo + centered title + genres | gradient 0.45 → black 0.7; TMDB logo or title text (centered); genres row | ✓ |
 * | Fade in/out | backdrop 900ms in / 700ms out; title 500/350ms; host 900/400ms | ✓ |
 * | ANY key exits | `AmbientIdleController.consumeKeyEvent` via `MainActivity.dispatchKeyEvent` (swallows the dismissing press) | ✓ |
 * | Idle detection resets on any key press | `AmbientIdleController.onInteraction()` on every dispatched key event | ✓ |
 * | Hosted above all screens | `AmbientModeHost` in `MainActivity` (RESUMED-only, keeps screen on while showing) | ✓ |
 *
 * Known minor deviation: the title block sits bottom-LEFT (72dp margin) with
 * the title text centered inside its column, where the Roku original centers
 * the block itself. Cosmetic only; not worth forking the slideshow.
 *
 * The constants below are the §12 reference values, kept here so a future
 * Netfly restyle of the slideshow reads them from one place.
 */
object NetflyAmbientSpec {

    /** Default idle delay before ambient mode starts (minutes). */
    const val DEFAULT_TIMEOUT_MINUTES = 5

    /** Idle-delay options offered in settings (minutes). */
    val TIMEOUT_OPTIONS_MINUTES = listOf(1, 2, 5, 10, 15, 30)

    /** Ken Burns zoom start scale. */
    const val KEN_BURNS_START_SCALE = 1.0f

    /** Ken Burns zoom end scale. */
    const val KEN_BURNS_END_SCALE = 1.06f

    /** Ken Burns motion duration (milliseconds, ≈13.6s). */
    const val KEN_BURNS_DURATION_MS = 13_600

    /** Backdrop fade-in duration (milliseconds). */
    const val BACKDROP_FADE_IN_MS = 900

    /** Backdrop fade-out duration (milliseconds). */
    const val BACKDROP_FADE_OUT_MS = 700

    /** Title fade-in duration (milliseconds). */
    const val TITLE_FADE_IN_MS = 500
}
