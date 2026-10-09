package com.glyphora.data.parsers

import android.content.Context
import android.net.Uri
import com.glyphora.domain.model.DocumentFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class EpubChapter(
    val id: String,
    val title: String,
    val contentHref: String,
    val contentHtml: String
)

data class EpubBookData(
    val metadata: ParsedMetadata,
    val chapters: List<EpubChapter>
)

class EpubParser : DocumentParser {
    override val supportedFormat: DocumentFormat = DocumentFormat.EPUB

    override suspend fun parseMetadata(context: Context, uri: Uri): ParsedMetadata = withContext(Dispatchers.IO) {
        val bookData = parseEpub(context, uri)
        bookData.metadata
    }

    suspend fun parseEpub(context: Context, uri: Uri): EpubBookData = withContext(Dispatchers.IO) {
        var opfPath = ""
        val zipFiles = mutableMapOf<String, ByteArray>()

        // 1. Lire les entrées ZIP du fichier EPUB
        context.contentResolver.openInputStream(uri)?.use { stream ->
            ZipInputStream(stream).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val bytes = zis.readBytes()
                        zipFiles[entry.name] = bytes
                    }
                    entry = zis.nextEntry
                }
            }
        } ?: throw IllegalArgumentException("Impossible d'ouvrir le fichier EPUB")

        // 2. Extraire container.xml pour trouver le chemin de l'OPF
        val containerXml = zipFiles["META-INF/container.xml"]
        if (containerXml != null) {
            opfPath = extractOpfPath(ByteArrayInputStream(containerXml))
        }
        if (opfPath.isEmpty()) {
            opfPath = zipFiles.keys.firstOrNull { it.endsWith(".opf", ignoreCase = true) } ?: ""
        }

        val opfBytes = zipFiles[opfPath] ?: throw IllegalStateException("Fichier OPF introuvable dans l'EPUB")
        val opfDir = if (opfPath.contains("/")) opfPath.substringBeforeLast("/") + "/" else ""

        // 3. Parser les métadonnées et la structure manifest/spine depuis l'OPF
        val (metadata, manifest, spine) = parseOpf(ByteArrayInputStream(opfBytes))

        // 4. Charger le contenu HTML des chapitres ordonnés par la spine
        val chapters = mutableListOf<EpubChapter>()
        spine.forEachIndexed { index, itemId ->
            val href = manifest[itemId]
            if (href != null) {
                val fullPath = opfDir + href
                val contentBytes = zipFiles[fullPath]
                if (contentBytes != null) {
                    val html = String(contentBytes, Charsets.UTF_8)
                    chapters.add(
                        EpubChapter(
                            id = itemId,
                            title = "Chapitre ${index + 1}",
                            contentHref = href,
                            contentHtml = html
                        )
                    )
                }
            }
        }

        val updatedMetadata = metadata.copy(
            totalPagesOrChapters = chapters.size.coerceAtLeast(1)
        )

        EpubBookData(
            metadata = updatedMetadata,
            chapters = chapters
        )
    }

    private fun extractOpfPath(stream: InputStream): String {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(stream, "UTF-8")
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name.equals("rootfile", ignoreCase = true)) {
                val fullPath = parser.getAttributeValue(null, "full-path")
                if (!fullPath.isNullOrBlank()) return fullPath
            }
            eventType = parser.next()
        }
        return ""
    }

    private data class OpfResult(
        val metadata: ParsedMetadata,
        val manifest: Map<String, String>,
        val spine: List<String>
    )

    private fun parseOpf(stream: InputStream): OpfResult {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(stream, "UTF-8")

        var title = "Livre sans titre"
        var author: String? = null
        val manifest = mutableMapOf<String, String>()
        val spine = mutableListOf<String>()

        var eventType = parser.eventType
        var currentTag = ""

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    if (currentTag.equals("item", ignoreCase = true)) {
                        val id = parser.getAttributeValue(null, "id")
                        val href = parser.getAttributeValue(null, "href")
                        if (!id.isNullOrBlank() && !href.isNullOrBlank()) {
                            manifest[id] = href
                        }
                    } else if (currentTag.equals("itemref", ignoreCase = true)) {
                        val idref = parser.getAttributeValue(null, "idref")
                        if (!idref.isNullOrBlank()) {
                            spine.add(idref)
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim().orEmpty()
                    if (text.isNotEmpty()) {
                        when {
                            currentTag.contains("title", ignoreCase = true) -> title = text
                            currentTag.contains("creator", ignoreCase = true) -> author = text
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return OpfResult(
            metadata = ParsedMetadata(
                title = title,
                author = author,
                totalPagesOrChapters = spine.size.coerceAtLeast(1)
            ),
            manifest = manifest,
            spine = spine
        )
    }
}
