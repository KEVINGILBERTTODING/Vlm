@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.vlm.features.player.presentation.ui

import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.vlm.features.player.presentation.contract.PlayerEffect
import com.vlm.features.player.presentation.contract.PlayerIntent
import com.vlm.features.player.presentation.contract.PlayerState
import kotlinx.coroutines.flow.Flow
import java.io.ByteArrayInputStream

@Composable
fun PlayerScreen(
    state: PlayerState,
    effect: Flow<PlayerEffect>,
    onIntent: (PlayerIntent) -> Unit,
    onNavigateBack: () -> Unit
) {
    LaunchedEffect(key1 = true) {
        effect.collect { eff ->
            when (eff) {
                PlayerEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "IdFlix HD Player", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = Color(0xFF4CAF50),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "100% AD-FREE",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onIntent(PlayerIntent.BackClicked) }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF141414))
            )
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (state.url.isNotEmpty()) {
                // Pure clean WebView Video Player Container (Loads putarin.xyz / embed player directly, 100% ad-free & full screen)
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                mediaPlaybackRequiresUserGesture = false
                                setSupportMultipleWindows(false)
                                javaScriptCanOpenWindowsAutomatically = false
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                userAgentString = "Mozilla/5.0 (Linux; Android 13; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onCreateWindow(
                                    view: WebView?,
                                    isDialog: Boolean,
                                    isUserGesture: Boolean,
                                    resultMsg: android.os.Message?
                                ): Boolean {
                                    return false // Reject all ad popup windows & new tabs
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun shouldInterceptRequest(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): WebResourceResponse? {
                                    val lowerUrl = request?.url?.toString()?.lowercase() ?: ""
                                    // Block known ad & tracking networks
                                    if (lowerUrl.contains("doubleclick") || lowerUrl.contains("google-analytics") ||
                                        lowerUrl.contains("adservice") || lowerUrl.contains("popads") ||
                                        lowerUrl.contains("exoclick") || lowerUrl.contains("histats") ||
                                        lowerUrl.contains("popunder") || lowerUrl.contains("adsterra")
                                    ) {
                                        return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream("".toByteArray()))
                                    }
                                    return super.shouldInterceptRequest(view, request)
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val urlStr = request?.url?.toString()?.lowercase() ?: ""
                                    // Allow putarin.xyz and streaming servers, block external ad redirects
                                    if (!urlStr.contains("idflix.top") && !urlStr.contains("putarin") &&
                                        !urlStr.contains("mcloud") && !urlStr.contains("streamtape") &&
                                        !urlStr.contains("embed") && !urlStr.contains("vidsrc") &&
                                        !urlStr.contains("player")
                                    ) {
                                        return true // Block redirect
                                    }
                                    return false
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    // Inject script to extract data-src from .ifx-pemutar or load putarin.xyz embed directly, expanded to 100% full screen with 0% ads
                                    val cleanPlayerScript = """
                                        javascript:(function() {
                                            window.open = function() { return null; };
                                            function setupCleanPlayer() {
                                                var pemutar = document.querySelector('.ifx-pemutar, [data-pemutar], iframe.ifx-pemutar-iframe, iframe[data-src], div[data-src]');
                                                if (pemutar) {
                                                    var pSrc = pemutar.getAttribute('data-src') || pemutar.getAttribute('data-pemutar');
                                                    if (pSrc) {
                                                        if (pSrc.indexOf('//') === 0) pSrc = 'https:' + pSrc;
                                                        if (pemutar.tagName === 'IFRAME' && (!pemutar.src || pemutar.src === 'about:blank')) {
                                                            pemutar.src = pSrc;
                                                        }
                                                    }
                                                }
                                                var iframes = document.querySelectorAll('iframe[src*="putarin"], iframe[src*="embed"], iframe[src*="player"], iframe[src*="stream"], iframe[data-src], #player, .player, video');
                                                if (iframes && iframes.length > 0) {
                                                    for (var k = 0; k < iframes.length; k++) {
                                                        iframes[k].style.position = 'fixed';
                                                        iframes[k].style.top = '0';
                                                        iframes[k].style.left = '0';
                                                        iframes[k].style.width = '100vw';
                                                        iframes[k].style.height = '100vh';
                                                        iframes[k].style.zIndex = '999999';
                                                    }
                                                }
                                                var ads = document.querySelectorAll('iframe[src*="ads"], .ad, .ads, .advertisement, div[id*="ad-"], div[class*="popup"], .modal-backdrop, .popunder, a[target="_blank"]');
                                                for (var i = 0; i < ads.length; i++) {
                                                    ads[i].style.display = 'none';
                                                    try { ads[i].remove(); } catch(e) {}
                                                }
                                            }
                                            setupCleanPlayer();
                                            setInterval(setupCleanPlayer, 500);
                                        })();
                                    """.trimIndent()
                                    view?.evaluateJavascript(cleanPlayerScript, null)
                                }
                            }

                            loadUrl(state.url)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
