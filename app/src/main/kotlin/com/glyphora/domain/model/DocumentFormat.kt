package com.glyphora.domain.model

enum class DocumentFormat(val extension: String, val mimeType: String, val displayName: String) {
    EPUB("epub", "application/epub+zip", "EPUB"),
    PDF("pdf", "application/pdf", "PDF"),
    HTML("html", "text/html", "HTML"),
    TXT("txt", "text/plain", "TXT");

    companion object {
        fun fromExtension(ext: String?): DocumentFormat {
            val normalized = ext?.lowercase()?.trim().orEmpty()
            return when {
                normalized.endsWith("epub") -> EPUB
                normalized.endsWith("pdf") -> PDF
                normalized.endsWith("html") || normalized.endsWith("htm") -> HTML
                else -> TXT
            }
        }

        fun fromMimeType(mime: String?): DocumentFormat {
            return when (mime) {
                "application/epub+zip" -> EPUB
                "application/pdf" -> PDF
                "text/html" -> HTML
                "text/plain" -> TXT
                else -> TXT
            }
        }
    }
}
