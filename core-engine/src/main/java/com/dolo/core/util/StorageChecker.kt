package com.dolo.core.util

import android.os.StatFs
import java.io.File

object StorageChecker {
    /**
     * Checks if the directory at [path] has at least [requiredBytes] free space.
     */
    fun hasEnoughSpace(path: String, requiredBytes: Long): Boolean {
        return try {
            val file = File(path)
            val dir = if (file.exists()) file else file.parentFile ?: return true
            val stat = StatFs(dir.absolutePath)
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            availableBytes >= requiredBytes
        } catch (e: Exception) {
            true // Fail open if stat fails
        }
    }

    /**
     * Returns total available bytes at [path].
     */
    fun getAvailableBytes(path: String): Long {
        return try {
            val file = File(path)
            val dir = if (file.exists()) file else file.parentFile ?: return 0L
            val stat = StatFs(dir.absolutePath)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (e: Exception) {
            0L
        }
    }
}
