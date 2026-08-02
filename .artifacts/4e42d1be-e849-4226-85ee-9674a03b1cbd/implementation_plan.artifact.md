# Implementation Plan - Phase 5 Polish & Fixes

This plan addresses the missing UI components and logic gaps in the Phase 5 (Settings & Customization) screens, and ensures settings like Theme are applied globally.

## User Review Required

> [!IMPORTANT]
> The **Save Location** feature will use the Storage Access Framework (SAF). The app will request a folder URI, which allows it to write to SD cards or custom directories without needing legacy storage permissions on modern Android versions.

## Proposed Changes

### [core-engine]

#### [MODIFY] [SettingsRepository.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/repository/SettingsRepository.kt)
- Add `DOWNLOAD_LOCATION_URI` and `DOWNLOAD_LOCATION_NAME` keys to DataStore.

### [app]

#### [NEW] [SettingsComponents.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/settings/SettingsComponents.kt)
- Centralize `SettingsSwitchItem`, `SettingsRadioItem`, and `SettingsSliderItem` for reuse across all settings screens.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/MainActivity.kt)
- Observe `themeMode` from `SettingsViewModel`.
- Apply dark/light theme logic based on preference + system status.

#### [MODIFY] [DownloadSettingsScreen.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/settings/DownloadSettingsScreen.kt)
- Add **Save Location** section with a folder picker launcher.
- Add **Global Speed Limit** slider (0-10 MB/s).
- Use centralized components.

#### [MODIFY] [EngineSettingsScreen.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/settings/EngineSettingsScreen.kt)
- Implement `cookies.txt` file picker.
- Copy selected cookie file to app internal storage for `yt-dlp` to read.
- Wire up the "Check for Engine Update" button to `EngineUpdateWorker`.

#### [MODIFY] [AboutScreen.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/settings/AboutScreen.kt)
- Wire up "Check for App Update" button to `AppUpdateWorker`.

#### [MODIFY] [HomeScreen.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/home/HomeScreen.kt)
- Implement the actual clipboard check logic in `onResume` (only if `clipboardWatcherEnabled` is true in settings).

## Verification Plan

### Manual Verification
1.  **Theme**: Change to Dark/Light in settings and verify the entire app updates instantly.
2.  **Path**: Change the download folder to a custom directory and verify new downloads appear there.
3.  **Cookies**: Import a file and verify the "Currently using" path updates.
4.  **Updates**: Tap "Check for Update" in About/Engine screens and verify (via logs or snackbar) that the worker triggers.
