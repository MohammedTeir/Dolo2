package com.dolo.dolo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.dolo.core.repository.SettingsRepository
import com.dolo.dolo.ui.onboarding.OnboardingScreen
import com.dolo.dolo.ui.settings.AboutScreen
import com.dolo.dolo.ui.settings.AudioSettingsScreen
import com.dolo.dolo.ui.settings.DownloadSettingsScreen
import com.dolo.dolo.ui.settings.EngineSettingsScreen
import com.dolo.dolo.ui.settings.GeneralSettingsScreen
import com.dolo.dolo.ui.settings.SettingsHubScreen
import com.dolo.dolo.ui.theme.DoloTheme
import com.dolo.dolo.ui.vault.VaultAuthScreen
import com.dolo.dolo.ui.vault.VaultScreen
import com.dolo.dolo.ui.vault.VaultSetupScreen
import com.dolo.dolo.ui.vault.VaultViewModel
import com.dolo.dolo.ui.home.PlaylistSelectionScreen
import com.dolo.dolo.ui.home.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private var sharedUrl by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            val themeMode by settingsRepository.themeMode.collectAsState(initial = "System")
            val darkTheme = when (themeMode) {
                "Light" -> false
                "Dark" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            var showChangelog by remember { mutableStateOf(false) }
            val lastSeenVersion by settingsRepository.lastCheckedAppVersion.collectAsState(initial = null)
            
            LaunchedEffect(lastSeenVersion) {
                val currentVersion = "1.0"
                if (lastSeenVersion != null && lastSeenVersion != currentVersion) {
                    showChangelog = true
                }
                settingsRepository.updateLastCheckedAppVersion(currentVersion)
            }

            DoloTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val homeViewModel: HomeViewModel = hiltViewModel()
                    val homeState by homeViewModel.uiState.collectAsState()

                    LaunchedEffect(homeState.extractedPlaylist) {
                        if (homeState.extractedPlaylist != null) {
                            navController.navigate("playlist_selection")
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = "onboarding",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("onboarding") {
                            OnboardingScreen(
                                onOnboardingComplete = {
                                    navController.navigate("main") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("main") {
                            val vaultViewModel: VaultViewModel = hiltViewModel()
                            com.dolo.dolo.ui.MainScreen(
                                sharedUrl = sharedUrl,
                                onNavigateToSettings = { navController.navigate("settings_hub") },
                                onNavigateToVault = {
                                    if (!vaultViewModel.isInitialized()) {
                                        navController.navigate("vault_setup")
                                    } else if (!vaultViewModel.isAuthenticated.value) {
                                        navController.navigate("vault_auth")
                                    } else {
                                        navController.navigate("vault_screen")
                                    }
                                },
                                homeViewModel = homeViewModel
                            )
                        }

                        composable("playlist_selection") {
                            homeState.extractedPlaylist?.let { playlist ->
                                PlaylistSelectionScreen(
                                    playlist = playlist,
                                    onBack = { 
                                        homeViewModel.clearState()
                                        navController.popBackStack() 
                                    },
                                    onConfirm = { indices, isAudio, format, bitrate ->
                                        homeViewModel.queuePlaylist(playlist, indices, isAudio, format, bitrate)
                                        navController.popBackStack()
                                    }
                                )
                            }
                        }

                        composable("vault_setup") {
                            VaultSetupScreen(
                                onBack = { navController.popBackStack() },
                                onSetupComplete = {
                                    navController.navigate("vault_screen") {
                                        popUpTo("vault_setup") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("vault_auth") {
                            VaultAuthScreen(
                                onBack = { navController.popBackStack() },
                                onAuthSuccess = {
                                    navController.navigate("vault_screen") {
                                        popUpTo("vault_auth") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("vault_screen") {
                            VaultScreen(
                                onBack = { navController.popBackStack() },
                                onPlayItem = { /* Handle play */ }
                            )
                        }

                        composable("settings_hub") {
                            SettingsHubScreen(
                                onBack = { navController.popBackStack() },
                                onNavigateToGeneral = { navController.navigate("settings_general") },
                                onNavigateToDownload = { navController.navigate("settings_download") },
                                onNavigateToAudio = { navController.navigate("settings_audio") },
                                onNavigateToEngine = { navController.navigate("settings_engine") },
                                onNavigateToAbout = { navController.navigate("settings_about") }
                            )
                        }

                        composable("settings_general") {
                            GeneralSettingsScreen(onBack = { navController.popBackStack() })
                        }

                        composable("settings_download") {
                            DownloadSettingsScreen(onBack = { navController.popBackStack() })
                        }

                        composable("settings_audio") {
                            AudioSettingsScreen(onBack = { navController.popBackStack() })
                        }

                        composable("settings_engine") {
                            EngineSettingsScreen(onBack = { navController.popBackStack() })
                        }

                        composable("settings_about") {
                            AboutScreen(
                                onBack = { navController.popBackStack() },
                                onNavigateToLicenses = { navController.navigate("settings_licenses") }
                            )
                        }

                        composable("settings_licenses") {
                            com.dolo.dolo.ui.settings.LicensesScreen(onBack = { navController.popBackStack() })
                        }
                    }

                    if (showChangelog) {
                        AlertDialog(
                            onDismissRequest = { showChangelog = false },
                            title = { Text("What's New") },
                            text = { 
                                Text("Welcome to Dolo v1.0!\n\n" +
                                     "- Playlist & Channel downloads\n" +
                                     "- Secure Private Vault\n" +
                                     "- Universal In-app Browser\n" +
                                     "- Smart Content Search\n" +
                                     "- Multi-connection aria2c engine") 
                            },
                            confirmButton = {
                                Button(onClick = { showChangelog = false }) {
                                    Text("Awesome!")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        
        when (intent.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (!text.isNullOrBlank()) {
                        sharedUrl = text.trim()
                    }
                }
            }
            "com.dolo.dolo.ACTION_PASTE_DOWNLOAD" -> {
                sharedUrl = "CLIPBOARD_PASTE_ACTION"
            }
        }
    }
}
