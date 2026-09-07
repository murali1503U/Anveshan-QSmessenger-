package com.example.meshchat.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RadioTransport {
    HYBRID_AUTO,       // Smart Auto-Routing: BLE if nearby (<100m), LoRa mesh if remote
    BLUETOOTH_DIRECT,  // Direct Bluetooth P2P (2.4 GHz, high bandwidth, <100m)
    LORA_MESH          // Sub-GHz LoRa Mesh (868/915 MHz, long-range multi-hop)
}

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val channelId: String = "global_broadcast",
    val text: String,
    val sender: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isEncrypted: Boolean = false,
    val isSent: Boolean = false, // true if sent by user, false if received
    val isConfidential: Boolean = false, // marked by AI
    val isDemo: Boolean = false,
    val mediaBase64: String? = null,
    val isMediaCorrupted: Boolean = false,
    val securityLevel: Int = 0, // 0=Auto, 1=Plain, 2=AES, 3=Session, 4=Post-Quantum
    val transport: RadioTransport = RadioTransport.HYBRID_AUTO,
    val transportDetail: String = "LoRa Mesh • 868 MHz" // e.g. "BLE Direct • -48 dBm" or "LoRa Mesh • 2 Hops"
)
