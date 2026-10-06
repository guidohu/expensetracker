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
            // Shared/short links (what the share flow usually hands us, vs. a real URL typed by
            // hand) commonly 30x-redirect to the real product page. HttpURLConnection's own
            // instanceFollowRedirects leaves getURL() pointing at the ORIGINAL short link even
            // after following the chain, so a page-relative og:image ("/img/x.jpg") would resolve
            // against the wrong host. Follow redirects manually so we know the real final URL.
            var currentUrl = url
            var html = ""
            for (hop in 0 until MAX_REDIRECTS) {
                val connection = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    requestMethod = "GET"
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; ExpenseTrackerBot/1.0)")
                }
                val responseCode = connection.responseCode
                if (responseCode in 300..399) {
                    val location = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (location == null) break
                    currentUrl = URL(URL(currentUrl), location).toString()
                    continue
                }
                html = connection.inputStream.bufferedReader().use { reader ->
                    val buffer = CharArray(MAX_CHARS)
                    val count = reader.read(buffer)
                    if (count <= 0) "" else String(buffer, 0, count)
                }
                connection.disconnect()
                break
            }
            currentUrl to html
        }.mapCatching { (finalUrl, html) ->
            val preview = UrlPreview(
                title = metaContent(html, "og:title") ?: titleTag(html),
                description = metaContent(html, "og:description") ?: metaContent(html, "description"),
                imageUrl = metaContent(html, "og:image")?.let { resolveUrl(finalUrl, it) },
            )
            if (preview.title == null && preview.description == null && preview.imageUrl == null) null else preview
        }.getOrNull()
    }

    /** Resolves a possibly relative or protocol-relative og:image value against the page it came from. */
    private fun resolveUrl(baseUrl: String, maybeRelative: String): String =
        runCatching { URL(URL(baseUrl), maybeRelative).toString() }.getOrDefault(maybeRelative)

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
        private const val MAX_REDIRECTS = 5
    }
}
