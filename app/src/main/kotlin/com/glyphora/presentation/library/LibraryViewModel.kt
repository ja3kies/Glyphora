package com.glyphora.presentation.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glyphora.GlyphoraApplication
import com.glyphora.data.repository.DocumentRepository
import com.glyphora.domain.model.Document
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val repository: DocumentRepository = GlyphoraApplication.instance.documentRepository
) : ViewModel() {

    val documents: StateFlow<List<Document>> = repository.getDocuments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** L'import n'ouvre pas le document : l'appelant reçoit le résultat (succès ou échec) pour informer l'utilisateur. */
    fun importDocument(uri: Uri, onResult: (Result<Document>) -> Unit = {}) {
        viewModelScope.launch {
            onResult(repository.importDocument(uri))
        }
    }

    fun deleteDocument(id: String) {
        viewModelScope.launch {
            repository.deleteDocument(id)
        }
    }
}
