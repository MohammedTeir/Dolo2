# Phase 0 & Phase 1 — Project Setup + Engine Integration

Build the engine proof-of-concept: prove yt-dlp + aria2c + ffmpeg + mutagen work reliably inside an Android app via `youtubedl-android`, with all wiring (Hilt DI, Room DB, Foreground Service, WorkManager, Repositories) in place before investing in UI.

## User Review Required

> [!IMPORTANT]
> **Room 3.0 vs Room 2.x decision.** Room 3.0 was released as stable and changes the package namespace to `androidx.room3`. It's coroutines-first and KSP-only — which aligns perfectly with this project. However, it's new and has a smaller ecosystem of blog posts/StackOverflow answers. I recommend using **Room 2.7.x** (latest 2.x stable) for stability since it also supports KSP. If you prefer the newer Room 3.0, let me know and I'll adjust.

> [!IMPORTANT]
> **Multi-module structure.** The spec calls for `app`, `core-engine`, and `core-ui` Gradle modules. For Phase 0+1 (no UI screens beyond a stub), I recommend we create `core-engine` now (it holds the engine wrappers, Room DB, repositories) and defer `core-ui` to Phase 2 when we actually build screens. The `app` module will hold `Application`, `MainActivity`, `AndroidManifest`, and service declarations. Does that work?

> [!WARNING]
> **`youtubedl-android` is a Kotlin `object` singleton, not injectable by default.** The spec says `EngineModule` provides `@Singleton` instances of `YoutubeDL`, `FFmpeg`, `Aria2c`. Since these are Kotlin `object` singletons (not classes with constructors), we can't constructor-inject them. Instead, `EngineModule` will provide them as `@Provides` functions that return the `object` instance and perform initialization. This is how Seal (the reference app by the same author) does it.

## Open Questions

> [!IMPORTANT]
> **Room schema fields for download history / library metadata.** The spec mentions Room DB for "download history / library metadata" but doesn't specify exact columns. I need to define tables for Phase 1. My proposed schema is below — please review before I implement:
>
> **`downloads` table** (active/queued/failed downloads):
> | Column | Type | Notes |
> |---|---|---|
> | `id` | `String` (PK) | UUID, generated on queue |
> | `url` | `String` | Source URL |
> | `title` | `String?` | Extracted title |
> | `thumbnail_url` | `String?` | Thumbnail URL |
> | `uploader` | `String?` | Uploader/channel name |
> | `duration_seconds` | `Int?` | Duration in seconds |
> | `format_id` | `String?` | yt-dlp format ID selected |
> | `format_label` | `String?` | Human-readable format description (e.g. "1080p mp4") |
> | `is_audio_only` | `Boolean` | Whether this is an audio extraction |
> | `audio_format` | `String?` | MP3/M4A/OPUS/FLAC if audio-only |
> | `audio_bitrate` | `Int?` | Bitrate in kbps if audio-only |
> | `estimated_size_bytes` | `Long?` | Estimated file size |
> | `downloaded_size_bytes` | `Long` | Bytes downloaded so far |
> | `status` | `String` | QUEUED / DOWNLOADING / PAUSED / COMPLETED / FAILED / CANCELLED |
> | `progress` | `Float` | 0.0–1.0 |
> | `file_path` | `String?` | Final file path on disk |
> | `error_message` | `String?` | Error message if failed |
> | `created_at` | `Long` | Epoch millis |
> | `updated_at` | `Long` | Epoch millis |
> | `trim_start_seconds` | `Float?` | Clip start time |
> | `trim_end_seconds` | `Float?` | Clip end time |
> | `use_cookies` | `Boolean` | Whether to use imported cookies |
> | `playlist_id` | `String?` | Group ID for playlist batch |
> | `playlist_index` | `Int?` | Zero-padded index within playlist |
>
> **`library_items` table** (completed downloads, the user's library):
> | Column | Type | Notes |
> |---|---|---|
> | `id` | `String` (PK) | Same UUID from download |
> | `source_url` | `String` | Original source URL |
> | `title` | `String` | Display title |
> | `file_path` | `String` | Absolute path to the file |
> | `file_size_bytes` | `Long` | Actual file size |
> | `mime_type` | `String?` | e.g. "video/mp4", "audio/mpeg" |
> | `thumbnail_path` | `String?` | Path to cached thumbnail |
> | `duration_seconds` | `Int?` | Duration |
> | `uploader` | `String?` | Uploader |
> | `format_label` | `String?` | Format description |
> | `is_audio` | `Boolean` | Audio vs video |
> | `downloaded_at` | `Long` | Epoch millis |
> | `playlist_id` | `String?` | For grouping |
> | `playlist_index` | `Int?` | Order within playlist |

---

## Proposed Changes

The plan follows the exact checklist order from SPEC.md §5, Phase 0 then Phase 1.

---

### Phase 0 — Project Setup

---

#### Step 0.1: Gradle module structure + dependencies

**SPEC checklist items covered:**
- ✅ Create Android Studio project (already exists — we modify it)
- ✅ Add `youtubedl-android` dependencies (`library`, `ffmpeg`, `aria2c`)
- ✅ Add Hilt dependency + `@HiltAndroidApp`, `hilt-navigation-compose`
- ✅ Add WorkManager dependency + Hilt-WorkManager integration
- ✅ Set up Gradle module structure (`app`, `core-engine`, `core-ui`)
- ✅ Configure per-ABI splits (arm64-v8a, armeabi-v7a)

##### [MODIFY] [libs.versions.toml](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/gradle/libs.versions.toml)
Add version entries and library/plugin declarations for:
- `youtubedl-android` (library, ffmpeg, aria2c) — `0.18.1`
- Hilt — `2.60.1`
- KSP — `2.2.10-2.0.2`
- Room — `2.7.0` (or 3.0.1 — pending your answer above)
- WorkManager — `2.11.2`
- `hilt-work` — `1.4.0`
- `hilt-navigation-compose` — `1.4.0`
- `navigation-compose` — `2.9.8`

##### [MODIFY] [build.gradle.kts](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/build.gradle.kts) (root)
Add KSP and Hilt plugins (apply false).

##### [MODIFY] [settings.gradle.kts](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/settings.gradle.kts)
Add `include(":core-engine")`. (`:core-ui` deferred to Phase 2.)

##### [NEW] [core-engine/build.gradle.kts](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/build.gradle.kts)
Android library module with:
- `youtubedl-android` dependencies
- Hilt, KSP, Room dependencies
- WorkManager + hilt-work
- Kotlin coroutines

##### [MODIFY] [app/build.gradle.kts](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/app/build.gradle.kts)
- Apply KSP + Hilt plugins
- Add `implementation(project(":core-engine"))`
- Add Hilt, navigation-compose, hilt-navigation-compose deps
- Configure per-ABI splits (arm64-v8a, armeabi-v7a)
- Add `extractNativeLibs = true` + `abiFilters`
- Add `kotlinOptions { jvmTarget = "11" }`

##### [MODIFY] [AndroidManifest.xml](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/AndroidManifest.xml)
- Add `android:extractNativeLibs="true"` to `<application>`
- Add `android:name=".DoloApplication"` to `<application>`
- Disable default WorkManager initializer (`androidx.startup.InitializationProvider` removal)
- Add permissions: `INTERNET`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `POST_NOTIFICATIONS`, `WRITE_EXTERNAL_STORAGE` (pre-API 29)

##### [MODIFY] [gradle.properties](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/gradle.properties)
- Add `android.useAndroidX=true`

---

#### Step 0.2: Hilt Application class

**SPEC checklist items covered:**
- ✅ `@HiltAndroidApp` Application class
- ✅ WorkManager `HiltWorkerFactory` integration

##### [NEW] [DoloApplication.kt](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/DoloApplication.kt)
- `@HiltAndroidApp` annotated `Application` subclass
- Implements `Configuration.Provider` for WorkManager custom initialization with `HiltWorkerFactory`

---

#### Step 0.3: Compose Navigation + Material 3 theming baseline

**SPEC checklist items covered:**
- ✅ Set up Compose Navigation graph and Material 3 theming baseline

##### [MODIFY] [MainActivity.kt](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/MainActivity.kt)
- Add `@AndroidEntryPoint`
- Replace stub content with a `NavHost` containing a single placeholder route (no real screens yet — Phase 2)

##### [MODIFY] [Theme.kt](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/theme/Theme.kt)
- Keep existing Material 3 theming (already generated by AS template)

---

#### Step 0.4: Room DB setup

**SPEC checklist items covered:**
- ✅ Set up Room DB for download history/library metadata

##### [NEW] core-engine module files:
- `core-engine/src/main/java/com/dolo/core/db/DoloDatabase.kt` — `@Database` class with `DownloadEntity` and `LibraryItemEntity`
- `core-engine/src/main/java/com/dolo/core/db/DownloadEntity.kt` — Room entity for the `downloads` table (schema above)
- `core-engine/src/main/java/com/dolo/core/db/LibraryItemEntity.kt` — Room entity for the `library_items` table
- `core-engine/src/main/java/com/dolo/core/db/DownloadDao.kt` — DAO with insert, update, query-by-status, observe-all, query-by-url-and-format (for duplicate check)
- `core-engine/src/main/java/com/dolo/core/db/LibraryItemDao.kt` — DAO with insert, update, delete, observe-all, query-by-source-url
- `core-engine/src/main/java/com/dolo/core/db/DatabaseModule.kt` — Hilt `@Module` providing Room DB + DAOs as singletons

---

#### Step 0.5: OnboardingScreen (engine init loading state)

**SPEC checklist items covered:**
- ✅ Build `OnboardingScreen` — welcome, storage permission request, engine setup loading state

##### [NEW] [OnboardingScreen.kt](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/onboarding/OnboardingScreen.kt)
- Minimal Compose screen: app logo/welcome text, storage permission request (runtime permission flow), then a loading spinner while the engine initializes (YoutubeDL + FFmpeg + Aria2c `.init()` calls)
- After successful init, navigates to the main screen (placeholder for Phase 2)
- Tracks first-launch via SharedPreferences / DataStore so subsequent launches skip to main

> [!NOTE]
> This is the one UI screen we build in Phase 0 per the spec. It's functional but intentionally minimal — visual polish is Phase 2+.

---

#### Step 0.6: CI / GitHub setup

**SPEC checklist items covered:**
- ✅ Set up GitHub repo, README, GPL/LGPL license file, CI (lint + build)
- ✅ Set up release keystore + store as GitHub Actions secret
- ✅ Build GitHub Actions release workflow

> [!IMPORTANT]
> **Skipping CI/CD for this task.** These are repo-level, non-code tasks (creating a GitHub repo, adding secrets, writing workflow YAML). They require your GitHub credentials and repo creation. I'll create the GitHub Actions workflow YAML files as templates, but the actual repo creation, secret setup, and keystore generation are manual steps for you. I'll document exactly what you need to do.

##### [NEW] [.github/workflows/ci.yml](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/.github/workflows/ci.yml)
- Template: on PR → lint + build
##### [NEW] [.github/workflows/release.yml](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/.github/workflows/release.yml)
- Template: on version tag → lint + test → assembleRelease → sign → attach to GitHub Release
##### [NEW] [README.md](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/README.md)
- Basic project description

---

### Phase 1 — Engine Integration (MVP core)

---

#### Step 1.1: EngineModule (Hilt)

**SPEC checklist items covered:**
- ✅ Build `EngineModule` (Hilt `@Module`, `@InstallIn(SingletonComponent::class)`)
- ✅ Initialize `YoutubeDL`, `FFmpeg`, and `Aria2c` singletons

##### [NEW] [EngineModule.kt](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/engine/EngineModule.kt)
- `@Module @InstallIn(SingletonComponent::class)`
- `@Provides @Singleton` for `YoutubeDL` (returns the Kotlin `object` singleton)
- `@Provides @Singleton` for `FFmpeg` (same pattern)
- `@Provides @Singleton` for `Aria2c` (same pattern)
- Initialization happens in `DoloApplication.onCreate()` (not in `@Provides`, since init requires `Context` and is one-time)

##### [NEW] [EngineInitializer.kt](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/engine/EngineInitializer.kt)
- Helper class (Hilt-injectable) that wraps the `YoutubeDL.init()`, `FFmpeg.init()`, `Aria2c.init()` calls
- Runs on a background coroutine; emits init status via a `StateFlow` for the `OnboardingScreen` to observe

---

#### Step 1.2: YtDlpExtractor

**SPEC checklist items covered:**
- ✅ Build `YtDlpExtractor` wrapper

##### [NEW] [VideoMetadata.kt](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/model/VideoMetadata.kt)
- App's own Kotlin data class: `title`, `thumbnailUrl`, `formats: List<FormatInfo>`, `durationSeconds`, `uploader`, `id`, `description`

##### [NEW] [FormatInfo.kt](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/model/FormatInfo.kt)
- Data class: `formatId`, `ext`, `resolution`, `fps`, `fileSizeBytes`, `codec`, `audioCodec`, `isVideoOnly`, `isAudioOnly`, `formatNote`

##### [NEW] [YtDlpExtractor.kt](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/engine/YtDlpExtractor.kt)
- `@Inject constructor(private val youtubeDL: YoutubeDL)`
- `suspend fun extractInfo(url: String): Result<VideoMetadata>` — calls `YoutubeDL.getInfo(url)`, maps `VideoInfo` → `VideoMetadata`
- Runs on `Dispatchers.IO`

---

#### Step 1.3: DownloadService

**SPEC checklist items covered:**
- ✅ Build `DownloadService` (Foreground Service, `@AndroidEntryPoint`)

##### [NEW] [DownloadService.kt](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/service/DownloadService.kt)
- Extends `Service`, annotated `@AndroidEntryPoint`
- Hilt field-injects `YoutubeDL` and `DownloadDao`
- Runs as a foreground service with a persistent notification (basic progress notification)
- Accepts download requests via `Intent` extras or a bound interface
- Builds `YoutubeDLRequest` from download parameters, calls `YoutubeDL.execute()` with a `DownloadProgressCallback`
- Updates `DownloadEntity` in Room as progress arrives
- Moves completed entries from `downloads` to `library_items`
- Handles pause/cancel via `YoutubeDL.destroyProcessById()`

##### Manifest additions:
- `<service android:name=".service.DownloadService" android:foregroundServiceType="dataSync" />`

---

#### Step 1.4: DownloadRepository + LibraryRepository

**SPEC checklist items covered:**
- ✅ Build `DownloadRepository` and `LibraryRepository` (Hilt `RepositoryModule`)

##### [NEW] [DownloadRepository.kt](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/repository/DownloadRepository.kt)
- `@Inject constructor(extractor, downloadDao, libraryItemDao)`
- `suspend fun extractInfo(url: String): Result<VideoMetadata>` — delegates to `YtDlpExtractor`
- `suspend fun queueDownload(params: DownloadParams): String` — creates `DownloadEntity`, inserts into Room, returns ID
- `fun observeQueue(): Flow<List<DownloadEntity>>` — Room reactive query
- `suspend fun checkDuplicate(url: String, formatId: String?): DownloadEntity?` — for duplicate detection

##### [NEW] [LibraryRepository.kt](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/repository/LibraryRepository.kt)
- `@Inject constructor(libraryItemDao)`
- `fun observeLibrary(): Flow<List<LibraryItemEntity>>`
- `suspend fun deleteItem(id: String)`
- `suspend fun addItem(item: LibraryItemEntity)`

##### [NEW] [RepositoryModule.kt](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/di/RepositoryModule.kt)
- Hilt `@Module` binding repositories (or they can be constructor-injected directly since they're concrete classes — binds pattern only needed if we introduce interfaces)

---

#### Step 1.5: DownloadRetryWorker

**SPEC checklist items covered:**
- ✅ Build `DownloadRetryWorker` (Hilt-assisted `CoroutineWorker`)

##### [NEW] [DownloadRetryWorker.kt](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/worker/DownloadRetryWorker.kt)
- `@HiltWorker` annotated `CoroutineWorker`
- `@AssistedInject constructor` with `DownloadDao` injected
- `doWork()`: queries downloads with DOWNLOADING status (interrupted), re-enqueues them by starting `DownloadService`

---

#### Step 1.6: MetadataEmbedder

**SPEC checklist items covered:**
- ✅ Build `MetadataEmbedder`

##### [NEW] [MetadataEmbedder.kt](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/engine/MetadataEmbedder.kt)
- `@Inject constructor(private val youtubeDL: YoutubeDL)`
- `suspend fun embedMetadata(filePath: String, title: String?, artist: String?, thumbnailPath: String?): Result<Unit>`
- Invokes mutagen via `YoutubeDL.execute()` with a Python script that calls mutagen to write tags
- Alternative approach: uses yt-dlp's built-in `--embed-thumbnail` and `--add-metadata` flags on the download request itself (much simpler, avoids needing a separate mutagen call). I'll implement both paths and document the trade-off.

> [!WARNING]
> **mutagen via bundled Python runtime.** The spec calls for running mutagen via the bundled Python runtime. However, `youtubedl-android`'s Python runtime is intentionally minimal — it may not include mutagen as a pre-installed package. **Safer approach**: use yt-dlp's own `--embed-thumbnail` + `--add-metadata` post-processor flags, which internally handle metadata embedding without needing a separate mutagen invocation. If this covers the tagging needs (title, artist, thumbnail), we avoid the fragility of trying to install/run mutagen separately. I'll implement this approach first and fall back to a manual mutagen script only if yt-dlp's built-in embedding proves insufficient. **This is a deviation from SPEC.md worth calling out.**

---

#### Step 1.7: WebDownloadInterceptor

**SPEC checklist items covered:**
- ✅ Build `WebDownloadInterceptor`

##### [NEW] [WebDownloadInterceptor.kt](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/engine/WebDownloadInterceptor.kt)
- Implements `DownloadListener` interface from `android.webkit.WebView`
- `onDownloadStart()` callback: extracts URL, user agent, content disposition, mime type
- Builds a `DownloadParams` and routes to `DownloadService` via `DownloadRepository`
- Extracts filename from URL path or `Content-Disposition` header for `FileNamer`

---

#### Step 1.8: ACTION_SEND intent filter

**SPEC checklist items covered:**
- ✅ Add `ACTION_SEND` intent filter to catch shared links

##### [MODIFY] [AndroidManifest.xml](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/AndroidManifest.xml)
- Add `<intent-filter>` on `MainActivity` for `ACTION_SEND` with `text/plain` MIME type

##### [MODIFY] [MainActivity.kt](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/MainActivity.kt)
- In `onCreate` / `onNewIntent`, check for `ACTION_SEND` intent and extract URL from extras
- Route extracted URL through the same extraction path (for now, log it — full UI wiring is Phase 2)

---

#### Step 1.9: POST_NOTIFICATIONS permission

**SPEC checklist items covered:**
- ✅ Handle Android 13+ `POST_NOTIFICATIONS` runtime permission request

##### [MODIFY] [AndroidManifest.xml](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/AndroidManifest.xml)
- Add `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />`

##### [MODIFY] [OnboardingScreen.kt or MainActivity.kt]
- Add runtime permission request for `POST_NOTIFICATIONS` on API 33+

---

#### Step 1.10: Duplicate download check

**SPEC checklist items covered:**
- ✅ Build duplicate-download check in `DownloadRepository`

Already covered in Step 1.4 — `DownloadRepository.checkDuplicate()` queries by source URL + format.

---

#### Step 1.11: Storage space check

**SPEC checklist items covered:**
- ✅ Build storage-space check (`StatFs`)

##### [NEW] [StorageChecker.kt](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/util/StorageChecker.kt)
- `fun hasEnoughSpace(path: String, requiredBytes: Long): Boolean` — uses `StatFs` to check available space
- Called by `DownloadRepository.queueDownload()` before enqueuing

---

#### Step 1.12: FileNamer

**SPEC checklist items covered:**
- ✅ Build `FileNamer`

##### [NEW] [FileNamer.kt](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/util/FileNamer.kt)
- `enum class NamingMode { CLEAN_TITLE, ORIGINAL_FILENAME }`
- `fun generateFileName(...)`: supports both modes, sanitizes for filesystem safety
- `fun sanitize(name: String)`: strips `/ \ : * ? " < > |`, collapses whitespace, truncates at ~255 bytes
- `fun handleCollision(path: String)`: appends `(1)`, `(2)`, etc.
- `fun applyPlaylistPrefix(index: Int, totalCount: Int, name: String)`: zero-padded index prefix in Clean Title mode
- `fun applyTemplate(template: String, metadata: Map<String, String>)`: custom naming template
- `fun extractOriginalFilename(url: String, contentDisposition: String?)`: for `WebDownloadInterceptor` downloads
- `fun applyTrimSuffix(name: String, startSeconds: Float, endSeconds: Float)`: e.g. `Title [02-15-04-30].mp4`

---

#### Step 1.13: Download sections + cookies support

**SPEC checklist items covered:**
- ✅ Add `--download-sections` support to `YoutubeDLRequest` builder
- ✅ Add `--cookies <path>` support to `YoutubeDLRequest` builder

##### [NEW] [DownloadRequestBuilder.kt](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/engine/DownloadRequestBuilder.kt)
- Builds a `YoutubeDLRequest` from a `DownloadParams` data class
- Adds `--download-sections "*START-END"` when trim params are present
- Adds `--cookies <path>` when cookies are enabled and file exists
- Adds `-x --audio-format <format> --audio-quality <bitrate>` for audio-only downloads
- Adds `--embed-thumbnail --add-metadata` for metadata embedding
- Adds `--external-downloader aria2c` to use aria2c backend
- Sets output template based on `FileNamer` result

##### [NEW] [DownloadParams.kt](file:///c:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/model/DownloadParams.kt)
- Data class capturing all download configuration: url, formatId, outputPath, audioOnly, audioFormat, bitrate, trimStart, trimEnd, useCookies, cookiesPath, namingMode, playlistIndex, etc.

---

#### Step 1.14: End-to-end testing

**SPEC checklist items covered:**
- ✅ Test end-to-end: paste YouTube link → extract formats → download via aria2c → file saved
- ✅ Test audio conversion end-to-end: audio only → ffmpeg re-encode → metadata embed → verify

> [!NOTE]
> These are manual device tests, not automated unit tests. I'll add a temporary debug screen or logcat-verified test flow in `MainActivity` that exercises the full pipeline: extract → download → verify file. This gets removed when Phase 2 provides the real UI.

---

## Verification Plan

### Automated Tests
- `./gradlew lint` — ensure no lint errors in the new code
- `./gradlew assembleDebug` — ensure the project builds cleanly with all new modules/dependencies

### Manual Verification
- Deploy debug APK to a physical device or emulator
- Verify `OnboardingScreen` appears on first launch, requests storage permission, shows engine init progress
- Verify `YtDlpExtractor.extractInfo()` works against a YouTube URL (logged to Logcat)
- Verify `DownloadService` downloads a file via aria2c and saves to disk
- Verify audio-only download produces a properly tagged MP3/M4A file
- Verify `FileNamer` produces expected filenames in both naming modes
- Verify `WebDownloadInterceptor` catches a direct download link
- Verify `ACTION_SEND` intent delivers a URL to the app
- Verify `POST_NOTIFICATIONS` permission is requested on API 33+
- Verify duplicate download check warns before re-downloading
- Verify storage space check warns when space is insufficient

---

## File Tree Summary (after Phase 0+1)

```
Dolo2/
├── app/
│   ├── build.gradle.kts                    [MODIFIED]
│   └── src/main/
│       ├── AndroidManifest.xml             [MODIFIED]
│       └── java/com/dolo/dolo/
│           ├── DoloApplication.kt          [NEW]
│           ├── MainActivity.kt             [MODIFIED]
│           ├── service/
│           │   └── DownloadService.kt      [NEW]
│           └── ui/
│               ├── onboarding/
│               │   └── OnboardingScreen.kt [NEW]
│               └── theme/ (existing)
├── core-engine/
│   ├── build.gradle.kts                    [NEW]
│   └── src/main/java/com/dolo/core/
│       ├── db/
│       │   ├── DoloDatabase.kt             [NEW]
│       │   ├── DownloadEntity.kt           [NEW]
│       │   ├── LibraryItemEntity.kt        [NEW]
│       │   ├── DownloadDao.kt              [NEW]
│       │   ├── LibraryItemDao.kt           [NEW]
│       │   └── DatabaseModule.kt           [NEW]
│       ├── di/
│       │   └── RepositoryModule.kt         [NEW]
│       ├── engine/
│       │   ├── EngineModule.kt             [NEW]
│       │   ├── EngineInitializer.kt        [NEW]
│       │   ├── YtDlpExtractor.kt           [NEW]
│       │   ├── MetadataEmbedder.kt         [NEW]
│       │   ├── WebDownloadInterceptor.kt   [NEW]
│       │   └── DownloadRequestBuilder.kt   [NEW]
│       ├── model/
│       │   ├── VideoMetadata.kt            [NEW]
│       │   ├── FormatInfo.kt               [NEW]
│       │   └── DownloadParams.kt           [NEW]
│       ├── repository/
│       │   ├── DownloadRepository.kt       [NEW]
│       │   └── LibraryRepository.kt        [NEW]
│       ├── util/
│       │   ├── FileNamer.kt               [NEW]
│       │   └── StorageChecker.kt           [NEW]
│       └── worker/
│           └── DownloadRetryWorker.kt      [NEW]
├── .github/workflows/
│   ├── ci.yml                              [NEW]
│   └── release.yml                         [NEW]
├── build.gradle.kts                        [MODIFIED]
├── settings.gradle.kts                     [MODIFIED]
├── gradle/libs.versions.toml              [MODIFIED]
├── gradle.properties                       [MODIFIED]
└── README.md                               [NEW]
```
