package com.example.meshchat

import com.example.meshchat.data.BinaryPacketType
import com.example.meshchat.data.ConnectionState
import com.example.meshchat.data.SentinelBinaryPacket
import com.example.meshchat.data.WifiTcpTransport
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

class WifiTcpTransportTest {

    @Test
    fun testInitialTransportStateIsDisconnected() {
        val transport = WifiTcpTransport()
        assertEquals(ConnectionState.DISCONNECTED, transport.connectionState.value)
        assertEquals(-1L, transport.pingLatencyMs.value)
        assertEquals(0L, transport.packetsSent.value)
        assertEquals(0L, transport.packetsReceived.value)
        assertNull(transport.lastError.value)
    }

    @Test
    fun testReadSinglePacketFromInputStream() {
        val transport = WifiTcpTransport()
        val originalPacket = SentinelBinaryPacket(
            type = BinaryPacketType.MESSAGE,
            senderId = 0x1122.toShort(),
            receiverId = 0x3344.toShort(),
            sequence = 7.toByte(),
            flags = 0x00.toByte(),
            payload = "Network Stream Test".toByteArray(Charsets.UTF_8)
        )
        val encodedBytes = originalPacket.encode()

        val inStream = ByteArrayInputStream(encodedBytes)
        val parsedPacket = transport.readSinglePacket(inStream)

        assertNotNull(parsedPacket)
        assertEquals(originalPacket.type, parsedPacket?.type)
        assertEquals(originalPacket.senderId, parsedPacket?.senderId)
        assertEquals(originalPacket.receiverId, parsedPacket?.receiverId)
        assertEquals(originalPacket.sequence, parsedPacket?.sequence)
        assertArrayEquals(originalPacket.payload, parsedPacket?.payload)
    }

    @Test
    fun testReadSinglePacketWithPrefixJunkBytes() {
        val transport = WifiTcpTransport()
        val originalPacket = SentinelBinaryPacket(
            type = BinaryPacketType.PING,
            senderId = 0x00AA.toShort(),
            receiverId = 0x00E1.toShort(),
            sequence = 1.toByte(),
            payload = byteArrayOf(0x01)
        )
        val packetBytes = originalPacket.encode()

        // Prefix stream with random noise bytes before magic 0xA5
        val noisyStreamBytes = byteArrayOf(0x12, 0x34, 0x56, 0x78) + packetBytes
        val inStream = ByteArrayInputStream(noisyStreamBytes)

        val parsedPacket = transport.readSinglePacket(inStream)
        assertNotNull("Should sync onto 0xA5 and successfully parse", parsedPacket)
        assertEquals(BinaryPacketType.PING, parsedPacket?.type)
    }

    @Test
    fun testReadSinglePacketRejectsCorruptedCrc() {
        val transport = WifiTcpTransport()
        val packet = SentinelBinaryPacket(
            type = BinaryPacketType.MESSAGE,
            senderId = 1,
            receiverId = 2,
            sequence = 1,
            payload = "CRC Test".toByteArray()
        )
        val corruptedBytes = packet.encode()
        corruptedBytes[corruptedBytes.size - 1] = (corruptedBytes[corruptedBytes.size - 1].toInt() xor 0xFF).toByte()

        val inStream = ByteArrayInputStream(corruptedBytes)
        val parsed = transport.readSinglePacket(inStream)
        assertNull("Corrupted packet must be rejected", parsed)
    }
}
