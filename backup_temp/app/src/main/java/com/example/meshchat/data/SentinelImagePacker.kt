package com.example.meshchat.data

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

object SentinelImagePacker {
    
    // Decompression via SentinelMemory for aggressive 50% RAM reduction (RGB_565)
    fun decompressImage(data: ByteArray) = com.example.meshchat.perf.SentinelMemory.loadBitmapOptimized(data)

    fun compressImage(bitmap: Bitmap, quality: Int = 60): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, stream)
        val compressed = stream.toByteArray()
        
        return if (compressed.size > 220) {
            val scaled = Bitmap.createScaledBitmap(bitmap, 256, 256, true)
            val stream2 = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, stream2)
            stream2.toByteArray()
        } else {
            compressed
        }
    }
}
