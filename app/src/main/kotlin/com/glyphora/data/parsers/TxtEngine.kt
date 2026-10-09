package com.glyphora.data.parsers

import android.content.Context
import android.net.Uri
import com.glyphora.domain.model.DocumentFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.nio.charset.Charset

data class TxtDocumentData(
    val title: String,
    val fullText: String,
    val pages: List<String>
)

class TxtEngine : DocumentParser {
    override val supportedFormat: DocumentFormat = DocumentFormat.TXT

    override suspend fun parseMetadata(context: Context, uri: Uri): ParsedMetadata = withContext(Dispatchers.IO) {
        val data = loadDocument(context, uri)
        ParsedMetadata(
            title = data.title,
            totalPagesOrChapters = data.pages.size.coerceAtLeast(1)
        )
    }

    suspend fun loadDocument(context: Context, uri: Uri, charsPerPage: Int = 2000): TxtDocumentData = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.readBytes()
        } ?: throw IllegalArgumentException("Impossible d'ouvrir le fichier texte : $uri")

        val charset = detectCharset(bytes)
        val text = String(bytes, charset)

        val title = uri.lastPathSegment?.substringAfterLast("/")?.substringBeforeLast(".") ?: "Document Texte"

        // Découpage virtuel en pages selon la taille de bloc
        val pages = if (text.isBlank()) {
            listOf("")
        } else {
            text.chunked(charsPerPage)
        }

        TxtDocumentData(
            title = title,
            fullText = text,
            pages = pages
        )
    }

    private fun detectCharset(bytes: ByteArray): Charset {
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return Charsets.UTF_8
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return Charsets.UTF_16BE
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return Charsets.UTF_16LE
        }
        return Charsets.UTF_8
    }
}
