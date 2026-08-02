package com.dolo.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLibraryItem(item: LibraryItemEntity)

    @Query("SELECT * FROM library_items ORDER BY downloadedAt DESC")
    fun observeAllLibraryItems(): Flow<List<LibraryItemEntity>>

    @Query("SELECT * FROM library_items WHERE id = :id")
    suspend fun getItemById(id: String): LibraryItemEntity?

    @Query("SELECT * FROM library_items WHERE sourceUrl = :sourceUrl LIMIT 1")
    suspend fun getItemBySourceUrl(sourceUrl: String): LibraryItemEntity?

    @Query("UPDATE library_items SET title = :newTitle, filePath = :newFilePath WHERE id = :id")
    suspend fun updateTitleAndPath(id: String, newTitle: String, newFilePath: String)

    @Query("DELETE FROM library_items WHERE id = :id")
    suspend fun deleteItem(id: String)
}
