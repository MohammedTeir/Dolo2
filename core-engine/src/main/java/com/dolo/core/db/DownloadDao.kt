package com.dolo.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity)

    @Update
    suspend fun updateDownload(download: DownloadEntity)

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: String): DownloadEntity?

    @Query("SELECT * FROM downloads ORDER BY priority DESC, createdAt DESC")
    fun observeAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY priority DESC, createdAt DESC")
    suspend fun getDownloadsByStatus(status: String): List<DownloadEntity>

    @Query("SELECT * FROM downloads WHERE status IN (:statuses) ORDER BY priority DESC, createdAt DESC")
    suspend fun getDownloadsByStatuses(statuses: List<String>): List<DownloadEntity>

    @Query("SELECT * FROM downloads WHERE status = 'QUEUED' ORDER BY priority DESC, createdAt ASC LIMIT 1")
    suspend fun getTopQueuedDownload(): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE url = :url AND (:formatId IS NULL OR formatId = :formatId) AND status = 'COMPLETED' LIMIT 1")
    suspend fun findCompletedDuplicate(url: String, formatId: String?): DownloadEntity?

    @Query("UPDATE downloads SET status = :status, errorMessage = :errorMessage, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, errorMessage: String? = null, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE downloads SET downloadedSizeBytes = :downloadedBytes, progress = :progress, downloadSpeedBytes = :speed, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateProgress(id: String, downloadedBytes: Long, progress: Float, speed: Long = 0, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE downloads SET priority = :priority WHERE id = :id")
    suspend fun updatePriority(id: String, priority: Int)

    @Query("UPDATE downloads SET speedLimitKbps = :speedLimit WHERE id = :id")
    suspend fun updateSpeedLimit(id: String, speedLimit: Int?)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownload(id: String)
}
