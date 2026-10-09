package com.glyphora.presentation.reader.html

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.glyphora.domain.model.ReaderSettings
import com.glyphora.domain.model.ReaderThemeMode

@Composable
fun HtmlReaderView(
    cleanHtml: String,
    settings: ReaderSettings,
    modifier: Modifier = Modifier
) {
    val styledHtml = remember(cleanHtml, settings) {
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
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body {
                    background-color: $bgColor;
                    color: $textColor;
                    font-size: ${settings.fontSizeSp}px;
                    font-family: $fontFamily;
                    line-height: ${settings.lineHeightMultiplier};
                    padding: ${settings.horizontalMarginDp}px;
                    word-wrap: break-word;
                }
                img { max-width: 100%; height: auto; }
            </style>
        </head>
        <body>
            $cleanHtml
        </body>
        </html>
        """.trimIndent()
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                this.settings.javaScriptEnabled = false
                this.settings.blockNetworkLoads = true
                this.settings.loadWithOverviewMode = true
                webViewClient = WebViewClient()
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, styledHtml, "text/html", "UTF-8", null)
        }
    )
}
