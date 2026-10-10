package com.glyphora.presentation.reader.epub

import android.annotation.SuppressLint
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
import androidx.compose.runtime.rememberUpdatedState

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EpubReaderView(
    htmlContent: String,
    settings: ReaderSettings,
    modifier: Modifier = Modifier,
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
    val currentScrollCallback = rememberUpdatedState(onScrollProgressChanged)
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
                WebView(context).apply {
                    this.settings.javaScriptEnabled = false
                    this.settings.blockNetworkLoads = true
                    this.settings.loadWithOverviewMode = true
                    this.settings.useWideViewPort = false
                    this.settings.builtInZoomControls = true
                    this.settings.displayZoomControls = false
                    webViewClient = WebViewClient()
                    var lastReportedPercent = -1
                    setOnScrollChangeListener { view, _, scrollY, _, _ ->
                        val webView = view as WebView
                        val contentHeight = (webView.contentHeight * webView.scale).toInt()
                        val range = (contentHeight - webView.height).coerceAtLeast(0)
                        val progress = if (range > 0) {
                            (scrollY.toFloat() / range).coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                        val percent = (progress * 100).toInt()
                        if (percent != lastReportedPercent) {
                            lastReportedPercent = percent
                            currentScrollCallback.value(progress)
                        }
                    }
                }
            },
            update = { webView ->
                if (webView.tag != styledHtml) {
                    webView.tag = styledHtml
                    webView.loadDataWithBaseURL(null, styledHtml, "text/html", "UTF-8", null)
                }
            }
        )
    }
}
