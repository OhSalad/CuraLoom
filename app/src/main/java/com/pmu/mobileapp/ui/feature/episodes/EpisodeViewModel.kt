package com.pmu.mobileapp.ui.feature.episodes

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pmu.mobileapp.CuraLoomApp
import com.pmu.mobileapp.model.Episode
import com.pmu.mobileapp.model.Feed
import com.pmu.mobileapp.player.PlaybackState
import com.pmu.mobileapp.ui.common.KEY_FOLLOW_PREFIX
import com.pmu.mobileapp.ui.common.PREFS_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EpisodeUiState(
    val feedId: Long = -1L,
    val feed: Feed? = null,
    val filter: String = FILTER_ALL,
    val following: Boolean = true,
    val episodes: List<Episode> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class EpisodeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as CuraLoomApp
    private val repository = app.podcastRepository
    private val player = app.podcastPlayer
    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var uiState by mutableStateOf(EpisodeUiState())
        private set

    val playbackState: StateFlow<PlaybackState> = player.playbackState

    fun initialize(feedId: Long) {
        if (uiState.feedId == feedId && uiState.feed != null && uiState.episodes.isNotEmpty()) return
        val feed = repository.getFeedById(feedId)
        val following = prefs.getBoolean("$KEY_FOLLOW_PREFIX$feedId", true)
        uiState = uiState.copy(
            feedId = feedId,
            feed = feed,
            following = following,
            filter = FILTER_ALL,
            isLoading = feed != null,
            errorMessage = null
        )
        if (feed != null) {
            syncFeed()
        }
        reloadEpisodes()
    }

    fun setFilter(filter: String) {
        uiState = uiState.copy(filter = filter)
        reloadEpisodes()
    }

    fun toggleFollowing() {
        val feedId = uiState.feedId
        if (feedId < 0) return
        val following = !uiState.following
        prefs.edit().putBoolean("$KEY_FOLLOW_PREFIX$feedId", following).apply()
        uiState = uiState.copy(following = following)
    }

    fun playEpisode(episode: Episode) {
        if (playbackState.value.episodeId == episode.id) {
            player.togglePlayPause()
            return
        }

        val feedTitle = uiState.feed?.title.orEmpty()
        player.play(
            episodeId = episode.id,
            episodeTitle = episode.title,
            feedTitle = feedTitle,
            audioUrl = episode.audioUrl,
            startPositionMs = episode.lastPositionMs
        )
        repository.markEpisodePlayed(episode.id)
        reloadEpisodes()
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

    private fun reloadEpisodes() {
        val feedId = uiState.feedId
        if (feedId < 0) return
        val all = repository.getEpisodesByFeedId(feedId)
        val episodes = when (uiState.filter) {
            FILTER_UNPLAYED -> all.filter { !it.isPlayed }
            FILTER_POPULAR -> all.filter { it.isDownloaded || it.isNew }
            else -> all
        }
        uiState = uiState.copy(episodes = episodes)
    }

    private fun syncFeed() {
        val feedId = uiState.feedId
        if (feedId < 0) return
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            val result = withContext(Dispatchers.IO) { repository.syncFeed(feedId) }
            val refreshedFeed = repository.getFeedById(feedId)
            reloadEpisodes()
            uiState = uiState.copy(
                feed = refreshedFeed,
                isLoading = false,
                errorMessage = if (result.success) null else result.errorMessage
            )
        }
    }
}
