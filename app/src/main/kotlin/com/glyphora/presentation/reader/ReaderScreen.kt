package com.glyphora.presentation.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.glyphora.presentation.reader.epub.EpubReaderView
import com.glyphora.presentation.reader.html.HtmlReaderView
import com.glyphora.presentation.reader.pdf.PdfReaderView
import com.glyphora.presentation.reader.txt.TxtReaderView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    documentId: String,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: ReaderViewModel = viewModel()
) {
    val contentState by viewModel.contentState.collectAsState()
    val document by viewModel.currentDocument.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(documentId) {
        viewModel.loadDocument(documentId)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = document?.title ?: "Lecture",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.addBookmark("Marque-page", document?.currentPage ?: 0)
                            scope.launch {
                                snackbarHostState.showSnackbar("Marque-page ajouté")
                            }
                        }
                    ) {
                        Icon(Icons.Default.BookmarkAdd, contentDescription = "Ajouter un marque-page")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Paramètres de lecture")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = contentState) {
                is ReaderContentState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ReaderContentState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                is ReaderContentState.Epub -> {
                    val currentChapter = state.data.chapters.getOrNull(state.currentChapterIndex)
                    if (currentChapter != null) {
                        EpubReaderView(
                            htmlContent = currentChapter.contentHtml,
                            settings = settings,
                            onScrollProgressChanged = { progress ->
                                viewModel.onPageChanged(state.currentChapterIndex, state.data.chapters.size)
                            }
                        )
                    }
                }
                is ReaderContentState.Pdf -> {
                    PdfReaderView(
                        pdfEngine = state.engine,
                        initialPage = state.initialPage,
                        totalPages = state.totalPages,
                        onPageChanged = { page, total ->
                            viewModel.onPageChanged(page, total)
                        }
                    )
                }
                is ReaderContentState.Html -> {
                    HtmlReaderView(
                        cleanHtml = state.data.cleanHtml,
                        settings = settings
                    )
                }
                is ReaderContentState.Txt -> {
                    TxtReaderView(
                        pages = state.data.pages,
                        initialPage = state.initialPage,
                        settings = settings,
                        onPageChanged = { page, total ->
                            viewModel.onPageChanged(page, total)
                        }
                    )
                }
            }
        }
    }
}
