package com.dolo.core.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DownloadEntity::class, LibraryItemEntity::class],
    version = 2,
    exportSchema = false
)
abstract class DoloDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    abstract fun libraryItemDao(): LibraryItemDao
}
