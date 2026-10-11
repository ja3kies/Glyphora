package com.glyphora.presentation.bookmarks

import com.glyphora.domain.model.Bookmark
import com.glyphora.domain.model.Document
import com.glyphora.domain.model.DocumentFormat
import com.glyphora.domain.model.positionLabel

/** Un marque-page prêt à afficher : le document d'origine et la position sont déjà résolus. */
data class BookmarkListItem(
    val bookmarkId: String,
    val documentId: String,
    val documentTitle: String,
    val format: DocumentFormat,
    val pageIndex: Int,
    val positionLabel: String,
    val timestamp: Long
)

/**
 * Associe chaque marque-page à son document, du plus récent au plus ancien.
 * Les marque-pages sans document correspondant sont ignorés : ils ne mèneraient nulle part.
 */
internal fun buildBookmarkItems(bookmarks: List<Bookmark>, documents: List<Document>): List<BookmarkListItem> {
    val byId = documents.associateBy { it.id }
    return bookmarks.mapNotNull { bookmark ->
        val document = byId[bookmark.documentId] ?: return@mapNotNull null
        BookmarkListItem(
            bookmarkId = bookmark.id,
            documentId = document.id,
            documentTitle = document.title,
            format = document.format,
            pageIndex = bookmark.pageIndex,
            positionLabel = positionLabel(document.format, bookmark.pageIndex, document.totalPages),
            timestamp = bookmark.timestamp
        )
    }.sortedByDescending { it.timestamp }
}
