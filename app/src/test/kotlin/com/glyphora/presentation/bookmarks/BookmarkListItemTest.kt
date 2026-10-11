package com.glyphora.presentation.bookmarks

import com.glyphora.domain.model.Bookmark
import com.glyphora.domain.model.Document
import com.glyphora.domain.model.DocumentFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookmarkListItemTest {

    private val book = Document(
        id = "doc-1",
        title = "Les Misérables",
        uriString = "content://x/1",
        format = DocumentFormat.PDF,
        totalPages = 4
    )

    @Test
    fun `un marque-page retrouve son document et sa position`() {
        val items = buildBookmarkItems(
            bookmarks = listOf(Bookmark(id = "b1", documentId = "doc-1", title = "x", pageIndex = 1, timestamp = 10L)),
            documents = listOf(book)
        )

        assertEquals(1, items.size)
        assertEquals("doc-1", items[0].documentId)
        assertEquals("Les Misérables", items[0].documentTitle)
        assertEquals(1, items[0].pageIndex)
        assertEquals("Page 2 / 4", items[0].positionLabel)
    }

    @Test
    fun `les plus recents viennent en premier`() {
        val items = buildBookmarkItems(
            bookmarks = listOf(
                Bookmark(id = "old", documentId = "doc-1", title = "x", pageIndex = 0, timestamp = 1L),
                Bookmark(id = "new", documentId = "doc-1", title = "x", pageIndex = 2, timestamp = 2L)
            ),
            documents = listOf(book)
        )

        assertEquals(listOf("new", "old"), items.map { it.bookmarkId })
    }

    @Test
    fun `un marque-page sans document est ignore`() {
        val items = buildBookmarkItems(
            bookmarks = listOf(Bookmark(id = "b1", documentId = "absent", title = "x", pageIndex = 0)),
            documents = listOf(book)
        )

        assertTrue(items.isEmpty())
    }
}
