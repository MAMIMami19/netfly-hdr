package com.nuvio.tv.ui.netflyhdr.themes

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.nuvio.tv.R
import com.nuvio.tv.ui.theme.ThemeColorPalette

/**
 * The 10 red Netfly themes (design spec §3). All share the Netfly red accent
 * #E50914 and differ only in background / surface / surface2.
 *
 * Colors are the EXACT values from the spec table — do not "improve" them.
 */
enum class NetflyThemePreset(
    @StringRes val nameRes: Int,
    @StringRes val descriptionRes: Int,
    val background: Color,
    val surface: Color,
    val surface2: Color
) {
    ORIGINAL(
        R.string.netfly_theme_original,
        R.string.netfly_theme_original_desc,
        background = Color(0xFF0B080B),
        surface = Color(0xFF191015),
        surface2 = Color(0xFF26171D)
    ),
    CINEMA_BLACK(
        R.string.netfly_theme_cinema_black,
        R.string.netfly_theme_cinema_black_desc,
        background = Color(0xFF050505),
        surface = Color(0xFF111111),
        surface2 = Color(0xFF1E1E1E)
    ),
    MIDNIGHT_CRIMSON(
        R.string.netfly_theme_midnight_crimson,
        R.string.netfly_theme_midnight_crimson_desc,
        background = Color(0xFF08070C),
        surface = Color(0xFF15101A),
        surface2 = Color(0xFF24131C)
    ),
    VELVET_RED(
        R.string.netfly_theme_velvet_red,
        R.string.netfly_theme_velvet_red_desc,
        background = Color(0xFF10060A),
        surface = Color(0xFF201018),
        surface2 = Color(0xFF32131C)
    ),
    CARBON_RED(
        R.string.netfly_theme_carbon_red,
        R.string.netfly_theme_carbon_red_desc,
        background = Color(0xFF0B0D10),
        surface = Color(0xFF171A1F),
        surface2 = Color(0xFF242A31)
    ),
    DARK_CHERRY(
        R.string.netfly_theme_dark_cherry,
        R.string.netfly_theme_dark_cherry_desc,
        background = Color(0xFF0D0608),
        surface = Color(0xFF1D0C10),
        surface2 = Color(0xFF2E1118)
    ),
    STUDIO_RED(
        R.string.netfly_theme_studio_red,
        R.string.netfly_theme_studio_red_desc,
        background = Color(0xFF080B10),
        surface = Color(0xFF131820),
        surface2 = Color(0xFF202A35)
    ),
    SCARLET_GLASS(
        R.string.netfly_theme_scarlet_glass,
        R.string.netfly_theme_scarlet_glass_desc,
        background = Color(0xFF12090B),
        surface = Color(0xFF241115),
        surface2 = Color(0xFF35171D)
    ),
    MINIMAL_RED(
        R.string.netfly_theme_minimal_red,
        R.string.netfly_theme_minimal_red_desc,
        background = Color(0xFF0C0C0C),
        surface = Color(0xFF171717),
        surface2 = Color(0xFF242424)
    ),
    DEEP_MAROON(
        R.string.netfly_theme_deep_maroon,
        R.string.netfly_theme_deep_maroon_desc,
        background = Color(0xFF0D0508),
        surface = Color(0xFF1B090E),
        surface2 = Color(0xFF2C1018)
    );

    companion object {
        val DEFAULT = ORIGINAL

        fun fromName(name: String?): NetflyThemePreset =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

/** The shared Netfly red accent. */
val NETFLY_RED = Color(0xFFE50914)

/**
 * Accent override presets (design spec §3). An override changes focus,
 * progress and buttons only — backgrounds and surfaces stay with the theme.
 */
enum class NetflyAccentPreset(
    @StringRes val nameRes: Int,
    val color: Color
) {
    NETFLY_RED(R.string.netfly_accent_netfly_red, Color(0xFFE50914)),
    BLUE(R.string.netfly_accent_blue, Color(0xFF3D8BFF)),
    TEAL(R.string.netfly_accent_teal, Color(0xFF19C3B1)),
    GREEN(R.string.netfly_accent_green, Color(0xFF3CCB6C)),
    ORANGE(R.string.netfly_accent_orange, Color(0xFFFF9A1F)),
    LIGHT_RED(R.string.netfly_accent_light_red, Color(0xFFFF4D5E)),
    PINK(R.string.netfly_accent_pink, Color(0xFFFF5CA8));

    companion object {
        /** Matches a color to a preset, or null when it is a custom hex value. */
        fun fromColor(color: Color): NetflyAccentPreset? =
            entries.firstOrNull { it.color == color }
    }
}

object NetflyThemePalettes {

    /**
     * Builds the app-wide [ThemeColorPalette] for a Netfly preset.
     *
     * @param preset the active red theme (exact spec colors).
     * @param accentOverride when non-null, replaces the accent used for focus,
     * progress and buttons only; backgrounds/surfaces are untouched.
     * @param lightMode when true, the preset switches to its light palette
     * (background #F4F4F4–#F9F0F1, surface #FFFFFF, surface2 #E8E8E8–#F0E0E4
     * per spec §3) while the accent stays red.
     */
    fun paletteFor(
        preset: NetflyThemePreset,
        accentOverride: Color? = null,
        lightMode: Boolean = false
    ): ThemeColorPalette {
        val accent = accentOverride ?: NETFLY_RED
        val accentDark = lerp(accent, Color.Black, 0.28f)
        val foreground: (Color) -> Color = { color ->
            if (color.luminance() > 0.179f) Color.Black else Color.White
        }

        return if (!lightMode) {
            ThemeColorPalette(
                secondary = accent,
                secondaryVariant = accentDark,
                onSecondary = foreground(accent),
                onSecondaryVariant = foreground(accentDark),
                accentGradient = listOf(accent),
                focusRing = accent,
                focusRingGradient = listOf(accent),
                focusBackground = lerp(preset.surface, accent, 0.28f),
                background = preset.background,
                backgroundElevated = preset.surface,
                backgroundCard = preset.surface2,
                surface = preset.surface,
                surfaceVariant = preset.surface2,
                panel = preset.surface,
                field = preset.surface2,
                menu = preset.surface,
                modal = preset.surface
            )
        } else {
            // Light palette per spec §3: background #F4F4F4–#F9F0F1, surface
            // #FFFFFF, surface2 #E8E8E8–#F0E0E4. A whisper of the preset's own
            // background tint keeps each theme identifiable in light mode.
            val lightBackground = lerp(Color(0xFFF4F4F4), preset.background, 0.05f)
            val lightSurface2 = lerp(Color(0xFFE8E8E8), preset.surface2, 0.07f)
            ThemeColorPalette(
                secondary = accent,
                secondaryVariant = accentDark,
                onSecondary = foreground(accent),
                onSecondaryVariant = foreground(accentDark),
                accentGradient = listOf(accent),
                focusRing = accent,
                focusRingGradient = listOf(accent),
                focusBackground = lerp(Color.White, accent, 0.14f),
                background = lightBackground,
                backgroundElevated = Color.White,
                backgroundCard = Color.White,
                surface = Color.White,
                surfaceVariant = lightSurface2,
                panel = Color.White,
                field = lightSurface2,
                menu = Color.White,
                modal = Color.White
            )
        }
    }
}

/** Parses a "#RRGGBB" hex string, or returns null when it is not valid. */
fun parseNetflyHexColor(value: String): Color? {
    val hex = value.trim().removePrefix("#")
    if (hex.length != 6 || hex.any { it !in '0'..'9' && it.uppercaseChar() !in 'A'..'F' }) return null
    val rgb = hex.toIntOrNull(16) ?: return null
    return Color(rgb or 0xFF000000.toInt())
}

/** Formats a [Color] as an uppercase "#RRGGBB" hex string. */
fun formatNetflyHexColor(color: Color): String {
    val r = (color.red * 255f + 0.5f).toInt().coerceIn(0, 255)
    val g = (color.green * 255f + 0.5f).toInt().coerceIn(0, 255)
    val b = (color.blue * 255f + 0.5f).toInt().coerceIn(0, 255)
    return "#%02X%02X%02X".format(r, g, b)
}
