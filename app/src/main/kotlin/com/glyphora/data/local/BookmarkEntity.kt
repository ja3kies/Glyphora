package com.glyphora.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bookmarks",
    indices = [Index(value = ["documentId"])]
)
data class BookmarkEntity(
    @PrimaryKey
    val id: String,
    val documentId: String,
    val title: String,
    val pageIndex: Int = 0,
    val scrollOffset: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
