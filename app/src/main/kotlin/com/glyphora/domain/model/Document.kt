package com.glyphora.domain.model

data class Document(
    val id: String,
    val title: String,
    val uriString: String,
    val format: DocumentFormat,
    val fileSize: Long = 0L,
    val lastOpenedTimestamp: Long = 0L,
    val readingProgressPercent: Float = 0f,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val author: String? = null
)
