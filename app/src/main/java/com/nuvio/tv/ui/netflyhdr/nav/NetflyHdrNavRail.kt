package com.nuvio.tv.ui.netflyhdr.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.DrawerItem
import com.nuvio.tv.R
import com.nuvio.tv.ui.components.ProfileAvatarCircle
import com.nuvio.tv.ui.netflyhdr.NetflyHdrAccent
import com.nuvio.tv.ui.netflyhdr.NetflyHdrDimensions
import com.nuvio.tv.ui.util.rememberRawSvgPainter

/**
 * Netfly HDR navigation rail (NOTE.md §6): a collapsible left sidebar.
 *
 * - Collapsed: an icon-only strip ([NetflyHdrCollapsedRail]) — 40×40 px icons
 *   in 76 px rows, labels hidden, profile avatar (60 px circle) at the top.
 *   The active item's icon is tinted with the Netfly red accent.
 * - Expanded: the modern floating panel ([NetflyHdrExpandedRailPanel]),
 *   470×1024 px, with the wordmark, the profile row and labeled items.
 * - While collapsed, navigating shows the floating section pill
 *   ([NetflyHdrSectionPill], 64 px, top-left) with the icon + current section
 *   name for 3 s.
 *
 * Surfaces and text come from `MaterialTheme.colorScheme` (theme system);
 * only the accent is the Netfly red constant.
 */

@Composable
internal fun NetflyHdrRailIcon(
    iconRes: Int?,
    icon: ImageVector?,
    tint: Color,
    modifier: Modifier = Modifier
) {
    when {
        icon != null -> Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = modifier
        )

        iconRes != null -> Icon(
            painter = rememberRawSvgPainter(iconRes),
            contentDescription = null,
            tint = tint,
            modifier = modifier
        )
    }
}

/**
 * Collapsed rail: icons only, never focusable — LEFT from content opens the
 * expanded panel instead (see [NetflyHdrNavRailScaffold]).
 */
@Composable
fun NetflyHdrCollapsedRail(
    items: List<DrawerItem>,
    selectedRoute: String?,
    profileName: String,
    profileColorHex: String,
    profileAvatarImageUrl: String?,
    showProfileSelector: Boolean,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .width(NetflyHdrDimensions.railCollapsedWidth)
            .fillMaxHeight()
            .background(colorScheme.surface)
    ) {
        if (showProfileSelector && profileName.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = NetflyHdrDimensions.railAvatarTopOffset),
                contentAlignment = Alignment.Center
            ) {
                ProfileAvatarCircle(
                    name = profileName,
                    colorHex = profileColorHex,
                    size = NetflyHdrDimensions.railAvatarSize,
                    avatarImageUrl = profileAvatarImageUrl,
                    imageCrossfade = false
                )
            }
        }
        Column(
            modifier = Modifier.align(Alignment.Center),
            verticalArrangement = Arrangement.spacedBy(NetflyHdrDimensions.railItemSpacing),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items.forEach { item ->
                val selected = selectedRoute == item.route
                Box(
                    modifier = Modifier
                        .size(NetflyHdrDimensions.railCollapsedWidth, NetflyHdrDimensions.railRowHeight)
                        .focusProperties { canFocus = false }
                        .semantics {
                            contentDescription = item.label
                            role = Role.Tab
                            this.selected = selected
                        },
                    contentAlignment = Alignment.Center
                ) {
                    NetflyHdrRailIcon(
                        iconRes = item.iconRes,
                        icon = item.icon,
                        tint = if (selected) NetflyHdrAccent else colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(NetflyHdrDimensions.railIconSize)
                    )
                }
            }
        }
    }
}

/**
 * Expanded floating panel: wordmark + profile row + labeled items.
 * Items are focusable only while the panel is expanded.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NetflyHdrExpandedRailPanel(
    items: List<DrawerItem>,
    selectedRoute: String?,
    expanded: Boolean,
    profileName: String,
    profileColorHex: String,
    profileAvatarImageUrl: String?,
    showProfileSelector: Boolean,
    drawerItemFocusRequesters: Map<String, FocusRequester>,
    profileFocusRequester: FocusRequester,
    onProfileClick: () -> Unit,
    onProfileFocused: () -> Unit,
    onItemClick: (DrawerItem) -> Unit,
    onItemFocused: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val panelShape = RoundedCornerShape(NetflyHdrDimensions.railPanelCornerRadius)
    Column(
        modifier = modifier
            .width(NetflyHdrDimensions.railPanelWidth)
            .height(NetflyHdrDimensions.railPanelHeight)
            .clip(panelShape)
            .background(colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Wordmark (text-based: no asset dependency; the theme builder owns
        // any future logo drawable).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(33.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = stringResource(R.string.app_name),
                color = NetflyHdrAccent,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (showProfileSelector && profileName.isNotEmpty()) {
            NetflyHdrProfileRow(
                name = profileName,
                colorHex = profileColorHex,
                avatarImageUrl = profileAvatarImageUrl,
                expanded = expanded,
                focusRequester = profileFocusRequester,
                onClick = onProfileClick,
                onFocused = onProfileFocused
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(NetflyHdrDimensions.railItemSpacing)
        ) {
            items.forEachIndexed { index, item ->
                NetflyHdrRailItemRow(
                    item = item,
                    selected = selectedRoute == item.route,
                    expanded = expanded,
                    focusRequester = drawerItemFocusRequesters[item.route],
                    onClick = { onItemClick(item) },
                    onFocused = { onItemFocused(index) }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NetflyHdrProfileRow(
    name: String,
    colorHex: String,
    avatarImageUrl: String?,
    expanded: Boolean,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
    onFocused: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var isFocused by remember { mutableStateOf(false) }
    val rowShape = RoundedCornerShape(NetflyHdrDimensions.posterCornerRadius)
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(NetflyHdrDimensions.railRowHeight + 8.dp)
            .focusRequester(focusRequester)
            .focusProperties { canFocus = expanded }
            .onFocusChanged { state ->
                isFocused = state.isFocused
                if (state.isFocused) onFocused()
            }
            .semantics {
                contentDescription = stringResource(R.string.netflyhdr_nav_switch_profile)
                role = Role.Button
            },
        colors = CardDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = colorScheme.surfaceVariant
        ),
        border = CardDefaults.border(
            border = Border.None,
            focusedBorder = Border(
                border = BorderStroke(NetflyHdrDimensions.focusRingWidth, NetflyHdrAccent),
                shape = rowShape
            )
        ),
        shape = CardDefaults.shape(shape = rowShape),
        scale = CardDefaults.scale(focusedScale = 1f, pressedScale = 1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileAvatarCircle(
                name = name,
                colorHex = colorHex,
                size = NetflyHdrDimensions.railAvatarSize,
                avatarImageUrl = avatarImageUrl,
                imageCrossfade = false
            )
            Text(
                text = name,
                color = if (isFocused) colorScheme.onSurface else colorScheme.onSurfaceVariant,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NetflyHdrRailItemRow(
    item: DrawerItem,
    selected: Boolean,
    expanded: Boolean,
    focusRequester: FocusRequester?,
    onClick: () -> Unit,
    onFocused: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var isFocused by remember { mutableStateOf(false) }
    val rowShape = RoundedCornerShape(NetflyHdrDimensions.posterCornerRadius)
    val contentColor = when {
        selected -> NetflyHdrAccent
        isFocused -> colorScheme.onSurface
        else -> colorScheme.onSurfaceVariant
    }
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(NetflyHdrDimensions.railRowHeight)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .focusProperties { canFocus = expanded }
            .onFocusChanged { state ->
                isFocused = state.isFocused
                if (state.isFocused) onFocused()
            }
            .semantics {
                contentDescription = item.label
                role = Role.Tab
                this.selected = selected
            },
        colors = CardDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = colorScheme.surfaceVariant
        ),
        border = CardDefaults.border(
            border = Border.None,
            focusedBorder = Border(
                border = BorderStroke(NetflyHdrDimensions.focusRingWidth, NetflyHdrAccent),
                shape = rowShape
            )
        ),
        shape = CardDefaults.shape(shape = rowShape),
        scale = CardDefaults.scale(focusedScale = 1f, pressedScale = 1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NetflyHdrRailIcon(
                iconRes = item.iconRes,
                icon = item.icon,
                tint = contentColor,
                modifier = Modifier.size(NetflyHdrDimensions.railIconSize)
            )
            Text(
                text = item.label,
                color = contentColor,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

/**
 * Floating section pill (64 px, top-left): icon + current section name,
 * shown for 3 s after navigating while the rail is collapsed.
 */
@Composable
fun NetflyHdrSectionPill(
    item: DrawerItem,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start, clip = true),
        exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start, clip = true),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .height(NetflyHdrDimensions.sectionPillHeight)
                .clip(RoundedCornerShape(50))
                .background(colorScheme.surfaceVariant)
                .padding(start = 6.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NetflyHdrRailIcon(
                iconRes = item.iconRes,
                icon = item.icon,
                tint = NetflyHdrAccent,
                modifier = Modifier.size(NetflyHdrDimensions.railIconSize)
            )
            Text(
                text = item.label,
                color = colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
