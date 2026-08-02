# Task List - Phase 5 Polish & Fixes

- [ ] Update `SettingsRepository`
    - [ ] Add `downloadLocationUri` and `downloadLocationName` keys
- [ ] Create `SettingsComponents.kt`
    - [ ] Extract reusable UI elements (Switch, Radio, Slider)
- [ ] Implement Live Theme Switching
    - [ ] Update `MainActivity.kt` to observe and apply theme
- [ ] Polish Settings Screens
    - [ ] Update `DownloadSettingsScreen.kt`: Add SAF Folder Picker and Speed Limit Slider
    - [ ] Update `EngineSettingsScreen.kt`: Add Cookies File Picker and Update Engine Trigger
    - [ ] Update `AboutScreen.kt`: Add App Update Trigger
    - [ ] Update `AudioSettingsScreen.kt`: Use centralized components
    - [ ] Update `GeneralSettingsScreen.kt`: Use centralized components
- [ ] Smart Features Logic
    - [ ] Implement actual clipboard check in `HomeScreen.kt`
- [ ] Verification
    - [ ] Verify theme switches instantly
    - [ ] Verify custom save location works
    - [ ] Verify cookies import persists
