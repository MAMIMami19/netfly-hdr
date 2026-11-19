package com.nuvio.tv.ui.netflyhdr.themes

import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.data.local.ProfileDataStoreFactory
import com.nuvio.tv.ui.netflyhdr.home.NETFLY_TITLES_PER_ROW_DEFAULT
import com.nuvio.tv.ui.netflyhdr.home.NetflyHeroMode
import com.nuvio.tv.ui.netflyhdr.home.NetflyHomeLayout
import com.nuvio.tv.ui.theme.ThemeColorPalette
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Everything Netfly HDR persists for one profile: the active red theme, the
 * accent override, light/dark, reduce-motion, the home layout, the hero mode
 * and titles-per-row.
 *
 * Per-profile, using the same [ProfileDataStoreFactory] pattern as
 * ThemeDataStore / LayoutPreferenceDataStore (one DataStore file per profile
 * per feature).
 */
data class NetflyHdrState(
    /** False only when the user went back to the base Nuvio themes. */
    val netflyThemeEnabled: Boolean = true,
    val preset: NetflyThemePreset = NetflyThemePreset.DEFAULT,
    val lightMode: Boolean = false,
    /** "#RRGGBB" accent override, or null for the theme's own red. */
    val accentOverrideHex: String? = null,
    val reduceMotion: Boolean = false,
    val homeLayout: NetflyHomeLayout = NetflyHomeLayout.DEFAULT,
    val heroMode: NetflyHeroMode = NetflyHeroMode.DEFAULT,
    val titlesPerRow: Int = NETFLY_TITLES_PER_ROW_DEFAULT
) {
    val accentOverride: Color?
        get() = accentOverrideHex?.let { parseNetflyHexColor(it) }

    /** The palette the whole app renders with while a Netfly theme is active. */
    fun toColorPalette(): ThemeColorPalette =
        NetflyThemePalettes.paletteFor(
            preset = preset,
            accentOverride = accentOverride,
            lightMode = lightMode
        )
}

@Singleton
class NetflyHdrDataStore @Inject constructor(
    private val factory: ProfileDataStoreFactory,
    private val profileManager: ProfileManager
) {
    companion object {
        private const val FEATURE = "netfly_hdr_settings"
    }

    private fun store(profileId: Int = profileManager.activeProfileId.value) =
        factory.get(profileId, FEATURE)

    private val themeEnabledKey = booleanPreferencesKey("netfly_theme_enabled")
    private val presetKey = stringPreferencesKey("theme_preset")
    private val lightModeKey = booleanPreferencesKey("light_mode")
    private val accentOverrideKey = stringPreferencesKey("accent_override_hex")
    private val reduceMotionKey = booleanPreferencesKey("reduce_motion")
    private val homeLayoutKey = stringPreferencesKey("home_layout")
    private val heroModeKey = stringPreferencesKey("hero_mode")
    private val titlesPerRowKey = intPreferencesKey("titles_per_row")
    // Previous expansion / trailer flags, saved when reduce-motion is turned
    // on so turning it off restores exactly what the user had.
    private val prevExpandEnabledKey = booleanPreferencesKey("reduce_motion_prev_expand")
    private val prevTrailerEnabledKey = booleanPreferencesKey("reduce_motion_prev_trailer")

    val state: Flow<NetflyHdrState> = profileManager.activeProfileId.flatMapLatest { pid ->
        factory.get(pid, FEATURE).data.map { prefs ->
            NetflyHdrState(
                netflyThemeEnabled = prefs[themeEnabledKey] ?: true,
                preset = NetflyThemePreset.fromName(prefs[presetKey]),
                lightMode = prefs[lightModeKey] ?: false,
                accentOverrideHex = prefs[accentOverrideKey]?.takeIf { it.isNotBlank() },
                reduceMotion = prefs[reduceMotionKey] ?: false,
                homeLayout = NetflyHomeLayout.fromName(prefs[homeLayoutKey]),
                heroMode = NetflyHeroMode.fromName(prefs[heroModeKey]),
                titlesPerRow = prefs[titlesPerRowKey] ?: NETFLY_TITLES_PER_ROW_DEFAULT
            )
        }
    }

    suspend fun setNetflyThemeEnabled(enabled: Boolean) {
        store().edit { prefs -> prefs[themeEnabledKey] = enabled }
    }

    suspend fun setPreset(preset: NetflyThemePreset) {
        store().edit { prefs ->
            prefs[presetKey] = preset.name
            prefs[themeEnabledKey] = true
        }
    }

    /**
     * Restores a previously active theme state (used when the Theme Manager
     * is cancelled: the previewed preset is reverted to the entry state,
     * including the case where no Netfly theme was active at entry).
     */
    suspend fun restoreTheme(enabled: Boolean, preset: NetflyThemePreset) {
        store().edit { prefs ->
            prefs[presetKey] = preset.name
            prefs[themeEnabledKey] = enabled
        }
    }

    suspend fun setLightMode(light: Boolean) {
        store().edit { prefs -> prefs[lightModeKey] = light }
    }

    /** Null clears the override and restores the theme's own red accent. */
    suspend fun setAccentOverride(color: Color?) {
        store().edit { prefs ->
            if (color == null) {
                prefs.remove(accentOverrideKey)
            } else {
                prefs[accentOverrideKey] = formatNetflyHexColor(color)
            }
        }
    }

    suspend fun setReduceMotion(
        enabled: Boolean,
        previousExpandEnabled: Boolean? = null,
        previousTrailerEnabled: Boolean? = null
    ) {
        store().edit { prefs ->
            prefs[reduceMotionKey] = enabled
            if (enabled) {
                if (previousExpandEnabled != null) prefs[prevExpandEnabledKey] = previousExpandEnabled
                if (previousTrailerEnabled != null) prefs[prevTrailerEnabledKey] = previousTrailerEnabled
            }
        }
    }

    /** Values saved when reduce-motion was turned on, for restore on turn-off. */
    suspend fun readReduceMotionPrevious(): Pair<Boolean, Boolean> {
        val prefs = store().data.first()
        val expand = prefs[prevExpandEnabledKey] ?: true
        val trailer = prefs[prevTrailerEnabledKey] ?: false
        return expand to trailer
    }

    suspend fun setHomeLayout(layout: NetflyHomeLayout) {
        store().edit { prefs -> prefs[homeLayoutKey] = layout.name }
    }

    suspend fun setHeroMode(mode: NetflyHeroMode) {
        store().edit { prefs -> prefs[heroModeKey] = mode.name }
    }

    suspend fun setTitlesPerRow(count: Int) {
        store().edit { prefs -> prefs[titlesPerRowKey] = count }
    }
}
