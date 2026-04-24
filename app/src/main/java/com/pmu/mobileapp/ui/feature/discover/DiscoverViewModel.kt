package com.pmu.mobileapp.ui.feature.discover

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pmu.mobileapp.CuraLoomApp
import com.pmu.mobileapp.data.DatabaseHelper
import com.pmu.mobileapp.data.DefaultPodcastFeed
import com.pmu.mobileapp.data.DefaultPodcastFeeds
import com.pmu.mobileapp.model.Feed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DiscoverUiState(
    val feeds: List<DefaultPodcastFeed> = DefaultPodcastFeeds.feeds,
    val followedUrls: Set<String> = emptySet(),
    val syncingUrls: Set<String> = emptySet(),
    val lastFollowedTitle: String? = null
)

class DiscoverViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as CuraLoomApp
    private val feedService = DatabaseHelper.getInstance(application).feedService
    private val repository = app.podcastRepository

    var uiState by mutableStateOf(DiscoverUiState())
        private set

    init {
        reloadFollowed()
    }

    fun reloadFollowed() {
        val urls = feedService.getAllFeeds(null).map { it.url }.toSet()
        uiState = uiState.copy(followedUrls = urls)
    }

    fun follow(feed: DefaultPodcastFeed) {
        if (uiState.syncingUrls.contains(feed.url)) return

        val existing = feedService.getFeedByUrl(feed.url)
        if (existing != null) {
            uiState = uiState.copy(
                followedUrls = uiState.followedUrls + feed.url,
                lastFollowedTitle = feed.title
            )
            return
        }

        uiState = uiState.copy(syncingUrls = uiState.syncingUrls + feed.url)
        viewModelScope.launch {
            val insertedFeedId = withContext(Dispatchers.IO) {
                feedService.insertFeed(
                    Feed().apply {
                        title = feed.title
                        url = feed.url
                        description = feed.description
                        category = feed.category
                        author = feed.author
                        isNew = true
                    }
                )
            }

            if (insertedFeedId > 0L) {
                withContext(Dispatchers.IO) {
                    repository.syncFeed(insertedFeedId)
                }
            }

            val followedUrls = withContext(Dispatchers.IO) {
                feedService.getAllFeeds(null).map { it.url }.toSet()
            }
            uiState = uiState.copy(
                followedUrls = followedUrls,
                syncingUrls = uiState.syncingUrls - feed.url,
                lastFollowedTitle = feed.title
            )
        }
    }
}
