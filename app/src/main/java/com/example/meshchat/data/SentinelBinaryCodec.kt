package com.example.meshchat.data

import java.nio.ByteBuffer
import java.nio.ByteOrder

enum class BinaryPacketType(val raw: Byte) {
    MESSAGE(0x01.toByte()),
    PING(0x02.toByte()),
    ACK(0x03.toByte()),
    SOS(0x04.toByte()),
    IDENTITY(0x05.toByte()),
    AUTH_REQ(0x10.toByte()),
    AUTH_RESP(0x11.toByte()),
    AUTH_OK(0x12.toByte()),
    AUTH_FAIL(0x13.toByte());

    companion object {
        fun fromByte(b: Byte): BinaryPacketType? = values().firstOrNull { it.raw == b }
    }
}

enum class NodeBoardType(val raw: Byte, val displayName: String) {
    UNKNOWN(0x00.toByte(), "Unknown Hardware"),
    ESP32(0x01.toByte(), "ESP32 LoRa Gateway"),
    ESP8266_ESP12E(0x02.toByte(), "ESP-12E (ESP8266) Wi-Fi Node"),
    ARDUINO_LORA(0x03.toByte(), "Arduino LoRa Bridge");

    companion object {
        fun fromByte(b: Byte): NodeBoardType = values().firstOrNull { it.raw == b } ?: UNKNOWN
    }
}

sealed class BinaryDecodeResult {
    data class Success(val packet: SentinelBinaryPacket) : BinaryDecodeResult()
    data class Error(val errorType: ErrorType, val message: String) : BinaryDecodeResult() {
        enum class ErrorType {
            EMPTY_PACKET,
            MALFORMED_HEADER,
            INVALID_MAGIC,
            UNSUPPORTED_VERSION,
            UNKNOWN_TYPE,
            OVERSIZED_PAYLOAD,
            CORRUPTED_CRC,
            TRUNCATED_PACKET
        }
    }
}

class PacketDeduplicator(private val windowSize: Int = 128) {
    private val seenSequences = mutableMapOf<Short, MutableSet<Byte>>()

    @Synchronized
    fun isDuplicate(senderId: Short, sequence: Byte): Boolean {
        val window = seenSequences.getOrPut(senderId) { mutableSetOf() }
        if (window.contains(sequence)) {
            return true
        }
        if (window.size >= windowSize) {
            window.clear()
        }
        window.add(sequence)
        return false
    }

    @Synchronized
    fun reset() {
        seenSequences.clear()
    }
}

data class SentinelBinaryPacket(
    val type: BinaryPacketType,
    val senderId: Short,
    val receiverId: Short,
    val sequence: Byte,
    val flags: Byte = 0,
    val payload: ByteArray = ByteArray(0)
) {
    companion object {
        const val MAGIC: Byte = 0xA5.toByte()
        const val VERSION: Byte = 0x01.toByte()
        const val HEADER_SIZE = 10 // Magic(1) + Ver(1) + Type(1) + Sender(2) + Recv(2) + Seq(1) + Flags(1) + Len(1)
        const val FOOTER_SIZE = 2 // CRC16(2)
        const val MIN_PACKET_SIZE = HEADER_SIZE + FOOTER_SIZE
        const val MAX_PAYLOAD_SIZE = 255
        const val MAX_PACKET_SIZE = HEADER_SIZE + MAX_PAYLOAD_SIZE + FOOTER_SIZE

        fun calculateCrc16(data: ByteArray, offset: Int = 0, length: Int = data.size): Short {
            var crc = 0xFFFF
            for (i in offset until (offset + length)) {
                crc = crc xor ((data[i].toInt() and 0xFF) shl 8)
                for (j in 0 until 8) {
                    crc = if ((crc and 0x8000) != 0) {
                        ((crc shl 1) xor 0x1021) and 0xFFFF
                    } else {
                        (crc shl 1) and 0xFFFF
                    }
                }
            }
            return (crc and 0xFFFF).toShort()
        }

        fun decodeSafe(bytes: ByteArray?): BinaryDecodeResult {
            if (bytes == null || bytes.isEmpty()) {
                return BinaryDecodeResult.Error(
                    BinaryDecodeResult.Error.ErrorType.EMPTY_PACKET,
                    "Empty or null packet"
                )
            }
            if (bytes.size < MIN_PACKET_SIZE) {
                return BinaryDecodeResult.Error(
                    BinaryDecodeResult.Error.ErrorType.TRUNCATED_PACKET,
                    "Packet size ${bytes.size} is less than minimum $MIN_PACKET_SIZE"
                )
            }
            if (bytes[0] != MAGIC) {
                return BinaryDecodeResult.Error(
                    BinaryDecodeResult.Error.ErrorType.INVALID_MAGIC,
                    "Invalid magic byte: 0x${(bytes[0].toInt() and 0xFF).toString(16).uppercase()}"
                )
            }
            if (bytes[1] != VERSION) {
                return BinaryDecodeResult.Error(
                    BinaryDecodeResult.Error.ErrorType.UNSUPPORTED_VERSION,
                    "Unsupported protocol version: ${bytes[1]}"
                )
            }

            val type = BinaryPacketType.fromByte(bytes[2])
                ?: return BinaryDecodeResult.Error(
                    BinaryDecodeResult.Error.ErrorType.UNKNOWN_TYPE,
                    "Unknown packet type: ${bytes[2]}"
                )

            return try {
                val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
                buffer.position(3)
                val senderId = buffer.short
                val receiverId = buffer.short
                val seq = buffer.get()
                val flags = buffer.get()
                val payloadLen = buffer.get().toInt() and 0xFF

                if (payloadLen > MAX_PAYLOAD_SIZE) {
                    return BinaryDecodeResult.Error(
                        BinaryDecodeResult.Error.ErrorType.OVERSIZED_PAYLOAD,
                        "Payload length $payloadLen exceeds maximum $MAX_PAYLOAD_SIZE"
                    )
                }

                val expectedTotalSize = HEADER_SIZE + payloadLen + FOOTER_SIZE
                if (bytes.size < expectedTotalSize) {
                    return BinaryDecodeResult.Error(
                        BinaryDecodeResult.Error.ErrorType.TRUNCATED_PACKET,
                        "Packet truncated: expected $expectedTotalSize bytes, got ${bytes.size}"
                    )
                }

                val payload = ByteArray(payloadLen)
                buffer.get(payload)
                val expectedCrc = buffer.short

                val actualCrc = calculateCrc16(bytes, 0, HEADER_SIZE + payloadLen)
                if (expectedCrc != actualCrc) {
                    return BinaryDecodeResult.Error(
                        BinaryDecodeResult.Error.ErrorType.CORRUPTED_CRC,
                        "CRC mismatch: expected $expectedCrc, got $actualCrc"
                    )
                }

                BinaryDecodeResult.Success(
                    SentinelBinaryPacket(type, senderId, receiverId, seq, flags, payload)
                )
            } catch (e: Exception) {
                BinaryDecodeResult.Error(
                    BinaryDecodeResult.Error.ErrorType.MALFORMED_HEADER,
                    "Malformed packet: ${e.message}"
                )
            }
        }

        fun decode(bytes: ByteArray?): SentinelBinaryPacket? {
            return when (val res = decodeSafe(bytes)) {
                is BinaryDecodeResult.Success -> res.packet
                is BinaryDecodeResult.Error -> null
            }
        }
    }

    fun encodeSafe(): Result<ByteArray> {
        if (payload.size > MAX_PAYLOAD_SIZE) {
            return Result.failure(
                IllegalArgumentException("Payload size ${payload.size} exceeds maximum $MAX_PAYLOAD_SIZE")
            )
        }
        val payloadLen = payload.size
        val totalSize = HEADER_SIZE + payloadLen + FOOTER_SIZE
        val buffer = ByteBuffer.allocate(totalSize).order(ByteOrder.BIG_ENDIAN)

        buffer.put(MAGIC)
        buffer.put(VERSION)
        buffer.put(type.raw)
        buffer.putShort(senderId)
        buffer.putShort(receiverId)
        buffer.put(sequence)
        buffer.put(flags)
        buffer.put(payloadLen.toByte())
        buffer.put(payload, 0, payloadLen)

        val crc = calculateCrc16(buffer.array(), 0, HEADER_SIZE + payloadLen)
        buffer.putShort(crc)

        return Result.success(buffer.array())
    }

    fun encode(): ByteArray {
        val payloadLen = payload.size.coerceAtMost(MAX_PAYLOAD_SIZE)
        val totalSize = HEADER_SIZE + payloadLen + FOOTER_SIZE
        val buffer = ByteBuffer.allocate(totalSize).order(ByteOrder.BIG_ENDIAN)

        buffer.put(MAGIC)
        buffer.put(VERSION)
        buffer.put(type.raw)
        buffer.putShort(senderId)
        buffer.putShort(receiverId)
        buffer.put(sequence)
        buffer.put(flags)
        buffer.put(payloadLen.toByte())
        buffer.put(payload, 0, payloadLen)

        val crc = calculateCrc16(buffer.array(), 0, HEADER_SIZE + payloadLen)
        buffer.putShort(crc)

        return buffer.array()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SentinelBinaryPacket
        if (type != other.type) return false
        if (senderId != other.senderId) return false
        if (receiverId != other.receiverId) return false
        if (sequence != other.sequence) return false
        if (flags != other.flags) return false
        if (!payload.contentEquals(other.payload)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + senderId
        result = 31 * result + receiverId
        result = 31 * result + sequence
        result = 31 * result + flags
        result = 31 * result + payload.contentHashCode()
        return result
    }
}
