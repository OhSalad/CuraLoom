package com.pmu.mobileapp.data.ai

import com.pmu.mobileapp.data.DefaultPodcastFeeds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AiResponseParserTest {
    @Test
    fun parseRecommendationsRejectsHallucinatedFeedUrls() {
        val knownFeed = DefaultPodcastFeeds.feeds.first()
        val rawJson = """
            {
              "summary": "Two matches",
              "recommendations": [
                {"feedUrl": "${knownFeed.url}", "reason": "Real catalog feed.", "rank": 1},
                {"feedUrl": "https://example.com/hallucinated.xml", "reason": "Not real.", "rank": 2}
              ]
            }
        """.trimIndent()

        val result = AiResponseParser.parseRecommendations(rawJson, DefaultPodcastFeeds.feeds)

        assertEquals("Two matches", result.summary)
        assertEquals(1, result.recommendations.size)
        assertEquals(knownFeed.url, result.recommendations.single().feedUrl)
    }

    @Test
    fun filterEpisodePlanToCandidatesKeepsOnlyExactLocalEpisodes() {
        val feedUrl = DefaultPodcastFeeds.feeds.first().url
        val plan = AiEpisodePlanResult(
            playlistTitle = "Walk",
            playlistReason = "Good order",
            episodes = listOf(
                AiPlannedEpisode(
                    feedUrl = feedUrl,
                    episodeTitle = "Known episode",
                    episodeReason = "Matches",
                    rank = 1
                ),
                AiPlannedEpisode(
                    feedUrl = feedUrl,
                    episodeTitle = "Unknown episode",
                    episodeReason = "Should be dropped",
                    rank = 2
                )
            )
        )
        val candidates = listOf(
            AiEpisodePromptCandidate(
                feedUrl = feedUrl,
                feedTitle = "Feed",
                episodeTitle = "Known episode",
                pubDate = null,
                duration = null,
                isPlayed = false
            )
        )

        val filtered = AiResponseParser.filterEpisodePlanToCandidates(plan, candidates)

        assertEquals(1, filtered.episodes.size)
        assertEquals("Known episode", filtered.episodes.single().episodeTitle)
    }

    @Test
    fun parseRecommendationsThrowsOnMalformedJson() {
        val error = assertThrows(PodcastAiException::class.java) {
            AiResponseParser.parseRecommendations("not json", DefaultPodcastFeeds.feeds)
        }

        assertEquals(AiFailureKind.MalformedResponse, error.kind)
    }
}
