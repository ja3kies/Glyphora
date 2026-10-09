package com.glyphora.data.parsers

import android.content.Context
import android.net.Uri
import com.glyphora.domain.model.DocumentFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist

data class HtmlDocumentData(
    val title: String,
    val cleanHtml: String,
    val rawText: String
)

class HtmlParser : DocumentParser {
    override val supportedFormat: DocumentFormat = DocumentFormat.HTML

    override suspend fun parseMetadata(context: Context, uri: Uri): ParsedMetadata = withContext(Dispatchers.IO) {
        val data = parseHtml(context, uri)
        ParsedMetadata(
            title = data.title,
            totalPagesOrChapters = 1
        )
    }

    suspend fun parseHtml(context: Context, uri: Uri): HtmlDocumentData = withContext(Dispatchers.IO) {
        val rawContent = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.reader(Charsets.UTF_8).readText()
        } ?: throw IllegalArgumentException("Impossible d'ouvrir le fichier HTML : $uri")

        val document = Jsoup.parse(rawContent)
        val title = document.title().ifBlank {
            document.selectFirst("h1")?.text()
                ?: uri.lastPathSegment?.substringAfterLast("/")?.substringBeforeLast(".")
                ?: "Document HTML"
        }

        // Nettoyage sécurisé pour la lecture hors-ligne
        val cleanBody = Jsoup.clean(
            document.body().html(),
            Safelist.relaxed()
                .addTags("hr", "abbr")
                .addAttributes(":all", "style", "class")
        )

        val rawText = document.text()

        HtmlDocumentData(
            title = title,
            cleanHtml = cleanBody,
            rawText = rawText
        )
    }
}
