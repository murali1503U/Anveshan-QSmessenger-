package com.example.meshchat.perf

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import java.util.concurrent.ConcurrentLinkedQueue

object SentinelMemory {
    // 50MB in-memory LRU Cache
    val imageCache = object : LruCache<String, Bitmap>(50 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    // Object Pooling for LoRa packets to prevent GC churn
    class ByteBufferPool(private val maxSize: Int = 100) {
        private val pool = ConcurrentLinkedQueue<ByteArray>()
        
        fun borrow(size: Int): ByteArray = pool.poll()?.let {
            if (it.size >= size) it else ByteArray(size)
        } ?: ByteArray(size)
        
        fun returnBuffer(buffer: ByteArray) {
            if (pool.size < maxSize) {
                buffer.fill(0)
                pool.add(buffer)
            }
        }
    }
    
    val packetBufferPool = ByteBufferPool()

    // Fast and memory-efficient bitmap loading (RGB_565 halves memory usage)
    fun loadBitmapOptimized(data: ByteArray, reqWidth: Int = 512, reqHeight: Int = 512): Bitmap {
        val opts = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            BitmapFactory.decodeByteArray(data, 0, data.size, this)
            inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)
            inJustDecodeBounds = false
            inPreferredConfig = Bitmap.Config.RGB_565 // 50% less memory than ARGB_8888
        }
        return BitmapFactory.decodeByteArray(data, 0, data.size, opts)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
