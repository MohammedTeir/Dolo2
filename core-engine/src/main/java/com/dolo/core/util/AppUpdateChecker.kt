package com.dolo.core.util

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUpdateChecker @Inject constructor() {

    private val client = OkHttpClient()
    private val GITHUB_RELEASES_URL = "https://api.github.com/repos/youruser/dolo/releases/latest"

    data class AppUpdateInfo(
        val latestVersion: String,
        val downloadUrl: String,
        val body: String,
        val isNewer: Boolean
    )

    suspend fun checkForUpdate(currentVersion: String): AppUpdateInfo? {
        return try {
            val request = Request.Builder()
                .url(GITHUB_RELEASES_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                
                val bodyString = response.body?.string() ?: return null
                val json = JSONObject(bodyString)
                
                val latestTag = json.getString("tag_name").replace("v", "")
                val downloadUrl = json.getJSONArray("assets")
                    .takeIf { it.length() > 0 }
                    ?.getJSONObject(0)
                    ?.getString("browser_download_url") ?: json.getString("html_url")
                
                val body = json.optString("body", "")
                
                AppUpdateInfo(
                    latestVersion = latestTag,
                    downloadUrl = downloadUrl,
                    body = body,
                    isNewer = isVersionNewer(currentVersion, latestTag)
                )
            }
        } catch (e: Exception) {
            Log.e("AppUpdateChecker", "Failed to check for update", e)
            null
        }
    }

    private fun isVersionNewer(current: String, latest: String): Boolean {
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        
        for (i in 0 until maxOf(currentParts.size, latestParts.size)) {
            val c = currentParts.getOrElse(i) { 0 }
            val l = latestParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (c > l) return false
        }
        return false
    }
}
