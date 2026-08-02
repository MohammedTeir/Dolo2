package com.dolo.dolo.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.dolo.core.db.DownloadDao
import com.dolo.core.db.DownloadEntity
import com.dolo.core.db.LibraryItemDao
import com.dolo.core.db.LibraryItemEntity
import com.dolo.core.engine.DownloadRequestBuilder
import com.dolo.core.model.DownloadParams
import com.dolo.core.repository.SettingsRepository
import com.dolo.core.util.StorageResolver
import com.yausername.youtubedl_android.YoutubeDL
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@AndroidEntryPoint
class DownloadService : Service() {

    @Inject
    lateinit var youtubeDL: YoutubeDL

    @Inject
    lateinit var downloadDao: DownloadDao

    @Inject
    lateinit var libraryItemDao: LibraryItemDao

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = ConcurrentHashMap<String, Job>()
    
    private val CHANNEL_ID = "dolo_download_channel"
    private val NOTIFICATION_ID = 1001

    private var maxConcurrent = 2
    private var isWifiOnly = false
    private var isWifiConnected = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(title = "Dolo Downloader", text = "Dolo Engine active", progress = 0f))
        
        setupConnectivityListener()
        observeSettings()
        startQueueProcessor()
    }

    private fun setupConnectivityListener() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        isWifiConnected = cm.getNetworkCapabilities(cm.activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        
        cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val caps = cm.getNetworkCapabilities(network)
                isWifiConnected = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                processQueue()
            }

            override fun onLost(network: Network) {
                isWifiConnected = false
                if (isWifiOnly) {
                    pauseAllDownloads()
                }
            }
        })
    }

    private fun observeSettings() {
        serviceScope.launch {
            combine(
                settingsRepository.maxConcurrentDownloads,
                settingsRepository.isWifiOnly
            ) { max, wifi ->
                Pair(max, wifi)
            }.distinctUntilChanged().collect { (max, wifi) ->
                maxConcurrent = max
                isWifiOnly = wifi
                processQueue()
            }
        }
    }

    private fun startQueueProcessor() {
        serviceScope.launch {
            downloadDao.observeAllDownloads().collect {
                processQueue()
            }
        }
    }

    private fun processQueue() {
        if (isWifiOnly && !isWifiConnected) return

        serviceScope.launch {
            val downloading = downloadDao.getDownloadsByStatus("DOWNLOADING")
            val toStartCount = maxConcurrent - downloading.size

            if (toStartCount > 0) {
                val queued = downloadDao.getDownloadsByStatus("QUEUED")
                queued.take(toStartCount).forEach { download ->
                    startDownload(download)
                }
            }
        }
    }

    private fun pauseAllDownloads() {
        serviceScope.launch {
            val downloading = downloadDao.getDownloadsByStatus("DOWNLOADING")
            downloading.forEach { download ->
                pauseDownload(download.id)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_DOWNLOAD" -> {
                processQueue()
            }
            "CANCEL_DOWNLOAD" -> {
                val downloadId = intent.getStringExtra("DOWNLOAD_ID")
                if (downloadId != null) cancelDownload(downloadId)
            }
            "PAUSE_DOWNLOAD" -> {
                val downloadId = intent.getStringExtra("DOWNLOAD_ID")
                if (downloadId != null) pauseDownload(downloadId)
            }
            "RESUME_DOWNLOAD" -> {
                val downloadId = intent.getStringExtra("DOWNLOAD_ID")
                if (downloadId != null) {
                    serviceScope.launch {
                        downloadDao.updateStatus(downloadId, "QUEUED")
                        processQueue()
                    }
                }
            }
        }
        return START_STICKY
    }

    private fun startDownload(download: DownloadEntity) {
        if (activeJobs.containsKey(download.id)) return

        val job = serviceScope.launch {
            executeDownload(download)
        }
        activeJobs[download.id] = job
    }

    private suspend fun executeDownload(download: DownloadEntity) {
        val title = download.title ?: "Media Download"
        try {
            downloadDao.updateStatus(download.id, "DOWNLOADING")
            updateNotification(title = title, text = "Starting...", progress = 0f, downloadId = download.id)

            // Always download to internal storage first for Scoped Storage compatibility
            val tempDir = File(applicationContext.getExternalFilesDir(null), "downloads")
            if (!tempDir.exists()) tempDir.mkdirs()
            
            // Get clean filename
            val fileName = download.filePath?.let { File(it).name } ?: "download_${System.currentTimeMillis()}.mp4"

            val params = DownloadParams(
                id = download.id,
                url = download.url,
                formatId = download.formatId,
                outputDir = tempDir.absolutePath, // FORCE internal path
                fileName = fileName,
                isAudioOnly = download.isAudioOnly,
                audioFormat = download.audioFormat,
                audioBitrate = download.audioBitrate,
                trimStartSeconds = download.trimStartSeconds,
                trimEndSeconds = download.trimEndSeconds,
                useCookies = download.useCookies,
                cookiesPath = settingsRepository.cookiesFilePath.first(),
                connectionsPerDownload = settingsRepository.connectionsPerDownload.first(),
                speedLimitKbps = settingsRepository.globalSpeedLimitKbps.first()
            )

            val request = DownloadRequestBuilder.buildRequest(params)

            youtubeDL.execute(request, download.id) { progress, etaInSeconds, line ->
                serviceScope.launch {
                    val validProgress = if (progress < 0f) 0f else progress
                    downloadDao.updateProgress(download.id, 0L, validProgress)
                    val statusText = if (progress < 0f) "Downloading..." else "Downloading: ${validProgress.toInt()}%"
                    updateNotification(title = title, text = statusText, progress = validProgress, downloadId = download.id)
                }
            }

            // Download complete
            val downloadedFile = File(tempDir, fileName)
            if (!downloadedFile.exists()) throw Exception("Downloaded file not found")

            // Move to final destination
            val destUri = settingsRepository.downloadLocationUri.first()
            val finalPath = StorageResolver.moveToDestination(applicationContext, downloadedFile, destUri, fileName)
            
            downloadDao.updateStatus(download.id, "COMPLETED")
            // Update the download record with the final path
            val completedDownload = download.copy(status = "COMPLETED", filePath = finalPath ?: downloadedFile.absolutePath, progress = 100f)
            downloadDao.updateDownload(completedDownload)

            val libraryItem = LibraryItemEntity(
                id = download.id,
                sourceUrl = download.url,
                title = title,
                filePath = finalPath ?: downloadedFile.absolutePath,
                fileSizeBytes = if (finalPath != null && finalPath.startsWith("content://")) 0L else downloadedFile.length(),
                isAudio = download.isAudioOnly
            )
            libraryItemDao.insertLibraryItem(libraryItem)
            updateNotification(title = title, text = "Download complete", progress = 100f, downloadId = download.id)
            
        } catch (e: Exception) {
            val status = downloadDao.getDownloadById(download.id)?.status
            if (status != "PAUSED" && status != "CANCELLED") {
                val errorMsg = e.localizedMessage ?: "Unknown error"
                downloadDao.updateStatus(download.id, "FAILED", errorMessage = errorMsg)
                updateNotification(title = title, text = "Failed: $errorMsg", progress = 0f, downloadId = download.id)
            }
        } finally {
            activeJobs.remove(download.id)
            processQueue()
        }
    }

    private fun pauseDownload(downloadId: String) {
        serviceScope.launch {
            youtubeDL.destroyProcessById(downloadId)
            activeJobs[downloadId]?.cancel()
            activeJobs.remove(downloadId)
            downloadDao.updateStatus(downloadId, "PAUSED")
            updateNotification("Download paused", 0f, downloadId = downloadId)
            processQueue()
        }
    }

    private fun cancelDownload(downloadId: String) {
        serviceScope.launch {
            youtubeDL.destroyProcessById(downloadId)
            activeJobs[downloadId]?.cancel()
            activeJobs.remove(downloadId)
            downloadDao.updateStatus(downloadId, "CANCELLED")
            updateNotification("Download cancelled", 0f, downloadId = downloadId)
            processQueue()
        }
    }

    private fun buildNotification(title: String, text: String, progress: Float, downloadId: String? = null): android.app.Notification {
        val isIndeterminate = progress <= 0f && text.startsWith("Downloading")
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress.toInt().coerceAtLeast(0), isIndeterminate)
            .setOngoing(progress < 100f && !text.contains("cancelled") && !text.contains("failed") && !text.contains("paused"))

        if (downloadId != null && (text.startsWith("Downloading") || text.startsWith("Starting"))) {
            val pauseIntent = Intent(this, DownloadService::class.java).apply {
                action = "PAUSE_DOWNLOAD"
                putExtra("DOWNLOAD_ID", downloadId)
            }
            val pausePI = android.app.PendingIntent.getService(this, downloadId.hashCode(), pauseIntent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)

            val cancelIntent = Intent(this, DownloadService::class.java).apply {
                action = "CANCEL_DOWNLOAD"
                putExtra("DOWNLOAD_ID", downloadId)
            }
            val cancelPI = android.app.PendingIntent.getService(this, downloadId.hashCode() + 1, cancelIntent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)

            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pausePI)
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPI)
        }

        return builder.build()
    }

    private fun updateNotification(text: String, progress: Float, title: String = "Dolo Downloader", downloadId: String? = null) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(title = title, text = text, progress = progress, downloadId = downloadId))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW)
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
