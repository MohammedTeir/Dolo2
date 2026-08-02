package com.dolo.dolo.ui.home

import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dolo.core.db.DownloadEntity
import com.dolo.core.model.PlaylistInfo
import com.dolo.core.model.VideoMetadata
import com.dolo.core.repository.DownloadRepository
import com.dolo.core.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val url: String = "",
    val isExtracting: Boolean = false,
    val extractionError: String? = null,
    val extractedMetadata: VideoMetadata? = null,
    val extractedPlaylist: PlaylistInfo? = null,
    val showAmbiguityPrompt: Boolean = false,
    val duplicateDownload: DownloadEntity? = null,
    val showDuplicateWarning: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun onUrlChanged(url: String) {
        _uiState.value = _uiState.value.copy(
            url = url,
            extractionError = null
        )
    }

    fun pasteFromClipboard(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = clipboard?.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val pastedText = clip.getItemAt(0).coerceToText(context).toString().trim()
            if (pastedText.isNotBlank()) {
                _uiState.value = _uiState.value.copy(url = pastedText, extractionError = null)
                extractInfo(pastedText)
            }
        }
    }

    fun extractInfo(url: String = _uiState.value.url) {
        val trimmedUrl = url.trim()
        if (trimmedUrl.isBlank()) {
            _uiState.value = _uiState.value.copy(extractionError = "Please enter a URL")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isExtracting = true,
                extractionError = null,
                extractedMetadata = null,
                extractedPlaylist = null,
                showAmbiguityPrompt = false,
                showDuplicateWarning = false
            )

            // Check for playlist ambiguity (watch?v=...&list=...)
            if (trimmedUrl.contains("watch?v=") && trimmedUrl.contains("list=")) {
                _uiState.update { it.copy(isExtracting = false, showAmbiguityPrompt = true) }
                return@launch
            }

            val result = downloadRepository.extractInfo(trimmedUrl)

            result.fold(
                onSuccess = { metadata ->
                    if (metadata.isPlaylist) {
                        extractPlaylist(trimmedUrl)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isExtracting = false,
                            extractedMetadata = metadata
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isExtracting = false,
                        extractionError = error.localizedMessage ?: "Extraction failed"
                    )
                }
            )
        }
    }

    fun extractPlaylist(url: String = _uiState.value.url) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExtracting = true, extractionError = null, showAmbiguityPrompt = false) }
            val result = downloadRepository.extractPlaylist(url)
            result.fold(
                onSuccess = { playlist ->
                    _uiState.update { it.copy(isExtracting = false, extractedPlaylist = playlist) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isExtracting = false, extractionError = error.localizedMessage) }
                }
            )
        }
    }

    fun startDownload(params: com.dolo.core.model.DownloadParams) {
        viewModelScope.launch {
            val title = _uiState.value.extractedMetadata?.title
            val finalParams = if (params.fileName == null && title != null) {
                val ext = if (params.isAudioOnly) (params.audioFormat ?: "mp3") else "mp4"
                val sanitizedTitle = com.dolo.core.util.FileNamer.sanitize(title)
                params.copy(fileName = "$sanitizedTitle.$ext")
            } else {
                params
            }
            downloadRepository.queueDownload(finalParams)
            clearState()
        }
    }

    fun importBatch(urls: List<String>) {
        viewModelScope.launch {
            urls.forEach { url ->
                extractInfo(url)
            }
        }
    }

    fun queuePlaylist(
        playlist: PlaylistInfo,
        selectedIndices: Set<Int>,
        isAudio: Boolean,
        format: String? = null,
        bitrate: Int? = null
    ) {
        viewModelScope.launch {
            val namingModeStr = settingsRepository.namingMode.first()
            val namingMode = com.dolo.core.util.NamingMode.valueOf(namingModeStr.uppercase().replace(" ", "_"))
            val organize = settingsRepository.organizePlaylistsInFolders.first()
            
            val outputDir = if (organize && playlist.title != null) {
                val folderName = com.dolo.core.util.FileNamer.generatePlaylistFolder(playlist.title!!)
                java.io.File(settingsRepository.downloadLocationUri.first() ?: "downloads", folderName).absolutePath
            } else {
                settingsRepository.downloadLocationUri.first() ?: "downloads"
            }

            val paramsList = selectedIndices.mapNotNull { index ->
                val entry = playlist.entries?.getOrNull(index) ?: return@mapNotNull null
                val fileName = com.dolo.core.util.FileNamer.generateFileName(
                    mode = namingMode,
                    title = entry.title,
                    originalUrl = entry.url,
                    contentDisposition = null,
                    ext = if (isAudio) (format ?: "mp3") else "mp4",
                    playlistIndex = index,
                    totalPlaylistItems = playlist.entries?.size
                )
                
                com.dolo.core.model.DownloadParams(
                    id = java.util.UUID.randomUUID().toString(),
                    url = entry.url ?: "",
                    outputDir = outputDir,
                    fileName = fileName,
                    isAudioOnly = isAudio,
                    audioFormat = format,
                    audioBitrate = bitrate
                )
            }
            
            downloadRepository.queueBatch(paramsList)
            _uiState.update { it.copy(extractedPlaylist = null) }
        }
    }

    fun dismissFormatPicker() {
        _uiState.value = _uiState.value.copy(
            extractedMetadata = null,
            showDuplicateWarning = false,
            duplicateDownload = null
        )
    }

    fun dismissDuplicateWarning() {
        _uiState.value = _uiState.value.copy(
            showDuplicateWarning = false,
            duplicateDownload = null
        )
    }

    fun clearState() {
        _uiState.value = HomeUiState()
    }
}
