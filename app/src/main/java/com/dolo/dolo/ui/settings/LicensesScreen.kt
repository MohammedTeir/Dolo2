package com.dolo.dolo.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class LicenseInfo(
    val name: String,
    val author: String,
    val license: String,
    val url: String
)

private val LICENSES = listOf(
    LicenseInfo("yt-dlp", "yt-dlp team", "The Unlicense", "https://github.com/yt-dlp/yt-dlp"),
    LicenseInfo("aria2c", "Tatsuhiro Tsujikawa", "GPL v2", "https://aria2.github.io/"),
    LicenseInfo("FFmpeg", "FFmpeg team", "LGPL v2.1+", "https://ffmpeg.org/"),
    LicenseInfo("mutagen", "Quod Libet team", "GPL v2", "https://github.com/quodlibet/mutagen"),
    LicenseInfo("youtubedl-android", "yausername / junkfood02", "GPL v3", "https://github.com/junkfood02/youtubedl-android"),
    LicenseInfo("Jetpack Compose", "Google", "Apache 2.0", "https://developer.android.com/jetpack/compose"),
    LicenseInfo("Hilt", "Google", "Apache 2.0", "https://dagger.dev/hilt/"),
    LicenseInfo("Coil", "Coil Contributors", "Apache 2.0", "https://coil-kt.github.io/coil/")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Licenses") },
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
            items(LICENSES) { license ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = license.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "by ${license.author}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = license.license,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = license.url,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}
