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

class ReaderViewModel(
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

    fun loadDocument(documentId: String) {
        viewModelScope.launch {
            _contentState.value = ReaderContentState.Loading
            val doc = repository.getDocumentById(documentId)
            if (doc == null) {
                _contentState.value = ReaderContentState.Error("Document introuvable")
                return@launch
            }
            _currentDocument.value = doc
            val uri = Uri.parse(doc.uriString)
            val context = getApplication<Application>()

            try {
                when (doc.format) {
                    DocumentFormat.EPUB -> {
                        val bookData = EpubParser().parseEpub(context, uri)
                        _contentState.value = ReaderContentState.Epub(
                            data = bookData,
                            currentChapterIndex = doc.currentPage.coerceIn(0, bookData.chapters.size - 1)
                        )
                    }
                    DocumentFormat.PDF -> {
                        val engine = PdfRendererEngine(context, uri).apply { initialize() }
                        activePdfEngine?.close()
                        activePdfEngine = engine
                        _contentState.value = ReaderContentState.Pdf(
                            engine = engine,
                            totalPages = engine.pageCount,
                            initialPage = doc.currentPage
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
                            initialPage = doc.currentPage
                        )
                    }
                }
            } catch (e: Exception) {
                _contentState.value = ReaderContentState.Error("Erreur d'ouverture : ${e.localizedMessage}")
            }
        }
    }

    fun onPageChanged(page: Int, total: Int) {
        val doc = _currentDocument.value ?: return
        val percent = if (total > 0) page.toFloat() / total.toFloat() else 0f
        viewModelScope.launch {
            repository.updateReadingProgress(
                ReadingProgress(
                    documentId = doc.id,
                    currentPage = page,
                    totalPages = total,
                    progressPercent = percent
                )
            )
        }
    }

    fun addBookmark(title: String, page: Int) {
        val doc = _currentDocument.value ?: return
        viewModelScope.launch {
            repository.addBookmark(
                Bookmark(
                    id = UUID.randomUUID().toString(),
                    documentId = doc.id,
                    title = title,
                    pageIndex = page
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        activePdfEngine?.close()
        activePdfEngine = null
    }
}
