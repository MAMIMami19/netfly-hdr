package com.nuvio.tv.ui.netflyhdr.hub

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * The store directory (§10).
 *
 * The base has no community addon directory, so the store reads a directory
 * JSON: the remote copy lives in this repo
 * ([REMOTE_URL], `docs/addon-directory.json` on main — curated by the
 * project, so entries can be added or fixed without an app update) and the
 * bundled asset ([ASSET_PATH]) is the fallback when the remote copy is
 * unreachable. Both copies hold the same curated list of real, public
 * Stremio addons and Nuvio plugin repositories.
 *
 * Nothing in the directory is trusted blindly: every entry is validated
 * against its live manifest before install, and entries whose manifest
 * cannot be fetched show as Unavailable — never as a fake install.
 */
object NetflyHdrDirectory {
    const val REMOTE_URL =
        "https://raw.githubusercontent.com/MAMIMami19/netfly-hdr/main/docs/addon-directory.json"
    const val ASSET_PATH = "netflyhdr/addon-directory.json"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    data class LoadResult(
        val entries: List<DirectoryEntry>,
        /** True when the remote copy loaded; false means the bundled fallback. */
        val fromRemote: Boolean
    )

    /**
     * Loads the directory. Tries the remote copy first (unless
     * [preferRemote] is false), then falls back to the bundled asset.
     * Returns an empty list only when both sources fail.
     */
    suspend fun load(
        context: Context,
        preferRemote: Boolean = true
    ): LoadResult = withContext(Dispatchers.IO) {
        if (preferRemote) {
            val remote = runCatching { fetchRemote() }.getOrNull()
            if (remote != null) return@withContext LoadResult(remote, fromRemote = true)
        }
        val bundled = runCatching { loadBundled(context) }.getOrNull().orEmpty()
        LoadResult(bundled, fromRemote = false)
    }

    private fun fetchRemote(): List<DirectoryEntry>? {
        val request = Request.Builder().url(REMOTE_URL).get().build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) return null
            return parse(body)
        }
    }

    private fun loadBundled(context: Context): List<DirectoryEntry> {
        val json = context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
        return parse(json)
    }

    fun parse(json: String): List<DirectoryEntry> {
        val root = JSONObject(json)
        val array = root.optJSONArray("entries") ?: return emptyList()
        val entries = ArrayList<DirectoryEntry>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val kind = when (obj.optString("kind").lowercase()) {
                "stremio" -> DirectoryKind.STREMIO
                "nuvio" -> DirectoryKind.NUVIO
                else -> continue
            }
            val name = obj.optString("name").trim()
            val manifestUrl = obj.optString("manifestUrl").trim()
            if (name.isEmpty() || manifestUrl.isEmpty()) continue
            val types = ArrayList<String>()
            val typesArray = obj.optJSONArray("types")
            if (typesArray != null) {
                for (j in 0 until typesArray.length()) {
                    typesArray.optString(j).trim().takeIf { it.isNotEmpty() }?.let { types += it }
                }
            }
            entries += DirectoryEntry(
                kind = kind,
                name = name,
                author = obj.optString("author").trim().takeIf { it.isNotEmpty() },
                manifestUrl = manifestUrl,
                description = obj.optString("description").trim().takeIf { it.isNotEmpty() },
                language = obj.optString("language").trim().takeIf { it.isNotEmpty() },
                types = types,
                official = obj.optBoolean("official", false)
            )
        }
        return entries.sortedBy { it.name.lowercase() }
    }
}

enum class DirectoryKind {
    STREMIO,
    NUVIO
}

/**
 * One curated directory entry. [manifestUrl] is the full manifest URL for
 * both kinds (Stremio addons and Nuvio plugin repositories); the store
 * normalizes it per kind before install. No scraper counts or versions are
 * stored here — the store reads those from the live manifest so they never
 * go stale.
 */
data class DirectoryEntry(
    val kind: DirectoryKind,
    val name: String,
    val author: String?,
    val manifestUrl: String,
    val description: String?,
    val language: String?,
    val types: List<String>,
    val official: Boolean = false
)

/** Case-insensitive URL comparison (trailing slashes ignored). */
fun normalizeDirectoryUrl(url: String): String =
    url.trim().trimEnd('/').lowercase()

/** Stremio addons are installed by base URL — the manifest suffix is stripped. */
fun stremioBaseUrl(manifestUrl: String): String {
    var base = manifestUrl.trim().trimEnd('/')
    if (base.endsWith("/manifest.json", ignoreCase = true)) {
        base = base.dropLast("/manifest.json".length)
    }
    return base.trimEnd('/')
}

/**
 * Nuvio plugin repositories are stored by their canonical manifest URL —
 * a base URL gets `/manifest.json` appended (mirrors PluginManager).
 */
fun nuvioManifestUrl(url: String): String {
    val trimmed = url.trim().trimEnd('/')
    return if (trimmed.substringAfterLast("/").endsWith(".json", ignoreCase = true)) {
        trimmed
    } else {
        "$trimmed/manifest.json"
    }
}
