package com.example.meshchat.protocol

object QsConstants {
    val MAGIC_BYTES = byteArrayOf(0x51.toByte(), 0x53.toByte())
    const val VERSION: Byte = 0x01
    const val MAX_PAYLOAD_SIZE = 200
    const val ML_DSA_SIG_SIZE = 3309
    const val MAX_PACKET_SIZE = 4096
}

data class QsMessage(
    val securityLevel: Byte,
    val senderId: Short,
    val receiverId: Short,
    val messageType: Byte,
    val sequenceNumber: Int,
    val nonce: ByteArray,
    val payload: ByteArray,
    val signature: ByteArray? = null
) {
    init {
        require(securityLevel in 1..4) { "Invalid security level" }
        require(messageType in 1..5) { "Invalid message type" }
        require(nonce.size == 12) { "Nonce must be 12 bytes" }
        require(payload.isNotEmpty()) { "Payload cannot be empty" }
        require(payload.size <= QsConstants.MAX_PAYLOAD_SIZE) { "Payload size exceeds ${QsConstants.MAX_PAYLOAD_SIZE} bytes" }
        if (securityLevel >= 3) {
            require(signature != null && signature.size == QsConstants.ML_DSA_SIG_SIZE) { "Signature required and must be ${QsConstants.ML_DSA_SIG_SIZE} bytes for security level >= 3" }
        } else {
            require(signature == null) { "Signature should be null for security level < 3" }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as QsMessage

        if (securityLevel != other.securityLevel) return false
        if (senderId != other.senderId) return false
        if (receiverId != other.receiverId) return false
        if (messageType != other.messageType) return false
        if (sequenceNumber != other.sequenceNumber) return false
        if (!nonce.contentEquals(other.nonce)) return false
        if (!payload.contentEquals(other.payload)) return false
        if (signature != null) {
            if (other.signature == null) return false
            if (!signature.contentEquals(other.signature)) return false
        } else if (other.signature != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = securityLevel.toInt()
        result = 31 * result + senderId
        result = 31 * result + receiverId
        result = 31 * result + messageType
        result = 31 * result + sequenceNumber
        result = 31 * result + nonce.contentHashCode()
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + (signature?.contentHashCode() ?: 0)
        return result
    }
}
