package com.example.meshchat.crypto

import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.Collections
import java.util.LinkedHashMap
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object AesGcmCipher {

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val NONCE_SIZE = 12
    private const val TAG_SIZE_BITS = 128
    private const val MAX_NONCE_CACHE = 1000
    
    // LRU Cache for tracking nonces per session to reject reuse
    private val usedNonces = Collections.synchronizedSet(
        Collections.newSetFromMap(
            object : LinkedHashMap<ByteBuffer, Boolean>(MAX_NONCE_CACHE, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<ByteBuffer, Boolean>?): Boolean {
                    return size > MAX_NONCE_CACHE
                }
            }
        )
    )

    /**
     * Encrypts plaintext using AES-GCM.
     *
     * @param plaintext The data to encrypt.
     * @param key The 32-byte session key.
     * @return A ByteArray containing nonce (12 bytes) followed by ciphertext and auth tag.
     */
    fun encrypt(plaintext: ByteArray, key: ByteArray): ByteArray {
        val nonce = ByteArray(NONCE_SIZE)
        SecureRandom().nextBytes(nonce)

        val cipher = Cipher.getInstance(ALGORITHM)
        val secretKey = SecretKeySpec(key, "AES")
        val parameterSpec = GCMParameterSpec(TAG_SIZE_BITS, nonce)

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)
        val ciphertextWithTag = cipher.doFinal(plaintext)

        // Result: nonce + ciphertext (which includes auth tag at the end)
        val result = ByteArray(nonce.size + ciphertextWithTag.size)
        System.arraycopy(nonce, 0, result, 0, nonce.size)
        System.arraycopy(ciphertextWithTag, 0, result, nonce.size, ciphertextWithTag.size)

        return result
    }

    /**
     * Decrypts ciphertext using AES-GCM.
     *
     * @param nonce The 12-byte nonce used for encryption.
     * @param ciphertext The encrypted data including the auth tag.
     * @param key The 32-byte session key.
     * @return The decrypted plaintext.
     */
    fun decrypt(nonce: ByteArray, ciphertext: ByteArray, key: ByteArray): ByteArray {
        val nonceBuffer = ByteBuffer.wrap(nonce)
        
        // Reject if nonce is reused
        if (!usedNonces.add(nonceBuffer)) {
            throw IllegalArgumentException("Nonce reuse detected")
        }

        val cipher = Cipher.getInstance(ALGORITHM)
        val secretKey = SecretKeySpec(key, "AES")
        val parameterSpec = GCMParameterSpec(TAG_SIZE_BITS, nonce)

        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)
        
        // This will throw AEADBadTagException if auth tag verification fails
        return cipher.doFinal(ciphertext)
    }
}
