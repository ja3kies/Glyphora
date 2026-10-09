package com.glyphora.data.repository

import android.net.Uri
import com.glyphora.domain.model.Bookmark
import com.glyphora.domain.model.Document
import com.glyphora.domain.model.ReaderSettings
import com.glyphora.domain.model.ReadingProgress
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    fun getDocuments(): Flow<List<Document>>

    suspend fun getDocumentById(id: String): Document?

    suspend fun importDocument(uri: Uri): Result<Document>

    suspend fun deleteDocument(id: String)

    suspend fun updateReadingProgress(progress: ReadingProgress)

    fun getBookmarks(documentId: String): Flow<List<Bookmark>>

    suspend fun addBookmark(bookmark: Bookmark)

    suspend fun removeBookmark(bookmarkId: String)

    fun getReaderSettings(): Flow<ReaderSettings>

    suspend fun updateReaderSettings(settings: ReaderSettings)
}
