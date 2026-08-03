package com.dolo.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: HistoryEntity)

    @Query("SELECT * FROM url_history ORDER BY timestamp DESC LIMIT 20")
    fun observeRecentHistory(): Flow<List<HistoryEntity>>

    @Query("DELETE FROM url_history WHERE url = :url")
    suspend fun deleteHistory(url: String)

    @Query("DELETE FROM url_history")
    suspend fun clearHistory()
}
