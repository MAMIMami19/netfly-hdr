package com.nuvio.tv.ui.netflyhdr.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.domain.model.WatchProgress
import com.nuvio.tv.domain.repository.WatchProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Recently-watched history (§9). The list is the watch-progress repository's
 * full history, most recent first — the same data Continue Watching is built
 * from, without the in-progress filter.
 */
@HiltViewModel
class NetflyHdrHistoryViewModel @Inject constructor(
    private val watchProgressRepository: WatchProgressRepository
) : ViewModel() {

    val items: StateFlow<List<WatchProgress>> = watchProgressRepository.allProgress
        .map { list -> list.sortedByDescending { it.lastWatched } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun removeItem(progress: WatchProgress) {
        viewModelScope.launch {
            watchProgressRepository.removeFromHistory(
                contentId = progress.contentId,
                videoId = progress.videoId,
                season = progress.season,
                episode = progress.episode
            )
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            watchProgressRepository.clearAll()
        }
    }
}
