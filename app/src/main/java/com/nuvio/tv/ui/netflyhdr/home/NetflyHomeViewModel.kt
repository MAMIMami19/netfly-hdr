package com.nuvio.tv.ui.netflyhdr.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.data.local.LayoutPreferenceDataStore
import com.nuvio.tv.ui.netflyhdr.themes.NetflyHdrDataStore
import com.nuvio.tv.ui.netflyhdr.themes.NetflyHdrState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NetflyHomeViewModel @Inject constructor(
    private val netflyHdrDataStore: NetflyHdrDataStore,
    private val layoutPreferenceDataStore: LayoutPreferenceDataStore
) : ViewModel() {

    val state: StateFlow<NetflyHdrState> = netflyHdrDataStore.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NetflyHdrState())

    /** Persists the layout and applies it to the base home machinery. */
    fun selectLayout(layout: NetflyHomeLayout) {
        viewModelScope.launch {
            netflyHdrDataStore.setHomeLayout(layout)
            NetflyHomeLayoutApplier.applyLayout(layout, layoutPreferenceDataStore)
            // The layout may have changed the fullscreen-hero flag; re-apply
            // the user's explicit hero-mode choice on top.
            NetflyHomeLayoutApplier.applyHeroMode(state.value.heroMode, layoutPreferenceDataStore)
        }
    }

    /** Persists the hero mode and applies its fullscreen flag. */
    fun selectHeroMode(mode: NetflyHeroMode) {
        viewModelScope.launch {
            netflyHdrDataStore.setHeroMode(mode)
            NetflyHomeLayoutApplier.applyHeroMode(mode, layoutPreferenceDataStore)
        }
    }

    fun setTitlesPerRow(count: Int) {
        viewModelScope.launch { netflyHdrDataStore.setTitlesPerRow(count) }
    }
}
