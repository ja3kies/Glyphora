package com.glyphora.data.parsers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.glyphora.domain.model.DocumentFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable

class PdfRendererEngine(
    private val context: Context,
    private val uri: Uri
) : Closeable, DocumentParser {

    override val supportedFormat: DocumentFormat = DocumentFormat.PDF

    private var fileDescriptor: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null

    val isInitialized: Boolean
        get() = renderer != null

    suspend fun initialize() = withContext(Dispatchers.IO) {
        if (renderer == null) {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                ?: throw IllegalArgumentException("Impossible d'ouvrir le fichier PDF : $uri")
            fileDescriptor = pfd
            renderer = PdfRenderer(pfd)
        }
    }

    val pageCount: Int
        get() = renderer?.pageCount ?: 0

    override suspend fun parseMetadata(context: Context, uri: Uri): ParsedMetadata = withContext(Dispatchers.IO) {
        initialize()
        val total = pageCount
        ParsedMetadata(
            title = uri.lastPathSegment?.substringAfterLast("/")?.substringBeforeLast(".") ?: "Document PDF",
            totalPagesOrChapters = total
        )
    }

    suspend fun renderPage(pageIndex: Int, targetWidth: Int = 1080): Bitmap = withContext(Dispatchers.Default) {
        initialize()
        val currentRenderer = renderer ?: throw IllegalStateException("PdfRenderer non initialisé")
        if (pageIndex < 0 || pageIndex >= currentRenderer.pageCount) {
            throw IndexOutOfBoundsException("Page $pageIndex hors limites (0-${currentRenderer.pageCount - 1})")
        }

        val page = currentRenderer.openPage(pageIndex)
        val aspectRatio = page.height.toFloat() / page.width.toFloat()
        val targetHeight = (targetWidth * aspectRatio).toInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)

        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()

        bitmap
    }

    override fun close() {
        renderer?.close()
        renderer = null
        fileDescriptor?.close()
        fileDescriptor = null
    }
}
