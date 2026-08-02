# Task List - Permission & Download Fixes

- [ ] Fix Onboarding Flow
    - [ ] Update `OnboardingScreen.kt` to auto-initialize if permissions are granted.
- [ ] Fix Download Path Error
    - [ ] Update `StorageResolver.kt` to handle `MediaStore` move on Android 10+.
    - [ ] Update `DownloadService.kt` to use a guaranteed writable directory for `yt-dlp`.
- [ ] Verification
    - [ ] Verify onboarding skips automatically.
    - [ ] Verify downloads complete successfully on Android 10+.
