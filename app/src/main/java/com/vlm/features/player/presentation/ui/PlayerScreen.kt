@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.media3.common.util.UnstableApi::class)

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.vlm.core.data.BackgroundStreamExtractor
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
    val context = LocalContext.current
    var extractedStreamUrl by remember { mutableStateOf<String?>(null) }
    var isExtractingStream by remember { mutableStateOf(true) }
    var playerErrorEncountered by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = state.url) {
        if (state.url.isNotEmpty()) {
            isExtractingStream = true
            playerErrorEncountered = false
            val streamUrl = BackgroundStreamExtractor.extractStreamUrl(context, state.url)
            extractedStreamUrl = streamUrl
            isExtractingStream = false
        }
    }

    LaunchedEffect(key1 = true) {
        effect.collect { eff ->
            when (eff) {
                PlayerEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    // Configure ExoPlayer with Referer & User-Agent headers required by streaming servers
    val exoPlayer = remember(context) {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 13; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            .setDefaultRequestProperties(mapOf("Referer" to "https://idflix.top/", "Origin" to "https://idflix.top"))

        val mediaSourceFactory = DefaultMediaSourceFactory(httpDataSourceFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        super.onPlayerError(error)
                        playerErrorEncountered = true
                    }
                })
            }
    }

    LaunchedEffect(extractedStreamUrl) {
        extractedStreamUrl?.let { streamUrl ->
            try {
                exoPlayer.setMediaItem(MediaItem.fromUri(streamUrl))
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            } catch (_: Exception) {
                playerErrorEncountered = true
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (!playerErrorEncountered) "ExoPlayer HD Stream" else "IdFlix Player",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = Color(0xFF4CAF50),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "100% NO ADS",
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
            if (isExtractingStream) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFFE50914))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Preparing HD Video Stream...",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (!playerErrorEncountered && extractedStreamUrl != null) {
                // Native ExoPlayer PlayerView
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                            keepScreenOn = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Clean full-screen ad-free video player fallback so it NEVER stays blank
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
                                override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?): Boolean {
                                    return false // Block ad popup windows
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                                    val lowerUrl = request?.url?.toString()?.lowercase() ?: ""
                                    if (lowerUrl.contains("doubleclick") || lowerUrl.contains("google-analytics") ||
                                        lowerUrl.contains("adservice") || lowerUrl.contains("popads") ||
                                        lowerUrl.contains("exoclick") || lowerUrl.contains("histats") ||
                                        lowerUrl.contains("popunder") || lowerUrl.contains("adsterra")
                                    ) {
                                        return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream("".toByteArray()))
                                    }
                                    return super.shouldInterceptRequest(view, request)
                                }

                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val urlStr = request?.url?.toString()?.lowercase() ?: ""
                                    if (!urlStr.contains("idflix.top") && !urlStr.contains("mcloud") &&
                                        !urlStr.contains("streamtape") && !urlStr.contains("embed") &&
                                        !urlStr.contains("vidsrc") && !urlStr.contains("player")
                                    ) {
                                        return true // Block external redirects
                                    }
                                    return false
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    val cleanScript = """
                                        javascript:(function() {
                                            window.open = function() { return null; };
                                            function isolatePlayer() {
                                                var pemutar = document.querySelector('[data-pemutar], .data-pemutar, iframe[data-src], div[data-src]');
                                                if (pemutar) {
                                                    var pSrc = pemutar.getAttribute('data-src') || pemutar.getAttribute('data-pemutar');
                                                    if (pSrc) {
                                                        if (pSrc.indexOf('//') === 0) pSrc = 'https:' + pSrc;
                                                        if (pemutar.tagName === 'IFRAME' && (!pemutar.src || pemutar.src === 'about:blank')) {
                                                            pemutar.src = pSrc;
                                                        }
                                                    }
                                                }
                                                var iframe = document.querySelectorAll('iframe[src*="embed"], iframe[src*="player"], iframe[src*="stream"], iframe[data-src], #player, .player');
                                                if (iframe && iframe.length > 0) {
                                                    for (var k = 0; k < iframe.length; k++) {
                                                        iframe[k].style.position = 'fixed';
                                                        iframe[k].style.top = '0';
                                                        iframe[k].style.left = '0';
                                                        iframe[k].style.width = '100vw';
                                                        iframe[k].style.height = '100vh';
                                                        iframe[k].style.zIndex = '999999';
                                                    }
                                                }
                                                var ads = document.querySelectorAll('iframe[src*="ads"], .ad, .ads, .advertisement, div[id*="ad-"], div[class*="popup"], .modal-backdrop, .popunder, a[target="_blank"]');
                                                for (var i = 0; i < ads.length; i++) {
                                                    ads[i].style.display = 'none';
                                                    try { ads[i].remove(); } catch(e) {}
                                                }
                                            }
                                            isolatePlayer();
                                            setInterval(isolatePlayer, 500);
                                        })();
                                    """.trimIndent()
                                    view?.evaluateJavascript(cleanScript, null)
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
