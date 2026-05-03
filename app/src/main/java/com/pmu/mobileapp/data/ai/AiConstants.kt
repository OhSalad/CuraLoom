package com.pmu.mobileapp.data.ai

object PodcastAiConstants {
    const val DEFAULT_MODEL = "gemini-2.5-flash"
    const val MAX_RECOMMENDATIONS = 5
    const val FEED_CANDIDATE_CAP = 20
    const val EPISODE_CANDIDATE_CAP = 60
    const val EPISODE_CANDIDATES_PER_FEED = 12
    const val MAX_EPISODE_PLAN_ITEMS = 12
    const val MAX_OUTPUT_TOKENS = 2048
    const val PROMPT_CHARACTER_BUDGET = 18_000
    const val USER_PROMPT_CHARACTER_CAP = 4_000
}
