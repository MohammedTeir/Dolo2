package com.dolo.dolo.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(it, flags)
            
            // Get folder name from URI if possible
            val folderName = it.lastPathSegment?.split(":")?.lastOrNull() ?: "Custom Folder"
            viewModel.updateDownloadLocation(it.toString(), folderName)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Download Settings") },
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
                    text = "Location",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item {
                SettingsClickableItem(
                    title = "Download Folder",
                    subtitle = uiState.downloadLocationName ?: "Public Downloads/Dolo",
                    onClick = { folderPickerLauncher.launch(null) }
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            item {
                SettingsSwitchItem(
                    title = "Wi-Fi Only",
                    subtitle = "Pause downloads when not on Wi-Fi",
                    checked = uiState.isWifiOnly,
                    onCheckedChange = { viewModel.updateWifiOnly(it) }
                )
            }
            item {
                SettingsSliderItem(
                    title = "Max Concurrent Downloads",
                    subtitle = "Current: ${uiState.maxConcurrentDownloads}",
                    value = uiState.maxConcurrentDownloads.toFloat(),
                    valueRange = 1f..5f,
                    steps = 3,
                    onValueChange = { viewModel.updateMaxConcurrentDownloads(it.toInt()) }
                )
            }
            
            item {
                val speedText = if (uiState.globalSpeedLimitKbps == 0) "Unlimited" 
                                else "${uiState.globalSpeedLimitKbps / 1024} MB/s"
                SettingsSliderItem(
                    title = "Global Speed Limit",
                    subtitle = "Current: $speedText",
                    value = (uiState.globalSpeedLimitKbps / 1024).toFloat(),
                    valueRange = 0f..10f,
                    steps = 9,
                    onValueChange = { viewModel.updateGlobalSpeedLimit(it.toInt() * 1024) }
                )
            }

            item {
                SettingsSliderItem(
                    title = "Connections per Download",
                    subtitle = "Current: ${uiState.connectionsPerDownload}",
                    value = uiState.connectionsPerDownload.toFloat(),
                    valueRange = 1f..16f,
                    steps = 14,
                    onValueChange = { viewModel.updateConnectionsPerDownload(it.toInt()) }
                )
            }
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            item {
                SettingsSwitchItem(
                    title = "Organize Playlists",
                    subtitle = "Save playlist items in dedicated folders",
                    checked = uiState.organizePlaylistsInFolders,
                    onCheckedChange = { viewModel.updateOrganizePlaylistsInFolders(it) }
                )
            }
        }
    }
}
