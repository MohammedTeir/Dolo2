package com.dolo.core.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "library_items")
data class LibraryItemEntity(
    @PrimaryKey
    val id: String,
    val sourceUrl: String,
    val title: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val mimeType: String? = null,
    val thumbnailPath: String? = null,
    val durationSeconds: Int? = null,
    val uploader: String? = null,
    val formatLabel: String? = null,
    val isAudio: Boolean = false,
    val downloadedAt: Long = System.currentTimeMillis(),
    val playlistId: String? = null,
    val playlistIndex: Int? = null
)
