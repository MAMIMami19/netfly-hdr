package com.nuvio.tv.ui.netflyhdr.hub

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.R
import com.nuvio.tv.core.network.NetworkResult
import com.nuvio.tv.core.plugin.PluginManager
import com.nuvio.tv.data.mediaserver.ServerConnection
import com.nuvio.tv.data.mediaserver.ServerRepository
import com.nuvio.tv.data.repository.isTrackingAddon
import com.nuvio.tv.domain.model.Addon
import com.nuvio.tv.domain.model.PluginRepository
import com.nuvio.tv.domain.repository.AddonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** Live-manifest probe budget per directory entry. */
private const val PROBE_TIMEOUT_MS = 12_000L

/** Update-check budget per installed provider. */
private const val UPDATE_CHECK_TIMEOUT_MS = 15_000L

/** M3U download cap for channel counting (8 MB is far beyond any playlist). */
private const val M3U_MAX_BYTES = 8L * 1024 * 1024

/**
 * One directory entry with its live state: installed or not, manifest probe
 * result, and live facts (version, logo, scraper count) read from the
 * manifest — never from the directory JSON, so they never go stale.
 */
data class DirectoryEntryState(
    val entry: DirectoryEntry,
    val installed: Boolean = false,
    val installedVersion: String? = null,
    /** True once the background manifest probe has completed. */
    val validated: Boolean = false,
    /** True when the live manifest fetched and parsed. */
    val available: Boolean = false,
    val liveVersion: String? = null,
    val liveLogo: String? = null,
    /** Nuvio only: scraper count from the live manifest. */
    val liveScraperCount: Int? = null,
    /** Stremio only: true when the manifest declares a stream resource. */
    val liveIsStreaming: Boolean? = null
)

/** One available update found by the version check. */
data class ProviderUpdateInfo(
    val name: String,
    val kind: DirectoryKind,
    val installedVersion: String?,
    val remoteVersion: String?,
    /** Nuvio only: scraper-count change, e.g. "12 → 15". */
    val scraperCountChange: String? = null
)

data class NetflyHdrHubUiState(
    val directoryEntries: List<DirectoryEntryState> = emptyList(),
    val directoryFromRemote: Boolean = false,
    val directoryLoading: Boolean = true,
    val installedAddons: List<Addon> = emptyList(),
    val installedRepos: List<PluginRepository> = emptyList(),
    val iptvLists: List<NetflyHdrIptvStore.IptvList> = emptyList(),
    val servers: List<ServerConnection> = emptyList(),
    val installingManifestUrl: String? = null,
    val message: String? = null,
    val messageIsError: Boolean = false,
    val checkingUpdates: Boolean = false,
    val updateResults: List<ProviderUpdateInfo>? = null,
    val updateCheckFailed: Boolean = false
) {
    val installedStremioCount: Int get() = installedAddons.size
    val installedNuvioCount: Int get() = installedRepos.size
    val installedIptvCount: Int get() = iptvLists.size
}

/**
 * Universal Provider Hub state (§10).
 *
 * Wiring (all real, no fake data):
 * - Stremio addons: the base [AddonRepository] — install validates the
 *   manifest with [AddonRepository.fetchAddon] before [AddonRepository.addAddon].
 * - Nuvio plugins: the base [PluginManager] — its addRepository validates
 *   the manifest itself.
 * - Media servers: the base [ServerRepository] (Jellyfin/Emby).
 * - IPTV lists: [NetflyHdrIptvStore] — list management only; the base has
 *   no IPTV playback, so nothing here plays a channel.
 */
@HiltViewModel
class NetflyHdrHubViewModel @Inject constructor(
    private val addonRepository: AddonRepository,
    private val pluginManager: PluginManager,
    private val serverRepository: ServerRepository,
    private val iptvStore: NetflyHdrIptvStore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(NetflyHdrHubUiState())
    val uiState: StateFlow<NetflyHdrHubUiState> = _uiState.asStateFlow()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private var probeJob: Job? = null
    private var installedAddons: List<Addon> = emptyList()
    private var installedRepos: List<PluginRepository> = emptyList()

    init {
        serverRepository.ensureLoaded()
        observeInstalled()
        refreshDirectory()
    }

    private fun observeInstalled() {
        viewModelScope.launch {
            combine(
                addonRepository.getInstalledAddons(),
                pluginManager.repositories,
                iptvStore.lists,
                serverRepository.uiState
            ) { addons, repos, lists, servers ->
                Quad(addons, repos, lists, servers.connections)
            }.collect { (addons, repos, lists, servers) ->
                installedAddons = addons
                installedRepos = repos
                _uiState.update { state ->
                    state.copy(
                        installedAddons = addons,
                        installedRepos = repos,
                        iptvLists = lists,
                        servers = servers,
                        directoryEntries = state.directoryEntries.map { entryState ->
                            entryState.copy(
                                installed = isInstalled(entryState.entry, addons, repos),
                                installedVersion = installedVersionOf(entryState.entry, addons)
                            )
                        }
                    )
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Directory
    // ------------------------------------------------------------------

    fun refreshDirectory() {
        viewModelScope.launch {
            _uiState.update { it.copy(directoryLoading = true) }
            val result = NetflyHdrDirectory.load(context, preferRemote = true)
            val states = result.entries.map { entry ->
                DirectoryEntryState(
                    entry = entry,
                    installed = isInstalled(entry, installedAddons, installedRepos),
                    installedVersion = installedVersionOf(entry, installedAddons)
                )
            }
            _uiState.update {
                it.copy(
                    directoryEntries = states,
                    directoryFromRemote = result.fromRemote,
                    directoryLoading = false
                )
            }
            probeDirectory(states)
        }
    }

    /**
     * Background manifest validation for every directory entry (§10
     * AddonInstallTask equivalent): probes each live manifest so the grid
     * can show real versions, logos, scraper counts and Unavailable badges.
     * Bounded concurrency, short timeouts — a dead entry never blocks the
     * store.
     */
    private fun probeDirectory(states: List<DirectoryEntryState>) {
        probeJob?.cancel()
        probeJob = viewModelScope.launch {
            val probed = states.map { state ->
                async(Dispatchers.IO) {
                    probeEntry(state)
                }
            }.awaitAll()
            _uiState.update { current ->
                // Only apply if the directory hasn't been refreshed since.
                if (current.directoryEntries.map { it.entry.manifestUrl } ==
                    states.map { it.entry.manifestUrl }
                ) {
                    current.copy(directoryEntries = probed)
                } else {
                    current
                }
            }
        }
    }

    private suspend fun probeEntry(state: DirectoryEntryState): DirectoryEntryState {
        return when (state.entry.kind) {
            DirectoryKind.STREMIO -> {
                val result = withTimeoutOrNull(PROBE_TIMEOUT_MS) {
                    runCatching {
                        addonRepository.fetchAddon(stremioBaseUrl(state.entry.manifestUrl))
                    }.getOrNull()
                }
                val addon = (result as? NetworkResult.Success)?.data
                if (addon != null) {
                    state.copy(
                        validated = true,
                        available = true,
                        liveVersion = addon.version,
                        liveLogo = addon.logo,
                        liveIsStreaming = addon.resources.any {
                            it.name.equals("stream", ignoreCase = true)
                        }
                    )
                } else {
                    state.copy(validated = true, available = false)
                }
            }
            DirectoryKind.NUVIO -> {
                val manifest = withTimeoutOrNull(PROBE_TIMEOUT_MS) {
                    runCatching { fetchJson(state.entry.manifestUrl) }.getOrNull()
                }
                if (manifest != null && manifest.has("scrapers")) {
                    state.copy(
                        validated = true,
                        available = true,
                        liveVersion = manifest.optString("version").takeIf { it.isNotBlank() },
                        liveScraperCount = manifest.optJSONArray("scrapers")?.length()
                    )
                } else {
                    state.copy(validated = true, available = false)
                }
            }
        }
    }

    private fun fetchJson(url: String): JSONObject? {
        val request = Request.Builder().url(url).get().build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) return null
            return runCatching { JSONObject(body) }.getOrNull()
        }
    }

    // ------------------------------------------------------------------
    // Install
    // ------------------------------------------------------------------

    fun install(entry: DirectoryEntry) {
        if (_uiState.value.installingManifestUrl != null) return
        if (isInstalled(entry, installedAddons, installedRepos)) {
            showMessage(context.getString(R.string.netflyhdr_store_already_installed), isError = false)
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(installingManifestUrl = entry.manifestUrl) }
            when (entry.kind) {
                DirectoryKind.STREMIO -> installStremio(entry)
                DirectoryKind.NUVIO -> installNuvio(entry)
            }
            _uiState.update { it.copy(installingManifestUrl = null) }
        }
    }

    /** Manual manifest-URL install (the Add action card). */
    fun installFromUrl(rawUrl: String, kind: DirectoryKind) {
        val url = rawUrl.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            showMessage(context.getString(R.string.netflyhdr_store_add_url_invalid), isError = true)
            return
        }
        val entry = DirectoryEntry(
            kind = kind,
            name = url,
            author = null,
            manifestUrl = url,
            description = null,
            language = null,
            types = emptyList()
        )
        install(entry)
    }

    private suspend fun installStremio(entry: DirectoryEntry) {
        // Background manifest validation before adding (§10).
        val baseUrl = stremioBaseUrl(entry.manifestUrl)
        val result = withContext(Dispatchers.IO) {
            runCatching { addonRepository.fetchAddon(baseUrl) }.getOrNull()
        }
        val addon = (result as? NetworkResult.Success)?.data
        if (addon == null) {
            val detail = (result as? NetworkResult.Error)?.message
                ?: context.getString(R.string.netflyhdr_common_error)
            showMessage(
                context.getString(R.string.netflyhdr_store_install_failed, detail),
                isError = true
            )
            return
        }
        if (addon.isTrackingAddon) {
            // Tracking addons need an explicit consent flow that lives in
            // the Addon Manager — the store refuses rather than skipping it.
            showMessage(
                context.getString(R.string.netflyhdr_store_tracking_addon),
                isError = true
            )
            return
        }
        addonRepository.addAddon(baseUrl)
        val isStreaming = addon.resources.any { it.name.equals("stream", ignoreCase = true) }
        val kindMessage = if (isStreaming) {
            context.getString(R.string.netflyhdr_store_streaming_provider)
        } else {
            context.getString(R.string.netflyhdr_store_catalog_provider)
        }
        showMessage(
            context.getString(R.string.netflyhdr_store_installed_message, addon.displayName) +
                " " + kindMessage,
            isError = false
        )
    }

    private suspend fun installNuvio(entry: DirectoryEntry) {
        // PluginManager.addRepository validates the manifest itself.
        val result = withContext(Dispatchers.IO) {
            runCatching { pluginManager.addRepository(entry.manifestUrl) }
                .getOrElse { Result.failure(it) }
        }
        result.fold(
            onSuccess = { repo ->
                val detail = context.getString(
                    R.string.netflyhdr_store_scrapers,
                    repo.scraperCount
                )
                showMessage(
                    context.getString(
                        R.string.netflyhdr_store_installed_message,
                        repo.name
                    ) + " · $detail",
                    isError = false
                )
            },
            onFailure = { error ->
                showMessage(
                    context.getString(
                        R.string.netflyhdr_store_install_failed,
                        error.message ?: context.getString(R.string.netflyhdr_common_error)
                    ),
                    isError = true
                )
            }
        )
    }

    fun removeAddon(baseUrl: String) {
        viewModelScope.launch { addonRepository.removeAddon(baseUrl) }
    }

    fun setAddonEnabled(baseUrl: String, enabled: Boolean) {
        viewModelScope.launch { addonRepository.setAddonEnabled(baseUrl, enabled) }
    }

    fun removeRepo(repoId: String) {
        viewModelScope.launch { pluginManager.removeRepository(repoId) }
    }

    fun refreshRepo(repoId: String) {
        viewModelScope.launch {
            val result = pluginManager.refreshRepository(repoId)
            result.fold(
                onSuccess = {
                    showMessage(
                        context.getString(R.string.netflyhdr_hub_up_to_date),
                        isError = false
                    )
                },
                onFailure = { error ->
                    showMessage(
                        context.getString(
                            R.string.netflyhdr_store_install_failed,
                            error.message ?: context.getString(R.string.netflyhdr_common_error)
                        ),
                        isError = true
                    )
                }
            )
        }
    }

    // ------------------------------------------------------------------
    // Update checks (§10: check installed manifest versions)
    // ------------------------------------------------------------------

    fun checkForUpdates() {
        if (_uiState.value.checkingUpdates) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(checkingUpdates = true, updateResults = null, updateCheckFailed = false)
            }
            val addons = installedAddons.toList()
            val repos = installedRepos.toList()
            val stremioUpdates = withContext(Dispatchers.IO) {
                addons.map { addon ->
                    async {
                        val result = withTimeoutOrNull(UPDATE_CHECK_TIMEOUT_MS) {
                            runCatching { addonRepository.fetchAddon(addon.baseUrl) }.getOrNull()
                        }
                        val remote = (result as? NetworkResult.Success)?.data
                        if (remote != null && remote.version != addon.version) {
                            ProviderUpdateInfo(
                                name = addon.displayName,
                                kind = DirectoryKind.STREMIO,
                                installedVersion = addon.version,
                                remoteVersion = remote.version
                            )
                        } else {
                            null
                        }
                    }
                }.awaitAll().filterNotNull()
            }
            // Nuvio repos carry no version field — a refresh that changes
            // the scraper count is the observable update signal.
            val nuvioUpdates = withContext(Dispatchers.IO) {
                repos.mapNotNull { repo ->
                    val before = repo.scraperCount
                    val result = withTimeoutOrNull(UPDATE_CHECK_TIMEOUT_MS) {
                        runCatching { pluginManager.refreshRepository(repo.id) }.getOrNull()
                    } ?: return@mapNotNull null
                    if (result.isFailure) return@mapNotNull null
                    val after = runCatching { pluginManager.repositories.first() }
                        .getOrNull()
                        ?.firstOrNull { it.id == repo.id }
                        ?.scraperCount
                        ?: before
                    if (after != before) {
                        ProviderUpdateInfo(
                            name = repo.name,
                            kind = DirectoryKind.NUVIO,
                            installedVersion = null,
                            remoteVersion = null,
                            scraperCountChange = "$before → $after"
                        )
                    } else {
                        null
                    }
                }
            }
            val all = stremioUpdates + nuvioUpdates
            _uiState.update {
                it.copy(
                    checkingUpdates = false,
                    updateResults = all,
                    updateCheckFailed = false
                )
            }
            if (all.isEmpty()) {
                showMessage(context.getString(R.string.netflyhdr_hub_up_to_date), isError = false)
            } else {
                showMessage(
                    context.getString(R.string.netflyhdr_hub_updates_available, all.size),
                    isError = false
                )
            }
        }
    }

    // ------------------------------------------------------------------
    // IPTV lists
    // ------------------------------------------------------------------

    fun addIptvList(name: String, url: String) {
        viewModelScope.launch {
            val entry = iptvStore.add(name, url)
            countIptvChannels(entry.id, entry.url)
        }
    }

    fun removeIptvList(id: String) {
        viewModelScope.launch { iptvStore.remove(id) }
    }

    fun refreshIptvCounts() {
        viewModelScope.launch {
            _uiState.value.iptvLists.forEach { list ->
                countIptvChannels(list.id, list.url)
            }
        }
    }

    /** Counts #EXTINF entries in the playlist — the channel count badge. */
    private suspend fun countIptvChannels(id: String, url: String) {
        val count = withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder().url(url).get().build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@runCatching null
                    val source = response.body?.source() ?: return@runCatching null
                    var bytesRead = 0L
                    var channels = 0
                    while (!source.exhausted() && bytesRead < M3U_MAX_BYTES) {
                        val line = source.readUtf8Line() ?: break
                        bytesRead += line.length + 1
                        if (line.startsWith("#EXTINF")) channels++
                    }
                    channels
                }
            }.getOrNull()
        }
        iptvStore.setChannelCount(id, count)
    }

    // ------------------------------------------------------------------
    // Media servers
    // ------------------------------------------------------------------

    fun removeServer(connectionId: String) {
        serverRepository.remove(connectionId)
    }

    fun setServerEnabled(connectionId: String, enabled: Boolean) {
        serverRepository.setEnabled(connectionId, enabled)
    }

    fun refreshServerLibraries(connectionId: String) {
        viewModelScope.launch {
            runCatching { serverRepository.refreshLibraries(connectionId) }
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    fun clearMessage() {
        _uiState.update { it.copy(message = null, messageIsError = false) }
    }

    private fun showMessage(message: String, isError: Boolean) {
        _uiState.update { it.copy(message = message, messageIsError = isError) }
    }

    private fun isInstalled(
        entry: DirectoryEntry,
        addons: List<Addon>,
        repos: List<PluginRepository>
    ): Boolean = when (entry.kind) {
        DirectoryKind.STREMIO -> {
            val base = normalizeDirectoryUrl(stremioBaseUrl(entry.manifestUrl))
            addons.any { normalizeDirectoryUrl(it.baseUrl) == base }
        }
        DirectoryKind.NUVIO -> {
            val url = normalizeDirectoryUrl(nuvioManifestUrl(entry.manifestUrl))
            repos.any { normalizeDirectoryUrl(it.url) == url }
        }
    }

    private fun installedVersionOf(entry: DirectoryEntry, addons: List<Addon>): String? {
        if (entry.kind != DirectoryKind.STREMIO) return null
        val base = normalizeDirectoryUrl(stremioBaseUrl(entry.manifestUrl))
        return addons.firstOrNull { normalizeDirectoryUrl(it.baseUrl) == base }?.version
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
