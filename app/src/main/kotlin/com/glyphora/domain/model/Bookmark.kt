package com.glyphora.domain.model

data class Bookmark(
    val id: String,
    val documentId: String,
    val title: String,
    val pageIndex: Int = 0,
    val scrollOffset: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
