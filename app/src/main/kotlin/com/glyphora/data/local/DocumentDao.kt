package com.glyphora.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Query("SELECT * FROM documents ORDER BY lastOpenedTimestamp DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: String): DocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocument(id: String)

    @Query("""
        UPDATE documents
        SET lastOpenedTimestamp = :timestamp
        WHERE id = :id
    """)
    suspend fun updateLastOpened(id: String, timestamp: Long)

    @Query("""
        UPDATE documents
        SET readingProgressPercent = :progress,
            currentPage = :currentPage,
            totalPages = :totalPages
        WHERE id = :id
    """)
    suspend fun updateReadingProgress(
        id: String,
        progress: Float,
        currentPage: Int,
        totalPages: Int
    )
}
