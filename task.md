# Dolo Phase 0 + Phase 1 Tasks

## Phase 0 — Project Setup

- [ ] Step 0.1: Gradle module structure + dependencies (libs.versions.toml, root build.gradle.kts, settings.gradle.kts, core-engine module, app build.gradle.kts, AndroidManifest.xml, gradle.properties)
- [ ] Step 0.2: Hilt Application class (DoloApplication.kt)
- [ ] Step 0.3: Compose Navigation + Material 3 theming baseline (MainActivity.kt)
- [ ] Step 0.4: Room DB setup (entities, DAOs, database, module)
- [ ] Step 0.5: OnboardingScreen (engine init loading state)
- [ ] Step 0.6: CI / GitHub setup (workflow YAMLs, README)

## Phase 1 — Engine Integration

- [ ] Step 1.1: EngineModule + EngineInitializer (Hilt DI)
- [ ] Step 1.2: YtDlpExtractor (VideoMetadata, FormatInfo, extractor wrapper)
- [ ] Step 1.3: DownloadService (Foreground Service)
- [ ] Step 1.4: DownloadRepository + LibraryRepository
- [ ] Step 1.5: DownloadRetryWorker
- [ ] Step 1.6: MetadataEmbedder
- [ ] Step 1.7: WebDownloadInterceptor
- [ ] Step 1.8: ACTION_SEND intent filter
- [ ] Step 1.9: POST_NOTIFICATIONS permission
- [ ] Step 1.10: Duplicate download check (in DownloadRepository)
- [ ] Step 1.11: Storage space check (StorageChecker)
- [ ] Step 1.12: FileNamer
- [ ] Step 1.13: Download sections + cookies support (DownloadRequestBuilder, DownloadParams)
- [ ] Step 1.14: Build verification
