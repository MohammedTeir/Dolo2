package com.dolo.core.repository

import com.dolo.core.db.LibraryItemDao
import com.dolo.core.db.LibraryItemEntity
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepository @Inject constructor(
    private val libraryItemDao: LibraryItemDao
) {
    fun observeLibrary(): Flow<List<LibraryItemEntity>> {
        return libraryItemDao.observeAllLibraryItems()
    }

    suspend fun addItem(item: LibraryItemEntity) {
        libraryItemDao.insertLibraryItem(item)
    }

    suspend fun deleteItem(id: String, deleteFileOnDisk: Boolean = true) {
        val item = libraryItemDao.getItemById(id)
        if (item != null) {
            if (deleteFileOnDisk) {
                try {
                    File(item.filePath).delete()
                    if (!item.thumbnailPath.isNull_Empty()) {
                        File(item.thumbnailPath).delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            libraryItemDao.deleteItem(id)
        }
    }

    private fun String?.isNull_Empty(): Boolean = this == null || this.isEmpty()
}
