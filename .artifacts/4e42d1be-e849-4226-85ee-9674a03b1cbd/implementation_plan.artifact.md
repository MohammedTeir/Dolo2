# Implementation Plan - Fix HTTP 403 & Download Stability

This plan addresses the `HTTP Error 403: Forbidden` error and the slow download start reported.

## Root Cause Analysis
- **HTTP 403 Forbidden**: Usually occurs when the server (e.g., YouTube) detects automated usage or the link signature expires. Segmented downloading with `aria2c` can sometimes trigger this if the headers aren't perfectly matched or if too many connections are used.
- **Slow Start**: Often caused by the engine performing heavy extraction or trying to initialize the multi-connection segments.

## Proposed Changes

### [core-engine]

#### [MODIFY] [DownloadRequestBuilder.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/engine/DownloadRequestBuilder.kt)
- **Add User-Agent**: Include a real browser User-Agent to bypass basic bot detection.
- **Add Evasion Flags**:
    - `--no-check-certificate`: To avoid SSL handshake issues in the Python runtime.
    - `--rm-cache-dir`: To clear old session data that might have expired tokens.
- **Tweak YouTube Extractor**:
    - Add `--extractor-args "youtube:player_client=android"` which is often more stable on Android.
- **Improve Aria2c args**:
    - Add `--summary-interval=0` to reduce logging overhead.
    - Ensure certificates are ignored in aria2c too if needed.

#### [MODIFY] [YtDlpExtractor.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/engine/YtDlpExtractor.kt)
- Add similar safety flags (`--no-check-certificate`) to the info extraction request to speed it up and avoid errors.

### [app]

#### [MODIFY] [EngineSettingsScreen.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/settings/EngineSettingsScreen.kt)
- Add a "Clear Engine Cache" button that calls a new method in the repository.

## Verification Plan

### Manual Verification
1.  **Retry Failed Download**: Use the **Resume** button on the failed items.
2.  **Stability Check**: Download a long video (10+ mins) and verify it doesn't fail mid-way with 403.
3.  **Speed**: Verify that the "Starting..." state is shorter.
4.  **Engine Update**: Ensure the user is prompted to use the **"Check for Engine Update"** button if they haven't recently, as YouTube frequently breaks older `yt-dlp` versions.
