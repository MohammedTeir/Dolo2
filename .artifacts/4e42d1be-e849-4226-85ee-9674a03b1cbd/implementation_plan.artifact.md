# Implementation Plan - Phase 5: Settings & Customization

This phase focuses on building the settings infrastructure, UI hub, and advanced features like batch import, engine updates, and app self-updates.

## Proposed Changes

### [core-engine]

#### [MODIFY] [SettingsRepository.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/repository/SettingsRepository.kt)
- Expand with new settings:
    - `defaultAudioFormat` (MP3, M4A, OPUS, FLAC)
    - `defaultAudioBitrate` (128, 192, 256, 320)
    - `isMetadataEmbeddingEnabled` (Boolean, default true)
    - `themeMode` (Follow System, Light, Dark)
    - `namingMode` (Clean Title, Original Filename)
    - `organizePlaylistsInFolders` (Boolean, default true)
    - `clipboardWatcherEnabled` (Boolean, default false)
    - `lastCheckedAppVersion` (String)

#### [NEW] [AppUpdateChecker.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/util/AppUpdateChecker.kt)
- Utility to check GitHub Releases API for the latest tag and compare with current `BuildConfig.VERSION_NAME`.

#### [NEW] [EngineUpdateWorker.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/worker/EngineUpdateWorker.kt)
- Periodic WorkManager job to call `youtubeDL.updateYoutubeDL(context)`.

#### [NEW] [AppUpdateWorker.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/worker/AppUpdateWorker.kt)
- Periodic WorkManager job to check for app updates.

### [app]

#### [NEW] Settings UI Screens
- **`SettingsHubScreen.kt`**: The main settings menu.
- **`GeneralSettingsScreen.kt`**: Theme, notifications.
- **`DownloadSettingsScreen.kt`**: Quality, concurrency, path, speed limit.
- **`AudioSettingsScreen.kt`**: Format, bitrate, metadata.
- **`EngineSettingsScreen.kt`**: Version, update, cookies.
- **`AboutScreen.kt`**: Privacy, version, update check button.

#### [NEW] [BatchImportSheet.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/home/BatchImportSheet.kt)
- Multi-line text field for pasting multiple URLs, validating them, and queueing.

#### [MODIFY] [HomeScreen.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/home/HomeScreen.kt)
- Add "Batch Import" icon/button.
- Implement on-resume clipboard check logic (if enabled).

#### [MODIFY] [MainActivity.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/MainActivity.kt)
- Add navigation routes for all settings screens.
- Implement app shortcut handling.

#### [NEW] `res/xml/shortcuts.xml`
- Define "Paste & Download" static shortcut.

## Verification Plan

### Automated Tests
- Unit tests for `AppUpdateChecker` parsing logic.
- Unit tests for `SettingsRepository` saving/loading new fields.

### Manual Verification
1.  **Settings**: Toggle every setting and verify it persists (reopen app).
2.  **Theme**: Change theme mode and verify UI updates immediately.
3.  **Batch Import**: Paste 3 URLs, verify all are extracted/queued.
4.  **Update Check**: Trigger manual update check in About screen.
5.  **Clipboard**: Enable watcher, copy a link, return to app -> verify prompt appears.
