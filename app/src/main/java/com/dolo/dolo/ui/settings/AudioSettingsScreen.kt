package com.dolo.dolo.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                Text(
                    text = "Default Format",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            listOf("mp3", "m4a", "opus", "flac").forEach { format ->
                item {
                    SettingsRadioItem(
                        title = format.uppercase(),
                        selected = uiState.defaultAudioFormat == format,
                        onClick = { viewModel.updateDefaultAudioFormat(format) }
                    )
                }
            }
            
            item {
                Text(
                    text = "Default Bitrate",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            listOf(128, 192, 256, 320).forEach { bitrate ->
                item {
                    SettingsRadioItem(
                        title = "${bitrate}kbps",
                        selected = uiState.defaultAudioBitrate == bitrate,
                        onClick = { viewModel.updateDefaultAudioBitrate(bitrate) }
                    )
                }
            }

            item {
                SettingsSwitchItem(
                    title = "Embed Metadata",
                    subtitle = "Add thumbnail and tags to audio files",
                    checked = uiState.isMetadataEmbeddingEnabled,
                    onCheckedChange = { viewModel.updateMetadataEmbeddingEnabled(it) }
                )
            }
        }
    }
}
