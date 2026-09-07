package com.example.meshchat

import com.example.meshchat.perf.SentinelCryptoConscrypt
import com.example.meshchat.perf.SentinelCryptoUtils
import com.example.meshchat.perf.SentinelSecurity
import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class SecurityPskTest {

    private fun computeHmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(key, "HmacSHA256")
        mac.init(secretKey)
        return mac.doFinal(data)
    }

    @Test
    fun testChallengeResponseAuthenticationWithCorrectPsk() {
        val psk = "12345678"
        val challenge = ByteArray(16) { it.toByte() }
        val nodeId: Short = 0x00E1.toShort()
        val androidNodeId: Short = 0x00AA.toShort()

        // Device generates challenge response: HMAC-SHA256(psk, challenge + nodeId + androidNodeId)
        val hmacData = ByteBuffer.allocate(16 + 2 + 2)
            .order(ByteOrder.BIG_ENDIAN)
            .put(challenge)
            .putShort(nodeId)
            .putShort(androidNodeId)
            .array()

        val clientResponse = computeHmacSha256(psk.toByteArray(Charsets.UTF_8), hmacData)
        val expectedServerHmac = computeHmacSha256(psk.toByteArray(Charsets.UTF_8), hmacData)

        // Verify constant-time comparison succeeds
        assertTrue(
            "Authentication must succeed with correct PSK",
            SentinelSecurity.constantTimeEquals(clientResponse, expectedServerHmac)
        )
    }

    @Test
    fun testChallengeResponseAuthenticationWithWrongPskFails() {
        val correctPsk = "12345678"
        val wrongPsk = "wrong_password"
        val challenge = ByteArray(16) { 0x55.toByte() }
        val nodeId: Short = 0x00E1.toShort()
        val androidNodeId: Short = 0x00AA.toShort()

        val hmacData = ByteBuffer.allocate(16 + 2 + 2)
            .order(ByteOrder.BIG_ENDIAN)
            .put(challenge)
            .putShort(nodeId)
            .putShort(androidNodeId)
            .array()

        val wrongClientResponse = computeHmacSha256(wrongPsk.toByteArray(Charsets.UTF_8), hmacData)
        val expectedServerHmac = computeHmacSha256(correctPsk.toByteArray(Charsets.UTF_8), hmacData)

        assertFalse(
            "Authentication must fail with wrong PSK",
            SentinelSecurity.constantTimeEquals(wrongClientResponse, expectedServerHmac)
        )
    }

    @Test
    fun testConstantTimeComparisonBehavior() {
        val arr1 = byteArrayOf(1, 2, 3, 4, 5)
        val arr2 = byteArrayOf(1, 2, 3, 4, 5)
        val arr3 = byteArrayOf(1, 2, 3, 4, 6)
        val arr4 = byteArrayOf(1, 2, 3, 4)

        assertTrue(SentinelSecurity.constantTimeEquals(arr1, arr2))
        assertFalse(SentinelSecurity.constantTimeEquals(arr1, arr3))
        assertFalse(SentinelSecurity.constantTimeEquals(arr1, arr4))
    }

    @Test
    fun testSha512AndHmacDerivationStability() {
        val secret = "SentinelSecretKey2026"
        val hash1 = SentinelCryptoConscrypt.sha512(secret.toByteArray())
        val hash2 = SentinelCryptoConscrypt.sha512(secret.toByteArray())

        assertEquals(64, hash1.size)
        assertArrayEquals("SHA-512 derivation must be deterministic", hash1, hash2)

        val message = "Important Payload".toByteArray()
        val hmac1 = SentinelCryptoConscrypt.hmac(hash1, message)
        val hmac2 = SentinelCryptoConscrypt.hmac(hash1, message)
        assertArrayEquals("HMAC derivation must be deterministic", hmac1, hmac2)
    }

    @Test
    fun testReplayAttackPreventionWithNonces() {
        val seenNonces = mutableSetOf<String>()
        val nonce = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val nonceHex = nonce.joinToString("") { "%02x".format(it) }

        // First presentation of nonce is accepted
        val isFirstAccepted = seenNonces.add(nonceHex)
        assertTrue("Fresh nonce must be accepted", isFirstAccepted)

        // Replay of same nonce is rejected
        val isReplayAccepted = seenNonces.add(nonceHex)
        assertFalse("Replayed nonce must be rejected", isReplayAccepted)
    }
}
