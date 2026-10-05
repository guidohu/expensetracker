package com.github.guidohu.expensetracker.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** Open Graph metadata scraped from a web page, for a WhatsApp-style link preview. */
data class UrlPreview(
    val title: String?,
    val description: String?,
    val imageUrl: String?,
)

/**
 * Fetches a page's `<head>` and pulls out Open Graph tags for a link preview. Hand-rolled rather
 * than pulling in an HTML parser — same reasoning as [ExchangeRateService]: one small, bounded
 * read, no need for a full DOM.
 */
class UrlPreviewService {

    suspend fun fetch(url: String): UrlPreview? = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                requestMethod = "GET"
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; ExpenseTrackerBot/1.0)")
            }
            val html = connection.inputStream.bufferedReader().use { reader ->
                val buffer = CharArray(MAX_CHARS)
                val count = reader.read(buffer)
                if (count <= 0) "" else String(buffer, 0, count)
            }
            connection.disconnect()
            html
        }.mapCatching { html ->
            val preview = UrlPreview(
                title = metaContent(html, "og:title") ?: titleTag(html),
                description = metaContent(html, "og:description") ?: metaContent(html, "description"),
                imageUrl = metaContent(html, "og:image"),
            )
            if (preview.title == null && preview.description == null && preview.imageUrl == null) null else preview
        }.getOrNull()
    }

    private fun metaContent(html: String, property: String): String? {
        val pattern = Regex(
            """<meta[^>]+(?:property|name)=["']$property["'][^>]+content=["']([^"']*)["']""",
            RegexOption.IGNORE_CASE,
        )
        val reversedPattern = Regex(
            """<meta[^>]+content=["']([^"']*)["'][^>]+(?:property|name)=["']$property["']""",
            RegexOption.IGNORE_CASE,
        )
        val match = pattern.find(html) ?: reversedPattern.find(html)
        return match?.groupValues?.get(1)?.takeIf { it.isNotBlank() }?.let(::unescapeHtml)
    }

    private fun titleTag(html: String): String? =
        Regex("""<title[^>]*>([^<]*)</title>""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }?.let(::unescapeHtml)

    private fun unescapeHtml(text: String): String = text
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")

    companion object {
        private const val MAX_CHARS = 200_000
    }
}
