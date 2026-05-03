package com.pmu.mobileapp.data.ai

interface PodcastAiService {
    suspend fun recommendFeeds(prompt: String, localeTag: String): AiRecommendationResult

    suspend fun buildEpisodeList(
        prompt: String,
        selectedFeedUrls: List<String>,
        localeTag: String
    ): AiEpisodePlanResult
}
