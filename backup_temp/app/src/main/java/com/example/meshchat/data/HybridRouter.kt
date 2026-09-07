package com.example.meshchat.data

import android.util.Log

// HybridRouter.kt — Routes messages: LoRa first, Bluetooth fallback
class HybridRouter(
    private val loRaHAL: LoRaHAL,
    private val bluetoothManager: RealBluetoothManager
) {
    companion object {
        const val PRIORITY_SOS = 100
        const val PRIORITY_NORMAL = 50
        const val PRIORITY_LOW = 10
    }

    private val loRaQueue = LoRaQueue()

    fun send(message: Message, priority: Int = PRIORITY_NORMAL) {
        val payloadStr = "${message.channelId}|${message.text}|${message.sender}|${message.mediaBase64 ?: ""}"
        val payload = payloadStr.toByteArray()

        // Step 1: Try LoRa first
        if (loRaHAL.isAvailable()) {
            loRaQueue.enqueue(payload, priority)
            val packet = loRaQueue.dequeue()
            if (packet != null && loRaHAL.send(packet.data)) {
                Log.d("HybridRouter", "Sent via LoRa (Priority: ${packet.priority})")
                return
            }
        }

        // Step 2: Fallback to Bluetooth
        Log.d("HybridRouter", "LoRa unavailable or send failed. Falling back to Bluetooth.")
        bluetoothManager.broadcast(payload)
    }

    fun receive(): Message? {
        // Check LoRa first
        val loraData = loRaHAL.receive()
        if (loraData != null) {
            return parsePayload(loraData, RadioTransport.LORA_MESH)
        }

        // Then Bluetooth
        val btData = bluetoothManager.receive()
        if (btData != null) {
            return parsePayload(btData, RadioTransport.BLUETOOTH_DIRECT)
        }

        return null
    }

    private fun parsePayload(payload: ByteArray, transport: RadioTransport): Message? {
        return try {
            val parts = String(payload).split("|")
            if (parts.size >= 3) {
                Message(
                    channelId = parts[0],
                    text = parts[1],
                    sender = parts[2],
                    mediaBase64 = if (parts.size >= 4 && parts[3].isNotEmpty()) parts[3] else null,
                    isSent = false,
                    transport = transport,
                    transportDetail = if (transport == RadioTransport.LORA_MESH) "LoRa Mesh Direct" else "Real BT Direct"
                )
            } else null
        } catch (e: Exception) {
            Log.e("HybridRouter", "Failed to parse payload", e)
            null
        }
    }
}
