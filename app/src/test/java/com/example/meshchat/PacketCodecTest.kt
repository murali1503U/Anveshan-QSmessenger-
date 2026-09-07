package com.example.meshchat

import com.example.meshchat.data.BinaryDecodeResult
import com.example.meshchat.data.BinaryPacketType
import com.example.meshchat.data.PacketDeduplicator
import com.example.meshchat.data.SentinelBinaryPacket
import org.junit.Assert.*
import org.junit.Test

class PacketCodecTest {

    @Test
    fun testEncodeDecodeRoundtrip() {
        val payload = "Hello Sentinel Mesh Radio!".toByteArray(Charsets.UTF_8)
        val packet = SentinelBinaryPacket(
            type = BinaryPacketType.MESSAGE,
            senderId = 0x1234.toShort(),
            receiverId = 0x5678.toShort(),
            sequence = 42.toByte(),
            flags = 0x01.toByte(),
            payload = payload
        )

        val encoded = packet.encode()
        assertNotNull(encoded)
        assertTrue(encoded.size >= SentinelBinaryPacket.MIN_PACKET_SIZE)

        val decodedResult = SentinelBinaryPacket.decodeSafe(encoded)
        assertTrue("Decode must succeed", decodedResult is BinaryDecodeResult.Success)

        val decoded = (decodedResult as BinaryDecodeResult.Success).packet
        assertEquals(BinaryPacketType.MESSAGE, decoded.type)
        assertEquals(0x1234.toShort(), decoded.senderId)
        assertEquals(0x5678.toShort(), decoded.receiverId)
        assertEquals(42.toByte(), decoded.sequence)
        assertEquals(0x01.toByte(), decoded.flags)
        assertArrayEquals(payload, decoded.payload)
    }

    @Test
    fun testCorruptedCrcIsRejected() {
        val payload = "Data to protect".toByteArray(Charsets.UTF_8)
        val packet = SentinelBinaryPacket(
            type = BinaryPacketType.MESSAGE,
            senderId = 0x0001,
            receiverId = 0x0002,
            sequence = 1,
            payload = payload
        )
        val encoded = packet.encode()

        // Tamper with last byte (CRC byte)
        encoded[encoded.size - 1] = (encoded[encoded.size - 1].toInt() xor 0xFF).toByte()

        val result = SentinelBinaryPacket.decodeSafe(encoded)
        assertTrue("Corrupted CRC must fail", result is BinaryDecodeResult.Error)
        assertEquals(
            BinaryDecodeResult.Error.ErrorType.CORRUPTED_CRC,
            (result as BinaryDecodeResult.Error).errorType
        )
        assertNull("Standard decode must return null", SentinelBinaryPacket.decode(encoded))
    }

    @Test
    fun testEmptyPacketIsRejected() {
        val emptyBytes = ByteArray(0)
        val result = SentinelBinaryPacket.decodeSafe(emptyBytes)
        assertTrue(result is BinaryDecodeResult.Error)
        assertEquals(BinaryDecodeResult.Error.ErrorType.EMPTY_PACKET, (result as BinaryDecodeResult.Error).errorType)
        assertNull(SentinelBinaryPacket.decode(emptyBytes))
    }

    @Test
    fun testOversizedPayloadIsRejected() {
        // Payload size > 255
        val oversizedPayload = ByteArray(256) { 0x41.toByte() }
        val packet = SentinelBinaryPacket(
            type = BinaryPacketType.MESSAGE,
            senderId = 1,
            receiverId = 2,
            sequence = 1,
            payload = oversizedPayload
        )

        val encodeResult = packet.encodeSafe()
        assertTrue("encodeSafe must reject payload > 255", encodeResult.isFailure)
    }

    @Test
    fun testMalformedHeaderDoesNotCrash() {
        val randomGarbage = byteArrayOf(0xA5.toByte(), 0x01, 0xFF.toByte(), 0x12, 0x34) // Truncated
        val result = SentinelBinaryPacket.decodeSafe(randomGarbage)
        assertTrue(result is BinaryDecodeResult.Error)
        assertNull(SentinelBinaryPacket.decode(randomGarbage))
    }

    @Test
    fun testInvalidMagicByteIsRejected() {
        val payload = "test".toByteArray()
        val packet = SentinelBinaryPacket(
            type = BinaryPacketType.PING,
            senderId = 1,
            receiverId = 2,
            sequence = 1,
            payload = payload
        )
        val encoded = packet.encode()
        encoded[0] = 0x00 // Change magic from 0xA5 to 0x00

        val result = SentinelBinaryPacket.decodeSafe(encoded)
        assertTrue(result is BinaryDecodeResult.Error)
        assertEquals(BinaryDecodeResult.Error.ErrorType.INVALID_MAGIC, (result as BinaryDecodeResult.Error).errorType)
    }

    @Test
    fun testDuplicatePacketRejectedByDeduplicator() {
        val deduplicator = PacketDeduplicator()
        val senderId: Short = 0x00AA.toShort()
        val seq: Byte = 15

        // First arrival -> Not duplicate
        assertFalse("First arrival must not be duplicate", deduplicator.isDuplicate(senderId, seq))

        // Second arrival with same sender and sequence -> Duplicate!
        assertTrue("Second arrival must be detected as duplicate", deduplicator.isDuplicate(senderId, seq))

        // Different sequence -> Not duplicate
        assertFalse("New sequence must not be duplicate", deduplicator.isDuplicate(senderId, 16.toByte()))
    }
}
