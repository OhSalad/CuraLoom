package com.pmu.mobileapp.ui.feature.add

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.pmu.mobileapp.data.DatabaseHelper
import com.pmu.mobileapp.model.Feed
import com.pmu.mobileapp.ui.common.titleFromUrl
import java.net.URI

enum class AddFeedResult {
    EMPTY_URL,
    INVALID_URL,
    ADDED,
    EXISTS,
    VALIDATED
}

data class MultiAddResult(
    val added: Int = 0,
    val exists: Int = 0,
    val invalid: Int = 0
)

data class AddFeedUiState(
    val url: String = "",
    val validated: Boolean = false,
    val selectedCategory: String = "Technology",
    val categories: List<String> = listOf("Technology", "Design", "Philosophy", "Science", "Arts"),
    val showNewCategoryDialog: Boolean = false,
    val newCategory: String = ""
)

class AddFeedViewModel(application: Application) : AndroidViewModel(application) {
    private val feedService = DatabaseHelper.getInstance(application).feedService

    var uiState by mutableStateOf(AddFeedUiState())
        private set

    fun onUrlChange(value: String) {
        uiState = uiState.copy(url = value, validated = false)
    }

    fun onCategorySelected(value: String) {
        uiState = uiState.copy(selectedCategory = value)
    }

    fun openCategoryDialog() {
        uiState = uiState.copy(showNewCategoryDialog = true)
    }

    fun closeCategoryDialog() {
        uiState = uiState.copy(showNewCategoryDialog = false)
    }

    fun onNewCategoryChange(value: String) {
        uiState = uiState.copy(newCategory = value)
    }

    fun addCategory() {
        val name = uiState.newCategory.trim()
        if (name.isNotEmpty() && !uiState.categories.contains(name)) {
            uiState = uiState.copy(
                categories = uiState.categories + name,
                selectedCategory = name,
                newCategory = "",
                showNewCategoryDialog = false
            )
            return
        }
        uiState = uiState.copy(newCategory = "", showNewCategoryDialog = false)
    }

    fun validateFeed(): AddFeedResult {
        if (uiState.url.isBlank()) return AddFeedResult.EMPTY_URL
        if (!isUrlValid(uiState.url)) return AddFeedResult.INVALID_URL
        uiState = uiState.copy(validated = true)
        return AddFeedResult.VALIDATED
    }

    fun addFeed(): AddFeedResult {
        if (uiState.url.isBlank()) return AddFeedResult.EMPTY_URL
        if (!isUrlValid(uiState.url)) return AddFeedResult.INVALID_URL
        val feed = Feed().apply {
            title = titleFromUrl(uiState.url)
            url = uiState.url
            description = "RSS Feed added manually"
            category = uiState.selectedCategory
            author = authorFromUrl(uiState.url)
            isNew = true
        }
        return if (feedService.insertFeed(feed) != -1L) AddFeedResult.ADDED else AddFeedResult.EXISTS
    }

    fun addFeedsFromCommaSeparatedInput(): MultiAddResult {
        val entries = parseCommaSeparatedUrls(uiState.url)
        if (entries.isEmpty()) return MultiAddResult(invalid = 1)

        var added = 0
        var exists = 0
        var invalid = 0
        val seen = mutableSetOf<String>()

        for (rawUrl in entries) {
            val normalized = rawUrl.lowercase()
            if (!seen.add(normalized)) {
                continue
            }
            if (!isUrlValid(rawUrl)) {
                invalid++
                continue
            }
            val feed = Feed().apply {
                title = titleFromUrl(rawUrl)
                url = rawUrl
                description = "RSS Feed added manually"
                category = uiState.selectedCategory
                author = authorFromUrl(rawUrl)
                isNew = true
            }
            val inserted = feedService.insertFeed(feed) != -1L
            if (inserted) added++ else exists++
        }

        return MultiAddResult(added = added, exists = exists, invalid = invalid)
    }

    private fun parseCommaSeparatedUrls(input: String): List<String> {
        return input
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun isUrlValid(url: String): Boolean {
        return url.startsWith("http://") || url.startsWith("https://")
    }

    private fun authorFromUrl(url: String): String {
        return try {
            val host = URI(url).host?.removePrefix("www.")?.ifBlank { null }
            if (host == null) {
                "Podcast"
            } else {
                host.substringBefore(".")
                    .replace("-", " ")
                    .replace("_", " ")
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
        } catch (_: Exception) {
            "Podcast"
        }
    }
}
