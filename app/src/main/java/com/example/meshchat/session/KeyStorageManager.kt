package com.example.meshchat.session

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest

interface KeyStorageManager {
    fun saveMlKemPrivateKey(key: ByteArray)
    fun getMlKemPrivateKey(): ByteArray?
    fun saveMlDsaPrivateKey(key: ByteArray)
    fun getMlDsaPrivateKey(): ByteArray?
    fun saveSessionKey(sessionId: String, key: ByteArray)
    fun getSessionKey(sessionId: String): ByteArray?
    fun savePskHash(hash: ByteArray)
    fun getPskHash(): ByteArray?
    fun getKeyFingerprint(key: ByteArray): String
}

class KeyStorageManagerImpl(context: Context) : KeyStorageManager {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_key_store",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun saveMlKemPrivateKey(key: ByteArray) {
        saveBytes("ml_kem_private_key", key)
    }

    override fun getMlKemPrivateKey(): ByteArray? {
        return getBytes("ml_kem_private_key")
    }

    override fun saveMlDsaPrivateKey(key: ByteArray) {
        saveBytes("ml_dsa_private_key", key)
    }

    override fun getMlDsaPrivateKey(): ByteArray? {
        return getBytes("ml_dsa_private_key")
    }

    override fun saveSessionKey(sessionId: String, key: ByteArray) {
        saveBytes("session_key_$sessionId", key)
    }

    override fun getSessionKey(sessionId: String): ByteArray? {
        return getBytes("session_key_$sessionId")
    }

    override fun savePskHash(hash: ByteArray) {
        saveBytes("psk_hash", hash)
    }

    override fun getPskHash(): ByteArray? {
        return getBytes("psk_hash")
    }

    private fun saveBytes(key: String, data: ByteArray) {
        val encoded = Base64.encodeToString(data, Base64.NO_WRAP)
        sharedPreferences.edit().putString(key, encoded).apply()
    }

    private fun getBytes(key: String): ByteArray? {
        val encoded = sharedPreferences.getString(key, null)
        return encoded?.let { Base64.decode(it, Base64.NO_WRAP) }
    }

    override fun getKeyFingerprint(key: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-512")
        val hash = digest.digest(key)
        val fingerprintBytes = hash.copyOfRange(0, 8)
        return fingerprintBytes.joinToString(":") { "%02X".format(it) }
    }
}
