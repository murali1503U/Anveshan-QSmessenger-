package com.example.meshchat.data

import com.example.meshchat.perf.SentinelLUT

object SentinelPacker {
    private const val MAX_PAYLOAD = 220
    
    fun pack(message: String, sender: Short, receiver: Short): ByteArray {
        val compressed = SentinelCompression.compress(message.toByteArray())
        
        val payload = if (compressed.size > MAX_PAYLOAD) {
            compressed.sliceArray(0 until MAX_PAYLOAD)
        } else compressed
        
        val packet = SentinelPacket(
            type = PacketType.MESSAGE,
            senderId = sender,
            receiverId = receiver,
            flags = 0b00000001,
            sequence = (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            payload = payload,
            checksum = crc16(payload)
        )
        
        return packet.toByteArray()
    }
    
    private fun crc16(data: ByteArray): Short {
        var crc = 0xFFFF
        for (b in data) {
            crc = (crc ushr 8) xor SentinelLUT.CRC32[(crc xor b.toInt()) and 0xFF]
        }
        return crc.toShort()
    }
}
