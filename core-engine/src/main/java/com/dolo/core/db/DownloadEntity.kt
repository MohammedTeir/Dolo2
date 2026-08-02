package com.dolo.core.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey
    val id: String,
    val url: String,
    val title: String? = null,
    val thumbnailUrl: String? = null,
    val uploader: String? = null,
    val durationSeconds: Int? = null,
    val formatId: String? = null,
    val formatLabel: String? = null,
    val isAudioOnly: Boolean = false,
    val audioFormat: String? = null,
    val audioBitrate: Int? = null,
    val estimatedSizeBytes: Long? = null,
    val downloadedSizeBytes: Long = 0L,
    val status: String, // QUEUED, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELLED
    val progress: Float = 0f,
    val filePath: String? = null,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val trimStartSeconds: Float? = null,
    val trimEndSeconds: Float? = null,
    val useCookies: Boolean = false,
    val playlistId: String? = null,
    val playlistIndex: Int? = null,
    val priority: Int = 0,
    val downloadSpeedBytes: Long = 0L
)
