package com.dolo.core.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import java.io.File

object StorageResolver {

    /**
     * Moves a file from a source path to a destination (SAF URI or Public Downloads).
     */
    fun moveToDestination(context: Context, sourceFile: File, destinationUriString: String?, fileName: String): String? {
        if (!sourceFile.exists()) return null
        
        // 1. Handle SAF Destination
        if (!destinationUriString.isNullOrBlank()) {
            return try {
                val destUri = Uri.parse(destinationUriString)
                val rootDoc = DocumentFile.fromTreeUri(context, destUri) ?: return sourceFile.absolutePath
                
                var finalName = fileName
                if (rootDoc.findFile(fileName) != null) {
                    finalName = "${fileName.substringBeforeLast(".")}_${System.currentTimeMillis()}.${fileName.substringAfterLast(".")}"
                }

                val newFile = rootDoc.createFile("*/*", finalName) ?: return sourceFile.absolutePath
                
                context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                    sourceFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
                
                sourceFile.delete()
                newFile.uri.toString()
            } catch (e: Exception) {
                e.printStackTrace()
                sourceFile.absolutePath
            }
        }
        
        // 2. Handle Public Downloads (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return try {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, getMimeType(fileName))
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Dolo")
                }
                
                val collection = if (isAudio(fileName)) MediaStore.Audio.Media.EXTERNAL_CONTENT_URI 
                                 else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                
                val uri = context.contentResolver.insert(collection, contentValues) ?: return sourceFile.absolutePath
                
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    sourceFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
                
                sourceFile.delete()
                uri.toString()
            } catch (e: Exception) {
                e.printStackTrace()
                sourceFile.absolutePath
            }
        } else {
            // 3. Handle Legacy Storage (Android 9 and below)
            val publicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Dolo")
            if (!publicDir.exists()) publicDir.mkdirs()
            
            val destFile = File(publicDir, fileName)
            val finalDest = if (destFile.exists()) {
                File(publicDir, "${fileName.substringBeforeLast(".")}_${System.currentTimeMillis()}.${fileName.substringAfterLast(".")}")
            } else {
                destFile
            }
            
            return if (sourceFile.renameTo(finalDest)) {
                finalDest.absolutePath
            } else {
                sourceFile.absolutePath
            }
        }
    }

    private fun getMimeType(fileName: String): String {
        return when (fileName.substringAfterLast(".").lowercase()) {
            "mp4" -> "video/mp4"
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "webm" -> "video/webm"
            "mkv" -> "video/x-matroska"
            "opus" -> "audio/opus"
            else -> "*/*"
        }
    }
    
    private fun isAudio(fileName: String): Boolean {
        val ext = fileName.substringAfterLast(".").lowercase()
        return listOf("mp3", "m4a", "opus", "flac", "wav").contains(ext)
    }
}
