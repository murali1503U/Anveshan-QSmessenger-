package com.example.meshchat.data

enum class ConnectionState {
    DISCONNECTED, SCANNING, CONNECTING, CONNECTED
}


data class IoTDevice(
    val id: String,
    val name: String,
    val rssi: Int,
    val isBleNearby: Boolean,
    val distanceEstimate: String,
    val radioType: RadioTransport
)
