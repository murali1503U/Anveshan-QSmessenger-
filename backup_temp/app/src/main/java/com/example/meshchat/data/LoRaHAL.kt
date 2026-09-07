package com.example.meshchat.data

// LoRaHAL.kt — Hardware abstraction for LoRa radio
interface LoRaHAL {
    fun begin(frequency: Long, sf: Int, bw: Int): Boolean
    fun send(data: ByteArray): Boolean
    fun receive(): ByteArray?
    fun getRssi(): Int
    fun getSnr(): Float
    fun isAvailable(): Boolean
    fun sleep()
    fun wake()
}

// Implementation for SX1278 (ESP32/Arduino via JNI or Serial)
class SX1278LoRa : LoRaHAL {
    // In a real JNI implementation, these would be external functions.
    // We stub them here to represent the HAL layer as requested.
    override fun begin(frequency: Long, sf: Int, bw: Int): Boolean = true
    override fun send(data: ByteArray): Boolean = true
    override fun receive(): ByteArray? = null
    override fun getRssi(): Int = -80
    override fun getSnr(): Float = 9.5f
    override fun isAvailable(): Boolean = true
    override fun sleep() {}
    override fun wake() {}
}
