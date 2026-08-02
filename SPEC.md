# Dolo — Full Product Spec & Roadmap
### A Snaptube-style Android video/audio downloader powered by yt-dlp + aria2c + ffmpeg

---

## 1. Concept

A native Android app that lets users find, preview, and download video/audio from major platforms (YouTube, Instagram, TikTok, Facebook, Twitter/X, Vimeo, Dailymotion, etc.), with:

- **Better download engine than Snaptube** — aria2c gives multi-connection segmented downloading with reliable pause/resume, instead of Snaptube's single-connection downloads.
- **More reliable extraction** — yt-dlp is actively maintained against site changes, far more robust than whatever closed-source extractor Snaptube uses.
- **Real audio conversion** — actual ffmpeg re-encoding to MP3/M4A/OPUS/FLAC, not just format renaming.
- **Proper metadata** — embedded thumbnails and tags on audio files via mutagen, like a real music library.
- **Capabilities Snaptube doesn't have** — clip/trim downloads (grab just a segment), preview-before-download streaming, cookies import for logged-in content, and batch multi-link import.
- **No ads, no watermarks, no paid tier** — fully free.
- **No telemetry, open source** — a real, verifiable answer to Snaptube's bundled-tracker reputation, not just a claim.
- **Fully local/offline-first** — everything runs on-device, no backend server, no user data leaves the phone.
- **Modern UI** — pure Jetpack Compose, no XML layouts or Fragments.

---

## 2. Core Architecture

| Layer | Technology | Purpose |
|---|---|---|
| Extraction + Download + Conversion engine | **`youtubedl-android` library** (bundles Python runtime + yt-dlp + ffmpeg + aria2c as Gradle modules) | Single dependency providing extraction, downloading, and transcoding — no manual per-ABI binary bundling needed |
| Extraction | yt-dlp (via `youtubedl-android`'s `library` module) | Resolves URLs → metadata, thumbnails, format list, subtitle list, direct media URLs |
| Download engine | aria2c (via `youtubedl-android`'s `aria2c` module) | Multi-connection segmented downloading, pause/resume, queue management |
| Audio/video conversion | ffmpeg (via `youtubedl-android`'s `ffmpeg` module) | Real re-encoding to MP3/M4A/OPUS/FLAC, not just container renaming |
| Metadata embedding | **mutagen** (Python library, runs on the bundled Python runtime) | Embeds thumbnails and ID3/Vorbis/MP4 tags into converted audio files |
| Background execution | Android Foreground Service (`DownloadService`) + **WorkManager** | Foreground Service keeps aria2c alive for active downloads; WorkManager schedules retries that survive process death, plus periodic background checks (yt-dlp engine update, app self-update) |
| Data/Domain layer | **Repository pattern** (`DownloadRepository`, `LibraryRepository`) | Sits between ViewModels and the engine/Room DB — ViewModels never call `youtubedl-android` or Room directly |
| Local player | AndroidX Media3 (ExoPlayer) | Plays downloaded files offline |
| Storage | Scoped storage / `MediaStore` API | Saves downloaded and converted files, manages Vault |
| UI | **Pure Jetpack Compose** (no XML, no Fragments), Material 3, **RTL-aware layouts** | All screens — single-Activity app with Compose Navigation |
| Dependency Injection | **Hilt** | Provides/scopes singletons (`YoutubeDL`, `FFmpeg`, `Aria2c`, Room DB, repositories) into ViewModels and services |
| Engine update mechanism | In-app binary updater (GitHub Releases API) | Keeps yt-dlp current as extractors break — `youtubedl-android` exposes an update method (`YoutubeDL.getInstance().updateYoutubeDL()`) |
| App update mechanism | Self-update checker (GitHub Releases API) | Since Dolo isn't on Play Store, checks for a newer Dolo APK release and prompts the user to download/install it |

**Data flow:** User pastes/shares a link → `YtDlpExtractor` calls `YoutubeDL.getInfo(url)` from `youtubedl-android` → returns a `VideoInfo` object with formats/thumbnail/title → user picks quality in `FormatPickerSheet` (Compose bottom sheet) → `DownloadService` builds a `YoutubeDLRequest` with the selected format and hands it to the library's aria2c-backed downloader → progress streamed via the library's `DownloadProgressCallback` → file lands in chosen storage path → indexed into local Room DB → appears in Library.

**Audio conversion flow:** If the user selects "audio only," the `YoutubeDLRequest` includes `-x --audio-format mp3` (or m4a/opus/flac) flags, which `youtubedl-android` passes through to its bundled ffmpeg for real re-encoding → once the file lands, `MetadataEmbedder` invokes mutagen (via the same bundled Python runtime) to write ID3/Vorbis/MP4 tags (title, artist, album art from the video thumbnail) into the file.

**Alternate flow (direct-link sites):** User taps a download button in the in-app browser → `WebDownloadInterceptor` catches the request via `WebView.setDownloadListener` (URL, user agent, content type) → if it's a direct file link, hand straight to `DownloadService`'s aria2c downloader, skipping the yt-dlp extraction step entirely. This makes the app work as a universal downloader for any site with a real download link, not just yt-dlp-supported platforms.

### Share-Intent & Playlist Handling (decision)

**Entry point:** A dedicated intent filter on an entry Activity catches `ACTION_SEND` (type `text/plain`) so "Share" from YouTube/Instagram/etc. lands directly in Dolo with the URL extracted from the intent extras — same code path as a manually pasted link.

**Single video link** (no playlist context):
1. `DownloadRepository.extractInfo(url)` → `YtDlpExtractor` → `YoutubeDL.getInfo(url)`
2. Returns one item's full format list → `FormatPickerSheet` opens immediately

**Playlist or channel link:**
1. `YtDlpExtractor` first calls yt-dlp with `--flat-playlist` — fast, metadata-only (titles/thumbnails/IDs/durations), avoids resolving full formats for every item up front
2. If `playlist_count > 1`, the app **skips `FormatPickerSheet`** and routes to `PlaylistSelectionScreen` instead
3. `PlaylistSelectionScreen` shows checkboxes per item, a "Select All" toggle, and **one global quality/format preset for the whole batch** (e.g. "Download all at 1080p") rather than asking per-video
4. Channel URLs are treated the same way (yt-dlp resolves a channel's implicit "uploads" playlist)

**Selecting one item vs. multiple items from `PlaylistSelectionScreen`** — same code path either way, just a different request count:
- **One item selected:** exactly one download request is built and queued — behaves identically to the single-video flow, just sourced from the playlist picker instead of a direct link. If the user wants to change quality for just that one item, tapping it again (or a "customize" option) can drop into the normal `FormatPickerSheet` for that single video instead of using the batch preset.
- **Multiple items selected:** one download request per selected video is queued in sequence via `DownloadRepository`, each using the batch quality/format preset, each appearing as its own separate entry in `DownloadQueueScreen` — they download independently (pause/resume/cancel per item still works individually, same as any other queued download).

**Ambiguous case — single video URL that's part of a playlist** (e.g. `watch?v=X&list=Y`): yt-dlp's default behavior (no `--yes-playlist` flag) resolves just that one video, and Dolo keeps that as the default — a shared single-video link downloads that video, not the whole playlist, unless the user explicitly wants otherwise. When Dolo detects a `list=` parameter alongside a `v=` parameter, it shows a small inline prompt: **"This video is part of a playlist — download just this video, or the whole playlist?"** with two buttons, before proceeding to either `FormatPickerSheet` (just this video) or `PlaylistSelectionScreen` (whole playlist).

### Engine Bundling Strategy (decision — updated)
Instead of manually bundling raw `yt-dlp` and `aria2c` binaries per-ABI, the app depends on the **`youtubedl-android`** library (Gradle dependency), which bundles a Python runtime, yt-dlp, ffmpeg, and aria2c as modular artifacts (`youtubedl-android:library`, `:ffmpeg`, `:aria2c`). Reasoning:

- **Maintained by someone else** — no need to hand-roll `ProcessBuilder`/JNI wrappers or manage per-ABI native binary extraction and `chmod` logic ourselves
- **Still fully offline/local** — the library bundles everything at build time, same offline-first principle as before, just via a proper dependency instead of raw assets
- **ffmpeg comes free** — this is what upgrades "audio-only download" from a renamed file to a real re-encoded MP3/M4A/OPUS/FLAC, and unlocks the metadata-embedding flow
- **Still paired with an update path** — `youtubedl-android` exposes `updateYoutubeDL()` to refresh the yt-dlp component independently, so the asymmetric-update principle still holds: yt-dlp gets refreshed often, aria2c/ffmpeg rarely need it

**Trade-offs accepted:** larger APK/AAB size (Python runtime + ffmpeg + yt-dlp + aria2c bundled together is heavier than raw binaries alone — mitigated by per-ABI splits, see Phase 0), and a dependency on the `youtubedl-android` project staying maintained upstream.

### Dependency Injection Strategy (decision)
**Hilt** is used for DI across the app instead of manual singleton management or a service locator. Reasoning:

- **Clean lifecycle scoping** — the `YoutubeDL`, `FFmpeg`, and `Aria2c` instances from `youtubedl-android` are expensive to initialize (they touch the bundled Python runtime), so they're provided as `@Singleton`-scoped via a Hilt `@Module` and injected wherever needed (ViewModels, `DownloadService`, repositories) instead of being manually passed around or re-initialized.
- **Testability** — repositories and use-cases can have their engine/DB dependencies swapped for fakes in unit tests via Hilt's test modules.
- **Standard for modern Compose apps** — pairs cleanly with `hiltViewModel()` in Compose Navigation destinations, avoiding manual `ViewModelProvider.Factory` boilerplate.
- **Service injection** — `DownloadService` (a Foreground Service, not an Activity/Fragment) needs `@AndroidEntryPoint` to receive its dependencies (download engine wrapper, Room DAO, notification builder) cleanly.

### App Architecture Layer (decision)
ViewModels never talk to `youtubedl-android` or Room directly — a **Repository layer** sits between them:

- **`DownloadRepository`** — wraps `YtDlpExtractor`, `DownloadService` interaction, and the Room download-history DAO. Exposes clean suspend functions/Flows (`extractInfo()`, `startDownload()`, `observeQueue()`) to ViewModels.
- **`LibraryRepository`** — wraps the Room library DAO plus file-system reads (thumbnails, metadata from mutagen-tagged files). Exposes `observeLibrary()`, `deleteItem()`, `renameItem()`.
- **Why:** keeps ViewModels thin and testable (mock the repository interface instead of the whole engine), keeps Hilt modules organized (`EngineModule` provides the engine singletons → `RepositoryModule` provides repositories built from them → ViewModels just inject repositories).

### Background Reliability: WorkManager + Foreground Service (decision)
The Foreground Service alone only survives while the process is alive. **WorkManager** is added alongside it for anything that needs to survive process death or run periodically:

- **`DownloadRetryWorker`** — re-enqueues a failed/interrupted download if the app process was killed mid-download (Foreground Service handles the *active* download; WorkManager handles *recovering* it after a crash/kill)
- **`EngineUpdateCheckWorker`** — periodic (e.g. weekly) background check via `updateYoutubeDL()` so the yt-dlp component doesn't silently go stale
- **`AppUpdateCheckWorker`** — periodic background check against GitHub Releases for a newer Dolo APK (see Self-Update Checker below)
- The active download itself still runs through `DownloadService` + aria2c for live progress/notifications — WorkManager is for retry/scheduling, not the live download path

### Self-Update Checker (decision)
Since Dolo is distributed as a direct APK (not Play Store), the app needs its own update-awareness mechanism, separate from the yt-dlp engine updater:

- On launch (throttled, e.g. once per day) and via the periodic `AppUpdateCheckWorker`, the app calls the GitHub Releases API for the Dolo repo and compares the latest tag against `BuildConfig.VERSION_NAME`
- If newer, show a non-blocking in-app banner/dialog: "A new version of Dolo is available" with a link to download the APK (opens the browser/download manager to the GitHub release asset — Dolo does **not** silently self-install, that requires the user's explicit action per Android's install-unknown-apps permission model)
- Surfaced in `AboutScreen` too, alongside a manual "Check for app update" button

### App Shortcuts (decision)
A static/dynamic **App Shortcut** ("Paste & Download") is added so a long-press on the launcher icon lets the user jump straight to pasting a link and starting a download without opening `HomeScreen` and tapping through manually — small UX win, especially for repeat users.

### CI/CD & Release Signing (decision)
Since there's no Play Store to handle signing/distribution, CI needs to produce a signed, installable release APK directly:

- **GitHub Actions workflow**, triggered on version tag push (e.g. `v1.2.0`):
  1. Checkout, set up JDK + Android SDK
  2. Run lint + unit tests (fail the workflow if either fails)
  3. Build the release AAB/APK with `./gradlew assembleRelease`
  4. Sign using a release keystore stored as a GitHub Actions secret (never committed to the repo)
  5. Attach the signed APK to a GitHub Release for that tag — this is the same release feed the in-app Self-Update Checker polls, so tagging a release *is* shipping the update
- Debug builds (unsigned, for local testing) can still build on every PR via a separate, lighter CI job

### RTL Layout Support (decision)
Jetpack Compose handles layout mirroring automatically for RTL locales (Arabic, Hebrew, etc.) via `LocalLayoutDirection` — paddings, alignment, and most Material 3 components mirror correctly with no extra code. What still needs explicit handling:
- **Icons that imply direction** (back arrows, playback seek/skip icons, the download-progress arrow) need mirrored variants or a manual flip check, since auto-mirroring doesn't apply to raster/vector icon content itself — only layout position
- **Testing pass required**: RTL doesn't "just work" without verifying each screen visually in an RTL locale (Settings → System → Languages, or an emulator locale override) — treat this as a dedicated QA pass, not just a coding task
- Relevant since Arabic is a likely target language given the language settings feature already planned

### Clipboard Watcher (decision — with an Android platform constraint)
The idea: detect when the user copies a supported link (YouTube, Instagram, etc.) anywhere on the device and surface a quick "Download this?" prompt — a real Snaptube-style convenience feature. The constraint: **Android 10+ blocks background apps from reading the clipboard** — only the app currently in the foreground (or the default input method) can read clipboard contents. This means:
- **What's achievable:** Dolo can check the clipboard the moment it's opened or brought to the foreground (e.g. via `Activity.onResume()`), and show a "We noticed a [YouTube] link in your clipboard — download it?" banner on `HomeScreen`. This is the same mechanism most legitimate downloader apps actually use, despite marketing language like "detects links automatically."
- **What's not achievable without extra permissions:** true always-on background detection while the app is fully closed. That would require an Accessibility Service or a persistent overlay-permission service, which is heavier, more invasive to the user's privacy, and more likely to get the app flagged as suspicious when sideloaded — **not recommended** for this app.
- **Setting:** a toggle in Settings → General to enable/disable the on-resume clipboard check, off by default (respects privacy, avoids surprising the user), with an in-app explanation of what it does when the user turns it on.

### Downloaded File Naming Strategy (decision)
- **Naming mode (setting)**: user chooses between two modes —
  - **Clean title** (default): yt-dlp's extracted `title` field, sanitized — strip filesystem-invalid characters (`/ \ : * ? " < > |`), collapse whitespace, truncate safely if over ~255 bytes (truncate the title, never the extension)
  - **Original filename**: preserves the source's own filename instead of the cleaned title — pulled from yt-dlp's raw `filename`/video-ID-based name for extractor downloads, or from the URL path / `Content-Disposition` header for direct-link downloads via `WebDownloadInterceptor` (which often have no "title" at all, just a filename). Useful when the user wants exact source filenames preserved, e.g. for matching against a site's own naming convention
- **Collision handling**: if a file with the resulting name already exists at the save path, append `(1)`, `(2)`, etc. — never silently overwrite an existing file. Applies in both naming modes, since Original Filename mode can still collide (e.g. two different playlist items happening to share a generic source filename)
- **Playlist items**: naming mode applies per item, same as single downloads —
  - **Clean Title mode**: prefixed with a zero-padded index (`01 - Title.mp4`) so order is preserved when browsing files outside the app
  - **Original Filename mode**: kept exactly as the source names each item, no added index prefix — if the source's own filenames already encode order (common on many sites), that's preserved naturally; if not, order is only guaranteed inside Dolo's Library (sorted by playlist position in the DB), not by browsing the raw filenames
- **Custom naming template** (power-user setting, Clean Title mode only): user-defined pattern using placeholders — `{title}`, `{uploader}`, `{date}`, `{resolution}` — e.g. `{uploader} - {title}`. Exposed as a simple template field in `DownloadSettingsScreen` rather than requiring raw yt-dlp output-template syntax
- **Audio files**: whichever naming mode is active determines the filename; the *original, unsanitized* title/uploader/etc. still get embedded correctly into the mutagen tags either way, since tag fields allow characters filenames can't

### Playlist Folder Organization (decision)
When downloading a playlist (2+ items selected from `PlaylistSelectionScreen`), items are saved into a **dedicated subfolder** named after the playlist, rather than dropped loose into the general downloads folder:

- **Folder name** follows the same naming mode as files: **Clean Title mode** → sanitized playlist title (e.g. `Save Location/My Coding Playlist/`); **Original Filename mode** → the source's own playlist/folder name if yt-dlp exposes one, otherwise falls back to the sanitized playlist title (a raw "original" playlist folder name doesn't always exist the way a video filename does)
- **Folder collision handling**: same `(1)`/`(2)` suffix rule as files, applied to the folder name if one already exists
- **Setting**: "Organize playlist downloads into folders" toggle in `DownloadSettingsScreen`, **on by default** — if turned off, playlist items save flat into the normal save location like any other download, distinguished only by their filenames/index prefixes
- **Single-item selection from a playlist** (per the earlier Share-Intent & Playlist Handling behavior) does **not** create a subfolder — it behaves exactly like a standalone single-video download, saving directly into the normal save location
- **Library display**: playlist downloads can still show as a flat list or grouped by playlist in `LibraryScreen` (grouped view is a nice-to-have, not required for MVP) — the underlying files being in a real subfolder means the organization holds up outside the app too, e.g. if the user browses their file manager or transfers files to a computer

### Clip/Trim Download (decision)
Lets the user download only a segment of a video instead of the whole file — something Snaptube cannot do at all:
- In `FormatPickerSheet`, an optional "Trim" section shows the video's total duration and a dual-handle range slider (or start/end time text fields) for selecting a start and end timestamp
- The selected range is passed to yt-dlp via `--download-sections "*START-END"`, so only the requested segment is fetched and processed — not the full file trimmed after the fact, which saves bandwidth and storage
- Works for both video and audio-only downloads (trim applies before the ffmpeg re-encode step for audio conversions)
- Filename gets a suffix in Clean Title mode when trimmed (e.g. `Title [02-15-04-30].mp4`) so trimmed clips are distinguishable from full downloads in the Library

### Preview Before Downloading (decision)
A real capability gap vs. Snaptube, which can't stream at all:
- After extraction, `FormatPickerSheet` includes a "Preview" button next to the format list
- Tapping it opens a lightweight `PreviewPlayerSheet` that streams the direct media URL (resolved by yt-dlp) directly into ExoPlayer — no download to disk, just a temporary stream for scrubbing/confirming content
- Lets the user verify they've got the right video (especially useful for search results or ambiguous playlist entries) before committing storage and bandwidth to a full download
- Preview is inherently best-effort: some sites' direct URLs are short-lived/signed and may need re-resolving if the user pauses and comes back later — treat as a "quick look," not a full streaming feature, and don't persist preview state across app restarts

### Cookies Import (decision)
Lets users access their own logged-in/members-only content without Dolo ever touching credentials directly:
- User exports a `cookies.txt` file from their own browser (standard browser extension/feature, external to Dolo) and imports it via a file picker in `EngineSettingsScreen`
- The file is stored in app-private storage and passed to yt-dlp via `--cookies <path>` on requests where the user has enabled "Use my cookies" (per-download toggle or global default)
- Dolo never prompts for or stores usernames/passwords — it only ever reads a cookie file the user explicitly chooses to import, and that file only leaves app-private storage when handed to the bundled yt-dlp process, never transmitted anywhere else
- Clear in-app warning when importing: cookies grant access equivalent to being logged in on that site — treat the file like a password and don't share it

### Batch URL Import (decision)
Faster multi-link input than pasting one at a time:
- `HomeScreen` gets a "Batch Import" option (icon or long-press on the paste field) opening a multi-line text area
- User pastes a list of URLs (one per line, or auto-split on whitespace/newlines) → app validates each line as a URL, ignores blanks/invalid entries with a small count shown ("18 valid links found")
- Each valid URL is queued through the normal `extractInfo()` → format-selection flow — for a large batch, a lightweight "apply this format to all" option (similar to the playlist batch preset) avoids requiring per-link format selection
- Distinct from Share-Intent/playlist handling — this is for links from *different, unrelated* sources pasted together (e.g. a text file of saved links), not one site's playlist

### Privacy Stance (decision)
Snaptube's reputation problem is bundled trackers/ad SDKs — Dolo's explicit differentiator is verifiably not doing that:
- **No telemetry by default** — no analytics SDK runs unless the user explicitly opts in (see Phase 8's "Crash reporting/analytics, privacy-respecting, opt-in only")
- **No ad SDKs, no ad network dependencies, period** — nothing to audit here because nothing is integrated
- **Published privacy statement** in `AboutScreen`: a short, plain-language section stating exactly what data stays on-device (everything — downloads, history, settings) and what (if anything, only when opted in) ever leaves the device
- **Open source** reinforces this — since the GPL/LGPL dependencies already require publishing source, the privacy claims are independently verifiable rather than just asserted, and the GitHub repo itself becomes the trust anchor for a sideloaded app that can't rely on Play Store vetting

### Smart Quality Presets (decision)
Reduces "pick a resolution every single time" friction:
- `FormatPickerSheet` leads with three one-tap presets above the full format list: **"Best Quality"** (highest available resolution/bitrate), **"Data Saver"** (lowest reasonable resolution, e.g. 480p / 128kbps audio), **"Quick MP3"** (audio-only, default bitrate, straight to the audio pipeline)
- Tapping a preset skips manual format selection entirely and starts the download immediately with that preset's settings
- The full manual format list stays available below the presets for anyone who wants precise control — presets are a shortcut, not a replacement
- Default preset (used when, e.g., the clipboard watcher banner's quick-download button is tapped) is configurable in `DownloadSettingsScreen`

---

## 3. Full Feature List

### Discovery & Input
- Paste-a-link download (clipboard auto-detect)
- **Clipboard watcher** (opt-in, off by default) — on app resume/foreground, checks clipboard for a supported link and shows a "Download this?" banner on `HomeScreen` (see architecture note on Android's background-clipboard-access restriction)
- Android Share-sheet integration (share a link from YouTube/Instagram app directly into this app)
- In-app browser tab for browsing supported sites without leaving the app
- **Universal download-link interception** — when browsing in the in-app browser, tapping any direct download link (`.mp4`, `.mkv`, `.mp3`, `.zip`, etc., or any `Content-Disposition: attachment` response) is caught via `WebView.setDownloadListener` and routed straight to the aria2c engine instead of the OS's default single-connection download
- Search bar for finding content directly within the app
- URL history (recently pasted/shared links)
- **Batch URL import** — paste a multi-line list of links at once instead of one at a time, with per-link validation and an "apply to all" format option

### Extraction & Format Selection
- Auto-fetch title, thumbnail, duration, and uploader on link paste
- Full format list (resolution, codec, filesize estimate, fps) pulled from yt-dlp
- **Smart Quality presets** — one-tap "Best Quality" / "Data Saver" / "Quick MP3" shortcuts above the full format list
- **Clip/trim download** — select a start/end timestamp to download only a segment of a video via yt-dlp's `--download-sections`, instead of the full file
- **Preview before downloading** — stream the resolved direct URL briefly via ExoPlayer to confirm content before committing to a download
- Audio-only extraction with **real ffmpeg re-encoding** to MP3, M4A, OPUS, or FLAC (selectable bitrate up to 320kbps) — not just a renamed container
- Subtitle/caption download (if available, multiple languages)
- Batch/playlist detection — download entire playlist or select individual items
- Thumbnail preview before download
- **Cookies import** — import a browser-exported `cookies.txt` to access the user's own logged-in/members-only content, without Dolo ever handling credentials directly

### Audio Conversion & Metadata
- Real re-encoding via bundled ffmpeg (not container renaming) to MP3 / M4A / OPUS / FLAC
- Selectable bitrate/quality per format
- **Embedded metadata via mutagen**: title, artist/uploader, album, and track thumbnail embedded directly into the audio file's tags (ID3 for MP3, Vorbis comments for OPUS/FLAC, MP4 atoms for M4A)
- Converted files show proper cover art and tags in any music player, not just inside Dolo

### Download Engine
- Multi-connection segmented downloads via aria2c (faster than single-stream)
- Pause / resume / cancel per download
- Auto-resume on network reconnect
- Queue management — reorder, prioritize, set max concurrent downloads
- Wi-Fi-only toggle (per-download or global default)
- Background downloading via foreground service with persistent notification + progress bar
- **Notification actions** — pause/cancel a download directly from the persistent notification without opening the app
- Retry-on-failure with backoff
- **Duplicate download detection** — before queuing, check `LibraryRepository`/`DownloadRepository` for an existing entry with the same source URL + format, warn the user ("Already downloaded — download again?") instead of silently re-downloading
- **Storage space check** — verify free space (via `StatFs`) against the estimated file size before starting; warn if insufficient rather than failing mid-download
- **Download speed control**:
  - Global speed limit (KB/s or MB/s cap across all active downloads) via aria2c's `max-overall-download-limit`
  - Per-download speed limit (throttle one file independently) via `max-download-limit`, adjustable from the Queue screen
  - Connections-per-download setting (power-user control, 1–16) via `--max-connection-per-server` / `-x` — more connections = faster on most networks, this is what beats Snaptube's single-connection downloads

### Library & Playback
- Built-in offline player (video + audio) via ExoPlayer — no buffering, no ads
- Library view: grid/list toggle, sort by date/size/name
- Rename, delete, share downloaded files
- **Bulk actions** — multi-select mode in Library for batch delete/share instead of one item at a time
- Converted audio files display embedded cover art and tags in the Library UI (sourced from the same mutagen-written metadata)

### Vault (Private Folder)
- Password/biometric-locked folder
- Move/hide specific downloads from main Library
- Separate encrypted storage option (stretch goal)

### App Maintenance & Distribution
- **Self-update checker**: periodic + on-launch check against GitHub Releases for a newer Dolo APK version, with an in-app banner/dialog prompting manual download+install (no silent self-install)
- **In-app changelog**: small "What's New" sheet shown once after a version bump is detected (compare stored last-seen version vs. current `BuildConfig.VERSION_NAME`)
- **App Shortcut**: long-press launcher icon → "Paste & Download" quick action straight into the download flow
- **About screen "Check for app update" button**: manual trigger for the same self-update check
- **Published privacy statement**: plain-language section in `AboutScreen` confirming no telemetry by default, no ad SDKs, and everything stays on-device unless the user explicitly opts into crash reporting

### Settings
- **General**: theme (light/dark/system), notification preferences, cache clearing, **clipboard watcher toggle (off by default)**
- **Download settings**: default resolution/quality preset, **default Smart Quality preset**, max simultaneous downloads, Wi-Fi-only default, default save location (internal storage / SD card / custom folder via SAF)
- **Download settings → File naming**: naming mode toggle — Clean Title (default) vs. Original Filename — plus a custom filename template using `{title}`, `{uploader}`, `{date}`, `{resolution}` placeholders (Clean Title mode only, default template: `{title}`)
- **Download settings → Playlist organization**: "Organize playlist downloads into folders" toggle, on by default
- **Download settings → Speed limit**: toggle Unlimited or set a global cap (KB/s or MB/s), maps to `max-overall-download-limit`
- **Download settings → Connections per download**: slider (1–16) for power users, maps to `--max-connection-per-server` / `-x`
- **Audio settings**: default conversion format (MP3/M4A/OPUS/FLAC), default bitrate, toggle metadata embedding on/off
- **Engine settings**: yt-dlp version + manual "check for update" button (`updateYoutubeDL()`), aria2c connection count per download (advanced/power-user toggle), **cookies.txt import + "Use my cookies" default toggle**
- **Vault**: set/change password, biometric unlock toggle
- **Language**: UI language selector
- **About**: app version, **manual "Check for app update" button** (GitHub Releases check), **privacy statement**, licenses (yt-dlp/aria2c/ffmpeg/`youtubedl-android`/mutagen are GPL/LGPL — must credit + link source per license), feedback/bug report link, link to open-source repo

### Pages/Screens Summary (Compose)
All screens are Composables navigated via **Compose Navigation** inside a single `MainActivity` — no Fragments, no XML.

1. `OnboardingScreen` — first-launch flow: welcome, storage permission request, engine setup loading state
2. `HomeScreen` — paste link / share-intent landing, hosts the in-app browser tab, entry point for Batch Import
3. `SearchScreen` — in-app content search
4. `BatchImportSheet` — multi-line link paste, per-line validation, batch format application
5. `FormatPickerSheet` — resolution/audio/subtitle selection bottom sheet, includes audio-format + bitrate picker, Smart Quality presets, trim range selector, Preview button
6. `PreviewPlayerSheet` — lightweight streaming preview of the resolved direct URL before download
7. `PlaylistSelectionScreen` — playlist/batch detection results, select all or individual items before queuing
8. `DownloadQueueScreen` — active/paused/queued downloads with progress bars, per-download speed limit control
9. `LibraryScreen` — completed downloads, playback, sort/grid-list toggle, shows embedded cover art for converted audio
10. `PlayerScreen` — full-screen ExoPlayer view (Compose-hosted `PlayerView`)
11. `VaultScreen` — locked private downloads
12. `SettingsScreen` — settings hub, navigates to the sub-screens below
    - `GeneralSettingsScreen` — theme, notifications, cache clearing
    - `DownloadSettingsScreen` — default quality, max concurrent downloads, Wi-Fi-only, save location, speed limit, connections-per-download
    - `AudioSettingsScreen` — default conversion format/bitrate, metadata embedding toggle
    - `EngineSettingsScreen` — yt-dlp version + update check, aria2c/ffmpeg advanced options, cookies.txt import
    - `VaultSettingsScreen` — set/change password, biometric toggle
    - `LanguageSettingsScreen` — UI language selector
13. `AboutScreen` — version, licenses, credits, feedback, manual "Check for app update" button, privacy statement, open-source repo link

---

## 4. Legal/Compliance Notes (read before shipping)
- yt-dlp, aria2c, ffmpeg, and mutagen carry **GPL/LGPL licenses** — if you distribute the app publicly, you must comply with those obligations (credit, link to source, and if you modify the components' source, make those modifications available). The `youtubedl-android` wrapper library has its own license terms as well — review it before shipping.
- Downloading copyrighted content from platforms like YouTube generally **violates those platforms' Terms of Service**, even though the yt-dlp tool itself is legal to build and distribute (established precedent from the youtube-dl DMCA case, which was reversed).
- Google Play policy explicitly **disallows apps whose primary purpose is downloading YouTube video/audio** — this is why Snaptube isn't on the Play Store. Plan to distribute via direct APK / F-Droid-style distribution, not Play Store, unless you strip that functionality for a Play-compliant build.

---

## 5. Roadmap: MVP → Full App

### 🟢 Phase 0 — Project Setup
- [ ] Create Android Studio project (Kotlin, min SDK 24+, single-Activity Compose template)
- [ ] Add `youtubedl-android` dependencies (`library`, `ffmpeg`, `aria2c` modules) to `build.gradle.kts`
- [ ] Add Hilt dependency + `@HiltAndroidApp` Application class, set up `hilt-navigation-compose` for `hiltViewModel()` support
- [ ] Add WorkManager dependency + Hilt-WorkManager integration (`HiltWorkerFactory`)
- [ ] Set up Gradle module structure (`app`, `core-engine`, `core-ui`)
- [ ] Configure per-ABI splits (arm64-v8a, armeabi-v7a) for AAB
- [ ] Set up Compose Navigation graph and Material 3 theming baseline
- [ ] Set up Room DB for download history/library metadata
- [ ] Build `OnboardingScreen` — welcome, storage permission request, engine setup loading state on first launch (library self-initializes Python/yt-dlp/ffmpeg on first run)
- [ ] Set up GitHub repo, README, GPL/LGPL license file, CI (lint + build)
- [ ] Set up release keystore + store as GitHub Actions secret (not committed to repo)
- [ ] Build GitHub Actions release workflow: on version tag push → lint + test → `assembleRelease` → sign → attach signed APK to GitHub Release

### 🟢 Phase 1 — Engine Integration (MVP core)
- [ ] Build `EngineModule` (Hilt `@Module`, `@InstallIn(SingletonComponent::class)`): provides `@Singleton` instances of `YoutubeDL`, `FFmpeg`, `Aria2c` from `youtubedl-android`
- [ ] Initialize `YoutubeDL`, `FFmpeg`, and `Aria2c` singletons from `youtubedl-android` (via Hilt-provided instances, initialized in `Application.onCreate()`)
- [ ] Build `YtDlpExtractor` wrapper (constructor-injected via Hilt): call `YoutubeDL.getInfo(url)`, map `VideoInfo` → app's Kotlin data class (title, thumbnail, formats[], duration, uploader)
- [ ] Build `DownloadService` (Foreground Service, `@AndroidEntryPoint`): wraps the library's aria2c-backed download execution, manages lifecycle, receives dependencies via Hilt field injection
- [ ] Build `DownloadRepository` and `LibraryRepository` (Hilt `RepositoryModule`): sit between ViewModels and the engine/Room DB, expose clean suspend functions/Flows
- [ ] Build `DownloadRetryWorker` (Hilt-assisted `CoroutineWorker`): re-enqueues failed/interrupted downloads after app/process death, scheduled via WorkManager
- [ ] Build `MetadataEmbedder`: invokes mutagen via the bundled Python runtime to write ID3/Vorbis/MP4 tags + thumbnail into converted audio files
- [ ] Build `WebDownloadInterceptor`: hook `WebView.setDownloadListener` in the in-app browser to catch direct file links / `Content-Disposition: attachment` responses and route them to `DownloadService`, bypassing yt-dlp extraction for sites that expose direct download links
- [ ] Add `ACTION_SEND` intent filter to catch shared links (YouTube/Instagram/etc. "Share" → Dolo), route through the same `extractInfo()` path as pasted links
- [ ] Handle Android 13+ `POST_NOTIFICATIONS` runtime permission request (required before the foreground service notification can display)
- [ ] Build duplicate-download check in `DownloadRepository` (query existing entries by source URL + format before queuing)
- [ ] Build storage-space check (`StatFs`) against estimated file size before starting a download, surfaced as a warning dialog if insufficient
- [ ] Build `FileNamer`: supports both naming modes (Clean Title vs. Original Filename) for both single items and playlist items, sanitizes for filesystem safety, handles collision suffixes `(1)`/`(2)` for files and folders, applies zero-padded index prefix for playlist items in Clean Title mode, supports the custom naming template from settings, extracts original filenames from URL path / `Content-Disposition` header for `WebDownloadInterceptor` downloads
- [ ] Add `--download-sections` support to `YoutubeDLRequest` builder for clip/trim downloads (start/end timestamp params)
- [ ] Add `--cookies <path>` support to `YoutubeDLRequest` builder, reading from an app-private cookies file path when the "Use my cookies" toggle is enabled
- [ ] Test end-to-end: paste YouTube link → extract formats → download via aria2c → file saved
- [ ] Test audio conversion end-to-end: select "audio only" → ffmpeg re-encode → mutagen metadata embed → verify tags/art in a third-party music player

### 🟡 Phase 2 — MVP UI
- [ ] `HomeScreen` with paste-link input + "Paste from clipboard" button, backed by a Hilt-injected `HomeViewModel` (via `hiltViewModel()`, calling `DownloadRepository`)
- [ ] Share-sheet intent handling (`ACTION_SEND` from other apps)
- [ ] `FormatPickerSheet` — list formats returned by extractor, tap to start download, includes audio format/bitrate selector and Smart Quality presets ("Best Quality" / "Data Saver" / "Quick MP3") above the manual list
- [ ] `DownloadQueueScreen` — list active downloads with live progress (collect from `DownloadRepository.observeQueue()`, exposed via a `DownloadQueueViewModel`)
- [ ] Persistent notification for foreground service showing overall download progress, with **pause/cancel action buttons** wired to `PendingIntent`s that call back into `DownloadService`
- [ ] Basic `LibraryScreen` — list completed files via `LibraryRepository.observeLibrary()`, tap to play via system player (stub)
- [ ] **MVP DONE**: user can paste a link, pick quality (video or converted audio), download, and see it in a list

### 🟡 Phase 3 — Playback & Library Polish
- [ ] Integrate Media3/ExoPlayer for in-app playback (video + audio), hosted inside a Compose `AndroidView`
- [ ] `PlayerScreen` full-screen player with standard controls
- [ ] Build `PreviewPlayerSheet`: streams the resolved direct URL via ExoPlayer for a quick scrub/preview before downloading, wired to a "Preview" button in `FormatPickerSheet`
- [ ] Add trim range selector (dual-handle slider or start/end fields) to `FormatPickerSheet`, wired to the `--download-sections` request builder from Phase 1
- [ ] Library grid/list toggle, sort options
- [ ] Rename/delete/share actions on library items
- [ ] Multi-select mode in `LibraryScreen` for bulk delete/share
- [ ] Display embedded cover art (from mutagen-tagged files) and metadata in Library cards

### 🟡 Phase 4 — Queue & Reliability
- [ ] Pause/resume/cancel controls per download in queue UI
- [ ] Reorder queue, set max concurrent downloads setting
- [ ] Wi-Fi-only toggle (global + per-download override)
- [ ] Auto-resume on reconnect / retry-on-failure with backoff
- [ ] Handle app kill/restart gracefully (aria2c session persistence via the library's session options)
- [ ] Global speed limit setting (maps to aria2c's `max-overall-download-limit` via the library's request options)
- [ ] Per-download speed limit control from Queue screen (`max-download-limit`)
- [ ] Connections-per-download slider (advanced setting) → `--max-connection-per-server` / `-x`
- [ ] Add `setGlobalSpeedLimit(kbps: Int)` and `setDownloadSpeedLimit(id: String, kbps: Int)` methods to the download engine wrapper

### 🟠 Phase 5 — Settings & Customization
- [ ] `SettingsScreen` hub with navigation to sub-screens below
- [ ] `GeneralSettingsScreen` — theme switcher (light/dark/system), notification preferences, cache clearing
- [ ] `DownloadSettingsScreen` — default quality preset, max concurrent downloads, Wi-Fi-only default, save location (SAF picker for custom/SD card paths), speed limit, connections-per-download
- [ ] `AudioSettingsScreen` — default conversion format (MP3/M4A/OPUS/FLAC), default bitrate, metadata embedding on/off toggle
- [ ] `EngineSettingsScreen` — yt-dlp version display + manual "check for update" (calls `updateYoutubeDL()`), aria2c/ffmpeg advanced options, **cookies.txt file picker + import (stores in app-private storage) + "Use my cookies" default toggle**, with an in-app warning about treating the file like a password
- [ ] `VaultSettingsScreen` — set/change password, biometric toggle
- [ ] `LanguageSettingsScreen` — UI language selector, localization setup (start with 2-3 languages, expand later)
- [ ] Build `AboutScreen` privacy statement section: plain-language "what stays on-device" summary, open-source repo link
- [ ] Build `BatchImportSheet`: multi-line paste input, per-line URL validation with valid/invalid count, "apply format to all" batch option, queues each valid link through `extractInfo()`
- [ ] Build `EngineUpdateCheckWorker`: periodic WorkManager job calling `updateYoutubeDL()` in the background
- [ ] Build `AppUpdateChecker`: compares GitHub Releases latest tag vs. `BuildConfig.VERSION_NAME`
- [ ] Build `AppUpdateCheckWorker`: periodic + on-launch (throttled) background check via `AppUpdateChecker`
- [ ] In-app update banner/dialog: non-blocking prompt linking to the GitHub release APK download when a newer version is found
- [ ] `AboutScreen` — manual "Check for app update" button wired to `AppUpdateChecker`
- [ ] Build clipboard watcher: `onResume()` check + "Download this?" banner on `HomeScreen`, gated behind the opt-in Settings toggle (off by default)
- [ ] Build "What's New" changelog sheet: shown once after detecting a version bump, dismissible, content sourced from a simple bundled changelog file/resource
- [ ] Add "Paste & Download" App Shortcut (static or dynamic `ShortcutManager` entry) linking directly into the paste-link flow

### 🟠 Phase 6 — Vault & Privacy
- [ ] Vault folder implementation with password/PIN
- [ ] Biometric unlock (BiometricPrompt API)
- [ ] Move-to-vault / remove-from-vault actions in Library
- [ ] (Stretch) Encrypt vault contents at rest

### 🔴 Phase 7 — Batch/Playlist & Advanced Extraction
- [ ] Playlist/channel detection in extractor via yt-dlp `--flat-playlist` (fast metadata-only pass before full extraction)
- [ ] Build `PlaylistSelectionScreen` — select all / select individual items UI, with a global batch quality/format preset
- [ ] Playlist subfolder creation: build the folder path via `FileNamer` (Clean Title or Original Filename mode, with collision suffixing), route all selected item downloads into it when "Organize playlist downloads into folders" is enabled; skip subfolder creation when only one item is selected
- [ ] Batch download queueing — one request per selected item, queued in sequence via `DownloadRepository`, each tracked independently in `DownloadQueueScreen`
- [ ] "Customize" option on a single playlist item to override the batch preset and open the normal `FormatPickerSheet` for just that item
- [ ] Detect `list=` + `v=` combo in a shared/pasted URL → show "just this video, or the whole playlist?" prompt before routing to `FormatPickerSheet` vs. `PlaylistSelectionScreen`
- [ ] Subtitle download support (list + select language)
- [ ] Audio-only extraction with format + bitrate selection, verify mutagen tagging works across all playlist items
- [ ] Verify `FileNamer` trim-suffix behavior (e.g. `Title [02-15-04-30].mp4`) works correctly in both naming modes and doesn't collide with the untrimmed full-download filename

### 🔴 Phase 8 — Hardening & Release Prep
- [ ] Error handling: invalid links, unsupported sites, extraction/conversion failures with user-friendly messages
- [ ] Storage permission handling across Android versions (scoped storage compliance)
- [ ] RTL QA pass — verify every screen in an RTL locale (e.g. Arabic), fix any icons needing manual mirroring
- [ ] APK size optimization, AAB per-ABI split testing (watch bundle size given Python + ffmpeg + yt-dlp + aria2c)
- [ ] GPL/LGPL compliance: license page, source links, attribution for yt-dlp/aria2c/ffmpeg/mutagen/`youtubedl-android`
- [ ] Crash reporting/analytics (privacy-respecting, opt-in only)
- [ ] Privacy audit: confirm no telemetry/analytics/ad SDK code runs unless explicitly opted in, verify claims in the `AboutScreen` privacy statement are accurate before shipping
- [ ] Verify release-signing CI workflow end-to-end (tag push → signed APK → GitHub Release → picked up by in-app Self-Update Checker)
- [ ] Beta test build, gather feedback
- [ ] Distribution: direct APK download page (own site) + optional F-Droid submission

### ⚪ Phase 9 — Post-Launch / Nice-to-Haves
- [ ] Download speed limiter presets (e.g. quick "throttle to 1MB/s" shortcut)
- [ ] Widget for quick paste-and-download
- [ ] Tasker/automation integration
- [ ] Tablet/foldable responsive Compose layouts
- [ ] Batch metadata editing in Library (edit tags after the fact via mutagen)
- [ ] **[Optional, not default] True background clipboard watcher via Accessibility Service or overlay permission** — evaluate only if the on-resume clipboard check proves insufficient for users. Trade-offs to weigh before building:
  - Requires the user to grant Accessibility Service or "draw over other apps" permission — both are high-trust, high-friction permissions that most users are (rightly) wary of granting to a sideloaded app
  - Must be strictly opt-in, off by default, with a clear in-app explanation of exactly what the service does and does not do (read clipboard only, nothing else) before the permission prompt
  - Increases the app's "invasive-looking" surface area, which matters more for a sideloaded/direct-APK app than a Play Store one, since users can't rely on Play's app-review vetting to reassure them
  - Purely an Accessibility Service used for clipboard-reading (not actual accessibility purposes) sits in a grey area of intended use for that API — worth a clear in-app disclosure regardless of store policy
  - **Recommendation if built:** keep the on-resume check as the default/primary path; gate this behind an explicit "Advanced" settings section, never enable it silently

---

## 6. Suggested Build Order (First 3 Sprints)

**Sprint 1 (Engine proof-of-concept):** Phase 0 + Phase 1. Goal: prove yt-dlp + aria2c + ffmpeg + mutagen work reliably inside an Android app (via `youtubedl-android`) before investing in UI.

**Sprint 2 (MVP):** Phase 2. Goal: a working, ugly-but-functional Compose app you can install and use end-to-end.

**Sprint 3 (Usable app):** Phase 3 + Phase 4. Goal: something you'd actually want to use daily, with playback and a reliable queue.

Everything after Sprint 3 (Settings, Vault, Batch, Hardening) is polish that can be sequenced based on what you personally want next.

---

*This spec is a living document — update it as design decisions change during development.*
