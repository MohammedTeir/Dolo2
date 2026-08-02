package com.dolo.core.engine

import android.webkit.DownloadListener
import com.dolo.core.model.DownloadParams
import com.dolo.core.repository.DownloadRepository
import com.dolo.core.util.FileNamer
import com.dolo.core.util.NamingMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebDownloadInterceptor @Inject constructor(
    private val downloadRepository: DownloadRepository
) : DownloadListener {

    var defaultOutputDir: String = ""
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onDownloadStart(
        url: String?,
        userAgent: String?,
        contentDisposition: String?,
        mimetype: String?,
        contentLength: Long
    ) {
        if (url.isNullOrEmpty()) return

        val fileName = FileNamer.generateFileName(
            mode = NamingMode.ORIGINAL_FILENAME,
            title = null,
            originalUrl = url,
            contentDisposition = contentDisposition,
            ext = getExtensionFromMimeOrUrl(mimetype, url)
        )

        val params = DownloadParams(
            id = java.util.UUID.randomUUID().toString(),
            url = url,
            outputDir = defaultOutputDir,
            fileName = fileName,
            namingMode = NamingMode.ORIGINAL_FILENAME
        )

        scope.launch {
            downloadRepository.queueDownload(params)
        }
    }

    private fun getExtensionFromMimeOrUrl(mimeType: String?, url: String): String {
        if (mimeType != null) {
            when {
                mimeType.contains("mp4") -> return "mp4"
                mimeType.contains("mpeg") || mimeType.contains("mp3") -> return "mp3"
                mimeType.contains("m4a") -> return "m4a"
                mimeType.contains("webm") -> return "webm"
                mimeType.contains("mkv") -> return "mkv"
            }
        }
        val cleanUrl = url.substringBefore('?').substringBefore('#')
        val ext = cleanUrl.substringAfterLast('.', "")
        return if (ext.isNotEmpty() && ext.length <= 4) ext else "mp4"
    }
}
