package com.dolo.core.repository

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val sharedPreferences = EncryptedSharedPreferences.create(
        "dolo_vault_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private object Keys {
        const val VAULT_PASSWORD = "vault_password"
        const val BIOMETRIC_ENABLED = "biometric_enabled"
        const val IS_INITIALIZED = "is_initialized"
    }

    fun isInitialized(): Boolean {
        return sharedPreferences.getBoolean(Keys.IS_INITIALIZED, false)
    }

    fun setPassword(password: String) {
        sharedPreferences.edit()
            .putString(Keys.VAULT_PASSWORD, password)
            .putBoolean(Keys.IS_INITIALIZED, true)
            .apply()
    }

    fun verifyPassword(password: String): Boolean {
        val storedPassword = sharedPreferences.getString(Keys.VAULT_PASSWORD, null)
        return storedPassword == password
    }

    fun isBiometricEnabled(): Boolean {
        return sharedPreferences.getBoolean(Keys.BIOMETRIC_ENABLED, false)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(Keys.BIOMETRIC_ENABLED, enabled)
            .apply()
    }
    
    fun getVaultDir(): java.io.File {
        val dir = java.io.File(context.filesDir, "vault")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
}
