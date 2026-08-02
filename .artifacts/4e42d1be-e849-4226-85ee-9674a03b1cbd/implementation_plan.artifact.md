# Implementation Plan - Phase 4: Queue & Reliability

Start Phase 4 of the Dolo specification, focusing on download queue management, concurrency control, and reliability improvements.

## Proposed Changes

### [core-engine]

#### [MODIFY] [DownloadEntity.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/db/DownloadEntity.kt)
- Add `priority` field (Int, default 0) to support queue reordering.
- Add `downloadSpeed` field (Long, default 0) to track current speed.

#### [MODIFY] [DownloadDao.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/db/DownloadDao.kt)
- Add `getTopQueuedDownload()` to find the next item to download.
- Add `updatePriority(id: String, newPriority: Int)` for reordering.
- Add `getDownloadsByStatus(status: List<String>)` for better filtering.
- Update `observeAllDownloads` to order by `priority` then `createdAt`.

#### [NEW] [SettingsRepository.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/repository/SettingsRepository.kt)
- Create a repository using `DataStore` to manage app settings:
    - `maxConcurrentDownloads` (default 2)
    - `isWifiOnly` (default false)
    - `globalSpeedLimitKbps` (default 0, unlimited)
    - `connectionsPerDownload` (default 4)

#### [MODIFY] [DownloadRepository.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/repository/DownloadRepository.kt)
- Add `pauseDownload(id: String)` and `resumeDownload(id: String)`.
- Add `reorderQueue(id: String, fromIndex: Int, toIndex: Int)`.

#### [MODIFY] [DownloadService.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/service/DownloadService.kt)
- Refactor to manage a pool of active downloads.
- Observe settings for concurrency changes.
- Implement `PAUSE_DOWNLOAD` and `RESUME_DOWNLOAD` actions.
- Improve notification to show overall status and individual progress if possible (or just top active).
- Listen for network changes to pause/resume based on "Wi-Fi only" setting.

### [app]

#### [MODIFY] [DownloadQueueViewModel.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/queue/DownloadQueueViewModel.kt)
- Add `pauseDownload(id: String)` and `resumeDownload(id: String)` methods.
- Add `moveDownloadUp(id: String)` and `moveDownloadDown(id: String)`.
- Update UI state to include paused downloads in a distinct way or merged with active.

#### [MODIFY] [DownloadQueueScreen.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/queue/DownloadQueueScreen.kt)
- Add "Pause" and "Resume" buttons to `DownloadQueueItem`.
- Show current download speed if available.
- (Optional) Add drag-and-drop or Up/Down buttons for reordering.

## Verification Plan

### Automated Tests
- Unit tests for `SettingsRepository` to ensure values are saved/retrieved correctly.
- Unit tests for `DownloadDao` to verify priority sorting and queue selection.

### Manual Verification
1.  **Queue Management**:
    - Add 5 downloads.
    - Verify only 2 (or configured max) start downloading, others stay QUEUED.
    - Pause an active download; verify the next QUEUED download starts.
    - Resume the paused download; verify it resumes (from where it left off if possible).
2.  **Concurrency**:
    - Change "Max concurrent downloads" in settings (via a temporary UI or ADB).
    - Verify active downloads count adjusts accordingly.
3.  **Reliability**:
    - Force close app during download.
    - Reopen app; verify `DownloadRetryWorker` or `DownloadService` resumes downloads.
4.  **Wi-Fi Only**:
    - Enable "Wi-Fi only".
    - Switch to mobile data; verify downloads pause.
    - Switch back to Wi-Fi; verify downloads resume.
