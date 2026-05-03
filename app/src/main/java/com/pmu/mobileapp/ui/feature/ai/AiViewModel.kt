package com.pmu.mobileapp.ui.feature.ai

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pmu.mobileapp.data.DefaultPodcastFeed
import com.pmu.mobileapp.data.DefaultPodcastFeeds
import com.pmu.mobileapp.data.ai.AiEpisodePlanResult
import com.pmu.mobileapp.data.ai.AiFailureKind
import com.pmu.mobileapp.data.ai.AiFeedRecommendation
import com.pmu.mobileapp.data.ai.PodcastAiException
import com.pmu.mobileapp.data.ai.PodcastAiService
import com.pmu.mobileapp.data.repository.FeedSyncResult
import com.pmu.mobileapp.data.repository.PodcastRepository
import com.pmu.mobileapp.data.service.FeedDataService
import com.pmu.mobileapp.model.Episode
import com.pmu.mobileapp.model.Feed
import com.pmu.mobileapp.player.PodcastPlayer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AiUiState(
    val isAiConfigured: Boolean = true,
    val prompt: String = "",
    val submittedPrompt: String = "",
    val isLoadingRecommendations: Boolean = false,
    val isBuildingEpisodeList: Boolean = false,
    val error: AiUiError? = null,
    val summary: String = "",
    val recommendations: List<AiRecommendedFeed> = emptyList(),
    val selectedFeedUrls: Set<String> = emptySet(),
    val syncingFeedUrls: Set<String> = emptySet(),
    val playlistTitle: String = "",
    val playlistReason: String = "",
    val plannedEpisodes: List<AiPlannedEpisodeUi> = emptyList()
)

data class AiRecommendedFeed(
    val feed: DefaultPodcastFeed,
    val reason: String,
    val rank: Int,
    val isFollowed: Boolean,
    val isSyncing: Boolean,
    val isSelected: Boolean,
    val localFeedId: Long?
)

data class AiPlannedEpisodeUi(
    val feedUrl: String,
    val feedId: Long,
    val feedTitle: String,
    val episode: Episode,
    val episodeReason: String,
    val rank: Int
)

data class AiUiError(
    val type: AiUiErrorType,
    val details: String? = null
)

enum class AiUiErrorType {
    EmptyPrompt,
    MissingApiKey,
    Network,
    RateLimited,
    MalformedResponse,
    NoSelectedFeeds,
    NoLocalEpisodes,
    Unknown
}

interface AiLibraryGateway {
    fun followedFeedUrls(): Set<String>
    fun getFeedByUrl(feedUrl: String): Feed?
    fun followDefaultFeed(feed: DefaultPodcastFeed): Long
    fun syncFeed(feedId: Long): FeedSyncResult
    fun episodesForFeed(feedId: Long): List<Episode>
    fun playEpisode(feedTitle: String, episode: Episode)
    fun markEpisodePlayed(episodeId: Long)
}

class DefaultAiLibraryGateway(
    private val feedService: FeedDataService,
    private val repository: PodcastRepository,
    private val player: PodcastPlayer
) : AiLibraryGateway {
    override fun followedFeedUrls(): Set<String> =
        feedService.getAllFeeds(null).map { it.url }.toSet()

    override fun getFeedByUrl(feedUrl: String): Feed? =
        feedService.getFeedByUrl(feedUrl)

    override fun followDefaultFeed(feed: DefaultPodcastFeed): Long {
        val existing = feedService.getFeedByUrl(feed.url)
        if (existing != null) return existing.id

        return feedService.insertFeed(
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

    override fun syncFeed(feedId: Long): FeedSyncResult =
        repository.syncFeed(feedId)

    override fun episodesForFeed(feedId: Long): List<Episode> =
        repository.getEpisodesByFeedId(feedId)

    override fun playEpisode(feedTitle: String, episode: Episode) {
        if (episode.audioUrl.isNullOrBlank()) return
        player.play(
            episodeId = episode.id,
            episodeTitle = episode.title,
            feedTitle = feedTitle,
            audioUrl = episode.audioUrl.orEmpty(),
            startPositionMs = episode.lastPositionMs
        )
    }

    override fun markEpisodePlayed(episodeId: Long) {
        repository.markEpisodePlayed(episodeId)
    }
}

class AiViewModel(
    private val aiService: PodcastAiService,
    private val libraryGateway: AiLibraryGateway,
    private val isAiConfigured: Boolean,
    private val localeTagProvider: () -> String,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val externalScope: CoroutineScope? = null
) : ViewModel() {
    private val feedCatalogByUrl = DefaultPodcastFeeds.feeds.associateBy { it.url }

    var uiState by mutableStateOf(
        AiUiState(
        isAiConfigured = isAiConfigured,
        error = if (isAiConfigured) null else AiUiError(AiUiErrorType.MissingApiKey)
        )
    )
        private set

    fun onPromptChange(prompt: String) {
        uiState = uiState.copy(prompt = prompt, error = null)
    }

    fun recommendFeeds(): Job? {
        if (!ensureConfigured()) return null
        val prompt = uiState.prompt.trim()
        if (prompt.isBlank()) {
            uiState = uiState.copy(error = AiUiError(AiUiErrorType.EmptyPrompt))
            return null
        }

        uiState = uiState.copy(
            submittedPrompt = prompt,
            isLoadingRecommendations = true,
            error = null,
            summary = "",
            recommendations = emptyList(),
            selectedFeedUrls = emptySet(),
            playlistTitle = "",
            playlistReason = "",
            plannedEpisodes = emptyList()
        )

        return launchWork {
            try {
                val result = aiService.recommendFeeds(prompt, localeTagProvider())
                val followed = withContext(ioDispatcher) { libraryGateway.followedFeedUrls() }
                uiState = uiState.copy(
                    isLoadingRecommendations = false,
                    summary = result.summary,
                    recommendations = result.recommendations.toUiRecommendations(
                        followedUrls = followed,
                        selectedFeedUrls = emptySet(),
                        syncingFeedUrls = uiState.syncingFeedUrls
                    )
                )
            } catch (e: Throwable) {
                uiState = uiState.copy(
                    isLoadingRecommendations = false,
                    error = e.toUiError()
                )
            }
        }
    }

    fun toggleFeedSelection(feedUrl: String) {
        val selected = if (feedUrl in uiState.selectedFeedUrls) {
            uiState.selectedFeedUrls - feedUrl
        } else {
            uiState.selectedFeedUrls + feedUrl
        }
        uiState = uiState.copy(
            selectedFeedUrls = selected,
            recommendations = uiState.recommendations.map {
                it.copy(isSelected = it.feed.url in selected)
            },
            error = null
        )
    }

    fun followFeed(feedUrl: String): Job? {
        if (feedUrl in uiState.syncingFeedUrls) return null
        val defaultFeed = feedCatalogByUrl[feedUrl] ?: return null
        uiState = updateRecommendationFlags(
            syncingFeedUrls = uiState.syncingFeedUrls + feedUrl,
            clearError = true
        )

        return launchWork {
            try {
                val followed = withContext(ioDispatcher) {
                    val feedId = libraryGateway.followDefaultFeed(defaultFeed)
                    if (feedId > 0L) {
                        libraryGateway.syncFeed(feedId)
                    }
                    libraryGateway.followedFeedUrls()
                }
                uiState = uiState.copy(syncingFeedUrls = uiState.syncingFeedUrls - feedUrl)
                uiState = updateRecommendationFlags(followedUrls = followed)
            } catch (e: Throwable) {
                uiState = uiState.copy(
                    syncingFeedUrls = uiState.syncingFeedUrls - feedUrl,
                    error = e.toUiError()
                )
                uiState = updateRecommendationFlags()
            }
        }
    }

    fun buildEpisodeList(): Job? {
        if (!ensureConfigured()) return null
        val selectedFeedUrls = uiState.selectedFeedUrls.toList()
        if (selectedFeedUrls.isEmpty()) {
            uiState = uiState.copy(error = AiUiError(AiUiErrorType.NoSelectedFeeds))
            return null
        }
        val prompt = uiState.prompt.trim()
        if (prompt.isBlank()) {
            uiState = uiState.copy(error = AiUiError(AiUiErrorType.EmptyPrompt))
            return null
        }

        uiState = uiState.copy(
            isBuildingEpisodeList = true,
            error = null,
            playlistTitle = "",
            playlistReason = "",
            plannedEpisodes = emptyList()
        )

        return launchWork {
            try {
                withContext(ioDispatcher) {
                    ensureSelectedFeedsSynced(selectedFeedUrls)
                }
                val result = aiService.buildEpisodeList(
                    prompt = prompt,
                    selectedFeedUrls = selectedFeedUrls,
                    localeTag = localeTagProvider()
                )
                val plannedEpisodes = withContext(ioDispatcher) {
                    result.toPlannedEpisodeUi(selectedFeedUrls)
                }
                uiState = uiState.copy(
                    isBuildingEpisodeList = false,
                    playlistTitle = result.playlistTitle,
                    playlistReason = result.playlistReason,
                    plannedEpisodes = plannedEpisodes,
                    error = if (plannedEpisodes.isEmpty()) {
                        AiUiError(AiUiErrorType.NoLocalEpisodes)
                    } else {
                        null
                    }
                )
            } catch (e: Throwable) {
                uiState = uiState.copy(
                    isBuildingEpisodeList = false,
                    error = e.toUiError()
                )
            }
        }
    }

    fun playFirstEpisode() {
        uiState.plannedEpisodes.firstOrNull()?.let { playEpisode(it.episode.id) }
    }

    fun playEpisode(episodeId: Long) {
        val item = uiState.plannedEpisodes.firstOrNull { it.episode.id == episodeId } ?: return
        if (item.episode.audioUrl.isNullOrBlank()) return
        libraryGateway.playEpisode(item.feedTitle, item.episode)
        libraryGateway.markEpisodePlayed(item.episode.id)
    }

    private fun ensureConfigured(): Boolean {
        if (isAiConfigured) return true
        uiState = uiState.copy(error = AiUiError(AiUiErrorType.MissingApiKey))
        return false
    }

    private fun ensureSelectedFeedsSynced(feedUrls: List<String>) {
        feedUrls.mapNotNull { feedCatalogByUrl[it] }.forEach { defaultFeed ->
            val feedId = libraryGateway.getFeedByUrl(defaultFeed.url)?.id
                ?: libraryGateway.followDefaultFeed(defaultFeed)
            if (feedId > 0L && libraryGateway.episodesForFeed(feedId).isEmpty()) {
                libraryGateway.syncFeed(feedId)
            }
        }
    }

    private fun AiEpisodePlanResult.toPlannedEpisodeUi(
        selectedFeedUrls: List<String>
    ): List<AiPlannedEpisodeUi> {
        val feedsByUrl = selectedFeedUrls
            .mapNotNull { libraryGateway.getFeedByUrl(it) }
            .associateBy { it.url }
        val localEpisodeKeys = feedsByUrl.values
            .flatMap { feed ->
                libraryGateway.episodesForFeed(feed.id).map { episode ->
                    (feed.url to episode.title.trim()) to (feed to episode)
                }
            }
            .toMap()

        return episodes.mapNotNull { planned ->
            val local = localEpisodeKeys[planned.feedUrl to planned.episodeTitle.trim()]
                ?: return@mapNotNull null
            val feed = local.first
            val episode = local.second
            AiPlannedEpisodeUi(
                feedUrl = planned.feedUrl,
                feedId = feed.id,
                feedTitle = feed.title,
                episode = episode,
                episodeReason = planned.episodeReason,
                rank = planned.rank
            )
        }
    }

    private fun List<AiFeedRecommendation>.toUiRecommendations(
        followedUrls: Set<String>,
        selectedFeedUrls: Set<String>,
        syncingFeedUrls: Set<String>
    ): List<AiRecommendedFeed> =
        mapNotNull { recommendation ->
            val feed = feedCatalogByUrl[recommendation.feedUrl] ?: return@mapNotNull null
            AiRecommendedFeed(
                feed = feed,
                reason = recommendation.reason,
                rank = recommendation.rank,
                isFollowed = feed.url in followedUrls,
                isSyncing = feed.url in syncingFeedUrls,
                isSelected = feed.url in selectedFeedUrls,
                localFeedId = libraryGateway.getFeedByUrl(feed.url)?.id
            )
        }

    private fun updateRecommendationFlags(
        followedUrls: Set<String> = libraryGateway.followedFeedUrls(),
        syncingFeedUrls: Set<String> = uiState.syncingFeedUrls,
        clearError: Boolean = false
    ): AiUiState =
        uiState.copy(
            syncingFeedUrls = syncingFeedUrls,
            error = if (clearError) null else uiState.error,
            recommendations = uiState.recommendations.map { item ->
                item.copy(
                    isFollowed = item.feed.url in followedUrls,
                    isSyncing = item.feed.url in syncingFeedUrls,
                    isSelected = item.feed.url in uiState.selectedFeedUrls,
                    localFeedId = libraryGateway.getFeedByUrl(item.feed.url)?.id
                )
            }
        )

    private fun launchWork(block: suspend CoroutineScope.() -> Unit): Job =
        (externalScope ?: viewModelScope).launch(block = block)

    private fun Throwable.toUiError(): AiUiError =
        when ((this as? PodcastAiException)?.kind) {
            AiFailureKind.MissingApiKey -> AiUiError(AiUiErrorType.MissingApiKey, message)
            AiFailureKind.RateLimited -> AiUiError(AiUiErrorType.RateLimited, message)
            AiFailureKind.MalformedResponse -> AiUiError(AiUiErrorType.MalformedResponse, message)
            AiFailureKind.NoLocalEpisodes -> AiUiError(AiUiErrorType.NoLocalEpisodes, message)
            AiFailureKind.Network -> AiUiError(AiUiErrorType.Network, message)
            null -> AiUiError(AiUiErrorType.Unknown, message)
        }

    class Factory(
        private val aiService: PodcastAiService,
        private val libraryGateway: AiLibraryGateway,
        private val isAiConfigured: Boolean,
        private val localeTagProvider: () -> String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AiViewModel::class.java)) {
                return AiViewModel(
                    aiService = aiService,
                    libraryGateway = libraryGateway,
                    isAiConfigured = isAiConfigured,
                    localeTagProvider = localeTagProvider
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class.")
        }
    }
}
