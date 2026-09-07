package com.example.meshchat.util

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Choreographer

/**
 * Sentinel Performance optimizations applied to UI thread and memory management
 * for butter-smooth rendering.
 */
object SentinelJankDetector {
    private const val FRAME_TIME_THRESHOLD = 16_000_000L // 16ms in nanoseconds
    private var frameCount = 0
    private var jankCount = 0
    private val jankHistory = mutableListOf<Long>()
    
    private val frameCallback = object : Choreographer.FrameCallback {
        private var lastFrameTime = 0L
        
        override fun doFrame(frameTimeNanos: Long) {
            if (lastFrameTime > 0) {
                val elapsed = frameTimeNanos - lastFrameTime
                frameCount++
                if (elapsed > FRAME_TIME_THRESHOLD) {
                    jankCount++
                    jankHistory.add(elapsed)
                    if (elapsed > 32_000_000L) {
                        Log.w("SentinelJankDetector", "Major Jank detected: ${elapsed / 1_000_000}ms")
                    }
                }
            }
            lastFrameTime = frameTimeNanos
            Choreographer.getInstance().postFrameCallback(this)
        }
    }
    
    fun startMonitoring() {
        SentinelUiThread.post {
            Choreographer.getInstance().postFrameCallback(frameCallback)
            Log.i("SentinelJankDetector", "Started monitoring UI frames")
        }
    }
    
    fun getJankStats(): String {
        return if (frameCount > 0) {
            val jankRate = (jankCount.toFloat() / frameCount) * 100
            "Frames: $frameCount, Jank: $jankCount (${"%.2f".format(jankRate)}%)"
        } else "No frames yet"
    }
}

object SentinelUiThread {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val uiThread = Looper.getMainLooper().thread
    
    fun post(task: () -> Unit) {
        mainHandler.post(task)
    }
    
    fun run(task: () -> Unit) {
        if (Thread.currentThread() == uiThread) {
            task()
        } else {
            mainHandler.post(task)
        }
    }
}

object SentinelMemory {
    private var gcCount = 0
    private var lastGcTime = 0L

    fun optimizeForSmoothness() {
        val runtime = Runtime.getRuntime()
        runtime.gc()
    }
    
    fun triggerGcIfNeeded() {
        if (gcCount % 10 == 0) {
            System.gc()
        }
        gcCount++
    }
}
