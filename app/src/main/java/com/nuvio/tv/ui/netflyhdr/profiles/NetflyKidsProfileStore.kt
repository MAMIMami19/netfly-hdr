package com.nuvio.tv.ui.netflyhdr.profiles

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.data.local.ProfileDataStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Per-profile kids settings (design §11: "kids age-rating limit").
 *
 * Stored in the profile-scoped DataStore, exactly like the theme and ambient
 * settings: each profile keeps its own kids flag and age limit, and deleting
 * a profile wipes its file (see [ProfileManager.deleteProfile]).
 *
 * Enforcement: [KidsContentFilter] turns these settings into a row filter.
 * Home/search pipelines apply `CatalogRow.filterKidsItems(maxAge)` when the
 * active profile has kids mode on (hook documented on [KidsContentFilter]).
 */
data class NetflyKidsSettings(
    val enabled: Boolean = false,
    val maxAgeYears: Int = DEFAULT_MAX_AGE
) {
    companion object {
        const val DEFAULT_MAX_AGE = 12

        /** Age limits offered in the picker. */
        val MAX_AGE_OPTIONS = listOf(7, 12, 16, 18)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class NetflyKidsProfileStore @Inject constructor(
    private val factory: ProfileDataStoreFactory,
    private val profileManager: ProfileManager
) {
    companion object {
        /** Profile-scoped DataStore feature name. */
        const val FEATURE = "netfly_kids_profile"
    }

    private fun store() = factory.get(profileManager.activeProfileId.value, FEATURE)

    private val enabledKey = booleanPreferencesKey("kids_mode_enabled")
    private val maxAgeKey = intPreferencesKey("kids_max_age_years")

    val settings: Flow<NetflyKidsSettings> =
        profileManager.activeProfileId.flatMapLatest { pid ->
            factory.get(pid, FEATURE).data.map { prefs ->
                NetflyKidsSettings(
                    enabled = prefs[enabledKey] ?: false,
                    maxAgeYears = prefs[maxAgeKey] ?: NetflyKidsSettings.DEFAULT_MAX_AGE
                )
            }
        }.distinctUntilChanged()

    suspend fun setEnabled(enabled: Boolean) {
        store().edit { it[enabledKey] = enabled }
    }

    suspend fun setMaxAgeYears(years: Int) {
        store().edit { it[maxAgeKey] = years }
    }
}
