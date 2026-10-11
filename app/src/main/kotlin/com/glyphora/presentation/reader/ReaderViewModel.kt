package com.glyphora.presentation.reader

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.glyphora.GlyphoraApplication
import com.glyphora.data.parsers.EpubBookData
import com.glyphora.data.parsers.EpubParser
import com.glyphora.data.parsers.HtmlDocumentData
import com.glyphora.data.parsers.HtmlParser
import com.glyphora.data.parsers.PdfRendererEngine
import com.glyphora.data.parsers.TxtDocumentData
import com.glyphora.data.parsers.TxtEngine
import com.glyphora.data.repository.DocumentRepository
import com.glyphora.domain.model.Bookmark
import com.glyphora.domain.model.Document
import com.glyphora.domain.model.DocumentFormat
import com.glyphora.domain.model.ReaderSettings
import com.glyphora.domain.model.ReadingProgress
import com.glyphora.domain.model.positionLabel
import com.glyphora.domain.model.progressFraction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed class ReaderContentState {
    data object Loading : ReaderContentState()
    data class Error(val message: String) : ReaderContentState()
    data class Epub(
        val data: EpubBookData,
        val currentChapterIndex: Int = 0,
        val initialChapterProgress: Float = 0f
    ) : ReaderContentState()
    data class Pdf(val engine: PdfRendererEngine, val totalPages: Int, val initialPage: Int) : ReaderContentState()
    data class Html(val data: HtmlDocumentData) : ReaderContentState()
    data class Txt(val data: TxtDocumentData, val initialPage: Int) : ReaderContentState()
}

/** Retours ponctuels affichés par l'écran de lecture (une seule fois, sans être rejoués à la rotation). */
sealed interface ReaderEvent {
    data class BookmarkAdded(val positionLabel: String) : ReaderEvent
    data class BookmarkAlreadyExists(val positionLabel: String) : ReaderEvent
    data object BookmarkFailed : ReaderEvent
}

class ReaderViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: DocumentRepository = GlyphoraApplication.instance.documentRepository
) : AndroidViewModel(application) {

    private val _contentState = MutableStateFlow<ReaderContentState>(ReaderContentState.Loading)
    val contentState: StateFlow<ReaderContentState> = _contentState.asStateFlow()

    private val _currentDocument = MutableStateFlow<Document?>(null)
    val currentDocument: StateFlow<Document?> = _currentDocument.asStateFlow()

    val settings: StateFlow<ReaderSettings> = repository.getReaderSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReaderSettings())

    private val _events = Channel<ReaderEvent>(Channel.BUFFERED)
    val events: Flow<ReaderEvent> = _events.receiveAsFlow()

    private var activePdfEngine: PdfRendererEngine? = null

    // Position réellement affichée (index à partir de 0), tenue à jour par onPageChanged
    private var currentPageIndex: Int = 0
    private var currentTotalPages: Int = 0
    private val epubProgressWriter = DebouncedProgressWriter<ReadingProgress>(
        scope = viewModelScope,
        debounceMillis = EPUB_PROGRESS_DEBOUNCE_MS,
        write = repository::updateReadingProgress
    )

    // La page demandée (ex. depuis un marque-page) ne vaut que pour la première ouverture :
    // un rechargement après un passage par les paramètres reprend la progression enregistrée.
    private var startPageConsumed = false

    fun loadDocument(documentId: String, requestedPage: Int? = null) {
        val startPageOverride = if (startPageConsumed) null else requestedPage
        startPageConsumed = true
        viewModelScope.launch {
            _contentState.value = ReaderContentState.Loading
            val doc = try {
                repository.getDocumentById(documentId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _contentState.value = ReaderContentState.Error(
                    "Impossible de récupérer le document : ${e.localizedMessage}"
                )
                return@launch
            }
            if (doc == null) {
                _contentState.value = ReaderContentState.Error("Document introuvable")
                return@launch
            }
            _currentDocument.value = doc
            val startPage = (startPageOverride ?: doc.currentPage).coerceAtLeast(0)
            currentPageIndex = startPage
            currentTotalPages = doc.totalPages
            val uri = Uri.parse(doc.uriString)
            val context = getApplication<Application>()

            try {
                when (doc.format) {
                    DocumentFormat.EPUB -> {
                        val bookData = EpubParser().parseEpub(context, uri)
                        if (bookData.chapters.isEmpty()) {
                            _contentState.value = ReaderContentState.Error(
                                "Ce fichier EPUB ne contient aucun chapitre lisible."
                            )
                            return@launch
                        }
                        _contentState.value = ReaderContentState.Epub(
                            data = bookData,
                            currentChapterIndex = startPage.coerceIn(0, bookData.chapters.lastIndex),
                            initialChapterProgress = if (startPageOverride != null) {
                                0f
                            } else {
                                doc.chapterProgress.coerceIn(0f, 1f)
                            }
                        )
                    }
                    DocumentFormat.PDF -> {
                        val engine = PdfRendererEngine(context, uri).apply { initialize() }
                        activePdfEngine?.close()
                        activePdfEngine = engine
                        _contentState.value = ReaderContentState.Pdf(
                            engine = engine,
                            totalPages = engine.pageCount,
                            initialPage = startPage
                        )
                    }
                    DocumentFormat.HTML -> {
                        val htmlData = HtmlParser().parseHtml(context, uri)
                        _contentState.value = ReaderContentState.Html(htmlData)
                    }
                    DocumentFormat.TXT -> {
                        val txtData = TxtEngine().loadDocument(context, uri)
                        _contentState.value = ReaderContentState.Txt(
                            data = txtData,
                            initialPage = startPage
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _contentState.value = ReaderContentState.Error("Erreur d'ouverture : ${e.localizedMessage}")
            }
        }
    }

    fun onPageChanged(page: Int, total: Int) {
        val doc = _currentDocument.value ?: return
        currentPageIndex = page
        currentTotalPages = total
        viewModelScope.launch {
            persistProgress(
                ReadingProgress(
                    documentId = doc.id,
                    currentPage = page,
                    totalPages = total,
                    progressPercent = progressFraction(page, total)
                )
            )
        }
    }

    fun onEpubProgressChanged(chapterIndex: Int, totalChapters: Int, progress: Float) {
        val doc = _currentDocument.value ?: return
        currentPageIndex = chapterIndex
        currentTotalPages = totalChapters
        val progress = ReadingProgress(
            documentId = doc.id,
            currentPage = chapterIndex,
            totalPages = totalChapters,
            progressPercent = progressFraction(chapterIndex, totalChapters),
            chapterProgress = progress.coerceIn(0f, 1f)
        )
        epubProgressWriter.schedule(progress)
    }

    suspend fun flushPendingProgress() {
        epubProgressWriter.flush()
    }

    private suspend fun persistProgress(progress: ReadingProgress) {
        repository.updateReadingProgress(progress)
    }

    fun addBookmark() {
        val doc = _currentDocument.value ?: return
        val pageIndex = currentPageIndex
        val total = if (currentTotalPages > 0) currentTotalPages else doc.totalPages
        val label = positionLabel(doc.format, pageIndex, total)
        viewModelScope.launch {
            try {
                val alreadySaved = repository.getBookmarks(doc.id).first()
                    .any { it.pageIndex == pageIndex }
                if (alreadySaved) {
                    _events.send(ReaderEvent.BookmarkAlreadyExists(label))
                    return@launch
                }
                repository.addBookmark(
                    Bookmark(
                        id = UUID.randomUUID().toString(),
                        documentId = doc.id,
                        title = label,
                        pageIndex = pageIndex
                    )
                )
                _events.send(ReaderEvent.BookmarkAdded(label))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ReaderViewModel", "Ajout du marque-page impossible", e)
                _events.send(ReaderEvent.BookmarkFailed)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        activePdfEngine?.close()
        activePdfEngine = null
    }

    private companion object {
        const val EPUB_PROGRESS_DEBOUNCE_MS = 500L
    }
}
