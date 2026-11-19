package com.nuvio.tv.ui.netflyhdr.nav

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Store
import androidx.navigation.NavHostController
import com.nuvio.tv.DrawerItem
import com.nuvio.tv.LocalContentFocusRequester
import com.nuvio.tv.LocalRootContentInset
import com.nuvio.tv.LocalSidebarExpanded
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.DiscoverLocation
import com.nuvio.tv.ui.navigation.NuvioNavHost
import com.nuvio.tv.ui.navigation.Screen
import com.nuvio.tv.ui.netflyhdr.NetflyHdrDimensions
import com.nuvio.tv.ui.netflyhdr.NetflyHdrMotion
import com.nuvio.tv.ui.netflyhdr.NetflyHdrRoutes
import com.nuvio.tv.ui.util.rememberDrawerItemFocusRequesters
import kotlinx.coroutines.delay

/** Focused-index value meaning "the profile row is focused". */
private const val PROFILE_FOCUSED_INDEX = -1

/**
 * Netfly HDR app scaffold (NOTE.md §6): the collapsible NavRail is the app's
 * primary navigation.
 *
 * - On root routes the collapsed icon strip is always visible (unless the
 *   "collapse sidebar" preference hides it entirely); the rail reappears on
 *   LEFT from content, which opens the expanded floating panel.
 * - BACK on a root route opens the panel; BACK while it is open exits the app
 *   (same contract as the base sidebars). Long-press BACK opens the panel
 *   directly.
 * - Navigating while collapsed shows the floating section pill for 3 s.
 * - Store / Episode Calendar / History entries are guarded: their routes do
 *   not exist in the nav graph until the owning builder's pages merge, so
 *   tapping one safely no-ops (the entry stays visible, nothing crashes).
 */
@Composable
fun NetflyHdrNavRailScaffold(
    longPressBackHeld: MutableState<Boolean>,
    navController: NavHostController,
    startDestination: String,
    currentRoute: String?,
    rootRoutes: Set<String>,
    discoverLocation: DiscoverLocation?,
    sidebarCollapsed: Boolean,
    hideBuiltInHeaders: Boolean,
    activeProfileName: String,
    activeProfileColorHex: String,
    activeProfileAvatarImageUrl: String?,
    showProfileSelector: Boolean,
    onSwitchProfile: () -> Unit,
    onNavigate: (String) -> Unit,
    onDrawerRouteClick: (String) -> Unit,
    onExitApp: () -> Unit
) {
    val strNavHome = stringResource(R.string.nav_home)
    val strNavSearch = stringResource(R.string.nav_search)
    val strNavDiscover = stringResource(R.string.nav_discover)
    val strNavCalendar = stringResource(R.string.netflyhdr_nav_calendar)
    val strNavMyList = stringResource(R.string.netflyhdr_nav_my_list)
    val strNavHistory = stringResource(R.string.netflyhdr_nav_history)
    val strNavStore = stringResource(R.string.netflyhdr_nav_store)
    val strNavSettings = stringResource(R.string.nav_settings)
    // Spec order (§6): Home, Search, Discover, Episode Calendar, My List,
    // History, Store, Settings. Discover stays out only when the user turned
    // it off entirely; otherwise the rail is its home.
    val railItems = remember(
        strNavHome,
        strNavSearch,
        strNavDiscover,
        strNavCalendar,
        strNavMyList,
        strNavHistory,
        strNavStore,
        strNavSettings,
        discoverLocation
    ) {
        buildList {
            add(DrawerItem(route = Screen.Home.route, label = strNavHome, icon = Icons.Default.Home))
            add(DrawerItem(route = Screen.Search.route, label = strNavSearch, iconRes = R.raw.sidebar_search))
            if (discoverLocation != DiscoverLocation.OFF) {
                add(DrawerItem(route = Screen.Discover.route, label = strNavDiscover, icon = Icons.Default.Explore))
            }
            add(DrawerItem(route = NetflyHdrRoutes.Calendar, label = strNavCalendar, icon = Icons.Default.DateRange))
            add(DrawerItem(route = Screen.Library.route, label = strNavMyList, iconRes = R.raw.sidebar_library))
            add(DrawerItem(route = NetflyHdrRoutes.History, label = strNavHistory, icon = Icons.Default.History))
            add(DrawerItem(route = NetflyHdrRoutes.Store, label = strNavStore, icon = Icons.Default.Store))
            add(DrawerItem(route = Screen.Settings.route, label = strNavSettings, iconRes = R.raw.sidebar_settings))
        }
    }

    val selectedDrawerRoute = railItems.firstOrNull { item ->
        currentRoute == item.route || currentRoute?.startsWith("${item.route}/") == true
    }?.route
    val selectedDrawerItem =
        railItems.firstOrNull { it.route == selectedDrawerRoute } ?: railItems.first()

    val showRail = currentRoute in rootRoutes
    val hasProfileItem = showProfileSelector && activeProfileName.isNotEmpty()

    val focusManager = LocalFocusManager.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val contentFocusRequester = remember { FocusRequester() }
    val profileFocusRequester = remember { FocusRequester() }
    val drawerItemFocusRequesters = rememberDrawerItemFocusRequesters(railItems)
    val keyboardController = LocalSoftwareKeyboardController.current

    var isRailExpanded by remember { mutableStateOf(false) }
    var pendingRailFocusRequest by remember { mutableStateOf(false) }
    var pendingContentFocusTransfer by remember { mutableStateOf(false) }
    var focusedRailIndex by remember { mutableIntStateOf(-2) }
    var pillVisible by remember { mutableStateOf(false) }

    LaunchedEffect(showRail) {
        if (!showRail) {
            isRailExpanded = false
            pendingRailFocusRequest = false
            pendingContentFocusTransfer = false
            pillVisible = false
        }
    }

    // Floating pill: visible for 3 s after navigating (or collapsing) while
    // the rail is collapsed; hidden while the panel is open.
    LaunchedEffect(selectedDrawerRoute, showRail, isRailExpanded) {
        if (!showRail || isRailExpanded) {
            pillVisible = false
            return@LaunchedEffect
        }
        pillVisible = true
        delay(NetflyHdrMotion.SECTION_PILL_VISIBLE_MS)
        pillVisible = false
    }

    BackHandler(enabled = currentRoute in rootRoutes && !isRailExpanded) {
        isRailExpanded = true
        pendingRailFocusRequest = true
    }

    BackHandler(enabled = currentRoute in rootRoutes && isRailExpanded) {
        if (longPressBackHeld.value) return@BackHandler
        onExitApp()
    }

    LaunchedEffect(isRailExpanded, pendingContentFocusTransfer, showRail) {
        if (!showRail || !pendingContentFocusTransfer || isRailExpanded) {
            return@LaunchedEffect
        }
        repeat(2) { withFrameNanos { } }
        runCatching { contentFocusRequester.requestFocus() }
        pendingContentFocusTransfer = false
    }

    LaunchedEffect(isRailExpanded, pendingRailFocusRequest, showRail, selectedDrawerRoute) {
        if (!showRail || !pendingRailFocusRequest || !isRailExpanded) {
            return@LaunchedEffect
        }
        val targetRoute = selectedDrawerRoute ?: railItems.firstOrNull()?.route ?: run {
            pendingRailFocusRequest = false
            return@LaunchedEffect
        }
        val requester = drawerItemFocusRequesters[targetRoute] ?: run {
            pendingRailFocusRequest = false
            return@LaunchedEffect
        }
        repeat(2) { withFrameNanos { } }
        runCatching { requester.requestFocus() }
        pendingRailFocusRequest = false
    }

    val expandProgress by animateFloatAsState(
        targetValue = if (isRailExpanded) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isRailExpanded) {
                NetflyHdrMotion.RAIL_PANEL_IN_MS
            } else {
                NetflyHdrMotion.RAIL_PANEL_OUT_MS
            }
        ),
        label = "netflyRailExpandProgress"
    )
    val showExpandedPanel by remember { derivedStateOf { expandProgress > 0.01f } }

    // Root destinations are inset by the collapsed strip so content never sits
    // underneath it; non-root routes (detail, player, …) get the full width.
    val rootContentInset =
        if (showRail && !sidebarCollapsed) NetflyHdrDimensions.railCollapsedWidth else 0.dp

    val onRailItemClick: (DrawerItem) -> Unit = { item ->
        // Route guard (route contract): Store / Calendar / History do not exist
        // in the nav graph until their pages merge — tapping one no-ops.
        val routeExists =
            runCatching { navController.graph.findNode(item.route) }.getOrNull() != null
        if (routeExists) {
            keyboardController?.hide()
            onNavigate(item.route)
            onDrawerRouteClick(item.route)
            pendingRailFocusRequest = false
            isRailExpanded = false
            pendingContentFocusTransfer = currentRoute == item.route
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { keyEvent ->
                // Consume all Back key events after long-press until released,
                // preventing the exit-app BackHandler from firing during the hold.
                if (longPressBackHeld.value && keyEvent.key == Key.Back) {
                    if (keyEvent.type == KeyEventType.KeyUp) longPressBackHeld.value = false
                    return@onPreviewKeyEvent true
                }
                false
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { keyEvent ->
                    // Long-press Back on a root route directly opens the rail,
                    // bypassing the "scroll row to start" BackHandler in home content.
                    if (keyEvent.key == Key.Back) {
                        if (
                            keyEvent.type == KeyEventType.KeyDown &&
                            showRail &&
                            !isRailExpanded &&
                            currentRoute in rootRoutes &&
                            keyEvent.nativeKeyEvent.isLongPress
                        ) {
                            if (!longPressBackHeld.value) {
                                longPressBackHeld.value = true
                                isRailExpanded = true
                                pendingRailFocusRequest = true
                            }
                            return@onPreviewKeyEvent true
                        }
                        if (longPressBackHeld.value) {
                            if (keyEvent.type == KeyEventType.KeyUp) longPressBackHeld.value = false
                            return@onPreviewKeyEvent true
                        }
                    }
                    if (
                        isRailExpanded &&
                        keyEvent.type == KeyEventType.KeyDown &&
                        isBlockedContentKey(keyEvent.key)
                    ) {
                        true
                    } else {
                        false
                    }
                }
                .onKeyEvent { keyEvent ->
                    // LEFT from content: move focus left if anything is there,
                    // otherwise open the rail.
                    if (showRail && !isRailExpanded && keyEvent.type == KeyEventType.KeyDown) {
                        val openKey = if (isRtl) Key.DirectionRight else Key.DirectionLeft
                        if (keyEvent.key == openKey) {
                            if (focusManager.moveFocus(if (isRtl) FocusDirection.Right else FocusDirection.Left)) {
                                true
                            } else {
                                isRailExpanded = true
                                pendingRailFocusRequest = true
                                true
                            }
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                }
        ) {
            CompositionLocalProvider(
                LocalSidebarExpanded provides isRailExpanded,
                LocalRootContentInset provides rootContentInset,
                LocalContentFocusRequester provides contentFocusRequester
            ) {
                NuvioNavHost(
                    navController = navController,
                    startDestination = startDestination,
                    hideBuiltInHeaders = hideBuiltInHeaders,
                    discoverIsRoot = Screen.Discover.route in rootRoutes
                )
            }
        }

        if (showRail) {
            if (!sidebarCollapsed) {
                NetflyHdrCollapsedRail(
                    items = railItems,
                    selectedRoute = selectedDrawerRoute,
                    profileName = activeProfileName,
                    profileColorHex = activeProfileColorHex,
                    profileAvatarImageUrl = activeProfileAvatarImageUrl,
                    showProfileSelector = showProfileSelector,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }

            if (showExpandedPanel) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 12.dp, top = 14.dp)
                        .graphicsLayer {
                            alpha = expandProgress
                            val scale = 0.92f + 0.08f * expandProgress
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(0f, 0f)
                        }
                        .selectableGroup()
                        .onPreviewKeyEvent { keyEvent ->
                            if (!isRailExpanded || keyEvent.type != KeyEventType.KeyDown) {
                                return@onPreviewKeyEvent false
                            }
                            when (keyEvent.key) {
                                Key.DirectionUp -> {
                                    val atTop = focusedRailIndex == PROFILE_FOCUSED_INDEX ||
                                        (focusedRailIndex == 0 && !hasProfileItem)
                                    if (atTop) {
                                        true
                                    } else {
                                        // Move focus within the rail; consume
                                        // unconditionally so focus never escapes
                                        // into the content behind.
                                        focusManager.moveFocus(FocusDirection.Up)
                                        true
                                    }
                                }

                                Key.DirectionDown -> {
                                    if (focusedRailIndex == railItems.lastIndex) {
                                        true
                                    } else {
                                        focusManager.moveFocus(FocusDirection.Down)
                                        true
                                    }
                                }

                                Key.DirectionRight, Key.DirectionLeft -> {
                                    val collapseKey =
                                        if (isRtl) Key.DirectionLeft else Key.DirectionRight
                                    if (keyEvent.key == collapseKey) {
                                        pendingContentFocusTransfer = false
                                        isRailExpanded = false
                                        true
                                    } else {
                                        false
                                    }
                                }

                                else -> false
                            }
                        }
                ) {
                    NetflyHdrExpandedRailPanel(
                        items = railItems,
                        selectedRoute = selectedDrawerRoute,
                        expanded = isRailExpanded,
                        profileName = activeProfileName,
                        profileColorHex = activeProfileColorHex,
                        profileAvatarImageUrl = activeProfileAvatarImageUrl,
                        showProfileSelector = showProfileSelector,
                        drawerItemFocusRequesters = drawerItemFocusRequesters,
                        profileFocusRequester = profileFocusRequester,
                        onProfileClick = {
                            onSwitchProfile()
                            isRailExpanded = false
                        },
                        onProfileFocused = { focusedRailIndex = PROFILE_FOCUSED_INDEX },
                        onItemClick = onRailItemClick,
                        onItemFocused = { focusedRailIndex = it }
                    )
                }
            }

            if (
                !sidebarCollapsed &&
                !isRailExpanded &&
                selectedDrawerRoute != Screen.Search.route
            ) {
                NetflyHdrSectionPill(
                    item = selectedDrawerItem,
                    visible = pillVisible,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = 12.dp, y = 14.dp)
                )
            }
        }
    }
}

private fun isBlockedContentKey(key: Key): Boolean {
    return key == Key.DirectionUp ||
        key == Key.DirectionDown ||
        key == Key.DirectionLeft ||
        key == Key.DirectionRight ||
        key == Key.DirectionCenter ||
        key == Key.Enter
}
