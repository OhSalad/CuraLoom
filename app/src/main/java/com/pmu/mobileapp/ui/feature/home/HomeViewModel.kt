package com.pmu.mobileapp.ui.feature.home

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pmu.mobileapp.CuraLoomApp
import com.pmu.mobileapp.data.DatabaseHelper
import com.pmu.mobileapp.model.Feed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeUiState(
    val selectedCategory: String = "All",
    val query: String = "",
    val feeds: List<Feed> = emptyList(),
    val categories: List<String> = emptyList(),
    val feedCount: Int = 0,
    val renameTarget: Feed? = null,
    val renameText: String = "",
    val menuFeedId: Long = -1L
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val feedService = DatabaseHelper.getInstance(application).feedService
    private val repository = (application as CuraLoomApp).podcastRepository

    var uiState by mutableStateOf(HomeUiState())
        private set

    init {
        reload()
    }

    fun onQueryChange(query: String) {
        uiState = uiState.copy(query = query)
        reload()
    }

    fun onCategorySelected(category: String) {
        uiState = uiState.copy(selectedCategory = category)
        reload()
    }

    fun toggleFeedMenu(feedId: Long) {
        uiState = uiState.copy(menuFeedId = if (uiState.menuFeedId == feedId) -1L else feedId)
    }

    fun beginRename(feed: Feed) {
        uiState = uiState.copy(renameTarget = feed, renameText = feed.title, menuFeedId = -1L)
    }

    fun onRenameTextChange(value: String) {
        uiState = uiState.copy(renameText = value)
    }

    fun cancelRename() {
        uiState = uiState.copy(renameTarget = null, renameText = "")
    }

    fun confirmRename() {
        val target = uiState.renameTarget ?: return
        val title = uiState.renameText.trim()
        if (title.isEmpty()) {
            cancelRename()
            return
        }
        feedService.updateFeed(target.id, title, target.category)
        uiState = uiState.copy(renameTarget = null, renameText = "")
        reload()
    }

    fun refreshFeed(feed: Feed) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.syncFeed(feed.id)
            }
            uiState = uiState.copy(menuFeedId = -1L)
            reload()
        }
    }

    fun reloadOnResume() {
        reload()
    }

    fun deleteFeed(feed: Feed) {
        feedService.deleteFeed(feed.id)
        uiState = uiState.copy(menuFeedId = -1L)
        reload()
    }

    private fun reload() {
        val categories = feedService.getAllCategories()
        val feeds = if (uiState.query.isBlank()) {
            feedService.getAllFeeds(if (uiState.selectedCategory == "All") null else uiState.selectedCategory)
        } else {
            feedService.searchFeeds(uiState.query)
        }
        uiState = uiState.copy(
            categories = categories,
            feeds = feeds,
            feedCount = feedService.getFeedCount()
        )
    }
}
