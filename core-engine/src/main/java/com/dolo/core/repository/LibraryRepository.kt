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
                    val thumbnailPath = item.thumbnailPath
                    if (!thumbnailPath.isNullOrBlank()) {
                        File(thumbnailPath).delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            libraryItemDao.deleteItem(id)
        }
    }

    suspend fun deleteItems(ids: List<String>, deleteFilesOnDisk: Boolean = true) {
        for (id in ids) {
            deleteItem(id, deleteFilesOnDisk)
        }
    }

    suspend fun renameItem(id: String, newTitle: String): Boolean {
        val item = libraryItemDao.getItemById(id) ?: return false
        val currentFile = File(item.filePath)
        if (!currentFile.exists()) {
            libraryItemDao.updateTitleAndPath(id, newTitle, item.filePath)
            return true
        }

        val parentDir = currentFile.parentFile ?: return false
        val ext = currentFile.extension
        val cleanName = com.dolo.core.util.FileNamer.sanitize(newTitle)
        val newFileName = if (ext.isNotEmpty()) "$cleanName.$ext" else cleanName
        val targetFile = com.dolo.core.util.FileNamer.handleCollision(parentDir, newFileName)

        val renamed = currentFile.renameTo(targetFile)
        val finalPath = if (renamed) targetFile.absolutePath else item.filePath
        libraryItemDao.updateTitleAndPath(id, newTitle, finalPath)
        return true
    }
}
