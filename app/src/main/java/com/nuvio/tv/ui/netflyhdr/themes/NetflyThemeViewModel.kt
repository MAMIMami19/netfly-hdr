package com.nuvio.tv.ui.netflyhdr.themes

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.data.local.LayoutPreferenceDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NetflyThemeViewModel @Inject constructor(
    private val netflyHdrDataStore: NetflyHdrDataStore,
    private val layoutPreferenceDataStore: LayoutPreferenceDataStore
) : ViewModel() {

    val state: StateFlow<NetflyHdrState> = netflyHdrDataStore.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NetflyHdrState())

    /**
     * The theme state as it was when the manager opened, captured from the
     * first real DataStore emission (never the default). BACK cancels a
     * preview by restoring exactly this state.
     */
    private val _entryState = kotlinx.coroutines.flow.MutableStateFlow<NetflyHdrState?>(null)
    val entryState: StateFlow<NetflyHdrState?> = _entryState

    init {
        viewModelScope.launch {
            _entryState.value = netflyHdrDataStore.state.first()
        }
    }

    /** Live-preview a preset (writes immediately; cancel restores the entry state). */
    fun previewPreset(preset: NetflyThemePreset) {
        viewModelScope.launch { netflyHdrDataStore.setPreset(preset) }
    }

    fun confirmPreset(preset: NetflyThemePreset) {
        viewModelScope.launch { netflyHdrDataStore.setPreset(preset) }
    }

    fun restoreEntryTheme(enabled: Boolean, preset: NetflyThemePreset) {
        viewModelScope.launch { netflyHdrDataStore.restoreTheme(enabled, preset) }
    }

    fun setLightMode(light: Boolean) {
        viewModelScope.launch { netflyHdrDataStore.setLightMode(light) }
    }

    fun setAccent(color: Color?) {
        viewModelScope.launch { netflyHdrDataStore.setAccentOverride(color) }
    }

    fun setCustomHex(hex: String) {
        val color = parseNetflyHexColor(hex) ?: return
        setAccent(color)
    }

    /**
     * Reduce-motion (spec §3): disables card expansions and auto-playing
     * trailers. The previous expansion / trailer flags are saved first, so
     * turning it off restores exactly what the user had.
     */
    fun setReduceMotion(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                val prevExpand = layoutPreferenceDataStore.focusedPosterBackdropExpandEnabled.first()
                val prevTrailer = layoutPreferenceDataStore.focusedPosterBackdropTrailerEnabled.first()
                netflyHdrDataStore.setReduceMotion(
                    enabled = true,
                    previousExpandEnabled = prevExpand,
                    previousTrailerEnabled = prevTrailer
                )
                layoutPreferenceDataStore.setFocusedPosterBackdropExpandEnabled(false)
                layoutPreferenceDataStore.setFocusedPosterBackdropTrailerEnabled(false)
            } else {
                val (prevExpand, prevTrailer) = netflyHdrDataStore.readReduceMotionPrevious()
                netflyHdrDataStore.setReduceMotion(enabled = false)
                layoutPreferenceDataStore.setFocusedPosterBackdropExpandEnabled(prevExpand)
                layoutPreferenceDataStore.setFocusedPosterBackdropTrailerEnabled(prevTrailer)
            }
        }
    }

    /** Leaves the Netfly themes: the base Nuvio theme selection applies again. */
    fun backToNuvioThemes() {
        viewModelScope.launch { netflyHdrDataStore.setNetflyThemeEnabled(false) }
    }
}
