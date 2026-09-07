package com.example.meshchat.data

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

enum class PacketType(val value: Byte) {
    MESSAGE(0x01),
    ACK(0x02),
    SOS(0x03),
    IMAGE(0x04),
    FILE(0x05),
    LOCATION(0x06)
}

data class SentinelPacket(
    val type: PacketType,
    val senderId: Short,       // 2 bytes
    val receiverId: Short,     // 2 bytes
    val flags: Byte,           // 1 byte (encrypted, compressed, etc.)
    val sequence: Int,         // 4 bytes
    val payload: ByteArray,    // Variable
    val checksum: Short        // 2 bytes
) {
    fun toByteArray(): ByteArray = ByteArrayOutputStream().use { bos ->
        DataOutputStream(bos).use { dos ->
            dos.writeByte(type.value.toInt())
            dos.writeShort(senderId.toInt())
            dos.writeShort(receiverId.toInt())
            dos.writeByte(flags.toInt())
            dos.writeInt(sequence)
            dos.writeShort(checksum.toInt())
            dos.write(payload)
            bos.toByteArray()
        }
    }
    
    companion object {
        fun fromByteArray(data: ByteArray): SentinelPacket = ByteArrayInputStream(data).use { bis ->
            DataInputStream(bis).use { dis ->
                val typeVal = dis.readByte()
                val type = PacketType.values().firstOrNull { it.value == typeVal } ?: PacketType.MESSAGE
                val sender = dis.readShort()
                val receiver = dis.readShort()
                val flags = dis.readByte()
                val seq = dis.readInt()
                val checksum = dis.readShort()
                val payload = ByteArray(dis.available())
                dis.readFully(payload)
                SentinelPacket(type, sender, receiver, flags, seq, payload, checksum)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SentinelPacket

        if (type != other.type) return false
        if (senderId != other.senderId) return false
        if (receiverId != other.receiverId) return false
        if (flags != other.flags) return false
        if (sequence != other.sequence) return false
        if (!payload.contentEquals(other.payload)) return false
        if (checksum != other.checksum) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + senderId
        result = 31 * result + receiverId
        result = 31 * result + flags
        result = 31 * result + sequence
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + checksum
        return result
    }
}
