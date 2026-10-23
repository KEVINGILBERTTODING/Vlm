package com.vlm.core.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.vlm.core.model.Movie
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

object IdFlixWebScraper {
    suspend fun fetchMovies(context: Context): List<Movie> = suspendCancellableCoroutine { continuation ->
        val handler = Handler(Looper.getMainLooper())
        val isFinished = AtomicBoolean(false)

        handler.post {
            var webView: WebView? = null

            fun finishWith(movies: List<Movie>) {
                if (isFinished.compareAndSet(false, true)) {
                    if (continuation.isActive) {
                        continuation.resume(movies)
                    }
                    try {
                        webView?.stopLoading()
                        webView?.destroy()
                    } catch (_: Exception) {}
                }
            }

            // Fallback timeout after 6 seconds
            val timeoutRunnable = Runnable {
                finishWith(MovieBoxRepository.getFallbackMovies())
            }
            handler.postDelayed(timeoutRunnable, 6000)

            try {
                webView = WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadsImagesAutomatically = true
                    // Spoof Chrome Mobile User-Agent to bypass Cloudflare WebView detection
                    settings.userAgentString = "Mozilla/5.0 (Linux; Android 13; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                }

                fun runExtraction() {
                    if (isFinished.get()) return
                    val jsExtractScript = """
                        (function() {
                            var items = document.querySelectorAll('.item, .film-item, article, .movie-card, .flw-item, .poster, .film_list-item, .ml-item, .col');
                            var list = [];
                            var seen = {};

                            for (var i = 0; i < items.length; i++) {
                                var item = items[i];
                                var linkElem = item.tagName === 'A' ? item : item.querySelector('a');
                                var imgElem = item.querySelector('img') || (item.parentElement ? item.parentElement.querySelector('img') : null);
                                var titleElem = item.querySelector('.film-name, .title, h2, h3, .name, .film-detail .name a');

                                if (!linkElem || !imgElem) continue;

                                var href = linkElem.href || '';
                                if (!href || href === 'javascript:void(0);' || seen[href]) continue;

                                var rawTitle = '';
                                if (titleElem) {
                                    rawTitle = titleElem.getAttribute('title') || titleElem.innerText || titleElem.textContent || '';
                                }
                                if (!rawTitle || /^\d+$/.test(rawTitle.trim())) {
                                    rawTitle = linkElem.getAttribute('title') || linkElem.getAttribute('alt') || imgElem.getAttribute('alt') || imgElem.getAttribute('title') || '';
                                }

                                var cleanTitle = rawTitle.replace(/[\n\r]+/g, ' ').replace(/\s+/g, ' ').trim();

                                if (cleanTitle.length > 1 && !/^\d+$/.test(cleanTitle)) {
                                    var poster = imgElem.getAttribute('data-src') || imgElem.getAttribute('data-original') || imgElem.getAttribute('data-lazy-src') || imgElem.src || '';

                                    if (poster && poster.indexOf('http') === 0) {
                                        seen[href] = true;
                                        list.push({
                                            title: cleanTitle,
                                            poster: poster.trim(),
                                            link: href.trim()
                                        });
                                    }
                                }
                            }
                            return JSON.stringify(list);
                        })();
                    """.trimIndent()

                    webView?.evaluateJavascript(jsExtractScript) { jsonResult ->
                        if (isFinished.get()) return@evaluateJavascript
                        try {
                            if (jsonResult.isNullOrBlank() || jsonResult == "null") {
                                return@evaluateJavascript
                            }
                            val unquoted = if (jsonResult.startsWith("\"") && jsonResult.endsWith("\"")) {
                                jsonResult.substring(1, jsonResult.length - 1).replace("\\\"", "\"").replace("\\\\", "\\")
                            } else {
                                jsonResult
                            }
                            val jsonArray = JSONArray(unquoted)
                            val movieList = mutableListOf<Movie>()

                            for (i in 0 until jsonArray.length()) {
                                val obj = jsonArray.getJSONObject(i)
                                val title = obj.getString("title")
                                val poster = obj.getString("poster")
                                val link = obj.getString("link")

                                movieList.add(
                                    Movie(
                                        id = (i + 1).toString(),
                                        title = title,
                                        overview = "Stream $title in HD on IdFlix without ads.",
                                        posterUrl = poster,
                                        backdropUrl = poster,
                                        releaseYear = "2024",
                                        rating = "8.1",
                                        genre = "Trending, HD",
                                        watchUrl = link.ifBlank { "https://idflix.top/" }
                                    )
                                )
                            }

                            if (movieList.isNotEmpty()) {
                                handler.removeCallbacks(timeoutRunnable)
                                finishWith(movieList)
                            }
                        } catch (_: Exception) {}
                    }
                }

                webView?.webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val urlStr = request?.url?.toString()?.lowercase() ?: ""
                        if (urlStr.contains("doubleclick") || urlStr.contains("google-analytics") ||
                            urlStr.contains("adservice") || urlStr.contains("popads") ||
                            urlStr.contains("exoclick") || urlStr.contains("histats")
                        ) {
                            return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream("".toByteArray()))
                        }
                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        runExtraction()
                    }
                }

                handler.postDelayed({ runExtraction() }, 2500)
                handler.postDelayed({ runExtraction() }, 4500)

                webView?.loadUrl("https://idflix.top/")
            } catch (e: Exception) {
                handler.removeCallbacks(timeoutRunnable)
                finishWith(MovieBoxRepository.getFallbackMovies())
            }
        }
    }
}
