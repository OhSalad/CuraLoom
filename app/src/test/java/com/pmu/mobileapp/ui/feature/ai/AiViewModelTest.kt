package com.pmu.mobileapp.ui.feature.ai

import com.pmu.mobileapp.data.DefaultPodcastFeed
import com.pmu.mobileapp.data.DefaultPodcastFeeds
import com.pmu.mobileapp.data.ai.AiEpisodePlanResult
import com.pmu.mobileapp.data.ai.AiFailureKind
import com.pmu.mobileapp.data.ai.AiFeedRecommendation
import com.pmu.mobileapp.data.ai.AiRecommendationResult
import com.pmu.mobileapp.data.ai.PodcastAiException
import com.pmu.mobileapp.data.ai.PodcastAiService
import com.pmu.mobileapp.data.repository.FeedSyncResult
import com.pmu.mobileapp.model.Episode
import com.pmu.mobileapp.model.Feed
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiViewModelTest {
    @Test
    fun missingApiKeyDisablesRecommendationFlow() = runTest {
        val vm = viewModel(isAiConfigured = false)
        vm.onPromptChange("science")

        val job = vm.recommendFeeds()

        assertNull(job)
        assertFalse(vm.uiState.isAiConfigured)
        assertEquals(AiUiErrorType.MissingApiKey, vm.uiState.error?.type)
    }

    @Test
    fun recommendFeedsShowsLoadingThenSuccess() = runTest {
        val feed = DefaultPodcastFeeds.feeds.first()
        val gate = CompletableDeferred<Unit>()
        val service = FakePodcastAiService(
            recommendationGate = gate,
            recommendationResult = AiRecommendationResult(
                summary = "Matched design shows.",
                recommendations = listOf(
                    AiFeedRecommendation(
                        feedUrl = feed.url,
                        reason = "Strong fit.",
                        rank = 1
                    )
                )
            )
        )
        val vm = viewModel(service = service)
        vm.onPromptChange("design")

        vm.recommendFeeds()
        runCurrent()

        assertTrue(vm.uiState.isLoadingRecommendations)

        gate.complete(Unit)
        advanceUntilIdle()

        assertFalse(vm.uiState.isLoadingRecommendations)
        assertEquals("design", vm.uiState.submittedPrompt)
        assertEquals("Matched design shows.", vm.uiState.summary)
        assertEquals(feed.url, vm.uiState.recommendations.single().feed.url)
        assertNull(vm.uiState.error)
    }

    @Test
    fun malformedRecommendationResponseSetsMalformedError() = runTest {
        val vm = viewModel(
            service = FakePodcastAiService(
                recommendationError = PodcastAiException(
                    kind = AiFailureKind.MalformedResponse,
                    message = "bad json"
                )
            )
        )
        vm.onPromptChange("history")

        vm.recommendFeeds()
        advanceUntilIdle()

        assertEquals(AiUiErrorType.MalformedResponse, vm.uiState.error?.type)
        assertFalse(vm.uiState.isLoadingRecommendations)
    }

    @Test
    fun rateLimitRecommendationFailureSetsRateLimitedError() = runTest {
        val vm = viewModel(
            service = FakePodcastAiService(
                recommendationError = PodcastAiException(
                    kind = AiFailureKind.RateLimited,
                    message = "429"
                )
            )
        )
        vm.onPromptChange("technology")

        vm.recommendFeeds()
        advanceUntilIdle()

        assertEquals(AiUiErrorType.RateLimited, vm.uiState.error?.type)
        assertFalse(vm.uiState.isLoadingRecommendations)
    }

    private fun TestScope.viewModel(
        service: PodcastAiService = FakePodcastAiService(),
        library: AiLibraryGateway = FakeLibraryGateway(),
        isAiConfigured: Boolean = true
    ): AiViewModel =
        AiViewModel(
            aiService = service,
            libraryGateway = library,
            isAiConfigured = isAiConfigured,
            localeTagProvider = { "en" },
            ioDispatcher = StandardTestDispatcher(testScheduler),
            externalScope = this
        )

    private class FakePodcastAiService(
        private val recommendationGate: CompletableDeferred<Unit>? = null,
        private val recommendationResult: AiRecommendationResult = AiRecommendationResult(
            summary = "",
            recommendations = emptyList()
        ),
        private val recommendationError: Throwable? = null
    ) : PodcastAiService {
        override suspend fun recommendFeeds(
            prompt: String,
            localeTag: String
        ): AiRecommendationResult {
            recommendationGate?.await()
            recommendationError?.let { throw it }
            return recommendationResult
        }

        override suspend fun buildEpisodeList(
            prompt: String,
            selectedFeedUrls: List<String>,
            localeTag: String
        ): AiEpisodePlanResult = AiEpisodePlanResult(
            playlistTitle = "",
            playlistReason = "",
            episodes = emptyList()
        )
    }

    private class FakeLibraryGateway : AiLibraryGateway {
        private val feedsByUrl = mutableMapOf<String, Feed>()
        private val episodesByFeedId = mutableMapOf<Long, List<Episode>>()
        private var nextFeedId = 1L

        override fun followedFeedUrls(): Set<String> = feedsByUrl.keys

        override fun getFeedByUrl(feedUrl: String): Feed? = feedsByUrl[feedUrl]

        override fun followDefaultFeed(feed: DefaultPodcastFeed): Long {
            feedsByUrl[feed.url]?.let { return it.id }
            val localFeed = Feed().apply {
                id = nextFeedId++
                title = feed.title
                url = feed.url
                description = feed.description
                category = feed.category
                author = feed.author
            }
            feedsByUrl[feed.url] = localFeed
            return localFeed.id
        }

        override fun syncFeed(feedId: Long): FeedSyncResult =
            FeedSyncResult(success = true)

        override fun episodesForFeed(feedId: Long): List<Episode> =
            episodesByFeedId[feedId].orEmpty()

        override fun playEpisode(feedTitle: String, episode: Episode) = Unit

        override fun markEpisodePlayed(episodeId: Long) = Unit
    }
}
