package com.nuvio.tv.ui.netflyhdr.hub

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

private val Context.netflyHdrIptvDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "netflyhdr_iptv"
)

/**
 * IPTV list registry (§10).
 *
 * The base has no IPTV feature at all — no player, no EPG, no channel list
 * UI — so this store only manages the lists themselves: name + M3U URL +
 * a channel count fetched by parsing the playlist. Lists are saved on this
 * device and shown with counts in the hub; playback is NOT wired in this
 * build and the UI says so plainly.
 */
class NetflyHdrIptvStore @Inject constructor(
    @ApplicationContext context: Context
) {

    data class IptvList(
        val id: String,
        val name: String,
        val url: String,
        /** Channel count from parsing the M3U; null when not counted yet. */
        val channelCount: Int?,
        val updatedAt: Long
    )

    private val dataStore = context.applicationContext.netflyHdrIptvDataStore

    val lists: Flow<List<IptvList>> = dataStore.data.map { prefs ->
        parse(prefs[LISTS_KEY])
    }

    suspend fun add(name: String, url: String): IptvList {
        val entry = IptvList(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifBlank { url.trim() },
            url = url.trim(),
            channelCount = null,
            updatedAt = System.currentTimeMillis()
        )
        dataStore.edit { prefs ->
            val current = parse(prefs[LISTS_KEY]).toMutableList()
            if (current.none { it.url.equals(entry.url, ignoreCase = true) }) {
                current += entry
                prefs[LISTS_KEY] = serialize(current)
            }
        }
        return entry
    }

    suspend fun remove(id: String) {
        dataStore.edit { prefs ->
            val current = parse(prefs[LISTS_KEY]).filterNot { it.id == id }
            prefs[LISTS_KEY] = serialize(current)
        }
    }

    suspend fun setChannelCount(id: String, count: Int?) {
        dataStore.edit { prefs ->
            val current = parse(prefs[LISTS_KEY]).map { entry ->
                if (entry.id == id) {
                    entry.copy(channelCount = count, updatedAt = System.currentTimeMillis())
                } else {
                    entry
                }
            }
            prefs[LISTS_KEY] = serialize(current)
        }
    }

    private fun parse(json: String?): List<IptvList> {
        if (json.isNullOrBlank()) return emptyList()
        val out = ArrayList<IptvList>()
        runCatching {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val id = obj.optString("id").takeIf { it.isNotBlank() } ?: continue
                val url = obj.optString("url").takeIf { it.isNotBlank() } ?: continue
                out += IptvList(
                    id = id,
                    name = obj.optString("name").takeIf { it.isNotBlank() } ?: url,
                    url = url,
                    channelCount = if (obj.has("channelCount") && !obj.isNull("channelCount")) {
                        obj.optInt("channelCount")
                    } else {
                        null
                    },
                    updatedAt = obj.optLong("updatedAt", 0L)
                )
            }
        }
        return out
    }

    private fun serialize(lists: List<IptvList>): String {
        val array = JSONArray()
        for (entry in lists) {
            array.put(
                JSONObject().apply {
                    put("id", entry.id)
                    put("name", entry.name)
                    put("url", entry.url)
                    if (entry.channelCount != null) put("channelCount", entry.channelCount)
                    put("updatedAt", entry.updatedAt)
                }
            )
        }
        return array.toString()
    }

    companion object {
        private val LISTS_KEY = stringPreferencesKey("iptv_lists_json")
    }
}
