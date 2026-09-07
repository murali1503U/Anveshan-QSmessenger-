package com.example.meshchat

import com.example.meshchat.data.BleConstants
import com.example.meshchat.data.Message
import com.example.meshchat.data.SecureSession
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class MediaAndSecurityTest {

    @Test
    fun testBleConstants() {
        assertEquals("6E400001-B5A3-F393-E0A9-E50E24DCCA9E", BleConstants.SERVICE_UUID.toString().uppercase())
        assertEquals("6E400003-B5A3-F393-E0A9-E50E24DCCA9E", BleConstants.TX_CHARACTERISTIC_UUID.toString().uppercase())
        assertEquals("6E400002-B5A3-F393-E0A9-E50E24DCCA9E", BleConstants.RX_CHARACTERISTIC_UUID.toString().uppercase())
        assertEquals(128, BleConstants.BLE_MTU_SIZE)
    }

    @Test
    fun testBase64MediaEncodingAndDecoding() {
        // Sample image byte array simulation (e.g. compressed JPEG magic bytes + payload)
        val dummyImageBytes = byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(),
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01, 0x01, 0x00,
            0x48, 0x45, 0x4C, 0x4C, 0x4F, 0x5F, 0x4C, 0x4F, 0x52, 0x41
        )

        // Encode to Base64 (Standard JVM Base64 for unit testing)
        val encodedBase64 = Base64.getEncoder().encodeToString(dummyImageBytes)
        assertNotNull(encodedBase64)
        assertTrue(encodedBase64.isNotEmpty())

        // Decode back from Base64
        val decodedBytes = Base64.getDecoder().decode(encodedBase64)
        assertArrayEquals(dummyImageBytes, decodedBytes)

        // Create Message with Base64 media
        val msg = Message(
            id = 1,
            text = "[Image Attachment]",
            sender = "Me",
            isSent = true,
            isEncrypted = true,
            mediaBase64 = encodedBase64,
            securityLevel = 2
        )

        assertEquals("[Image Attachment]", msg.text)
        assertEquals(encodedBase64, msg.mediaBase64)
        assertFalse(msg.isMediaCorrupted)
        assertTrue(msg.isEncrypted)
        assertEquals(2, msg.securityLevel)
    }

    @Test
    fun testMessageStateCreation() {
        val textMsg = Message(
            id = 10,
            text = "Coordinates: 37.7749,-122.4194",
            sender = "Peer_Node_A",
            isSent = false,
            isEncrypted = true,
            isConfidential = true,
            mediaBase64 = null,
            securityLevel = 3
        )

        assertEquals("Peer_Node_A", textMsg.sender)
        assertFalse(textMsg.isSent)
        assertTrue(textMsg.isConfidential)
        assertTrue(textMsg.isEncrypted)
        assertEquals(3, textMsg.securityLevel)
        assertNull(textMsg.mediaBase64)
    }

    @Test
    fun testSecureSessionDefaultsAndTwoOptionConfig() {
        val session = SecureSession(
            sessionId = "primary_session",
            isQsvmReady = true,
            selectedModelName = "QSVM Quantum Kernel",
            securityEngineMode = "QSVM",
            isDeveloperMode = false,
            isSetupCompleted = true
        )

        assertEquals("primary_session", session.sessionId)
        assertEquals("QSVM Quantum Kernel", session.selectedModelName)
        assertEquals("QSVM", session.securityEngineMode)
        assertFalse(session.isDeveloperMode)
        assertTrue(session.isSetupCompleted)

        // Switch to Cloud or Custom Security Engine
        val customSession = session.copy(
            selectedModelName = "Cloud Gemini AI",
            securityEngineMode = "CLOUD",
            isDeveloperMode = true
        )

        assertEquals("Cloud Gemini AI", customSession.selectedModelName)
        assertEquals("CLOUD", customSession.securityEngineMode)
        assertTrue(customSession.isDeveloperMode)
    }

    @Test
    fun testClassifyHiAndSensitiveMessages() {
        val qsvm = com.example.meshchat.ai.QsvmClassifier()
        val hiResult = qsvm.classify("hi")
        assertEquals(1, hiResult.decision.level)
        assertEquals("Level 1: Fast (Efficiency Mode)", hiResult.decision.levelName)

        val passResult = qsvm.classify("My secret password is AdminKey123")
        assertTrue(passResult.decision.level >= 2)
    }
}
