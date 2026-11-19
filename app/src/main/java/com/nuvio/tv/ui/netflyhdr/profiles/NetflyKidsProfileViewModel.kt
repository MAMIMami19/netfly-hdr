package com.nuvio.tv.ui.netflyhdr.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NetflyKidsProfileViewModel @Inject constructor(
    private val store: NetflyKidsProfileStore
) : ViewModel() {

    val settings: StateFlow<NetflyKidsSettings> = store.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, NetflyKidsSettings())

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch { store.setEnabled(enabled) }
    }

    fun setMaxAgeYears(years: Int) {
        viewModelScope.launch { store.setMaxAgeYears(years) }
    }
}
