package com.dolo.core.engine

import com.dolo.core.model.DownloadParams
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File

object DownloadRequestBuilder {

    fun buildRequest(params: DownloadParams): YoutubeDLRequest {
        val request = YoutubeDLRequest(params.url)

        // Output template / location
        val fileName = params.fileName
        val outputTemplate = if (!fileName.isNullOrBlank()) {
            File(params.outputDir, fileName).absolutePath
        } else {
            File(params.outputDir, "%(title)s.%(ext)s").absolutePath
        }
        request.addOption("-o", outputTemplate)

        // Format selection or Audio extraction
        if (params.isAudioOnly) {
            request.addOption("-x")
            val format = params.audioFormat ?: "mp3"
            request.addOption("--audio-format", format)
            if (params.audioBitrate != null && params.audioBitrate > 0) {
                request.addOption("--audio-quality", "${params.audioBitrate}k")
            }
        } else {
            val formatId = params.formatId
            if (!formatId.isNullOrBlank()) {
                request.addOption("-f", formatId)
            }
        }

        // Clip / Trim download section
        if (params.trimStartSeconds != null && params.trimEndSeconds != null) {
            val startStr = formatTimestamp(params.trimStartSeconds)
            val endStr = formatTimestamp(params.trimEndSeconds)
            request.addOption("--download-sections", "*$startStr-$endStr")
        }

        // Cookies file
        val cookiesPath = params.cookiesPath
        if (params.useCookies && !cookiesPath.isNullOrBlank()) {
            val cookiesFile = File(cookiesPath)
            if (cookiesFile.exists()) {
                request.addOption("--cookies", cookiesFile.absolutePath)
            }
        }

        // Speed limit
        if (params.speedLimitKbps != null && params.speedLimitKbps > 0) {
            request.addOption("-r", "${params.speedLimitKbps}K")
        }

        // External downloader (aria2c) for multi-connection download
        request.addOption("--external-downloader", "aria2c")
        val connections = params.connectionsPerDownload.coerceIn(1, 16)
        request.addOption("--external-downloader-args", "aria2c:-x $connections -s $connections -k 1M")

        // Metadata embedding
        if (params.embedMetadata) {
            request.addOption("--add-metadata")
            request.addOption("--embed-thumbnail")
        }

        // Compatibility flags
        request.addOption("--no-mtime")

        return request
    }

    private fun formatTimestamp(seconds: Float): String {
        val totalSecs = seconds.toInt()
        val hrs = totalSecs / 3600
        val mins = (totalSecs % 3600) / 60
        val secs = totalSecs % 60
        val millis = ((seconds - totalSecs) * 1000).toInt()
        return String.format("%02d:%02d:%02d.%03d", hrs, mins, secs, millis)
    }
}
