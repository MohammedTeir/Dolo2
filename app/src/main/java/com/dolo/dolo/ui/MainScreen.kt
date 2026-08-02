package com.dolo.dolo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dolo.core.db.LibraryItemEntity
import com.dolo.dolo.ui.formatpicker.FormatPickerSheet
import com.dolo.dolo.ui.home.HomeScreen
import com.dolo.dolo.ui.home.HomeViewModel
import com.dolo.dolo.ui.library.LibraryScreen
import com.dolo.dolo.ui.player.PlayerScreen
import com.dolo.dolo.ui.queue.DownloadQueueScreen
import com.dolo.dolo.ui.queue.DownloadQueueViewModel

enum class NavigationTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    QUEUE("Downloads", Icons.Filled.Download, Icons.Outlined.Download),
    LIBRARY("Library", Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    sharedUrl: String? = null,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToVault: () -> Unit = {},
    homeViewModel: HomeViewModel = hiltViewModel(),
    queueViewModel: DownloadQueueViewModel = hiltViewModel()
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var playingItem by remember { mutableStateOf<LibraryItemEntity?>(null) }
    
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val queueUiState by queueViewModel.uiState.collectAsStateWithLifecycle()

    val activeCount = queueUiState.activeDownloads.size + queueUiState.queuedDownloads.size

    // Handle back button when player is active
    BackHandler(enabled = playingItem != null) {
        playingItem = null
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationTab.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            if (tab == NavigationTab.QUEUE && activeCount > 0) {
                                BadgedBox(
                                    badge = { Badge { Text(text = "$activeCount") } }
                                ) {
                                    Icon(
                                        imageVector = if (selectedTab == index) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.title
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (selectedTab == index) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title
                                )
                            }
                        },
                        label = { Text(text = tab.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (NavigationTab.entries[selectedTab]) {
                NavigationTab.HOME -> {
                    HomeScreen(
                        sharedUrl = sharedUrl,
                        viewModel = homeViewModel
                    )
                }
                NavigationTab.QUEUE -> {
                    DownloadQueueScreen(
                        viewModel = queueViewModel
                    )
                }
                NavigationTab.LIBRARY -> {
                    LibraryScreen(
                        onPlayItem = { playingItem = it },
                        onNavigateToVault = onNavigateToVault
                    )
                }
                NavigationTab.SETTINGS -> {
                    // This is just a placeholder because we use Navigation to another screen
                    LaunchedEffect(Unit) {
                        onNavigateToSettings()
                        selectedTab = 0 // Reset to home when coming back if we want
                    }
                }
            }

            // FormatPickerSheet overlay
            homeUiState.extractedMetadata?.let { metadata ->
                FormatPickerSheet(
                    metadata = metadata,
                    onDismiss = { homeViewModel.dismissFormatPicker() },
                    onStartDownload = { params ->
                        homeViewModel.startDownload(params)
                        selectedTab = 1 // Switch to downloads tab
                    }
                )
            }
        }
    }

    // Full-screen Player Overlay
    AnimatedVisibility(
        visible = playingItem != null,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        playingItem?.let { item ->
            PlayerScreen(
                filePath = item.filePath,
                title = item.title,
                isAudioOnly = item.isAudio,
                onBackClick = { playingItem = null }
            )
        }
    }
}
