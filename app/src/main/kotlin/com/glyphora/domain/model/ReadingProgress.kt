package com.glyphora.domain.model

data class ReadingProgress(
    val documentId: String,
    val currentPage: Int,
    val totalPages: Int,
    val scrollOffset: Int = 0,
    val progressPercent: Float = 0f
)
