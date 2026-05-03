package com.pmu.mobileapp.data.ai

import com.pmu.mobileapp.data.DefaultPodcastFeed

object AiPromptBuilder {
    fun recommendationPrompt(
        prompt: String,
        localeTag: String,
        feeds: List<DefaultPodcastFeed>
    ): String = trimToBudget(
        """
        You are CuraLoom's podcast recommender.
        Respond in JSON only. Do not include markdown.
        Use locale "$localeTag" for summary and reasons.
        Recommend only feedUrl values present in the catalog.
        Rank the best matches first.

        User request:
        "${prompt.trim().take(PodcastAiConstants.USER_PROMPT_CHARACTER_CAP)}"

        Catalog:
        ${feedCatalogJson(feeds)}

        Return this shape:
        {"summary":"short response","recommendations":[{"feedUrl":"exact catalog URL","reason":"why this fits","rank":1}]}
        """.trimIndent()
    )

    fun episodePlanPrompt(
        prompt: String,
        selectedFeedUrls: List<String>,
        localeTag: String,
        candidates: List<AiEpisodePromptCandidate>
    ): String = trimToBudget(
        """
        You are CuraLoom's episode planner.
        Respond in JSON only. Do not include markdown.
        Use locale "$localeTag" for playlistTitle, playlistReason, and episodeReason.
        Choose only episodes listed in the candidate pool.
        Match episodeTitle exactly as written in the candidate pool.
        Rank the best listening order first.

        User request:
        "${prompt.trim().take(PodcastAiConstants.USER_PROMPT_CHARACTER_CAP)}"

        Selected feed URLs:
        ${selectedFeedUrls.distinct().joinToString(prefix = "[", postfix = "]") { it.jsonQuoted() }}

        Candidate episodes:
        ${episodeCatalogJson(candidates)}

        Return this shape:
        {"playlistTitle":"title","playlistReason":"why this order fits","episodes":[{"feedUrl":"exact selected URL","episodeTitle":"exact candidate title","episodeReason":"why this episode fits","rank":1}]}
        """.trimIndent()
    )

    private fun feedCatalogJson(feeds: List<DefaultPodcastFeed>): String =
        feeds.take(PodcastAiConstants.FEED_CANDIDATE_CAP)
            .joinToString(prefix = "[", postfix = "]") { feed ->
                """
                {"title":${feed.title.jsonQuoted()},"feedUrl":${feed.url.jsonQuoted()},"category":${feed.category.jsonQuoted()},"author":${feed.author.jsonQuoted()},"description":${feed.description.jsonQuoted()}}
                """.trimIndent()
            }

    private fun episodeCatalogJson(candidates: List<AiEpisodePromptCandidate>): String =
        candidates.take(PodcastAiConstants.EPISODE_CANDIDATE_CAP)
            .joinToString(prefix = "[", postfix = "]") { episode ->
                """
                {"feedUrl":${episode.feedUrl.jsonQuoted()},"feedTitle":${episode.feedTitle.jsonQuoted()},"episodeTitle":${episode.episodeTitle.jsonQuoted()},"pubDate":${episode.pubDate.orEmpty().jsonQuoted()},"duration":${episode.duration.orEmpty().jsonQuoted()},"isPlayed":${episode.isPlayed}}
                """.trimIndent()
            }

    private fun trimToBudget(prompt: String): String =
        prompt.take(PodcastAiConstants.PROMPT_CHARACTER_BUDGET)

    private fun String.jsonQuoted(): String = buildString {
        append('"')
        for (char in this@jsonQuoted) {
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
        append('"')
    }
}
