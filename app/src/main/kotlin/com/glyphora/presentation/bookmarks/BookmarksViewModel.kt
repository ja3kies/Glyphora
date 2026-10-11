package com.glyphora.presentation.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glyphora.GlyphoraApplication
import com.glyphora.data.repository.DocumentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** `bookmarks` vaut null tant que la première lecture de la base n'est pas terminée (évite d'afficher « vide » à tort). */
class BookmarksViewModel(
    repository: DocumentRepository = GlyphoraApplication.instance.documentRepository
) : ViewModel() {

    val bookmarks: StateFlow<List<BookmarkListItem>?> = combine(
        repository.getAllBookmarks(),
        repository.getDocuments()
    ) { bookmarks, documents ->
        buildBookmarkItems(bookmarks, documents)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
