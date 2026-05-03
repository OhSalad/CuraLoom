package com.pmu.mobileapp.data.ai

import com.pmu.mobileapp.data.repository.PodcastRepository
import com.pmu.mobileapp.data.service.FeedDataService
import com.pmu.mobileapp.model.Episode

interface AiEpisodeCatalogProvider {
    fun candidatesForFeedUrls(feedUrls: List<String>): List<AiEpisodePromptCandidate>
}

class DefaultAiEpisodeCatalogProvider(
    private val feedService: FeedDataService,
    private val repository: PodcastRepository
) : AiEpisodeCatalogProvider {
    override fun candidatesForFeedUrls(feedUrls: List<String>): List<AiEpisodePromptCandidate> =
        feedUrls.distinct()
            .flatMap { feedUrl ->
                val feed = feedService.getFeedByUrl(feedUrl)
                    ?: return@flatMap emptyList<AiEpisodePromptCandidate>()
                repository.getEpisodesByFeedId(feed.id)
                    .asSequence()
                    .filter { !it.audioUrl.isNullOrBlank() }
                    .sortedWith(episodeSort())
                    .take(PodcastAiConstants.EPISODE_CANDIDATES_PER_FEED)
                    .map { episode ->
                        AiEpisodePromptCandidate(
                            feedUrl = feed.url,
                            feedTitle = feed.title,
                            episodeTitle = episode.title,
                            pubDate = episode.pubDate,
                            duration = episode.duration,
                            isPlayed = episode.isPlayed
                        )
                    }
                    .toList()
            }
            .take(PodcastAiConstants.EPISODE_CANDIDATE_CAP)

    private fun episodeSort(): Comparator<Episode> =
        compareBy<Episode> { it.isPlayed }
            .thenByDescending { it.pubDate.orEmpty() }
            .thenByDescending { it.id }
}
