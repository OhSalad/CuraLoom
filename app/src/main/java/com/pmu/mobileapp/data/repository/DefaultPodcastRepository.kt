package com.pmu.mobileapp.data.repository

import com.pmu.mobileapp.data.DatabaseHelper
import com.pmu.mobileapp.data.rss.RssFeedImporter
import com.pmu.mobileapp.data.service.EpisodeDataService
import com.pmu.mobileapp.data.service.FeedDataService
import com.pmu.mobileapp.data.service.LibraryDataService
import com.pmu.mobileapp.model.Episode
import com.pmu.mobileapp.model.Feed
import com.pmu.mobileapp.model.LibraryEpisode

class DefaultPodcastRepository(
    private val dbHelper: DatabaseHelper,
    private val feedService: FeedDataService,
    private val episodeService: EpisodeDataService,
    private val libraryService: LibraryDataService
) : PodcastRepository {
    override fun getFeedById(feedId: Long): Feed? = feedService.getFeedById(feedId)

    override fun getEpisodesByFeedId(feedId: Long): List<Episode> = episodeService.getEpisodesByFeedId(feedId)

    override fun getEpisodeById(episodeId: Long): Episode? = episodeService.getEpisodeById(episodeId)

    override fun getLibraryEpisodes(): List<LibraryEpisode> = libraryService.getLibraryEpisodes()

    override fun markEpisodePlayed(episodeId: Long) {
        episodeService.markEpisodePlayed(episodeId)
    }

    override fun updateEpisodePlaybackPosition(episodeId: Long, positionMs: Long) {
        episodeService.updateEpisodePlaybackPosition(episodeId, positionMs)
    }

    override fun syncFeed(feedId: Long): FeedSyncResult {
        val feed = feedService.getFeedById(feedId) ?: return FeedSyncResult(
            success = false,
            errorMessage = "Feed not found"
        )
        return try {
            val imported = RssFeedImporter.import(feedId = feedId, feedUrl = feed.url)
            dbHelper.withTransaction { db ->
                feedService.updateFeedFromImport(
                    db = db,
                    id = feedId,
                    title = imported.title,
                    description = imported.description,
                    author = imported.author,
                    resolvedUrl = imported.resolvedUrl
                )
                episodeService.replaceEpisodesForFeed(db, feedId, imported.episodes)
            }
            FeedSyncResult(success = true, importedEpisodes = imported.episodes.size)
        } catch (e: Exception) {
            FeedSyncResult(success = false, errorMessage = e.message ?: "Unable to sync feed")
        }
    }
}
