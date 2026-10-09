package com.glyphora.data.local

import com.glyphora.domain.model.Bookmark
import com.glyphora.domain.model.Document
import com.glyphora.domain.model.DocumentFormat

fun DocumentEntity.toDomain(): Document = Document(
    id = id,
    title = title,
    uriString = uriString,
    format = runCatching { DocumentFormat.valueOf(format) }.getOrDefault(DocumentFormat.TXT),
    fileSize = fileSize,
    lastOpenedTimestamp = lastOpenedTimestamp,
    readingProgressPercent = readingProgressPercent,
    currentPage = currentPage,
    totalPages = totalPages,
    author = author
)

fun Document.toEntity(): DocumentEntity = DocumentEntity(
    id = id,
    title = title,
    uriString = uriString,
    format = format.name,
    fileSize = fileSize,
    lastOpenedTimestamp = lastOpenedTimestamp,
    readingProgressPercent = readingProgressPercent,
    currentPage = currentPage,
    totalPages = totalPages,
    author = author
)

fun BookmarkEntity.toDomain(): Bookmark = Bookmark(
    id = id,
    documentId = documentId,
    title = title,
    pageIndex = pageIndex,
    scrollOffset = scrollOffset,
    timestamp = timestamp
)

fun Bookmark.toEntity(): BookmarkEntity = BookmarkEntity(
    id = id,
    documentId = documentId,
    title = title,
    pageIndex = pageIndex,
    scrollOffset = scrollOffset,
    timestamp = timestamp
)
