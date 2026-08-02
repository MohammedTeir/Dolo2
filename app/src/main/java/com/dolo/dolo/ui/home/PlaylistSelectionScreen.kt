package com.dolo.dolo.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.dolo.core.model.PlaylistInfo

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlaylistSelectionScreen(
    playlist: PlaylistInfo,
    onBack: () -> Unit,
    onConfirm: (indices: Set<Int>, isAudio: Boolean, format: String?, bitrate: Int?) -> Unit
) {
    var selectedIndices by remember { mutableStateOf(playlist.entries?.indices?.toSet() ?: emptySet()) }
    var isAudioMode by remember { mutableStateOf(false) }
    var selectedFormat by remember { mutableStateOf("mp3") }
    var selectedBitrate by remember { mutableIntStateOf(192) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(playlist.title ?: "Playlist", style = MaterialTheme.typography.titleMedium)
                        Text("${playlist.entries?.size ?: 0} items", style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        selectedIndices = if (selectedIndices.size == (playlist.entries?.size ?: 0)) emptySet() 
                                         else playlist.entries?.indices?.toSet() ?: emptySet()
                    }) {
                        Text(if (selectedIndices.size == (playlist.entries?.size ?: 0)) "Deselect All" else "Select All")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 8.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Batch Quality Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isAudioMode) Icons.Default.MusicNote else Icons.Default.Movie,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isAudioMode) "Audio (Batch)" else "Video (Batch)", fontWeight = FontWeight.Bold)
                        }
                        Switch(checked = isAudioMode, onCheckedChange = { isAudioMode = it })
                    }
                    
                    if (isAudioMode) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("mp3", "m4a", "opus").forEach { fmt ->
                                FilterChip(
                                    selected = selectedFormat == fmt,
                                    onClick = { selectedFormat = fmt },
                                    label = { Text(fmt.uppercase()) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onConfirm(selectedIndices, isAudioMode, selectedFormat, selectedBitrate) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedIndices.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Download ${selectedIndices.size} Items")
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(playlist.entries ?: emptyList()) { index, entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            selectedIndices = if (selectedIndices.contains(index)) selectedIndices - index 
                                             else selectedIndices + index
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = selectedIndices.contains(index),
                        onCheckedChange = { checked ->
                            selectedIndices = if (checked) selectedIndices + index else selectedIndices - index
                        }
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    if (!entry.thumbnail.isNullOrBlank()) {
                        AsyncImage(
                            model = entry.thumbnail,
                            contentDescription = null,
                            modifier = Modifier.size(width = 80.dp, height = 45.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(width = 80.dp, height = 45.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = entry.title ?: "Unknown Title",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (entry.duration != null && entry.duration!! > 0) {
                            Text(
                                text = formatDuration(entry.duration!!),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    return String.format("%d:%02d", mins, secs)
}
