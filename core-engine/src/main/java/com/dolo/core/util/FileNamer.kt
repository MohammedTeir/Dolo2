package com.dolo.core.util

import java.io.File

enum class NamingMode {
    CLEAN_TITLE,
    ORIGINAL_FILENAME
}

object FileNamer {

    private val INVALID_CHARS_REGEX = Regex("[\\\\/:*?\"<>|]")
    private val WHITESPACE_REGEX = Regex("\\s+")

    fun sanitize(input: String): String {
        var clean = input.replace(INVALID_CHARS_REGEX, "_")
        clean = clean.replace(WHITESPACE_REGEX, " ").trim()
        if (clean.length > 200) {
            clean = clean.substring(0, 200)
        }
        return if (clean.isEmpty()) "download" else clean
    }

    fun generateFileName(
        mode: NamingMode,
        title: String?,
        originalUrl: String?,
        contentDisposition: String?,
        ext: String = "mp4",
        playlistIndex: Int? = null,
        totalPlaylistItems: Int? = null,
        trimStartSeconds: Float? = null,
        trimEndSeconds: Float? = null,
        template: String? = null,
        metadata: Map<String, String> = emptyMap()
    ): String {
        var baseName: String = when (mode) {
            NamingMode.CLEAN_TITLE -> {
                val templateStr = template
                if (!templateStr.isNullOrBlank() && metadata.isNotEmpty()) {
                    applyTemplate(templateStr, metadata)
                } else {
                    sanitize(title ?: "download")
                }
            }
            NamingMode.ORIGINAL_FILENAME -> {
                extractOriginalFilename(originalUrl, contentDisposition)
                    ?: sanitize(title ?: "download")
            }
        }

        // Apply playlist index prefix if in Clean Title mode
        if (mode == NamingMode.CLEAN_TITLE && playlistIndex != null) {
            val count = totalPlaylistItems ?: 99
            val padLength = count.toString().length.coerceAtLeast(2)
            val paddedIndex = playlistIndex.toString().padStart(padLength, '0')
            baseName = "$paddedIndex - $baseName"
        }

        // Apply trim suffix if applicable
        if (trimStartSeconds != null && trimEndSeconds != null) {
            val startStr = formatDurationForFilename(trimStartSeconds.toInt())
            val endStr = formatDurationForFilename(trimEndSeconds.toInt())
            baseName = "$baseName [$startStr-$endStr]"
        }

        val extension = ext.trimStart('.').ifEmpty { "mp4" }
        return "$baseName.$extension"
    }

    fun handleCollision(destinationDir: File, fileName: String): File {
        var target = File(destinationDir, fileName)
        if (!target.exists()) return target

        val dotIndex = fileName.lastIndexOf('.')
        val nameWithoutExt = if (dotIndex > 0) fileName.substring(0, dotIndex) else fileName
        val ext = if (dotIndex > 0) fileName.substring(dotIndex) else ""

        var counter = 1
        while (target.exists()) {
            target = File(destinationDir, "$nameWithoutExt ($counter)$ext")
            counter++
        }
        return target
    }

    fun extractOriginalFilename(url: String?, contentDisposition: String?): String? {
        val disposition = contentDisposition
        if (!disposition.isNullOrBlank()) {
            val filenameMatch = Regex("filename\\*=.*?''([^;]+)|filename=\"?([^\";]+)\"?")
                .find(disposition)
            val name = filenameMatch?.groupValues?.get(1)?.ifEmpty { null }
                ?: filenameMatch?.groupValues?.get(2)?.ifEmpty { null }
            if (name != null) return sanitize(name)
        }

        val rawUrl = url
        if (!rawUrl.isNullOrBlank()) {
            val path = rawUrl.substringBefore('?').substringBefore('#')
            val lastSegment = path.substringAfterLast('/')
            if (lastSegment.isNotEmpty() && lastSegment.contains('.')) {
                val nameWithoutExt = lastSegment.substringBeforeLast('.')
                return sanitize(nameWithoutExt)
            }
        }
        return null
    }

    fun applyTemplate(template: String, metadata: Map<String, String>): String {
        var result = template
        metadata.forEach { (key, value) ->
            result = result.replace("{$key}", sanitize(value))
        }
        return sanitize(result)
    }

    private fun formatDurationForFilename(seconds: Int): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hrs > 0) {
            String.format("%02d-%02d-%02d", hrs, mins, secs)
        } else {
            String.format("%02d-%02d", mins, secs)
        }
    }
}
