package com.glyphora.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE documents ADD COLUMN chapterProgress REAL NOT NULL DEFAULT 0.0"
        )
    }
}

@Database(
    entities = [
        DocumentEntity::class,
        BookmarkEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class GlyphoraDatabase : RoomDatabase() {

    abstract fun documentDao(): DocumentDao

    abstract fun bookmarkDao(): BookmarkDao
}
