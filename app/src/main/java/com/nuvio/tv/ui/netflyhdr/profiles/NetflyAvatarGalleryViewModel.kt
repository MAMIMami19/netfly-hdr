package com.nuvio.tv.ui.netflyhdr.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.core.sync.ProfileSyncService
import com.nuvio.tv.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NetflyAvatarGalleryViewModel @Inject constructor(
    private val profileManager: ProfileManager,
    private val profileSyncService: ProfileSyncService
) : ViewModel() {

    val profiles: StateFlow<List<UserProfile>> = profileManager.profiles
    val activeProfileId: StateFlow<Int> = profileManager.activeProfileId

    /** Applies the chosen local avatar to the active profile. */
    fun selectAvatar(avatarId: String) {
        viewModelScope.launch {
            val profile = profileManager.activeProfile ?: return@launch
            if (profile.avatarId == avatarId) return@launch
            profileManager.updateProfile(profile.copy(avatarId = avatarId))
            profileSyncService.pushToRemote()
        }
    }
}
