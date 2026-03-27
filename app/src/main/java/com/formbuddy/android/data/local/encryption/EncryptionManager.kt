package com.formbuddy.android.data.local.encryption

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptionManager @Inject constructor(
    private val context: Context
) {
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .setUserAuthenticationRequired(false)
            .build()
    }

    // Cache the EncryptedSharedPreferences instance — it's expensive to create
    private val securePrefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            "formbuddy_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getDatabasePassphrase(): ByteArray {
        val existingKey = securePrefs.getString("db_passphrase", null)
        if (existingKey != null) {
            return existingKey.toByteArray(Charsets.ISO_8859_1)
        }

        // Use SecureRandom for cryptographically strong passphrase (not KeyGenerator which is for Keystore keys)
        val passphrase = ByteArray(32)
        SecureRandom().nextBytes(passphrase)

        // Store as ISO_8859_1 to preserve byte values through String roundtrip
        securePrefs.edit()
            .putString("db_passphrase", String(passphrase, Charsets.ISO_8859_1))
            .apply()

        return passphrase
    }

    fun writeEncryptedFile(file: File, data: ByteArray) {
        if (file.exists()) file.delete()
        val encryptedFile = EncryptedFile.Builder(
            context,
            file,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        // Use buffered stream for large file writes — ~3x faster than unbuffered
        BufferedOutputStream(encryptedFile.openFileOutput(), BUFFER_SIZE).use { output ->
            output.write(data)
        }
    }

    fun readEncryptedFile(file: File): ByteArray? {
        if (!file.exists()) return null
        val encryptedFile = EncryptedFile.Builder(
            context,
            file,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        // Use buffered stream for large file reads
        return BufferedInputStream(encryptedFile.openFileInput(), BUFFER_SIZE).use { input ->
            input.readBytes()
        }
    }

    companion object {
        private const val BUFFER_SIZE = 8 * 1024 // 8KB buffer
    }
}
