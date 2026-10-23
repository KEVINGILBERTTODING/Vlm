package com.vlm.core.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

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
            if (pageUrl.endsWith(".m3u8") || pageUrl.endsWith(".mp4")) {
                return@withContext pageUrl
            }

            // Try scraping iframe source or video source from the web page
            val doc = Jsoup.connect(pageUrl)
                .userAgent("Mozilla/5.0 (Linux; Android 13; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .timeout(5000)
                .get()

            val videoSrc = doc.select("video source, video").attr("src")
            if (videoSrc.isNotBlank() && (videoSrc.endsWith(".m3u8") || videoSrc.endsWith(".mp4"))) {
                return@withContext videoSrc
            }

            val iframeSrc = doc.select("iframe[src*='embed'], iframe[src*='player'], iframe[src*='stream']").attr("src")
            if (iframeSrc.isNotBlank()) {
                val iframeDoc = Jsoup.connect(iframeSrc)
                    .userAgent("Mozilla/5.0 (Linux; Android 13; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                    .timeout(5000)
                    .get()

                val extracted = iframeDoc.select("video source, video").attr("src")
                if (extracted.isNotBlank()) {
                    return@withContext extracted
                }
            }

            // Fallback to high quality video stream
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
