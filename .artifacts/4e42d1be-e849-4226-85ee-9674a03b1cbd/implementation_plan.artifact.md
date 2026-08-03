# Implementation Plan - Snaptube Style Format Picker

Redesign the `FormatPickerSheet` to provide a cleaner, more organized, and descriptive selection experience inspired by Snaptube.

## Proposed Changes

### [app]

#### [MODIFY] [FormatPickerSheet.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/formatpicker/FormatPickerSheet.kt)
- **New Selection State**:
    - Add `selectedFormatId` and `selectedType` (Music/Video/Re-encode) to track user choice.
    - Add `isMoreFormatsExpanded` state.
- **Header**: Maintain thumbnail and title, but polish layout.
- **"Music" Section**:
    - Fixed list of common conversion options:
        - "Fast" (M4A 128k)
        - "Standard" (MP3 128k)
        - "High Quality" (MP3 320k)
    - Show estimated file sizes based on bitrate and duration.
- **"Video" Section**:
    - Pick top resolutions (360p, 480p, 720p, 1080p, 2K/4K) from extracted formats.
    - Add descriptive sub-labels (e.g., "Good for mobile", "Best for large screens").
- **"More Formats" Row**: A toggle to show the full technical list of streams from `yt-dlp`.
- **Subtitles Row**: A single row that shows current selection and opens the selection grid.
- **Primary Action Button**:
    - Move "Download" to a large button at the bottom of the sheet.
    - Enable/Disable based on selection.
- **Radio Selection UI**: Use radio buttons instead of immediate download triggers.

## Verification Plan

### Manual Verification
1.  **Selection**: Select "Music -> High Quality". Verify the radio button updates.
2.  **Download**: Click the bottom "Download" button. Verify the correct parameters are sent to `DownloadService`.
3.  **Expansion**: Click "More formats". Verify the full list appears.
4.  **Subtitles**: Select a subtitle language. Verify it is reflected in the main row.
5.  **Visuals**: Compare against the provided Snaptube screenshots.
