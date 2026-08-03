package com.dolo.dolo.ui.formatpicker

import android.content.Intent
import android.net.Uri
import java.io.File
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

    var selectedFormatId by remember { mutableStateOf<String?>(null) }
    var selectedType by remember { mutableStateOf("video") } // music, video, technical
    var musicFormat by remember { mutableStateOf("mp3") }
    var musicBitrate by remember { mutableIntStateOf(128) }
    
    var isMoreFormatsExpanded by remember { mutableStateOf(false) }
    var selectedSubtitleLang by remember { mutableStateOf<String?>(null) }
    var previewUrl by remember { mutableStateOf<String?>(null) }

    val maxDuration = remember(metadata) { (metadata.durationSeconds.takeIf { it > 0 } ?: 0) }

    fun onDownloadClick() {
        val params = when (selectedType) {
            "music" -> DownloadParams(
                id = "",
                url = metadata.originalUrl,
                outputDir = defaultOutputDir,
                isAudioOnly = true,
                audioFormat = musicFormat,
                audioBitrate = musicBitrate,
                selectedSubtitleLanguage = selectedSubtitleLang
            )
            else -> DownloadParams(
                id = "",
                url = metadata.originalUrl,
                formatId = selectedFormatId,
                outputDir = defaultOutputDir,
                isAudioOnly = false,
                selectedSubtitleLanguage = selectedSubtitleLang
            )
        }
        onStartDownload(params)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "Download video as",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // --- MUSIC SECTION ---
                item {
                    SectionHeader("Music")
                }

                item {
                    SelectionRow(
                        title = "Fast",
                        subtitle = "M4A (128K), best for mobile play",
                        sizeMb = estimateSizeMb(128, maxDuration),
                        isSelected = selectedType == "music" && musicFormat == "m4a",
                        onClick = { 
                            selectedType = "music"; musicFormat = "m4a"; musicBitrate = 128; selectedFormatId = null 
                        }
                    )
                }
                
                item {
                    SelectionRow(
                        title = "Classic MP3 (128K)",
                        subtitle = "High compatibility for all devices",
                        sizeMb = estimateSizeMb(128, maxDuration),
                        isSelected = selectedType == "music" && musicFormat == "mp3" && musicBitrate == 128,
                        onClick = { 
                            selectedType = "music"; musicFormat = "mp3"; musicBitrate = 128; selectedFormatId = null 
                        }
                    )
                }

                item {
                    SelectionRow(
                        title = "High Quality MP3 (320K)",
                        subtitle = "Best audio details",
                        sizeMb = estimateSizeMb(320, maxDuration),
                        isSelected = selectedType == "music" && musicFormat == "mp3" && musicBitrate == 320,
                        onClick = { 
                            selectedType = "music"; musicFormat = "mp3"; musicBitrate = 320; selectedFormatId = null 
                        }
                    )
                }

                // --- VIDEO SECTION ---
                item {
                    SectionHeader("Video")
                }

                val topResolutions = listOf(360, 480, 720, 1080, 1440, 2160)
                val videoFormats = metadata.formats.filter { !it.isAudioOnly }
                
                topResolutions.forEach { res ->
                    val format = videoFormats.filter { extractHeight(it.resolution) == res }
                        .sortedByDescending { it.vbr ?: 0f }
                        .firstOrNull()
                    
                    if (format != null) {
                        item {
                            SelectionRow(
                                title = getDescriptiveLabel(res),
                                subtitle = if (res >= 720) "High details for big screen" else "Good for mobile play",
                                sizeMb = if (format.fileSizeBytes > 0) format.fileSizeBytes / 1024f / 1024f else null,
                                isSelected = selectedType == "video" && selectedFormatId == format.formatId,
                                onClick = { 
                                    selectedType = "video"; selectedFormatId = format.formatId 
                                }
                            )
                        }
                    }
                }

                // --- MORE FORMATS ---
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isMoreFormatsExpanded = !isMoreFormatsExpanded }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "More formats",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "All",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (isMoreFormatsExpanded) {
                    items(videoFormats) { format ->
                        SelectionRow(
                            title = "${format.resolution ?: "Video"} (${format.ext})",
                            subtitle = "${format.videoCodec} · ${format.vbr?.toInt() ?: 0}k",
                            sizeMb = format.fileSizeBytes / 1024f / 1024f,
                            isSelected = selectedType == "technical" && selectedFormatId == format.formatId,
                            onClick = { selectedType = "technical"; selectedFormatId = format.formatId }
                        )
                    }
                }

                // --- SUBTITLES ---
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { /* Show subtitle selector */ }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ClosedCaption,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Subtitles/CC",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = selectedSubtitleLang ?: "None",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            // Bottom Download Button
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Button(
                    onClick = { onDownloadClick() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD600), // Snaptube Yellow
                        contentColor = Color.Black
                    ),
                    enabled = selectedType == "music" || selectedFormatId != null
                ) {
                    Text(
                        text = "Download",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun SelectionRow(
    title: String,
    subtitle: String,
    sizeMb: Float?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = if (title.contains("MP3") || title.contains("Fast") && !title.contains("p")) 
                   Icons.Default.MusicNote else Icons.Default.PlayArrow
        
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (sizeMb != null) {
            Text(
                text = if (sizeMb >= 1024) String.format("%.1f GB", sizeMb / 1024) 
                       else String.format("%.1f MB", sizeMb),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
        
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFFD600))
        )
    }
}

private fun estimateSizeMb(bitrateKbps: Int, durationSeconds: Int): Float {
    if (durationSeconds <= 0) return 0f
    return (bitrateKbps / 8f) * durationSeconds / 1024f
}

private fun getDescriptiveLabel(resHeight: Int): String {
    return when (resHeight) {
        144, 240 -> "Fast ($resHeight" + "p)"
        360, 480 -> "Standard ($resHeight" + "p)"
        720, 1080 -> "High quality ($resHeight" + "p)"
        1440 -> "2K"
        2160 -> "4K"
        else -> "$resHeight" + "p"
    }
}

private fun extractHeight(resolution: String?): Int {
    if (resolution == null) return 0
    val matchWxH = Regex("""(\d+)x(\d+)""").find(resolution)
    if (matchWxH != null) return matchWxH.groupValues[2].toIntOrNull() ?: 0
    val matchP = Regex("""(\d+)p""").find(resolution)
    if (matchP != null) return matchP.groupValues[1].toIntOrNull() ?: 0
    return 0
}
