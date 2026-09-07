package com.example.meshchat

import com.example.meshchat.ai.QsvmClassifier
import com.example.meshchat.ai.QsvmSecurityEngine
import com.example.meshchat.data.BinaryPacketType
import com.example.meshchat.data.BitChatMeshEngine
import com.example.meshchat.data.NodeBoardType
import com.example.meshchat.data.SentinelBinaryPacket
import com.example.meshchat.perf.SentinelCryptoConscrypt
import com.example.meshchat.perf.SentinelCryptoUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import kotlin.math.abs

/**
 * Comprehensive Test Suite for QSVM (Quantum Support Vector Machine)
 * and Cryptographic Engines (Conscrypt AES-256-GCM, HMAC, Ratchets, Binary Codecs).
 */
class QsvmAndCryptoComprehensiveTest {

    private val qsvm = QsvmClassifier()

    // =========================================================================
    // SECTION 1: QSVM QUANTUM MATHEMATICS & HILBERT SPACE PROPERTIES
    // =========================================================================

    @Test
    fun testQuantumStateNormalization() {
        val features = qsvm.extractFeatures("Critical test payload with lat: 12.9716 lon: 77.5946")
        val state = qsvm.computeQuantumState(features)

        assertEquals(64, state.size) // 2^6 = 64 basis states in Hilbert space

        var totalProbability = 0.0
        for (c in state) {
            totalProbability += c.absSquared()
        }

        // Sum of probabilities in a valid quantum state must equal 1.0 (within floating point precision)
        assertTrue(
            "Total probability in Hilbert space must equal 1.0 (was $totalProbability)",
            abs(totalProbability - 1.0) < 1e-4
        )
    }

    @Test
    fun testQuantumKernelSelfFidelity() {
        val features = qsvm.extractFeatures("Meet at coordinates 37.7749, -122.4194 with access token 0x9AF83E12B")
        val state1 = qsvm.computeQuantumState(features)
        val state2 = qsvm.computeQuantumState(features)

        val fidelity = qsvm.computeQuantumKernel(state1, state2)
        assertTrue(
            "Self-fidelity K_Q(x, x) must be ~1.0 (was $fidelity)",
            abs(fidelity - 1.0f) < 1e-3f
        )
    }

    @Test
    fun testQuantumKernelSymmetry() {
        val featA = qsvm.extractFeatures("Hey, how is everything going today?")
        val featB = qsvm.extractFeatures("Initiate Kyber-768 lattice post-quantum encryption master key")

        val stateA = qsvm.computeQuantumState(featA)
        val stateB = qsvm.computeQuantumState(featB)

        val fidelityAB = qsvm.computeQuantumKernel(stateA, stateB)
        val fidelityBA = qsvm.computeQuantumKernel(stateB, stateA)

        assertEquals("Quantum kernel must be Hermitian symmetric K_Q(A, B) == K_Q(B, A)", fidelityAB, fidelityBA, 1e-4f)
    }

    // =========================================================================
    // SECTION 2: 4-TIER SECURITY CLASSIFICATION & ADVERSARIAL RESILIENCE
    // =========================================================================

    @Test
    fun testTier1_FastLightweight() {
        val benignMessages = listOf(
            "Hello there!",
            "Good morning team, let's start the standup",
            "What time are we meeting today?",
            "The weather in the mountains looks great",
            "I watched an interesting movie about quantum physics yesterday" // Science context check
        )

        for (msg in benignMessages) {
            val result = qsvm.classify(msg)
            assertEquals("Message '$msg' should be classified as Tier 1", 1, result.decision.level)
            assertEquals(0, result.decision.estimatedPacketOverheadBytes)
        }
    }

    @Test
    fun testTier2_StandardProtection() {
        val piiMessages = listOf(
            "Please email me at john.doe@sentinel-mesh.org",
            "My direct contact number is +1-555-0199-432",
            "This is confidential project documentation, please keep internal"
        )

        for (msg in piiMessages) {
            val result = qsvm.classify(msg)
            assertTrue("Message '$msg' should be classified as Tier >= 2", result.decision.level >= 2)
            assertTrue(result.decision.estimatedPacketOverheadBytes >= 28)
        }
    }

    @Test
    fun testTier3_EnhancedProtection_CredentialsAndGps() {
        val credentialMessages = listOf(
            "Your temporary OTP is 839201 for system login",
            "Node access token: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9",
            "Meeting coordinates: 12.9716, 77.5946",
            "Rendezvous at 48°51'24\"N 2°21'07\"E",
            "Device privatekey hash: 0x9AFB3C2D1E0F4A5B"
        )

        for (msg in credentialMessages) {
            val result = qsvm.classify(msg)
            assertTrue("Message '$msg' should be classified as Tier >= 3 (was ${result.decision.level})", result.decision.level >= 3)
            assertEquals(44, result.decision.estimatedPacketOverheadBytes)
        }
    }

    @Test
    fun testTier4_MaximumProtection_PostQuantumLattice() {
        val criticalMessages = listOf(
            "Initiate kyber lattice exchange with root masterkey",
            "TOP SECRET: defenseclearance granted, cipheroverride active",
            "Deploy kyber masterkey to rendezvous lat: 48.8566 lon: 2.3522 immediately"
        )

        for (msg in criticalMessages) {
            val result = qsvm.classify(msg)
            assertEquals("Message '$msg' should be classified as Tier 4", 4, result.decision.level)
            assertEquals(128, result.decision.estimatedPacketOverheadBytes)
        }
    }

    @Test
    fun testAdversarialEvasions_LeetspeakAndZeroWidthSpaces() {
        // Obfuscated password with leetspeak: "p@ssw0rd"
        val leet = qsvm.classify("My new p@ssw0rd is 123456")
        assertTrue("Leetspeak password must trigger Tier >= 3", leet.decision.level >= 3)

        // Obfuscated secret with zero-width spaces (\u200B)
        val zwsSecret = "s\u200Be\u200Bc\u200Br\u200Be\u200Bt token: 0xDEADBEEF"
        val zwsResult = qsvm.classify(zwsSecret)
        assertTrue("Zero-width space obfuscation must trigger Tier >= 3", zwsResult.decision.level >= 3)
    }

    @Test
    fun testBatchClassification() {
        val batch = listOf(
            "Hi",
            "Send to alice@test.com",
            "Coordinates 37.77, -122.41",
            "Kyber masterkey override"
        )
        val results = qsvm.classifyBatch(batch)
        assertEquals(4, results.size)
        assertEquals(1, results[0].decision.level)
        assertTrue(results[1].decision.level >= 2)
        assertTrue(results[2].decision.level >= 3)
        assertEquals(4, results[3].decision.level)
    }

    // =========================================================================
    // SECTION 3: HARDWARE CRYPTOGRAPHY (CONSCRYPT AES-256-GCM & HMAC)
    // =========================================================================

    @Test
    fun testAes256GcmRoundTripEncryptionAndDecryption() {
        val key = ByteArray(32) // 256-bit key
        val iv = ByteArray(12)  // 96-bit IV standard for GCM
        val random = SecureRandom()
        random.nextBytes(key)
        random.nextBytes(iv)

        val plaintext = "Sentinel mesh military-grade off-grid payload 2026".toByteArray(StandardCharsets.UTF_8)

        // Encrypt with Conscrypt AES-256-GCM
        val ciphertextWithTag = SentinelCryptoConscrypt.aesGcmEncrypt(plaintext, key, iv)
        assertTrue("Ciphertext must be longer than plaintext due to 16-byte GCM tag", ciphertextWithTag.size == plaintext.size + 16)

        // Decrypt
        val decrypted = SentinelCryptoConscrypt.aesGcmDecrypt(ciphertextWithTag, key, iv)
        assertArrayEquals("Decrypted bytes must match original plaintext exactly", plaintext, decrypted)
        assertEquals("Sentinel mesh military-grade off-grid payload 2026", String(decrypted, StandardCharsets.UTF_8))
    }

    @Test
    fun testAes256GcmTamperResistance() {
        val key = ByteArray(32)
        val iv = ByteArray(12)
        SecureRandom().nextBytes(key)
        SecureRandom().nextBytes(iv)

        val plaintext = "Sensitive data payload".toByteArray(StandardCharsets.UTF_8)
        val ciphertextWithTag = SentinelCryptoConscrypt.aesGcmEncrypt(plaintext, key, iv)

        // Tamper with 1 byte of the ciphertext
        val tamperedCiphertext = ciphertextWithTag.clone()
        tamperedCiphertext[0] = (tamperedCiphertext[0].toInt() xor 0xFF).toByte()

        try {
            SentinelCryptoConscrypt.aesGcmDecrypt(tamperedCiphertext, key, iv)
            fail("Decryption of tampered ciphertext must throw AEAD / GCM tag validation failure")
        } catch (e: Exception) {
            // Expected: AEADBadTagException or GeneralSecurityException
            assertTrue(e is AEADBadTagException || e.javaClass.simpleName.contains("Tag") || e is javax.crypto.BadPaddingException)
        }
    }

    @Test
    fun testHmacSha512AndSha512Integrity() {
        val key = "SentinelSecretKey_2026".toByteArray(StandardCharsets.UTF_8)
        val data = "Packet payload for integrity verification".toByteArray(StandardCharsets.UTF_8)

        val hmac1 = SentinelCryptoConscrypt.hmac(key, data)
        val hmac2 = SentinelCryptoConscrypt.hmac(key, data)

        assertEquals(64, hmac1.size) // 512-bit HMAC = 64 bytes
        assertArrayEquals("HMAC output must be deterministic", hmac1, hmac2)

        // Modifying data must completely change HMAC
        val modifiedData = "Packet payload for integrity verification!".toByteArray(StandardCharsets.UTF_8)
        val hmacModified = SentinelCryptoConscrypt.hmac(key, modifiedData)
        assertFalse("Different data must produce different HMAC", hmac1.contentEquals(hmacModified))
    }

    @Test
    fun testConstantTimeComparison() {
        val arr1 = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)
        val arr2 = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)
        val arr3 = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x06)

        assertTrue(SentinelCryptoUtils.constantTimeEquals(arr1, arr2))
        assertFalse(SentinelCryptoUtils.constantTimeEquals(arr1, arr3))
        assertFalse(SentinelCryptoUtils.constantTimeEquals(arr1, byteArrayOf(0x01)))
    }

    // =========================================================================
    // SECTION 4: SENTINEL BINARY PACKET CODEC & AUTHENTICATION HANDSHAKE
    // =========================================================================

    @Test
    fun testSentinelBinaryCodecFrameAndCRC16() {
        val payload = "HELLO ESP32 MESH NODE".toByteArray(StandardCharsets.UTF_8)
        val packet = SentinelBinaryPacket(
            type = BinaryPacketType.MESSAGE,
            senderId = 0x1234.toShort(),
            receiverId = 0x5678.toShort(),
            sequence = 1.toByte(),
            flags = 0.toByte(),
            payload = payload
        )

        val encoded = packet.encode()
        assertEquals(0xA5.toByte(), encoded[0]) // Magic byte
        assertEquals(0x01.toByte(), encoded[1]) // Version byte

        val decoded = SentinelBinaryPacket.decode(encoded)
        assertNotNull("Packet frame must parse successfully", decoded)
        assertEquals(BinaryPacketType.MESSAGE, decoded!!.type)
        assertEquals(0x1234.toShort(), decoded.senderId)
        assertEquals(0x5678.toShort(), decoded.receiverId)
        assertArrayEquals(payload, decoded.payload)
    }

    @Test
    fun testSentinelBinaryCodecCorruptedCRCRejection() {
        val payload = "TEST MESSAGE".toByteArray(StandardCharsets.UTF_8)
        val packet = SentinelBinaryPacket(
            type = BinaryPacketType.MESSAGE,
            senderId = 0x1111.toShort(),
            receiverId = 0x2222.toShort(),
            sequence = 1.toByte(),
            flags = 0.toByte(),
            payload = payload
        )

        val encoded = packet.encode()

        // Corrupt 1 byte in payload
        val corruptedFrame = encoded.clone()
        corruptedFrame[12] = (corruptedFrame[12].toInt() xor 0xFF).toByte()

        val decoded = SentinelBinaryPacket.decode(corruptedFrame)
        assertTrue("Corrupted CRC frame must be rejected and return null", decoded == null)
    }

    @Test
    fun testSentinelHmacSha256ChallengeResponseHandshake() {
        val psk = "12345678"
        val nonce = ByteArray(16)
        SecureRandom().nextBytes(nonce)

        val nodeId: Short = 0x33C4.toShort()
        val hmacData = java.nio.ByteBuffer.allocate(18)
            .putShort(nodeId)
            .put(nonce)
            .array()

        // Android computes response to ESP challenge nonce
        val response = SentinelCryptoConscrypt.hmacSha256(psk.toByteArray(StandardCharsets.UTF_8), hmacData)
        assertEquals(32, response.size) // SHA-256 HMAC is 32 bytes

        // Recomputing with same PSK produces identical response
        val expectedResponse = SentinelCryptoConscrypt.hmacSha256(psk.toByteArray(StandardCharsets.UTF_8), hmacData)
        assertArrayEquals(expectedResponse, response)

        // Wrong PSK produces rejected response
        val wrongPskResponse = SentinelCryptoConscrypt.hmacSha256("wrong_psk".toByteArray(StandardCharsets.UTF_8), hmacData)
        assertFalse(wrongPskResponse.contentEquals(response))
    }

    @Test
    fun testBitChatMeshEngineSecurityLevels() {
        val msg = "Target rendezvous point alpha"
        val p1 = BitChatMeshEngine.packMessage(msg, "NODE-A", "NODE-B", 1)
        assertEquals(1, p1.securityLevel)
        assertEquals("NONE (Level 1 Bypass)", p1.authTagHex)
        assertEquals(92, p1.workSavedPercent)

        val p2 = BitChatMeshEngine.packMessage(msg, "NODE-A", "NODE-B", 2)
        assertEquals(2, p2.securityLevel)
        assertNotEquals("NONE (Level 1 Bypass)", p2.authTagHex)

        val p3 = BitChatMeshEngine.packMessage(msg, "NODE-A", "NODE-B", 3)
        assertEquals(3, p3.securityLevel)

        val p4 = BitChatMeshEngine.packMessage(msg, "NODE-A", "NODE-B", 4)
        assertEquals(4, p4.securityLevel)
        assertTrue(p4.cipherBytesHex.startsWith("KYBER768_"))
    }
}
