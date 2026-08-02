# Phase 2 — MVP UI Tasks

## Prerequisites
- [x] Add Coil dependency for thumbnail loading
- [x] Add Material Icons Extended dependency

## Step 1: HomeViewModel + HomeScreen
- [x] Create `HomeViewModel.kt` in `com.dolo.dolo.ui.home`
- [x] Create `HomeScreen.kt` in `com.dolo.dolo.ui.home`

## Step 2: Share-intent wiring
- [ ] Update `MainActivity.kt` to route `sharedUrl` into `HomeScreen`

## Step 3: FormatPickerSheet
- [ ] Create `FormatPickerSheet.kt` in `com.dolo.dolo.ui.formatpicker`

## Step 4: DownloadQueueViewModel + DownloadQueueScreen
- [ ] Create `DownloadQueueViewModel.kt` in `com.dolo.dolo.ui.queue`
- [ ] Create `DownloadQueueScreen.kt` in `com.dolo.dolo.ui.queue`

## Step 5: Notification action buttons
- [ ] Update `DownloadService.kt` with pause/cancel PendingIntent actions

## Step 6: LibraryScreen
- [ ] Create `LibraryViewModel.kt` in `com.dolo.dolo.ui.library`
- [ ] Create `LibraryScreen.kt` in `com.dolo.dolo.ui.library`
- [ ] Create `file_provider_paths.xml`
- [ ] Register FileProvider in `AndroidManifest.xml`

## Step 7: Navigation wiring + MainActivity update
- [ ] Create `MainScreen.kt` with bottom navigation
- [ ] Update `MainActivity.kt` — replace placeholder, wire navigation
- [ ] Verify build compiles
