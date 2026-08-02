package com.dolo.core.engine

import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetadataEmbedder @Inject constructor(
    private val youtubeDL: YoutubeDL
) {
    /**
     * Embeds metadata tags and thumbnail into an existing media file using yt-dlp / ffmpeg.
     * Note: Primary embedding is performed directly during download via yt-dlp's --embed-thumbnail
     * and --add-metadata flags in DownloadRequestBuilder.
     */
    suspend fun embedMetadata(
        mediaFile: File,
        thumbnailFile: File? = null,
        title: String? = null,
        artist: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!mediaFile.exists()) {
                return@withContext Result.failure(IllegalArgumentException("Media file does not exist"))
            }

            val request = YoutubeDLRequest(mediaFile.absolutePath)
            request.addOption("--add-metadata")
            if (thumbnailFile != null && thumbnailFile.exists()) {
                request.addOption("--embed-thumbnail")
            }

            youtubeDL.execute(request, null)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
