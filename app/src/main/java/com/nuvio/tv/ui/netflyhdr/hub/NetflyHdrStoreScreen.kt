package com.nuvio.tv.ui.netflyhdr.hub

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.ui.components.EmptyScreenState
import com.nuvio.tv.ui.components.LoadingIndicator
import com.nuvio.tv.ui.components.NuvioDialog
import kotlinx.coroutines.delay

/**
 * The addon store grid (§10): 5 columns × 3 rows of cards, action cards
 * first (search, add manifest URL, refresh directory, installed providers),
 * then the community directory — alphabetically sorted, searchable by
 * name/developer/language/type, with an "N repositories · M installed"
 * counter.
 *
 * Install flow: card → detail (author · version · language · scrapers) →
 * install with background manifest validation → duplicate detection →
 * version recorded by the base repos → the user stays in the store with
 * badges updated, and the result message says whether the addon is a
 * streaming provider or a catalog/metadata-only provider.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NetflyHdrStoreScreen(
    kind: DirectoryKind,
    viewModel: NetflyHdrHubViewModel,
    onNavigateToInstalled: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    var searchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var detailEntry by remember { mutableStateOf<DirectoryEntryState?>(null) }
    var showAddUrl by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }

    // Transient install/result message auto-clears after a few seconds.
    val message = uiState.message
    LaunchedEffect(message) {
        if (message != null) {
            delay(6_000)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(searchActive) {
        if (searchActive) {
            runCatching { searchFocusRequester.requestFocus() }
        }
    }

    val entries = remember(uiState.directoryEntries, kind, searchQuery) {
        val query = searchQuery.trim().lowercase()
        uiState.directoryEntries
            .filter { it.entry.kind == kind }
            .filter { state ->
                if (query.isEmpty()) return@filter true
                val entry = state.entry
                entry.name.lowercase().contains(query) ||
                    entry.author?.lowercase()?.contains(query) == true ||
                    entry.language?.lowercase()?.contains(query) == true ||
                    entry.types.any { it.lowercase().contains(query) } ||
                    entry.description?.lowercase()?.contains(query) == true
            }
            .sortedBy { it.entry.name.lowercase() }
    }
    val installedCount = remember(uiState.directoryEntries, kind) {
        uiState.directoryEntries.count { it.entry.kind == kind && it.installed }
    }
    val totalCount = remember(uiState.directoryEntries, kind) {
        uiState.directoryEntries.count { it.entry.kind == kind }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 48.dp, vertical = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        if (kind == DirectoryKind.STREMIO) R.string.netflyhdr_store_title_stremio
                        else R.string.netflyhdr_store_title_nuvio
                    ),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(
                        R.string.netflyhdr_store_counter,
                        totalCount,
                        installedCount
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = { viewModel.refreshDirectory() },
                colors = ButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(text = stringResource(R.string.netflyhdr_store_refresh))
            }
        }

        if (!uiState.directoryFromRemote && !uiState.directoryLoading) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.netflyhdr_store_offline),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (message != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (uiState.messageIsError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (searchActive) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(searchFocusRequester),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                placeholder = {
                    androidx.compose.material3.Text(
                        text = stringResource(R.string.netflyhdr_store_search_hint),
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
                colors = TextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = MaterialTheme.colorScheme.border,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.border
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            uiState.directoryLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            }
            entries.isEmpty() -> {
                EmptyScreenState(
                    title = stringResource(R.string.netflyhdr_store_empty_title),
                    subtitle = stringResource(R.string.netflyhdr_store_empty_subtitle),
                    icon = Icons.Default.Store
                )
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(STORE_CARD_GAP),
                    verticalArrangement = Arrangement.spacedBy(STORE_CARD_GAP)
                ) {
                    // Action cards FIRST in the same grid (§10).
                    item(key = "__action_search__") {
                        NetflyHdrStoreActionCard(
                            action = StoreAction.SEARCH,
                            onClick = {
                                searchActive = !searchActive
                                if (!searchActive) searchQuery = ""
                            }
                        )
                    }
                    item(key = "__action_add__") {
                        NetflyHdrStoreActionCard(
                            action = StoreAction.ADD_URL,
                            onClick = { showAddUrl = true }
                        )
                    }
                    item(key = "__action_refresh__") {
                        NetflyHdrStoreActionCard(
                            action = StoreAction.REFRESH,
                            onClick = { viewModel.refreshDirectory() }
                        )
                    }
                    item(key = "__action_installed__") {
                        NetflyHdrStoreActionCard(
                            action = StoreAction.INSTALLED,
                            onClick = onNavigateToInstalled
                        )
                    }
                    items(
                        items = entries,
                        key = { it.entry.manifestUrl }
                    ) { state ->
                        val badge = when {
                            uiState.installingManifestUrl == state.entry.manifestUrl ->
                                StoreBadge.INSTALLING
                            state.installed -> StoreBadge.INSTALLED
                            state.validated && !state.available -> StoreBadge.UNAVAILABLE
                            !state.validated -> StoreBadge.CHECKING
                            else -> StoreBadge.INSTALL
                        }
                        NetflyHdrStoreCard(
                            state = state,
                            badge = badge,
                            onClick = { detailEntry = state }
                        )
                    }
                }
            }
        }
    }

    detailEntry?.let { state ->
        StoreDetailDialog(
            state = state,
            isInstalling = uiState.installingManifestUrl == state.entry.manifestUrl,
            onInstall = {
                viewModel.install(state.entry)
                detailEntry = null
            },
            onRemove = {
                when (state.entry.kind) {
                    DirectoryKind.STREMIO -> viewModel.removeAddon(
                        stremioBaseUrl(state.entry.manifestUrl)
                    )
                    DirectoryKind.NUVIO -> {
                        val repo = uiState.installedRepos.firstOrNull {
                            normalizeDirectoryUrl(it.url) ==
                                normalizeDirectoryUrl(nuvioManifestUrl(state.entry.manifestUrl))
                        }
                        repo?.let { viewModel.removeRepo(it.id) }
                    }
                }
                detailEntry = null
            },
            onDismiss = { detailEntry = null }
        )
    }

    if (showAddUrl) {
        AddManifestUrlDialog(
            kind = kind,
            onInstall = { url ->
                viewModel.installFromUrl(url, kind)
                showAddUrl = false
            },
            onDismiss = { showAddUrl = false }
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun StoreDetailDialog(
    state: DirectoryEntryState,
    isInstalling: Boolean,
    onInstall: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    val entry = state.entry
    val version = state.liveVersion ?: state.installedVersion
    val scrapersLabel = state.liveScraperCount?.let {
        stringResource(R.string.netflyhdr_store_scrapers, it)
    }
    val metaLine = remember(entry, version, scrapersLabel) {
        buildList {
            entry.author?.let { add(it) }
            version?.let { add("v$it") }
            entry.language?.let { add(it) }
            scrapersLabel?.let { add(it) }
            if (entry.types.isNotEmpty()) add(entry.types.joinToString("/"))
        }.joinToString(" · ")
    }

    NuvioDialog(
        onDismiss = onDismiss,
        title = entry.name,
        subtitle = metaLine.ifBlank { null }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!entry.description.isNullOrBlank()) {
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.installed) {
                    Button(
                        onClick = onRemove,
                        colors = ButtonDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                            focusedContainerColor = MaterialTheme.colorScheme.error,
                            focusedContentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text(text = stringResource(R.string.netflyhdr_store_detail_remove))
                    }
                } else {
                    Button(
                        onClick = onInstall,
                        enabled = !isInstalling && (!state.validated || state.available),
                        colors = ButtonDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            focusedContainerColor = MaterialTheme.colorScheme.primary,
                            focusedContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = stringResource(
                                if (isInstalling) R.string.netflyhdr_store_installing
                                else R.string.netflyhdr_store_detail_install
                            )
                        )
                    }
                }
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(text = stringResource(R.string.netflyhdr_store_detail_close))
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AddManifestUrlDialog(
    kind: DirectoryKind,
    onInstall: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var url by remember { mutableStateOf("") }
    var showInvalid by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val urlFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching { urlFocusRequester.requestFocus() }
    }

    NuvioDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.netflyhdr_store_add_url_title)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = if (kind == DirectoryKind.STREMIO) {
                    stringResource(R.string.netflyhdr_store_title_stremio)
                } else {
                    stringResource(R.string.netflyhdr_store_title_nuvio)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = url,
                onValueChange = {
                    url = it
                    showInvalid = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(urlFocusRequester),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                placeholder = {
                    androidx.compose.material3.Text(
                        text = stringResource(R.string.netflyhdr_store_add_url_hint),
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
                colors = TextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = MaterialTheme.colorScheme.border,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.border
                )
            )
            if (showInvalid) {
                Text(
                    text = stringResource(R.string.netflyhdr_store_add_url_invalid),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        val trimmed = url.trim()
                        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                            onInstall(trimmed)
                        } else {
                            showInvalid = true
                        }
                    },
                    colors = ButtonDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        focusedContainerColor = MaterialTheme.colorScheme.primary,
                        focusedContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(text = stringResource(R.string.netflyhdr_store_add_url_install))
                }
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(text = stringResource(R.string.netflyhdr_common_cancel))
                }
            }
        }
    }
}
