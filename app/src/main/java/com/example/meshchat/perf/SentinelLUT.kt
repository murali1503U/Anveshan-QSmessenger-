package com.example.meshchat.perf

/**
 * Precomputed Look-Up Tables (LUTs) initialized at compile/load time
 * to prevent runtime computation costs.
 */
object SentinelLUT {
    // CRC32 table (precomputed)
    val CRC32 = IntArray(256).apply {
        for (i in 0..255) {
            var crc = i
            repeat(8) {
                crc = if ((crc and 1) != 0) (crc ushr 1) xor 0xEDB88320.toInt() else crc ushr 1
            }
            this[i] = crc
        }
    }
    
    // Sine table (precomputed)
    val SIN = FloatArray(6284) { i -> kotlin.math.sin(i * 0.001f) }
    val COS = FloatArray(6284) { i -> kotlin.math.cos(i * 0.001f) }
}
