package com.pmu.mobileapp.data.ai

object AiResponseSchemas {
    val recommendation: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "summary" to mapOf("type" to "string"),
            "recommendations" to mapOf(
                "type" to "array",
                "items" to mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "feedUrl" to mapOf("type" to "string"),
                        "reason" to mapOf("type" to "string"),
                        "rank" to mapOf("type" to "integer")
                    ),
                    "required" to listOf("feedUrl", "reason", "rank")
                )
            )
        ),
        "required" to listOf("summary", "recommendations")
    )

    val episodePlan: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "playlistTitle" to mapOf("type" to "string"),
            "playlistReason" to mapOf("type" to "string"),
            "episodes" to mapOf(
                "type" to "array",
                "items" to mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "feedUrl" to mapOf("type" to "string"),
                        "episodeTitle" to mapOf("type" to "string"),
                        "episodeReason" to mapOf("type" to "string"),
                        "rank" to mapOf("type" to "integer")
                    ),
                    "required" to listOf("feedUrl", "episodeTitle", "episodeReason", "rank")
                )
            )
        ),
        "required" to listOf("playlistTitle", "playlistReason", "episodes")
    )
}
