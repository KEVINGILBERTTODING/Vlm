package com.vlm.core.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.util.regex.Pattern

object StreamResolver {
    private val sampleStreams = listOf(
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
    )

    suspend fun resolveStreamUrl(pageUrl: String): String = withContext(Dispatchers.IO) {
        try {
            if (pageUrl.contains(".m3u8") || pageUrl.contains(".mp4")) {
                return@withContext pageUrl
            }

            // 1. Fetch movie page HTML
            val doc = Jsoup.connect(pageUrl)
                .userAgent("Mozilla/5.0 (Linux; Android 13; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .timeout(6000)
                .get()

            // 2. Extract data-src from .ifx-pemutar or iframe
            var embedUrl = doc.select(".ifx-pemutar[data-src], [data-pemutar][data-src]").attr("data-src")
            if (embedUrl.isBlank()) {
                embedUrl = doc.select("iframe.ifx-pemutar-iframe, iframe[src*='putarin'], iframe[src*='embed']").attr("src")
            }

            if (embedUrl.isNotBlank()) {
                if (embedUrl.startsWith("//")) {
                    embedUrl = "https:$embedUrl"
                } else if (!embedUrl.startsWith("http")) {
                    embedUrl = "https://idflix.top$embedUrl"
                }

                // 3. Connect to embed player URL (e.g. putarin.xyz)
                val embedDoc = Jsoup.connect(embedUrl)
                    .referrer("https://idflix.top/")
                    .userAgent("Mozilla/5.0 (Linux; Android 13; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                    .timeout(6000)
                    .get()

                val html = embedDoc.html()

                // Extract .m3u8 or .mp4 URL using Regex
                val streamPattern = Pattern.compile("(https?://[^\"'\\s]+\\.(?:m3u8|mp4)[^\"'\\s]*)")
                val matcher = streamPattern.matcher(html)
                if (matcher.find()) {
                    val streamUrl = matcher.group(1)
                    if (!streamUrl.isNullOrBlank()) {
                        return@withContext streamUrl
                    }
                }

                // Try finding inside video tag
                val videoSrc = embedDoc.select("video source, video").attr("src")
                if (videoSrc.isNotBlank()) {
                    return@withContext videoSrc
                }
            }

            getSampleStream(pageUrl)
        } catch (e: Exception) {
            getSampleStream(pageUrl)
        }
    }

    private fun getSampleStream(seed: String): String {
        val index = Math.abs(seed.hashCode()) % sampleStreams.size
        return sampleStreams[index]
    }
}
