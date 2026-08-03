# Implementation Plan - Notification & Service Lifecycle Fix

Address the issue where the download notification persists indefinitely even after work is complete.

## Proposed Changes

### [app]

#### [MODIFY] [DownloadService.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/service/DownloadService.kt)
- **Automatic Shutdown**: Implement logic in `processQueue` to detect when the service is idle (no active jobs and empty queue).
- **Foreground Transition**: Call `stopForeground(STOP_FOREGROUND_DETACH)` when all active downloads are done, making the notification dismissible.
- **Service Termination**: Call `stopSelf()` when completely idle to free system resources.
- **Task Removal**: Override `onTaskRemoved` to cancel notifications and stop processes if the user kills the app from recents.

## Verification Plan

### Manual Verification
1.  **Single Download**: Complete one download. Verify the notification is either gone or easily swiped away immediately after.
2.  **Batch Download**: Queue 3 videos. Verify the service stays active through all three and only terminates after the final one.
3.  **Cancellation**: Cancel all downloads. Verify the notification clears.
4.  **App Kill**: Swipe the app away from the task switcher. Verify the foreground notification doesn't get stuck.
