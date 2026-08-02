package com.dolo.core.repository

import android.content.Context
import android.content.Intent
import com.dolo.core.db.DownloadDao
import com.dolo.core.db.DownloadEntity
import com.dolo.core.engine.YtDlpExtractor
import com.dolo.core.model.DownloadParams
import com.dolo.core.model.VideoMetadata
import com.dolo.core.util.StorageChecker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val extractor: YtDlpExtractor,
    private val downloadDao: DownloadDao,
    private val settingsRepository: SettingsRepository
) {

    suspend fun extractInfo(url: String): Result<VideoMetadata> {
        return extractor.extractInfo(url)
    }

    suspend fun checkDuplicate(url: String, formatId: String?): DownloadEntity? {
        return downloadDao.findCompletedDuplicate(url, formatId)
    }

    suspend fun queueDownload(params: DownloadParams): String = withContext(Dispatchers.IO) {
        val downloadId = params.id.ifEmpty { java.util.UUID.randomUUID().toString() }

        val entity = DownloadEntity(
            id = downloadId,
            url = params.url,
            formatId = params.formatId,
            isAudioOnly = params.isAudioOnly,
            audioFormat = params.audioFormat,
            audioBitrate = params.audioBitrate,
            status = "QUEUED",
            filePath = if (params.fileName != null) "${params.outputDir}/${params.fileName}" else null,
            trimStartSeconds = params.trimStartSeconds,
            trimEndSeconds = params.trimEndSeconds,
            useCookies = params.useCookies
        )

        downloadDao.insertDownload(entity)
        startDownloadService(downloadId, params)

        downloadId
    }

    fun observeQueue(): Flow<List<DownloadEntity>> {
        return downloadDao.observeAllDownloads()
    }

    suspend fun getDownload(id: String): DownloadEntity? {
        return downloadDao.getDownloadById(id)
    }

    suspend fun pauseDownload(id: String) {
        downloadDao.updateStatus(id, "PAUSED")
        sendServiceCommand("PAUSE_DOWNLOAD", id)
    }

    suspend fun resumeDownload(id: String) {
        downloadDao.updateStatus(id, "QUEUED")
        sendServiceCommand("RESUME_DOWNLOAD", id)
    }

    suspend fun cancelDownload(id: String) {
        downloadDao.updateStatus(id, "CANCELLED")
        sendServiceCommand("CANCEL_DOWNLOAD", id)
    }

    suspend fun moveDownloadUp(id: String) = withContext(Dispatchers.IO) {
        val all = downloadDao.observeAllDownloads().first()
        val index = all.indexOfFirst { it.id == id }
        if (index > 0) {
            val current = all[index]
            val above = all[index - 1]
            downloadDao.updatePriority(current.id, above.priority + 1)
        }
    }

    suspend fun moveDownloadDown(id: String) = withContext(Dispatchers.IO) {
        val all = downloadDao.observeAllDownloads().first()
        val index = all.indexOfFirst { it.id == id }
        if (index >= 0 && index < all.size - 1) {
            val current = all[index]
            val below = all[index + 1]
            downloadDao.updatePriority(current.id, below.priority - 1)
        }
    }

    fun hasEnoughStorageSpace(path: String, requiredBytes: Long): Boolean {
        return StorageChecker.hasEnoughSpace(path, requiredBytes)
    }

    private fun sendServiceCommand(action: String, downloadId: String) {
        try {
            val intent = Intent().apply {
                setClassName(context.packageName, "com.dolo.dolo.service.DownloadService")
                this.action = action
                putExtra("DOWNLOAD_ID", downloadId)
            }
            context.startService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startDownloadService(downloadId: String, params: DownloadParams) {
        try {
            val intent = Intent().apply {
                setClassName(context.packageName, "com.dolo.dolo.service.DownloadService")
                action = "START_DOWNLOAD"
                putExtra("DOWNLOAD_ID", downloadId)
                putExtra("URL", params.url)
                putExtra("FORMAT_ID", params.formatId)
                putExtra("OUTPUT_DIR", params.outputDir)
                putExtra("FILE_NAME", params.fileName)
                putExtra("IS_AUDIO_ONLY", params.isAudioOnly)
                putExtra("AUDIO_FORMAT", params.audioFormat)
                putExtra("AUDIO_BITRATE", params.audioBitrate ?: 192)
                putExtra("USE_COOKIES", params.useCookies)
                putExtra("COOKIES_PATH", params.cookiesPath)
                if (params.trimStartSeconds != null) putExtra("TRIM_START", params.trimStartSeconds)
                if (params.trimEndSeconds != null) putExtra("TRIM_END", params.trimEndSeconds)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
