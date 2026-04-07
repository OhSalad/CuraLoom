package com.pmu.mobileapp.data.repository

import com.pmu.mobileapp.model.Episode
import com.pmu.mobileapp.model.Feed
import com.pmu.mobileapp.model.LibraryEpisode

interface PodcastRepository {
    fun getFeedById(feedId: Long): Feed?
    fun getEpisodesByFeedId(feedId: Long): List<Episode>
    fun getLibraryEpisodes(): List<LibraryEpisode>
    fun markEpisodePlayed(episodeId: Long)
    fun syncFeed(feedId: Long): FeedSyncResult
}

data class FeedSyncResult(
    val success: Boolean,
    val importedEpisodes: Int = 0,
    val errorMessage: String? = null
)
