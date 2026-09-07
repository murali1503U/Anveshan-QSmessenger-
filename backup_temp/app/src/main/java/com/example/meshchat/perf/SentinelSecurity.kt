package com.example.meshchat.perf

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

object SentinelSecurity {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    val secureRandom = SecureRandom()

    // Retrieve or generate secure key bounded to hardware Keystore
    fun getOrCreateKey(alias: String): SecretKey {
        if (!keyStore.containsAlias(alias)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            keyGenerator.init(
                KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            keyGenerator.generateKey()
        }
        return keyStore.getKey(alias, null) as SecretKey
    }

    // Encrypted Shared Preferences wrapper
    fun getSecurePrefs(context: Context) = EncryptedSharedPreferences.create(
        "secure_prefs",
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // Constant-time array comparison to prevent side-channel timing attacks
    fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].toInt() xor b[i].toInt())
        }
        return result == 0
    }
    
    // Strict input sanitizer preventing injection patterns
    fun sanitizeInput(input: String): String = input.replace(Regex("[<>\"'&]"), "")
    
    fun validateMessage(message: String): Boolean = message.length in 1..4096 && !message.contains(Regex("[<>]"))
}
