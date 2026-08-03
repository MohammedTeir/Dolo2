# Implementation Plan - Dynamic Format Display

Improve `FormatPickerSheet` to show a comprehensive and dynamic list of formats directly from the extracted URL metadata, ensuring no formats are hidden by aggressive filtering.

## Proposed Changes

### [app]

#### [MODIFY] [FormatPickerSheet.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/formatpicker/FormatPickerSheet.kt)
- **Refine Video Format List**:
    - Remove `distinctBy` or make it more inclusive (e.g., include codecs in the key).
    - Ensure formats without a standard resolution string (but with `formatNote`) are displayed.
    - Show more technical details like bitrate if available.
- **Refine Audio Format List**:
    - Ensure all extracted audio formats are shown.
- **Improve Format Labels**:
    - Show codec information (H.264, VP9, AV1, etc.) directly in the list so users can see the actual variety available.
- **Toggle for "Advanced Formats"**:
    - (Optional) Add a toggle to show *every* raw format returned by the extractor for power users.

### [core-engine]

#### [MODIFY] [FormatInfo.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/model/FormatInfo.kt)
- Add `abr` (audio bitrate) and `vbr` (video bitrate) fields to provide more info in the UI.

#### [MODIFY] [YtDlpExtractor.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/engine/YtDlpExtractor.kt)
- Update `mapFormat` to extract `abr` and `vbr` from `VideoFormat`.

## Verification Plan

### Manual Verification
1.  **Variety Check**: Paste a link to a video known to have many formats (e.g., a popular YouTube video with multiple HDR/codec options).
2.  **Codecs**: Verify that different codecs (e.g., mp4 vs webm for the same resolution) are both visible in the list.
3.  **Details**: Verify that format notes or bitrates are visible where applicable.
4.  **Audio**: Check that the "Audio-only formats" section accurately reflects the source's available streams.
