# Implementation Plan - Phase 7: Batch, Playlists & Channels

This phase implements advanced extraction for playlists and channels, a dedicated selection UI for batches, and automatic subfolder organization for playlist downloads.

## Proposed Changes

### [core-engine]

#### [MODIFY] [YtDlpExtractor.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/engine/YtDlpExtractor.kt)
- Add `extractPlaylist(url: String)`: Use `YoutubeDLRequest` with `--flat-playlist` and `--dump-single-json` to get the list of entries without resolving full formats for each.
- Since `youtubedl-android`'s `VideoInfo` mapper might not support the `entries` field, we will execute the request and parse the raw JSON output if needed, or check if the library supports `PlaylistInfo`.

#### [MODIFY] [FileNamer.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/util/FileNamer.kt)
- Add `generatePlaylistFolder(title: String)`: Sanitizes the playlist title to create a safe subfolder name.
- Enhance `generateFileName` to better handle index prefixing for playlist items.

#### [MODIFY] [DownloadRepository.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/repository/DownloadRepository.kt)
- Add `queueBatch(paramsList: List<DownloadParams>)` to enqueue multiple items at once.

### [app]

#### [NEW] [PlaylistSelectionScreen.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/home/PlaylistSelectionScreen.kt)
- Displays playlist metadata (title, item count).
- List of entries with checkboxes and thumbnails.
- "Select All" / "Deselect All" options.
- **Batch Quality Preset**: One-tap selection to apply "1080p Video" or "Best Audio" to all selected items.
- "Add to Queue" button.

#### [MODIFY] [HomeViewModel.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/home/HomeViewModel.kt)
- Update `extractInfo` logic:
    - If URL contains `list=` and `v=`, show a dialog: "Download just this video or the whole playlist?".
    - If it's a playlist/channel URL, navigate to `PlaylistSelectionScreen`.
- Handle batch queueing results.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/MainActivity.kt)
- Add navigation route for `PlaylistSelectionScreen`.

#### [MODIFY] [MainScreen.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/MainScreen.kt)
- Handle the "Video vs Playlist" choice dialog.

## User Review Required

> [!NOTE]
> Extracting full metadata (formats) for every item in a 100-video playlist up front is slow. We will use a **Flat Extraction** first (Titles + IDs only) and resolve the actual media URLs only when the download starts for each item in the service.

## Verification Plan

### Manual Verification
1.  **Playlist Detection**: Paste a YouTube playlist link. Verify the app navigates to `PlaylistSelectionScreen`.
2.  **Batch Selection**: Select 3 specific videos from a playlist, choose "Quick MP3" preset, and tap "Download". Verify 3 tasks appear in the Queue.
3.  **Subfolders**: Verify the 3 downloaded files are saved inside a folder named after the playlist in your Download directory.
4.  **Ambiguity**: Paste a link that is both a video and part of a playlist. Verify the prompt appears correctly.
