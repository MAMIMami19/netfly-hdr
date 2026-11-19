package com.nuvio.tv.ui.netflyhdr.profiles

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.data.local.ProfileDataStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Per-profile appearance contract for the theme system (design §3 + §11:
 * "theme and light/dark mode per profile").
 *
 * The base theme selection already lives per-profile in
 * [com.nuvio.tv.data.local.ThemeDataStore] (feature `theme_settings`, key
 * `selected_theme`, values are [com.nuvio.tv.domain.model.AppTheme] names).
 * That store has NO light/dark mode, so this store adds the missing half:
 *
 * - Feature: `netfly_profile_appearance` (profile-scoped DataStore file,
 *   one per profile id — deleted with the profile).
 * - Key `netfly_light_mode` (boolean, default false): the profile's
 *   light/dark mode. The theme system reads it via [lightMode] /
 *   [observeLightModeForProfile] and applies the light palette variant of
 *   the active Netfly theme when true.
 * - Key `netfly_theme_id` (string, null = Netfly Original): the Netfly red
 *   theme id from [NetflyThemeIds]. The theme builder maps these ids to its
 *   palettes; this store only persists the choice per profile.
 *
 * No UI lives here on purpose: the theme builder owns the appearance
 * settings UI and reads/writes these keys from there.
 */
data class NetflyProfileAppearance(
    val lightMode: Boolean = false,
    val netflyThemeId: String? = null
)

/** The ten Netfly red themes (design §3), as stable per-profile ids. */
object NetflyThemeIds {
    const val NETFLY_ORIGINAL = "netfly_original"
    const val CINEMA_BLACK = "cinema_black"
    const val MIDNIGHT_CRIMSON = "midnight_crimson"
    const val VELVET_RED = "velvet_red"
    const val CARBON_RED = "carbon_red"
    const val DARK_CHERRY = "dark_cherry"
    const val STUDIO_RED = "studio_red"
    const val SCARLET_GLASS = "scarlet_glass"
    const val MINIMAL_RED = "minimal_red"
    const val DEEP_MAROON = "deep_maroon"

    val ALL = listOf(
        NETFLY_ORIGINAL,
        CINEMA_BLACK,
        MIDNIGHT_CRIMSON,
        VELVET_RED,
        CARBON_RED,
        DARK_CHERRY,
        STUDIO_RED,
        SCARLET_GLASS,
        MINIMAL_RED,
        DEEP_MAROON
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class NetflyProfileAppearanceStore @Inject constructor(
    private val factory: ProfileDataStoreFactory,
    private val profileManager: ProfileManager
) {
    companion object {
        /** Profile-scoped DataStore feature name. */
        const val FEATURE = "netfly_profile_appearance"

        /** Boolean key: true = light mode for this profile. */
        const val LIGHT_MODE_KEY = "netfly_light_mode"

        /** String key: Netfly theme id (see [NetflyThemeIds]). */
        const val THEME_ID_KEY = "netfly_theme_id"
    }

    private fun store(profileId: Int = profileManager.activeProfileId.value) =
        factory.get(profileId, FEATURE)

    private val lightModeKey = booleanPreferencesKey(LIGHT_MODE_KEY)
    private val themeIdKey = stringPreferencesKey(THEME_ID_KEY)

    /** Appearance of the currently active profile. */
    val appearance: Flow<NetflyProfileAppearance> =
        profileManager.activeProfileId.flatMapLatest { pid ->
            factory.get(pid, FEATURE).data.map { prefs ->
                NetflyProfileAppearance(
                    lightMode = prefs[lightModeKey] ?: false,
                    netflyThemeId = prefs[themeIdKey]
                )
            }
        }.distinctUntilChanged()

    /** Light/dark mode of the currently active profile. */
    val lightMode: Flow<Boolean> = appearance.map { it.lightMode }.distinctUntilChanged()

    suspend fun setLightMode(enabled: Boolean) {
        store().edit { it[lightModeKey] = enabled }
    }

    suspend fun setNetflyThemeId(themeId: String?) {
        store().edit { prefs ->
            if (themeId == null) prefs.remove(themeIdKey) else prefs[themeIdKey] = themeId
        }
    }

    suspend fun getLightModeForProfile(profileId: Int): Boolean {
        val prefs = factory.get(profileId, FEATURE).data.first()
        return prefs[lightModeKey] ?: false
    }

    fun observeLightModeForProfile(profileId: Int): Flow<Boolean> =
        factory.get(profileId, FEATURE).data.map { prefs ->
            prefs[lightModeKey] ?: false
        }

    suspend fun getNetflyThemeIdForProfile(profileId: Int): String? {
        val prefs = factory.get(profileId, FEATURE).data.first()
        return prefs[themeIdKey]
    }
}
