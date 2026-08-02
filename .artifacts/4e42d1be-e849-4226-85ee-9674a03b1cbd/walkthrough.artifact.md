# Walkthrough - Phase 4: Queue & Reliability

I have implemented the Queue Management and Reliability features as part of Phase 4.

## Key Changes

### 1. Database & Models
- **[DownloadEntity.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/db/DownloadEntity.kt)**: Added `priority` and `downloadSpeedBytes` fields to support queue ordering and speed tracking.
- **[DownloadDao.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/db/DownloadDao.kt)**: Added queries for queue selection (`getTopQueuedDownload`), reordering (`updatePriority`), and status filtering (`getDownloadsByStatuses`).

### 2. Settings Management
- **[SettingsRepository.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/repository/SettingsRepository.kt)**: New repository using Jetpack DataStore to manage user preferences like `maxConcurrentDownloads` and `isWifiOnly`.

### 3. Download Service Refactoring
- **[DownloadService.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/service/DownloadService.kt)**:
    - **Concurrency Control**: Now manages a pool of active downloads. It respects the `maxConcurrentDownloads` setting and automatically starts new downloads from the queue when one finishes.
    - **Pause/Resume**: Fully implemented pause and resume logic. Paused downloads stop the `yt-dlp` process and free up a concurrency slot.
    - **Network Awareness**: Added a connectivity listener that pauses downloads when "Wi-Fi only" is enabled and the connection is lost, and resumes them when Wi-Fi is restored.

### 4. User Interface Enhancements
- **[DownloadQueueScreen.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/queue/DownloadQueueScreen.kt)**:
    - Added **Pause** and **Resume** buttons for active/paused items.
    - Added **Up/Down arrows** for queued items to allow manual reordering of the download queue.
    - Separated the list into "Active", "Paused", "Queued", and "Failed" sections for better clarity.

## Verification Results

### Build Success
The project compiles successfully with the new dependencies and database schema changes.

### Manual Verification Path
1.  Open the **Queue** screen.
2.  Start multiple downloads; observe the concurrency limit (default 2).
3.  Tap **Pause** on an active download; verify another starts from the queue.
4.  Use **Up/Down arrows** to reorder queued items and see them start in the new order.
5.  (Simulated) Toggle Wi-Fi; verify downloads respond to the "Wi-Fi only" setting.

> [!NOTE]
> Concurrency and Wi-Fi settings are currently managed via the `SettingsRepository` but the UI for changing them will be implemented in Phase 5 (Settings Screen). For now, they use their default values (Max: 2, Wi-Fi only: False).
