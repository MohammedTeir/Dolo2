package com.dolo.dolo.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.dolo.core.db.DownloadDao
import com.dolo.core.db.LibraryItemDao
import com.dolo.core.db.LibraryItemEntity
import com.dolo.core.engine.DownloadRequestBuilder
import com.dolo.core.model.DownloadParams
import com.yausername.youtubedl_android.DownloadProgressCallback
import com.yausername.youtubedl_android.YoutubeDL
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class DownloadService : Service() {

    @Inject
    lateinit var youtubeDL: YoutubeDL

    @Inject
    lateinit var downloadDao: DownloadDao

    @Inject
    lateinit var libraryItemDao: LibraryItemDao

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val CHANNEL_ID = "dolo_download_channel"
    private val NOTIFICATION_ID = 1001

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Dolo Engine active", 0f, 0, 0))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_DOWNLOAD" -> {
                val downloadId = intent.getStringExtra("DOWNLOAD_ID") ?: return START_NOT_STICKY
                val url = intent.getStringExtra("URL") ?: return START_NOT_STICKY
                val formatId = intent.getStringExtra("FORMAT_ID")
                val outputDir = intent.getStringExtra("OUTPUT_DIR") ?: cacheDir.absolutePath
                val fileName = intent.getStringExtra("FILE_NAME")
                val isAudioOnly = intent.getBooleanExtra("IS_AUDIO_ONLY", false)
                val audioFormat = intent.getStringExtra("AUDIO_FORMAT") ?: "mp3"
                val audioBitrate = intent.getIntExtra("AUDIO_BITRATE", 192)
                val useCookies = intent.getBooleanExtra("USE_COOKIES", false)
                val cookiesPath = intent.getStringExtra("COOKIES_PATH")
                val trimStart = if (intent.hasExtra("TRIM_START")) intent.getFloatExtra("TRIM_START", 0f) else null
                val trimEnd = if (intent.hasExtra("TRIM_END")) intent.getFloatExtra("TRIM_END", 0f) else null

                val params = DownloadParams(
                    id = downloadId,
                    url = url,
                    formatId = formatId,
                    outputDir = outputDir,
                    fileName = fileName,
                    isAudioOnly = isAudioOnly,
                    audioFormat = audioFormat,
                    audioBitrate = audioBitrate,
                    trimStartSeconds = trimStart,
                    trimEndSeconds = trimEnd,
                    useCookies = useCookies,
                    cookiesPath = cookiesPath
                )

                executeDownload(params)
            }
            "CANCEL_DOWNLOAD" -> {
                val downloadId = intent.getStringExtra("DOWNLOAD_ID")
                if (downloadId != null) {
                    youtubeDL.destroyProcessById(downloadId)
                    serviceScope.launch {
                        downloadDao.updateStatus(downloadId, "CANCELLED")
                    }
                    updateNotification("Download cancelled", 0f, downloadId = downloadId)
                }
            }
            "PAUSE_DOWNLOAD" -> {
                val downloadId = intent.getStringExtra("DOWNLOAD_ID")
                if (downloadId != null) {
                    youtubeDL.destroyProcessById(downloadId)
                    serviceScope.launch {
                        downloadDao.updateStatus(downloadId, "PAUSED")
                    }
                    updateNotification("Download paused", 0f, downloadId = downloadId)
                }
            }
        }
        return START_STICKY
    }

    private fun executeDownload(params: DownloadParams) {
        serviceScope.launch {
            try {
                downloadDao.updateStatus(params.id, "DOWNLOADING")
                updateNotification("Downloading...", 0f, downloadId = params.id)

                val request = DownloadRequestBuilder.buildRequest(params)

                youtubeDL.execute(request, params.id) { progress, etaInSeconds, line ->
                    serviceScope.launch {
                        downloadDao.updateProgress(params.id, 0L, progress)
                        updateNotification("Downloading: ${progress.toInt()}%", progress, downloadId = params.id)
                    }
                }

                // Download complete
                downloadDao.updateStatus(params.id, "COMPLETED")
                val completedFile = File(params.outputDir, params.fileName ?: "download.mp4")

                // Add to library
                val libraryItem = LibraryItemEntity(
                    id = params.id,
                    sourceUrl = params.url,
                    title = params.fileName ?: "Download",
                    filePath = completedFile.absolutePath,
                    fileSizeBytes = if (completedFile.exists()) completedFile.length() else 0L,
                    isAudio = params.isAudioOnly
                )
                libraryItemDao.insertLibraryItem(libraryItem)

                updateNotification("Download complete", 100f, downloadId = params.id)
            } catch (e: Exception) {
                e.printStackTrace()
                downloadDao.updateStatus(params.id, "FAILED", errorMessage = e.localizedMessage)
                updateNotification("Download failed", 0f, downloadId = params.id)
            }
        }
    }

    private fun buildNotification(text: String, progress: Float, downloadedBytes: Long = 0, totalBytes: Long = 0, downloadId: String? = null): android.app.Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Dolo Downloader")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress.toInt(), progress == 0f && text.contains("Downloading"))
            .setOngoing(progress < 100f && !text.contains("cancelled") && !text.contains("failed") && !text.contains("paused"))

        if (downloadId != null && text.startsWith("Downloading")) {
            val pauseIntent = Intent(this, DownloadService::class.java).apply {
                action = "PAUSE_DOWNLOAD"
                putExtra("DOWNLOAD_ID", downloadId)
            }
            val pausePendingIntent = android.app.PendingIntent.getService(
                this,
                downloadId.hashCode(),
                pauseIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            val cancelIntent = Intent(this, DownloadService::class.java).apply {
                action = "CANCEL_DOWNLOAD"
                putExtra("DOWNLOAD_ID", downloadId)
            }
            val cancelPendingIntent = android.app.PendingIntent.getService(
                this,
                downloadId.hashCode() + 1,
                cancelIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent)
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent)
        }

        return builder.build()
    }

    private fun updateNotification(text: String, progress: Float, downloadId: String? = null) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(text, progress, downloadId = downloadId))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Download Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
