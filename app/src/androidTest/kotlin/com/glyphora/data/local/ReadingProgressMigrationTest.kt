package com.glyphora.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReadingProgressMigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseName = "reading-progress-migration-test.db"
    private var database: GlyphoraDatabase? = null

    @Before
    fun createVersionOneDatabase() {
        context.deleteDatabase(databaseName)
        context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null).use { db ->
            db.execSQL(
                """CREATE TABLE documents (
                    id TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    uriString TEXT NOT NULL,
                    format TEXT NOT NULL,
                    fileSize INTEGER NOT NULL,
                    lastOpenedTimestamp INTEGER NOT NULL,
                    readingProgressPercent REAL NOT NULL,
                    currentPage INTEGER NOT NULL,
                    totalPages INTEGER NOT NULL,
                    author TEXT
                )""".trimIndent()
            )
            db.execSQL(
                """CREATE TABLE bookmarks (
                    id TEXT NOT NULL PRIMARY KEY,
                    documentId TEXT NOT NULL,
                    title TEXT NOT NULL,
                    pageIndex INTEGER NOT NULL,
                    scrollOffset INTEGER NOT NULL,
                    timestamp INTEGER NOT NULL
                )""".trimIndent()
            )
            db.execSQL("CREATE INDEX index_bookmarks_documentId ON bookmarks(documentId)")
            db.execSQL(
                """INSERT INTO documents (
                    id, title, uriString, format, fileSize, lastOpenedTimestamp,
                    readingProgressPercent, currentPage, totalPages, author
                ) VALUES ('doc-1', 'Title', 'content://book', 'EPUB', 42, 123, 0.5, 2, 5, NULL)""".trimIndent()
            )
            db.version = 1
        }
    }

    @After
    fun closeDatabase() {
        database?.close()
        database = null
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migrationPreservesDocumentAndStoresNormalizedChapterProgress() = runBlocking {
        database = Room.databaseBuilder(context, GlyphoraDatabase::class.java, databaseName)
            .addMigrations(MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

        val dao = requireNotNull(database).documentDao()
        val migrated = requireNotNull(dao.getDocumentById("doc-1"))
        assertEquals("Title", migrated.title)
        assertEquals(2, migrated.currentPage)
        assertEquals(0f, migrated.chapterProgress, 0f)

        dao.updateReadingProgress(
            id = migrated.id,
            progress = migrated.readingProgressPercent,
            currentPage = migrated.currentPage,
            totalPages = migrated.totalPages,
            chapterProgress = 0.625f
        )

        assertEquals(0.625f, requireNotNull(dao.getDocumentById("doc-1")).chapterProgress, 0f)
    }
}
