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
        
        val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        val ariaArgs = StringBuilder("aria2c:-x $connections -s $connections -k 1M --user-agent=\"$userAgent\"")
        if (params.speedLimitKbps != null && params.speedLimitKbps > 0) {
            ariaArgs.append(" --max-overall-download-limit=${params.speedLimitKbps}K")
        }
        request.addOption("--external-downloader-args", ariaArgs.toString())

        // Metadata embedding
        if (params.embedMetadata) {
            request.addOption("--add-metadata")
            request.addOption("--embed-thumbnail")
        }

        // Subtitles
        if (!params.selectedSubtitleLanguage.isNullOrBlank()) {
            request.addOption("--write-subs")
            request.addOption("--sub-langs", params.selectedSubtitleLanguage)
        }

        // Compatibility & warning suppression flags
        request.addOption("--no-mtime")
        request.addOption("--no-warnings")
        request.addOption("--no-check-certificate")
        request.addOption("--prefer-insecure")
        request.addOption("--geo-bypass")
        request.addOption("--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        request.addOption("--extractor-args", "youtube:player_client=android")

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
