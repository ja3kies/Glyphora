package com.glyphora.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        DocumentEntity::class,
        BookmarkEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GlyphoraDatabase : RoomDatabase() {

    abstract fun documentDao(): DocumentDao

    abstract fun bookmarkDao(): BookmarkDao
}
