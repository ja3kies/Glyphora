package com.glyphora.presentation.reader

import android.app.Application
import android.net.Uri
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed class ReaderContentState {
    data object Loading : ReaderContentState()
    data class Error(val message: String) : ReaderContentState()
    data class Epub(val data: EpubBookData, val currentChapterIndex: Int = 0) : ReaderContentState()
    data class Pdf(val engine: PdfRendererEngine, val totalPages: Int, val initialPage: Int) : ReaderContentState()
    data class Html(val data: HtmlDocumentData) : ReaderContentState()
    data class Txt(val data: TxtDocumentData, val initialPage: Int) : ReaderContentState()
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

    private var activePdfEngine: PdfRendererEngine? = null

    fun loadDocument(documentId: String, initialPage: Int? = null) {
        viewModelScope.launch {
            _contentState.value = ReaderContentState.Loading
            val doc = repository.getDocumentById(documentId)
            if (doc == null) {
                _contentState.value = ReaderContentState.Error("Document introuvable")
                return@launch
            }
            val requestedPage = (initialPage ?: doc.currentPage).coerceAtLeast(0)
            _currentDocument.value = doc.copy(currentPage = requestedPage)
            val uri = Uri.parse(doc.uriString)
            val context = getApplication<Application>()

            try {
                when (doc.format) {
                    DocumentFormat.EPUB -> {
                        val bookData = EpubParser().parseEpub(context, uri)
                        val chapterIndex = if (bookData.chapters.isNotEmpty()) {
                            requestedPage.coerceIn(0, bookData.chapters.lastIndex)
                        } else {
                            0
                        }
                        _contentState.value = ReaderContentState.Epub(
                            data = bookData,
                            currentChapterIndex = chapterIndex
                        )
                        onPageChanged(chapterIndex, bookData.chapters.size)
                    }
                    DocumentFormat.PDF -> {
                        val engine = PdfRendererEngine(context, uri).apply { initialize() }
                        activePdfEngine?.close()
                        activePdfEngine = engine
                        val pageIndex = requestedPage.coerceAtMost(
                            (engine.pageCount - 1).coerceAtLeast(0)
                        )
                        _contentState.value = ReaderContentState.Pdf(
                            engine = engine,
                            totalPages = engine.pageCount,
                            initialPage = pageIndex
                        )
                        onPageChanged(pageIndex, engine.pageCount)
                    }
                    DocumentFormat.HTML -> {
                        val htmlData = HtmlParser().parseHtml(context, uri)
                        _contentState.value = ReaderContentState.Html(htmlData)
                    }
                    DocumentFormat.TXT -> {
                        val txtData = TxtEngine().loadDocument(context, uri)
                        val pageIndex = requestedPage.coerceAtMost(
                            (txtData.pages.size - 1).coerceAtLeast(0)
                        )
                        _contentState.value = ReaderContentState.Txt(
                            data = txtData,
                            initialPage = pageIndex
                        )
                        onPageChanged(pageIndex, txtData.pages.size)
                    }
                }
            } catch (e: Exception) {
                _contentState.value = ReaderContentState.Error("Erreur d'ouverture : ${e.localizedMessage}")
            }
        }
    }

    fun onPageChanged(page: Int, total: Int) {
        val doc = _currentDocument.value ?: return
        val safeTotal = total.coerceAtLeast(0)
        val safePage = if (safeTotal > 0) page.coerceIn(0, safeTotal - 1) else 0
        val percent = if (safeTotal > 0) {
            (safePage + 1).toFloat() / safeTotal.toFloat()
        } else {
            0f
        }

        _currentDocument.value = doc.copy(
            currentPage = safePage,
            totalPages = safeTotal,
            readingProgressPercent = percent
        )

        viewModelScope.launch {
            repository.updateReadingProgress(
                ReadingProgress(
                    documentId = doc.id,
                    currentPage = safePage,
                    totalPages = safeTotal,
                    progressPercent = percent
                )
            )
        }
    }

    fun addBookmark(title: String, page: Int, onResult: (Boolean) -> Unit = {}) {
        val doc = _currentDocument.value
        if (doc == null) {
            onResult(false)
            return
        }

        viewModelScope.launch {
            val result = runCatching {
                repository.addBookmark(
                    Bookmark(
                        id = UUID.randomUUID().toString(),
                        documentId = doc.id,
                        title = title,
                        pageIndex = page.coerceAtLeast(0)
                    )
                )
            }
            onResult(result.isSuccess)
        }
    }

    override fun onCleared() {
        super.onCleared()
        activePdfEngine?.close()
        activePdfEngine = null
    }
}
