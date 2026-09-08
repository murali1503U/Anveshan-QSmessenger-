package com.example.meshchat

import com.example.meshchat.crypto.AesGcmCipher
import org.junit.Assert.*
import org.junit.Test
import java.security.SecureRandom
import javax.crypto.AEADBadTagException

class AesGcmCipherTest {

    @Test
    fun testEncryptionAndDecryption() {
        val key = ByteArray(32).apply { SecureRandom().nextBytes(this) }
        val plaintext = "Top Secret Message".toByteArray(Charsets.UTF_8)
        
        val encrypted = AesGcmCipher.encrypt(plaintext, key)
        val nonce = encrypted.copyOfRange(0, 12)
        val ciphertext = encrypted.copyOfRange(12, encrypted.size)
        
        val decrypted = AesGcmCipher.decrypt(nonce, ciphertext, key)
        assertArrayEquals("Decrypted text should match original", plaintext, decrypted)
    }

    @Test
    fun testNonceReuseRejected() {
        val key = ByteArray(32).apply { SecureRandom().nextBytes(this) }
        val plaintext = "Message".toByteArray(Charsets.UTF_8)
        
        val encrypted = AesGcmCipher.encrypt(plaintext, key)
        val nonce = encrypted.copyOfRange(0, 12)
        val ciphertext = encrypted.copyOfRange(12, encrypted.size)
        
        // First decryption should succeed
        AesGcmCipher.decrypt(nonce, ciphertext, key)
        
        // Second decryption with the same nonce should throw exception
        try {
            AesGcmCipher.decrypt(nonce, ciphertext, key)
            fail("Expected exception due to nonce reuse")
        } catch (e: Exception) {
            assertTrue(e is IllegalArgumentException)
            assertEquals("Nonce reuse detected", e.message)
        }
    }

    @Test
    fun testTamperingRejected() {
        val key = ByteArray(32).apply { SecureRandom().nextBytes(this) }
        val plaintext = "Another Message".toByteArray(Charsets.UTF_8)
        
        val encrypted = AesGcmCipher.encrypt(plaintext, key)
        val nonce = encrypted.copyOfRange(0, 12)
        val ciphertext = encrypted.copyOfRange(12, encrypted.size)
        
        // Tamper with the ciphertext (or auth tag)
        ciphertext[0] = (ciphertext[0].toInt() xor 0xFF).toByte()
        
        try {
            AesGcmCipher.decrypt(nonce, ciphertext, key)
            fail("Expected exception due to tampering")
        } catch (e: Exception) {
            assertTrue("Expected AEADBadTagException or similar", e is AEADBadTagException || e.cause is AEADBadTagException || e is javax.crypto.BadPaddingException)
        }
    }
}
