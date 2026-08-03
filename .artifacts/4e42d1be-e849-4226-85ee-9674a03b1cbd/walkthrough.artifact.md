# Walkthrough - Dynamic Format Display

I have transformed the **Format Picker** into a fully dynamic experience that mirrors exactly what the source URL provides.

## Key Enhancements

### 1. Transparent Format Listing
- Removed the strict filtering that hidden different codecs for the same resolution.
- You can now see every available variant of a video (e.g., if a 1080p video is available in both **MP4/H.264** and **WebM/VP9**, both will appear).

### 2. Technical Bitrate Info
- Added **Bitrate (k)** display to both video and audio formats.
- This allows you to choose between formats not just by resolution, but by actual data quality (e.g., choosing a high-bitrate 720p over a low-bitrate 1080p).

### 3. Rich Metadata
- Added **Codec** and **Format Note** labels.
- You'll now see if a format is **HDR**, **Premium**, or uses a specific efficient codec like **AV1** or **OPUS**.

### 4. Smarter Sorting
- Improved the list ordering to use a combination of resolution height and video bitrate, ensuring the highest quality options always stay at the top.

## How it looks now
Each row in the list now looks like:
> **1080p · 60fps · 4500k**
> MP4 · H.264 · HDR · 245 MB

## Verification Results
- Verified with various platforms (YouTube, SoundCloud).
- All formats returned by `yt-dlp` are now mapped and displayed correctly without arbitrary exclusion.
