package com.example.meshchat

import com.example.meshchat.protocol.QsConstants
import com.example.meshchat.protocol.QsMessage
import com.example.meshchat.protocol.QsPacketCodec
import org.junit.Assert.*
import org.junit.Test

class QsPacketCodecTest {

    @Test
    fun testEncodeDecodeRoundtrip() {
        val msg = QsMessage(
            securityLevel = 1,
            senderId = 123,
            receiverId = 456,
            messageType = 1,
            sequenceNumber = 100,
            nonce = ByteArray(12) { it.toByte() },
            payload = "Hello".toByteArray()
        )
        val encoded = QsPacketCodec.encode(msg)
        val decoded = QsPacketCodec.decode(encoded).getOrThrow()
        
        assertEquals(msg, decoded)
    }
    
    @Test
    fun testEncodeDecodeRoundtripLevel3() {
        val msg = QsMessage(
            securityLevel = 3,
            senderId = 123,
            receiverId = 456,
            messageType = 1,
            sequenceNumber = 105,
            nonce = ByteArray(12) { it.toByte() },
            payload = "Hello Security".toByteArray(),
            signature = ByteArray(QsConstants.ML_DSA_SIG_SIZE) { 1 } // non-zero for valid mock signature
        )
        val encoded = QsPacketCodec.encode(msg)
        val decoded = QsPacketCodec.decode(encoded).getOrThrow()
        
        assertEquals(msg, decoded)
    }

    @Test
    fun testCrcValidation() {
        val msg = QsMessage(
            securityLevel = 1,
            senderId = 123,
            receiverId = 456,
            messageType = 1,
            sequenceNumber = 101,
            nonce = ByteArray(12),
            payload = "Hello".toByteArray()
        )
        val encoded = QsPacketCodec.encode(msg)
        encoded[encoded.size - 1] = (encoded[encoded.size - 1] + 1).toByte() // Corrupt CRC
        
        val result = QsPacketCodec.decode(encoded)
        assertTrue(result.isFailure)
        assertEquals("CRC mismatch", result.exceptionOrNull()?.message)
    }

    @Test
    fun testDuplicateSequenceRejection() {
        val msg = QsMessage(
            securityLevel = 1,
            senderId = 123,
            receiverId = 456,
            messageType = 1,
            sequenceNumber = 102,
            nonce = ByteArray(12),
            payload = "Hello".toByteArray()
        )
        val encoded = QsPacketCodec.encode(msg)
        
        val res1 = QsPacketCodec.decode(encoded)
        assertTrue(res1.isSuccess)
        
        val res2 = QsPacketCodec.decode(encoded)
        assertTrue(res2.isFailure)
        assertEquals("Duplicate sequence number", res2.exceptionOrNull()?.message)
    }

    @Test
    fun testOversizedPacketRejection() {
        val data = ByteArray(5000)
        val result = QsPacketCodec.decode(data)
        assertTrue(result.isFailure)
        assertEquals("Packet oversized", result.exceptionOrNull()?.message)
    }

    @Test
    fun testMalformedMagicByteRejection() {
        val msg = QsMessage(
            securityLevel = 1,
            senderId = 123,
            receiverId = 456,
            messageType = 1,
            sequenceNumber = 103,
            nonce = ByteArray(12),
            payload = "Hello".toByteArray()
        )
        val encoded = QsPacketCodec.encode(msg)
        encoded[0] = 0x00 // Corrupt magic byte
        
        val result = QsPacketCodec.decode(encoded)
        assertTrue(result.isFailure)
        assertEquals("Invalid magic bytes", result.exceptionOrNull()?.message)
    }

    @Test
    fun testEmptyPayloadRejection() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            QsMessage(
                securityLevel = 1,
                senderId = 123,
                receiverId = 456,
                messageType = 1,
                sequenceNumber = 104,
                nonce = ByteArray(12),
                payload = ByteArray(0)
            )
        }
        assertEquals("Payload cannot be empty", ex.message)
    }
}
