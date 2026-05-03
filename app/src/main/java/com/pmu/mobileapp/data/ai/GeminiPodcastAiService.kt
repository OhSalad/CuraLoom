package com.pmu.mobileapp.data.ai

import com.pmu.mobileapp.data.DefaultPodcastFeed
import com.pmu.mobileapp.data.DefaultPodcastFeeds

class GeminiPodcastAiService(
    private val jsonClient: AiJsonClient,
    private val feedCatalog: List<DefaultPodcastFeed> = DefaultPodcastFeeds.feeds,
    private val episodeCatalogProvider: AiEpisodeCatalogProvider
) : PodcastAiService {
    override suspend fun recommendFeeds(prompt: String, localeTag: String): AiRecommendationResult {
        val rawJson = jsonClient.generateJson(
            prompt = AiPromptBuilder.recommendationPrompt(
                prompt = prompt,
                localeTag = localeTag,
                feeds = feedCatalog
            ),
            responseSchema = AiResponseSchemas.recommendation
        )
        return AiResponseParser.parseRecommendations(rawJson, feedCatalog)
    }

    override suspend fun buildEpisodeList(
        prompt: String,
        selectedFeedUrls: List<String>,
        localeTag: String
    ): AiEpisodePlanResult {
        val selectedUrls = selectedFeedUrls.distinct()
        val candidates = episodeCatalogProvider.candidatesForFeedUrls(selectedUrls)
        if (candidates.isEmpty()) {
            throw PodcastAiException(
                kind = AiFailureKind.NoLocalEpisodes,
                message = "No synced local episodes are available for the selected feeds."
            )
        }

        val rawJson = jsonClient.generateJson(
            prompt = AiPromptBuilder.episodePlanPrompt(
                prompt = prompt,
                selectedFeedUrls = selectedUrls,
                localeTag = localeTag,
                candidates = candidates
            ),
            responseSchema = AiResponseSchemas.episodePlan
        )
        val parsed = AiResponseParser.parseEpisodePlan(rawJson, selectedUrls.toSet())
        return AiResponseParser.filterEpisodePlanToCandidates(parsed, candidates)
    }
}
