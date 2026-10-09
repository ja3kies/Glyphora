package com.glyphora.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glyphora.GlyphoraApplication
import com.glyphora.data.repository.DocumentRepository
import com.glyphora.domain.model.ReaderFontFamily
import com.glyphora.domain.model.ReaderSettings
import com.glyphora.domain.model.ReaderThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: DocumentRepository = GlyphoraApplication.instance.documentRepository
) : ViewModel() {

    val settings: StateFlow<ReaderSettings> = repository.getReaderSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReaderSettings())

    fun updateThemeMode(mode: ReaderThemeMode) {
        viewModelScope.launch {
            repository.updateReaderSettings(settings.value.copy(themeMode = mode))
        }
    }

    fun updateFontSize(sizeSp: Float) {
        viewModelScope.launch {
            repository.updateReaderSettings(settings.value.copy(fontSizeSp = sizeSp))
        }
    }

    fun updateFontFamily(family: ReaderFontFamily) {
        viewModelScope.launch {
            repository.updateReaderSettings(settings.value.copy(fontFamily = family))
        }
    }

    fun updateLineHeight(lineHeight: Float) {
        viewModelScope.launch {
            repository.updateReaderSettings(settings.value.copy(lineHeightMultiplier = lineHeight))
        }
    }

    fun updateMargin(marginDp: Int) {
        viewModelScope.launch {
            repository.updateReaderSettings(settings.value.copy(horizontalMarginDp = marginDp))
        }
    }
}
