package com.dolo.core.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dolo_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val MAX_CONCURRENT_DOWNLOADS = intPreferencesKey("max_concurrent_downloads")
        val IS_WIFI_ONLY = booleanPreferencesKey("is_wifi_only")
        val GLOBAL_SPEED_LIMIT_KBPS = intPreferencesKey("global_speed_limit_kbps")
        val CONNECTIONS_PER_DOWNLOAD = intPreferencesKey("connections_per_download")
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
}
