package com.pmu.mobileapp.data.repository

import com.pmu.mobileapp.data.DatabaseHelper
import com.pmu.mobileapp.data.rss.RssFeedImporter
import com.pmu.mobileapp.model.Episode
import com.pmu.mobileapp.model.Feed
import com.pmu.mobileapp.model.LibraryEpisode

class DefaultPodcastRepository(
    private val db: DatabaseHelper
) : PodcastRepository {
    override fun getFeedById(feedId: Long): Feed? = db.getFeedById(feedId)

    override fun getEpisodesByFeedId(feedId: Long): List<Episode> = db.getEpisodesByFeedId(feedId)

    override fun getLibraryEpisodes(): List<LibraryEpisode> = db.getLibraryEpisodes()

    override fun markEpisodePlayed(episodeId: Long) {
        db.markEpisodePlayed(episodeId)
    }

    override fun syncFeed(feedId: Long): FeedSyncResult {
        val feed = db.getFeedById(feedId) ?: return FeedSyncResult(
            success = false,
            errorMessage = "Feed not found"
        )
        return try {
            val imported = RssFeedImporter.import(feedId = feedId, feedUrl = feed.url)
            db.updateFeedFromImport(
                id = feedId,
                title = imported.title,
                description = imported.description,
                author = imported.author,
                resolvedUrl = imported.resolvedUrl
            )
            db.replaceEpisodesForFeed(feedId, imported.episodes)
            FeedSyncResult(success = true, importedEpisodes = imported.episodes.size)
        } catch (e: Exception) {
            FeedSyncResult(success = false, errorMessage = e.message ?: "Unable to sync feed")
        }
    }
}
