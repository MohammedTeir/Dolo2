# Task List - Dynamic Format Display

- [x] Update Engine Models
    - [x] Add `abr` and `vbr` to `FormatInfo.kt`
- [x] Update Extractor Logic
    - [x] Map bitrates from `yt-dlp` response in `YtDlpExtractor.kt`
- [x] Refactor Format Picker UI
    - [x] Remove aggressive `distinctBy` filtering in `FormatPickerSheet.kt`
    - [x] Show codec details for every format
    - [x] Display bitrates (k) for better quality comparison
    - [x] Include format notes (e.g., "HDR", "Premium") in the list
- [x] Verification
    - [x] Test with multi-codec videos (VP9/AV1/H.264)
    - [x] Test with audio-only sources
