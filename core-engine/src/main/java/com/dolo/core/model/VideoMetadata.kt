package com.dolo.core.model

data class VideoMetadata(
    val id: String,
    val title: String,
    val thumbnailUrl: String?,
    val uploader: String?,
    val durationSeconds: Int,
    val description: String?,
    val formats: List<FormatInfo>,
    val subtitles: List<SubtitleInfo> = emptyList(),
    val originalUrl: String,
    val isPlaylist: Boolean = false,
    val playlistCount: Int = 0
)
