# Implementation Plan - Phase 4: Queue & Reliability

Start Phase 4 of the Dolo specification, focusing on download queue management, concurrency control, and reliability improvements.

## Proposed Changes

### [core-engine]

#### [MODIFY] [DownloadEntity.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/db/DownloadEntity.kt)
- Add `priority` field (Int, default 0) to support queue reordering.
- Add `downloadSpeed` field (Long, default 0) to track current speed.

#### [MODIFY] [DownloadDao.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/db/DownloadDao.kt)
- Add `getTopQueuedDownload()` to find the next item to download (ordered by priority DESC, createdAt ASC).
- Add `updatePriority(id: String, newPriority: Int)` for reordering.
- Add `getDownloadsByStatuses(statuses: List<String>)` for better filtering.
- Update `observeAllDownloads` to order by `priority` DESC, `createdAt` DESC.

#### [NEW] [SettingsRepository.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/repository/SettingsRepository.kt)
- Create a repository using `androidx.datastore:datastore-preferences` to manage app settings:
    - `maxConcurrentDownloads` (default 2)
    - `isWifiOnly` (default false)
    - `globalSpeedLimitKbps` (default 0, unlimited)
    - `connectionsPerDownload` (default 4)

#### [MODIFY] [DownloadRepository.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/repository/DownloadRepository.kt)
- Add `pauseDownload(id: String)`.
- Add `resumeDownload(id: String)`.
- Add `moveDownloadUp(id: String)` and `moveDownloadDown(id: String)` by adjusting priorities.

#### [MODIFY] [DownloadService.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/service/DownloadService.kt)
- Refactor to manage a pool of active downloads.
- Use a `CoroutineScope` to manage multiple `youtubeDL.execute` calls.
- Implement a loop/trigger that checks for QUEUED downloads when an active one finishes or when `maxConcurrentDownloads` increases.
- Observe `SettingsRepository` for concurrency changes.
- Implement `PAUSE_DOWNLOAD` and `RESUME_DOWNLOAD` actions.
- Improve notification to show "X active downloads" and a summary of progress.

### [app]

#### [MODIFY] [DownloadQueueViewModel.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/queue/DownloadQueueViewModel.kt)
- Add `pauseDownload(id: String)`, `resumeDownload(id: String)`.
- Add `moveDownloadUp(id: String)`, `moveDownloadDown(id: String)`.
- Update `DownloadQueueUiState` to include `pausedDownloads`.

#### [MODIFY] [DownloadQueueScreen.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/queue/DownloadQueueScreen.kt)
- Add "Pause" icon button for `DOWNLOADING` items.
- Add "Resume" icon button for `PAUSED` items.
- (Optional) Show speed and ETA if available from `youtubedl-android` callback.

## Verification Plan

### Automated Tests
- Unit tests for `DownloadDao` sorting and queue selection logic.
- Unit tests for `FileNamer` (Phase 1 already did some, but ensure it handles trimmed clips correctly).

### Manual Verification
1.  **Queue Management**:
    - Add 5 downloads.
    - Verify only 2 start downloading initially.
    - Pause one; verify a new one starts.
    - Change max concurrency to 3; verify another one starts.
2.  **Reliability**:
    - Terminate app; verify WorkManager `DownloadRetryWorker` or Service restart resumes pending downloads.
3.  **Wi-Fi Only**:
    - Enable Wi-Fi only, start download on mobile data -> should stay QUEUED or PAUSED.
    - Connect to Wi-Fi -> should start.
