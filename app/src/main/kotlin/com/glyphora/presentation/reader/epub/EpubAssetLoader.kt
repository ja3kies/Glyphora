package com.glyphora.presentation.reader.epub

import android.net.Uri
import android.util.Log
import android.webkit.WebResourceResponse
import androidx.webkit.WebViewAssetLoader
import com.glyphora.data.parsers.EpubResource
import com.glyphora.data.parsers.EpubResourcePathResolver
import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentHashMap

internal object EpubAssetLoader {
    private const val TAG = "EpubWebView"
    private const val PATH_PREFIX = "/epub/"

    fun create(resources: Map<String, EpubResource>): WebViewAssetLoader =
        WebViewAssetLoader.Builder()
            .addPathHandler(PATH_PREFIX, ResourcePathHandler(resources))
            .build()

    fun baseUrlFor(contentPath: String): String {
        val builder = Uri.Builder()
            .scheme("https")
            .authority(WebViewAssetLoader.DEFAULT_DOMAIN)
            .appendPath("epub")
        contentPath.split('/').filter(String::isNotEmpty).forEach(builder::appendPath)
        return builder.build().toString()
    }

    private class ResourcePathHandler(
        private val resources: Map<String, EpubResource>
    ) : WebViewAssetLoader.PathHandler {
        private val reportedMissingPaths = ConcurrentHashMap.newKeySet<String>()

        override fun handle(path: String): WebResourceResponse {
            val archivePath = EpubResourcePathResolver.normalizeArchivePath(path)
            val resource = archivePath?.let(resources::get)
            if (resource == null) {
                if (reportedMissingPaths.add(path)) {
                    Log.w(TAG, "EPUB resource missing from archive: $path")
                }
                return WebResourceResponse(
                    "text/plain",
                    "UTF-8",
                    404,
                    "Not Found",
                    mapOf("Cache-Control" to "no-store"),
                    ByteArrayInputStream(ByteArray(0))
                )
            }
            return WebResourceResponse(
                resource.mimeType,
                resource.charset,
                ByteArrayInputStream(resource.bytes)
            )
        }
    }
}
