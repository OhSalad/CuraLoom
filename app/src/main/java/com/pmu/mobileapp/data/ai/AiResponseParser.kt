package com.pmu.mobileapp.data.ai

import com.pmu.mobileapp.data.DefaultPodcastFeed
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject

object AiResponseParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parseRecommendations(
        rawJson: String,
        knownFeeds: List<DefaultPodcastFeed>
    ): AiRecommendationResult {
        val root = parseRootObject(rawJson)
        val knownUrls = knownFeeds.map { it.url }.toSet()
        val recommendations = root.array("recommendations")
            .mapIndexedNotNull { index, item ->
                val obj = item as? JsonObject ?: return@mapIndexedNotNull null
                val feedUrl = obj.string("feedUrl")
                val reason = obj.string("reason")
                if (feedUrl !in knownUrls || reason.isBlank()) return@mapIndexedNotNull null
                AiFeedRecommendation(
                    feedUrl = feedUrl,
                    reason = reason,
                    rank = obj.int("rank") ?: index + 1
                )
            }
            .sortedBy { it.rank }
            .take(PodcastAiConstants.MAX_RECOMMENDATIONS)

        return AiRecommendationResult(
            summary = root.string("summary"),
            recommendations = recommendations
        )
    }

    fun parseEpisodePlan(rawJson: String, selectedFeedUrls: Set<String>): AiEpisodePlanResult {
        val root = parseRootObject(rawJson)
        val episodes = root.array("episodes")
            .mapIndexedNotNull { index, item ->
                val obj = item as? JsonObject ?: return@mapIndexedNotNull null
                val feedUrl = obj.string("feedUrl")
                val episodeTitle = obj.string("episodeTitle")
                val episodeReason = obj.string("episodeReason")
                if (
                    feedUrl !in selectedFeedUrls ||
                    episodeTitle.isBlank() ||
                    episodeReason.isBlank()
                ) {
                    return@mapIndexedNotNull null
                }
                AiPlannedEpisode(
                    feedUrl = feedUrl,
                    episodeTitle = episodeTitle,
                    episodeReason = episodeReason,
                    rank = obj.int("rank") ?: index + 1
                )
            }
            .sortedBy { it.rank }
            .take(PodcastAiConstants.MAX_EPISODE_PLAN_ITEMS)

        return AiEpisodePlanResult(
            playlistTitle = root.string("playlistTitle"),
            playlistReason = root.string("playlistReason"),
            episodes = episodes
        )
    }

    fun filterEpisodePlanToCandidates(
        plan: AiEpisodePlanResult,
        candidates: Collection<AiEpisodePromptCandidate>
    ): AiEpisodePlanResult {
        val candidateKeys = candidates
            .map { it.feedUrl.trim() to it.episodeTitle.trim() }
            .toSet()
        return plan.copy(
            episodes = plan.episodes.filter { planned ->
                planned.feedUrl.trim() to planned.episodeTitle.trim() in candidateKeys
            }
        )
    }

    private fun parseRootObject(rawJson: String): JsonObject {
        val normalized = extractJsonObject(rawJson)
        return try {
            json.parseToJsonElement(normalized).jsonObject
        } catch (e: Exception) {
            throw PodcastAiException(
                kind = AiFailureKind.MalformedResponse,
                message = "Gemini returned malformed JSON.",
                cause = e
            )
        }
    }

    private fun extractJsonObject(rawJson: String): String {
        val trimmed = rawJson.trim()
        val first = trimmed.indexOf('{')
        val last = trimmed.lastIndexOf('}')
        if (first < 0 || last <= first) {
            throw PodcastAiException(
                kind = AiFailureKind.MalformedResponse,
                message = "Gemini response did not include a JSON object."
            )
        }
        return trimmed.substring(first, last + 1)
    }

    private fun JsonObject.string(key: String): String =
        (this[key] as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()

    private fun JsonObject.int(key: String): Int? =
        (this[key] as? JsonPrimitive)?.intOrNull

    private fun JsonObject.array(key: String): JsonArray =
        this[key] as? JsonArray ?: JsonArray(emptyList())
}
