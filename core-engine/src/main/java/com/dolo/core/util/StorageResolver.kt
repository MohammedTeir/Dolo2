package com.dolo.core.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File

object StorageResolver {

    /**
     * Resolves a SAF URI to a File if possible, or returns null.
     */
    fun resolvePath(context: Context, uriString: String?): String? {
        if (uriString == null) return null
        val uri = Uri.parse(uriString)
        
        // Try to get direct path if it's a file URI
        if (uri.scheme == "file") return uri.path

        // For SAF tree URIs, we can't easily get a raw path.
        // We'll return null to indicate that we should download to temp first.
        return null
    }

    /**
     * Moves a file from a source path to a SAF destination URI.
     */
    fun moveToDestination(context: Context, sourceFile: File, destinationUriString: String?, fileName: String): String? {
        if (destinationUriString == null) return sourceFile.absolutePath
        
        return try {
            val destUri = Uri.parse(destinationUriString)
            val rootDoc = DocumentFile.fromTreeUri(context, destUri) ?: return sourceFile.absolutePath
            
            // Check if file already exists in destination
            val existingFile = rootDoc.findFile(fileName)
            val finalName = if (existingFile != null) {
                // Handle collision
                val nameWithoutExt = fileName.substringBeforeLast(".")
                val ext = fileName.substringAfterLast(".", "")
                "${nameWithoutExt}_${System.currentTimeMillis()}.$ext"
            } else {
                fileName
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
}
