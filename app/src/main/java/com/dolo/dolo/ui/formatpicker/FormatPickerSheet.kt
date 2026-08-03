package com.dolo.dolo.ui.formatpicker

import android.content.Intent
import android.net.Uri
import android.os.Environment
import java.io.File
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.dolo.core.model.DownloadParams
import com.dolo.core.model.FormatInfo
import com.dolo.core.model.VideoMetadata
import com.dolo.dolo.ui.player.PreviewPlayerSheet

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
    val context = androidx.compose.ui.platform.LocalContext.current
    val defaultOutputDir = remember(context) {
        context.getExternalFilesDir(null)?.absolutePath ?: context.cacheDir.absolutePath
    }

    var previewUrl by remember { mutableStateOf<String?>(null) }
    var isTrimEnabled by remember { mutableStateOf(false) }
    val maxDuration = remember(metadata) { (metadata.durationSeconds.takeIf { it > 0 } ?: 300).toFloat() }
    var trimStartSeconds by remember { mutableFloatStateOf(0f) }
    var trimEndSeconds by remember { mutableFloatStateOf(maxDuration) }
    var selectedSubtitleLang by remember { mutableStateOf<String?>(null) }

    fun buildParams(
        formatId: String? = null,
        isAudioOnly: Boolean = false,
        audioFormat: String? = null,
        audioBitrate: Int? = null
    ): DownloadParams {
        return DownloadParams(
            id = "",
            url = metadata.originalUrl,
            formatId = formatId,
            outputDir = defaultOutputDir,
            isAudioOnly = isAudioOnly,
            audioFormat = audioFormat,
            audioBitrate = audioBitrate,
            trimStartSeconds = if (isTrimEnabled) trimStartSeconds else null,
            trimEndSeconds = if (isTrimEnabled) trimEndSeconds else null,
            selectedSubtitleLanguage = selectedSubtitleLang
        )
    }

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
            // Header: Thumbnail + Title + Preview Button
            item {
                MetadataHeader(
                    metadata = metadata,
                    onPreviewClick = {
                        val firstStreamableUrl = metadata.formats.firstOrNull { !it.url.isNullOrEmpty() }?.url
                            ?: metadata.originalUrl
                        previewUrl = firstStreamableUrl
                    }
                )
            }

            // Trim Segment Section
            item {
                TrimSection(
                    durationSeconds = maxDuration,
                    isTrimEnabled = isTrimEnabled,
                    onToggleTrim = { isTrimEnabled = it },
                    startSeconds = trimStartSeconds,
                    endSeconds = trimEndSeconds,
                    onRangeChange = { start, end ->
                        trimStartSeconds = start
                        trimEndSeconds = end
                    }
                )
            }

            // Subtitles Section
            if (metadata.subtitles.isNotEmpty()) {
                item {
                    SubtitleSection(
                        subtitles = metadata.subtitles,
                        selectedLang = selectedSubtitleLang,
                        onLangSelected = { selectedSubtitleLang = it }
                    )
                }
            }

            // Smart Quality Presets
            item {
                SmartQualityPresets(
                    metadata = metadata,
                    onStartDownload = { isAudio, fmtId, audFormat, audBitrate ->
                        onStartDownload(buildParams(formatId = fmtId, isAudioOnly = isAudio, audioFormat = audFormat, audioBitrate = audBitrate))
                    }
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

            // Report issue helper
            item {
                TextButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/youruser/dolo/issues/new?title=Extraction+Issue&body=URL:+${metadata.originalUrl}"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Report extraction issue", style = MaterialTheme.typography.labelSmall)
                }
            }

            // Video formats list
            item {
                AudioOnlySection(
                    onStartDownload = { audFormat, audBitrate ->
                        onStartDownload(buildParams(isAudioOnly = true, audioFormat = audFormat, audioBitrate = audBitrate))
                    }
                )
            }

            // Video formats list
            val videoFormats = metadata.formats.filter { !it.isAudioOnly }
                .sortedByDescending { it: FormatInfo -> 
                    (extractHeight(it.resolution)?.toFloat() ?: it.vbr ?: 0f)
                }

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
                        onPreviewClick = if (!format.url.isNullOrEmpty()) {
                            { previewUrl = format.url }
                        } else null,
                        onClick = {
                            onStartDownload(buildParams(formatId = format.formatId, isAudioOnly = false))
                        }
                    )
                }
            }

            // Audio-only formats from extractor
            val audioFormats = metadata.formats.filter { it.isAudioOnly }
                .sortedByDescending { it.abr ?: it.fileSizeBytes.toFloat() }

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
                        onPreviewClick = if (!format.url.isNullOrEmpty()) {
                            { previewUrl = format.url }
                        } else null,
                        onClick = {
                            onStartDownload(buildParams(formatId = format.formatId, isAudioOnly = true))
                        }
                    )
                }
            }
        }
    }

    // Preview player sheet overlay
    previewUrl?.let { url ->
        PreviewPlayerSheet(
            streamUrl = url,
            title = metadata.title,
            onDismiss = { previewUrl = null }
        )
    }
}

@Composable
private fun MetadataHeader(
    metadata: VideoMetadata,
    onPreviewClick: () -> Unit
) {
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

            val uploader = metadata.uploader
            if (!uploader.isNullOrBlank()) {
                Text(
                    text = uploader,
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

        IconButton(onClick = onPreviewClick) {
            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = "Preview media",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun TrimSection(
    durationSeconds: Float,
    isTrimEnabled: Boolean,
    onToggleTrim: (Boolean) -> Unit,
    startSeconds: Float,
    endSeconds: Float,
    onRangeChange: (Float, Float) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleTrim(!isTrimEnabled) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCut,
                    contentDescription = null,
                    tint = if (isTrimEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isTrimEnabled) "Clip / Trim segment: ON" else "Trim segment (Optional)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (isTrimEnabled) "▲" else "▼",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isTrimEnabled) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Start: ${formatDuration(startSeconds.toInt())}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "End: ${formatDuration(endSeconds.toInt())}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Slider(
                    value = startSeconds,
                    onValueChange = { newStart ->
                        if (newStart < endSeconds - 1f) {
                            onRangeChange(newStart, endSeconds)
                        }
                    },
                    valueRange = 0f..durationSeconds,
                    modifier = Modifier.fillMaxWidth()
                )

                Slider(
                    value = endSeconds,
                    onValueChange = { newEnd ->
                        if (newEnd > startSeconds + 1f) {
                            onRangeChange(startSeconds, newEnd)
                        }
                    },
                    valueRange = 0f..durationSeconds,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubtitleSection(
    subtitles: List<com.dolo.core.model.SubtitleInfo>,
    selectedLang: String?,
    onLangSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
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
                    imageVector = Icons.Default.Subtitles,
                    contentDescription = null,
                    tint = if (selectedLang != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedLang != null) "Subtitles: $selectedLang" else "Subtitles (Optional)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (expanded) "▲" else "▼",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedLang == null,
                        onClick = { onLangSelected(null) },
                        label = { Text("None") }
                    )
                    subtitles.forEach { sub ->
                        FilterChip(
                            selected = selectedLang == sub.language,
                            onClick = { onLangSelected(sub.language) },
                            label = { Text(sub.name ?: sub.language) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartQualityPresets(
    metadata: VideoMetadata,
    onStartDownload: (isAudioOnly: Boolean, formatId: String?, audioFormat: String?, audioBitrate: Int?) -> Unit
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
            PresetButton(
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Default.HighQuality, contentDescription = null, modifier = Modifier.size(18.dp)) },
                label = "Best",
                sublabel = "Quality",
                onClick = { onStartDownload(false, "bestvideo+bestaudio/best", null, null) }
            )

            PresetButton(
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Default.DataSaverOn, contentDescription = null, modifier = Modifier.size(18.dp)) },
                label = "Data",
                sublabel = "Saver",
                onClick = { onStartDownload(false, "bestvideo[height<=480]+bestaudio/best[height<=480]/best", null, null) }
            )

            PresetButton(
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp)) },
                label = "Quick",
                sublabel = "MP3",
                onClick = { onStartDownload(true, null, "mp3", 192) }
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
        modifier = modifier.clickable(onClick = onClick),
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
    onStartDownload: (audioFormat: String, audioBitrate: Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedFormatIndex by remember { mutableIntStateOf(0) }
    var selectedBitrateIndex by remember { mutableIntStateOf(1) }

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

                Button(
                    onClick = {
                        onStartDownload(
                            AUDIO_FORMATS[selectedFormatIndex],
                            BITRATE_OPTIONS[selectedBitrateIndex]
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
    onPreviewClick: (() -> Unit)?,
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
                    
                    val bitrate = if (isVideo) format.vbr else format.abr
                    if (bitrate != null && bitrate > 0) {
                        Text(
                            text = " · ${bitrate.toInt()}k",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
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
                    
                    if (format.formatNote != null && format.formatNote != format.resolution) {
                        Text(
                            text = " · ${format.formatNote}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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

            if (onPreviewClick != null) {
                IconButton(onClick = onPreviewClick) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = "Preview format stream",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

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

private fun extractHeight(resolution: String?): Int? {
    if (resolution == null) return null
    val matchWxH = Regex("""(\d+)x(\d+)""").find(resolution)
    if (matchWxH != null) return matchWxH.groupValues[2].toIntOrNull()
    val matchP = Regex("""(\d+)p""").find(resolution)
    if (matchP != null) return matchP.groupValues[1].toIntOrNull()
    return null
}
