
package com.glyphora

import android.app.Application
import androidx.room.Room
import com.glyphora.data.local.GlyphoraDatabase
import com.glyphora.data.repository.DocumentRepository
import com.glyphora.data.repository.DocumentRepositoryImpl

class GlyphoraApplication : Application() {

    lateinit var database: GlyphoraDatabase
        private set

    lateinit var documentRepository: DocumentRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = Room.databaseBuilder(
            applicationContext,
            GlyphoraDatabase::class.java,
            "glyphora.db"
        ).build()

        documentRepository = DocumentRepositoryImpl(
            context = applicationContext,
            database = database
        )
    }

    companion object {
        lateinit var instance: GlyphoraApplication
            private set
    }
}
