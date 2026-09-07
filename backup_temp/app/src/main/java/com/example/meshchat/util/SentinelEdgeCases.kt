package com.example.meshchat.util

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.app.ActivityCompat
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Sentinel QA Edge Case & Performance Fixes
 */
object SentinelEdgeCases {

    // Fix DA-02: Chunk long messages
    fun sendLongMessage(text: String, sendPacket: (String) -> Unit) {
        val maxLen = 250
        if (text.length > maxLen) {
            val chunks = text.chunked(maxLen)
            chunks.forEachIndexed { i, chunk ->
                sendPacket("${i + 1}/${chunks.size}:$chunk")
            }
        } else {
            sendPacket(text)
        }
    }

    // Fix DA-05: Duplicate detection using a lightweight heuristic (Bloom Filter approximation)
    private val seenIds = ConcurrentHashMap.newKeySet<String>()
    
    fun isDuplicate(id: String): Boolean {
        // In a real BloomFilter, this would be:
        // if (bloomFilter.mightContain(id)) return true
        // bloomFilter.add(id)
        if (seenIds.contains(id)) return true
        seenIds.add(id)
        // Prevent unbounded memory growth in memory-constrained environment
        if (seenIds.size > 100_000) {
            seenIds.clear() // Simple eviction for demo
        }
        return false
    }

    // Fix ME-04: Limit image memory
    fun loadImageOptimized(data: ByteArray): Bitmap {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            BitmapFactory.decodeByteArray(data, 0, data.size, this)
            inSampleSize = calculateInSampleSize(this, 512, 512)
            inJustDecodeBounds = false
        }
        return BitmapFactory.decodeByteArray(data, 0, data.size, options)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    // Fix LL-03: Graceful fallback
    fun initializeModel(modelFile: File, loadModel: (File) -> Unit, useHeuristicFallback: () -> Unit): Boolean {
        return try {
            if (modelFile.exists()) {
                loadModel(modelFile)
                true
            } else {
                Log.w("SentinelQSVM", "Model missing, using heuristic")
                useHeuristicFallback()
                true
            }
        } catch (e: Exception) {
            Log.e("SentinelQSVM", "Model load failed", e)
            useHeuristicFallback()
            false
        }
    }

    // Fix BT-04: Permission handling
    fun safeStartDiscovery(
        context: Context,
        bluetoothAdapter: BluetoothAdapter?,
        requestBluetoothPermissions: () -> Unit
    ): Boolean {
        if (bluetoothAdapter == null) return false
        return try {
            val hasPermission = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
            
            if (hasPermission) {
                bluetoothAdapter.startDiscovery()
                true
            } else {
                requestBluetoothPermissions()
                false
            }
        } catch (e: SecurityException) {
            Log.e("SentinelBT", "Permission denied", e)
            false
        }
    }
}
