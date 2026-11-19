package com.nuvio.tv.ui.netflyhdr.hub

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.data.mediaserver.ServerConnection
import com.nuvio.tv.data.mediaserver.ServerProvider
import com.nuvio.tv.domain.model.Addon
import com.nuvio.tv.domain.model.PluginRepository
import com.nuvio.tv.ui.components.EmptyScreenState
import com.nuvio.tv.ui.components.NuvioDialog
import com.nuvio.tv.ui.netflyhdr.browse.NetflyHdrListRow
import com.nuvio.tv.ui.netflyhdr.browse.NetflyHdrListScreen
import com.nuvio.tv.ui.screens.settings.MediaServersViewModel
import com.nuvio.tv.ui.screens.settings.ServerSignInDialog
import com.nuvio.tv.ui.screens.settings.ServerSignInRequest

/**
 * Universal Provider Hub (§10, route "netflyhdr_store"): the hub menu plus
 * its sub-screens — Stremio store, Nuvio store, installed providers, IPTV
 * lists and media servers. Sub-screens are internal destinations inside the
 * single store route; BACK returns to the hub menu.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NetflyHdrHubScreen(
    viewModel: NetflyHdrHubViewModel = hiltViewModel()
) {
    var destination by rememberSaveable { mutableStateOf(HubDestination.MENU) }

    BackHandler(enabled = destination != HubDestination.MENU) {
        destination = HubDestination.MENU
    }

    when (destination) {
        HubDestination.MENU -> HubMenuScreen(
            viewModel = viewModel,
            onNavigate = { destination = it }
        )
        HubDestination.STREMIO_STORE -> NetflyHdrStoreScreen(
            kind = DirectoryKind.STREMIO,
            viewModel = viewModel,
            onNavigateToInstalled = { destination = HubDestination.INSTALLED }
        )
        HubDestination.NUVIO_STORE -> NetflyHdrStoreScreen(
            kind = DirectoryKind.NUVIO,
            viewModel = viewModel,
            onNavigateToInstalled = { destination = HubDestination.INSTALLED }
        )
        HubDestination.INSTALLED -> InstalledProvidersScreen(viewModel = viewModel)
        HubDestination.IPTV -> IptvListsScreen(viewModel = viewModel)
        HubDestination.SERVERS -> ServersScreen(viewModel = viewModel)
    }
}

private enum class HubDestination {
    MENU,
    STREMIO_STORE,
    NUVIO_STORE,
    INSTALLED,
    IPTV,
    SERVERS
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HubMenuScreen(
    viewModel: NetflyHdrHubViewModel,
    onNavigate: (HubDestination) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    NetflyHdrListScreen(
        title = stringResource(R.string.netflyhdr_hub_title),
        subtitle = stringResource(R.string.netflyhdr_hub_subtitle),
        backdropUrl = null,
        detailLine = stringResource(R.string.netflyhdr_store_directory_source)
    ) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item(key = "menu_stremio") {
                NetflyHdrListRow(
                    title = stringResource(R.string.netflyhdr_hub_stremio_store),
                    subtitle = stringResource(R.string.netflyhdr_hub_stremio_store_sub),
                    onClick = { onNavigate(HubDestination.STREMIO_STORE) }
                )
            }
            item(key = "menu_nuvio") {
                NetflyHdrListRow(
                    title = stringResource(R.string.netflyhdr_hub_nuvio_store),
                    subtitle = stringResource(R.string.netflyhdr_hub_nuvio_store_sub),
                    onClick = { onNavigate(HubDestination.NUVIO_STORE) }
                )
            }
            item(key = "menu_iptv") {
                NetflyHdrListRow(
                    title = stringResource(R.string.netflyhdr_hub_iptv),
                    subtitle = stringResource(
                        R.string.netflyhdr_hub_iptv_sub,
                        uiState.installedIptvCount
                    ),
                    onClick = { onNavigate(HubDestination.IPTV) }
                )
            }
            item(key = "menu_servers") {
                NetflyHdrListRow(
                    title = stringResource(R.string.netflyhdr_hub_servers),
                    subtitle = stringResource(
                        R.string.netflyhdr_hub_servers_sub,
                        uiState.servers.size
                    ),
                    onClick = { onNavigate(HubDestination.SERVERS) }
                )
            }
            item(key = "menu_installed") {
                NetflyHdrListRow(
                    title = stringResource(R.string.netflyhdr_hub_installed),
                    subtitle = stringResource(
                        R.string.netflyhdr_hub_installed_sub,
                        uiState.installedStremioCount,
                        uiState.installedNuvioCount,
                        uiState.installedIptvCount
                    ),
                    onClick = { onNavigate(HubDestination.INSTALLED) }
                )
            }
            item(key = "menu_updates") {
                NetflyHdrListRow(
                    title = stringResource(R.string.netflyhdr_hub_updates),
                    subtitle = if (uiState.checkingUpdates) {
                        stringResource(R.string.netflyhdr_hub_checking)
                    } else {
                        stringResource(R.string.netflyhdr_hub_updates_sub)
                    },
                    onClick = { viewModel.checkForUpdates() }
                )
            }
            // Update results, shown inline after a check.
            val updates = uiState.updateResults
            if (updates != null && updates.isNotEmpty()) {
                item(key = "updates_header") {
                    Text(
                        text = stringResource(
                            R.string.netflyhdr_hub_updates_available,
                            updates.size
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                }
                items(updates, key = { "${it.kind}:${it.name}" }) { update ->
                    val detail = when {
                        update.scraperCountChange != null ->
                            update.scraperCountChange
                        update.installedVersion != null && update.remoteVersion != null ->
                            "v${update.installedVersion} → v${update.remoteVersion}"
                        else -> null
                    }
                    NetflyHdrListRow(
                        title = update.name,
                        subtitle = detail,
                        onClick = { }
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// Installed providers
// ------------------------------------------------------------------

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun InstalledProvidersScreen(
    viewModel: NetflyHdrHubViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedAddon by remember { mutableStateOf<Addon?>(null) }
    var selectedRepo by remember { mutableStateOf<PluginRepository?>(null) }
    var selectedList by remember { mutableStateOf<NetflyHdrIptvStore.IptvList?>(null) }

    val hasAnything = uiState.installedAddons.isNotEmpty() ||
        uiState.installedRepos.isNotEmpty() ||
        uiState.iptvLists.isNotEmpty()

    NetflyHdrListScreen(
        title = stringResource(R.string.netflyhdr_installed_title),
        subtitle = stringResource(
            R.string.netflyhdr_hub_installed_sub,
            uiState.installedStremioCount,
            uiState.installedNuvioCount,
            uiState.installedIptvCount
        ),
        backdropUrl = null
    ) {
        if (!hasAnything) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                EmptyScreenState(
                    title = stringResource(R.string.netflyhdr_installed_empty),
                    subtitle = stringResource(R.string.netflyhdr_hub_stremio_store_sub),
                    icon = Icons.Default.Storage
                )
            }
            return@NetflyHdrListScreen
        }
        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (uiState.installedAddons.isNotEmpty()) {
                item(key = "section_stremio") {
                    SectionHeader(text = stringResource(R.string.netflyhdr_installed_stremio))
                }
                items(uiState.installedAddons, key = { it.baseUrl }) { addon ->
                    val update = uiState.updateResults?.firstOrNull {
                        it.kind == DirectoryKind.STREMIO && it.name == addon.displayName
                    }
                    val subtitle = buildList {
                        add(
                            stringResource(
                                R.string.netflyhdr_installed_version,
                                addon.version
                            )
                        )
                        if (!addon.enabled) {
                            add(stringResource(R.string.netflyhdr_servers_disabled))
                        }
                        update?.remoteVersion?.let {
                            add(
                                stringResource(
                                    R.string.netflyhdr_installed_update_available,
                                    "v$it"
                                )
                            )
                        }
                    }.joinToString(" · ")
                    NetflyHdrListRow(
                        title = addon.displayName,
                        subtitle = subtitle,
                        onClick = { selectedAddon = addon }
                    )
                }
            }
            if (uiState.installedRepos.isNotEmpty()) {
                item(key = "section_nuvio") {
                    SectionHeader(text = stringResource(R.string.netflyhdr_installed_nuvio))
                }
                items(uiState.installedRepos, key = { it.id }) { repo ->
                    NetflyHdrListRow(
                        title = repo.name,
                        subtitle = stringResource(
                            R.string.netflyhdr_store_scrapers,
                            repo.scraperCount
                        ),
                        onClick = { selectedRepo = repo }
                    )
                }
            }
            if (uiState.iptvLists.isNotEmpty()) {
                item(key = "section_iptv") {
                    SectionHeader(text = stringResource(R.string.netflyhdr_installed_iptv))
                }
                items(uiState.iptvLists, key = { it.id }) { list ->
                    val subtitle = list.channelCount?.let {
                        stringResource(R.string.netflyhdr_iptv_channels, it)
                    } ?: stringResource(R.string.netflyhdr_iptv_counting)
                    NetflyHdrListRow(
                        title = list.name,
                        subtitle = subtitle,
                        onClick = { selectedList = list }
                    )
                }
            }
        }
    }

    selectedAddon?.let { addon ->
        NuvioDialog(
            onDismiss = { selectedAddon = null },
            title = addon.displayName,
            subtitle = addon.description
        ) {
            DialogActionRow {
                HubButton(
                    text = stringResource(
                        if (addon.enabled) R.string.netflyhdr_installed_disable
                        else R.string.netflyhdr_installed_enable
                    ),
                    onClick = {
                        viewModel.setAddonEnabled(addon.baseUrl, !addon.enabled)
                        selectedAddon = null
                    }
                )
                HubButton(
                    text = stringResource(R.string.netflyhdr_installed_remove),
                    isDestructive = true,
                    onClick = {
                        viewModel.removeAddon(addon.baseUrl)
                        selectedAddon = null
                    }
                )
            }
        }
    }

    selectedRepo?.let { repo ->
        NuvioDialog(
            onDismiss = { selectedRepo = null },
            title = repo.name,
            subtitle = stringResource(
                R.string.netflyhdr_store_scrapers,
                repo.scraperCount
            )
        ) {
            DialogActionRow {
                HubButton(
                    text = stringResource(R.string.netflyhdr_installed_refresh),
                    onClick = {
                        viewModel.refreshRepo(repo.id)
                        selectedRepo = null
                    }
                )
                HubButton(
                    text = stringResource(R.string.netflyhdr_installed_remove),
                    isDestructive = true,
                    onClick = {
                        viewModel.removeRepo(repo.id)
                        selectedRepo = null
                    }
                )
            }
        }
    }

    selectedList?.let { list ->
        NuvioDialog(
            onDismiss = { selectedList = null },
            title = list.name,
            subtitle = list.url
        ) {
            DialogActionRow {
                HubButton(
                    text = stringResource(R.string.netflyhdr_installed_remove),
                    isDestructive = true,
                    onClick = {
                        viewModel.removeIptvList(list.id)
                        selectedList = null
                    }
                )
            }
        }
    }
}

// ------------------------------------------------------------------
// IPTV lists
// ------------------------------------------------------------------

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun IptvListsScreen(
    viewModel: NetflyHdrHubViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var selectedList by remember { mutableStateOf<NetflyHdrIptvStore.IptvList?>(null) }

    NetflyHdrListScreen(
        title = stringResource(R.string.netflyhdr_iptv_title),
        subtitle = stringResource(R.string.netflyhdr_iptv_subtitle),
        backdropUrl = null,
        detailLine = stringResource(R.string.netflyhdr_iptv_no_playback),
        headerActions = {
            HubButton(
                text = stringResource(R.string.netflyhdr_iptv_add),
                onClick = { showAdd = true }
            )
        }
    ) {
        if (uiState.iptvLists.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                EmptyScreenState(
                    title = stringResource(R.string.netflyhdr_iptv_empty_title),
                    subtitle = stringResource(R.string.netflyhdr_iptv_empty_subtitle),
                    icon = Icons.Default.LiveTv
                )
            }
            return@NetflyHdrListScreen
        }
        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(uiState.iptvLists, key = { it.id }) { list ->
                val subtitle = list.channelCount?.let {
                    stringResource(R.string.netflyhdr_iptv_channels, it)
                } ?: stringResource(R.string.netflyhdr_iptv_counting)
                NetflyHdrListRow(
                    title = list.name,
                    subtitle = subtitle,
                    onClick = { selectedList = list }
                )
            }
        }
    }

    if (showAdd) {
        AddIptvListDialog(
            onAdd = { name, url ->
                viewModel.addIptvList(name, url)
                showAdd = false
            },
            onDismiss = { showAdd = false }
        )
    }

    selectedList?.let { list ->
        NuvioDialog(
            onDismiss = { selectedList = null },
            title = list.name,
            subtitle = list.url
        ) {
            DialogActionRow {
                HubButton(
                    text = stringResource(R.string.netflyhdr_installed_remove),
                    isDestructive = true,
                    onClick = {
                        viewModel.removeIptvList(list.id)
                        selectedList = null
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AddIptvListDialog(
    onAdd: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var showInvalid by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val nameFocusRequester = remember { FocusRequester() }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        runCatching { nameFocusRequester.requestFocus() }
    }

    NuvioDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.netflyhdr_iptv_add)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(nameFocusRequester),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                placeholder = {
                    androidx.compose.material3.Text(
                        text = stringResource(R.string.netflyhdr_iptv_name_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                colors = hubTextFieldColors()
            )
            OutlinedTextField(
                value = url,
                onValueChange = {
                    url = it
                    showInvalid = false
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                placeholder = {
                    androidx.compose.material3.Text(
                        text = stringResource(R.string.netflyhdr_iptv_url_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done,
                    autoCorrectEnabled = false
                ),
                keyboardActions = KeyboardActions(
                    onDone = { keyboardController?.hide() }
                ),
                colors = hubTextFieldColors()
            )
            if (showInvalid) {
                Text(
                    text = stringResource(R.string.netflyhdr_store_add_url_invalid),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            DialogActionRow {
                HubButton(
                    text = stringResource(R.string.netflyhdr_iptv_add),
                    isPrimary = true,
                    onClick = {
                        val trimmed = url.trim()
                        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                            onAdd(name, trimmed)
                        } else {
                            showInvalid = true
                        }
                    }
                )
                HubButton(
                    text = stringResource(R.string.netflyhdr_common_cancel),
                    onClick = onDismiss
                )
            }
        }
    }
}

// ------------------------------------------------------------------
// Media servers (Jellyfin / Emby — wired to the base ServerRepository)
// ------------------------------------------------------------------

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ServersScreen(
    viewModel: NetflyHdrHubViewModel,
    serversViewModel: MediaServersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val providers = serversViewModel.providers
    var showProviderPicker by remember { mutableStateOf(false) }
    var signInRequest by remember { mutableStateOf<ServerSignInRequest?>(null) }
    var selectedServer by remember { mutableStateOf<ServerConnection?>(null) }

    NetflyHdrListScreen(
        title = stringResource(R.string.netflyhdr_servers_title),
        subtitle = stringResource(R.string.netflyhdr_servers_subtitle),
        backdropUrl = null,
        headerActions = {
            HubButton(
                text = stringResource(R.string.netflyhdr_servers_add),
                onClick = { showProviderPicker = true }
            )
        }
    ) {
        if (uiState.servers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                EmptyScreenState(
                    title = stringResource(R.string.netflyhdr_servers_empty_title),
                    subtitle = stringResource(R.string.netflyhdr_servers_empty_subtitle),
                    icon = Icons.Default.Dns
                )
            }
            return@NetflyHdrListScreen
        }
        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(uiState.servers, key = { it.id }) { connection ->
                val subtitle = buildList {
                    add(connection.address)
                    add(
                        stringResource(
                            R.string.netflyhdr_servers_libraries,
                            connection.libraries.size
                        )
                    )
                    if (!connection.enabled) {
                        add(stringResource(R.string.netflyhdr_servers_disabled))
                    }
                }.joinToString(" · ")
                NetflyHdrListRow(
                    title = connection.name,
                    subtitle = subtitle,
                    onClick = { selectedServer = connection }
                )
            }
        }
    }

    if (showProviderPicker) {
        NuvioDialog(
            onDismiss = { showProviderPicker = false },
            title = stringResource(R.string.netflyhdr_servers_add)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                providers.forEach { provider ->
                    ProviderRowButton(
                        provider = provider,
                        onClick = {
                            showProviderPicker = false
                            signInRequest = ServerSignInRequest(provider = provider)
                        }
                    )
                }
            }
        }
    }

    signInRequest?.let { request ->
        ServerSignInDialog(
            request = request,
            viewModel = serversViewModel,
            onConnected = { signInRequest = null },
            onDismiss = { signInRequest = null }
        )
    }

    selectedServer?.let { connection ->
        NuvioDialog(
            onDismiss = { selectedServer = null },
            title = connection.name,
            subtitle = connection.address
        ) {
            DialogActionRow {
                HubButton(
                    text = stringResource(
                        if (connection.enabled) R.string.netflyhdr_installed_disable
                        else R.string.netflyhdr_installed_enable
                    ),
                    onClick = {
                        viewModel.setServerEnabled(connection.id, !connection.enabled)
                        selectedServer = null
                    }
                )
                HubButton(
                    text = stringResource(R.string.netflyhdr_servers_refresh),
                    onClick = {
                        viewModel.refreshServerLibraries(connection.id)
                        selectedServer = null
                    }
                )
                HubButton(
                    text = stringResource(R.string.netflyhdr_servers_remove),
                    isDestructive = true,
                    onClick = {
                        viewModel.removeServer(connection.id)
                        selectedServer = null
                    }
                )
            }
        }
    }
}

// ------------------------------------------------------------------
// Shared hub bits
// ------------------------------------------------------------------

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DialogActionRow(
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HubButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    isDestructive: Boolean = false
) {
    val colors = when {
        isDestructive -> ButtonDefaults.colors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
            focusedContainerColor = MaterialTheme.colorScheme.error,
            focusedContentColor = MaterialTheme.colorScheme.onError
        )
        isPrimary -> ButtonDefaults.colors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            focusedContainerColor = MaterialTheme.colorScheme.primary,
            focusedContentColor = MaterialTheme.colorScheme.onPrimary
        )
        else -> ButtonDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContentColor = MaterialTheme.colorScheme.onSurface
        )
    }
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = colors
    ) {
        Text(text = text, maxLines = 1)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProviderRowButton(
    provider: ServerProvider,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(text = provider.displayName)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun hubTextFieldColors(): androidx.compose.material3.TextFieldColors =
    TextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        focusedIndicatorColor = MaterialTheme.colorScheme.border,
        unfocusedIndicatorColor = MaterialTheme.colorScheme.border
    )
