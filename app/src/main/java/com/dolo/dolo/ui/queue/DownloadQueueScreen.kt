package com.dolo.dolo.ui.queue

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dolo.core.db.DownloadEntity

@Composable
fun DownloadQueueScreen(
    viewModel: DownloadQueueViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allEmpty = uiState.activeDownloads.isEmpty() &&
            uiState.queuedDownloads.isEmpty() &&
            uiState.failedDownloads.isEmpty()

    if (allEmpty) {
        EmptyQueueState()
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Active downloads
            if (uiState.activeDownloads.isNotEmpty()) {
                item {
                    SectionHeader(title = "Active", count = uiState.activeDownloads.size)
                }
                items(uiState.activeDownloads, key = { it.id }) { download ->
                    DownloadQueueItem(
                        download = download,
                        onCancel = { viewModel.cancelDownload(download.id) },
                        onPause = { viewModel.pauseDownload(download.id) },
                        onResume = { viewModel.resumeDownload(download.id) },
                        onSetSpeedLimit = { viewModel.setSpeedLimit(download.id, it) }
                    )
                }
            }

            // Paused downloads
            if (uiState.pausedDownloads.isNotEmpty()) {
                item {
                    SectionHeader(title = "Paused", count = uiState.pausedDownloads.size)
                }
                items(uiState.pausedDownloads, key = { it.id }) { download ->
                    DownloadQueueItem(
                        download = download,
                        onCancel = { viewModel.cancelDownload(download.id) },
                        onPause = { viewModel.pauseDownload(download.id) },
                        onResume = { viewModel.resumeDownload(download.id) }
                    )
                }
            }

            // Queued downloads
            if (uiState.queuedDownloads.isNotEmpty()) {
                item {
                    SectionHeader(title = "Queued", count = uiState.queuedDownloads.size)
                }
                items(uiState.queuedDownloads, key = { it.id }) { download ->
                    DownloadQueueItem(
                        download = download,
                        onCancel = { viewModel.cancelDownload(download.id) },
                        onPause = { viewModel.pauseDownload(download.id) },
                        onResume = { viewModel.resumeDownload(download.id) },
                        onMoveUp = { viewModel.moveDownloadUp(download.id) },
                        onMoveDown = { viewModel.moveDownloadDown(download.id) }
                    )
                }
            }

            // Failed downloads
            if (uiState.failedDownloads.isNotEmpty()) {
                item {
                    SectionHeader(title = "Failed", count = uiState.failedDownloads.size)
                }
                items(uiState.failedDownloads, key = { it.id }) { download ->
                    DownloadQueueItem(
                        download = download,
                        onCancel = { viewModel.cancelDownload(download.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "($count)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DownloadQueueItem(
    download: DownloadEntity,
    onCancel: () -> Unit,
    onPause: () -> Unit = {},
    onResume: () -> Unit = {},
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    onSetSpeedLimit: (Int?) -> Unit = {}
) {
    val animatedProgress by animateFloatAsState(
        targetValue = download.progress / 100f,
        animationSpec = tween(durationMillis = 300),
        label = "progress"
    )

    var showSpeedMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusIcon = when (download.status) {
                    "DOWNLOADING" -> Icons.Default.CloudDownload
                    "PAUSED" -> Icons.Default.Pause
                    "QUEUED" -> Icons.Default.HourglassTop
                    "FAILED" -> Icons.Default.Error
                    else -> Icons.Default.CloudDownload
                }
                val statusColor = when (download.status) {
                    "DOWNLOADING" -> MaterialTheme.colorScheme.primary
                    "PAUSED" -> MaterialTheme.colorScheme.tertiary
                    "QUEUED" -> MaterialTheme.colorScheme.onSurfaceVariant
                    "FAILED" -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Icon(
                    imageVector = statusIcon,
                    contentDescription = download.status,
                    tint = statusColor,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = download.title ?: download.url,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = download.status.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor
                        )

                        if (download.status == "DOWNLOADING") {
                            if (download.progress > 0f) {
                                Text(
                                    text = " · ${download.progress.toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (download.downloadSpeedBytes > 0) {
                                Text(
                                    text = " · ${formatSpeed(download.downloadSpeedBytes)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }

                // Actions
                Row {
                    if (download.status == "DOWNLOADING") {
                        Box {
                            IconButton(onClick = { showSpeedMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "Speed Limit",
                                    tint = if (download.speedLimitKbps != null) MaterialTheme.colorScheme.primary 
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(expanded = showSpeedMenu, onDismissRequest = { showSpeedMenu = false }) {
                                DropdownMenuItem(text = { Text("Unlimited") }, onClick = { onSetSpeedLimit(null); showSpeedMenu = false })
                                listOf(512, 1024, 2048, 5120).forEach { limit ->
                                    DropdownMenuItem(
                                        text = { Text("${limit / 1024} MB/s") },
                                        onClick = { onSetSpeedLimit(limit); showSpeedMenu = false }
                                    )
                                }
                            }
                        }
                        IconButton(onClick = onPause) {
                            Icon(Icons.Default.Pause, contentDescription = "Pause", tint = MaterialTheme.colorScheme.primary)
                        }
                    } else if (download.status == "PAUSED" || download.status == "QUEUED") {
                        IconButton(onClick = onResume) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    if (download.status == "QUEUED") {
                        IconButton(onClick = onMoveUp) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Up")
                        }
                        IconButton(onClick = onMoveDown) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "Down")
                        }
                    }

                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Cancel, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                    }
                }
            }

            if (download.status == "DOWNLOADING") {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            }

            val errorMessage = download.errorMessage
            if (download.status == "FAILED" && !errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun formatSpeed(bytesPerSecond: Long): String {
    val kbps = bytesPerSecond / 1024.0
    return if (kbps >= 1024) String.format("%.1f MB/s", kbps / 1024.0)
    else String.format("%.0f KB/s", kbps)
}

@Composable
private fun EmptyQueueState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.CloudDownload,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "No downloads yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            Text(text = "Paste a link to get started", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        }
    }
}
