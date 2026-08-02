package com.dolo.core.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dolo.core.repository.SettingsRepository
import com.dolo.core.util.AppUpdateChecker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class AppUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val appUpdateChecker: AppUpdateChecker,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // We need to pass the current version. 
            // In a real app, this would come from BuildConfig.VERSION_NAME
            // For now, let's assume we pass it or get it from context.
            val packageInfo = applicationContext.packageManager.getPackageInfo(applicationContext.packageName, 0)
            val currentVersion = packageInfo.versionName ?: "1.0"

            val updateInfo = appUpdateChecker.checkForUpdate(currentVersion)
            if (updateInfo != null && updateInfo.isNewer) {
                Log.d("AppUpdateWorker", "New version available: ${updateInfo.latestVersion}")
                settingsRepository.updateLastCheckedAppVersion(updateInfo.latestVersion)
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("AppUpdateWorker", "Failed to check for app update", e)
            Result.retry()
        }
    }
}
