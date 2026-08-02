package com.dolo.dolo.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dolo.core.repository.DownloadRepository
import com.dolo.core.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val maxConcurrentDownloads: Int = 2,
    val isWifiOnly: Boolean = false,
    val globalSpeedLimitKbps: Int = 0,
    val connectionsPerDownload: Int = 4,
    val defaultAudioFormat: String = "mp3",
    val defaultAudioBitrate: Int = 192,
    val isMetadataEmbeddingEnabled: Boolean = true,
    val themeMode: String = "System",
    val namingMode: String = "Clean Title",
    val organizePlaylistsInFolders: Boolean = true,
    val clipboardWatcherEnabled: Boolean = false,
    val cookiesFilePath: String? = null,
    val downloadLocationUri: String? = null,
    val downloadLocationName: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.maxConcurrentDownloads,
        settingsRepository.isWifiOnly,
        settingsRepository.globalSpeedLimitKbps,
        settingsRepository.connectionsPerDownload,
        settingsRepository.defaultAudioFormat,
        settingsRepository.defaultAudioBitrate,
        settingsRepository.isMetadataEmbeddingEnabled,
        settingsRepository.themeMode,
        settingsRepository.namingMode,
        settingsRepository.organizePlaylistsInFolders,
        settingsRepository.clipboardWatcherEnabled,
        settingsRepository.cookiesFilePath,
        settingsRepository.downloadLocationUri,
        settingsRepository.downloadLocationName
    ) { args ->
        SettingsUiState(
            maxConcurrentDownloads = args[0] as Int,
            isWifiOnly = args[1] as Boolean,
            globalSpeedLimitKbps = args[2] as Int,
            connectionsPerDownload = args[3] as Int,
            defaultAudioFormat = args[4] as String,
            defaultAudioBitrate = args[5] as Int,
            isMetadataEmbeddingEnabled = args[6] as Boolean,
            themeMode = args[7] as String,
            namingMode = args[8] as String,
            organizePlaylistsInFolders = args[9] as Boolean,
            clipboardWatcherEnabled = args[10] as Boolean,
            cookiesFilePath = args[11] as String?,
            downloadLocationUri = args[12] as String?,
            downloadLocationName = args[13] as String?
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun updateMaxConcurrentDownloads(value: Int) {
        viewModelScope.launch { settingsRepository.updateMaxConcurrentDownloads(value) }
    }

    fun updateWifiOnly(value: Boolean) {
        viewModelScope.launch { settingsRepository.updateWifiOnly(value) }
    }

    fun updateGlobalSpeedLimit(kbps: Int) {
        viewModelScope.launch { settingsRepository.updateGlobalSpeedLimit(kbps) }
    }

    fun updateConnectionsPerDownload(value: Int) {
        viewModelScope.launch { settingsRepository.updateConnectionsPerDownload(value) }
    }

    fun updateDefaultAudioFormat(value: String) {
        viewModelScope.launch { settingsRepository.updateDefaultAudioFormat(value) }
    }

    fun updateDefaultAudioBitrate(value: Int) {
        viewModelScope.launch { settingsRepository.updateDefaultAudioBitrate(value) }
    }

    fun updateMetadataEmbeddingEnabled(value: Boolean) {
        viewModelScope.launch { settingsRepository.updateMetadataEmbeddingEnabled(value) }
    }

    fun updateThemeMode(value: String) {
        viewModelScope.launch { settingsRepository.updateThemeMode(value) }
    }

    fun updateNamingMode(value: String) {
        viewModelScope.launch { settingsRepository.updateNamingMode(value) }
    }

    fun updateOrganizePlaylistsInFolders(value: Boolean) {
        viewModelScope.launch { settingsRepository.updateOrganizePlaylistsInFolders(value) }
    }

    fun updateClipboardWatcherEnabled(value: Boolean) {
        viewModelScope.launch { settingsRepository.updateClipboardWatcherEnabled(value) }
    }

    fun updateCookiesFilePath(value: String?) {
        viewModelScope.launch { settingsRepository.updateCookiesFilePath(value) }
    }

    fun updateDownloadLocation(uri: String?, name: String?) {
        viewModelScope.launch { settingsRepository.updateDownloadLocation(uri, name) }
    }

    fun clearEngineCache() {
        viewModelScope.launch {
            downloadRepository.clearEngineCache()
        }
    }
}
