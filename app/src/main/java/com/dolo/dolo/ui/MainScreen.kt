package com.dolo.dolo.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dolo.core.model.DownloadParams
import com.dolo.dolo.ui.formatpicker.FormatPickerSheet
import com.dolo.dolo.ui.home.HomeScreen
import com.dolo.dolo.ui.home.HomeViewModel
import com.dolo.dolo.ui.library.LibraryScreen
import com.dolo.dolo.ui.queue.DownloadQueueScreen
import com.dolo.dolo.ui.queue.DownloadQueueViewModel

enum class NavigationTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    QUEUE("Downloads", Icons.Filled.Download, Icons.Outlined.Download),
    LIBRARY("Library", Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary)
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    sharedUrl: String? = null,
    homeViewModel: HomeViewModel = hiltViewModel(),
    queueViewModel: DownloadQueueViewModel = hiltViewModel()
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val queueUiState by queueViewModel.uiState.collectAsStateWithLifecycle()

    val activeCount = queueUiState.activeDownloads.size + queueUiState.queuedDownloads.size

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
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(innerPadding)) {
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
                    LibraryScreen()
                }
            }

            // FormatPickerSheet overlay
            homeUiState.extractedMetadata?.let { metadata ->
                FormatPickerSheet(
                    metadata = metadata,
                    onDismiss = { homeViewModel.dismissFormatPicker() },
                    onStartDownload = { params ->
                        homeViewModel.startDownload(params)
                        // Switch to downloads tab to show progress
                        selectedTab = 1
                    }
                )
            }
        }
    }
}
