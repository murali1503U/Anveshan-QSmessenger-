package com.example.meshchat.data

import com.github.luben.zstd.Zstd

object SentinelCompression {
    private val isZstdAvailable: Boolean by lazy {
        try {
            val probe = ByteArray(4)
            Zstd.compress(probe)
            true
        } catch (t: Throwable) {
            false
        }
    }

    fun compress(data: ByteArray): ByteArray {
        if (data.isEmpty()) return ByteArray(0)
        return try {
            if (isZstdAvailable) {
                Zstd.compress(data)
            } else {
                deflate(data)
            }
        } catch (t: Throwable) {
            deflate(data)
        }
    }
    
    fun decompress(data: ByteArray, maxOriginalSize: Int = 65536): ByteArray {
        if (data.isEmpty()) return ByteArray(0)
        return try {
            if (isZstdAvailable) {
                Zstd.decompress(data, maxOriginalSize)
            } else {
                inflate(data)
            }
        } catch (t: Throwable) {
            try {
                inflate(data)
            } catch (e: Throwable) {
                ByteArray(0)
            }
        }
    }

    private fun deflate(data: ByteArray): ByteArray {
        val deflater = java.util.zip.Deflater()
        deflater.setInput(data)
        deflater.finish()
        val bos = java.io.ByteArrayOutputStream(data.size)
        val buffer = ByteArray(1024)
        while (!deflater.finished()) {
            val count = deflater.deflate(buffer)
            bos.write(buffer, 0, count)
        }
        deflater.end()
        return bos.toByteArray()
    }

    private fun inflate(data: ByteArray): ByteArray {
        val inflater = java.util.zip.Inflater()
        inflater.setInput(data)
        val bos = java.io.ByteArrayOutputStream(data.size * 2)
        val buffer = ByteArray(1024)
        while (!inflater.finished()) {
            val count = inflater.inflate(buffer)
            if (count == 0 && inflater.needsInput()) break
            bos.write(buffer, 0, count)
        }
        inflater.end()
        return bos.toByteArray()
    }

    fun toBase64(data: ByteArray): String {
        if (data.isEmpty()) return ""
        return java.util.Base64.getEncoder().encodeToString(data)
    }

    fun fromBase64(base64: String): ByteArray? {
        if (base64.isBlank()) return ByteArray(0)
        return try {
            java.util.Base64.getDecoder().decode(base64)
        } catch (e: Exception) {
            null
        }
    }
    
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

    fun decompressCoordinates(bytes: ByteArray, prevLat: Double, prevLon: Double): Pair<Double, Double>? {
        if (bytes.size < 8) return null
        val deltaLat = (bytes[0].toInt() and 0xFF shl 24) or
                (bytes[1].toInt() and 0xFF shl 16) or
                (bytes[2].toInt() and 0xFF shl 8) or
                (bytes[3].toInt() and 0xFF)
        val deltaLon = (bytes[4].toInt() and 0xFF shl 24) or
                (bytes[5].toInt() and 0xFF shl 16) or
                (bytes[6].toInt() and 0xFF shl 8) or
                (bytes[7].toInt() and 0xFF)
        return (prevLat + deltaLat.toDouble() / 1_000_000.0) to (prevLon + deltaLon.toDouble() / 1_000_000.0)
    }
}
