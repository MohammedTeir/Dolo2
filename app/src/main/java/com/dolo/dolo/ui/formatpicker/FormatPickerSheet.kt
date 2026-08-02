package com.dolo.dolo.ui.formatpicker

import android.os.Environment
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DataSaverOn
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.dolo.core.model.DownloadParams
import com.dolo.core.model.FormatInfo
import com.dolo.core.model.VideoMetadata

private val AUDIO_FORMATS = listOf("mp3", "m4a", "opus", "flac")
private val BITRATE_OPTIONS = listOf(128, 192, 256, 320)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatPickerSheet(
    metadata: VideoMetadata,
    onDismiss: () -> Unit,
    onStartDownload: (DownloadParams) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val defaultOutputDir = Environment.getExternalStoragePublicDirectory(
        Environment.DIRECTORY_DOWNLOADS
    ).absolutePath + "/Dolo"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Thumbnail + Title + Uploader
            item {
                MetadataHeader(metadata)
            }

            // Smart Quality Presets
            item {
                SmartQualityPresets(
                    metadata = metadata,
                    outputDir = defaultOutputDir,
                    onStartDownload = onStartDownload
                )
            }

            // Divider
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        text = "Or choose manually",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }
            }

            // Audio-only section
            item {
                AudioOnlySection(
                    metadata = metadata,
                    outputDir = defaultOutputDir,
                    onStartDownload = onStartDownload
                )
            }

            // Video formats list
            val videoFormats = metadata.formats.filter { !it.isAudioOnly && it.resolution != null }
                .sortedByDescending { extractHeight(it.resolution) }
                .distinctBy { "${it.resolution}-${it.ext}" }

            if (videoFormats.isNotEmpty()) {
                item {
                    Text(
                        text = "Video formats",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                items(videoFormats, key = { it.formatId }) { format ->
                    FormatRow(
                        format = format,
                        isVideo = true,
                        onClick = {
                            onStartDownload(
                                DownloadParams(
                                    id = "",
                                    url = metadata.originalUrl,
                                    formatId = format.formatId,
                                    outputDir = defaultOutputDir,
                                    isAudioOnly = false
                                )
                            )
                        }
                    )
                }
            }

            // Audio-only formats from extractor
            val audioFormats = metadata.formats.filter { it.isAudioOnly }
                .sortedByDescending { it.fileSizeBytes }

            if (audioFormats.isNotEmpty()) {
                item {
                    Text(
                        text = "Audio-only formats",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                items(audioFormats, key = { it.formatId }) { format ->
                    FormatRow(
                        format = format,
                        isVideo = false,
                        onClick = {
                            onStartDownload(
                                DownloadParams(
                                    id = "",
                                    url = metadata.originalUrl,
                                    formatId = format.formatId,
                                    outputDir = defaultOutputDir,
                                    isAudioOnly = true
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MetadataHeader(metadata: VideoMetadata) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Thumbnail
        if (!metadata.thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = metadata.thumbnailUrl,
                contentDescription = "Thumbnail",
                modifier = Modifier
                    .size(width = 120.dp, height = 68.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = metadata.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!metadata.uploader.isNullOrBlank()) {
                Text(
                    text = metadata.uploader,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (metadata.durationSeconds > 0) {
                Text(
                    text = formatDuration(metadata.durationSeconds),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SmartQualityPresets(
    metadata: VideoMetadata,
    outputDir: String,
    onStartDownload: (DownloadParams) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Quick download",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Best Quality
            PresetButton(
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Default.HighQuality, contentDescription = null, modifier = Modifier.size(18.dp)) },
                label = "Best",
                sublabel = "Quality",
                onClick = {
                    onStartDownload(
                        DownloadParams(
                            id = "",
                            url = metadata.originalUrl,
                            formatId = "bestvideo+bestaudio/best",
                            outputDir = outputDir,
                            isAudioOnly = false
                        )
                    )
                }
            )

            // Data Saver
            PresetButton(
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Default.DataSaverOn, contentDescription = null, modifier = Modifier.size(18.dp)) },
                label = "Data",
                sublabel = "Saver",
                onClick = {
                    onStartDownload(
                        DownloadParams(
                            id = "",
                            url = metadata.originalUrl,
                            formatId = "bestvideo[height<=480]+bestaudio/best[height<=480]/best",
                            outputDir = outputDir,
                            isAudioOnly = false
                        )
                    )
                }
            )

            // Quick MP3
            PresetButton(
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp)) },
                label = "Quick",
                sublabel = "MP3",
                onClick = {
                    onStartDownload(
                        DownloadParams(
                            id = "",
                            url = metadata.originalUrl,
                            outputDir = outputDir,
                            isAudioOnly = true,
                            audioFormat = "mp3",
                            audioBitrate = 192
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun PresetButton(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    label: String,
    sublabel: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            icon()
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = sublabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioOnlySection(
    metadata: VideoMetadata,
    outputDir: String,
    onStartDownload: (DownloadParams) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedFormatIndex by remember { mutableIntStateOf(0) } // mp3 default
    var selectedBitrateIndex by remember { mutableIntStateOf(1) } // 192 default

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AudioFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Audio conversion",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (expanded) "▲" else "▼",
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))

                // Format selector
                Text(
                    text = "Format",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    AUDIO_FORMATS.forEachIndexed { index, format ->
                        SegmentedButton(
                            selected = selectedFormatIndex == index,
                            onClick = { selectedFormatIndex = index },
                            shape = SegmentedButtonDefaults.itemShape(index, AUDIO_FORMATS.size)
                        ) {
                            Text(text = format.uppercase(), fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bitrate selector
                Text(
                    text = "Bitrate: ${BITRATE_OPTIONS[selectedBitrateIndex]}kbps",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BITRATE_OPTIONS.forEachIndexed { index, bitrate ->
                        FilterChip(
                            selected = selectedBitrateIndex == index,
                            onClick = { selectedBitrateIndex = index },
                            label = { Text(text = "${bitrate}k", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Download audio button
                Button(
                    onClick = {
                        onStartDownload(
                            DownloadParams(
                                id = "",
                                url = metadata.originalUrl,
                                outputDir = outputDir,
                                isAudioOnly = true,
                                audioFormat = AUDIO_FORMATS[selectedFormatIndex],
                                audioBitrate = BITRATE_OPTIONS[selectedBitrateIndex]
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Download ${AUDIO_FORMATS[selectedFormatIndex].uppercase()}")
                }
            }
        }
    }
}

@Composable
private fun FormatRow(
    format: FormatInfo,
    isVideo: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isVideo) Icons.Default.VideoFile else Icons.Default.AudioFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = format.resolution ?: format.formatNote ?: format.ext,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (format.fps > 0 && isVideo) {
                        Text(
                            text = " · ${format.fps}fps",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    Text(
                        text = format.ext.uppercase(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val codec = if (isVideo) format.videoCodec else format.audioCodec
                    if (!codec.isNullOrBlank() && codec != "none") {
                        Text(
                            text = " · $codec",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (format.fileSizeBytes > 0) {
                Text(
                    text = formatFileSize(format.fileSizeBytes),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = "Download",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val hrs = totalSeconds / 3600
    val mins = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60
    return if (hrs > 0) {
        String.format("%d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format("%d:%02d", mins, secs)
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824 -> String.format("%.1f GB", bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> String.format("%.1f MB", bytes / 1_048_576.0)
        bytes >= 1024 -> String.format("%.0f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}

private fun extractHeight(resolution: String?): Int {
    if (resolution == null) return 0
    // Try extracting height from "WxH" format
    val matchWxH = Regex("""(\d+)x(\d+)""").find(resolution)
    if (matchWxH != null) return matchWxH.groupValues[2].toIntOrNull() ?: 0
    // Try extracting from "Hp" format
    val matchP = Regex("""(\d+)p""").find(resolution)
    if (matchP != null) return matchP.groupValues[1].toIntOrNull() ?: 0
    return 0
}
