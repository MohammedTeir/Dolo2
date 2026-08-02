# Implementation Plan - SPEC Full Implementation & Polish

This plan addresses all missing functionalities and malfunctions identified in the audit of Phases 0 to 6, bringing the app to full SPEC compliance.

## Proposed Changes

### [core-engine]

#### [MODIFY] [VideoMetadata.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/model/VideoMetadata.kt)
- Add `subtitles: List<SubtitleInfo>` field.
- Add `SubtitleInfo` data class (language, url, ext).

#### [MODIFY] [YtDlpExtractor.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/engine/YtDlpExtractor.kt)
- Parse subtitles from `VideoInfo` and include them in `VideoMetadata`.

#### [MODIFY] [DownloadService.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/service/DownloadService.kt)
- Read and apply settings: `globalSpeedLimitKbps`, `isMetadataEmbeddingEnabled`, and `namingMode`.
- Pass `speedLimitKbps` to `DownloadRequestBuilder`.
- Fix: Ensure `outputDir` handles SAF paths if possible, or fallback to internal if invalid. (Note: `yt-dlp` requires a file path; will add logic to resolve SAF URI to path where possible).

### [app]

#### [NEW] Browser & Search
- **`BrowserScreen.kt`**: In-app browser with `WebView`, navigation controls, and `WebDownloadInterceptor` integration.
- **`SearchScreen.kt`**: Simple search interface using `yt-dlp`'s search capabilities (e.g., `ytsearch:`).

#### [MODIFY] [MainScreen.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/MainScreen.kt)
- Add **Browser** and **Search** tabs to the bottom navigation bar.

#### [MODIFY] [FormatPickerSheet.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/formatpicker/FormatPickerSheet.kt)
- Add **Subtitles** section to allow language selection.

#### [MODIFY] [DownloadQueueScreen.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/queue/DownloadQueueScreen.kt)
- Add **Speed Limit** control (icon/popup) to each item in the queue.

#### [MODIFY] [VaultScreen.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/vault/VaultScreen.kt)
- Polish to use `LibraryList` and `LibraryGrid` components for a rich, consistent experience.

#### [NEW] [LicensesScreen.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/settings/LicensesScreen.kt)
- Display GPL/LGPL licenses and source links for yt-dlp, aria2c, ffmpeg, mutagen, etc.

#### [MODIFY] [AboutScreen.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/settings/AboutScreen.kt)
- Add navigation to `LicensesScreen`.

## Verification Plan

### Manual Verification
1.  **Browser**: Open "Browser" tab, navigate to a site, and verify the download is intercepted.
2.  **Search**: Open "Search" tab, type a query, and verify results are extracted.
3.  **Subtitles**: Pick a video with subtitles, select a language in `FormatPickerSheet`, and verify the `.vtt`/`.srt` file is downloaded.
4.  **Vault**: Open Vault and verify it looks identical to the Library (with grid/list toggle).
5.  **Settings**: Change Theme, Speed Limit, and Metadata toggle; verify all are applied in the next download.
6.  **Licenses**: Open "About" -> "Licenses" and verify all credits are present.
