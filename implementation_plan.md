# Phase 2 — MVP UI

Build the user-facing screens that connect to the Phase 1 engine layer, replacing the `MainPlaceholderScreen` with real screens. After this phase, a user can paste a link → pick quality (video or audio) → download → see it in a list.

---

## User Review Required

> [!IMPORTANT]
> **Bottom navigation vs. single-scroll layout.** The spec defines `HomeScreen`, `DownloadQueueScreen`, and `LibraryScreen` as separate screens. For MVP I propose a **3-tab bottom navigation bar** (Home / Downloads / Library) inside a single `MainScreen` composable, which replaces the current `MainPlaceholderScreen`. This is the standard Snaptube/TubeMate-style UX. If you prefer a different navigation pattern (e.g., side drawer, top tabs), let me know.

> [!IMPORTANT]
> **`FormatPickerSheet` as a Modal Bottom Sheet.** The spec calls it a "bottom sheet." I'll use Material 3's `ModalBottomSheet` composable. It will slide up over whatever screen triggered it (HomeScreen, or from share-intent). Confirm this is what you want vs. a full-screen dialog.

> [!IMPORTANT]
> **`core-ui` module.** The spec originally mentioned `app`, `core-engine`, and `core-ui` modules. Phase 1 deferred `core-ui`. For Phase 2, I propose keeping all UI code in the `app` module under `com.dolo.dolo.ui.*` packages, and **still deferring `core-ui`** to a later phase when shared UI components justify a separate module. This avoids premature abstraction. If you want `core-ui` now, I'll add it.

> [!IMPORTANT]
> **"Tap to play via system player (stub)."** The spec says `LibraryScreen` should "tap to play via system player (stub)" — I'll implement this as an `ACTION_VIEW` intent with the file's URI and MIME type, which opens whatever media player the user has installed. This is a real feature, not just a placeholder — "stub" in the spec seems to mean "not the in-app ExoPlayer yet" (that's Phase 3). Confirm?

> [!WARNING]
> **Notification pause/cancel actions.** The checklist item says "Persistent notification for foreground service showing overall download progress, with **pause/cancel action buttons** wired to `PendingIntent`s." The current `DownloadService` has a basic notification with no action buttons. Adding pause/cancel buttons requires:
> 1. Adding `PendingIntent` actions to the notification
> 2. Handling those intents in `DownloadService.onStartCommand()`
> 3. Implementing actual pause/resume logic in the download engine
>
> However, `youtubedl-android`'s `YoutubeDL.execute()` doesn't natively support pause/resume mid-stream — `cancel()` works, but pause requires process-level control. For MVP, I'll implement **cancel** as a real action and **pause** as a "cancel + remember state for retry" pattern (similar to what Seal does). True aria2c session-based pause/resume is Phase 4's scope. Is this acceptable for MVP?

## Open Questions

> [!IMPORTANT]
> **Default save location for downloads.** `DownloadParams.outputDir` is required. For MVP, what should the default save path be? Options:
> 1. App-specific external storage: `context.getExternalFilesDir("Downloads")` — no extra permissions needed but files are deleted on app uninstall
> 2. Public Downloads folder: `Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)/Dolo/` — survives uninstall, but needs MANAGE_EXTERNAL_STORAGE or MediaStore on Android 11+
> 3. A configurable path (deferred to Phase 5's Settings) with a hardcoded default for now
>
> I recommend option 1 for MVP (simplest, no extra permissions), with the setting added in Phase 5. What do you prefer?

> [!IMPORTANT]
> **Smart Quality Presets behavior details.** The spec says "Best Quality" / "Data Saver" / "Quick MP3" presets. For "Best Quality" and "Data Saver" I'll pass `bestvideo+bestaudio/best` or a capped resolution format ID to yt-dlp. Questions:
> - **"Data Saver"** — what resolution cap? I'll default to 480p. Confirm?
> - **"Quick MP3"** — default bitrate? I'll use 192kbps. Confirm?
> - **Tapping a preset starts download immediately** (spec says "skips manual format selection entirely and starts the download immediately") — this means no format picker at all, just queue and go. Confirm this is the MVP behavior?

---

## Proposed Changes

### Step 1: `HomeViewModel` + `HomeScreen`
**Checklist item:** *"`HomeScreen` with paste-link input + "Paste from clipboard" button, backed by a Hilt-injected `HomeViewModel` (via `hiltViewModel()`, calling `DownloadRepository`)"*

#### [NEW] [HomeViewModel.kt](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/home/HomeViewModel.kt)
- `@HiltViewModel` with `DownloadRepository` injected
- State: `url: String`, `isExtracting: Boolean`, `extractionError: String?`, `extractedMetadata: VideoMetadata?`
- Actions: `onUrlChanged(url)`, `pasteFromClipboard(context)`, `extractInfo(url)` → calls `DownloadRepository.extractInfo()` → on success, sets `extractedMetadata` (which triggers `FormatPickerSheet` in the UI)
- Checks for duplicate download via `DownloadRepository.checkDuplicate()` before proceeding

#### [NEW] [HomeScreen.kt](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/home/HomeScreen.kt)
- URL text field with paste icon button
- "Paste from clipboard" prominent button (reads clipboard, fills field, auto-triggers extraction)
- Loading state while extracting (spinner + thumbnail/title preview as they arrive)
- Error display (invalid URL, extraction failure)
- When `extractedMetadata` is non-null → show `FormatPickerSheet`

---

### Step 2: Share-sheet intent handling
**Checklist item:** *"Share-sheet intent handling (`ACTION_SEND` from other apps)"*

#### [MODIFY] [MainActivity.kt](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/MainActivity.kt)
- The `ACTION_SEND` intent filter is already in the manifest and `handleIntent()` already extracts the URL into `sharedUrl` state
- Update: route `sharedUrl` into `HomeScreen` (or directly into `HomeViewModel.extractInfo()`) instead of showing `MainPlaceholderScreen`
- After `HomeScreen` is built, pass `sharedUrl` as an argument so it auto-triggers extraction on arrival
- Handle `onNewIntent` for when the app is already running and receives a new share

---

### Step 3: `FormatPickerSheet`
**Checklist item:** *"`FormatPickerSheet` — list formats returned by extractor, tap to start download, includes audio format/bitrate selector and Smart Quality presets ("Best Quality" / "Data Saver" / "Quick MP3") above the manual list"*

#### [NEW] [FormatPickerSheet.kt](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/formatpicker/FormatPickerSheet.kt)
- Material 3 `ModalBottomSheet`
- **Header section:** thumbnail, title, uploader, duration
- **Smart Quality Presets row:** 3 buttons/cards — "Best Quality" / "Data Saver" / "Quick MP3"
  - Tapping a preset calls `HomeViewModel.startDownload(preset)` immediately (no further selection)
- **Divider:** "Or choose manually"
- **Format list:** `LazyColumn` of formats from `VideoMetadata.formats`
  - Grouped: video formats (with resolution/codec/size), then audio-only formats
  - Each row shows: resolution/quality, codec, estimated size, format note
  - Tap a video format → starts download with that format ID
- **Audio section (if audio-only toggle is on):**
  - Audio format selector: MP3 / M4A / OPUS / FLAC radio buttons
  - Bitrate slider/selector: 128 / 192 / 256 / 320 kbps
  - Tap "Download Audio" → starts audio extraction download
- Download action calls `DownloadRepository.queueDownload()` with the selected `DownloadParams`

---

### Step 4: `DownloadQueueViewModel` + `DownloadQueueScreen`
**Checklist item:** *"`DownloadQueueScreen` — list active downloads with live progress (collect from `DownloadRepository.observeQueue()`, exposed via a `DownloadQueueViewModel`)"*

#### [NEW] [DownloadQueueViewModel.kt](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/queue/DownloadQueueViewModel.kt)
- `@HiltViewModel` with `DownloadRepository` injected
- Collects `DownloadRepository.observeQueue()` as `StateFlow<List<DownloadEntity>>`
- Filters/groups by status: active (DOWNLOADING), queued (QUEUED), completed (COMPLETED), failed (FAILED)
- Actions: `cancelDownload(id)` → `DownloadRepository.cancelDownload(id)`

#### [NEW] [DownloadQueueScreen.kt](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/queue/DownloadQueueScreen.kt)
- `LazyColumn` of download items
- Each item: title, progress bar (animated), percentage, status badge, file size/speed (if available)
- Active downloads at top, then queued, then failed (with error message)
- Cancel button per active/queued item
- Empty state when no downloads

---

### Step 5: Persistent notification with pause/cancel action buttons
**Checklist item:** *"Persistent notification for foreground service showing overall download progress, with pause/cancel action buttons wired to `PendingIntent`s that call back into `DownloadService`"*

#### [MODIFY] [DownloadService.kt](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/service/DownloadService.kt)
- Add `PendingIntent` for cancel action → fires intent with `action = "CANCEL_DOWNLOAD"` + `downloadId` extra
- Add `PendingIntent` for pause action → fires intent with `action = "PAUSE_DOWNLOAD"` + `downloadId` extra
- Add notification action buttons using `NotificationCompat.addAction()`
- Handle `"CANCEL_DOWNLOAD"` in `onStartCommand()`: calls `YoutubeDL.destroyProcessById(id)` + updates DB status to `CANCELLED`
- Handle `"PAUSE_DOWNLOAD"` in `onStartCommand()`: cancels the process + updates DB status to `PAUSED` (MVP pause = cancel with saved state, true resume is Phase 4)
- Show current download title in notification, not just "Downloading..."
- Notification progress bar uses actual percentage from the `DownloadProgressCallback`

---

### Step 6: `LibraryScreen`
**Checklist item:** *"Basic `LibraryScreen` — list completed files via `LibraryRepository.observeLibrary()`, tap to play via system player (stub)"*

#### [NEW] [LibraryViewModel.kt](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/library/LibraryViewModel.kt)
- `@HiltViewModel` with `LibraryRepository` injected
- Collects `LibraryRepository.observeLibrary()` as `StateFlow<List<LibraryItemEntity>>`

#### [NEW] [LibraryScreen.kt](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/library/LibraryScreen.kt)
- `LazyColumn` of library items
- Each item: title, file size, date, audio/video badge
- Tap → open file with system player via `ACTION_VIEW` intent (using `FileProvider` URI + MIME type)
- Empty state when library is empty

#### [NEW] [file_provider_paths.xml](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/res/xml/file_provider_paths.xml)
- FileProvider paths config for sharing downloaded files with system player

#### [MODIFY] [AndroidManifest.xml](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/AndroidManifest.xml)
- Register `FileProvider` for safe file URI sharing

---

### Step 7: Navigation wiring + `MainActivity` update
**Ties it all together for the MVP DONE milestone.**

#### [NEW] [MainScreen.kt](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/MainScreen.kt)
- Bottom navigation bar with 3 tabs: Home / Downloads / Library
- Each tab hosts its respective screen
- `FormatPickerSheet` overlays from HomeScreen when metadata is extracted

#### [MODIFY] [MainActivity.kt](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/MainActivity.kt)
- Replace `MainPlaceholderScreen` route with `MainScreen`
- Remove `MainPlaceholderScreen` composable entirely
- Pass `sharedUrl` into `MainScreen` → `HomeScreen` → auto-extract
- Handle onboarding → main navigation (already exists)

#### [DELETE] `MainPlaceholderScreen` (inline composable in MainActivity.kt)
- Removed as part of the `MainActivity.kt` modification above

---

## File Summary (new files)

| # | File | Package |
|---|---|---|
| 1 | `HomeViewModel.kt` | `com.dolo.dolo.ui.home` |
| 2 | `HomeScreen.kt` | `com.dolo.dolo.ui.home` |
| 3 | `FormatPickerSheet.kt` | `com.dolo.dolo.ui.formatpicker` |
| 4 | `DownloadQueueViewModel.kt` | `com.dolo.dolo.ui.queue` |
| 5 | `DownloadQueueScreen.kt` | `com.dolo.dolo.ui.queue` |
| 6 | `LibraryViewModel.kt` | `com.dolo.dolo.ui.library` |
| 7 | `LibraryScreen.kt` | `com.dolo.dolo.ui.library` |
| 8 | `MainScreen.kt` | `com.dolo.dolo.ui` |
| 9 | `file_provider_paths.xml` | `res/xml` |

## Files Modified

| # | File | Changes |
|---|---|---|
| 1 | `MainActivity.kt` | Replace placeholder with `MainScreen`, wire `sharedUrl` |
| 2 | `DownloadService.kt` | Add notification action buttons (pause/cancel via `PendingIntent`) |
| 3 | `AndroidManifest.xml` | Register `FileProvider` |

---

## Verification Plan

### Automated Tests
- `./gradlew assembleDebug` — verify all new files compile cleanly
- `./gradlew lint` — no new warnings introduced

### Manual Verification
1. Launch app → onboarding → bottom nav with 3 tabs visible
2. Paste a YouTube URL into HomeScreen → extraction spinner → `FormatPickerSheet` slides up with formats
3. Tap "Best Quality" preset → download starts → appears in Downloads tab with live progress bar
4. Notification shows progress with Cancel button → tap Cancel → download cancelled
5. Download completes → appears in Library tab → tap → opens in system media player
6. Share a link from YouTube app → Dolo opens → auto-extracts → format picker shows
7. Verify RTL layout doesn't break (quick visual check in emulator with RTL locale)
