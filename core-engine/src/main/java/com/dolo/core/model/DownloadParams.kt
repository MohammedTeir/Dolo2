package com.dolo.core.model

import com.dolo.core.util.NamingMode

data class DownloadParams(
    val id: String,
    val url: String,
    val formatId: String? = null,
    val outputDir: String,
    val fileName: String? = null,
    val isAudioOnly: Boolean = false,
    val audioFormat: String? = "mp3", // mp3, m4a, opus, flac
    val audioBitrate: Int? = 192,
    val trimStartSeconds: Float? = null,
    val trimEndSeconds: Float? = null,
    val useCookies: Boolean = false,
    val cookiesPath: String? = null,
    val connectionsPerDownload: Int = 4,
    val speedLimitKbps: Int? = null,
    val embedMetadata: Boolean = true,
    val namingMode: NamingMode = NamingMode.CLEAN_TITLE,
    val selectedSubtitleLanguage: String? = null
)
