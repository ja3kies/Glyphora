package com.glyphora.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import android.util.Log
import androidx.room.withTransaction
import com.glyphora.data.local.GlyphoraDatabase
import com.glyphora.data.local.toDomain
import com.glyphora.data.local.toEntity
import com.glyphora.data.parsers.EpubParser
import com.glyphora.data.parsers.HtmlParser
import com.glyphora.data.parsers.PdfRendererEngine
import com.glyphora.data.parsers.TxtEngine
import com.glyphora.domain.model.Bookmark
import com.glyphora.domain.model.Document
import com.glyphora.domain.model.DocumentFormat
import com.glyphora.domain.model.ReaderFontFamily
import com.glyphora.domain.model.ReaderSettings
import com.glyphora.domain.model.ReaderThemeMode
import com.glyphora.domain.model.ReadingProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "glyphora_settings")

class DocumentRepositoryImpl(
    private val context: Context,
    private val database: GlyphoraDatabase
) : DocumentRepository {

    private val documentDao = database.documentDao()
    private val bookmarkDao = database.bookmarkDao()

    private val epubParser = EpubParser()
    private val htmlParser = HtmlParser()
    private val txtEngine = TxtEngine()

    // Clés de préférences DataStore
    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("reader_theme_mode")
        val FONT_SIZE = floatPreferencesKey("reader_font_size")
        val FONT_FAMILY = stringPreferencesKey("reader_font_family")
        val LINE_HEIGHT = floatPreferencesKey("reader_line_height")
        val HORIZONTAL_MARGIN = intPreferencesKey("reader_horizontal_margin")
    }

    override fun getDocuments(): Flow<List<Document>> =
        documentDao.getAllDocuments()
            .map { list -> list.map { it.toDomain() } }
            .catch { e ->
                Log.e(TAG, "Lecture de la bibliothèque impossible", e)
                emit(emptyList())
            }

    override suspend fun getDocumentById(id: String): Document? = withContext(Dispatchers.IO) {
        try {
            documentDao.getDocumentById(id)?.toDomain()
        } catch (e: Exception) {
            Log.e(TAG, "Lecture du document $id impossible", e)
            null
        }
    }

    override suspend fun importDocument(uri: Uri): Result<Document> = withContext(Dispatchers.IO) {
        runCatching {
            // Prendre la permission d'accès persistante pour Scoped Storage
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                // Ignore si déjà accordé ou non applicable
            }

            // Document déjà connu : on conserve son identifiant, sa progression et ses signets
            documentDao.getDocumentByUri(uri.toString())?.let { existing ->
                val now = System.currentTimeMillis()
                documentDao.updateLastOpened(existing.id, now)
                return@runCatching existing.copy(lastOpenedTimestamp = now).toDomain()
            }

            var fileName = "Document inconnu"
            var fileSize = 0L

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }

            val format = DocumentFormat.fromExtension(fileName)
            val docId = UUID.randomUUID().toString()

            val metadata = when (format) {
                DocumentFormat.EPUB -> runCatching { epubParser.parseMetadata(context, uri) }.getOrNull()
                DocumentFormat.PDF -> {
                    PdfRendererEngine(context, uri).use { engine ->
                        runCatching { engine.parseMetadata(context, uri) }.getOrNull()
                    }
                }
                DocumentFormat.HTML -> runCatching { htmlParser.parseMetadata(context, uri) }.getOrNull()
                DocumentFormat.TXT -> runCatching { txtEngine.parseMetadata(context, uri) }.getOrNull()
            }

            val doc = Document(
                id = docId,
                title = metadata?.title ?: fileName.substringBeforeLast("."),
                uriString = uri.toString(),
                format = format,
                fileSize = fileSize,
                lastOpenedTimestamp = System.currentTimeMillis(),
                currentPage = 0,
                totalPages = metadata?.totalPagesOrChapters ?: 1,
                author = metadata?.author
            )

            documentDao.insertDocument(doc.toEntity())

            doc
        }
    }

    override suspend fun deleteDocument(id: String) {
        withContext(Dispatchers.IO) {
            try {
                // Signets et document supprimés ensemble, ou pas du tout
                database.withTransaction {
                    bookmarkDao.deleteBookmarksForDocument(id)
                    documentDao.deleteDocument(id)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Suppression du document $id impossible", e)
            }
        }
    }

    override suspend fun updateReadingProgress(progress: ReadingProgress) {
        withContext(Dispatchers.IO) {
            try {
                database.withTransaction {
                    documentDao.updateReadingProgress(
                        id = progress.documentId,
                        progress = progress.progressPercent,
                        currentPage = progress.currentPage,
                        totalPages = progress.totalPages
                    )
                    documentDao.updateLastOpened(progress.documentId, System.currentTimeMillis())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Enregistrement de la progression impossible", e)
            }
        }
    }

    override fun getBookmarks(documentId: String): Flow<List<Bookmark>> =
        bookmarkDao.getBookmarks(documentId)
            .map { list -> list.map { it.toDomain() } }
            .catch { e ->
                Log.e(TAG, "Lecture des signets impossible", e)
                emit(emptyList())
            }

    override fun getAllBookmarks(): Flow<List<Bookmark>> =
        bookmarkDao.getAllBookmarks()
            .map { list -> list.map { it.toDomain() } }
            .catch { e ->
                Log.e(TAG, "Lecture de tous les signets impossible", e)
                emit(emptyList())
            }

    override suspend fun addBookmark(bookmark: Bookmark) {
        withContext(Dispatchers.IO) {
            bookmarkDao.insertBookmark(bookmark.toEntity())
        }
    }

    override suspend fun removeBookmark(bookmarkId: String) {
        withContext(Dispatchers.IO) {
            bookmarkDao.deleteBookmark(bookmarkId)
        }
    }

    override fun getReaderSettings(): Flow<ReaderSettings> {
        return context.dataStore.data.map { prefs ->
            val themeStr = prefs[PreferencesKeys.THEME_MODE] ?: ReaderThemeMode.SYSTEM.name
            val themeMode = runCatching { ReaderThemeMode.valueOf(themeStr) }.getOrDefault(ReaderThemeMode.SYSTEM)

            val fontStr = prefs[PreferencesKeys.FONT_FAMILY] ?: ReaderFontFamily.SERIF.name
            val fontFamily = runCatching { ReaderFontFamily.valueOf(fontStr) }.getOrDefault(ReaderFontFamily.SERIF)

            ReaderSettings(
                themeMode = themeMode,
                fontSizeSp = prefs[PreferencesKeys.FONT_SIZE] ?: 16f,
                fontFamily = fontFamily,
                lineHeightMultiplier = prefs[PreferencesKeys.LINE_HEIGHT] ?: 1.4f,
                horizontalMarginDp = prefs[PreferencesKeys.HORIZONTAL_MARGIN] ?: 16
            )
        }
    }

    override suspend fun updateReaderSettings(settings: ReaderSettings) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.THEME_MODE] = settings.themeMode.name
            prefs[PreferencesKeys.FONT_SIZE] = settings.fontSizeSp
            prefs[PreferencesKeys.FONT_FAMILY] = settings.fontFamily.name
            prefs[PreferencesKeys.LINE_HEIGHT] = settings.lineHeightMultiplier
            prefs[PreferencesKeys.HORIZONTAL_MARGIN] = settings.horizontalMarginDp
        }
    }

    private companion object {
        const val TAG = "DocumentRepository"
    }
}
