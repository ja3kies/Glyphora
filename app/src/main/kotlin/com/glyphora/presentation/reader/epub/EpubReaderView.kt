package com.glyphora.presentation.reader.epub

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.glyphora.core.theme.DarkReaderBackground
import com.glyphora.core.theme.DarkReaderOnSurface
import com.glyphora.core.theme.SepiaBackground
import com.glyphora.core.theme.SepiaOnSurface
import com.glyphora.domain.model.ReaderSettings
import com.glyphora.domain.model.ReaderThemeMode

private class ProgressWebView(context: Context) : WebView(context) {
    var onProgressChanged: (Float) -> Unit = {}
    var initialProgress: Float = 0f
    var loadedHtml: String? = null
    private var lastReportedProgress = -1f
    private var restoringPosition = true
    private var currentProgress = 0f
    private var hasRestoredOnce = false

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
    settings: ReaderSettings,
    modifier: Modifier = Modifier,
    initialProgress: Float = 0f,
    onScrollProgressChanged: (Float) -> Unit = {}
) {
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
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String?) {
                            (view as? ProgressWebView)?.restorePositionAfterLoad()
                        }
                    }
                }
            },
            update = { webView ->
                webView.onProgressChanged = onScrollProgressChanged
                webView.initialProgress = initialProgress
                if (webView.loadedHtml != styledHtml) {
                    webView.loadedHtml = styledHtml
                    webView.prepareForLoad()
                    webView.loadDataWithBaseURL(null, styledHtml, "text/html", "UTF-8", null)
                }
            }
        )
    }
}
