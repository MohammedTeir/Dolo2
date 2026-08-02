package com.dolo.dolo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dolo.dolo.ui.onboarding.OnboardingScreen
import com.dolo.dolo.ui.settings.AboutScreen
import com.dolo.dolo.ui.settings.AudioSettingsScreen
import com.dolo.dolo.ui.settings.DownloadSettingsScreen
import com.dolo.dolo.ui.settings.EngineSettingsScreen
import com.dolo.dolo.ui.settings.GeneralSettingsScreen
import com.dolo.dolo.ui.settings.SettingsHubScreen
import com.dolo.dolo.ui.theme.DoloTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var sharedUrl by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            DoloTheme {
                val navController = rememberNavController()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
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
                            com.dolo.dolo.ui.MainScreen(
                                sharedUrl = sharedUrl,
                                onNavigateToSettings = { navController.navigate("settings_hub") }
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
                            AboutScreen(onBack = { navController.popBackStack() })
                        }
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
