package com.glyphora

import android.app.Application
import com.glyphora.data.repository.DocumentRepository
import com.glyphora.data.repository.DocumentRepositoryImpl

class GlyphoraApplication : Application() {

    // Service locator simple et robuste pour éviter toute dépendance d'injection lourde
    lateinit var documentRepository: DocumentRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        documentRepository = DocumentRepositoryImpl(this)
    }

    companion object {
        lateinit var instance: GlyphoraApplication
            private set
    }
}
