package com.glyphora.presentation.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glyphora.GlyphoraApplication
import com.glyphora.data.repository.DocumentRepository
import com.glyphora.domain.model.Bookmark
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BookmarkListItem(
    val bookmark: Bookmark,
    val documentTitle: String
)

class BookmarksViewModel(
    private val repository: DocumentRepository =
        GlyphoraApplication.instance.documentRepository
) : ViewModel() {

    val bookmarks: StateFlow<List<BookmarkListItem>> =
        combine(repository.getAllBookmarks(), repository.getDocuments()) { bookmarks, documents ->
            val titles = documents.associate { it.id to it.title }
            bookmarks.mapNotNull { bookmark ->
                titles[bookmark.documentId]?.let { title ->
                    BookmarkListItem(bookmark, title)
                }
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun removeBookmark(
        id: String,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val success = try {
                repository.removeBookmark(id)
                true
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
            onResult(success)
        }
    }
}
