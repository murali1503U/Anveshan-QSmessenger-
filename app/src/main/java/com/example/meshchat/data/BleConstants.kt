package com.example.meshchat.data

import java.util.UUID

/**
 * Constants for connecting the Android App to an ESP32 or Arduino Nano 33 BLE.
 * We use the standard Nordic UART Service (NUS) UUIDs which are commonly used
 * in BLE-to-Serial bridging applications (like LoRa nodes).
 * 
 * Hardware Setup for ESP32:
 * 1. Create a BLE Server with SERVICE_UUID
 * 2. Create a BLE Characteristic for TX (Notify) with TX_CHARACTERISTIC_UUID
 * 3. Create a BLE Characteristic for RX (Write) with RX_CHARACTERISTIC_UUID
 */
object BleConstants {
    // The main service UUID the app will scan for
    val SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    
    // Characteristic for ESP32 -> App (App receives data here)
    val TX_CHARACTERISTIC_UUID: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
    
    // Characteristic for App -> ESP32 (App writes data here)
    val RX_CHARACTERISTIC_UUID: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
    
    // Maximum Transmission Unit for BLE (default is usually 20, but BLE 4.2+ supports up to 512)
    // We chunk large messages based on this size.
    const val BLE_MTU_SIZE = 128
}
