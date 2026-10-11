package com.glyphora.data.parsers

import java.net.URI

/** Resolves EPUB hrefs to canonical, archive-relative paths without allowing paths above the ZIP root. */
object EpubResourcePathResolver {
    fun resolve(baseFilePath: String, href: String): String? {
        val escapedHref = href.replace(" ", "%20")
        val hrefUri = runCatching { URI(escapedHref) }.getOrNull() ?: return null
        if (hrefUri.isAbsolute || hrefUri.rawAuthority != null) return null
        val hrefPath = hrefUri.path.orEmpty()
        if (hrefPath.isEmpty()) return null

        val combinedPath = if (hrefPath.startsWith('/')) {
            hrefPath
        } else {
            "${baseFilePath.substringBeforeLast('/', "")}/$hrefPath"
        }
        return normalizeArchivePath(combinedPath)
    }

    /** Normalizes a path returned by the asset loader, without reparsing already-decoded path characters. */
    fun normalizeArchivePath(path: String): String? {
        val segments = mutableListOf<String>()
        for (segment in path.split('/')) {
            when (segment) {
                "", "." -> Unit
                ".." -> if (segments.isEmpty()) return null else segments.removeAt(segments.lastIndex)
                else -> segments += segment
            }
        }
        return segments.takeIf { it.isNotEmpty() }?.joinToString("/")
    }
}
