package com.jarvisai.assistant.service

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class WebPageSnapshot(
    val url: String,
    val title: String,
    val extractedText: String
)

/**
 * Lightweight, crash-proof headless Web Agent using OkHttp & pure Kotlin HTML/DOM text extraction.
 * Avoids instantiating an offscreen Chromium WebView renderer process, eliminating
 * `[ERROR:aw_browser_terminator.cc(154)] Renderer process crash detected (code -1)` on emulators
 * and low-RAM devices while reliably extracting readable page titles and DOM text.
 */
class WebAgentManager(private val context: Context) {

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun fetchAndExtractDomText(
        targetUrl: String,
        timeoutMs: Long = 15_000L
    ): Result<WebPageSnapshot> = withContext(Dispatchers.IO) {
        val trimmed = targetUrl.trim()
        if (trimmed.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Target URL cannot be empty."))
        }

        val normalizedUrl = if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "https://$trimmed"
        }

        try {
            val request = Request.Builder()
                .url(normalizedUrl)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14; JARVIS-AI-Agent) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                )
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,text/plain;q=0.8,*/*;q=0.5")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val finalUrl = response.request.url.toString()
                val rawBody = response.body?.string().orEmpty()

                if (!response.isSuccessful && rawBody.isBlank()) {
                    return@withContext Result.failure(
                        Exception("HTTP ${response.code}: ${response.message.ifBlank { "Unable to fetch URL" }}")
                    )
                }

                val extractedTitle = extractHtmlTitle(rawBody).ifBlank { finalUrl }
                val readableText = extractReadableDomText(rawBody)

                Result.success(
                    WebPageSnapshot(
                        url = finalUrl,
                        title = extractedTitle,
                        extractedText = readableText.ifBlank { "Page loaded successfully (no visible body text)." }
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception("Web fetch failed: ${e.localizedMessage ?: "Network error"}"))
        }
    }

    private fun extractHtmlTitle(html: String): String {
        val match = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .find(html)
        return decodeHtmlEntities(match?.groupValues?.getOrNull(1).orEmpty())
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun extractReadableDomText(html: String): String {
        if (html.isBlank()) return ""
        val withoutScriptsOrStyles = html
            .replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<noscript[^>]*>[\\s\\S]*?</noscript>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<!--[\\s\\S]*?-->"), " ")
            .replace(Regex("<(br|p|div|li|tr|h[1-6]|section|article|header|footer)[^>]*>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), " ")

        return decodeHtmlEntities(withoutScriptsOrStyles)
            .lines()
            .map { it.replace(Regex("[ \\t]+"), " ").trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")
            .take(12_000)
    }

    private fun decodeHtmlEntities(input: String): String {
        return input
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
    }
}
