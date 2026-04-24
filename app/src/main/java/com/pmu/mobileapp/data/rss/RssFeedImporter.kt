package com.pmu.mobileapp.data.rss

import com.pmu.mobileapp.model.Episode
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.Node

data class ImportedFeedData(
    val resolvedUrl: String,
    val title: String?,
    val description: String?,
    val author: String?,
    val episodes: List<Episode>
)

object RssFeedImporter {
    private const val CONNECT_TIMEOUT_MS = 12000
    private const val READ_TIMEOUT_MS = 20000

    fun import(feedId: Long, feedUrl: String): ImportedFeedData {
        val first = download(feedUrl)
        val rssUrl = discoverRssUrl(first, feedUrl)
        val xmlResponse = if (rssUrl == first.finalUrl) first else download(rssUrl)
        val parsed = parseFeedXml(xmlResponse.body, feedId, xmlResponse.finalUrl)
        return ImportedFeedData(
            resolvedUrl = xmlResponse.finalUrl,
            title = parsed.title,
            description = parsed.description,
            author = parsed.author,
            episodes = parsed.episodes
        )
    }

    private fun discoverRssUrl(response: HttpPayload, fallbackUrl: String): String {
        if (looksLikeXml(response.contentType, response.body)) return response.finalUrl
        val discovered = findRssUrlInHtml(response.body, response.finalUrl)
        return discovered ?: fallbackUrl
    }

    private fun looksLikeXml(contentType: String?, body: String): Boolean {
        if (contentType != null) {
            val lower = contentType.lowercase()
            if (lower.contains("xml") || lower.contains("rss") || lower.contains("atom")) return true
        }
        val trimmed = body.trimStart()
        return trimmed.startsWith("<?xml", ignoreCase = true) ||
            trimmed.startsWith("<rss", ignoreCase = true) ||
            trimmed.startsWith("<feed", ignoreCase = true) ||
            trimmed.startsWith("<rdf", ignoreCase = true)
    }

    private fun findRssUrlInHtml(html: String, baseUrl: String): String? {
        val linkRegex = Regex(
            "<link[^>]*rel=[\"'][^\"']*alternate[^\"']*[\"'][^>]*type=[\"'](?:application|text)/(?:rss\\+xml|atom\\+xml|xml)[\"'][^>]*href=[\"']([^\"']+)[\"'][^>]*>",
            RegexOption.IGNORE_CASE
        )
        val reverseLinkRegex = Regex(
            "<link[^>]*href=[\"']([^\"']+)[\"'][^>]*rel=[\"'][^\"']*alternate[^\"']*[\"'][^>]*type=[\"'](?:application|text)/(?:rss\\+xml|atom\\+xml|xml)[\"'][^>]*>",
            RegexOption.IGNORE_CASE
        )
        val firstLink = linkRegex.find(html)?.groupValues?.getOrNull(1)
            ?: reverseLinkRegex.find(html)?.groupValues?.getOrNull(1)
        if (!firstLink.isNullOrBlank()) return resolveUrl(baseUrl, firstLink)

        val anchorRegex = Regex("<a[^>]*href=[\"']([^\"']+)[\"'][^>]*>", RegexOption.IGNORE_CASE)
        val candidates = anchorRegex.findAll(html)
            .mapNotNull { it.groupValues.getOrNull(1) }
            .filter { href ->
                val lower = href.lowercase()
                lower.contains("rss") || lower.contains("feed") || lower.endsWith(".xml")
            }
            .toList()
        return candidates.firstOrNull()?.let { resolveUrl(baseUrl, it) }
    }

    private fun resolveUrl(baseUrl: String, candidate: String): String {
        return try {
            URI(baseUrl).resolve(candidate).toString()
        } catch (_: Exception) {
            candidate
        }
    }

    private fun download(url: String): HttpPayload {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("User-Agent", "CuraLoom/1.0 (Android)")
            setRequestProperty("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml, text/html;q=0.8, */*;q=0.7")
        }
        try {
            val bytes = BufferedInputStream(connection.inputStream).use { it.readBytes() }
            return HttpPayload(
                finalUrl = connection.url.toString(),
                contentType = connection.contentType,
                body = bytes.toString(Charsets.UTF_8)
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun parseFeedXml(xml: String, feedId: Long, sourceUrl: String): ParsedFeedXml {
        val builderFactory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            isIgnoringComments = true
            isCoalescing = true
        }
        val document = builderFactory.newDocumentBuilder().parse(xml.byteInputStream())
        val root = document.documentElement ?: error("Invalid feed XML")
        val rootName = root.tagName.lowercase()

        return if (rootName == "feed") {
            parseAtom(root, feedId, sourceUrl)
        } else {
            parseRss(document.documentElement, feedId, sourceUrl)
        }
    }

    private fun parseRss(root: Element, feedId: Long, sourceUrl: String): ParsedFeedXml {
        val channel = root.getElementsByTagName("channel").item(0) as? Element ?: root
        val title = textOfFirst(channel, "title")
        val description = textOfFirst(channel, "description")
        val author = textOfFirst(channel, "itunes:author") ?: textOfFirst(channel, "managingEditor")
        val itemNodes = channel.getElementsByTagName("item")
        val episodes = buildList {
            for (i in 0 until itemNodes.length) {
                val item = itemNodes.item(i) as? Element ?: continue
                val episodeTitle = textOfFirst(item, "title") ?: continue
                val enclosure = (item.getElementsByTagName("enclosure").item(0) as? Element)?.getAttribute("url")
                val mediaContent = (item.getElementsByTagName("media:content").item(0) as? Element)?.getAttribute("url")
                val audioUrl = (enclosure?.ifBlank { null } ?: mediaContent?.ifBlank { null })
                    ?.let { resolveUrl(sourceUrl, it) }
                add(
                    Episode().apply {
                        this.feedId = feedId
                        this.title = episodeTitle
                        this.description = textOfFirst(item, "description")
                        this.pubDate = textOfFirst(item, "pubDate")
                        this.duration = textOfFirst(item, "itunes:duration")
                        this.audioUrl = audioUrl
                        this.isNew = true
                    }
                )
            }
        }
        return ParsedFeedXml(title = title, description = description, author = author, episodes = episodes)
    }

    private fun parseAtom(root: Element, feedId: Long, sourceUrl: String): ParsedFeedXml {
        val title = textOfFirst(root, "title")
        val description = textOfFirst(root, "subtitle")
        val authorNode = root.getElementsByTagName("author").item(0) as? Element
        val author = authorNode?.let { textOfFirst(it, "name") }
        val entryNodes = root.getElementsByTagName("entry")
        val episodes = buildList {
            for (i in 0 until entryNodes.length) {
                val entry = entryNodes.item(i) as? Element ?: continue
                val episodeTitle = textOfFirst(entry, "title") ?: continue
                val links = entry.getElementsByTagName("link")
                var audioUrl: String? = null
                for (j in 0 until links.length) {
                    val link = links.item(j) as? Element ?: continue
                    val rel = link.getAttribute("rel")
                    val type = link.getAttribute("type")
                    val href = link.getAttribute("href")
                    if (href.isBlank()) continue
                    if (type.startsWith("audio/") || rel.equals("enclosure", ignoreCase = true)) {
                        audioUrl = resolveUrl(sourceUrl, href)
                        break
                    }
                }
                add(
                    Episode().apply {
                        this.feedId = feedId
                        this.title = episodeTitle
                        this.description = textOfFirst(entry, "summary") ?: textOfFirst(entry, "content")
                        this.pubDate = textOfFirst(entry, "updated") ?: textOfFirst(entry, "published")
                        this.duration = null
                        this.audioUrl = audioUrl
                        this.isNew = true
                    }
                )
            }
        }
        return ParsedFeedXml(title = title, description = description, author = author, episodes = episodes)
    }

    private fun textOfFirst(parent: Element, tag: String): String? {
        val node = parent.getElementsByTagName(tag).item(0) ?: return null
        return nodeText(node)
    }

    private fun nodeText(node: Node): String? {
        val text = node.textContent?.trim()
        return if (text.isNullOrBlank()) null else text
    }

    private data class HttpPayload(
        val finalUrl: String,
        val contentType: String?,
        val body: String
    )

    private data class ParsedFeedXml(
        val title: String?,
        val description: String?,
        val author: String?,
        val episodes: List<Episode>
    )
}
