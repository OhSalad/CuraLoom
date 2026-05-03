package com.pmu.mobileapp.data.ai

import com.google.genai.Client
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.Schema
import com.google.genai.types.Type
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface AiJsonClient {
    suspend fun generateJson(prompt: String, responseSchema: Map<String, Any>): String
}

class GoogleGenAiJsonClient(
    private val apiKey: String,
    private val modelName: String,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AiJsonClient {
    private val client: Client by lazy {
        Client.builder().apiKey(apiKey).build()
    }

    override suspend fun generateJson(prompt: String, responseSchema: Map<String, Any>): String =
        withContext(ioDispatcher) {
            if (apiKey.isBlank()) {
                throw PodcastAiException(
                    kind = AiFailureKind.MissingApiKey,
                    message = "Gemini API key is missing."
                )
            }

            try {
                val config = GenerateContentConfig.builder()
                    .responseMimeType("application/json")
                    .candidateCount(1)
                    .maxOutputTokens(PodcastAiConstants.MAX_OUTPUT_TOKENS)
                    .responseSchema(responseSchema.toGenAiSchema())
                    .build()
                val response = client.models.generateContent(modelName, prompt, config)
                response.text().orEmpty().ifBlank {
                    throw PodcastAiException(
                        kind = AiFailureKind.MalformedResponse,
                        message = "Gemini returned an empty response."
                    )
                }
            } catch (e: PodcastAiException) {
                throw e
            } catch (e: Throwable) {
                throw e.toPodcastAiException()
            }
        }

    private fun Throwable.toPodcastAiException(): PodcastAiException {
        val details = "${this::class.java.name} ${message.orEmpty()}".lowercase()
        val kind = if (
            "429" in details ||
            "rate" in details ||
            "quota" in details ||
            "resource_exhausted" in details
        ) {
            AiFailureKind.RateLimited
        } else {
            AiFailureKind.Network
        }
        return PodcastAiException(
            kind = kind,
            message = message ?: "Unable to reach Gemini.",
            cause = this
        )
    }

    private fun Map<String, Any>.toGenAiSchema(): Schema {
        val builder = Schema.builder()
        (this["type"] as? String)?.let { builder.type(it.toGenAiType()) }
        (this["properties"] as? Map<*, *>)?.let { properties ->
            builder.properties(
                properties.mapNotNull { (key, value) ->
                    val name = key as? String ?: return@mapNotNull null
                    val schema = (value as? Map<*, *>)?.toStringKeyedMap()?.toGenAiSchema()
                        ?: return@mapNotNull null
                    name to schema
                }.toMap()
            )
        }
        (this["items"] as? Map<*, *>)?.toStringKeyedMap()?.let { items ->
            builder.items(items.toGenAiSchema())
        }
        (this["required"] as? List<*>)?.mapNotNull { it as? String }?.let { required ->
            builder.required(required)
        }
        return builder.build()
    }

    private fun Map<*, *>.toStringKeyedMap(): Map<String, Any> =
        mapNotNull { (key, value) ->
            val name = key as? String ?: return@mapNotNull null
            val nonNullValue = value ?: return@mapNotNull null
            name to nonNullValue
        }.toMap()

    private fun String.toGenAiType(): Type.Known =
        when (lowercase()) {
            "object" -> Type.Known.OBJECT
            "array" -> Type.Known.ARRAY
            "string" -> Type.Known.STRING
            "integer" -> Type.Known.INTEGER
            "number" -> Type.Known.NUMBER
            "boolean" -> Type.Known.BOOLEAN
            "null" -> Type.Known.NULL
            else -> Type.Known.TYPE_UNSPECIFIED
        }
}
