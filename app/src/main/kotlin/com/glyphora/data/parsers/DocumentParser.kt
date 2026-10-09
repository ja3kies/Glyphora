package com.glyphora.data.parsers

import android.content.Context
import android.net.Uri
import com.glyphora.domain.model.DocumentFormat

data class ParsedMetadata(
    val title: String,
    val author: String? = null,
    val totalPagesOrChapters: Int = 1,
    val tableOfContents: List<TocItem> = emptyList()
)

data class TocItem(
    val title: String,
    val targetIndex: Int,
    val subItems: List<TocItem> = emptyList()
)

interface DocumentParser {
    val supportedFormat: DocumentFormat

    suspend fun parseMetadata(context: Context, uri: Uri): ParsedMetadata
}
