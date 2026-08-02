# Dolo

A native Android video/audio downloader powered by `yt-dlp` + `aria2c` + `ffmpeg` via the `youtubedl-android` library. Built with Kotlin, Jetpack Compose, Hilt, Room, and WorkManager.

## Architecture

- **UI**: Jetpack Compose, Material 3, Single Activity architecture
- **Extraction & Engine**: `youtubedl-android` (`yt-dlp`, `aria2c`, `ffmpeg`)
- **Dependency Injection**: Hilt
- **Local Storage**: Room DB
- **Background Execution**: Foreground Service + WorkManager

## License

GPL-3.0 License. Bundles `yt-dlp`, `ffmpeg`, `aria2c`, and `youtubedl-android`.
