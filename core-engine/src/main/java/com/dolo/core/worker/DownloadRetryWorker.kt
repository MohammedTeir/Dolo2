package com.dolo.core.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dolo.core.db.DownloadDao
import com.dolo.core.repository.DownloadRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class DownloadRetryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val downloadDao: DownloadDao,
    private val downloadRepository: DownloadRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Find any downloads interrupted in DOWNLOADING or QUEUED status
            val pendingDownloads = downloadDao.getDownloadsByStatus("DOWNLOADING") +
                    downloadDao.getDownloadsByStatus("QUEUED")

            for (download in pendingDownloads) {
                val params = com.dolo.core.model.DownloadParams(
                    id = download.id,
                    url = download.url,
                    formatId = download.formatId,
                    outputDir = download.filePath?.let { java.io.File(it).parent } ?: applicationContext.cacheDir.absolutePath,
                    fileName = download.filePath?.let { java.io.File(it).name },
                    isAudioOnly = download.isAudioOnly,
                    audioFormat = download.audioFormat,
                    audioBitrate = download.audioBitrate,
                    trimStartSeconds = download.trimStartSeconds,
                    trimEndSeconds = download.trimEndSeconds,
                    useCookies = download.useCookies
                )
                downloadRepository.queueDownload(params)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
