# Phase 3 Implementation Plan — Playback & Library Polish

This plan details the technical steps to implement **Phase 3: Playback & Library Polish** as defined in `SPEC.md`. It adds native Media3/ExoPlayer video and audio playback, streaming preview before download, clip trimming range selection, and advanced Library management features (grid/list view, multi-select, renaming, sorting, and bulk actions).

## Proposed Changes

### Dependency Configuration

#### [MODIFY] [libs.versions.toml](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/gradle/libs.versions.toml)
- Add `media3 = "1.6.0"` (or latest stable) to `[versions]`.
- Add `androidx-media3-exoplayer` and `androidx-media3-ui` under `[libraries]`.

#### [MODIFY] [app/build.gradle.kts](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/app/build.gradle.kts)
- Add dependencies for Media3 ExoPlayer and UI modules (`androidx.media3:media3-exoplayer` and `androidx.media3:media3-ui`).

---

### Playback & Preview Components

#### [NEW] [PlayerScreen.kt](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/player/PlayerScreen.kt)
- Create full-screen video/audio player hosted via Compose `AndroidView(factory = { PlayerView(...) })`.
- Bind `ExoPlayer` instance with lifecycle awareness (`DisposableEffect` to release player on dispose).
- Support play/pause, seek forward/backward (10s), scrub bar, duration display, aspect ratio toggling, and close/back button.

#### [NEW] [PreviewPlayerSheet.kt](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/player/PreviewPlayerSheet.kt)
- Modal bottom sheet displaying a lightweight `ExoPlayer` instance streaming direct media URL (from yt-dlp `VideoMetadata` / `FormatInfo`).
- Allows users to preview and scrub content before committing storage/bandwidth for download.

---

### FormatPickerSheet Enhancements

#### [MODIFY] [FormatPickerSheet.kt](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/formatpicker/FormatPickerSheet.kt)
- **Preview Button**: Add a "Preview" button alongside format items to open `PreviewPlayerSheet` with the direct stream URL.
- **Trim Range Selector**: Add expandable "Trim Segment" section with dual start/end time fields (or range slider) and duration display. Pass `trimStartSeconds` and `trimEndSeconds` into `DownloadParams`.

---

### Library Polish & Management

#### [MODIFY] [LibraryRepository.kt](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/repository/LibraryRepository.kt)
- Add `renameItem(id: String, newTitle: String): Boolean` to update both Room database record and file name on disk.
- Add `deleteItems(ids: List<String>)` for bulk deletion.

#### [MODIFY] [LibraryViewModel.kt](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/library/LibraryViewModel.kt)
- Add state properties for:
  - `isGridView: Boolean` (Grid layout vs List layout).
  - `sortOption: SortOption` (DATE_DESC, DATE_ASC, TITLE_ASC, SIZE_DESC).
  - `selectedItemIds: Set<String>` (Multi-select state).
  - `isMultiSelectMode: Boolean`.
- Add functions: `toggleViewMode()`, `setSortOption()`, `toggleSelection()`, `selectAll()`, `clearSelection()`, `deleteSelectedItems()`, `renameItem()`.

#### [MODIFY] [LibraryScreen.kt](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/library/LibraryScreen.kt)
- Top Bar actions: Search/Filter bar, Grid/List view toggle button, Sort dropdown menu, and Multi-select toggle button.
- Support both `LazyColumn` (list view) and `LazyVerticalGrid` (2-column grid view).
- Add Multi-Select contextual bottom bar (Bulk Delete, Bulk Share via `Intent.ACTION_SEND_MULTIPLE`).
- Add Rename item dialog when user selects "Rename" from item menu.
- Tap completed item to open in-app `PlayerScreen` overlay.

#### [MODIFY] [MainScreen.kt](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/MainScreen.kt)
- Add full-screen `PlayerScreen` overlay when an item is selected for playback from `LibraryScreen`.

---

## Verification Plan

### Automated Tests & Compilation
- Run `./gradlew assembleDebug` to ensure all new Media3 dependencies, ExoPlayer components, Compose layouts, and ViewModel state handlers compile without errors.

### Manual Verification
1. **In-App Playback**: Tap a completed download in `LibraryScreen` -> Verify full-screen `PlayerScreen` opens and plays video/audio with playback controls.
2. **Preview Stream**: Tap "Preview" on a format in `FormatPickerSheet` -> Confirm direct URL streams in `PreviewPlayerSheet`.
3. **Clip Trimming**: Set start timestamp 00:10 and end timestamp 00:30 in `FormatPickerSheet` -> Verify download request includes `--download-sections "*00:10-00:30"`.
4. **Library Polish**:
   - Toggle between Grid and List view.
   - Change sorting (Date, Name, Size) and verify item ordering updates.
   - Long-press item to enter Multi-Select mode -> select multiple items -> test Bulk Delete and Bulk Share.
   - Tap item menu -> select Rename -> verify title and disk filename update cleanly.
