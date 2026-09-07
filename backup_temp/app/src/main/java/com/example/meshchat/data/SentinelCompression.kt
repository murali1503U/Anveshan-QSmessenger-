package com.example.meshchat.data

import com.github.luben.zstd.Zstd

object SentinelCompression {
    fun compress(data: ByteArray): ByteArray = Zstd.compress(data)
    
    fun decompress(data: ByteArray): ByteArray = Zstd.decompress(data, 4096)
    
    fun compressCoordinates(lat: Double, lon: Double, prevLat: Double, prevLon: Double): ByteArray {
        val deltaLat = ((lat - prevLat) * 1_000_000).toInt()
        val deltaLon = ((lon - prevLon) * 1_000_000).toInt()
        return byteArrayOf(
            (deltaLat ushr 24).toByte(),
            (deltaLat ushr 16).toByte(),
            (deltaLat ushr 8).toByte(),
            deltaLat.toByte(),
            (deltaLon ushr 24).toByte(),
            (deltaLon ushr 16).toByte(),
            (deltaLon ushr 8).toByte(),
            deltaLon.toByte()
        )
    }
}
