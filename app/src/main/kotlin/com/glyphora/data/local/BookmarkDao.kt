package com.glyphora.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {

    @Query("""
        SELECT * FROM bookmarks
        WHERE documentId = :documentId
        ORDER BY timestamp DESC
    """)
    fun getBookmarks(documentId: String): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :bookmarkId")
    suspend fun deleteBookmark(bookmarkId: String)

    @Query("DELETE FROM bookmarks WHERE documentId = :documentId")
    suspend fun deleteBookmarksForDocument(documentId: String)
}
