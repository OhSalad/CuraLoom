package com.pmu.mobileapp.ui.feature.library

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.pmu.mobileapp.CuraLoomApp
import com.pmu.mobileapp.model.LibraryEpisode
import com.pmu.mobileapp.player.PlaybackState
import kotlinx.coroutines.flow.StateFlow

private const val LIBRARY_FILTER_ALL = "ALL"
private const val LIBRARY_FILTER_PLAYED = "PLAYED"
private const val LIBRARY_FILTER_UNPLAYED = "UNPLAYED"

data class LibraryUiState(
    val filter: String = LIBRARY_FILTER_ALL,
    val episodes: List<LibraryEpisode> = emptyList()
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as CuraLoomApp
    private val repository = app.podcastRepository
    private val player = app.podcastPlayer

    var uiState by mutableStateOf(LibraryUiState())
        private set

    val playbackState: StateFlow<PlaybackState> = player.playbackState

    init {
        reload()
    }

    fun setFilter(filter: String) {
        uiState = uiState.copy(filter = filter)
        reload()
    }

    fun playEpisode(item: LibraryEpisode) {
        if (item.audioUrl.isNullOrBlank()) return

        if (playbackState.value.episodeId == item.id) {
            player.togglePlayPause()
            return
        }

        player.play(
            episodeId = item.id,
            episodeTitle = item.title,
            feedTitle = item.feedTitle,
            audioUrl = item.audioUrl,
            startPositionMs = item.lastPositionMs
        )
        repository.markEpisodePlayed(item.id)
        reload()
    }

    fun togglePlayPause() {
        player.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    fun seekBack10() {
        player.seekBy(-10_000L)
    }

    fun seekForward10() {
        player.seekBy(10_000L)
    }

    fun hasEpisodes(): Boolean = repository.getLibraryEpisodes().isNotEmpty()

    private fun reload() {
        val all = repository.getLibraryEpisodes()
        val filtered = when (uiState.filter) {
            LIBRARY_FILTER_PLAYED -> all.filter { it.isPlayed }
            LIBRARY_FILTER_UNPLAYED -> all.filter { !it.isPlayed }
            else -> all
        }
        uiState = uiState.copy(episodes = filtered)
    }
}

internal object LibraryFilters {
    const val All = LIBRARY_FILTER_ALL
    const val Played = LIBRARY_FILTER_PLAYED
    const val Unplayed = LIBRARY_FILTER_UNPLAYED
}
