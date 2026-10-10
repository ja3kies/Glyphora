package com.glyphora.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val uriString: String,
    val format: String,
    val fileSize: Long = 0L,
    val lastOpenedTimestamp: Long = 0L,
    val readingProgressPercent: Float = 0f,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val author: String? = null
)
