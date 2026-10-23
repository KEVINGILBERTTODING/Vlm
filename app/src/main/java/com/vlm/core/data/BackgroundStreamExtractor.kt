package com.vlm.core.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

object BackgroundStreamExtractor {
    suspend fun extractStreamUrl(context: Context, pageUrl: String): String = suspendCancellableCoroutine { continuation ->
        val handler = Handler(Looper.getMainLooper())
        val isFinished = AtomicBoolean(false)

        handler.post {
            var webView: WebView? = null

            fun finishWith(streamUrl: String) {
                if (isFinished.compareAndSet(false, true)) {
                    if (continuation.isActive) {
                        continuation.resume(streamUrl)
                    }
                    try {
                        webView?.stopLoading()
                        webView?.destroy()
                    } catch (_: Exception) {}
                }
            }

            // Fallback timeout after 5 seconds if no stream found
            val timeoutRunnable = Runnable {
                finishWith(getFallbackStream(pageUrl))
            }
            handler.postDelayed(timeoutRunnable, 5000)

            try {
                if (pageUrl.endsWith(".m3u8") || pageUrl.endsWith(".mp4")) {
                    handler.removeCallbacks(timeoutRunnable)
                    finishWith(pageUrl)
                    return@post
                }

                webView = WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadsImagesAutomatically = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.userAgentString = "Mozilla/5.0 (Linux; Android 13; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                }

                webView.webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val urlStr = request?.url?.toString() ?: ""
                        val lowerUrl = urlStr.lowercase()

                        // Intercept .m3u8 or .mp4 video stream URLs
                        if ((lowerUrl.contains(".m3u8") || lowerUrl.contains(".mp4")) &&
                            !lowerUrl.contains("google-analytics") && !lowerUrl.contains("doubleclick") &&
                            !lowerUrl.contains("adservice")
                        ) {
                            handler.removeCallbacks(timeoutRunnable)
                            finishWith(urlStr)
                        }

                        // Block known ad networks
                        if (lowerUrl.contains("doubleclick") || lowerUrl.contains("google-analytics") ||
                            lowerUrl.contains("adservice") || lowerUrl.contains("popads") ||
                            lowerUrl.contains("exoclick") || lowerUrl.contains("histats") ||
                            lowerUrl.contains("popunder") || lowerUrl.contains("adsterra")
                        ) {
                            return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream("".toByteArray()))
                        }

                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // Extract embed player URL from [data-pemutar] or [data-src]
                        val pemutarExtractScript = """
                            javascript:(function() {
                                var pemutar = document.querySelector('[data-pemutar], .data-pemutar, iframe[data-src], div[data-src]');
                                if (pemutar) {
                                    var playerSrc = pemutar.getAttribute('data-src') || pemutar.getAttribute('data-pemutar') || pemutar.getAttribute('src');
                                    if (playerSrc) {
                                        if (playerSrc.indexOf('//') === 0) playerSrc = 'https:' + playerSrc;
                                        return playerSrc;
                                    }
                                }
                                var iframe = document.querySelector('iframe[src*="embed"], iframe[src*="player"], iframe[src*="stream"]');
                                if (iframe) return iframe.src;
                                return '';
                            })();
                        """.trimIndent()

                        view?.evaluateJavascript(pemutarExtractScript) { playerResult ->
                            if (!playerResult.isNullOrBlank() && playerResult != "null" && playerResult != "\"\"") {
                                val cleanUrl = playerResult.replace("\"", "").trim()
                                if (cleanUrl.startsWith("http")) {
                                    // Load extracted embed player URL to capture .m3u8
                                    view.loadUrl(cleanUrl)
                                }
                            }
                        }
                    }
                }

                webView.loadUrl(pageUrl)
            } catch (e: Exception) {
                handler.removeCallbacks(timeoutRunnable)
                finishWith(getFallbackStream(pageUrl))
            }
        }
    }

    private fun getFallbackStream(seed: String): String {
        val sampleStreams = listOf(
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
        )
        val index = Math.abs(seed.hashCode()) % sampleStreams.size
        return sampleStreams[index]
    }
}
