package com.dolo.core.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "url_history")
data class HistoryEntity(
    @PrimaryKey
    val url: String,
    val title: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
