package com.dolo.core.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yausername.youtubedl_android.YoutubeDL
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class EngineUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val youtubeDL: YoutubeDL
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("EngineUpdateWorker", "Checking for yt-dlp updates...")
            val result = youtubeDL.updateYoutubeDL(applicationContext)
            Log.d("EngineUpdateWorker", "Update result: $result")
            Result.success()
        } catch (e: Exception) {
            Log.e("EngineUpdateWorker", "Failed to update yt-dlp", e)
            Result.retry()
        }
    }
}
