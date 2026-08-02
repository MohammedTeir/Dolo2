# Implementation Plan - Phase 6: Vault & Privacy

This phase implements a secure, private folder (Vault) for sensitive downloads, protected by a password/PIN and biometric authentication.

## Proposed Changes

### [build]

#### [MODIFY] [libs.versions.toml](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/gradle/libs.versions.toml)
- Add `securityCrypto = "1.1.0-alpha06"`
- Add `biometric = "1.2.0-alpha05"`

#### [MODIFY] [app/build.gradle.kts](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/build.gradle.kts)
- Add `implementation(libs.androidx.security.crypto)`
- Add `implementation(libs.androidx.biometric.ktx)`

### [core-engine]

#### [MODIFY] [LibraryItemEntity.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/db/LibraryItemEntity.kt)
- Add `isInVault` boolean field (default: `false`).
- Update database version in `DoloDatabase.kt`.

#### [MODIFY] [LibraryItemDao.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/db/LibraryItemDao.kt)
- Update `observeAllLibraryItems` to exclude items where `isInVault == true`.
- Add `observeVaultItems()` to get only vaulted items.
- Add `updateVaultStatus(id: String, isInVault: Boolean, newPath: String)`.

#### [NEW] [VaultRepository.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/repository/VaultRepository.kt)
- Manage vault state:
    - Set/Change Password (using `androidx.security:security-crypto` for encrypted storage).
    - Verify Password.
    - Check if Vault is initialized.
    - Handle Biometric authentication status.

#### [MODIFY] [LibraryRepository.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/core-engine/src/main/java/com/dolo/core/repository/LibraryRepository.kt)
- Add `moveToVault(id: String)`:
    - Moves the file from the public download folder to an app-private "vault" directory.
    - Updates DB flag and path.
- Add `removeFromVault(id: String)`:
    - Moves the file back to the public download folder.
    - Updates DB flag and path.

### [app]

#### [NEW] Vault UI
- **`VaultAuthScreen.kt`**: Password/PIN entry and Biometric trigger.
- **`VaultScreen.kt`**: The library view for private items.
- **`VaultSetupScreen.kt`**: First-time password setup.

#### [MODIFY] [LibraryScreen.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/library/LibraryScreen.kt)
- Add "Move to Vault" action to library items.

#### [MODIFY] [MainScreen.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/ui/MainScreen.kt)
- Add a "Vault" entry point (perhaps in the drawer or a floating button in Library).
- Actually, the spec says "VaultScreen" is a separate destination.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/teirm/AndroidStudioProjects/Dolo2/app/src/main/java/com/dolo/dolo/MainActivity.kt)
- Add navigation routes for Vault screens.

## User Review Required

> [!IMPORTANT]
> Files moved to the Vault will be relocated to the app's internal storage. If the user clears app data or uninstalls the app without moving files out of the Vault, **the files will be lost**. I will add an in-app warning about this during Vault setup.

## Verification Plan

### Automated Tests
- Unit tests for `VaultRepository` to verify password encryption and verification.
- Unit tests for file moving logic in `LibraryRepository`.

### Manual Verification
1.  **Setup**: Open Vault for the first time, set a PIN.
2.  **Hiding**: Move a video to the Vault. Verify it disappears from the main Library and is NOT visible in the phone's Gallery app.
3.  **Authentication**: Close and reopen the Vault. Verify it asks for the PIN.
4.  **Unhiding**: Move the video out of the Vault. Verify it reappears in the main Library and the Gallery app.
5.  **Biometrics**: Enable Biometric unlock in settings and verify it works when entering the Vault.
