# Implementation Plan - Phase 8: Hardening & Release Prep

Finalize the app for a stable release by implementing comprehensive error handling, ensuring license compliance, and polishing the user experience.

## Proposed Changes

### [core-engine]

#### [MODIFY] [YtDlpExtractor.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/engine/YtDlpExtractor.kt)
- Add specialized error mapping:
    - Detect "Unsupported URL" and return a user-friendly message.
    - Detect "Private video" or "Age restricted" and suggest using Cookies.
    - Handle network timeouts gracefully.

#### [MODIFY] [DownloadService.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/service/DownloadService.kt)
- Improve error reporting in the notification and database.
- Add "Retry" action directly to the notification if a download fails.

### [app]

#### [MODIFY] [HomeScreen.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/home/HomeScreen.kt)
- Show a **Snackbar** with a "Help" action when extraction fails, leading to the Engine Settings (to update yt-dlp or add cookies).

#### [MODIFY] [LicensesScreen.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/settings/LicensesScreen.kt)
- Add a section for "Third-party Assets" (icons, etc.).
- Ensure every library mentioned in `libs.versions.toml` has a corresponding license entry.

#### [MODIFY] [AboutScreen.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/settings/AboutScreen.kt)
- Add a "Privacy Policy" button that opens a simple in-app dialog or a link to the repo.
- Finalize the "Feedback" button to open an email intent.

#### [MODIFY] [FormatPickerSheet.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/formatpicker/FormatPickerSheet.kt)
- Add a "Report Issue" button if metadata extraction looks wrong.

## Verification Plan

### Manual Verification
1.  **Error States**: Paste an invalid URL and verify the error message is clear and helpful.
2.  **Privacy/Age Restricted**: Paste a private video link; verify the app suggests adding cookies.
3.  **Licenses**: Open the Licenses screen and verify it is complete and professional.
4.  **RTL Check**: Switch phone language to Arabic (or similar RTL) and verify layouts mirror correctly.
5.  **Build**: Perform a clean `./gradlew assembleRelease` to ensure the app is ready for distribution.
