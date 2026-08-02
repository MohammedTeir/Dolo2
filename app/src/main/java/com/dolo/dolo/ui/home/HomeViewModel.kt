package com.dolo.dolo.ui.home

import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dolo.core.db.DownloadEntity
import com.dolo.core.model.VideoMetadata
import com.dolo.core.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val url: String = "",
    val isExtracting: Boolean = false,
    val extractionError: String? = null,
    val extractedMetadata: VideoMetadata? = null,
    val duplicateDownload: DownloadEntity? = null,
    val showDuplicateWarning: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository
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
                showDuplicateWarning = false
            )

            val result = downloadRepository.extractInfo(trimmedUrl)

            result.fold(
                onSuccess = { metadata ->
                    _uiState.value = _uiState.value.copy(
                        isExtracting = false,
                        extractedMetadata = metadata
                    )
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
