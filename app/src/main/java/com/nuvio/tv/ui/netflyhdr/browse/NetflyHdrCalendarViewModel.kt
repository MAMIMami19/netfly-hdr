package com.nuvio.tv.ui.netflyhdr.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.core.network.NetworkResult
import com.nuvio.tv.core.util.parseEpisodeReleaseInstant
import com.nuvio.tv.domain.repository.LibraryRepository
import com.nuvio.tv.domain.repository.MetaRepository
import com.nuvio.tv.domain.repository.WatchProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/** How far ahead the calendar looks for air dates (§9). */
private const val CALENDAR_WINDOW_DAYS = 30L

/** Bounds the parallel meta fetches — the calendar gathers from followed series only. */
private const val MAX_FOLLOWED_SERIES = 30

/** Per-show meta fetch budget, so one slow addon cannot stall the calendar. */
private const val META_FETCH_TIMEOUT_MS = 20_000L

/**
 * Episode calendar (§9): upcoming episodes gathered while loading series
 * data — the realistic version of a «coming soon» row, as its own nav page.
 *
 * Sources: series in Continue Watching + series in the library. For each
 * show the meta is fetched from the addons and episodes whose release date
 * falls inside the next [CALENDAR_WINDOW_DAYS] days are collected, grouped
 * by local day. Shows with no known future air dates simply contribute
 * nothing — the calendar never invents dates.
 */
@HiltViewModel
class NetflyHdrCalendarViewModel @Inject constructor(
    private val watchProgressRepository: WatchProgressRepository,
    private val libraryRepository: LibraryRepository,
    private val metaRepository: MetaRepository
) : ViewModel() {

    data class EpisodeEntry(
        val showId: String,
        val showName: String,
        val poster: String?,
        val backdrop: String?,
        val addonBaseUrl: String?,
        val season: Int,
        val episode: Int,
        val episodeTitle: String?,
        val airDate: LocalDate
    )

    data class DayGroup(
        val date: LocalDate,
        val entries: List<EpisodeEntry>
    )

    data class UiState(
        val loading: Boolean = true,
        val days: List<DayGroup> = emptyList(),
        val error: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = false) }
            try {
                val days = loadUpcoming()
                _uiState.update { it.copy(loading = false, days = days, error = false) }
            } catch (_: Exception) {
                _uiState.update { it.copy(loading = false, error = true) }
            }
        }
    }

    private suspend fun loadUpcoming(): List<DayGroup> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val windowEnd = today.plusDays(CALENDAR_WINDOW_DAYS)

        // Followed series: Continue Watching first, then the library.
        val followed = LinkedHashMap<String, FollowedSeries>()
        runCatching { watchProgressRepository.continueWatching.first() }
            .getOrNull()
            .orEmpty()
            .filter { it.contentType.equals("series", ignoreCase = true) }
            .forEach { progress ->
                followed.putIfAbsent(
                    progress.contentId,
                    FollowedSeries(
                        id = progress.contentId,
                        name = progress.name,
                        poster = progress.poster,
                        backdrop = progress.backdrop,
                        addonBaseUrl = progress.addonBaseUrl
                    )
                )
            }
        runCatching { libraryRepository.libraryItems.first() }
            .getOrNull()
            .orEmpty()
            .filter { it.type.equals("series", ignoreCase = true) }
            .forEach { entry ->
                followed.putIfAbsent(
                    entry.id,
                    FollowedSeries(
                        id = entry.id,
                        name = entry.name,
                        poster = entry.poster,
                        backdrop = entry.background,
                        addonBaseUrl = entry.addonBaseUrl
                    )
                )
            }

        val shows = followed.values.take(MAX_FOLLOWED_SERIES)
        if (shows.isEmpty()) return emptyList()

        val entries = kotlinx.coroutines.coroutineScope {
            shows.map { show ->
                async(Dispatchers.IO) {
                    upcomingEpisodesFor(show, today, windowEnd, zone)
                }
            }.awaitAll().flatten()
        }

        return entries
            .sortedWith(compareBy({ it.airDate }, { it.showName }, { it.season }, { it.episode }))
            .groupBy { it.airDate }
            .map { (date, dayEntries) -> DayGroup(date, dayEntries) }
            .sortedBy { it.date }
    }

    private suspend fun upcomingEpisodesFor(
        show: FollowedSeries,
        today: LocalDate,
        windowEnd: LocalDate,
        zone: ZoneId
    ): List<EpisodeEntry> {
        val result = withTimeoutOrNull(META_FETCH_TIMEOUT_MS) {
            runCatching {
                metaRepository.getMetaFromAllAddons(
                    type = "series",
                    id = show.id,
                    sourceAddonBaseUrl = show.addonBaseUrl
                ).first { it !is NetworkResult.Loading }
            }.getOrNull()
        } ?: return emptyList()
        val meta = (result as? NetworkResult.Success)?.data ?: return emptyList()
        return meta.videos.mapNotNull { video ->
            val season = video.season ?: return@mapNotNull null
            val episode = video.episode ?: return@mapNotNull null
            if (season <= 0) return@mapNotNull null
            val airDate = parseEpisodeReleaseInstant(video.released, zone)
                ?.atZone(zone)
                ?.toLocalDate()
                ?: return@mapNotNull null
            if (airDate.isBefore(today) || airDate.isAfter(windowEnd)) return@mapNotNull null
            EpisodeEntry(
                showId = show.id,
                showName = meta.name.ifBlank { show.name },
                poster = meta.poster ?: show.poster,
                backdrop = meta.background ?: show.backdrop,
                addonBaseUrl = show.addonBaseUrl,
                season = season,
                episode = episode,
                episodeTitle = video.title.takeIf { it.isNotBlank() },
                airDate = airDate
            )
        }
    }

    private data class FollowedSeries(
        val id: String,
        val name: String,
        val poster: String?,
        val backdrop: String?,
        val addonBaseUrl: String?
    )
}
