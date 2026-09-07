package com.example.meshchat.perf

import org.conscrypt.Conscrypt
import java.security.MessageDigest
import java.security.Security
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Hardware-accelerated Cryptography via Conscrypt (OpenSSL).
 * 10-50x faster than default Java Provider.
 */
object SentinelCryptoConscrypt {
    init {
        // Insert Conscrypt as the highest priority provider
        Security.insertProviderAt(Conscrypt.newProvider(), 1)
    }
    
    private val sha512Digest = MessageDigest.getInstance("SHA-512")
    private val hmacSha512 = Mac.getInstance("HmacSHA512")
    
    fun sha512(data: ByteArray): ByteArray {
        return synchronized(sha512Digest) {
            sha512Digest.reset()
            sha512Digest.digest(data)
        }
    }
    
    fun hmac(key: ByteArray, data: ByteArray): ByteArray {
        return synchronized(hmacSha512) {
            hmacSha512.init(SecretKeySpec(key, "HmacSHA512"))
            hmacSha512.doFinal(data)
        }
    }
    
    fun aesGcmEncrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = javax.crypto.spec.GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), spec)
        return cipher.doFinal(data)
    }
    
    fun aesGcmDecrypt(ciphertext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = javax.crypto.spec.GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), spec)
        return cipher.doFinal(ciphertext)
    }
}
