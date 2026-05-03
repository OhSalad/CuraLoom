package com.pmu.mobileapp.data.ai

data class AiRecommendationResult(
    val summary: String,
    val recommendations: List<AiFeedRecommendation>
)

data class AiFeedRecommendation(
    val feedUrl: String,
    val reason: String,
    val rank: Int
)

data class AiEpisodePlanResult(
    val playlistTitle: String,
    val playlistReason: String,
    val episodes: List<AiPlannedEpisode>
)

data class AiPlannedEpisode(
    val feedUrl: String,
    val episodeTitle: String,
    val episodeReason: String,
    val rank: Int
)

data class AiEpisodePromptCandidate(
    val feedUrl: String,
    val feedTitle: String,
    val episodeTitle: String,
    val pubDate: String?,
    val duration: String?,
    val isPlayed: Boolean
)

enum class AiFailureKind {
    MissingApiKey,
    Network,
    RateLimited,
    MalformedResponse,
    NoLocalEpisodes
}

class PodcastAiException(
    val kind: AiFailureKind,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
