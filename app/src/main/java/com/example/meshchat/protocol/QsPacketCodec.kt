package com.example.meshchat.protocol

import java.nio.ByteBuffer

object QsPacketCodec {
    private const val MAX_TRACKED_SEQS = 10000
    
    private val seenSequences = mutableMapOf<Short, java.util.LinkedHashMap<Int, Boolean>>()

    private fun getSenderCache(senderId: Short): java.util.LinkedHashMap<Int, Boolean> {
        return seenSequences.getOrPut(senderId) {
            object : java.util.LinkedHashMap<Int, Boolean>(MAX_TRACKED_SEQS, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Boolean>): Boolean {
                    return size > MAX_TRACKED_SEQS
                }
            }
        }
    }

    fun encode(message: QsMessage): ByteArray {
        val payloadLen = message.payload.size
        val hasSig = message.securityLevel >= 3
        val sigLen = if (hasSig) QsConstants.ML_DSA_SIG_SIZE else 0
        
        val capacity = 27 + payloadLen + sigLen + 2 // 27 byte header, payload, sig, 2 byte crc
        val buffer = ByteBuffer.allocate(capacity)
        
        buffer.put(QsConstants.MAGIC_BYTES)
        buffer.put(QsConstants.VERSION)
        buffer.put(message.securityLevel)
        buffer.putShort(message.senderId)
        buffer.putShort(message.receiverId)
        buffer.put(message.messageType)
        buffer.putInt(message.sequenceNumber)
        buffer.put(message.nonce)
        buffer.putShort(payloadLen.toShort())
        buffer.put(message.payload)
        
        if (hasSig) {
            buffer.put(message.signature!!)
        }
        
        val dataForCrc = buffer.array().copyOfRange(0, capacity - 2)
        val crc = calculateCrc16(dataForCrc)
        buffer.putShort(crc.toShort())
        
        return buffer.array()
    }

    fun decode(data: ByteArray): Result<QsMessage> {
        return runCatching {
            require(data.size <= QsConstants.MAX_PACKET_SIZE) { "Packet oversized" }
            require(data.size >= 30) { "Packet too small" } // header(27) + min payload(1) + crc(2)
            
            val buffer = ByteBuffer.wrap(data)
            
            val magic1 = buffer.get()
            val magic2 = buffer.get()
            require(magic1 == QsConstants.MAGIC_BYTES[0] && magic2 == QsConstants.MAGIC_BYTES[1]) { "Invalid magic bytes" }
            
            val version = buffer.get()
            require(version == QsConstants.VERSION) { "Invalid version" }
            
            val secLevel = buffer.get()
            require(secLevel in 1..4) { "Invalid security level" }
            
            val senderId = buffer.short
            val receiverId = buffer.short
            
            val msgType = buffer.get()
            require(msgType in 1..5) { "Invalid message type" }
            
            val seqNum = buffer.int
            val cache = getSenderCache(senderId)
            require(!cache.containsKey(seqNum)) { "Duplicate sequence number" }
            
            val nonce = ByteArray(12)
            buffer.get(nonce)
            
            val payloadLen = buffer.short.toInt()
            require(payloadLen in 1..QsConstants.MAX_PAYLOAD_SIZE) { "Invalid payload length" }
            
            val payload = ByteArray(payloadLen)
            buffer.get(payload)
            
            var signature: ByteArray? = null
            if (secLevel >= 3) {
                signature = ByteArray(QsConstants.ML_DSA_SIG_SIZE)
                buffer.get(signature)
                // Basic validation: signature shouldn't be all zero
                require(!signature.all { it == 0.toByte() }) { "Invalid ML-DSA signature" }
            }
            
            val packetCrc = buffer.short.toInt() and 0xFFFF
            val dataForCrc = data.copyOfRange(0, data.size - 2)
            val computedCrc = calculateCrc16(dataForCrc)
            
            require(packetCrc == computedCrc) { "CRC mismatch" }
            
            // Mark sequence as seen
            cache[seqNum] = true
            
            QsMessage(secLevel, senderId, receiverId, msgType, seqNum, nonce, payload, signature)
        }
    }

    private fun calculateCrc16(data: ByteArray): Int {
        var crc = 0xFFFF
        for (b in data) {
            crc = crc xor (b.toInt() and 0xFF).shl(8)
            for (i in 0..7) {
                crc = if ((crc and 0x8000) != 0) {
                    (crc shl 1) xor 0x1021
                } else {
                    crc shl 1
                }
            }
        }
        return crc and 0xFFFF
    }
}
