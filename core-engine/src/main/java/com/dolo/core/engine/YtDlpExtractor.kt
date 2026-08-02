package com.dolo.core.engine

import com.dolo.core.model.FormatInfo
import com.dolo.core.model.VideoMetadata
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.mapper.VideoFormat
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YtDlpExtractor @Inject constructor(
    private val youtubeDL: YoutubeDL
) {
    suspend fun extractInfo(url: String): Result<VideoMetadata> = withContext(Dispatchers.IO) {
        try {
            val videoInfo: VideoInfo = youtubeDL.getInfo(url)
            Result.success(mapMetadata(videoInfo, url))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun search(query: String, platform: String = "youtube", limit: Int = 10): Result<List<VideoMetadata>> = withContext(Dispatchers.IO) {
        try {
            val prefix = when (platform.lowercase()) {
                "youtube" -> "ytsearch"
                "soundcloud" -> "scsearch"
                else -> "ytsearch"
            }
            val searchUrl = "$prefix$limit:$query"
            val videoInfo: VideoInfo = youtubeDL.getInfo(searchUrl)
            
            // Search result might return a playlist (collection of entries)
            val results = if (videoInfo.formats == null && videoInfo.url == null) {
                // It's a collection of search results
                // We'd need to parse entries. For now, we'll return a single one or empty.
                // VideoInfo might not expose entries directly in this library version.
                emptyList()
            } else {
                listOf(mapMetadata(videoInfo, videoInfo.webpageUrl ?: ""))
            }
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapMetadata(videoInfo: VideoInfo, url: String): VideoMetadata {
        val formats = videoInfo.formats?.map { mapFormat(it) } ?: emptyList()
        return VideoMetadata(
            id = videoInfo.id ?: "",
            title = videoInfo.title ?: videoInfo.fulltitle ?: "Unknown Title",
            thumbnailUrl = videoInfo.thumbnail,
            uploader = videoInfo.uploader ?: videoInfo.uploaderId,
            durationSeconds = videoInfo.duration,
            description = videoInfo.description,
            formats = formats,
            originalUrl = url
        )
    }

    private fun mapFormat(format: VideoFormat): FormatInfo {
        val isAudioOnly = format.vcodec == "none" || format.vcodec == null
        val isVideoOnly = format.acodec == "none" || format.acodec == null

        val resolution = when {
            format.height > 0 && format.width > 0 -> "${format.width}x${format.height}"
            format.height > 0 -> "${format.height}p"
            format.formatNote != null -> format.formatNote
            else -> null
        }

        val fileSize = format.fileSize
        return FormatInfo(
            formatId = format.formatId ?: "best",
            ext = format.ext ?: "mp4",
            resolution = resolution,
            fps = format.fps,
            fileSizeBytes = if (fileSize > 0) fileSize else 0L,
            videoCodec = format.vcodec,
            audioCodec = format.acodec,
            isVideoOnly = isVideoOnly,
            isAudioOnly = isAudioOnly,
            formatNote = format.formatNote,
            url = format.url
        )
    }
}
