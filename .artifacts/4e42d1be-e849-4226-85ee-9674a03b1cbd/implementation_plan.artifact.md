# Implementation Plan - Permission & Download Fixes

This plan addresses the "Storage Permission Required" loop and the "No such file or directory" download error on modern Android versions.

## Proposed Changes

### [app]

#### [MODIFY] [OnboardingScreen.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/onboarding/OnboardingScreen.kt)
- Add a `LaunchedEffect(Unit)` to check if storage permissions are already granted.
- If granted, automatically call `viewModel.initializeEngine()` to skip the permission prompt.
- This ensures users who already gave permissions aren't stuck on the onboarding screen.

#### [MODIFY] [DownloadService.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/service/DownloadService.kt)
- **Robust Path Handling**: Instead of using the public Download folder as the primary output for `yt-dlp`, we will use `context.getExternalFilesDir(null)` or `cacheDir`. These directories are always writable by the app without special permissions.
- **Post-Download Move**: Once the download is complete in the private/scoped directory, we will move it to the final destination (Public Downloads or SAF location) using the `StorageResolver`.
- **Error Handling**: Improve error logging to identify exactly why `yt-dlp` is failing with `Errno 2`.

### [core-engine]

#### [MODIFY] [StorageResolver.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/util/StorageResolver.kt)
- Ensure it handles moving files to the public `Download` folder on Android 10+ using `MediaStore` if a SAF URI is not provided.
- This bypasses the restriction on direct file path access in public directories.

## Verification Plan

### Manual Verification
1.  **Permission Skip**:
    - Grant storage permissions to Dolo in system settings.
    - Launch the app.
    - Verify it skips the "Grant Permission" screen and goes straight to engine initialization.
2.  **Download Success**:
    - Paste a link and start a download.
    - Verify the download completes without the "No such file or directory" error.
    - Verify the file appears in the phone's "Download/Dolo" folder and the Gallery.
