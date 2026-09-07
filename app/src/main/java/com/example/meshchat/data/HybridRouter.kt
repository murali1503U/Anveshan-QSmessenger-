package com.example.meshchat.data

import android.util.Log

// HybridRouter.kt — Routes messages: Wi-Fi HTTP/TCP Node first, LoRa HAL, Bluetooth fallback
class HybridRouter(
    private val loRaHAL: LoRaHAL,
    private val bluetoothManager: RealBluetoothManager,
    private val wifiTransport: WifiTcpTransport? = null,
    private val httpTransport: HttpNodeTransport? = null
) {
    companion object {
        const val PRIORITY_SOS = 100
        const val PRIORITY_NORMAL = 50
        const val PRIORITY_LOW = 10
    }

    private val loRaQueue = LoRaQueue()

    fun getActiveTransport(): RadioTransport {
        return when {
            httpTransport?.connectionState?.value == ConnectionState.CONNECTED -> RadioTransport.WIFI_TCP_NODE
            wifiTransport?.connectionState?.value == ConnectionState.CONNECTED -> RadioTransport.WIFI_TCP_NODE
            bluetoothManager.connectionState.value == ConnectionState.CONNECTED -> RadioTransport.BLUETOOTH_DIRECT
            loRaHAL.isAvailable() -> RadioTransport.LORA_MESH
            else -> RadioTransport.HYBRID_AUTO
        }
    }

    suspend fun send(message: Message, priority: Int = PRIORITY_NORMAL): Boolean {
        val payloadStr = "${message.channelId}|${message.text}|${message.sender}|${message.mediaBase64 ?: ""}"
        val payload = payloadStr.toByteArray(Charsets.UTF_8)

        // Step 1A: Wi-Fi HTTP REST Node Bridge (Preferred for WebServer firmware on port 80)
        if (httpTransport?.connectionState?.value == ConnectionState.CONNECTED) {
            val success = httpTransport.send(payloadStr)
            if (success) {
                Log.d("HybridRouter", "Sent via ESP HTTP REST Bridge (${httpTransport.nodeBoardType.value.displayName} @ ${httpTransport.nodeIp.value})")
                return true
            }
        }

        // Step 1B: Wi-Fi TCP Node Link (For binary socket firmware on port 8266)
        if (wifiTransport?.connectionState?.value == ConnectionState.CONNECTED) {
            val success = wifiTransport.sendMessagePayload(
                channelId = message.channelId,
                text = message.text,
                senderName = message.sender,
                isEncrypted = message.isEncrypted
            )
            if (success) {
                Log.d("HybridRouter", "Sent via Wi-Fi TCP Node (${wifiTransport.nodeBoardType.value.displayName})")
                return true
            }
        }

        // Step 2: Try LoRa HAL (if local LoRa peripheral/bridge is active)
        if (loRaHAL.isAvailable()) {
            loRaQueue.enqueue(payload, priority)
            val packet = loRaQueue.dequeue()
            if (packet != null && loRaHAL.send(packet.data)) {
                Log.d("HybridRouter", "Sent via LoRa HAL (Priority: ${packet.priority})")
                return true
            }
        }

        // Step 3: Fallback to Bluetooth Classic / BLE SPP (ESP32 only)
        if (bluetoothManager.connectionState.value == ConnectionState.CONNECTED) {
            Log.d("HybridRouter", "Sending via Bluetooth fallback.")
            bluetoothManager.broadcast(payload)
            return true
        }

        Log.w("HybridRouter", "No active hardware transport connected to send message.")
        return false
    }

    fun receive(): Message? {
        // 1. Check LoRa HAL
        val loraData = loRaHAL.receive()
        if (loraData != null) {
            return parsePayload(loraData, RadioTransport.LORA_MESH)
        }

        // 2. Check Bluetooth
        val btData = bluetoothManager.receive()
        if (btData != null) {
            return parsePayload(btData, RadioTransport.BLUETOOTH_DIRECT)
        }

        return null
    }

    fun parsePayload(payload: ByteArray, transport: RadioTransport): Message? {
        return try {
            val parts = String(payload, Charsets.UTF_8).split("|")
            if (parts.size >= 3) {
                val detailStr = when (transport) {
                    RadioTransport.WIFI_TCP_NODE -> "Wi-Fi ESP LoRa Bridge"
                    RadioTransport.LORA_MESH -> "LoRa Mesh Direct"
                    RadioTransport.BLUETOOTH_DIRECT -> "Real BT Direct"
                    else -> "Hybrid Auto"
                }
                Message(
                    channelId = parts[0],
                    text = parts[1],
                    sender = parts[2],
                    mediaBase64 = if (parts.size >= 4 && parts[3].isNotEmpty()) parts[3] else null,
                    isSent = false,
                    transport = transport,
                    transportDetail = detailStr
                )
            } else null
        } catch (e: Exception) {
            Log.e("HybridRouter", "Failed to parse payload", e)
            null
        }
    }
}
