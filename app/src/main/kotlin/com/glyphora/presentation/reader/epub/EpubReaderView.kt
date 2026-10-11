package com.glyphora.presentation.reader.epub

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebView
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebViewClient
import android.webkit.RenderProcessGoneDetail
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.glyphora.core.theme.DarkReaderBackground
import com.glyphora.core.theme.DarkReaderOnSurface
import com.glyphora.core.theme.SepiaBackground
import com.glyphora.core.theme.SepiaOnSurface
import com.glyphora.domain.model.ReaderSettings
import com.glyphora.domain.model.ReaderThemeMode
import com.glyphora.data.parsers.EpubResource
import java.lang.ref.WeakReference

private class ProgressWebView(context: Context) : WebView(context) {
    var onProgressChanged: (Float) -> Unit = {}
    var initialProgress: Float = 0f
    var loadedHtml: String? = null
    private var lastReportedProgress = -1f
    private var restoringPosition = true
    private var currentProgress = 0f
    private var hasRestoredOnce = false
    private var diagnosticLoadId = 0L
    private var diagnosticFinished = false
    private var destroyed = false

    fun beginDiagnosticLoad(): Long {
        diagnosticLoadId += 1
        diagnosticFinished = false
        return diagnosticLoadId
    }

    fun logPageFinishedDiagnostics(url: String?) {
        if (diagnosticFinished || isDestroyedCompat()) return
        diagnosticFinished = true
        val loadId = diagnosticLoadId
        logMeasurement("onPageFinished", url)
        val weakView = WeakReference(this)
        if (WebViewFeature.isFeatureSupported(WebViewFeature.VISUAL_STATE_CALLBACK)) {
            try {
                WebViewCompat.postVisualStateCallback(this, loadId, object : WebViewCompat.VisualStateCallback {
                    override fun onComplete(requestId: Long) {
                        val view = weakView.get() ?: return
                        if (view.isDestroyedCompat() || view.diagnosticLoadId != requestId) return
                        view.logMeasurement("postVisualStateCallback", url)
                        view.postOnAnimation {
                            val nextFrameView = weakView.get() ?: return@postOnAnimation
                            if (!nextFrameView.isDestroyedCompat() && nextFrameView.diagnosticLoadId == requestId) {
                                nextFrameView.logMeasurement("nextFrame", url)
                            }
                        }
                    }
                })
            } catch (error: RuntimeException) {
                Log.w("EpubWebView", "Visual state callback unavailable at runtime: ${error.javaClass.simpleName}")
                postFallbackFrameMeasurement(weakView, loadId, url)
            }
        } else {
            Log.i("EpubWebView", "Visual state callback unsupported; using next-frame measurement")
            postFallbackFrameMeasurement(weakView, loadId, url)
        }
    }

    private fun postFallbackFrameMeasurement(viewRef: WeakReference<ProgressWebView>, loadId: Long, url: String?) {
        postOnAnimation {
            val view = viewRef.get() ?: return@postOnAnimation
            if (!view.isDestroyedCompat() && view.diagnosticLoadId == loadId) view.logMeasurement("fallbackNextFrame", url)
        }
    }

    private fun logMeasurement(stage: String, url: String?) {
        Log.i(
            "EpubWebView",
            "$stage: url=$url, width=$width, height=$height, measuredWidth=$measuredWidth, " +
                "measuredHeight=$measuredHeight, attached=$isAttachedToWindow, visibility=$visibility, " +
                "contentHeight=$contentHeight, scale=$scale"
        )
    }

    private fun isDestroyedCompat(): Boolean = destroyed

    override fun destroy() {
        destroyed = true
        super.destroy()
    }

    fun prepareForLoad() {
        restoringPosition = true
    }

    override fun onScrollChanged(scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int) {
        super.onScrollChanged(scrollX, scrollY, oldScrollX, oldScrollY)
        if (restoringPosition) return
        val scrollRange = (contentHeight * scale - height).coerceAtLeast(0f)
        val progress = if (scrollRange == 0f) 0f else (scrollY / scrollRange).coerceIn(0f, 1f)
        if (progress != lastReportedProgress) {
            lastReportedProgress = progress
            currentProgress = progress
            onProgressChanged(progress)
        }
    }

    fun restorePositionAfterLoad() {
        restoringPosition = true
        post {
            val scrollRange = (contentHeight * scale - height).coerceAtLeast(0f)
            val progress = (if (hasRestoredOnce) currentProgress else initialProgress).coerceIn(0f, 1f)
            scrollTo(scrollX, (scrollRange * progress).toInt())
            currentProgress = progress
            hasRestoredOnce = true
            lastReportedProgress = progress
            restoringPosition = false
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EpubReaderView(
    htmlContent: String,
    contentPath: String,
    resources: Map<String, EpubResource>,
    settings: ReaderSettings,
    modifier: Modifier = Modifier,
    initialProgress: Float = 0f,
    onScrollProgressChanged: (Float) -> Unit = {}
) {
    val assetLoader = remember(resources) { EpubAssetLoader.create(resources) }
    val baseUrl = remember(contentPath) { EpubAssetLoader.baseUrlFor(contentPath) }

    LaunchedEffect(htmlContent, contentPath) {
        Log.i(
            "EpubWebView",
            "EPUB content received: htmlLength=${htmlContent.length}, contentPath=$contentPath, " +
                "hasHtml=${Regex("<\\s*html\\b", RegexOption.IGNORE_CASE).containsMatchIn(htmlContent)}, " +
                "hasHead=${Regex("<\\s*head\\b", RegexOption.IGNORE_CASE).containsMatchIn(htmlContent)}, " +
                "hasBody=${Regex("<\\s*body\\b", RegexOption.IGNORE_CASE).containsMatchIn(htmlContent)}, " +
                "scriptTags=${Regex("<\\s*script\\b", RegexOption.IGNORE_CASE).findAll(htmlContent).count()}"
        )
    }

    // Calcul des styles CSS injectés pour respecter la personnalisation utilisateur tout en conservant le CSS d'origine
    val injectedStyles = remember(settings) {
        val (bgColor, textColor) = when (settings.themeMode) {
            ReaderThemeMode.DARK -> "#121212" to "#E0E0E0"
            ReaderThemeMode.SEPIA -> "#FBF0D9" to "#5F4B32"
            else -> "#FFFFFF" to "#1A1C1E"
        }
        val fontFamily = when (settings.fontFamily) {
            com.glyphora.domain.model.ReaderFontFamily.SERIF -> "serif"
            com.glyphora.domain.model.ReaderFontFamily.MONOSPACE -> "monospace"
            com.glyphora.domain.model.ReaderFontFamily.SANS_SERIF -> "sans-serif"
        }

        """
        <style>
            body {
                background-color: $bgColor !important;
                color: $textColor !important;
                font-size: ${settings.fontSizeSp}px !important;
                font-family: $fontFamily !important;
                line-height: ${settings.lineHeightMultiplier} !important;
                padding: ${settings.horizontalMarginDp}px !important;
                word-wrap: break-word !important;
            }
            img {
                max-width: 100% !important;
                height: auto !important;
            }
        </style>
        """.trimIndent()
    }

    val styledHtml = remember(htmlContent, injectedStyles) {
        if (htmlContent.contains("<head>", ignoreCase = true)) {
            htmlContent.replace("<head>", "<head>$injectedStyles", ignoreCase = true)
        } else {
            "$injectedStyles$htmlContent"
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                ProgressWebView(context).apply {
                    this.settings.javaScriptEnabled = false
                    this.settings.blockNetworkLoads = true
                    this.settings.loadWithOverviewMode = true
                    this.settings.useWideViewPort = false
                    this.settings.builtInZoomControls = true
                    this.settings.displayZoomControls = false
                    this.settings.allowFileAccess = false
                    this.settings.allowContentAccess = false
                    Log.i(
                        "EpubWebView",
                        "WebView created: javaScriptEnabled=${this.settings.javaScriptEnabled}, " +
                            "blockNetworkLoads=${this.settings.blockNetworkLoads}"
                    )
                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView,
                            request: WebResourceRequest
                        ) = assetLoader.shouldInterceptRequest(request.url)

                        override fun onPageFinished(view: WebView, url: String?) {
                            val progressView = view as? ProgressWebView ?: return
                            progressView.logPageFinishedDiagnostics(url)
                            progressView.restorePositionAfterLoad()
                        }

                        override fun onReceivedError(
                            view: WebView,
                            request: WebResourceRequest,
                            error: android.webkit.WebResourceError
                        ) {
                            Log.e(
                                "EpubWebView",
                                "Load failed (mainFrame=${request.isForMainFrame}, code=${error.errorCode}): ${request.url} — ${error.description}"
                            )
                        }

                        override fun onReceivedHttpError(
                            view: WebView,
                            request: WebResourceRequest,
                            errorResponse: android.webkit.WebResourceResponse
                        ) {
                            Log.w(
                                "EpubWebView",
                                "HTTP ${errorResponse.statusCode} for EPUB resource: ${request.url}"
                            )
                        }

                        override fun onRenderProcessGone(
                            view: WebView,
                            detail: RenderProcessGoneDetail
                        ): Boolean {
                            Log.e(
                                "EpubWebView",
                                "Renderer exited (crashed=${detail.didCrash()}, priority=${detail.rendererPriorityAtExit()})"
                            )
                            return true
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                            Log.w(
                                "EpubWebView",
                                "Console ${message.messageLevel()} at ${message.sourceId()}:${message.lineNumber()}: ${message.message()}"
                            )
                            return true
                        }
                    }
                }
            },
            update = { webView ->
                webView.onProgressChanged = onScrollProgressChanged
                webView.initialProgress = initialProgress
                if (webView.loadedHtml != styledHtml) {
                    webView.loadedHtml = styledHtml
                    webView.beginDiagnosticLoad()
                    webView.prepareForLoad()
                    Log.i(
                        "EpubWebView",
                        "loadDataWithBaseURL: baseUrl=$baseUrl, htmlLength=${htmlContent.length}, contentPath=$contentPath"
                    )
                    webView.loadDataWithBaseURL(baseUrl, styledHtml, "text/html", "UTF-8", baseUrl)
                }
            }
        )
    }
}
