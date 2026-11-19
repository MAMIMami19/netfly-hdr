package com.nuvio.tv.ui.netflyhdr.profiles

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.ui.netflyhdr.NetflyRed

/**
 * Netfly HDR avatar gallery (design §11): 10 columns, 50 avatars per page,
 * 256x256 assets, no labels.
 *
 * Shows the built-in original avatars from [NetflyLocalAvatars] and applies
 * the chosen one to the ACTIVE profile. Remote (server) avatars keep living
 * in the profile create/edit picker, which also lists the local set.
 */
@Composable
fun NetflyAvatarGalleryScreen(
    onBackPress: () -> Unit,
    viewModel: NetflyAvatarGalleryViewModel = hiltViewModel()
) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val activeProfileId by viewModel.activeProfileId.collectAsStateWithLifecycle()
    val activeProfile = profiles.firstOrNull { it.id == activeProfileId }
    val selectedAvatarId = activeProfile?.avatarId

    val avatars = remember { NetflyLocalAvatars.drawables }
    val pageCount = ((avatars.size - 1) / NetflyAvatarGalleryDefaults.PageSize) + 1
    var currentPage by remember { mutableIntStateOf(0) }
    val pageAvatars = remember(avatars, currentPage) {
        avatars.drop(currentPage * NetflyAvatarGalleryDefaults.PageSize)
            .take(NetflyAvatarGalleryDefaults.PageSize)
    }
    val focusRequesters = remember(pageAvatars) {
        pageAvatars.associateWith { FocusRequester() }
    }
    var initialFocusDone by remember { mutableStateOf(false) }

    BackHandler(onBack = onBackPress)

    // First entry focuses the profile's current avatar (when it is a local
    // one); page changes focus the first avatar of the new page.
    LaunchedEffect(currentPage, pageAvatars) {
        val target = if (!initialFocusDone) {
            val selectedRes = NetflyLocalAvatars.drawableResForId(selectedAvatarId)
            if (selectedRes != null && selectedRes in focusRequesters) selectedRes
            else pageAvatars.firstOrNull()
        } else {
            pageAvatars.firstOrNull()
        }
        initialFocusDone = true
        target?.let { runCatching { focusRequesters.getValue(it).requestFocus() } }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 48.dp, vertical = 32.dp)
    ) {
        Text(
            text = stringResource(R.string.netfly_avatar_gallery_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (activeProfile != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.netfly_avatar_gallery_subtitle,
                    activeProfile.name
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.netfly_avatar_gallery_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { if (currentPage > 0) currentPage-- },
                enabled = currentPage > 0
            ) {
                Text(text = stringResource(R.string.netfly_avatar_gallery_previous_page))
            }
            Text(
                text = stringResource(
                    R.string.netfly_avatar_gallery_page,
                    currentPage + 1,
                    pageCount
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = { if (currentPage < pageCount - 1) currentPage++ },
                enabled = currentPage < pageCount - 1
            ) {
                Text(text = stringResource(R.string.netfly_avatar_gallery_next_page))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(NetflyAvatarGalleryDefaults.Columns),
            contentPadding = PaddingValues(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(pageAvatars, key = { it }) { drawableRes ->
                val index = avatars.indexOf(drawableRes)
                NetflyAvatarGalleryItem(
                    drawableRes = drawableRes,
                    contentDescription = stringResource(R.string.netfly_avatar_name, index + 1),
                    isSelected = NetflyLocalAvatars.avatarId(index) == selectedAvatarId,
                    focusRequester = focusRequesters.getValue(drawableRes),
                    onClick = { viewModel.selectAvatar(NetflyLocalAvatars.avatarId(index)) }
                )
            }
        }
    }
}

private object NetflyAvatarGalleryDefaults {
    /** Design §11: 10 columns. */
    const val Columns = 10

    /** Design §11: 50 avatars per page. */
    const val PageSize = 50
}

@Composable
private fun NetflyAvatarGalleryItem(
    drawableRes: Int,
    contentDescription: String,
    isSelected: Boolean,
    focusRequester: FocusRequester,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(96.dp)
            .graphicsLayer {
                val scale = if (isFocused) 1.08f else 1f
                scaleX = scale
                scaleY = scale
            }
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .clip(CircleShape)
            .border(
                width = if (isFocused || isSelected) 3.dp else 0.dp,
                color = if (isFocused || isSelected) NetflyRed else Color.Transparent,
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(drawableRes),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(NetflyRed),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
