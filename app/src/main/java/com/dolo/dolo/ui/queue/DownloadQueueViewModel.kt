package com.dolo.dolo.ui.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dolo.core.db.DownloadEntity
import com.dolo.core.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DownloadQueueUiState(
    val activeDownloads: List<DownloadEntity> = emptyList(),
    val pausedDownloads: List<DownloadEntity> = emptyList(),
    val queuedDownloads: List<DownloadEntity> = emptyList(),
    val failedDownloads: List<DownloadEntity> = emptyList()
)

@HiltViewModel
class DownloadQueueViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    val uiState: StateFlow<DownloadQueueUiState> = downloadRepository.observeQueue()
        .map { downloads ->
            DownloadQueueUiState(
                activeDownloads = downloads.filter { it.status == "DOWNLOADING" },
                pausedDownloads = downloads.filter { it.status == "PAUSED" },
                queuedDownloads = downloads.filter { it.status == "QUEUED" },
                failedDownloads = downloads.filter { it.status == "FAILED" }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DownloadQueueUiState()
        )

    fun cancelDownload(id: String) {
        viewModelScope.launch {
            downloadRepository.cancelDownload(id)
        }
    }

    fun pauseDownload(id: String) {
        viewModelScope.launch {
            downloadRepository.pauseDownload(id)
        }
    }

    fun resumeDownload(id: String) {
        viewModelScope.launch {
            downloadRepository.resumeDownload(id)
        }
    }

    fun moveDownloadUp(id: String) {
        viewModelScope.launch {
            downloadRepository.moveDownloadUp(id)
        }
    }

    fun moveDownloadDown(id: String) {
        viewModelScope.launch {
            downloadRepository.moveDownloadDown(id)
        }
    }

    fun setSpeedLimit(id: String, limitKbps: Int?) {
        viewModelScope.launch {
            downloadRepository.setSpeedLimit(id, limitKbps)
        }
    }
}
