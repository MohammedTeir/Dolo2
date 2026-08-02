# Dolo Phase 0 + Phase 1 Tasks

## Phase 0 — Project Setup

- [x] Step 0.1: Gradle module structure + dependencies (libs.versions.toml, root build.gradle.kts, settings.gradle.kts, core-engine module, app build.gradle.kts, AndroidManifest.xml, gradle.properties)
- [x] Step 0.2: Hilt Application class (DoloApplication.kt)
- [x] Step 0.3: Compose Navigation + Material 3 theming baseline (MainActivity.kt)
- [x] Step 0.4: Room DB setup (entities, DAOs, database, module)
- [x] Step 0.5: OnboardingScreen (engine init loading state)
- [x] Step 0.6: CI / GitHub setup (workflow YAMLs, README)

## Phase 1 — Engine Integration

- [x] Step 1.1: EngineModule + EngineInitializer (Hilt DI)
- [x] Step 1.2: YtDlpExtractor (VideoMetadata, FormatInfo, extractor wrapper)
- [x] Step 1.3: DownloadService (Foreground Service)
- [x] Step 1.4: DownloadRepository + LibraryRepository
- [x] Step 1.5: DownloadRetryWorker
- [x] Step 1.6: MetadataEmbedder
- [x] Step 1.7: WebDownloadInterceptor
- [x] Step 1.8: ACTION_SEND intent filter
- [x] Step 1.9: POST_NOTIFICATIONS permission
- [x] Step 1.10: Duplicate download check (in DownloadRepository)
- [x] Step 1.11: Storage space check (StorageChecker)
- [x] Step 1.12: FileNamer
- [x] Step 1.13: Download sections + cookies support (DownloadRequestBuilder, DownloadParams)
- [x] Step 1.14: Build verification
