@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.settings

import com.nuvio.tv.ui.theme.NuvioTheme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.nuvio.tv.R
import com.nuvio.tv.ui.netflyhdr.profiles.NetflyKidsProfileSection

@Composable
internal fun ProfileSettingsContent(
    onManageProfiles: () -> Unit,
    onOpenAvatarGallery: () -> Unit = {},
    initialFocusRequester: FocusRequester? = null
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.md)
    ) {
        SettingsDetailHeader(
            title = stringResource(R.string.profile_title),
            subtitle = stringResource(R.string.profile_subtitle)
        )
        SettingsGroupCard(modifier = Modifier.fillMaxWidth()) {
            SettingsActionRow(
                title = stringResource(R.string.profile_manage_button),
                subtitle = null,
                onClick = onManageProfiles,
                modifier = if (initialFocusRequester != null) {
                    Modifier.focusRequester(initialFocusRequester)
                } else {
                    Modifier
                }
            )
        }
        // Netfly HDR (§11): avatar gallery for the active profile.
        SettingsGroupCard(modifier = Modifier.fillMaxWidth()) {
            SettingsActionRow(
                title = stringResource(R.string.netfly_avatar_gallery_row_title),
                subtitle = stringResource(R.string.netfly_avatar_gallery_row_subtitle),
                onClick = onOpenAvatarGallery
            )
        }
        // Netfly HDR (§11): kids age-rating limit for the active profile.
        SettingsGroupCard(modifier = Modifier.fillMaxWidth()) {
            NetflyKidsProfileSection()
        }
    }
}
