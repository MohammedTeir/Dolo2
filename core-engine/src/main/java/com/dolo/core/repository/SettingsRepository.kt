package com.dolo.core.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dolo_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val MAX_CONCURRENT_DOWNLOADS = intPreferencesKey("max_concurrent_downloads")
        val IS_WIFI_ONLY = booleanPreferencesKey("is_wifi_only")
        val GLOBAL_SPEED_LIMIT_KBPS = intPreferencesKey("global_speed_limit_kbps")
        val CONNECTIONS_PER_DOWNLOAD = intPreferencesKey("connections_per_download")
        
        val DEFAULT_AUDIO_FORMAT = stringPreferencesKey("default_audio_format")
        val DEFAULT_AUDIO_BITRATE = intPreferencesKey("default_audio_bitrate")
        val IS_METADATA_EMBEDDING_ENABLED = booleanPreferencesKey("is_metadata_embedding_enabled")
        
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val NAMING_MODE = stringPreferencesKey("naming_mode")
        val ORGANIZE_PLAYLISTS_IN_FOLDERS = booleanPreferencesKey("organize_playlists_in_folders")
        val CLIPBOARD_WATCHER_ENABLED = booleanPreferencesKey("clipboard_watcher_enabled")
        
        val LAST_CHECKED_APP_VERSION = stringPreferencesKey("last_checked_app_version")
        val COOKIES_FILE_PATH = stringPreferencesKey("cookies_file_path")
        
        val DOWNLOAD_LOCATION_URI = stringPreferencesKey("download_location_uri")
        val DOWNLOAD_LOCATION_NAME = stringPreferencesKey("download_location_name")
    }

    val maxConcurrentDownloads: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.MAX_CONCURRENT_DOWNLOADS] ?: 2
        }

    val isWifiOnly: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.IS_WIFI_ONLY] ?: false
        }

    val globalSpeedLimitKbps: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.GLOBAL_SPEED_LIMIT_KBPS] ?: 0
        }

    val connectionsPerDownload: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.CONNECTIONS_PER_DOWNLOAD] ?: 4
        }
        
    val defaultAudioFormat: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.DEFAULT_AUDIO_FORMAT] ?: "mp3"
        }

    val defaultAudioBitrate: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.DEFAULT_AUDIO_BITRATE] ?: 192
        }

    val isMetadataEmbeddingEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.IS_METADATA_EMBEDDING_ENABLED] ?: true
        }

    val themeMode: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.THEME_MODE] ?: "System"
        }

    val namingMode: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.NAMING_MODE] ?: "Clean Title"
        }

    val organizePlaylistsInFolders: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.ORGANIZE_PLAYLISTS_IN_FOLDERS] ?: true
        }

    val clipboardWatcherEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.CLIPBOARD_WATCHER_ENABLED] ?: false
        }

    val lastCheckedAppVersion: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.LAST_CHECKED_APP_VERSION]
        }

    val cookiesFilePath: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.COOKIES_FILE_PATH]
        }

    val downloadLocationUri: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.DOWNLOAD_LOCATION_URI]
        }

    val downloadLocationName: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.DOWNLOAD_LOCATION_NAME]
        }

    suspend fun updateMaxConcurrentDownloads(value: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MAX_CONCURRENT_DOWNLOADS] = value
        }
    }

    suspend fun updateWifiOnly(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_WIFI_ONLY] = value
        }
    }

    suspend fun updateGlobalSpeedLimit(kbps: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GLOBAL_SPEED_LIMIT_KBPS] = kbps
        }
    }

    suspend fun updateConnectionsPerDownload(value: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CONNECTIONS_PER_DOWNLOAD] = value
        }
    }

    suspend fun updateDefaultAudioFormat(value: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_AUDIO_FORMAT] = value
        }
    }

    suspend fun updateDefaultAudioBitrate(value: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_AUDIO_BITRATE] = value
        }
    }

    suspend fun updateMetadataEmbeddingEnabled(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_METADATA_EMBEDDING_ENABLED] = value
        }
    }

    suspend fun updateThemeMode(value: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = value
        }
    }

    suspend fun updateNamingMode(value: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NAMING_MODE] = value
        }
    }

    suspend fun updateOrganizePlaylistsInFolders(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ORGANIZE_PLAYLISTS_IN_FOLDERS] = value
        }
    }

    suspend fun updateClipboardWatcherEnabled(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CLIPBOARD_WATCHER_ENABLED] = value
        }
    }

    suspend fun updateLastCheckedAppVersion(value: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_CHECKED_APP_VERSION] = value
        }
    }

    suspend fun updateCookiesFilePath(value: String?) {
        context.dataStore.edit { preferences ->
            if (value == null) {
                preferences.remove(PreferencesKeys.COOKIES_FILE_PATH)
            } else {
                preferences[PreferencesKeys.COOKIES_FILE_PATH] = value
            }
        }
    }

    suspend fun updateDownloadLocation(uri: String?, name: String?) {
        context.dataStore.edit { preferences ->
            if (uri == null) {
                preferences.remove(PreferencesKeys.DOWNLOAD_LOCATION_URI)
                preferences.remove(PreferencesKeys.DOWNLOAD_LOCATION_NAME)
            } else {
                preferences[PreferencesKeys.DOWNLOAD_LOCATION_URI] = uri
                preferences[PreferencesKeys.DOWNLOAD_LOCATION_NAME] = name ?: "Custom Folder"
            }
        }
    }
}
